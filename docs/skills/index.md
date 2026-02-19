# Moltler Skills

Moltler Skills are pre-built capabilities that AI agents can use to interact with Elasticsearch. They expose Elasticsearch's full functionality through a natural language interface.

## Overview

**130 skills** currently available across 10 categories:

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

Skills for discovering and navigating other skills.

| Skill | Description |
|-------|-------------|
| `list_all_skills` | List all available skills with descriptions |
| `explain_skill` | Get detailed explanation of a skill |
| `recommend_skills` | Get skill recommendations for a context |
| `search_skills` | Search skills by keyword |
| `list_skills_by_category` | List skills in a category |
| `get_related_skills` | Find related skills |
| `hello_moltler` | Test connectivity |
| `check_cluster_health` | Verify Elasticsearch connection |

### Example: Discover Skills

```json
{
  "name": "search_skills",
  "arguments": {"query": "security threats"}
}
```

---

## Search Skills

Core Elasticsearch search and data operations.

| Skill | Description |
|-------|-------------|
| `search_documents` | Full-text search across indices |
| `search_logs` | Search logs with text query |
| `count_documents` | Count documents matching criteria |
| `fuzzy_search` | Search with typo tolerance |
| `semantic_search` | Vector/semantic similarity search |
| `multi_field_search` | Search across multiple fields |
| `date_histogram` | Aggregate by time intervals |
| `top_values` | Get top N values for a field |
| `percentiles` | Calculate percentile distributions |
| `get_field_stats` | Get min/max/avg for fields |
| `get_unique_values` | Get unique values (cardinality) |
| `aggregate_by_field` | Group by field aggregation |
| `list_indices` | List available indices |
| `list_all_indices` | List all indices with stats |
| `get_index_stats` | Get detailed index statistics |
| `list_data_streams` | List data streams |
| `list_ilm_policies` | List ILM policies |
| `get_mapping` | Get field mappings |
| `create_document` | Create a new document |
| `update_document` | Update an existing document |
| `delete_document` | Delete a document |
| `get_document` | Retrieve a document by ID |
| `bulk_index` | Index multiple documents |
| `reindex` | Copy documents between indices |
| `create_index` | Create a new index |
| `delete_index` | Delete an index |
| `set_alias` | Create/update index alias |
| `list_transforms` | List data transforms |
| `get_transform_status` | Get transform status |
| `list_ingest_pipelines` | List ingest pipelines |
| `test_ingest_pipeline` | Test an ingest pipeline |

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

Log management, analysis, and monitoring.

| Skill | Description |
|-------|-------------|
| `get_recent_errors` | Get recent error logs |
| `count_logs_by_level` | Count logs by severity |
| `logs_by_service` | Get logs for a service |
| `error_rate` | Calculate error rate percentage |
| `search_logs` | Search logs with query |
| `get_log_patterns` | Identify common log patterns |
| `correlate_logs` | Find correlated events by trace ID |
| `get_error_context` | Get logs before/after an error |
| `compare_time_periods` | Compare metrics between periods |
| `get_slo_status` | Get SLO status for a service |
| `list_monitors` | List uptime monitors |
| `get_monitor_status` | Get monitor health |
| `get_availability` | Get availability percentage |
| `get_ssl_status` | Check SSL certificate expiry |
| `slow_requests` | Find slow requests |
| `top_error_messages` | Get most common errors |
| `get_metrics_summary` | Summary of key metrics |
| `high_cpu_hosts` | Find hosts with high CPU |
| `metrics_summary` | Aggregated metrics view |
| `recent_documents` | Get recent documents |
| `get_sample_documents` | Sample documents from index |
| `service_health` | Get service health status |

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

Application Performance Monitoring.

| Skill | Description |
|-------|-------------|
| `list_services` | List monitored services |
| `get_service_health` | Get service health metrics |
| `get_slow_transactions` | Find slowest transactions |
| `get_error_groups` | Errors grouped by type |
| `get_service_dependencies` | Service dependency map |
| `get_latency_percentiles` | p50/p95/p99 latencies |
| `get_trace` | Get distributed trace |
| `get_service_map` | Full service topology |
| `get_throughput` | Request throughput over time |
| `get_failed_transactions` | Failed/errored transactions |
| `analyze_database_queries` | Slow database query analysis |

### Example: Latency Analysis

```json
{
  "name": "get_latency_percentiles",
  "arguments": {"service": "checkout-service"}
}
```

---

## Metrics Skills

Infrastructure metrics and monitoring.

| Skill | Description |
|-------|-------------|
| `list_hosts` | List monitored hosts |
| `get_host_metrics` | CPU/memory/disk for a host |
| `get_container_metrics` | Container resource metrics |
| `get_disk_usage` | Disk usage by host |
| `get_network_metrics` | Network throughput |
| `get_memory_pressure` | Hosts with high memory |

---

## Security Skills

Security analysis and threat hunting.

| Skill | Description |
|-------|-------------|
| `suspicious_activity` | Recent suspicious events |
| `threat_summary` | Threat landscape summary |
| `user_activity` | User activity timeline |
| `list_detection_rules` | List SIEM rules |
| `list_cases` | List security cases |
| `get_user_risk_score` | User risk assessment |
| `get_host_risk_score` | Host risk assessment |
| `hunt_ioc` | Hunt for IOC (IP/hash/domain) |
| `get_authentication_summary` | Login success/failure stats |
| `search_security_events` | Search security events |
| `get_process_events` | Process execution events |
| `get_network_events` | Network connection events |
| `get_file_events` | File system events |
| `get_dns_queries` | DNS query events |
| `get_risky_users` | Highest risk users |
| `get_risky_hosts` | Highest risk hosts |
| `create_case` | Create investigation case |
| `get_security_alerts` | Active security alerts |
| `failed_logins` | Failed login attempts |

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

Machine Learning and AI capabilities.

| Skill | Description |
|-------|-------------|
| `list_ml_jobs` | List ML anomaly jobs |
| `get_anomalies` | Get detected anomalies |
| `list_trained_models` | List trained models |
| `run_inference` | Run model inference |
| `embed_text` | Generate text embeddings |
| `detect_anomalies_realtime` | Real-time anomaly detection |
| `get_job_status` | ML job status |
| `get_influencers` | Top anomaly influencers |
| `explain_anomaly` | Anomaly explanation |
| `classify_text` | Text classification |
| `extract_entities` | Named entity recognition |

---

## Alerting Skills

Alert management and automation.

| Skill | Description |
|-------|-------------|
| `list_alert_rules` | List alerting rules |
| `get_active_alerts` | Currently firing alerts |
| `get_alert_history` | Historical alerts |
| `acknowledge_alert` | Acknowledge an alert |
| `list_connectors` | List alert connectors |
| `create_threshold_rule` | Create threshold alert |
| `mute_alert` | Temporarily mute alert |
| `test_connector` | Test connector |

---

## Cluster Skills

Elasticsearch cluster operations.

| Skill | Description |
|-------|-------------|
| `cluster_health` | Cluster health status |
| `get_cluster_health` | Detailed cluster health |
| `list_nodes` | List cluster nodes |
| `get_node_stats` | Node statistics |
| `get_shard_allocation` | Shard distribution |
| `list_snapshots` | List backup snapshots |
| `get_pending_tasks` | Pending cluster tasks |
| `get_unassigned_shards` | Unassigned shards |
| `explain_allocation` | Shard allocation reason |
| `list_running_tasks` | Running tasks |
| `get_hot_threads` | Hot threads for debugging |

---

## Integration Skills

External service integrations.

| Skill | Description |
|-------|-------------|
| `send_slack_message` | Send Slack message |
| `create_jira_issue` | Create Jira ticket |
| `trigger_pagerduty` | Trigger PagerDuty incident |
| `send_email` | Send email notification |
| `send_webhook` | Send webhook POST |
| `send_teams_message` | Microsoft Teams message |
| `send_opsgenie_alert` | OpsGenie alert |
| `create_servicenow_incident` | ServiceNow incident |
| `trigger_github_workflow` | GitHub Actions workflow |
| `invoke_aws_lambda` | Invoke AWS Lambda |

---

## Fleet Skills

Elastic Agent management.

| Skill | Description |
|-------|-------------|
| `list_agents` | List enrolled agents |
| `get_agent_status` | Agent health status |
| `list_agent_policies` | Agent policies |
| `get_agent_logs` | Agent log output |
| `list_integrations` | Available integrations |
| `get_enrollment_tokens` | Enrollment tokens |

---

## Enterprise Search Skills

Search applications and analytics.

| Skill | Description |
|-------|-------------|
| `list_search_apps` | List search applications |
| `get_search_analytics` | Search analytics |
| `get_top_queries` | Most popular queries |
| `get_no_results_queries` | Queries with no results |

---

## Creating Custom Skills

See the [Skills Roadmap](skills-roadmap.md) for planned skills and [SKILL_FORMAT.md](../../hub/SKILL_FORMAT.md) for creating your own.

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
