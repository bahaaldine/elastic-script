# Bulk Operations Notebook

This notebook demonstrates FORALL and BULK COLLECT for efficient batch processing.

## Topics Covered

1. **Basic FORALL** - Execute actions for each element in a collection
2. **FORALL with Functions** - Use with built-in functions like INDEX_DOCUMENT
3. **SAVE EXCEPTIONS** - Continue on errors, collect failures
4. **BULK COLLECT** - Collect ES|QL results into arrays
5. **ETL Pipelines** - Combine BULK COLLECT and FORALL
6. **Cursor Integration** - Process large datasets with cursors + FORALL

## Open the Notebook

Launch the notebook server and open `17-bulk-operations.ipynb`:

```bash
./scripts/quick-start.sh
# Then navigate to http://localhost:8888
```

## Key Concepts

### FORALL Syntax

```sql
FORALL element IN collection action [SAVE EXCEPTIONS];
```

- `action` can be `CALL procedure(...)` or a function call
- With `SAVE EXCEPTIONS`, errors are collected in `bulk_errors`

### BULK COLLECT Syntax

```sql
BULK COLLECT INTO variable FROM esql_query;
```

Collects all query results into an ARRAY variable.

## See Also

- [Bulk Operations Language Reference](../language/bulk-operations.md)
- [Cursors Notebook](cursors.md)
