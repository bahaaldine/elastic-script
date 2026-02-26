---
name: elasticsearch-ops
description: Perform Elasticsearch operations including document CRUD, index management, and bulk operations. Use when the user needs to index, update, delete, or retrieve documents, or manage indices.
---

# Elasticsearch Operations

This skill enables you to perform core Elasticsearch operations through the Moltler/elastic-script system.

## When to Use

- User wants to **index or create** documents
- User needs to **retrieve, update, or delete** documents
- User wants to perform **bulk operations** on multiple documents
- User needs to **manage indices** (create, delete, configure)
- User asks about **reindexing** or **data migration**

## Available Functions

### Document Operations

| Function | Description | Example |
|----------|-------------|---------|
| `ES_INDEX(index, document, id?)` | Index a document | `ES_INDEX('users', {'name': 'John'}, 'user-1')` |
| `ES_GET(index, id)` | Get document by ID | `ES_GET('users', 'user-1')` |
| `ES_DELETE(index, id)` | Delete a document | `ES_DELETE('users', 'user-1')` |
| `ES_UPDATE(index, id, doc)` | Partial update | `ES_UPDATE('users', 'user-1', {'age': 30})` |
| `ES_EXISTS(index, id)` | Check if document exists | `ES_EXISTS('users', 'user-1')` |
| `ES_BULK(operations)` | Bulk operations | `ES_BULK([{index: {...}}, {delete: {...}}])` |
| `ES_MGET(index, ids)` | Get multiple documents | `ES_MGET('users', ['id1', 'id2'])` |

### Index Operations

| Function | Description | Example |
|----------|-------------|---------|
| `ES_CREATE_INDEX(name, settings?)` | Create new index | `ES_CREATE_INDEX('logs-2024', {...})` |
| `ES_DELETE_INDEX(name)` | Delete an index | `ES_DELETE_INDEX('old-logs')` |
| `ES_INDEX_EXISTS(name)` | Check if index exists | `ES_INDEX_EXISTS('users')` |
| `ES_GET_MAPPING(index)` | Get index mappings | `ES_GET_MAPPING('users')` |
| `ES_PUT_MAPPING(index, mappings)` | Update mappings | `ES_PUT_MAPPING('users', {...})` |
| `ES_REFRESH_INDEX(index)` | Refresh index | `ES_REFRESH_INDEX('users')` |

### Data Migration

| Function | Description | Example |
|----------|-------------|---------|
| `ES_REINDEX(source, dest, query?)` | Reindex documents | `ES_REINDEX('old-index', 'new-index')` |
| `ES_UPDATE_BY_QUERY(index, query, script)` | Update by query | `ES_UPDATE_BY_QUERY('logs', {...}, {...})` |
| `ES_DELETE_BY_QUERY(index, query)` | Delete by query | `ES_DELETE_BY_QUERY('logs', {...})` |

## Usage via elastic-script

```sql
-- Index a document
DECLARE result DOCUMENT;
SET result = ES_INDEX('users', {'name': 'John', 'email': 'john@example.com'}, 'user-1');

-- Get a document
DECLARE user DOCUMENT;
SET user = ES_GET('users', 'user-1');
PRINT user['name'];

-- Update a document
SET result = ES_UPDATE('users', 'user-1', {'status': 'active'});

-- Bulk operations
DECLARE ops ARRAY;
SET ops = [
    {'index': {'_index': 'users', '_id': 'u1'}, 'doc': {'name': 'Alice'}},
    {'index': {'_index': 'users', '_id': 'u2'}, 'doc': {'name': 'Bob'}}
];
SET result = ES_BULK(ops);
```

## Pre-built Skills (Moltler)

These Moltler skills combine multiple operations:

| Skill | Description |
|-------|-------------|
| `RUN SKILL index_document(...)` | Index with validation |
| `RUN SKILL create_index(...)` | Create index with best practices |
| `RUN SKILL search_documents(...)` | Search with pagination |
| `RUN SKILL delete_document(...)` | Delete with confirmation |

## Best Practices

1. **Always check if index exists** before creating
2. **Use bulk operations** for more than 10 documents
3. **Refresh index** after bulk writes if immediate search is needed
4. **Use update_by_query** for batch updates instead of looping

## Error Handling

```sql
TRY
    SET result = ES_INDEX('users', doc);
CATCH
    PRINT 'Indexing failed: ' || @error['message'];
END TRY;
```
