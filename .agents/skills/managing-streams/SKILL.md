---
name: managing-streams
description: >
  Manage Elasticsearch Streams for data routing, field extraction, and retention.
  Use when working with data streams, partitioning data into child streams,
  extracting fields from logs, configuring data lifecycle, or routing observability data.
---

# Streams Management

Streams provides centralized management for data streams including field extraction, retention, and routing.

## Stream Types

| Type | Description | Use Case |
|------|-------------|----------|
| **Classic** | Works with existing data streams | Existing ingestion pipelines |
| **Wired** | Hierarchical with parent-child routing | New observability data with partitioning |

## List Streams

```sql
-- List all streams
CALL HTTP_GET('/_streams');

-- Get specific stream details
CALL HTTP_GET('/_streams/logs-nginx');
```

## Create Wired Stream

```sql
-- Create a parent stream for routing
CALL HTTP_PUT('/_streams/logs-app', '{
  "stream": {
    "ingest": {
      "processing": [],
      "routing": []
    }
  }
}');
```

## Field Extraction (Processing)

Extract fields from unstructured log data:

```sql
-- Add grok processor to extract fields
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

-- Dissect processor for simpler patterns
CALL HTTP_PUT('/_streams/logs-app/_processing', '{
  "processing": [
    {
      "dissect": {
        "field": "message",
        "pattern": "%{timestamp} %{level} %{logger} - %{msg}"
      }
    }
  ]
}');
```

## Partitioning (Routing to Child Streams)

Route data to child streams based on conditions:

```sql
-- Route errors to dedicated stream
CALL HTTP_PUT('/_streams/logs-app/_routing', '{
  "routing": [
    {
      "destination": "logs-app-errors",
      "if": {
        "field": "log.level",
        "operator": "eq",
        "value": "error"
      }
    },
    {
      "destination": "logs-app-warnings",
      "if": {
        "field": "log.level", 
        "operator": "eq",
        "value": "warn"
      }
    }
  ]
}');
```

## Data Retention

Configure lifecycle settings:

```sql
-- Set retention policy
CALL HTTP_PUT('/_streams/logs-app/_lifecycle', '{
  "lifecycle": {
    "data_retention": "30d"
  }
}');

-- Get retention stats
CALL HTTP_GET('/_streams/logs-app/_stats');
```

## Schema Management

Manage field mappings:

```sql
-- Update schema/mappings
CALL HTTP_PUT('/_streams/logs-app/_schema', '{
  "schema": {
    "properties": {
      "response_time": { "type": "float" },
      "status_code": { "type": "keyword" },
      "client_ip": { "type": "ip" }
    }
  }
}');
```

## Data Quality

Check for failed/degraded documents:

```sql
-- Get data quality metrics
CALL HTTP_GET('/_streams/logs-app/_data_quality');

-- Check failure store
CALL HTTP_GET('/_streams/logs-app/_failure_store');
```

## Wired Stream Hierarchy

```
logs (root)
├── logs-app
│   ├── logs-app-errors      (routed by log.level=error)
│   └── logs-app-warnings    (routed by log.level=warn)
├── logs-nginx
│   └── logs-nginx-5xx       (routed by status>=500)
└── logs-system
```

Child streams inherit:
- Field mappings from parent
- Lifecycle settings
- Processors (can override)

## Common Patterns

### Log Level Routing

```sql
CREATE PROCEDURE setup_log_routing(IN stream_name STRING)
BEGIN
  DECLARE routing_config STRING;
  
  SET routing_config = '{
    "routing": [
      {"destination": "' || stream_name || '-errors", "if": {"field": "log.level", "operator": "eq", "value": "error"}},
      {"destination": "' || stream_name || '-warnings", "if": {"field": "log.level", "operator": "eq", "value": "warn"}},
      {"destination": "' || stream_name || '-info", "if": {"field": "log.level", "operator": "eq", "value": "info"}}
    ]
  }';
  
  CALL HTTP_PUT('/_streams/' || stream_name || '/_routing', routing_config);
END PROCEDURE;
```

### Service-Based Routing

```sql
-- Route by service.name
CALL HTTP_PUT('/_streams/logs/_routing', '{
  "routing": [
    {"destination": "logs-api", "if": {"field": "service.name", "operator": "contains", "value": "api"}},
    {"destination": "logs-worker", "if": {"field": "service.name", "operator": "contains", "value": "worker"}},
    {"destination": "logs-frontend", "if": {"field": "service.name", "operator": "contains", "value": "frontend"}}
  ]
}');
```

### Tiered Retention

```sql
-- Hot data: 7 days
CALL HTTP_PUT('/_streams/logs-app-errors/_lifecycle', '{"lifecycle": {"data_retention": "7d"}}');

-- Warm data: 30 days  
CALL HTTP_PUT('/_streams/logs-app-info/_lifecycle', '{"lifecycle": {"data_retention": "30d"}}');

-- Cold data: 90 days
CALL HTTP_PUT('/_streams/logs-app-debug/_lifecycle', '{"lifecycle": {"data_retention": "90d"}}');
```

## Required Permissions

| Action | Cluster Privileges | Index Privileges |
|--------|-------------------|------------------|
| View | - | `read`, `view_index_metadata`, `monitor` |
| Manage | `manage_index_templates`, `manage_ingest_pipelines` | `manage`, `manage_data_stream_lifecycle` |

## See Also

- [Elastic Streams Docs](https://www.elastic.co/docs/solutions/observability/streams/streams)
- [Streams API](https://www.elastic.co/docs/api/doc/kibana/group/endpoint-streams)
