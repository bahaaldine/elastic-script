"""
Output formatting for Moltler CLI using Rich.

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
    """Formats and displays Moltler output."""
    
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
    
    def print_welcome(self, connection_info: str, skill_count: int = 0):
        """Print welcome banner with Moltler branding."""
        # Moltler - The Skills Creation Framework for Elasticsearch
        moltler_banner = """
[bold magenta]         ███╗   ███╗ ██████╗ ██╗  ████████╗██╗     ███████╗██████╗
         ████╗ ████║██╔═══██╗██║  ╚══██╔══╝██║     ██╔════╝██╔══██╗
         ██╔████╔██║██║   ██║██║     ██║   ██║     █████╗  ██████╔╝
         ██║╚██╔╝██║██║   ██║██║     ██║   ██║     ██╔══╝  ██╔══██╗
         ██║ ╚═╝ ██║╚██████╔╝███████╗██║   ███████╗███████╗██║  ██║
         ╚═╝     ╚═╝ ╚═════╝ ╚══════╝╚═╝   ╚══════╝╚══════╝╚═╝  ╚═╝[/]

[bold cyan]         ⚡ The AI Skills Creation Framework for Elasticsearch ⚡[/]
"""
        self.console.print(moltler_banner)
        
        # Build status line
        if skill_count > 0:
            status = f"[bold green]{connection_info}[/] • [cyan]{skill_count} skill{'s' if skill_count != 1 else ''} available[/]"
        else:
            status = f"[bold green]{connection_info}[/] • [yellow]No skills yet[/]"
        
        # Build quick start tip based on state
        if skill_count > 0:
            quick_tip = "[dim]Try:[/] [yellow]SHOW SKILLS[/] [dim]to see available skills[/]"
        else:
            quick_tip = "[dim]Try:[/] [yellow]help examples[/] [dim]to create your first skill[/]"
        
        self.console.print(Panel(
            f"{status}\n{quick_tip}\n"
            "[dim]Type [bold]help[/bold] for commands, Ctrl+D to exit[/]",
            title="[bold blue]🦌 Ready to build![/]",
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
        """Print an action result (CREATE, DROP, SHOW, TEST, etc.)."""
        action = data.get("action", "")
        success = data.get("success", True)
        
        # Special handling for SHOW commands that return lists
        if action == "SHOW SKILLS" and "skills" in data:
            self._print_skills_table(data["skills"], data.get("count", len(data["skills"])))
            return
        elif action == "SHOW CONNECTORS" and "connectors" in data:
            self._print_connectors_table(data["connectors"], data.get("count", 0))
            return
        elif action == "SHOW AGENTS" and "agents" in data:
            self._print_agents_table(data["agents"], data.get("count", 0))
            return
        elif action == "TEST SKILL":
            self._print_test_result(data)
            return
        
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
    
    def _print_skills_table(self, skills: list, count: int):
        """Print skills as a formatted table."""
        if not skills:
            self.console.print("[yellow]No skills found.[/]")
            self.console.print("[dim]Create one with: CREATE SKILL name VERSION '1.0' ...[/]")
            return
        
        table = Table(
            title=f"[bold]Skills[/] ({count} total)",
            box=self._box,
            show_header=True,
            header_style="bold cyan"
        )
        table.add_column("Name", style="green")
        table.add_column("Description")
        table.add_column("Returns", style="yellow")
        table.add_column("Params", justify="center")
        
        for skill in skills:
            table.add_row(
                skill.get("name", ""),
                skill.get("description", "")[:50] + ("..." if len(skill.get("description", "")) > 50 else ""),
                skill.get("return_type", ""),
                str(skill.get("parameters", 0))
            )
        
        self.console.print(table)
    
    def _print_connectors_table(self, connectors: list, count: int):
        """Print connectors as a formatted table."""
        if not connectors:
            self.console.print("[yellow]No connectors found.[/]")
            return
        
        table = Table(
            title=f"[bold]Connectors[/] ({count} total)",
            box=self._box,
            show_header=True,
            header_style="bold cyan"
        )
        table.add_column("Name", style="green")
        table.add_column("Type")
        table.add_column("Status", style="yellow")
        
        for conn in connectors:
            table.add_row(
                conn.get("name", ""),
                conn.get("type", ""),
                conn.get("status", "")
            )
        
        self.console.print(table)
    
    def _print_agents_table(self, agents: list, count: int):
        """Print agents as a formatted table."""
        if not agents:
            self.console.print("[yellow]No agents found.[/]")
            return
        
        table = Table(
            title=f"[bold]Agents[/] ({count} total)",
            box=self._box,
            show_header=True,
            header_style="bold cyan"
        )
        table.add_column("Name", style="green")
        table.add_column("Description")
        table.add_column("Skills")
        table.add_column("Enabled", justify="center")
        
        for agent in agents:
            table.add_row(
                agent.get("name", ""),
                agent.get("description", "")[:40],
                str(agent.get("skills_count", 0)),
                "[green]●[/]" if agent.get("enabled", False) else "[red]○[/]"
            )
        
        self.console.print(table)
    
    def _print_test_result(self, data: Dict[str, Any]):
        """Print skill test result."""
        skill_name = data.get("skill", "unknown")
        passed = data.get("passed", False)
        status = data.get("status", "unknown")
        actual = data.get("actual_result")
        expected = data.get("expected")
        
        if passed:
            self.console.print(f"[green]✓ TEST PASSED[/] - Skill '{skill_name}'")
        else:
            self.console.print(f"[red]✗ TEST FAILED[/] - Skill '{skill_name}'")
        
        # Show details
        if actual is not None:
            self.console.print(f"  [dim]Result:[/] {actual}")
        if expected and not passed:
            self.console.print(f"  [dim]Expected:[/] {expected}")
        if "message" in data:
            self.console.print(f"  [dim]Note:[/] {data['message']}")
    
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
    
    def print_help(self, topic: str = None):
        """Print help information."""
        if topic is None:
            self._print_main_help()
        elif topic == "examples":
            self._print_examples_help()
        elif topic == "functions":
            self._print_functions_help()
        elif topic == "tutorial":
            self._print_tutorial()
        elif topic == "syntax":
            self._print_syntax_help()
        else:
            self._print_main_help()
    
    def _print_main_help(self):
        """Print main help menu."""
        help_text = """
[bold cyan]Welcome to Moltler![/] The AI Skills Creation Framework for Elasticsearch.

[bold]Quick Start:[/]
  [green]help examples[/]   Show ready-to-run examples
  [green]help tutorial[/]   Interactive getting started guide
  [green]help functions[/]  List available built-in functions
  [green]help syntax[/]     Language syntax reference

[bold]Moltler Statements:[/]
  [green]SHOW SKILLS[/]           List available skills
  [green]SHOW SKILL name[/]       Show skill details
  [green]TEST SKILL name[/]       Test a skill
  [green]SHOW CONNECTORS[/]       List connectors
  [green]SHOW AGENTS[/]           List agents
  [green]TRIGGER AGENT name[/]    Trigger an agent

[bold]CLI Commands:[/]
  [cyan]help[/]             This help menu
  [cyan]exit[/]             Exit (or Ctrl+D)
  [cyan]clear[/]            Clear screen
  [cyan]history[/]          Show command history
  [cyan]config[/]           Show configuration

[bold]Try These:[/]
  [dim]# List existing skills[/]
  [yellow]SHOW SKILLS[/]
  
  [dim]# Test a skill (after loading sample skills)[/]
  [yellow]TEST SKILL hello_moltler[/]
  
  [dim]# See more examples[/]
  [yellow]help examples[/]

[bold]Keyboard:[/] [cyan]Tab[/]=complete  [cyan]↑↓[/]=history  [cyan]Ctrl+R[/]=search  [cyan]Ctrl+L[/]=clear
        """
        self.console.print(Panel(help_text.strip(), title="[bold]Moltler Help[/]", box=self._box))
    
    def _print_examples_help(self):
        """Print example queries."""
        examples = """
[bold cyan]Ready-to-Run Moltler Examples[/]

[bold]1. Create a Skill[/]
[yellow]CREATE SKILL check_health
  VERSION '1.0'
  DESCRIPTION 'Check cluster health status'
  RETURNS DOCUMENT
BEGIN
  RETURN {'status': 'healthy', 'timestamp': CURRENT_TIMESTAMP()};
END SKILL;[/]

[bold]2. Test a Skill[/]
[yellow]-- Test with default parameters
TEST SKILL check_health

-- Test with specific parameters
TEST SKILL analyze_logs WITH index = 'logs-*', threshold = 0.9[/]

[bold]3. Create a Connector[/]
[yellow]CREATE CONNECTOR my_github
  TYPE github
  CONFIG {
    'org': 'mycompany',
    'token': ENV('GITHUB_TOKEN')
  };

-- Test connectivity
TEST CONNECTOR my_github

-- Sync data
SYNC CONNECTOR my_github TO 'github-*';[/]

[bold]4. Query Connector Data[/]
[yellow]-- Query issues from GitHub connector
QUERY my_github.issues WHERE state = 'open' LIMIT 10

-- Execute an action
EXEC my_github.create_issue(title = 'New bug', body = 'Description here')[/]

[bold]5. Create an Agent[/]
[yellow]CREATE AGENT incident_responder
  GOAL 'Respond to production incidents automatically'
  WITH SKILLS check_health, analyze_logs, notify_team
  EXECUTION SUPERVISED
  MODEL 'gpt-4'
  ON ALERT high_error_rate;

-- Trigger manually
TRIGGER AGENT incident_responder WITH {'priority': 'high'}

-- Check history
SHOW AGENT incident_responder HISTORY[/]

[bold]6. Generate a Skill with AI[/]
[yellow]-- Let AI generate a skill from description
GENERATE SKILL FROM 'Analyze logs for error patterns and return top 10'
  MODEL 'gpt-4'
  SAVE AS analyze_errors;[/]

[dim]Copy any example and paste it to try![/]
        """
        self.console.print(Panel(examples.strip(), title="[bold]Moltler Examples[/]", box=self._box))
    
    def _print_functions_help(self):
        """Print available functions."""
        functions = """
[bold cyan]Moltler Statements & Built-in Functions[/]

[bold]Skill Statements[/]
  [green]CREATE SKILL name ...[/]       Create a new skill
  [green]TEST SKILL name[/]             Test a skill
  [green]SHOW SKILLS[/]                 List all skills
  [green]DROP SKILL name[/]             Delete a skill
  [green]GENERATE SKILL FROM '...'[/]   AI-generate a skill

[bold]Connector Statements[/]
  [green]CREATE CONNECTOR name ...[/]   Create a connector
  [green]TEST CONNECTOR name[/]         Test connectivity
  [green]SYNC CONNECTOR name[/]         Sync data to Elasticsearch
  [green]QUERY connector.entity[/]      Query connector data
  [green]EXEC connector.action()[/]     Execute connector action

[bold]Agent Statements[/]
  [green]CREATE AGENT name ...[/]       Create an agent
  [green]TRIGGER AGENT name[/]          Manually trigger agent
  [green]SHOW AGENT name HISTORY[/]     View execution history
  [green]ENABLE/DISABLE AGENT[/]        Toggle agent status

[bold]Elasticsearch Functions[/]
  ESQL_QUERY, INDEX_DOCUMENT, GET_DOCUMENT, UPDATE_DOCUMENT

[bold]AI/LLM Functions[/]
  LLM_COMPLETE, LLM_CHAT, LLM_SUMMARIZE, LLM_CLASSIFY, LLM_EMBED

[bold]String Functions[/]
  LENGTH, UPPER, LOWER, TRIM, SUBSTR, REPLACE, SPLIT, CONCAT

[bold]Array Functions[/]
  ARRAY_LENGTH, ARRAY_APPEND, ARRAY_MAP, ARRAY_FILTER, ARRAY_REDUCE

[bold]Integration Functions[/]
  SLACK_SEND, HTTP_GET, HTTP_POST, WEBHOOK, PAGERDUTY_TRIGGER

[dim]Use Tab to auto-complete! Type 'help syntax' for full reference.[/]
        """
        self.console.print(Panel(functions.strip(), title="[bold]Moltler Reference[/]", box=self._box))
    
    def _print_tutorial(self):
        """Print interactive tutorial."""
        tutorial = """
[bold cyan]Moltler Getting Started Tutorial[/]

[bold]Step 1: Your First Skill[/]
Skills are reusable units of automation. Create one:

[yellow]CREATE SKILL hello_world
  VERSION '1.0'
  DESCRIPTION 'A simple greeting skill'
  RETURNS STRING
BEGIN
  RETURN 'Hello from Moltler!';
END SKILL;[/]

Test it:
[yellow]TEST SKILL hello_world[/]

[bold]Step 2: Skill with Parameters[/]
[yellow]CREATE SKILL count_logs
  VERSION '1.0'
  DESCRIPTION 'Count documents in an index'
  (index STRING DEFAULT 'logs-*')
  RETURNS NUMBER
BEGIN
  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM ' || index || ' | STATS c = COUNT(*)');
  RETURN result[0].c;
END SKILL;[/]

Test with parameters:
[yellow]TEST SKILL count_logs WITH index = 'metrics-*'[/]

[bold]Step 3: Create a Connector[/]
Connectors bring external data into Elasticsearch:

[yellow]CREATE CONNECTOR my_github
  TYPE github
  CONFIG {'org': 'mycompany', 'token': ENV('GITHUB_TOKEN')};

TEST CONNECTOR my_github[/]

[bold]Step 4: Query Connector Data[/]
[yellow]-- List open issues
QUERY my_github.issues WHERE state = 'open' LIMIT 5

-- Sync to Elasticsearch
SYNC CONNECTOR my_github TO 'github-data-*';[/]

[bold]Step 5: Create an Agent[/]
Agents orchestrate skills to achieve goals:

[yellow]CREATE AGENT health_monitor
  GOAL 'Monitor system health and alert on issues'
  WITH SKILLS count_logs, check_errors
  EXECUTION AUTONOMOUS
  ON SCHEDULE EVERY '5m';[/]

Trigger manually:
[yellow]TRIGGER AGENT health_monitor[/]

[bold]What's Next?[/]
  • [cyan]help examples[/]   - More Moltler examples
  • [cyan]help functions[/]  - All available functions
  • [cyan]help syntax[/]     - Language reference
        """
        self.console.print(Panel(tutorial.strip(), title="[bold]Moltler Tutorial[/]", box=self._box))
    
    def _print_syntax_help(self):
        """Print syntax reference."""
        syntax = """
[bold cyan]Moltler Syntax Reference[/]

[bold]Skills[/]
[yellow]CREATE SKILL name
  VERSION '1.0'
  DESCRIPTION 'What the skill does'
  AUTHOR 'Your Name'
  TAGS ['tag1', 'tag2']
  (param TYPE DEFAULT value)
  RETURNS TYPE
BEGIN ... END SKILL;[/]

[bold]Connectors[/]
[yellow]CREATE CONNECTOR name TYPE github CONFIG {...};
SYNC CONNECTOR name TO 'index-*' [INCREMENTAL ON field];
QUERY connector.entity WHERE expr LIMIT n;
EXEC connector.action(arg = value);[/]

[bold]Agents[/]
[yellow]CREATE AGENT name
  GOAL 'Agent objective'
  WITH SKILLS skill1, skill2
  EXECUTION AUTONOMOUS|SUPERVISED|HUMAN_APPROVAL|DRY_RUN
  MODEL 'gpt-4'
  ON SCHEDULE EVERY '5m' | ON ALERT name | ON WEBHOOK path;

TRIGGER AGENT name WITH {...};[/]

[bold]Variables & Types[/]
[yellow]DECLARE var_name TYPE;
SET var_name = expression;[/]
  Types: STRING, NUMBER, BOOLEAN, DATE, ARRAY, DOCUMENT, MAP

[bold]Control Flow[/]
[yellow]IF cond THEN ... ELSEIF cond THEN ... ELSE ... END IF;
FOR item IN array LOOP ... END LOOP;
WHILE cond LOOP ... END LOOP;[/]

[bold]Error Handling[/]
[yellow]TRY ... CATCH PRINT @error.message; FINALLY ... END TRY;[/]

[bold]Operators[/]
  [cyan]||[/] Concatenation  [cyan]??[/] Null coalescing  [cyan]?.[/] Safe nav  [cyan]..[/] Range
        """
        self.console.print(Panel(syntax.strip(), title="[bold]Moltler Syntax[/]", box=self._box))
    
    def print_syntax(self, code: str):
        """Print syntax-highlighted elastic-script code."""
        # Use SQL highlighting as a close approximation
        syntax = Syntax(code, "sql", theme="monokai", background_color="default")
        self.console.print(syntax)
