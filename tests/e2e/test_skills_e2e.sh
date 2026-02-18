#!/bin/bash
# End-to-End Skills Test Script
# Tests all demo skills and procedures against a running Elasticsearch instance

set -e

ES_URL="${ES_URL:-http://localhost:9200}"
ES_USER="${ES_USER:-elastic-admin}"
ES_PASS="${ES_PASS:-elastic-password}"
AUTH="-u ${ES_USER}:${ES_PASS}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

PASSED=0
FAILED=0
SKIPPED=0

test_passed() {
    echo -e "${GREEN}✓ PASSED${NC}: $1"
    ((PASSED++))
}

test_failed() {
    echo -e "${RED}✗ FAILED${NC}: $1"
    echo "  Error: $2"
    ((FAILED++))
}

test_skipped() {
    echo -e "${YELLOW}○ SKIPPED${NC}: $1"
    echo "  Reason: $2"
    ((SKIPPED++))
}

run_query() {
    local query="$1"
    curl -s $AUTH -X POST "${ES_URL}/_escript" \
        -H "Content-Type: application/json" \
        -d "{\"query\": \"$query\"}" 2>&1
}

check_result() {
    local result="$1"
    local test_name="$2"
    
    if echo "$result" | grep -q '"error"'; then
        local error=$(echo "$result" | jq -r '.error // .error.reason // "Unknown error"' 2>/dev/null || echo "$result")
        test_failed "$test_name" "$error"
        return 1
    else
        test_passed "$test_name"
        return 0
    fi
}

echo "========================================"
echo "  Moltler E2E Skills Test Suite"
echo "========================================"
echo ""
echo "Elasticsearch: $ES_URL"
echo "User: $ES_USER"
echo ""

# Check Elasticsearch connectivity
echo "Checking Elasticsearch connectivity..."
if ! curl -s $AUTH "${ES_URL}/_cluster/health" | grep -q '"status"'; then
    echo -e "${RED}ERROR: Cannot connect to Elasticsearch${NC}"
    exit 1
fi
echo -e "${GREEN}Connected to Elasticsearch${NC}"
echo ""

# ============================================
# Test Procedures
# ============================================
echo "========================================"
echo "  Testing Procedures"
echo "========================================"
echo ""

# Test hello_world
echo "Testing: hello_world"
result=$(run_query "CALL hello_world()")
check_result "$result" "CALL hello_world()"

# Test order_summary
echo "Testing: order_summary"
result=$(run_query "CALL order_summary()")
if check_result "$result" "CALL order_summary()"; then
    # Verify expected fields
    if echo "$result" | jq -e '.result.total_orders' > /dev/null 2>&1; then
        test_passed "order_summary returns total_orders"
    else
        test_failed "order_summary returns total_orders" "Missing total_orders field"
    fi
fi

# Test get_user_stats
echo "Testing: get_user_stats"
result=$(run_query "CALL get_user_stats()")
if check_result "$result" "CALL get_user_stats()"; then
    # Verify expected fields
    if echo "$result" | jq -e '.result.total_users' > /dev/null 2>&1; then
        test_passed "get_user_stats returns total_users"
    else
        test_failed "get_user_stats returns total_users" "Missing total_users field"
    fi
fi

# Test health_check
echo "Testing: health_check"
result=$(run_query "CALL health_check()")
if check_result "$result" "CALL health_check()"; then
    if echo "$result" | jq -e '.result.status' > /dev/null 2>&1; then
        test_passed "health_check returns status"
    else
        test_failed "health_check returns status" "Missing status field"
    fi
fi

# Test analyze_logs (if exists)
echo "Testing: analyze_logs"
result=$(run_query "CALL analyze_logs()")
check_result "$result" "CALL analyze_logs()" || true

echo ""

# ============================================
# Test Skills
# ============================================
echo "========================================"
echo "  Testing Skills"
echo "========================================"
echo ""

# First, check if skills exist
echo "Listing skills..."
result=$(run_query "SHOW SKILLS")
skill_count=$(echo "$result" | jq -r '.result.count // 0' 2>/dev/null || echo "0")
echo "Found $skill_count skills"
echo ""

if [ "$skill_count" -eq "0" ]; then
    test_skipped "All skill tests" "No skills found. Skills need to be created first."
else
    # Get skill names
    skills=$(echo "$result" | jq -r '.result.skills[].name' 2>/dev/null || echo "")
    
    for skill in $skills; do
        echo "Testing skill: $skill"
        result=$(run_query "CALL $skill()")
        check_result "$result" "CALL $skill()"
    done
fi

echo ""

# ============================================
# Test MCP Endpoint
# ============================================
echo "========================================"
echo "  Testing MCP Endpoint"
echo "========================================"
echo ""

# Test MCP info endpoint
echo "Testing: GET /_escript/mcp"
result=$(curl -s $AUTH "${ES_URL}/_escript/mcp" 2>&1)
if echo "$result" | grep -q '"name".*"moltler"'; then
    test_passed "MCP info endpoint"
else
    test_failed "MCP info endpoint" "$result"
fi

# Test MCP tools/list
echo "Testing: MCP tools/list"
result=$(curl -s $AUTH -X POST "${ES_URL}/_escript/mcp" \
    -H "Content-Type: application/json" \
    -d '{"jsonrpc": "2.0", "method": "tools/list", "id": 1}' 2>&1)
if echo "$result" | grep -q '"tools"'; then
    test_passed "MCP tools/list"
    tool_count=$(echo "$result" | jq '.result.tools | length' 2>/dev/null || echo "0")
    echo "  Found $tool_count MCP tools"
else
    test_failed "MCP tools/list" "$result"
fi

# Test MCP initialize
echo "Testing: MCP initialize"
result=$(curl -s $AUTH -X POST "${ES_URL}/_escript/mcp" \
    -H "Content-Type: application/json" \
    -d '{"jsonrpc": "2.0", "method": "initialize", "params": {"protocolVersion": "2024-11-05", "clientInfo": {"name": "test", "version": "1.0"}}, "id": 2}' 2>&1)
if echo "$result" | grep -q '"protocolVersion"'; then
    test_passed "MCP initialize"
else
    test_failed "MCP initialize" "$result"
fi

echo ""

# ============================================
# Summary
# ============================================
echo "========================================"
echo "  Test Summary"
echo "========================================"
echo ""
echo -e "  ${GREEN}Passed${NC}:  $PASSED"
echo -e "  ${RED}Failed${NC}:  $FAILED"
echo -e "  ${YELLOW}Skipped${NC}: $SKIPPED"
echo ""

TOTAL=$((PASSED + FAILED))
if [ "$TOTAL" -gt 0 ]; then
    PERCENT=$((PASSED * 100 / TOTAL))
    echo "  Pass Rate: ${PERCENT}%"
fi
echo ""

if [ "$FAILED" -gt 0 ]; then
    echo -e "${RED}Some tests failed!${NC}"
    exit 1
else
    echo -e "${GREEN}All tests passed!${NC}"
    exit 0
fi
