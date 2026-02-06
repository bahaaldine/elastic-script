# OpenTelemetry Distributed Tracing

elastic-script provides **out-of-the-box distributed tracing** via an integrated OpenTelemetry Collector. Tracing is enabled by default—just run `quick-start.sh` and traces appear in Kibana APM automatically.

## Quick Start

```bash
# One command to rule them all
./scripts/quick-start.sh
```

This starts:
- **Elasticsearch** on port 9200
- **OTEL Collector** on ports 4317 (gRPC) and 4318 (HTTP)
- **Kibana** on port 5601
- **Jupyter** on port 8888

View traces at: **http://localhost:5601/app/apm**

---

## Automatic Jupyter Notebook Tracing

**Every notebook cell you execute is automatically traced!** The PL|ESQL Jupyter kernel includes built-in OTEL tracing that sends spans to the collector for each cell execution.

### What Gets Traced

- **Procedure calls**: `CALL my_procedure()` → span named `escript: CALL MY_PROCEDURE`
- **Procedure creation**: `CREATE PROCEDURE ...` → span named `escript: CREATE PROCEDURE name`
- **Function creation**: `CREATE FUNCTION ...` → span named `escript: CREATE FUNCTION name`
- **ESQL queries**: Cells containing `ESQL_QUERY` → span named `escript: ESQL_QUERY`
- **All other statements**: First 30 characters of the code

### Trace Attributes

Each kernel trace includes comprehensive attributes following OTEL semantic conventions:

| Attribute | Description | Example |
|-----------|-------------|---------|
| `service.name` | Service identifier | `elastic-script` |
| `service.version` | Kernel version | `1.0.0` |
| `db.system` | Database type | `elasticsearch` |
| `db.operation` | Operation type | `execute` |
| `db.statement` | Full query (truncated to 500 chars) | `CREATE PROCEDURE...` |
| `escript.statement.type` | Statement type | `CALL`, `CREATE PROCEDURE` |
| `escript.statement.name` | Full statement name | `CALL MY_PROCEDURE` |
| `escript.execution.cell_number` | Notebook cell number | `5` |
| `escript.execution.duration_ms` | Execution time | `123.45` |
| `escript.execution.status` | Result status | `ok` or `error` |
| `escript.code.length` | Query character count | `256` |
| `escript.code.lines` | Query line count | `12` |
| `error.type` | Error classification | `ExecutionError` |
| `error.message` | Error details (if failed) | `Procedure not found` |

### Viewing Notebook Traces

1. Run notebook cells in Jupyter
2. Open Kibana APM: http://localhost:5601/app/apm
3. Look for service: **elastic-script**
4. Click on transactions to see individual cell executions

### Kernel Installation

The kernel is automatically installed/updated when you run `quick-start.sh`. This ensures you always have the latest tracing features. The kernel detects your platform:

- **macOS**: `~/Library/Jupyter/kernels/plesql_kernel/`
- **Linux**: `~/.local/share/jupyter/kernels/plesql_kernel/`

To manually reinstall the kernel:
```bash
cd notebooks/kernel && bash install.sh
```

**Note**: After updating the kernel, restart your Jupyter kernel (Kernel → Restart) to pick up changes.

---

## Architecture

```
┌─────────────────────┐     OTLP      ┌─────────────────┐    OTLP/HTTP   ┌────────────────┐
│  Jupyter Kernel     │──────────────▶│  OTEL Collector │──────────────▶│   APM Server   │
│  (plesql_kernel)    │  :4317/:4318  │  (otelcol)      │     :8200     │   (apm-server) │
└─────────────────────┘               └─────────────────┘               └────────────────┘
                                                                               │
                                                                               │ ES API
                                                                               ▼
                                                                        ┌───────────────┐
                                                                        │ Elasticsearch │
                                                                        │    :9200      │
                                                                        └───────────────┘
                                                                               │
                                                                               ▼
                                                                        ┌───────────────┐
                                                                        │ Kibana APM    │
                                                                        │    :5601      │
                                                                        └───────────────┘
```

**Components:**

1. **OTEL Collector** (ports 4317/4318):
   - Receives traces via OTLP (gRPC on 4317, HTTP on 4318)
   - Batches and processes spans
   - Forwards traces to APM Server via OTLP/HTTP

2. **APM Server** (port 8200):
   - Receives OTLP traces from the collector
   - Converts to proper Elastic APM format
   - Indexes into Elasticsearch with correct mappings
   - Creates service metrics and transaction summaries

3. **Kibana APM** (/app/apm):
   - Full APM UI with service map, transactions, traces
   - Works with any OTEL-instrumented application

---

## Sending Traces from Your Application

### Environment Variables (Any Language)

Configure any OTEL-instrumented application to send traces:

```bash
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318
export OTEL_SERVICE_NAME=my-service
export OTEL_TRACES_EXPORTER=otlp
```

### Python Example

```bash
pip install opentelemetry-api opentelemetry-sdk opentelemetry-exporter-otlp
```

```python
from opentelemetry import trace
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.exporter.otlp.proto.http.trace_exporter import OTLPSpanExporter

# Configure
trace.set_tracer_provider(TracerProvider())
tracer = trace.get_tracer("my-service")

# Export to OTEL Collector
exporter = OTLPSpanExporter(endpoint="http://localhost:4318/v1/traces")
trace.get_tracer_provider().add_span_processor(BatchSpanProcessor(exporter))

# Use
with tracer.start_as_current_span("my-operation"):
    # your code here
    pass
```

### JavaScript/Node.js Example

```bash
npm install @opentelemetry/api @opentelemetry/sdk-node @opentelemetry/exporter-trace-otlp-http
```

```javascript
const { NodeSDK } = require('@opentelemetry/sdk-node');
const { OTLPTraceExporter } = require('@opentelemetry/exporter-trace-otlp-http');

const sdk = new NodeSDK({
  serviceName: 'my-service',
  traceExporter: new OTLPTraceExporter({
    url: 'http://localhost:4318/v1/traces',
  }),
});

sdk.start();
```

### Java Example

```bash
# Download OTEL Java agent
curl -L -o opentelemetry-javaagent.jar \
  https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar
```

```bash
# Run your Java app with auto-instrumentation
java -javaagent:opentelemetry-javaagent.jar \
  -Dotel.service.name=my-service \
  -Dotel.exporter.otlp.endpoint=http://localhost:4318 \
  -jar my-app.jar
```

### Go Example

```go
import (
    "go.opentelemetry.io/otel"
    "go.opentelemetry.io/otel/exporters/otlp/otlptrace/otlptracehttp"
    "go.opentelemetry.io/otel/sdk/trace"
)

exporter, _ := otlptracehttp.New(ctx,
    otlptracehttp.WithEndpoint("localhost:4318"),
    otlptracehttp.WithInsecure(),
)

tp := trace.NewTracerProvider(trace.WithBatcher(exporter))
otel.SetTracerProvider(tp)
```

### cURL Test (Manual Trace)

```bash
curl -X POST http://localhost:4318/v1/traces \
  -H "Content-Type: application/json" \
  -d '{
    "resourceSpans": [{
      "resource": {
        "attributes": [{"key": "service.name", "value": {"stringValue": "test-service"}}]
      },
      "scopeSpans": [{
        "scope": {"name": "test"},
        "spans": [{
          "traceId": "5B8EFFF798038103D269B633813FC60C",
          "spanId": "EEE19B7EC3C1B174",
          "name": "test-span",
          "startTimeUnixNano": "'$(date +%s)000000000'",
          "endTimeUnixNano": "'$(date +%s)000000001'",
          "kind": 1
        }]
      }]
    }]
  }'
```

---

## OTEL Collector Configuration

The collector configuration is at `otel-collector/config.yaml`:

```yaml
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
  elasticsearch/traces:
    endpoints: ["http://localhost:9200"]
    user: elastic-admin
    password: elastic-password
    traces_index: traces-apm-default
    mapping:
      mode: ecs

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [elasticsearch/traces]
```

### Customizing the Configuration

You can modify `otel-collector/config.yaml` to:

1. **Add more exporters** (e.g., Jaeger, Zipkin, Grafana Tempo)
2. **Add processors** (e.g., filtering, sampling, attribute modification)
3. **Configure authentication** for Elasticsearch
4. **Enable metrics and logs** collection

Example with multiple backends:

```yaml
exporters:
  elasticsearch/traces:
    endpoints: ["http://localhost:9200"]
    user: elastic-admin
    password: elastic-password
    mapping:
      mode: ecs
  
  jaeger:
    endpoint: jaeger:14250
    tls:
      insecure: true

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [elasticsearch/traces, jaeger]
```

---

## CLI Commands

### Start OTEL Collector Only

```bash
./scripts/quick-start.sh --otel
```

### Stop OTEL Collector

```bash
./scripts/quick-start.sh --stop-otel
```

### Check Status

```bash
./scripts/quick-start.sh --status
```

Output:
```
Elasticsearch (port 9200):
✓ Running

APM Server (port 8200):
✓ Running (OTLP: http://localhost:8200)

OTEL Collector (ports 4317/4318):
✓ Running (gRPC: 4317, HTTP: 4318)

Kibana (port 5601):
✓ Running at http://localhost:5601

Jupyter (port 8888):
✓ Running at http://localhost:8888
```

### Stop All Services

```bash
./scripts/quick-start.sh --stop
```

---

## Viewing Traces in Kibana APM

1. Open Kibana: **http://localhost:5601**
2. Login: `elastic-admin` / `elastic-password`
3. Navigate to **Observability → APM**
4. Select your service from the services list
5. Click on a transaction to see the trace waterfall

### Key Views

| View | URL | Description |
|------|-----|-------------|
| Services | `/app/apm/services` | All instrumented services |
| Transactions | `/app/apm/services/{service}/transactions` | Transaction breakdown |
| Traces | `/app/apm/traces` | Full distributed traces |
| Dependencies | `/app/apm/services/{service}/dependencies` | Service map |

---

## Trace Attributes (Semantic Conventions)

### Standard OTEL Attributes

| Attribute | Description |
|-----------|-------------|
| `service.name` | Name of the service |
| `service.version` | Version of the service |
| `trace.id` | Unique trace identifier |
| `span.id` | Unique span identifier |
| `parent.id` | Parent span ID (for child spans) |

### Database Spans

| Attribute | Description |
|-----------|-------------|
| `db.system` | Database type (e.g., "elasticsearch") |
| `db.operation` | Operation type (e.g., "esql") |
| `db.statement` | Query text |

### HTTP Spans

| Attribute | Description |
|-----------|-------------|
| `http.method` | HTTP method (GET, POST, etc.) |
| `http.url` | Full URL |
| `http.status_code` | Response status code |
| `http.response_content_length` | Response size |

### elastic-script Specific Attributes

| Attribute | Description |
|-----------|-------------|
| `escript.execution.id` | Unique procedure execution ID |
| `escript.procedure.name` | Procedure name |
| `escript.function.name` | Built-in function name |
| `escript.loop.type` | Loop type (FOR, WHILE, FORALL) |
| `escript.loop.iteration` | Current loop iteration |

---

## Sampling Configuration

For high-volume production environments, configure sampling:

```bash
# Sample 10% of traces
export OTEL_TRACES_SAMPLER=parentbased_traceidratio
export OTEL_TRACES_SAMPLER_ARG=0.1

# Always sample errors
export OTEL_TRACES_SAMPLER=parentbased_always_on
```

Or configure in the collector:

```yaml
processors:
  probabilistic_sampler:
    sampling_percentage: 10

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [probabilistic_sampler, batch]
      exporters: [elasticsearch/traces]
```

---

## Sending Traces to Elastic Cloud

To send traces to Elastic Cloud instead of local Elasticsearch:

1. Get your Cloud ID and API key from Elastic Cloud console
2. Update `otel-collector/config.yaml`:

```yaml
exporters:
  elasticsearch/cloud:
    endpoints: ["https://YOUR_CLOUD_ID.es.us-central1.gcp.cloud.es.io:443"]
    api_key: "YOUR_API_KEY"
    mapping:
      mode: ecs

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [elasticsearch/cloud]
```

---

## Troubleshooting

### No Traces Appearing

1. **Check if collector is running:**
   ```bash
   ./scripts/quick-start.sh --status
   ```

2. **Check collector logs:**
   ```bash
   cat otel-collector/collector.log
   ```

3. **Test OTLP endpoint:**
   ```bash
   curl -v http://localhost:4318/v1/traces
   # Should return 405 (Method Not Allowed) - means it's listening
   ```

4. **Verify Elasticsearch indices:**
   ```bash
   curl -u elastic-admin:elastic-password http://localhost:9200/_cat/indices?v | grep traces
   ```

### Notebook Cells Not Producing Traces

1. **Reinstall the kernel** (the installed kernel may be outdated):
   ```bash
   cd notebooks/kernel && bash install.sh
   ```

2. **Restart your Jupyter kernel**: In the notebook, go to Kernel → Restart

3. **Verify the kernel has tracing code:**
   ```bash
   # macOS
   grep -c "OTLPTracer" ~/Library/Jupyter/kernels/plesql_kernel/plesql_kernel.py
   # Should return 2 (meaning tracing code is present)
   
   # Linux
   grep -c "OTLPTracer" ~/.local/share/jupyter/kernels/plesql_kernel/plesql_kernel.py
   ```

4. **Check trace count before and after:**
   ```bash
   curl -s -u elastic-admin:elastic-password "http://localhost:9200/traces-apm-default/_count"
   # Run a notebook cell, then check again - count should increase
   ```

### Traces Not Showing in Kibana

1. **Check data stream exists:**
   ```bash
   curl -u elastic-admin:elastic-password http://localhost:9200/_data_stream/traces-*
   ```

2. **Check for recent documents:**
   ```bash
   curl -u elastic-admin:elastic-password \
     "http://localhost:9200/traces-*/_search?size=1&sort=@timestamp:desc"
   ```

3. **Refresh Kibana index pattern:**
   - Go to Stack Management → Data Views
   - Create or refresh the APM data view

### High Latency

1. **Increase batch size:**
   ```yaml
   processors:
     batch:
       timeout: 5s
       send_batch_size: 2048
   ```

2. **Use gRPC instead of HTTP:**
   ```bash
   export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317
   export OTEL_EXPORTER_OTLP_PROTOCOL=grpc
   ```

### Collector Crashes

1. **Check memory:**
   ```yaml
   processors:
     memory_limiter:
       limit_mib: 512
       spike_limit_mib: 128
       check_interval: 1s
   ```

2. **Enable debug logging:**
   ```yaml
   service:
     telemetry:
       logs:
         level: debug
   ```

---

## Files and Locations

| File | Description |
|------|-------------|
| `otel-collector/config.yaml` | Collector configuration |
| `otel-collector/otelcol-contrib` | Collector binary (auto-downloaded) |
| `otel-collector/collector.log` | Collector logs |
| `otel-collector/collector.pid` | Collector process ID |

---

## See Also

- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [OTEL Collector Configuration](https://opentelemetry.io/docs/collector/configuration/)
- [Elasticsearch OTEL Exporter](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/exporter/elasticsearchexporter)
- [Kibana APM Documentation](https://www.elastic.co/guide/en/kibana/current/apm-getting-started.html)
- [Elastic APM OTLP Integration](https://www.elastic.co/guide/en/apm/guide/current/open-telemetry.html)
