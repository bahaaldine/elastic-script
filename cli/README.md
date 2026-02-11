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

### Manage Skills

```bash
# List all skills
escript skill list

# Show skill details
escript skill show analyze_logs

# Test a skill
escript skill test analyze_logs --with "index=logs-*,threshold=0.9"
```

### Manage Connectors

```bash
# List all connectors
escript connector list

# Show connector details
escript connector show my_github

# Test connectivity
escript connector test my_github

# Sync data from connector
escript connector sync my_github

# Sync specific entity
escript connector sync my_github --entity issues
```

### Manage Agents

```bash
# List all agents
escript agent list

# Show agent details
escript agent show incident_responder

# Trigger an agent
escript agent trigger incident_responder

# Trigger with context
escript agent trigger incident_responder --context '{"priority": "high"}'

# Show execution history
escript agent history incident_responder

# Enable/disable agents
escript agent enable incident_responder
escript agent disable incident_responder
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

### General Commands

| Command | Description |
|---------|-------------|
| `help` | Show help information |
| `exit` | Exit the CLI |
| `clear` | Clear the screen |
| `history` | Show recent commands |
| `config` | Show current configuration |
| `refresh` | Reload completions from server |

### Skill Commands

| Command | Description |
|---------|-------------|
| `skill list` | List all available skills |
| `skill show <name>` | Show skill details |
| `skill test <name>` | Test a skill with parameters |

### Connector Commands

| Command | Description |
|---------|-------------|
| `connector list` | List all connectors |
| `connector show <name>` | Show connector details |
| `connector test <name>` | Test connector connectivity |
| `connector sync <name>` | Sync data from connector |

### Agent Commands

| Command | Description |
|---------|-------------|
| `agent list` | List all agents |
| `agent show <name>` | Show agent details |
| `agent trigger <name>` | Manually trigger an agent |
| `agent history <name>` | Show execution history |
| `agent enable <name>` | Enable an agent |
| `agent disable <name>` | Disable an agent |

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

### Create and Test a Skill

```bash
$ escript skill list
┌──────────────────┬─────────┬─────────────────────────────┐
│ name             │ version │ description                 │
├──────────────────┼─────────┼─────────────────────────────┤
│ check_health     │ 1.0     │ Check cluster health        │
│ analyze_logs     │ 1.2     │ Analyze application logs    │
└──────────────────┴─────────┴─────────────────────────────┘

$ escript skill test analyze_logs --with "index=logs-*"
✓ Test PASSED
  Execution time: 1.24s
  Result: {"log_count": 1523, "error_rate": 0.02}
```

### Manage Connectors

```bash
$ escript connector list
┌─────────────┬───────────┬──────────────────────────────┐
│ name        │ type      │ status                       │
├─────────────┼───────────┼──────────────────────────────┤
│ github_ops  │ github    │ enabled (last sync: 1h ago)  │
│ jira_bugs   │ jira      │ enabled (last sync: 30m ago) │
└─────────────┴───────────┴──────────────────────────────┘

$ escript connector test github_ops
✓ Connection successful
  Rate limit: 4987/5000 remaining

$ escript connector sync github_ops --entity issues
✓ Synced 127 issues to github-ops-issues-*
```

### Agent Operations

```bash
$ escript agent list
┌─────────────────────┬─────────────┬──────────────────────────┐
│ name                │ status      │ last run                 │
├─────────────────────┼─────────────┼──────────────────────────┤
│ incident_responder  │ enabled     │ 2024-01-15 10:23:45      │
│ log_analyzer        │ enabled     │ 2024-01-15 09:00:00      │
└─────────────────────┴─────────────┴──────────────────────────┘

$ escript agent trigger incident_responder --context '{"alert": "high-cpu"}'
✓ Agent triggered
  Execution ID: exec_abc123

$ escript agent history incident_responder
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
