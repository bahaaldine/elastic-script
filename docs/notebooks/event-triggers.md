# Event Triggers

This notebook demonstrates how to create event-driven triggers that react to new documents in Elasticsearch indices.

## What are Triggers?

Triggers are event-driven handlers that automatically execute EScript code when specific conditions are met in your Elasticsearch data. Unlike scheduled jobs that run at fixed times, triggers poll indices for new or matching documents and fire when they're found.

Use cases include:

- **Real-time alerting**: Notify when errors or anomalies appear in logs
- **Data processing**: Process documents as they arrive
- **Workflow automation**: Trigger actions based on document state changes
- **Security monitoring**: Detect and respond to suspicious activity

## 1. Creating a Simple Trigger

Let's create a trigger that watches for ERROR level log entries:

```sql
CREATE TRIGGER error_alert
ON INDEX 'logs-*'
WHEN level == 'ERROR'
AS BEGIN
    PRINT 'Found ' || @document_count || ' new errors';
END TRIGGER
```

## 2. Viewing Triggers

Use `SHOW TRIGGERS` to list all triggers:

```sql
SHOW TRIGGERS
```

View details of a specific trigger:

```sql
SHOW TRIGGER error_alert
```

## 3. Polling Intervals

By default, triggers poll every 60 seconds. You can customize this:

### Poll Every 30 Seconds

```sql
CREATE TRIGGER fast_trigger
ON INDEX 'events-*'
WHEN priority == 'high'
EVERY 30 SECONDS
AS BEGIN
    PRINT 'High priority event detected!';
END TRIGGER
```

### Poll Every 5 Minutes

```sql
CREATE TRIGGER slow_trigger
ON INDEX 'metrics-*'
WHEN cpu_usage > 90
EVERY 5 MINUTES
DESCRIPTION 'Checks for high CPU usage'
AS BEGIN
    PRINT 'High CPU detected';
END TRIGGER
```

### Interval Options

| Interval | Example |
|----------|---------|
| Seconds | `EVERY 30 SECONDS` |
| Minutes | `EVERY 5 MINUTES` |
| Hours | `EVERY 2 HOURS` |

## 4. Complex Conditions

Triggers support compound conditions using AND/OR:

```sql
CREATE TRIGGER payment_error_alert
ON INDEX 'logs-*'
WHEN (level == 'ERROR' OR level == 'FATAL') AND service == 'payment'
EVERY 10 SECONDS
DESCRIPTION 'Monitors payment service for errors'
AS BEGIN
    PRINT 'Payment service error detected!';
    -- Send notification
    -- CALL SLACK_SEND('#payment-alerts', 'Payment error detected');
END TRIGGER
```

## 5. Processing Matched Documents

When a trigger fires, it provides access to the matched documents through special bindings:

- `@documents` - An array of matched documents
- `@document_count` - The number of matched documents

```sql
CREATE TRIGGER document_processor
ON INDEX 'queue-*'
WHEN status == 'pending'
EVERY 30 SECONDS
AS BEGIN
    PRINT 'Processing ' || @document_count || ' pending documents';
    
    -- Iterate through matched documents
    DECLARE doc DOCUMENT;
    FOR doc IN @documents LOOP
        PRINT 'Processing document: ' || doc._id;
        -- Process each document here
    END LOOP
END TRIGGER
```

## 6. Managing Triggers

### Disable a Trigger

```sql
ALTER TRIGGER error_alert DISABLE
```

### Enable a Trigger

```sql
ALTER TRIGGER error_alert ENABLE
```

### Change Polling Interval

```sql
ALTER TRIGGER error_alert EVERY 45 SECONDS
```

## 7. Viewing Trigger History

See when a trigger has fired and what it processed:

```sql
SHOW TRIGGER RUNS FOR error_alert
```

## 8. How Triggers Work

### Checkpoint-Based Processing

Triggers use checkpoint tracking to avoid reprocessing documents:

1. On first poll, the trigger records the current timestamp as checkpoint
2. Subsequent polls only query for documents newer than the checkpoint
3. After processing, the checkpoint advances to the newest document's timestamp

This ensures each document is processed exactly once.

### Index Pattern Support

Triggers support Elasticsearch index patterns:

- `logs-*` - All indices starting with "logs-"
- `logs-2026.01.*` - All January 2026 log indices
- `my-exact-index` - A specific index

## 9. Cleanup

Delete triggers when no longer needed:

```sql
DROP TRIGGER error_alert
DROP TRIGGER fast_trigger
DROP TRIGGER slow_trigger
DROP TRIGGER payment_error_alert
DROP TRIGGER document_processor
```

## Summary

| Statement | Description |
|-----------|-------------|
| `CREATE TRIGGER` | Define event-driven triggers on indices |
| `SHOW TRIGGERS` | List all triggers or view specific details |
| `ALTER TRIGGER` | Enable, disable, or change polling interval |
| `SHOW TRIGGER RUNS` | View execution history |
| `DROP TRIGGER` | Delete a trigger |

### Key Features

- **Index patterns**: Watch multiple indices with wildcards
- **Flexible conditions**: Simple or compound conditions
- **Configurable polling**: From seconds to hours
- **Document access**: Process matched documents with `@documents`
- **Checkpoint tracking**: Process each document exactly once

---

[View the interactive notebook →](https://github.com/bahaaldine/elastic-script/blob/main/notebooks/07-event-triggers.ipynb)
