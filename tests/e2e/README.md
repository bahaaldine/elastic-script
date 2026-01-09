# E2E Test Framework for elastic-script

This framework programmatically executes Jupyter notebooks to test elastic-script functionality end-to-end.

## Quick Start

```bash
# Run all notebook tests
./run_tests.sh

# Run a specific notebook
./run_tests.sh --notebook 01-getting-started

# With verbose output
./run_tests.sh --verbose

# Setup infrastructure and run tests
./run_tests.sh --setup
```

## Prerequisites

1. **Elasticsearch running** with elastic-script plugin
   ```bash
   cd /path/to/elastic-script
   ./scripts/quick-start.sh
   ```

2. **plesql_kernel installed** for Jupyter
   ```bash
   cd notebooks/kernel
   ./install.sh
   ```

3. **Python dependencies**
   ```bash
   pip install -r requirements.txt
   ```

## How It Works

The test runner uses `nbclient` to:
1. Connect to Elasticsearch and verify it's running
2. Execute each notebook cell through the `plesql_kernel`
3. Check for errors in cell outputs
4. Report pass/fail for each notebook

## Test Results

| Notebook | Status | Requirements |
|----------|--------|--------------|
| 01-getting-started | ✅ Pass | ES running |
| 02-esql-integration | ❌ | Sample data loaded |
| 03-ai-observability | ❌ | OpenAI API key |
| 04-async-execution | ❌ | Investigation needed |
| 05-runbook-integrations | ✅ Pass | ES running |
| 00-complete-reference | ❌ | Sample data + OpenAI |

## Running with Sample Data

To run tests that require sample data:

```bash
# Load sample data first
cd /path/to/elastic-script
./scripts/quick-start.sh  # This loads sample data

# Then run tests
./tests/e2e/run_tests.sh
```

## Running with OpenAI API Key

For AI/LLM tests:

```bash
export OPENAI_API_KEY="your-key-here"
./scripts/quick-start.sh  # Starts ES with key passed through
./tests/e2e/run_tests.sh --notebook 03
```

## Command Line Options

```
usage: run_notebook_tests.py [-h] [--notebook NOTEBOOK] [--verbose] [--list]

Run E2E notebook tests for elastic-script

options:
  -h, --help            show this help message and exit
  --notebook, -n        Filter notebooks by name (e.g., '01', 'getting-started')
  --verbose, -v         Verbose output showing each cell execution
  --list, -l            List available notebooks without running tests
```

## Adding New Tests

1. Create a new notebook in `/notebooks/` directory
2. Use `plesql_kernel` as the kernel
3. Ensure procedures are self-contained (CREATE + CALL)
4. Run the test: `./run_tests.sh --notebook your-notebook`

## Troubleshooting

### "Elasticsearch is not running"
Start ES with: `./scripts/quick-start.sh`

### "Kernel not found"
Install the kernel: `cd notebooks/kernel && ./install.sh`

### Cell timeout errors
Increase timeout in `run_notebook_tests.py`:
```python
client = NotebookClient(nb, timeout=300, ...)  # 5 minutes
```

### "no such index" errors
Load sample data: `./scripts/quick-start.sh`

## Architecture

```
tests/e2e/
├── README.md                 # This file
├── requirements.txt          # Python dependencies
├── run_tests.sh              # Shell wrapper
├── run_notebook_tests.py     # Main test runner
└── __init__.py               # Python package marker
```

The test runner:
1. Checks ES connectivity
2. Loads each notebook as nbformat
3. Creates NotebookClient with plesql_kernel
4. Executes cells sequentially
5. Collects pass/fail results
6. Reports summary
