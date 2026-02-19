#!/bin/bash
# Comprehensive test suite for all Moltler skills via MCP
# Tests each skill by calling it through the MCP endpoint

set -uo pipefail

ES_URL="${ES_URL:-http://localhost:9200}"
ES_USER="${ES_USER:-elastic-admin}"
ES_PASS="${ES_PASS:-elastic-password}"
MCP_ENDPOINT="$ES_URL/_escript/mcp"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

PASSED=0
FAILED=0
SKIPPED=0
TOTAL=0

mcp_call() {
    local tool="$1"
    local args="$2"
    
    curl -s -u "$ES_USER:$ES_PASS" "$MCP_ENDPOINT" \
        -H "Content-Type: application/json" \
        -d "{
            \"jsonrpc\": \"2.0\",
            \"id\": 1,
            \"method\": \"tools/call\",
            \"params\": {
                \"name\": \"$tool\",
                \"arguments\": $args
            }
        }"
}

test_skill() {
    local skill="$1"
    local args="${2:-{}}"
    local expect_error="${3:-false}"
    
    TOTAL=$((TOTAL+1))
    
    local result
    result=$(mcp_call "$skill" "$args")
    
    if echo "$result" | grep -q '"error"'; then
        if [ "$expect_error" = "true" ]; then
            echo -e "${YELLOW}[SKIP]${NC} $skill (expected error)"
            SKIPPED=$((SKIPPED+1))
        else
            echo -e "${RED}[FAIL]${NC} $skill"
            echo "  Error: $(echo "$result" | jq -r '.error.message // .error // "Unknown error"' 2>/dev/null | head -c 100)"
            FAILED=$((FAILED+1))
        fi
    else
        echo -e "${GREEN}[PASS]${NC} $skill"
        PASSED=$((PASSED+1))
    fi
}

echo "========================================"
echo "Moltler Skills Test Suite"
echo "========================================"
echo ""
echo "Testing against: $ES_URL"
echo ""

# Get list of all available skills
echo "Fetching skill list..."
SKILLS=$(curl -s -u "$ES_USER:$ES_PASS" "$MCP_ENDPOINT" \
    -H "Content-Type: application/json" \
    -d '{"jsonrpc": "2.0", "id": 1, "method": "tools/list"}' | \
    jq -r '.result.tools[].name' 2>/dev/null | sort)

SKILL_COUNT=$(echo "$SKILLS" | wc -l | tr -d ' ')
echo "Found $SKILL_COUNT skills"
echo ""
echo "========================================"
echo "Running Tests..."
echo "========================================"
echo ""

# Test each skill with minimal/default parameters
for skill in $SKILLS; do
    case "$skill" in
        # Meta skills
        list_all_skills) test_skill "$skill" '{}' ;;
        explain_skill) test_skill "$skill" '{"skill_name": "list_all_skills"}' ;;
        recommend_skills) test_skill "$skill" '{"context": "debug errors"}' ;;
        search_skills) test_skill "$skill" '{"query": "error"}' ;;
        list_skills_by_category) test_skill "$skill" '{"category": "observability"}' ;;
        
        # Observability
        get_recent_errors) test_skill "$skill" '{"index_pattern": "logs-*"}' ;;
        count_logs_by_level) test_skill "$skill" '{"index_pattern": "logs-*"}' ;;
        search_logs) test_skill "$skill" '{"query": "error", "index_pattern": "logs-*"}' ;;
        error_rate) test_skill "$skill" '{"index_pattern": "logs-*"}' ;;
        service_health) test_skill "$skill" '{"service": "api"}' ;;
        slow_requests) test_skill "$skill" '{"index_pattern": "logs-*"}' ;;
        get_log_patterns) test_skill "$skill" '{"index_pattern": "logs-*"}' ;;
        correlate_logs) test_skill "$skill" '{"trace_id": "abc123"}' ;;
        get_error_context) test_skill "$skill" '{"timestamp": "now", "service": "api"}' ;;
        compare_time_periods) test_skill "$skill" '{"metric": "cpu"}' ;;
        get_slo_status) test_skill "$skill" '{"service": "api"}' ;;
        list_monitors) test_skill "$skill" '{}' ;;
        get_monitor_status) test_skill "$skill" '{"monitor_id": "test"}' ;;
        get_availability) test_skill "$skill" '{"monitor_id": "test"}' ;;
        get_ssl_status) test_skill "$skill" '{}' ;;
        
        # APM
        list_services) test_skill "$skill" '{}' ;;
        get_service_health) test_skill "$skill" '{"service": "api"}' ;;
        get_slow_transactions) test_skill "$skill" '{}' ;;
        get_error_groups) test_skill "$skill" '{"service": "api"}' ;;
        get_service_dependencies) test_skill "$skill" '{"service": "api"}' ;;
        get_latency_percentiles) test_skill "$skill" '{"service": "api"}' ;;
        get_trace) test_skill "$skill" '{"trace_id": "abc123"}' ;;
        get_service_map) test_skill "$skill" '{}' ;;
        get_throughput) test_skill "$skill" '{"service": "api"}' ;;
        get_failed_transactions) test_skill "$skill" '{}' ;;
        analyze_database_queries) test_skill "$skill" '{}' ;;
        
        # Metrics
        list_hosts) test_skill "$skill" '{}' ;;
        get_host_metrics) test_skill "$skill" '{"hostname": "prod-1"}' ;;
        get_container_metrics) test_skill "$skill" '{}' ;;
        get_disk_usage) test_skill "$skill" '{}' ;;
        get_network_metrics) test_skill "$skill" '{}' ;;
        get_memory_pressure) test_skill "$skill" '{}' ;;
        
        # ML
        list_ml_jobs) test_skill "$skill" '{}' ;;
        get_anomalies) test_skill "$skill" '{}' ;;
        list_trained_models) test_skill "$skill" '{}' ;;
        run_inference) test_skill "$skill" '{"model_id": "test", "input_text": "hello"}' ;;
        embed_text) test_skill "$skill" '{"text": "hello world"}' ;;
        detect_anomalies_realtime) test_skill "$skill" '{"index_pattern": "metrics-*", "field": "value"}' ;;
        get_job_status) test_skill "$skill" '{"job_id": "test"}' ;;
        get_influencers) test_skill "$skill" '{"job_id": "test"}' ;;
        explain_anomaly) test_skill "$skill" '{"job_id": "test", "anomaly_id": "1"}' ;;
        classify_text) test_skill "$skill" '{"text": "hello"}' ;;
        extract_entities) test_skill "$skill" '{"text": "John works at Elastic"}' ;;
        
        # Security
        suspicious_activity) test_skill "$skill" '{"index_pattern": "security-*"}' ;;
        threat_summary) test_skill "$skill" '{"index_pattern": "security-*"}' ;;
        user_activity) test_skill "$skill" '{"username": "admin", "index_pattern": "security-*"}' ;;
        list_detection_rules) test_skill "$skill" '{}' ;;
        list_cases) test_skill "$skill" '{}' ;;
        get_user_risk_score) test_skill "$skill" '{"username": "admin"}' ;;
        get_host_risk_score) test_skill "$skill" '{"hostname": "prod-1"}' ;;
        hunt_ioc) test_skill "$skill" '{"ioc": "192.168.1.1"}' ;;
        get_authentication_summary) test_skill "$skill" '{}' ;;
        search_security_events) test_skill "$skill" '{"query": "login"}' ;;
        get_process_events) test_skill "$skill" '{}' ;;
        get_network_events) test_skill "$skill" '{}' ;;
        get_file_events) test_skill "$skill" '{}' ;;
        get_dns_queries) test_skill "$skill" '{}' ;;
        get_risky_users) test_skill "$skill" '{}' ;;
        get_risky_hosts) test_skill "$skill" '{}' ;;
        create_case) test_skill "$skill" '{"title": "Test Case", "description": "Testing"}' ;;
        
        # Search
        count_documents) test_skill "$skill" '{"index_pattern": "logs-*"}' ;;
        list_indices) test_skill "$skill" '{}' ;;
        list_all_indices) test_skill "$skill" '{}' ;;
        get_index_stats) test_skill "$skill" '{"index_name": "logs-sample"}' ;;
        list_data_streams) test_skill "$skill" '{}' ;;
        list_ilm_policies) test_skill "$skill" '{}' ;;
        fuzzy_search) test_skill "$skill" '{"query": "error"}' ;;
        semantic_search) test_skill "$skill" '{"query": "find errors"}' ;;
        date_histogram) test_skill "$skill" '{"index_pattern": "logs-*"}' ;;
        top_values) test_skill "$skill" '{"index_pattern": "logs-*", "field": "level"}' ;;
        percentiles) test_skill "$skill" '{"index_pattern": "metrics-*", "field": "value"}' ;;
        multi_field_search) test_skill "$skill" '{"query": "test"}' ;;
        create_document) test_skill "$skill" '{"index_name": "test", "document": "{}"}' ;;
        update_document) test_skill "$skill" '{"index_name": "test", "doc_id": "1", "updates": "{}"}' ;;
        delete_document) test_skill "$skill" '{"index_name": "test", "doc_id": "1"}' ;;
        bulk_index) test_skill "$skill" '{"index_name": "test", "documents": "[]"}' ;;
        reindex) test_skill "$skill" '{"source_index": "logs-*", "dest_index": "logs-copy"}' ;;
        create_index) test_skill "$skill" '{"index_name": "test-new"}' ;;
        delete_index) test_skill "$skill" '{"index_name": "test-old"}' ;;
        set_alias) test_skill "$skill" '{"alias_name": "logs", "index_name": "logs-sample"}' ;;
        get_mapping) test_skill "$skill" '{"index_name": "logs-sample"}' ;;
        list_transforms) test_skill "$skill" '{}' ;;
        get_transform_status) test_skill "$skill" '{"transform_id": "test"}' ;;
        list_ingest_pipelines) test_skill "$skill" '{}' ;;
        test_ingest_pipeline) test_skill "$skill" '{"pipeline_id": "test", "sample_doc": "{}"}' ;;
        
        # Alerting
        list_alert_rules) test_skill "$skill" '{}' ;;
        get_active_alerts) test_skill "$skill" '{}' ;;
        get_alert_history) test_skill "$skill" '{}' ;;
        acknowledge_alert) test_skill "$skill" '{"alert_id": "test"}' ;;
        list_connectors) test_skill "$skill" '{}' ;;
        create_threshold_rule) test_skill "$skill" '{"name": "Test", "index_pattern": "logs-*", "field": "level", "threshold": 100}' ;;
        mute_alert) test_skill "$skill" '{"alert_id": "test"}' ;;
        test_connector) test_skill "$skill" '{"connector_id": "test"}' ;;
        
        # Cluster
        cluster_health) test_skill "$skill" '{}' ;;
        get_cluster_health) test_skill "$skill" '{}' ;;
        list_nodes) test_skill "$skill" '{}' ;;
        get_node_stats) test_skill "$skill" '{"node_name": "node-1"}' ;;
        get_shard_allocation) test_skill "$skill" '{}' ;;
        list_snapshots) test_skill "$skill" '{}' ;;
        get_pending_tasks) test_skill "$skill" '{}' ;;
        get_unassigned_shards) test_skill "$skill" '{}' ;;
        explain_allocation) test_skill "$skill" '{"index": "logs-sample"}' ;;
        list_running_tasks) test_skill "$skill" '{}' ;;
        get_hot_threads) test_skill "$skill" '{}' ;;
        
        # Integrations
        send_slack_message) test_skill "$skill" '{"channel": "test", "message": "hello"}' ;;
        create_jira_issue) test_skill "$skill" '{"project": "TEST", "summary": "Test issue"}' ;;
        trigger_pagerduty) test_skill "$skill" '{"service_key": "test", "description": "Test alert"}' ;;
        send_email) test_skill "$skill" '{"to": "test@example.com", "subject": "Test", "body": "Hello"}' ;;
        send_webhook) test_skill "$skill" '{"url": "https://example.com", "payload": "{}"}' ;;
        send_teams_message) test_skill "$skill" '{"webhook_url": "https://example.com", "message": "hello"}' ;;
        send_opsgenie_alert) test_skill "$skill" '{"message": "Test alert"}' ;;
        create_servicenow_incident) test_skill "$skill" '{"short_description": "Test", "description": "Test incident"}' ;;
        trigger_github_workflow) test_skill "$skill" '{"repo": "owner/repo", "workflow": "ci.yml"}' ;;
        invoke_aws_lambda) test_skill "$skill" '{"function_name": "test-function"}' ;;
        
        # Fleet
        list_agents) test_skill "$skill" '{}' ;;
        get_agent_status) test_skill "$skill" '{"agent_id": "test"}' ;;
        list_agent_policies) test_skill "$skill" '{}' ;;
        get_agent_logs) test_skill "$skill" '{"agent_id": "test"}' ;;
        list_integrations) test_skill "$skill" '{}' ;;
        get_enrollment_tokens) test_skill "$skill" '{}' ;;
        
        # Enterprise Search
        list_search_apps) test_skill "$skill" '{}' ;;
        get_search_analytics) test_skill "$skill" '{"app_name": "test"}' ;;
        get_top_queries) test_skill "$skill" '{"app_name": "test"}' ;;
        get_no_results_queries) test_skill "$skill" '{"app_name": "test"}' ;;
        
        # Default case for any skill not explicitly listed
        *) test_skill "$skill" '{}' ;;
    esac
done

echo ""
echo "========================================"
echo "Test Results"
echo "========================================"
echo -e "${GREEN}Passed:${NC}  $PASSED"
echo -e "${RED}Failed:${NC}  $FAILED"
echo -e "${YELLOW}Skipped:${NC} $SKIPPED"
echo "Total:   $TOTAL"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}Some tests failed.${NC}"
    exit 1
fi
