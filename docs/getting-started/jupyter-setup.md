# Jupyter Notebook Setup

elastic-script includes a custom Jupyter kernel for interactive development.

## Quick Setup

If you used `quick-start.sh`, Jupyter is already configured. Access it at:

```
http://localhost:8888
```

## Manual Setup

### 1. Install Python Dependencies

```bash
cd notebooks
pip install -r requirements.txt
```

### 2. Install the Kernel

```bash
cd kernel
./install.sh
```

This installs `plesql_kernel` as a Jupyter kernel.

### 3. Start Jupyter

```bash
jupyter notebook
```

Or via quick-start:
```bash
./scripts/quick-start.sh --notebooks
```

## Using the Kernel

### Create a New Notebook

1. Open Jupyter at `http://localhost:8888`
2. Click "New" → "plesql_kernel"
3. Start writing procedures!

### Example Cells

**Cell 1: Create a procedure**
```sql
CREATE PROCEDURE hello()
BEGIN
    RETURN 'Hello from Jupyter!';
END PROCEDURE
```

**Cell 2: Call it**
```sql
CALL hello()
```

**Output:**
```
Hello from Jupyter!
```

## Available Notebooks

The `notebooks/` directory includes example notebooks:

| Notebook | Description |
|----------|-------------|
| `01-getting-started.ipynb` | Basic language features |
| `02-esql-integration.ipynb` | ES|QL queries and cursors |
| `03-ai-observability.ipynb` | LLM functions |
| `04-async-execution.ipynb` | ON_DONE, PARALLEL |
| `05-runbook-integrations.ipynb` | Slack, K8s, PagerDuty |
| `00-complete-reference.ipynb` | All functions showcase |

## Kernel Configuration

The kernel connects to Elasticsearch at:

```python
# notebooks/kernel/plesql_kernel.py
self.es_endpoint = "http://localhost:9200/_escript"
self.es_auth = ("elastic-admin", "elastic-password")
```

To change credentials, edit the kernel file.

## Troubleshooting

### Kernel Not Found

```bash
# Reinstall the kernel
cd notebooks/kernel
./install.sh

# Verify installation
jupyter kernelspec list
```

### Connection Refused

Ensure Elasticsearch is running:
```bash
curl -u elastic-admin:elastic-password http://localhost:9200
```

### Syntax Errors

The kernel expects elastic-script syntax, not Python:

```sql
-- ✅ Correct
CREATE PROCEDURE test() BEGIN RETURN 42; END PROCEDURE;

-- ❌ Wrong (Python syntax)
def test():
    return 42
```

## Tips

### Use PRINT for Debugging

```sql
CREATE PROCEDURE debug_demo()
BEGIN
    DECLARE x NUMBER = 10;
    PRINT 'Value of x: ' || x;
    
    SET x = x * 2;
    PRINT 'After doubling: ' || x;
    
    RETURN x;
END PROCEDURE;
```

### Comments

```sql
-- Single line comment

/*
 * Multi-line
 * comment
 */
```

### View All Procedures

```sql
-- List stored procedures
CALL ESCRIPT_PROCEDURES()
```
