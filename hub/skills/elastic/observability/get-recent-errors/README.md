# get_recent_errors

Get recent error logs with full details.

## Usage

```sql
-- Get recent errors
RUN SKILL get_recent_errors();

-- Get errors from specific service
RUN SKILL get_recent_errors(service => 'api-gateway');

-- Get more errors
RUN SKILL get_recent_errors(limit => 50);
```

## Parameters

| Name | Type | Default | Description |
|------|------|---------|-------------|
| `index_pattern` | STRING | logs-* | Index pattern to search |
| `limit` | INT | 20 | Maximum results |
| `service` | STRING | NULL | Filter by service |

## Returns

Array of error log entries with full details.
