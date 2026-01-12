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

| Category | Feature | PL/SQL | elastic-script | Priority |
|----------|---------|--------|----------------|----------|
| **Error Handling** | TRY/CATCH blocks | ✅ | ❌ | 🔴 P0 |
| | Named exceptions | ✅ | ❌ | 🟡 P1 |
| | RAISE/THROW | ✅ | ❌ | 🔴 P0 |
| **Functions** | User-defined functions | ✅ | ❌ | 🔴 P0 |
| | Function overloading | ✅ | ❌ | 🟢 P2 |
| **Cursors** | Explicit cursors | ✅ | ❌ | 🔴 P0 |
| | FETCH INTO | ✅ | ❌ | 🔴 P0 |
| | BULK COLLECT | ✅ | ❌ | 🔴 P0 |
| **Modules** | Packages | ✅ | ❌ | 🟡 P1 |
| | Package state | ✅ | ❌ | 🟡 P1 |
| **Events** | Triggers | ✅ | ❌ | 🔴 P0 |
| | Scheduled jobs | ✅ | ❌ | 🔴 P0 |
| **Collections** | Associative arrays (MAP) | ✅ | ❌ | 🔴 P0 |
| | User-defined types | ✅ | ❌ | 🟡 P1 |
| **Dynamic** | EXECUTE IMMEDIATE | ✅ | ❌ | 🔴 P0 |
| | Bind variables | ✅ | ❌ | 🔴 P0 |
| **Bulk Ops** | FORALL | ✅ | ❌ | 🔴 P0 |
| **Security** | GRANT/REVOKE | ✅ | ❌ | 🟡 P1 |
| **Debug** | Profiler | ✅ | ❌ | 🟡 P1 |

---

## 🚧 Phase 1: Core Language Completeness (Q1-Q2 2026)

### 1.1 Exception Handling (TRY/CATCH)
**Priority:** 🔴 P0

```sql
TRY
    SET result = HTTP_GET('https://api.example.com/data')
CATCH http_error
    PRINT 'HTTP failed: ' || @error.message
CATCH parse_error
    SET result = {}
FINALLY
    CALL cleanup()
END TRY
```

### 1.2 User-Defined Functions
**Priority:** 🔴 P0

```sql
CREATE FUNCTION calculate_severity(errors NUMBER, warns NUMBER) RETURNS STRING AS
BEGIN
    IF errors * 10 + warns > 100 THEN RETURN 'critical' END IF
    IF errors * 10 + warns > 50 THEN RETURN 'high' END IF
    RETURN 'low'
END FUNCTION

SET sev = calculate_severity(5, 10)
```

### 1.3 Dynamic ES|QL
**Priority:** 🔴 P0

```sql
DECLARE query STRING = 'FROM logs-* | WHERE level = ''ERROR'''
IF service IS NOT NULL THEN
    SET query = query || ' | WHERE service = :svc'
END IF
EXECUTE IMMEDIATE query USING service INTO results
```

### 1.4 Associative Arrays (MAP)
**Priority:** 🔴 P0

```sql
DECLARE counts MAP<STRING, NUMBER> = {}
SET counts['api'] = 42
FOR svc, cnt IN counts LOOP
    PRINT svc || ': ' || cnt
END LOOP
```

---

## 🚧 Phase 2: Scale & Performance (Q2-Q3 2026)

### 2.1 Cursor Management
**Priority:** 🔴 P0

```sql
DECLARE CURSOR c FOR FROM logs-* | LIMIT 100000
OPEN c
WHILE FETCH c LIMIT 1000 INTO batch LOOP
    FOR doc IN batch LOOP CALL process(doc) END LOOP
END LOOP
CLOSE c
```

### 2.2 Bulk Operations (FORALL)
**Priority:** 🔴 P0

```sql
BULK COLLECT INTO logs FROM logs-* | WHERE level = 'ERROR' | LIMIT 5000
FORALL log IN logs
    CALL archive(log)
    SAVE EXCEPTIONS
```

### 2.3 Scheduled Jobs
**Priority:** 🔴 P0

```sql
CREATE JOB daily_cleanup
SCHEDULE '0 2 * * *'  -- 2 AM daily
AS
BEGIN
    CALL archive_old_logs(30)
    CALL send_report()
END JOB
```

### 2.4 Triggers & Events
**Priority:** 🔴 P0

```sql
CREATE TRIGGER on_error
WHEN DOCUMENT INSERTED INTO logs-*
WHERE level = 'ERROR' AND service = 'payment'
BEGIN
    CALL SLACK_SEND('#alerts', 'Error: ' || @document.message)
    IF @document.error_code = 'PAY001' THEN
        CALL PAGERDUTY_TRIGGER('Payment failure', 'critical')
    END IF
END TRIGGER

CREATE TRIGGER on_alert
WHEN ALERT 'high-error-rate' FIRES
BEGIN
    CALL escalate(@alert)
END TRIGGER
```

---

## 🚧 Phase 3: Enterprise Features (Q3-Q4 2026)

### 3.1 Packages & Modules

```sql
CREATE PACKAGE incident_response AS
    PROCEDURE handle(id STRING)
    FUNCTION get_severity(id STRING) RETURNS STRING
END PACKAGE

CREATE PACKAGE BODY incident_response AS
    PROCEDURE handle(id STRING) AS BEGIN ... END
END PACKAGE BODY
```

### 3.2 Security (GRANT/REVOKE)

```sql
GRANT EXECUTE ON PROCEDURE analyze_logs TO ROLE 'analyst'
CREATE PROCEDURE admin_op() AUTHID DEFINER AS BEGIN ... END
```

### 3.3 Profiling & Debugging

```sql
SET PROFILING ON
CALL complex_pipeline()
SHOW PROFILE  -- Line-by-line timing breakdown
```

---

## 🚧 Phase 4: Elasticsearch-Native (2027+)

### 4.1 Vector Search & ML
```sql
VECTOR_SEARCH INTO results FROM kb QUERY_VECTOR LLM_EMBED(question) K 10
```

### 4.2 Cross-Cluster
```sql
FROM cluster:us-west/logs-* | WHERE level = 'ERROR' INTO remote_errors
```

---

## 🗓️ Release Timeline

| Version | Target | Focus |
|---------|--------|-------|
| v1.0 | ✅ Current | Core language, 106 functions, async |
| v1.1 | Q1 2026 | Exception handling, UDFs |
| v1.2 | Q2 2026 | Dynamic ES|QL, MAPs |
| v1.3 | Q3 2026 | Cursors, bulk ops |
| **v2.0** | **Q4 2026** | **Triggers, scheduled jobs** |
| v2.1 | Q1 2027 | Packages, security |
| v3.0 | Q3 2027 | Vector search, cross-cluster |

---

## 💡 Feature Requests

[:fontawesome-brands-github: Open a Feature Request](https://github.com/bahaaldine/elastic-script/issues/new?labels=enhancement){ .md-button .md-button--primary }

---

## 🔧 Near-Term Improvements (v1.1)

These are implementation improvements for the current v1.0 codebase:

### API Response Enhancement
**Status:** 🔄 Planned | **Priority:** High

Return richer execution information in the API response:

```json
{
  "result": "procedure return value",
  "output": ["PRINT line 1", "PRINT line 2"],
  "execution_id": "abc-123-def",
  "duration_ms": 150
}
```

**Why:** Makes debugging easier - PRINT statements visible in API calls, execution tracking.

---

### Function Registration Optimization
**Status:** 🔄 Planned | **Priority:** High

Currently, all 106 built-in functions register on every execution request. Moving to startup-time registration for significant performance gains.

**Current (per-request):**
```
Request 1: Register 106 functions → Execute
Request 2: Register 106 functions → Execute  
Request 3: Register 106 functions → Execute
```

**Target (once at startup):**
```
Plugin Startup: Register 106 functions (once)
Request 1: Execute
Request 2: Execute
Request 3: Execute
```

**Impact:** Reduced latency on every procedure call.

---

### Known Issues

| Issue | Status | Workaround |
|-------|--------|------------|
| `.escript_executions` index not auto-created | 🔄 In Progress | Create manually before STATUS calls |
