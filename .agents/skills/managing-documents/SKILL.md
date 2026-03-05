---
name: managing-documents
description: >
  Perform document CRUD operations and index management. Use when indexing,
  retrieving, updating, or deleting documents, managing indices, or performing
  bulk operations and reindexing.
---

# Document & Index Operations

## Document Functions

| Function | Example |
|----------|---------|
| `ES_INDEX(index, doc, id?)` | `ES_INDEX('users', {'name': 'John'}, 'u1')` |
| `ES_GET(index, id)` | `ES_GET('users', 'u1')` |
| `ES_UPDATE(index, id, doc)` | `ES_UPDATE('users', 'u1', {'age': 30})` |
| `ES_DELETE(index, id)` | `ES_DELETE('users', 'u1')` |
| `ES_EXISTS(index, id)` | `ES_EXISTS('users', 'u1')` |
| `ES_BULK(operations)` | `ES_BULK([{index: {...}}, {delete: {...}}])` |
| `ES_MGET(index, ids)` | `ES_MGET('users', ['u1', 'u2'])` |

## Index Functions

| Function | Example |
|----------|---------|
| `ES_CREATE_INDEX(name, settings?)` | `ES_CREATE_INDEX('logs-2024', {...})` |
| `ES_DELETE_INDEX(name)` | `ES_DELETE_INDEX('old-logs')` |
| `ES_INDEX_EXISTS(name)` | `ES_INDEX_EXISTS('users')` |
| `ES_GET_MAPPING(index)` | `ES_GET_MAPPING('users')` |
| `ES_PUT_MAPPING(index, mappings)` | `ES_PUT_MAPPING('users', {...})` |
| `ES_REFRESH_INDEX(index)` | `ES_REFRESH_INDEX('users')` |

## Data Migration

| Function | Example |
|----------|---------|
| `ES_REINDEX(source, dest, query?)` | `ES_REINDEX('old', 'new')` |
| `ES_UPDATE_BY_QUERY(index, query, script)` | Batch updates |
| `ES_DELETE_BY_QUERY(index, query)` | Batch deletes |

## Usage

```sql
DECLARE result DOCUMENT;
SET result = ES_INDEX('users', {'name': 'John', 'email': 'john@example.com'}, 'u1');

DECLARE user DOCUMENT;
SET user = ES_GET('users', 'u1');
```

## Pre-built Skills

| Skill | Description |
|-------|-------------|
| `RUN SKILL index_document(...)` | Index with validation |
| `RUN SKILL create_index(...)` | Create with best practices |
| `RUN SKILL search_documents(...)` | Search with pagination |

## Best Practices

- Check index exists before creating
- Use bulk for >10 documents
- Refresh after bulk writes if immediate search needed
- Use `update_by_query` for batch updates

## Error Handling

```sql
TRY
    SET result = ES_INDEX('users', doc);
CATCH
    PRINT 'Failed: ' || @error['message'];
END TRY;
```
