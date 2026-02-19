# Moltler Skills Roadmap

A comprehensive catalog of skills exposing all Elasticsearch capabilities to AI agents.

---

## Overview

This roadmap defines **500+ skills** organized across Elastic's product areas:

| Category | Skills | Priority |
|----------|--------|----------|
| [Meta-Skills](#meta-skills) | 15 | P0 |
| [Search & Data](#search--data) | 45 | P0 |
| [Observability - Logs](#observability---logs) | 35 | P0 |
| [Observability - APM](#observability---apm) | 40 | P1 |
| [Observability - Metrics](#observability---metrics) | 30 | P1 |
| [Observability - Uptime](#observability---uptime) | 20 | P1 |
| [Observability - Profiling](#observability---profiling) | 15 | P2 |
| [Security - SIEM](#security---siem) | 50 | P0 |
| [Security - Endpoint](#security---endpoint) | 35 | P1 |
| [Security - Cloud Security](#security---cloud-security) | 25 | P2 |
| [Machine Learning](#machine-learning) | 40 | P1 |
| [Alerting & Actions](#alerting--actions) | 25 | P0 |
| [Index & Data Management](#index--data-management) | 35 | P1 |
| [Cluster Operations](#cluster-operations) | 30 | P1 |
| [Fleet & Agent](#fleet--agent) | 20 | P2 |
| [Enterprise Search](#enterprise-search) | 25 | P2 |
| [Integrations](#integrations) | 30 | P2 |

**Total: ~515 skills**

---

## Meta-Skills

Skills that help AI agents discover, understand, and compose other skills.

### Discovery & Navigation

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_all_skills` | List all available skills with descriptions | P0 |
| `search_skills` | Search skills by keyword or capability | P0 |
| `get_skill_details` | Get full documentation for a specific skill | P0 |
| `list_skills_by_category` | List skills in a specific category | P0 |
| `list_skills_by_use_case` | Find skills for a specific use case (e.g., "investigate incident") | P0 |

### Recommendations & Guidance

| Skill | Description | Priority |
|-------|-------------|----------|
| `recommend_skills` | Recommend skills based on current context/data | P0 |
| `suggest_next_skill` | Given current results, suggest what to do next | P0 |
| `explain_skill` | Get a detailed explanation of what a skill does | P0 |
| `get_skill_examples` | Get usage examples for a skill | P1 |
| `compare_skills` | Compare two skills to understand differences | P1 |

### Composition & Workflows

| Skill | Description | Priority |
|-------|-------------|----------|
| `create_skill_workflow` | Chain multiple skills into a workflow | P1 |
| `get_workflow_templates` | Get pre-built workflow templates | P1 |
| `validate_skill_inputs` | Validate inputs before calling a skill | P1 |
| `get_skill_dependencies` | Show what other skills this skill depends on | P2 |
| `get_related_skills` | Find skills commonly used together | P1 |

---

## Search & Data

Core Elasticsearch search and data operations.

### Query & Search

| Skill | Description | Priority |
|-------|-------------|----------|
| `search_documents` | Full-text search across any index | P0 |
| `search_with_filters` | Search with field-level filters | P0 |
| `search_by_time_range` | Search within a time window | P0 |
| `fuzzy_search` | Search with typo tolerance | P0 |
| `phrase_search` | Search for exact phrases | P0 |
| `wildcard_search` | Search with wildcards | P0 |
| `regex_search` | Search with regular expressions | P1 |
| `semantic_search` | Vector/semantic similarity search | P0 |
| `hybrid_search` | Combined keyword + semantic search | P0 |
| `more_like_this` | Find similar documents | P1 |
| `search_as_you_type` | Autocomplete/typeahead search | P1 |

### Aggregations & Analytics

| Skill | Description | Priority |
|-------|-------------|----------|
| `count_documents` | Count documents matching criteria | P0 |
| `get_field_stats` | Get min/max/avg/sum for numeric fields | P0 |
| `get_unique_values` | Get unique values for a field (cardinality) | P0 |
| `histogram_by_field` | Create histogram buckets | P0 |
| `date_histogram` | Aggregate by time intervals | P0 |
| `top_values` | Get top N values for a field | P0 |
| `percentiles` | Calculate percentile distributions | P1 |
| `geo_bounds` | Get geographic bounds of data | P1 |
| `geo_centroid` | Calculate geographic center | P1 |
| `significant_terms` | Find statistically significant terms | P1 |
| `rare_terms` | Find rare/unusual values | P1 |

### Document Operations

| Skill | Description | Priority |
|-------|-------------|----------|
| `get_document` | Retrieve a document by ID | P0 |
| `create_document` | Create a new document | P0 |
| `update_document` | Update an existing document | P0 |
| `delete_document` | Delete a document | P0 |
| `bulk_index` | Index multiple documents | P0 |
| `bulk_update` | Update multiple documents | P1 |
| `bulk_delete` | Delete multiple documents | P1 |
| `reindex_documents` | Copy documents between indices | P1 |
| `update_by_query` | Update documents matching a query | P1 |
| `delete_by_query` | Delete documents matching a query | P1 |

### ES|QL Operations

| Skill | Description | Priority |
|-------|-------------|----------|
| `esql_query` | Execute any ES|QL query | P0 |
| `esql_stats` | Calculate statistics with ES|QL | P0 |
| `esql_enrich` | Enrich data with lookup tables | P1 |
| `esql_grok` | Parse unstructured text | P1 |
| `esql_dissect` | Extract fields from text | P1 |
| `esql_eval` | Calculate derived fields | P0 |
| `esql_sort_limit` | Sort and limit results | P0 |
| `esql_join` | Join multiple data sources | P2 |

---

## Observability - Logs

Log management, analysis, and troubleshooting.

### Log Search & Query

| Skill | Description | Priority |
|-------|-------------|----------|
| `search_logs` | Search logs with text query | P0 |
| `get_recent_logs` | Get most recent log entries | P0 |
| `get_logs_by_level` | Get logs filtered by level (ERROR, WARN, etc.) | P0 |
| `get_logs_by_service` | Get logs for a specific service | P0 |
| `get_logs_by_host` | Get logs from a specific host | P0 |
| `get_logs_by_trace_id` | Get all logs for a trace | P0 |
| `get_logs_by_user` | Get logs related to a user | P0 |
| `get_logs_in_time_range` | Get logs within a time window | P0 |
| `search_logs_regex` | Search logs with regex pattern | P1 |

### Log Analysis

| Skill | Description | Priority |
|-------|-------------|----------|
| `count_logs_by_level` | Count logs grouped by severity | P0 |
| `count_logs_by_service` | Count logs grouped by service | P0 |
| `count_logs_by_host` | Count logs grouped by host | P0 |
| `get_error_rate` | Calculate error rate over time | P0 |
| `get_log_volume` | Get log volume over time | P0 |
| `get_log_patterns` | Identify common log patterns | P1 |
| `detect_log_anomalies` | Find unusual log patterns | P1 |
| `get_error_breakdown` | Break down errors by type/code | P0 |
| `correlate_logs` | Find correlated log events | P1 |
| `get_log_trends` | Show log trends over time | P1 |

### Log Investigation

| Skill | Description | Priority |
|-------|-------------|----------|
| `get_error_context` | Get logs before/after an error | P0 |
| `trace_request_flow` | Follow a request across services | P0 |
| `find_root_cause` | Analyze logs to find root cause | P1 |
| `get_first_occurrence` | Find when an error first appeared | P0 |
| `get_error_frequency` | How often does this error occur? | P0 |
| `compare_time_periods` | Compare logs between two periods | P1 |
| `get_affected_users` | Find users affected by an error | P0 |
| `get_affected_endpoints` | Find endpoints with errors | P0 |
| `summarize_errors` | AI summary of error patterns | P1 |

### Log Management

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_log_indices` | List all log indices | P0 |
| `get_log_index_stats` | Get statistics for log indices | P0 |
| `get_log_retention` | Check log retention policies | P1 |
| `archive_logs` | Archive old logs to cold storage | P2 |
| `delete_old_logs` | Delete logs older than X days | P2 |
| `create_log_view` | Create a saved log view | P2 |

---

## Observability - APM

Application Performance Monitoring.

### Service Overview

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_services` | List all monitored services | P0 |
| `get_service_health` | Get health status of a service | P0 |
| `get_service_dependencies` | Show service dependency map | P0 |
| `get_service_metrics` | Get key metrics for a service | P0 |
| `compare_services` | Compare metrics across services | P1 |
| `get_service_endpoints` | List endpoints for a service | P0 |

### Transaction Analysis

| Skill | Description | Priority |
|-------|-------------|----------|
| `get_transactions` | Get transactions for a service | P0 |
| `get_slow_transactions` | Find slowest transactions | P0 |
| `get_failed_transactions` | Get failed transactions | P0 |
| `get_transaction_details` | Get full details of a transaction | P0 |
| `get_transaction_trace` | Get distributed trace for a transaction | P0 |
| `analyze_transaction_latency` | Break down latency by component | P0 |
| `get_transaction_throughput` | Get transaction rate over time | P0 |
| `compare_transaction_versions` | Compare performance across versions | P1 |

### Error Analysis

| Skill | Description | Priority |
|-------|-------------|----------|
| `get_apm_errors` | Get APM error events | P0 |
| `get_error_groups` | Get errors grouped by type | P0 |
| `get_error_rate_by_service` | Error rate per service | P0 |
| `get_exception_details` | Get stack trace and context | P0 |
| `get_error_impact` | Assess user/business impact of error | P1 |
| `correlate_errors_with_deploys` | Link errors to deployments | P1 |

### Performance Analysis

| Skill | Description | Priority |
|-------|-------------|----------|
| `get_latency_distribution` | Get p50, p95, p99 latencies | P0 |
| `get_latency_by_endpoint` | Latency breakdown by endpoint | P0 |
| `detect_latency_anomalies` | Find latency spikes | P0 |
| `get_throughput_trends` | Throughput over time | P0 |
| `get_apdex_score` | Calculate Apdex satisfaction score | P1 |
| `identify_bottlenecks` | Find performance bottlenecks | P1 |
| `get_external_service_latency` | Latency to external dependencies | P1 |
| `analyze_database_queries` | Analyze slow database queries | P1 |

### Spans & Traces

| Skill | Description | Priority |
|-------|-------------|----------|
| `get_trace` | Get full distributed trace | P0 |
| `search_traces` | Search traces by attributes | P0 |
| `get_trace_breakdown` | Break down trace by service/span | P0 |
| `get_span_details` | Get details of a specific span | P0 |
| `find_trace_anomalies` | Find unusual trace patterns | P1 |
| `compare_traces` | Compare two traces | P1 |
| `get_critical_path` | Identify critical path in trace | P1 |

### Service Maps

| Skill | Description | Priority |
|-------|-------------|----------|
| `get_service_map` | Get full service dependency map | P0 |
| `get_upstream_services` | Find services that call this one | P0 |
| `get_downstream_services` | Find services this one calls | P0 |
| `detect_circular_dependencies` | Find circular service dependencies | P2 |
| `get_service_impact` | Impact analysis if service fails | P1 |

---

## Observability - Metrics

Infrastructure and application metrics.

### Metric Discovery

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_metrics` | List all available metrics | P0 |
| `search_metrics` | Search metrics by name/label | P0 |
| `get_metric_metadata` | Get metadata for a metric | P0 |
| `get_metric_dimensions` | Get available dimensions/labels | P0 |

### Metric Queries

| Skill | Description | Priority |
|-------|-------------|----------|
| `get_metric_value` | Get current value of a metric | P0 |
| `get_metric_timeseries` | Get metric values over time | P0 |
| `get_metric_stats` | Get min/max/avg for a metric | P0 |
| `get_metric_percentiles` | Get percentile values | P0 |
| `compare_metric_periods` | Compare metric across time periods | P1 |
| `aggregate_metric_by_dimension` | Aggregate by host, region, etc. | P0 |

### Infrastructure Metrics

| Skill | Description | Priority |
|-------|-------------|----------|
| `get_host_cpu` | Get CPU usage for hosts | P0 |
| `get_host_memory` | Get memory usage for hosts | P0 |
| `get_host_disk` | Get disk usage for hosts | P0 |
| `get_host_network` | Get network metrics for hosts | P0 |
| `get_container_metrics` | Get container resource metrics | P0 |
| `get_pod_metrics` | Get Kubernetes pod metrics | P0 |
| `get_node_metrics` | Get Kubernetes node metrics | P0 |
| `list_hosts` | List all monitored hosts | P0 |
| `get_host_health` | Get health status of a host | P0 |

### Metric Analysis

| Skill | Description | Priority |
|-------|-------------|----------|
| `detect_metric_anomalies` | Find unusual metric values | P0 |
| `get_metric_forecast` | Forecast future metric values | P1 |
| `correlate_metrics` | Find correlated metrics | P1 |
| `get_metric_baseline` | Get normal baseline for a metric | P1 |
| `compare_host_metrics` | Compare metrics across hosts | P1 |
| `get_resource_saturation` | Find saturated resources | P0 |

---

## Observability - Uptime

Synthetic monitoring and availability.

### Monitor Management

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_monitors` | List all uptime monitors | P0 |
| `get_monitor_status` | Get current status of a monitor | P0 |
| `get_monitor_history` | Get historical status | P0 |
| `create_monitor` | Create a new uptime monitor | P1 |
| `update_monitor` | Update monitor configuration | P1 |
| `delete_monitor` | Delete a monitor | P1 |

### Availability Analysis

| Skill | Description | Priority |
|-------|-------------|----------|
| `get_availability` | Get availability percentage | P0 |
| `get_downtime_events` | List downtime incidents | P0 |
| `get_response_time` | Get response time metrics | P0 |
| `get_ssl_certificate_status` | Check SSL certificate expiry | P0 |
| `get_dns_resolution_time` | Get DNS lookup times | P1 |
| `get_tcp_connection_time` | Get TCP connection times | P1 |
| `compare_location_availability` | Compare availability by location | P1 |

### Alerting

| Skill | Description | Priority |
|-------|-------------|----------|
| `get_uptime_alerts` | Get active uptime alerts | P0 |
| `acknowledge_uptime_alert` | Acknowledge an alert | P0 |
| `get_alert_history` | Get historical alerts | P0 |
| `create_uptime_alert` | Create availability alert | P1 |

---

## Observability - Profiling

Universal profiling for performance analysis.

### Profile Analysis

| Skill | Description | Priority |
|-------|-------------|----------|
| `get_flamegraph` | Get flamegraph for a service | P1 |
| `get_cpu_hotspots` | Find CPU-intensive functions | P1 |
| `get_memory_allocations` | Find memory allocation hotspots | P1 |
| `compare_profiles` | Compare profiles between versions | P2 |
| `get_profile_diff` | Differential flamegraph | P2 |

### Resource Analysis

| Skill | Description | Priority |
|-------|-------------|----------|
| `get_top_cpu_consumers` | Top CPU-consuming processes | P1 |
| `get_top_memory_consumers` | Top memory-consuming processes | P1 |
| `estimate_cost_savings` | Estimate infra cost savings | P2 |
| `get_co2_footprint` | Estimate carbon footprint | P2 |

---

## Security - SIEM

Security Information and Event Management.

### Detection & Alerts

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_security_alerts` | List all active security alerts | P0 |
| `get_alert_details` | Get full details of a security alert | P0 |
| `acknowledge_alert` | Acknowledge a security alert | P0 |
| `close_alert` | Close/resolve a security alert | P0 |
| `get_alert_timeline` | Get timeline of alert events | P0 |
| `escalate_alert` | Escalate alert to case/incident | P0 |
| `get_alert_trends` | Alert trends over time | P0 |
| `get_alerts_by_severity` | Alerts grouped by severity | P0 |
| `get_alerts_by_rule` | Alerts grouped by detection rule | P0 |
| `suppress_alert` | Add suppression for false positive | P1 |

### Detection Rules

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_detection_rules` | List all detection rules | P0 |
| `get_rule_details` | Get rule definition and logic | P0 |
| `enable_rule` | Enable a detection rule | P0 |
| `disable_rule` | Disable a detection rule | P0 |
| `get_rule_matches` | Get documents matching a rule | P0 |
| `test_rule` | Test a rule against data | P1 |
| `create_custom_rule` | Create a custom detection rule | P1 |
| `import_rules` | Import rules from package | P1 |
| `get_rule_exceptions` | Get exception list for a rule | P0 |
| `add_rule_exception` | Add exception to a rule | P0 |

### Threat Hunting

| Skill | Description | Priority |
|-------|-------------|----------|
| `search_security_events` | Search security event logs | P0 |
| `get_events_by_host` | Security events for a host | P0 |
| `get_events_by_user` | Security events for a user | P0 |
| `get_process_events` | Get process execution events | P0 |
| `get_network_events` | Get network connection events | P0 |
| `get_file_events` | Get file system events | P0 |
| `get_authentication_events` | Get auth success/failure events | P0 |
| `hunt_ioc` | Hunt for indicator of compromise | P0 |
| `search_by_hash` | Search for file by hash | P0 |
| `search_by_ip` | Search for IP address activity | P0 |
| `search_by_domain` | Search for domain activity | P0 |
| `get_dns_queries` | Get DNS query events | P0 |
| `get_registry_events` | Get Windows registry events | P1 |

### Investigation & Cases

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_cases` | List all security cases | P0 |
| `get_case_details` | Get full case information | P0 |
| `create_case` | Create a new security case | P0 |
| `update_case` | Update case status/details | P0 |
| `add_case_comment` | Add comment to a case | P0 |
| `attach_alert_to_case` | Link alert to a case | P0 |
| `get_case_timeline` | Get investigation timeline | P0 |
| `close_case` | Close a security case | P0 |
| `get_similar_cases` | Find similar past cases | P1 |

### Threat Intelligence

| Skill | Description | Priority |
|-------|-------------|----------|
| `search_threat_intel` | Search threat intelligence | P0 |
| `check_ioc_reputation` | Check IOC against threat feeds | P0 |
| `get_threat_intel_matches` | Get events matching threat intel | P0 |
| `add_indicator` | Add indicator to threat intel | P1 |
| `list_threat_feeds` | List configured threat feeds | P0 |

### User & Entity Analytics

| Skill | Description | Priority |
|-------|-------------|----------|
| `get_user_risk_score` | Get risk score for a user | P0 |
| `get_host_risk_score` | Get risk score for a host | P0 |
| `get_risky_users` | List highest risk users | P0 |
| `get_risky_hosts` | List highest risk hosts | P0 |
| `get_user_behavior_anomalies` | Detect unusual user behavior | P1 |
| `get_user_activity_summary` | Summary of user activity | P0 |
| `compare_user_baseline` | Compare user to their baseline | P1 |

---

## Security - Endpoint

Endpoint detection and response.

### Endpoint Overview

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_endpoints` | List all managed endpoints | P0 |
| `get_endpoint_status` | Get endpoint health/status | P0 |
| `get_endpoint_details` | Get detailed endpoint info | P0 |
| `get_endpoints_by_policy` | Endpoints grouped by policy | P0 |
| `get_endpoints_by_os` | Endpoints grouped by OS | P0 |
| `get_unhealthy_endpoints` | Find endpoints with issues | P0 |

### Endpoint Response

| Skill | Description | Priority |
|-------|-------------|----------|
| `isolate_endpoint` | Isolate endpoint from network | P0 |
| `release_endpoint` | Release endpoint from isolation | P0 |
| `get_isolation_status` | Check endpoint isolation status | P0 |
| `kill_process` | Kill a process on endpoint | P1 |
| `suspend_process` | Suspend a process on endpoint | P1 |
| `get_running_processes` | List running processes | P0 |
| `get_file_info` | Get file info from endpoint | P0 |
| `retrieve_file` | Retrieve file from endpoint | P1 |
| `execute_osquery` | Run osquery on endpoint | P1 |

### Endpoint Events

| Skill | Description | Priority |
|-------|-------------|----------|
| `get_endpoint_alerts` | Get alerts for an endpoint | P0 |
| `get_endpoint_events` | Get all events for an endpoint | P0 |
| `get_malware_detections` | Get malware detection events | P0 |
| `get_ransomware_detections` | Get ransomware detections | P0 |
| `get_memory_threats` | Get memory threat detections | P0 |
| `get_behavior_detections` | Get behavioral detections | P0 |

### Policy Management

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_endpoint_policies` | List endpoint policies | P0 |
| `get_policy_details` | Get policy configuration | P0 |
| `create_policy` | Create endpoint policy | P1 |
| `update_policy` | Update policy settings | P1 |
| `assign_policy` | Assign policy to endpoints | P1 |
| `get_policy_response_actions` | Get response action settings | P0 |

---

## Security - Cloud Security

Cloud security posture and workload protection.

### Cloud Posture

| Skill | Description | Priority |
|-------|-------------|----------|
| `get_cloud_posture_score` | Get overall cloud security score | P0 |
| `list_cloud_findings` | List cloud security findings | P0 |
| `get_findings_by_provider` | Findings by cloud provider | P0 |
| `get_findings_by_severity` | Findings by severity | P0 |
| `get_findings_by_resource` | Findings for specific resource | P0 |
| `get_compliance_status` | Get compliance framework status | P0 |
| `get_benchmark_results` | Get CIS benchmark results | P0 |
| `mute_finding` | Mute a false positive finding | P1 |
| `get_finding_remediation` | Get remediation guidance | P0 |

### Cloud Assets

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_cloud_assets` | List all cloud assets | P0 |
| `get_asset_inventory` | Get full asset inventory | P0 |
| `search_cloud_assets` | Search assets by attributes | P0 |
| `get_asset_vulnerabilities` | Get vulnerabilities for asset | P0 |
| `get_asset_misconfigurations` | Get misconfigs for asset | P0 |
| `get_exposed_assets` | Find publicly exposed assets | P0 |

### Kubernetes Security

| Skill | Description | Priority |
|-------|-------------|----------|
| `get_k8s_findings` | Get Kubernetes security findings | P0 |
| `get_vulnerable_images` | Find vulnerable container images | P0 |
| `get_privileged_containers` | Find privileged containers | P0 |
| `get_k8s_network_policies` | Analyze network policies | P1 |
| `get_rbac_analysis` | Analyze RBAC permissions | P1 |

---

## Machine Learning

Anomaly detection and machine learning.

### Anomaly Detection Jobs

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_ml_jobs` | List all ML anomaly jobs | P0 |
| `get_job_status` | Get ML job status | P0 |
| `start_ml_job` | Start an ML job | P0 |
| `stop_ml_job` | Stop an ML job | P0 |
| `get_job_results` | Get anomaly detection results | P0 |
| `create_anomaly_job` | Create new anomaly detection job | P1 |
| `delete_ml_job` | Delete an ML job | P1 |

### Anomalies

| Skill | Description | Priority |
|-------|-------------|----------|
| `get_anomalies` | Get detected anomalies | P0 |
| `get_anomalies_by_severity` | Anomalies by severity score | P0 |
| `get_anomalies_for_host` | Anomalies for a specific host | P0 |
| `get_anomalies_for_user` | Anomalies for a specific user | P0 |
| `get_anomaly_details` | Get full anomaly details | P0 |
| `explain_anomaly` | Get explanation for anomaly | P0 |
| `get_influencers` | Get influencing factors | P0 |

### Data Frame Analytics

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_dfa_jobs` | List data frame analytics jobs | P0 |
| `get_dfa_results` | Get DFA job results | P0 |
| `get_outliers` | Get outlier detection results | P0 |
| `get_regression_predictions` | Get regression predictions | P1 |
| `get_classification_predictions` | Get classification results | P1 |
| `create_dfa_job` | Create DFA job | P2 |

### Trained Models

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_trained_models` | List trained models | P0 |
| `get_model_details` | Get model information | P0 |
| `deploy_model` | Deploy a trained model | P1 |
| `undeploy_model` | Undeploy a model | P1 |
| `get_model_stats` | Get model inference stats | P0 |
| `infer_with_model` | Run inference with a model | P1 |

### Inference

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_inference_endpoints` | List inference endpoints | P0 |
| `create_inference_endpoint` | Create inference endpoint | P1 |
| `run_inference` | Run inference request | P0 |
| `embed_text` | Generate text embeddings | P0 |
| `classify_text` | Classify text | P1 |
| `extract_entities` | Extract named entities | P1 |
| `summarize_text` | Summarize text content | P1 |
| `rerank_results` | Rerank search results | P1 |

---

## Alerting & Actions

Rules, alerts, and automated actions.

### Alert Rules

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_alert_rules` | List all alerting rules | P0 |
| `get_rule_details` | Get rule configuration | P0 |
| `create_alert_rule` | Create new alert rule | P0 |
| `update_alert_rule` | Update rule settings | P0 |
| `enable_alert_rule` | Enable an alert rule | P0 |
| `disable_alert_rule` | Disable an alert rule | P0 |
| `delete_alert_rule` | Delete an alert rule | P0 |
| `test_alert_rule` | Test rule execution | P1 |
| `get_rule_execution_log` | Get rule execution history | P0 |

### Active Alerts

| Skill | Description | Priority |
|-------|-------------|----------|
| `get_active_alerts` | Get all active alerts | P0 |
| `get_alerts_by_rule` | Alerts for a specific rule | P0 |
| `get_alert_history` | Historical alert data | P0 |
| `mute_alert` | Mute an alert temporarily | P0 |
| `unmute_alert` | Unmute an alert | P0 |

### Connectors & Actions

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_connectors` | List all connectors | P0 |
| `get_connector_details` | Get connector configuration | P0 |
| `create_connector` | Create a new connector | P1 |
| `test_connector` | Test connector connectivity | P0 |
| `delete_connector` | Delete a connector | P1 |
| `execute_connector` | Execute connector action | P1 |
| `list_connector_types` | List available connector types | P0 |

---

## Index & Data Management

Index lifecycle, transforms, and data streams.

### Index Management

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_indices` | List all indices | P0 |
| `get_index_details` | Get index settings/mappings | P0 |
| `create_index` | Create a new index | P1 |
| `delete_index` | Delete an index | P1 |
| `get_index_stats` | Get index statistics | P0 |
| `get_index_health` | Get index health status | P0 |
| `refresh_index` | Refresh an index | P1 |
| `flush_index` | Flush an index | P1 |
| `freeze_index` | Freeze an index | P2 |
| `unfreeze_index` | Unfreeze an index | P2 |

### Data Streams

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_data_streams` | List all data streams | P0 |
| `get_data_stream_details` | Get data stream info | P0 |
| `create_data_stream` | Create a data stream | P1 |
| `delete_data_stream` | Delete a data stream | P1 |
| `get_data_stream_stats` | Get data stream statistics | P0 |
| `rollover_data_stream` | Force rollover | P1 |

### Index Lifecycle Management

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_ilm_policies` | List ILM policies | P0 |
| `get_ilm_policy` | Get ILM policy details | P0 |
| `create_ilm_policy` | Create ILM policy | P1 |
| `delete_ilm_policy` | Delete ILM policy | P1 |
| `explain_ilm` | Explain index ILM status | P0 |
| `retry_ilm_step` | Retry failed ILM step | P1 |
| `move_to_ilm_step` | Move to specific ILM step | P2 |

### Transforms

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_transforms` | List all transforms | P0 |
| `get_transform_details` | Get transform configuration | P0 |
| `start_transform` | Start a transform | P0 |
| `stop_transform` | Stop a transform | P0 |
| `get_transform_stats` | Get transform statistics | P0 |
| `create_transform` | Create a new transform | P1 |
| `delete_transform` | Delete a transform | P1 |
| `preview_transform` | Preview transform results | P1 |

---

## Cluster Operations

Cluster management and operations.

### Cluster Health

| Skill | Description | Priority |
|-------|-------------|----------|
| `get_cluster_health` | Get cluster health status | P0 |
| `get_cluster_stats` | Get cluster statistics | P0 |
| `get_cluster_state` | Get cluster state summary | P0 |
| `get_cluster_settings` | Get cluster settings | P0 |
| `update_cluster_settings` | Update cluster settings | P1 |

### Node Management

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_nodes` | List all cluster nodes | P0 |
| `get_node_stats` | Get node statistics | P0 |
| `get_node_info` | Get node information | P0 |
| `get_hot_threads` | Get hot threads on nodes | P1 |
| `exclude_node` | Exclude node from allocation | P1 |

### Shard Management

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_shards` | List all shards | P0 |
| `get_shard_allocation` | Get shard allocation info | P0 |
| `explain_allocation` | Explain shard allocation | P0 |
| `reroute_shard` | Reroute a shard | P1 |
| `get_unassigned_shards` | List unassigned shards | P0 |

### Tasks & Operations

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_tasks` | List running tasks | P0 |
| `get_task_details` | Get task information | P0 |
| `cancel_task` | Cancel a running task | P1 |
| `get_pending_tasks` | Get pending cluster tasks | P0 |

### Snapshots & Restore

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_repositories` | List snapshot repositories | P0 |
| `list_snapshots` | List snapshots in repository | P0 |
| `get_snapshot_details` | Get snapshot information | P0 |
| `create_snapshot` | Create a new snapshot | P1 |
| `restore_snapshot` | Restore from snapshot | P1 |
| `delete_snapshot` | Delete a snapshot | P1 |
| `get_snapshot_status` | Get in-progress snapshot status | P0 |

---

## Fleet & Agent

Elastic Agent and Fleet management.

### Agent Management

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_agents` | List all enrolled agents | P0 |
| `get_agent_details` | Get agent information | P0 |
| `get_agent_status` | Get agent health status | P0 |
| `unenroll_agent` | Unenroll an agent | P1 |
| `upgrade_agent` | Upgrade an agent | P1 |
| `get_agent_logs` | Get agent log output | P0 |
| `restart_agent` | Restart an agent | P1 |

### Agent Policies

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_agent_policies` | List agent policies | P0 |
| `get_policy_details` | Get policy configuration | P0 |
| `create_agent_policy` | Create new agent policy | P1 |
| `update_agent_policy` | Update agent policy | P1 |
| `delete_agent_policy` | Delete agent policy | P1 |
| `get_policy_agents` | Get agents using policy | P0 |

### Integrations

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_integrations` | List available integrations | P0 |
| `get_integration_details` | Get integration info | P0 |
| `install_integration` | Install an integration | P1 |
| `uninstall_integration` | Uninstall integration | P1 |
| `list_installed_integrations` | List installed integrations | P0 |

---

## Enterprise Search

App Search and Workplace Search.

### Search Applications

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_search_applications` | List search applications | P0 |
| `get_search_application` | Get application details | P0 |
| `search_application` | Search within application | P0 |
| `create_search_application` | Create search application | P1 |
| `delete_search_application` | Delete search application | P1 |

### Search Analytics

| Skill | Description | Priority |
|-------|-------------|----------|
| `get_search_analytics` | Get search analytics | P0 |
| `get_top_queries` | Get most popular queries | P0 |
| `get_no_results_queries` | Queries with no results | P0 |
| `get_click_analytics` | Get click-through data | P0 |
| `get_search_trends` | Search trends over time | P1 |

### Crawlers

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_crawlers` | List web crawlers | P0 |
| `get_crawler_status` | Get crawler status | P0 |
| `start_crawl` | Start a crawl | P1 |
| `stop_crawl` | Stop a crawl | P1 |
| `get_crawl_stats` | Get crawl statistics | P0 |

### Connectors

| Skill | Description | Priority |
|-------|-------------|----------|
| `list_search_connectors` | List search connectors | P0 |
| `get_connector_status` | Get connector sync status | P0 |
| `sync_connector` | Trigger connector sync | P1 |
| `get_sync_history` | Get sync history | P0 |

---

## Integrations

External service integrations.

### Notification Integrations

| Skill | Description | Priority |
|-------|-------------|----------|
| `send_slack_message` | Send Slack notification | P0 |
| `send_email` | Send email notification | P0 |
| `send_pagerduty_alert` | Trigger PagerDuty incident | P0 |
| `send_teams_message` | Send Microsoft Teams message | P1 |
| `send_webhook` | Send webhook notification | P0 |
| `send_opsgenie_alert` | Send OpsGenie alert | P1 |

### Ticketing Integrations

| Skill | Description | Priority |
|-------|-------------|----------|
| `create_jira_issue` | Create Jira ticket | P0 |
| `update_jira_issue` | Update Jira ticket | P0 |
| `create_servicenow_incident` | Create ServiceNow incident | P1 |
| `update_servicenow_incident` | Update ServiceNow incident | P1 |
| `create_github_issue` | Create GitHub issue | P1 |

### Cloud Integrations

| Skill | Description | Priority |
|-------|-------------|----------|
| `aws_lambda_invoke` | Invoke AWS Lambda | P1 |
| `aws_ssm_command` | Run AWS SSM command | P1 |
| `azure_function_invoke` | Invoke Azure Function | P2 |
| `gcp_function_invoke` | Invoke GCP Function | P2 |

### Orchestration

| Skill | Description | Priority |
|-------|-------------|----------|
| `run_ansible_playbook` | Run Ansible playbook | P2 |
| `trigger_jenkins_job` | Trigger Jenkins build | P2 |
| `trigger_github_workflow` | Trigger GitHub Action | P2 |
| `run_terraform_plan` | Run Terraform plan | P2 |

---

## Implementation Plan

### Phase 1: Foundation (Q1 2026)

**Meta-Skills + Core Search + Log Basics**

- [ ] All 15 Meta-Skills
- [ ] Top 20 Search & Data skills
- [ ] Top 15 Log skills
- [ ] Top 10 Alerting skills

**Total: ~60 skills**

### Phase 2: Observability (Q2 2026)

**Full Logs + APM + Metrics**

- [ ] Remaining Log skills
- [ ] All APM skills
- [ ] All Metrics skills
- [ ] Uptime basics

**Total: ~120 skills**

### Phase 3: Security (Q3 2026)

**SIEM + Endpoint**

- [ ] All SIEM skills
- [ ] All Endpoint skills
- [ ] Core ML skills

**Total: ~130 skills**

### Phase 4: Operations (Q4 2026)

**Cluster Ops + Index Management + Fleet**

- [ ] All Cluster Operations skills
- [ ] All Index Management skills
- [ ] All Fleet skills

**Total: ~85 skills**

### Phase 5: Advanced (Q1 2027)

**Cloud Security + Enterprise Search + Integrations**

- [ ] Cloud Security skills
- [ ] Enterprise Search skills
- [ ] Profiling skills
- [ ] Remaining Integrations

**Total: ~110 skills**

---

## Skill Naming Conventions

### Pattern

```
{action}_{resource}[_modifier]
```

### Actions

| Action | Description | Example |
|--------|-------------|---------|
| `list_` | List multiple items | `list_services` |
| `get_` | Get single item or specific data | `get_service_health` |
| `search_` | Full-text search | `search_logs` |
| `count_` | Count aggregation | `count_logs_by_level` |
| `create_` | Create new item | `create_alert_rule` |
| `update_` | Update existing item | `update_case` |
| `delete_` | Delete item | `delete_index` |
| `start_` | Start a process | `start_ml_job` |
| `stop_` | Stop a process | `stop_transform` |
| `enable_` | Enable something | `enable_rule` |
| `disable_` | Disable something | `disable_rule` |
| `send_` | Send notification | `send_slack_message` |

### Resources

Use Elastic's terminology:
- `logs`, `metrics`, `traces`, `events`
- `alerts`, `rules`, `cases`
- `hosts`, `endpoints`, `agents`
- `services`, `transactions`, `spans`
- `indices`, `shards`, `nodes`

---

## Skill Quality Guidelines

### Required Elements

1. **Clear name** following naming conventions
2. **Rich description** for AI understanding
3. **Typed parameters** with descriptions
4. **Sensible defaults** for optional params
5. **Structured return type**

### Example Skill Definition

```sql
CREATE SKILL get_error_rate
  VERSION '1.0'
  DESCRIPTION 'Calculate the error rate (percentage of ERROR logs) for a service over a time period. Use this skill when investigating service health, comparing error rates, or setting up monitoring. Returns the error count, total count, and error rate percentage.'
  AUTHOR 'Elastic'
  TAGS ['observability', 'logs', 'errors', 'health']
  (
    service STRING 
      DESCRIPTION 'Service name to analyze (e.g., api-gateway, auth-service)',
    time_range STRING DEFAULT '1h' 
      DESCRIPTION 'Time range to analyze (e.g., 15m, 1h, 24h, 7d)',
    index_pattern STRING DEFAULT 'logs-*' 
      DESCRIPTION 'Index pattern to search'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE total INT;
  DECLARE errors INT;
  
  SET total = ESQL_QUERY('FROM ' || index_pattern || 
    ' | WHERE service == "' || service || '" | STATS count=COUNT()')[0].count;
  SET errors = ESQL_QUERY('FROM ' || index_pattern || 
    ' | WHERE service == "' || service || '" AND level == "ERROR" | STATS count=COUNT()')[0].count;
  
  RETURN {
    'service': service,
    'time_range': time_range,
    'total_logs': total,
    'error_count': errors,
    'error_rate': ROUND((errors * 100.0) / total, 2),
    'status': CASE WHEN errors * 100.0 / total > 5 THEN 'critical'
                   WHEN errors * 100.0 / total > 1 THEN 'warning'
                   ELSE 'healthy' END
  };
END SKILL;
```

---

## See Also

- [Creating Skills](creating-skills.md)
- [Skill Parameters](parameters.md)
- [MCP Bridge](../tools/mcp-bridge.md)
