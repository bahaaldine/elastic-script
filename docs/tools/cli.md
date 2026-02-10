# elastic-script CLI

A beautiful, feature-rich command-line interface for elastic-script.

## Overview

The `escript` CLI provides an interactive REPL (Read-Eval-Print Loop) for elastic-script with:

- **Syntax Highlighting** - Full Pygments-based syntax highlighting
- **Auto-Completion** - Smart completion for keywords, functions, procedures, and skills
- **Multi-line Editing** - Intelligent statement continuation
- **Rich Output** - Beautiful tables and formatted JSON using Rich
- **Persistent History** - Searchable command history

## Installation

### From Source

```bash
cd cli
pip install -e .
```

### From PyPI

```bash
pip install escript-cli
```

### Dependencies

The CLI requires Python 3.9+ and installs these packages:

- `prompt_toolkit` - Interactive prompt with completion and history
- `rich` - Beautiful terminal formatting
- `pygments` - Syntax highlighting
- `requests` - HTTP client
- `click` - CLI framework
- `toml` - Configuration file parsing

## Quick Start

### Start Interactive Mode

```bash
escript
```

This launches the interactive REPL:

```
┌─────────────────────────────────────┐
│ elastic-script                      │
├─────────────────────────────────────┤
│ Connected to my-cluster             │
│ (Elasticsearch 8.17.0)              │
│                                     │
│ Type 'help' for commands, Ctrl+D    │
│ to exit                             │
└─────────────────────────────────────┘

escript> 
```

### Execute a Single Query

```bash
# Execute and display result
escript query "CALL hello()"

# Output as JSON
escript query --json "SHOW PROCEDURES"
```

### Run a Script File

```bash
# Execute a script
escript run myscript.es

# Verbose mode (show each statement)
escript run --verbose setup.es

# Dry run (parse only, don't execute)
escript run --dry-run test.es
```

## Commands

### Interactive Mode

Start the interactive REPL:

```bash
escript
```

Options:

| Option | Description |
|--------|-------------|
| `--host`, `-h` | Elasticsearch host |
| `--port`, `-p` | Elasticsearch port |
| `--user`, `-u` | Username |
| `--password`, `-P` | Password |
| `--ssl` / `--no-ssl` | Enable/disable SSL |
| `--no-color` | Disable colored output |

### Query Command

Execute a single statement:

```bash
escript query "STATEMENT"
```

Options:

| Option | Description |
|--------|-------------|
| `--json`, `-j` | Output result as JSON |

Examples:

```bash
# Create a procedure
escript query "CREATE PROCEDURE greet() BEGIN PRINT 'Hello!'; END PROCEDURE;"

# Call a procedure
escript query "CALL greet()"

# Show all procedures
escript query "SHOW PROCEDURES"

# Get JSON output for scripting
escript query --json "SHOW SKILLS" | jq '.[] | .name'
```

### Run Command

Execute a script file:

```bash
escript run SCRIPT_FILE
```

Options:

| Option | Description |
|--------|-------------|
| `--verbose`, `-v` | Show each statement as it executes |
| `--dry-run`, `-n` | Parse only, don't execute |

Example script file (`setup.es`):

```sql
-- Initialize application data
CREATE PROCEDURE init_app()
BEGIN
    INDEX_DOCUMENT('app-config', {'version': '1.0', 'initialized': true});
    PRINT 'Application initialized';
END PROCEDURE;

-- Run initialization
CALL init_app();
```

Execute:

```bash
escript run --verbose setup.es
```

### Config Command

Manage CLI configuration:

```bash
# Show current configuration
escript config --show

# Create sample config file
escript config --init
```

### Test Command

Test connection to Elasticsearch:

```bash
escript test
```

## Configuration

### Config File

Create `~/.escriptrc` in TOML format:

```toml
[connection]
host = "localhost"
port = 9200
username = "elastic-admin"
password = "elastic-password"
use_ssl = false
verify_certs = true

[repl]
multiline = true
vi_mode = false
history_file = "~/.escript_history"
history_size = 10000

[output]
color = true
table_style = "rounded"  # rounded, simple, minimal, markdown
json_indent = 2
# max_width = 120  # Uncomment to set fixed width

[editor]
command = "vim"
```

Generate a sample config:

```bash
escript config --init
```

### Environment Variables

| Variable | Description |
|----------|-------------|
| `ESCRIPT_HOST` | Elasticsearch host |
| `ESCRIPT_PORT` | Elasticsearch port |
| `ESCRIPT_USERNAME` | Username |
| `ESCRIPT_PASSWORD` | Password |
| `ESCRIPT_USE_SSL` | Enable SSL (`true`/`false`) |
| `ESCRIPT_COLOR` | Enable colors (`true`/`false`) |
| `ESCRIPT_VI_MODE` | Enable vi mode (`true`/`false`) |

### Priority

Configuration is loaded in this order (later overrides earlier):

1. Default values
2. Config file (`~/.escriptrc`)
3. Environment variables (`ESCRIPT_*`)
4. Command-line options

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Tab` | Auto-complete |
| `Ctrl+D` | Exit (or delete char) |
| `Ctrl+C` | Cancel current input |
| `Ctrl+L` | Clear screen |
| `Ctrl+R` | Reverse search history |
| `Up/Down` | Navigate history |
| `Ctrl+A` | Go to beginning of line |
| `Ctrl+E` | Go to end of line |

## Multi-line Input

The CLI automatically detects multi-line statements. When you type a keyword that starts a block (like `BEGIN`, `THEN`, `LOOP`), the prompt continues to the next line:

```
escript> CREATE PROCEDURE analyze_logs(days NUMBER)
   ... BEGIN
   ...   DECLARE log_count NUMBER;
   ...   SET log_count := ESQL_QUERY(
   ...     'FROM logs-* | WHERE @timestamp > NOW() - ' || days || 'd | STATS count = COUNT(*)'
   ...   )[0].count;
   ...   PRINT 'Found ' || log_count || ' log entries in last ' || days || ' days';
   ... END PROCEDURE;

✓ CREATE PROCEDURE 'analyze_logs'
```

Press `Enter` on an empty line or end with `;` to execute.

## REPL Commands

Special commands available in interactive mode:

| Command | Description |
|---------|-------------|
| `help` | Show help information |
| `exit` / `quit` / `q` | Exit the CLI |
| `clear` | Clear the screen |
| `history` | Show recent commands |
| `config` | Show current configuration |
| `refresh` | Reload completions from server |

## Auto-Completion

The CLI provides intelligent auto-completion:

### Keywords

Type the beginning of any keyword and press `Tab`:

```
escript> CRE<Tab>
CREATE
```

### Built-in Functions

```
escript> ESQL_<Tab>
ESQL_QUERY
```

```
escript> ARRAY_<Tab>
ARRAY_APPEND    ARRAY_CONTAINS  ARRAY_DISTINCT  ...
```

### Context-Aware

After `CALL`, the CLI suggests procedure names:

```
escript> CALL my<Tab>
my_procedure    my_other_proc
```

After `SHOW`, it suggests viewable objects:

```
escript> SHOW <Tab>
PROCEDURES  FUNCTIONS  SKILLS  APPLICATIONS  ...
```

## Output Formatting

### Tables

List results are displayed as formatted tables:

```
escript> SHOW PROCEDURES

┌──────────────────┬────────────┬─────────────────────┐
│ name             │ parameters │ created             │
├──────────────────┼────────────┼─────────────────────┤
│ analyze_logs     │ 1          │ 2024-01-15T10:30:00 │
│ cleanup_old      │ 2          │ 2024-01-14T08:15:00 │
│ generate_report  │ 0          │ 2024-01-13T14:45:00 │
└──────────────────┴────────────┴─────────────────────┘
3 row(s)
```

### Action Results

CREATE/DROP operations show clear feedback:

```
escript> CREATE PROCEDURE hello() BEGIN PRINT 'Hi!'; END PROCEDURE;

✓ CREATE PROCEDURE 'hello'
  parameters: 0
```

### Errors

Errors are displayed in a distinct panel:

```
escript> CRATE PROCEDURE typo()

┌─────────────────────────────────────┐
│ Error                               │
├─────────────────────────────────────┤
│ Syntax error: unexpected token      │
│ 'CRATE' at line 1, column 1.        │
│ Did you mean 'CREATE'?              │
└─────────────────────────────────────┘
```

## Examples

### Interactive Session

```
escript> -- Create a simple logging procedure
escript> CREATE PROCEDURE log_error(message STRING)
   ... BEGIN
   ...   INDEX_DOCUMENT('app-logs', {
   ...     'level': 'ERROR',
   ...     'message': message,
   ...     'timestamp': CURRENT_TIMESTAMP()
   ...   });
   ... END PROCEDURE;

✓ CREATE PROCEDURE 'log_error'

escript> CALL log_error('Database connection failed')

✓ Query executed successfully

escript> SHOW PROCEDURES

┌───────────┬────────────┐
│ name      │ parameters │
├───────────┼────────────┤
│ log_error │ 1          │
└───────────┴────────────┘
```

### Script Execution

Create `etl_job.es`:

```sql
-- ETL Job: Process daily metrics

CREATE PROCEDURE process_daily_metrics()
BEGIN
    DECLARE yesterday STRING;
    SET yesterday := DATE_SUB(CURRENT_DATE(), 1, 'DAY');
    
    -- Aggregate metrics
    DECLARE metrics ARRAY;
    SET metrics := ESQL_QUERY(
        'FROM raw-metrics-* | WHERE date = "' || yesterday || '" | STATS avg_cpu = AVG(cpu), max_mem = MAX(memory) BY host'
    );
    
    -- Store aggregated results
    FOR metric IN metrics LOOP
        INDEX_DOCUMENT('daily-metrics', {
            'date': yesterday,
            'host': metric.host,
            'avg_cpu': metric.avg_cpu,
            'max_mem': metric.max_mem
        });
    END LOOP;
    
    PRINT 'Processed ' || ARRAY_LENGTH(metrics) || ' hosts for ' || yesterday;
END PROCEDURE;

CALL process_daily_metrics();
```

Run:

```bash
escript run --verbose etl_job.es
```

## Troubleshooting

### Connection Issues

```bash
# Test connection
escript test

# Check with custom host
escript --host elasticsearch.local --port 9200 test
```

### SSL Certificate Issues

```bash
# Disable certificate verification (not recommended for production)
export ESCRIPT_VERIFY_CERTS=false
escript
```

### Completion Not Working

```bash
# Refresh completions from server
escript> refresh
✓ Completions refreshed
```

## Development

### Running Tests

```bash
cd cli
pip install -e ".[dev]"
pytest tests/ -v
```

### Building

```bash
pip install build
python -m build
```

### Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests
5. Submit a pull request

## See Also

- [Getting Started](../getting-started.md) - Introduction to elastic-script
- [Language Reference](../language/procedures.md) - Full language documentation
- [Jupyter Kernel](jupyter-kernel.md) - Using elastic-script in Jupyter notebooks
