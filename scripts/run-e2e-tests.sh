#!/bin/bash
# E2E Test Runner with APM Tracing
# 
# Runs all E2E notebook tests and shows traces in Kibana APM.
#
# Usage:
#   ./scripts/run-e2e-tests.sh                  # Run all tests
#   ./scripts/run-e2e-tests.sh --notebook 01    # Run specific notebook
#   ./scripts/run-e2e-tests.sh --list           # List available notebooks
#   ./scripts/run-e2e-tests.sh --open-apm       # Run tests and open APM in browser

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

print_header() {
    echo -e "\n${GREEN}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║${NC}  $1"
    echo -e "${GREEN}╚══════════════════════════════════════════════════════════════╝${NC}\n"
}

# Parse arguments
OPEN_APM=false
ARGS=()
for arg in "$@"; do
    if [ "$arg" == "--open-apm" ]; then
        OPEN_APM=true
    else
        ARGS+=("$arg")
    fi
done

# Check prerequisites
print_header "E2E Test Runner with APM Tracing"

# Check Elasticsearch
echo -n "Checking Elasticsearch... "
if curl -s -u elastic-admin:elastic-password http://localhost:9200/_cluster/health > /dev/null 2>&1; then
    echo -e "${GREEN}✔ Running${NC}"
else
    echo -e "${YELLOW}✖ Not running${NC}"
    echo ""
    echo "Please start Elasticsearch first:"
    echo "  ./scripts/quick-start.sh"
    exit 1
fi

# Check OTEL Collector
echo -n "Checking OTEL Collector... "
if curl -s http://localhost:4318 > /dev/null 2>&1; then
    echo -e "${GREEN}✔ Running${NC}"
else
    echo -e "${YELLOW}⚠ Not running (traces won't be sent)${NC}"
fi

# Check APM Server
echo -n "Checking APM Server... "
if curl -s http://localhost:8200 > /dev/null 2>&1; then
    echo -e "${GREEN}✔ Running${NC}"
else
    echo -e "${YELLOW}⚠ Not running (traces won't appear in Kibana)${NC}"
fi

# Check Kibana
echo -n "Checking Kibana... "
if curl -s http://localhost:5601/api/status > /dev/null 2>&1; then
    echo -e "${GREEN}✔ Running${NC}"
    KIBANA_RUNNING=true
else
    echo -e "${YELLOW}⚠ Not running${NC}"
    KIBANA_RUNNING=false
fi

echo ""

# Install dependencies if needed
if ! python -c "import nbclient" 2>/dev/null; then
    echo "Installing test dependencies..."
    pip install -q nbclient nbformat
fi

# Run tests
print_header "Running E2E Notebook Tests"
echo -e "${CYAN}Each test cell sends a trace to APM Server → Kibana${NC}"
echo ""

cd "$PROJECT_ROOT/tests/e2e"
python run_notebook_tests.py "${ARGS[@]}"
TEST_EXIT_CODE=$?

# Show APM info
echo ""
print_header "APM Traces"

if [ "$KIBANA_RUNNING" = true ]; then
    APM_URL="http://localhost:5601/app/apm/services/elastic-script/transactions"
    echo -e "View traces in Kibana APM:"
    echo -e "  ${BLUE}${APM_URL}${NC}"
    echo ""
    echo -e "Service: ${GREEN}elastic-script${NC}"
    echo -e "Each notebook cell execution appears as a transaction"
    echo ""
    
    if [ "$OPEN_APM" = true ]; then
        echo "Opening Kibana APM..."
        if command -v open &> /dev/null; then
            open "$APM_URL"
        elif command -v xdg-open &> /dev/null; then
            xdg-open "$APM_URL"
        fi
    else
        echo -e "Tip: Run with ${YELLOW}--open-apm${NC} to automatically open APM in browser"
    fi
else
    echo "Kibana is not running. Start it with:"
    echo "  ./scripts/quick-start.sh"
fi

exit $TEST_EXIT_CODE
