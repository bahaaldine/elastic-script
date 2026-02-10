"""
Output formatting for elastic-script CLI using Rich.

Provides beautiful formatting for:
- Query results (tables, JSON)
- Errors (panels with context)
- Status messages
- PRINT output
"""

import json
from typing import Any, Dict, List, Optional, Union

from rich.console import Console
from rich.table import Table
from rich.panel import Panel
from rich.syntax import Syntax
from rich.json import JSON
from rich.text import Text
from rich.box import ROUNDED, SIMPLE, MINIMAL, MARKDOWN

from .config import Config
from .client import ExecutionResult


class OutputFormatter:
    """Formats and displays elastic-script output."""
    
    BOX_STYLES = {
        "rounded": ROUNDED,
        "simple": SIMPLE,
        "minimal": MINIMAL,
        "markdown": MARKDOWN,
    }
    
    def __init__(self, config: Config):
        self.config = config
        self.console = Console(
            force_terminal=config.color,
            width=config.max_width,
        )
        self._box = self.BOX_STYLES.get(config.table_style, ROUNDED)
    
    def print_welcome(self, connection_info: str):
        """Print welcome banner."""
        self.console.print(Panel(
            f"[bold green]{connection_info}[/]\n"
            "[dim]Type 'help' for commands, Ctrl+D to exit[/]",
            title="[bold blue]elastic-script[/]",
            box=self._box,
        ))
    
    def print_result(self, result: ExecutionResult):
        """Print an execution result."""
        # Print any PRINT output first
        if result.output:
            for line in result.output:
                self.console.print(f"[cyan]{line}[/]")
        
        if not result.success:
            self.print_error(result.error or "Unknown error")
            return
        
        data = result.data
        
        if data is None:
            self.console.print("[green]✓[/] Query executed successfully")
            return
        
        # Handle different result types
        if isinstance(data, dict):
            self._print_dict(data)
        elif isinstance(data, list):
            self._print_list(data)
        elif isinstance(data, (str, int, float, bool)):
            self.console.print(f"[green]Result:[/] {data}")
        else:
            # Fallback to JSON
            self._print_json(data)
        
        # Print execution metadata if available
        if result.elapsed_ms is not None:
            self.console.print(
                f"[dim]Executed in {result.elapsed_ms:.2f}ms[/]",
                justify="right"
            )
    
    def _print_dict(self, data: Dict[str, Any]):
        """Print a dictionary result."""
        # Check if it looks like an action result
        if "action" in data:
            self._print_action_result(data)
            return
        
        # Check if it has tabular data
        if self._is_tabular(data):
            self._print_as_table([data])
            return
        
        # Default to formatted key-value display
        table = Table(box=self._box, show_header=False)
        table.add_column("Key", style="cyan")
        table.add_column("Value")
        
        for key, value in data.items():
            if isinstance(value, (dict, list)):
                value_str = json.dumps(value, indent=2)
            else:
                value_str = str(value)
            table.add_row(key, value_str)
        
        self.console.print(table)
    
    def _print_list(self, data: List[Any]):
        """Print a list result."""
        if not data:
            self.console.print("[dim]Empty result[/]")
            return
        
        # Check if it's a list of dicts (tabular)
        if all(isinstance(item, dict) for item in data):
            self._print_as_table(data)
            return
        
        # Simple list
        for i, item in enumerate(data):
            if isinstance(item, dict):
                self.console.print(f"[cyan]{i + 1}.[/]")
                self._print_dict(item)
            else:
                self.console.print(f"[cyan]{i + 1}.[/] {item}")
    
    def _print_as_table(self, data: List[Dict[str, Any]]):
        """Print a list of dicts as a table."""
        if not data:
            return
        
        # Get all keys
        keys = []
        for item in data:
            for key in item.keys():
                if key not in keys:
                    keys.append(key)
        
        table = Table(box=self._box)
        for key in keys:
            table.add_column(key, style="cyan")
        
        for item in data:
            row = []
            for key in keys:
                value = item.get(key, "")
                if isinstance(value, (dict, list)):
                    value = json.dumps(value)
                row.append(str(value) if value is not None else "")
            table.add_row(*row)
        
        self.console.print(table)
        self.console.print(f"[dim]{len(data)} row(s)[/]")
    
    def _print_action_result(self, data: Dict[str, Any]):
        """Print an action result (CREATE, DROP, etc.)."""
        action = data.get("action", "")
        success = data.get("success", True)
        
        if success or "success" not in data:
            icon = "[green]✓[/]"
        else:
            icon = "[red]✗[/]"
        
        # Build message
        parts = [icon, f"[bold]{action}[/]"]
        
        # Add context based on action type
        for key in ["procedure", "function", "skill", "application", "job", "trigger"]:
            if key in data:
                parts.append(f"'{data[key]}'")
                break
        
        self.console.print(" ".join(parts))
        
        # Print additional details
        detail_keys = ["parameters", "count", "version", "status"]
        details = {k: v for k, v in data.items() if k in detail_keys and v is not None}
        if details:
            for key, value in details.items():
                self.console.print(f"  [dim]{key}:[/] {value}")
    
    def _print_json(self, data: Any):
        """Print data as formatted JSON."""
        json_str = json.dumps(data, indent=self.config.json_indent, default=str)
        syntax = Syntax(json_str, "json", theme="monokai", background_color="default")
        self.console.print(syntax)
    
    def _is_tabular(self, data: Dict[str, Any]) -> bool:
        """Check if a dict looks like it should be a table row."""
        # Heuristic: all values are simple types
        for value in data.values():
            if isinstance(value, (dict, list)) and len(str(value)) > 50:
                return False
        return True
    
    def print_error(self, error: str):
        """Print an error message."""
        self.console.print(Panel(
            f"[red]{error}[/]",
            title="[red bold]Error[/]",
            box=self._box,
            border_style="red",
        ))
    
    def print_warning(self, message: str):
        """Print a warning message."""
        self.console.print(f"[yellow]⚠[/] {message}")
    
    def print_info(self, message: str):
        """Print an info message."""
        self.console.print(f"[blue]ℹ[/] {message}")
    
    def print_success(self, message: str):
        """Print a success message."""
        self.console.print(f"[green]✓[/] {message}")
    
    def print_help(self):
        """Print help information."""
        help_text = """
[bold]Commands:[/]
  [cyan]help[/]        Show this help message
  [cyan]exit[/]        Exit the CLI (or Ctrl+D)
  [cyan]clear[/]       Clear the screen
  [cyan]history[/]     Show command history
  [cyan]config[/]      Show current configuration
  
[bold]Keyboard Shortcuts:[/]
  [cyan]Tab[/]         Auto-complete
  [cyan]Ctrl+D[/]      Exit
  [cyan]Ctrl+C[/]      Cancel current input
  [cyan]Ctrl+L[/]      Clear screen
  [cyan]Up/Down[/]     Navigate history
  [cyan]Ctrl+R[/]      Search history
  
[bold]Multi-line Input:[/]
  Statements ending with keywords like BEGIN, THEN, LOOP
  automatically continue to the next line. End with a
  semicolon or blank line to execute.
        """
        self.console.print(Panel(help_text.strip(), title="[bold]Help[/]", box=self._box))
    
    def print_syntax(self, code: str):
        """Print syntax-highlighted elastic-script code."""
        # Use SQL highlighting as a close approximation
        syntax = Syntax(code, "sql", theme="monokai", background_color="default")
        self.console.print(syntax)
