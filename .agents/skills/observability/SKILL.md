---
name: observability
description: Analyze logs, metrics, traces, and APM data in Elasticsearch. Use when the user wants to troubleshoot issues, find errors, analyze performance, or investigate incidents.
---

# Observability

This skill enables you to analyze observability data (logs, metrics, traces, APM) stored in Elasticsearch.

## When to Use

- User wants to **find errors** or **troubleshoot issues**
- User asks about **application performance**
- User needs to **analyze logs** or **metrics**
- User wants to **trace requests** across services
- User is investigating an **incident**
- User asks about **error rates** or **latency**

## Log Analysis

### Find Recent Errors

```sql
-- Find errors in the last hour
ESQL FROM logs-* 
| WHERE @timestamp > NOW() - 1 HOUR 
| WHERE level = 'ERROR' OR log.level = 'error'
| SORT @timestamp DESC
| LIMIT 50;

-- Count errors by service
ESQL FROM logs-* 
| WHERE @timestamp > NOW() - 24 HOURS 
| WHERE level = 'ERROR'
| STATS error_count = COUNT(*) BY service.name
| SORT error_count DESC;
```

### Log Pattern Analysis

```sql
-- Most common error messages
ESQL FROM logs-* 
| WHERE @timestamp > NOW() - 1 HOUR 
| WHERE level = 'ERROR'
| STATS count = COUNT(*) BY message
| SORT count DESC
| LIMIT 20;

-- Errors by host
ESQL FROM logs-* 
| WHERE level = 'ERROR' AND @timestamp > NOW() - 6 HOURS
| STATS count = COUNT(*) BY host.name
| SORT count DESC;
```

## Metrics Analysis

### System Metrics

```sql
-- CPU usage by host
ESQL FROM metrics-* 
| WHERE @timestamp > NOW() - 1 HOUR
| WHERE metricset.name = 'cpu'
| STATS avg_cpu = AVG(system.cpu.total.pct) BY host.name
| SORT avg_cpu DESC;

-- Memory usage trends
ESQL FROM metrics-* 
| WHERE metricset.name = 'memory'
| STATS avg_mem = AVG(system.memory.used.pct) BY host.name
| WHERE avg_mem > 80
| SORT avg_mem DESC;

-- Disk space check
ESQL FROM metrics-* 
| WHERE metricset.name = 'filesystem'
| STATS used_pct = AVG(system.filesystem.used.pct) BY host.name, system.filesystem.mount_point
| WHERE used_pct > 85;
```

## APM & Traces

### Service Performance

```sql
-- Slow transactions
ESQL FROM traces-apm* 
| WHERE @timestamp > NOW() - 1 HOUR
| WHERE transaction.duration.us > 5000000  -- > 5 seconds
| SORT transaction.duration.us DESC
| LIMIT 20;

-- Error rate by service
ESQL FROM traces-apm* 
| WHERE @timestamp > NOW() - 1 HOUR
| STATS 
    total = COUNT(*),
    errors = SUM(CASE WHEN event.outcome = 'failure' THEN 1 ELSE 0 END)
BY service.name
| EVAL error_rate = errors * 100.0 / total
| SORT error_rate DESC;
```

### Distributed Tracing

```sql
-- Get a specific trace
ESQL FROM traces-apm*
| WHERE trace.id = 'abc123...'
| SORT @timestamp ASC;

-- Find slow spans in a trace
ESQL FROM traces-apm*
| WHERE trace.id = 'abc123...'
| WHERE span.duration.us > 1000000  -- > 1 second
| SORT span.duration.us DESC;
```

## Kibana APM Functions

| Function | Description |
|----------|-------------|
| `KIBANA_APM_SERVICES()` | List APM services |
| `KIBANA_APM_SERVICE_TRANSACTIONS(service)` | Get transactions |
| `KIBANA_APM_SERVICE_ERRORS(service)` | Get service errors |
| `KIBANA_APM_TRACE(trace_id)` | Get trace details |

## Pre-built Skills (Moltler)

| Skill | Description |
|-------|-------------|
| `RUN SKILL get_recent_errors(minutes, limit)` | Find recent errors |
| `RUN SKILL count_logs_by_level(index, hours)` | Log level distribution |
| `RUN SKILL list_apm_services()` | List monitored services |
| `RUN SKILL get_slow_transactions(threshold_ms)` | Find slow transactions |
| `RUN SKILL error_rate_by_service(hours)` | Calculate error rates |

## Incident Investigation Workflow

### Step 1: Identify the Problem

```sql
-- Check for spike in errors
ESQL FROM logs-* 
| WHERE @timestamp > NOW() - 2 HOURS
| STATS count = COUNT(*) BY level, BUCKET(@timestamp, 5 MINUTE)
| WHERE level = 'ERROR';
```

### Step 2: Find Affected Services

```sql
-- Which services are affected?
ESQL FROM logs-* 
| WHERE @timestamp > NOW() - 30 MINUTES
| WHERE level = 'ERROR'
| STATS error_count = COUNT(*) BY service.name
| SORT error_count DESC;
```

### Step 3: Analyze Error Details

```sql
-- Get error details
DECLARE service STRING;
SET service = 'payment-service';

ESQL FROM logs-* 
| WHERE @timestamp > NOW() - 30 MINUTES
| WHERE level = 'ERROR' AND service.name = service
| KEEP @timestamp, message, error.message, error.stack_trace, trace.id
| LIMIT 20;
```

### Step 4: Check Infrastructure

```sql
-- Check host metrics during incident
ESQL FROM metrics-* 
| WHERE @timestamp > NOW() - 30 MINUTES
| WHERE host.name IN ('host1', 'host2')
| STATS 
    avg_cpu = AVG(system.cpu.total.pct),
    avg_mem = AVG(system.memory.used.pct)
BY host.name, BUCKET(@timestamp, 1 MINUTE);
```

## Alerting Integration

```sql
-- Create an alert condition check
DECLARE error_count NUMBER;
ESQL FROM logs-* 
| WHERE @timestamp > NOW() - 5 MINUTES
| WHERE level = 'ERROR'
| STATS count = COUNT(*)
INTO error_count;

IF error_count > 100 THEN
    SLACK_SEND('#alerts', 'High error rate detected: ' || error_count || ' errors in 5 minutes');
END IF;
```

## Best Practices

1. **Always add time filters** - Observability data is time-series
2. **Start broad, then narrow** - Begin with aggregations, then drill down
3. **Use trace IDs** - Correlate logs, metrics, and traces with trace.id
4. **Check multiple signals** - Logs alone may not tell the full story
5. **Compare to baseline** - Is this normal or anomalous?
