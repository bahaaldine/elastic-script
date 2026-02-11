# Moltler CLI

A beautiful, feature-rich command-line interface for **Moltler** - the AI Skills Creation Framework for Elasticsearch.

![Demo](../docs/images/cli-demo.gif)

## Features

- **Syntax Highlighting** - Full Pygments-based syntax highlighting for elastic-script
- **Auto-Completion** - Intelligent completion for keywords, functions, procedures, and skills
- **Multi-line Editing** - Smart statement continuation with BEGIN/END block detection
- **Rich Output** - Beautiful tables, JSON formatting, and colored output using Rich
- **Persistent History** - Searchable command history with Ctrl+R
- **Script Execution** - Run `.es` script files from the command line
- **Configuration** - Flexible config via file, environment, or CLI flags

## Installation

### From Source

```bash
cd cli
pip install -e .
```

### From PyPI (Coming Soon)

```bash
pip install escript-cli
```

## Quick Start

### Interactive Mode

Start an interactive session:

```bash
escript
```

You'll see:

```
┌─────────────────────────────────────┐
│ elastic-script                      │
├─────────────────────────────────────┤
│ Connected to elastic-cluster        │
│ (Elasticsearch 8.17.0)              │
│                                     │
│ Type 'help' for commands, Ctrl+D    │
│ to exit                             │
└─────────────────────────────────────┘

escript> 
```

### Execute a Query

```bash
# Single query
escript query "CALL hello()"

# With JSON output
escript query --json "SHOW PROCEDURES"
```

### Run a Script File

```bash
# Execute script
escript run myscript.es

# With verbose output
escript run --verbose setup.es

# Dry run (parse only)
escript run --dry-run test.es
```

### Using the Query Language

All Moltler operations are performed through the query language. Use `escript query` to execute statements:

```bash
# Skills
escript query "SHOW SKILLS"
escript query "SHOW SKILL analyze_logs"
escript query "TEST SKILL analyze_logs WITH index = 'logs-*'"
escript query "CREATE SKILL hello() RETURNS STRING AS 'Hello!';"

# Connectors
escript query "SHOW CONNECTORS"
escript query "TEST CONNECTOR my_github"
escript query "SYNC CONNECTOR my_github TO 'github-*'"
escript query "QUERY my_github.issues WHERE state = 'open' LIMIT 10"

# Agents
escript query "SHOW AGENTS"
escript query "TRIGGER AGENT incident_responder"
escript query "TRIGGER AGENT incident_responder WITH {'priority': 'high'}"
escript query "SHOW AGENT incident_responder HISTORY"
escript query "ENABLE AGENT incident_responder"
escript query "DISABLE AGENT incident_responder"
```

## Configuration

### Config File

Create `~/.escriptrc` (TOML format):

```toml
[connection]
host = "localhost"
port = 9200
username = "elastic-admin"
password = "elastic-password"
use_ssl = false

[repl]
multiline = true
vi_mode = false
history_file = "~/.escript_history"

[output]
color = true
table_style = "rounded"  # rounded, simple, minimal, markdown
json_indent = 2
```

Generate a sample config:

```bash
escript config --init
```

### Environment Variables

```bash
export ESCRIPT_HOST=localhost
export ESCRIPT_PORT=9200
export ESCRIPT_USERNAME=elastic-admin
export ESCRIPT_PASSWORD=elastic-password
export ESCRIPT_USE_SSL=false
```

### Command Line Options

```bash
escript --host prod.example.com --port 9243 --ssl --user admin
```

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Tab` | Auto-complete |
| `Ctrl+D` | Exit (or delete char) |
| `Ctrl+C` | Cancel current input |
| `Ctrl+L` | Clear screen |
| `Ctrl+R` | Search history |
| `Up/Down` | Navigate history |

## Multi-line Input

The CLI automatically detects multi-line statements. Statements ending with keywords like `BEGIN`, `THEN`, `LOOP`, or `(` will continue to the next line:

```
escript> CREATE PROCEDURE greet(name STRING)
   ... BEGIN
   ...   PRINT 'Hello, ' || name || '!';
   ... END PROCEDURE;

✓ CREATE PROCEDURE 'greet'
```

## Commands

### CLI Commands

| Command | Description |
|---------|-------------|
| `escript` | Start interactive REPL |
| `escript query "..."` | Execute a single statement |
| `escript run <file>` | Execute a script file |
| `escript config` | Show current configuration |
| `escript test` | Test connection to Elasticsearch |

### REPL Commands

| Command | Description |
|---------|-------------|
| `help` | Show help information |
| `help examples` | Show ready-to-run examples |
| `help tutorial` | Interactive getting started guide |
| `help functions` | List available functions |
| `help syntax` | Language syntax reference |
| `exit` | Exit the CLI |
| `clear` | Clear the screen |
| `history` | Show recent commands |
| `config` | Show current configuration |
| `refresh` | Reload completions from server |

### Language Statements

Use these in the REPL or via `escript query`:

| Statement | Description |
|-----------|-------------|
| `SHOW SKILLS` | List all skills |
| `SHOW SKILL <name>` | Show skill details |
| `TEST SKILL <name>` | Test a skill |
| `CREATE SKILL ...` | Create a new skill |
| `SHOW CONNECTORS` | List all connectors |
| `TEST CONNECTOR <name>` | Test connector connectivity |
| `SYNC CONNECTOR <name> TO ...` | Sync data to Elasticsearch |
| `QUERY <conn>.<entity> ...` | Query connector data |
| `SHOW AGENTS` | List all agents |
| `TRIGGER AGENT <name>` | Manually trigger an agent |
| `SHOW AGENT <name> HISTORY` | Show execution history |

## Examples

### Create and Call a Procedure

```
escript> CREATE PROCEDURE analyze_logs()
   ... BEGIN
   ...   DECLARE log_count NUMBER;
   ...   SET log_count := ESQL_QUERY('FROM logs-* | STATS count = COUNT(*)')[0].count;
   ...   PRINT 'Found ' || log_count || ' log entries';
   ... END PROCEDURE;

✓ CREATE PROCEDURE 'analyze_logs'

escript> CALL analyze_logs()
Found 1523 log entries
```

### Query with Pretty Output

```
escript> SHOW SKILLS

┌──────────────────┬─────────────────────────────┬────────────┐
│ name             │ description                 │ parameters │
├──────────────────┼─────────────────────────────┼────────────┤
│ analyze_errors   │ Analyze error patterns      │ 2          │
│ check_health     │ Check cluster health        │ 0          │
│ generate_report  │ Generate system report      │ 1          │
└──────────────────┴─────────────────────────────┴────────────┘
3 row(s)
```

### Working with Skills

```
escript> SHOW SKILLS

┌──────────────────┬─────────┬─────────────────────────────┐
│ name             │ version │ description                 │
├──────────────────┼─────────┼─────────────────────────────┤
│ check_health     │ 1.0     │ Check cluster health        │
│ analyze_logs     │ 1.2     │ Analyze application logs    │
└──────────────────┴─────────┴─────────────────────────────┘

escript> TEST SKILL analyze_logs WITH index = 'logs-*'
✓ Test PASSED
  Result: {"log_count": 1523, "error_rate": 0.02}
```

### Working with Connectors

```
escript> SHOW CONNECTORS

┌─────────────┬───────────┬──────────────────────────────┐
│ name        │ type      │ status                       │
├─────────────┼───────────┼──────────────────────────────┤
│ github_ops  │ github    │ enabled (last sync: 1h ago)  │
│ jira_bugs   │ jira      │ enabled (last sync: 30m ago) │
└─────────────┴───────────┴──────────────────────────────┘

escript> TEST CONNECTOR github_ops
✓ Connection successful
  Rate limit: 4987/5000 remaining

escript> SYNC CONNECTOR github_ops TO 'github-issues-*'
✓ Synced 127 issues
```

### Working with Agents

```
escript> SHOW AGENTS

┌─────────────────────┬─────────────┬──────────────────────────┐
│ name                │ status      │ last run                 │
├─────────────────────┼─────────────┼──────────────────────────┤
│ incident_responder  │ enabled     │ 2024-01-15 10:23:45      │
│ log_analyzer        │ enabled     │ 2024-01-15 09:00:00      │
└─────────────────────┴─────────────┴──────────────────────────┘

escript> TRIGGER AGENT incident_responder WITH {'alert': 'high-cpu'}
✓ Agent triggered
  Execution ID: exec_abc123

escript> SHOW AGENT incident_responder HISTORY

┌─────────────────┬──────────┬─────────────────┬──────────┐
│ execution_id    │ status   │ started         │ duration │
├─────────────────┼──────────┼─────────────────┼──────────┤
│ exec_abc123     │ success  │ 10:23:45        │ 12.3s    │
│ exec_def456     │ success  │ 09:00:00        │ 8.7s     │
└─────────────────┴──────────┴─────────────────┴──────────┘
```

### Run a Script File

```bash
$ cat setup.es
-- Setup script for my application
CREATE PROCEDURE init_data()
BEGIN
  INDEX_DOCUMENT('my-index', {'initialized': true});
END PROCEDURE;

CALL init_data();

$ escript run --verbose setup.es
ℹ Running setup.es...

Statement 1/2:
CREATE PROCEDURE init_data()...
✓ CREATE PROCEDURE 'init_data'

Statement 2/2:
CALL init_data()
✓ Query executed successfully

✓ Executed 2 statement(s)
```

## Development

### Running Tests

```bash
cd cli
pytest tests/ -v
```

### Building

```bash
pip install build
python -m build
```

## License

Apache 2.0 License
