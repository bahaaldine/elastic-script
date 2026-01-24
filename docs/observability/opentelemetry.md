# OpenTelemetry Tracing

elastic-script provides native OpenTelemetry (OTEL) support for distributed tracing. This enables you to trace procedure executions across your infrastructure using standard observability tools.

## Quick Start

### 1. Download the OTEL Java Agent

```bash
curl -L -o opentelemetry-javaagent.jar \
  https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar
```

### 2. Configure Environment Variables

```bash
# Service identification
export OTEL_SERVICE_NAME=elasticsearch

# Exporter configuration (OTLP is the standard)
export OTEL_TRACES_EXPORTER=otlp
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317

# Optional: Resource attributes
export OTEL_RESOURCE_ATTRIBUTES="deployment.environment=production,service.version=1.0.0"
```

### 3. Start Elasticsearch with the Agent

```bash
ES_JAVA_OPTS="-javaagent:/path/to/opentelemetry-javaagent.jar" ./bin/elasticsearch
```

Or with quick-start.sh:

```bash
./scripts/quick-start.sh --otel
```

## Backend Options

### Jaeger (Open Source)

```bash
# Run Jaeger all-in-one
docker run -d --name jaeger \
  -p 16686:16686 \
  -p 4317:4317 \
  jaegertracing/all-in-one:latest

# Configure OTEL
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317

# View traces at http://localhost:16686
```

### Elastic APM (via OTLP)

```bash
export OTEL_EXPORTER_OTLP_ENDPOINT=https://your-apm-server:8200
export OTEL_EXPORTER_OTLP_HEADERS="Authorization=Bearer YOUR_SECRET_TOKEN"
```

### Grafana Tempo

```bash
export OTEL_EXPORTER_OTLP_ENDPOINT=http://tempo:4317
```

### Zipkin

```bash
export OTEL_TRACES_EXPORTER=zipkin
export OTEL_EXPORTER_ZIPKIN_ENDPOINT=http://localhost:9411/api/v2/spans
```

### Console (Development)

```bash
export OTEL_TRACES_EXPORTER=console
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
