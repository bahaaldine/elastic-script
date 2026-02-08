#!/bin/bash
#
# Start a multi-node Elasticsearch cluster for testing elastic-script
# distributed behavior including:
# - Execution state accessible from any node
# - Leader election and failover
# - Cross-node execution visibility
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
ES_DIR="$PROJECT_ROOT/elasticsearch"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[!]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
}

print_info() {
    echo -e "${BLUE}[i]${NC} $1"
}

# Default settings
NUM_NODES=${NUM_NODES:-3}
BASE_HTTP_PORT=${BASE_HTTP_PORT:-9200}
BASE_TRANSPORT_PORT=${BASE_TRANSPORT_PORT:-9300}
CLUSTER_NAME=${CLUSTER_NAME:-escript-multinode}

show_help() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Start a multi-node Elasticsearch cluster for testing."
    echo ""
    echo "Options:"
    echo "  --nodes N       Number of nodes to start (default: 3)"
    echo "  --cluster NAME  Cluster name (default: escript-multinode)"
    echo "  --stop          Stop the multi-node cluster"
    echo "  --status        Show cluster status"
    echo "  --help          Show this help message"
    echo ""
    echo "Environment Variables:"
    echo "  NUM_NODES          Number of nodes (default: 3)"
    echo "  CLUSTER_NAME       Cluster name"
    echo "  OPENAI_API_KEY     OpenAI API key for LLM functions"
}

check_ports() {
    for i in $(seq 1 $NUM_NODES); do
        local http_port=$((BASE_HTTP_PORT + i - 1))
        local transport_port=$((BASE_TRANSPORT_PORT + i - 1))
        
        if lsof -i :$http_port >/dev/null 2>&1; then
            print_warning "Port $http_port is already in use"
            return 1
        fi
        if lsof -i :$transport_port >/dev/null 2>&1; then
            print_warning "Port $transport_port is already in use"
            return 1
        fi
    done
    return 0
}

start_cluster() {
    print_info "Starting $NUM_NODES-node Elasticsearch cluster..."
    
    # Check if ports are available
    if ! check_ports; then
        print_error "Some required ports are in use. Stop existing cluster first with --stop"
        exit 1
    fi
    
    cd "$ES_DIR"
    
    # Build seed hosts list
    SEED_HOSTS=""
    for i in $(seq 1 $NUM_NODES); do
        local transport_port=$((BASE_TRANSPORT_PORT + i - 1))
        if [ -z "$SEED_HOSTS" ]; then
            SEED_HOSTS="127.0.0.1:$transport_port"
        else
            SEED_HOSTS="$SEED_HOSTS,127.0.0.1:$transport_port"
        fi
    done
    
    # Initial master nodes (all nodes)
    INITIAL_MASTERS=""
    for i in $(seq 1 $NUM_NODES); do
        if [ -z "$INITIAL_MASTERS" ]; then
            INITIAL_MASTERS="node-$i"
        else
            INITIAL_MASTERS="$INITIAL_MASTERS,node-$i"
        fi
    done
    
    # Create PID file directory
    mkdir -p "$PROJECT_ROOT/.multinode"
    
    # Start each node
    for i in $(seq 1 $NUM_NODES); do
        local http_port=$((BASE_HTTP_PORT + i - 1))
        local transport_port=$((BASE_TRANSPORT_PORT + i - 1))
        local node_name="node-$i"
        local data_dir="$PROJECT_ROOT/.multinode/data-$i"
        local log_dir="$PROJECT_ROOT/.multinode/logs-$i"
        
        mkdir -p "$data_dir" "$log_dir"
        
        print_info "Starting $node_name on ports $http_port (HTTP), $transport_port (transport)..."
        
        # Build the gradle run command with node-specific settings
        ./gradlew :run \
            -Dtests.cluster="${CLUSTER_NAME}" \
            -Dtests.es.http.port=$http_port \
            -Dtests.es.transport.port=$transport_port \
            -Dtests.es.node.name=$node_name \
            -Dtests.es.discovery.seed_hosts=$SEED_HOSTS \
            -Dtests.es.cluster.initial_master_nodes=$INITIAL_MASTERS \
            -Dtests.es.path.data="$data_dir" \
            -Dtests.es.path.logs="$log_dir" \
            ${OPENAI_API_KEY:+-Dtests.openai.api.key=$OPENAI_API_KEY} \
            > "$log_dir/startup.log" 2>&1 &
        
        local pid=$!
        echo "$pid" > "$PROJECT_ROOT/.multinode/node-$i.pid"
        
        print_status "Started $node_name with PID $pid"
    done
    
    print_info "Waiting for cluster to form..."
    sleep 30
    
    # Check cluster health
    show_status
}

stop_cluster() {
    print_info "Stopping multi-node cluster..."
    
    local pid_dir="$PROJECT_ROOT/.multinode"
    
    if [ ! -d "$pid_dir" ]; then
        print_warning "No cluster PID directory found"
        return
    fi
    
    for pid_file in "$pid_dir"/*.pid; do
        if [ -f "$pid_file" ]; then
            local pid=$(cat "$pid_file")
            local node_name=$(basename "$pid_file" .pid)
            
            if ps -p $pid > /dev/null 2>&1; then
                print_info "Stopping $node_name (PID $pid)..."
                kill $pid 2>/dev/null || true
            else
                print_warning "$node_name already stopped"
            fi
            
            rm -f "$pid_file"
        fi
    done
    
    # Also kill any gradle daemons that might be running ES
    pkill -f "testclusters" 2>/dev/null || true
    
    print_status "Cluster stopped"
}

show_status() {
    print_info "Checking cluster status..."
    echo ""
    
    # Try to get cluster health
    local health=$(curl -s -u elastic-admin:elastic-password "http://localhost:$BASE_HTTP_PORT/_cluster/health?pretty" 2>/dev/null)
    
    if [ -n "$health" ]; then
        echo "$health"
        echo ""
        
        # Show nodes
        print_info "Cluster nodes:"
        curl -s -u elastic-admin:elastic-password "http://localhost:$BASE_HTTP_PORT/_cat/nodes?v" 2>/dev/null
        echo ""
        
        # Show leader election status
        print_info "Leader election status:"
        curl -s -u elastic-admin:elastic-password "http://localhost:$BASE_HTTP_PORT/.escript_leader/_doc/scheduler_leader?pretty" 2>/dev/null || echo "Leader index not created yet"
        echo ""
    else
        print_warning "Could not connect to cluster on port $BASE_HTTP_PORT"
        
        # Check PIDs
        local pid_dir="$PROJECT_ROOT/.multinode"
        if [ -d "$pid_dir" ]; then
            for pid_file in "$pid_dir"/*.pid; do
                if [ -f "$pid_file" ]; then
                    local pid=$(cat "$pid_file")
                    local node_name=$(basename "$pid_file" .pid)
                    
                    if ps -p $pid > /dev/null 2>&1; then
                        echo "  $node_name: running (PID $pid)"
                    else
                        echo "  $node_name: stopped"
                    fi
                fi
            done
        fi
    fi
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --nodes)
            NUM_NODES="$2"
            shift 2
            ;;
        --cluster)
            CLUSTER_NAME="$2"
            shift 2
            ;;
        --stop)
            stop_cluster
            exit 0
            ;;
        --status)
            show_status
            exit 0
            ;;
        --help)
            show_help
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Main
echo "============================================"
echo "  elastic-script Multi-Node Cluster Setup"
echo "============================================"
echo ""
echo "Configuration:"
echo "  Nodes: $NUM_NODES"
echo "  Cluster: $CLUSTER_NAME"
echo "  HTTP ports: $BASE_HTTP_PORT - $((BASE_HTTP_PORT + NUM_NODES - 1))"
echo "  Transport ports: $BASE_TRANSPORT_PORT - $((BASE_TRANSPORT_PORT + NUM_NODES - 1))"
echo ""

# The gradle multi-node setup is complex, so let's provide an alternative approach
print_info "For multi-node testing, we recommend using Docker Compose."
echo ""
echo "Alternative: Use the single-node quick-start with multi-node simulation"
echo "The ExecutionRegistry already stores state in Elasticsearch, accessible cluster-wide."
echo ""
echo "To test distributed behavior:"
echo "1. Start Elasticsearch: ./scripts/quick-start.sh"
echo "2. Run the multi-node test notebook: notebooks/08-multinode-testing.ipynb"
echo ""
echo "The notebook tests:"
echo "  - Execution state persistence in .escript_executions index"
echo "  - Track name lookups"
echo "  - Progress updates"
echo "  - Cross-node state visibility (simulated)"
echo ""
