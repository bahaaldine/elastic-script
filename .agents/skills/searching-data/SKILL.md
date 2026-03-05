---
name: searching-data
description: >
  Execute searches, aggregations, and queries. Use when finding documents,
  running analytics, counting records, exploring data, or performing semantic
  search with vectors.
---

# Search & Query

## ES|QL (Recommended)

```sql
-- Simple query
ESQL FROM logs-* | WHERE level = 'ERROR' | LIMIT 10;

-- Aggregations
ESQL FROM metrics-* 
| STATS avg_cpu = AVG(cpu_percent), max_mem = MAX(memory_percent) BY host.name
| SORT avg_cpu DESC;

-- Time filtering
ESQL FROM logs-* 
| WHERE @timestamp > NOW() - 1 HOUR 
| WHERE message LIKE '*error*'
| LIMIT 100;
```

## Search Functions

| Function | Example |
|----------|---------|
| `ES_SEARCH(index, query)` | `ES_SEARCH('logs', {'query': {...}})` |
| `ES_COUNT(index, query?)` | `ES_COUNT('logs', {'query': {...}})` |
| `ES_MSEARCH(requests)` | Multi-search |
| `ES_SCROLL(index, query, size)` | Large result sets |
| `ES_KNN_SEARCH(index, vector, k)` | Vector similarity |

## Aggregations

| Function | Example |
|----------|---------|
| `ES_TERMS_AGG(index, field, size)` | `ES_TERMS_AGG('logs', 'level', 10)` |
| `ES_STATS_AGG(index, field)` | `ES_STATS_AGG('metrics', 'cpu_percent')` |
| `ES_DATE_HISTOGRAM(index, field, interval)` | Time buckets |

## Common Patterns

### Full-Text Search

```sql
DECLARE results DOCUMENT;
SET results = ES_SEARCH('documents', {
    'query': {
        'multi_match': {
            'query': 'kubernetes deployment error',
            'fields': ['title', 'body', 'tags']
        }
    }
});
```

### Semantic Search

```sql
SET similar = ES_KNN_SEARCH('embeddings', 
    INFERENCE_EMBED('embedding-model', 'query text'),
    10
);
```

## Pre-built Skills

| Skill | Description |
|-------|-------------|
| `RUN SKILL search_documents(index, query, size)` | Paginated search |
| `RUN SKILL top_values(index, field, n)` | Top N values |
| `RUN SKILL count_by_field(index, field)` | Distribution |

## Tips

- Use ES|QL for analytics (readable, optimized)
- Always filter by time for logs/metrics
- Use LIMIT to avoid large results
- Prefer aggregations over fetching all docs
