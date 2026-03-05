---
name: analyzing-observability
description: >
  Analyze logs, metrics, traces, and APM data. Use when troubleshooting issues,
  finding errors, analyzing performance, investigating incidents, or checking
  error rates and latency.
---

# Observability

## Log Analysis

```sql
-- Recent errors
ESQL FROM logs-* 
| WHERE @timestamp > NOW() - 1 HOUR 
| WHERE level = 'ERROR' OR log.level = 'error'
| SORT @timestamp DESC
| LIMIT 50;

-- Errors by service
ESQL FROM logs-* 
| WHERE @timestamp > NOW() - 24 HOURS AND level = 'ERROR'
| STATS error_count = COUNT(*) BY service.name
| SORT error_count DESC;

-- Top error messages
ESQL FROM logs-* 
| WHERE @timestamp > NOW() - 1 HOUR AND level = 'ERROR'
| STATS count = COUNT(*) BY message
| SORT count DESC | LIMIT 20;
```

## Metrics

```sql
-- CPU by host
ESQL FROM metrics-* 
| WHERE @timestamp > NOW() - 1 HOUR AND metricset.name = 'cpu'
| STATS avg_cpu = AVG(system.cpu.total.pct) BY host.name
| SORT avg_cpu DESC;

-- High memory
ESQL FROM metrics-* 
| WHERE metricset.name = 'memory'
| STATS avg_mem = AVG(system.memory.used.pct) BY host.name
| WHERE avg_mem > 80;

-- Disk check
ESQL FROM metrics-* 
| WHERE metricset.name = 'filesystem'
| STATS used_pct = AVG(system.filesystem.used.pct) BY host.name, system.filesystem.mount_point
| WHERE used_pct > 85;
```

## APM & Traces

```sql
-- Slow transactions (>5s)
ESQL FROM traces-apm* 
| WHERE @timestamp > NOW() - 1 HOUR
| WHERE transaction.duration.us > 5000000
| SORT transaction.duration.us DESC | LIMIT 20;

-- Error rate by service
ESQL FROM traces-apm* 
| WHERE @timestamp > NOW() - 1 HOUR
| STATS total = COUNT(*), errors = SUM(CASE WHEN event.outcome = 'failure' THEN 1 ELSE 0 END) BY service.name
| EVAL error_rate = errors * 100.0 / total
| SORT error_rate DESC;
```

## Kibana APM Functions

| Function | Description |
|----------|-------------|
| `KIBANA_APM_SERVICES()` | List services |
| `KIBANA_APM_SERVICE_TRANSACTIONS(service)` | Transactions |
| `KIBANA_APM_SERVICE_ERRORS(service)` | Errors |
| `KIBANA_APM_TRACE(trace_id)` | Trace details |

## Pre-built Skills

| Skill | Description |
|-------|-------------|
| `RUN SKILL get_recent_errors(minutes, limit)` | Find errors |
| `RUN SKILL count_logs_by_level(index, hours)` | Level distribution |
| `RUN SKILL list_apm_services()` | List services |
| `RUN SKILL get_slow_transactions(threshold_ms)` | Slow transactions |

## Incident Workflow

1. **Identify:** Check error spike with time buckets
2. **Scope:** Find affected services
3. **Analyze:** Get error details and stack traces
4. **Correlate:** Check infrastructure metrics

## Alert on Errors

```sql
DECLARE error_count NUMBER;
ESQL FROM logs-* | WHERE @timestamp > NOW() - 5 MINUTES AND level = 'ERROR' | STATS count = COUNT(*) INTO error_count;

IF error_count > 100 THEN
    SLACK_SEND('#alerts', 'High error rate: ' || error_count || ' in 5 min');
END IF;
```
