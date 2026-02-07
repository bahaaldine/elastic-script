# Document Functions

## Quick Reference

| Function | Description |
|----------|-------------|
| [`DOCUMENT_CONTAINS`](#document-contains) | Returns true if the document contains the given key. |
| [`DOCUMENT_GET`](#document-get) | Returns the value for a given key in the document. |
| [`DOCUMENT_KEYS`](#document-keys) | Returns the list of keys in the given document. |
| [`DOCUMENT_MERGE`](#document-merge) | Returns a new document that is the result of merging two doc... |
| [`DOCUMENT_REMOVE`](#document-remove) | Returns a new document with the specified key removed. |
| [`DOCUMENT_VALUES`](#document-values) | Returns the list of values in the given document. |

---

## Function Details

### DOCUMENT_CONTAINS

```
DOCUMENT_CONTAINS(doc DOCUMENT, key STRING) -> BOOLEAN
```

Returns true if the document contains the given key.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `doc` | DOCUMENT | The input document. |
| `key` | STRING | The key to check in the document. |

**Returns:** `BOOLEAN`
 - True if the document contains the key, false otherwise.


**Examples:**

```sql
DOCUMENT_CONTAINS({\
:1,\
```

---

### DOCUMENT_GET

```
DOCUMENT_GET(doc DOCUMENT, key STRING) -> ANY
```

Returns the value for a given key in the document.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `doc` | DOCUMENT | The input document. |
| `key` | STRING | The key to retrieve from the document. |

**Returns:** `ANY`
 - The value associated with the given key.


**Examples:**

```sql
DOCUMENT_GET({\
:1,\
```

---

### DOCUMENT_KEYS

```
DOCUMENT_KEYS(doc DOCUMENT) -> ARRAY OF STRING
```

Returns the list of keys in the given document.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `doc` | DOCUMENT | The input document. |

**Returns:** `ARRAY OF STRING`
 - An array of keys from the document.


**Examples:**

```sql
DOCUMENT_KEYS({\
:1,\
```

---

### DOCUMENT_MERGE

```
DOCUMENT_MERGE(doc1 DOCUMENT, doc2 DOCUMENT) -> DOCUMENT
```

Returns a new document that is the result of merging two documents.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `doc1` | DOCUMENT | The first document. |
| `doc2` | DOCUMENT | The second document. |

**Returns:** `DOCUMENT`
 - A new document containing all keys and values from both documents.


**Examples:**

```sql
DOCUMENT_MERGE({\
```

---

### DOCUMENT_REMOVE

```
DOCUMENT_REMOVE(doc DOCUMENT, key STRING) -> DOCUMENT
```

Returns a new document with the specified key removed.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `doc` | DOCUMENT | The input document. |
| `key` | STRING | The key to remove from the document. |

**Returns:** `DOCUMENT`
 - A new document without the specified key.


**Examples:**

```sql
DOCUMENT_REMOVE({\
:1,\
```

---

### DOCUMENT_VALUES

```
DOCUMENT_VALUES(doc DOCUMENT) -> ARRAY
```

Returns the list of values in the given document.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `doc` | DOCUMENT | The input document. |

**Returns:** `ARRAY`
 - An array of values from the document.


**Examples:**

```sql
DOCUMENT_VALUES({\
:1,\
```

---
