# Triggers & Scheduling

Event-driven automation and scheduled job execution in elastic-script.

---

## Overview

elastic-script provides two mechanisms for automated execution:

| Feature | Description | Use Case |
|---------|-------------|----------|
| **Scheduled Jobs** | Cron-based recurring execution | Daily cleanup, hourly reports |
| **Event Triggers** | React to new documents in indices | Alert on errors, process events |

Both use a **polling-based architecture** that is:
- ✅ Non-invasive (no indexing performance impact)
- ✅ Scalable (configurable poll intervals)
- ✅ Reliable (checkpoint-based, survives restarts)

---

## Scheduled Jobs

### CREATE JOB

```sql
CREATE JOB job_name
SCHEDULE 'cron_expression'
[TIMEZONE 'timezone']
[ENABLED true|false]
[DESCRIPTION 'description']
AS
BEGIN
    -- procedure body
END JOB
```

### Examples

**Daily cleanup at 2 AM:**
```sql
CREATE JOB daily_cleanup
SCHEDULE '0 2 * * *'
TIMEZONE 'UTC'
DESCRIPTION 'Archive logs older than 30 days'
AS
BEGIN
    DECLARE old_logs ARRAY
    SET old_logs = ESQL_QUERY('FROM logs-* | WHERE @timestamp < NOW() - 30 DAYS | LIMIT 10000')
    PRINT 'Archiving ' || ARRAY_LENGTH(old_logs) || ' old logs'
    
    FOR doc IN old_logs LOOP
        CALL archive_to_s3(doc)
    END LOOP
END JOB
```

**Hourly health check:**
```sql
CREATE JOB hourly_health_check
SCHEDULE '@hourly'
AS
BEGIN
    DECLARE error_count NUMBER
    SET error_count = ESQL_QUERY(
        'FROM logs-* | WHERE level = ''ERROR'' AND @timestamp > NOW() - 1 HOUR | STATS count = COUNT(*)'
    )[0].count
    
    IF error_count > 100 THEN
        CALL SLACK_SEND('#alerts', '⚠️ High error rate: ' || error_count || ' errors in last hour')
    END IF
END JOB
```

### Schedule Patterns

| Pattern | Description |
|---------|-------------|
| `* * * * *` | Every minute |
| `*/5 * * * *` | Every 5 minutes |
| `0 * * * *` | Every hour at :00 |
| `0 2 * * *` | Daily at 2:00 AM |
| `0 0 * * 0` | Weekly on Sunday at midnight |
| `0 0 1 * *` | Monthly on 1st at midnight |
| `@hourly` | Alias for `0 * * * *` |
| `@daily` | Alias for `0 0 * * *` |
| `@weekly` | Alias for `0 0 * * 0` |
| `@monthly` | Alias for `0 0 1 * *` |

### Managing Jobs

```sql
-- Disable a job
ALTER JOB daily_cleanup DISABLE

-- Enable a job
ALTER JOB daily_cleanup ENABLE

-- Change schedule
ALTER JOB daily_cleanup SCHEDULE '0 3 * * *'

-- Delete a job
DROP JOB daily_cleanup

-- List all jobs
SHOW JOBS

-- Show job details
SHOW JOB daily_cleanup

-- Show execution history
SHOW JOB RUNS FOR daily_cleanup
```

---

## Event Triggers

### CREATE TRIGGER

```sql
CREATE TRIGGER trigger_name
ON INDEX 'index_pattern'
[WHEN condition]
[EVERY interval]
[ENABLED true|false]
[DESCRIPTION 'description']
AS
BEGIN
    -- @documents: array of matched documents
    -- @document_count: number of matched documents
END TRIGGER
```

### Examples

**Alert on payment errors:**
```sql
CREATE TRIGGER on_payment_error
ON INDEX 'logs-*'
WHEN level = 'ERROR' AND service = 'payment'
EVERY 5 SECONDS
DESCRIPTION 'Alert team on payment service errors'
AS
BEGIN
    FOR doc IN @documents LOOP
        CALL SLACK_SEND('#payment-alerts', 
            '🚨 Payment Error: ' || doc.message || ' | User: ' || doc.user_id
        )
        
        IF doc.error_code = 'PAYMENT_DECLINED' THEN
            CALL PAGERDUTY_TRIGGER('Payment system issue', 'high', doc)
        END IF
    END LOOP
END TRIGGER
```

**Security event monitoring:**
```sql
CREATE TRIGGER on_security_event
ON INDEX 'security-events-*'
WHEN event_type IN ('login_failed', 'unauthorized_access')
EVERY 10 SECONDS
AS
BEGIN
    IF @document_count > 10 THEN
        CALL SLACK_SEND('#security', 
            '🔒 Security Alert: ' || @document_count || ' suspicious events'
        )
    END IF
    
    FOR doc IN @documents LOOP
        CALL log_security_event(doc)
    END LOOP
END TRIGGER
```

**Error spike detection:**
```sql
CREATE TRIGGER on_error_spike
ON INDEX 'logs-*'
WHEN level = 'ERROR'
EVERY 1 MINUTE
AS
BEGIN
    IF @document_count > 50 THEN
        CALL SLACK_SEND('#alerts', 
            '📈 Error spike: ' || @document_count || ' errors in last minute'
        )
    END IF
END TRIGGER
```

### Poll Intervals

| Interval | Syntax |
|----------|--------|
| 1 second | `EVERY 1 SECOND` |
| 5 seconds | `EVERY 5 SECONDS` |
| 30 seconds | `EVERY 30 SECONDS` |
| 1 minute | `EVERY 1 MINUTE` |
| 5 minutes | `EVERY 5 MINUTES` |

Default: `EVERY 30 SECONDS`

### Managing Triggers

```sql
-- Disable a trigger
ALTER TRIGGER on_payment_error DISABLE

-- Enable a trigger
ALTER TRIGGER on_payment_error ENABLE

-- Change poll interval
ALTER TRIGGER on_payment_error EVERY 10 SECONDS

-- Delete a trigger
DROP TRIGGER on_payment_error

-- List all triggers
SHOW TRIGGERS

-- Show trigger details
SHOW TRIGGER on_payment_error

-- Show execution history
SHOW TRIGGER RUNS FOR on_payment_error
```

---

## Architecture

### Polling-Based Design

```
┌─────────────────────────────────────────────────────────────┐
│                    TRIGGER POLLING                           │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  .escript_triggers (stored trigger definition)               │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ name: on_payment_error                                  ││
│  │ index_pattern: logs-*                                   ││
│  │ condition: level = 'ERROR' AND service = 'payment'      ││
│  │ poll_interval: 5 seconds                                ││
│  │ last_checkpoint: 2026-01-12T10:00:00Z                   ││
│  └─────────────────────────────────────────────────────────┘│
│                              │                               │
│                              ▼                               │
│  Every poll_interval:                                        │
│  1. Query: FROM logs-* WHERE @timestamp > last_checkpoint   │
│  2. Filter: Apply WHEN condition                            │
│  3. Execute: Trigger procedure with @documents bound        │
│  4. Update: last_checkpoint = max(@timestamp)               │
└─────────────────────────────────────────────────────────────┘
```

### Leader Election

Only one node executes jobs and triggers to prevent duplicates:

- Uses `.escript_leader` index with TTL-based lock
- Leader refreshes lock periodically
- If leader fails, another node takes over

### Storage Indices

| Index | Purpose |
|-------|---------|
| `.escript_jobs` | Job definitions |
| `.escript_triggers` | Trigger definitions |
| `.escript_job_runs` | Job execution history |
| `.escript_trigger_runs` | Trigger execution history |
| `.escript_leader` | Leader election lock |

---

## Best Practices

### 1. Choose Appropriate Intervals

```sql
-- Critical alerts: short interval
CREATE TRIGGER critical_alerts ON INDEX 'logs-*'
WHEN level = 'CRITICAL'
EVERY 1 SECOND  -- Fast response needed
...

-- Regular monitoring: longer interval
CREATE TRIGGER daily_summary ON INDEX 'metrics-*'
EVERY 5 MINUTES  -- Aggregated view is fine
...
```

### 2. Use Batch Processing

```sql
-- Good: Process all documents together
CREATE TRIGGER batch_processor
ON INDEX 'events-*'
EVERY 30 SECONDS
AS
BEGIN
    -- Process all at once
    CALL bulk_process(@documents)
END TRIGGER

-- Avoid: Individual processing with external calls
CREATE TRIGGER slow_processor
ON INDEX 'events-*'
EVERY 1 SECOND
AS
BEGIN
    -- This could be slow with many documents
    FOR doc IN @documents LOOP
        CALL HTTP_POST('https://api.example.com', doc)  -- Slow!
    END LOOP
END TRIGGER
```

### 3. Handle Failures Gracefully

```sql
CREATE TRIGGER robust_handler
ON INDEX 'logs-*'
WHEN level = 'ERROR'
EVERY 10 SECONDS
AS
BEGIN
    FOR doc IN @documents LOOP
        TRY
            CALL SLACK_SEND('#alerts', doc.message)
        CATCH
            PRINT 'Failed to send alert: ' || @error.message
            -- Don't fail entire trigger
        END TRY
    END LOOP
END TRIGGER
```
