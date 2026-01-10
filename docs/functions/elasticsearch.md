# Elasticsearch Functions

Native integration with Elasticsearch for querying and data manipulation.

## ESQL Integration

### ESQL_QUERY

Executes an ES|QL query and returns results.

```sql
DECLARE results CURSOR FOR ESQL_QUERY('
    FROM logs-* 
    | WHERE level = "ERROR"
    | STATS count = COUNT(*) BY service
    | SORT count DESC
    | LIMIT 10
');

FOR row IN results LOOP
    PRINT DOCUMENT_GET(row, 'service') || ': ' || DOCUMENT_GET(row, 'count');
END LOOP;
```

**Syntax:** `ESQL_QUERY(query_string)`

**Returns:** CURSOR - Iterable result set

!!! tip "ES|QL Reference"
    See the [Elasticsearch ES|QL documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/esql.html) for query syntax.

---

### Using Cursors

Results from `ESQL_QUERY` are returned as a cursor, which can be iterated:

```sql
-- Declare cursor
DECLARE logs CURSOR FOR ESQL_QUERY('FROM logs-* | LIMIT 100');

-- Iterate results
FOR log IN logs LOOP
    DECLARE message STRING = DOCUMENT_GET(log, 'message');
    PRINT message;
END LOOP;
```

---

## Document Operations

### ES_GET

Retrieves a document by ID.

```sql
DECLARE doc DOCUMENT = ES_GET('my-index', 'doc-id-123');
PRINT DOCUMENT_GET(doc, 'title');
```

**Syntax:** `ES_GET(index, document_id)`

---

### ES_INDEX

Indexes (creates or updates) a document.

```sql
DECLARE doc DOCUMENT = {
    "title": "My Document",
    "content": "Hello World",
    "timestamp": CURRENT_TIMESTAMP()
};

DECLARE result DOCUMENT = ES_INDEX('my-index', 'doc-id', doc);
PRINT 'Indexed: ' || DOCUMENT_GET(result, '_id');
```

**Syntax:** `ES_INDEX(index, document_id, document)`

---

### ES_DELETE

Deletes a document by ID.

```sql
ES_DELETE('my-index', 'doc-id-123');
```

**Syntax:** `ES_DELETE(index, document_id)`

---

### ES_UPDATE

Updates specific fields in a document.

```sql
DECLARE updates DOCUMENT = {
    "status": "processed",
    "processed_at": CURRENT_TIMESTAMP()
};

ES_UPDATE('my-index', 'doc-id', updates);
```

**Syntax:** `ES_UPDATE(index, document_id, partial_document)`

---

## Aggregations with ES|QL

ES|QL supports powerful aggregations:

```sql
-- Count by category
DECLARE stats CURSOR FOR ESQL_QUERY('
    FROM sales 
    | STATS 
        total_revenue = SUM(amount),
        avg_order = AVG(amount),
        order_count = COUNT(*)
      BY category
    | SORT total_revenue DESC
');

FOR row IN stats LOOP
    PRINT DOCUMENT_GET(row, 'category') || ': $' || DOCUMENT_GET(row, 'total_revenue');
END LOOP;
```

### Common Aggregation Functions

| Function | Description |
|----------|-------------|
| `COUNT(*)` | Count documents |
| `SUM(field)` | Sum numeric values |
| `AVG(field)` | Calculate average |
| `MIN(field)` | Find minimum value |
| `MAX(field)` | Find maximum value |
| `PERCENTILE(field, n)` | Calculate percentile |

---

## Example: Log Analysis Pipeline

```sql
CREATE PROCEDURE analyze_errors(time_range STRING)
BEGIN
    -- Get error counts by service
    DECLARE errors CURSOR FOR ESQL_QUERY('
        FROM logs-*
        | WHERE level = "ERROR" AND @timestamp > NOW() - ' || time_range || '
        | STATS error_count = COUNT(*) BY service.name
        | SORT error_count DESC
        | LIMIT 20
    ');
    
    PRINT '=== Error Summary ===';
    
    FOR row IN errors LOOP
        DECLARE service STRING = DOCUMENT_GET(row, 'service.name');
        DECLARE count NUMBER = DOCUMENT_GET(row, 'error_count');
        PRINT service || ': ' || count || ' errors';
    END LOOP;
END PROCEDURE;

-- Usage
CALL analyze_errors('1 hour');
```

---

## Example: Data Enrichment

```sql
CREATE PROCEDURE enrich_and_index(source_index STRING, target_index STRING)
BEGIN
    DECLARE docs CURSOR FOR ESQL_QUERY('FROM ' || source_index || ' | LIMIT 1000');
    DECLARE processed NUMBER = 0;
    
    FOR doc IN docs LOOP
        -- Enrich document
        DECLARE enriched DOCUMENT = DOCUMENT_MERGE(doc, {
            "processed_at": CURRENT_TIMESTAMP(),
            "processed_by": "elastic-script",
            "message_length": LENGTH(DOCUMENT_GET(doc, 'message'))
        });
        
        -- Index to new location
        ES_INDEX(target_index, NULL, enriched);
        SET processed = processed + 1;
    END LOOP;
    
    PRINT 'Processed ' || processed || ' documents';
    RETURN processed;
END PROCEDURE;
```

---

## Best Practices

!!! tip "Query Performance"
    - Use specific index patterns instead of wildcards when possible
    - Add `LIMIT` to prevent processing excessive data
    - Use `WHERE` clauses to filter early in the query

!!! warning "Rate Limiting"
    When processing large datasets, consider adding delays or batch processing
    to avoid overwhelming the cluster.
