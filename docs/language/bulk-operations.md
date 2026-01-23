# Bulk Operations

Bulk operations enable efficient batch processing of collections with built-in error handling.

## FORALL Statement

The `FORALL` statement executes an action for each element in a collection.

### Syntax

```sql
FORALL element IN collection action [SAVE EXCEPTIONS];
```

Where:
- `element` - Variable bound to each item during iteration
- `collection` - An ARRAY to iterate over
- `action` - Either `CALL procedure(...)` or a function call
- `SAVE EXCEPTIONS` - Optional clause to continue on errors

### Basic Usage

```sql
-- Call a procedure for each element
DECLARE documents ARRAY = [{'id': 1}, {'id': 2}, {'id': 3}];
FORALL doc IN documents CALL process_document(doc);
```

### With Built-in Functions

```sql
-- Index each document
DECLARE logs ARRAY = [
    {'message': 'Log 1', 'level': 'INFO'},
    {'message': 'Log 2', 'level': 'WARN'}
];
FORALL log IN logs INDEX_DOCUMENT('my-index', log);
```

### Error Handling with SAVE EXCEPTIONS

Without `SAVE EXCEPTIONS`, FORALL stops at the first error. With `SAVE EXCEPTIONS`, processing continues and errors are collected in the `bulk_errors` variable.

```sql
FORALL item IN items CALL validate(item) SAVE EXCEPTIONS;

-- Check for errors after processing
IF ARRAY_LENGTH(bulk_errors) > 0 THEN
    PRINT 'Some items failed validation';
    
    DECLARE i NUMBER;
    FOR i IN 1..ARRAY_LENGTH(bulk_errors) LOOP
        PRINT 'Error at index ' || bulk_errors[i]['index'] || ': ' || bulk_errors[i]['error'];
    END LOOP;
END IF;
```

### bulk_errors Structure

When using `SAVE EXCEPTIONS`, the `bulk_errors` array contains error records:

| Field | Type | Description |
|-------|------|-------------|
| `index` | NUMBER | Position in collection where error occurred |
| `element` | ANY | The element that caused the error |
| `error` | STRING | Error message |
| `type` | STRING | Exception type name |

## BULK COLLECT Statement

The `BULK COLLECT` statement collects all rows from an ES|QL query into an array.

### Syntax

```sql
BULK COLLECT INTO variable FROM esql_query;
```

### Basic Usage

```sql
-- Collect all error logs
BULK COLLECT INTO all_errors 
    FROM logs-* 
    | WHERE level = 'ERROR' 
    | LIMIT 5000;

PRINT 'Found ' || ARRAY_LENGTH(all_errors) || ' errors';
```

### Processing Collected Data

```sql
BULK COLLECT INTO metrics
    FROM metrics-*
    | WHERE cpu_percent > 90
    | SORT @timestamp DESC
    | LIMIT 1000;

-- Process the collected data
FORALL metric IN metrics CALL send_alert(metric) SAVE EXCEPTIONS;
```

## Combined Example: ETL Pipeline

```sql
CREATE PROCEDURE etl_daily_errors()
BEGIN
    -- 1. BULK COLLECT: Gather data
    BULK COLLECT INTO source_data
        FROM logs-*
        | WHERE level = 'ERROR' AND @timestamp > NOW() - 1 DAY
        | LIMIT 10000;
    
    PRINT 'Collected ' || ARRAY_LENGTH(source_data) || ' error logs';
    
    -- 2. Transform (if needed)
    DECLARE transformed ARRAY = [];
    DECLARE i NUMBER;
    FOR i IN 1..ARRAY_LENGTH(source_data) LOOP
        DECLARE doc DOCUMENT = source_data[i];
        SET doc['processed_at'] = CURRENT_TIMESTAMP();
        SET transformed = ARRAY_APPEND(transformed, doc);
    END LOOP;
    
    -- 3. FORALL: Bulk process
    FORALL doc IN transformed 
        CALL archive_and_notify(doc) 
        SAVE EXCEPTIONS;
    
    -- 4. Report results
    PRINT 'Processed: ' || ARRAY_LENGTH(transformed);
    PRINT 'Failures: ' || ARRAY_LENGTH(bulk_errors);
END PROCEDURE;
```

## With Cursors for Large Datasets

For very large datasets, combine cursors with FORALL:

```sql
CREATE PROCEDURE process_large_dataset()
BEGIN
    DECLARE data_cursor CURSOR FOR 
        FROM logs-* 
        | WHERE level = 'ERROR' 
        | LIMIT 100000;
    
    DECLARE batch ARRAY;
    DECLARE total NUMBER = 0;
    
    OPEN data_cursor;
    FETCH data_cursor LIMIT 1000 INTO batch;
    
    WHILE ARRAY_LENGTH(batch) > 0 LOOP
        -- Process batch
        FORALL item IN batch CALL process(item) SAVE EXCEPTIONS;
        
        SET total = total + ARRAY_LENGTH(batch);
        PRINT 'Processed ' || total || ' items...';
        
        -- Get next batch
        FETCH data_cursor LIMIT 1000 INTO batch;
    END LOOP;
    
    CLOSE data_cursor;
    PRINT 'Complete! Total: ' || total;
END PROCEDURE;
```

## Best Practices

1. **Use SAVE EXCEPTIONS for resilience** - Prevents one bad record from stopping entire batch
2. **Check bulk_errors after processing** - Always review and handle failures
3. **Combine with cursors for large data** - Avoid memory issues with huge collections
4. **Log progress for long operations** - Use PRINT to track batch processing progress

## See Also

- [Cursors](cursors.md) - Streaming large result sets
- [Error Handling](error-handling.md) - TRY/CATCH/FINALLY
- [Control Flow](control-flow.md) - Loops and conditions
