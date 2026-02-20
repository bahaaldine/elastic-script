# Observability Skills

**Investigate incidents faster.** Pre-built skills for logs, metrics, APM, and SLOs.

---

## Quick Start

```bash
# 1. Install observability skills
./hub/moltler-cli.sh install --all --category observability
./hub/moltler-cli.sh install --all --category apm
./hub/moltler-cli.sh install --all --category metrics

# 2. Run your first skill
curl -u elastic-admin:elastic-password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "RUN SKILL get_recent_errors()"}'
```

---

## Common Scenarios

### "What errors happened in the last hour?"

```sql
RUN SKILL get_recent_errors();
```

Returns the latest ERROR logs with full context.

### "Why is the API slow?"

```sql
-- Find slow transactions
RUN SKILL get_slow_transactions(service => 'api-gateway', threshold_ms => 1000);

-- Get the trace
RUN SKILL get_trace(trace_id => 'abc123');

-- Analyze database queries
RUN SKILL analyze_database_queries(service => 'api-gateway');
```

### "What's the error rate trend?"

```sql
-- Count by level
RUN SKILL count_logs_by_level(index_pattern => 'logs-*');

-- Compare time periods
RUN SKILL compare_time_periods(
  index_pattern => 'logs-*',
  current_period => '1h',
  previous_period => '1h'
);
```

### "Show me the context around this error"

```sql
-- Get surrounding logs for a trace
RUN SKILL correlate_logs(trace_id => 'abc123');

-- Get error context with stack trace
RUN SKILL get_error_context(error_id => 'err-456');
```

### "What patterns are in my logs?"

```sql
RUN SKILL get_log_patterns(index_pattern => 'logs-*', limit => 20);
```

---

## Available Skills

### Log Analysis

| Skill | Description |
|-------|-------------|
| `get_recent_errors` | Get recent ERROR logs with details |
| `count_logs_by_level` | Count logs grouped by level |
| `get_log_patterns` | Identify common log patterns |
| `correlate_logs` | Find related logs by trace ID |
| `get_error_context` | Get context around an error |
| `compare_time_periods` | Compare metrics across time |

### APM & Tracing

| Skill | Description |
|-------|-------------|
| `list_services` | List all monitored services |
| `get_slow_transactions` | Find transactions over threshold |
| `get_trace` | Get full distributed trace |
| `get_failed_transactions` | Find failed transactions |
| `get_throughput` | Get transaction throughput |
| `get_service_map` | Visualize service dependencies |
| `analyze_database_queries` | Find slow DB queries |

### Metrics & Infrastructure

| Skill | Description |
|-------|-------------|
| `get_host_metrics` | CPU, memory, disk for a host |
| `list_hosts` | List all monitored hosts |
| `get_memory_pressure` | Find hosts with memory issues |

### SLOs & Synthetics

| Skill | Description |
|-------|-------------|
| `get_slo_status` | Check SLO burn rate |
| `list_monitors` | List synthetic monitors |
| `get_monitor_status` | Get monitor health |
| `get_availability` | Calculate uptime percentage |

---

## Real-World Workflow

**Scenario: Production incident - API returning 500 errors**

```sql
-- Step 1: How bad is it?
RUN SKILL count_logs_by_level(
  index_pattern => 'logs-api-*',
  time_range => '15m'
);
-- Result: ERROR: 234, WARN: 89, INFO: 12,456

-- Step 2: What are the errors?
RUN SKILL get_recent_errors(
  service => 'api-gateway',
  limit => 10
);
-- Result: "Database connection timeout" repeated

-- Step 3: Is this affecting all endpoints?
RUN SKILL get_failed_transactions(
  service => 'api-gateway',
  limit => 20
);
-- Result: /api/orders endpoint has 90% failure rate

-- Step 4: What's the database doing?
RUN SKILL analyze_database_queries(
  service => 'api-gateway',
  threshold_ms => 5000
);
-- Result: SELECT * FROM orders - avg 8.2s (normally 50ms)

-- Step 5: When did this start?
RUN SKILL compare_time_periods(
  index_pattern => 'logs-api-*',
  current_period => '1h',
  previous_period => '1h'
);
-- Result: Error rate increased 400% starting 45 minutes ago
```

**Resolution time: 5 minutes instead of 30.**

---

## Combine with AI

Connect to an AI assistant that can run these skills:

```
User: "Why is checkout failing?"

AI: Let me investigate...
    [Runs get_recent_errors(service => 'checkout')]
    [Runs get_slow_transactions(service => 'checkout')]
    [Runs get_trace(trace_id => '...')]

AI: "Checkout is failing because the payment service is timing out.
     The database query for fraud detection is taking 12 seconds.
     This started 2 hours ago when a new index was deployed."
```

See [MCP Integration](../mcp.md) to connect your AI assistant.

---

## Build Custom Skills

Have a specific investigation pattern? Create a skill:

```sql
CREATE SKILL investigate_service
  VERSION '1.0.0'
  DESCRIPTION 'Full investigation of a service - errors, latency, throughput'
  (service STRING DESCRIPTION 'Service name to investigate')
  RETURNS DOCUMENT
BEGIN
  DECLARE errors ARRAY;
  DECLARE slow_txns ARRAY;
  DECLARE error_count INT;

  SET errors = ESQL_QUERY('FROM logs-* | WHERE service == "' || service || '" AND level == "ERROR" | LIMIT 10');
  SET slow_txns = ESQL_QUERY('FROM traces-* | WHERE service.name == "' || service || '" AND duration > 1000 | LIMIT 10');
  SET error_count = ARRAY_LENGTH(errors);

  RETURN {
    'service': service,
    'error_count': error_count,
    'recent_errors': errors,
    'slow_transactions': slow_txns,
    'status': CASE WHEN error_count > 10 THEN 'critical' WHEN error_count > 0 THEN 'warning' ELSE 'healthy' END
  };
END SKILL;
```

---

## Next Steps

- [Browse all O11y skills](../moltlerhub/index.md)
- [Create custom skills](../skills/creating-skills.md)
- [Connect AI assistant](../mcp.md)
