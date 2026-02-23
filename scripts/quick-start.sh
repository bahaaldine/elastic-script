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
        
        # Apply necessary patches to the submodule
        apply_elasticsearch_patches
    else
        print_success "Elasticsearch source found"
        
        # Ensure patches are applied even for existing submodules
        apply_elasticsearch_patches
    fi
}

# Apply patches to elasticsearch submodule for compatibility
apply_elasticsearch_patches() {
    local RUN_GRADLE="$ES_DIR/build-tools-internal/src/main/groovy/elasticsearch.run.gradle"
    
    if [ ! -f "$RUN_GRADLE" ]; then
        return
    fi
    
    # Check if ML disable patch is already applied
    if ! grep -q "xpack.ml.enabled.*false" "$RUN_GRADLE" 2>/dev/null; then
        print_step "Applying ML disable patch for compatibility..."
        
        # Insert ML disable setting after xpack.security.enabled line
        if [[ "$OSTYPE" == "darwin"* ]]; then
            # macOS sed
            sed -i '' "/setting 'xpack.security.enabled', 'true'/a\\
      // Disable ML to avoid native code issues on some systems\\
      setting 'xpack.ml.enabled', 'false'" "$RUN_GRADLE"
        else
            # Linux sed
            sed -i "/setting 'xpack.security.enabled', 'true'/a\\      // Disable ML to avoid native code issues on some systems\n      setting 'xpack.ml.enabled', 'false'" "$RUN_GRADLE"
        fi
        
        print_success "ML disable patch applied"
    fi
    
    # Check if OpenAI key passthrough is already applied
    if ! grep -q "OPENAI_API_KEY" "$RUN_GRADLE" 2>/dev/null; then
        print_step "Applying OpenAI key passthrough patch..."
        
        # Insert OpenAI key passthrough after numberOfNodes line
        if [[ "$OSTYPE" == "darwin"* ]]; then
            # macOS sed
            sed -i '' "/numberOfNodes = 1/a\\
      \\
      // Pass OpenAI API key from environment to the cluster\\
      String openaiKey = System.getenv(\"OPENAI_API_KEY\")\\
      if (openaiKey != null \&\& !openaiKey.isEmpty()) {\\
        environment 'OPENAI_API_KEY', openaiKey\\
      }" "$RUN_GRADLE"
        else
            # Linux sed
            sed -i "/numberOfNodes = 1/a\\      \n      // Pass OpenAI API key from environment to the cluster\n      String openaiKey = System.getenv(\"OPENAI_API_KEY\")\n      if (openaiKey != null \&\& !openaiKey.isEmpty()) {\n        environment 'OPENAI_API_KEY', openaiKey\n      }" "$RUN_GRADLE"
        fi
        
        print_success "OpenAI key passthrough patch applied"
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
    
    # Also load sample skills
    load_sample_skills
}

# Load sample skills for quick wins
load_sample_skills() {
    print_header "Loading Sample Skills"
    
    local AUTH="-u elastic-admin:elastic-password"
    local ES="http://localhost:9200"
    
    print_step "Creating sample skills..."
    
    # First, clean up any old skills and their associated procedures
    for skill_name in "check_cluster_health" "count_logs_by_level" "get_recent_errors" "hello_moltler" "metrics_summary"; do
        curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d "{\"query\": \"DROP SKILL $skill_name\"}" > /dev/null 2>&1
        curl -s $AUTH -X DELETE "$ES/.elastic_script_procedures/_doc/$skill_name" > /dev/null 2>&1
    done
    
    # Skill 1: Check cluster health (simple, no external data needed)
    curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
        "query": "CREATE SKILL check_cluster_health VERSION '\''1.0'\'' DESCRIPTION '\''Check Elasticsearch cluster health status'\'' AUTHOR '\''Moltler'\'' TAGS ['\''health'\'', '\''monitoring'\''] RETURNS DOCUMENT BEGIN DECLARE health DOCUMENT; SET health = {'\''status'\'': '\''green'\'', '\''cluster'\'': '\''moltler-demo'\'', '\''checked_at'\'': CURRENT_TIMESTAMP()}; RETURN health; END SKILL;"
    }' > /dev/null 2>&1
    echo "    ✓ check_cluster_health"
    
    # Skill 2: Count logs by level (fixed ESQL syntax)
    curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
        "query": "CREATE SKILL count_logs_by_level VERSION '\''1.0'\'' DESCRIPTION '\''Count log entries grouped by severity level'\'' AUTHOR '\''Moltler'\'' TAGS ['\''logs'\'', '\''analytics'\''] (index_pattern STRING DEFAULT '\''logs-sample'\'') RETURNS ARRAY BEGIN DECLARE results ARRAY; SET results = ESQL_QUERY('\''FROM '\'' || index_pattern || '\'' | STATS count = COUNT(*) BY level | SORT count DESC'\''); RETURN results; END SKILL;"
    }' > /dev/null 2>&1
    echo "    ✓ count_logs_by_level"
    
    # Skill 3: Get recent errors (fixed ESQL == syntax, uses INT for LIMIT)
    curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
        "query": "CREATE SKILL get_recent_errors VERSION '\''1.0'\'' DESCRIPTION '\''Retrieve the most recent error log entries'\'' AUTHOR '\''Moltler'\'' TAGS ['\''logs'\'', '\''errors'\'', '\''debugging'\''] (limit_count INT DEFAULT 10) RETURNS ARRAY BEGIN DECLARE errors ARRAY; SET errors = ESQL_QUERY('\''FROM logs-sample | WHERE level == \"ERROR\" | SORT @timestamp DESC | LIMIT '\'' || limit_count); RETURN errors; END SKILL;"
    }' > /dev/null 2>&1
    echo "    ✓ get_recent_errors"
    
    # Skill 4: Simple greeting (hello world - simplest skill)
    curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
        "query": "CREATE SKILL hello_moltler VERSION '\''1.0'\'' DESCRIPTION '\''A friendly greeting - your first Moltler skill!'\'' AUTHOR '\''Moltler'\'' TAGS ['\''demo'\'', '\''beginner'\''] (name STRING DEFAULT '\''World'\'') RETURNS STRING BEGIN RETURN '\''Hello, '\'' || name || '\''! Welcome to Moltler.'\''; END SKILL;"
    }' > /dev/null 2>&1
    echo "    ✓ hello_moltler"
    
    # Skill 5: Metrics summary (fixed to use actual field names from metrics-sample)
    curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
        "query": "CREATE SKILL metrics_summary VERSION '\''1.0'\'' DESCRIPTION '\''Get metrics summary by metric name'\'' AUTHOR '\''Moltler'\'' TAGS ['\''metrics'\'', '\''monitoring'\'', '\''performance'\''] RETURNS ARRAY BEGIN DECLARE summary ARRAY; SET summary = ESQL_QUERY('\''FROM metrics-sample | STATS avg_value = AVG(value), max_value = MAX(value), min_value = MIN(value) BY metric_name'\''); RETURN summary; END SKILL;"
    }' > /dev/null 2>&1
    echo "    ✓ metrics_summary"
    
    print_success "Sample skills loaded!"
    echo ""
    echo "    Try these in moltler CLI:"
    echo "      SHOW SKILLS"
    echo "      TEST SKILL hello_moltler"
    echo "      TEST SKILL check_cluster_health"
    echo "      TEST SKILL count_logs_by_level"
    echo ""
}

# Load demo procedures for the Skills Manager UI
load_demo_procedures() {
    print_header "Loading Demo Procedures"
    
    local AUTH="-u elastic-admin:elastic-password"
    local ES="http://localhost:9200"
    local DEMO_FILE="$PROJECT_ROOT/scripts/demo-procedures.sql"
    
    if [ ! -f "$DEMO_FILE" ]; then
        print_warning "Demo procedures file not found: $DEMO_FILE"
        return 1
    fi
    
    print_step "Loading demo procedures..."
    
    # Parse and execute each procedure from the file
    # We'll extract procedure names and their full definitions
    local PROCEDURE_NAMES=(
        "hello_world"
        "analyze_logs"
        "get_user_stats"
        "aggregate_metrics"
        "order_summary"
        "security_audit"
        "search_products"
        "generate_report"
        "health_check"
        "demo_workflow"
    )
    
    for proc_name in "${PROCEDURE_NAMES[@]}"; do
        # Drop existing procedure first
        curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d "{
            \"query\": \"DROP PROCEDURE $proc_name\"
        }" > /dev/null 2>&1
    done
    
    # Now load the full demo file by reading and executing each procedure
    # Read the file, remove comments, and extract procedure blocks
    
    # Procedure 1: hello_world
    curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
        "query": "CREATE PROCEDURE hello_world() BEGIN PRINT '\''Hello from Moltler!'\''; PRINT '\''elastic-script is running inside Elasticsearch.'\''; END PROCEDURE;"
    }' > /dev/null 2>&1
    echo "    ✓ hello_world"
    
    # Procedure 2: analyze_logs  
    curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
        "query": "CREATE PROCEDURE analyze_logs(log_index STRING DEFAULT '\''logs-sample'\'') BEGIN DECLARE results ARRAY; DECLARE error_count NUMBER; DECLARE total_count NUMBER; SET results = ESQL_QUERY('\''FROM '\'' || log_index || '\'' | STATS total = COUNT(*)'\''); SET total_count = DOCUMENT_GET(results[0], '\''total'\''); SET results = ESQL_QUERY('\''FROM '\'' || log_index || '\'' | WHERE level == \"ERROR\" | STATS errors = COUNT(*)'\''); SET error_count = DOCUMENT_GET(results[0], '\''errors'\''); SET results = ESQL_QUERY('\''FROM '\'' || log_index || '\'' | WHERE level == \"ERROR\" | STATS count = COUNT(*) BY message | SORT count DESC | LIMIT 5'\''); PRINT '\''Log Analysis: '\'' || total_count || '\'' total logs, '\'' || error_count || '\'' errors'\''; RETURN results; END PROCEDURE;"
    }' > /dev/null 2>&1
    echo "    ✓ analyze_logs"
    
    # Procedure 3: get_user_stats
    curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
        "query": "CREATE PROCEDURE get_user_stats() BEGIN DECLARE users ARRAY; DECLARE admins ARRAY; DECLARE active_users ARRAY; SET users = ESQL_QUERY('\''FROM users-sample | STATS total = COUNT(*)'\''); SET admins = ESQL_QUERY('\''FROM users-sample | WHERE role == \"admin\" | STATS count = COUNT(*)'\''); SET active_users = ESQL_QUERY('\''FROM users-sample | WHERE active == true | STATS count = COUNT(*)'\''); RETURN {\"total_users\": DOCUMENT_GET(users[0], '\''total'\''), \"admin_count\": DOCUMENT_GET(admins[0], '\''count'\''), \"active_users\": DOCUMENT_GET(active_users[0], '\''count'\'')}; END PROCEDURE;"
    }' > /dev/null 2>&1
    echo "    ✓ get_user_stats"
    
    # Procedure 4: aggregate_metrics
    curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
        "query": "CREATE PROCEDURE aggregate_metrics(metric_name_param STRING) BEGIN DECLARE results ARRAY; DECLARE query STRING; SET query = '\''FROM metrics-sample | WHERE metric_name == \"'\'' || metric_name_param || '\''\" | STATS avg_value = AVG(value), max_value = MAX(value), min_value = MIN(value)'\''; SET results = ESQL_QUERY(query); IF ARRAY_LENGTH(results) > 0 THEN RETURN {\"metric\": metric_name_param, \"average\": DOCUMENT_GET(results[0], '\''avg_value'\''), \"maximum\": DOCUMENT_GET(results[0], '\''max_value'\''), \"minimum\": DOCUMENT_GET(results[0], '\''min_value'\'')}; ELSE RETURN {\"error\": \"No data found for metric\"}; END IF; END PROCEDURE;"
    }' > /dev/null 2>&1
    echo "    ✓ aggregate_metrics"
    
    # Procedure 5: order_summary
    curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
        "query": "CREATE PROCEDURE order_summary() BEGIN DECLARE orders ARRAY; DECLARE by_status ARRAY; SET orders = ESQL_QUERY('\''FROM orders-sample | STATS order_count = COUNT(*), total_revenue = SUM(total_amount)'\''); SET by_status = ESQL_QUERY('\''FROM orders-sample | STATS count = COUNT(*) BY status | SORT count DESC'\''); PRINT '\''Order Summary: '\'' || DOCUMENT_GET(orders[0], '\''order_count'\'') || '\'' orders, $'\'' || DOCUMENT_GET(orders[0], '\''total_revenue'\'') || '\'' revenue'\''; RETURN {\"total_orders\": DOCUMENT_GET(orders[0], '\''order_count'\''), \"total_revenue\": DOCUMENT_GET(orders[0], '\''total_revenue'\''), \"by_status\": by_status}; END PROCEDURE;"
    }' > /dev/null 2>&1
    echo "    ✓ order_summary"
    
    # Procedure 6: security_audit
    curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
        "query": "CREATE PROCEDURE security_audit(sev STRING DEFAULT '\''all'\'') BEGIN DECLARE events ARRAY; DECLARE by_type ARRAY; IF sev == '\''all'\'' THEN SET events = ESQL_QUERY('\''FROM security-events | STATS total = COUNT(*)'\''); SET by_type = ESQL_QUERY('\''FROM security-events | STATS count = COUNT(*) BY event_type | SORT count DESC'\''); ELSE SET events = ESQL_QUERY('\''FROM security-events | WHERE severity == \"'\'' || sev || '\''\" | STATS total = COUNT(*)'\''); SET by_type = ESQL_QUERY('\''FROM security-events | WHERE severity == \"'\'' || sev || '\''\" | STATS count = COUNT(*) BY event_type | SORT count DESC'\''); END IF; RETURN {\"total_events\": DOCUMENT_GET(events[0], '\''total'\''), \"events_by_type\": by_type}; END PROCEDURE;"
    }' > /dev/null 2>&1
    echo "    ✓ security_audit"
    
    # Procedure 7: search_products
    curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
        "query": "CREATE PROCEDURE search_products(search_term STRING, max_results NUMBER DEFAULT 10) BEGIN DECLARE results ARRAY; SET results = ESQL_QUERY('\''FROM products-sample | WHERE name LIKE \"*'\'' || search_term || '\''*\" | SORT price ASC | LIMIT '\'' || max_results); PRINT '\''Found '\'' || ARRAY_LENGTH(results) || '\'' products matching \"'\'' || search_term || '\''\"'\''; RETURN results; END PROCEDURE;"
    }' > /dev/null 2>&1
    echo "    ✓ search_products"
    
    # Procedure 8: health_check
    curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
        "query": "CREATE PROCEDURE health_check() BEGIN DECLARE error_results ARRAY; DECLARE security_results ARRAY; DECLARE health_status STRING; DECLARE issues ARRAY; SET issues = []; SET health_status = '\''healthy'\''; SET error_results = ESQL_QUERY('\''FROM logs-sample | WHERE level == \"ERROR\" | STATS error_count = COUNT(*)'\''); IF DOCUMENT_GET(error_results[0], '\''error_count'\'') > 10 THEN SET health_status = '\''warning'\''; SET issues = ARRAY_APPEND(issues, '\''High error count in logs'\''); END IF; SET security_results = ESQL_QUERY('\''FROM security-events | WHERE severity == \"critical\" | STATS critical_count = COUNT(*)'\''); IF DOCUMENT_GET(security_results[0], '\''critical_count'\'') > 0 THEN SET health_status = '\''critical'\''; SET issues = ARRAY_APPEND(issues, '\''Critical security events detected'\''); END IF; RETURN {\"status\": health_status, \"timestamp\": CURRENT_TIMESTAMP(), \"issues\": issues}; END PROCEDURE;"
    }' > /dev/null 2>&1
    echo "    ✓ health_check"
    
    # Procedure 9: generate_report
    curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
        "query": "CREATE PROCEDURE generate_report(report_type STRING) BEGIN DECLARE report DOCUMENT; IF report_type == '\''users'\'' THEN SET report = CALL get_user_stats(); ELSEIF report_type == '\''orders'\'' THEN SET report = CALL order_summary(); ELSEIF report_type == '\''security'\'' THEN SET report = CALL security_audit('\''all'\''); ELSE RETURN {\"error\": \"Unknown report type. Available: users, orders, security\"}; END IF; RETURN {\"report_type\": report_type, \"generated_at\": CURRENT_TIMESTAMP(), \"data\": report}; END PROCEDURE;"
    }' > /dev/null 2>&1
    echo "    ✓ generate_report"
    
    # Procedure 10: demo_workflow
    curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
        "query": "CREATE PROCEDURE demo_workflow() BEGIN PRINT '\''=== Moltler Demo Workflow ==='\''; PRINT '\''Step 1: Running health check...'\''; DECLARE health DOCUMENT; SET health = CALL health_check(); PRINT '\''  Status: '\'' || DOCUMENT_GET(health, '\''status'\''); PRINT '\''Step 2: Getting user statistics...'\''; DECLARE user_stats DOCUMENT; SET user_stats = CALL get_user_stats(); PRINT '\''  Total users: '\'' || DOCUMENT_GET(user_stats, '\''total_users'\''); PRINT '\''Step 3: Generating order summary...'\''; DECLARE orders DOCUMENT; SET orders = CALL order_summary(); PRINT '\''  Total revenue: $'\'' || DOCUMENT_GET(orders, '\''total_revenue'\''); PRINT '\''=== Demo Complete ==='\''; RETURN {\"success\": TRUE, \"health\": health, \"users\": user_stats, \"orders\": orders}; END PROCEDURE;"
    }' > /dev/null 2>&1
    echo "    ✓ demo_workflow"
    
    print_success "Demo procedures loaded!"
    
    # Now create Skills that wrap the procedures
    print_step "Creating demo skills..."
    
    # Drop existing skills first (clean slate)
    for skill_name in "hello_moltler" "log_analyzer" "user_stats" "order_report" "system_health" "product_search" \
                      "check_cluster_health" "count_logs_by_level" "get_recent_errors" "metrics_summary" "find_popular_issues"; do
        curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d "{
            \"query\": \"DROP SKILL $skill_name\"
        }" > /dev/null 2>&1
    done
    
    # Skill 1: greeting_skill - wraps hello_world
    curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
        "query": "CREATE SKILL greeting_skill VERSION '\''1.0'\'' DESCRIPTION '\''A friendly greeting demonstrating basic skill usage'\'' AUTHOR '\''Moltler'\'' TAGS ['\''demo'\'', '\''beginner'\''] RETURNS STRING BEGIN CALL hello_world(); RETURN '\''Greeting complete!'\''; END SKILL;"
    }' > /dev/null 2>&1
    echo "    ✓ greeting_skill (wraps hello_world)"
    
    # Skill 2: log_analyzer - wraps analyze_logs
    curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
        "query": "CREATE SKILL log_analyzer VERSION '\''1.0'\'' DESCRIPTION '\''Analyze application logs and identify error patterns'\'' AUTHOR '\''Moltler'\'' TAGS ['\''logs'\'', '\''analytics'\'', '\''monitoring'\''] RETURNS ARRAY BEGIN DECLARE results ARRAY; SET results = CALL analyze_logs('\''logs-sample'\''); RETURN results; END SKILL;"
    }' > /dev/null 2>&1
    echo "    ✓ log_analyzer (wraps analyze_logs)"
    
    # Skill 3: user_statistics - wraps get_user_stats
    curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
        "query": "CREATE SKILL user_statistics VERSION '\''1.0'\'' DESCRIPTION '\''Get comprehensive user statistics including admin and active counts'\'' AUTHOR '\''Moltler'\'' TAGS ['\''users'\'', '\''analytics'\''] RETURNS DOCUMENT BEGIN DECLARE stats DOCUMENT; SET stats = CALL get_user_stats(); RETURN stats; END SKILL;"
    }' > /dev/null 2>&1
    echo "    ✓ user_statistics (wraps get_user_stats)"
    
    # Skill 4: order_report - wraps order_summary
    curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
        "query": "CREATE SKILL order_report VERSION '\''1.0'\'' DESCRIPTION '\''Generate e-commerce order summary with revenue and status breakdown'\'' AUTHOR '\''Moltler'\'' TAGS ['\''orders'\'', '\''ecommerce'\'', '\''analytics'\''] RETURNS DOCUMENT BEGIN DECLARE report DOCUMENT; SET report = CALL order_summary(); RETURN report; END SKILL;"
    }' > /dev/null 2>&1
    echo "    ✓ order_report (wraps order_summary)"
    
    # Skill 5: system_health - wraps health_check
    curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
        "query": "CREATE SKILL system_health VERSION '\''1.0'\'' DESCRIPTION '\''Check overall system health and identify issues'\'' AUTHOR '\''Moltler'\'' TAGS ['\''health'\'', '\''monitoring'\'', '\''devops'\''] RETURNS DOCUMENT BEGIN DECLARE health DOCUMENT; SET health = CALL health_check(); RETURN health; END SKILL;"
    }' > /dev/null 2>&1
    echo "    ✓ system_health (wraps health_check)"
    
    # Skill 6: product_finder - wraps search_products
    curl -s $AUTH -X POST "$ES/_escript" -H "Content-Type: application/json" -d '{
        "query": "CREATE SKILL product_finder VERSION '\''1.0'\'' DESCRIPTION '\''Search for products by keyword'\'' AUTHOR '\''Moltler'\'' TAGS ['\''products'\'', '\''search'\'', '\''ecommerce'\''] (keyword STRING) RETURNS ARRAY BEGIN DECLARE products ARRAY; SET products = CALL search_products(keyword, 10); RETURN products; END SKILL;"
    }' > /dev/null 2>&1
    echo "    ✓ product_finder (wraps search_products)"
    
    print_success "Demo skills loaded!"
    echo ""
    echo "    The Skills Manager UI shows 3 tabs:"
    echo "      • Skills (6) - AI-ready wrappers: greeting_skill, log_analyzer, user_statistics, etc."
    echo "      • Procedures (10) - Reusable logic: hello_world, analyze_logs, get_user_stats, etc."
    echo "      • Functions - User-defined functions"
    echo ""
    echo "    MCP Endpoint for AI agents:"
    echo "      POST http://localhost:9200/_escript/mcp"
    echo ""
    echo "    For Claude Desktop, add to your config:"
    echo "      {\"mcpServers\": {\"moltler\": {\"command\": \"npx\", \"args\": [\"@moltler/mcp-bridge\", \"--es-url\", \"http://localhost:9200\"]}}}"
    echo ""
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
    echo "  --load-skills     Load sample Moltler skills"
    echo "  --notebooks       Start Jupyter notebooks"
    echo ""
    echo "MoltlerHub (Web Portal):"
    echo "  --hub             Start MoltlerHub web portal (http://localhost:3000) [recommended]"
    echo "  --stop-hub        Stop MoltlerHub"
    echo ""
    echo "Legacy Skills Manager (deprecated):"
    echo "  --moltler         Full setup: ES + demo data + Legacy Skills Manager UI"
    echo "  --ui              Start Legacy Skills Manager web UI only (http://localhost:3000)"
    echo "  --stop-ui         Stop the Legacy UI"
    echo ""
    echo "Kibana:"
    echo "  --kibana          Start pre-built Kibana (for APM/observability)"
    echo ""
    echo "Observability:"
    echo "  --otel            Start OTEL Collector (for distributed tracing)"
    echo ""
    echo "Stop Services:"
    echo "  --stop            Stop all services"
    echo "  --stop-notebooks  Stop only Jupyter notebooks"
    echo "  --stop-kibana     Stop only Kibana"
    echo "  --stop-otel       Stop only OTEL Collector"
    echo ""
    echo "Other:"
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
    stop_moltler_hub
    stop_moltler_ui
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
# Moltler Skills Manager Web UI
# =============================================================================

MOLTLER_UI_DIR="$PROJECT_ROOT/moltler-ui"
MOLTLER_UI_PORT=3000
MOLTLER_UI_PID="$PROJECT_ROOT/.moltler_ui_pid"
MOLTLER_UI_LOG="$PROJECT_ROOT/moltler-ui.log"

# Setup Moltler UI (install dependencies)
setup_moltler_ui() {
    print_header "Setting up Moltler Skills Manager UI"
    
    if [ ! -d "$MOLTLER_UI_DIR" ]; then
        print_error "Moltler UI directory not found at $MOLTLER_UI_DIR"
        return 1
    fi
    
    # Check if Node.js is installed
    if ! command -v node &> /dev/null; then
        print_error "Node.js is required for Moltler UI"
        echo "  Install Node.js 18+ from https://nodejs.org/"
        return 1
    fi
    
    print_success "Node.js $(node -v)"
    
    # Install dependencies if needed
    if [ ! -d "$MOLTLER_UI_DIR/node_modules" ]; then
        print_step "Installing dependencies..."
        cd "$MOLTLER_UI_DIR"
        npm install
        print_success "Dependencies installed"
        cd "$PROJECT_ROOT"
    else
        print_success "Dependencies already installed"
    fi
    
    return 0
}

# Start Moltler UI
start_moltler_ui() {
    print_header "Starting Moltler Skills Manager UI"
    
    # Setup if needed
    setup_moltler_ui || return 1
    
    # Check if already running
    if curl -s http://localhost:$MOLTLER_UI_PORT > /dev/null 2>&1; then
        print_success "Moltler UI already running at http://localhost:$MOLTLER_UI_PORT"
        return 0
    fi
    
    # Check if Elasticsearch is running
    if ! curl -s -u elastic-admin:elastic-password http://localhost:9200 > /dev/null 2>&1; then
        print_warning "Elasticsearch not running. Starting it first..."
        start_elasticsearch_background
        print_step "Waiting for Elasticsearch..."
        for i in {1..30}; do
            if curl -s -u elastic-admin:elastic-password http://localhost:9200 > /dev/null 2>&1; then
                print_success "Elasticsearch ready"
                break
            fi
            sleep 2
            echo -n "."
        done
        echo ""
    fi
    
    print_step "Starting Moltler UI in background..."
    cd "$MOLTLER_UI_DIR"
    
    nohup npm run dev -- --host > "$MOLTLER_UI_LOG" 2>&1 &
    UI_PID=$!
    echo $UI_PID > "$MOLTLER_UI_PID"
    
    print_step "Waiting for UI to be ready..."
    
    for i in {1..30}; do
        if curl -s http://localhost:$MOLTLER_UI_PORT > /dev/null 2>&1; then
            echo ""
            print_success "Moltler Skills Manager UI is ready!"
            echo ""
            echo "  ⚡ Open: http://localhost:$MOLTLER_UI_PORT"
            echo ""
            echo "  Features:"
            echo "    - View all skills as a sortable, filterable table"
            echo "    - Click a skill to see its details in a flyout panel"
            echo "    - Edit skills with Monaco editor (syntax highlighting + autocomplete)"
            echo "    - Create new procedures and functions"
            echo "    - Execute skills directly from the UI"
            echo ""
            cd "$PROJECT_ROOT"
            return 0
        fi
        sleep 1
        echo -n "."
    done
    
    echo ""
    print_warning "UI taking longer than expected. Check moltler-ui.log"
    print_step "Tail of log:"
    tail -10 "$MOLTLER_UI_LOG" 2>/dev/null || true
    cd "$PROJECT_ROOT"
    return 1
}

# Stop Moltler UI
stop_moltler_ui() {
    print_header "Stopping Moltler Skills Manager UI"
    
    if [ -f "$MOLTLER_UI_PID" ]; then
        PID=$(cat "$MOLTLER_UI_PID")
        if kill -0 $PID 2>/dev/null; then
            kill $PID
            rm "$MOLTLER_UI_PID"
            print_success "Moltler UI stopped"
        else
            print_warning "Moltler UI process not found"
            rm "$MOLTLER_UI_PID"
        fi
    else
        # Try to find and kill by port
        PID=$(lsof -ti:$MOLTLER_UI_PORT 2>/dev/null)
        if [ -n "$PID" ]; then
            kill $PID 2>/dev/null
            print_success "Moltler UI stopped (port $MOLTLER_UI_PORT)"
        else
            print_warning "Moltler UI not running"
        fi
    fi
}

# =============================================================================
# MoltlerHub (Web Portal) - RECOMMENDED
# =============================================================================

MOLTLER_HUB_DIR="$PROJECT_ROOT/moltler-hub"
MOLTLER_HUB_PORT=3000
MOLTLER_HUB_PID="$PROJECT_ROOT/.moltler_hub_pid"
MOLTLER_HUB_LOG="$PROJECT_ROOT/moltler-hub.log"

# Setup MoltlerHub (install dependencies)
setup_moltler_hub() {
    print_header "Setting up MoltlerHub"
    
    if [ ! -d "$MOLTLER_HUB_DIR" ]; then
        print_error "MoltlerHub directory not found: $MOLTLER_HUB_DIR"
        return 1
    fi
    
    cd "$MOLTLER_HUB_DIR"
    
    # Check if node_modules exists
    if [ ! -d "node_modules" ]; then
        print_step "Installing dependencies..."
        npm install
    else
        print_success "Dependencies already installed"
    fi
    
    # Generate skills data from local hub/skills directory
    if [ -f "scripts/generate-skills.ts" ]; then
        print_step "Generating skills data..."
        npx ts-node scripts/generate-skills.ts 2>/dev/null || true
    fi
    
    cd "$PROJECT_ROOT"
    print_success "MoltlerHub setup complete"
}

# Start MoltlerHub
start_moltler_hub() {
    print_header "Starting MoltlerHub"
    
    # Stop legacy UI if running (same port)
    if [ -f "$MOLTLER_UI_PID" ]; then
        stop_moltler_ui
    fi
    
    # Check if already running
    if [ -f "$MOLTLER_HUB_PID" ]; then
        PID=$(cat "$MOLTLER_HUB_PID")
        if kill -0 $PID 2>/dev/null; then
            print_success "MoltlerHub already running (PID: $PID)"
            echo "  Open http://localhost:$MOLTLER_HUB_PORT"
            return 0
        fi
    fi
    
    # Setup if needed
    setup_moltler_hub
    
    cd "$MOLTLER_HUB_DIR"
    
    print_step "Starting MoltlerHub on port $MOLTLER_HUB_PORT..."
    npm run dev > "$MOLTLER_HUB_LOG" 2>&1 &
    echo $! > "$MOLTLER_HUB_PID"
    
    # Wait for startup
    print_step "Waiting for MoltlerHub to start"
    for i in {1..30}; do
        if curl -s http://localhost:$MOLTLER_HUB_PORT > /dev/null 2>&1; then
            echo ""
            print_success "MoltlerHub started!"
            echo ""
            echo "  Browse skills at: http://localhost:$MOLTLER_HUB_PORT"
            echo ""
            echo "  MoltlerHub features:"
            echo "    - Browse 155+ skills"
            echo "    - Search and filter by category"
            echo "    - View skill documentation"
            echo "    - Get install commands"
            echo ""
            cd "$PROJECT_ROOT"
            return 0
        fi
        sleep 1
        echo -n "."
    done
    
    echo ""
    print_warning "MoltlerHub taking longer than expected. Check moltler-hub.log"
    print_step "Tail of log:"
    tail -10 "$MOLTLER_HUB_LOG" 2>/dev/null || true
    cd "$PROJECT_ROOT"
    return 1
}

# Stop MoltlerHub
stop_moltler_hub() {
    print_header "Stopping MoltlerHub"
    
    if [ -f "$MOLTLER_HUB_PID" ]; then
        PID=$(cat "$MOLTLER_HUB_PID")
        if kill -0 $PID 2>/dev/null; then
            kill $PID
            rm "$MOLTLER_HUB_PID"
            print_success "MoltlerHub stopped"
        else
            print_warning "MoltlerHub process not found"
            rm "$MOLTLER_HUB_PID"
        fi
    else
        # Try to find and kill by port
        PID=$(lsof -ti:$MOLTLER_HUB_PORT 2>/dev/null)
        if [ -n "$PID" ]; then
            kill $PID 2>/dev/null
            print_success "MoltlerHub stopped (port $MOLTLER_HUB_PORT)"
        else
            print_warning "MoltlerHub not running"
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
    
    # Web Portal (MoltlerHub or legacy UI)
    echo ""
    echo "Web Portal (port 3000):"
    if curl -s http://localhost:3000 > /dev/null 2>&1; then
        if [ -f "$MOLTLER_HUB_PID" ]; then
            print_success "MoltlerHub running at http://localhost:3000"
        elif [ -f "$MOLTLER_UI_PID" ]; then
            print_success "Legacy Skills Manager running at http://localhost:3000"
        else
            print_success "Running at http://localhost:3000"
        fi
    else
        print_warning "Not running (start with --hub)"
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
    --load-skills)
        load_sample_skills
        ;;
    --notebooks)
        start_notebooks
        ;;
    --kibana)
        start_kibana_background
        ;;
    --moltler)
        # Full setup: ES + Demo + Moltler UI - ONE COMMAND
        print_header "⚡ Moltler Full Setup (ES + Skills Manager UI)"
        
        # 1. Check prerequisites (includes submodule init)
        check_prerequisites
        
        # 2. Build if needed
        build_plugin
        
        # 3. Start ES in background if not running
        if ! curl -s -u elastic-admin:elastic-password http://localhost:9200 > /dev/null 2>&1; then
            start_elasticsearch_background
            # Wait for ES
            print_step "Waiting for Elasticsearch..."
            for i in {1..60}; do
                if curl -s -u elastic-admin:elastic-password http://localhost:9200 > /dev/null 2>&1; then
                    print_success "Elasticsearch ready"
                    break
                fi
                sleep 2
                echo -n "."
            done
            echo ""
        else
            print_success "Elasticsearch already running"
        fi
        
        # 4. Load sample data
        load_sample_data
        
        # 5. Load demo procedures for the Skills Manager
        load_demo_procedures
        
        # 6. Start Moltler Skills Manager UI
        start_moltler_ui
        
        # 7. Final summary
        echo ""
        print_header "🎉 Moltler is Ready!"
        echo "  Elasticsearch:        http://localhost:9200"
        echo "  Skills Manager UI:    http://localhost:$MOLTLER_UI_PORT"
        echo ""
        echo "  Open the Skills Manager to view, edit, and run your skills!"
        echo ""
        ;;
    --hub)
        start_moltler_hub
        ;;
    --stop-hub)
        stop_moltler_hub
        ;;
    --ui)
        start_moltler_ui
        ;;
    --stop-ui)
        stop_moltler_ui
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
                
                # Start Moltler Skills Manager UI if not running
                if ! curl -s http://localhost:3000 > /dev/null 2>&1; then
                    start_moltler_ui
                fi
                
                echo ""
                print_header "🚀 Launching Jupyter Notebooks & Kibana"
                cd "$NOTEBOOKS_DIR"
                python3 -m notebook --notebook-dir="$NOTEBOOKS_DIR" &
                JUPYTER_PID=$!
                echo ""
                print_success "Moltler Skills Manager at http://localhost:3000"
                print_success "Jupyter started at http://localhost:8888"
                print_success "Kibana available at http://localhost:5601"
                print_success "OTEL Collector ready at localhost:4317/4318"
                print_success "View traces at http://localhost:5601/app/apm"
                echo ""
                
                # Open UI, notebooks, and Kibana in browser
                sleep 2
                if command -v open &> /dev/null; then
                    open "http://localhost:3000"
                    open "http://localhost:8888"
                    open "http://localhost:5601/app/apm"
                elif command -v xdg-open &> /dev/null; then
                    xdg-open "http://localhost:3000"
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
            echo "  9. Start Moltler Skills Manager UI"
            echo "  10. Start Jupyter notebooks"
            echo "  11. Open Skills Manager, Jupyter, and Kibana APM in browser"
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
                
                # Start Moltler Skills Manager UI
                start_moltler_ui
                
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
