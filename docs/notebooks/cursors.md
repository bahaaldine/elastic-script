# Cursors Notebook

This notebook demonstrates cursor operations for streaming large ES|QL result sets.

## Topics Covered

1. **Basic Cursor Usage** - DECLARE, OPEN, FETCH, CLOSE lifecycle
2. **Batch Fetching** - Processing rows in batches with FETCH LIMIT
3. **Cursor Attributes** - Using %NOTFOUND and %ROWCOUNT
4. **Processing Large Datasets** - Efficient batch processing patterns
5. **Error Handling** - Safe cleanup with TRY/FINALLY

## Open the Notebook

Launch the notebook server and open `16-cursors.ipynb`:

```bash
./scripts/quick-start.sh
# Then navigate to http://localhost:8888
```

Or run the notebook directly:

```bash
cd notebooks
jupyter notebook 16-cursors.ipynb
```

## Key Concepts

### Cursor Lifecycle

```sql
-- 1. Declare cursor
DECLARE my_cursor CURSOR FOR FROM logs-* | LIMIT 1000;

-- 2. Open (execute query)
OPEN my_cursor;

-- 3. Fetch rows
FETCH my_cursor INTO row;
FETCH my_cursor LIMIT 100 INTO batch;

-- 4. Close
CLOSE my_cursor;
```

### Cursor Attributes

| Attribute | Description |
|-----------|-------------|
| `cursor%NOTFOUND` | TRUE when last FETCH returned no rows |
| `cursor%ROWCOUNT` | Total rows fetched so far |

## See Also

- [Cursors Language Reference](../language/cursors.md)
- [ESQL Integration Notebook](esql-integration.md)
