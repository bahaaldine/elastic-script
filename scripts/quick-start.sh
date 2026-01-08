#!/bin/bash
#
# elastic-script Quick Start
# ==========================
# This script sets up everything you need to start playing with elastic-script notebooks.
#
# Usage:
#   ./scripts/quick-start.sh          # Full setup (build + start ES + notebooks)
#   ./scripts/quick-start.sh --build  # Just build
#   ./scripts/quick-start.sh --start  # Just start ES (assumes already built)
#   ./scripts/quick-start.sh --notebooks  # Just start notebooks
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
ES_DIR="$PROJECT_ROOT/elasticsearch"
NOTEBOOKS_DIR="$PROJECT_ROOT/notebooks"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_header() {
    echo ""
    echo -e "${BLUE}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║${NC}  $1"
    echo -e "${BLUE}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

print_step() {
    echo -e "${GREEN}▶${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✖${NC} $1"
}

print_success() {
    echo -e "${GREEN}✔${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    print_header "Checking Prerequisites"
    
    # Java
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [ "$JAVA_VERSION" -ge 21 ]; then
            print_success "Java $JAVA_VERSION found"
        else
            print_error "Java 21+ required, found Java $JAVA_VERSION"
            exit 1
        fi
    else
        print_error "Java not found. Please install Java 21+"
        exit 1
    fi
    
    # Python (for notebooks)
    if command -v python3 &> /dev/null; then
        print_success "Python3 found"
    else
        print_warning "Python3 not found. Notebooks won't work."
    fi
    
    # Jupyter
    if command -v jupyter &> /dev/null; then
        print_success "Jupyter found"
    else
        print_warning "Jupyter not found. Run: pip install jupyter"
    fi
    
    # Check if elasticsearch folder exists and has content
    if [ ! -d "$ES_DIR" ] || [ ! -f "$ES_DIR/gradlew" ]; then
        print_step "Elasticsearch submodule not initialized. Setting it up..."
        cd "$PROJECT_ROOT"
        git submodule init
        git submodule update
        
        # Verify it worked
        if [ ! -f "$ES_DIR/gradlew" ]; then
            print_error "Failed to initialize Elasticsearch submodule"
            echo ""
            echo "  Try manually running:"
            echo "    git submodule init"
            echo "    git submodule update"
            echo ""
            exit 1
        fi
        print_success "Elasticsearch submodule initialized"
    else
        print_success "Elasticsearch source found"
    fi
}

# Setup plugin symlink
setup_plugin_symlink() {
    PLUGIN_SOURCE="$PROJECT_ROOT/elastic-script"
    PLUGIN_TARGET="$ES_DIR/x-pack/plugin/elastic-script"
    
    # Check if elastic-script source exists
    if [ ! -d "$PLUGIN_SOURCE" ]; then
        print_error "elastic-script plugin source not found at $PLUGIN_SOURCE"
        exit 1
    fi
    
    # Create symlink if it doesn't exist
    if [ ! -e "$PLUGIN_TARGET" ]; then
        print_step "Creating plugin symlink..."
        ln -s "$PLUGIN_SOURCE" "$PLUGIN_TARGET"
        print_success "Symlink created: elastic-script -> x-pack/plugin/"
    elif [ -L "$PLUGIN_TARGET" ]; then
        print_success "Plugin symlink already exists"
    else
        print_warning "Plugin directory exists but is not a symlink"
    fi
}

# Build elastic-script
build_plugin() {
    print_header "Building elastic-script Plugin"
    
    # Ensure symlink exists
    setup_plugin_symlink
    
    cd "$ES_DIR"
    
    print_step "Compiling elastic-script..."
    ./gradlew :x-pack:plugin:elastic-script:assemble --quiet
    
    print_success "Build complete!"
}

# Start Elasticsearch
start_elasticsearch() {
    print_header "Starting Elasticsearch with elastic-script"
    
    cd "$ES_DIR"
    
    # Check if already running
    if curl -s http://localhost:9200 > /dev/null 2>&1; then
        print_warning "Elasticsearch already running on port 9200"
        return 0
    fi
    
    print_step "Starting Elasticsearch (this takes 1-2 minutes)..."
    echo ""
    echo "    When you see 'started' in the logs, ES is ready!"
    echo "    Press Ctrl+C to stop Elasticsearch"
    echo ""
    
    ./gradlew :run
}

# Start Elasticsearch in background
start_elasticsearch_background() {
    print_header "Starting Elasticsearch in Background"
    
    cd "$ES_DIR"
    
    # Check if already running
    if curl -s http://localhost:9200 > /dev/null 2>&1; then
        print_warning "Elasticsearch already running on port 9200"
        return 0
    fi
    
    print_step "Starting Elasticsearch in background..."
    nohup ./gradlew :run > "$PROJECT_ROOT/elasticsearch.log" 2>&1 &
    ES_PID=$!
    echo $ES_PID > "$PROJECT_ROOT/.es_pid"
    
    print_step "Waiting for Elasticsearch to be ready..."
    for i in {1..60}; do
        if curl -s http://localhost:9200 > /dev/null 2>&1; then
            print_success "Elasticsearch is ready!"
            return 0
        fi
        sleep 2
        echo -n "."
    done
    
    print_error "Elasticsearch failed to start. Check elasticsearch.log"
    exit 1
}

# Load sample data
load_sample_data() {
    print_header "Loading Sample Data"
    
    # Wait for ES to be ready
    for i in {1..30}; do
        if curl -s http://localhost:9200 > /dev/null 2>&1; then
            break
        fi
        sleep 1
    done
    
    if ! curl -s http://localhost:9200 > /dev/null 2>&1; then
        print_error "Elasticsearch not running. Start it first."
        exit 1
    fi
    
    print_step "Creating sample logs index..."
    curl -s -X PUT "localhost:9200/logs-sample" -H "Content-Type: application/json" -d '{
        "mappings": {
            "properties": {
                "timestamp": { "type": "date" },
                "level": { "type": "keyword" },
                "message": { "type": "text" },
                "service": { "type": "keyword" },
                "host": { "type": "keyword" }
            }
        }
    }' > /dev/null
    
    print_step "Indexing sample log data..."
    LEVELS=("INFO" "WARN" "ERROR" "DEBUG")
    SERVICES=("api" "gateway" "auth" "db" "cache")
    for i in {1..20}; do
        LEVEL=${LEVELS[$((RANDOM % ${#LEVELS[@]}))]}
        SERVICE=${SERVICES[$((RANDOM % ${#SERVICES[@]}))]}
        curl -s -X POST "localhost:9200/logs-sample/_doc" -H "Content-Type: application/json" -d "{
            \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",
            \"level\": \"$LEVEL\",
            \"message\": \"Sample log message $i\",
            \"service\": \"$SERVICE\",
            \"host\": \"host-$((i % 3 + 1))\"
        }" > /dev/null
    done
    
    print_step "Creating sample metrics index..."
    curl -s -X PUT "localhost:9200/metrics-sample" -H "Content-Type: application/json" -d '{
        "mappings": {
            "properties": {
                "timestamp": { "type": "date" },
                "metric": { "type": "keyword" },
                "value": { "type": "float" },
                "service": { "type": "keyword" }
            }
        }
    }' > /dev/null
    
    print_step "Indexing sample metrics data..."
    METRICS=("cpu_usage" "memory_usage" "disk_io" "latency_ms")
    METRIC_SERVICES=("api" "gateway" "auth")
    for i in {1..15}; do
        METRIC=${METRICS[$((RANDOM % ${#METRICS[@]}))]}
        SERVICE=${METRIC_SERVICES[$((RANDOM % ${#METRIC_SERVICES[@]}))]}
        VALUE=$((RANDOM % 100))
        curl -s -X POST "localhost:9200/metrics-sample/_doc" -H "Content-Type: application/json" -d "{
            \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",
            \"metric\": \"$METRIC\",
            \"value\": $VALUE,
            \"service\": \"$SERVICE\"
        }" > /dev/null
    done
    
    curl -s -X POST "localhost:9200/_refresh" > /dev/null
    
    print_success "Sample data loaded!"
    echo ""
    echo "    Available indices:"
    curl -s "localhost:9200/_cat/indices?v&h=index,docs.count" | grep -E "logs-|metrics-"
}

# Setup notebooks
setup_notebooks() {
    print_header "Setting Up Notebooks"
    
    cd "$NOTEBOOKS_DIR"
    
    # Check if kernel exists
    if jupyter kernelspec list 2>/dev/null | grep -q plesql; then
        print_success "PL|ESQL kernel already installed"
    else
        print_step "Installing PL|ESQL Jupyter kernel..."
        if [ -f "kernel/install.sh" ]; then
            bash kernel/install.sh
            print_success "Kernel installed!"
        else
            print_warning "Kernel install script not found. Manual setup needed."
        fi
    fi
}

# Start notebooks
start_notebooks() {
    print_header "Starting Jupyter Notebooks"
    
    cd "$NOTEBOOKS_DIR"
    
    print_step "Starting Jupyter..."
    echo ""
    echo "    Opening notebook server at http://localhost:8888"
    echo "    Press Ctrl+C to stop"
    echo ""
    
    python3 -m notebook --notebook-dir="$NOTEBOOKS_DIR"
}

# Show help
show_help() {
    echo ""
    echo "elastic-script Quick Start"
    echo "=========================="
    echo ""
    echo "Usage: ./scripts/quick-start.sh [OPTION]"
    echo ""
    echo "Options:"
    echo "  (no option)     Full setup: build, start ES, load data, start notebooks"
    echo "  --build         Just build the plugin"
    echo "  --start         Start Elasticsearch (foreground)"
    echo "  --start-bg      Start Elasticsearch (background)"
    echo "  --load-data     Load sample data into Elasticsearch"
    echo "  --notebooks     Start Jupyter notebooks"
    echo "  --stop          Stop Elasticsearch and Jupyter notebooks
  --stop-notebooks  Stop only Jupyter notebooks"
    echo "  --status        Check if Elasticsearch is running"
    echo "  --help          Show this help"
    echo ""
}

# Stop Elasticsearch
stop_elasticsearch() {
    print_header "Stopping Elasticsearch"
    
    if [ -f "$PROJECT_ROOT/.es_pid" ]; then
        PID=$(cat "$PROJECT_ROOT/.es_pid")
        if kill -0 $PID 2>/dev/null; then
            kill $PID
            rm "$PROJECT_ROOT/.es_pid"
            print_success "Elasticsearch stopped"
        else
            print_warning "Elasticsearch process not found"
            rm "$PROJECT_ROOT/.es_pid"
        fi
    else
        print_warning "No PID file found"
    fi
}


# Stop Jupyter notebooks
stop_notebooks() {
    print_header "Stopping Jupyter Notebooks"
    
    # Try to find and kill jupyter processes
    if pgrep -f "jupyter" > /dev/null 2>&1; then
        pkill -f "jupyter"
        print_success "Jupyter notebooks stopped"
    elif lsof -ti:8888 > /dev/null 2>&1; then
        lsof -ti:8888 | xargs kill 2>/dev/null
        print_success "Jupyter notebooks stopped (port 8888)"
    else
        print_warning "No Jupyter notebooks running"
    fi
}

# Stop everything
stop_all() {
    stop_notebooks
    stop_elasticsearch
}

# Check status
check_status() {
    print_header "Checking Status"
    
    if curl -s http://localhost:9200 > /dev/null 2>&1; then
        print_success "Elasticsearch is running"
        echo ""
        curl -s http://localhost:9200 | head -10
    else
        print_warning "Elasticsearch is not running"
    fi
}

# Print curl examples
print_examples() {
    print_header "📋 Try These Examples (copy & paste)"
    echo ""
    echo -e "${GREEN}# 1. Simple ESQL query${NC}"
    echo 'curl -X POST "localhost:9200/_escript" -H "Content-Type: application/json" -d '\''{"query": "FROM logs-sample | LIMIT 5"}'\'''
    echo ""
    echo -e "${GREEN}# 2. Define and call a procedure${NC}"
    echo 'curl -X POST "localhost:9200/_escript" -H "Content-Type: application/json" -d '\''{"query": "PROCEDURE get_logs() BEGIN FROM logs-sample | LIMIT 3 END; get_logs()"}'\'''
    echo ""
    echo -e "${GREEN}# 3. Async with pipe-driven continuation${NC}"
    echo 'curl -X POST "localhost:9200/_escript" -H "Content-Type: application/json" -d '\''{"query": "PROCEDURE analyze() BEGIN FROM logs-sample | STATS count=COUNT(*) BY level END; analyze() | ON_DONE process(@result) | TRACK AS \"test\""}'\'''
    echo ""
    echo -e "${GREEN}# 4. Check execution status${NC}"
    echo 'curl -X POST "localhost:9200/_escript" -H "Content-Type: application/json" -d '\''{"query": "EXECUTION(\"test\") | STATUS"}'\'''
    echo ""
    echo -e "${GREEN}# 5. Parallel execution${NC}"
    echo 'curl -X POST "localhost:9200/_escript" -H "Content-Type: application/json" -d '\''{"query": "PARALLEL [task_a(), task_b()] | ON_ALL_DONE aggregate(@results)"}'\'''
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
}

# Main
case "${1:-}" in
    --help|-h)
        show_help
        ;;
    --build)
        check_prerequisites
        build_plugin
        ;;
    --start)
        start_elasticsearch
        ;;
    --start-bg)
        start_elasticsearch_background
        ;;
    --load-data)
        load_sample_data
        ;;
    --notebooks)
        start_notebooks
        ;;
    --stop-notebooks)
        stop_notebooks
        ;;
    --stop)
        stop_all
        ;;
    --status)
        check_status
        ;;
    "")
        # Full setup
        print_header "🚀 elastic-script Quick Start"
        
        # Check if ES is already running
        if curl -s http://localhost:9200 > /dev/null 2>&1; then
            print_success "Elasticsearch is already running!"
            echo ""
            echo "Skipping build and startup. Going straight to examples and notebooks."
            echo ""
            read -p "Continue? [Y/n] " -n 1 -r
            echo
            if [[ ! $REPLY =~ ^[Nn]$ ]]; then
                print_examples
                setup_notebooks
                echo ""
                print_header "🚀 Launching Jupyter Notebooks"
                cd "$NOTEBOOKS_DIR"
                jupyter notebook --notebook-dir="$NOTEBOOKS_DIR" &
                JUPYTER_PID=$!
                echo ""
                print_success "Jupyter started at http://localhost:8888"
                echo ""
                echo "Press Enter to continue (Jupyter runs in background)..."
                read
            fi
        else
            echo "This will:"
            echo "  1. Check prerequisites"
            echo "  2. Build the plugin"
            echo "  3. Start Elasticsearch"
            echo "  4. Load sample data"
            echo "  5. Show examples & start Jupyter notebooks"
            echo ""
            read -p "Continue? [Y/n] " -n 1 -r
            echo
            if [[ ! $REPLY =~ ^[Nn]$ ]]; then
                check_prerequisites
                build_plugin
                start_elasticsearch_background
                load_sample_data
                print_examples
                setup_notebooks
                echo ""
                print_header "🚀 Launching Jupyter Notebooks"
                cd "$NOTEBOOKS_DIR"
                jupyter notebook --notebook-dir="$NOTEBOOKS_DIR" &
                JUPYTER_PID=$!
                echo ""
                print_success "Jupyter started at http://localhost:8888"
                echo ""
                echo "Press Enter to continue (Jupyter runs in background)..."
                read
            fi
        fi
        ;;
    *)
        print_error "Unknown option: $1"
        show_help
        exit 1
        ;;
esac

                read
            fi
        fi
        ;;
    *)
        print_error "Unknown option: $1"
        show_help
        exit 1
        ;;
esac
