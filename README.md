# elastic-script Language Documentation

## Introduction

**elastic-script** (`escript`) is a procedural extension to ESQL (Elasticsearch Query Language), providing structured programming constructs and advanced workflow capabilities for agents and complex Elasticsearch automation. It enables users to define reusable procedures, control execution flow, manage variables, handle errors, and compose modular logic that integrates seamlessly with ESQL queries and Elasticsearch operations.

elastic-script is designed for:
- **AI Agents**: Automate multi-step Elasticsearch workflows with LLM integration.
- **Observability**: Analyze logs, metrics, and traces with semantic search and AI.
- **Advanced Users**: Orchestrate data processing, error handling, and conditional logic.
- **Developers**: Contribute custom functions and extend the language for new use cases.

---

## Table of Contents
- [Quick Start](#quick-start)
- [Language Constructs](#language-constructs)
  - [DECLARE](#declare)
  - [CURSOR](#cursor)
  - [SET](#set)
  - [IF / ELSEIF / ELSE](#if--elseif--else)
  - [FOR Loop](#for-loop)
  - [WHILE Loop](#while-loop)
  - [BREAK](#break)
  - [PRINT](#print)
  - [TRY / CATCH / FINALLY](#try--catch--finally)
  - [THROW](#throw)
  - [PROCEDURE / RETURN](#procedure--return)
- [Built-in Functions](#built-in-functions)
  - [String Functions](#string-functions)
  - [Number Functions](#number-functions)
  - [Array Functions](#array-functions)
  - [Date Functions](#date-functions)
  - [Document Functions](#document-functions)
  - [Elasticsearch Functions](#elasticsearch-functions)
  - [ESQL Functions](#esql-functions)
- [AI & ML Functions](#ai--ml-functions)
  - [Inference API Functions](#inference-api-functions)
  - [OpenAI Functions](#openai-functions)
- [Third-Party Integrations](#third-party-integrations)
  - [Slack Functions](#slack-functions)
  - [S3 Functions](#s3-functions)
- [Types & Type System](#types--type-system)
- [REST API](#rest-api)
- [Examples](#examples)

---

## Quick Start

### Create a Simple Procedure

```bash
curl -X PUT "http://localhost:9200/_query/escript/procedure/hello_world" \
  -H "Content-Type: application/json" \
  -d '{
    "procedure": "CREATE PROCEDURE hello_world() BEGIN PRINT '\''Hello, World!'\''; RETURN '\''success'\''; END PROCEDURE"
  }'
```

### Execute the Procedure

```bash
curl -X POST "http://localhost:9200/_escript" \
  -H "Content-Type: application/json" \
  -d '{"procedure": "hello_world"}'
```

---

## Language Constructs

### DECLARE

**Purpose:** Declare variables of a specific type, optionally with an initial value.

**Syntax:**
```sql
DECLARE var_name TYPE;
DECLARE var_name TYPE = value;
DECLARE var1 TYPE1, var2 TYPE2 = value2;
```

**Supported Types:** `INT`, `FLOAT`, `NUMBER`, `STRING`, `DATE`, `BOOLEAN`, `ARRAY`, `ARRAY OF <TYPE>`, `DOCUMENT`

**Examples:**
```sql
DECLARE count INT = 0;
DECLARE message STRING = 'hello';
DECLARE items ARRAY = ["a", "b", "c"];
DECLARE numbers ARRAY OF NUMBER = [1, 2, 3];
DECLARE doc DOCUMENT = {};
```

---

### CURSOR

**Purpose:** Declare a cursor for lazy execution of ESQL queries. Cursors are executed when iterated in a FOR loop.

**Syntax:**
```sql
DECLARE cursor_name CURSOR FOR <esql_query>;
```

**Examples:**
```sql
-- Declare a cursor for error logs
DECLARE error_logs CURSOR FOR 
    FROM application-logs 
    | WHERE log.level == "ERROR" 
    | LIMIT 100;

-- Iterate over the cursor results
FOR log_entry IN error_logs LOOP
    PRINT log_entry['message'];
END LOOP
```

**Key Features:**
- Lazy execution: Query runs only when cursor is used in FOR loop
- Supports all ESQL query syntax including pipes
- Results are available as DOCUMENT objects in the loop variable

---

### SET

**Purpose:** Assign a value or expression result to a previously declared variable.

**Syntax:**
```sql
SET var_name = expression;
```

**Examples:**
```sql
SET count = count + 1;
SET message = 'Hello ' || name;  -- String concatenation
SET items = ARRAY_APPEND(items, 'new_item');
```

---

### IF / ELSEIF / ELSE

**Purpose:** Conditional logic; execute code blocks based on conditions.

**Syntax:**
```sql
IF <condition> THEN
    <statements>
[ELSEIF <condition> THEN
    <statements>]
[ELSE
    <statements>]
END IF
```

**Examples:**
```sql
IF count > 10 THEN
    PRINT 'Many items';
ELSEIF count > 0 THEN
    PRINT 'Some items';
ELSE
    PRINT 'No items';
END IF
```

---

### FOR Loop

**Purpose:** Iterate over a numeric range, array, or cursor.

**Syntax:**
```sql
-- Range loop (inclusive)
FOR var IN start..end LOOP
    <statements>
END LOOP

-- Array/Cursor loop
FOR item IN array_or_cursor LOOP
    <statements>
END LOOP
```

**Examples:**
```sql
-- Range loop
FOR i IN 1..5 LOOP
    SET sum = sum + i;
END LOOP

-- Array loop
DECLARE fruits ARRAY = ["apple", "banana", "cherry"];
FOR fruit IN fruits LOOP
    PRINT fruit;
END LOOP

-- Cursor loop
DECLARE logs CURSOR FOR FROM application-logs | LIMIT 10;
FOR log IN logs LOOP
    PRINT log['message'];
END LOOP
```

---

### WHILE Loop

**Purpose:** Repeat a block as long as a condition is true.

**Syntax:**
```sql
WHILE <condition> LOOP
    <statements>
END LOOP
```

**Example:**
```sql
DECLARE i INT = 0;
WHILE i < 10 LOOP
    SET i = i + 1;
END LOOP
```

---

### BREAK

**Purpose:** Exit the nearest enclosing loop immediately.

```sql
FOR i IN 1..100 LOOP
    IF i > 5 THEN 
        BREAK; 
    END IF
END LOOP
```

---

### PRINT

**Purpose:** Output a message to the execution log.

**Syntax:**
```sql
PRINT expression;
```

**Examples:**
```sql
PRINT 'Processing started';
PRINT 'Count: ' || count;
PRINT 'Results: ' || LENGTH(results) || ' items';
```

---

### TRY / CATCH / FINALLY

**Purpose:** Structured error handling for procedure blocks.

**Syntax:**
```sql
TRY
    <statements>
[CATCH
    <statements>]
[FINALLY
    <statements>]
END TRY
```

**Example:**
```sql
TRY
    DECLARE result STRING = risky_operation();
CATCH
    PRINT 'Operation failed, using default';
    SET result = 'default';
FINALLY
    PRINT 'Cleanup complete';
END TRY
```

---

### THROW

**Purpose:** Raise a custom exception with a message.

```sql
IF amount < 0 THEN
    THROW 'Amount cannot be negative';
END IF
```

---

### PROCEDURE / RETURN

**Purpose:** Define reusable procedures with parameters and return values.

**Syntax:**
```sql
CREATE PROCEDURE name(param1 TYPE, param2 TYPE, ...)
BEGIN
    <statements>
    RETURN expression;
END PROCEDURE
```

**Parameter Modes:**
- `IN` (default): Input parameter
- `OUT`: Output parameter
- `INOUT`: Both input and output

**Examples:**
```sql
CREATE PROCEDURE greet(IN name STRING)
BEGIN
    PRINT 'Hello, ' || name || '!';
    RETURN 'Greeted ' || name;
END PROCEDURE

CREATE PROCEDURE calculate_sum(IN numbers ARRAY OF NUMBER)
BEGIN
    DECLARE total NUMBER = 0;
    FOR n IN numbers LOOP
        SET total = total + n;
    END LOOP
    RETURN total;
END PROCEDURE
```

---

## Built-in Functions

### String Functions

| Function | Description | Example |
|----------|-------------|---------|
| `LENGTH(s)` | Length of string or array | `LENGTH('hello')` → `5` |
| `UPPER(s)` | Convert to uppercase | `UPPER('hello')` → `'HELLO'` |
| `LOWER(s)` | Convert to lowercase | `LOWER('HELLO')` → `'hello'` |
| `TRIM(s)` | Remove leading/trailing whitespace | `TRIM('  hi  ')` → `'hi'` |
| `LTRIM(s)` | Remove leading whitespace | `LTRIM('  hi')` → `'hi'` |
| `RTRIM(s)` | Remove trailing whitespace | `RTRIM('hi  ')` → `'hi'` |
| `SUBSTR(s, start, len)` | Extract substring | `SUBSTR('hello', 1, 3)` → `'ell'` |
| `REPLACE(s, old, new)` | Replace occurrences | `REPLACE('hello', 'l', 'L')` → `'heLLo'` |
| `CONCAT(s1, s2, ...)` | Concatenate strings | `CONCAT('a', 'b', 'c')` → `'abc'` |
| `INSTR(s, sub)` | Find position of substring | `INSTR('hello', 'l')` → `2` |
| `LPAD(s, len, pad)` | Left-pad string | `LPAD('5', 3, '0')` → `'005'` |
| `RPAD(s, len, pad)` | Right-pad string | `RPAD('5', 3, '0')` → `'500'` |
| `INITCAP(s)` | Capitalize first letter of each word | `INITCAP('hello world')` → `'Hello World'` |
| `REVERSE(s)` | Reverse string | `REVERSE('hello')` → `'olleh'` |
| `ENV(name, default?)` | Get environment variable | `ENV('OPENAI_API_KEY')` |

**String Concatenation Operator:** Use `||` to concatenate strings:
```sql
SET greeting = 'Hello, ' || name || '!';
```

---

### Number Functions

| Function | Description | Example |
|----------|-------------|---------|
| `ABS(n)` | Absolute value | `ABS(-5)` → `5` |
| `CEIL(n)` | Round up | `CEIL(4.2)` → `5` |
| `FLOOR(n)` | Round down | `FLOOR(4.8)` → `4` |
| `ROUND(n, d?)` | Round to d decimal places | `ROUND(3.456, 2)` → `3.46` |
| `TRUNC(n, d?)` | Truncate to d decimal places | `TRUNC(3.456, 2)` → `3.45` |
| `MOD(a, b)` | Modulo (remainder) | `MOD(10, 3)` → `1` |
| `POWER(base, exp)` | Exponentiation | `POWER(2, 3)` → `8` |
| `SQRT(n)` | Square root | `SQRT(16)` → `4` |
| `SIGN(n)` | Sign of number (-1, 0, 1) | `SIGN(-5)` → `-1` |
| `GREATEST(a, b, ...)` | Maximum value | `GREATEST(1, 5, 3)` → `5` |
| `LEAST(a, b, ...)` | Minimum value | `LEAST(1, 5, 3)` → `1` |
| `RANDOM()` | Random number 0-1 | `RANDOM()` → `0.742...` |

---

### Array Functions

| Function | Description | Example |
|----------|-------------|---------|
| `ARRAY_APPEND(arr, elem)` | Add element to end | `ARRAY_APPEND([1,2], 3)` → `[1,2,3]` |
| `ARRAY_PREPEND(arr, elem)` | Add element to start | `ARRAY_PREPEND([2,3], 1)` → `[1,2,3]` |
| `ARRAY_REMOVE(arr, elem)` | Remove element | `ARRAY_REMOVE([1,2,3], 2)` → `[1,3]` |
| `ARRAY_CONTAINS(arr, elem)` | Check if contains | `ARRAY_CONTAINS([1,2,3], 2)` → `true` |
| `ARRAY_LENGTH(arr)` | Get length | `ARRAY_LENGTH([1,2,3])` → `3` |
| `ARRAY_GET(arr, idx)` | Get element at index | `ARRAY_GET([1,2,3], 1)` → `2` |
| `ARRAY_SLICE(arr, start, end)` | Get subarray | `ARRAY_SLICE([1,2,3,4], 1, 3)` → `[2,3]` |
| `ARRAY_REVERSE(arr)` | Reverse array | `ARRAY_REVERSE([1,2,3])` → `[3,2,1]` |
| `ARRAY_SORT(arr)` | Sort array | `ARRAY_SORT([3,1,2])` → `[1,2,3]` |
| `ARRAY_DISTINCT(arr)` | Remove duplicates | `ARRAY_DISTINCT([1,1,2])` → `[1,2]` |
| `LENGTH(arr)` | Get array length | `LENGTH([1,2,3])` → `3` |

---

### Date Functions

| Function | Description | Example |
|----------|-------------|---------|
| `NOW()` | Current timestamp | `NOW()` |
| `CURRENT_DATE()` | Current date | `CURRENT_DATE()` |
| `DATE_ADD(date, interval)` | Add to date | `DATE_ADD(NOW(), '1 day')` |
| `DATE_SUB(date, interval)` | Subtract from date | `DATE_SUB(NOW(), '1 hour')` |
| `DATE_DIFF(date1, date2)` | Difference in days | `DATE_DIFF(date1, date2)` |
| `DATE_FORMAT(date, fmt)` | Format date | `DATE_FORMAT(NOW(), 'yyyy-MM-dd')` |
| `DATE_PARSE(str, fmt)` | Parse string to date | `DATE_PARSE('2024-01-01', 'yyyy-MM-dd')` |

---

### Document Functions

| Function | Description | Example |
|----------|-------------|---------|
| `DOC_GET(doc, key)` | Get field value | `DOC_GET(doc, 'name')` |
| `DOC_SET(doc, key, value)` | Set field value | `DOC_SET(doc, 'count', 5)` |
| `DOC_HAS(doc, key)` | Check if field exists | `DOC_HAS(doc, 'email')` |
| `DOC_REMOVE(doc, key)` | Remove field | `DOC_REMOVE(doc, 'temp')` |
| `DOC_KEYS(doc)` | Get all keys | `DOC_KEYS(doc)` |
| `DOC_VALUES(doc)` | Get all values | `DOC_VALUES(doc)` |
| `DOC_MERGE(doc1, doc2)` | Merge documents | `DOC_MERGE(doc1, doc2)` |
| `TO_JSON(doc)` | Convert to JSON string | `TO_JSON(doc)` |
| `FROM_JSON(str)` | Parse JSON to document | `FROM_JSON('{"a":1}')` |

**Field Access:** Use bracket notation to access document fields:
```sql
SET name = doc['name'];
SET nested = doc['user']['email'];
```

---

### Elasticsearch Functions

| Function | Description | Example |
|----------|-------------|---------|
| `INDEX_DOCUMENT(index, id, doc)` | Index a document | `INDEX_DOCUMENT('my-index', 'doc-1', doc)` |
| `GET_DOCUMENT(index, id)` | Get document by ID | `GET_DOCUMENT('my-index', 'doc-1')` |
| `UPDATE_DOCUMENT(index, id, doc)` | Update a document | `UPDATE_DOCUMENT('my-index', 'doc-1', updates)` |
| `DELETE_DOCUMENT(index, id)` | Delete a document | `DELETE_DOCUMENT('my-index', 'doc-1')` |
| `INDEX_BULK(index, docs)` | Bulk index documents | `INDEX_BULK('my-index', docs_array)` |
| `REFRESH_INDEX(index)` | Refresh index | `REFRESH_INDEX('my-index')` |

---

### ESQL Functions

| Function | Description | Example |
|----------|-------------|---------|
| `ESQL_QUERY(query)` | Execute ESQL query | `ESQL_QUERY('FROM logs \| LIMIT 10')` |

---

## AI & ML Functions

### Inference API Functions

These functions use Elasticsearch's built-in Inference API and configured inference endpoints.

| Function | Description |
|----------|-------------|
| `INFERENCE(endpoint_id, input)` | General inference |
| `INFERENCE_EMBED(endpoint_id, input)` | Generate embeddings |
| `INFERENCE_RERANK(endpoint_id, query, documents)` | Rerank documents by relevance |
| `INFERENCE_CHAT(endpoint_id, messages_json)` | Chat completion |
| `INFERENCE_CREATE_ENDPOINT(id, task_type, config)` | Create inference endpoint |
| `INFERENCE_DELETE_ENDPOINT(id)` | Delete inference endpoint |
| `INFERENCE_LIST_ENDPOINTS()` | List all endpoints |
| `INFERENCE_GET_ENDPOINT(id)` | Get endpoint details |

**Built-in Elasticsearch Endpoints:**
- `.multilingual-e5-small-elasticsearch` - Text embeddings (384 dimensions)
- `.elser-2-elasticsearch` - Sparse embeddings
- `.rerank-v1-elasticsearch` - Document reranking

**Examples:**
```sql
-- Generate embeddings
DECLARE embedding ARRAY = INFERENCE_EMBED('.multilingual-e5-small-elasticsearch', 'search query');

-- Rerank documents by relevance
DECLARE docs ARRAY = ["Doc 1 content", "Doc 2 content", "Doc 3 content"];
DECLARE ranked ARRAY = INFERENCE_RERANK('.rerank-v1-elasticsearch', 'my query', docs);

-- List available endpoints
DECLARE endpoints ARRAY = INFERENCE_LIST_ENDPOINTS();
```

---

### OpenAI Functions

These functions call OpenAI's API directly. Requires `OPENAI_API_KEY` environment variable.

| Function | Description |
|----------|-------------|
| `LLM_COMPLETE(prompt, model?, api_key?)` | Text completion |
| `LLM_CHAT(messages_json, model?, api_key?)` | Chat completion |
| `LLM_EMBED(text, model?, api_key?)` | Generate embeddings |
| `LLM_SUMMARIZE(text, max_length?, api_key?)` | Summarize text |
| `LLM_CLASSIFY(text, categories, api_key?)` | Classify text |
| `LLM_EXTRACT(text, fields, api_key?)` | Extract structured data |

**Examples:**
```sql
-- Simple completion
DECLARE response STRING = LLM_COMPLETE('Explain Elasticsearch in one sentence');

-- Summarize error logs
DECLARE summary STRING = LLM_SUMMARIZE(error_messages, 100);

-- Classify log severity
DECLARE category STRING = LLM_CLASSIFY(log_message, 'critical,warning,info');
```

---

## Third-Party Integrations

### Slack Functions

Send notifications to Slack. Requires `SLACK_TOKEN` or `SLACK_WEBHOOK_URL` environment variable.

| Function | Description |
|----------|-------------|
| `SLACK_WEBHOOK(message)` | Send via webhook |
| `SLACK_SEND(channel, message)` | Send to channel |
| `SLACK_SEND_BLOCKS(channel, blocks_json)` | Send rich blocks |
| `SLACK_REACT(channel, timestamp, emoji)` | Add reaction |
| `SLACK_LIST_CHANNELS()` | List channels |

**Example:**
```sql
SLACK_SEND('#alerts', 'High error rate detected: ' || error_count || ' errors in last hour');
```

---

### S3 Functions

Interact with AWS S3. Requires `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, and `AWS_REGION` environment variables.

| Function | Description |
|----------|-------------|
| `S3_GET(bucket, key)` | Get object content |
| `S3_PUT(bucket, key, content)` | Put object |
| `S3_DELETE(bucket, key)` | Delete object |
| `S3_LIST(bucket, prefix?)` | List objects |
| `S3_EXISTS(bucket, key)` | Check if exists |
| `S3_CREATE_BUCKET(bucket, region?)` | Create bucket |

**Example:**
```sql
-- Archive daily report to S3
DECLARE report STRING = generate_report();
S3_PUT('my-reports-bucket', 'reports/daily-' || CURRENT_DATE() || '.txt', report);
```

---

## Types & Type System

### Primitive Types
- `INT` - Integer numbers
- `FLOAT` / `NUMBER` - Floating point numbers
- `STRING` - Text values
- `BOOLEAN` - true/false
- `DATE` - Date/time values

### Complex Types
- `ARRAY` - Ordered collection (untyped)
- `ARRAY OF <TYPE>` - Typed array
- `DOCUMENT` - Key-value structure (like JSON object)
- `CURSOR` - ESQL query reference for lazy execution

### Type Coercion
- Automatic coercion between compatible numeric types
- `||` operator converts operands to strings for concatenation

---

## REST API

### Create/Update Procedure
```bash
PUT /_query/escript/procedure/{procedure_id}
Content-Type: application/json

{
  "procedure": "CREATE PROCEDURE name() BEGIN ... END PROCEDURE"
}
```

### Execute Procedure
```bash
POST /_escript
Content-Type: application/json

{
  "procedure": "procedure_name",
  "arguments": {"arg1": "value1"}
}
```

### Get Procedure
```bash
GET /_query/escript/procedure/{procedure_id}
```

### Delete Procedure
```bash
DELETE /_query/escript/procedure/{procedure_id}
```

### List Procedures
```bash
GET /_query/escript/procedures
```

---

## Examples

### Log Analysis with Reranking

```sql
CREATE PROCEDURE smart_log_search(IN query STRING)
BEGIN
    -- Fetch error logs using CURSOR
    DECLARE error_logs CURSOR FOR 
        FROM application-logs 
        | WHERE log.level == "ERROR" 
        | LIMIT 20;
    
    -- Collect error messages
    DECLARE messages ARRAY = [];
    FOR log IN error_logs LOOP
        SET messages = ARRAY_APPEND(messages, log['message']);
    END LOOP
    
    -- Rerank by relevance to query
    IF LENGTH(messages) > 0 THEN
        DECLARE ranked ARRAY = INFERENCE_RERANK('.rerank-v1-elasticsearch', query, messages);
        PRINT 'Top matches for: ' || query;
        RETURN ranked;
    ELSE
        RETURN 'No error logs found';
    END IF
END PROCEDURE
```

### AI-Powered Error Analysis

```sql
CREATE PROCEDURE analyze_errors()
BEGIN
    DECLARE error_logs CURSOR FOR 
        FROM application-logs 
        | WHERE log.level == "ERROR" 
        | LIMIT 10;
    
    DECLARE error_summary STRING = '';
    DECLARE count INT = 0;
    
    FOR log IN error_logs LOOP
        SET error_summary = error_summary || log['message'] || '\n';
        SET count = count + 1;
    END LOOP
    
    IF count > 0 THEN
        DECLARE prompt STRING = 'Summarize these ' || count || ' errors:\n' || error_summary;
        DECLARE analysis STRING = LLM_COMPLETE(prompt);
        PRINT 'Analysis: ' || analysis;
        RETURN analysis;
    ELSE
        RETURN 'No errors found';
    END IF
END PROCEDURE
```

### Archive Reports to S3

```sql
CREATE PROCEDURE archive_daily_report()
BEGIN
    DECLARE error_logs CURSOR FOR 
        FROM application-logs 
        | WHERE log.level == "ERROR" 
        | LIMIT 100;
    
    DECLARE report STRING = 'Daily Error Report\n==================\n';
    DECLARE count INT = 0;
    
    FOR log IN error_logs LOOP
        SET report = report || log['service.name'] || ': ' || log['message'] || '\n';
        SET count = count + 1;
    END LOOP
    
    -- Add AI summary
    DECLARE summary STRING = LLM_COMPLETE('Summarize in 3 bullets:\n' || report);
    SET report = report || '\n\nAI Summary:\n' || summary;
    
    -- Archive to S3
    DECLARE key STRING = 'reports/errors/daily-' || DATE_FORMAT(NOW(), 'yyyy-MM-dd') || '.txt';
    S3_PUT('my-logs-bucket', key, report);
    
    PRINT 'Archived ' || count || ' errors to S3: ' || key;
    RETURN key;
END PROCEDURE
```

### Semantic Search with Embeddings

```sql
CREATE PROCEDURE semantic_search(IN search_query STRING)
BEGIN
    -- Generate embedding for the query
    DECLARE query_embedding ARRAY = INFERENCE_EMBED('.multilingual-e5-small-elasticsearch', search_query);
    
    PRINT 'Query: ' || search_query;
    PRINT 'Embedding dimensions: ' || LENGTH(query_embedding);
    
    -- Return embedding for use in kNN search
    RETURN query_embedding;
END PROCEDURE
```

---

## Configuration

### Environment Variables

| Variable | Description |
|----------|-------------|
| `OPENAI_API_KEY` | OpenAI API key for LLM functions |
| `SLACK_TOKEN` | Slack Bot Token |
| `SLACK_WEBHOOK_URL` | Slack Webhook URL |
| `AWS_ACCESS_KEY_ID` | AWS access key |
| `AWS_SECRET_ACCESS_KEY` | AWS secret key |
| `AWS_REGION` | AWS region (e.g., us-east-1) |

Access environment variables in scripts:
```sql
DECLARE api_key STRING = ENV('OPENAI_API_KEY');
DECLARE region STRING = ENV('AWS_REGION', 'us-east-1');  -- with default
```

---

## See Also
- [ESQL Documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/esql.html)
- [Elasticsearch Inference API](https://www.elastic.co/guide/en/elasticsearch/reference/current/inference-apis.html)
- [Elasticsearch ML](https://www.elastic.co/guide/en/elasticsearch/reference/current/ml-apis.html)
