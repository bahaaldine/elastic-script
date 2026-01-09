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
- [x] Exception handling (`TRY/CATCH/FINALLY`)
- [x] Functions with parameters (`IN`, `OUT`, `INOUT` modes)
- [x] Case-insensitive booleans (`TRUE`, `true`, `True`)
- [x] Array literals with single/double quotes (`['a', "b"]`)
- [x] Null coalescing (`??`) and safe navigation (`?.`)
- [x] Range operator (`1..10`)

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

### 3. Built-in Functions (106 functions across 12 categories)

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

### 7. Observability Refactor (NEW - In Progress)
- [x] `EScriptLogger` - Centralized structured logging utility
  - INFO: Procedure start/end, user output (PRINT)
  - DEBUG: Variable assignments, function calls, loops
  - TRACE: Expression evaluation, handler internals
  - Separate `o.e.x.e.EScript.OUTPUT` logger for PRINT statements
- [x] `EScriptTracer` - APM tracing integration
  - Transaction per procedure execution
  - Spans for statements, functions, external calls
  - Works with or without APM agent (reflection-based)
- [x] `ExecutionContext` enhancements
  - `executionId` for log/trace correlation
  - `printOutput` list to capture PRINT statements
  - `startTimeMs` for execution timing
- [x] Logging refactor across handlers
  - `PrintStatementHandler` - Uses `EScriptLogger.userOutput`
  - `ExecuteStatementHandler` - Uses `EScriptLogger.esqlQuery`
  - `ContinuationExecutor` - Cleaner error logging
  - `AsyncProcedureStatementHandler` - Lifecycle logging
- [x] `quick-start.sh` Kibana support
  - `--kibana` to start Kibana
  - `--stop-kibana` to stop Kibana
  - Enhanced `--status` shows all services
- [x] `plesql_kernel.py` PRINT output handling
  - `_display_output()` for captured PRINT statements
  - Ready for API response enhancements
- [ ] Full PRINT capture in API response (requires deeper integration)
- [ ] APM agent attachment and testing

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
1. **Automated E2E Test Framework** - Spin up full infrastructure, run notebooks as tests
   - Create test harness that starts ES + loads data + runs kernel
   - Execute each notebook cell and validate output
   - Notebooks to automate: 01-getting-started, 02-esql, 03-ai, 04-async, 00-complete-reference
2. **Complete Observability Integration** - API response with PRINT output, execution metadata
3. **APM Agent Testing** - Attach Elastic APM agent to verify tracing
4. **Async Execution Runtime** - Complete runtime for pipe-driven execution
5. **ExecutionRegistry Persistence** - Store execution state in `.escript_executions` index

### Medium Priority
4. **Function Registration Performance** - Currently all 106 built-in functions are registered on EVERY execution:
   ```
   [INFO] Registering ESQL built-in functions
   [INFO] Registering String built-in functions
   [INFO] Registering Array built-in functions
   ... (repeated for each request)
   ```
   Should register once at plugin startup, not per-request. Potential significant performance gain.
5. **Intent System** - Complete DEFINE INTENT functionality
6. **CURSOR Iteration** - Full FOR loop over CURSOR results
7. **Error Handling** - Better error messages and stack traces
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
│   ├── 01-getting-started.ipynb
│   ├── 02-esql-integration.ipynb
│   ├── 03-ai-observability.ipynb
│   ├── 04-async-execution.ipynb
│   └── 05-runbook-integrations.ipynb
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

### Regenerate ANTLR Parser
```bash
cd elastic-script/elasticsearch
./gradlew :elastic-script:regen
```

### Run Tests
```bash
cd elastic-script/elasticsearch
./gradlew :elastic-script:test
```

---

## 📝 Notes

- The `elasticsearch` folder is a git submodule - changes there need separate commits
- Gradle daemon can cache old environment variables - use `./gradlew --stop` before restart
- macOS compatibility issues may arise (shuf, bash 4.0+ features)
- The kernel authenticates with hardcoded credentials - for production, use proper secrets management

---

*Last updated: January 9, 2026*
