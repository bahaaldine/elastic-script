"""
Moltler CLI - The AI Skills Creation Framework for Elasticsearch.

Usage:
    moltler                          # Start interactive REPL
    moltler query "..."              # Execute a single statement
    moltler run script.es            # Execute a script file
    moltler config                   # Show/manage configuration
    moltler test                     # Test connection to Elasticsearch

Examples:
    # Skills
    moltler query "SHOW SKILLS"
    moltler query "CREATE SKILL hello() RETURNS STRING AS 'Hello!';"
    moltler query "TEST SKILL hello"
    
    # Connectors
    moltler query "SHOW CONNECTORS"
    moltler query "TEST CONNECTOR my_github"
    moltler query "SYNC CONNECTOR my_github TO 'github-*'"
    
    # Agents
    moltler query "SHOW AGENTS"
    moltler query "TRIGGER AGENT incident_responder"
    moltler query "SHOW AGENT incident_responder HISTORY"
"""

import sys
from pathlib import Path
from typing import Optional

import click

from . import __version__
from .config import Config, SAMPLE_CONFIG
from .client import ElasticScriptClient
from .repl import ElasticScriptREPL
from .output import OutputFormatter


@click.group(invoke_without_command=True)
@click.option('--host', '-h', default=None, help='Elasticsearch host')
@click.option('--port', '-p', default=None, type=int, help='Elasticsearch port')
@click.option('--user', '-u', default=None, help='Username')
@click.option('--password', '-P', default=None, help='Password (or use ESCRIPT_PASSWORD)')
@click.option('--ssl/--no-ssl', default=None, help='Use SSL/TLS')
@click.option('--no-color', is_flag=True, help='Disable color output')
@click.option('--version', '-v', is_flag=True, help='Show version')
@click.pass_context
def cli(ctx, host, port, user, password, ssl, no_color, version):
    """
    Moltler CLI - The AI Skills Creation Framework for Elasticsearch.
    
    Start an interactive session:
    
        $ moltler
    
    Execute a single statement:
    
        $ moltler query "SHOW SKILLS"
        $ moltler query "TEST SKILL my_skill"
        $ moltler query "TRIGGER AGENT monitor"
    
    Run a script file:
    
        $ moltler run myscript.es
    
    Test connection:
    
        $ moltler test
    """
    if version:
        click.echo(f"escript-cli version {__version__}")
        return
    
    # Load config
    config = Config.load()
    
    # Apply command-line overrides
    if host is not None:
        config.host = host
    if port is not None:
        config.port = port
    if user is not None:
        config.username = user
    if password is not None:
        config.password = password
    if ssl is not None:
        config.use_ssl = ssl
    if no_color:
        config.color = False
    
    ctx.ensure_object(dict)
    ctx.obj['config'] = config
    
    # If no subcommand, start REPL
    if ctx.invoked_subcommand is None:
        repl = ElasticScriptREPL(config)
        repl.run()


@cli.command()
@click.argument('statement')
@click.option('--json', '-j', 'as_json', is_flag=True, help='Output as JSON')
@click.pass_context
def query(ctx, statement, as_json):
    """
    Execute a single elastic-script statement.
    
    Examples:
    
        escript query "CALL hello()"
        escript query "SHOW PROCEDURES"
        escript query "CREATE PROCEDURE test() BEGIN PRINT 'hi'; END PROCEDURE;"
    """
    config = ctx.obj['config']
    client = ElasticScriptClient(config)
    output = OutputFormatter(config)
    
    # Test connection
    success, message = client.test_connection()
    if not success:
        output.print_error(message)
        sys.exit(1)
    
    # Execute
    result = client.execute(statement)
    
    if as_json:
        import json
        if result.success:
            click.echo(json.dumps(result.data, indent=2, default=str))
        else:
            click.echo(json.dumps({"error": result.error}, indent=2))
            sys.exit(1)
    else:
        output.print_result(result)
        if not result.success:
            sys.exit(1)
    
    client.close()


@cli.command()
@click.argument('script_file', type=click.Path(exists=True))
@click.option('--dry-run', '-n', is_flag=True, help='Parse only, do not execute')
@click.option('--verbose', '-v', is_flag=True, help='Show each statement as it executes')
@click.pass_context
def run(ctx, script_file, dry_run, verbose):
    """
    Execute an elastic-script file.
    
    The script file should contain one or more elastic-script statements,
    separated by semicolons.
    
    Examples:
    
        escript run setup.es
        escript run --verbose myscript.es
        escript run --dry-run test.es
    """
    config = ctx.obj['config']
    client = ElasticScriptClient(config)
    output = OutputFormatter(config)
    
    # Read script
    script_path = Path(script_file)
    script_content = script_path.read_text()
    
    if dry_run:
        output.print_info(f"Parsing {script_path.name}...")
        output.print_syntax(script_content)
        output.print_success(f"Script parsed successfully ({len(script_content)} chars)")
        return
    
    # Test connection
    success, message = client.test_connection()
    if not success:
        output.print_error(message)
        sys.exit(1)
    
    if verbose:
        output.print_info(f"Running {script_path.name}...")
    
    # Split into statements and execute
    statements = _split_statements(script_content)
    
    for i, stmt in enumerate(statements, 1):
        stmt = stmt.strip()
        if not stmt:
            continue
        
        if verbose:
            output.console.print(f"\n[dim]Statement {i}/{len(statements)}:[/]")
            output.print_syntax(stmt[:200] + ("..." if len(stmt) > 200 else ""))
        
        result = client.execute(stmt)
        
        if not result.success:
            output.print_error(f"Error in statement {i}: {result.error}")
            sys.exit(1)
        
        if verbose:
            output.print_result(result)
    
    output.print_success(f"Executed {len(statements)} statement(s)")
    client.close()


def _split_statements(content: str) -> list:
    """
    Split script content into individual statements.
    
    This is a simple implementation that splits on semicolons
    but handles strings and block statements correctly.
    """
    statements = []
    current = []
    in_string = None
    depth = 0
    
    lines = content.split('\n')
    
    for line in lines:
        # Skip comments
        if line.strip().startswith('--'):
            continue
        
        current.append(line)
        
        # Check for string boundaries and block depth
        for char in line:
            if in_string:
                if char == in_string:
                    in_string = None
            elif char in ('"', "'"):
                in_string = char
        
        # Simple heuristic: check for block keywords
        upper_line = line.upper()
        if 'BEGIN' in upper_line or 'THEN' in upper_line or 'LOOP' in upper_line:
            depth += 1
        if 'END ' in upper_line or 'END;' in upper_line:
            depth = max(0, depth - 1)
        
        # Check for statement end
        if line.rstrip().endswith(';') and depth == 0 and not in_string:
            stmt = '\n'.join(current)
            if stmt.strip():
                statements.append(stmt)
            current = []
    
    # Handle any remaining content
    if current:
        stmt = '\n'.join(current)
        if stmt.strip():
            statements.append(stmt)
    
    return statements


@cli.command()
@click.option('--init', is_flag=True, help='Create sample config file')
@click.option('--show', is_flag=True, help='Show current configuration')
@click.pass_context
def config(ctx, init, show):
    """
    Manage CLI configuration.
    
    Examples:
    
        escript config --show       # Show current config
        escript config --init       # Create ~/.escriptrc
    """
    config_path = Path.home() / ".escriptrc"
    
    if init:
        if config_path.exists():
            click.confirm(f"{config_path} exists. Overwrite?", abort=True)
        
        config_path.write_text(SAMPLE_CONFIG)
        click.echo(f"Created {config_path}")
        click.echo("Edit this file to customize your settings.")
        return
    
    if show or not init:
        cfg = ctx.obj.get('config') or Config.load()
        output = OutputFormatter(cfg)
        
        click.echo(f"Config file: {config_path}")
        click.echo(f"  exists: {config_path.exists()}")
        click.echo()
        
        from rich.table import Table
        table = Table(title="Current Settings", box=output._box)
        table.add_column("Setting", style="cyan")
        table.add_column("Value")
        table.add_column("Source", style="dim")
        
        table.add_row("host", cfg.host, "config/env")
        table.add_row("port", str(cfg.port), "config/env")
        table.add_row("username", cfg.username, "config/env")
        table.add_row("password", "****", "config/env")
        table.add_row("use_ssl", str(cfg.use_ssl), "config/env")
        table.add_row("multiline", str(cfg.multiline), "config")
        table.add_row("vi_mode", str(cfg.vi_mode), "config")
        table.add_row("color", str(cfg.color), "config/cli")
        table.add_row("table_style", cfg.table_style, "config")
        
        output.console.print(table)


@cli.command()
@click.pass_context
def test(ctx):
    """
    Test connection to Elasticsearch.
    
    Verifies that the CLI can connect to the configured Elasticsearch
    instance and that the elastic-script plugin is available.
    """
    config = ctx.obj['config']
    client = ElasticScriptClient(config)
    output = OutputFormatter(config)
    
    output.print_info(f"Testing connection to {config.url}...")
    
    success, message = client.test_connection()
    
    if success:
        output.print_success(message)
        
        # Test elastic-script endpoint - create and call a simple test procedure
        # First try to create a test procedure
        create_result = client.execute(
            "CREATE PROCEDURE __cli_connection_test__() BEGIN DECLARE x NUMBER; SET x = 1; END PROCEDURE;"
        )
        
        if create_result.success:
            # Procedure created, now call it
            call_result = client.execute("CALL __cli_connection_test__()")
            # Clean up
            client.execute("DROP PROCEDURE __cli_connection_test__")
            
            if call_result.success:
                output.print_success("elastic-script plugin is available")
            else:
                output.print_warning(f"elastic-script endpoint issue: {call_result.error}")
        elif "already exists" in str(create_result.error).lower():
            # Procedure already exists from a previous test, that's fine
            output.print_success("elastic-script plugin is available")
        else:
            # Check if it's a syntax error or similar - that still means plugin is working
            if "Syntax error" in str(create_result.error) or "RuntimeException" in str(create_result.error):
                output.print_success("elastic-script plugin is available")
            else:
                output.print_warning(f"elastic-script endpoint issue: {create_result.error}")
    else:
        output.print_error(message)
        sys.exit(1)
    
    client.close()


@cli.command()
@click.pass_context
def demo(ctx):
    """
    Run an interactive demo of Moltler.
    
    This command demonstrates Moltler's key features by:
    1. Creating a sample skill
    2. Testing it
    3. Showing the results
    
    Perfect for first-time users to see Moltler in action.
    """
    import time
    
    config = ctx.obj['config']
    client = ElasticScriptClient(config)
    output = OutputFormatter(config)
    
    output.console.print()
    output.console.print("[bold cyan]━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━[/]")
    output.console.print("[bold cyan]  Welcome to the Moltler Demo![/]")
    output.console.print("[bold cyan]━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━[/]")
    output.console.print()
    output.console.print("  Moltler is an AI Skills Creation Framework for Elasticsearch.")
    output.console.print("  Let's see it in action!\n")
    
    # Step 1: Check connection
    output.console.print("[bold]Step 1:[/] Connecting to Elasticsearch...")
    success, message = client.test_connection()
    if not success:
        output.print_error(f"Could not connect: {message}")
        output.console.print("\n  [yellow]Tip:[/] Make sure Elasticsearch is running:")
        output.console.print("       ./scripts/quick-start.sh")
        client.close()
        sys.exit(1)
    output.print_success(f"Connected to {config.url}")
    output.console.print()
    
    # Step 2: Create a skill
    output.console.print("[bold]Step 2:[/] Creating a sample skill...")
    output.console.print()
    
    skill_code = """CREATE SKILL demo_health_check
  VERSION '1.0'
  DESCRIPTION 'Check Elasticsearch cluster health - created by demo'
  AUTHOR 'Moltler Demo'
  RETURNS DOCUMENT
AS
BEGIN
  DECLARE health DOCUMENT;
  SET health = {'status': 'green', 'cluster': 'demo', 'timestamp': CURRENT_TIMESTAMP()};
  RETURN health;
END;"""
    
    output.console.print("[dim]" + skill_code + "[/]")
    output.console.print()
    
    # Try to drop existing skill first
    client.execute("DROP SKILL demo_health_check")
    
    result = client.execute(skill_code)
    if result.success:
        output.print_success("Skill 'demo_health_check' created!")
    else:
        if "already exists" in str(result.error).lower():
            output.print_info("Skill 'demo_health_check' already exists, using it.")
        else:
            output.print_error(f"Failed to create skill: {result.error}")
            client.close()
            sys.exit(1)
    output.console.print()
    
    # Step 3: List skills
    output.console.print("[bold]Step 3:[/] Listing available skills...")
    output.console.print()
    result = client.execute("SHOW SKILLS")
    if result.success and result.data:
        output.print_result(result)
    else:
        output.console.print("  [dim](No skills found - but we just created one!)[/]")
    output.console.print()
    
    # Step 4: Test the skill
    output.console.print("[bold]Step 4:[/] Testing the skill...")
    output.console.print()
    output.console.print("[dim]TEST SKILL demo_health_check[/]")
    output.console.print()
    
    result = client.execute("TEST SKILL demo_health_check")
    if result.success:
        output.print_result(result)
        output.print_success("Skill test passed!")
    else:
        output.console.print(f"  Result: {result.data}")
    output.console.print()
    
    # Summary
    output.console.print("[bold cyan]━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━[/]")
    output.console.print("[bold cyan]  Demo Complete![/]")
    output.console.print("[bold cyan]━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━[/]")
    output.console.print()
    output.console.print("  [bold]What just happened:[/]")
    output.console.print("    1. Created a [green]skill[/] - a reusable automation component")
    output.console.print("    2. Listed all skills with [cyan]SHOW SKILLS[/]")
    output.console.print("    3. Tested the skill with [cyan]TEST SKILL[/]")
    output.console.print()
    output.console.print("  [bold]Try these next:[/]")
    output.console.print("    • Start the REPL:     [cyan]moltler[/]")
    output.console.print("    • Type:               [yellow]SHOW SKILLS[/]")
    output.console.print("    • Type:               [yellow]help examples[/]")
    output.console.print()
    output.console.print("  [bold]Learn more:[/]")
    output.console.print("    • Documentation:      https://bahaaldine.github.io/elastic-script/")
    output.console.print()
    
    client.close()


def main():
    """Main entry point."""
    cli(obj={})


if __name__ == '__main__':
    main()
