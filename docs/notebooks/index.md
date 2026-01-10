# Interactive Notebooks

Learn elastic-script through hands-on Jupyter notebooks.

## Overview

These notebooks provide interactive tutorials for learning elastic-script. Each notebook focuses on a specific topic and includes runnable code examples.

!!! tip "Running the Notebooks"
    The quickest way to get started:
    
    ```bash
    git clone https://github.com/bahaaldine/elastic-script.git
    cd elastic-script
    ./scripts/quick-start.sh
    ```
    
    This starts Elasticsearch, loads sample data, and launches Jupyter at `http://localhost:8888`

---

## Available Notebooks

### 1. Getting Started
**File:** `01-getting-started.ipynb`

Learn the fundamentals:

- Creating your first procedure
- Variables and data types
- Control flow (IF/ELSE, FOR loops)
- Error handling with TRY/CATCH

[View Notebook →](getting-started.md){ .md-button }

---

### 2. ES|QL Integration
**File:** `02-esql-integration.ipynb`

Query Elasticsearch data:

- Using `ESQL_QUERY()` to fetch data
- Working with cursors for result iteration
- Aggregations and statistics
- Data transformation pipelines

[View Notebook →](esql-integration.md){ .md-button }

---

### 3. AI & Observability
**File:** `03-ai-observability.ipynb`

AI-powered automation:

- Log summarization with LLMs
- Intelligent error classification
- Anomaly detection
- Automated root cause analysis

!!! note "Requires OpenAI API Key"
    Set `OPENAI_API_KEY` environment variable before running.

[View Notebook →](ai-observability.md){ .md-button }

---

### 4. Async Execution
**File:** `04-async-execution.ipynb`

Pipe-driven workflows:

- Async procedure execution with `ON_DONE`/`ON_FAIL`
- Error handling with `FINALLY`
- Parallel execution with `PARALLEL`
- Execution tracking with `EXECUTION() | STATUS`

[View Notebook →](async-execution.md){ .md-button }

---

### 5. Runbook Integrations
**File:** `05-runbook-integrations.ipynb`

External service integration:

- Kubernetes operations
- PagerDuty incident management
- AWS Lambda invocation
- Slack notifications
- HTTP requests

[View Notebook →](runbook-integrations.md){ .md-button }

---

### Complete Reference
**File:** `00-complete-reference.ipynb`

Comprehensive examples of all features:

- All 106 built-in functions
- Every language construct
- Edge cases and advanced patterns

[View Notebook →](complete-reference.md){ .md-button }

---

## Notebook Requirements

The notebooks use a custom Jupyter kernel (`plesql_kernel`) that communicates with Elasticsearch.

### Prerequisites

- Python 3.9+
- Elasticsearch running with elastic-script plugin
- Jupyter Notebook or JupyterLab

### Manual Setup

If you prefer manual setup over the quick-start script:

```bash
# Install Python dependencies
cd notebooks
pip install -r requirements.txt

# Install the kernel
cd kernel
./install.sh

# Start Jupyter
jupyter notebook
```

---

## Sample Data

The notebooks expect sample data to be loaded. The quick-start script handles this automatically, but you can also load it manually:

```bash
# Load sample logs data
curl -u elastic-admin:elastic-password -X POST "localhost:9200/logs-sample/_doc" \
  -H "Content-Type: application/json" \
  -d '{"@timestamp": "2026-01-09T10:00:00Z", "level": "ERROR", "message": "Connection timeout"}'
```

---

## Troubleshooting

### Kernel Not Found

If Jupyter doesn't show the `plesql_kernel`:

```bash
cd notebooks/kernel
./install.sh
# Restart Jupyter
```

### Connection Refused

Make sure Elasticsearch is running:

```bash
curl -u elastic-admin:elastic-password http://localhost:9200
```

### Syntax Errors

Ensure you're using the `plesql_kernel`, not Python:

1. In Jupyter, go to Kernel → Change Kernel
2. Select "PL|ESQL (elastic-script)"
