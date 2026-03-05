# PromQL in ES|QL

ES|QL natively supports PromQL syntax for querying Prometheus-style metrics, enabling familiar metric analysis patterns.

## Rate Functions

### rate()

Calculates per-second average rate of increase over a time range. Use for counters.

```sql
ESQL FROM metrics-*
| EVAL request_rate = rate(http_requests_total[5m])
| STATS avg_rate = avg(request_rate) BY service.name;
```

### irate()

Calculates instant rate using the last two data points. More responsive to changes.

```sql
ESQL FROM metrics-*
| EVAL instant_rate = irate(http_requests_total[5m])
| STATS BY service.name;
```

### increase()

Calculates total increase over a time range. Returns absolute increase, not rate.

```sql
ESQL FROM metrics-*
| EVAL total_requests = increase(http_requests_total[1h])
| STATS BY service.name;
```

---

## Histogram Functions

### histogram_quantile()

Calculates quantiles from histogram buckets.

```sql
-- P99 latency
ESQL FROM metrics-*
| EVAL p99 = histogram_quantile(0.99, rate(http_request_duration_seconds_bucket[5m]))
| STATS avg_p99 = avg(p99) BY service.name;

-- Multiple percentiles
ESQL FROM metrics-*
| EVAL 
    p50 = histogram_quantile(0.50, rate(duration_bucket[5m])),
    p90 = histogram_quantile(0.90, rate(duration_bucket[5m])),
    p99 = histogram_quantile(0.99, rate(duration_bucket[5m]))
| STATS BY service.name;
```

---

## Aggregation Functions

| Function | Description | Example |
|----------|-------------|---------|
| `sum()` | Sum values | `sum(rate(requests[5m])) BY service` |
| `avg()` | Average | `avg(rate(cpu_seconds[1m])) BY instance` |
| `max()` | Maximum | `max(memory_bytes) BY pod` |
| `min()` | Minimum | `min(temperature_celsius)` |
| `count()` | Count series | `count(up == 1) BY job` |
| `stddev()` | Standard deviation | `stddev(latency_seconds)` |
| `topk()` | Top K series | `topk(5, rate(requests[5m]))` |
| `bottomk()` | Bottom K series | `bottomk(5, rate(requests[5m]))` |

---

## Label Matchers

| Operator | Description | Example |
|----------|-------------|---------|
| `=` | Exact match | `{job="api-server"}` |
| `!=` | Not equal | `{status!="200"}` |
| `=~` | Regex match | `{service=~"api-.*"}` |
| `!~` | Regex not match | `{status!~"2.."}` |

```sql
ESQL FROM metrics-*
| WHERE job = "prometheus" AND instance =~ "prod-.*"
| EVAL rate = rate(requests_total[5m]);
```

---

## Time Range Selectors

| Duration | Syntax |
|----------|--------|
| 30 seconds | `[30s]` |
| 5 minutes | `[5m]` |
| 1 hour | `[1h]` |
| 1 day | `[1d]` |
| 1 week | `[1w]` |

---

## Metric Types

### Counters

Always increasing values (requests, bytes transferred). Use `rate()` or `increase()`.

```sql
-- Correct: use rate() for counters
| EVAL qps = rate(http_requests_total[5m])

-- Wrong: don't use counters directly
| EVAL qps = http_requests_total  -- This shows cumulative, not rate
```

### Gauges

Values that can go up or down (temperature, memory usage). Use directly.

```sql
| STATS avg_memory = avg(node_memory_MemAvailable_bytes) BY instance
```

### Histograms

Distribution of values in buckets. Use `histogram_quantile()`.

```sql
| EVAL p99 = histogram_quantile(0.99, rate(http_request_duration_seconds_bucket[5m]))
```

---

## Common Patterns

### RED Method (Rate, Errors, Duration)

For monitoring request-driven services:

```sql
ESQL FROM metrics-*
| EVAL 
    request_rate = rate(http_requests_total[5m]),
    error_rate = rate(http_requests_total{status=~"5.."}[5m]) 
               / rate(http_requests_total[5m]) * 100,
    p99_latency = histogram_quantile(0.99, rate(http_request_duration_bucket[5m]))
| STATS 
    rate = sum(request_rate),
    errors = avg(error_rate),
    latency_p99 = avg(p99_latency)
BY service.name;
```

### USE Method (Utilization, Saturation, Errors)

For monitoring resources:

```sql
ESQL FROM metrics-*
| EVAL 
    cpu_utilization = 1 - rate(node_cpu_seconds_total{mode="idle"}[5m]) 
                        / rate(node_cpu_seconds_total[5m]),
    memory_utilization = 1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes),
    disk_io_utilization = rate(node_disk_io_time_seconds_total[5m])
| STATS avg_cpu = avg(cpu_utilization), avg_mem = avg(memory_utilization) BY instance;
```

### Service Latency Analysis

```sql
ESQL FROM metrics-*
| EVAL 
    p50 = histogram_quantile(0.50, rate(request_duration_bucket[5m])),
    p95 = histogram_quantile(0.95, rate(request_duration_bucket[5m])),
    p99 = histogram_quantile(0.99, rate(request_duration_bucket[5m]))
| STATS 
    median = avg(p50),
    p95_latency = avg(p95),
    p99_latency = avg(p99)
BY service.name, endpoint
| SORT p99_latency DESC;
```

### Error Budget

```sql
ESQL FROM metrics-*
| EVAL 
    success_rate = rate(http_requests_total{status=~"2.."}[1h]) 
                 / rate(http_requests_total[1h]),
    slo_target = 0.999,
    error_budget_remaining = (success_rate - slo_target) / (1 - slo_target) * 100
| STATS avg_budget = avg(error_budget_remaining) BY service.name;
```

---

## Best Practices

1. **Always use `rate()` for counters** - Raw counter values are cumulative
2. **Match range to scrape interval** - Use `[5m]` for 15s scrape intervals (covers 4+ samples)
3. **Use `irate()` sparingly** - More volatile, use for dashboards not alerts
4. **Filter before aggregating** - Reduces data processed
5. **Use recording rules for expensive queries** - Pre-compute common aggregations

---

## See Also

- [Analyzing Observability](../skills/analyzing-observability/) - Logs, traces, APM
- [Searching Data](../skills/searching-data/) - General ES|QL queries
