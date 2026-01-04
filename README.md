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
- [Runbook Integrations](#runbook-integrations)
  - [Kubernetes Functions](#kubernetes-functions)
  - [PagerDuty Functions](#pagerduty-functions)
  - [Terraform Cloud Functions](#terraform-cloud-functions)
  - [CI/CD Functions](#cicd-functions)
  - [AWS Functions](#aws-functions)
  - [Generic HTTP Functions](#generic-http-functions)
- [Introspection Functions](#introspection-functions)
- [Types & Type System](#types--type-system)
- [REST API](#rest-api)
- [Scenarios & Examples](#scenarios--examples)
  - [🟢 Beginner: Hello World](#-beginner-hello-world)
  - [🟢 Beginner: Simple Query Loop](#-beginner-simple-query-loop)
  - [🟢 Beginner: Variables and Conditionals](#-beginner-variables-and-conditionals)
  - [🟡 Intermediate: Parameterized Queries](#-intermediate-parameterized-queries)
  - [🟡 Intermediate: Cross-Index Correlation](#-intermediate-cross-index-correlation)
  - [🟡 Intermediate: Error Handling](#-intermediate-error-handling)
  - [🟠 Advanced: AI-Powered Semantic Search](#-advanced-ai-powered-semantic-search)
  - [🟠 Advanced: LLM-Powered Error Analysis](#-advanced-llm-powered-error-analysis)
  - [🔴 Expert: Full Incident Investigation](#-expert-full-incident-investigation)
  - [🔴 Expert: Archive Reports to S3](#-expert-archive-reports-to-s3)
  - [🔴 Expert: Slack Alerting with AI Summary](#-expert-slack-alerting-with-ai-summary)
  - [🔴 Expert: Scheduled Health Check](#-expert-scheduled-health-check)
- [Configuration](#configuration)
- [Roadmap: INTENT Layer & Encoded Expertise](#roadmap-intent-layer--encoded-expertise)

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
- **Variable substitution**: Use `:variableName` to reference elastic-script variables in ESQL queries

**Variable Substitution:**

Use the `:variableName` syntax to dynamically inject elastic-script variable values into cursor queries:

```sql
-- Parameterized cursor using :variable syntax
CREATE PROCEDURE investigate_service(target_service STRING)
BEGIN
    -- :target_service will be replaced with the actual value
    DECLARE events CURSOR FOR 
        FROM kubernetes-events 
        | WHERE kubernetes.deployment.name == :target_service
        | SORT @timestamp DESC 
        | LIMIT 10;
    
    FOR event IN events LOOP
        PRINT event['kubernetes.event.reason'] || ': ' || event['kubernetes.event.message'];
    END LOOP
END PROCEDURE

-- Call with specific service
CALL investigate_service('recommendation-engine');
```

String variables are automatically quoted when substituted. This allows building dynamic, reusable procedures that can query different data based on parameters.

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

## Runbook Integrations

Runbook integrations enable elastic-script to orchestrate existing automation infrastructure, making it an ideal "AI SRE" that can investigate issues and take remediation actions.

### Kubernetes Functions

Manage Kubernetes resources. Requires `K8S_API_SERVER` and `K8S_TOKEN` environment variables, or pass them as parameters.

| Function | Description |
|----------|-------------|
| `K8S_SCALE(deployment, replicas, namespace?, api_server?, token?)` | Scale a deployment |
| `K8S_RESTART(deployment, namespace?, api_server?, token?)` | Trigger rolling restart |
| `K8S_GET(resource_type, name, namespace?, api_server?, token?)` | Get resource details |
| `K8S_GET_PODS(label_selector?, namespace?, api_server?, token?)` | List pods |
| `K8S_DELETE(resource_type, name, namespace?, api_server?, token?)` | Delete a resource |
| `K8S_LOGS(pod, container?, tail_lines?, namespace?, api_server?, token?)` | Get pod logs |
| `K8S_DESCRIBE(resource_type, name, namespace?, api_server?, token?)` | Describe a resource |

**Example:**
```sql
-- Scale up during high traffic, then scale back down
K8S_SCALE('api-server', 10, 'production');
PRINT 'Scaled api-server to 10 replicas';

-- After traffic subsides
K8S_SCALE('api-server', 3, 'production');
```

---

### PagerDuty Functions

Manage incidents in PagerDuty. Requires `PAGERDUTY_ROUTING_KEY` for events and `PAGERDUTY_API_KEY` for REST API.

| Function | Description |
|----------|-------------|
| `PAGERDUTY_TRIGGER(summary, severity?, dedup_key?, routing_key?)` | Create incident, returns dedup_key |
| `PAGERDUTY_RESOLVE(dedup_key, routing_key?)` | Resolve incident |
| `PAGERDUTY_ACKNOWLEDGE(dedup_key, routing_key?)` | Acknowledge incident |
| `PAGERDUTY_ADD_NOTE(incident_id, content, user_email, api_key?)` | Add note to incident |
| `PAGERDUTY_GET_INCIDENT(incident_id, api_key?)` | Get incident details |
| `PAGERDUTY_LIST_INCIDENTS(statuses?, limit?, api_key?)` | List incidents |

**Example:**
```sql
-- Trigger an incident and store the dedup key
DECLARE incident_key STRING;
SET incident_key = PAGERDUTY_TRIGGER('Database connection pool exhausted', 'critical');

-- Later, when resolved
PAGERDUTY_RESOLVE(incident_key);
```

---

### Terraform Cloud Functions

Manage Terraform Cloud workspaces and runs. Requires `TFC_TOKEN` and `TFC_ORG` environment variables.

| Function | Description |
|----------|-------------|
| `TF_CLOUD_RUN(workspace, message?, auto_apply?, token?, org?)` | Trigger a run, returns run_id |
| `TF_CLOUD_STATUS(run_id, token?)` | Get run status |
| `TF_CLOUD_WAIT(run_id, timeout?, token?)` | Wait for run completion |
| `TF_CLOUD_CANCEL(run_id, token?)` | Cancel a run |
| `TF_CLOUD_OUTPUTS(workspace, token?, org?)` | Get workspace outputs |
| `TF_CLOUD_LIST_WORKSPACES(search?, token?, org?)` | List workspaces |

**Example:**
```sql
-- Trigger infrastructure scaling via Terraform
DECLARE run_id STRING;
SET run_id = TF_CLOUD_RUN('production-scaling', 'Auto-scale triggered by elastic-script', true);

-- Wait for completion
DECLARE result DOCUMENT;
SET result = TF_CLOUD_WAIT(run_id);
PRINT 'Terraform run completed with status: ' || result.status;
```

---

### CI/CD Functions

Trigger and monitor CI/CD pipelines across Jenkins, GitHub Actions, GitLab CI, and ArgoCD.

#### Jenkins
Requires `JENKINS_URL`, `JENKINS_USER`, and `JENKINS_TOKEN` environment variables.

| Function | Description |
|----------|-------------|
| `JENKINS_BUILD(job)` | Trigger a build |
| `JENKINS_STATUS(job)` | Get last build status |

#### GitHub Actions
Requires `GITHUB_TOKEN` environment variable.

| Function | Description |
|----------|-------------|
| `GITHUB_WORKFLOW(repo, workflow, ref?, inputs_json?)` | Trigger workflow |
| `GITHUB_WORKFLOW_STATUS(repo, run_id)` | Get run status |

#### GitLab CI
Requires `GITLAB_TOKEN` environment variable. Optionally set `GITLAB_URL` (defaults to gitlab.com).

| Function | Description |
|----------|-------------|
| `GITLAB_PIPELINE(project, ref)` | Trigger pipeline |
| `GITLAB_PIPELINE_STATUS(project, pipeline_id)` | Get pipeline status |

#### ArgoCD
Requires `ARGOCD_URL` and `ARGOCD_TOKEN` environment variables.

| Function | Description |
|----------|-------------|
| `ARGOCD_SYNC(app, prune?)` | Sync application |
| `ARGOCD_GET_APP(app)` | Get application details |

**Example:**
```sql
-- Trigger deployment via GitHub Actions
GITHUB_WORKFLOW('myorg/myrepo', 'deploy.yml', 'main', '{"environment": "production"}');

-- Or sync via ArgoCD
ARGOCD_SYNC('production-app', true);
```

---

### AWS Functions

Interact with AWS services. Requires `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, and optionally `AWS_REGION`.

#### SSM (Systems Manager)

| Function | Description |
|----------|-------------|
| `AWS_SSM_RUN(instance_ids, command, access_key?, secret_key?, region?)` | Run command, returns command_id |
| `AWS_SSM_STATUS(command_id, instance_id, access_key?, secret_key?, region?)` | Get command status |

#### Lambda

| Function | Description |
|----------|-------------|
| `AWS_LAMBDA_INVOKE(function_name, payload?, access_key?, secret_key?, region?)` | Invoke Lambda function |

#### EC2

| Function | Description |
|----------|-------------|
| `AWS_EC2_REBOOT(instance_ids, access_key?, secret_key?, region?)` | Reboot instances |
| `AWS_EC2_START(instance_ids, access_key?, secret_key?, region?)` | Start instances |
| `AWS_EC2_STOP(instance_ids, force?, access_key?, secret_key?, region?)` | Stop instances |

#### Auto Scaling

| Function | Description |
|----------|-------------|
| `AWS_ASG_SET_CAPACITY(asg_name, desired_capacity, access_key?, secret_key?, region?)` | Set desired capacity |
| `AWS_ASG_DESCRIBE(asg_name, access_key?, secret_key?, region?)` | Describe ASG |

**Example:**
```sql
-- Run a diagnostic command on multiple instances
DECLARE cmd_id STRING;
SET cmd_id = AWS_SSM_RUN('i-1234567890,i-0987654321', 'df -h && free -m');

-- Check the result
DECLARE result DOCUMENT;
SET result = AWS_SSM_STATUS(cmd_id, 'i-1234567890');
PRINT 'Command output: ' || result.StandardOutputContent;
```

---

### Generic HTTP Functions

Make arbitrary HTTP requests to any endpoint.

| Function | Description |
|----------|-------------|
| `WEBHOOK(url, method?, headers_json?, body?, timeout?)` | Generic webhook call, returns full response |
| `HTTP_GET(url, headers_json?)` | Simple GET request, returns body |
| `HTTP_POST(url, body, headers_json?)` | Simple POST request, returns body |

**Example:**
```sql
-- Send to a custom webhook
DECLARE response DOCUMENT;
SET response = WEBHOOK('https://api.example.com/notify', 'POST', 
    '{"Authorization": "Bearer token123"}',
    '{"event": "incident", "severity": "high"}');

IF response.success THEN
    PRINT 'Notification sent successfully';
END IF;
```

---

## Introspection Functions

Introspection functions allow agents and users to discover what capabilities are available in the current execution context. This is essential for AI agents that need to understand what tools they can use.

### Available Functions

| Function | Description |
|----------|-------------|
| `ESCRIPT_FUNCTIONS()` | Returns an array of all registered functions with metadata |
| `ESCRIPT_FUNCTION(name)` | Returns detailed information about a specific function |
| `ESCRIPT_VARIABLES()` | Returns an array of all declared variables in scope |

### ESCRIPT_FUNCTIONS()

Returns an array of all registered functions. Each element is a DOCUMENT containing:
- `name` - The function name
- `is_builtin` - Whether this is a built-in function
- `parameter_count` - Number of parameters
- `parameters` - Array of parameter definitions (name, type, mode)

**Example:**
```sql
-- List all available functions
DECLARE all_functions ARRAY;
SET all_functions = ESCRIPT_FUNCTIONS();

-- Print function names
FOR func IN all_functions LOOP
    PRINT func['name'] || ' (' || func['parameter_count'] || ' params)';
END LOOP
```

### ESCRIPT_FUNCTION(name)

Returns detailed information about a specific function.

**Returns a DOCUMENT with:**
- `name` - The function name
- `exists` - Whether the function exists (true/false)
- `is_builtin` - Whether this is a built-in function
- `parameter_count` - Number of parameters
- `parameters` - Array of parameter definitions
- `signature` - Human-readable signature string

**Example:**
```sql
-- Get details about K8S_SCALE
DECLARE func_info DOCUMENT;
SET func_info = ESCRIPT_FUNCTION('K8S_SCALE');

IF func_info['exists'] THEN
    PRINT 'Signature: ' || func_info['signature'];
    
    -- Print each parameter
    FOR param IN func_info['parameters'] LOOP
        PRINT '  - ' || param['name'] || ': ' || param['type'];
    END LOOP
ELSE
    PRINT 'Function not found';
END IF
```

**Output:**
```
Signature: K8S_SCALE(deployment STRING, replicas NUMBER, namespace STRING)
  - deployment: STRING
  - replicas: NUMBER
  - namespace: STRING
```

### ESCRIPT_VARIABLES()

Returns an array of all declared variables in the current scope, including parent scopes.

**Each element contains:**
- `name` - The variable name
- `type` - The variable type
- `value` - The current value (summarized for large arrays/documents)

**Example:**
```sql
DECLARE service STRING = 'api-server';
DECLARE replicas NUMBER = 5;
DECLARE items ARRAY = [1, 2, 3, 4, 5];

-- Inspect all variables
DECLARE vars ARRAY;
SET vars = ESCRIPT_VARIABLES();

FOR v IN vars LOOP
    PRINT v['name'] || ' (' || v['type'] || ') = ' || v['value'];
END LOOP
```

**Output:**
```
items (ARRAY) = [1, 2, 3, 4, 5]
replicas (NUMBER) = 5
service (STRING) = api-server
```

### Use Case: AI Agent Discovery

Introspection enables AI agents to discover available capabilities:

```sql
-- Agent wants to find Kubernetes-related functions
DECLARE funcs ARRAY = ESCRIPT_FUNCTIONS();
DECLARE k8s_funcs ARRAY = [];

FOR func IN funcs LOOP
    IF INSTR(func['name'], 'K8S_') = 0 THEN
        SET k8s_funcs = ARRAY_APPEND(k8s_funcs, func);
    END IF
END LOOP

PRINT 'Found ' || ARRAY_LENGTH(k8s_funcs) || ' Kubernetes functions:';
FOR f IN k8s_funcs LOOP
    DECLARE info DOCUMENT = ESCRIPT_FUNCTION(f['name']);
    PRINT '  ' || info['signature'];
END LOOP
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

## Scenarios & Examples

This section provides scenarios from **beginner to advanced**, demonstrating elastic-script's capabilities for observability, AI integration, and automation.

### 🟢 Beginner: Hello World

Your first elastic-script procedure:

```sql
CREATE PROCEDURE hello_world()
BEGIN
    PRINT 'Hello from elastic-script!';
    RETURN 'Hello, World!';
END PROCEDURE
```

Execute it:
```bash
POST /_escript
{"procedure": "hello_world"}
```

---

### 🟢 Beginner: Simple Query Loop

Iterate over ESQL query results:

```sql
CREATE PROCEDURE list_error_services()
BEGIN
    DECLARE error_logs CURSOR FOR 
        FROM application-logs 
        | WHERE log.level == "ERROR" 
        | STATS count = COUNT(*) BY service.name 
        | SORT count DESC 
        | LIMIT 5;
    
    DECLARE result STRING = 'Top 5 services with errors:\n';
    
    FOR row IN error_logs LOOP
        SET result = result || row['service.name'] || ': ' || row['count'] || ' errors\n';
    END LOOP
    
    PRINT result;
    RETURN result;
END PROCEDURE
```

---

### 🟢 Beginner: Variables and Conditionals

Working with variables and IF statements:

```sql
CREATE PROCEDURE check_error_threshold(threshold INT)
BEGIN
    DECLARE error_logs CURSOR FOR 
        FROM application-logs 
        | WHERE log.level == "ERROR" 
        | STATS error_count = COUNT(*);
    
    DECLARE count INT = 0;
    FOR row IN error_logs LOOP
        SET count = row['error_count'];
    END LOOP
    
    IF count > threshold THEN
        PRINT 'ALERT: ' || count || ' errors exceed threshold of ' || threshold;
        RETURN 'CRITICAL';
    ELSEIF count > 0 THEN
        PRINT 'OK: ' || count || ' errors (below threshold)';
        RETURN 'WARNING';
    ELSE
        PRINT 'OK: No errors found';
        RETURN 'OK';
    END IF
END PROCEDURE
```

Call with parameter:
```bash
POST /_escript
{"procedure": "check_error_threshold", "arguments": {"threshold": 100}}
```

---

### 🟡 Intermediate: Parameterized Queries

Use `:variable` syntax to pass parameters into CURSOR queries:

```sql
CREATE PROCEDURE get_service_errors(service_name STRING, max_results INT)
BEGIN
    -- :service_name is substituted before ESQL execution
    DECLARE errors CURSOR FOR 
        FROM application-logs 
        | WHERE kubernetes.deployment.name == :service_name
        | WHERE log.level == "ERROR"
        | SORT @timestamp DESC 
        | LIMIT :max_results;
    
    DECLARE messages ARRAY = [];
    FOR err IN errors LOOP
        SET messages = ARRAY_APPEND(messages, err['message']);
    END LOOP
    
    PRINT 'Found ' || LENGTH(messages) || ' errors for ' || service_name;
    RETURN messages;
END PROCEDURE
```

---

### 🟡 Intermediate: Cross-Index Correlation

Query multiple indices and correlate data:

```sql
CREATE PROCEDURE correlate_pod_issues(pod_name STRING)
BEGIN
    -- Get logs for this pod
    DECLARE logs CURSOR FOR 
        FROM application-logs 
        | WHERE kubernetes.pod.name == :pod_name
        | WHERE log.level IN ("ERROR", "WARN")
        | LIMIT 10;
    
    -- Get K8s events for this pod  
    DECLARE events CURSOR FOR 
        FROM kubernetes-events 
        | WHERE kubernetes.pod.name == :pod_name
        | SORT @timestamp DESC 
        | LIMIT 5;
    
    -- Get metrics for this pod
    DECLARE metrics CURSOR FOR 
        FROM system-metrics 
        | WHERE kubernetes.pod.name == :pod_name
        | SORT @timestamp DESC 
        | LIMIT 5;
    
    DECLARE report STRING = 'Pod: ' || pod_name || '\n\n';
    
    SET report = report || '=== LOGS ===\n';
    FOR log IN logs LOOP
        SET report = report || '[' || log['log.level'] || '] ' || log['message'] || '\n';
    END LOOP
    
    SET report = report || '\n=== K8S EVENTS ===\n';
    FOR event IN events LOOP
        SET report = report || '[' || event['kubernetes.event.reason'] || '] ' || event['kubernetes.event.message'] || '\n';
    END LOOP
    
    SET report = report || '\n=== METRICS ===\n';
    FOR metric IN metrics LOOP
        SET report = report || 'CPU: ' || ROUND(metric['system.cpu.total.pct'] * 100) || '%, '
                    || 'Memory: ' || ROUND(metric['system.memory.used.pct'] * 100) || '%\n';
    END LOOP
    
    PRINT report;
    RETURN report;
END PROCEDURE
```

---

### 🟡 Intermediate: Error Handling

Use TRY/CATCH for robust procedures:

```sql
CREATE PROCEDURE safe_analysis(service_name STRING)
BEGIN
    DECLARE result STRING = '';
    
    TRY
        DECLARE logs CURSOR FOR 
            FROM application-logs 
            | WHERE kubernetes.deployment.name == :service_name
            | WHERE log.level == "ERROR"
            | LIMIT 5;
        
        DECLARE log_text STRING = '';
        FOR log IN logs LOOP
            SET log_text = log_text || log['message'] || '\n';
        END LOOP
        
        IF LENGTH(log_text) > 0 THEN
            -- This might fail if LLM is unavailable
            SET result = LLM_COMPLETE('Summarize: ' || log_text);
        ELSE
            SET result = 'No errors found for ' || service_name;
        END IF
        
    CATCH
        SET result = 'Analysis failed - falling back to raw data';
        PRINT 'Error during analysis, returning fallback response';
    END TRY
    
    RETURN result;
END PROCEDURE
```

---

### 🟠 Advanced: AI-Powered Semantic Search

Use embeddings and reranking for intelligent log search:

```sql
CREATE PROCEDURE smart_log_search(query STRING)
BEGIN
    -- Fetch error logs
    DECLARE error_logs CURSOR FOR 
        FROM application-logs 
        | WHERE log.level == "ERROR" 
        | LIMIT 20;
    
    -- Collect messages into array
    DECLARE messages ARRAY = [];
    FOR log IN error_logs LOOP
        SET messages = ARRAY_APPEND(messages, log['message']);
    END LOOP
    
    -- Use ML reranking to find most relevant
    IF LENGTH(messages) > 0 THEN
        DECLARE ranked ARRAY = INFERENCE_RERANK('.rerank-v1-elasticsearch', query, messages);
        PRINT 'Top matches for: ' || query;
        RETURN ranked;
    ELSE
        RETURN 'No error logs found';
    END IF
END PROCEDURE
```

Call it:
```bash
POST /_escript
{"procedure": "smart_log_search", "arguments": {"query": "authentication failure"}}
```

---

### 🟠 Advanced: LLM-Powered Error Analysis

Generate AI summaries of error patterns:

```sql
CREATE PROCEDURE analyze_errors_with_ai(service_name STRING)
BEGIN
    DECLARE error_logs CURSOR FOR 
        FROM application-logs 
        | WHERE kubernetes.deployment.name == :service_name
        | WHERE log.level IN ("ERROR", "FATAL")
        | SORT @timestamp DESC
        | LIMIT 15;
    
    DECLARE error_summary STRING = '';
    DECLARE count INT = 0;
    
    FOR log IN error_logs LOOP
        SET error_summary = error_summary || '[' || log['log.level'] || '] ' || log['message'] || '\n';
        SET count = count + 1;
    END LOOP
    
    IF count > 0 THEN
        DECLARE prompt STRING = 'Analyze these ' || count || ' errors from ' || service_name || 
                               ' and provide: 1) Root cause 2) Impact 3) Fix:\n\n' || error_summary;
        DECLARE analysis STRING = LLM_COMPLETE(prompt);
        PRINT 'Analysis for ' || service_name || ':';
        PRINT analysis;
        RETURN analysis;
    ELSE
        RETURN 'No errors found for ' || service_name;
    END IF
END PROCEDURE
```

---

### 🔴 Expert: Full Incident Investigation

Comprehensive incident analysis correlating logs, metrics, K8s events, and traces:

```sql
CREATE PROCEDURE investigate_service(target_service STRING)
BEGIN
    PRINT 'Investigating service: ' || target_service;
    
    -- Step 1: Get K8s events for this service
    DECLARE k8s_events CURSOR FOR 
        FROM kubernetes-events 
        | WHERE kubernetes.deployment.name == :target_service
        | SORT @timestamp DESC 
        | LIMIT 10;
    
    DECLARE event_summary STRING = '';
    DECLARE event_count INT = 0;
    
    FOR event IN k8s_events LOOP
        SET event_summary = event_summary || '[' || event['kubernetes.event.reason'] || '] ' 
                           || event['kubernetes.event.message'] || '\n';
        SET event_count = event_count + 1;
    END LOOP
    
    PRINT 'Found ' || event_count || ' K8s events';
    
    -- Step 2: Get error logs for this service
    DECLARE error_logs CURSOR FOR
        FROM application-logs
        | WHERE kubernetes.deployment.name == :target_service
        | WHERE log.level IN ("ERROR", "FATAL")
        | SORT @timestamp DESC
        | LIMIT 15;
    
    DECLARE log_summary STRING = '';
    DECLARE error_count INT = 0;
    
    FOR log_entry IN error_logs LOOP
        SET log_summary = log_summary || '[' || log_entry['log.level'] || '] ' 
                         || log_entry['message'] || '\n';
        SET error_count = error_count + 1;
    END LOOP
    
    PRINT 'Found ' || error_count || ' errors';
    
    -- Step 3: Get memory metrics
    DECLARE high_mem CURSOR FOR
        FROM system-metrics
        | WHERE kubernetes.deployment.name == :target_service
        | WHERE system.memory.used.pct > 0.6
        | SORT system.memory.used.pct DESC
        | LIMIT 5;
    
    DECLARE metric_summary STRING = '';
    FOR metric IN high_mem LOOP
        SET metric_summary = metric_summary || 'Memory: ' || ROUND(metric['system.memory.used.pct'] * 100) || '%, '
                            || 'CPU: ' || ROUND(metric['system.cpu.total.pct'] * 100) || '%\n';
    END LOOP
    
    -- Step 4: Generate AI analysis
    DECLARE prompt STRING = 'Analyze this incident for service ' || target_service || ':

K8s Events (' || event_count || '):
' || event_summary || '

Error Logs (' || error_count || '):
' || log_summary || '

High Resource Usage:
' || metric_summary || '

Provide: 1) Root cause 2) Impact 3) Recommended fix';

    DECLARE analysis STRING = LLM_COMPLETE(prompt);
    
    PRINT '========================================';
    PRINT 'ANALYSIS FOR: ' || target_service;
    PRINT '========================================';
    PRINT analysis;
    
    RETURN analysis;
END PROCEDURE
```

Call it:
```bash
POST /_escript
{"procedure": "investigate_service", "arguments": {"target_service": "recommendation-engine"}}
```

---

### 🔴 Expert: Archive Reports to S3

Generate reports and archive them to cloud storage:

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
    DECLARE summary STRING = LLM_COMPLETE('Summarize in 3 bullet points:\n' || report);
    SET report = report || '\n\nAI Summary:\n' || summary;
    
    -- Archive to S3
    DECLARE key STRING = 'reports/errors/daily-' || DATE_FORMAT(NOW(), 'yyyy-MM-dd') || '.txt';
    S3_PUT('my-logs-bucket', key, report);
    
    PRINT 'Archived ' || count || ' errors to S3: ' || key;
    RETURN key;
END PROCEDURE
```

---

### 🔴 Expert: Slack Alerting with AI Summary

Send intelligent alerts to Slack:

```sql
CREATE PROCEDURE alert_critical_errors()
BEGIN
    DECLARE critical_logs CURSOR FOR 
        FROM application-logs 
        | WHERE log.level == "FATAL"
        | SORT @timestamp DESC
        | LIMIT 5;
    
    DECLARE messages STRING = '';
    DECLARE count INT = 0;
    
    FOR log IN critical_logs LOOP
        SET messages = messages || '• ' || log['service.name'] || ': ' || log['message'] || '\n';
        SET count = count + 1;
    END LOOP
    
    IF count > 0 THEN
        -- Generate AI summary
        DECLARE summary STRING = LLM_COMPLETE('Summarize these critical errors in 2 sentences:\n' || messages);
        
        -- Send to Slack
        DECLARE slack_message STRING = ':rotating_light: *' || count || ' CRITICAL ERRORS*\n\n' 
                                      || summary || '\n\n```' || messages || '```';
        SLACK_SEND('#alerts', slack_message);
        
        PRINT 'Sent alert to Slack for ' || count || ' critical errors';
        RETURN 'Alert sent';
    ELSE
        RETURN 'No critical errors';
    END IF
END PROCEDURE
```

---

### 🔴 Expert: Scheduled Health Check

A reusable health check procedure:

```sql
CREATE PROCEDURE health_check_all_services()
BEGIN
    DECLARE services ARRAY = ['api-gateway', 'user-service', 'order-service', 
                              'payment-service', 'inventory-service', 'recommendation-engine'];
    DECLARE report STRING = 'Health Check Report\n===================\n';
    DECLARE unhealthy_count INT = 0;
    
    FOR i IN 1..LENGTH(services) LOOP
        DECLARE svc STRING = services[i];
        
        -- Check error rate
        DECLARE errors CURSOR FOR 
            FROM application-logs 
            | WHERE kubernetes.deployment.name == svc
            | WHERE log.level == "ERROR"
            | STATS error_count = COUNT(*);
        
        DECLARE error_count INT = 0;
        FOR row IN errors LOOP
            SET error_count = row['error_count'];
        END LOOP
        
        -- Check K8s events
        DECLARE events CURSOR FOR 
            FROM kubernetes-events 
            | WHERE kubernetes.deployment.name == svc
            | WHERE kubernetes.event.type == "Warning"
            | STATS warning_count = COUNT(*);
        
        DECLARE warning_count INT = 0;
        FOR row IN events LOOP
            SET warning_count = row['warning_count'];
        END LOOP
        
        -- Determine status
        DECLARE status STRING = 'OK';
        IF error_count > 50 OR warning_count > 10 THEN
            SET status = 'CRITICAL';
            SET unhealthy_count = unhealthy_count + 1;
        ELSEIF error_count > 10 OR warning_count > 3 THEN
            SET status = 'WARNING';
        END IF
        
        SET report = report || svc || ': ' || status 
                    || ' (errors: ' || error_count || ', warnings: ' || warning_count || ')\n';
    END LOOP
    
    SET report = report || '\n' || unhealthy_count || ' unhealthy services';
    
    IF unhealthy_count > 0 THEN
        SLACK_SEND('#ops-alerts', ':warning: ' || report);
    END IF
    
    PRINT report;
    RETURN report;
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

---

## Roadmap: INTENT Layer & Encoded Expertise

> **Note:** This section documents planned capabilities that are not yet implemented. It serves as a design vision for how elastic-script will evolve to better serve AI agents and encode SRE best practices.

### The Problem: Knowledge Silos in SRE

Today, SRE expertise lives in scattered places:

| Location | Problem |
|----------|---------|
| Runbooks (Confluence, Notion) | Often outdated, rarely followed |
| Tribal Knowledge | Leaves when people leave |
| Post-mortem Docs | Rarely referenced again |
| Slack Threads | Buried, unsearchable |
| Senior Engineer's Head | Single point of failure |

When an incident happens at 3 AM, the on-call engineer (or an AI agent) needs to:
1. **Know** what to do
2. **Know** what order to do it in
3. **Know** what to check first
4. **Know** what NOT to do
5. **Know** when to escalate

### The Solution: Encoded Expertise via INTENTs

The INTENT layer transforms tacit SRE knowledge into **executable, versioned, testable code**.

An INTENT is a goal-oriented abstraction that:
- **Encapsulates domain expertise** - SRE best practices baked in
- **Handles safety checks** - Pre/post conditions, automatic rollback
- **Provides guardrails** - Limits what agents can do
- **Is auditable** - Clear intent = clear audit trail

### INTENT Syntax (Planned)

```sql
-- Defined by humans (SRE experts), used by agents
DEFINE INTENT safe_restart(
    service STRING,
    namespace STRING DEFAULT 'production'
)
DESCRIPTION 'Restart a service following SRE best practices'
REQUIRES
    -- Pre-conditions that must be true before execution
    NOT is_peak_traffic_window() 
        OR HAS_APPROVAL('restart-during-peak', service),
    K8S_GET('deployment', service, namespace).spec.replicas >= 2
ACTIONS
    -- Capture state before changes (expertise: always have rollback data)
    DECLARE pre_restart_state DOCUMENT;
    SET pre_restart_state = capture_service_state(service, namespace);
    
    -- Use rolling restart, not hard kill (expertise: minimize disruption)
    K8S_ROLLOUT_RESTART(service, namespace);
    
    -- Wait for rollout with timeout (expertise: don't assume success)
    DECLARE status STRING;
    SET status = K8S_ROLLOUT_WAIT(service, namespace, timeout=300);
    
    IF status != 'success' THEN
        -- Auto-rollback on failure (expertise: fail safe)
        K8S_ROLLOUT_UNDO(service, namespace);
        THROW 'Restart failed, rolled back automatically';
    END IF
    
    -- Verify health after restart (expertise: trust but verify)
    WAIT 30 SECONDS;
    IF NOT health_check_passing(service, namespace) THEN
        K8S_ROLLOUT_UNDO(service, namespace);
        PAGERDUTY_TRIGGER('Unhealthy after restart: ' || service, 'high');
    END IF
ON_FAILURE
    SLACK_SEND('#sre-incidents', 'safe_restart failed for ' || service);
    PAGERDUTY_TRIGGER('Automated restart failed: ' || service, 'high');
END INTENT;
```

### Expertise Encoding Examples

Each line in an INTENT can encode hard-won SRE knowledge:

| Code | Expertise Encoded |
|------|-------------------|
| `NOT is_peak_traffic_window()` | Don't restart during peak hours |
| `replicas >= 2` | Never restart single-replica services |
| `capture_service_state()` | Always capture state before changes |
| `K8S_ROLLOUT_RESTART` | Use rolling restart, not kill-all |
| `K8S_ROLLOUT_WAIT(timeout=300)` | Don't assume restart succeeded |
| `K8S_ROLLOUT_UNDO` on failure | Auto-rollback on failure |
| `WAIT 30 SECONDS` | Give service time to stabilize |
| `health_check_passing()` | Verify service is actually healthy |
| `ON_FAILURE` block | Always have a failure path |

### More Expertise Examples

**Scaling Expertise:**
```sql
DEFINE INTENT safe_scale(service STRING, target_replicas NUMBER)
REQUIRES
    -- Never scale to 0 in production
    target_replicas > 0 OR namespace != 'production',
    -- Cap maximum to prevent cost explosion
    target_replicas <= get_max_replicas(service),
    -- Don't scale during deployments
    NOT K8S_ROLLOUT_IN_PROGRESS(service)
ACTIONS
    DECLARE current NUMBER;
    SET current = K8S_GET('deployment', service).spec.replicas;
    
    -- Gradual scaling for large changes (expertise: avoid resource starvation)
    IF ABS(target_replicas - current) > 10 THEN
        WHILE current < target_replicas DO
            SET current = LEAST(current + 5, target_replicas);
            K8S_SCALE(service, current);
            WAIT 60 SECONDS;
        END WHILE;
    ELSE
        K8S_SCALE(service, target_replicas);
    END IF
END INTENT;
```

**Database Failover Expertise:**
```sql
DEFINE INTENT database_failover(cluster STRING)
REQUIRES
    -- Only failover if replica is in sync (expertise: prevent data loss)
    get_replica_lag(cluster) < 100,
    -- Verify replica is healthy (expertise: don't failover to broken replica)
    health_check_replica(cluster) = 'healthy',
    -- Require approval for production DBs (expertise: high-risk operations need human)
    HAS_APPROVAL('db-failover', cluster) OR NOT is_production(cluster)
ACTIONS
    -- Drain connections before failover (expertise: graceful transition)
    SET connection_drain_mode(cluster, true);
    WAIT 30 SECONDS;
    
    -- Final sync check (expertise: last-second safety)
    IF get_replica_lag(cluster) > 50 THEN
        THROW 'Replica lag too high for safe failover';
    END IF
    
    promote_replica_to_primary(cluster);
    update_service_endpoint(cluster);
    
    -- Verify writes work (expertise: verify the critical path)
    IF NOT test_write_operation(cluster) THEN
        THROW 'New primary not accepting writes';
    END IF
    
    SLACK_SEND('#database-ops', 'Failover complete for ' || cluster);
END INTENT;
```

### Agent Invocation

With INTENTs, an LLM agent generates minimal, semantic code:

```sql
-- Instead of 20+ lines of imperative code, agent generates:
INTENT safe_restart FOR SERVICE 'api-server';

-- Or with parameters:
INTENT safe_scale(service='payment-api', target_replicas=10);
```

### Benefits for AI Agents

| Without INTENT | With INTENT |
|----------------|-------------|
| Agent generates 20+ lines of code | Agent generates 1-3 lines |
| Agent must know all API details | Agent just picks the right intent |
| High chance of subtle bugs | Intent is pre-tested by humans |
| Hard to audit what agent "tried" to do | Clear semantic meaning |
| No guardrails | Built-in safety checks |

### Introspection (Planned)

Agents will be able to discover available intents using ESQL-style syntax:

```sql
-- List all available intents
FROM escript_intents() 
| WHERE category = 'remediation' 
| KEEP name, description, parameters

-- Returns:
-- name                  | description                          | parameters
-- mitigate_latency      | Reduce latency through remediation   | service, namespace, strategy
-- safe_restart          | Restart following best practices     | service, namespace
-- safe_scale            | Scale with guardrails                | service, target_replicas
-- database_failover     | Safely failover database cluster     | cluster
```

```sql
-- Get details about a specific intent
FROM escript_intent_details('safe_restart')
| KEEP parameter, type, default_value, description

-- Returns:
-- parameter  | type    | default_value | description
-- service    | STRING  | null          | Name of the service to restart
-- namespace  | STRING  | 'production'  | Kubernetes namespace
```

### The Expertise Lifecycle

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                  │
│   1. INCIDENT OCCURS                                            │
│      └─→ Post-mortem reveals: "We should have checked X"       │
│                                                                  │
│   2. EXPERTISE ENCODED                                          │
│      └─→ Add REQUIRES X_is_safe() to relevant intent           │
│                                                                  │
│   3. EXPERTISE TESTED                                           │
│      └─→ CI tests verify the new guard works                   │
│                                                                  │
│   4. EXPERTISE DEPLOYED                                         │
│      └─→ All future invocations protected                      │
│                                                                  │
│   5. EXPERTISE USED                                             │
│      └─→ Agents and humans benefit automatically               │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Composable Intents

Intents can invoke other intents, enabling complex workflows:

```sql
DEFINE INTENT full_incident_response(service STRING, incident_id STRING)
ACTIONS
    -- Acknowledge the incident
    PAGERDUTY_ACKNOWLEDGE(incident_id);
    
    -- Try automated remediation
    INTENT mitigate_latency FOR SERVICE service WITH STRATEGY 'gradual';
    
    -- Update the incident
    PAGERDUTY_ADD_NOTE(incident_id, 'Automated remediation attempted', 'sre-bot@company.com');
    
    -- Notify team
    SLACK_SEND('#incidents', 'Automated response initiated for ' || service);
END INTENT;
```

### The Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      AI Agent                                    │
│    "Handle the latency issue on api-server"                     │
└───────────────────────┬─────────────────────────────────────────┘
                        │ generates
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│                   INTENT Layer                                   │
│    INTENT mitigate_latency FOR SERVICE 'api-server'             │
│                                                                  │
│    • Semantic, goal-oriented                                    │
│    • Safe, pre-tested                                           │
│    • Auditable                                                  │
└───────────────────────┬─────────────────────────────────────────┘
                        │ expands to
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│              elastic-script Procedures                           │
│    K8S_RESTART('api-server', 'production');                     │
│    K8S_SCALE('api-server', 10, 'production');                   │
│    PAGERDUTY_ADD_NOTE(...);                                     │
└───────────────────────┬─────────────────────────────────────────┘
                        │ executes
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│              External Systems                                    │
│    Kubernetes, PagerDuty, Slack, AWS, Terraform, etc.           │
└─────────────────────────────────────────────────────────────────┘
```

### Key Takeaways

1. **Knowledge Becomes Versionable** - INTENTs live in git, with full history
2. **Knowledge Becomes Testable** - Unit tests verify safety guards work
3. **Knowledge Becomes Discoverable** - Agents query available intents
4. **Knowledge Accumulates** - Each incident improves the intents
5. **Junior Engineers Get Senior-Level Safety** - Guardrails apply to everyone

The INTENT layer transforms elastic-script from a procedural language into a **living repository of institutional knowledge** that outlives any individual engineer.

---

## See Also
- [ESQL Documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/esql.html)
- [Elasticsearch Inference API](https://www.elastic.co/guide/en/elasticsearch/reference/current/inference-apis.html)
- [Elasticsearch ML](https://www.elastic.co/guide/en/elasticsearch/reference/current/ml-apis.html)
