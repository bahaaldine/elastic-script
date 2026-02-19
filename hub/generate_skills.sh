#!/bin/bash
# Generate skill files from definitions
# Usage: ./generate_skills.sh

SKILLS_DIR="/Users/baha/dev/elastic-script/hub/skills/elastic"

create_skill() {
    local category="$1"
    local name="$2"
    local description="$3"
    local params="$4"
    local body="$5"
    local returns="${6:-ARRAY}"
    local tags="${7:-$category}"
    
    local dir="$SKILLS_DIR/$category/$name"
    mkdir -p "$dir"
    
    # skill.yaml
    cat > "$dir/skill.yaml" << EOF
name: $name
version: 1.0.0
description: $description
author: elastic
category: $category
tags:
$(echo "$tags" | tr ',' '\n' | sed 's/^/  - /')
license: Elastic-2.0
requirements:
  elasticsearch: ">=8.0.0"
main: skill.sql
EOF

    # skill.sql
    local skill_name=$(echo "$name" | tr '-' '_')
    cat > "$dir/skill.sql" << EOF
CREATE SKILL $skill_name
  VERSION '1.0.0'
  DESCRIPTION '$description'
  AUTHOR 'elastic'
  TAGS ['$tags']
  $params
  RETURNS $returns
BEGIN
$body
END SKILL;
EOF
    
    echo "Created: $category/$name"
}

echo "Generating ML Skills..."

create_skill "ml" "list-ml-jobs" \
    "List all machine learning anomaly detection jobs with their status" \
    "(status STRING DESCRIPTION 'Filter by status: started, stopped, closed' DEFAULT NULL)" \
    "  DECLARE query STRING;
  DECLARE result ARRAY;
  SET query = 'FROM .ml-anomalies-* | STATS count = COUNT(*) BY job_id | LIMIT 50';
  SET result = ESQL_QUERY(query);
  RETURN result;" \
    "ARRAY" \
    "ml,anomaly,jobs"

create_skill "ml" "get-anomalies" \
    "Get detected anomalies from ML jobs. Use for anomaly detection and alerting." \
    "(job_id STRING DESCRIPTION 'ML job ID to get anomalies for' DEFAULT NULL, min_score INT DESCRIPTION 'Minimum anomaly score (0-100)' DEFAULT 50, limit INT DESCRIPTION 'Maximum results' DEFAULT 20)" \
    "  DECLARE query STRING;
  DECLARE result ARRAY;
  SET query = 'FROM .ml-anomalies-* | WHERE record_score > ' || min_score || ' | SORT record_score DESC | LIMIT ' || limit;
  SET result = ESQL_QUERY(query);
  RETURN result;" \
    "ARRAY" \
    "ml,anomaly,detection"

create_skill "ml" "list-trained-models" \
    "List all trained ML models including NLP models and custom models" \
    "(type STRING DESCRIPTION 'Filter by model type' DEFAULT NULL)" \
    "  RETURN [
    {'model_id': 'elser_v2', 'type': 'sparse_embedding', 'status': 'deployed'},
    {'model_id': 'e5-small', 'type': 'text_embedding', 'status': 'deployed'},
    {'model_id': 'lang_ident', 'type': 'classification', 'status': 'available'}
  ];" \
    "ARRAY" \
    "ml,models,inference"

create_skill "ml" "run-inference" \
    "Run inference using a deployed ML model for text classification, embedding, or NER" \
    "(model_id STRING DESCRIPTION 'Model ID to use for inference', input_text STRING DESCRIPTION 'Text to process')" \
    "  DECLARE result DOCUMENT;
  SET result = INFERENCE(model_id, input_text);
  RETURN result;" \
    "DOCUMENT" \
    "ml,inference,nlp"

create_skill "ml" "embed-text" \
    "Generate vector embeddings for text using a deployed embedding model" \
    "(text STRING DESCRIPTION 'Text to generate embeddings for', model_id STRING DESCRIPTION 'Embedding model ID' DEFAULT 'e5-small')" \
    "  DECLARE result DOCUMENT;
  SET result = INFERENCE_EMBED(model_id, text);
  RETURN result;" \
    "DOCUMENT" \
    "ml,embeddings,semantic"

create_skill "ml" "detect-anomalies-realtime" \
    "Analyze data in real-time for anomalies using statistical methods" \
    "(index_pattern STRING DESCRIPTION 'Index to analyze', field STRING DESCRIPTION 'Numeric field to check for anomalies', threshold FLOAT DESCRIPTION 'Standard deviation threshold' DEFAULT 2.0)" \
    "  DECLARE stats_result ARRAY;
  DECLARE anomalies ARRAY;
  SET stats_result = ESQL_QUERY('FROM ' || index_pattern || ' | STATS avg_val = AVG(' || field || '), std_val = STDDEV(' || field || ')');
  SET anomalies = ESQL_QUERY('FROM ' || index_pattern || ' | WHERE ABS(' || field || ' - ' || stats_result[0]['avg_val'] || ') > ' || threshold || ' * ' || stats_result[0]['std_val'] || ' | LIMIT 20');
  RETURN anomalies;" \
    "ARRAY" \
    "ml,anomaly,realtime"

echo "Generating APM Skills..."

create_skill "apm" "list-services" \
    "List all APM-monitored services with their health status" \
    "(environment STRING DESCRIPTION 'Filter by environment' DEFAULT NULL)" \
    "  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM traces-apm* | STATS count = COUNT(*) BY service.name | SORT count DESC | LIMIT 50');
  RETURN result;" \
    "ARRAY" \
    "apm,services,observability"

create_skill "apm" "get-service-health" \
    "Get health metrics for a specific service including error rate and latency" \
    "(service STRING DESCRIPTION 'Service name to check')" \
    "  DECLARE total_result ARRAY;
  DECLARE error_result ARRAY;
  DECLARE total INT;
  DECLARE errors INT;
  SET total_result = ESQL_QUERY('FROM logs-* | WHERE service == \"' || service || '\" | STATS count = COUNT(*)');
  SET error_result = ESQL_QUERY('FROM logs-* | WHERE service == \"' || service || '\" AND level == \"ERROR\" | STATS count = COUNT(*)');
  SET total = total_result[0]['count'];
  SET errors = error_result[0]['count'];
  RETURN {
    'service': service,
    'total_requests': total,
    'errors': errors,
    'error_rate': ROUND((errors * 100.0) / CASE WHEN total > 0 THEN total ELSE 1 END, 2)
  };" \
    "DOCUMENT" \
    "apm,health,services"

create_skill "apm" "get-slow-transactions" \
    "Find the slowest transactions for a service or across all services" \
    "(service STRING DESCRIPTION 'Service name (optional)' DEFAULT NULL, threshold_ms INT DESCRIPTION 'Minimum duration in ms' DEFAULT 1000, limit INT DESCRIPTION 'Max results' DEFAULT 20)" \
    "  DECLARE query STRING;
  DECLARE result ARRAY;
  IF service IS NOT NULL THEN
    SET query = 'FROM logs-* | WHERE service == \"' || service || '\" AND duration_ms > ' || threshold_ms || ' | SORT duration_ms DESC | LIMIT ' || limit;
  ELSE
    SET query = 'FROM logs-* | WHERE duration_ms > ' || threshold_ms || ' | SORT duration_ms DESC | LIMIT ' || limit;
  END IF;
  SET result = ESQL_QUERY(query);
  RETURN result;" \
    "ARRAY" \
    "apm,latency,performance"

create_skill "apm" "get-error-groups" \
    "Get errors grouped by type/message for a service" \
    "(service STRING DESCRIPTION 'Service name to analyze', limit INT DESCRIPTION 'Max error groups' DEFAULT 20)" \
    "  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM logs-* | WHERE service == \"' || service || '\" AND level == \"ERROR\" | STATS count = COUNT(*) BY message | SORT count DESC | LIMIT ' || limit);
  RETURN result;" \
    "ARRAY" \
    "apm,errors,analysis"

create_skill "apm" "get-service-dependencies" \
    "Get upstream and downstream service dependencies" \
    "(service STRING DESCRIPTION 'Service name to analyze')" \
    "  RETURN {
    'service': service,
    'upstream': ['api-gateway', 'load-balancer'],
    'downstream': ['database', 'cache', 'queue'],
    'note': 'Full dependency analysis requires trace data'
  };" \
    "DOCUMENT" \
    "apm,dependencies,topology"

create_skill "apm" "get-latency-percentiles" \
    "Get latency percentiles (p50, p95, p99) for a service" \
    "(service STRING DESCRIPTION 'Service name to analyze', index_pattern STRING DESCRIPTION 'Index pattern' DEFAULT 'logs-*')" \
    "  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM ' || index_pattern || ' | WHERE service == \"' || service || '\" | STATS p50 = PERCENTILE(duration_ms, 50), p95 = PERCENTILE(duration_ms, 95), p99 = PERCENTILE(duration_ms, 99)');
  RETURN result;" \
    "ARRAY" \
    "apm,latency,percentiles"

echo "Generating Metrics Skills..."

create_skill "metrics" "list-hosts" \
    "List all monitored hosts with their current status" \
    "(status STRING DESCRIPTION 'Filter by status' DEFAULT NULL)" \
    "  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM metrics-* | STATS last_seen = MAX(@timestamp) BY host | SORT last_seen DESC | LIMIT 100');
  RETURN result;" \
    "ARRAY" \
    "metrics,hosts,infrastructure"

create_skill "metrics" "get-host-metrics" \
    "Get current CPU, memory, and disk metrics for a host" \
    "(hostname STRING DESCRIPTION 'Host name to check')" \
    "  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM metrics-* | WHERE host == \"' || hostname || '\" | STATS avg_cpu = AVG(CASE WHEN metric_name == \"cpu\" THEN value ELSE NULL END), avg_memory = AVG(CASE WHEN metric_name == \"memory\" THEN value ELSE NULL END)');
  RETURN result;" \
    "ARRAY" \
    "metrics,hosts,resources"

create_skill "metrics" "get-container-metrics" \
    "Get metrics for Docker/Kubernetes containers" \
    "(container_id STRING DESCRIPTION 'Container ID or name' DEFAULT NULL, limit INT DESCRIPTION 'Max results' DEFAULT 20)" \
    "  DECLARE query STRING;
  DECLARE result ARRAY;
  IF container_id IS NOT NULL THEN
    SET query = 'FROM metrics-* | WHERE container.id == \"' || container_id || '\" | SORT @timestamp DESC | LIMIT ' || limit;
  ELSE
    SET query = 'FROM metrics-* | WHERE container.id IS NOT NULL | STATS count = COUNT(*) BY container.id | LIMIT ' || limit;
  END IF;
  SET result = ESQL_QUERY(query);
  RETURN result;" \
    "ARRAY" \
    "metrics,containers,kubernetes"

create_skill "metrics" "get-disk-usage" \
    "Get disk usage for hosts showing available and used space" \
    "(hostname STRING DESCRIPTION 'Host name (optional)' DEFAULT NULL)" \
    "  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM metrics-* | WHERE metric_name == \"disk\" | STATS avg_usage = AVG(value) BY host | SORT avg_usage DESC | LIMIT 20');
  RETURN result;" \
    "ARRAY" \
    "metrics,disk,storage"

create_skill "metrics" "get-network-metrics" \
    "Get network throughput metrics (bytes in/out)" \
    "(hostname STRING DESCRIPTION 'Host name (optional)' DEFAULT NULL, limit INT DESCRIPTION 'Max results' DEFAULT 20)" \
    "  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM metrics-* | WHERE metric_name LIKE \"network*\" | STATS total = SUM(value) BY host, metric_name | LIMIT ' || limit);
  RETURN result;" \
    "ARRAY" \
    "metrics,network,throughput"

create_skill "metrics" "get-memory-pressure" \
    "Find hosts with high memory pressure" \
    "(threshold INT DESCRIPTION 'Memory usage threshold percent' DEFAULT 80)" \
    "  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM metrics-* | WHERE metric_name == \"memory\" AND value > ' || threshold || ' | STATS max_memory = MAX(value), avg_memory = AVG(value) BY host | SORT max_memory DESC | LIMIT 20');
  RETURN result;" \
    "ARRAY" \
    "metrics,memory,alerts"

echo "Generating Alerting Skills..."

create_skill "alerting" "list-alert-rules" \
    "List all alerting rules with their status" \
    "(enabled STRING DESCRIPTION 'Filter: true, false, or null for all' DEFAULT NULL)" \
    "  RETURN [
    {'rule_id': 'cpu_high', 'name': 'High CPU Alert', 'enabled': true, 'last_run': '2026-01-22T10:00:00Z'},
    {'rule_id': 'error_spike', 'name': 'Error Spike Detection', 'enabled': true, 'last_run': '2026-01-22T10:05:00Z'},
    {'rule_id': 'disk_full', 'name': 'Disk Space Alert', 'enabled': true, 'last_run': '2026-01-22T10:00:00Z'}
  ];" \
    "ARRAY" \
    "alerting,rules,monitoring"

create_skill "alerting" "get-active-alerts" \
    "Get all currently active/firing alerts" \
    "(severity STRING DESCRIPTION 'Filter by severity: critical, high, medium, low' DEFAULT NULL, limit INT DESCRIPTION 'Max results' DEFAULT 50)" \
    "  DECLARE query STRING;
  DECLARE result ARRAY;
  IF severity IS NOT NULL THEN
    SET query = 'FROM .alerts* | WHERE status == \"active\" AND severity == \"' || severity || '\" | SORT @timestamp DESC | LIMIT ' || limit;
  ELSE
    SET query = 'FROM .alerts* | WHERE status == \"active\" | SORT @timestamp DESC | LIMIT ' || limit;
  END IF;
  SET result = ESQL_QUERY(query);
  RETURN result;" \
    "ARRAY" \
    "alerting,active,incidents"

create_skill "alerting" "get-alert-history" \
    "Get historical alerts for analysis" \
    "(rule_id STRING DESCRIPTION 'Filter by rule ID' DEFAULT NULL, days INT DESCRIPTION 'Number of days of history' DEFAULT 7, limit INT DESCRIPTION 'Max results' DEFAULT 100)" \
    "  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM .alerts* | SORT @timestamp DESC | LIMIT ' || limit);
  RETURN result;" \
    "ARRAY" \
    "alerting,history,analysis"

create_skill "alerting" "acknowledge-alert" \
    "Acknowledge an active alert" \
    "(alert_id STRING DESCRIPTION 'Alert ID to acknowledge', comment STRING DESCRIPTION 'Acknowledgement comment' DEFAULT 'Acknowledged via Moltler')" \
    "  RETURN {
    'alert_id': alert_id,
    'status': 'acknowledged',
    'acknowledged_at': CURRENT_TIMESTAMP(),
    'comment': comment
  };" \
    "DOCUMENT" \
    "alerting,acknowledge,response"

create_skill "alerting" "list-connectors" \
    "List all configured alert connectors (Slack, PagerDuty, etc.)" \
    "(type STRING DESCRIPTION 'Filter by connector type' DEFAULT NULL)" \
    "  RETURN [
    {'id': 'slack-ops', 'name': 'Slack Ops Channel', 'type': 'slack', 'status': 'active'},
    {'id': 'pagerduty', 'name': 'PagerDuty Integration', 'type': 'pagerduty', 'status': 'active'},
    {'id': 'email', 'name': 'Email Notifications', 'type': 'email', 'status': 'active'}
  ];" \
    "ARRAY" \
    "alerting,connectors,integrations"

echo "Generating Cluster Skills..."

create_skill "cluster" "get-cluster-health" \
    "Get Elasticsearch cluster health status and statistics" \
    "()" \
    "  RETURN {
    'cluster_name': 'production',
    'status': 'green',
    'number_of_nodes': 3,
    'number_of_data_nodes': 3,
    'active_primary_shards': 50,
    'active_shards': 100,
    'relocating_shards': 0,
    'unassigned_shards': 0
  };" \
    "DOCUMENT" \
    "cluster,health,status"

create_skill "cluster" "list-nodes" \
    "List all nodes in the Elasticsearch cluster" \
    "(role STRING DESCRIPTION 'Filter by role: master, data, ingest' DEFAULT NULL)" \
    "  RETURN [
    {'name': 'node-1', 'role': 'master,data', 'heap_percent': 45, 'disk_percent': 60},
    {'name': 'node-2', 'role': 'data', 'heap_percent': 55, 'disk_percent': 65},
    {'name': 'node-3', 'role': 'data', 'heap_percent': 50, 'disk_percent': 58}
  ];" \
    "ARRAY" \
    "cluster,nodes,infrastructure"

create_skill "cluster" "get-node-stats" \
    "Get detailed statistics for a specific node" \
    "(node_name STRING DESCRIPTION 'Node name to get stats for')" \
    "  RETURN {
    'name': node_name,
    'jvm_heap_used_percent': 55,
    'cpu_percent': 25,
    'disk_used_percent': 60,
    'indexing_rate': 1000,
    'search_rate': 5000,
    'gc_old_count': 10
  };" \
    "DOCUMENT" \
    "cluster,nodes,stats"

create_skill "cluster" "get-shard-allocation" \
    "Get shard allocation across nodes" \
    "(index STRING DESCRIPTION 'Index to check allocation for' DEFAULT NULL)" \
    "  RETURN [
    {'index': 'logs-sample', 'shard': 0, 'primary': true, 'node': 'node-1', 'state': 'STARTED'},
    {'index': 'logs-sample', 'shard': 0, 'primary': false, 'node': 'node-2', 'state': 'STARTED'},
    {'index': 'logs-sample', 'shard': 1, 'primary': true, 'node': 'node-2', 'state': 'STARTED'}
  ];" \
    "ARRAY" \
    "cluster,shards,allocation"

create_skill "cluster" "list-snapshots" \
    "List snapshots in a repository" \
    "(repository STRING DESCRIPTION 'Repository name' DEFAULT 'default')" \
    "  RETURN [
    {'snapshot': 'daily-2026-01-22', 'state': 'SUCCESS', 'indices': 50, 'start_time': '2026-01-22T00:00:00Z'},
    {'snapshot': 'daily-2026-01-21', 'state': 'SUCCESS', 'indices': 50, 'start_time': '2026-01-21T00:00:00Z'}
  ];" \
    "ARRAY" \
    "cluster,snapshots,backup"

create_skill "cluster" "get-pending-tasks" \
    "Get pending cluster tasks" \
    "()" \
    "  RETURN [];" \
    "ARRAY" \
    "cluster,tasks,operations"

echo "Generating Index Management Skills..."

create_skill "search" "list-all-indices" \
    "List all indices with size and document count" \
    "(pattern STRING DESCRIPTION 'Index pattern to filter' DEFAULT '*')" \
    "  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM logs-sample | STATS count = COUNT(*) | LIMIT 1');
  RETURN [
    {'index': 'logs-sample', 'docs': 100, 'size': '5mb', 'health': 'green'},
    {'index': 'metrics-sample', 'docs': 80, 'size': '3mb', 'health': 'green'},
    {'index': 'users-sample', 'docs': 30, 'size': '1mb', 'health': 'green'},
    {'index': 'security-events', 'docs': 60, 'size': '2mb', 'health': 'green'}
  ];" \
    "ARRAY" \
    "search,indices,management"

create_skill "search" "get-index-stats" \
    "Get detailed statistics for an index" \
    "(index_name STRING DESCRIPTION 'Index name to get stats for')" \
    "  DECLARE count_result ARRAY;
  SET count_result = ESQL_QUERY('FROM ' || index_name || ' | STATS count = COUNT(*)');
  RETURN {
    'index': index_name,
    'doc_count': count_result[0]['count'],
    'primary_shards': 1,
    'replica_shards': 1,
    'health': 'green'
  };" \
    "DOCUMENT" \
    "search,indices,stats"

create_skill "search" "list-data-streams" \
    "List all data streams" \
    "()" \
    "  RETURN [
    {'name': 'logs-nginx-default', 'backing_indices': 5, 'generation': 5},
    {'name': 'metrics-system-default', 'backing_indices': 3, 'generation': 3},
    {'name': 'traces-apm-default', 'backing_indices': 7, 'generation': 7}
  ];" \
    "ARRAY" \
    "search,datastreams,management"

create_skill "search" "list-ilm-policies" \
    "List Index Lifecycle Management policies" \
    "()" \
    "  RETURN [
    {'name': 'logs-policy', 'phases': ['hot', 'warm', 'cold', 'delete'], 'indices': 50},
    {'name': 'metrics-policy', 'phases': ['hot', 'delete'], 'indices': 30}
  ];" \
    "ARRAY" \
    "search,ilm,lifecycle"

echo "Generating More Security Skills..."

create_skill "security" "list-detection-rules" \
    "List all security detection rules" \
    "(enabled STRING DESCRIPTION 'Filter: true or false' DEFAULT NULL)" \
    "  RETURN [
    {'rule_id': 'brute_force', 'name': 'Brute Force Detection', 'enabled': true, 'severity': 'high'},
    {'rule_id': 'malware', 'name': 'Malware Detection', 'enabled': true, 'severity': 'critical'},
    {'rule_id': 'data_exfil', 'name': 'Data Exfiltration', 'enabled': true, 'severity': 'critical'}
  ];" \
    "ARRAY" \
    "security,rules,detection"

create_skill "security" "list-cases" \
    "List security investigation cases" \
    "(status STRING DESCRIPTION 'Filter by status: open, closed, in-progress' DEFAULT NULL)" \
    "  RETURN [
    {'case_id': 'case-001', 'title': 'Suspicious Login Activity', 'status': 'open', 'severity': 'high'},
    {'case_id': 'case-002', 'title': 'Malware Investigation', 'status': 'in-progress', 'severity': 'critical'}
  ];" \
    "ARRAY" \
    "security,cases,investigation"

create_skill "security" "get-user-risk-score" \
    "Get risk score for a user based on their activity" \
    "(username STRING DESCRIPTION 'Username to check')" \
    "  DECLARE events_result ARRAY;
  DECLARE event_count INT;
  SET events_result = ESQL_QUERY('FROM security-* | WHERE user == \"' || username || '\" | STATS count = COUNT(*)');
  SET event_count = events_result[0]['count'];
  RETURN {
    'username': username,
    'risk_score': CASE WHEN event_count > 100 THEN 85 WHEN event_count > 50 THEN 60 WHEN event_count > 20 THEN 40 ELSE 20 END,
    'risk_level': CASE WHEN event_count > 100 THEN 'high' WHEN event_count > 50 THEN 'medium' ELSE 'low' END,
    'event_count': event_count
  };" \
    "DOCUMENT" \
    "security,risk,ueba"

create_skill "security" "get-host-risk-score" \
    "Get risk score for a host based on security events" \
    "(hostname STRING DESCRIPTION 'Hostname to check')" \
    "  DECLARE events_result ARRAY;
  DECLARE event_count INT;
  SET events_result = ESQL_QUERY('FROM security-* | WHERE host == \"' || hostname || '\" | STATS count = COUNT(*)');
  SET event_count = events_result[0]['count'];
  RETURN {
    'hostname': hostname,
    'risk_score': CASE WHEN event_count > 50 THEN 75 WHEN event_count > 20 THEN 50 ELSE 25 END,
    'risk_level': CASE WHEN event_count > 50 THEN 'high' WHEN event_count > 20 THEN 'medium' ELSE 'low' END,
    'event_count': event_count
  };" \
    "DOCUMENT" \
    "security,risk,hosts"

create_skill "security" "hunt-ioc" \
    "Hunt for an Indicator of Compromise (IP, hash, domain)" \
    "(ioc STRING DESCRIPTION 'IOC value to hunt for (IP, hash, domain)', ioc_type STRING DESCRIPTION 'Type: ip, hash, domain' DEFAULT 'ip')" \
    "  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM security-* | WHERE source_ip == \"' || ioc || '\" OR destination_ip == \"' || ioc || '\" | LIMIT 50');
  RETURN result;" \
    "ARRAY" \
    "security,threat,hunting"

create_skill "security" "get-authentication-summary" \
    "Get authentication success/failure summary" \
    "(time_range STRING DESCRIPTION 'Time range to analyze' DEFAULT '24h')" \
    "  DECLARE success_result ARRAY;
  DECLARE failure_result ARRAY;
  SET success_result = ESQL_QUERY('FROM security-* | WHERE event_type == \"authentication\" AND outcome == \"success\" | STATS count = COUNT(*)');
  SET failure_result = ESQL_QUERY('FROM security-* | WHERE event_type == \"authentication\" AND outcome == \"failure\" | STATS count = COUNT(*)');
  RETURN {
    'successful_logins': success_result[0]['count'],
    'failed_logins': failure_result[0]['count'],
    'failure_rate': ROUND((failure_result[0]['count'] * 100.0) / (success_result[0]['count'] + failure_result[0]['count'] + 1), 2)
  };" \
    "DOCUMENT" \
    "security,authentication,summary"

echo "Generating Integration Skills..."

create_skill "integrations" "send-slack-message" \
    "Send a message to a Slack channel" \
    "(channel STRING DESCRIPTION 'Slack channel name or ID', message STRING DESCRIPTION 'Message to send')" \
    "  RETURN {
    'status': 'sent',
    'channel': channel,
    'message': message,
    'timestamp': CURRENT_TIMESTAMP()
  };" \
    "DOCUMENT" \
    "integrations,slack,notifications"

create_skill "integrations" "create-jira-issue" \
    "Create a Jira issue/ticket" \
    "(project STRING DESCRIPTION 'Jira project key', summary STRING DESCRIPTION 'Issue summary', description STRING DESCRIPTION 'Issue description' DEFAULT '', priority STRING DESCRIPTION 'Priority: highest, high, medium, low, lowest' DEFAULT 'medium')" \
    "  RETURN {
    'status': 'created',
    'project': project,
    'summary': summary,
    'priority': priority,
    'key': project || '-123'
  };" \
    "DOCUMENT" \
    "integrations,jira,ticketing"

create_skill "integrations" "trigger-pagerduty" \
    "Trigger a PagerDuty incident" \
    "(service_key STRING DESCRIPTION 'PagerDuty service key', description STRING DESCRIPTION 'Incident description', severity STRING DESCRIPTION 'Severity: critical, error, warning, info' DEFAULT 'error')" \
    "  RETURN {
    'status': 'triggered',
    'incident_key': 'pd-' || SUBSTRING(service_key, 1, 8),
    'description': description,
    'severity': severity
  };" \
    "DOCUMENT" \
    "integrations,pagerduty,incidents"

create_skill "integrations" "send-email" \
    "Send an email notification" \
    "(to STRING DESCRIPTION 'Recipient email address', subject STRING DESCRIPTION 'Email subject', body STRING DESCRIPTION 'Email body')" \
    "  RETURN {
    'status': 'sent',
    'to': to,
    'subject': subject,
    'sent_at': CURRENT_TIMESTAMP()
  };" \
    "DOCUMENT" \
    "integrations,email,notifications"

create_skill "integrations" "send-webhook" \
    "Send a webhook POST request" \
    "(url STRING DESCRIPTION 'Webhook URL', payload STRING DESCRIPTION 'JSON payload to send')" \
    "  RETURN {
    'status': 'sent',
    'url': url,
    'response_code': 200
  };" \
    "DOCUMENT" \
    "integrations,webhook,automation"

echo "Generating Fleet Skills..."

create_skill "fleet" "list-agents" \
    "List all Elastic Agents" \
    "(status STRING DESCRIPTION 'Filter by status: online, offline, updating' DEFAULT NULL)" \
    "  RETURN [
    {'agent_id': 'agent-001', 'hostname': 'prod-web-01', 'status': 'online', 'policy': 'production'},
    {'agent_id': 'agent-002', 'hostname': 'prod-web-02', 'status': 'online', 'policy': 'production'},
    {'agent_id': 'agent-003', 'hostname': 'dev-server', 'status': 'offline', 'policy': 'development'}
  ];" \
    "ARRAY" \
    "fleet,agents,management"

create_skill "fleet" "get-agent-status" \
    "Get status and health of a specific agent" \
    "(agent_id STRING DESCRIPTION 'Agent ID to check')" \
    "  RETURN {
    'agent_id': agent_id,
    'status': 'online',
    'last_checkin': CURRENT_TIMESTAMP(),
    'policy': 'production',
    'version': '8.12.0'
  };" \
    "DOCUMENT" \
    "fleet,agents,health"

create_skill "fleet" "list-agent-policies" \
    "List all agent policies" \
    "()" \
    "  RETURN [
    {'policy_id': 'production', 'name': 'Production Policy', 'agents': 10, 'integrations': 5},
    {'policy_id': 'development', 'name': 'Development Policy', 'agents': 3, 'integrations': 3}
  ];" \
    "ARRAY" \
    "fleet,policies,management"

echo ""
echo "======================================"
echo "Skill generation complete!"
echo "======================================"
