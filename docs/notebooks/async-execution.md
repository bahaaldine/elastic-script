# Async Execution

!!! tip "Interactive Notebook"
    This page is generated from a Jupyter notebook. For the best experience, 
    run it interactively in Jupyter:
    
    ```bash
    ./scripts/quick-start.sh
    # Then open http://localhost:8888/notebooks/04-async-execution.ipynb
    ```
    
    [:fontawesome-brands-github: View on GitHub](https://github.com/bahaaldine/elastic-script/blob/main/notebooks/04-async-execution.ipynb){ .md-button }

---


This notebook demonstrates elastic-script's pipe-driven async execution model - a modern approach to asynchronous workflows.

## Syntax Overview

```sql
-- Async with continuations
procedure() | ON_DONE handler(@result) | ON_FAIL handler(@error) | TRACK AS 'name';

-- Execution control
EXECUTION('name') | STATUS;
EXECUTION('name') | CANCEL;

-- Parallel execution
PARALLEL [proc1(), proc2()] | ON_ALL_DONE merge(@results);
```


## Step 1: Create Helper Procedures


```sql
-- Create a procedure that simulates log analysis
CREATE PROCEDURE analyze_logs(index_pattern STRING)
BEGIN
    PRINT 'Analyzing logs from: ' || index_pattern;
    DECLARE result DOCUMENT = { "analyzed": true, "pattern": index_pattern, "count": 1000 };
    RETURN result;
END PROCEDURE

```

```sql
-- Create a success handler
CREATE PROCEDURE handle_success(result DOCUMENT)
BEGIN
    PRINT 'SUCCESS: Analysis complete!';
    PRINT 'Result: ' || result;
    RETURN 'handled';
END PROCEDURE

```

```sql
-- Create an error handler
CREATE PROCEDURE handle_error(error STRING)
BEGIN
    PRINT 'ERROR: ' || error;
    PRINT 'Alerting on-call team...';
    RETURN 'error_handled';
END PROCEDURE

```

## Step 2: Test Async with ON_DONE


```sql
CREATE PROCEDURE test_async_basic()
BEGIN
    analyze_logs('logs-*')
    | ON_DONE handle_success(@result)
    | TRACK AS 'basic-test';

    RETURN 'Async execution started';
END PROCEDURE

```

```sql
CALL test_async_basic()

```

## Step 3: Check Execution Status


```sql
CREATE PROCEDURE check_status()
BEGIN
    EXECUTION('basic-test') | STATUS;
END PROCEDURE

```

```sql
CALL check_status()

```

## Step 4: Parallel Execution


```sql
CREATE PROCEDURE fetch_logs()
BEGIN
    PRINT 'Fetching logs...';
    RETURN { "source": "logs", "count": 500 };
END PROCEDURE

```

```sql
CREATE PROCEDURE fetch_metrics()
BEGIN
    PRINT 'Fetching metrics...';
    RETURN { "source": "metrics", "count": 200 };
END PROCEDURE

```

```sql
CREATE PROCEDURE merge_results(results ARRAY)
BEGIN
    PRINT 'Merging ' || ARRAY_LENGTH(results) || ' results...';
    RETURN { "merged": true, "total": ARRAY_LENGTH(results) };
END PROCEDURE

```

```sql
CREATE PROCEDURE test_parallel()
BEGIN
    PARALLEL [fetch_logs(), fetch_metrics()]
    | ON_ALL_DONE merge_results(@results)
    | TRACK AS 'parallel-fetch';

    RETURN 'Parallel execution started';
END PROCEDURE

```

```sql
CALL test_parallel()

```

## Summary

| Feature | Syntax |
|---------|--------|
| Async with continuation | `proc() \| ON_DONE handler(@result)` |
| Error handling | `\| ON_FAIL handler(@error)` |
| Track execution | `\| TRACK AS 'name'` |
| Check status | `EXECUTION('name') \| STATUS` |
| Parallel execution | `PARALLEL [p1(), p2()] \| ON_ALL_DONE merge(@results)` |

**Query Executions with ESQL:**
```sql
FROM .escript_executions | WHERE status == 'RUNNING'
```


