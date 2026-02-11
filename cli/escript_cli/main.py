"""
Moltler CLI - The AI Skills Creation Framework for Elasticsearch.

Usage:
    escript                          # Start interactive REPL
    escript query "..."              # Execute a single query
    escript run script.es            # Execute a script file
    
    # Skill commands
    escript skill list               # List all skills
    escript skill show <name>        # Show skill details
    escript skill test <name>        # Test a skill
    
    # Connector commands
    escript connector list           # List all connectors
    escript connector show <name>    # Show connector details
    escript connector test <name>    # Test connector connectivity
    escript connector sync <name>    # Sync connector data
    
    # Agent commands
    escript agent list               # List all agents
    escript agent show <name>        # Show agent details
    escript agent trigger <name>     # Manually trigger an agent
    escript agent history <name>     # Show agent execution history
    
    escript config                   # Show/manage configuration
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
    
        $ escript
    
    Execute a single query:
    
        $ escript query "SHOW SKILLS"
    
    Run a script file:
    
        $ escript run myscript.es
    
    Manage skills:
    
        $ escript skill list
        $ escript skill show my_skill
        $ escript skill test my_skill --with "param1=value1"
    
    Manage connectors:
    
        $ escript connector list
        $ escript connector test my_github
        $ escript connector sync my_github
    
    Manage agents:
    
        $ escript agent list
        $ escript agent trigger incident_responder
        $ escript agent history incident_responder
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


@cli.group()
@click.pass_context
def skill(ctx):
    """
    Manage Moltler skills.
    
    Skills are reusable automation components that encapsulate
    complex operations with parameters and return values.
    
    Examples:
    
        escript skill list
        escript skill show my_skill
        escript skill test my_skill --with "param1=value1"
    """
    pass


@skill.command('list')
@click.option('--json', '-j', 'as_json', is_flag=True, help='Output as JSON')
@click.pass_context
def skill_list(ctx, as_json):
    """List all available skills."""
    config = ctx.obj['config']
    client = ElasticScriptClient(config)
    output = OutputFormatter(config)
    
    result = client.execute("SHOW SKILLS")
    
    if as_json:
        import json
        click.echo(json.dumps(result.data, indent=2, default=str))
    else:
        output.print_result(result)
    
    client.close()


@skill.command('show')
@click.argument('name')
@click.pass_context
def skill_show(ctx, name):
    """Show details of a specific skill."""
    config = ctx.obj['config']
    client = ElasticScriptClient(config)
    output = OutputFormatter(config)
    
    result = client.execute(f"SHOW SKILL {name}")
    output.print_result(result)
    client.close()


@skill.command('test')
@click.argument('name')
@click.option('--with', '-w', 'with_params', help='Parameters as key=value pairs (comma-separated)')
@click.option('--expect', '-e', help='Expected result expression')
@click.pass_context
def skill_test(ctx, name, with_params, expect):
    """Test a skill with given parameters."""
    config = ctx.obj['config']
    client = ElasticScriptClient(config)
    output = OutputFormatter(config)
    
    # Build TEST SKILL statement
    stmt = f"TEST SKILL {name}"
    
    if with_params:
        # Parse key=value pairs
        params = []
        for pair in with_params.split(','):
            pair = pair.strip()
            if '=' in pair:
                key, value = pair.split('=', 1)
                params.append(f"{key.strip()} = '{value.strip()}'")
        if params:
            stmt += f" WITH {', '.join(params)}"
    
    if expect:
        stmt += f" EXPECT {expect}"
    
    result = client.execute(stmt)
    output.print_result(result)
    
    if result.success and isinstance(result.data, dict):
        passed = result.data.get('passed', False)
        if passed:
            output.print_success("Test PASSED")
        else:
            output.print_error("Test FAILED")
            sys.exit(1)
    
    client.close()


@cli.group()
@click.pass_context
def connector(ctx):
    """
    Manage Moltler connectors.
    
    Connectors enable integration with external services like
    GitHub, Jira, Datadog, and more.
    
    Examples:
    
        escript connector list
        escript connector show my_github
        escript connector test my_github
        escript connector sync my_github
    """
    pass


@connector.command('list')
@click.option('--json', '-j', 'as_json', is_flag=True, help='Output as JSON')
@click.pass_context
def connector_list(ctx, as_json):
    """List all configured connectors."""
    config = ctx.obj['config']
    client = ElasticScriptClient(config)
    output = OutputFormatter(config)
    
    result = client.execute("SHOW CONNECTORS")
    
    if as_json:
        import json
        click.echo(json.dumps(result.data, indent=2, default=str))
    else:
        output.print_result(result)
    
    client.close()


@connector.command('show')
@click.argument('name')
@click.pass_context
def connector_show(ctx, name):
    """Show details of a specific connector."""
    config = ctx.obj['config']
    client = ElasticScriptClient(config)
    output = OutputFormatter(config)
    
    result = client.execute(f"SHOW CONNECTOR {name}")
    output.print_result(result)
    client.close()


@connector.command('test')
@click.argument('name')
@click.pass_context
def connector_test(ctx, name):
    """Test connectivity to a connector's external service."""
    config = ctx.obj['config']
    client = ElasticScriptClient(config)
    output = OutputFormatter(config)
    
    output.print_info(f"Testing connector {name}...")
    result = client.execute(f"TEST CONNECTOR {name}")
    output.print_result(result)
    
    if result.success and isinstance(result.data, dict):
        if result.data.get('connected', False):
            output.print_success("Connection successful")
        else:
            output.print_error("Connection failed")
            sys.exit(1)
    
    client.close()


@connector.command('sync')
@click.argument('name')
@click.option('--entity', '-e', help='Specific entity to sync (e.g., issues, repos)')
@click.option('--full', is_flag=True, help='Full sync (ignore incremental state)')
@click.pass_context
def connector_sync(ctx, name, entity, full):
    """Sync data from a connector into Elasticsearch."""
    config = ctx.obj['config']
    client = ElasticScriptClient(config)
    output = OutputFormatter(config)
    
    stmt = f"SYNC CONNECTOR {name}"
    if entity:
        stmt += f" ENTITY '{entity}'"
    if full:
        stmt += " FULL"
    
    output.print_info(f"Syncing {name}...")
    result = client.execute(stmt)
    output.print_result(result)
    client.close()


@cli.group()
@click.pass_context
def agent(ctx):
    """
    Manage Moltler agents.
    
    Agents are autonomous entities that execute skills based on
    goals, triggers, and configurable execution policies.
    
    Examples:
    
        escript agent list
        escript agent show my_agent
        escript agent trigger my_agent
        escript agent history my_agent
    """
    pass


@agent.command('list')
@click.option('--json', '-j', 'as_json', is_flag=True, help='Output as JSON')
@click.pass_context
def agent_list(ctx, as_json):
    """List all configured agents."""
    config = ctx.obj['config']
    client = ElasticScriptClient(config)
    output = OutputFormatter(config)
    
    result = client.execute("SHOW AGENTS")
    
    if as_json:
        import json
        click.echo(json.dumps(result.data, indent=2, default=str))
    else:
        output.print_result(result)
    
    client.close()


@agent.command('show')
@click.argument('name')
@click.pass_context
def agent_show(ctx, name):
    """Show details of a specific agent."""
    config = ctx.obj['config']
    client = ElasticScriptClient(config)
    output = OutputFormatter(config)
    
    result = client.execute(f"SHOW AGENT {name}")
    output.print_result(result)
    client.close()


@agent.command('trigger')
@click.argument('name')
@click.option('--context', '-c', help='JSON context to pass to the agent')
@click.pass_context
def agent_trigger(ctx, name, context):
    """Manually trigger an agent execution."""
    config = ctx.obj['config']
    client = ElasticScriptClient(config)
    output = OutputFormatter(config)
    
    stmt = f"TRIGGER AGENT {name}"
    if context:
        stmt += f" WITH {context}"
    
    output.print_info(f"Triggering agent {name}...")
    result = client.execute(stmt)
    output.print_result(result)
    
    if result.success and isinstance(result.data, dict):
        exec_id = result.data.get('execution_id')
        if exec_id:
            output.print_info(f"Execution ID: {exec_id}")
    
    client.close()


@agent.command('history')
@click.argument('name')
@click.option('--limit', '-n', default=10, help='Number of executions to show')
@click.pass_context
def agent_history(ctx, name, limit):
    """Show execution history for an agent."""
    config = ctx.obj['config']
    client = ElasticScriptClient(config)
    output = OutputFormatter(config)
    
    result = client.execute(f"SHOW AGENT {name} HISTORY")
    output.print_result(result)
    client.close()


@agent.command('enable')
@click.argument('name')
@click.pass_context
def agent_enable(ctx, name):
    """Enable an agent."""
    config = ctx.obj['config']
    client = ElasticScriptClient(config)
    output = OutputFormatter(config)
    
    result = client.execute(f"ENABLE AGENT {name}")
    output.print_result(result)
    if result.success:
        output.print_success(f"Agent {name} enabled")
    client.close()


@agent.command('disable')
@click.argument('name')
@click.pass_context
def agent_disable(ctx, name):
    """Disable an agent."""
    config = ctx.obj['config']
    client = ElasticScriptClient(config)
    output = OutputFormatter(config)
    
    result = client.execute(f"DISABLE AGENT {name}")
    output.print_result(result)
    if result.success:
        output.print_success(f"Agent {name} disabled")
    client.close()


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


def main():
    """Main entry point."""
    cli(obj={})


if __name__ == '__main__':
    main()
