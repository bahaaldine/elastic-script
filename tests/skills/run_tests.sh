#!/bin/bash
#
# Moltler Skill Test Runner
# 
# Usage:
#   ./run_tests.sh                    # Run all tests
#   ./run_tests.sh --category security  # Test specific category
#   ./run_tests.sh --skill hunt_ioc     # Test specific skill
#   ./run_tests.sh --generate           # Generate test cases for all skills
#   ./run_tests.sh --setup              # Setup fixtures only
#   ./run_tests.sh --help               # Show help
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default settings
ES_URL="${ES_URL:-http://localhost:9200}"
ES_USERNAME="${ES_USERNAME:-elastic-admin}"
ES_PASSWORD="${ES_PASSWORD:-elastic-password}"

show_help() {
    cat << EOF
Moltler Skill Test Runner

USAGE:
    ./run_tests.sh [OPTIONS]

OPTIONS:
    -c, --category CATEGORY    Test skills in specific category
    -s, --skill SKILL          Test specific skill by name
    -g, --generate             Generate test case files for all skills
    -f, --setup-fixtures       Setup test fixtures only
    -t, --teardown             Remove test fixtures
    -v, --verbose              Verbose output
    -x, --fail-fast            Stop on first failure
    --skip-fixtures            Skip fixture setup
    --es-url URL               Elasticsearch URL (default: $ES_URL)
    -h, --help                 Show this help message

EXAMPLES:
    ./run_tests.sh                           # Run all tests
    ./run_tests.sh --category observability  # Test observability skills
    ./run_tests.sh --skill hunt_ioc          # Test specific skill
    ./run_tests.sh --generate                # Generate test cases
    ./run_tests.sh --verbose --fail-fast     # Verbose with early exit

ENVIRONMENT VARIABLES:
    ES_URL          Elasticsearch URL (default: http://localhost:9200)
    ES_USERNAME     Elasticsearch username (default: elastic-admin)
    ES_PASSWORD     Elasticsearch password (default: elastic-password)

EOF
}

check_python() {
    if ! command -v python3 &> /dev/null; then
        echo -e "${RED}Error: Python 3 is required${NC}"
        exit 1
    fi
}

check_dependencies() {
    cd "$SCRIPT_DIR"
    
    # Check if dependencies are installed
    if ! python3 -c "import requests, yaml" 2>/dev/null; then
        echo -e "${YELLOW}Installing dependencies...${NC}"
        pip3 install -q -r requirements.txt
    fi
}

check_elasticsearch() {
    echo -e "${BLUE}Checking Elasticsearch...${NC}"
    
    if ! curl -s -u "$ES_USERNAME:$ES_PASSWORD" "$ES_URL/_cluster/health" > /dev/null 2>&1; then
        echo -e "${RED}Error: Cannot connect to Elasticsearch at $ES_URL${NC}"
        echo "Make sure Elasticsearch is running with the elastic-script plugin."
        echo ""
        echo "Quick start:"
        echo "  ./scripts/quick-start.sh"
        exit 1
    fi
    
    echo -e "${GREEN}✓ Elasticsearch available${NC}"
}

run_tests() {
    local args=("$@")
    
    cd "$SCRIPT_DIR"
    
    echo -e "${BLUE}Running skill tests...${NC}"
    echo ""
    
    python3 run_skill_tests.py \
        --es-url "$ES_URL" \
        --es-user "$ES_USERNAME" \
        --es-pass "$ES_PASSWORD" \
        "${args[@]}"
    
    exit_code=$?
    
    if [ $exit_code -eq 0 ]; then
        echo ""
        echo -e "${GREEN}All tests passed!${NC}"
    else
        echo ""
        echo -e "${RED}Some tests failed${NC}"
    fi
    
    return $exit_code
}

generate_tests() {
    cd "$SCRIPT_DIR"
    
    echo -e "${BLUE}Generating test cases for all skills...${NC}"
    echo ""
    
    python3 generate_test_cases.py "$@"
}

# Parse arguments
ARGS=()
GENERATE=false
SETUP_ONLY=false
TEARDOWN=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -g|--generate)
            GENERATE=true
            shift
            ;;
        -f|--setup-fixtures)
            SETUP_ONLY=true
            shift
            ;;
        -t|--teardown)
            TEARDOWN=true
            shift
            ;;
        --es-url)
            ES_URL="$2"
            shift 2
            ;;
        -c|--category)
            ARGS+=("--category" "$2")
            shift 2
            ;;
        -s|--skill)
            ARGS+=("--skill" "$2")
            shift 2
            ;;
        -v|--verbose)
            ARGS+=("--verbose")
            shift
            ;;
        -x|--fail-fast)
            ARGS+=("--fail-fast")
            shift
            ;;
        --skip-fixtures)
            ARGS+=("--skip-fixtures")
            shift
            ;;
        --overwrite)
            ARGS+=("--overwrite")
            shift
            ;;
        *)
            ARGS+=("$1")
            shift
            ;;
    esac
done

# Main
echo ""
echo "=========================================="
echo "  Moltler Skill Test Runner"
echo "=========================================="
echo ""

check_python
check_dependencies

if [ "$GENERATE" = true ]; then
    generate_tests "${ARGS[@]}"
    exit $?
fi

check_elasticsearch

if [ "$TEARDOWN" = true ]; then
    run_tests --teardown-fixtures
    exit $?
fi

if [ "$SETUP_ONLY" = true ]; then
    run_tests --setup-fixtures
    exit $?
fi

run_tests "${ARGS[@]}"
