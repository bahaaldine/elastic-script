# Roadmap

Current status and future direction for elastic-script — a procedural language for Elasticsearch inspired by Oracle PL/SQL.

---

## ✅ Completed Features (v1.0)

### Core Language
- [x] **Procedure Creation** - `CREATE PROCEDURE ... END PROCEDURE`
- [x] **Variable System** - `DECLARE`, `VAR`, `CONST` with type inference
- [x] **Control Flow** - IF/THEN/ELSEIF/ELSE, FOR loops, WHILE loops
- [x] **Data Types** - STRING, NUMBER, BOOLEAN, ARRAY, DOCUMENT, DATE
- [x] **Functions with Parameters** - IN/OUT/INOUT parameter modes

### Built-in Functions (106 total)
- [x] **String Functions** (18) - LENGTH, SUBSTR, REPLACE, REGEXP_*, etc.
- [x] **Number Functions** (11) - ABS, ROUND, SQRT, LOG, etc.
- [x] **Array Functions** (18) - ARRAY_LENGTH, APPEND, FILTER, MAP, etc.
- [x] **Date Functions** (8) - CURRENT_DATE, DATE_ADD, EXTRACT_*, etc.
- [x] **Document Functions** (6) - DOCUMENT_GET, KEYS, VALUES, MERGE, etc.
- [x] **Elasticsearch Functions** (5) - ESQL_QUERY, ES_GET, ES_INDEX, etc.
- [x] **AI/LLM Functions** (6) - LLM_COMPLETE, LLM_SUMMARIZE, ES_INFERENCE
- [x] **Integration Functions** (~30) - Slack, PagerDuty, K8s, AWS, HTTP

### Async Execution Model
- [x] **Pipe-Driven Syntax** - `procedure() | ON_DONE handler(@result)`
- [x] **Error Continuations** - `ON_FAIL`, `FINALLY` handlers
- [x] **Parallel Execution** - `PARALLEL [proc1(), proc2()] | ON_ALL_DONE`
- [x] **Execution Control** - `EXECUTION('name') | STATUS/CANCEL/RETRY`
- [x] **State Persistence** - Execution state stored in `.escript_executions`

### Developer Experience
- [x] **Quick Start Script** - `./scripts/quick-start.sh` for one-command setup
- [x] **Jupyter Integration** - Custom kernel for interactive development
- [x] **Sample Notebooks** - 6 comprehensive tutorial notebooks
- [x] **E2E Test Framework** - Automated notebook execution with HTML reports
- [x] **GitHub Pages Documentation** - Full documentation site

---

## 📊 Feature Gap Analysis (PL/SQL Comparison)

The table below compares elastic-script to Oracle PL/SQL and identifies missing features:

| Category | Feature | PL/SQL | elastic-script | Priority |
|----------|---------|--------|----------------|----------|
| **Error Handling** | TRY/CATCH blocks | ✅ | ❌ | 🔴 P0 |
| | Named exceptions | ✅ | ❌ | 🟡 P1 |
| | RAISE/THROW | ✅ | ❌ | 🔴 P0 |
| **Functions** | User-defined functions | ✅ | ❌ | 🔴 P0 |
| | Function overloading | ✅ | ❌ | 🟢 P2 |
| | Recursive functions | ✅ | ❓ | 🟡 P1 |
| **Cursors** | Explicit cursors | ✅ | ❌ | 🔴 P0 |
| | FETCH INTO | ✅ | ❌ | 🔴 P0 |
| | BULK COLLECT | ✅ | ❌ | 🔴 P0 |
| **Modules** | Packages | ✅ | ❌ | 🟡 P1 |
| | Package state | ✅ | ❌ | 🟡 P1 |
| | Public/Private | ✅ | ❌ | 🟡 P1 |
| **Events** | Triggers | ✅ | ❌ | 🔴 P0 |
| | Scheduled jobs | ✅ | ❌ | 🔴 P0 |
| **Collections** | Associative arrays | ✅ | ❌ | 🔴 P0 |
| | User-defined types | ✅ | ❌ | 🟡 P1 |
| **Dynamic** | EXECUTE IMMEDIATE | ✅ | ❌ | 🔴 P0 |
| | Bind variables | ✅ | ❌ | 🔴 P0 |
| **Bulk Ops** | FORALL | ✅ | ❌ | 🔴 P0 |
| | SAVE EXCEPTIONS | ✅ | ❌ | 🟡 P1 |
| **Security** | GRANT/REVOKE | ✅ | ❌ | 🟡 P1 |
| | AUTHID | ✅ | ❌ | 🟡 P1 |
| **Debug** | Profiler | ✅ | ❌ | 🟡 P1 |
| | Breakpoints | ✅ | ❌ | 🟢 P2 |

---

## 🚧 Phase 1: Core Language Completeness (Q1-Q2 2026)

### 1.1 Exception Handling (TRY/CATCH)

**Status:** 🔴 Not Started | **Priority:** P0

Full exception handling with named exceptions and propagation.

```sql
TRY
    SET result = HTTP_GET('https://api.example.com/data')
    SET parsed = JSON_PARSE(result)
CATCH http_error
    PRINT 'HTTP call failed: ' || @error.message
    CALL log_error(@error)
CATCH parse_error
    PRINT 'JSON parsing failed'
    SET parsed = {}
FINALLY
    -- Always runs (cleanup)
    CALL close_connections()
END TRY
```

**Key Features:**

- Named exception types (`http_error`, `division_error`, `timeout_error`)
- `@error` binding with `message`, `code`, `stack_trace`
- `RAISE` statement to throw custom exceptions
- Exception propagation through procedure calls
- `FINALLY` block for cleanup (always runs)

---

### 1.2 User-Defined Functions (CREATE FUNCTION)

**Status:** 🔴 Not Started | **Priority:** P0

Distinguish functions (return values) from procedures (side effects).

```sql
-- Define a function that returns a value
CREATE FUNCTION calculate_severity(error_count NUMBER, warn_count NUMBER) 
RETURNS STRING AS
BEGIN
    DECLARE score NUMBER = error_count * 10 + warn_count
    
    IF score > 100 THEN
        RETURN 'critical'
    ELSIF score > 50 THEN
        RETURN 'high'
    ELSIF score > 20 THEN
        RETURN 'medium'
    ELSE
        RETURN 'low'
    END IF
END FUNCTION

-- Usage: Functions can be used in expressions
SET severity = calculate_severity(errors, warnings)
SET message = 'Status: ' || calculate_severity(5, 10)
```

**Key Differences from Procedures:**

| Aspect | PROCEDURE | FUNCTION |
|--------|-----------|----------|
| Returns value | No (OUT params only) | Yes (RETURN statement) |
| Use in expressions | No | Yes |
| Side effects | Expected | Discouraged |
| Call syntax | `CALL proc()` | `func()` in expressions |

---

### 1.3 Dynamic ES|QL (EXECUTE IMMEDIATE)

**Status:** 🔴 Not Started | **Priority:** P0

Build and execute queries dynamically at runtime.

```sql
-- Build query dynamically based on conditions
DECLARE query STRING = 'FROM logs-*'

IF severity_filter IS NOT NULL THEN
    SET query = query || ' | WHERE level = ''' || severity_filter || ''''
END IF

IF service_filter IS NOT NULL THEN
    SET query = query || ' | WHERE service = ''' || service_filter || ''''
END IF

SET query = query || ' | LIMIT ' || max_results

-- Execute the dynamic query
EXECUTE IMMEDIATE query INTO results

-- With bind variables (SQL injection safe)
EXECUTE IMMEDIATE 
    'FROM logs-* | WHERE service = :svc AND level = :lvl | LIMIT :lim'
    USING service_name, 'ERROR', 100
    INTO results
```

**Safety Features:**

- Bind variables prevent injection attacks
- Query validation before execution
- Clear error messages for syntax errors

---

### 1.4 Associative Arrays (MAP Type)

**Status:** 🔴 Not Started | **Priority:** P0

Key-value data structures for counting, grouping, and caching.

```sql
-- Declare a map
DECLARE error_counts MAP<STRING, NUMBER> = {}
DECLARE cache MAP<STRING, DOCUMENT> = {}

-- Add/update entries
SET error_counts['api-service'] = 42
SET error_counts['db-service'] = (error_counts['db-service'] ?? 0) + 1

-- Check existence
IF error_counts.CONTAINS('api-service') THEN
    PRINT 'API has ' || error_counts['api-service'] || ' errors'
END IF

-- Iterate over map
FOR service, count IN error_counts LOOP
    IF count > 10 THEN
        CALL alert_team(service, count)
    END IF
END LOOP

-- Map methods
PRINT 'Services with errors: ' || error_counts.KEYS()
PRINT 'Total errors: ' || ARRAY_SUM(error_counts.VALUES())
```

---

## 🚧 Phase 2: Scale & Performance (Q2-Q3 2026)

### 2.1 Cursor Management & Streaming

**Status:** 🔴 Not Started | **Priority:** P0

Handle large result sets without memory exhaustion.

```sql
-- Explicit cursor for large datasets
DECLARE CURSOR log_cursor FOR 
    FROM logs-* 
    | WHERE @timestamp > NOW() - 1 HOUR 
    | LIMIT 100000

OPEN log_cursor

-- Process in batches
DECLARE batch ARRAY<DOCUMENT>
DECLARE processed NUMBER = 0

WHILE FETCH log_cursor LIMIT 1000 INTO batch LOOP
    -- Process batch
    FOR doc IN batch LOOP
        CALL process_log(doc)
    END LOOP
    
    SET processed = processed + ARRAY_LENGTH(batch)
    PRINT 'Processed: ' || processed || ' documents'
    
    -- Optional: yield control for long-running operations
    IF processed % 10000 = 0 THEN
        COMMIT WORK  -- Checkpoint progress
    END IF
END LOOP

CLOSE log_cursor
```

**Cursor Features:**

| Feature | Description |
|---------|-------------|
| `OPEN cursor` | Initialize and execute query |
| `FETCH cursor INTO var` | Get next row |
| `FETCH cursor LIMIT n INTO arr` | Get next n rows as array |
| `CLOSE cursor` | Release resources |
| `cursor%ROWCOUNT` | Number of rows fetched |
| `cursor%NOTFOUND` | True when no more rows |

---

### 2.2 Bulk Operations (FORALL)

**Status:** 🔴 Not Started | **Priority:** P0

Efficient batch processing with error handling.

```sql
-- Bulk collect from query
DECLARE logs ARRAY<DOCUMENT>
BULK COLLECT INTO logs
    FROM logs-*
    | WHERE level = 'ERROR'
    | LIMIT 5000

-- Bulk process with FORALL
FORALL log IN logs
    CALL process_and_archive(log)
    SAVE EXCEPTIONS  -- Continue on individual failures

-- Check for errors
IF @bulk_errors.COUNT > 0 THEN
    PRINT @bulk_errors.COUNT || ' documents failed processing'
    FOR err IN @bulk_errors LOOP
        PRINT 'Index ' || err.index || ': ' || err.message
    END LOOP
END IF

-- Bulk index with retry
FORALL doc IN transformed_docs
    INDEX_DOCUMENT('output-index', doc)
    ON_FAIL RETRY 3 THEN SKIP
```

---

### 2.3 Scheduled Jobs (CREATE JOB)

**Status:** 🔴 Not Started | **Priority:** P0

Built-in job scheduling with cron syntax.

```sql
-- Create a recurring job
CREATE JOB daily_log_cleanup
SCHEDULE '0 2 * * *'  -- 2 AM daily (cron syntax)
TIMEZONE 'UTC'
ENABLED true
AS
BEGIN
    PRINT 'Starting daily cleanup at ' || CURRENT_TIMESTAMP
    
    -- Archive logs older than 30 days
    CALL archive_old_logs(30)
    
    -- Clean up temporary indices
    CALL cleanup_temp_indices()
    
    -- Send daily report
    CALL generate_and_send_report()
    
    PRINT 'Cleanup completed'
END JOB

-- Job management
ALTER JOB daily_log_cleanup DISABLE
ALTER JOB daily_log_cleanup SCHEDULE '0 3 * * *'  -- Change to 3 AM
DROP JOB daily_log_cleanup

-- View job history
SELECT * FROM @job_runs 
    WHERE job_name = 'daily_log_cleanup' 
    ORDER BY start_time DESC 
    LIMIT 10
```

**Schedule Patterns:**

| Pattern | Description |
|---------|-------------|
| `0 * * * *` | Every hour |
| `*/15 * * * *` | Every 15 minutes |
| `0 2 * * *` | Daily at 2 AM |
| `0 0 * * 0` | Weekly on Sunday |
| `0 0 1 * *` | Monthly on 1st |
| `@hourly` | Alias for every hour |
| `@daily` | Alias for midnight daily |

---

### 2.4 Triggers & Event-Driven Execution

**Status:** 🔴 Not Started | **Priority:** P0

React to Elasticsearch events automatically.

```sql
-- Trigger on new documents
CREATE TRIGGER on_critical_error
WHEN DOCUMENT INSERTED INTO logs-*
WHERE level = 'ERROR' AND service IN ('payment', 'auth', 'checkout')
BEGIN
    -- @document contains the new document
    DECLARE doc DOCUMENT = @document
    
    -- Immediate alerting for critical services
    CALL SLACK_SEND(
        '#critical-alerts',
        '🚨 Critical Error in ' || doc.service || ': ' || doc.message
    )
    
    -- Check if this is a pattern
    DECLARE recent_count NUMBER
    SET recent_count = ESQL_QUERY(
        'FROM logs-* 
         | WHERE service = ''' || doc.service || ''' 
         AND level = ''ERROR'' 
         AND @timestamp > NOW() - 5 MINUTES
         | STATS count = COUNT(*)'
    )[0].count
    
    IF recent_count > 10 THEN
        CALL PAGERDUTY_TRIGGER(
            'Error storm in ' || doc.service,
            'critical',
            {'service': doc.service, 'count': recent_count}
        )
    END IF
END TRIGGER

-- Trigger on alert firing (Elasticsearch Alerting integration)
CREATE TRIGGER on_alert_fire
WHEN ALERT 'high-error-rate' FIRES
BEGIN
    -- @alert contains alert context
    CALL escalate_to_oncall(@alert)
END TRIGGER

-- Trigger on index lifecycle events
CREATE TRIGGER on_index_rollover
WHEN INDEX ROLLED OVER IN logs-*
BEGIN
    -- @old_index, @new_index available
    PRINT 'Index rolled over: ' || @old_index || ' -> ' || @new_index
    CALL archive_to_s3(@old_index)
END TRIGGER

-- Trigger management
ALTER TRIGGER on_critical_error DISABLE
DROP TRIGGER on_critical_error
SHOW TRIGGERS
```

**Trigger Event Types:**

| Event | Description | Variables |
|-------|-------------|-----------|
| `DOCUMENT INSERTED INTO index` | New document indexed | `@document` |
| `DOCUMENT UPDATED IN index` | Document updated | `@document`, `@old_document` |
| `DOCUMENT DELETED FROM index` | Document deleted | `@document_id` |
| `ALERT name FIRES` | Elasticsearch alert fires | `@alert` |
| `INDEX ROLLED OVER IN pattern` | ILM rollover | `@old_index`, `@new_index` |
| `INDEX CREATED pattern` | New index created | `@index` |
| `CLUSTER STATUS CHANGED TO status` | Cluster health change | `@status`, `@previous_status` |

---

## 🚧 Phase 3: Enterprise Features (Q3-Q4 2026)

### 3.1 Packages & Modules

**Status:** 🟡 Planned | **Priority:** P1

Organize related procedures and functions into packages.

```sql
-- Package specification (public interface)
CREATE PACKAGE incident_response AS
    -- Public procedures
    PROCEDURE handle_incident(incident_id STRING)
    PROCEDURE escalate(incident_id STRING, level NUMBER)
    PROCEDURE resolve(incident_id STRING, resolution STRING)
    
    -- Public functions
    FUNCTION get_severity(incident_id STRING) RETURNS STRING
    FUNCTION get_oncall() RETURNS STRING
    
    -- Package constants
    CONSTANT DEFAULT_TIMEOUT NUMBER = 300
    CONSTANT ESCALATION_LEVELS ARRAY = ['low', 'medium', 'high', 'critical']
END PACKAGE

-- Package body (implementation)
CREATE PACKAGE BODY incident_response AS
    -- Private state (per-session)
    DECLARE active_incidents MAP<STRING, DOCUMENT> = {}
    
    -- Private helper (not visible outside package)
    PROCEDURE internal_notify(channel STRING, message STRING) AS
    BEGIN
        CALL SLACK_SEND(channel, message)
    END
    
    -- Public procedure implementation
    PROCEDURE handle_incident(incident_id STRING) AS
    BEGIN
        CALL internal_notify('#incidents', 'Handling: ' || incident_id)
        SET active_incidents[incident_id] = {'status': 'in_progress'}
    END
    
    -- Public function implementation
    FUNCTION get_severity(incident_id STRING) RETURNS STRING AS
    BEGIN
        DECLARE incident DOCUMENT
        SET incident = active_incidents[incident_id]
        RETURN incident.severity ?? 'unknown'
    END
END PACKAGE BODY

-- Usage
CALL incident_response.handle_incident('INC-001')
SET sev = incident_response.get_severity('INC-001')
PRINT incident_response.DEFAULT_TIMEOUT
```

---

### 3.2 Security & Access Control

**Status:** 🟡 Planned | **Priority:** P1

Fine-grained access control for procedures and packages.

```sql
-- Grant execute permission
GRANT EXECUTE ON PROCEDURE analyze_logs TO ROLE 'analyst'
GRANT EXECUTE ON PACKAGE incident_response TO ROLE 'sre'

-- Revoke permission
REVOKE EXECUTE ON PROCEDURE delete_old_data FROM ROLE 'analyst'

-- Invoker vs definer rights
CREATE PROCEDURE admin_cleanup()
AUTHID DEFINER  -- Runs with procedure owner's privileges
AS
BEGIN
    -- Can perform admin operations even if caller is limited user
    CALL delete_old_indices()
    CALL vacuum_data()
END

CREATE PROCEDURE user_report()
AUTHID CURRENT_USER  -- Runs with caller's privileges (default)
AS
BEGIN
    -- Limited to what the calling user can access
    CALL generate_report()
END

-- Secure credential reference (no plaintext secrets)
CALL HTTP_POST(
    'https://api.pagerduty.com/incidents',
    headers = {'Authorization': CREDENTIAL('pagerduty_api_key')},
    body = incident_data
)
```

---

### 3.3 Debugging & Profiling

**Status:** 🟡 Planned | **Priority:** P1

Built-in performance analysis and debugging.

```sql
-- Enable profiling for session
SET PROFILING ON

-- Run procedure
CALL complex_data_pipeline()

-- View execution profile
SHOW PROFILE

-- Output:
-- ┌─────────────────────────────────────────────────────────────┐
-- │ Line │ Statement                        │ Time    │ Calls  │
-- ├──────┼──────────────────────────────────┼─────────┼────────┤
-- │ 10   │ SET results = ESQL_QUERY(...)    │ 2.345s  │ 1      │ <-- Bottleneck
-- │ 15   │ FOR doc IN results LOOP          │ 0.523s  │ 1000   │
-- │ 20   │   CALL process_document(doc)     │ 0.412s  │ 1000   │
-- │ 25   │   CALL HTTP_POST(...)            │ 1.890s  │ 1000   │ <-- Bottleneck
-- ├──────┼──────────────────────────────────┼─────────┼────────┤
-- │      │ TOTAL                            │ 5.170s  │        │
-- └─────────────────────────────────────────────────────────────┘

-- Assertions for testing
ASSERT result > 0, 'Result should be positive'
ASSERT response.status = 200, 'HTTP call should succeed'
ASSERT ARRAY_LENGTH(items) <= 100, 'Too many items returned'

-- Debug logging
SET DEBUG ON
-- Shows variable assignments, function calls, branch decisions
```

---

## 🚧 Phase 4: Elasticsearch-Native Features (2027+)

These features leverage Elasticsearch's unique capabilities beyond traditional databases.

### 4.1 Vector Search & ML Integration

```sql
-- Semantic search with embeddings
DECLARE similar_docs ARRAY<DOCUMENT>
VECTOR_SEARCH INTO similar_docs
    FROM knowledge-base
    QUERY_VECTOR LLM_EMBED(user_question)
    FIELD 'embedding'
    K 10
    NUM_CANDIDATES 100

-- RAG (Retrieval Augmented Generation) pattern
DECLARE context STRING = ARRAY_JOIN(
    ARRAY_MAP(similar_docs, d => d.content),
    '\n---\n'
)

SET answer = LLM_COMPLETE(
    'Answer based on context:\n' || context || '\n\nQuestion: ' || user_question
)
```

### 4.2 Index Lifecycle Automation

```sql
-- Programmatic ILM
CREATE PROCEDURE smart_retention(pattern STRING, hot_days NUMBER, warm_days NUMBER) AS
BEGIN
    FOR idx IN (SHOW INDICES pattern) LOOP
        DECLARE age_days NUMBER = DATE_DIFF(NOW(), idx.creation_date, 'days')
        
        IF age_days > hot_days + warm_days THEN
            CALL archive_to_s3(idx.name)
            CALL delete_index(idx.name)
        ELSIF age_days > hot_days THEN
            CALL move_to_warm_tier(idx.name)
        END IF
    END LOOP
END
```

### 4.3 Cross-Cluster Operations

```sql
-- Query remote clusters
DECLARE remote_errors ARRAY<DOCUMENT>
FROM cluster:us-west/logs-* | WHERE level = 'ERROR' INTO remote_errors

-- Aggregate across clusters
DECLARE global_stats DOCUMENT
AGGREGATE INTO global_stats
    FROM cluster:*/logs-*
    | STATS total = COUNT(*), errors = COUNT(*) WHERE level = 'ERROR'
    BY cluster
```

---

## 🤖 Modernization Framework

### Guiding Principles

The procedural style (`BEGIN/END`, `DECLARE`, `PROCEDURE`) is a **strength** — familiar to database developers, SREs, and data engineers. Modernization focuses on **capabilities and tooling**, not syntax changes.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        ELASTIC-SCRIPT PRINCIPLES                             │
├─────────────────────────────────────────────────────────────────────────────┤
│   ┌─────────┐  ┌──────────┐  ┌────────────┐  ┌─────────────────────────┐   │
│   │   AI    │  │   EASE   │  │   DATA     │  │    ELASTICSEARCH       │   │
│   │ NATIVE  │  │  OF USE  │  │  DRIVEN    │  │      EVERYTHING        │   │
│   └────┬────┘  └────┬─────┘  └─────┬──────┘  └───────────┬─────────────┘   │
│        └────────────┴──────────────┴──────────────────────┘                 │
│                              │                                               │
│        ┌─────────────────────┼─────────────────────┐                        │
│   ┌────┴─────┐        ┌──────┴──────┐       ┌──────┴──────┐                │
│   │  MODERN  │        │ INTEROPER-  │       │  PLUGGABLE  │                │
│   │   TECH   │        │    ABLE     │       │             │                │
│   └──────────┘        └─────────────┘       └─────────────┘                │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Key Design Decisions

!!! warning "ES|QL is Untouched"
    elastic-script **augments** ES|QL with new commands (like `INTO`, `PROCESS WITH`) — it does **not modify** ES|QL itself. ES|QL remains the standard query language.

!!! info "Leverage Existing Elastic Platform APIs"
    elastic-script is an **orchestration layer** that uses existing Elastic APIs:
    
    - **Agent Builder** → Build and manage AI agents
    - **One Workflow** → Create and execute workflows  
    - **Dashboard-as-Code** → Define dashboards programmatically
    - **Elasticsearch APIs** → Full access to all ES functionality
    - **Kibana APIs** → Saved objects, spaces, features

---

### 1. AI Native

**Vision**: elastic-script is the language AI agents speak to operate Elasticsearch.

#### Agent-First Architecture (via Agent Builder API)

```sql
-- Create agent using Elastic Agent Builder
CREATE AGENT log_analyst
    USING AGENT_BUILDER {
        MODEL 'azure-openai-gpt4'
        CAPABILITIES ['query_logs', 'identify_patterns', 'summarize']
        PROMPT "You are an expert at analyzing log data and identifying anomalies"
        TOOLS [
            PROCEDURE analyze_errors,
            PROCEDURE summarize_trends,
            FUNCTION ESQL_QUERY
        ]
    }

-- Invoke agent
DECLARE analysis = AGENT log_analyst 
    TASK "Analyze payment errors in the last hour and identify patterns"

-- Multi-agent orchestration
CREATE WORKFLOW incident_investigation 
    USING ONE_WORKFLOW AS
BEGIN
    SET analysis = AGENT log_analyst TASK "Investigate errors"
    IF analysis.severity = 'critical' THEN
        AGENT incident_responder TASK "Create P1 incident"
    END IF
END WORKFLOW
```

#### MCP Server (Model Context Protocol)

Enable external AI agents (Claude, GPT, etc.) to operate Elasticsearch:

```
┌─────────────────────────────────────────────────────────────────┐
│                    External AI Agents                            │
│              (Claude, GPT, Custom Agents)                        │
├─────────────────────────────────────────────────────────────────┤
│                    MCP Protocol                                  │
├─────────────────────────────────────────────────────────────────┤
│              elastic-script MCP Server                           │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────────────────┐ │
│  │execute_code  │ │call_procedure│ │ discover_procedures     │ │
│  └──────────────┘ └──────────────┘ └──────────────────────────┘ │
├─────────────────────────────────────────────────────────────────┤
│                    Elasticsearch                                 │
└─────────────────────────────────────────────────────────────────┘
```

| MCP Tool | Description |
|----------|-------------|
| `execute_escript` | Run arbitrary elastic-script code |
| `call_procedure` | Call a stored procedure with arguments |
| `discover_procedures` | List available procedures with descriptions |
| `query_elasticsearch` | Execute ES\|QL queries |

#### Natural Language Interface

```sql
-- Direct natural language execution
EXECUTE "Find all payment errors in the last hour and alert the team"

-- Generate procedures from description
GENERATE PROCEDURE "Monitor payment service, alert on 5+ errors in 5 minutes"
    SAVE AS payment_monitor

-- Semantic procedure discovery
DISCOVER PROCEDURES LIKE "handle incidents"
-- Returns: incident_response, alert_handler, on_call_escalation

-- AI-powered recommendations
RECOMMEND PROCEDURES FOR "I need to set up monitoring for a new microservice"
```

#### Agent Memory & Context

```sql
-- Persistent context across sessions
REMEMBER "The payment service has been unstable since deployment v2.3.1"

-- Recall in procedures
CREATE PROCEDURE smart_alert()
BEGIN
    DECLARE context = RECALL "recent incidents for payment service"
    IF context.has_ongoing_incident THEN
        PRINT "Suppressing alert - ongoing incident exists"
    ELSE
        CALL create_incident()
    END IF
END PROCEDURE
```

---

### 2. Ease of Use

**Vision**: From zero to productive in minutes. Progressive complexity.

#### Smart Defaults & One-Liners

```sql
-- Minimal syntax, sensible defaults
CREATE JOB cleanup SCHEDULE '@daily' AS
    CALL delete_old_logs()
END JOB
-- Defaults: ENABLED true, TIMEZONE UTC

-- One-liners for common tasks
ALERT ON (FROM logs-* | WHERE level = 'ERROR' | STATS count) > 10 
    SEND SLACK '#alerts'

-- Quick monitoring
MONITOR 'payment-service' EVERY 5 MINUTES 
    ALERT IF error_rate > 0.01
```

#### Progressive Disclosure

```sql
-- Level 1: Simple inline
FROM logs-* | WHERE level = 'ERROR' | STATS count

-- Level 2: Named procedure
CREATE PROCEDURE count_errors()
    RETURN FROM logs-* | WHERE level = 'ERROR' | STATS count
END PROCEDURE

-- Level 3: Parameterized with defaults
CREATE PROCEDURE count_errors(time_range STRING DEFAULT '24h')
    RETURN FROM logs-* 
        | WHERE level = 'ERROR' AND @timestamp > NOW() - @time_range
        | STATS count
END PROCEDURE

-- Level 4: Full-featured with security, observability
@description "Counts errors with alerting"
CREATE PROCEDURE count_errors(time_range STRING, alert_threshold NUMBER)
WITH { TRACING ON, MAX_EXECUTION_TIME = 30 SECONDS }
BEGIN
    TRY
        DECLARE count = FROM logs-* | WHERE ... | STATS count
        IF count > alert_threshold THEN CALL alert_team(count) END IF
        RETURN count
    CATCH
        CALL log_error(@error)
        RAISE
    END TRY
END PROCEDURE
```

#### Contextual Help

```sql
-- Inline help
HELP SLACK_SEND
-- Shows: signature, parameters, examples

-- Interactive examples
EXAMPLE "send slack notification"
-- Returns runnable example code

-- Smart error messages
> CALL SLCK_SEND('#alerts', 'test')
-- Error: Unknown function 'SLCK_SEND'. Did you mean 'SLACK_SEND'?
```

---

### 3. Data-Driven (Close to ES|QL)

**Vision**: ES|QL is the native query language. elastic-script augments it, never replaces it.

#### ES|QL Augmentation (Not Modification)

```sql
-- ES|QL is used as-is
DECLARE errors = FROM logs-* | WHERE level = 'ERROR' | LIMIT 100

-- elastic-script ADDS commands that work with ES|QL results
FROM logs-* 
| WHERE level = 'ERROR' 
| INTO my_results                    -- NEW: Store results

FROM logs-* 
| PROCESS WITH analyze_error         -- NEW: Call procedure per row

-- ES|QL in expressions (ES|QL unchanged, elastic-script wraps)
IF (FROM metrics-* | STATS AVG(cpu)) > 80 THEN
    CALL alert_high_cpu()
END IF
```

#### Query Composition

```sql
-- Build queries programmatically (generates valid ES|QL)
CREATE FUNCTION build_log_query(
    indices STRING DEFAULT 'logs-*',
    level STRING DEFAULT NULL,
    service STRING DEFAULT NULL
) RETURNS QUERY AS
BEGIN
    DECLARE q = QUERY FROM @indices
    IF level IS NOT NULL THEN
        SET q = q | WHERE level = @level
    END IF
    IF service IS NOT NULL THEN
        SET q = q | WHERE service = @service
    END IF
    RETURN q
END FUNCTION

-- Execute composed query
DECLARE results = EXECUTE build_log_query(level := 'ERROR') | LIMIT 100
```

#### Schema Awareness

```sql
-- Introspect index mappings (uses ES _mapping API)
DECLARE schema = SCHEMA FOR 'logs-*'
PRINT schema.fields.level.type  -- 'keyword'

-- Validate data against schema
VALIDATE document AGAINST SCHEMA 'logs-*'
```

#### Streaming & Continuous Queries

```sql
-- Continuous query (uses ES async search / PIT)
CREATE STREAM error_monitor AS
    FROM logs-* 
    | WHERE level = 'ERROR'
    | WINDOW TUMBLING 5 MINUTES
    | STATS count BY service
    | EMIT TO PROCEDURE handle_high_errors
```

---

### 4. Everything Elasticsearch

**Vision**: Full access to the Elasticsearch ecosystem via existing APIs.

#### Complete API Coverage

```sql
-- Uses ES Cluster APIs
CLUSTER HEALTH
CLUSTER SETTINGS SET 'cluster.routing.allocation.enable' = 'all'

-- Uses ES Index APIs  
CREATE INDEX 'my-index' WITH MAPPINGS { ... }
REINDEX FROM 'source-*' TO 'dest'

-- Uses ES Alias APIs
CREATE ALIAS 'current' FOR 'logs-2026.01'
SWAP ALIAS 'current' FROM 'logs-2026.01' TO 'logs-2026.02'
```

#### ILM Integration

```sql
-- Uses ES ILM APIs
CREATE ILM POLICY 'logs-policy' AS {
    HOT { ROLLOVER MAX_SIZE '50GB' MAX_AGE '1d' }
    WARM { MIN_AGE '7d', SHRINK NUMBER_OF_SHARDS 1 }
    DELETE { MIN_AGE '90d' }
}

APPLY ILM POLICY 'logs-policy' TO INDEX TEMPLATE 'logs-template'
```

#### Alerting Integration

```sql
-- Uses ES Alerting / Watcher APIs
CREATE ALERT high_error_rate
    TRIGGER SCHEDULE EVERY 5 MINUTES
    INPUT (FROM logs-* | WHERE level = 'ERROR' | STATS count)
    CONDITION result.count > 100
    ACTIONS {
        SLACK '#alerts' MESSAGE 'High error rate: {{count}}'
    }
```

#### ML Integration

```sql
-- Uses ES ML APIs
CREATE ML JOB error_anomaly
    ANALYSIS_CONFIG { DETECTORS [{ FUNCTION 'count' }], BUCKET_SPAN '15m' }
    DATAFEED (FROM logs-* | WHERE level = 'ERROR')

-- Uses ES Inference APIs
DECLARE sentiment = INFER 'sentiment-model' WITH { text: message }
```

#### Ingest Pipeline Integration

```sql
-- Uses ES Ingest APIs
CREATE INGEST PIPELINE 'enrich-logs' AS {
    GROK FIELD 'message' PATTERNS ['%{TIMESTAMP:ts} %{LOGLEVEL:level}']
    ENRICH POLICY 'geo-lookup' FIELD 'ip' TARGET 'geo'
}

INDEX document INTO 'logs' PIPELINE 'enrich-logs'
```

---

### 5. Modern Technologies

**Vision**: Built with and for modern infrastructure.

#### Real-Time Streaming

```sql
-- Uses ES async capabilities
WEBSOCKET NOTIFY '#channel' ON 
    FROM logs-* | WHERE level = 'ERROR' | STATS count > 10

-- Event publishing (to Kafka, etc.)
PUBLISH EVENT 'order.created' TO 'events-topic' WITH order_data
```

#### Container & Kubernetes Native

```sql
-- Uses K8s API
DECLARE pods = K8S_GET_PODS(namespace := 'production')

FOR pod IN pods LOOP
    IF (FROM logs-* | WHERE k8s.pod.name = pod.name | WHERE level = 'ERROR' | STATS count) > 10 THEN
        CALL K8S_RESTART_POD(pod.name)
    END IF
END LOOP
```

#### Serverless Functions

```sql
-- Serverless execution model
CREATE FUNCTION process_webhook(event DOCUMENT)
SERVERLESS
TRIGGER HTTP POST '/webhook/github'
AS
BEGIN
    CALL handle_github_event(event.body)
END FUNCTION
```

#### GitOps & Infrastructure as Code

```sql
-- Procedures stored as files, deployed via CI/CD
-- File: procedures/incident_response.escript

-- Deploy command
DEPLOY FROM GIT 'main'
PREVIEW DEPLOY 'procedures/*.escript'
```

---

### 6. Interoperable

**Vision**: Works with everything. Standards-based.

#### OpenTelemetry Native

```sql
-- Automatic OTEL instrumentation
CREATE PROCEDURE my_operation()
WITH OTEL { SERVICE_NAME 'my-service' }
AS
BEGIN
    -- Traces automatically created and sent to APM
END PROCEDURE

-- Custom spans
OTEL_SPAN 'process-batch' BEGIN
    FOR item IN batch LOOP
        OTEL_METRIC 'items.processed' INCREMENT 1
    END LOOP
END OTEL_SPAN
```

#### Protocol Support

```sql
-- HTTP
DECLARE response = HTTP_GET('https://api.example.com/data')

-- gRPC  
DECLARE response = GRPC_CALL('orders.OrderService/GetOrder', { id: '123' })

-- GraphQL
DECLARE response = GRAPHQL_QUERY('https://api/graphql', 
    'query { user(id: "123") { name } }')

-- Message Queues
KAFKA_PRODUCE('topic', message)
RABBITMQ_PUBLISH('exchange', 'routing.key', message)
```

#### Data Format Support

```sql
-- Parquet (for data interchange)
EXPORT (FROM logs-* | LIMIT 10000) TO PARQUET 's3://bucket/logs.parquet'
IMPORT PARQUET 's3://bucket/data.parquet' INTO 'imported-data'

-- CSV, JSON, YAML, XML
DECLARE records = PARSE_CSV(csv_string)
DECLARE config = PARSE_YAML(yaml_string)
```

#### Cloud Provider Integration

```sql
-- AWS
DECLARE secret = AWS_SECRETS_MANAGER_GET('my-secret')
CALL AWS_LAMBDA_INVOKE('my-function', payload)

-- GCP
CALL GCP_PUBSUB_PUBLISH('projects/.../topics/my-topic', message)

-- Azure
DECLARE secret = AZURE_KEYVAULT_GET('my-secret')
```

---

### 7. Pluggable

**Vision**: Extensible at every layer without forking.

#### Custom Functions

```sql
-- Register custom function (Java)
REGISTER FUNCTION my_custom_function
    CLASS 'com.mycompany.escript.MyFunction'
    JAR 's3://plugins/my-functions.jar'

-- Register from external service
REGISTER FUNCTION external_calc
    HTTP POST 'https://calc.service/compute'
```

#### Function Registries

```sql
-- Connect to function registry
CONNECT REGISTRY 'https://registry.company.com/escript-functions'

-- Install functions from registry
INSTALL FUNCTIONS FROM 'company/data-quality' VERSION '^2.0'
```

#### Middleware / Interceptors

```sql
-- Define middleware
CREATE MIDDLEWARE audit_all
BEFORE EXECUTE ANY PROCEDURE
AS
BEGIN
    CALL log_execution_start(@procedure, @args, @user)
END MIDDLEWARE

AFTER EXECUTE ANY PROCEDURE
AS
BEGIN
    CALL log_execution_end(@procedure, @result, @duration)
END MIDDLEWARE

-- Apply middleware
APPLY MIDDLEWARE audit_all TO ALL PROCEDURES
```

#### Custom Statements (DSL Extensions)

```sql
-- Extend grammar with domain-specific syntax
EXTEND GRAMMAR WITH {
    'MONITOR' service:STRING 'FOR' duration:DURATION 'ALERT' 'IF' condition:EXPRESSION
    => CREATE TRIGGER ... (generates standard elastic-script)
}

-- Use new syntax
MONITOR 'payment-api' FOR 5 MINUTES ALERT IF error_rate > 0.05
```

---

### Security & Governance

Enterprise-grade security controls.

#### Sandboxing & Resource Limits

```sql
CREATE PROCEDURE untrusted_operation()
WITH {
    -- Execution limits
    MAX_EXECUTION_TIME = 60 SECONDS
    MAX_MEMORY = 256MB
    MAX_ES_QUERIES = 100
    MAX_RESULT_SIZE = 10MB
    
    -- Data access restrictions  
    ALLOWED_INDICES = ['logs-*', 'metrics-*']
    DENIED_INDICES = ['security-*', '.kibana*']
    
    -- Network restrictions
    NETWORK = DENY
    -- OR
    ALLOWED_HOSTS = ['api.slack.com', 'api.pagerduty.com']
    
    -- Function restrictions
    DENIED_FUNCTIONS = ['ES_DELETE', 'DROP_INDEX']
}
BEGIN
    -- Runs in sandboxed environment
END PROCEDURE
```

#### Execution Policies

```sql
CREATE POLICY production_safety AS
BEGIN
    REQUIRE MAX_EXECUTION_TIME <= 300 SECONDS
    DENY FUNCTION 'ES_DELETE' UNLESS ROLE IN ('admin')
    DENY INDEX_PATTERN '.security-*'
    REQUIRE APPROVAL FOR FUNCTION 'DROP_INDEX'
END POLICY

APPLY POLICY production_safety TO ROLE 'developer'
```

#### Secrets Management

```sql
-- Reference secrets by name (uses ES keystore or external vault)
CALL HTTP_POST(
    'https://api.pagerduty.com/incidents',
    headers = {'Authorization': SECRET('pagerduty_key')}
)
```

#### Audit Trail & RBAC

```sql
-- Audit logging
SHOW AUDIT LOG FOR PROCEDURE sensitive_operation
    WHERE user = 'john' AND @timestamp > NOW() - 7 DAYS

-- Role-based access
GRANT EXECUTE ON PROCEDURE analyze_logs TO ROLE 'analyst'
REVOKE EXECUTE ON PROCEDURE delete_data FROM ROLE 'developer'
```

---

### Developer Experience

Modern tooling around the procedural language.

#### Language Server Protocol (LSP)

| Feature | Description |
|---------|-------------|
| **Autocomplete** | Procedures, functions, variables, ES\|QL fields |
| **Hover docs** | Function signatures, procedure documentation |
| **Go to definition** | Jump to procedure source |
| **Diagnostics** | Real-time error detection |

Enables: VS Code extension, Cursor extension, JetBrains plugin.

#### Rich Notebook Outputs

```sql
-- Rich output in Jupyter notebooks
DISPLAY TABLE errors WITH { TITLE 'Errors by Service', CHART 'bar' }
DISPLAY CHART { TYPE 'timeseries', DATA query_results }
```

#### Procedure Versioning

```sql
SHOW PROCEDURE HISTORY my_procedure
DIFF PROCEDURE my_procedure VERSION 3 WITH VERSION 5
ROLLBACK PROCEDURE my_procedure TO VERSION 3
```

#### Built-in OpenTelemetry

```sql
CREATE PROCEDURE process_orders()
WITH TRACING ON
BEGIN
    -- Traces automatically created and sent to APM
END PROCEDURE

SHOW PROCEDURE METRICS my_procedure
-- Shows: executions, success rate, P50/P99 duration
```

---

### Modernization Priority

| Strategy | Impact | Effort | Priority |
|----------|--------|--------|----------|
| **MCP Server** | 🔥🔥🔥 | Medium | ⭐ P0 |
| **Agent Builder Integration** | 🔥🔥🔥 | Medium | ⭐ P0 |
| **Sandboxing & Resource Limits** | 🔥🔥🔥 | Medium | ⭐ P0 |
| **Natural Language Interface** | 🔥🔥🔥 | High | P1 |
| **ES\|QL Augmentation (INTO, PROCESS)** | 🔥🔥 | Medium | P1 |
| **One Workflow Integration** | 🔥🔥 | Medium | P1 |
| **Dashboard-as-Code Integration** | 🔥🔥 | Medium | P1 |
| **Secrets Management** | 🔥🔥 | Low | P1 |
| **LSP Implementation** | 🔥🔥 | High | P1 |
| **OpenTelemetry Integration** | 🔥🔥 | Medium | P1 |
| **Custom Function Registry** | 🔥🔥 | High | P2 |
| **Procedure Versioning** | 🔥 | Low | P2 |

---

## 🚀 App Deployment Platform (Vercel-Style)

Deploy data-driven applications directly from elastic-script. Write code, get a deployed app URL.

### Vision

```
┌─────────────────────────────────────────────────────────────────┐
│                     Developer Experience                         │
│  CREATE APP inventory_manager                                    │
│  ROUTE '/inventory'                                              │
│  AS BEGIN                                                        │
│      RENDER EUI.Page { ... }                                     │
│      RENDER DASHBOARD 'inventory-overview'                       │
│  END APP                                                         │
│                            ↓ DEPLOY                              │
├─────────────────────────────────────────────────────────────────┤
│                     EScript App Runtime                          │
│  • Hosts apps at /apps/{app-name}                                │
│  • Renders EUI components                                        │
│  • Embeds Dashboards-as-Code                                     │
│  • Executes elastic-script logic                                 │
│  • Connects to Elasticsearch for data                            │
├─────────────────────────────────────────────────────────────────┤
│                     Elasticsearch                                │
│  .escript_apps      Your Data Indices      .kibana_dashboards   │
└─────────────────────────────────────────────────────────────────┘
```

### Architecture

**Standalone Runtime** (not a Kibana plugin):

- Serves apps at routes like `https://apps.company.com/inventory`
- Uses Kibana's rendering stack (EUI, Elastic Charts, React)
- Embeds dashboards via dashboards-as-code API (March 2026)
- Executes elastic-script for business logic
- Authenticates via Elasticsearch security
- Apps can work standalone OR be embedded in Kibana later

---

### App Definition Syntax

```sql
CREATE APP incident_response
    ROUTE '/incidents'                    -- URL path
    TITLE 'Incident Response Center'      -- Browser title
    ICON 'alert'                          -- EUI icon
    AUTH REQUIRED                         -- Requires login
    ROLES ['sre', 'oncall']               -- Who can access
AS
BEGIN
    -- App state (reactive)
    STATE selected_incident DOCUMENT DEFAULT NULL
    STATE severity_filter STRING DEFAULT 'all'
    
    -- UI components
    RENDER HEADER 'Incident Response' WITH {
        ACTIONS [
            BUTTON 'Create' ON_CLICK => MODAL create_modal
        ]
    }
    
    RENDER STATS { ... }
    RENDER TABLE { ... }
    RENDER CHART { ... }
    RENDER FORM { ... }
    RENDER DASHBOARD 'service-health' { ... }
    
END APP
```

---

### EUI Component Mapping

#### Data Display

```sql
-- Table (EUI.EuiBasicTable)
RENDER TABLE inventory_table {
    DATA (FROM inventory-* | SORT last_updated DESC | LIMIT 100)
    COLUMNS [
        { FIELD 'sku' LABEL 'SKU' SORTABLE },
        { FIELD 'name' LABEL 'Product Name' },
        { FIELD 'quantity' LABEL 'Qty' TYPE 'number' }
    ]
    PAGINATION { PAGE_SIZE 25 }
    ACTIONS [
        { LABEL 'Edit' ON_CLICK (row) => CALL edit_item(row) }
    ]
}

-- Stats (EUI.EuiStat)
RENDER STATS {
    STAT { TITLE 'Total Items' VALUE (FROM inventory-* | STATS COUNT(*)) }
    STAT { TITLE 'Low Stock' VALUE (...) COLOR 'danger' }
}

-- Chart (Elastic Charts)
RENDER CHART inventory_trend {
    TYPE 'area'
    DATA (FROM inventory-* | STATS sum(quantity) BY @timestamp BUCKET 1d)
    X '@timestamp'
    Y 'sum_quantity'
}
```

#### Forms & Input

```sql
RENDER FORM add_item_form {
    FIELD sku { TYPE 'text' LABEL 'SKU' REQUIRED }
    FIELD quantity { TYPE 'number' DEFAULT 0 }
    FIELD category { 
        TYPE 'select' 
        OPTIONS (FROM categories-* | STATS DISTINCT(name))
    }
    
    ON_SUBMIT (values) => BEGIN
        CALL add_inventory_item(values)
        TOAST 'Item added' TYPE 'success'
        REFRESH inventory_table
    END
}

RENDER SEARCH {
    PLACEHOLDER 'Search...'
    FILTERS [
        { FIELD 'category' TYPE 'select' OPTIONS [...] }
    ]
    ON_CHANGE (query) => REFRESH table WITH FILTER query
}
```

#### Dashboard Embedding

```sql
-- Embed existing dashboard
RENDER DASHBOARD 'abc-123-def' {
    HEIGHT '600px'
    FILTERS { 'service' = @selected_service }
}

-- Or define inline (dashboards-as-code)
RENDER DASHBOARD {
    TITLE 'Error Analysis'
    TIME_RANGE 'now-24h' TO 'now'
    
    PANEL 'errors_over_time' {
        TYPE 'lens'
        VISUALIZATION 'line'
        DATA (FROM logs-* | WHERE level = 'ERROR' | STATS count BY @timestamp)
    }
}
```

#### Layout

```sql
RENDER ROW {
    RENDER COLUMN { WIDTH '30%' ... }
    RENDER COLUMN { WIDTH '70%' ... }
}

RENDER TABS {
    TAB 'Overview' { ... }
    TAB 'Details' { ... }
}

MODAL create_modal {
    TITLE 'Create New Item'
    RENDER FORM { ... }
    ON_SUBMIT => BEGIN ... CLOSE MODAL END
}
```

---

### State & Interactivity

```sql
CREATE APP interactive_demo ROUTE '/demo' AS
BEGIN
    -- Reactive state
    STATE selected_service STRING DEFAULT 'all'
    STATE date_range STRING DEFAULT 'now-24h'
    
    -- Components react to state changes
    RENDER SELECT {
        VALUE @selected_service
        ON_CHANGE (val) => SET selected_service = val
    }
    
    RENDER TABLE {
        DATA (FROM logs-* | WHERE service = @selected_service)
        REFRESH EVERY 30 SECONDS
    }
    
    -- Lifecycle hooks
    ON_MOUNT => CALL log_app_access()
    ON_UNMOUNT => CALL cleanup()
END APP
```

---

### Deployment Management

```sql
-- Deploy an app
DEPLOY APP incident_response

-- View all apps
SHOW APPS
-- ┌────────────────────────────────────────────────────┐
-- │ Name              │ Route      │ Status  │ Version│
-- ├───────────────────┼────────────┼─────────┼────────┤
-- │ incident_response │ /incidents │ Running │ v3     │
-- │ inventory_manager │ /inventory │ Running │ v1     │
-- └────────────────────────────────────────────────────┘

-- Version management
ROLLBACK APP incident_response TO VERSION 2
STOP APP my_app
START APP my_app
DROP APP my_app
```

**Access URLs:**

```
https://your-cluster.com/escript-apps/incidents
https://apps.company.com/incidents  (custom domain)
```

---

### Implementation Phases

| Phase | Deliverables | Duration |
|-------|--------------|----------|
| **1. Runtime Foundation** | App Server, Router, Auth | 2 weeks |
| **2. Core Components** | TABLE, STATS, CHART, FORM | 2 weeks |
| **3. Dashboard Integration** | Embed dashboards-as-code | 1 week |
| **4. Interactivity** | STATE, Events, Modals | 2 weeks |
| **5. Advanced Components** | Search, Filters, Tabs | 1 week |
| **6. Deployment CLI** | DEPLOY, SHOW, ROLLBACK | 1 week |

---

## 🗓️ Release Timeline

| Version | Target | Focus |
|---------|--------|-------|
| **v1.0** | ✅ Current | Core language, 106 functions, async execution |
| **v1.1** | Q1 2026 | Exception handling, user-defined functions |
| **v1.2** | Q2 2026 | Dynamic ES\|QL, associative arrays, **sandboxing** |
| **v1.3** | Q3 2026 | Cursors, bulk operations |
| **v2.0** | Q4 2026 | **Triggers, scheduled jobs** |
| **v2.1** | Q1 2027 | Packages, security, secrets management |
| **v2.5** | Q2 2027 | **MCP Server, natural language interface** |
| **v3.0** | Q3 2027 | **App Deployment Platform (Phase 1-3)** |
| **v3.5** | Q4 2027 | **App Platform (Phase 4-6), LSP** |
| **v4.0** | 2028 | Vector search, cross-cluster, package registry |

---

## 🐛 Known Issues

| Issue | Status | Workaround |
|-------|--------|------------|
| `.escript_executions` index not auto-created | 🔄 In Progress | Create manually before STATUS calls |
| 106 functions registered per-request | 🔄 Planned | Move to startup registration |
| No transaction support | 📋 Backlog | Use compensating actions |

---

## 💡 Feature Requests

Have an idea for elastic-script? We'd love to hear it!

[:fontawesome-brands-github: Open a Feature Request](https://github.com/bahaaldine/elastic-script/issues/new?labels=enhancement&template=feature_request.md){ .md-button .md-button--primary }

---

## 🤝 Contributing

Want to help build these features? Check out the [Contributing Guide](development/contributing.md) to get started!

---

## 🚀 Current Implementation: Triggers & Scheduling

### Overview

Polling-based architecture for non-invasive event automation:

- **Scheduled Jobs**: Cron-based recurring execution
- **Event Triggers**: React to new documents in indices
- **No indexing impact**: Polling doesn't affect write performance

### Syntax Preview

```sql
-- Scheduled Job
CREATE JOB daily_cleanup
SCHEDULE '0 2 * * *'
AS
BEGIN
    CALL archive_old_logs(30)
END JOB

-- Event Trigger
CREATE TRIGGER on_payment_error
ON INDEX 'logs-*'
WHEN level = 'ERROR' AND service = 'payment'
EVERY 5 SECONDS
AS
BEGIN
    FOR doc IN @documents LOOP
        CALL SLACK_SEND('#alerts', doc.message)
    END LOOP
END TRIGGER
```

### Implementation Phases

| Phase | Deliverables | Duration |
|-------|--------------|----------|
| 1. Grammar & Storage | ANTLR rules, index mappings | 1 week |
| 2. Statement Handlers | CREATE/ALTER/DROP/SHOW handlers | 1 week |
| 3. Execution Services | Scheduler, polling, leader election | 1 week |
| 4. Testing & Docs | Unit tests, E2E notebooks | 1 week |

See [Triggers & Scheduling](triggers-and-scheduling.md) for full documentation.
