# Async Execution

elastic-script supports **pipe-driven asynchronous execution**, allowing you to chain procedures with continuations and run tasks in parallel.

## ON_DONE / ON_FAIL

Chain procedures with success and failure handlers:

```sql
CREATE PROCEDURE run_analysis()
BEGIN
    analyze_logs()
        | ON_DONE handle_success(@result)
        | ON_FAIL handle_error(@error);
    
    RETURN 'Analysis started';
END PROCEDURE;
```

### How It Works

1. `analyze_logs()` executes asynchronously
2. On success: `handle_success()` is called with `@result`
3. On failure: `handle_error()` is called with `@error`
4. The calling procedure returns immediately

### Bindings

| Binding | Description | Available In |
|---------|-------------|--------------|
| `@result` | Return value of previous procedure | `ON_DONE` |
| `@error` | Error message from failed procedure | `ON_FAIL` |
| `@results` | Array of results from parallel execution | `ON_ALL_DONE` |

## PARALLEL Execution

Run multiple procedures concurrently:

```sql
CREATE PROCEDURE fetch_all_data()
BEGIN
    PARALLEL [fetch_logs(), fetch_metrics(), fetch_events()]
        | ON_ALL_DONE merge_results(@results);
    
    RETURN 'Parallel fetch started';
END PROCEDURE;

CREATE PROCEDURE merge_results(results ARRAY)
BEGIN
    PRINT 'Received ' || ARRAY_LENGTH(results) || ' results';
    -- Process merged results
    RETURN results;
END PROCEDURE;
```

### Parallel Continuations

| Continuation | Triggered When |
|--------------|----------------|
| `ON_ALL_DONE` | All procedures complete successfully |
| `ON_ANY_FAIL` | Any procedure fails |

## TRACK AS

Track execution state for later querying:

```sql
analyze_logs()
    | ON_DONE handle_success(@result)
    | TRACK AS 'daily-analysis';
```

Execution state is stored in `.escript_executions` index.

## EXECUTION Control

Query and control tracked executions:

```sql
-- Check status
EXECUTION('daily-analysis') | STATUS;

-- Cancel execution
EXECUTION('daily-analysis') | CANCEL;

-- Retry failed execution
EXECUTION('daily-analysis') | RETRY;

-- Wait for completion
EXECUTION('daily-analysis') | WAIT;
```

## FINALLY

Execute cleanup code regardless of success/failure:

```sql
process_data()
    | ON_DONE save_results(@result)
    | ON_FAIL log_error(@error)
    | FINALLY cleanup();
```

## Complete Example

```sql
-- Main analysis procedure
CREATE PROCEDURE analyze_logs(index_pattern STRING)
BEGIN
    PRINT 'Analyzing logs from: ' || index_pattern;
    
    DECLARE errors CURSOR FOR
        FROM logs-*
        | WHERE level == "ERROR"
        | WHERE @timestamp > NOW() - 1 HOUR
        | LIMIT 100;
    
    DECLARE count NUMBER = 0;
    FOR error IN errors LOOP
        SET count = count + 1;
    END LOOP;
    
    RETURN { "error_count": count, "index": index_pattern };
END PROCEDURE;

-- Success handler
CREATE PROCEDURE handle_success(result DOCUMENT)
BEGIN
    PRINT 'Analysis complete: ' || result;
    
    IF result.error_count > 50 THEN
        -- Trigger alert
        SLACK_SEND('#alerts', 'High error count: ' || result.error_count);
    END IF;
    
    RETURN 'handled';
END PROCEDURE;

-- Error handler
CREATE PROCEDURE handle_error(error STRING)
BEGIN
    PRINT 'Analysis failed: ' || error;
    PAGERDUTY_TRIGGER('Analysis pipeline failed', error);
    RETURN 'error_handled';
END PROCEDURE;

-- Run the pipeline
CREATE PROCEDURE run_daily_analysis()
BEGIN
    analyze_logs('logs-*')
        | ON_DONE handle_success(@result)
        | ON_FAIL handle_error(@error)
        | TRACK AS 'daily-analysis';
    
    RETURN 'Analysis pipeline started';
END PROCEDURE;
```

## Execution State

Tracked executions are stored in `.escript_executions`:

```json
{
  "execution_id": "exec-abc123",
  "track_name": "daily-analysis",
  "status": "COMPLETED",
  "procedure_name": "analyze_logs",
  "result": { "error_count": 42, "index": "logs-*" },
  "started_at": "2026-01-09T10:00:00Z",
  "completed_at": "2026-01-09T10:00:05Z",
  "duration_ms": 5000
}
```

## Best Practices

!!! tip "Keep handlers simple"
    Continuation handlers should be lightweight.
    Heavy processing should happen in the main procedure.

!!! tip "Use TRACK for important workflows"
    Track executions that you need to monitor or retry.

!!! warning "Async is inside procedures only"
    Async syntax (`|`) is only valid inside procedures,
    not as top-level API calls.
