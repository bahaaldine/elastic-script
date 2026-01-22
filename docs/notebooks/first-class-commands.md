# First-Class Commands

This guide demonstrates **first-class commands** in elastic-script - a modern syntax for core Elasticsearch operations.

## Overview

Instead of using function calls like `INDEX_DOCUMENT()`, you can use cleaner command syntax:

| Old Syntax | New Command Syntax |
|------------|-------------------|
| `INDEX_DOCUMENT('idx', doc)` | `INDEX doc INTO 'idx';` |
| `DELETE_DOCUMENT('idx', id)` | `DELETE FROM 'idx' WHERE id = '123';` |
| `SEARCH_INDEX('idx', query)` | `SEARCH 'idx' QUERY {...};` |
| `REFRESH_INDEX('idx')` | `REFRESH 'idx';` |
| `CREATE_INDEX('idx', mappings)` | `CREATE INDEX 'idx' WITH MAPPINGS {...};` |

## INDEX Command

Index a document into Elasticsearch:

```sql
-- Basic indexing
DECLARE doc DOCUMENT = {'name': 'Product', 'price': 29.99};
INDEX doc INTO 'products';

-- Inline document
INDEX {'status': 'active', 'timestamp': NOW()} INTO 'events';
```

## DELETE Command

Delete a document by ID:

```sql
-- Delete specific document
DELETE FROM 'products' WHERE id = 'abc123';

-- Delete with variable
DECLARE doc_id STRING = 'xyz789';
DELETE FROM 'logs' WHERE id = doc_id;
```

## SEARCH Command

Execute a search query:

```sql
-- Search and store results
SEARCH 'products' QUERY {'term': {'category': 'electronics'}} INTO results;

-- Process results
FOR item IN results LOOP
    PRINT('Found: ' || item['name']);
END LOOP;
```

## REFRESH Command

Refresh an index to make documents searchable:

```sql
-- After bulk indexing
FOR item IN items LOOP
    INDEX item INTO 'my-index';
END LOOP;

REFRESH 'my-index';
```

## CREATE INDEX Command

Create an index with mappings and settings:

```sql
-- With mappings
CREATE INDEX 'logs-2024' WITH MAPPINGS {
    'properties': {
        'message': {'type': 'text'},
        'level': {'type': 'keyword'},
        'timestamp': {'type': 'date'}
    }
};

-- With settings and mappings
CREATE INDEX 'high-perf' WITH 
    SETTINGS {'number_of_shards': 3}
    MAPPINGS {'properties': {'data': {'type': 'object'}}};
```

## Complete Example

```sql
CREATE PROCEDURE product_catalog()
BEGIN
    -- Create index
    CREATE INDEX 'products-demo' WITH MAPPINGS {
        'properties': {
            'name': {'type': 'text'},
            'price': {'type': 'float'},
            'category': {'type': 'keyword'}
        }
    };
    
    -- Index products
    INDEX {'name': 'Laptop', 'price': 999.99, 'category': 'electronics'} INTO 'products-demo';
    INDEX {'name': 'Mouse', 'price': 29.99, 'category': 'electronics'} INTO 'products-demo';
    INDEX {'name': 'Desk', 'price': 199.99, 'category': 'furniture'} INTO 'products-demo';
    
    REFRESH 'products-demo';
    
    -- Search electronics
    SEARCH 'products-demo' QUERY {'term': {'category': 'electronics'}} INTO electronics;
    
    PRINT('Found ' || ARRAY_LENGTH(electronics) || ' electronics');
    
    FOR product IN electronics LOOP
        PRINT(product['name'] || ': $' || product['price']);
    END LOOP;
END PROCEDURE

CALL product_catalog();
```

## Try It

Download the [10-first-class-commands.ipynb](https://github.com/bahaaldine/elastic-script/blob/main/notebooks/10-first-class-commands.ipynb) notebook.
