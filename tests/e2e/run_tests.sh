#!/bin/bash
# E2E Test Runner for elastic-script
# 
# Usage:
#   ./run_tests.sh                    # Run all tests
#   ./run_tests.sh --notebook 01      # Run specific notebook
#   ./run_tests.sh --verbose          # Verbose output
#   ./run_tests.sh --setup            # Setup + run tests

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

print_header() {
    echo -e "\n${GREEN}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║${NC}  $1"
    echo -e "${GREEN}╚══════════════════════════════════════════════════════════════╝${NC}\n"
}

# Check for --setup flag
SETUP=false
ARGS=()
for arg in "$@"; do
    if [ "$arg" == "--setup" ]; then
        SETUP=true
    else
        ARGS+=("$arg")
    fi
done

# Setup infrastructure if requested
if [ "$SETUP" = true ]; then
    print_header "Setting up infrastructure..."
    
    # Check if ES is running
    if curl -s -u elastic-admin:elastic-password http://localhost:9200 > /dev/null 2>&1; then
        echo -e "${GREEN}✔${NC} Elasticsearch already running"
    else
        echo "Starting Elasticsearch..."
        cd "$PROJECT_ROOT"
        ./scripts/quick-start.sh &
        
        # Wait for ES
        echo "Waiting for Elasticsearch to start..."
        for i in {1..60}; do
            if curl -s -u elastic-admin:elastic-password http://localhost:9200 > /dev/null 2>&1; then
                echo -e "${GREEN}✔${NC} Elasticsearch is ready"
                break
            fi
            sleep 2
        done
    fi
fi

# Install dependencies
print_header "Installing E2E test dependencies..."
pip install -q -r "$SCRIPT_DIR/requirements.txt"

# Run tests
print_header "Running E2E notebook tests..."
cd "$SCRIPT_DIR"
python run_notebook_tests.py "${ARGS[@]}"
