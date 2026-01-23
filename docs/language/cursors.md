# Cursors

Cursors provide streaming access to ES|QL query results, enabling efficient processing of large datasets without loading everything into memory at once.

## Cursor Lifecycle

1. **DECLARE** - Define the cursor with an ES|QL query
2. **OPEN** - Execute the query and position before the first row
3. **FETCH** - Retrieve rows one at a time or in batches
4. **CLOSE** - Release resources

## Declaring Cursors

Declare a cursor with the ES|QL query that defines the data to process:

```sql
DECLARE cursor_name CURSOR FOR esql_query;
```

### Example

```sql
DECLARE error_cursor CURSOR FOR
    FROM logs-*
    | WHERE level = 'ERROR'
    | SORT @timestamp DESC
    | LIMIT 1000;
```

## Opening Cursors

The `OPEN` statement executes the cursor's query:

```sql
OPEN cursor_name;
```

After opening:
- The query has been executed
- Results are available for fetching
- Position is before the first row

## Fetching Data

### Single Row Fetch

Fetch the next row into a DOCUMENT variable:

```sql
DECLARE row DOCUMENT;
FETCH cursor_name INTO row;
```

The variable receives the row as a document with column names as keys.

### Batch Fetch

Fetch multiple rows at once into an ARRAY variable:

```sql
DECLARE batch ARRAY;
FETCH cursor_name LIMIT 100 INTO batch;
```

This is more efficient for processing large datasets. The limit can be:
- A literal number: `LIMIT 100`
- A variable: `LIMIT batch_size`
- An expression: `LIMIT page_num * 10`

## Cursor Attributes

### %NOTFOUND

Returns `TRUE` when the last FETCH operation retrieved no rows:

```sql
WHILE NOT cursor_name%NOTFOUND LOOP
    FETCH cursor_name INTO row;
    -- Process row...
END LOOP;
```

### %ROWCOUNT

Returns the total number of rows fetched so far:

```sql
PRINT 'Processed ' || cursor_name%ROWCOUNT || ' rows';
```

## Closing Cursors

Always close cursors to release resources:

```sql
CLOSE cursor_name;
```

Use `TRY/FINALLY` to ensure cursors are closed even when errors occur:

```sql
TRY
    OPEN my_cursor;
    -- Process data...
CATCH
    PRINT 'Error: ' || error['message'];
FINALLY
    CLOSE my_cursor;
END TRY;
```

## Complete Example

```sql
CREATE PROCEDURE process_errors()
BEGIN
    -- 1. Declare cursor
    DECLARE error_cursor CURSOR FOR
        FROM logs-*
        | WHERE level = 'ERROR'
        | SORT @timestamp DESC
        | LIMIT 1000;

    DECLARE batch ARRAY;
    DECLARE total_processed NUMBER = 0;

    TRY
        -- 2. Open cursor
        OPEN error_cursor;
        PRINT 'Starting error processing...';

        -- 3. Fetch and process in batches
        FETCH error_cursor LIMIT 100 INTO batch;

        WHILE ARRAY_LENGTH(batch) > 0 LOOP
            -- Process each row in batch
            DECLARE i NUMBER;
            FOR i IN 1..ARRAY_LENGTH(batch) LOOP
                DECLARE row DOCUMENT = batch[i];
                -- Do something with each error...
                PRINT row['@timestamp'] || ': ' || row['message'];
            END LOOP;

            SET total_processed = total_processed + ARRAY_LENGTH(batch);
            PRINT 'Progress: ' || total_processed || ' rows processed';

            -- Fetch next batch
            FETCH error_cursor LIMIT 100 INTO batch;
        END LOOP;

        PRINT 'Complete! Total: ' || error_cursor%ROWCOUNT;

    FINALLY
        -- 4. Always close cursor
        CLOSE error_cursor;
    END TRY;
END PROCEDURE;
```

## Cursor Attributes Reference

| Attribute | Type | Description |
|-----------|------|-------------|
| `cursor%NOTFOUND` | BOOLEAN | `TRUE` if last FETCH returned no rows |
| `cursor%ROWCOUNT` | NUMBER | Total number of rows fetched |

## Best Practices

1. **Always close cursors** - Use `TRY/FINALLY` to ensure cleanup
2. **Use batch fetching** - More efficient than row-by-row for large datasets
3. **Monitor %ROWCOUNT** - Useful for progress reporting and debugging
4. **Check %NOTFOUND after FETCH** - Standard pattern for loop termination

## Common Patterns

### Basic Loop Pattern

```sql
OPEN cursor;
FETCH cursor INTO row;
WHILE NOT cursor%NOTFOUND LOOP
    -- Process row
    FETCH cursor INTO row;
END LOOP;
CLOSE cursor;
```

### Batch Processing Pattern

```sql
OPEN cursor;
FETCH cursor LIMIT batch_size INTO batch;
WHILE ARRAY_LENGTH(batch) > 0 LOOP
    -- Process batch
    FETCH cursor LIMIT batch_size INTO batch;
END LOOP;
CLOSE cursor;
```

### Safe Processing with Error Handling

```sql
TRY
    OPEN cursor;
    -- Process...
CATCH
    PRINT 'Error: ' || error['message'];
FINALLY
    CLOSE cursor;
END TRY;
```

## See Also

- [ESQL Integration](../esql-integration.md) - Working with ES|QL queries
- [Control Flow](control-flow.md) - Loops and conditions
- [Error Handling](error-handling.md) - TRY/CATCH/FINALLY
