# elastic-script Language Documentation

## The Observability Intelligence Language

**elastic-script** is a domain-specific programming language that extends ESQL (Elasticsearch Query Language) with procedural logic, encoded expertise, and AI-agent capabilities. It transforms Elasticsearch from a query engine into a programmable observability platform.

---

## Why elastic-script Exists

### The Problem

Observability is drowning in complexity:

1. **Knowledge lives in runbooks that humans read, not execute.** When an incident occurs, SREs scramble through documentation, copy-paste commands, and hope they remember the right sequence.

2. **AI agents can query data but can't act safely.** LLMs can write ESQL queries, but there's no structured way for them to discover capabilities, understand safety constraints, or execute remediation.

3. **Best practices aren't enforceable.** "Always check for active incidents before scaling" is written in a wiki. It's not code. It's not enforced. It's forgotten at 3 AM.

4. **Complex analysis requires leaving Elasticsearch.** Aggregating, correlating, and computing across logs, metrics, and traces means exporting data to external systems.

### The Solution

**Make observability expertise executable.**

---

## 🚀 Quick Start

Get elastic-script running in minutes:

```bash
# Clone the repository
git clone https://github.com/elastic/elastic-script.git
cd elastic-script

# Run the quick start (builds, starts ES, loads data, opens notebooks)
./scripts/quick-start.sh
```

**What this does:**
1. ✅ Checks prerequisites (Java 21+, Python 3.8+)
2. ✅ Builds the elastic-script plugin
3. ✅ Starts Elasticsearch with the plugin
4. ✅ Loads sample data (logs, metrics)
5. ✅ Launches Jupyter with interactive notebooks

**Try your first procedure:**
```sql
CREATE PROCEDURE hello_elastic()
BEGIN
    RETURN 'Welcome to elastic-script!';
END PROCEDURE
```

```sql
CALL hello_elastic()  -- Returns: "Welcome to elastic-script!"
```

See the [notebooks/](notebooks/) directory for interactive tutorials.

---

elastic-script allows you to:
- **Encode SRE knowledge as code** - Runbooks become procedures. Best practices become INTENTs with guardrails.
- **Query and compute in one language** - ESQL for data, elastic-script for logic. No context switching.
- **Enable AI agents to act safely** - Discoverable capabilities, machine-readable pre-conditions, introspectable contracts.
- **Store expertise in Elasticsearch** - Procedures are documents. Searchable, versionable, part of your data plane.

---

## What elastic-script Is

### A Programming Language, Not Configuration

elastic-script has a formal grammar (ANTLR-based), a type system, and full programming constructs:

```sql
-- Variables, types, expressions
DECLARE error_rate NUMBER = errors / total * 100;

-- Control flow
IF error_rate > threshold THEN
    INTENT ALERT_ONCALL FOR service;
END IF

-- Functional operations
DECLARE critical_services ARRAY = ARRAY_FILTER(services, s => s['error_rate'] > 5.0);

-- Inline ESQL
FOR log IN (FROM logs | WHERE level == 'error' | STATS count = COUNT(*) BY service) LOOP
    PRINT log['service'] || ': ' || log['count'] || ' errors';
END LOOP
```

### ESQL-Native, Not a Wrapper

elastic-script doesn't call Elasticsearch APIs—it **extends ESQL itself**:

```sql
-- ESQL queries embedded directly in the language
FOR trace IN (
    FROM traces 
    | WHERE duration_ms > 1000 
    | STATS p99 = PERCENTILE(duration_ms, 99) BY endpoint
    | SORT p99 DESC 
    | LIMIT 10
) LOOP
    IF trace['p99'] > SLA_THRESHOLD THEN
        INTENT INVESTIGATE_LATENCY FOR trace['endpoint'];
    END IF
END LOOP

-- Functions as data sources (unique capability)
FOR func IN (FROM ESCRIPT_CAPABILITIES() | WHERE category == 'RUNBOOK') LOOP
    PRINT func['name'] || ': ' || func['description'];
END LOOP
```

### Stored Procedures as Elasticsearch Documents

Procedures are stored **in Elasticsearch**, not in a separate system:

```bash
# Create a procedure
PUT /_elastic_script/procedures/analyze_incident
{
  "source": "PROCEDURE analyze_incident(incident_id STRING) BEGIN ... END PROCEDURE"
}
```

```sql
-- Query your procedures with ESQL
FROM .elastic_script_procedures 
| WHERE author == 'sre-team' 
| WHERE tags CONTAINS 'kubernetes'
| SORT last_modified DESC
```

This means:
- **Version control** via document history
- **Access control** via Elasticsearch security
- **Discovery** via ESQL queries
- **Replication** across clusters automatically

---

## How elastic-script Works

### 1. Procedural Logic for Complex Operations

```sql
PROCEDURE investigate_high_latency(service STRING, threshold_ms NUMBER)
BEGIN
    -- Query recent slow traces
    DECLARE slow_traces ARRAY = (
        FROM traces 
        | WHERE service_name == :service AND duration_ms > :threshold_ms
        | SORT duration_ms DESC 
        | LIMIT 100
    );
    
    -- Aggregate by endpoint
    DECLARE by_endpoint DOCUMENT = {};
    FOR trace IN slow_traces LOOP
        VAR endpoint = trace['endpoint'];
        SET by_endpoint[endpoint] = (by_endpoint[endpoint] ?? 0) + 1;
    END LOOP
    
    -- Find worst endpoint using functional operations
    VAR worst = ARRAY_REDUCE(
        OBJECT_KEYS(by_endpoint),
        (max, ep) => by_endpoint[ep] > (by_endpoint[max] ?? 0) ? ep : max
    );
    
    RETURN {
        "service": service,
        "worst_endpoint": worst,
        "slow_trace_count": ARRAY_LENGTH(slow_traces),
        "by_endpoint": by_endpoint
    };
END PROCEDURE
```

### 2. INTENTs: Encoded Expertise with Guardrails

INTENTs are semantic contracts that encode **what to do**, **when it's safe**, and **how to recover**:

```sql
DEFINE INTENT SCALE_SERVICE
    DESCRIPTION 'Safely scale a Kubernetes service with operational guardrails'
    
    REQUIRES
        -- Don't scale during active incidents
        PAGERDUTY_ACTIVE_INCIDENTS(:service) == 0,
        
        -- Verify cluster has capacity
        :target_replicas <= K8S_CLUSTER_CAPACITY(:cluster) * 0.8,
        
        -- Require approval for large scale-ups
        :target_replicas < 10 OR APPROVAL_GRANTED(:change_id)
    
    ACTIONS
        K8S_SCALE_DEPLOYMENT(:deployment, :target_replicas);
        LOG_AUDIT('scale', :service, :target_replicas);
        PRINT 'Scaled ' || :deployment || ' to ' || :target_replicas || ' replicas';
    
    ON_FAILURE
        PAGERDUTY_TRIGGER('Scale operation failed: ' || :service, 'high');
        INTENT NOTIFY_ONCALL FOR :service WITH message = 'Scale failed, manual intervention required';
END INTENT

-- Execute with safety guarantees
INTENT SCALE_SERVICE 
    FOR 'payment-service' 
    WITH target_replicas = 5, cluster = 'production';
```

### 3. AI-Agent Introspection

AI agents can **discover**, **understand**, and **reason about** capabilities:

```sql
-- Discovery: What capabilities exist?
FOR cap IN (FROM ESCRIPT_CAPABILITIES() | WHERE category == 'RUNBOOK') LOOP
    PRINT cap['name'] || ' - ' || cap['description'];
END LOOP

-- Understanding: How do I use this?
DECLARE func_info DOCUMENT = ESCRIPT_FUNCTION('K8S_SCALE_DEPLOYMENT');
PRINT 'Signature: ' || func_info['signature'];
PRINT 'Parameters: ' || func_info['parameters'];

-- Reasoning: Can I safely execute this intent?
DECLARE intent_status DOCUMENT = ESCRIPT_INTENT_STATUS('SCALE_SERVICE', 'payment-service');
IF intent_status['can_execute'] THEN
    INTENT SCALE_SERVICE FOR 'payment-service' WITH target_replicas = 5;
ELSE
    FOR req IN intent_status['failed_requirements'] LOOP
        PRINT 'Blocked: ' || req['description'] || ' (current: ' || req['value'] || ')';
    END LOOP
END IF
```

### 4. Expression-Level Composition

Complex computations in single expressions, not multi-step data passing:

```sql
-- Single expression: filter, map, reduce, compute
DECLARE avg_error_rate NUMBER = 
    ARRAY_REDUCE(
        ARRAY_MAP(
            ARRAY_FILTER(services, s => s['status'] == 'degraded'),
            s => s['error_count'] / s['request_count'] * 100
        ),
        (sum, rate) => sum + rate
    ) / ARRAY_LENGTH(ARRAY_FILTER(services, s => s['status'] == 'degraded'));

-- Ternary, null coalescing, safe navigation - all inline
DECLARE alert_level STRING = 
    error_rate > 10 ? 'critical' : 
    error_rate > 5 ? 'warning' : 
    'normal';

DECLARE owner STRING = service?.team?.oncall ?? 'platform-team';
```

---

## Vision: The Future of Observability

### Phase 1: Language Foundation ✅ Complete
- Procedural constructs (variables, loops, conditionals)
- ESQL integration (inline queries, cursors)
- Built-in functions (string, array, document, date)
- Stored procedures in Elasticsearch

### Phase 2: Expertise Encoding ✅ Complete
- INTENT layer with pre-conditions and guardrails
- Introspection functions for AI agents
- Runbook integrations (Kubernetes, AWS, PagerDuty)
- Lambda expressions and functional operations

### Phase 3: AI-Native Operations 🚧 In Progress
- `ESCRIPT_INTENT_STATUS()` - Machine-readable safety checks
- `ESCRIPT_SUGGEST()` - AI-powered capability recommendations
- Natural language to elastic-script compilation
- Agent memory and context persistence

### Phase 4: Observability Semantics 🔲 Planned
- First-class `LOG`, `METRIC`, `TRACE` types
- Semantic correlation (`CORRELATE logs WITH traces BY trace_id`)
- Anomaly detection primitives (`DETECT_ANOMALY(metric, window)`)
- SLO/SLI language constructs

### Phase 5: Distributed Execution 🔲 Planned

**Pipe-driven async execution.** Every procedure is async. Pipes define continuations.

```sql
analyze_logs()
| ON_DONE process_result(@result)
| ON_FAIL handle_error(@error)
| TRACK AS 'daily-analysis';

-- Query executions with ESQL
FROM .escript_executions | WHERE status == 'RUNNING';
```

See [Distributed Execution Roadmap](#distributed-execution-roadmap) for the complete architecture and implementation plan.

---

## Who elastic-script Is For

| User | Use Case |
|------|----------|
| **SRE Teams** | Encode runbooks as executable procedures with safety guardrails |
| **AI Agents** | Discover, reason about, and execute observability operations safely |
| **Platform Engineers** | Build reusable, composable observability primitives |
| **Data Engineers** | Complex data transformations within Elasticsearch |
| **Security Teams** | Automated threat detection and response procedures |

---

## Design Principles

1. **ESQL-Native**: Extend ESQL, don't wrap it. The same query syntax, same mental model.

2. **Pipe-Driven**: Everything flows through pipes—data transformations AND execution continuations. The pipe is the universal primitive.

3. **Expertise as Code**: Best practices should be executable, not documented.

4. **AI-Agent First**: Every capability is discoverable, introspectable, and has machine-readable contracts.

5. **Stored as Data**: Procedures AND executions are Elasticsearch documents—searchable, versionable, queryable.

6. **Expression Composition**: Complex logic in single expressions, not multi-step data passing.

7. **Safety by Design**: INTENTs encode when operations are safe, not just how to execute them.

8. **Observability Built-In**: Execution state is queryable data. No instrumentation needed.

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
  - [Async Execution (Pipe-Driven)](#async-execution-pipe-driven)
  - [Execution Control](#execution-control)
  - [Parallel Execution](#parallel-execution)
  - [INTENT](#intent)
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

### SWITCH / CASE

**Purpose:** Multi-branch selection based on matching a value against multiple cases.

**Syntax:**
```sql
SWITCH <expression>
    CASE <value1>:
        <statements>
    CASE <value2>:
        <statements>
    [DEFAULT:
        <statements>]
END SWITCH
```

**Examples:**
```sql
-- Numeric switch
SWITCH day_of_week
    CASE 1:
        RETURN 'Monday';
    CASE 2:
        RETURN 'Tuesday';
    CASE 3:
        RETURN 'Wednesday';
    DEFAULT:
        RETURN 'Other day';
END SWITCH

-- String switch
SWITCH log_level
    CASE 'ERROR':
        SET color = 'red';
    CASE 'WARNING':
        SET color = 'yellow';
    CASE 'INFO':
        SET color = 'blue';
    DEFAULT:
        SET color = 'gray';
END SWITCH
```

**Notes:**
- Only the first matching case is executed (no fall-through)
- BREAK can be used to exit the switch early
- DEFAULT is optional; if no case matches and no default, execution continues after END SWITCH

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

### Async Execution (Pipe-Driven)

**Purpose:** Execute procedures asynchronously with pipe-driven continuations. Every procedure call can be made async by piping to handlers.

**The pipe is the universal primitive** - same syntax users know from ESQL, now for execution flow.

**Core Syntax:**
```sql
-- Basic async with continuation
procedure_name(args)
| ON_DONE handler(@result)
| ON_FAIL handler(@error)
| TRACK AS 'execution-name';

-- Example: Analyze logs with error handling
analyze_logs('logs-*')
| ON_DONE process_results(@result)
| ON_DONE notify_team(@result)
| ON_FAIL alert_oncall(@error)
| FINALLY cleanup()
| TRACK AS 'daily-analysis';

-- Fire and forget (no continuations)
send_notification('All done!');
```

**Pipe Operations:**
| Pipe | Description |
|------|-------------|
| `\| ON_DONE handler(@result)` | Called when procedure succeeds. @result binds the return value. |
| `\| ON_FAIL handler(@error)` | Called when procedure fails. @error binds the error message. |
| `\| FINALLY handler()` | Always called at the end (like try/finally). |
| `\| TRACK AS 'name'` | Name the execution for tracking and querying. |
| `\| TIMEOUT seconds` | Maximum execution time before auto-cancel. |

**Chaining Continuations:**
Multiple ON_DONE handlers are executed in sequence:
```sql
fetch_data('source')
| ON_DONE transform(@result)
| ON_DONE validate(@result)
| ON_DONE store(@result)
| ON_FAIL rollback(@error);
```

---

### Execution Control

**Purpose:** Query and control async executions by name.

**Syntax:**
```sql
-- Get execution status
EXECUTION('execution-name') | STATUS;

-- Cancel a running execution
EXECUTION('execution-name') | CANCEL;

-- Retry a failed execution
EXECUTION('execution-name') | RETRY;

-- Block until completion (for notebooks/interactive use)
EXECUTION('execution-name') | WAIT;
EXECUTION('execution-name') | WAIT TIMEOUT 60;
```

**Querying Executions with ESQL:**
Executions are stored in `.escript_executions` - query them with ESQL:
```sql
-- List all running executions
FROM .escript_executions
| WHERE status == 'RUNNING'
| KEEP name, procedure, progress, started_at
| SORT started_at DESC;

-- Find recent failures
FROM .escript_executions
| WHERE status == 'FAILED' AND started_at > NOW() - 1 HOUR
| KEEP name, error, started_at;

-- Execution statistics
FROM .escript_executions
| STATS 
    running = COUNT(*) WHERE status == 'RUNNING',
    completed = COUNT(*) WHERE status == 'COMPLETED',
    failed = COUNT(*) WHERE status == 'FAILED';
```

---

### Parallel Execution

**Purpose:** Execute multiple procedures simultaneously and handle their combined results.

**Syntax:**
```sql
PARALLEL [proc1(), proc2(), proc3()]
| ON_ALL_DONE merge_results(@results)
| ON_ANY_FAIL handle_partial(@error)
| TRACK AS 'parallel-job';
```

**Example:**
```sql
-- Fetch from multiple sources in parallel
PARALLEL [
    fetch_logs(),
    fetch_metrics(),
    fetch_traces()
]
| ON_ALL_DONE merge_data(@results)
| ON_ANY_FAIL partial_analysis(@error)
| TRACK AS 'parallel-fetch';
```

**Parallel Pipes:**
| Pipe | Description |
|------|-------------|
| `\| ON_ALL_DONE handler(@results)` | Called when ALL procedures complete. @results is an array. |
| `\| ON_ANY_FAIL handler(@error)` | Called if ANY procedure fails. |
| `\| TRACK AS 'name'` | Name the parallel execution. |
| `\| TIMEOUT seconds` | Timeout for all parallel procedures. |

---

### INTENT

**Purpose:** Define goal-oriented automation with encoded expertise, guardrails, and pre-conditions.

INTENTs are a higher-level abstraction than procedures, designed for:
- **AI Agents**: Safe, semantic automation that agents can discover and invoke
- **Encoded Expertise**: SRE best practices baked into the definition
- **Guardrails**: REQUIRES clause ensures pre-conditions are met before execution
- **Auditability**: Clear intent = clear audit trail

**Define Syntax:**
```sql
DEFINE INTENT name(param1 TYPE, param2 TYPE, ...)
DESCRIPTION 'What this intent does'
REQUIRES
    condition1,
    condition2
ACTIONS
    <statements>
ON_FAILURE
    <statements>
END INTENT
```

**Invoke Syntax:**
```sql
-- With positional arguments
INTENT name(arg1, arg2);

-- With named arguments
INTENT name WITH param1 = value1, param2 = value2;
```

**Example - Safe Restart Intent:**
```sql
DEFINE INTENT safe_restart(service STRING, namespace STRING)
DESCRIPTION 'Restart a service following SRE best practices'
REQUIRES
    K8S_GET('deployment', service, namespace) IS NOT NULL,
    NOT is_peak_traffic_window()
ACTIONS
    -- Capture state before changes
    DECLARE pre_state DOCUMENT;
    SET pre_state = K8S_GET('deployment', service, namespace);
    
    -- Perform rolling restart
    K8S_ROLLOUT_RESTART(service, namespace);
    
    -- Wait and verify
    DECLARE status STRING;
    SET status = K8S_ROLLOUT_WAIT(service, namespace);
    
    IF status != 'success' THEN
        K8S_ROLLOUT_UNDO(service, namespace);
        THROW 'Restart failed, rolled back';
    END IF
ON_FAILURE
    SLACK_SEND('#sre-alerts', 'safe_restart failed for ' || service);
    PAGERDUTY_TRIGGER('Automated restart failed: ' || service, 'high');
END INTENT
```

**Invoking the Intent:**
```sql
-- Simple invocation
INTENT safe_restart('api-server', 'production');

-- With named parameters
INTENT safe_restart WITH service = 'api-server', namespace = 'production';
```

**Introspection:**
```sql
-- List all defined intents
DECLARE intents ARRAY = ESCRIPT_INTENTS();

-- Get details about a specific intent
DECLARE info DOCUMENT = ESCRIPT_INTENT('safe_restart');
PRINT info['signature'];  -- safe_restart(service STRING, namespace STRING)
PRINT info['description']; -- Restart a service following SRE best practices
PRINT info['has_requires']; -- true
```

**Key Concepts:**
- **REQUIRES**: Pre-conditions that must be true before ACTIONS execute. If any condition fails, the intent is blocked.
- **ACTIONS**: The main logic that executes when the intent is invoked.
- **ON_FAILURE**: Statements that execute if ACTIONS throws an error.
- **DESCRIPTION**: Human-readable explanation (useful for agent discovery).

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
| `ESCRIPT_PROCEDURES()` | Returns an array of all stored procedures |
| `ESCRIPT_PROCEDURE(name)` | Returns detailed information about a specific procedure |
| `ESCRIPT_INTENTS()` | Returns an array of all defined intents |
| `ESCRIPT_INTENT(name)` | Returns detailed information about a specific intent |
| `ESCRIPT_VARIABLES()` | Returns an array of all declared variables in scope |
| `ESCRIPT_CAPABILITIES(category?)` | Returns all capabilities in a table-friendly format |

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

### ESCRIPT_PROCEDURES()

Returns an array of all stored procedures in Elasticsearch.

**Each element contains:**
- `name` - The procedure name (document ID)
- `parameter_count` - Number of parameters
- `parameters` - Array of parameter definitions (name, type)
- `signature` - Human-readable signature string

**Example:**
```sql
-- List all stored procedures
DECLARE procs ARRAY;
SET procs = ESCRIPT_PROCEDURES();

PRINT 'Available procedures:';
FOR proc IN procs LOOP
    PRINT '  ' || proc['signature'];
END LOOP
```

**Output:**
```
Available procedures:
  investigate_service(target_service STRING)
  health_check_all_services()
  archive_daily_report()
```

### ESCRIPT_PROCEDURE(name)

Returns detailed information about a specific stored procedure.

**Returns a DOCUMENT with:**
- `name` - The procedure name
- `exists` - Whether the procedure exists (true/false)
- `parameter_count` - Number of parameters
- `parameters` - Array of parameter definitions
- `signature` - Human-readable signature string
- `source` - The full procedure source code

**Example:**
```sql
-- Get details about a specific procedure
DECLARE proc_info DOCUMENT;
SET proc_info = ESCRIPT_PROCEDURE('investigate_service');

IF proc_info['exists'] THEN
    PRINT 'Signature: ' || proc_info['signature'];
    PRINT 'Parameters:';
    FOR param IN proc_info['parameters'] LOOP
        PRINT '  - ' || param['name'] || ': ' || param['type'];
    END LOOP
ELSE
    PRINT 'Procedure not found';
END IF
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

### ESCRIPT_CAPABILITIES(category?)

Returns all capabilities (functions and procedures) in a unified, table-friendly format. Results are sorted by category, then by name.

**Optional category parameter** filters by function name prefix: `"K8S"`, `"AWS"`, `"PAGERDUTY"`, etc.

**Each element contains:**
- `type` - "FUNCTION" or "PROCEDURE"
- `name` - The name
- `signature` - Human-readable signature
- `parameter_count` - Number of parameters
- `category` - Detected category (KUBERNETES, AWS, PAGERDUTY, CICD, AI, STRING, NUMBER, etc.)

**Example - List all capabilities:**
```sql
DECLARE caps ARRAY = ESCRIPT_CAPABILITIES(null);
-- Returns all functions and procedures sorted by category
```

**Example - Filter by category:**
```sql
-- Get only Kubernetes functions
DECLARE k8s_caps ARRAY = ESCRIPT_CAPABILITIES('K8S');

-- Get only AWS functions  
DECLARE aws_caps ARRAY = ESCRIPT_CAPABILITIES('AWS');
```

**Example output (as displayed in a notebook):**

| type | category | name | signature | parameter_count |
|------|----------|------|-----------|-----------------|
| FUNCTION | AWS | AWS_EC2_REBOOT | AWS_EC2_REBOOT(instance_ids STRING) | 1 |
| FUNCTION | AWS | AWS_LAMBDA_INVOKE | AWS_LAMBDA_INVOKE(function_name STRING, payload STRING) | 2 |
| FUNCTION | KUBERNETES | K8S_SCALE | K8S_SCALE(deployment STRING, replicas NUMBER) | 2 |
| PROCEDURE | STORED_PROCEDURE | investigate_service | investigate_service(target_service STRING) | 1 |

---

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

### Use Case: Comprehensive Capability Report

Generate a full report of all available capabilities:

```sql
CREATE PROCEDURE capability_report()
BEGIN
    PRINT '=== ELASTIC-SCRIPT CAPABILITY REPORT ===';
    PRINT '';
    
    -- List all built-in functions
    DECLARE funcs ARRAY = ESCRIPT_FUNCTIONS();
    PRINT 'Built-in Functions: ' || ARRAY_LENGTH(funcs);
    
    -- List all stored procedures
    DECLARE procs ARRAY = ESCRIPT_PROCEDURES();
    PRINT 'Stored Procedures: ' || ARRAY_LENGTH(procs);
    
    PRINT '';
    PRINT '--- Stored Procedures ---';
    FOR proc IN procs LOOP
        PRINT proc['signature'];
    END LOOP
    
    PRINT '';
    PRINT '--- Runbook Functions ---';
    DECLARE info DOCUMENT;
    FOR func IN funcs LOOP
        -- Filter for runbook-related functions
        IF INSTR(func['name'], 'K8S_') == 0 
           OR INSTR(func['name'], 'AWS_') == 0 
           OR INSTR(func['name'], 'PAGERDUTY_') == 0 THEN
            SET info = ESCRIPT_FUNCTION(func['name']);
            PRINT info['signature'];
        END IF
    END LOOP
    
    RETURN 'Report complete';
END PROCEDURE
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

> **Status:** The INTENT layer grammar and introspection are implemented. See the [INTENT](#intent) section in Language Constructs for syntax documentation. The examples below show advanced patterns and the vision for expertise encoding.

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

### Intent Introspection

Agents can discover available intents using the introspection functions:

```sql
-- List all available intents
DECLARE intents ARRAY = ESCRIPT_INTENTS();
FOR intent IN intents LOOP
    PRINT intent['signature'] || ' - ' || intent['description'];
END LOOP

-- Output:
-- safe_restart(service STRING, namespace STRING) - Restart following best practices
-- safe_scale(service STRING, target_replicas NUMBER) - Scale with guardrails
-- database_failover(cluster STRING) - Safely failover database cluster
```

```sql
-- Get details about a specific intent
DECLARE info DOCUMENT = ESCRIPT_INTENT('safe_restart');
IF info['exists'] THEN
    PRINT 'Signature: ' || info['signature'];
    PRINT 'Has pre-conditions: ' || info['has_requires'];
    FOR param IN info['parameters'] LOOP
        PRINT '  - ' || param['name'] || ': ' || param['type'];
    END LOOP
END IF

-- Output:
-- Signature: safe_restart(service STRING, namespace STRING)
-- Has pre-conditions: true
--   - service: STRING
--   - namespace: STRING
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

---

## Language Roadmap: Planned Enhancements

This section outlines planned language improvements for elastic-script, organized by priority.

### Phase 1: Core Language Primitives ✅ Priority: High | Effort: Small

| Feature | Description | Status |
|---------|-------------|--------|
| `NOT` / `!` operator | Logical negation: `IF NOT active THEN` or `IF !is_admin THEN` | ✅ Implemented |
| `IS NULL` / `IS NOT NULL` | SQL-style null checking: `IF value IS NULL THEN` | ✅ Implemented |
| `CONTINUE` statement | Skip to next loop iteration | ✅ Implemented |

**Example usage after implementation:**
```sql
IF NOT user_active THEN
    PRINT 'User is inactive';
END IF

IF value IS NULL THEN
    SET value = 'default';
END IF

FOR item IN items LOOP
    IF item['skip'] == true THEN
        CONTINUE;  -- Skip to next iteration
    END IF
    process_item(item);
END LOOP
```

---

### Phase 2: Expression Operators ✅ Priority: High | Effort: Medium

| Feature | Description | Status |
|---------|-------------|--------|
| `%` modulo operator | Inline modulo: `IF i % 2 == 0 THEN` | ✅ Implemented |
| `IN` operator | Collection membership: `IF status IN ('active', 'pending') THEN` | ✅ Implemented |
| `NOT IN` operator | Negated collection membership: `IF x NOT IN blocked THEN` | ✅ Implemented |
| `SWITCH/CASE` statement | Multi-branch selection | ✅ Implemented |

**Example usage:**
```sql
-- Modulo operator
IF i % 2 == 0 THEN
    PRINT 'Even';
END IF

-- IN operator with list
IF status IN ('active', 'pending', 'processing') THEN
    PRINT 'Valid status';
END IF

-- IN operator with array variable
DECLARE allowed ARRAY = ["admin", "user", "guest"];
IF role IN allowed THEN
    PRINT 'Allowed';
END IF

-- NOT IN operator
IF user NOT IN blocked THEN
    PRINT 'Access granted';
END IF

-- SWITCH/CASE statement
SWITCH severity
    CASE 'critical':
        PRINT 'Page immediately!';
    CASE 'warning':
        PRINT 'Send to #alerts';
    DEFAULT:
        PRINT message;
END SWITCH
```

---

### Phase 3: Null Safety & Ternary ✅ Priority: Medium | Effort: Medium

| Feature | Description | Status |
|---------|-------------|--------|
| `??` null coalescing | Default if null: `value ?? 'Default'` | ✅ Implemented |
| `?.` safe navigation | Safe property access: `user?.profile?.settings` | ✅ Implemented |
| Ternary `? :` | Inline conditional: `active ? 'Yes' : 'No'` | ✅ Implemented |

**Example usage:**
```sql
-- Null coalescing: returns 'Unknown' if doc['name'] is null
RETURN doc['name'] ?? 'Unknown';
RETURN config['port'] ?? 8080;

-- Null coalesce chain: returns first non-null value
RETURN doc['primary'] ?? doc['secondary'] ?? 'Default';

-- Safe navigation: returns null if any part is null (instead of error)
RETURN user?.profile?.settings?.theme;
RETURN doc['profile']?.preferences ?? 'No preferences';

-- Ternary operator: inline conditional
RETURN is_active ? 'Active' : 'Inactive';
RETURN error_count > 0 ? 'red' : 'green';

-- Combined: ternary with null coalescing
RETURN has_data ? data : (fallback ?? 'Default');
```

---

### Phase 4: Type Inference & Constants ✅ Priority: Medium | Effort: Small

| Feature | Description | Status |
|---------|-------------|--------|
| `VAR` keyword | Type inference: `VAR name = 'John';` | ✅ Implemented |
| `CONST` keyword | Immutable constants: `CONST MAX_RETRIES = 3;` | ✅ Implemented |

**Example usage:**
```sql
-- Type inference with VAR (type is inferred from the value)
VAR name = 'John';           -- Inferred as STRING
VAR count = 42;              -- Inferred as NUMBER
VAR items = [1, 2, 3];       -- Inferred as ARRAY
VAR active = true;           -- Inferred as BOOLEAN
VAR user = {"name": "Alice"};-- Inferred as DOCUMENT

-- Multiple VAR declarations
VAR a = 10, b = 20, c = 30;

-- Constants (immutable) - type inferred
CONST MAX_RETRIES = 3;
CONST API_URL = 'https://api.example.com';

-- Constants with explicit type
CONST FACTOR NUMBER = 2.5;
CONST PREFIX STRING = 'app_';

-- Using VAR and CONST together
CONST MULTIPLIER = 10;
VAR value = 5;
SET value = value * MULTIPLIER;  -- OK: value is mutable

-- This throws an error:
-- SET MAX_RETRIES = 5;  -- Error: Cannot modify constant 'MAX_RETRIES'
```

---

### Phase 5: Functional Array Operations ✅ Complete

| Function | Description | Status |
|----------|-------------|--------|
| `ARRAY_MAP(arr, property)` | Extract property from each object | ✅ Implemented |
| `ARRAY_FILTER(arr, property, value)` | Keep elements where property equals value | ✅ Implemented |
| `ARRAY_REDUCE(arr, initial)` | Sum numbers or concatenate strings | ✅ Implemented |
| `ARRAY_FIND(arr, property, value)` | Find first matching element | ✅ Implemented |
| `ARRAY_FIND_INDEX(arr, property, value)` | Find index of first match | ✅ Implemented |
| `ARRAY_EVERY(arr, property, value)` | Check if all match | ✅ Implemented |
| `ARRAY_SOME(arr, property, value)` | Check if any match | ✅ Implemented |
| `ARRAY_FLATTEN(arr)` | Flatten nested arrays | ✅ Implemented |
| `ARRAY_JOIN(arr, delimiter)` | Join to string | ✅ Implemented |
| `ARRAY_REVERSE(arr)` | Reverse array order | ✅ Implemented |
| `ARRAY_SLICE(arr, start, end)` | Extract portion of array | ✅ Implemented |
| `ARRAY_SORT(arr, property)` | Sort array by property | ✅ Implemented |

**Example usage:**
```sql
-- Filter active users by status property
DECLARE active_users ARRAY = ARRAY_FILTER(users, 'status', 'active');

-- Map to extract names
DECLARE names ARRAY = ARRAY_MAP(users, 'name');

-- Sum all values
DECLARE total NUMBER = ARRAY_REDUCE(numbers, 0);

-- Find first admin
DECLARE admin DOCUMENT = ARRAY_FIND(users, 'role', 'admin');

-- Join array to string
DECLARE csv STRING = ARRAY_JOIN(names, ', ');

-- Flatten nested arrays
DECLARE flat ARRAY = ARRAY_FLATTEN([[1, 2], [3, 4]]);  -- [1, 2, 3, 4]

-- Reverse array
DECLARE reversed ARRAY = ARRAY_REVERSE([1, 2, 3]);  -- [3, 2, 1]

-- Slice array
DECLARE subset ARRAY = ARRAY_SLICE([1, 2, 3, 4, 5], 1, 4);  -- [2, 3, 4]

-- Sort by property
DECLARE sorted ARRAY = ARRAY_SORT(users, 'age');

-- Check conditions
DECLARE all_active BOOLEAN = ARRAY_EVERY(users, 'active', true);
DECLARE any_admin BOOLEAN = ARRAY_SOME(users, 'role', 'admin');
```

---

### Phase 6: String Interpolation ✅ Complete

| Feature | Description | Status |
|---------|-------------|--------|
| `${var}` interpolation | Template strings: `"User ${name} has ${count} items"` | ✅ Implemented |

**Usage (double-quoted strings only):**
```sql
-- Variable interpolation
DECLARE name STRING = 'Alice';
DECLARE greeting STRING = "Hello, ${name}!";  -- "Hello, Alice!"

-- Expression interpolation
DECLARE x NUMBER = 10;
DECLARE y NUMBER = 5;
DECLARE msg STRING = "Sum: ${x + y}";  -- "Sum: 15.0"

-- Ternary in interpolation
DECLARE score NUMBER = 85;
DECLARE result STRING = "Result: ${score >= 60 ? 'Pass' : 'Fail'}";  -- "Result: Pass"

-- Consecutive interpolations
DECLARE a STRING = 'A';
DECLARE b STRING = 'B';
DECLARE combined STRING = "${a}${b}";  -- "AB"

-- Single-quoted strings do NOT interpolate
DECLARE noInterp STRING = 'Hello ${name}';  -- "Hello ${name}" (literal)
```

**Note:** Numbers interpolate with decimal notation (e.g., `42` becomes `"42.0"`).

---

### Phase 7: Lambda Expressions ✅ Complete

| Feature | Description | Status |
|---------|-------------|--------|
| Lambda syntax | Anonymous functions: `(x) => x * 2` | ✅ Implemented |
| ARRAY_MAP with lambda | Transform elements | ✅ Implemented |
| ARRAY_FILTER with lambda | Filter with predicate | ✅ Implemented |

**Usage:**
```sql
-- Double all numbers
DECLARE doubled ARRAY = ARRAY_MAP([1, 2, 3], (x) => x * 2);  -- [2.0, 4.0, 6.0]

-- Filter even numbers
DECLARE evens ARRAY = ARRAY_FILTER([1, 2, 3, 4, 5, 6], (x) => x % 2 == 0);  -- [2.0, 4.0, 6.0]

-- Extract property from objects
DECLARE names ARRAY = ARRAY_MAP(users, (u) => u['name']);

-- Filter by condition
DECLARE adults ARRAY = ARRAY_FILTER(users, (u) => u['age'] >= 18);

-- Use outer scope variables in lambdas
DECLARE threshold NUMBER = 100;
DECLARE large ARRAY = ARRAY_FILTER(values, (x) => x > threshold);

-- Chain operations
DECLARE evens ARRAY = ARRAY_FILTER(numbers, (x) => x % 2 == 0);
DECLARE doubled ARRAY = ARRAY_MAP(evens, (x) => x * 2);
```

**Note:** For property-based filtering (without lambda), use `ARRAY_FILTER_BY`:
```sql
DECLARE active ARRAY = ARRAY_FILTER_BY(users, 'status', 'active');
```

---

### Phase 8: Enhanced ESQL Integration ✅ Complete

| Feature | Description | Status |
|---------|-------------|--------|
| Inline ESQL expressions | `FOR log IN (FROM logs \| LIMIT 10) LOOP` | ✅ Implemented |
| Parameterized ESQL | Safe parameter binding with `:param` | ✅ Implemented |
| FROM function() | Use function results as ESQL sources | ✅ Implemented |

**Usage:**
```sql
-- Inline ESQL (no CURSOR declaration needed)
FOR log IN (FROM logs | WHERE level == 'error' | LIMIT 10) LOOP
    PRINT log['message'];
END LOOP

-- With aggregations
FOR result IN (FROM sales | STATS total = SUM(amount) BY category) LOOP
    PRINT result['category'] || ': $' || result['total'];
END LOOP

-- With sorting and filtering
FOR user IN (FROM users | WHERE status == 'active' | SORT created_at DESC | LIMIT 5) LOOP
    PRINT user['email'];
END LOOP

-- With KEEP and EVAL
FOR order IN (FROM orders | EVAL total = quantity * price | KEEP product, total | WHERE total > 100) LOOP
    PRINT order['product'] || ' - $' || order['total'];
END LOOP

-- Variable substitution with :varName
DECLARE min_score NUMBER = 80;
FOR student IN (FROM students | WHERE score >= :min_score) LOOP
    PRINT student['name'] || ': ' || student['score'];
END LOOP
```

**Benefits:**
- No separate CURSOR declaration needed
- Cleaner, more readable code
- Direct inline ESQL with full query capabilities
- Supports all ESQL operators: WHERE, SORT, LIMIT, EVAL, STATS, KEEP, etc.
- Variable substitution with `:varName` syntax

**FROM function() - Function Results as Data Sources:**

Use any function that returns an array/list as an ESQL-like data source:

```sql
-- Use introspection functions as data sources
FOR func IN (FROM ESCRIPT_FUNCTIONS() | LIMIT 10) LOOP
    PRINT func['name'];
END LOOP

-- Filter function results with WHERE
FOR v IN (FROM ESCRIPT_VARIABLES() | WHERE type == 'NUMBER') LOOP
    PRINT v['name'] || ' = ' || v['value'];
END LOOP

-- Use custom functions as data sources
FOR user IN (FROM GET_USERS() | WHERE role == 'admin' | SORT name) LOOP
    PRINT user['name'];
END LOOP

-- Combine with SORT DESC
FOR top IN (FROM GET_SCORES() | SORT score DESC | LIMIT 3) LOOP
    PRINT top['player'] || ': ' || top['score'];
END LOOP

-- Use KEEP to select specific fields
FOR item IN (FROM GET_PRODUCTS() | KEEP name, price | LIMIT 5) LOOP
    PRINT item['name'] || ': $' || item['price'];
END LOOP
```

**Supported FROM function() operations:**
- `WHERE field == 'value'` - Filter by equality
- `SORT field [ASC|DESC]` - Sort results
- `LIMIT n` - Limit number of results
- `KEEP field1, field2` - Select specific fields

---

### Implementation Status Legend

| Symbol | Meaning |
|--------|---------|
| ✅ | Implemented |
| 🚧 | In Progress |
| 🔲 | Planned |
| ❌ | Not Planned |

---

## Distributed Execution Roadmap

### The Problem: Single-Node Execution

Currently, elastic-script executes on the coordinating node that receives the REST request:

```
┌─────────────────────────────────────────────────────────────────┐
│                     Elasticsearch Cluster                        │
│                                                                  │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐  │
│  │  Node 1  │    │  Node 2  │    │  Node 3  │    │  Node 4  │  │
│  │          │    │          │    │          │    │          │  │
│  │ [Shards] │    │ [Shards] │    │ [Shards] │    │ [Shards] │  │
│  └──────────┘    └──────────┘    └──────────┘    └──────────┘  │
│       ▲                                                         │
│       │ ALL elastic-script code runs on ONE node                │
└───────┼─────────────────────────────────────────────────────────┘
        │
   [Client Request]
```

**Limitations:**
- No load balancing across nodes
- Loops execute sequentially on one node
- Data ships to code (not code to data)
- No data locality optimization
- Memory bottleneck on coordinating node

### The Vision: Distributed elastic-script

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Distributed elastic-script                       │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                    Procedure Coordinator                        │ │
│  │  - Parse procedure, identify parallel regions                   │ │
│  │  - Plan distribution, merge results                             │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                               │                                      │
│              ┌────────────────┼────────────────┐                    │
│              ▼                ▼                ▼                    │
│  ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐    │
│  │   Node 1         │ │   Node 2         │ │   Node 3         │    │
│  │ ┌──────────────┐ │ │ ┌──────────────┐ │ │ ┌──────────────┐ │    │
│  │ │ Procedure    │ │ │ │ Procedure    │ │ │ │ Procedure    │ │    │
│  │ │ Executor     │ │ │ │ Executor     │ │ │ │ Executor     │ │    │
│  │ └──────────────┘ │ │ └──────────────┘ │ │ └──────────────┘ │    │
│  │ ┌──────────────┐ │ │ ┌──────────────┐ │ │ ┌──────────────┐ │    │
│  │ │ Local Shards │ │ │ │ Local Shards │ │ │ │ Local Shards │ │    │
│  │ └──────────────┘ │ │ └──────────────┘ │ │ └──────────────┘ │    │
│  └──────────────────┘ └──────────────────┘ └──────────────────┘    │
│                                                                      │
│  ✓ Code ships to data    ✓ Parallel execution    ✓ Linear scaling  │
└─────────────────────────────────────────────────────────────────────┘
```

---

### Level 1: Pipe-Driven Async Execution 🔲

**Goal:** Enable truly async, event-driven execution using ESQL-inspired pipe syntax.

**Design Principle:** Every procedure call is async by default. No special "async" keyword needed. Pipes define what happens next.

**Core Syntax:**
```sql
-- Basic async with continuation
analyze_logs()
| ON_DONE process_result(@result)
| ON_FAIL handle_error(@error);

-- Chain multiple steps
fetch_data()
| ON_DONE transform(@result)
| ON_DONE store(@result)
| ON_FAIL rollback(@error);

-- Named execution for tracking
analyze_logs()
| ON_DONE process_result(@result)
| TRACK AS 'daily-analysis';

-- Fire and forget
analyze_logs();
```

**Execution Control (also pipe-driven):**
```sql
-- Check status
EXECUTION('daily-analysis') | STATUS;

-- Cancel
EXECUTION('daily-analysis') | CANCEL;

-- Retry failed
EXECUTION('daily-analysis') | RETRY;

-- Block and wait (for interactive/notebook use)
EXECUTION('daily-analysis') | WAIT;
```

**Query Executions with ESQL:**
```sql
-- Executions are ES documents - query them with ESQL!
FROM .escript_executions
| WHERE status == 'RUNNING'
| KEEP name, procedure, progress, started_at
| SORT started_at DESC;

-- Find failures
FROM .escript_executions
| WHERE status == 'FAILED' AND started_at > NOW() - 1 HOUR
| KEEP name, error, started_at;

-- Execution statistics
FROM .escript_executions
| STATS 
    running = COUNT(*) WHERE status == 'RUNNING',
    completed = COUNT(*) WHERE status == 'COMPLETED',
    failed = COUNT(*) WHERE status == 'FAILED';
```

**Advanced Patterns:**
```sql
-- Parallel execution
PARALLEL [fetch_logs(), fetch_metrics(), fetch_traces()]
| ON_ALL_DONE merge_data(@results)
| ON_ANY_FAIL handle_partial(@error)
| TRACK AS 'parallel-fetch';

-- Integration with INTENTs
check_service_health('payment-service')
| ON_DONE (health) => {
    IF health['status'] == 'degraded' THEN
        INTENT SCALE_SERVICE FOR 'payment-service';
    END IF
}
| ON_FAIL INTENT ALERT_ONCALL
| TRACK AS 'health-check';

-- Sequential workflow with context
START WITH { incident_id: 'INC-123' }
| DO analyze_incident(@context)
| DO remediate(@context, @result)
| DO verify(@context, @result)
| ON_FAIL rollback(@context, @error)
| TRACK AS 'incident-workflow';
```

**Execution Data Model:**
```json
// .escript_executions/exec-abc123
{
    "execution_id": "exec-abc123",
    "name": "daily-analysis",
    "procedure": "analyze_logs",
    "status": "RUNNING",
    "progress": { "percent": 45, "message": "Processing..." },
    "pipeline": {
        "on_done": ["process_result"],
        "on_fail": ["handle_error"]
    },
    "started_at": "2025-01-06T10:00:00Z",
    "node": "node-1",
    "result": null,
    "error": null
}
```

**Implementation:**
- Executions stored in `.escript_executions` index
- Execution Listener (persistent task) monitors for state changes
- On completion: triggers continuation pipeline
- Fault-tolerant: state survives node restarts
- Queryable: use ESQL to monitor and analyze executions

**Syntax Reference:**

| Pattern | Description |
|---------|-------------|
| `proc() \| ON_DONE handler(@result)` | Success continuation |
| `proc() \| ON_FAIL handler(@error)` | Error continuation |
| `proc() \| FINALLY cleanup()` | Always runs |
| `proc() \| TRACK AS 'name'` | Named for tracking |
| `proc() \| TIMEOUT 60` | Execution timeout |
| `EXECUTION('name') \| STATUS` | Get status |
| `EXECUTION('name') \| CANCEL` | Cancel |
| `EXECUTION('name') \| RETRY` | Retry failed |
| `EXECUTION('name') \| WAIT` | Block until done |
| `PARALLEL [...] \| ON_ALL_DONE` | Parallel execution |

**Benefits:**
- ESQL-native: Same pipe syntax users know
- Truly async: No blocking, no timeout guessing
- Queryable: Executions are ES documents
- Fault-tolerant: State persists in Elasticsearch
- Composable: Pipes chain naturally

#### Why Pipe-Driven? The Philosophy

The pipe-driven async model isn't just syntax - it's a fundamental design choice that makes elastic-script unique:

**1. The Pipe is the Universal Primitive**
ESQL users already think in pipes. Data flows through transformations. In elastic-script, *execution itself* flows through continuation pipes. This creates a unified mental model where everything - data and execution - flows through pipes.

**2. Execution as Data**
By storing executions in `.escript_executions`, we treat execution state as first-class data. This means:
- Query executions with ESQL (no new query language)
- Build dashboards over execution history
- Analyze patterns in procedure performance
- Alert on execution anomalies using standard Elastic Stack

**3. Composition Over Configuration**
Traditional async programming requires explicit configuration: thread pools, timeouts, retry policies. In the pipe model:
```sql
-- Retry is just another pipe operation
risky_operation()
| ON_FAIL RETRY WITH backoff = 'exponential', max_attempts = 3
| ON_FAIL escalate(@error);
```

**4. The Language is the Orchestrator**
Other systems need separate workflow engines (Airflow, Temporal, Step Functions) to orchestrate async work. In elastic-script, the language IS the orchestrator. No context switching between code and workflow definition.

**5. Observability is Built In**
Every execution is observable by default. No instrumentation needed. Want to know what's running? Query the index. Want historical trends? ESQL aggregate. Want alerts? Elastic Alerting rules on the execution index.

```sql
-- Example: Build an execution health dashboard
FROM .escript_executions
| STATS 
    avg_duration = AVG(duration_ms),
    p99_duration = PERCENTILE(duration_ms, 99),
    failure_rate = COUNT(*) WHERE status == 'FAILED' / COUNT(*) * 100
| BY procedure;
```

This is what makes elastic-script different: **the pipe isn't just syntax, it's an architecture**.

---

### Level 2: Parallel Loop Execution 🔲

**Goal:** Distribute loop iterations across cluster nodes.

**New Syntax:**
```sql
-- Parallel execution across nodes
FOR PARALLEL item IN large_array LOOP
    PROCESS(item);
END LOOP

-- With explicit partitioning
FOR PARALLEL log IN logs PARTITION BY log['service'] LOOP
    -- All logs for same service go to same node
    PROCESS(log);
END LOOP

-- Control parallelism
FOR PARALLEL item IN data PARALLEL DEGREE 8 LOOP
    PROCESS(item);
END LOOP
```

**Implementation:**
- Coordinator splits data into partitions
- Each partition sent to a node via transport action
- Results merged at coordinator
- Fault tolerance: retry failed partitions

**Benefits:**
- Linear speedup with cluster size
- Automatic work distribution
- Configurable parallelism

---

### Level 3: Data-Local Execution 🔲

**Goal:** Ship code to data, not data to code.

**New Syntax:**
```sql
-- Execute on node holding specific routing key
EXECUTE LOCAL ON INDEX logs WITH ROUTING 'user-123'
    analyze_user_behavior('user-123');

-- Execute on all shards, aggregate results
DECLARE results ARRAY = EXECUTE ON ALL SHARDS OF logs
    count_errors_per_shard();

-- Execute on specific node
EXECUTE ON NODE 'node-3'
    process_local_data();
```

**Implementation:**
- Route procedure to node with relevant shards
- Similar to how search requests are routed
- Minimize data transfer over network

**Benefits:**
- Eliminate network transfer of large datasets
- Leverage data locality
- Reduce memory pressure on coordinator

---

### Level 4: Map-Reduce Primitives 🔲

**Goal:** Native distributed aggregation patterns.

**New Syntax:**
```sql
-- Map-Reduce pattern
DECLARE result DOCUMENT = MAP_REDUCE(
    -- Source: distributed across shards
    SOURCE (FROM logs | WHERE level == 'error'),
    
    -- Map: runs on each shard
    MAP (log) => {
        "service": log['service'],
        "count": 1
    },
    
    -- Reduce: merges shard results
    REDUCE (a, b) => {
        "service": a['service'],
        "count": a['count'] + b['count']
    }
);

-- Full aggregation pipeline
DECLARE stats DOCUMENT = PARALLEL_AGGREGATE(
    SOURCE (FROM metrics),
    INIT () => {"sum": 0, "count": 0},
    ACCUMULATE (state, doc) => {
        "sum": state['sum'] + doc['value'],
        "count": state['count'] + 1
    },
    COMBINE (a, b) => {
        "sum": a['sum'] + b['sum'],
        "count": a['count'] + b['count']
    },
    FINALIZE (state) => {
        "average": state['sum'] / state['count']
    }
);
```

**Implementation:**
- Similar to `scripted_metric` aggregation
- Map phase runs on data nodes
- Reduce phase runs on coordinator
- Leverage existing aggregation framework

---

### Level 5: Streaming Execution 🔲

**Goal:** Process large datasets without full materialization.

**New Syntax:**
```sql
-- Chunked streaming
FOR STREAM chunk IN (FROM logs | WHERE level == 'error') 
    CHUNK SIZE 1000 
    PARALLEL DEGREE 4 
LOOP
    DECLARE processed ARRAY = ARRAY_MAP(chunk, log => analyze(log));
    EMIT processed;  -- Emit results without waiting
END LOOP

-- Time-windowed streaming
FOR STREAM window IN (FROM metrics) 
    WINDOW TUMBLING 5 MINUTES 
LOOP
    DECLARE avg NUMBER = compute_average(window);
    IF avg > threshold THEN
        INTENT ALERT FOR window[0]['service'];
    END IF
END LOOP
```

**Benefits:**
- Handle datasets larger than memory
- Continuous processing
- Real-time windowed analytics

---

### Level 6: Cross-Cluster Execution 🔲

**Goal:** Federated procedure execution across clusters.

**New Syntax:**
```sql
-- Execute on remote cluster
DECLARE remote_result = EXECUTE ON CLUSTER 'us-west'
    analyze_regional_data('us-west');

-- Parallel across clusters
DECLARE results ARRAY = EXECUTE PARALLEL ON CLUSTERS ['us-west', 'us-east', 'eu-central']
    get_regional_stats();

-- Aggregate cross-cluster
DECLARE global_stats = REDUCE(results, merge_stats);
```

**Implementation:**
- Leverage cross-cluster search infrastructure
- Serialize procedure source for remote execution
- Handle cluster connectivity failures

---

### New Language Constructs Summary

| Construct | Level | Description |
|-----------|-------|-------------|
| `SUBMIT ASYNC` | 1 | Background execution, returns task ID |
| `AWAIT task_id` | 1 | Wait for async task completion |
| `TASK_STATUS()` | 1 | Query task progress |
| `CANCEL TASK` | 1 | Cancel running task |
| `FOR PARALLEL` | 2 | Parallel loop across nodes |
| `PARTITION BY` | 2 | Control data distribution |
| `PARALLEL DEGREE` | 2 | Control parallelism level |
| `EXECUTE LOCAL` | 3 | Data-local execution |
| `EXECUTE ON NODE` | 3 | Target specific node |
| `EXECUTE ON ALL SHARDS` | 3 | Shard-level execution |
| `MAP_REDUCE()` | 4 | Distributed map-reduce |
| `PARALLEL_AGGREGATE()` | 4 | Full aggregation pipeline |
| `FOR STREAM` | 5 | Streaming execution |
| `CHUNK SIZE` | 5 | Control chunk sizes |
| `WINDOW TUMBLING` | 5 | Time-windowed processing |
| `EXECUTE ON CLUSTER` | 6 | Cross-cluster execution |

---

### Testing Strategy for Distributed Execution

#### Test Environment

**Multi-Node Local Cluster:**
```bash
# Start 3-node cluster for testing
./gradlew :x-pack:plugin:elastic-script:integTestCluster \
    -Dtests.cluster.nodes=3 \
    -Dtests.cluster.name=distributed-test
```

**Docker Compose Setup:**
```yaml
# docker-compose.yml for distributed testing
version: '3.8'
services:
  es01:
    image: elasticsearch:9.0.0
    environment:
      - node.name=es01
      - cluster.name=distributed-test
      - discovery.seed_hosts=es02,es03
      - cluster.initial_master_nodes=es01,es02,es03
    ports:
      - 9200:9200

  es02:
    image: elasticsearch:9.0.0
    environment:
      - node.name=es02
      - cluster.name=distributed-test
      - discovery.seed_hosts=es01,es03
      - cluster.initial_master_nodes=es01,es02,es03

  es03:
    image: elasticsearch:9.0.0
    environment:
      - node.name=es03
      - cluster.name=distributed-test
      - discovery.seed_hosts=es01,es02
      - cluster.initial_master_nodes=es01,es02,es03
```

#### Functional Tests

**1. Task Distribution Tests:**
```java
public class DistributedExecutionTests extends ESIntegTestCase {
    
    @Override
    protected int numberOfNodes() {
        return 3;  // 3-node cluster
    }
    
    public void testAsyncTaskDistribution() {
        // Submit multiple async tasks
        // Verify tasks are distributed across nodes
        // Check task status and results
    }
    
    public void testParallelLoopDistribution() {
        // Create large dataset
        // Execute FOR PARALLEL loop
        // Verify work distributed across nodes
        // Verify correct merged results
    }
    
    public void testDataLocalExecution() {
        // Create index with specific routing
        // Execute LOCAL procedure
        // Verify execution on correct node
    }
}
```

**2. Correctness Tests:**
```java
public void testParallelResultsMatchSequential() {
    // Execute same operation sequentially
    Object sequentialResult = executeSequential(procedure, data);
    
    // Execute same operation in parallel
    Object parallelResult = executeParallel(procedure, data);
    
    // Results must be identical
    assertEquals(sequentialResult, parallelResult);
}

public void testMapReduceCorrectness() {
    // Known dataset with expected aggregation
    // Execute MAP_REDUCE
    // Verify result matches expected
}
```

#### Performance Benchmarks

**1. Baseline Measurement (Single Node):**
```java
public class DistributedPerformanceTests extends ESIntegTestCase {
    
    public void benchmarkSingleNodeBaseline() {
        // Run procedure on 1 node
        // Record: execution time, memory usage, CPU
        long singleNodeTime = runOnSingleNode(procedure, largeDataset);
        
        // Store as baseline for comparison
        recordBaseline("single_node", singleNodeTime);
    }
}
```

**2. Scaling Tests:**
```java
public void benchmarkScalingEfficiency() {
    int[] nodeCounts = {1, 2, 3, 4, 6, 8};
    
    for (int nodes : nodeCounts) {
        // Restart cluster with N nodes
        restartClusterWithNodes(nodes);
        
        // Run parallel procedure
        long executionTime = runParallelProcedure(procedure, largeDataset);
        
        // Calculate speedup
        double speedup = baselineTime / executionTime;
        double efficiency = speedup / nodes;
        
        // Log results
        log("Nodes: {}, Time: {}ms, Speedup: {}x, Efficiency: {}%", 
            nodes, executionTime, speedup, efficiency * 100);
        
        // Assert minimum efficiency (e.g., 70%)
        assertTrue("Efficiency should be >= 70%", efficiency >= 0.70);
    }
}
```

**3. Comparative Benchmarks:**
```java
public void compareSequentialVsParallel() {
    // Dataset sizes to test
    int[] sizes = {1000, 10000, 100000, 1000000};
    
    for (int size : sizes) {
        Object data = generateTestData(size);
        
        // Sequential execution
        long seqStart = System.nanoTime();
        executeSequential(procedure, data);
        long seqTime = System.nanoTime() - seqStart;
        
        // Parallel execution (3 nodes)
        long parStart = System.nanoTime();
        executeParallel(procedure, data);
        long parTime = System.nanoTime() - parStart;
        
        // Calculate and assert improvement
        double improvement = (double) seqTime / parTime;
        log("Size: {}, Sequential: {}ms, Parallel: {}ms, Improvement: {}x",
            size, seqTime/1e6, parTime/1e6, improvement);
        
        // For large datasets, parallel should be faster
        if (size >= 10000) {
            assertTrue("Parallel should be faster for large datasets", 
                improvement > 1.5);
        }
    }
}
```

#### Performance Metrics to Track

| Metric | Description | Target |
|--------|-------------|--------|
| **Speedup** | Sequential time / Parallel time | > 2x for 3 nodes |
| **Efficiency** | Speedup / Number of nodes | > 70% |
| **Latency Overhead** | Added latency for distribution | < 50ms |
| **Memory Distribution** | Memory usage per node | Even distribution |
| **Network Transfer** | Data moved between nodes | Minimize |

#### Benchmark Reporting

```java
public class DistributedBenchmarkReport {
    
    public void generateReport() {
        System.out.println("=== Distributed Execution Benchmark Report ===\n");
        
        System.out.println("## Cluster Configuration");
        System.out.println("- Nodes: " + getNodeCount());
        System.out.println("- Shards: " + getShardCount());
        System.out.println("- Dataset Size: " + getDatasetSize());
        
        System.out.println("\n## Performance Results");
        System.out.println("| Nodes | Time (ms) | Speedup | Efficiency |");
        System.out.println("|-------|-----------|---------|------------|");
        for (BenchmarkResult r : results) {
            System.out.printf("| %d | %d | %.2fx | %.1f%% |\n",
                r.nodes, r.timeMs, r.speedup, r.efficiency * 100);
        }
        
        System.out.println("\n## Memory Usage");
        for (NodeStats n : nodeStats) {
            System.out.printf("- %s: %d MB (%.1f%% of total)\n",
                n.name, n.memoryMB, n.percentage);
        }
    }
}
```

#### CI/CD Integration

```yaml
# .github/workflows/distributed-tests.yml
name: Distributed Execution Tests

on:
  push:
    paths:
      - 'elastic-script/src/main/java/**/distributed/**'
      
jobs:
  distributed-tests:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Start 3-node cluster
        run: docker-compose -f docker-compose.test.yml up -d
        
      - name: Wait for cluster health
        run: |
          for i in {1..30}; do
            curl -s http://localhost:9200/_cluster/health | grep -q '"status":"green"' && break
            sleep 2
          done
          
      - name: Run distributed tests
        run: ./gradlew :x-pack:plugin:elastic-script:distributedTest
        
      - name: Run performance benchmarks
        run: ./gradlew :x-pack:plugin:elastic-script:benchmarkDistributed
        
      - name: Upload benchmark results
        uses: actions/upload-artifact@v3
        with:
          name: benchmark-results
          path: build/reports/benchmark/
```

---

### Implementation Priority

| Priority | Level | Feature | Effort | Impact |
|----------|-------|---------|--------|--------|
| 1 | 1 | Task-Based Async | Medium | High - Foundation for all |
| 2 | 2 | FOR PARALLEL | High | Very High - Biggest win |
| 3 | 3 | Data-Local Execution | Medium | High - Network savings |
| 4 | 4 | MAP_REDUCE | Medium | Medium - Specific use cases |
| 5 | 5 | Streaming | High | Medium - Large datasets |
| 6 | 6 | Cross-Cluster | High | Medium - Federated ops |

---

## See Also
- [ESQL Documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/esql.html)
- [Elasticsearch Inference API](https://www.elastic.co/guide/en/elasticsearch/reference/current/inference-apis.html)
- [Elasticsearch ML](https://www.elastic.co/guide/en/elasticsearch/reference/current/ml-apis.html)
- [Elasticsearch Task Management](https://www.elastic.co/guide/en/elasticsearch/reference/current/tasks.html)
