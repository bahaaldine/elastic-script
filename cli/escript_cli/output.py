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
        """Print welcome banner with Moltler mascot."""
        # Moltler - The Skills Creation Framework for Elasticsearch
        moltler_banner = """
[bold yellow]
               ╱╲             ╱╲
              ╱  ╲   ╱╲ ╱╲   ╱  ╲
             ╱    ╲ ╱  ╳  ╲ ╱    ╲
                   ╲╱╱  ╲╲╱
                    ╱    ╲
                 __╱      ╲__
                ╱  ◕    ◕   ╲
               │      ▽      │
               │   ╲_____╱   │
                ╲    ‿‿‿    ╱
                 ╲_________╱
[/]
[bold magenta]         ███╗   ███╗ ██████╗ ██╗  ████████╗██╗     ███████╗██████╗
         ████╗ ████║██╔═══██╗██║  ╚══██╔══╝██║     ██╔════╝██╔══██╗
         ██╔████╔██║██║   ██║██║     ██║   ██║     █████╗  ██████╔╝
         ██║╚██╔╝██║██║   ██║██║     ██║   ██║     ██╔══╝  ██╔══██╗
         ██║ ╚═╝ ██║╚██████╔╝███████╗██║   ███████╗███████╗██║  ██║
         ╚═╝     ╚═╝ ╚═════╝ ╚══════╝╚═╝   ╚══════╝╚══════╝╚═╝  ╚═╝[/]

[bold cyan]         ⚡ The Skills Creation Framework for Elasticsearch ⚡[/]
"""
        self.console.print(moltler_banner)
        self.console.print(Panel(
            f"[bold green]{connection_info}[/]\n"
            "[dim]Type [bold]help[/bold] for commands, [bold]help examples[/bold] for quick start, Ctrl+D to exit[/]",
            title="[bold blue]🦌 Moltler says: Let's build some skills![/]",
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
[bold cyan]Welcome to elastic-script![/] A procedural scripting language for Elasticsearch.

[bold]Quick Start:[/]
  [green]help examples[/]   Show ready-to-run examples
  [green]help tutorial[/]   Interactive getting started guide
  [green]help functions[/]  List available built-in functions
  [green]help syntax[/]     Language syntax reference

[bold]CLI Commands:[/]
  [cyan]help[/]             This help menu
  [cyan]exit[/]             Exit (or Ctrl+D)
  [cyan]clear[/]            Clear screen
  [cyan]history[/]          Show command history
  [cyan]config[/]           Show configuration

[bold]Try These:[/]
  [dim]# Create a simple procedure[/]
  [yellow]CREATE PROCEDURE hello() BEGIN PRINT 'Hello World!'; END PROCEDURE;[/]
  
  [dim]# Call it[/]
  [yellow]CALL hello()[/]
  
  [dim]# Query Elasticsearch with ES|QL[/]
  [yellow]CALL ESQL_QUERY('FROM logs-* | LIMIT 5')[/]

[bold]Keyboard:[/] [cyan]Tab[/]=complete  [cyan]↑↓[/]=history  [cyan]Ctrl+R[/]=search  [cyan]Ctrl+L[/]=clear
        """
        self.console.print(Panel(help_text.strip(), title="[bold]elastic-script Help[/]", box=self._box))
    
    def _print_examples_help(self):
        """Print example queries."""
        examples = """
[bold cyan]Ready-to-Run Examples[/]

[bold]1. Hello World[/]
[yellow]CREATE PROCEDURE greet(name STRING)
BEGIN
  PRINT 'Hello, ' || name || '!';
END PROCEDURE;

CALL greet('World')[/]

[bold]2. Query Elasticsearch[/]
[yellow]-- Get recent logs
CALL ESQL_QUERY('FROM logs-* | SORT @timestamp DESC | LIMIT 10')

-- Count by status
CALL ESQL_QUERY('FROM logs-* | STATS count = COUNT(*) BY status')[/]

[bold]3. Variables and Control Flow[/]
[yellow]CREATE PROCEDURE analyze_logs()
BEGIN
  DECLARE error_count NUMBER;
  SET error_count = ESQL_QUERY('FROM logs-* | WHERE level = "ERROR" | STATS c = COUNT(*)')[0].c;
  
  IF error_count > 100 THEN
    PRINT 'Alert: ' || error_count || ' errors found!';
  ELSE
    PRINT 'System healthy: ' || error_count || ' errors';
  END IF;
END PROCEDURE;[/]

[bold]4. Loop Through Results[/]
[yellow]CREATE PROCEDURE list_indices()
BEGIN
  DECLARE indices ARRAY;
  SET indices = ESQL_QUERY('SHOW INDICES');
  
  FOR idx IN indices LOOP
    PRINT 'Index: ' || idx.index;
  END LOOP;
END PROCEDURE;[/]

[bold]5. Error Handling[/]
[yellow]CREATE PROCEDURE safe_query()
BEGIN
  TRY
    CALL ESQL_QUERY('FROM nonexistent-index');
  CATCH
    PRINT 'Error: ' || @error.message;
  END TRY;
END PROCEDURE;[/]

[dim]Copy any example and paste it to try![/]
        """
        self.console.print(Panel(examples.strip(), title="[bold]Examples[/]", box=self._box))
    
    def _print_functions_help(self):
        """Print available functions."""
        functions = """
[bold cyan]Built-in Functions (118 total)[/]

[bold]String Functions[/]
  LENGTH, UPPER, LOWER, TRIM, SUBSTR, REPLACE, SPLIT, CONCAT
  REGEXP_REPLACE, REGEXP_SUBSTR, REVERSE, INITCAP, LPAD, RPAD

[bold]Array Functions[/]
  ARRAY_LENGTH, ARRAY_APPEND, ARRAY_CONTAINS, ARRAY_JOIN
  ARRAY_MAP, ARRAY_FILTER, ARRAY_REDUCE, ARRAY_SORT, ARRAY_SLICE

[bold]Number Functions[/]
  ABS, CEIL, FLOOR, ROUND, MOD, POWER, SQRT, LOG, EXP

[bold]Date Functions[/]
  CURRENT_DATE, CURRENT_TIMESTAMP, DATE_ADD, DATE_SUB, DATE_DIFF

[bold]Document/Map Functions[/]
  DOCUMENT_GET, DOCUMENT_KEYS, DOCUMENT_MERGE
  MAP_GET, MAP_PUT, MAP_KEYS, MAP_VALUES

[bold]Elasticsearch Functions[/]
  [green]ESQL_QUERY(query)[/]           Execute ES|QL query
  [green]INDEX_DOCUMENT(idx, doc)[/]    Index a document
  [green]GET_DOCUMENT(idx, id)[/]       Get document by ID
  [green]UPDATE_DOCUMENT(idx, id, d)[/] Update a document

[bold]AI/LLM Functions[/]
  LLM_COMPLETE, LLM_CHAT, LLM_SUMMARIZE, LLM_CLASSIFY

[bold]Integration Functions[/]
  SLACK_SEND, HTTP_GET, HTTP_POST, WEBHOOK
  AWS_LAMBDA_INVOKE, K8S_GET, PAGERDUTY_TRIGGER

[dim]Use Tab to auto-complete function names![/]
        """
        self.console.print(Panel(functions.strip(), title="[bold]Functions Reference[/]", box=self._box))
    
    def _print_tutorial(self):
        """Print interactive tutorial."""
        tutorial = """
[bold cyan]Getting Started Tutorial[/]

[bold]Step 1: Your First Procedure[/]
Copy and paste this to create a procedure:

[yellow]CREATE PROCEDURE my_first_proc()
BEGIN
  PRINT 'It works!';
END PROCEDURE;[/]

Then call it:
[yellow]CALL my_first_proc()[/]

[bold]Step 2: Using Variables[/]
[yellow]CREATE PROCEDURE greet(name STRING)
BEGIN
  DECLARE message STRING;
  SET message = 'Hello, ' || name || '!';
  PRINT message;
END PROCEDURE;

CALL greet('Developer')[/]

[bold]Step 3: Query Elasticsearch[/]
[yellow]-- Replace 'your-index' with an actual index name
CALL ESQL_QUERY('FROM your-index | LIMIT 5')[/]

[bold]Step 4: Store Query Results[/]
[yellow]CREATE PROCEDURE count_docs(index_name STRING)
BEGIN
  DECLARE result ARRAY;
  DECLARE count NUMBER;
  
  SET result = ESQL_QUERY('FROM ' || index_name || ' | STATS c = COUNT(*)');
  SET count = result[0].c;
  
  PRINT 'Documents in ' || index_name || ': ' || count;
END PROCEDURE;

CALL count_docs('logs-*')[/]

[bold]Step 5: Control Flow[/]
[yellow]CREATE PROCEDURE check_health()
BEGIN
  DECLARE errors NUMBER;
  SET errors = ESQL_QUERY('FROM logs-* | WHERE level = "ERROR" | STATS c = COUNT(*)')[0].c;
  
  IF errors > 50 THEN
    PRINT '⚠️  High error count: ' || errors;
  ELSEIF errors > 10 THEN
    PRINT '⚡ Moderate errors: ' || errors;
  ELSE
    PRINT '✅ System healthy!';
  END IF;
END PROCEDURE;[/]

[bold]What's Next?[/]
  • [cyan]help examples[/]   - More examples
  • [cyan]help functions[/]  - All available functions
  • [cyan]help syntax[/]     - Language reference
        """
        self.console.print(Panel(tutorial.strip(), title="[bold]Tutorial[/]", box=self._box))
    
    def _print_syntax_help(self):
        """Print syntax reference."""
        syntax = """
[bold cyan]Language Syntax Reference[/]

[bold]Procedures[/]
[yellow]CREATE PROCEDURE name(param1 TYPE, param2 TYPE)
BEGIN
  -- statements
END PROCEDURE;

DROP PROCEDURE name;
CALL name(arg1, arg2);[/]

[bold]Variables[/]
[yellow]DECLARE var_name TYPE;           -- Declare
DECLARE var_name TYPE = value;   -- Declare with value
SET var_name = expression;       -- Assign[/]

[bold]Types[/]
  STRING, NUMBER, BOOLEAN, DATE, ARRAY, DOCUMENT, MAP

[bold]Control Flow[/]
[yellow]IF condition THEN
  -- statements
ELSEIF condition THEN
  -- statements
ELSE
  -- statements
END IF;

FOR item IN array LOOP
  -- statements  
END LOOP;

WHILE condition LOOP
  -- statements
END LOOP;[/]

[bold]Error Handling[/]
[yellow]TRY
  -- statements that might fail
CATCH
  PRINT @error.message;
FINALLY
  -- always runs
END TRY;[/]

[bold]Operators[/]
  [cyan]||[/]   String concatenation
  [cyan]??[/]   Null coalescing (a ?? b = a if a not null, else b)
  [cyan]?.[/]   Safe navigation (a?.b = null if a is null)
  [cyan]..[/]   Range (1..10)

[bold]Comments[/]
[yellow]-- Single line comment
/* Multi-line
   comment */[/]
        """
        self.console.print(Panel(syntax.strip(), title="[bold]Syntax Reference[/]", box=self._box))
    
    def print_syntax(self, code: str):
        """Print syntax-highlighted elastic-script code."""
        # Use SQL highlighting as a close approximation
        syntax = Syntax(code, "sql", theme="monokai", background_color="default")
        self.console.print(syntax)
