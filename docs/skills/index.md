# Moltler Skills

Moltler Skills are pre-built capabilities that AI agents can use to interact with Elasticsearch. They expose Elasticsearch's full functionality through a natural language interface.

## Overview

**143 skills** currently available across 12 categories:

| Category | Skills | Description |
|----------|--------|-------------|
| [Meta](#meta-skills) | 8 | Discovery, recommendations, skill navigation |
| [Search](#search-skills) | 28 | Queries, aggregations, document operations |
| [Observability](#observability-skills) | 22 | Logs, SLOs, monitors, patterns |
| [APM](#apm-skills) | 11 | Services, traces, latency, dependencies |
| [Metrics](#metrics-skills) | 6 | Hosts, containers, resources |
| [Security](#security-skills) | 20 | Threats, hunting, cases, risk scores |
| [Machine Learning](#ml-skills) | 11 | Anomalies, inference, NLP |
| [Alerting](#alerting-skills) | 8 | Rules, alerts, connectors |
| [Cluster](#cluster-skills) | 10 | Health, nodes, shards, snapshots |
| [Integrations](#integration-skills) | 10 | Slack, Jira, PagerDuty, webhooks |
| [Fleet](#fleet-skills) | 6 | Agents, policies, integrations |
| [Agent Builder](#agent-builder-skills) | 13 | Kibana Agent Builder integration, A2A, MCP |

**Browse all skill source code:** [hub/skills/elastic](https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic)

---

## Quick Start

### Via MCP (AI Agents)

```bash
# List all available skills
curl -u elastic-admin:elastic-password http://localhost:9200/_escript/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/list"
  }'

# Call a skill
curl -u elastic-admin:elastic-password http://localhost:9200/_escript/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/call",
    "params": {
      "name": "get_recent_errors",
      "arguments": {"index_pattern": "logs-*", "limit": 10}
    }
  }'
```

### Via CLI

```bash
# Install all skills
./hub/moltler-cli.sh install --all

# Test skills
./hub/tests/test_all_skills.sh
```

---

## Meta Skills

Skills for discovering and navigating other skills. [:material-github: Source](https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/meta)

| Skill | Description | Source |
|-------|-------------|--------|
| `list_all_skills` | List all available skills with descriptions | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/meta/list-all-skills/skill.sql) |
| `explain_skill` | Get detailed explanation of a skill | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/meta/explain-skill/skill.sql) |
| `recommend_skills` | Get skill recommendations for a context | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/meta/recommend-skills/skill.sql) |
| `search_skills` | Search skills by keyword | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/meta/search-skills/skill.sql) |
| `list_skills_by_category` | List skills in a category | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/meta/list-skills-by-category/skill.sql) |
| `get_related_skills` | Find related skills | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/meta/get-related-skills/skill.sql) |
| `hello_moltler` | Test connectivity | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/meta/hello-moltler/skill.sql) |
| `check_cluster_health` | Verify Elasticsearch connection | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/meta/check-cluster-health/skill.sql) |

### Example: Discover Skills

```json
{
  "name": "search_skills",
  "arguments": {"query": "security threats"}
}
```

---

## Search Skills

Core Elasticsearch search and data operations. [:material-github: Source](https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search)

| Skill | Description | Source |
|-------|-------------|--------|
| `search_documents` | Full-text search across indices | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/search/search-documents/skill.sql) |
| `search_logs` | Search logs with text query | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/search/search-logs/skill.sql) |
| `count_documents` | Count documents matching criteria | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/search/count-documents/skill.sql) |
| `fuzzy_search` | Search with typo tolerance | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/search/fuzzy-search/skill.sql) |
| `semantic_search` | Vector/semantic similarity search | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/search/semantic-search/skill.sql) |
| `multi_field_search` | Search across multiple fields | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/search/multi-field-search/skill.sql) |
| `date_histogram` | Aggregate by time intervals | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/search/date-histogram/skill.sql) |
| `top_values` | Get top N values for a field | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/search/top-values/skill.sql) |
| `percentiles` | Calculate percentile distributions | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/search/percentiles/skill.sql) |
| `get_field_stats` | Get min/max/avg for fields | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/search/get-field-stats/skill.sql) |
| `get_unique_values` | Get unique values (cardinality) | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/search/get-unique-values/skill.sql) |
| `aggregate_by_field` | Group by field aggregation | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/search/aggregate-by-field/skill.sql) |
| `list_indices` | List available indices | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/search/list-indices/skill.sql) |
| `list_all_indices` | List all indices with stats | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/search/list-all-indices/skill.sql) |
| `get_index_stats` | Get detailed index statistics | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/search/get-index-stats/skill.sql) |
| `list_data_streams` | List data streams | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/search/list-data-streams/skill.sql) |
| `list_ilm_policies` | List ILM policies | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/search/list-ilm-policies/skill.sql) |
| `get_mapping` | Get field mappings | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/search/get-mapping/skill.sql) |
| `create_document` | Create a new document | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/search/create-document/skill.sql) |
| `update_document` | Update an existing document | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/search/update-document/skill.sql) |
| `delete_document` | Delete a document | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/search/delete-document/skill.sql) |
| `get_document` | Retrieve a document by ID | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/search/get-document/skill.sql) |
| `bulk_index` | Index multiple documents | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/search/bulk-index/skill.sql) |
| `reindex` | Copy documents between indices | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/search/reindex/skill.sql) |
| `create_index` | Create a new index | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/search/create-index/skill.sql) |
| `delete_index` | Delete an index | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/search/delete-index/skill.sql) |
| `set_alias` | Create/update index alias | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/search/set-alias/skill.sql) |
| `list_transforms` | List data transforms | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/search/list-transforms/skill.sql) |

### Example: Search with Aggregation

```json
{
  "name": "top_values",
  "arguments": {
    "index_pattern": "logs-*",
    "field": "level",
    "limit": 5
  }
}
```

---

## Observability Skills

Log management, analysis, and monitoring. [:material-github: Source](https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/observability)

| Skill | Description | Source |
|-------|-------------|--------|
| `get_recent_errors` | Get recent error logs | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/observability/get-recent-errors/skill.sql) |
| `count_logs_by_level` | Count logs by severity | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/observability/count-logs-by-level/skill.sql) |
| `logs_by_service` | Get logs for a service | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/observability/logs-by-service/skill.sql) |
| `error_rate` | Calculate error rate percentage | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/observability/error-rate/skill.sql) |
| `search_logs` | Search logs with query | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/observability/search-logs/skill.sql) |
| `get_log_patterns` | Identify common log patterns | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/observability/get-log-patterns/skill.sql) |
| `correlate_logs` | Find correlated events by trace ID | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/observability/correlate-logs/skill.sql) |
| `get_error_context` | Get logs before/after an error | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/observability/get-error-context/skill.sql) |
| `compare_time_periods` | Compare metrics between periods | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/observability/compare-time-periods/skill.sql) |
| `get_slo_status` | Get SLO status for a service | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/observability/get-slo-status/skill.sql) |
| `list_monitors` | List uptime monitors | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/observability/list-monitors/skill.sql) |
| `get_monitor_status` | Get monitor health | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/observability/get-monitor-status/skill.sql) |
| `get_availability` | Get availability percentage | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/observability/get-availability/skill.sql) |
| `get_ssl_status` | Check SSL certificate expiry | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/observability/get-ssl-status/skill.sql) |
| `slow_requests` | Find slow requests | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/observability/slow-requests/skill.sql) |
| `top_error_messages` | Get most common errors | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/observability/top-error-messages/skill.sql) |
| `get_metrics_summary` | Summary of key metrics | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/observability/get-metrics-summary/skill.sql) |
| `high_cpu_hosts` | Find hosts with high CPU | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/observability/high-cpu-hosts/skill.sql) |
| `service_health` | Get service health status | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/observability/service-health/skill.sql) |

### Example: SLO Monitoring

```json
{
  "name": "get_slo_status",
  "arguments": {
    "service": "api-gateway",
    "slo_target": 99.9
  }
}
```

---

## APM Skills

Application Performance Monitoring. [:material-github: Source](https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/apm)

| Skill | Description | Source |
|-------|-------------|--------|
| `list_services` | List monitored services | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/apm/list-services/skill.sql) |
| `get_service_health` | Get service health metrics | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/apm/get-service-health/skill.sql) |
| `get_slow_transactions` | Find slowest transactions | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/apm/get-slow-transactions/skill.sql) |
| `get_error_groups` | Errors grouped by type | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/apm/get-error-groups/skill.sql) |
| `get_service_dependencies` | Service dependency map | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/apm/get-service-dependencies/skill.sql) |
| `get_latency_percentiles` | p50/p95/p99 latencies | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/apm/get-latency-percentiles/skill.sql) |
| `get_trace` | Get distributed trace | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/apm/get-trace/skill.sql) |
| `get_service_map` | Full service topology | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/apm/get-service-map/skill.sql) |
| `get_throughput` | Request throughput over time | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/apm/get-throughput/skill.sql) |
| `get_failed_transactions` | Failed/errored transactions | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/apm/get-failed-transactions/skill.sql) |
| `analyze_database_queries` | Slow database query analysis | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/apm/analyze-database-queries/skill.sql) |

### Example: Latency Analysis

```json
{
  "name": "get_latency_percentiles",
  "arguments": {"service": "checkout-service"}
}
```

---

## Metrics Skills

Infrastructure metrics and monitoring. [:material-github: Source](https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/metrics)

| Skill | Description | Source |
|-------|-------------|--------|
| `list_hosts` | List monitored hosts | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/metrics/list-hosts/skill.sql) |
| `get_host_metrics` | CPU/memory/disk for a host | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/metrics/get-host-metrics/skill.sql) |
| `get_container_metrics` | Container resource metrics | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/metrics/get-container-metrics/skill.sql) |
| `get_disk_usage` | Disk usage by host | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/metrics/get-disk-usage/skill.sql) |
| `get_network_metrics` | Network throughput | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/metrics/get-network-metrics/skill.sql) |
| `get_memory_pressure` | Hosts with high memory | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/metrics/get-memory-pressure/skill.sql) |

---

## Security Skills

Security analysis and threat hunting. [:material-github: Source](https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/security)

| Skill | Description | Source |
|-------|-------------|--------|
| `suspicious_activity` | Recent suspicious events | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/security/suspicious-activity/skill.sql) |
| `threat_summary` | Threat landscape summary | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/security/threat-summary/skill.sql) |
| `user_activity` | User activity timeline | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/security/user-activity/skill.sql) |
| `list_detection_rules` | List SIEM rules | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/security/list-detection-rules/skill.sql) |
| `list_cases` | List security cases | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/security/list-cases/skill.sql) |
| `get_user_risk_score` | User risk assessment | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/security/get-user-risk-score/skill.sql) |
| `get_host_risk_score` | Host risk assessment | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/security/get-host-risk-score/skill.sql) |
| `hunt_ioc` | Hunt for IOC (IP/hash/domain) | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/security/hunt-ioc/skill.sql) |
| `get_authentication_summary` | Login success/failure stats | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/security/get-authentication-summary/skill.sql) |
| `search_security_events` | Search security events | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/security/search-security-events/skill.sql) |
| `get_process_events` | Process execution events | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/security/get-process-events/skill.sql) |
| `get_network_events` | Network connection events | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/security/get-network-events/skill.sql) |
| `get_file_events` | File system events | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/security/get-file-events/skill.sql) |
| `get_dns_queries` | DNS query events | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/security/get-dns-queries/skill.sql) |
| `get_risky_users` | Highest risk users | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/security/get-risky-users/skill.sql) |
| `get_risky_hosts` | Highest risk hosts | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/security/get-risky-hosts/skill.sql) |
| `create_case` | Create investigation case | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/security/create-case/skill.sql) |
| `get_security_alerts` | Active security alerts | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/security/get-security-alerts/skill.sql) |
| `failed_logins` | Failed login attempts | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/security/failed-logins/skill.sql) |

### Example: Threat Hunting

```json
{
  "name": "hunt_ioc",
  "arguments": {
    "ioc": "192.168.1.100",
    "ioc_type": "ip"
  }
}
```

---

## ML Skills

Machine Learning and AI capabilities. [:material-github: Source](https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/ml)

| Skill | Description | Source |
|-------|-------------|--------|
| `list_ml_jobs` | List ML anomaly jobs | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/ml/list-ml-jobs/skill.sql) |
| `get_anomalies` | Get detected anomalies | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/ml/get-anomalies/skill.sql) |
| `list_trained_models` | List trained models | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/ml/list-trained-models/skill.sql) |
| `run_inference` | Run model inference | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/ml/run-inference/skill.sql) |
| `embed_text` | Generate text embeddings | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/ml/embed-text/skill.sql) |
| `detect_anomalies_realtime` | Real-time anomaly detection | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/ml/detect-anomalies-realtime/skill.sql) |
| `get_job_status` | ML job status | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/ml/get-job-status/skill.sql) |
| `get_influencers` | Top anomaly influencers | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/ml/get-influencers/skill.sql) |
| `explain_anomaly` | Anomaly explanation | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/ml/explain-anomaly/skill.sql) |
| `classify_text` | Text classification | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/ml/classify-text/skill.sql) |
| `extract_entities` | Named entity recognition | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/ml/extract-entities/skill.sql) |

---

## Alerting Skills

Alert management and automation. [:material-github: Source](https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/alerting)

| Skill | Description | Source |
|-------|-------------|--------|
| `list_alert_rules` | List alerting rules | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/alerting/list-alert-rules/skill.sql) |
| `get_active_alerts` | Currently firing alerts | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/alerting/get-active-alerts/skill.sql) |
| `get_alert_history` | Historical alerts | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/alerting/get-alert-history/skill.sql) |
| `acknowledge_alert` | Acknowledge an alert | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/alerting/acknowledge-alert/skill.sql) |
| `list_connectors` | List alert connectors | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/alerting/list-connectors/skill.sql) |
| `create_threshold_rule` | Create threshold alert | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/alerting/create-threshold-rule/skill.sql) |
| `mute_alert` | Temporarily mute alert | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/alerting/mute-alert/skill.sql) |
| `test_connector` | Test connector | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/alerting/test-connector/skill.sql) |

---

## Cluster Skills

Elasticsearch cluster operations. [:material-github: Source](https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/cluster)

| Skill | Description | Source |
|-------|-------------|--------|
| `cluster_health` | Cluster health status | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/cluster/cluster-health/skill.sql) |
| `get_cluster_health` | Detailed cluster health | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/cluster/get-cluster-health/skill.sql) |
| `list_nodes` | List cluster nodes | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/cluster/list-nodes/skill.sql) |
| `get_node_stats` | Node statistics | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/cluster/get-node-stats/skill.sql) |
| `get_shard_allocation` | Shard distribution | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/cluster/get-shard-allocation/skill.sql) |
| `list_snapshots` | List backup snapshots | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/cluster/list-snapshots/skill.sql) |
| `get_pending_tasks` | Pending cluster tasks | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/cluster/get-pending-tasks/skill.sql) |
| `get_unassigned_shards` | Unassigned shards | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/cluster/get-unassigned-shards/skill.sql) |
| `explain_allocation` | Shard allocation reason | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/cluster/explain-allocation/skill.sql) |
| `list_running_tasks` | Running tasks | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/cluster/list-running-tasks/skill.sql) |

---

## Integration Skills

External service integrations. [:material-github: Source](https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/integrations)

| Skill | Description | Source |
|-------|-------------|--------|
| `send_slack_message` | Send Slack message | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/integrations/send-slack-message/skill.sql) |
| `create_jira_issue` | Create Jira ticket | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/integrations/create-jira-issue/skill.sql) |
| `trigger_pagerduty` | Trigger PagerDuty incident | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/integrations/trigger-pagerduty/skill.sql) |
| `send_email` | Send email notification | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/integrations/send-email/skill.sql) |
| `send_webhook` | Send webhook POST | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/integrations/send-webhook/skill.sql) |
| `send_teams_message` | Microsoft Teams message | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/integrations/send-teams-message/skill.sql) |
| `send_opsgenie_alert` | OpsGenie alert | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/integrations/send-opsgenie-alert/skill.sql) |
| `create_servicenow_incident` | ServiceNow incident | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/integrations/create-servicenow-incident/skill.sql) |
| `trigger_github_workflow` | GitHub Actions workflow | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/integrations/trigger-github-workflow/skill.sql) |
| `invoke_aws_lambda` | Invoke AWS Lambda | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/integrations/invoke-aws-lambda/skill.sql) |

---

## Fleet Skills

Elastic Agent management. [:material-github: Source](https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/fleet)

| Skill | Description | Source |
|-------|-------------|--------|
| `list_agents` | List enrolled agents | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/fleet/list-agents/skill.sql) |
| `get_agent_status` | Agent health status | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/fleet/get-agent-status/skill.sql) |
| `list_agent_policies` | Agent policies | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/fleet/list-agent-policies/skill.sql) |
| `get_agent_logs` | Agent log output | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/fleet/get-agent-logs/skill.sql) |
| `list_integrations` | Available integrations | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/fleet/list-integrations/skill.sql) |
| `get_enrollment_tokens` | Enrollment tokens | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/fleet/get-enrollment-tokens/skill.sql) |

---

## Enterprise Search Skills

Search applications and analytics. [:material-github: Source](https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/enterprise-search)

| Skill | Description | Source |
|-------|-------------|--------|
| `list_search_apps` | List search applications | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/enterprise-search/list-search-apps/skill.sql) |
| `get_search_analytics` | Search analytics | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/enterprise-search/get-search-analytics/skill.sql) |
| `get_top_queries` | Most popular queries | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/enterprise-search/get-top-queries/skill.sql) |
| `get_no_results_queries` | Queries with no results | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/enterprise-search/get-no-results-queries/skill.sql) |

---

## Agent Builder Skills

Kibana Agent Builder integration for orchestrating AI agents. [:material-github: Source](https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/agent-builder)

!!! note "Requires Kibana 8.18+ with Agent Builder"
    These skills require the Kibana Agent Builder feature (Enterprise subscription).

| Skill | Description | Source |
|-------|-------------|--------|
| `ab_list_agents` | List all Agent Builder agents | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/agent-builder/list-agents/skill.sql) |
| `ab_get_agent` | Get agent details by ID | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/agent-builder/get-agent/skill.sql) |
| `ab_create_agent` | Create a new agent | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/agent-builder/create-agent/skill.sql) |
| `ab_delete_agent` | Delete an agent | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/agent-builder/delete-agent/skill.sql) |
| `ab_chat` | Send chat message to an agent | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/agent-builder/chat/skill.sql) |
| `ab_list_conversations` | List all conversations | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/agent-builder/list-conversations/skill.sql) |
| `ab_get_conversation` | Get conversation history | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/agent-builder/get-conversation/skill.sql) |
| `ab_list_tools` | List available tools | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/agent-builder/list-tools/skill.sql) |
| `ab_create_tool` | Create a new tool | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/agent-builder/create-tool/skill.sql) |
| `ab_execute_tool` | Execute a tool directly | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/agent-builder/execute-tool/skill.sql) |
| `ab_get_agent_card` | Get A2A agent card | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/agent-builder/get-agent-card/skill.sql) |
| `ab_send_a2a_task` | Send A2A task to agent | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/agent-builder/send-a2a-task/skill.sql) |
| `ab_mcp_call` | Call Agent Builder MCP endpoint | [:material-code-braces:](https://github.com/bahaaldine/moltler/blob/main/hub/skills/elastic/agent-builder/mcp-call/skill.sql) |

### Example: Chat with an Agent Builder Agent

```json
{
  "name": "ab_chat",
  "arguments": {
    "agent_id": "my-security-agent",
    "message": "What are the top security threats today?"
  }
}
```

### Example: Create an Agent Programmatically

```json
{
  "name": "ab_create_agent",
  "arguments": {
    "name": "Log Analyzer",
    "description": "Analyzes logs for errors and anomalies",
    "instructions": "You are a log analysis expert. Help users understand their logs.",
    "model": "gpt-4"
  }
}
```

---

## Creating Custom Skills

See the [Skills Roadmap](skills-roadmap.md) for planned skills and the [Skill Format Specification](https://github.com/bahaaldine/moltler/blob/main/hub/SKILL_FORMAT.md) for creating your own.

```sql
CREATE SKILL my_custom_skill
  VERSION '1.0.0'
  DESCRIPTION 'Description of what this skill does'
  AUTHOR 'your-name'
  TAGS ['category', 'tag2']
  (
    param1 STRING DESCRIPTION 'First parameter',
    param2 INT DESCRIPTION 'Second parameter' DEFAULT 10
  )
  RETURNS ARRAY
BEGIN
  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM logs-* | LIMIT ' || param2);
  RETURN result;
END SKILL;
```
