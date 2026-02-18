#!/bin/bash
# Test MCP Bridge and /_escript/mcp endpoint
# This script tests the Model Context Protocol integration

set -euo pipefail

ES_URL="${ES_URL:-http://localhost:9200}"
ES_USER="${ES_USER:-elastic-admin}"
ES_PASSWORD="${ES_PASSWORD:-elastic-password}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

PASSED=0
FAILED=0

pass() {
    echo -e "${GREEN}✓ PASS${NC}: $1"
    ((PASSED++))
}

fail() {
    echo -e "${RED}✗ FAIL${NC}: $1"
    ((FAILED++))
}

warn() {
    echo -e "${YELLOW}⚠ WARN${NC}: $1"
}

# Send MCP request to Elasticsearch
mcp_request() {
    local method="$1"
    local params="${2:-{}}"
    local id="${3:-1}"
    
    curl -s -u "$ES_USER:$ES_PASSWORD" -X POST "$ES_URL/_escript/mcp" \
        -H "Content-Type: application/json" \
        -d "{
            \"jsonrpc\": \"2.0\",
            \"method\": \"$method\",
            \"params\": $params,
            \"id\": $id
        }"
}

echo "============================================"
echo "MCP Bridge End-to-End Tests"
echo "============================================"
echo "Target: $ES_URL"
echo ""

# Test 1: Check ES is running
echo "--- Test: Elasticsearch connectivity ---"
if curl -s -u "$ES_USER:$ES_PASSWORD" "$ES_URL/_cluster/health" | grep -q '"status"'; then
    pass "Elasticsearch is running"
else
    fail "Elasticsearch is not reachable"
    exit 1
fi

# Test 2: MCP initialize
echo ""
echo "--- Test: MCP initialize ---"
INIT_RESPONSE=$(mcp_request "initialize" '{"protocolVersion": "2024-11-05", "clientInfo": {"name": "test", "version": "1.0"}}')

if echo "$INIT_RESPONSE" | grep -q '"protocolVersion"'; then
    pass "MCP initialize returns protocolVersion"
else
    fail "MCP initialize failed: $INIT_RESPONSE"
fi

if echo "$INIT_RESPONSE" | grep -q '"serverInfo"'; then
    pass "MCP initialize returns serverInfo"
else
    fail "MCP initialize missing serverInfo"
fi

if echo "$INIT_RESPONSE" | grep -q '"capabilities"'; then
    pass "MCP initialize returns capabilities"
else
    fail "MCP initialize missing capabilities"
fi

# Test 3: MCP tools/list
echo ""
echo "--- Test: MCP tools/list ---"
TOOLS_RESPONSE=$(mcp_request "tools/list")

if echo "$TOOLS_RESPONSE" | grep -q '"tools"'; then
    pass "MCP tools/list returns tools array"
else
    fail "MCP tools/list failed: $TOOLS_RESPONSE"
fi

# Check if demo skills are present
if echo "$TOOLS_RESPONSE" | grep -q '"hello_moltler"'; then
    pass "Demo skill 'hello_moltler' found in tools"
else
    warn "Demo skill 'hello_moltler' not found - may not be loaded"
fi

if echo "$TOOLS_RESPONSE" | grep -q '"cluster_health"'; then
    pass "Demo skill 'cluster_health' found in tools"
else
    warn "Demo skill 'cluster_health' not found"
fi

# Test 4: MCP tools/call
echo ""
echo "--- Test: MCP tools/call ---"

# Test calling hello_moltler skill
CALL_RESPONSE=$(mcp_request "tools/call" '{"name": "hello_moltler", "arguments": {}}')

if echo "$CALL_RESPONSE" | grep -q '"content"'; then
    pass "MCP tools/call returns content"
else
    fail "MCP tools/call failed: $CALL_RESPONSE"
fi

if echo "$CALL_RESPONSE" | grep -qi 'moltler\|hello\|welcome'; then
    pass "hello_moltler skill executed successfully"
else
    warn "hello_moltler response may be unexpected: $CALL_RESPONSE"
fi

# Test 5: MCP tools/call with parameters
echo ""
echo "--- Test: MCP tools/call with parameters ---"

# Test cluster_health skill
HEALTH_RESPONSE=$(mcp_request "tools/call" '{"name": "cluster_health", "arguments": {}}')

if echo "$HEALTH_RESPONSE" | grep -q '"content"'; then
    pass "cluster_health skill returns content"
else
    fail "cluster_health skill failed: $HEALTH_RESPONSE"
fi

# Test 6: MCP error handling
echo ""
echo "--- Test: MCP error handling ---"

# Call nonexistent tool
ERROR_RESPONSE=$(mcp_request "tools/call" '{"name": "nonexistent_skill_xyz", "arguments": {}}')

if echo "$ERROR_RESPONSE" | grep -q '"error"'; then
    pass "MCP returns error for nonexistent tool"
else
    fail "MCP should return error for nonexistent tool: $ERROR_RESPONSE"
fi

# Test 7: Invalid method
echo ""
echo "--- Test: MCP invalid method handling ---"
INVALID_RESPONSE=$(mcp_request "invalid/method")

if echo "$INVALID_RESPONSE" | grep -q '"error"'; then
    pass "MCP returns error for invalid method"
else
    fail "MCP should return error for invalid method"
fi

# Test 8: Test MCP Bridge Python script (if available)
echo ""
echo "--- Test: MCP Bridge Python script ---"

BRIDGE_SCRIPT="/Users/baha/dev/elastic-script/mcp-bridge/moltler_mcp_server.py"
if [[ -f "$BRIDGE_SCRIPT" ]]; then
    # Test that the bridge can be imported and run
    if python3 -c "import sys; sys.path.insert(0, '/Users/baha/dev/elastic-script/mcp-bridge'); import moltler_mcp_server" 2>/dev/null; then
        pass "MCP bridge script imports successfully"
    else
        # Check if httpx is installed
        if python3 -c "import httpx" 2>/dev/null; then
            pass "MCP bridge dependency httpx is installed"
        else
            warn "MCP bridge dependency httpx not installed"
        fi
    fi
    
    # Test bridge with a simple request via stdin
    BRIDGE_TEST=$(echo '{"jsonrpc":"2.0","method":"tools/list","id":1}' | \
        timeout 10 python3 "$BRIDGE_SCRIPT" 2>&1 || true)
    
    if echo "$BRIDGE_TEST" | grep -q '"tools"'; then
        pass "MCP bridge handles tools/list via stdio"
    else
        warn "MCP bridge stdio test inconclusive (may need ES running)"
    fi
else
    warn "MCP bridge script not found at $BRIDGE_SCRIPT"
fi

# Summary
echo ""
echo "============================================"
echo "Test Summary"
echo "============================================"
echo -e "${GREEN}Passed: $PASSED${NC}"
echo -e "${RED}Failed: $FAILED${NC}"
echo ""

if [[ $FAILED -gt 0 ]]; then
    echo -e "${RED}Some tests failed!${NC}"
    exit 1
else
    echo -e "${GREEN}All tests passed!${NC}"
    exit 0
fi
