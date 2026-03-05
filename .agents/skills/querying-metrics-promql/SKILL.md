---
name: querying-metrics-promql
description: >
  Query metrics using PromQL syntax in ES|QL. Use when analyzing Prometheus-style
  metrics, calculating rates, histograms, quantiles, or when the user mentions
  PromQL, Prometheus, rate(), sum(), histogram_quantile(), or metric queries.
---

# PromQL in ES|QL

## Rate Functions

| Function | Description | Example |
|----------|-------------|---------|
| `rate()` | Per-second rate over range | `rate(http_requests_total[5m])` |
| `irate()` | Instant rate (last 2 points) | `irate(http_requests_total[5m])` |
| `increase()` | Total increase over range | `increase(http_requests_total[1h])` |

```sql
-- Request rate per second
ESQL FROM metrics-*
| EVAL request_rate = rate(http_requests_total[5m])
| STATS avg_rate = avg(request_rate) BY service.name
| SORT avg_rate DESC;

-- Error rate percentage
ESQL FROM metrics-*
| EVAL error_rate = rate(http_requests_total{status=~"5.."}[5m]) 
                  / rate(http_requests_total[5m]) * 100
| STATS BY service.name;
```

## Histogram Percentiles

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

## Aggregations

| Function | Example |
|----------|---------|
| `sum()` | `sum(rate(requests[5m])) BY service` |
| `avg()` | `avg(rate(cpu_seconds[1m])) BY instance` |
| `max()` / `min()` | `max(memory_bytes) BY pod` |
| `count()` | `count(up == 1) BY job` |
| `topk()` | `topk(5, rate(requests[5m]))` |
| `bottomk()` | `bottomk(5, rate(requests[5m]))` |

```sql
-- Sum across instances
ESQL FROM metrics-*
| EVAL req_rate = rate(http_requests_total[5m])
| STATS total = sum(req_rate) BY service.name;

-- Top 5 by request rate
ESQL FROM metrics-*
| EVAL req_rate = rate(http_requests_total[5m])
| STATS rate = sum(req_rate) BY service.name
| SORT rate DESC | LIMIT 5;
```

## Label Matching

| Operator | Meaning | Example |
|----------|---------|---------|
| `=` | Exact match | `job="api"` |
| `!=` | Not equal | `status!="200"` |
| `=~` | Regex match | `service=~"api-.*"` |
| `!~` | Regex not match | `status!~"2.."` |

```sql
ESQL FROM metrics-*
| WHERE job = "prometheus" AND instance =~ "prod-.*"
| EVAL rate = rate(requests_total[5m]);
```

## Time Ranges

| Suffix | Duration |
|--------|----------|
| `[30s]` | 30 seconds |
| `[5m]` | 5 minutes |
| `[1h]` | 1 hour |
| `[1d]` | 1 day |
| `[1w]` | 1 week |

## RED Metrics Pattern

Rate, Errors, Duration for services:

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
    latency = avg(p99_latency)
BY service.name;
```

## USE Metrics Pattern

Utilization, Saturation, Errors for resources:

```sql
ESQL FROM metrics-*
| EVAL 
    cpu_util = 1 - rate(node_cpu_seconds_total{mode="idle"}[5m]) 
                 / rate(node_cpu_seconds_total[5m]),
    mem_util = 1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes),
    disk_util = rate(node_disk_io_time_seconds_total[5m])
| STATS BY instance;
```

## Counter vs Gauge

**Counters** (always increasing): Use `rate()` or `increase()`
```sql
| EVAL qps = rate(http_requests_total[5m])
```

**Gauges** (can go up/down): Use directly or with `avg()`
```sql
| STATS avg_temp = avg(temperature_celsius) BY location
```

## Pre-built Skills

| Skill | Description |
|-------|-------------|
| `RUN SKILL request_rate(service, window)` | Request rate |
| `RUN SKILL error_rate(service, window)` | Error percentage |
| `RUN SKILL latency_percentiles(service)` | P50/P90/P99 |
| `RUN SKILL red_metrics(service)` | Full RED metrics |
| `RUN SKILL use_metrics(instance)` | Full USE metrics |
