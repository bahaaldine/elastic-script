#!/bin/bash
# Comprehensive E2E test for skills
# Tests: creation, invocation, parameters, defaults

# Don't exit on error - we want to see all test results

ES="http://localhost:9200"
AUTH="-u elastic-admin:elastic-password"
PASS=0
FAIL=0

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_pass() { echo -e "${GREEN}✓ PASS${NC}: $1"; PASS=$((PASS+1)); }
log_fail() { echo -e "${RED}✗ FAIL${NC}: $1"; FAIL=$((FAIL+1)); }
log_info() { echo -e "${YELLOW}→${NC} $1"; }

# Check ES is running
check_es() {
    log_info "Checking Elasticsearch..."
    if ! curl -s $AUTH "$ES/_cluster/health" > /dev/null 2>&1; then
        echo "ERROR: Elasticsearch not running at $ES"
        exit 1
    fi
    log_pass "Elasticsearch is running"
}

# Clean up all test skills and procedures
cleanup() {
    log_info "Cleaning up old skills and procedures..."
    
    # Drop skills via API
    for skill in "test_simple" "test_with_param" "test_with_default" "test_esql"; do
        curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" \
            -d "{\"query\": \"DROP SKILL $skill\"}" > /dev/null 2>&1 || true
    done
    
    # Delete procedures directly
    for proc in "test_simple" "test_with_param" "test_with_default" "test_esql"; do
        curl -s $AUTH -X DELETE "$ES/.elastic_script_procedures/_doc/$proc" > /dev/null 2>&1 || true
    done
    
    # Also delete from skills index
    for skill in "test_simple" "test_with_param" "test_with_default" "test_esql"; do
        curl -s $AUTH -X DELETE "$ES/.escript_skills/_doc/$skill" > /dev/null 2>&1 || true
    done
    
    # Refresh indices
    curl -s $AUTH -X POST "$ES/.elastic_script_procedures/_refresh" > /dev/null 2>&1 || true
    curl -s $AUTH -X POST "$ES/.escript_skills/_refresh" > /dev/null 2>&1 || true
    
    sleep 1
    log_pass "Cleanup complete"
}

# Test 1: Simple skill with no parameters
test_simple_skill() {
    log_info "TEST 1: Simple skill (no parameters)"
    
    # Create skill
    RESULT=$(curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
        "query": "CREATE SKILL test_simple VERSION '\''1.0'\'' DESCRIPTION '\''Simple test'\'' RETURNS STRING BEGIN RETURN '\''Hello World'\''; END SKILL;"
    }')
    
    if echo "$RESULT" | grep -q '"success"'; then
        log_pass "Created test_simple skill"
    else
        log_fail "Failed to create test_simple: $RESULT"
        return
    fi
    
    # Check procedure was stored
    PROC=$(curl -s $AUTH "$ES/.elastic_script_procedures/_doc/test_simple")
    if echo "$PROC" | grep -q '"found":true'; then
        log_pass "Procedure stored in index"
        
        # Check it doesn't start with CREATE
        PROC_TEXT=$(echo "$PROC" | grep -o '"procedure":"[^"]*"' | head -1)
        if echo "$PROC_TEXT" | grep -q '"procedure":"PROCEDURE'; then
            log_pass "Procedure format correct (starts with PROCEDURE)"
        else
            log_fail "Procedure format wrong: $PROC_TEXT"
        fi
    else
        log_fail "Procedure not found in index"
    fi
    
    # Invoke skill
    INVOKE=$(curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
        "query": "RUN SKILL test_simple"
    }')
    
    if echo "$INVOKE" | grep -q "Hello World"; then
        log_pass "Invoked test_simple successfully"
    else
        log_fail "Failed to invoke test_simple: $INVOKE"
    fi
}

# Test 2: Skill with required parameter (no default)
test_skill_with_param() {
    log_info "TEST 2: Skill with required parameter"
    
    # Create skill
    RESULT=$(curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
        "query": "CREATE SKILL test_with_param VERSION '\''1.0'\'' DESCRIPTION '\''Test with param'\'' (name STRING) RETURNS STRING BEGIN RETURN '\''Hello '\'' || name; END SKILL;"
    }')
    
    if echo "$RESULT" | grep -q '"success"'; then
        log_pass "Created test_with_param skill"
    else
        log_fail "Failed to create test_with_param: $RESULT"
        return
    fi
    
    # Invoke with argument
    INVOKE=$(curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
        "query": "RUN SKILL test_with_param('\''Alice'\'')"
    }')
    
    if echo "$INVOKE" | grep -q "Hello Alice"; then
        log_pass "Invoked test_with_param('Alice') successfully"
    else
        log_fail "Failed to invoke test_with_param: $INVOKE"
    fi
}

# Test 3: Skill with default parameter
test_skill_with_default() {
    log_info "TEST 3: Skill with default parameter"
    
    # Create skill
    RESULT=$(curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
        "query": "CREATE SKILL test_with_default VERSION '\''1.0'\'' DESCRIPTION '\''Test with default'\'' (greeting STRING DEFAULT '\''Hello'\'') RETURNS STRING BEGIN RETURN greeting || '\'' World'\''; END SKILL;"
    }')
    
    if echo "$RESULT" | grep -q '"success"'; then
        log_pass "Created test_with_default skill"
    else
        log_fail "Failed to create test_with_default: $RESULT"
        return
    fi
    
    # Check procedure has parameters field
    PROC=$(curl -s $AUTH "$ES/.elastic_script_procedures/_doc/test_with_default?pretty")
    if echo "$PROC" | grep -q '"parameters"'; then
        log_pass "Procedure has parameters field"
    else
        log_fail "Procedure missing parameters field: $PROC"
    fi
    
    # Invoke WITHOUT argument (should use default)
    INVOKE=$(curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
        "query": "RUN SKILL test_with_default"
    }')
    
    if echo "$INVOKE" | grep -q "Hello World"; then
        log_pass "Invoked test_with_default() with default value"
    else
        log_fail "Failed to invoke with default: $INVOKE"
    fi
    
    # Invoke WITH argument (should override default)
    INVOKE2=$(curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
        "query": "RUN SKILL test_with_default('\''Hi'\'')"
    }')
    
    if echo "$INVOKE2" | grep -q "Hi World"; then
        log_pass "Invoked test_with_default('Hi') with custom value"
    else
        log_fail "Failed to invoke with custom value: $INVOKE2"
    fi
}

# Test 4: Skill with ESQL query
test_skill_with_esql() {
    log_info "TEST 4: Skill with ESQL query"
    
    # First check if logs-sample exists
    if ! curl -s $AUTH "$ES/logs-sample/_count" | grep -q '"count"'; then
        log_info "Skipping ESQL test - logs-sample index not found"
        return
    fi
    
    # Create skill
    RESULT=$(curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
        "query": "CREATE SKILL test_esql VERSION '\''1.0'\'' DESCRIPTION '\''Test ESQL'\'' RETURNS ARRAY BEGIN DECLARE results ARRAY; SET results = ESQL_QUERY('\''FROM logs-sample | STATS count = COUNT(*) BY level | LIMIT 5'\''); RETURN results; END SKILL;"
    }')
    
    if echo "$RESULT" | grep -q '"success"'; then
        log_pass "Created test_esql skill"
    else
        log_fail "Failed to create test_esql: $RESULT"
        return
    fi
    
    # Invoke
    INVOKE=$(curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
        "query": "RUN SKILL test_esql"
    }')
    
    if echo "$INVOKE" | grep -q "count"; then
        log_pass "Invoked test_esql successfully"
    else
        log_fail "Failed to invoke test_esql: $INVOKE"
    fi
}

# Test the demo skills from quick-start.sh
test_demo_skills() {
    log_info "TEST 5: Check demo skills (from quick-start.sh)"
    
    # Check hello_moltler
    SKILL=$(curl -s $AUTH "$ES/.escript_skills/_doc/hello_moltler")
    if echo "$SKILL" | grep -q '"found":true'; then
        log_pass "hello_moltler skill exists"
        
        # Check procedure
        PROC=$(curl -s $AUTH "$ES/.elastic_script_procedures/_doc/hello_moltler?pretty")
        PROC_BODY=$(echo "$PROC" | grep -o '"procedure":"[^"]*"' | head -1)
        
        # Should NOT contain "CALL hello_moltler" (old format)
        if echo "$PROC_BODY" | grep -q "CALL hello_moltler"; then
            log_fail "hello_moltler has OLD format (CALL inside body)"
            echo "  Procedure: $PROC_BODY"
        else
            log_pass "hello_moltler has NEW format"
        fi
        
        # Try to invoke
        INVOKE=$(curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
            "query": "RUN SKILL hello_moltler"
        }')
        
        if echo "$INVOKE" | grep -q "Hello"; then
            log_pass "hello_moltler invocation works"
        else
            log_fail "hello_moltler invocation failed: $INVOKE"
        fi
    else
        log_info "hello_moltler not found - run quick-start.sh first"
    fi
}

# Print summary
summary() {
    echo ""
    echo "========================================"
    echo "TEST SUMMARY"
    echo "========================================"
    echo -e "Passed: ${GREEN}$PASS${NC}"
    echo -e "Failed: ${RED}$FAIL${NC}"
    echo "========================================"
    
    if [ $FAIL -gt 0 ]; then
        exit 1
    fi
}

# Run all tests
main() {
    echo "========================================"
    echo "COMPREHENSIVE SKILL E2E TESTS"
    echo "========================================"
    echo ""
    
    check_es
    cleanup
    
    test_simple_skill
    echo ""
    
    test_skill_with_param
    echo ""
    
    test_skill_with_default
    echo ""
    
    test_skill_with_esql
    echo ""
    
    test_demo_skills
    
    summary
}

main "$@"
