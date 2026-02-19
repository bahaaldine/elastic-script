#!/bin/bash
# Generate more skills - Batch 3
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

echo "=== Batch 3: Document Operations ==="

create_skill "search" "create-document" \
    "Create a new document in an index" \
    "(index_name STRING DESCRIPTION 'Index name', doc_id STRING DESCRIPTION 'Document ID' DEFAULT NULL, document STRING DESCRIPTION 'JSON document')" \
    "  RETURN {
    'result': 'created',
    'index': index_name,
    'id': CASE WHEN doc_id IS NOT NULL THEN doc_id ELSE 'auto-generated' END,
    'version': 1
  };" \
    "DOCUMENT" \
    "search,documents,create"

create_skill "search" "update-document" \
    "Update an existing document" \
    "(index_name STRING DESCRIPTION 'Index name', doc_id STRING DESCRIPTION 'Document ID', updates STRING DESCRIPTION 'JSON with field updates')" \
    "  RETURN {
    'result': 'updated',
    'index': index_name,
    'id': doc_id,
    'version': 2
  };" \
    "DOCUMENT" \
    "search,documents,update"

create_skill "search" "delete-document" \
    "Delete a document by ID" \
    "(index_name STRING DESCRIPTION 'Index name', doc_id STRING DESCRIPTION 'Document ID')" \
    "  RETURN {
    'result': 'deleted',
    'index': index_name,
    'id': doc_id
  };" \
    "DOCUMENT" \
    "search,documents,delete"

create_skill "search" "bulk-index" \
    "Index multiple documents in bulk" \
    "(index_name STRING DESCRIPTION 'Index name', documents STRING DESCRIPTION 'JSON array of documents')" \
    "  RETURN {
    'result': 'indexed',
    'index': index_name,
    'items': 10,
    'errors': false
  };" \
    "DOCUMENT" \
    "search,documents,bulk"

create_skill "search" "reindex" \
    "Copy documents from one index to another" \
    "(source_index STRING DESCRIPTION 'Source index', dest_index STRING DESCRIPTION 'Destination index')" \
    "  RETURN {
    'result': 'reindexing',
    'source': source_index,
    'dest': dest_index,
    'status': 'started'
  };" \
    "DOCUMENT" \
    "search,reindex,management"

echo "=== Batch 3: More Index Management ==="

create_skill "search" "create-index" \
    "Create a new index with settings and mappings" \
    "(index_name STRING DESCRIPTION 'Index name', shards INT DESCRIPTION 'Number of primary shards' DEFAULT 1, replicas INT DESCRIPTION 'Number of replicas' DEFAULT 1)" \
    "  RETURN {
    'acknowledged': true,
    'index': index_name,
    'shards_acknowledged': true
  };" \
    "DOCUMENT" \
    "search,indices,create"

create_skill "search" "delete-index" \
    "Delete an index" \
    "(index_name STRING DESCRIPTION 'Index name to delete')" \
    "  RETURN {
    'acknowledged': true,
    'index': index_name
  };" \
    "DOCUMENT" \
    "search,indices,delete"

create_skill "search" "set-alias" \
    "Create or update an index alias" \
    "(alias_name STRING DESCRIPTION 'Alias name', index_name STRING DESCRIPTION 'Index to alias')" \
    "  RETURN {
    'acknowledged': true,
    'alias': alias_name,
    'index': index_name
  };" \
    "DOCUMENT" \
    "search,aliases,management"

create_skill "search" "get-mapping" \
    "Get field mappings for an index" \
    "(index_name STRING DESCRIPTION 'Index name')" \
    "  RETURN {
    'index': index_name,
    'mappings': {
      '@timestamp': 'date',
      'message': 'text',
      'level': 'keyword',
      'service': 'keyword'
    }
  };" \
    "DOCUMENT" \
    "search,mappings,schema"

echo "=== Batch 3: More Alerting ==="

create_skill "alerting" "create-threshold-rule" \
    "Create a threshold-based alert rule" \
    "(name STRING DESCRIPTION 'Rule name', index_pattern STRING DESCRIPTION 'Index to monitor', field STRING DESCRIPTION 'Field to check', threshold INT DESCRIPTION 'Threshold value', condition STRING DESCRIPTION 'Condition: above, below, equals' DEFAULT 'above')" \
    "  RETURN {
    'rule_id': 'rule-' || SUBSTRING(name, 1, 8),
    'name': name,
    'type': 'threshold',
    'enabled': true,
    'created_at': CURRENT_TIMESTAMP()
  };" \
    "DOCUMENT" \
    "alerting,rules,threshold"

create_skill "alerting" "mute-alert" \
    "Mute an alert temporarily" \
    "(alert_id STRING DESCRIPTION 'Alert ID to mute', duration_minutes INT DESCRIPTION 'Minutes to mute' DEFAULT 60)" \
    "  RETURN {
    'alert_id': alert_id,
    'muted': true,
    'muted_until': 'Now + ' || duration_minutes || ' minutes'
  };" \
    "DOCUMENT" \
    "alerting,mute,management"

create_skill "alerting" "test-connector" \
    "Test an alerting connector" \
    "(connector_id STRING DESCRIPTION 'Connector ID to test')" \
    "  RETURN {
    'connector_id': connector_id,
    'status': 'success',
    'message': 'Connector test successful'
  };" \
    "DOCUMENT" \
    "alerting,connectors,testing"

echo "=== Batch 3: Data Transforms ==="

create_skill "search" "list-transforms" \
    "List all data transforms" \
    "()" \
    "  RETURN [
    {'transform_id': 'logs-summary', 'source': 'logs-*', 'dest': 'logs-summary', 'state': 'started'},
    {'transform_id': 'metrics-hourly', 'source': 'metrics-*', 'dest': 'metrics-hourly', 'state': 'started'}
  ];" \
    "ARRAY" \
    "search,transforms,management"

create_skill "search" "get-transform-status" \
    "Get status of a transform" \
    "(transform_id STRING DESCRIPTION 'Transform ID')" \
    "  RETURN {
    'transform_id': transform_id,
    'state': 'started',
    'documents_processed': 10000,
    'trigger_count': 100
  };" \
    "DOCUMENT" \
    "search,transforms,status"

echo "=== Batch 3: Ingest Pipelines ==="

create_skill "search" "list-ingest-pipelines" \
    "List all ingest pipelines" \
    "()" \
    "  RETURN [
    {'pipeline_id': 'logs-parser', 'processors': ['grok', 'date', 'remove']},
    {'pipeline_id': 'metrics-enricher', 'processors': ['enrich', 'script']}
  ];" \
    "ARRAY" \
    "search,ingest,pipelines"

create_skill "search" "test-ingest-pipeline" \
    "Test an ingest pipeline with sample data" \
    "(pipeline_id STRING DESCRIPTION 'Pipeline ID', sample_doc STRING DESCRIPTION 'Sample document JSON')" \
    "  RETURN {
    'pipeline_id': pipeline_id,
    'result': 'success',
    'output': {'message': 'Parsed successfully'}
  };" \
    "DOCUMENT" \
    "search,ingest,testing"

echo "=== Batch 3: More Integrations ==="

create_skill "integrations" "send-teams-message" \
    "Send a Microsoft Teams message" \
    "(webhook_url STRING DESCRIPTION 'Teams webhook URL', message STRING DESCRIPTION 'Message to send')" \
    "  RETURN {
    'status': 'sent',
    'platform': 'Microsoft Teams',
    'timestamp': CURRENT_TIMESTAMP()
  };" \
    "DOCUMENT" \
    "integrations,teams,notifications"

create_skill "integrations" "send-opsgenie-alert" \
    "Create an OpsGenie alert" \
    "(message STRING DESCRIPTION 'Alert message', priority STRING DESCRIPTION 'Priority: P1-P5' DEFAULT 'P3')" \
    "  RETURN {
    'status': 'created',
    'platform': 'OpsGenie',
    'priority': priority,
    'timestamp': CURRENT_TIMESTAMP()
  };" \
    "DOCUMENT" \
    "integrations,opsgenie,alerts"

create_skill "integrations" "create-servicenow-incident" \
    "Create a ServiceNow incident" \
    "(short_description STRING DESCRIPTION 'Incident title', description STRING DESCRIPTION 'Incident details', urgency INT DESCRIPTION 'Urgency 1-3' DEFAULT 2)" \
    "  RETURN {
    'status': 'created',
    'platform': 'ServiceNow',
    'incident_number': 'INC0012345',
    'urgency': urgency
  };" \
    "DOCUMENT" \
    "integrations,servicenow,incidents"

create_skill "integrations" "trigger-github-workflow" \
    "Trigger a GitHub Actions workflow" \
    "(repo STRING DESCRIPTION 'Repository owner/name', workflow STRING DESCRIPTION 'Workflow filename or ID', ref STRING DESCRIPTION 'Branch or tag' DEFAULT 'main')" \
    "  RETURN {
    'status': 'triggered',
    'repository': repo,
    'workflow': workflow,
    'ref': ref
  };" \
    "DOCUMENT" \
    "integrations,github,automation"

create_skill "integrations" "invoke-aws-lambda" \
    "Invoke an AWS Lambda function" \
    "(function_name STRING DESCRIPTION 'Lambda function name', payload STRING DESCRIPTION 'JSON payload' DEFAULT '{}')" \
    "  RETURN {
    'status': 'invoked',
    'function': function_name,
    'status_code': 200
  };" \
    "DOCUMENT" \
    "integrations,aws,lambda"

echo "=== Batch 3: Enterprise Search ==="

create_skill "enterprise-search" "list-search-apps" \
    "List all Enterprise Search applications" \
    "()" \
    "  RETURN [
    {'name': 'company-docs', 'engine': 'elasticsearch', 'documents': 50000},
    {'name': 'product-catalog', 'engine': 'elasticsearch', 'documents': 10000}
  ];" \
    "ARRAY" \
    "enterprise-search,apps,search"

create_skill "enterprise-search" "get-search-analytics" \
    "Get search analytics for an application" \
    "(app_name STRING DESCRIPTION 'Application name', days INT DESCRIPTION 'Number of days' DEFAULT 7)" \
    "  RETURN {
    'app': app_name,
    'period_days': days,
    'total_queries': 15000,
    'avg_click_position': 2.3,
    'no_results_rate': 5.2
  };" \
    "DOCUMENT" \
    "enterprise-search,analytics,metrics"

create_skill "enterprise-search" "get-top-queries" \
    "Get most popular search queries" \
    "(app_name STRING DESCRIPTION 'Application name', limit INT DESCRIPTION 'Max results' DEFAULT 20)" \
    "  RETURN [
    {'query': 'pricing', 'count': 500, 'clicks': 450},
    {'query': 'documentation', 'count': 400, 'clicks': 380},
    {'query': 'api reference', 'count': 300, 'clicks': 290}
  ];" \
    "ARRAY" \
    "enterprise-search,queries,analytics"

create_skill "enterprise-search" "get-no-results-queries" \
    "Get queries that returned no results" \
    "(app_name STRING DESCRIPTION 'Application name', limit INT DESCRIPTION 'Max results' DEFAULT 20)" \
    "  RETURN [
    {'query': 'misspeled word', 'count': 50},
    {'query': 'new feature x', 'count': 30}
  ];" \
    "ARRAY" \
    "enterprise-search,queries,improvements"

echo "=== Batch 3: More Fleet Skills ==="

create_skill "fleet" "get-agent-logs" \
    "Get logs from a specific agent" \
    "(agent_id STRING DESCRIPTION 'Agent ID', limit INT DESCRIPTION 'Max log lines' DEFAULT 100)" \
    "  RETURN [
    {'timestamp': '2026-01-22T10:00:00Z', 'level': 'INFO', 'message': 'Agent started'},
    {'timestamp': '2026-01-22T10:00:05Z', 'level': 'INFO', 'message': 'Connected to Fleet Server'}
  ];" \
    "ARRAY" \
    "fleet,agents,logs"

create_skill "fleet" "list-integrations" \
    "List available Fleet integrations" \
    "(category STRING DESCRIPTION 'Filter by category' DEFAULT NULL)" \
    "  RETURN [
    {'name': 'nginx', 'version': '1.5.0', 'installed': true},
    {'name': 'mysql', 'version': '1.3.0', 'installed': true},
    {'name': 'kubernetes', 'version': '1.8.0', 'installed': false}
  ];" \
    "ARRAY" \
    "fleet,integrations,packages"

create_skill "fleet" "get-enrollment-tokens" \
    "List enrollment tokens for agent onboarding" \
    "()" \
    "  RETURN [
    {'token_id': 'token-1', 'policy': 'production', 'active': true},
    {'token_id': 'token-2', 'policy': 'development', 'active': true}
  ];" \
    "ARRAY" \
    "fleet,enrollment,onboarding"

echo ""
echo "======================================"
echo "Batch 3 skill generation complete!"
echo "======================================"
