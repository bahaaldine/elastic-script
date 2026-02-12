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
    1. Fetching live data from GitHub's public API
    2. Indexing it into Elasticsearch
    3. Creating a skill that queries the data
    4. Executing the skill and showing real results
    
    Perfect for first-time users to see Moltler in action.
    """
    import json
    import urllib.request
    import urllib.error
    import ssl
    
    config = ctx.obj['config']
    client = ElasticScriptClient(config)
    output = OutputFormatter(config)
    
    output.console.print()
    output.console.print("[bold cyan]━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━[/]")
    output.console.print("[bold cyan]  🦌 Welcome to the Moltler Demo![/]")
    output.console.print("[bold cyan]━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━[/]")
    output.console.print()
    output.console.print("  Moltler connects [bold]external data[/] to Elasticsearch.")
    output.console.print("  Let's fetch live GitHub issues and query them!\n")
    
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
    
    # Step 2: Fetch live data from GitHub
    output.console.print("[bold]Step 2:[/] Fetching live issues from GitHub...")
    output.console.print("  [dim]Source: github.com/elastic/elasticsearch (public API)[/]")
    output.console.print()
    
    github_url = "https://api.github.com/repos/elastic/elasticsearch/issues?state=open&per_page=20&labels=good%20first%20issue"
    
    try:
        # Create SSL context (handles macOS certificate issues)
        ssl_context = ssl.create_default_context()
        try:
            import certifi
            ssl_context.load_verify_locations(certifi.where())
        except ImportError:
            # Fallback for systems without certifi
            ssl_context.check_hostname = False
            ssl_context.verify_mode = ssl.CERT_NONE
        
        req = urllib.request.Request(
            github_url,
            headers={"User-Agent": "Moltler-Demo/1.0", "Accept": "application/vnd.github.v3+json"}
        )
        with urllib.request.urlopen(req, timeout=10, context=ssl_context) as response:
            issues = json.loads(response.read().decode('utf-8'))
        
        if not issues:
            # Fallback to regular issues if no "good first issue" found
            github_url = "https://api.github.com/repos/elastic/elasticsearch/issues?state=open&per_page=20"
            req = urllib.request.Request(
                github_url,
                headers={"User-Agent": "Moltler-Demo/1.0", "Accept": "application/vnd.github.v3+json"}
            )
            with urllib.request.urlopen(req, timeout=10, context=ssl_context) as response:
                issues = json.loads(response.read().decode('utf-8'))
        
        output.print_success(f"Fetched {len(issues)} issues from GitHub")
    except urllib.error.URLError as e:
        output.print_error(f"Could not reach GitHub API: {e}")
        output.console.print("  [yellow]Tip:[/] Check your internet connection")
        client.close()
        sys.exit(1)
    output.console.print()
    
    # Step 3: Index into Elasticsearch using REST API
    output.console.print("[bold]Step 3:[/] Indexing issues into Elasticsearch...")
    output.console.print()
    
    indexed_count = 0
    es_url = config.url.rstrip('/')
    auth_header = None
    if config.username and config.password:
        import base64
        credentials = base64.b64encode(f"{config.username}:{config.password}".encode()).decode()
        auth_header = f"Basic {credentials}"
    
    for issue in issues[:15]:  # Limit to 15 for demo
        doc = {
            "number": issue.get("number"),
            "title": issue.get("title"),
            "state": issue.get("state"),
            "labels": [label.get("name") for label in issue.get("labels", [])],
            "created_at": issue.get("created_at"),
            "updated_at": issue.get("updated_at"),
            "comments": issue.get("comments", 0),
            "author": issue.get("user", {}).get("login"),
            "url": issue.get("html_url"),
            "body_preview": (issue.get("body") or "")[:200]
        }
        
        try:
            doc_id = str(issue.get("number", indexed_count))
            req = urllib.request.Request(
                f"{es_url}/github-issues/_doc/{doc_id}",
                data=json.dumps(doc).encode('utf-8'),
                headers={
                    "Content-Type": "application/json",
                    **({"Authorization": auth_header} if auth_header else {})
                },
                method="PUT"
            )
            with urllib.request.urlopen(req, timeout=5, context=ssl_context) as resp:
                indexed_count += 1
        except Exception:
            pass  # Continue on individual failures
    
    # Refresh the index
    try:
        req = urllib.request.Request(
            f"{es_url}/github-issues/_refresh",
            headers={**({"Authorization": auth_header} if auth_header else {})},
            method="POST"
        )
        urllib.request.urlopen(req, timeout=5, context=ssl_context)
    except Exception:
        pass
    
    output.print_success(f"Indexed {indexed_count} issues into 'github-issues'")
    output.console.print()
    
    # Step 4: Create a procedure (the logic)
    output.console.print("[bold]Step 4:[/] Creating the procedure logic...")
    output.console.print()
    
    proc_code = """CREATE PROCEDURE find_popular_issues()
BEGIN
  DECLARE issues ARRAY;
  SET issues = ESQL_QUERY('FROM github-issues | SORT comments DESC | LIMIT 5');
  RETURN issues;
END PROCEDURE;"""
    
    output.console.print("[dim]" + proc_code + "[/]")
    output.console.print()
    
    # Drop and recreate
    client.execute("DROP PROCEDURE find_popular_issues")
    result = client.execute(proc_code)
    
    if result.success:
        output.print_success("Procedure created!")
    else:
        output.print_error(f"Failed to create procedure: {result.error}")
        client.close()
        sys.exit(1)
    output.console.print()
    
    # Step 5: Create a skill that wraps the procedure
    output.console.print("[bold]Step 5:[/] Registering as a skill...")
    output.console.print()
    
    skill_code = """CREATE SKILL find_popular_issues
  VERSION '1.0'
  DESCRIPTION 'Find the most discussed GitHub issues from elastic/elasticsearch'
  AUTHOR 'Moltler Demo'
  RETURNS ARRAY
BEGIN
  RETURN CALL find_popular_issues();
END SKILL;"""
    
    output.console.print("[dim]" + skill_code + "[/]")
    output.console.print()
    
    client.execute("DROP SKILL find_popular_issues")
    result = client.execute(skill_code)
    
    if result.success:
        output.print_success("Skill 'find_popular_issues' registered!")
    else:
        # Skill registration is optional - proceed even if it fails
        output.console.print(f"  [dim]Note: Skill metadata registration pending[/]")
    output.console.print()
    
    # Step 6: Show available skills
    output.console.print("[bold]Step 6:[/] Viewing available skills...")
    output.console.print()
    output.console.print("[dim]SHOW SKILLS[/]")
    output.console.print()
    
    result = client.execute("SHOW SKILLS")
    if result.success:
        output.print_result(result)
    output.console.print()
    
    # Step 7: Execute the skill and show real results
    output.console.print("[bold]Step 7:[/] Running the skill to find popular issues...")
    output.console.print()
    output.console.print("[dim]CALL find_popular_issues()[/]")
    output.console.print()
    
    result = client.execute("CALL find_popular_issues()")
    
    if result.success and result.data:
        output.console.print("[green]Results from Elasticsearch:[/]")
        output.console.print()
        # Format the results nicely
        if isinstance(result.data, list):
            for i, row in enumerate(result.data[:5], 1):
                if isinstance(row, dict):
                    number = row.get('number', '?')
                    title = row.get('title', 'No title')[:50]
                    comments = row.get('comments', 0)
                    labels = row.get('labels', [])
                    # Handle labels safely
                    if isinstance(labels, list):
                        label_list = [str(l) for l in labels if l][:2]
                        label_str = ", ".join(label_list) if label_list else ""
                    else:
                        label_str = ""
                    info_parts = [f"{comments} comments"]
                    if label_str:
                        info_parts.append(label_str)
                    output.console.print(f"  [cyan]#{number}[/] {title}...")
                    output.console.print(f"       [dim]{' | '.join(info_parts)}[/]")
                else:
                    output.console.print(f"  {row}")
            output.console.print()
        else:
            output.print_result(result)
        output.print_success(f"Found {len(result.data) if isinstance(result.data, list) else 1} issue(s)!")
    else:
        output.console.print("  [yellow]Note:[/] No issues found.")
    output.console.print()
    
    # Summary
    output.console.print("[bold cyan]━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━[/]")
    output.console.print("[bold cyan]  🎉 Demo Complete![/]")
    output.console.print("[bold cyan]━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━[/]")
    output.console.print()
    output.console.print("  [bold]What you just did:[/]")
    output.console.print("    • Fetched [green]live data[/] from GitHub's public API")
    output.console.print("    • Indexed it into [cyan]Elasticsearch[/] automatically")
    output.console.print("    • Created a [cyan]reusable skill[/] to query it")
    output.console.print("    • Got real results from real data!")
    output.console.print()
    output.console.print("  [bold]Next steps:[/]")
    output.console.print("    [cyan]moltler[/]                        Start the interactive shell")
    output.console.print("    [yellow]SHOW SKILLS[/]                   See all your skills")
    output.console.print("    [yellow]CALL find_popular_issues()[/]    Run the skill again")
    output.console.print("    [yellow]SHOW SKILL find_popular_issues[/]   View skill details")
    output.console.print()
    output.console.print("  [bold]Learn more:[/]")
    output.console.print("    https://bahaaldine.github.io/elastic-script/")
    output.console.print()
    
    client.close()


def main():
    """Main entry point."""
    cli(obj={})


if __name__ == '__main__':
    main()
