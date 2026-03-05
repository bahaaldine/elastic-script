# Elasticsearch Streams

Streams provides centralized management for data streams, enabling field extraction, data retention configuration, and intelligent routing—all from a single interface.

## Overview

Streams simplifies common data stream operations:

- **Field Extraction**: Parse unstructured logs into structured fields
- **Data Retention**: Configure lifecycle policies per stream
- **Routing**: Partition data into child streams based on conditions
- **Schema Management**: Define and update field mappings

## Stream Types

### Classic Streams

Work with existing Elasticsearch data streams. Use when you have existing ingestion pipelines.

```sql
-- Classic streams inherit from existing data streams
CALL HTTP_GET('/_streams/logs-nginx');
```

### Wired Streams

Hierarchical streams with parent-child relationships. Data flows through a root endpoint and is routed to child streams.

```sql
-- Create wired stream hierarchy
CALL HTTP_PUT('/_streams/logs-app', '{
  "stream": {
    "ingest": {
      "processing": [],
      "routing": []
    }
  }
}');
```

**Benefits of Wired Streams:**
- Child streams inherit mappings, lifecycle, and processors
- Configuration changes cascade through hierarchy
- AI-assisted routing suggestions

---

## API Reference

### List Streams

```sql
CALL HTTP_GET('/_streams');
```

### Get Stream Details

```sql
CALL HTTP_GET('/_streams/{stream_name}');
```

### Create/Update Stream

```sql
CALL HTTP_PUT('/_streams/{stream_name}', '{...}');
```

### Delete Stream

```sql
CALL HTTP_DELETE('/_streams/{stream_name}');
```

---

## Field Extraction (Processing)

Add processors to extract fields from log messages.

### Grok Processor

For complex log patterns:

```sql
CALL HTTP_PUT('/_streams/logs-nginx/_processing', '{
  "processing": [
    {
      "grok": {
        "field": "message",
        "patterns": ["%{COMBINEDAPACHELOG}"]
      }
    }
  ]
}');
```

Common Grok patterns:
- `%{COMBINEDAPACHELOG}` - Apache/Nginx access logs
- `%{SYSLOGLINE}` - Syslog format
- `%{TIMESTAMP_ISO8601}` - ISO timestamps

### Dissect Processor

For simpler, delimiter-based extraction:

```sql
CALL HTTP_PUT('/_streams/logs-app/_processing', '{
  "processing": [
    {
      "dissect": {
        "field": "message",
        "pattern": "%{timestamp} [%{level}] %{logger}: %{msg}"
      }
    }
  ]
}');
```

### Multiple Processors

Chain processors for complex transformations:

```sql
CALL HTTP_PUT('/_streams/logs-app/_processing', '{
  "processing": [
    {
      "grok": {
        "field": "message",
        "patterns": ["%{TIMESTAMP_ISO8601:@timestamp} %{LOGLEVEL:level} %{GREEDYDATA:content}"]
      }
    },
    {
      "lowercase": {
        "field": "level"
      }
    },
    {
      "remove": {
        "field": "message",
        "ignore_missing": true
      }
    }
  ]
}');
```

---

## Partitioning (Routing)

Route documents to child streams based on field values.

### Basic Routing

```sql
CALL HTTP_PUT('/_streams/logs/_routing', '{
  "routing": [
    {
      "destination": "logs-errors",
      "if": {
        "field": "log.level",
        "operator": "eq",
        "value": "error"
      }
    }
  ]
}');
```

### Routing Operators

| Operator | Description | Example |
|----------|-------------|---------|
| `eq` | Equals | `"value": "error"` |
| `neq` | Not equals | `"value": "debug"` |
| `gt` | Greater than | `"value": 500` |
| `gte` | Greater or equal | `"value": 400` |
| `lt` | Less than | `"value": 200` |
| `lte` | Less or equal | `"value": 299` |
| `contains` | Contains substring | `"value": "timeout"` |
| `exists` | Field exists | (no value needed) |

### Multi-Level Routing

```sql
-- Route by severity, then by service
CALL HTTP_PUT('/_streams/logs/_routing', '{
  "routing": [
    {
      "destination": "logs-critical",
      "if": {"field": "log.level", "operator": "eq", "value": "critical"}
    },
    {
      "destination": "logs-errors",
      "if": {"field": "log.level", "operator": "eq", "value": "error"}
    }
  ]
}');

-- Further route errors by service
CALL HTTP_PUT('/_streams/logs-errors/_routing', '{
  "routing": [
    {
      "destination": "logs-errors-api",
      "if": {"field": "service.name", "operator": "contains", "value": "api"}
    },
    {
      "destination": "logs-errors-worker",
      "if": {"field": "service.name", "operator": "contains", "value": "worker"}
    }
  ]
}');
```

---

## Data Retention

Configure how long data is retained per stream.

### Set Retention

```sql
CALL HTTP_PUT('/_streams/logs-app/_lifecycle', '{
  "lifecycle": {
    "data_retention": "30d"
  }
}');
```

### Tiered Retention Strategy

```sql
-- Critical logs: keep longer
CALL HTTP_PUT('/_streams/logs-errors/_lifecycle', '{
  "lifecycle": {"data_retention": "90d"}
}');

-- Info logs: standard retention
CALL HTTP_PUT('/_streams/logs-info/_lifecycle', '{
  "lifecycle": {"data_retention": "30d"}
}');

-- Debug logs: short retention
CALL HTTP_PUT('/_streams/logs-debug/_lifecycle', '{
  "lifecycle": {"data_retention": "7d"}
}');
```

### Get Retention Stats

```sql
CALL HTTP_GET('/_streams/logs-app/_stats');
```

Returns storage size, document count, and retention status.

---

## Schema Management

Define field mappings for your streams.

### Update Schema

```sql
CALL HTTP_PUT('/_streams/logs-app/_schema', '{
  "schema": {
    "properties": {
      "response_time_ms": {"type": "float"},
      "status_code": {"type": "keyword"},
      "client_ip": {"type": "ip"},
      "request_path": {"type": "keyword"},
      "user_agent": {"type": "text"}
    }
  }
}');
```

### Schema Inheritance

Child streams inherit parent schema but can add fields:

```sql
-- Parent stream
CALL HTTP_PUT('/_streams/logs/_schema', '{
  "schema": {
    "properties": {
      "@timestamp": {"type": "date"},
      "message": {"type": "text"},
      "log.level": {"type": "keyword"}
    }
  }
}');

-- Child stream adds specific fields
CALL HTTP_PUT('/_streams/logs-api/_schema', '{
  "schema": {
    "properties": {
      "http.request.method": {"type": "keyword"},
      "http.response.status_code": {"type": "integer"}
    }
  }
}');
```

---

## Data Quality

Monitor ingestion health and handle failures.

### Check Data Quality

```sql
CALL HTTP_GET('/_streams/logs-app/_data_quality');
```

### Access Failure Store

Failed documents are stored for analysis:

```sql
CALL HTTP_GET('/_streams/logs-app/_failure_store');
```

### Common Failure Causes

| Issue | Cause | Solution |
|-------|-------|----------|
| Mapping conflict | Field type mismatch | Update schema or fix source |
| Parsing error | Grok pattern doesn't match | Add fallback pattern |
| Missing field | Required field absent | Use `ignore_missing: true` |

---

## Permissions

### View Streams

```
Index privileges: read, view_index_metadata, monitor
```

### Manage Streams

```
Cluster privileges: manage_index_templates, manage_ingest_pipelines, manage_pipeline, read_pipeline
Index privileges: read, write, create, manage, monitor, manage_data_stream_lifecycle, manage_failure_store
```

---

## Best Practices

1. **Use wired streams for new data** - Better organization and inheritance
2. **Route early** - Partition at ingestion for efficient queries
3. **Tiered retention** - Keep errors longer, debug shorter
4. **Extract fields at ingestion** - Don't parse at query time
5. **Monitor data quality** - Check failure store regularly

---

## See Also

- [Elastic Streams Documentation](https://www.elastic.co/docs/solutions/observability/streams/streams)
- [Streams API Reference](https://www.elastic.co/docs/api/doc/kibana/group/endpoint-streams)
- [Managing Data Lifecycle](./data-lifecycle.md)
