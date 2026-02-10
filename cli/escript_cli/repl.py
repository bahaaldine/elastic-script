"""
Interactive REPL for elastic-script.

Features:
- Syntax highlighting
- Auto-completion
- Multi-line editing
- Command history
- Smart statement continuation
"""

import os
from typing import Optional

from prompt_toolkit import PromptSession
from prompt_toolkit.lexers import PygmentsLexer
from prompt_toolkit.history import FileHistory
from prompt_toolkit.auto_suggest import AutoSuggestFromHistory
from prompt_toolkit.key_binding import KeyBindings
from prompt_toolkit.keys import Keys
from prompt_toolkit.styles import Style
from prompt_toolkit.formatted_text import HTML

from .lexer import ElasticScriptLexer
from .config import Config
from .client import ElasticScriptClient
from .completer import ElasticScriptCompleter
from .output import OutputFormatter


# Custom style for the prompt
PROMPT_STYLE = Style.from_dict({
    'prompt': 'ansigreen bold',
    'continuation': 'ansigray',
    'rprompt': 'ansigray italic',
})

# Keywords that indicate statement continues
CONTINUATION_KEYWORDS = [
    'BEGIN', 'THEN', 'LOOP', 'AS', 'IS', 'BODY',
    'TRY', 'DO', 'CASE', 'PARALLEL', '('
]

# Keywords that start blocks (need END)
BLOCK_STARTERS = {'BEGIN', 'IF', 'FOR', 'WHILE', 'TRY', 'CASE', 'LOOP'}

# Keywords that end blocks
BLOCK_ENDERS = {'END', 'END;'}


class ElasticScriptREPL:
    """Interactive REPL for elastic-script."""
    
    def __init__(self, config: Config):
        self.config = config
        self.client = ElasticScriptClient(config)
        self.output = OutputFormatter(config)
        self.completer = ElasticScriptCompleter()
        
        # Set up history
        history_path = config.history_path
        history_path.parent.mkdir(parents=True, exist_ok=True)
        
        # Create prompt session
        # Note: multiline=False so Enter submits. We handle multi-line manually.
        self.session = PromptSession(
            lexer=PygmentsLexer(ElasticScriptLexer),
            completer=self.completer,
            complete_while_typing=True,
            auto_suggest=AutoSuggestFromHistory(),
            history=FileHistory(str(history_path)),
            style=PROMPT_STYLE,
            multiline=False,  # We handle multi-line manually
            key_bindings=self._create_key_bindings(),
            enable_history_search=True,
        )
        
        self._multiline_buffer = []
        self._in_multiline = False
    
    def _create_key_bindings(self) -> KeyBindings:
        """Create custom key bindings."""
        kb = KeyBindings()
        
        @kb.add(Keys.ControlD)
        def _(event):
            """Exit on Ctrl+D."""
            if not event.current_buffer.text:
                event.app.exit()
            else:
                event.current_buffer.delete()
        
        @kb.add(Keys.ControlL)
        def _(event):
            """Clear screen on Ctrl+L."""
            os.system('clear' if os.name == 'posix' else 'cls')
            event.app.renderer.clear()
        
        return kb
    
    def _continuation_prompt(self, width, line_number, is_soft_wrap):
        """Generate continuation prompt for multi-line input."""
        return HTML('<continuation>   ... </continuation>')
    
    def _needs_continuation(self, text: str) -> bool:
        """Check if the statement needs more input."""
        text = text.strip()
        
        # Empty line ends multi-line
        if not text and self._in_multiline:
            return False
        
        # Empty input doesn't need continuation
        if not text:
            return False
        
        upper_text = text.upper()
        
        # Check if we started a CREATE PROCEDURE/FUNCTION that hasn't ended
        if ('CREATE PROCEDURE' in upper_text or 'CREATE FUNCTION' in upper_text):
            if 'END PROCEDURE' not in upper_text and 'END FUNCTION' not in upper_text:
                return True
        
        # Check if we started a CREATE SKILL that hasn't ended
        if 'CREATE SKILL' in upper_text and 'END SKILL' not in upper_text:
            return True
        
        # Check if we started TRY that hasn't ended
        if 'TRY' in upper_text and 'END TRY' not in upper_text:
            return True
        
        # Semicolon at end usually means complete
        if text.endswith(';'):
            # But check for block balance
            return not self._is_balanced(text)
        
        # Check for continuation keywords at end
        for keyword in CONTINUATION_KEYWORDS:
            if upper_text.endswith(keyword):
                return True
        
        # Check block balance
        return not self._is_balanced(text)
    
    def _is_balanced(self, text: str) -> bool:
        """Check if BEGIN/END blocks are balanced."""
        upper_text = text.upper()
        
        # Count block starters and enders
        # This is a simple heuristic - not perfect
        begins = upper_text.count('BEGIN') + upper_text.count('THEN') + upper_text.count('LOOP')
        ends = upper_text.count('END ') + upper_text.count('END;') + upper_text.count('END\n')
        
        # Also count END PROCEDURE, END FUNCTION, etc.
        ends += upper_text.count('END PROCEDURE')
        ends += upper_text.count('END FUNCTION')
        ends += upper_text.count('END IF')
        ends += upper_text.count('END LOOP')
        
        # Parentheses balance
        parens = text.count('(') - text.count(')')
        
        return begins <= ends and parens <= 0
    
    def run(self):
        """Run the REPL."""
        # Test connection
        success, message = self.client.test_connection()
        if not success:
            self.output.print_error(message)
            self.output.print_info(f"Trying to connect to {self.config.url}")
            return
        
        # Print welcome
        self.output.print_welcome(message)
        
        # Load completions from server
        self._load_completions()
        
        # Main loop
        while True:
            try:
                # Get input
                text = self._get_input()
                
                if text is None:
                    # EOF
                    break
                
                if not text.strip():
                    continue
                
                # Handle special commands
                if self._handle_command(text):
                    continue
                
                # Execute query
                result = self.client.execute(text)
                self.output.print_result(result)
                
                # Refresh completions after CREATE/DROP
                upper = text.upper()
                if 'CREATE' in upper or 'DROP' in upper:
                    self._load_completions()
            
            except KeyboardInterrupt:
                self.output.print_info("Use Ctrl+D or 'exit' to quit")
                continue
            except EOFError:
                break
            except Exception as e:
                self.output.print_error(str(e))
        
        self.output.print_info("Goodbye!")
        self.client.close()
    
    # Special commands that should be executed immediately (no multi-line)
    IMMEDIATE_COMMANDS = {'help', 'exit', 'quit', 'q', 'clear', 'history', 'config', 'refresh'}
    # Help topics
    HELP_TOPICS = {'examples', 'tutorial', 'functions', 'syntax'}
    
    def _get_input(self) -> Optional[str]:
        """Get input, handling multi-line statements."""
        try:
            # Get initial line
            text = self.session.prompt(
                HTML('<prompt>escript> </prompt>'),
            )
            
            # Check if it's a special command - execute immediately
            cmd_lower = text.strip().lower()
            if cmd_lower in self.IMMEDIATE_COMMANDS:
                return text
            # Also handle "help <topic>" commands
            if cmd_lower.startswith('help '):
                topic = cmd_lower.split(' ', 1)[1].strip()
                if topic in self.HELP_TOPICS:
                    return text
            
            # Check for multi-line
            self._multiline_buffer = [text]
            self._in_multiline = self._needs_continuation(text)
            
            while self._in_multiline:
                line = self.session.prompt(
                    HTML('<continuation>   ... </continuation>'),
                )
                self._multiline_buffer.append(line)
                combined = '\n'.join(self._multiline_buffer)
                self._in_multiline = self._needs_continuation(combined)
            
            return '\n'.join(self._multiline_buffer)
        
        except EOFError:
            return None
    
    def _handle_command(self, text: str) -> bool:
        """
        Handle special CLI commands.
        
        Returns True if the command was handled (don't execute as query).
        """
        cmd = text.strip().lower()
        
        if cmd in ('exit', 'quit', 'q'):
            raise EOFError()
        
        if cmd == 'help':
            self.output.print_help()
            return True
        
        if cmd.startswith('help '):
            topic = cmd.split(' ', 1)[1].strip()
            self.output.print_help(topic)
            return True
        
        if cmd == 'clear':
            os.system('clear' if os.name == 'posix' else 'cls')
            return True
        
        if cmd == 'history':
            self._show_history()
            return True
        
        if cmd == 'config':
            self._show_config()
            return True
        
        if cmd == 'refresh':
            self._load_completions()
            self.output.print_success("Completions refreshed")
            return True
        
        return False
    
    def _show_history(self):
        """Show command history."""
        history_path = self.config.history_path
        if history_path.exists():
            with open(history_path) as f:
                lines = f.readlines()[-20:]  # Last 20 commands
            
            self.output.console.print("[bold]Recent Commands:[/]")
            for i, line in enumerate(lines, 1):
                line = line.strip().replace('\n', ' ')[:80]
                self.output.console.print(f"  [cyan]{i:2}[/] {line}")
        else:
            self.output.print_info("No history yet")
    
    def _show_config(self):
        """Show current configuration."""
        from rich.table import Table
        
        table = Table(title="Configuration", box=self.output._box)
        table.add_column("Setting", style="cyan")
        table.add_column("Value")
        
        table.add_row("Host", f"{self.config.host}:{self.config.port}")
        table.add_row("Username", self.config.username)
        table.add_row("SSL", str(self.config.use_ssl))
        table.add_row("Multi-line", str(self.config.multiline))
        table.add_row("Vi Mode", str(self.config.vi_mode))
        table.add_row("History", str(self.config.history_path))
        table.add_row("Color", str(self.config.color))
        table.add_row("Table Style", self.config.table_style)
        
        self.output.console.print(table)
    
    def _load_completions(self):
        """Load procedure/function/skill names for completion."""
        # Try to load procedures
        result = self.client.list_procedures()
        if result.success and isinstance(result.data, list):
            names = [p.get("name", p) if isinstance(p, dict) else str(p) 
                     for p in result.data]
            self.completer.update_procedures(names)
        
        # Try to load skills
        result = self.client.list_skills()
        if result.success and isinstance(result.data, list):
            names = [s.get("name", s) if isinstance(s, dict) else str(s) 
                     for s in result.data]
            self.completer.update_skills(names)
