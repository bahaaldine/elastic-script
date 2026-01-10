# Document Functions

Functions for working with document (object/map) data types.

## Overview

Documents in elastic-script are key-value maps similar to JSON objects. They're commonly used when working with Elasticsearch documents.

## Function Reference

### DOCUMENT_GET

Gets a value from a document by key.

```sql
DECLARE doc DOCUMENT = {"name": "John", "age": 30};
DECLARE name STRING = DOCUMENT_GET(doc, 'name');
-- Returns: 'John'
```

**Syntax:** `DOCUMENT_GET(document, key)`

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| document | DOCUMENT | Input document |
| key | STRING | Key to retrieve |

**Returns:** Value at the specified key, or NULL if not found

---

### DOCUMENT_KEYS

Returns all keys in a document.

```sql
DECLARE doc DOCUMENT = {"name": "John", "age": 30, "city": "NYC"};
DECLARE keys ARRAY = DOCUMENT_KEYS(doc);
-- Returns: ['name', 'age', 'city']
```

**Syntax:** `DOCUMENT_KEYS(document)`

**Returns:** ARRAY - Array of key names

---

### DOCUMENT_VALUES

Returns all values in a document.

```sql
DECLARE doc DOCUMENT = {"a": 1, "b": 2, "c": 3};
DECLARE values ARRAY = DOCUMENT_VALUES(doc);
-- Returns: [1, 2, 3]
```

**Syntax:** `DOCUMENT_VALUES(document)`

**Returns:** ARRAY - Array of values

---

### DOCUMENT_CONTAINS

Checks if a document contains a key.

```sql
DECLARE doc DOCUMENT = {"name": "John"};
DECLARE has_name BOOLEAN = DOCUMENT_CONTAINS(doc, 'name');
-- Returns: true
DECLARE has_email BOOLEAN = DOCUMENT_CONTAINS(doc, 'email');
-- Returns: false
```

**Syntax:** `DOCUMENT_CONTAINS(document, key)`

**Returns:** BOOLEAN

---

### DOCUMENT_MERGE

Merges two documents into one.

```sql
DECLARE doc1 DOCUMENT = {"a": 1, "b": 2};
DECLARE doc2 DOCUMENT = {"c": 3, "d": 4};
DECLARE merged DOCUMENT = DOCUMENT_MERGE(doc1, doc2);
-- Returns: {"a": 1, "b": 2, "c": 3, "d": 4}
```

**Syntax:** `DOCUMENT_MERGE(document1, document2)`

!!! note "Conflict Resolution"
    When both documents contain the same key, the value from `document2` takes precedence.

---

### DOCUMENT_REMOVE

Removes a key from a document.

```sql
DECLARE doc DOCUMENT = {"a": 1, "b": 2, "c": 3};
DECLARE result DOCUMENT = DOCUMENT_REMOVE(doc, 'b');
-- Returns: {"a": 1, "c": 3}
```

**Syntax:** `DOCUMENT_REMOVE(document, key)`

---

## Working with Elasticsearch Documents

When querying Elasticsearch, results come back as documents:

```sql
CREATE PROCEDURE analyze_log(log_id STRING)
BEGIN
    -- Get a document from Elasticsearch
    DECLARE log DOCUMENT = ES_GET('logs-sample', log_id);
    
    -- Access fields
    DECLARE level STRING = DOCUMENT_GET(log, 'level');
    DECLARE message STRING = DOCUMENT_GET(log, 'message');
    DECLARE timestamp DATE = DOCUMENT_GET(log, '@timestamp');
    
    -- Check for specific fields
    IF DOCUMENT_CONTAINS(log, 'error') THEN
        PRINT 'Error details: ' || DOCUMENT_GET(log, 'error');
    END IF;
    
    RETURN log;
END PROCEDURE;
```

## Nested Document Access

For nested documents, chain `DOCUMENT_GET` calls:

```sql
DECLARE doc DOCUMENT = {
    "user": {
        "name": "John",
        "address": {
            "city": "NYC"
        }
    }
};

DECLARE user DOCUMENT = DOCUMENT_GET(doc, 'user');
DECLARE name STRING = DOCUMENT_GET(user, 'name');
DECLARE address DOCUMENT = DOCUMENT_GET(user, 'address');
DECLARE city STRING = DOCUMENT_GET(address, 'city');
```

## Example: Transform Document

```sql
CREATE PROCEDURE transform_log(log DOCUMENT)
BEGIN
    -- Create new document with transformed fields
    DECLARE result DOCUMENT = {};
    
    -- Copy and transform fields
    SET result = DOCUMENT_MERGE(result, {
        "timestamp": DOCUMENT_GET(log, '@timestamp'),
        "severity": UPPER(DOCUMENT_GET(log, 'level')),
        "service": DOCUMENT_GET(log, 'service.name'),
        "message": DOCUMENT_GET(log, 'message')
    });
    
    RETURN result;
END PROCEDURE;
```

## Example: Iterate Document Keys

```sql
CREATE PROCEDURE print_document(doc DOCUMENT)
BEGIN
    DECLARE keys ARRAY = DOCUMENT_KEYS(doc);
    
    FOR i IN 0..(ARRAY_LENGTH(keys)-1) LOOP
        DECLARE key STRING = keys[i];
        DECLARE value STRING = DOCUMENT_GET(doc, key);
        PRINT key || ': ' || value;
    END LOOP;
END PROCEDURE;
```
