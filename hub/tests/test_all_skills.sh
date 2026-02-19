#!/bin/bash
# Comprehensive skill test suite
set -uo pipefail

ES_URL="${ES_URL:-http://localhost:9200}"
ES_USER="${ES_USER:-elastic-admin}"
ES_PASSWORD="${ES_PASSWORD:-elastic-password}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PASSED=0
FAILED=0
SKIPPED=0

log_test() { echo -e "${BLUE}[TEST]${NC} $1"; }
pass() { echo -e "${GREEN}[PASS]${NC} $1"; PASSED=$((PASSED+1)); }
fail() { echo -e "${RED}[FAIL]${NC} $1"; FAILED=$((FAILED+1)); }
skip() { echo -e "${YELLOW}[SKIP]${NC} $1"; SKIPPED=$((SKIPPED+1)); }

mcp_call() {
    local name="$1"
    local args="${2:-{}}"
    curl -s -u "$ES_USER:$ES_PASSWORD" "$ES_URL/_escript/mcp" \
        -H "Content-Type: application/json" \
        -d "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"tools/call\", \"params\": {\"name\": \"$name\", \"arguments\": $args}}"
}

test_skill() {
    local name="$1"
    local args="${2:-{}}"
    local expect="${3:-}"
    
    log_test "$name"
    local response
    response=$(mcp_call "$name" "$args")
    
    if echo "$response" | grep -q '"isError":false'; then
        if [ -n "$expect" ]; then
            if echo "$response" | grep -q "$expect"; then
                pass "$name - contains expected content"
            else
                fail "$name - missing expected: $expect"
            fi
        else
            pass "$name - executed successfully"
        fi
    else
        fail "$name - error: $(echo "$response" | jq -r '.result.content[0].text // .error // "unknown"' | head -c 100)"
    fi
}

echo "========================================="
echo "  Moltler Skills Test Suite"
echo "========================================="
echo

# Check connectivity
if ! curl -s -u "$ES_USER:$ES_PASSWORD" "$ES_URL" > /dev/null 2>&1; then
    echo -e "${RED}Cannot connect to Elasticsearch${NC}"
    exit 1
fi
echo -e "${GREEN}Connected to Elasticsearch${NC}"
echo

# Count available skills
SKILL_COUNT=$(curl -s -u "$ES_USER:$ES_PASSWORD" "$ES_URL/_escript/mcp" \
    -H "Content-Type: application/json" \
    -d '{"jsonrpc": "2.0", "id": 1, "method": "tools/list"}' | jq '.result.tools | length')
echo "Available skills: $SKILL_COUNT"
echo

echo "--- Meta Skills ---"
test_skill "list_all_skills" '{}' 
test_skill "search_skills" '{"query": "logs"}'
test_skill "explain_skill" '{"skill_name": "count_logs_by_level"}'
test_skill "recommend_skills" '{"goal": "investigate errors"}'
test_skill "get_related_skills" '{"skill_name": "count_logs_by_level"}'

echo
echo "--- Observability Skills ---"
test_skill "count_logs_by_level" '{"index_pattern": "logs-sample"}' "level"
test_skill "get_recent_errors" '{"index_pattern": "logs-sample", "limit": 5}'
test_skill "error_rate" '{"index_pattern": "logs-sample"}'
test_skill "top_error_messages" '{"index_pattern": "logs-sample", "limit": 5}'
test_skill "logs_by_service" '{"index_pattern": "logs-sample"}'
test_skill "search_logs" '{"query": "error", "index_pattern": "logs-sample", "limit": 5}'
test_skill "get_metrics_summary" '{"index_pattern": "metrics-sample"}'
test_skill "high_cpu_hosts" '{"index_pattern": "metrics-sample", "threshold": 50}'
test_skill "slow_requests" '{"index_pattern": "logs-sample", "threshold_ms": 100, "limit": 5}'
test_skill "service_health" '{"service": "api-gateway", "index_pattern": "logs-sample"}'

echo
echo "--- Search Skills ---"
test_skill "search_documents" '{"query": "user", "index_pattern": "users-sample", "limit": 5}'
test_skill "count_documents" '{"index_pattern": "logs-sample"}'
test_skill "aggregate_by_field" '{"index_pattern": "logs-sample", "field": "service", "limit": 5}'
test_skill "get_sample_documents" '{"index_pattern": "logs-sample", "limit": 3}'
test_skill "get_unique_values" '{"index_pattern": "logs-sample", "field": "level", "limit": 10}'
test_skill "get_field_stats" '{"index_pattern": "metrics-sample", "field": "value"}'
test_skill "recent_documents" '{"index_pattern": "logs-sample", "limit": 5}'
test_skill "cluster_health" '{}'
test_skill "list_indices" '{}'

echo
echo "--- Security Skills ---"
test_skill "get_security_alerts" '{"index_pattern": "security-events", "limit": 5}'
test_skill "failed_logins" '{"index_pattern": "security-events", "limit": 5}'
test_skill "suspicious_activity" '{"index_pattern": "security-events", "limit": 5}'
test_skill "user_activity" '{"username": "admin", "index_pattern": "security-events", "limit": 5}'
test_skill "threat_summary" '{"index_pattern": "security-events"}'

echo
echo "========================================="
echo "  Test Results"
echo "========================================="
echo -e "Passed: ${GREEN}$PASSED${NC}"
echo -e "Failed: ${RED}$FAILED${NC}"
echo -e "Skipped: ${YELLOW}$SKIPPED${NC}"
echo

if [ $FAILED -gt 0 ]; then
    exit 1
fi
