---
name: search-query
description: Execute searches, aggregations, and queries against Elasticsearch. Use when the user wants to find documents, run analytics, count records, or explore data.
---

# Search & Query

This skill enables you to search and query Elasticsearch data using ES|QL, the Search API, and aggregations.

## When to Use

- User wants to **find or search** for documents
- User needs to **count** or **aggregate** data
- User asks for **analytics** or **statistics**
- User wants to **explore** or **browse** data
- User needs **real-time** data insights

## Query Approaches

### 1. ES|QL (Recommended for Analytics)

```sql
-- Simple query
ESQL FROM logs-* | WHERE level = 'ERROR' | LIMIT 10;

-- With aggregations
ESQL FROM metrics-* 
| STATS avg_cpu = AVG(cpu_percent), max_mem = MAX(memory_percent) BY host.name
| SORT avg_cpu DESC;

-- Time-based filtering
ESQL FROM logs-* 
| WHERE @timestamp > NOW() - 1 HOUR 
| WHERE message LIKE '*error*'
| LIMIT 100;
```

### 2. Search API Functions

| Function | Description | Example |
|----------|-------------|---------|
| `ES_SEARCH(index, query)` | Full-text search | `ES_SEARCH('logs', {'query': {...}})` |
| `ES_COUNT(index, query?)` | Count documents | `ES_COUNT('logs', {'query': {...}})` |
| `ES_MSEARCH(requests)` | Multi-search | `ES_MSEARCH([{index: 'a', query: {...}}])` |
| `ES_SCROLL(index, query, size)` | Scroll large results | `ES_SCROLL('logs', {...}, 1000)` |
| `ES_KNN_SEARCH(index, vector, k)` | Vector similarity | `ES_KNN_SEARCH('docs', [0.1, 0.2], 10)` |

### 3. Aggregations

| Function | Description | Example |
|----------|-------------|---------|
| `ES_TERMS_AGG(index, field, size)` | Top values | `ES_TERMS_AGG('logs', 'level', 10)` |
| `ES_STATS_AGG(index, field)` | Numeric stats | `ES_STATS_AGG('metrics', 'cpu_percent')` |
| `ES_DATE_HISTOGRAM(index, field, interval)` | Time buckets | `ES_DATE_HISTOGRAM('logs', '@timestamp', '1h')` |

## Common Patterns

### Find Recent Errors

```sql
DECLARE errors ARRAY;
ESQL FROM logs-* 
| WHERE @timestamp > NOW() - 1 HOUR 
| WHERE level = 'ERROR'
| SORT @timestamp DESC
| LIMIT 50
INTO errors;

FOR error IN errors LOOP
    PRINT error['message'];
END LOOP;
```

### Top Values Analysis

```sql
DECLARE top_hosts DOCUMENT;
SET top_hosts = ES_TERMS_AGG('metrics-*', 'host.name', 20);

-- Or with ES|QL
ESQL FROM metrics-* | STATS count = COUNT(*) BY host.name | SORT count DESC | LIMIT 20;
```

### Full-Text Search

```sql
DECLARE results DOCUMENT;
SET results = ES_SEARCH('documents', {
    'query': {
        'multi_match': {
            'query': 'kubernetes deployment error',
            'fields': ['title', 'body', 'tags']
        }
    },
    'highlight': {
        'fields': {'body': {}}
    }
});
```

### Semantic/Vector Search

```sql
DECLARE similar_docs DOCUMENT;
SET similar_docs = ES_KNN_SEARCH('embeddings', 
    INFERENCE_EMBED('my-embedding-model', 'search query text'),
    10
);
```

## Pre-built Skills (Moltler)

| Skill | Description |
|-------|-------------|
| `RUN SKILL search_documents(index, query, size)` | Paginated search |
| `RUN SKILL top_values(index, field, n)` | Top N values |
| `RUN SKILL count_by_field(index, field)` | Count distribution |
| `RUN SKILL time_series(index, interval)` | Time-based aggregation |

## Tips for Better Queries

1. **Use ES|QL for analytics** - it's more readable and optimized
2. **Add time filters** - always filter by time for log/metrics data
3. **Use LIMIT** - avoid fetching too many documents
4. **Prefer aggregations** over fetching all docs and counting in code
5. **Use KNN search** for semantic similarity with embeddings
