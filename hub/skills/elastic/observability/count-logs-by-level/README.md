# count_logs_by_level

Count logs grouped by severity level.

## Usage

```sql
-- Count logs in default index
RUN SKILL count_logs_by_level();

-- Count logs in specific index pattern
RUN SKILL count_logs_by_level(index_pattern => 'logs-production-*');
```

## Parameters

| Name | Type | Default | Description |
|------|------|---------|-------------|
| `index_pattern` | STRING | logs-* | Index pattern to search |
| `time_range` | STRING | 24h | Time range to analyze |

## Returns

Array of objects with level and count.
