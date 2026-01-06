# 📓 elastic-script Notebooks

Interactive Jupyter notebooks to learn and test elastic-script capabilities.

## Quick Start

```bash
# From the elastic-script root directory:
./scripts/quick-start.sh
```

This will:
1. ✅ Check prerequisites (Java 21+, Python, Jupyter)
2. ✅ Build the elastic-script plugin
3. ✅ Start Elasticsearch
4. ✅ Load sample data
5. ✅ Start Jupyter with the notebooks

## Manual Setup

### 1. Install the Jupyter Kernel

```bash
cd notebooks
pip install -r requirements.txt
./kernel/install.sh
```

### 2. Start Elasticsearch

```bash
cd ../elasticsearch
./gradlew :run
```

### 3. Load Sample Data

```bash
../scripts/quick-start.sh --load-data
```

### 4. Start Jupyter

```bash
jupyter notebook
```

## Available Notebooks

| Notebook | Description | Difficulty |
|----------|-------------|------------|
| **01-getting-started.ipynb** | Hello World, variables, control flow | 🟢 Beginner |
| **02-esql-integration.ipynb** | ESQL queries, cursors, aggregations | 🟢 Beginner |
| **03-ai-observability.ipynb** | LLM-powered log analysis | 🟡 Intermediate |
| **04-async-execution.ipynb** | Pipe-driven async workflows | 🟡 Intermediate |
| **05-runbook-integrations.ipynb** | K8s, PagerDuty, AWS integrations | 🔴 Advanced |

## Notebook Order

We recommend going through the notebooks in order:

```
01-getting-started → 02-esql-integration → 03-ai-observability
                                                    ↓
                        05-runbook-integrations ← 04-async-execution
```

## Prerequisites

- **Java 21+** - Required to build and run Elasticsearch
- **Python 3.8+** - Required for Jupyter
- **Jupyter** - `pip install jupyter`

### Optional for AI Features

- **OpenAI API Key** - Set `OPENAI_API_KEY` environment variable
- **Elasticsearch Inference Endpoints** - For embeddings and reranking

## Sample Data

The quick-start script loads sample data into these indices:

| Index | Description | Fields |
|-------|-------------|--------|
| `logs-sample` | Sample application logs | timestamp, level, message, service, host |
| `metrics-sample` | Sample system metrics | timestamp, metric, value, service |

## Kernel Details

The PL|ESQL kernel sends code to Elasticsearch's `/_escript` endpoint. Results are:
- Displayed as tables (for arrays of documents)
- Displayed as JSON (for single documents)
- Displayed as text (for simple values)

### Kernel Location

```
~/.local/share/jupyter/kernels/plesql/
```

## Troubleshooting

### "Connection Error: Cannot connect to Elasticsearch"

Make sure Elasticsearch is running:
```bash
curl http://localhost:9200
```

If not, start it:
```bash
./scripts/quick-start.sh --start
```

### "Kernel not found"

Reinstall the kernel:
```bash
cd notebooks
./kernel/install.sh
jupyter kernelspec list  # Should show 'plesql'
```

### "Module not found: pandas"

Install dependencies:
```bash
pip install -r notebooks/requirements.txt
```

## Creating Your Own Notebooks

1. Create a new notebook with the **elastic-script (PL|ESQL)** kernel
2. Write procedures using elastic-script syntax
3. Execute cells to create and call procedures

Example cell:
```sql
CREATE PROCEDURE my_procedure()
BEGIN
    RETURN 'Hello from my procedure!';
END PROCEDURE
```

Then in another cell:
```sql
CALL my_procedure()
```

## License

Apache 2.0 - See the main repository LICENSE file.

