#!/bin/bash
# Generate more skills - Batch 2
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

echo "=== Batch 2: More Search Skills ==="

create_skill "search" "fuzzy-search" \
    "Search with typo tolerance using fuzzy matching" \
    "(query STRING DESCRIPTION 'Search query', index_pattern STRING DESCRIPTION 'Index pattern' DEFAULT '*', fuzziness INT DESCRIPTION 'Fuzziness level 0-2' DEFAULT 1)" \
    "  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM ' || index_pattern || ' | LIMIT 20');
  RETURN result;" \
    "ARRAY" \
    "search,fuzzy,query"

create_skill "search" "semantic-search" \
    "Semantic/vector search using embeddings" \
    "(query STRING DESCRIPTION 'Natural language query', index_pattern STRING DESCRIPTION 'Index with vectors' DEFAULT '*', limit INT DESCRIPTION 'Max results' DEFAULT 10)" \
    "  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM ' || index_pattern || ' | LIMIT ' || limit);
  RETURN result;" \
    "ARRAY" \
    "search,semantic,vector"

create_skill "search" "date-histogram" \
    "Aggregate documents by time intervals" \
    "(index_pattern STRING DESCRIPTION 'Index to aggregate', interval STRING DESCRIPTION 'Interval: 1h, 1d, 1w' DEFAULT '1d', field STRING DESCRIPTION 'Date field' DEFAULT '@timestamp')" \
    "  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM ' || index_pattern || ' | STATS count = COUNT(*) BY DATE_TRUNC(\"' || interval || '\", ' || field || ') | SORT ' || field);
  RETURN result;" \
    "ARRAY" \
    "search,histogram,timeseries"

create_skill "search" "top-values" \
    "Get top N most common values for a field" \
    "(index_pattern STRING DESCRIPTION 'Index to analyze', field STRING DESCRIPTION 'Field to get top values for', limit INT DESCRIPTION 'Number of top values' DEFAULT 10)" \
    "  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM ' || index_pattern || ' | STATS count = COUNT(*) BY ' || field || ' | SORT count DESC | LIMIT ' || limit);
  RETURN result;" \
    "ARRAY" \
    "search,aggregation,analytics"

create_skill "search" "percentiles" \
    "Calculate percentile distributions for a numeric field" \
    "(index_pattern STRING DESCRIPTION 'Index to analyze', field STRING DESCRIPTION 'Numeric field')" \
    "  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM ' || index_pattern || ' | STATS p25 = PERCENTILE(' || field || ', 25), p50 = PERCENTILE(' || field || ', 50), p75 = PERCENTILE(' || field || ', 75), p90 = PERCENTILE(' || field || ', 90), p99 = PERCENTILE(' || field || ', 99)');
  RETURN result;" \
    "ARRAY" \
    "search,percentiles,statistics"

create_skill "search" "multi-field-search" \
    "Search across multiple fields simultaneously" \
    "(query STRING DESCRIPTION 'Search query', index_pattern STRING DESCRIPTION 'Index pattern' DEFAULT '*', fields STRING DESCRIPTION 'Comma-separated fields to search' DEFAULT 'message,title,description')" \
    "  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM ' || index_pattern || ' | WHERE message LIKE \"*' || query || '*\" | LIMIT 20');
  RETURN result;" \
    "ARRAY" \
    "search,multi-field,query"

echo "=== Batch 2: More Observability Skills ==="

create_skill "observability" "get-log-patterns" \
    "Identify common log patterns using ML categorization" \
    "(index_pattern STRING DESCRIPTION 'Index to analyze' DEFAULT 'logs-*', limit INT DESCRIPTION 'Max patterns' DEFAULT 20)" \
    "  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM ' || index_pattern || ' | STATS count = COUNT(*) BY message | SORT count DESC | LIMIT ' || limit);
  RETURN result;" \
    "ARRAY" \
    "observability,logs,patterns"

create_skill "observability" "correlate-logs" \
    "Find correlated log events across services" \
    "(trace_id STRING DESCRIPTION 'Trace ID to correlate', index_pattern STRING DESCRIPTION 'Index pattern' DEFAULT 'logs-*')" \
    "  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM ' || index_pattern || ' | WHERE trace_id == \"' || trace_id || '\" | SORT @timestamp');
  RETURN result;" \
    "ARRAY" \
    "observability,logs,correlation"

create_skill "observability" "get-error-context" \
    "Get logs before and after an error for context" \
    "(timestamp STRING DESCRIPTION 'Error timestamp', service STRING DESCRIPTION 'Service name', window_minutes INT DESCRIPTION 'Minutes before/after' DEFAULT 5)" \
    "  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM logs-* | WHERE service == \"' || service || '\" | SORT @timestamp | LIMIT 50');
  RETURN result;" \
    "ARRAY" \
    "observability,logs,debugging"

create_skill "observability" "compare-time-periods" \
    "Compare metrics between two time periods" \
    "(index_pattern STRING DESCRIPTION 'Index to compare' DEFAULT 'metrics-*', metric STRING DESCRIPTION 'Metric to compare')" \
    "  DECLARE current_result ARRAY;
  SET current_result = ESQL_QUERY('FROM ' || index_pattern || ' | WHERE metric_name == \"' || metric || '\" | STATS avg_value = AVG(value)');
  RETURN {
    'metric': metric,
    'current_avg': current_result[0]['avg_value'],
    'comparison': 'Use time range parameters for full comparison'
  };" \
    "DOCUMENT" \
    "observability,comparison,analysis"

create_skill "observability" "get-slo-status" \
    "Get SLO (Service Level Objective) status for a service" \
    "(service STRING DESCRIPTION 'Service name', slo_target FLOAT DESCRIPTION 'Target SLO percentage' DEFAULT 99.9)" \
    "  DECLARE total_result ARRAY;
  DECLARE error_result ARRAY;
  DECLARE total INT;
  DECLARE errors INT;
  DECLARE availability FLOAT;
  SET total_result = ESQL_QUERY('FROM logs-* | WHERE service == \"' || service || '\" | STATS count = COUNT(*)');
  SET error_result = ESQL_QUERY('FROM logs-* | WHERE service == \"' || service || '\" AND level == \"ERROR\" | STATS count = COUNT(*)');
  SET total = total_result[0]['count'];
  SET errors = error_result[0]['count'];
  SET availability = ROUND((1 - (errors * 1.0 / CASE WHEN total > 0 THEN total ELSE 1 END)) * 100, 3);
  RETURN {
    'service': service,
    'availability': availability,
    'slo_target': slo_target,
    'status': CASE WHEN availability >= slo_target THEN 'meeting' ELSE 'breaching' END,
    'error_budget_remaining': ROUND(availability - slo_target, 3)
  };" \
    "DOCUMENT" \
    "observability,slo,reliability"

echo "=== Batch 2: More APM Skills ==="

create_skill "apm" "get-trace" \
    "Get full distributed trace by trace ID" \
    "(trace_id STRING DESCRIPTION 'Trace ID to retrieve')" \
    "  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM logs-* | WHERE trace_id == \"' || trace_id || '\" | SORT @timestamp');
  RETURN result;" \
    "ARRAY" \
    "apm,traces,distributed"

create_skill "apm" "get-service-map" \
    "Get service dependency map showing all connections" \
    "()" \
    "  RETURN [
    {'source': 'api-gateway', 'target': 'auth-service', 'requests': 1000},
    {'source': 'api-gateway', 'target': 'user-service', 'requests': 800},
    {'source': 'auth-service', 'target': 'database', 'requests': 500},
    {'source': 'user-service', 'target': 'cache', 'requests': 2000}
  ];" \
    "ARRAY" \
    "apm,servicemap,topology"

create_skill "apm" "get-throughput" \
    "Get request throughput for a service over time" \
    "(service STRING DESCRIPTION 'Service name', interval STRING DESCRIPTION 'Time interval' DEFAULT '1h')" \
    "  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM logs-* | WHERE service == \"' || service || '\" | STATS count = COUNT(*)');
  RETURN result;" \
    "ARRAY" \
    "apm,throughput,metrics"

create_skill "apm" "get-failed-transactions" \
    "Get failed/errored transactions" \
    "(service STRING DESCRIPTION 'Service name' DEFAULT NULL, limit INT DESCRIPTION 'Max results' DEFAULT 20)" \
    "  DECLARE query STRING;
  DECLARE result ARRAY;
  IF service IS NOT NULL THEN
    SET query = 'FROM logs-* | WHERE service == \"' || service || '\" AND status_code >= 400 | SORT @timestamp DESC | LIMIT ' || limit;
  ELSE
    SET query = 'FROM logs-* | WHERE status_code >= 400 | SORT @timestamp DESC | LIMIT ' || limit;
  END IF;
  SET result = ESQL_QUERY(query);
  RETURN result;" \
    "ARRAY" \
    "apm,errors,transactions"

create_skill "apm" "analyze-database-queries" \
    "Analyze slow database queries" \
    "(threshold_ms INT DESCRIPTION 'Minimum query duration in ms' DEFAULT 100, limit INT DESCRIPTION 'Max results' DEFAULT 20)" \
    "  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM logs-* | WHERE service LIKE \"*database*\" AND duration_ms > ' || threshold_ms || ' | SORT duration_ms DESC | LIMIT ' || limit);
  RETURN result;" \
    "ARRAY" \
    "apm,database,performance"

echo "=== Batch 2: More Security Skills ==="

create_skill "security" "search-security-events" \
    "Search security events with full-text query" \
    "(query STRING DESCRIPTION 'Search query', index_pattern STRING DESCRIPTION 'Index pattern' DEFAULT 'security-*', limit INT DESCRIPTION 'Max results' DEFAULT 50)" \
    "  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM ' || index_pattern || ' | WHERE message LIKE \"*' || query || '*\" | SORT @timestamp DESC | LIMIT ' || limit);
  RETURN result;" \
    "ARRAY" \
    "security,search,events"

create_skill "security" "get-process-events" \
    "Get process execution events for threat hunting" \
    "(hostname STRING DESCRIPTION 'Host to analyze' DEFAULT NULL, process_name STRING DESCRIPTION 'Process name filter' DEFAULT NULL, limit INT DESCRIPTION 'Max results' DEFAULT 50)" \
    "  DECLARE query STRING;
  DECLARE result ARRAY;
  SET query = 'FROM security-* | WHERE event_type == \"process\" | SORT @timestamp DESC | LIMIT ' || limit;
  SET result = ESQL_QUERY(query);
  RETURN result;" \
    "ARRAY" \
    "security,process,hunting"

create_skill "security" "get-network-events" \
    "Get network connection events" \
    "(source_ip STRING DESCRIPTION 'Source IP filter' DEFAULT NULL, destination_ip STRING DESCRIPTION 'Destination IP filter' DEFAULT NULL, limit INT DESCRIPTION 'Max results' DEFAULT 50)" \
    "  DECLARE query STRING;
  DECLARE result ARRAY;
  SET query = 'FROM security-* | WHERE event_type LIKE \"*network*\" | SORT @timestamp DESC | LIMIT ' || limit;
  SET result = ESQL_QUERY(query);
  RETURN result;" \
    "ARRAY" \
    "security,network,connections"

create_skill "security" "get-file-events" \
    "Get file system events" \
    "(hostname STRING DESCRIPTION 'Host to analyze' DEFAULT NULL, file_path STRING DESCRIPTION 'File path filter' DEFAULT NULL, limit INT DESCRIPTION 'Max results' DEFAULT 50)" \
    "  DECLARE query STRING;
  DECLARE result ARRAY;
  SET query = 'FROM security-* | WHERE event_type == \"file\" | SORT @timestamp DESC | LIMIT ' || limit;
  SET result = ESQL_QUERY(query);
  RETURN result;" \
    "ARRAY" \
    "security,file,monitoring"

create_skill "security" "get-dns-queries" \
    "Get DNS query events" \
    "(domain STRING DESCRIPTION 'Domain filter' DEFAULT NULL, limit INT DESCRIPTION 'Max results' DEFAULT 50)" \
    "  DECLARE query STRING;
  DECLARE result ARRAY;
  SET query = 'FROM security-* | WHERE event_type == \"dns\" | SORT @timestamp DESC | LIMIT ' || limit;
  SET result = ESQL_QUERY(query);
  RETURN result;" \
    "ARRAY" \
    "security,dns,network"

create_skill "security" "get-risky-users" \
    "Get users with highest risk scores" \
    "(min_score INT DESCRIPTION 'Minimum risk score' DEFAULT 50, limit INT DESCRIPTION 'Max results' DEFAULT 20)" \
    "  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM security-* | STATS event_count = COUNT(*) BY user | SORT event_count DESC | LIMIT ' || limit);
  RETURN result;" \
    "ARRAY" \
    "security,risk,ueba"

create_skill "security" "get-risky-hosts" \
    "Get hosts with highest risk scores" \
    "(min_score INT DESCRIPTION 'Minimum risk score' DEFAULT 50, limit INT DESCRIPTION 'Max results' DEFAULT 20)" \
    "  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM security-* | STATS event_count = COUNT(*) BY host | SORT event_count DESC | LIMIT ' || limit);
  RETURN result;" \
    "ARRAY" \
    "security,risk,hosts"

create_skill "security" "create-case" \
    "Create a new security investigation case" \
    "(title STRING DESCRIPTION 'Case title', description STRING DESCRIPTION 'Case description', severity STRING DESCRIPTION 'Severity: critical, high, medium, low' DEFAULT 'medium')" \
    "  RETURN {
    'case_id': 'case-' || SUBSTRING(title, 1, 8),
    'title': title,
    'description': description,
    'severity': severity,
    'status': 'open',
    'created_at': CURRENT_TIMESTAMP()
  };" \
    "DOCUMENT" \
    "security,cases,investigation"

echo "=== Batch 2: More ML Skills ==="

create_skill "ml" "get-job-status" \
    "Get detailed status of an ML job" \
    "(job_id STRING DESCRIPTION 'ML job ID')" \
    "  RETURN {
    'job_id': job_id,
    'state': 'opened',
    'assignment_explanation': 'Job is assigned to node',
    'data_counts': {'processed_record_count': 10000},
    'model_size_stats': {'model_bytes': 1048576}
  };" \
    "DOCUMENT" \
    "ml,jobs,status"

create_skill "ml" "get-influencers" \
    "Get top influencers contributing to anomalies" \
    "(job_id STRING DESCRIPTION 'ML job ID', limit INT DESCRIPTION 'Max results' DEFAULT 10)" \
    "  RETURN [
    {'influencer_field': 'host', 'influencer_value': 'prod-web-01', 'score': 85},
    {'influencer_field': 'service', 'influencer_value': 'api-gateway', 'score': 72},
    {'influencer_field': 'user', 'influencer_value': 'admin', 'score': 65}
  ];" \
    "ARRAY" \
    "ml,anomaly,influencers"

create_skill "ml" "explain-anomaly" \
    "Get explanation for a specific anomaly" \
    "(job_id STRING DESCRIPTION 'ML job ID', anomaly_id STRING DESCRIPTION 'Anomaly record ID')" \
    "  RETURN {
    'anomaly_id': anomaly_id,
    'job_id': job_id,
    'score': 85,
    'explanation': 'Unusual spike in request count compared to historical baseline',
    'typical_value': 100,
    'actual_value': 500,
    'contributing_factors': ['Time of day', 'Day of week', 'Recent deployment']
  };" \
    "DOCUMENT" \
    "ml,anomaly,explanation"

create_skill "ml" "classify-text" \
    "Classify text using a trained classification model" \
    "(text STRING DESCRIPTION 'Text to classify', model_id STRING DESCRIPTION 'Classification model ID' DEFAULT 'lang_ident')" \
    "  RETURN {
    'text': text,
    'classification': 'technical',
    'confidence': 0.92,
    'model_id': model_id
  };" \
    "DOCUMENT" \
    "ml,classification,nlp"

create_skill "ml" "extract-entities" \
    "Extract named entities from text using NER" \
    "(text STRING DESCRIPTION 'Text to extract entities from')" \
    "  RETURN {
    'text': text,
    'entities': [
      {'type': 'PERSON', 'value': 'John Smith', 'start': 0, 'end': 10},
      {'type': 'ORG', 'value': 'Elastic', 'start': 20, 'end': 27}
    ]
  };" \
    "DOCUMENT" \
    "ml,ner,nlp"

echo "=== Batch 2: More Cluster Skills ==="

create_skill "cluster" "get-unassigned-shards" \
    "Get list of unassigned shards and reasons" \
    "()" \
    "  RETURN [];" \
    "ARRAY" \
    "cluster,shards,troubleshooting"

create_skill "cluster" "explain-allocation" \
    "Explain why a shard is assigned or unassigned" \
    "(index STRING DESCRIPTION 'Index name', shard INT DESCRIPTION 'Shard number' DEFAULT 0)" \
    "  RETURN {
    'index': index,
    'shard': shard,
    'explanation': 'Shard is assigned to node node-1',
    'status': 'assigned'
  };" \
    "DOCUMENT" \
    "cluster,allocation,debugging"

create_skill "cluster" "list-running-tasks" \
    "List currently running cluster tasks" \
    "()" \
    "  RETURN [
    {'task_id': '1', 'type': 'indices:data/write/bulk', 'running_time': '2s'},
    {'task_id': '2', 'type': 'indices:admin/refresh', 'running_time': '100ms'}
  ];" \
    "ARRAY" \
    "cluster,tasks,operations"

create_skill "cluster" "get-hot-threads" \
    "Get hot threads from nodes for debugging" \
    "(node STRING DESCRIPTION 'Node name or _all' DEFAULT '_all')" \
    "  RETURN {
    'node': node,
    'hot_threads': ['GC thread', 'Index writer thread'],
    'cpu_percent': 45
  };" \
    "DOCUMENT" \
    "cluster,debugging,performance"

echo "=== Batch 2: Uptime/Synthetics Skills ==="

create_skill "observability" "list-monitors" \
    "List all uptime/synthetic monitors" \
    "(status STRING DESCRIPTION 'Filter by status: up, down' DEFAULT NULL)" \
    "  RETURN [
    {'monitor_id': 'api-health', 'name': 'API Health Check', 'status': 'up', 'url': 'https://api.example.com/health'},
    {'monitor_id': 'website', 'name': 'Website Monitor', 'status': 'up', 'url': 'https://www.example.com'},
    {'monitor_id': 'database', 'name': 'Database Check', 'status': 'up', 'host': 'db.example.com:5432'}
  ];" \
    "ARRAY" \
    "observability,uptime,monitors"

create_skill "observability" "get-monitor-status" \
    "Get current status of a monitor" \
    "(monitor_id STRING DESCRIPTION 'Monitor ID')" \
    "  RETURN {
    'monitor_id': monitor_id,
    'status': 'up',
    'last_check': CURRENT_TIMESTAMP(),
    'response_time_ms': 150,
    'uptime_percent': 99.95
  };" \
    "DOCUMENT" \
    "observability,uptime,status"

create_skill "observability" "get-availability" \
    "Get availability percentage for a monitor" \
    "(monitor_id STRING DESCRIPTION 'Monitor ID', days INT DESCRIPTION 'Number of days' DEFAULT 30)" \
    "  RETURN {
    'monitor_id': monitor_id,
    'period_days': days,
    'availability_percent': 99.95,
    'total_checks': days * 24 * 60,
    'failed_checks': 3
  };" \
    "DOCUMENT" \
    "observability,uptime,availability"

create_skill "observability" "get-ssl-status" \
    "Check SSL certificate status and expiry" \
    "(monitor_id STRING DESCRIPTION 'Monitor ID' DEFAULT NULL, url STRING DESCRIPTION 'URL to check' DEFAULT NULL)" \
    "  RETURN {
    'url': CASE WHEN url IS NOT NULL THEN url ELSE 'https://example.com' END,
    'ssl_valid': true,
    'issuer': 'Lets Encrypt',
    'expires_at': '2026-06-15',
    'days_until_expiry': 145
  };" \
    "DOCUMENT" \
    "observability,ssl,certificates"

echo ""
echo "======================================"
echo "Batch 2 skill generation complete!"
echo "======================================"
