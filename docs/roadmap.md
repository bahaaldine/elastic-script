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

## 🗓️ Release Timeline

| Version | Target | Focus |
|---------|--------|-------|
| **v1.0** | ✅ Current | Core language, 106 functions, async execution |
| **v1.1** | Q1 2026 | Exception handling, user-defined functions |
| **v1.2** | Q2 2026 | Dynamic ES|QL, associative arrays |
| **v1.3** | Q3 2026 | Cursors, bulk operations |
| **v2.0** | Q4 2026 | **Triggers, scheduled jobs** |
| **v2.1** | Q1 2027 | Packages, security |
| **v3.0** | Q3 2027 | Vector search, cross-cluster |

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
