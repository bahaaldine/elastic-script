# OpenTelemetry Tracing

elastic-script provides native OpenTelemetry support using the **Elastic Distribution of OpenTelemetry (EDOT)** Java agent. Tracing is **enabled by default** - just run quick-start.sh and traces will appear in Kibana APM.

## Quick Start

The quickest way to get started is to run the full setup:

```bash
./scripts/quick-start.sh
```

This will:
1. Download the EDOT Java agent
2. Build Elasticsearch with elastic-script
3. Download and configure Kibana
4. Start everything with tracing enabled
5. Open Jupyter notebooks and Kibana APM in your browser

View traces at: **http://localhost:5601/app/apm**

## Elastic Distribution of OpenTelemetry (EDOT)

elastic-script uses EDOT, Elastic's distribution of OpenTelemetry that provides:
- Optimized performance for Elastic backends
- Pre-configured for Elastic APM
- Full compatibility with OpenTelemetry standards

Reference: [EDOT Java Setup](https://www.elastic.co/docs/reference/opentelemetry/edot-sdks/java/setup)

## Manual Configuration

If you need to customize the EDOT configuration:

```bash
# Download EDOT agent manually
curl -L -o elastic-otel-javaagent.jar \
  https://repo1.maven.org/maven2/co/elastic/otel/elastic-otel-javaagent/1.3.0/elastic-otel-javaagent-1.3.0.jar

# Configure environment
export OTEL_SERVICE_NAME=elastic-script
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:9200
export OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf

# Start with agent
ES_JAVA_OPTS="-javaagent:elastic-otel-javaagent.jar" ./bin/elasticsearch
```

## Backend Options

### Kibana APM (Default)

When using quick-start.sh, traces are automatically sent to the local Elasticsearch and visible in Kibana APM:

```
http://localhost:5601/app/apm
```

### Elastic Cloud

For Elastic Cloud deployments:

```bash
export OTEL_EXPORTER_OTLP_ENDPOINT=https://<your-cloud-id>.apm.us-east-1.aws.elastic.cloud
export OTEL_EXPORTER_OTLP_HEADERS="Authorization=ApiKey YOUR_API_KEY"
```

### Other OTEL Backends

EDOT is compatible with any OpenTelemetry backend:

**Jaeger:**
```bash
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317
```

**Grafana Tempo:**
```bash
export OTEL_EXPORTER_OTLP_ENDPOINT=http://tempo:4317
```

## Trace Structure

elastic-script creates the following span hierarchy:

```
escript.procedure.execute <procedure_name>
├── escript.statement.declare
├── escript.statement.set
├── escript.function.esql_query
│   └── ESQL: FROM logs-* | WHERE ...
├── escript.loop.for[0]
│   └── escript.function.index_document
├── escript.loop.for[1]
│   └── escript.function.index_document
└── escript.statement.return
```

## Semantic Attributes

### Procedure Spans

| Attribute | Description |
|-----------|-------------|
| `escript.execution.id` | Unique execution identifier |
| `escript.procedure.name` | Name of the procedure |
| `code.function` | Procedure name |
| `code.namespace` | Always "escript" |

### Database Spans (ESQL)

| Attribute | Description |
|-----------|-------------|
| `db.system` | "elasticsearch" |
| `db.operation` | "esql" |
| `db.statement` | The full ESQL query |

### HTTP Spans (External Calls)

| Attribute | Description |
|-----------|-------------|
| `http.method` | GET, POST, etc. |
| `http.url` | Target URL |
| `peer.service` | Service name |

### Loop Spans

| Attribute | Description |
|-----------|-------------|
| `escript.loop.type` | FOR, WHILE, FORALL |
| `escript.loop.iteration` | Current iteration number |

## Example: Complete Setup with Jaeger

```bash
# 1. Start Jaeger
docker run -d --name jaeger \
  -p 16686:16686 \
  -p 4317:4317 \
  jaegertracing/all-in-one:latest

# 2. Download OTEL agent
curl -L -o opentelemetry-javaagent.jar \
  https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar

# 3. Set environment
export OTEL_SERVICE_NAME=elasticsearch
export OTEL_TRACES_EXPORTER=otlp
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317

# 4. Start Elasticsearch
ES_JAVA_OPTS="-javaagent:$(pwd)/opentelemetry-javaagent.jar" \
  ./scripts/quick-start.sh

# 5. Execute a procedure
curl -u elastic-admin:elastic-password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "CALL my_procedure()"}'

# 6. View traces at http://localhost:16686
```

## Trace Correlation

Every procedure execution receives a unique `execution.id` that appears in:

- All related spans
- Log messages (when DEBUG logging is enabled)
- API responses

This enables end-to-end correlation across distributed systems.

## Sampling Configuration

For high-volume production environments:

```bash
# Sample 10% of traces
export OTEL_TRACES_SAMPLER=parentbased_traceidratio
export OTEL_TRACES_SAMPLER_ARG=0.1

# Always sample errors
export OTEL_TRACES_SAMPLER=parentbased_always_on
```

## Troubleshooting

### No Traces Appearing

1. Verify the agent is loaded:
   ```bash
   # Check Elasticsearch logs for:
   # "OpenTelemetry detected - distributed tracing enabled"
   ```

2. Test OTLP connectivity:
   ```bash
   curl -v http://localhost:4317
   ```

3. Enable debug logging:
   ```bash
   export OTEL_LOG_LEVEL=debug
   ```

### High Overhead

1. Reduce sampling rate
2. Use batch exporter (default)
3. Increase batch size:
   ```bash
   export OTEL_BSP_MAX_EXPORT_BATCH_SIZE=1024
   ```

## See Also

- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [OTEL Java Agent Releases](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases)
- [Jaeger Documentation](https://www.jaegertracing.io/docs/)
- [Elastic APM OTLP Integration](https://www.elastic.co/guide/en/apm/guide/current/open-telemetry.html)
