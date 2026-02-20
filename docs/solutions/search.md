# Search Skills

**Query smarter.** Pre-built skills for documents, aggregations, and semantic search.

---

## Quick Start

```bash
# 1. Install search skills
./hub/moltler-cli.sh install --all --category search
./hub/moltler-cli.sh install --all --category enterprise-search

# 2. Run your first skill
curl -u elastic-admin:elastic-password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "RUN SKILL search_documents(query => '\''pricing'\'')"}'
```

---

## Common Scenarios

### "Find documents matching my query"

```sql
-- Simple search
RUN SKILL search_documents(query => 'pricing plans');

-- With filters
RUN SKILL search_documents(
  query => 'pricing',
  index_pattern => 'products-*',
  limit => 50
);
```

### "What are the top values in this field?"

```sql
RUN SKILL top_values(
  index_pattern => 'orders-*',
  field => 'product_category',
  limit => 10
);
```

Returns aggregation of most common values.

### "Search by meaning, not keywords"

```sql
-- Semantic/vector search
RUN SKILL semantic_search(
  query => 'how do I reset my password',
  index => 'support-articles'
);
```

Finds documents that match the meaning, even if words differ.

### "Fuzzy match for typos"

```sql
RUN SKILL fuzzy_search(
  query => 'Jonh Smth',  -- typos
  index_pattern => 'customers-*',
  field => 'name'
);
```

### "Show me a time histogram"

```sql
RUN SKILL date_histogram(
  index_pattern => 'orders-*',
  interval => 'day',
  time_range => '30d'
);
```

---

## Available Skills

### Document Operations

| Skill | Description |
|-------|-------------|
| `search_documents` | Full-text search |
| `get_document` | Get document by ID |
| `create_document` | Index a new document |
| `update_document` | Update existing document |
| `delete_document` | Remove a document |
| `bulk_index` | Bulk index documents |

### Aggregations

| Skill | Description |
|-------|-------------|
| `top_values` | Terms aggregation |
| `date_histogram` | Time-based bucketing |
| `percentiles` | Percentile statistics |
| `multi_field_search` | Search across fields |

### Semantic & AI Search

| Skill | Description |
|-------|-------------|
| `semantic_search` | Vector/embedding search |
| `fuzzy_search` | Typo-tolerant search |

### Index Management

| Skill | Description |
|-------|-------------|
| `create_index` | Create new index |
| `delete_index` | Remove index |
| `get_mapping` | Get index mapping |
| `set_alias` | Manage aliases |
| `reindex` | Reindex data |

### Data Pipelines

| Skill | Description |
|-------|-------------|
| `list_ingest_pipelines` | List pipelines |
| `test_ingest_pipeline` | Test pipeline |
| `list_transforms` | List transforms |
| `get_transform_status` | Transform health |

### Enterprise Search

| Skill | Description |
|-------|-------------|
| `list_search_apps` | Search applications |
| `get_search_analytics` | Query analytics |
| `get_top_queries` | Popular searches |
| `get_no_results_queries` | Searches with no hits |

---

## Real-World Workflow

**Scenario: Build a product search feature**

```sql
-- Step 1: What fields do I have?
RUN SKILL get_mapping(index => 'products');
-- Result: name, description, category, price, rating

-- Step 2: What are the top categories?
RUN SKILL top_values(
  index_pattern => 'products',
  field => 'category',
  limit => 20
);
-- Result: Electronics, Clothing, Home, Sports...

-- Step 3: Test a search query
RUN SKILL search_documents(
  query => 'wireless headphones',
  index_pattern => 'products',
  limit => 5
);
-- Result: Matching products with scores

-- Step 4: Try semantic search for better results
RUN SKILL semantic_search(
  query => 'noise cancelling earbuds for running',
  index => 'products'
);
-- Result: Products matching intent, not just keywords

-- Step 5: Check search analytics
RUN SKILL get_no_results_queries(limit => 20);
-- Result: Queries users tried but found nothing
```

---

## Data Pipeline Example

**Scenario: Set up data ingestion**

```sql
-- Step 1: Check existing pipelines
RUN SKILL list_ingest_pipelines();

-- Step 2: Test a pipeline
RUN SKILL test_ingest_pipeline(
  pipeline => 'logs-processor',
  document => '{"message": "ERROR: Connection failed"}'
);
-- Result: Processed document with extracted fields

-- Step 3: Bulk index data
RUN SKILL bulk_index(
  index => 'products',
  documents => [
    {"name": "Widget A", "price": 29.99},
    {"name": "Widget B", "price": 39.99}
  ]
);

-- Step 4: Verify the data
RUN SKILL search_documents(
  query => 'Widget',
  index_pattern => 'products'
);
```

---

## Search Analytics

Understand how users search:

```sql
-- What are people searching for?
RUN SKILL get_top_queries(
  app => 'ecommerce-search',
  limit => 50
);

-- What searches return nothing?
RUN SKILL get_no_results_queries(
  app => 'ecommerce-search',
  limit => 20
);

-- Overall search health
RUN SKILL get_search_analytics(app => 'ecommerce-search');
```

---

## Combine with AI

Let an AI assistant query your data:

```
User: "Find all products under $50 in the electronics category"

AI: [Runs search_documents with filters]

AI: "Found 47 products. Top results:
     1. Wireless Mouse - $24.99
     2. USB Hub - $19.99
     3. Phone Stand - $12.99
     
     Want me to filter further or show analytics?"
```

See [MCP Integration](../mcp.md) to connect your AI assistant.

---

## Build Custom Skills

Create search skills for your use case:

```sql
CREATE SKILL product_search
  VERSION '1.0.0'
  DESCRIPTION 'Search products with category and price filters'
  (
    query STRING DESCRIPTION 'Search query',
    category STRING DEFAULT NULL DESCRIPTION 'Filter by category',
    max_price NUMBER DEFAULT NULL DESCRIPTION 'Maximum price',
    limit INT DEFAULT 20 DESCRIPTION 'Results limit'
  )
  RETURNS ARRAY
BEGIN
  DECLARE base_query STRING;
  
  SET base_query = 'FROM products | WHERE MATCH(name, "' || query || '") OR MATCH(description, "' || query || '")';
  
  IF category IS NOT NULL THEN
    SET base_query = base_query || ' AND category == "' || category || '"';
  END IF;
  
  IF max_price IS NOT NULL THEN
    SET base_query = base_query || ' AND price <= ' || max_price;
  END IF;
  
  SET base_query = base_query || ' | SORT _score DESC | LIMIT ' || limit;
  
  RETURN ESQL_QUERY(base_query);
END SKILL;
```

---

## Next Steps

- [Browse all search skills](../moltlerhub/index.md)
- [Create custom skills](../skills/creating-skills.md)
- [Connect AI assistant](../mcp.md)
