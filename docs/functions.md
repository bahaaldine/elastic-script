---
layout: default
title: Built-in Functions
---

# Built-in Functions

elastic-script includes **106 built-in functions** across 15 categories.

## String Functions (18)

| Function | Description | Example |
|----------|-------------|---------|
| `LENGTH(str)` | String length | `LENGTH('hello')` → `5` |
| `SUBSTR(str, start, len)` | Substring | `SUBSTR('hello', 1, 3)` → `hel` |
| `UPPER(str)` | Uppercase | `UPPER('hello')` → `HELLO` |
| `LOWER(str)` | Lowercase | `LOWER('HELLO')` → `hello` |
| `TRIM(str)` | Remove whitespace | `TRIM('  hi  ')` → `hi` |
| `LTRIM(str)` | Left trim | `LTRIM('  hi')` → `hi` |
| `RTRIM(str)` | Right trim | `RTRIM('hi  ')` → `hi` |
| `REPLACE(str, old, new)` | Replace text | `REPLACE('hello', 'l', 'x')` → `hexxo` |
| `INSTR(str, substr)` | Find position | `INSTR('hello', 'l')` → `3` |
| `LPAD(str, len, pad)` | Left pad | `LPAD('5', 3, '0')` → `005` |
| `RPAD(str, len, pad)` | Right pad | `RPAD('5', 3, '0')` → `500` |
| `SPLIT(str, delim)` | Split to array | `SPLIT('a,b,c', ',')` → `['a','b','c']` |
| `CONCAT(str1, str2, ...)` | Concatenate | `CONCAT('a', 'b')` → `ab` |
| `REGEXP_REPLACE(str, pat, rep)` | Regex replace | `REGEXP_REPLACE('a1b2', '\\d', 'X')` → `aXbX` |
| `REGEXP_SUBSTR(str, pat)` | Regex extract | `REGEXP_SUBSTR('abc123', '\\d+')` → `123` |
| `REVERSE(str)` | Reverse string | `REVERSE('hello')` → `olleh` |
| `INITCAP(str)` | Title case | `INITCAP('hello world')` → `Hello World` |
| `ENV(name)` | Environment var | `ENV('HOME')` |

## Number Functions (11)

| Function | Description | Example |
|----------|-------------|---------|
| `ABS(n)` | Absolute value | `ABS(-5)` → `5` |
| `CEIL(n)` | Round up | `CEIL(4.1)` → `5` |
| `FLOOR(n)` | Round down | `FLOOR(4.9)` → `4` |
| `ROUND(n, decimals)` | Round | `ROUND(3.14159, 2)` → `3.14` |
| `TRUNC(n)` | Truncate | `TRUNC(4.9)` → `4` |
| `MOD(a, b)` | Modulo | `MOD(10, 3)` → `1` |
| `POWER(base, exp)` | Power | `POWER(2, 3)` → `8` |
| `SQRT(n)` | Square root | `SQRT(16)` → `4` |
| `EXP(n)` | e^n | `EXP(1)` → `2.718...` |
| `LOG(n)` | Natural log | `LOG(2.718)` → `1` |
| `SIGN(n)` | Sign (-1,0,1) | `SIGN(-5)` → `-1` |

## Array Functions (18)

| Function | Description | Example |
|----------|-------------|---------|
| `ARRAY_LENGTH(arr)` | Length | `ARRAY_LENGTH([1,2,3])` → `3` |
| `ARRAY_APPEND(arr, val)` | Add to end | `ARRAY_APPEND([1,2], 3)` → `[1,2,3]` |
| `ARRAY_PREPEND(arr, val)` | Add to start | `ARRAY_PREPEND([2,3], 1)` → `[1,2,3]` |
| `ARRAY_REMOVE(arr, val)` | Remove value | `ARRAY_REMOVE([1,2,2,3], 2)` → `[1,3]` |
| `ARRAY_CONTAINS(arr, val)` | Check contains | `ARRAY_CONTAINS([1,2,3], 2)` → `TRUE` |
| `ARRAY_DISTINCT(arr)` | Unique values | `ARRAY_DISTINCT([1,1,2])` → `[1,2]` |
| `ARRAY_JOIN(arr, sep)` | Join to string | `ARRAY_JOIN(['a','b'], ',')` → `a,b` |
| `ARRAY_FLATTEN(arr)` | Flatten nested | `ARRAY_FLATTEN([[1],[2]])` → `[1,2]` |
| `ARRAY_REVERSE(arr)` | Reverse | `ARRAY_REVERSE([1,2,3])` → `[3,2,1]` |
| `ARRAY_SLICE(arr, start, end)` | Slice | `ARRAY_SLICE([1,2,3,4], 1, 3)` → `[2,3]` |
| `ARRAY_MAP(arr, expr)` | Transform each | `ARRAY_MAP([1,2,3], 'x * 2')` → `[2,4,6]` |
| `ARRAY_FILTER(arr, expr)` | Filter | `ARRAY_FILTER([1,2,3,4], 'x > 2')` → `[3,4]` |
| `ARRAY_REDUCE(arr, init, expr)` | Reduce | `ARRAY_REDUCE([1,2,3], 0, 'acc + x')` → `6` |
| `ARRAY_FIND(arr, expr)` | Find first | `ARRAY_FIND([1,2,3], 'x > 1')` → `2` |
| `ARRAY_FIND_INDEX(arr, expr)` | Find index | `ARRAY_FIND_INDEX([1,2,3], 'x > 1')` → `1` |
| `ARRAY_EVERY(arr, expr)` | All match | `ARRAY_EVERY([2,4,6], 'x % 2 = 0')` → `TRUE` |
| `ARRAY_SOME(arr, expr)` | Any match | `ARRAY_SOME([1,2,3], 'x > 2')` → `TRUE` |
| `ARRAY_SORT(arr)` | Sort | `ARRAY_SORT([3,1,2])` → `[1,2,3]` |

## Date Functions (8)

| Function | Description | Example |
|----------|-------------|---------|
| `CURRENT_DATE()` | Today's date | `CURRENT_DATE()` → `2024-01-15` |
| `CURRENT_TIMESTAMP()` | Now | `CURRENT_TIMESTAMP()` → `2024-01-15T10:30:00Z` |
| `DATE_ADD(date, n, unit)` | Add time | `DATE_ADD(NOW(), 7, 'DAYS')` |
| `DATE_SUB(date, n, unit)` | Subtract time | `DATE_SUB(NOW(), 1, 'HOURS')` |
| `DATE_DIFF(d1, d2, unit)` | Difference | `DATE_DIFF(d1, d2, 'DAYS')` |
| `EXTRACT_YEAR(date)` | Get year | `EXTRACT_YEAR(NOW())` → `2024` |
| `EXTRACT_MONTH(date)` | Get month | `EXTRACT_MONTH(NOW())` → `1` |
| `EXTRACT_DAY(date)` | Get day | `EXTRACT_DAY(NOW())` → `15` |

## Document Functions (6)

| Function | Description | Example |
|----------|-------------|---------|
| `DOCUMENT_GET(doc, key)` | Get value | `DOCUMENT_GET({"a": 1}, "a")` → `1` |
| `DOCUMENT_KEYS(doc)` | Get keys | `DOCUMENT_KEYS({"a":1, "b":2})` → `["a","b"]` |
| `DOCUMENT_VALUES(doc)` | Get values | `DOCUMENT_VALUES({"a":1})` → `[1]` |
| `DOCUMENT_CONTAINS(doc, key)` | Has key | `DOCUMENT_CONTAINS({"a":1}, "a")` → `TRUE` |
| `DOCUMENT_MERGE(d1, d2)` | Merge | `DOCUMENT_MERGE({"a":1}, {"b":2})` → `{"a":1,"b":2}` |
| `DOCUMENT_REMOVE(doc, key)` | Remove key | `DOCUMENT_REMOVE({"a":1,"b":2}, "a")` → `{"b":2}` |

## Elasticsearch Functions (5)

| Function | Description |
|----------|-------------|
| `ESQL_QUERY(query)` | Execute ES|QL query |
| `INDEX_DOCUMENT(idx, doc)` | Index a document |
| `INDEX_BULK(idx, docs)` | Bulk index |
| `GET_DOCUMENT(idx, id)` | Get document by ID |
| `REFRESH_INDEX(idx)` | Refresh index |

## OpenAI Functions (6)

| Function | Description |
|----------|-------------|
| `LLM_COMPLETE(prompt)` | Text completion |
| `LLM_CHAT(messages)` | Chat completion |
| `LLM_EMBED(text)` | Generate embeddings |
| `LLM_SUMMARIZE(text)` | Summarize text |
| `LLM_CLASSIFY(text, labels)` | Classify text |
| `LLM_EXTRACT(text, schema)` | Extract structured data |

## Elasticsearch Inference API (8)

| Function | Description |
|----------|-------------|
| `INFERENCE_CREATE_ENDPOINT(...)` | Create inference endpoint |
| `INFERENCE_DELETE_ENDPOINT(id)` | Delete endpoint |
| `INFERENCE_LIST_ENDPOINTS()` | List endpoints |
| `INFERENCE_GET_ENDPOINT(id)` | Get endpoint details |
| `INFERENCE(endpoint, input)` | Run inference |
| `INFERENCE_CHAT(endpoint, msgs)` | Chat inference |
| `INFERENCE_EMBED(endpoint, text)` | Embedding inference |
| `INFERENCE_RERANK(endpoint, ...)` | Rerank documents |

## Introspection Functions (8)

| Function | Description |
|----------|-------------|
| `ESCRIPT_FUNCTIONS()` | List all functions |
| `ESCRIPT_FUNCTION(name)` | Get function details |
| `ESCRIPT_PROCEDURES()` | List all procedures |
| `ESCRIPT_PROCEDURE(name)` | Get procedure details |
| `ESCRIPT_VARIABLES()` | List variables in scope |
| `ESCRIPT_CAPABILITIES()` | List capabilities |
| `ESCRIPT_INTENTS()` | List intents |
| `ESCRIPT_INTENT(name)` | Get intent details |

## Slack Functions (5)

| Function | Description |
|----------|-------------|
| `SLACK_SEND(channel, message)` | Send message |
| `SLACK_SEND_BLOCKS(channel, blocks)` | Send Block Kit message |
| `SLACK_WEBHOOK(url, payload)` | Send via webhook |
| `SLACK_LIST_CHANNELS()` | List channels |
| `SLACK_POST_REACTION(channel, ts, emoji)` | Add reaction |

## AWS Functions (5)

| Function | Description |
|----------|-------------|
| `AWS_LAMBDA_INVOKE(fn, payload)` | Invoke Lambda |
| `AWS_SSM_RUN(instance, cmd)` | Run SSM command |
| `AWS_SSM_STATUS(cmd_id)` | Get command status |
| `AWS_ASG_DESCRIBE(asg)` | Describe ASG |
| `AWS_ASG_SET_CAPACITY(asg, cap)` | Set ASG capacity |

## Kubernetes Functions (3)

| Function | Description |
|----------|-------------|
| `K8S_GET(kind, name, ns)` | Get resource |
| `K8S_PATCH(kind, name, ns, patch)` | Patch resource |
| `K8S_SCALE(kind, name, ns, replicas)` | Scale deployment |

## PagerDuty Functions (6)

| Function | Description |
|----------|-------------|
| `PAGERDUTY_TRIGGER(service, title)` | Create incident |
| `PAGERDUTY_ACKNOWLEDGE(id)` | Acknowledge incident |
| `PAGERDUTY_RESOLVE(id)` | Resolve incident |
| `PAGERDUTY_GET_INCIDENT(id)` | Get incident |
| `PAGERDUTY_LIST_INCIDENTS()` | List incidents |
| `PAGERDUTY_ADD_NOTE(id, note)` | Add note |

## Terraform Cloud Functions (6)

| Function | Description |
|----------|-------------|
| `TF_CLOUD_RUN(workspace, ...)` | Start run |
| `TF_CLOUD_STATUS(run_id)` | Get run status |
| `TF_CLOUD_WAIT(run_id)` | Wait for completion |
| `TF_CLOUD_OUTPUTS(workspace)` | Get outputs |
| `TF_CLOUD_CANCEL(run_id)` | Cancel run |
| `TF_CLOUD_LIST_WORKSPACES()` | List workspaces |

## CI/CD Functions (6)

| Function | Description |
|----------|-------------|
| `GITHUB_WORKFLOW(owner, repo, wf)` | Trigger workflow |
| `GITHUB_WORKFLOW_STATUS(...)` | Get workflow status |
| `GITLAB_PIPELINE(project, ref)` | Trigger pipeline |
| `GITLAB_PIPELINE_STATUS(...)` | Get pipeline status |
| `JENKINS_BUILD(job)` | Trigger build |
| `JENKINS_STATUS(job, build)` | Get build status |

## S3 Functions (3)

| Function | Description |
|----------|-------------|
| `S3_GET(bucket, key)` | Get object |
| `S3_PUT(bucket, key, data)` | Put object |
| `S3_LIST(bucket, prefix)` | List objects |

## HTTP Functions (3)

| Function | Description |
|----------|-------------|
| `HTTP_GET(url, headers)` | GET request |
| `HTTP_POST(url, body, headers)` | POST request |
| `WEBHOOK(url, payload)` | Send webhook |
