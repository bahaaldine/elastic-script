# CLAUDE.md - elastic-script Development Status

## Project Overview

**elastic-script** is a procedural scripting language that runs inside Elasticsearch, designed for:
- **Runbook automation** - Operational procedures with control flow
- **AI/LLM integration** - Native OpenAI and Elasticsearch Inference API support
- **Data processing** - ESQL integration with cursors and loops
- **Async execution** - Pipe-driven asynchronous workflows

---

## ✅ Completed Tasks

### 1. Core Language Features
- [x] Procedure creation and storage (`CREATE PROCEDURE ... END PROCEDURE`)
- [x] Variable declarations (`DECLARE`, `VAR`, `CONST`)
- [x] Control flow (`IF/THEN/ELSEIF/ELSE`, `FOR` loops, `WHILE` loops)
- [x] Exception handling (`TRY/CATCH/FINALLY`) with enhancements:
  - [x] `@error` variable binding in CATCH blocks (message, code, type, stack_trace)
  - [x] Named exceptions: `CATCH http_error`, `CATCH timeout_error`
  - [x] Enhanced `THROW`/`RAISE` with expressions and `WITH CODE`
  - [x] `EScriptException` class with structured error information
- [x] Inline functions with parameters (`IN`, `OUT`, `INOUT` modes)
- [x] **User-Defined Functions (NEW)**:
  - [x] `CREATE FUNCTION name(params) RETURNS type AS BEGIN ... END FUNCTION`
  - [x] Explicit `RETURNS` clause for return type (NUMBER, STRING, BOOLEAN, DATE, ARRAY, DOCUMENT)
  - [x] `DELETE FUNCTION name;` to remove stored functions
  - [x] Functions stored in `.elastic_script_functions` index
  - [x] Functions callable from expressions and other functions
  - [x] Auto-loading of stored functions when called
- [x] **EXECUTE IMMEDIATE (NEW)**:
  - [x] `EXECUTE IMMEDIATE query_expr [INTO vars] [USING binds];`
  - [x] Dynamic ES|QL query building at runtime
  - [x] Bind variables with positional syntax (:1, :2, :3)
  - [x] INTO clause for result capture
  - [x] Full ES|QL support for any valid query
- [x] Case-insensitive booleans (`TRUE`, `true`, `True`)
- [x] Array literals with single/double quotes (`['a', "b"]`)
- [x] Null coalescing (`??`) and safe navigation (`?.`)
- [x] Range operator (`1..10`)
- [x] MAP type with literal syntax (`MAP { 'key' => value }`)
- [x] TRY/CATCH/FINALLY exception handling with `@error` document
- [x] CREATE FUNCTION for stored user-defined functions
- [x] EXECUTE IMMEDIATE for dynamic ES|QL

### 2. Pipe-Driven Async Execution Model (NEW)
- [x] Grammar tokens: `ON_DONE`, `ON_FAIL`, `TRACK`, `TIMEOUT`, `FINALLY`
- [x] Parser rules for async procedure chains
- [x] `AsyncProcedureStatementHandler` - initiates async execution
- [x] `ContinuationExecutor` - executes `ON_DONE`/`ON_FAIL`/`FINALLY` chains
- [x] `ExecutionControlStatementHandler` - `EXECUTION('name') | STATUS/CANCEL/RETRY/WAIT`
- [x] `ParallelStatementHandler` - `PARALLEL [...] | ON_ALL_DONE`
- [x] Data structures: `ExecutionState`, `ExecutionPipeline`, `Continuation`, `ExecutionProgress`
- [x] `ExecutionRegistry` for state persistence
- [x] `@result` and `@results` bindings for continuations
- [x] Unit tests for async syntax parsing
- [x] Unit tests for continuation bindings

### 3. Built-in Functions (118 functions across 13 categories)

#### String Functions (18)
- `LENGTH`, `SUBSTR`, `UPPER`, `LOWER`, `TRIM`, `LTRIM`, `RTRIM`
- `REPLACE`, `INSTR`, `LPAD`, `RPAD`, `SPLIT`, `CONCAT`
- `REGEXP_REPLACE`, `REGEXP_SUBSTR`, `REVERSE`, `INITCAP`, `ENV`

#### Number Functions (11)
- `ABS`, `CEIL`, `FLOOR`, `ROUND`, `TRUNC`, `MOD`
- `POWER`, `SQRT`, `EXP`, `LOG`, `SIGN`

#### Array Functions (18)
- `ARRAY_LENGTH`, `ARRAY_APPEND`, `ARRAY_PREPEND`, `ARRAY_REMOVE`
- `ARRAY_CONTAINS`, `ARRAY_DISTINCT`, `ARRAY_JOIN`, `ARRAY_FLATTEN`
- `ARRAY_REVERSE`, `ARRAY_SLICE`, `ARRAY_MAP`, `ARRAY_FILTER`
- `ARRAY_REDUCE`, `ARRAY_FIND`, `ARRAY_FIND_INDEX`
- `ARRAY_EVERY`, `ARRAY_SOME`, `ARRAY_SORT`

#### Date Functions (8)
- `CURRENT_DATE`, `CURRENT_TIMESTAMP`
- `DATE_ADD`, `DATE_SUB`, `DATE_DIFF`
- `EXTRACT_YEAR`, `EXTRACT_MONTH`, `EXTRACT_DAY`

#### Document Functions (6)
- `DOCUMENT_GET`, `DOCUMENT_KEYS`, `DOCUMENT_VALUES`
- `DOCUMENT_CONTAINS`, `DOCUMENT_MERGE`, `DOCUMENT_REMOVE`

#### MAP Functions (12)
- `MAP_GET`, `MAP_GET_OR_DEFAULT`, `MAP_PUT`, `MAP_REMOVE`
- `MAP_KEYS`, `MAP_VALUES`, `MAP_SIZE`
- `MAP_CONTAINS_KEY`, `MAP_CONTAINS_VALUE`
- `MAP_MERGE`, `MAP_FROM_ARRAYS`, `MAP_ENTRIES`

#### Elasticsearch Functions (5)
- `ESQL_QUERY` - Execute ES|QL queries
- `INDEX_DOCUMENT`, `INDEX_BULK`, `UPDATE_DOCUMENT`
- `GET_DOCUMENT`, `REFRESH_INDEX`

#### OpenAI Functions (6)
- `LLM_COMPLETE`, `LLM_CHAT`, `LLM_EMBED`
- `LLM_SUMMARIZE`, `LLM_CLASSIFY`, `LLM_EXTRACT`

#### Elasticsearch Inference API (8)
- `INFERENCE_CREATE_ENDPOINT`, `INFERENCE_DELETE_ENDPOINT`
- `INFERENCE_LIST_ENDPOINTS`, `INFERENCE_GET_ENDPOINT`
- `INFERENCE`, `INFERENCE_CHAT`, `INFERENCE_EMBED`, `INFERENCE_RERANK`

#### Introspection Functions (8)
- `ESCRIPT_FUNCTIONS`, `ESCRIPT_FUNCTION`
- `ESCRIPT_PROCEDURES`, `ESCRIPT_PROCEDURE`
- `ESCRIPT_VARIABLES`, `ESCRIPT_CAPABILITIES`
- `ESCRIPT_INTENTS`, `ESCRIPT_INTENT`

#### Slack Functions (5)
- `SLACK_SEND`, `SLACK_SEND_BLOCKS`, `SLACK_WEBHOOK`
- `SLACK_LIST_CHANNELS`, `SLACK_POST_REACTION`

#### AWS Functions (5)
- `AWS_LAMBDA_INVOKE`, `AWS_SSM_RUN`, `AWS_SSM_STATUS`
- `AWS_ASG_DESCRIBE`, `AWS_ASG_SET_CAPACITY`

#### Kubernetes Functions (3)
- `K8S_GET`, `K8S_PATCH`, `K8S_SCALE`

#### PagerDuty Functions (6)
- `PAGERDUTY_TRIGGER`, `PAGERDUTY_ACKNOWLEDGE`, `PAGERDUTY_RESOLVE`
- `PAGERDUTY_GET_INCIDENT`, `PAGERDUTY_LIST_INCIDENTS`, `PAGERDUTY_ADD_NOTE`

#### Terraform Cloud Functions (6)
- `TF_CLOUD_RUN`, `TF_CLOUD_STATUS`, `TF_CLOUD_WAIT`
- `TF_CLOUD_OUTPUTS`, `TF_CLOUD_CANCEL`, `TF_CLOUD_LIST_WORKSPACES`

#### CI/CD Functions (6)
- `GITHUB_WORKFLOW`, `GITHUB_WORKFLOW_STATUS`
- `GITLAB_PIPELINE`, `GITLAB_PIPELINE_STATUS`
- `JENKINS_BUILD`, `JENKINS_STATUS`

#### S3 Functions (3)
- `S3_GET`, `S3_PUT`, `S3_LIST`

#### Generic/HTTP Functions (3)
- `HTTP_GET`, `HTTP_POST`, `WEBHOOK`

### 4. Developer Experience
- [x] `quick-start.sh` script for one-command setup
- [x] Automatic Elasticsearch startup with plugin
- [x] Sample data loading (360 documents across 6 indices)
- [x] OpenAI API key prompt and passthrough to Elasticsearch
- [x] Jupyter notebook kernel (`plesql_kernel`)
- [x] Test notebooks in `notebooks/` directory

### 5. Sample Data Indices
- `logs-sample` (100 docs) - Application logs with 20 detailed ERROR logs
- `metrics-sample` (80 docs) - System metrics (CPU, memory, latency)
- `users-sample` (30 docs) - User profiles with roles
- `orders-sample` (50 docs) - E-commerce orders
- `products-sample` (40 docs) - Product catalog
- `security-events` (60 docs) - Audit events

### 6. Bug Fixes Applied
- [x] Case-insensitive boolean support (`TRUE`/`true`)
- [x] Array literals with single quotes
- [x] FOR loop syntax (`FOR i IN 1..n LOOP`)
- [x] OpenAI API key passthrough via `elasticsearch.run.gradle`
- [x] `@result`/`@results` bindings in continuations
- [x] macOS PATH variable collision fix in quick-start.sh
- [x] Division by zero in sample data generation

### 7. Observability Refactor (Complete)
- [x] `EScriptLogger` - Centralized structured logging utility
  - INFO: Procedure start/end, user output (PRINT) only
  - DEBUG: Variable assignments, function calls, ESQL queries
  - TRACE: Async chain tracing, expression evaluation, handler internals
  - Separate `o.e.x.e.EScript.OUTPUT` logger for PRINT statements
  - Async chain methods: `asyncEnter`, `asyncExit`, `asyncStep`, `asyncCallback`
- [x] `EScriptTracer` - APM tracing integration (ready for agent)
  - Transaction per procedure execution
  - Spans for statements, functions, external calls
  - Works with or without APM agent (reflection-based)
- [x] `ExecutionContext` enhancements
  - `executionId` for log/trace correlation
  - `printOutput` list to capture PRINT statements
  - `startTimeMs` for execution timing
- [x] **Complete logging cleanup**
  - Removed all 172+ verbose INFO logs
  - `ActionListenerUtils` - Now uses TRACE with execution ID (skipped if TRACE disabled)
  - `ExpressionEvaluator` - Removed "Evaluating primary expression" logs
  - `ElasticScriptExecutor` - Changed all INFO to DEBUG
  - All REST actions - Changed all INFO to DEBUG
  - All function registration logs - Removed (were duplicated per-request)
  - `EsqlBuiltInFunctions` - Changed query logs to DEBUG/TRACE
  - Only remaining INFO: `EScriptLogger` user output + APM detection
- [x] `quick-start.sh` Kibana support
  - `--kibana` to start Kibana
  - `--stop-kibana` to stop Kibana
  - Enhanced `--status` shows all services
- [x] `plesql_kernel.py` PRINT output handling
  - `_display_output()` for captured PRINT statements
  - Ready for API response enhancements
- [ ] Full PRINT capture in API response (requires deeper integration)
- [x] **OTEL Collector Integration (NEW)**
  - OTEL Collector auto-download and setup in quick-start.sh
  - Receives OTLP traces on ports 4317 (gRPC) and 4318 (HTTP)
  - Exports traces to Elasticsearch APM indices
  - ES telemetry enabled automatically (`telemetry.tracing.enabled`)
  - Traces visible in Kibana APM: http://localhost:5601/app/apm

---

## 🔧 Current State

### Working
- All 106 built-in functions registered and functional
- Procedure creation, storage, and execution
- ESQL integration with cursors
- OpenAI LLM functions (when API key configured)
- Async execution syntax parsing (partial runtime)
- Jupyter notebook integration

### Environment
- Elasticsearch runs via `./gradlew :run` from testclusters
- Default credentials: `elastic-admin` / `elastic-password`
- Notebooks at `http://localhost:8888`
- Elasticsearch at `http://localhost:9200`

---

## 📋 Pending Tasks

### High Priority
1. ~~**Automated E2E Test Framework**~~ ✅ Complete
   - `tests/e2e/run_notebook_tests.py` - Programmatic notebook execution
   - `tests/e2e/run_tests.sh` - Shell wrapper with setup options
   - `tests/e2e/skip_cells.json` - Configuration for known issues
   - Uses `nbclient` to execute Jupyter notebooks as tests
   - **Current results: 6/6 pass (100%)**
     - ✅ 00-complete-reference.ipynb
     - ✅ 01-getting-started.ipynb
     - ✅ 02-esql-integration.ipynb
     - ⏭️ 03-ai-observability.ipynb (skipped - requires OpenAI key)
     - ✅ 04-async-execution.ipynb (1 cell skipped - STATUS storage bug)
     - ✅ 05-runbook-integrations.ipynb
2. **Complete Observability Integration** - API response with PRINT output, execution metadata
3. ~~**APM/Tracing Integration**~~ ✅ Complete - OTEL Collector-based tracing
   - OTEL Collector downloaded and started automatically by quick-start.sh
   - Receives OTLP on ports 4317 (gRPC) and 4318 (HTTP)
   - Traces exported to Elasticsearch and visible in Kibana APM
   - ES telemetry enabled by default (`telemetry.tracing.enabled: true`)
   - View traces at http://localhost:5601/app/apm
4. **Instrument elastic-script procedures** (For Later) - Auto-trace procedure execution:
   - Wire `EScriptTracer` to send spans to OTEL Collector
   - Each procedure call creates a trace automatically
   - Child spans for statements, ESQL queries, external calls
4. ~~**Async Execution Runtime**~~ ✅ Complete - ON_DONE, ON_FAIL, PARALLEL all working
5. ~~**ExecutionRegistry Persistence**~~ ✅ Complete - `.escript_executions` index auto-created
6. **Multi-Node Distributed Execution** (For Later) - Test async execution across multiple ES nodes:
   - Verify execution state is accessible from any node
   - Test parallel execution distribution across cluster
   - Ensure continuation handlers can run on different nodes than initiator

### Medium Priority
4. ~~**Function Registration Performance**~~ ✅ Complete
   - Created `BuiltInFunctionRegistry` singleton that caches function definitions
   - Stateless functions (~70): cached permanently after first initialization
   - Client-dependent functions (~20): cached per Client instance
   - Executor-dependent functions (ESQL_QUERY): registered per request (required for variable substitution)
   - Reduces per-request overhead from ~106 registrations to ~1
5. **Intent System** - Complete DEFINE INTENT functionality
6. **Error Handling** - Better error messages and stack traces
8. **Function Documentation** - Auto-generated docs from `@FunctionSpec` annotations

### Low Priority
8. **Performance Optimization** - Batch operations, connection pooling
9. **Security** - API key management, RBAC integration
10. **Monitoring** - Execution metrics, slow query logging

---

## 📁 Key Files & Locations

### Core Source
```
elastic-script/elastic-script/src/main/java/org/elasticsearch/xpack/escript/
├── parser/ElasticScript.g4          # ANTLR grammar
├── executors/ProcedureExecutor.java  # Main execution engine
├── handlers/                         # Statement handlers
├── functions/builtin/                # All 106 built-in functions
├── execution/                        # Async execution framework
├── logging/EScriptLogger.java        # Structured logging utility
└── tracing/EScriptTracer.java        # APM tracing integration
```

### Scripts & Notebooks
```
elastic-script/
├── scripts/quick-start.sh            # Developer setup script
├── notebooks/                         # Jupyter notebooks
│   ├── kernel/plesql_kernel.py       # Custom Jupyter kernel
│   ├── 00-complete-reference.ipynb   # All functions showcase
│   ├── 01-getting-started.ipynb
│   ├── 02-esql-integration.ipynb
│   ├── 03-ai-observability.ipynb
│   ├── 04-async-execution.ipynb
│   ├── 05-runbook-integrations.ipynb
│   ├── 12-exception-handling.ipynb    # TRY/CATCH/FINALLY
│   ├── 13-user-defined-functions.ipynb # CREATE FUNCTION
│   ├── 14-execute-immediate.ipynb      # Dynamic ES|QL
│   └── 15-map-type.ipynb               # MAP associative arrays
├── tests/e2e/                         # E2E test framework
│   ├── README.md                      # E2E documentation
│   ├── run_notebook_tests.py          # Programmatic notebook execution
│   ├── run_tests.sh                   # Shell wrapper
│   └── requirements.txt               # nbclient, nbformat
├── otel-collector/                    # OpenTelemetry Collector
│   ├── config.yaml                    # Collector configuration
│   └── .gitignore                     # Ignores binary and logs
└── CLAUDE.md                          # This file
```

### Elasticsearch Integration
```
elastic-script/elasticsearch/
├── build-tools-internal/src/main/groovy/elasticsearch.run.gradle  # Modified for OPENAI_API_KEY
└── (submodule pointing to elastic/elasticsearch)
```

---

## 🚀 Quick Reference

### Start Development Environment
```bash
cd elastic-script
./scripts/quick-start.sh
```

### Run a Procedure
```bash
curl -u elastic-admin:elastic-password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "CREATE PROCEDURE hello() BEGIN PRINT \"Hello World!\"; END PROCEDURE;"}'

curl -u elastic-admin:elastic-password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "CALL hello()"}'
```

### Send Traces via OTEL
```bash
# Traces are collected via OTEL Collector on localhost:4318
# Any OTEL-instrumented app can send traces:
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318
export OTEL_SERVICE_NAME=my-service

# View traces in Kibana APM:
# http://localhost:5601/app/apm
```

### Regenerate ANTLR Parser
```bash
cd elastic-script/elasticsearch
./gradlew :elastic-script:regen
```

### Run Unit Tests
```bash
cd elastic-script/elasticsearch
./gradlew :x-pack:plugin:elastic-script:test
```

### Run E2E Notebook Tests
```bash
# Run all notebooks
./tests/e2e/run_tests.sh

# Run specific notebook
./tests/e2e/run_tests.sh --notebook 01

# Verbose mode
./tests/e2e/run_tests.sh --verbose

# Setup + run (starts ES if needed)
./tests/e2e/run_tests.sh --setup
```

---

## 📝 Notes

- The `elasticsearch` folder is a git submodule - changes there need separate commits
- Gradle daemon can cache old environment variables - use `./gradlew --stop` before restart
- macOS compatibility issues may arise (shuf, bash 4.0+ features)
- The kernel authenticates with hardcoded credentials - for production, use proper secrets management

---

*Last updated: January 9, 2026*

---

## 🔮 Future Language Features (PL/SQL Comparison)

Based on comprehensive analysis comparing elastic-script to Oracle PL/SQL:

### Phase 1: Core Completeness (Q1-Q2 2026)
| Feature | Priority | Description |
|---------|----------|-------------|
| TRY/CATCH | ✅ Done | Exception handling with named exceptions |
| CREATE FUNCTION | ✅ Done | User-defined functions (vs procedures) |
| EXECUTE IMMEDIATE | ✅ Done | Dynamic ES|QL building |
| MAP type | ✅ Done | Associative arrays (key-value) with 12 functions |

### Phase 2: Scale & Performance (Q2-Q3 2026)
| Feature | Priority | Description |
|---------|----------|-------------|
| Cursors | ✅ Done | Streaming large result sets (OPEN/CLOSE/FETCH, %NOTFOUND/%ROWCOUNT) |
| CREATE JOB | ✅ Done | Scheduled job execution (cron) |
| CREATE TRIGGER | ✅ Done | Event-driven execution |
| FORALL/BULK COLLECT | ✅ Done | Bulk operations with SAVE EXCEPTIONS |

### Phase 3: Enterprise (Q3-Q4 2026)
| Feature | Priority | Description |
|---------|----------|-------------|
| Packages | ✅ Done | Module organization with public/private visibility |
| GRANT/REVOKE | ✅ Done | Fine-grained permissions with roles and users |
| Profiler | ✅ Done | Performance analysis with timing and recommendations |
| User-Defined Types | ✅ Done | Custom record types with named fields |
| AUTHID | ✅ Done | Invoker/definer rights for procedures and functions |
| SAVE EXCEPTIONS | ✅ Done | Error collection in FORALL bulk operations |

### Trigger Event Types (Planned)
- `DOCUMENT INSERTED INTO index` - New document indexed
- `DOCUMENT UPDATED IN index` - Document updated  
- `ALERT name FIRES` - Elasticsearch alert fires
- `INDEX ROLLED OVER IN pattern` - ILM rollover event
- `CLUSTER STATUS CHANGED TO status` - Health change

See `docs/roadmap.md` for full details.

*Last updated: January 22, 2026*
