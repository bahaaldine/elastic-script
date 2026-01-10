# Roadmap

Current status and future direction for elastic-script.

---

## ✅ Completed Features

### Core Language (v1.0)
- [x] **Procedure Creation** - `CREATE PROCEDURE ... END PROCEDURE`
- [x] **Variable System** - `DECLARE`, `VAR`, `CONST` with type inference
- [x] **Control Flow** - IF/THEN/ELSEIF/ELSE, FOR loops, WHILE loops
- [x] **Exception Handling** - TRY/CATCH/FINALLY blocks
- [x] **Functions** - IN/OUT/INOUT parameter modes
- [x] **Data Types** - STRING, NUMBER, BOOLEAN, ARRAY, DOCUMENT, DATE

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
- [x] **E2E Test Framework** - Automated notebook execution tests (6/6 passing)
- [x] **GitHub Pages Documentation** - Full documentation site

---

## 🚧 In Progress

### Observability & Debugging

| Feature | Status | Description |
|---------|--------|-------------|
| Structured Logging | ✅ Done | `EScriptLogger` with log levels and execution IDs |
| PRINT Output Capture | 🔄 Planned | Return PRINT statements in API response |
| Execution Metadata | 🔄 Planned | Include `execution_id`, `duration_ms` in response |
| APM Integration | 🔄 Ready | `EScriptTracer` implemented, needs agent testing |

---

## 📋 Planned Features

### High Priority

#### 1. API Response Enhancement
Return richer execution information:

```json
{
  "result": "procedure return value",
  "output": ["PRINT line 1", "PRINT line 2"],
  "execution_id": "abc-123",
  "duration_ms": 150
}
```

**Why:** Makes debugging easier, PRINT statements visible in API calls.

---

#### 2. Function Registration Optimization
Currently, all 106 functions register on every execution. Moving to startup-time registration for significant performance gains.

**Impact:** Reduced latency on every procedure call.

---

#### 3. Multi-Node Distributed Execution
Test and validate async execution across Elasticsearch clusters:

- Execution state accessible from any node
- Parallel execution distributed across cluster
- Continuation handlers can run on different nodes

**Why:** Essential for production-grade distributed workflows.

---

#### 4. Kibana APM Visualization
Full distributed tracing in Kibana APM:

```
┌─────────────────────────────────────────────────────────────┐
│ Transaction: analyze_logs_pipeline                          │
│ Duration: 2.5s                                               │
├─────────────────────────────────────────────────────────────┤
│ ├── analyze_logs() ─────────────────── 1.2s                │
│ │   ├── ESQL_QUERY ─────────── 800ms                       │
│ │   └── LLM_COMPLETE ───────── 400ms                       │
│ ├── ON_DONE: process_results() ─────── 800ms               │
│ └── FINALLY: cleanup() ──────────────── 500ms              │
└─────────────────────────────────────────────────────────────┘
```

**Why:** Production-grade debugging without log pollution.

---

### Medium Priority

#### 5. Intent System
Complete the `DEFINE INTENT` functionality for declarative automation:

```sql
DEFINE INTENT maintain_replicas AS
    'Ensure index has at least 2 replicas'
    CHECK ESQL_QUERY('FROM _cat/indices | WHERE replicas < 2')
    REMEDIATE increase_replicas(@index);
```

---

#### 6. Enhanced CURSOR Support
Full iteration patterns for ESQL results:

```sql
FOR row IN ESQL_QUERY('FROM logs-* | LIMIT 1000') LOOP
    -- Process each row
END LOOP;
```

---

#### 7. Better Error Messages
Improved error reporting with:

- Line numbers and column positions
- Contextual suggestions
- Stack traces for nested procedure calls

---

#### 8. Auto-Generated Documentation
Generate function docs from `@FunctionSpec` annotations:

```java
@FunctionSpec(
    name = "ARRAY_LENGTH",
    description = "Returns the number of elements in an array",
    parameters = {"array ARRAY"},
    returns = "NUMBER"
)
```

---

### Low Priority

#### 9. Performance Optimization
- Batch operations for bulk indexing
- Connection pooling for external services
- Query result caching

---

#### 10. Security Enhancements
- Encrypted API key storage
- RBAC integration for procedure access
- Audit logging for sensitive operations

---

#### 11. Monitoring & Metrics
- Execution metrics (count, duration, errors)
- Slow query logging
- Resource usage tracking

---

## 🗓️ Release Timeline

| Version | Target | Focus |
|---------|--------|-------|
| v1.0 | ✅ Current | Core language, 106 functions, async execution |
| v1.1 | Q1 2026 | API enhancement, performance optimization |
| v1.2 | Q2 2026 | Multi-node execution, APM integration |
| v2.0 | Q3 2026 | Intent system, enhanced security |

---

## 💡 Feature Requests

Have an idea for elastic-script? We'd love to hear it!

[:fontawesome-brands-github: Open a Feature Request](https://github.com/bahaaldine/elastic-script/issues/new?labels=enhancement&template=feature_request.md){ .md-button .md-button--primary }

---

## 🤝 Contributing

Want to help build these features? Check out the [Contributing Guide](development/contributing.md) to get started!
