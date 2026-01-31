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

# Elastic Distribution of OpenTelemetry (EDOT) configuration
# NOTE: EDOT is disabled for now as ES_JAVA_OPTS doesn't work with Gradle :run
# TODO: Integrate EDOT properly with testclusters or production ES
EDOT_ENABLED=false
EDOT_AGENT_PATH="$PROJECT_ROOT/elastic-otel-javaagent.jar"
EDOT_AGENT_VERSION="1.3.0"
EDOT_AGENT_URL="https://repo1.maven.org/maven2/co/elastic/otel/elastic-otel-javaagent/${EDOT_AGENT_VERSION}/elastic-otel-javaagent-${EDOT_AGENT_VERSION}.jar"

# Kibana configuration - must match Elasticsearch version exactly
KIBANA_VERSION="9.4.0-SNAPSHOT"

# OpenTelemetry Collector configuration
OTEL_COLLECTOR_VERSION="0.116.0"
OTEL_COLLECTOR_DIR="$PROJECT_ROOT/otel-collector"
OTEL_COLLECTOR_BINARY="$OTEL_COLLECTOR_DIR/otelcol-contrib"
OTEL_COLLECTOR_CONFIG="$OTEL_COLLECTOR_DIR/config.yaml"
OTEL_COLLECTOR_LOG="$OTEL_COLLECTOR_DIR/collector.log"
OTEL_COLLECTOR_PID="$OTEL_COLLECTOR_DIR/collector.pid"

# APM Server configuration (for OTLP trace ingestion)
APM_SERVER_VERSION="8.17.0"
APM_SERVER_DIR="$PROJECT_ROOT/apm-server"
APM_SERVER_BINARY="$APM_SERVER_DIR/apm-server"
APM_SERVER_CONFIG="$APM_SERVER_DIR/apm-server.yml"
APM_SERVER_LOG="$APM_SERVER_DIR/apm-server.log"
APM_SERVER_PID="$APM_SERVER_DIR/apm-server.pid"
APM_SERVER_PORT=8200

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

# Prompt for OpenAI API key (optional)
prompt_openai_key() {
    print_header "OpenAI API Key (Optional)"
    
    # Check if already set
    if [ -n "$OPENAI_API_KEY" ]; then
        print_success "OPENAI_API_KEY already set in environment"
        return 0
    fi
    
    echo "The AI features (LLM_COMPLETE, etc.) require an OpenAI API key."
    echo "You can skip this if you don't need AI features."
    echo ""
    read -p "Enter OpenAI API key (or press Enter to skip): " -r OPENAI_KEY
    
    if [ -n "$OPENAI_KEY" ]; then
        export OPENAI_API_KEY="$OPENAI_KEY"
        print_success "OpenAI API key configured"
    else
        print_warning "Skipped. AI features won't work without OPENAI_API_KEY."
    fi
    echo ""
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
        echo "    (This may take a few minutes - the Elasticsearch repo is large)"
        cd "$PROJECT_ROOT"
        git submodule init
        
        # Use --progress to show download status (otherwise appears hung)
        # Use --depth 1 for faster clone (only need current commit, not full history)
        git submodule update --progress --depth 1
        
        # Verify it worked
        if [ ! -f "$ES_DIR/gradlew" ]; then
            print_error "Failed to initialize Elasticsearch submodule"
            echo ""
            echo "  The Elasticsearch repo is large (~500MB). Try manually:"
            echo "    cd $PROJECT_ROOT"
            echo "    git submodule init"
            echo "    git submodule update --progress --depth 1"
            echo ""
            echo "  Or if that fails, try a direct clone:"
            echo "    rm -rf elasticsearch"
            echo "    git clone --depth 1 https://github.com/elastic/elasticsearch.git"
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
    
    # Pass OPENAI_API_KEY if set
    if [ -n "$OPENAI_API_KEY" ]; then
        ./gradlew --stop > /dev/null 2>&1 || true
        OPENAI_API_KEY="$OPENAI_API_KEY" ./gradlew :run --no-daemon
    else
        ./gradlew :run
    fi
}

# Start Elasticsearch in background
# Setup Elastic Distribution of OpenTelemetry (EDOT) agent
setup_edot() {
    print_header "Setting up Elastic OpenTelemetry (EDOT) Tracing"
    
    # Download EDOT agent if not present
    if [ ! -f "$EDOT_AGENT_PATH" ]; then
        print_step "Downloading EDOT Java agent v${EDOT_AGENT_VERSION}..."
        curl -L -o "$EDOT_AGENT_PATH" "$EDOT_AGENT_URL"
        if [ $? -eq 0 ]; then
            print_success "Downloaded EDOT agent to $EDOT_AGENT_PATH"
        else
            print_error "Failed to download EDOT agent"
            EDOT_ENABLED=false
            return 1
        fi
    else
        print_step "EDOT agent already present at $EDOT_AGENT_PATH"
    fi
    
    # Configure EDOT to send traces to local Elasticsearch APM
    # When Kibana is running, traces are visible in Observability > APM
    export OTEL_SERVICE_NAME="${OTEL_SERVICE_NAME:-elastic-script}"
    export OTEL_EXPORTER_OTLP_ENDPOINT="${OTEL_EXPORTER_OTLP_ENDPOINT:-http://localhost:9200}"
    export OTEL_EXPORTER_OTLP_PROTOCOL="${OTEL_EXPORTER_OTLP_PROTOCOL:-http/protobuf}"
    export ELASTIC_OTEL_JAVA_EXPERIMENTAL_SPAN_STACKTRACE_MIN_DURATION="${ELASTIC_OTEL_JAVA_EXPERIMENTAL_SPAN_STACKTRACE_MIN_DURATION:-5ms}"
    
    print_step "EDOT Configuration:"
    echo "  OTEL_SERVICE_NAME=$OTEL_SERVICE_NAME"
    echo "  OTEL_EXPORTER_OTLP_ENDPOINT=$OTEL_EXPORTER_OTLP_ENDPOINT"
    echo "  OTEL_EXPORTER_OTLP_PROTOCOL=$OTEL_EXPORTER_OTLP_PROTOCOL"
    
    EDOT_ENABLED=true
    print_success "EDOT tracing enabled - traces will appear in Kibana APM"
}

start_elasticsearch_background() {
    print_header "Starting Elasticsearch in Background"
    
    cd "$ES_DIR"
    
    # Check if already running
    if curl -s http://localhost:9200 > /dev/null 2>&1; then
        print_warning "Elasticsearch already running on port 9200"
        return 0
    fi
    
    print_step "Starting Elasticsearch in background..."
    
    # Build environment variables
    print_step "Stopping Gradle daemon to ensure environment is inherited..."
    ./gradlew --stop > /dev/null 2>&1 || true
    
    # Build Java options for EDOT agent
    local JAVA_OPTS=""
    if [ "$EDOT_ENABLED" = true ] && [ -f "$EDOT_AGENT_PATH" ]; then
        JAVA_OPTS="-javaagent:$EDOT_AGENT_PATH"
        print_step "Starting with EDOT Java agent enabled"
    fi
    
    # Configure environment
    if [ -n "$OPENAI_API_KEY" ]; then
        print_step "Starting with OPENAI_API_KEY configured"
    fi
    
    # Start Elasticsearch with all configurations
    OPENAI_API_KEY="${OPENAI_API_KEY:-}" \
        ES_JAVA_OPTS="${JAVA_OPTS}" \
        OTEL_SERVICE_NAME="${OTEL_SERVICE_NAME:-elastic-script}" \
        OTEL_EXPORTER_OTLP_ENDPOINT="${OTEL_EXPORTER_OTLP_ENDPOINT:-http://localhost:9200}" \
        OTEL_EXPORTER_OTLP_PROTOCOL="${OTEL_EXPORTER_OTLP_PROTOCOL:-http/protobuf}" \
        nohup ./gradlew :run --no-daemon > "$PROJECT_ROOT/elasticsearch.log" 2>&1 &
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
    
    local AUTH="-u elastic-admin:elastic-password"
    local ES="localhost:9200"
    
    # Wait for ES to be ready
    for i in {1..30}; do
        if curl -s $AUTH http://$ES > /dev/null 2>&1; then
            break
        fi
        sleep 1
    done
    
    if ! curl -s $AUTH http://$ES > /dev/null 2>&1; then
        print_error "Elasticsearch not running. Start it first."
        exit 1
    fi
    
    # =========================================================================
    # 1. LOGS - Application logs with realistic messages
    # =========================================================================
    print_step "Creating logs-sample index..."
    curl -s $AUTH -X DELETE "$ES/logs-sample" > /dev/null 2>&1
    curl -s $AUTH -X PUT "$ES/logs-sample" -H "Content-Type: application/json" -d '{
        "settings": { "number_of_replicas": 0 },
        "mappings": {
            "properties": {
                "@timestamp": { "type": "date" },
                "level": { "type": "keyword" },
                "message": { "type": "text" },
                "service": { "type": "keyword" },
                "host": { "type": "keyword" },
                "trace_id": { "type": "keyword" },
                "user_id": { "type": "keyword" },
                "duration_ms": { "type": "integer" },
                "status_code": { "type": "integer" },
                "method": { "type": "keyword" },
                "path": { "type": "keyword" }
            }
        }
    }' > /dev/null
    
    print_step "Indexing application logs (100 documents)..."
    
    SERVICES=("api-gateway" "user-service" "order-service" "payment-service" "notification-service" "inventory-service" "auth-service" "search-service")
    HOSTS=("prod-app-01" "prod-app-02" "prod-app-03" "prod-worker-01" "prod-worker-02")
    METHODS=("GET" "POST" "PUT" "DELETE" "PATCH")
    API_PATHS=("/api/v1/users" "/api/v1/orders" "/api/v1/products" "/api/v1/payments" "/api/v1/search" "/health" "/api/v1/auth/login" "/api/v1/cart")
    
    # First, create 20 guaranteed ERROR logs with detailed, varied messages
    ERROR_DETAILS=(
        "Connection refused to database server db-primary.internal:5432 after 3 retries. Last error: Connection timed out"
        "Authentication failed for user john.doe@example.com - Invalid credentials. IP: 192.168.1.100, Attempt: 5/5"
        "Timeout waiting for response from payment-service after 30000ms. Transaction ID: TXN-78234"
        "OutOfMemoryError: Java heap space. Current heap: 7.8GB, Max heap: 8GB. Consider increasing -Xmx"
        "SSL handshake failed with upstream server api.stripe.com: certificate has expired"
        "Disk space critical on /var/log: 98% used (47.2GB/48GB). Immediate action required"
        "NullPointerException in OrderProcessor.processPayment at line 234. Order ID: ORD-445566"
        "Failed to connect to Redis cache cluster: All nodes are unreachable. Falling back to database"
        "Rate limit exceeded for API key sk-prod-***89. Limit: 1000 req/min, Current: 1247 req/min"
        "Kafka consumer lag critical: Topic orders-events partition 3 lag is 50000 messages"
        "Database query timeout after 60s: SELECT * FROM orders WHERE status = 'pending' ORDER BY created_at"
        "Service discovery failed: No healthy instances found for inventory-service in region us-east-1"
        "File upload failed: Maximum file size exceeded. Received: 52MB, Limit: 50MB. User: user-42"
        "Elasticsearch cluster health RED: 2 of 5 shards unassigned. Index: logs-2024.01"
        "Circuit breaker OPEN for payment-service: Failure rate 67% exceeds threshold 50%"
        "Failed to deserialize message from queue orders-dlq: Unexpected token at position 234"
        "Permission denied: User analyst-bob attempted to access /admin/settings without admin role"
        "External API error from shipping-partner.com: HTTP 503 Service Unavailable after 5 retries"
        "Memory leak detected in user-session-cache: Size grew from 100MB to 2.3GB in 1 hour"
        "Deadlock detected in database connection pool. Threads waiting: 15. Max pool size: 10"
    )
    
    for i in {0..19}; do
        MSG="${ERROR_DETAILS[$i]}"
        SERVICE=${SERVICES[$((RANDOM % ${#SERVICES[@]}))]}
        HOST=${HOSTS[$((RANDOM % ${#HOSTS[@]}))]}
        METHOD=${METHODS[$((RANDOM % ${#METHODS[@]}))]}
        API_PATH=${API_PATHS[$((RANDOM % ${#API_PATHS[@]}))]}
        TRACE_ID=$(printf '%08x-%04x-%04x' $RANDOM $RANDOM $RANDOM)
        USER_ID="user-$((RANDOM % 50 + 1))"
        DURATION=$((RANDOM % 30000 + 5000))  # Errors typically have longer durations
        
        HOURS_AGO=$((RANDOM % 12))  # More recent errors
        MINS_AGO=$((RANDOM % 60))
        if [[ "$OSTYPE" == "darwin"* ]]; then
            TS=$(date -u -v-${HOURS_AGO}H -v-${MINS_AGO}M +%Y-%m-%dT%H:%M:%SZ)
        else
            TS=$(date -u -d "$HOURS_AGO hours ago $MINS_AGO minutes ago" +%Y-%m-%dT%H:%M:%SZ)
        fi
        
        curl -s $AUTH -X POST "$ES/logs-sample/_doc" -H "Content-Type: application/json" -d "{
            \"@timestamp\": \"$TS\",
            \"level\": \"ERROR\",
            \"message\": \"$MSG\",
            \"service\": \"$SERVICE\",
            \"host\": \"$HOST\",
            \"trace_id\": \"$TRACE_ID\",
            \"user_id\": \"$USER_ID\",
            \"duration_ms\": $DURATION,
            \"status_code\": 500,
            \"method\": \"$METHOD\",
            \"path\": \"$API_PATH\"
        }" > /dev/null
    done
    
    # Now add INFO, DEBUG, and WARN logs
    INFO_MSGS=("Request processed successfully" "User authenticated" "Cache hit for key" "Database connection established" "Health check passed" "Configuration loaded" "Session created" "Payment processed" "Order confirmed" "Email sent successfully")
    WARN_MSGS=("High memory usage detected: 78% utilized" "Slow query execution: 2340ms for user lookup" "Rate limit approaching: 890/1000 requests" "Certificate expires in 30 days" "Deprecated API v1 called, please migrate to v2" "Retry attempt 2/3 for external service call" "Connection pool 85% exhausted" "Message queue size growing: 5000 pending")
    DEBUG_MSGS=("Entering function processOrder with orderId=12345" "Query parameters validated successfully" "Cache miss for key user:42, fetching from database" "Serializing response object to JSON" "Checking user permissions for resource /api/admin")
    
    for i in {1..80}; do
        # Weight: 55% INFO, 25% DEBUG, 20% WARN
        RAND=$((RANDOM % 100))
        if [ $RAND -lt 55 ]; then
            LEVEL="INFO"
            MSG=${INFO_MSGS[$((RANDOM % ${#INFO_MSGS[@]}))]}
            STATUS_CODE=200
        elif [ $RAND -lt 80 ]; then
            LEVEL="DEBUG"
            MSG=${DEBUG_MSGS[$((RANDOM % ${#DEBUG_MSGS[@]}))]}
            STATUS_CODE=200
        else
            LEVEL="WARN"
            MSG=${WARN_MSGS[$((RANDOM % ${#WARN_MSGS[@]}))]}
            STATUS_CODE=$((RANDOM % 2 == 0 ? 200 : 429))
        fi
        
        SERVICE=${SERVICES[$((RANDOM % ${#SERVICES[@]}))]}
        HOST=${HOSTS[$((RANDOM % ${#HOSTS[@]}))]}
        METHOD=${METHODS[$((RANDOM % ${#METHODS[@]}))]}
        API_PATH=${API_PATHS[$((RANDOM % ${#API_PATHS[@]}))]}
        DURATION=$((RANDOM % 500 + 10))
        TRACE_ID=$(printf '%08x-%04x-%04x' $RANDOM $RANDOM $RANDOM)
        USER_ID="user-$((RANDOM % 50 + 1))"
        
        HOURS_AGO=$((RANDOM % 24))
        MINS_AGO=$((RANDOM % 60))
        if [[ "$OSTYPE" == "darwin"* ]]; then
            TS=$(date -u -v-${HOURS_AGO}H -v-${MINS_AGO}M +%Y-%m-%dT%H:%M:%SZ)
        else
            TS=$(date -u -d "$HOURS_AGO hours ago $MINS_AGO minutes ago" +%Y-%m-%dT%H:%M:%SZ)
        fi
        
        curl -s $AUTH -X POST "$ES/logs-sample/_doc" -H "Content-Type: application/json" -d "{
            \"@timestamp\": \"$TS\",
            \"level\": \"$LEVEL\",
            \"message\": \"$MSG\",
            \"service\": \"$SERVICE\",
            \"host\": \"$HOST\",
            \"trace_id\": \"$TRACE_ID\",
            \"user_id\": \"$USER_ID\",
            \"duration_ms\": $DURATION,
            \"status_code\": $STATUS_CODE,
            \"method\": \"$METHOD\",
            \"path\": \"$API_PATH\"
        }" > /dev/null
    done
    
    # =========================================================================
    # 2. METRICS - System and application metrics
    # =========================================================================
    print_step "Creating metrics-sample index..."
    curl -s $AUTH -X DELETE "$ES/metrics-sample" > /dev/null 2>&1
    curl -s $AUTH -X PUT "$ES/metrics-sample" -H "Content-Type: application/json" -d '{
        "settings": { "number_of_replicas": 0 },
        "mappings": {
            "properties": {
                "@timestamp": { "type": "date" },
                "metric_name": { "type": "keyword" },
                "value": { "type": "float" },
                "unit": { "type": "keyword" },
                "service": { "type": "keyword" },
                "host": { "type": "keyword" },
                "environment": { "type": "keyword" }
            }
        }
    }' > /dev/null
    
    print_step "Indexing system metrics (80 documents)..."
    
    METRIC_NAMES=("cpu_percent" "memory_percent" "disk_used_percent" "network_in_bytes" "network_out_bytes" "request_latency_p99" "request_rate" "error_rate" "gc_pause_ms" "thread_count")
    UNITS=("percent" "percent" "percent" "bytes" "bytes" "ms" "req/s" "percent" "ms" "count")
    ENVIRONMENTS=("production" "staging")
    
    for i in {1..80}; do
        METRIC_IDX=$((RANDOM % ${#METRIC_NAMES[@]}))
        METRIC_NAME=${METRIC_NAMES[$METRIC_IDX]}
        UNIT=${UNITS[$METRIC_IDX]}
        
        # Generate realistic values based on metric type
        case $METRIC_NAME in
            "cpu_percent"|"memory_percent"|"disk_used_percent")
                VALUE=$((RANDOM % 40 + 30)).$((RANDOM % 100))
                ;;
            "network_in_bytes"|"network_out_bytes")
                VALUE=$((RANDOM % 10000000 + 100000))
                ;;
            "request_latency_p99")
                VALUE=$((RANDOM % 500 + 50)).$((RANDOM % 100))
                ;;
            "request_rate")
                VALUE=$((RANDOM % 1000 + 100)).$((RANDOM % 100))
                ;;
            "error_rate")
                VALUE=$((RANDOM % 5)).$((RANDOM % 100))
                ;;
            "gc_pause_ms")
                VALUE=$((RANDOM % 100 + 5))
                ;;
            "thread_count")
                VALUE=$((RANDOM % 200 + 50))
                ;;
        esac
        
        SERVICE=${SERVICES[$((RANDOM % ${#SERVICES[@]}))]}
        HOST=${HOSTS[$((RANDOM % ${#HOSTS[@]}))]}
        ENV=${ENVIRONMENTS[$((RANDOM % ${#ENVIRONMENTS[@]}))]}
        
        HOURS_AGO=$((RANDOM % 24))
        if [[ "$OSTYPE" == "darwin"* ]]; then
            TS=$(date -u -v-${HOURS_AGO}H +%Y-%m-%dT%H:%M:%SZ)
        else
            TS=$(date -u -d "$HOURS_AGO hours ago" +%Y-%m-%dT%H:%M:%SZ)
        fi
        
        curl -s $AUTH -X POST "$ES/metrics-sample/_doc" -H "Content-Type: application/json" -d "{
            \"@timestamp\": \"$TS\",
            \"metric_name\": \"$METRIC_NAME\",
            \"value\": $VALUE,
            \"unit\": \"$UNIT\",
            \"service\": \"$SERVICE\",
            \"host\": \"$HOST\",
            \"environment\": \"$ENV\"
        }" > /dev/null
    done
    
    # =========================================================================
    # 3. USERS - User profiles for testing
    # =========================================================================
    print_step "Creating users-sample index..."
    curl -s $AUTH -X DELETE "$ES/users-sample" > /dev/null 2>&1
    curl -s $AUTH -X PUT "$ES/users-sample" -H "Content-Type: application/json" -d '{
        "settings": { "number_of_replicas": 0 },
        "mappings": {
            "properties": {
                "user_id": { "type": "keyword" },
                "email": { "type": "keyword" },
                "name": { "type": "text" },
                "role": { "type": "keyword" },
                "department": { "type": "keyword" },
                "created_at": { "type": "date" },
                "last_login": { "type": "date" },
                "active": { "type": "boolean" },
                "login_count": { "type": "integer" }
            }
        }
    }' > /dev/null
    
    print_step "Indexing user profiles (30 documents)..."
    
    FIRST_NAMES=("Alice" "Bob" "Charlie" "Diana" "Eve" "Frank" "Grace" "Henry" "Ivy" "Jack" "Kate" "Leo" "Maya" "Nick" "Olivia" "Peter" "Quinn" "Rachel" "Sam" "Tara")
    LAST_NAMES=("Smith" "Johnson" "Williams" "Brown" "Jones" "Garcia" "Miller" "Davis" "Wilson" "Taylor")
    ROLES=("admin" "developer" "analyst" "manager" "viewer")
    DEPARTMENTS=("Engineering" "Sales" "Marketing" "Support" "Finance" "Operations")
    
    for i in {1..30}; do
        FIRST=${FIRST_NAMES[$((RANDOM % ${#FIRST_NAMES[@]}))]}
        LAST=${LAST_NAMES[$((RANDOM % ${#LAST_NAMES[@]}))]}
        NAME="$FIRST $LAST"
        FIRST_LOWER=$(echo "$FIRST" | tr '[:upper:]' '[:lower:]')
        LAST_LOWER=$(echo "$LAST" | tr '[:upper:]' '[:lower:]')
        EMAIL="${FIRST_LOWER}.${LAST_LOWER}@example.com"
        ROLE=${ROLES[$((RANDOM % ${#ROLES[@]}))]}
        DEPT=${DEPARTMENTS[$((RANDOM % ${#DEPARTMENTS[@]}))]}
        ACTIVE=$([[ $((RANDOM % 10)) -lt 9 ]] && echo "true" || echo "false")
        LOGIN_COUNT=$((RANDOM % 500 + 1))
        
        DAYS_AGO=$((RANDOM % 365))
        if [[ "$OSTYPE" == "darwin"* ]]; then
            CREATED=$(date -u -v-${DAYS_AGO}d +%Y-%m-%dT%H:%M:%SZ)
            LAST_LOGIN=$(date -u -v-$((RANDOM % 30))d +%Y-%m-%dT%H:%M:%SZ)
        else
            CREATED=$(date -u -d "$DAYS_AGO days ago" +%Y-%m-%dT%H:%M:%SZ)
            LAST_LOGIN=$(date -u -d "$((RANDOM % 30)) days ago" +%Y-%m-%dT%H:%M:%SZ)
        fi
        
        curl -s $AUTH -X POST "$ES/users-sample/_doc" -H "Content-Type: application/json" -d "{
            \"user_id\": \"user-$i\",
            \"email\": \"$EMAIL\",
            \"name\": \"$NAME\",
            \"role\": \"$ROLE\",
            \"department\": \"$DEPT\",
            \"created_at\": \"$CREATED\",
            \"last_login\": \"$LAST_LOGIN\",
            \"active\": $ACTIVE,
            \"login_count\": $LOGIN_COUNT
        }" > /dev/null
    done
    
    # =========================================================================
    # 4. ORDERS - E-commerce orders
    # =========================================================================
    print_step "Creating orders-sample index..."
    curl -s $AUTH -X DELETE "$ES/orders-sample" > /dev/null 2>&1
    curl -s $AUTH -X PUT "$ES/orders-sample" -H "Content-Type: application/json" -d '{
        "settings": { "number_of_replicas": 0 },
        "mappings": {
            "properties": {
                "order_id": { "type": "keyword" },
                "customer_id": { "type": "keyword" },
                "status": { "type": "keyword" },
                "total_amount": { "type": "float" },
                "currency": { "type": "keyword" },
                "items_count": { "type": "integer" },
                "shipping_country": { "type": "keyword" },
                "payment_method": { "type": "keyword" },
                "created_at": { "type": "date" },
                "updated_at": { "type": "date" }
            }
        }
    }' > /dev/null
    
    print_step "Indexing order data (50 documents)..."
    
    STATUSES=("pending" "processing" "shipped" "delivered" "cancelled" "refunded")
    COUNTRIES=("US" "UK" "DE" "FR" "CA" "AU" "JP" "BR" "IN" "MX")
    PAYMENT_METHODS=("credit_card" "debit_card" "paypal" "apple_pay" "google_pay" "bank_transfer")
    
    for i in {1..50}; do
        ORDER_ID=$(printf 'ORD-%06d' $((RANDOM % 999999)))
        CUSTOMER_ID="user-$((RANDOM % 30 + 1))"
        STATUS=${STATUSES[$((RANDOM % ${#STATUSES[@]}))]}
        TOTAL=$((RANDOM % 500 + 10)).$((RANDOM % 100))
        ITEMS=$((RANDOM % 10 + 1))
        COUNTRY=${COUNTRIES[$((RANDOM % ${#COUNTRIES[@]}))]}
        PAYMENT=${PAYMENT_METHODS[$((RANDOM % ${#PAYMENT_METHODS[@]}))]}
        
        DAYS_AGO=$((RANDOM % 89 + 1))  # Ensure at least 1 to avoid division by zero
        if [[ "$OSTYPE" == "darwin"* ]]; then
            CREATED=$(date -u -v-${DAYS_AGO}d +%Y-%m-%dT%H:%M:%SZ)
            UPDATED=$(date -u -v-$((RANDOM % DAYS_AGO + 1))d +%Y-%m-%dT%H:%M:%SZ)
        else
            CREATED=$(date -u -d "$DAYS_AGO days ago" +%Y-%m-%dT%H:%M:%SZ)
            UPDATED=$(date -u -d "$((RANDOM % DAYS_AGO + 1)) days ago" +%Y-%m-%dT%H:%M:%SZ)
        fi
        
        curl -s $AUTH -X POST "$ES/orders-sample/_doc" -H "Content-Type: application/json" -d "{
            \"order_id\": \"$ORDER_ID\",
            \"customer_id\": \"$CUSTOMER_ID\",
            \"status\": \"$STATUS\",
            \"total_amount\": $TOTAL,
            \"currency\": \"USD\",
            \"items_count\": $ITEMS,
            \"shipping_country\": \"$COUNTRY\",
            \"payment_method\": \"$PAYMENT\",
            \"created_at\": \"$CREATED\",
            \"updated_at\": \"$UPDATED\"
        }" > /dev/null
    done
    
    # =========================================================================
    # 5. PRODUCTS - Product catalog
    # =========================================================================
    print_step "Creating products-sample index..."
    curl -s $AUTH -X DELETE "$ES/products-sample" > /dev/null 2>&1
    curl -s $AUTH -X PUT "$ES/products-sample" -H "Content-Type: application/json" -d '{
        "settings": { "number_of_replicas": 0 },
        "mappings": {
            "properties": {
                "product_id": { "type": "keyword" },
                "name": { "type": "text" },
                "description": { "type": "text" },
                "category": { "type": "keyword" },
                "price": { "type": "float" },
                "stock": { "type": "integer" },
                "rating": { "type": "float" },
                "reviews_count": { "type": "integer" },
                "in_stock": { "type": "boolean" },
                "tags": { "type": "keyword" }
            }
        }
    }' > /dev/null
    
    print_step "Indexing product catalog (40 documents)..."
    
    CATEGORIES=("Electronics" "Clothing" "Home & Garden" "Sports" "Books" "Toys" "Beauty" "Automotive")
    PRODUCT_NAMES=(
        "Wireless Bluetooth Headphones" "Smart Watch Pro" "USB-C Hub Adapter" "Mechanical Keyboard" "4K Webcam"
        "Cotton T-Shirt" "Running Shoes" "Winter Jacket" "Denim Jeans" "Wool Sweater"
        "LED Desk Lamp" "Coffee Maker" "Air Purifier" "Robot Vacuum" "Smart Thermostat"
        "Yoga Mat" "Resistance Bands" "Foam Roller" "Jump Rope" "Dumbbells Set"
        "Programming Guide" "Science Fiction Novel" "Cookbook" "Biography" "Self-Help Book"
        "Building Blocks Set" "Remote Control Car" "Board Game" "Puzzle Set" "Art Supplies"
        "Face Moisturizer" "Shampoo Set" "Sunscreen SPF 50" "Makeup Palette" "Perfume"
        "Car Phone Mount" "Dash Camera" "Tire Inflator" "Seat Cushion" "Car Vacuum"
    )
    
    for i in {1..40}; do
        PRODUCT_ID=$(printf 'PROD-%04d' $i)
        NAME="${PRODUCT_NAMES[$((i - 1))]}"
        CATEGORY=${CATEGORIES[$((i / 5 % ${#CATEGORIES[@]}))]}
        PRICE=$((RANDOM % 200 + 10)).$((RANDOM % 100))
        STOCK=$((RANDOM % 500))
        IN_STOCK=$([[ $STOCK -gt 0 ]] && echo "true" || echo "false")
        RATING=$((RANDOM % 20 + 30))  # 3.0 to 5.0
        RATING_FLOAT=$(echo "scale=1; $RATING / 10" | bc)
        REVIEWS=$((RANDOM % 500 + 1))
        
        curl -s $AUTH -X POST "$ES/products-sample/_doc" -H "Content-Type: application/json" -d "{
            \"product_id\": \"$PRODUCT_ID\",
            \"name\": \"$NAME\",
            \"description\": \"High quality $NAME for your needs.\",
            \"category\": \"$CATEGORY\",
            \"price\": $PRICE,
            \"stock\": $STOCK,
            \"rating\": $RATING_FLOAT,
            \"reviews_count\": $REVIEWS,
            \"in_stock\": $IN_STOCK,
            \"tags\": [\"popular\", \"$(echo $CATEGORY | tr '[:upper:]' '[:lower:]' | tr ' ' '-')\"]
        }" > /dev/null
    done
    
    # =========================================================================
    # 6. EVENTS - Security/Audit events
    # =========================================================================
    print_step "Creating security-events index..."
    curl -s $AUTH -X DELETE "$ES/security-events" > /dev/null 2>&1
    curl -s $AUTH -X PUT "$ES/security-events" -H "Content-Type: application/json" -d '{
        "settings": { "number_of_replicas": 0 },
        "mappings": {
            "properties": {
                "@timestamp": { "type": "date" },
                "event_type": { "type": "keyword" },
                "severity": { "type": "keyword" },
                "source_ip": { "type": "ip" },
                "user": { "type": "keyword" },
                "action": { "type": "keyword" },
                "resource": { "type": "keyword" },
                "outcome": { "type": "keyword" },
                "message": { "type": "text" }
            }
        }
    }' > /dev/null
    
    print_step "Indexing security events (60 documents)..."
    
    EVENT_TYPES=("authentication" "authorization" "data_access" "configuration_change" "network" "file_operation")
    SEVERITIES=("low" "medium" "high" "critical")
    ACTIONS=("login" "logout" "read" "write" "delete" "create" "modify" "execute")
    RESOURCES=("/admin/users" "/api/data" "/config/settings" "/reports/financial" "/files/sensitive" "/system/logs")
    OUTCOMES=("success" "failure" "blocked")
    
    for i in {1..60}; do
        EVENT_TYPE=${EVENT_TYPES[$((RANDOM % ${#EVENT_TYPES[@]}))]}
        SEVERITY_RAND=$((RANDOM % 100))
        if [ $SEVERITY_RAND -lt 60 ]; then
            SEVERITY="low"
        elif [ $SEVERITY_RAND -lt 85 ]; then
            SEVERITY="medium"
        elif [ $SEVERITY_RAND -lt 95 ]; then
            SEVERITY="high"
        else
            SEVERITY="critical"
        fi
        
        IP="192.168.$((RANDOM % 256)).$((RANDOM % 256))"
        USER="user-$((RANDOM % 30 + 1))"
        ACTION=${ACTIONS[$((RANDOM % ${#ACTIONS[@]}))]}
        RESOURCE=${RESOURCES[$((RANDOM % ${#RESOURCES[@]}))]}
        OUTCOME=${OUTCOMES[$((RANDOM % ${#OUTCOMES[@]}))]}
        
        HOURS_AGO=$((RANDOM % 48))
        if [[ "$OSTYPE" == "darwin"* ]]; then
            TS=$(date -u -v-${HOURS_AGO}H +%Y-%m-%dT%H:%M:%SZ)
        else
            TS=$(date -u -d "$HOURS_AGO hours ago" +%Y-%m-%dT%H:%M:%SZ)
        fi
        
        curl -s $AUTH -X POST "$ES/security-events/_doc" -H "Content-Type: application/json" -d "{
            \"@timestamp\": \"$TS\",
            \"event_type\": \"$EVENT_TYPE\",
            \"severity\": \"$SEVERITY\",
            \"source_ip\": \"$IP\",
            \"user\": \"$USER\",
            \"action\": \"$ACTION\",
            \"resource\": \"$RESOURCE\",
            \"outcome\": \"$OUTCOME\",
            \"message\": \"User $USER performed $ACTION on $RESOURCE with $OUTCOME\"
        }" > /dev/null
    done
    
    # Refresh all indices
    curl -s $AUTH -X POST "$ES/_refresh" > /dev/null
    
    print_success "Sample data loaded!"
    echo ""
    echo "    Available indices:"
    curl -s $AUTH "$ES/_cat/indices?v&h=index,docs.count" | grep -E "logs-|metrics-|users-|orders-|products-|security-" | sort
    echo ""
    echo "    Total: 360 documents across 6 indices"
}

# Setup notebooks
setup_notebooks() {
    print_header "Setting Up Notebooks"
    
    cd "$NOTEBOOKS_DIR"
    
    # Always install/update the kernel to get latest features (like tracing)
    print_step "Installing/updating PL|ESQL Jupyter kernel..."
    if [ -f "kernel/install.sh" ]; then
        bash kernel/install.sh
        print_success "Kernel installed/updated!"
    else
        print_warning "Kernel install script not found. Manual setup needed."
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
    echo "  (no option)       Full setup: build, start ES, OTEL, Kibana, notebooks"
    echo "  --build           Just build the plugin"
    echo "  --start           Start Elasticsearch (foreground)"
    echo "  --start-bg        Start Elasticsearch (background)"
    echo "  --load-data       Load sample data into Elasticsearch"
    echo "  --notebooks       Start Jupyter notebooks"
    echo "  --kibana          Start Kibana (for APM/observability)"
    echo "  --otel            Start OTEL Collector (for distributed tracing)"
    echo "  --stop            Stop all (ES, OTEL, Kibana, Jupyter)"
    echo "  --stop-notebooks  Stop only Jupyter notebooks"
    echo "  --stop-kibana     Stop only Kibana"
    echo "  --stop-otel       Stop only OTEL Collector"
    echo "  --status          Check service status"
    echo "  --help            Show this help"
    echo ""
    echo "Distributed Tracing:"
    echo "  Traces are collected via OTEL Collector (ports 4317/4318)"
    echo "  View traces in Kibana APM: http://localhost:5601/app/apm"
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
    stop_otel_collector
    stop_apm_server
    stop_kibana
    stop_elasticsearch
}

# =========================================================================
# KIBANA SUPPORT (for APM/Observability)
# =========================================================================

# Detect platform for Kibana directory name
detect_kibana_dir() {
    local OS=""
    local ARCH=""
    
    case "$(uname -s)" in
        Darwin) OS="darwin" ;;
        Linux) OS="linux" ;;
        *) OS="linux" ;;
    esac
    
    case "$(uname -m)" in
        x86_64) ARCH="x86_64" ;;
        arm64|aarch64) ARCH="aarch64" ;;
        *) ARCH="x86_64" ;;
    esac
    
    # Try both possible directory names (with and without OS/arch suffix)
    local FULL_NAME="$PROJECT_ROOT/kibana-${KIBANA_VERSION}-${OS}-${ARCH}"
    local SHORT_NAME="$PROJECT_ROOT/kibana-${KIBANA_VERSION}"
    
    # Check for bin/kibana to verify it's a valid Kibana directory
    if [ -f "$SHORT_NAME/bin/kibana" ]; then
        echo "$SHORT_NAME"
    elif [ -f "$FULL_NAME/bin/kibana" ]; then
        echo "$FULL_NAME"
    else
        # Return the expected full name (download will create it)
        echo "$FULL_NAME"
    fi
}

# Download and configure Kibana
download_kibana() {
    print_header "Setting up Kibana ${KIBANA_VERSION}"
    
    local OS=""
    local ARCH=""
    local EXT=""
    
    case "$(uname -s)" in
        Darwin) 
            OS="darwin" 
            EXT="tar.gz"
            ;;
        Linux) 
            OS="linux" 
            EXT="tar.gz"
            ;;
        *) 
            print_error "Unsupported OS: $(uname -s)"
            return 1 
            ;;
    esac
    
    case "$(uname -m)" in
        x86_64) ARCH="x86_64" ;;
        arm64|aarch64) ARCH="aarch64" ;;
        *) 
            print_error "Unsupported architecture: $(uname -m)"
            return 1 
            ;;
    esac
    
    local KIBANA_FILENAME="kibana-${KIBANA_VERSION}-${OS}-${ARCH}"
    # Use snapshot builds to match ES main branch
    local KIBANA_URL="https://snapshots.elastic.co/downloads/kibana/${KIBANA_FILENAME}.${EXT}"
    KIBANA_DIR="$PROJECT_ROOT/${KIBANA_FILENAME}"
    
    if [ -d "$KIBANA_DIR" ]; then
        print_success "Kibana already downloaded at $KIBANA_DIR"
        return 0
    fi
    
    print_step "Downloading Kibana from ${KIBANA_URL}..."
    
    cd "$PROJECT_ROOT"
    
    if curl -L -o "${KIBANA_FILENAME}.${EXT}" "$KIBANA_URL"; then
        print_step "Extracting Kibana..."
        tar -xzf "${KIBANA_FILENAME}.${EXT}"
        rm -f "${KIBANA_FILENAME}.${EXT}"
        
        # The archive may extract to a different directory name (without OS/arch suffix)
        # Detect the actual extracted directory
        local EXTRACTED_DIR=""
        for dir in kibana-${KIBANA_VERSION} ${KIBANA_FILENAME}; do
            if [ -d "$PROJECT_ROOT/$dir" ] && [ -f "$PROJECT_ROOT/$dir/bin/kibana" ]; then
                EXTRACTED_DIR="$dir"
                break
            fi
        done
        
        if [ -z "$EXTRACTED_DIR" ]; then
            print_error "Could not find extracted Kibana directory"
            return 1
        fi
        
        # If extracted dir is different from expected, update KIBANA_DIR
        if [ "$EXTRACTED_DIR" != "${KIBANA_FILENAME}" ]; then
            KIBANA_DIR="$PROJECT_ROOT/$EXTRACTED_DIR"
            print_success "Kibana extracted to $KIBANA_DIR"
        else
            print_success "Kibana extracted to $KIBANA_DIR"
        fi
    else
        print_error "Failed to download Kibana. Check network connection."
        return 1
    fi
    
    # Configure Kibana
    configure_kibana
    
    return 0
}

# Configure Kibana for elastic-script development
configure_kibana() {
    # Ensure KIBANA_DIR is set
    if [ -z "$KIBANA_DIR" ]; then
        KIBANA_DIR="$(detect_kibana_dir)"
    fi
    
    print_step "Configuring Kibana at $KIBANA_DIR..."
    
    local KIBANA_CONFIG_DIR="$KIBANA_DIR/config"
    local KIBANA_YML="$KIBANA_CONFIG_DIR/kibana.yml"
    
    # Create config directory if it doesn't exist
    if [ ! -d "$KIBANA_CONFIG_DIR" ]; then
        mkdir -p "$KIBANA_CONFIG_DIR"
    fi
    
    # Backup original config if it exists
    if [ -f "$KIBANA_YML" ] && [ ! -f "${KIBANA_YML}.original" ]; then
        cp "$KIBANA_YML" "${KIBANA_YML}.original"
    fi
    
    # Create new config
    cat > "$KIBANA_YML" << 'EOF'
# elastic-script development configuration
server.host: "0.0.0.0"
server.port: 5601
server.name: "elastic-script-kibana"

# Elasticsearch connection
elasticsearch.hosts: ["http://localhost:9200"]
elasticsearch.username: "elastic-admin"
elasticsearch.password: "elastic-password"

# Required encryption keys for saved objects (must be exactly 32 characters)
xpack.encryptedSavedObjects.encryptionKey: "escript-dev-key-0123456789abcdef"
xpack.reporting.encryptionKey: "escript-rep-key-0123456789abcdef"
xpack.security.encryptionKey: "escript-sec-key-0123456789abcdef"

# Telemetry
telemetry.enabled: false

# Logging
logging.root.level: info
EOF
    
    print_success "Kibana configured for elastic-script development"
}

# Start Kibana in background
start_kibana_background() {
    # Ensure KIBANA_DIR is set
    if [ -z "$KIBANA_DIR" ]; then
        KIBANA_DIR="$(detect_kibana_dir)"
    fi
    
    if [ ! -d "$KIBANA_DIR" ]; then
        download_kibana
        if [ $? -ne 0 ]; then
            return 1
        fi
    fi
    
    # Check if already running
    if curl -s http://localhost:5601/api/status > /dev/null 2>&1; then
        print_warning "Kibana already running on port 5601"
        return 0
    fi
    
    print_step "Starting Kibana in background..."
    
    cd "$KIBANA_DIR"
    nohup ./bin/kibana > "$PROJECT_ROOT/kibana.log" 2>&1 &
    KIBANA_PID=$!
    echo $KIBANA_PID > "$PROJECT_ROOT/.kibana_pid"
    
    print_step "Waiting for Kibana to be ready..."
    for i in {1..90}; do
        if curl -s http://localhost:5601/api/status > /dev/null 2>&1; then
            print_success "Kibana is ready at http://localhost:5601"
            return 0
        fi
        sleep 2
        echo -n "."
    done
    
    print_warning "Kibana taking longer than expected. Check kibana.log"
    return 1
}

# Stop Kibana
stop_kibana() {
    print_header "Stopping Kibana"
    
    if [ -f "$PROJECT_ROOT/.kibana_pid" ]; then
        PID=$(cat "$PROJECT_ROOT/.kibana_pid")
        if kill -0 $PID 2>/dev/null; then
            kill $PID
            rm "$PROJECT_ROOT/.kibana_pid"
            print_success "Kibana stopped"
        else
            print_warning "Kibana process not found"
            rm "$PROJECT_ROOT/.kibana_pid"
        fi
    else
        # Try to kill by port
        if lsof -ti:5601 > /dev/null 2>&1; then
            lsof -ti:5601 | xargs kill 2>/dev/null
            print_success "Kibana stopped (port 5601)"
        else
            print_warning "No Kibana running"
        fi
    fi
}

# =============================================================================
# OpenTelemetry Collector Setup
# =============================================================================

# Download OTEL Collector
download_otel_collector() {
    print_header "Setting up OpenTelemetry Collector"
    
    mkdir -p "$OTEL_COLLECTOR_DIR"
    
    if [ -f "$OTEL_COLLECTOR_BINARY" ]; then
        print_success "OTEL Collector already downloaded"
        return 0
    fi
    
    # Detect platform
    case "$(uname -s)" in
        Darwin) OS="darwin" ;;
        Linux)  OS="linux" ;;
        *)      print_error "Unsupported OS"; return 1 ;;
    esac
    
    case "$(uname -m)" in
        arm64|aarch64) ARCH="arm64" ;;
        x86_64|amd64)  ARCH="amd64" ;;
        *)             print_error "Unsupported architecture"; return 1 ;;
    esac
    
    local URL="https://github.com/open-telemetry/opentelemetry-collector-releases/releases/download/v${OTEL_COLLECTOR_VERSION}/otelcol-contrib_${OTEL_COLLECTOR_VERSION}_${OS}_${ARCH}.tar.gz"
    
    print_step "Downloading OTEL Collector v${OTEL_COLLECTOR_VERSION}..."
    curl -sL "$URL" -o /tmp/otelcol.tar.gz
    
    if [ $? -ne 0 ]; then
        print_error "Failed to download OTEL Collector"
        return 1
    fi
    
    tar -xzf /tmp/otelcol.tar.gz -C "$OTEL_COLLECTOR_DIR"
    rm /tmp/otelcol.tar.gz
    
    print_success "OTEL Collector downloaded"
}

# Configure OTEL Collector
configure_otel_collector() {
    print_step "Configuring OTEL Collector..."
    
    mkdir -p "$OTEL_COLLECTOR_DIR"
    
    # Collector sends traces to APM Server via OTLP/HTTP (for proper Kibana APM integration)
    # and metrics/logs directly to Elasticsearch
    cat > "$OTEL_COLLECTOR_CONFIG" << 'EOF'
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

processors:
  batch:
    timeout: 1s
    send_batch_size: 1024

exporters:
  # Traces go to APM Server via OTLP/HTTP for proper Kibana APM integration
  otlphttp/apm:
    endpoint: http://localhost:8200
    tls:
      insecure: true
  # Metrics go directly to ES native OTLP endpoint
  elasticsearch/metrics:
    endpoints: ["http://localhost:9200"]
    user: elastic-admin
    password: elastic-password
    mapping:
      mode: ecs
  # Logs go directly to ES
  elasticsearch/logs:
    endpoints: ["http://localhost:9200"]
    user: elastic-admin
    password: elastic-password
    mapping:
      mode: ecs
  # Debug exporter for troubleshooting (set to basic to reduce noise)
  debug:
    verbosity: basic

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [otlphttp/apm, debug]
    metrics:
      receivers: [otlp]
      processors: [batch]
      exporters: [elasticsearch/metrics]
    logs:
      receivers: [otlp]
      processors: [batch]
      exporters: [elasticsearch/logs]
EOF
    
    print_success "OTEL Collector configured (traces -> APM Server -> Kibana APM)"
}

# Start OTEL Collector
start_otel_collector() {
    # Download if not present
    if [ ! -f "$OTEL_COLLECTOR_BINARY" ]; then
        download_otel_collector
        if [ $? -ne 0 ]; then
            return 1
        fi
    fi
    
    # Configure
    if [ ! -f "$OTEL_COLLECTOR_CONFIG" ]; then
        configure_otel_collector
    fi
    
    # Check if already running
    if curl -s http://localhost:4318 > /dev/null 2>&1; then
        print_warning "OTEL Collector already running on port 4318"
        return 0
    fi
    
    # Stop any existing
    pkill -f otelcol-contrib 2>/dev/null || true
    sleep 1
    
    print_step "Starting OTEL Collector..."
    
    nohup "$OTEL_COLLECTOR_BINARY" --config="$OTEL_COLLECTOR_CONFIG" > "$OTEL_COLLECTOR_LOG" 2>&1 &
    OTEL_PID=$!
    echo $OTEL_PID > "$OTEL_COLLECTOR_PID"
    
    # Wait for startup
    sleep 2
    
    if kill -0 $OTEL_PID 2>/dev/null; then
        print_success "OTEL Collector started (ports 4317/4318)"
        return 0
    else
        print_error "OTEL Collector failed to start. Check $OTEL_COLLECTOR_LOG"
        return 1
    fi
}

# Stop OTEL Collector
stop_otel_collector() {
    print_step "Stopping OTEL Collector..."
    
    if [ -f "$OTEL_COLLECTOR_PID" ]; then
        PID=$(cat "$OTEL_COLLECTOR_PID")
        if kill -0 $PID 2>/dev/null; then
            kill $PID
            rm "$OTEL_COLLECTOR_PID"
            print_success "OTEL Collector stopped"
        else
            rm "$OTEL_COLLECTOR_PID"
        fi
    fi
    
    pkill -f otelcol-contrib 2>/dev/null || true
}

# =============================================================================
# APM Server (for OTLP trace ingestion into Kibana APM)
# =============================================================================

# Download APM Server
download_apm_server() {
    print_header "Setting up APM Server"
    
    mkdir -p "$APM_SERVER_DIR"
    
    if [ -f "$APM_SERVER_BINARY" ]; then
        print_success "APM Server already downloaded"
        return 0
    fi
    
    print_step "Downloading APM Server ${APM_SERVER_VERSION}..."
    
    # Detect OS and architecture
    OS=$(uname -s | tr '[:upper:]' '[:lower:]')
    ARCH=$(uname -m)
    
    case "$OS" in
        darwin)
            OS_NAME="darwin"
            # APM Server doesn't have darwin-aarch64 builds, use x86_64 (runs via Rosetta)
            ARCH="x86_64"
            ;;
        linux)
            OS_NAME="linux"
            case "$ARCH" in
                x86_64) ARCH="x86_64" ;;
                aarch64|arm64) ARCH="aarch64" ;;
                *) print_error "Unsupported architecture: $ARCH"; return 1 ;;
            esac
            ;;
        *) print_error "Unsupported OS: $OS"; return 1 ;;
    esac
    
    DOWNLOAD_URL="https://artifacts.elastic.co/downloads/apm-server/apm-server-${APM_SERVER_VERSION}-${OS_NAME}-${ARCH}.tar.gz"
    TARBALL="$APM_SERVER_DIR/apm-server.tar.gz"
    
    echo "  URL: $DOWNLOAD_URL"
    
    # Download with progress
    if curl -L --fail -o "$TARBALL" "$DOWNLOAD_URL" 2>&1; then
        # Verify download succeeded (file exists and has content)
        if [ ! -s "$TARBALL" ]; then
            print_error "Download failed - empty file"
            rm -f "$TARBALL"
            return 1
        fi
        
        print_step "Extracting APM Server..."
        if tar -xzf "$TARBALL" -C "$APM_SERVER_DIR" --strip-components=1; then
            rm "$TARBALL"
            chmod +x "$APM_SERVER_BINARY"
            print_success "APM Server downloaded successfully"
        else
            print_error "Failed to extract APM Server"
            rm -f "$TARBALL"
            return 1
        fi
    else
        print_error "Failed to download APM Server from $DOWNLOAD_URL"
        rm -f "$TARBALL"
        return 1
    fi
}

# Configure APM Server
configure_apm_server() {
    print_step "Configuring APM Server..."
    
    cat > "$APM_SERVER_CONFIG" << 'EOF'
# APM Server Configuration for elastic-script
# Accepts OTLP traces and forwards to Elasticsearch

apm-server:
  host: "0.0.0.0:8200"
  
  # Enable OTLP support
  auth:
    anonymous:
      enabled: true
      allow_agent: ["otlp/"]
      allow_service: []

output.elasticsearch:
  hosts: ["localhost:9200"]
  username: "elastic-admin"
  password: "elastic-password"

# Disable self-monitoring for simplicity
monitoring:
  enabled: false

# Logging
logging:
  level: info
  to_files: false
EOF
    
    print_success "APM Server configured"
}

# Start APM Server
start_apm_server() {
    # Download if not present
    if [ ! -f "$APM_SERVER_BINARY" ]; then
        download_apm_server
        if [ $? -ne 0 ]; then
            return 1
        fi
    fi
    
    # Configure
    configure_apm_server
    
    # Check if already running
    if pgrep -f "apm-server" > /dev/null 2>&1; then
        print_success "APM Server already running"
        return 0
    fi
    
    print_step "Starting APM Server on port ${APM_SERVER_PORT}..."
    
    cd "$APM_SERVER_DIR"
    nohup ./apm-server -c apm-server.yml > "$APM_SERVER_LOG" 2>&1 &
    APM_PID=$!
    echo $APM_PID > "$APM_SERVER_PID"
    
    # Wait for startup
    sleep 3
    
    if kill -0 $APM_PID 2>/dev/null; then
        print_success "APM Server started (PID: $APM_PID)"
        echo "  OTLP endpoint: http://localhost:${APM_SERVER_PORT}"
        return 0
    else
        print_error "APM Server failed to start. Check $APM_SERVER_LOG"
        return 1
    fi
}

# Stop APM Server
stop_apm_server() {
    print_step "Stopping APM Server..."
    
    if [ -f "$APM_SERVER_PID" ]; then
        PID=$(cat "$APM_SERVER_PID")
        if kill -0 $PID 2>/dev/null; then
            kill $PID
            rm "$APM_SERVER_PID"
            print_success "APM Server stopped"
        else
            rm "$APM_SERVER_PID"
        fi
    fi
    
    pkill -f "apm-server" 2>/dev/null || true
}

# Enable ES telemetry settings
enable_telemetry() {
    print_step "Enabling Elasticsearch telemetry..."
    
    curl -s -u elastic-admin:elastic-password -X PUT "localhost:9200/_cluster/settings" \
        -H "Content-Type: application/json" -d '{
        "persistent": {
            "telemetry.tracing.enabled": true,
            "telemetry.metrics.enabled": true
        }
    }' > /dev/null 2>&1
    
    print_success "Telemetry enabled"
}

# =============================================================================

# Check status
check_status() {
    print_header "Checking Status"
    
    # Elasticsearch
    echo ""
    echo "Elasticsearch (port 9200):"
    if curl -s http://localhost:9200 > /dev/null 2>&1; then
        print_success "Running"
        curl -s -u elastic-admin:elastic-password http://localhost:9200 2>/dev/null | grep "cluster_name" || true
    else
        print_warning "Not running"
    fi
    
    # Kibana
    echo ""
    echo "Kibana (port 5601):"
    if curl -s http://localhost:5601/api/status > /dev/null 2>&1; then
        print_success "Running at http://localhost:5601"
    else
        print_warning "Not running"
    fi
    
    # APM Server
    echo ""
    echo "APM Server (port 8200):"
    if pgrep -f "apm-server" > /dev/null 2>&1; then
        print_success "Running (OTLP: http://localhost:8200)"
    else
        print_warning "Not running"
    fi
    
    # OTEL Collector
    echo ""
    echo "OTEL Collector (ports 4317/4318):"
    if pgrep -f otelcol-contrib > /dev/null 2>&1; then
        print_success "Running (gRPC: 4317, HTTP: 4318)"
    else
        print_warning "Not running"
    fi
    
    # Jupyter
    echo ""
    echo "Jupyter (port 8888):"
    if curl -s http://localhost:8888 > /dev/null 2>&1; then
        print_success "Running at http://localhost:8888"
    elif pgrep -f "jupyter" > /dev/null 2>&1; then
        print_success "Running (process found)"
    else
        print_warning "Not running"
    fi
    echo ""
}

# Print curl examples
print_examples() {
    print_header "📋 Try These Examples (copy & paste)"
    echo ""
    echo -e "${GREEN}# 1. Simple ESQL query${NC}"
    echo 'curl -u elastic-admin:elastic-password -X POST "localhost:9200/_escript" -H "Content-Type: application/json" -d '\''{"query": "FROM logs-sample | LIMIT 5"}'\'''
    echo ""
    echo -e "${GREEN}# 2. Define and call a procedure${NC}"
    echo 'curl -u elastic-admin:elastic-password -X POST "localhost:9200/_escript" -H "Content-Type: application/json" -d '\''{"query": "CREATE PROCEDURE get_logs() BEGIN DECLARE logs ARRAY = ESQL_QUERY('\''FROM logs-sample | LIMIT 3'\''); RETURN logs; END PROCEDURE"}'\'''
    echo ""
    echo -e "${GREEN}# 3. Call the procedure${NC}"
    echo 'curl -u elastic-admin:elastic-password -X POST "localhost:9200/_escript" -H "Content-Type: application/json" -d '\''{"query": "CALL get_logs()"}'\'''
    echo ""
    echo -e "${GREEN}# 4. Check available indices${NC}"
    echo 'curl -u elastic-admin:elastic-password "localhost:9200/_cat/indices?v&h=index,docs.count"'
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
    --kibana)
        start_kibana_background
        ;;
    --no-otel)
        EDOT_ENABLED=false
        print_step "EDOT tracing disabled"
        ;;
    --stop-notebooks)
        stop_notebooks
        ;;
    --stop-kibana)
        stop_kibana
        ;;
    --otel)
        start_otel_collector
        ;;
    --stop-otel)
        stop_otel_collector
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
                
                # Start OTEL Collector if not running
                if ! pgrep -f otelcol-contrib > /dev/null 2>&1; then
                    start_otel_collector
                fi
                
                # Enable telemetry
                enable_telemetry
                
                # Start Kibana if not running
                if ! curl -s http://localhost:5601/api/status > /dev/null 2>&1; then
                    download_kibana && start_kibana_background
                fi
                
                echo ""
                print_header "🚀 Launching Jupyter Notebooks & Kibana"
                cd "$NOTEBOOKS_DIR"
                python3 -m notebook --notebook-dir="$NOTEBOOKS_DIR" &
                JUPYTER_PID=$!
                echo ""
                print_success "Jupyter started at http://localhost:8888"
                print_success "Kibana available at http://localhost:5601"
                print_success "OTEL Collector ready at localhost:4317/4318"
                print_success "View traces at http://localhost:5601/app/apm"
                echo ""
                
                # Open both in browser
                sleep 2
                if command -v open &> /dev/null; then
                    open "http://localhost:8888"
                    open "http://localhost:5601/app/apm"
                elif command -v xdg-open &> /dev/null; then
                    xdg-open "http://localhost:8888"
                    xdg-open "http://localhost:5601/app/apm"
                fi
                
                echo "Press Enter to continue (services run in background)..."
                read
            fi
        else
            echo "This will:"
            echo "  1. Check prerequisites"
            echo "  2. Configure OpenAI API key (optional, for AI features)"
            echo "  3. Build the plugin"
            echo "  4. Download and configure Kibana & OTEL Collector"
            echo "  5. Start Elasticsearch"
            echo "  6. Start OTEL Collector (for distributed tracing)"
            echo "  7. Start Kibana"
            echo "  8. Load sample data"
            echo "  9. Start Jupyter notebooks"
            echo "  10. Open Jupyter and Kibana APM in browser"
            echo ""
            read -p "Continue? [Y/n] " -n 1 -r
            echo
            if [[ ! $REPLY =~ ^[Nn]$ ]]; then
                check_prerequisites
                prompt_openai_key
                build_plugin
                
                # Download and configure Kibana
                download_kibana
                
                start_elasticsearch_background
                
                # Start APM Server for trace ingestion
                start_apm_server
                
                # Start OTEL Collector for tracing (sends to APM Server)
                start_otel_collector
                
                # Enable ES telemetry
                enable_telemetry
                
                # Start Kibana
                start_kibana_background
                
                load_sample_data
                print_examples
                setup_notebooks
                
                echo ""
                print_header "🚀 Launching Jupyter Notebooks"
                cd "$NOTEBOOKS_DIR"
                python3 -m notebook --notebook-dir="$NOTEBOOKS_DIR" &
                JUPYTER_PID=$!
                echo ""
                print_success "Jupyter started at http://localhost:8888"
                print_success "Kibana available at http://localhost:5601"
                print_success "OTEL Collector ready at localhost:4317/4318"
                print_success "View traces at http://localhost:5601/app/apm"
                echo ""
                
                # Open both in browser
                sleep 2
                if command -v open &> /dev/null; then
                    open "http://localhost:8888"
                    open "http://localhost:5601/app/apm"
                elif command -v xdg-open &> /dev/null; then
                    xdg-open "http://localhost:8888"
                    xdg-open "http://localhost:5601/app/apm"
                fi
                
                echo ""
                print_header "✅ Setup Complete!"
                echo ""
                echo "Services running:"
                echo "  • Elasticsearch:   http://localhost:9200"
                echo "  • Kibana:          http://localhost:5601"
                echo "  • Kibana APM:      http://localhost:5601/app/apm"
                echo "  • OTEL Collector:  localhost:4317 (gRPC), localhost:4318 (HTTP)"
                echo "  • Jupyter:         http://localhost:8888"
                echo ""
                echo "Distributed tracing is enabled! Send traces via OTEL to localhost:4318"
                echo ""
                echo "To stop all services: ./scripts/quick-start.sh --stop"
                echo ""
            fi
        fi
        ;;
    *)
        print_error "Unknown option: $1"
        show_help
        exit 1
        ;;
esac
