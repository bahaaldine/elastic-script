# Datasource Functions

## Quick Reference

| Function | Description |
|----------|-------------|
| [`ESQL_QUERY`](#esql-query) | Executes a raw ESQL query string and returns a list of norma... |
| [`GET_DOCUMENT`](#get-document) | Fetches a document by ID from the specified index. Returns t... |
| [`INDEX_BULK`](#index-bulk) | Bulk indexes a list of documents into the specified index. R... |
| [`INDEX_DOCUMENT`](#index-document) | Indexes a single document into the specified index.  |
| [`INFERENCE_CREATE_ENDPOINT`](#inference-create-endpoint) | Creates a new inference endpoint with the specified configur... |
| [`INFERENCE_DELETE_ENDPOINT`](#inference-delete-endpoint) | Deletes an existing inference endpoint. |
| [`INFERENCE_GET_ENDPOINT`](#inference-get-endpoint) | Gets the configuration details of a specific inference endpo... |
| [`INFERENCE_LIST_ENDPOINTS`](#inference-list-endpoints) | Lists all configured inference endpoints. Optionally filter ... |
| [`REFRESH_INDEX`](#refresh-index) | Refreshes the specified index so that newly indexed document... |
| [`S3_CREATE_BUCKET`](#s3-create-bucket) | Creates a new S3 bucket. Returns true if created successfull... |
| [`S3_DELETE`](#s3-delete) | Deletes an object from S3. |
| [`S3_EXISTS`](#s3-exists) | Checks if an object exists in S3. |
| [`S3_GET`](#s3-get) | Reads an object from S3 and returns its contents as a string... |
| [`S3_LIST`](#s3-list) | Lists objects in an S3 bucket with an optional prefix filter... |
| [`S3_PUT`](#s3-put) | Writes content to an S3 object. Creates the object if it doe... |
| [`UPDATE_DOCUMENT`](#update-document) | Updates an existing document by ID in the specified index wi... |

---

## Function Details

### ESQL_QUERY

```
ESQL_QUERY(query STRING) -> ARRAY OF DOCUMENT
```

Executes a raw ESQL query string and returns a list of normalized documents. 

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `query` | STRING | The raw ESQL query string to execute.  |

**Returns:** `ARRAY OF DOCUMENT`
 - A list of documents representing the result rows, with each document mapping column names to values.


**Examples:**

```sql
ESQL_QUERY('FROM games | LIMIT 10')
ESQL_QUERY('FROM purchases | WHERE user_id == :userId')
```

---

### GET_DOCUMENT

```
GET_DOCUMENT(indexName STRING, id STRING) -> DOCUMENT
```

Fetches a document by ID from the specified index. Returns the document if it exists, or null otherwise.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `indexName` | STRING | The name of the index |
| `id` | STRING | The ID of the document to retrieve |

**Returns:** `DOCUMENT`
 - The retrieved document if found, otherwise null


**Examples:**

```sql
GET_DOCUMENT('index', 'id123')
```

---

### INDEX_BULK

```
INDEX_BULK(indexName STRING, documents ARRAY OF DOCUMENT) -> DOCUMENT
```

Bulk indexes a list of documents into the specified index. Returns number of items and the operation duration.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `indexName` | STRING | The name of the index to write to |
| `documents` | ARRAY OF DOCUMENT | List of documents to index |

**Returns:** `DOCUMENT`
 - Summary including the number of indexed items and operation duration


**Examples:**

```sql
INDEX_BULK('my-index', [{\
```

---

### INDEX_DOCUMENT

```
INDEX_DOCUMENT(indexName STRING, document DOCUMENT) -> DOCUMENT
```

Indexes a single document into the specified index. 

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `indexName` | STRING | The name of the index to write to |
| `document` | DOCUMENT | The document to index |

**Returns:** `DOCUMENT`
 - Metadata of the indexed document including ID, index, and result status


**Examples:**

```sql
INDEX_DOCUMENT('my-index', {\
:\
```

---

### INFERENCE_CREATE_ENDPOINT

```
INFERENCE_CREATE_ENDPOINT(endpoint_id STRING, task_type STRING, config_json STRING) -> BOOLEAN
```

Creates a new inference endpoint with the specified configuration. 

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `endpoint_id` | STRING | Unique identifier for the endpoint |
| `task_type` | STRING | Task type: completion, chat_completion, text_embedding, rerank, sparse_embedding |
| `config_json` | STRING | JSON configuration including service and service_settings |

**Returns:** `BOOLEAN`
 - true if endpoint was created successfully


**Examples:**

```sql
INFERENCE_CREATE_ENDPOINT('my-openai', 'completion', '{\
:\
,\
:{\
:\
,\
:\
```

---

### INFERENCE_DELETE_ENDPOINT

```
INFERENCE_DELETE_ENDPOINT(endpoint_id STRING, force BOOLEAN) -> BOOLEAN
```

Deletes an existing inference endpoint.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `endpoint_id` | STRING | The endpoint ID to delete |
| `force` | BOOLEAN | Force delete even if endpoint is in use. Optional, defaults to false |

**Returns:** `BOOLEAN`
 - true if endpoint was deleted successfully


**Examples:**

```sql
INFERENCE_DELETE_ENDPOINT('my-old-endpoint')
INFERENCE_DELETE_ENDPOINT('unused-endpoint', true)
```

---

### INFERENCE_GET_ENDPOINT

```
INFERENCE_GET_ENDPOINT(endpoint_id STRING) -> DOCUMENT
```

Gets the configuration details of a specific inference endpoint.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `endpoint_id` | STRING | The endpoint ID to retrieve |

**Returns:** `DOCUMENT`
 - The endpoint configuration as a document


**Examples:**

```sql
INFERENCE_GET_ENDPOINT('my-openai-endpoint')
```

---

### INFERENCE_LIST_ENDPOINTS

```
INFERENCE_LIST_ENDPOINTS(task_type STRING) -> ARRAY
```

Lists all configured inference endpoints. Optionally filter by task type.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `task_type` | STRING | Filter by task type (optional). Use '_all' for all types. |

**Returns:** `ARRAY`
 - Array of endpoint configurations


**Examples:**

```sql
INFERENCE_LIST_ENDPOINTS()
INFERENCE_LIST_ENDPOINTS('completion')
INFERENCE_LIST_ENDPOINTS('text_embedding')
```

---

### REFRESH_INDEX

```
REFRESH_INDEX(indexName STRING) -> DOCUMENT
```

Refreshes the specified index so that newly indexed documents become visible for search.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `indexName` | STRING | The name of the index to refresh |

**Returns:** `DOCUMENT`
 - Status of the refresh operation including total, successful, and failed shards


**Examples:**

```sql
REFRESH_INDEX('my-index')
```

---

### S3_CREATE_BUCKET

```
S3_CREATE_BUCKET(bucket STRING, access_key STRING, secret_key STRING, region STRING) -> BOOLEAN
```

Creates a new S3 bucket. Returns true if created successfully or if bucket already exists.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `bucket` | STRING | The S3 bucket name to create |
| `access_key` | STRING | AWS access key ID (optional) |
| `secret_key` | STRING | AWS secret access key (optional) |
| `region` | STRING | AWS region (optional) |

**Returns:** `BOOLEAN`
 - true if the bucket was created or already exists


**Examples:**

```sql
S3_CREATE_BUCKET('my-new-bucket')
S3_CREATE_BUCKET('my-bucket', 'AKIAIOSFODNN7EXAMPLE', 'wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY', 'us-west-2')
```

---

### S3_DELETE

```
S3_DELETE(bucket STRING, key STRING, access_key STRING, secret_key STRING, region STRING) -> BOOLEAN
```

Deletes an object from S3.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `bucket` | STRING | The S3 bucket name |
| `key` | STRING | The object key to delete |
| `access_key` | STRING | AWS access key ID (optional) |
| `secret_key` | STRING | AWS secret access key (optional) |
| `region` | STRING | AWS region (optional) |

**Returns:** `BOOLEAN`
 - true if the object was deleted successfully


**Examples:**

```sql
S3_DELETE('my-bucket', 'temp/file.txt')
```

---

### S3_EXISTS

```
S3_EXISTS(bucket STRING, key STRING, access_key STRING, secret_key STRING, region STRING) -> BOOLEAN
```

Checks if an object exists in S3.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `bucket` | STRING | The S3 bucket name |
| `key` | STRING | The object key to check |
| `access_key` | STRING | AWS access key ID (optional) |
| `secret_key` | STRING | AWS secret access key (optional) |
| `region` | STRING | AWS region (optional) |

**Returns:** `BOOLEAN`
 - true if the object exists, false otherwise


**Examples:**

```sql
S3_EXISTS('my-bucket', 'data/config.json')
```

---

### S3_GET

```
S3_GET(bucket STRING, key STRING, access_key STRING, secret_key STRING, region STRING) -> STRING
```

Reads an object from S3 and returns its contents as a string. 

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `bucket` | STRING | The S3 bucket name |
| `key` | STRING | The object key (path within the bucket) |
| `access_key` | STRING | AWS access key ID (optional) |
| `secret_key` | STRING | AWS secret access key (optional) |
| `region` | STRING | AWS region (optional, default: us-east-1) |

**Returns:** `STRING`
 - The object contents as a string


**Examples:**

```sql
S3_GET('my-bucket', 'data/file.txt')
S3_GET('my-bucket', 'config.json', access_key, secret_key, 'eu-west-1')
```

---

### S3_LIST

```
S3_LIST(bucket STRING, prefix STRING, max_keys NUMBER, access_key STRING, secret_key STRING, region STRING) -> ARRAY OF STRING
```

Lists objects in an S3 bucket with an optional prefix filter.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `bucket` | STRING | The S3 bucket name |
| `prefix` | STRING | Prefix to filter objects (optional) |
| `max_keys` | NUMBER | Maximum number of objects to return (default: 1000) |
| `access_key` | STRING | AWS access key ID (optional) |
| `secret_key` | STRING | AWS secret access key (optional) |
| `region` | STRING | AWS region (optional) |

**Returns:** `ARRAY OF STRING`
 - List of object keys matching the prefix


**Examples:**

```sql
S3_LIST('my-bucket')
S3_LIST('my-bucket', 'logs/2024/')
S3_LIST('my-bucket', 'data/', 100)
```

---

### S3_PUT

```
S3_PUT(bucket STRING, key STRING, content STRING, content_type STRING, access_key STRING, secret_key STRING, region STRING) -> BOOLEAN
```

Writes content to an S3 object. Creates the object if it doesn't exist, 

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `bucket` | STRING | The S3 bucket name |
| `key` | STRING | The object key (path within the bucket) |
| `content` | STRING | The content to write |
| `content_type` | STRING | Content type (optional, default: text/plain) |
| `access_key` | STRING | AWS access key ID (optional) |
| `secret_key` | STRING | AWS secret access key (optional) |
| `region` | STRING | AWS region (optional) |

**Returns:** `BOOLEAN`
 - true if the object was written successfully


**Examples:**

```sql
S3_PUT('my-bucket', 'output/result.json', json_content, 'application/json')
S3_PUT('my-bucket', 'logs/export.txt', log_data)
```

---

### UPDATE_DOCUMENT

```
UPDATE_DOCUMENT(indexName STRING, id STRING, document DOCUMENT) -> DOCUMENT
```

Updates an existing document by ID in the specified index with partial fields from the given document.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `indexName` | STRING | The name of the index |
| `id` | STRING | The ID of the document to update |
| `document` | DOCUMENT | Partial document fields to update |

**Returns:** `DOCUMENT`
 - Metadata of the updated document including ID, index, and result status


**Examples:**

```sql
UPDATE_DOCUMENT('index', 'id123', {\
:\
```

---
