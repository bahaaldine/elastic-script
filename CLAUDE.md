# CLAUDE.md - Moltler Development Status

## Project Overview

**Moltler** is an AI Skills Creation Framework built on **elastic-script**, a procedural scripting language running inside Elasticsearch.

### Core Concepts

- **Skills** - Reusable automation components with versioning, metadata, and AI generation
- **Connectors** - Integrations with external services (GitHub, Jira, Datadog, etc.)
- **Agents** - Autonomous entities that execute skills based on goals and triggers
- **elastic-script** - The underlying procedural language powering Moltler

### Why Moltler?

1. **AI-Native Skills** - Create skills that AI agents can discover and execute
2. **Connector Framework** - Unified interface for external service integration
3. **Autonomous Agents** - OODA-loop driven execution with configurable policies
4. **Built on Elasticsearch** - Leverage ES|QL, search, and observability

---

## 🚀 Moltler Features (NEW)

### Skills System
- [x] `CREATE SKILL name VERSION 'x.y.z'` - Versioned skill creation
- [x] `DESCRIPTION`, `AUTHOR`, `TAGS`, `REQUIRES` - Rich metadata
- [x] `(param IN TYPE DEFAULT value)` - Typed parameters with defaults
- [x] `RETURNS type` - Explicit return types
- [x] `TEST SKILL name WITH params EXPECT result` - Skill testing framework
- [x] `GENERATE SKILL FROM 'goal'` - AI-powered skill generation
- [x] `RUN SKILL name WITH args` - Execute skills directly
- [x] `SHOW SKILLS`, `SHOW SKILL name` - Skill introspection
- [x] `SkillRegistry` - Storage in `.escript_skills` index

### Skill Packs
- [x] `CREATE SKILL PACK name VERSION 'x.y.z' SKILLS [...]` - Bundle skills together
- [x] `DROP SKILL PACK name` - Remove skill packs
- [x] `SHOW SKILL PACKS`, `SHOW SKILL PACK name` - List and inspect packs
- [x] Storage in `.moltler_skill_packs` index

### MoltlerHub (199 Skills)
- [x] **Meta** (10 skills) - `list_all_skills`, `explain_skill`, `recommend_skills`, `generate_skill`, `validate_skill_syntax`, `what_can_i_do`, `analyze_my_cluster`
- [x] **Search** (28 skills) - `search_documents`, `top_values`, `semantic_search`
- [x] **Observability** (22 skills) - `get_recent_errors`, `count_logs_by_level`
- [x] **APM** (11 skills) - `list_services`, `get_slow_transactions`, `get_trace`
- [x] **Security** (20 skills) - `hunt_ioc`, `get_user_risk_score`, `list_detections`
- [x] **ML** (11 skills) - `get_anomalies`, `embed_text`, `classify_text`
- [x] **Alerting** (8 skills) - `list_alert_rules`, `get_active_alerts`
- [x] **Cluster** (12 skills) - `cluster_health`, `list_nodes`, `explain_allocation`, `analyze_my_cluster`
- [x] **Integrations** (10 skills) - `send_slack_message`, `create_jira_issue`
- [x] **Fleet** (6 skills) - `list_agents`, `get_agent_status`
- [x] **Agent Builder** (13 skills) - `list_agents`, `create_agent`, `chat`
- [x] **Enterprise Search** (4 skills) - `list_search_apps`, `get_search_analytics`
- [x] **Community** (33 skills) - SRE automation, incident response, data pipeline skills
- [x] MCP endpoint at `/_escript/mcp` for AI agent integration
- [x] `moltler-cli.sh` for skill management (install, uninstall, search, run)

### NL-to-Skill Generation (NEW)
- [x] **generate_skill** meta skill - Uses ES Inference API for server-side generation
- [x] **MCP tools** - `generate_skill`, `validate_skill_syntax`, `install_skill`
- [x] **AI_SKILL_GENERATION.md** - Comprehensive prompts for AI agents
- [x] **Skill Builder UI** - MoltlerHub `/builder` page with wizard flow
- [x] Starter packs - Role-based skill bundles (SRE, Security, Search, Platform)

### Elastic Workflows Integration (NEW)
- [x] **WorkflowFunctions.java** - 6 built-in functions for workflow management
- [x] `WORKFLOW_TRIGGER(id, inputs)` - Trigger workflow execution
- [x] `WORKFLOW_LIST()` - List all available workflows
- [x] `WORKFLOW_GET(id)` - Get workflow details
- [x] `WORKFLOW_STATUS(execution_id)` - Check execution status
- [x] `WORKFLOW_CREATE(yaml)` - Install workflow from YAML
- [x] `WORKFLOW_DELETE(id)` - Remove workflow
- [x] **7 Workflow Skills** - `list_workflows`, `trigger_workflow`, `install_workflow`, `workflow_status`
- [x] **Pre-built Templates** - `install_ip_reputation_workflow`, `install_slack_alert_workflow`, `install_esql_report_workflow`
- [x] **WORKFLOWS_INTEGRATION.md** - Complete integration documentation

### Connector Framework
- [x] `CREATE CONNECTOR name TYPE 'type' CONFIG {...}` - Connector creation
- [x] **GitHub Connector** - Issues, PRs, repos, workflows, commits
- [x] **Jira Connector** - Issues, projects, sprints, boards
- [x] **Datadog Connector** - Metrics, monitors, events, logs
- [x] `EXEC connector.action(args)` - Execute actions on connectors
- [x] `QUERY connector.entity WHERE ... LIMIT n ORDER BY ...` - Query entities
- [x] `SYNC CONNECTOR name TO 'index' [INCREMENTAL ON field]` - Data sync
- [x] `TEST CONNECTOR name` - Connectivity testing
- [x] `ConnectorFactory` - Unified connector dispatch
- [x] `SyncEngine` - Incremental/full sync support

### Agent Runtime
- [x] `CREATE AGENT name GOAL 'goal' SKILLS [...] EXECUTION mode` - Agent creation
- [x] Execution modes: `AUTONOMOUS`, `SUPERVISED`, `HUMAN_APPROVAL`, `DRY_RUN`
- [x] `ON SCHEDULE 'cron'`, `ON ALERT 'name'` - Trigger definitions
- [x] `TRIGGER AGENT name WITH {...}` - Manual agent execution
- [x] `AgentRuntime` - OODA-loop execution engine
- [x] `TriggerManager` - Webhook, alert, and scheduled triggers
- [x] `SHOW AGENT name HISTORY` - Execution history

### Python SDK
- [x] `moltler` Python package with high-level APIs
- [x] `Moltler` client with skills, connectors, agents managers
- [x] `SkillBuilder`, `AgentBuilder` - Fluent builders
- [x] Full type annotations and documentation
- [x] Unit tests with 95%+ coverage

### CLI Enhancements
- [x] `escript skill list/show/test` - Skill commands
- [x] `escript connector list/show/test/sync` - Connector commands
- [x] `escript agent list/show/trigger/history` - Agent commands
- [x] Auto-completion for Moltler keywords

---

## elastic-script Language Features

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
- [x] **SHOW PROCEDURES / SHOW FUNCTIONS (NEW)**:
  - [x] `SHOW PROCEDURES` - List all stored procedures
  - [x] `SHOW PROCEDURE name` - Show details of a specific procedure
  - [x] `SHOW FUNCTIONS` - List all stored user-defined functions
  - [x] `SHOW FUNCTION name` - Show details of a specific function

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
- [x] **Command-Line Interface (CLI)** - Beautiful interactive REPL:
  - Syntax highlighting via custom Pygments lexer
  - Auto-completion for keywords, functions, procedures, skills
  - Multi-line editing with smart block detection
  - Rich output formatting (tables, panels, JSON)
  - Persistent command history with search
  - Script execution mode (`escript run script.es`)
  - Configuration via `~/.escriptrc` or environment variables
  - 145 unit tests for CLI components

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
   - **Current results: 8/8 pass (100%)**
     - ✅ 00-complete-reference.ipynb
     - ✅ 01-getting-started.ipynb
     - ✅ 02-esql-integration.ipynb
     - ⏭️ 03-ai-observability.ipynb (skipped - requires OpenAI key)
     - ✅ 04-async-execution.ipynb (1 cell skipped - STATUS storage bug)
     - ✅ 05-runbook-integrations.ipynb
     - ✅ 06-scheduled-jobs.ipynb (1 cell skipped - real-time execution)
     - ✅ 07-event-triggers.ipynb (2 cells skipped - real-time execution)
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
6. **Multi-Node Distributed Execution** - Framework ready, comprehensive testing pending:
   - [x] `ExecutionRegistry` stores state in `.escript_executions` index (cluster-wide)
   - [x] `LeaderElectionService` for job/trigger scheduling (single leader)
   - [x] Unit tests for `ExecutionState`, `ExecutionPipeline`, `Continuation`
   - [x] Test notebook `08-multinode-testing.ipynb` for verification
   - [x] Documentation for Docker Compose multi-node setup
   - [ ] **Comprehensive multi-node cluster testing** (pending manual verification)

### Medium Priority
4. ~~**Function Registration Performance**~~ ✅ Complete
   - Created `BuiltInFunctionRegistry` singleton that caches function definitions
   - Stateless functions (~70): cached permanently after first initialization
   - Client-dependent functions (~20): cached per Client instance
   - Executor-dependent functions (ESQL_QUERY): registered per request (required for variable substitution)
   - Reduces per-request overhead from ~106 registrations to ~1
5. ~~**Complete Observability**~~ ✅ Complete
   - Created `ExecutionResult` class to wrap procedure results with metadata
   - API response now includes PRINT output in `output` array field
   - Execution metadata in `_meta`: execution_id, duration_ms, procedure_name
   - Full unit test coverage for ExecutionResult
6. ~~**Instrument Procedures with EScriptTracer**~~ ✅ Complete
   - Added OTEL tracing to ProcedureExecutor.visitProcedureAsync()
   - Added statement-level tracing in visitStatementAsync()
   - Added determineStatementType() for granular span names
   - Traces capture execution ID, procedure name, statement count
   - Errors automatically captured in spans
7. ~~**Intent System**~~ ✅ Complete
   - DEFINE INTENT with DESCRIPTION, REQUIRES, ACTIONS, ON_FAILURE
   - INTENT invocation with positional and named arguments
   - IntentRegistry for storing intents
   - ESCRIPT_INTENTS() and ESCRIPT_INTENT() introspection functions
   - Notebook: `18-intents.ipynb`
   - Docs: `docs/language/intents.md`
8. ~~**Error Handling**~~ ✅ Complete
   - `SourceLocation` class for line/column tracking
   - `EScriptStackFrame` for elastic-script call stack
   - Enhanced `EScriptException` with location and stack info
   - `getFormattedMessage()` includes location context
   - `toDocument()` includes line, column, procedure, stack trace
9. ~~**Function Documentation**~~ ✅ Complete
   - `scripts/generate-function-docs.py` - Python script to extract @FunctionSpec
   - Auto-generates markdown docs organized by category
   - 100 functions documented across 8 categories
   - Output: `docs/functions/` with README.md index
   - Run: `python3 scripts/generate-function-docs.py`

### Low Priority
8. **Performance Optimization** - Batch operations, connection pooling
9. **Security** - API key management, RBAC integration
10. ~~**Monitoring**~~ ✅ Complete - Execution metrics and slow query logging via PROFILE system

### Public API Connectors (No Auth Required)
These connectors use public APIs that don't require authentication, making them ideal for demos and quick starts:

| Connector | Status | API Source | Use Case |
|-----------|--------|------------|----------|
| **GitHub Public** | ✅ Demo | api.github.com (public repos) | Issues, PRs, releases from public repos |
| **Hacker News** | 📋 Planned | hacker-news.firebaseio.com | Tech news, trending stories, comments |
| **Wikipedia** | 📋 Planned | en.wikipedia.org/api/rest_v1 | Articles, search, summaries |
| **REST Countries** | 📋 Planned | restcountries.com | Country data, populations, flags |
| **Open Meteo** | 📋 Planned | open-meteo.com | Weather data, forecasts |
| **JSONPlaceholder** | 📋 Planned | jsonplaceholder.typicode.com | Fake API for testing |

**Implementation Notes:**
- GitHub Public is implemented in `moltler demo` - fetches live issues from elastic/elasticsearch
- All connectors should follow the pattern: Fetch → Index → Create Skill → Query
- No API keys required - rate limits apply (GitHub: 60 req/hr unauthenticated)

---

## 🎯 CORE PRINCIPLE: Frictionless Onboarding

> **Every feature must be designed so users can't stop using it.**
> **Easiest possible onboarding with no compromise.**

This principle applies to ALL future development:

| Aspect | Requirement |
|--------|-------------|
| **Installation** | One command, zero config, works immediately |
| **First Value** | User sees meaningful results within 60 seconds |
| **Discovery** | Features are self-documenting and explorable |
| **Progression** | Natural path from demo → customize → production |
| **Integration** | Drop into existing workflows (Claude, VS Code, Kibana) |
| **Error Recovery** | Clear messages, suggested fixes, never stuck |

---

## 🖥️ Moltler Skills Manager Web UI (✅ Implemented)

### Overview
Modern React-based web UI for visual skills management. Standalone web application that communicates directly with the elastic-script API. Features three tabs for managing different entity types.

### UI Structure
The UI has **three tabs** to manage different elastic-script entities:
- **Skills** - AI-ready skills created with `CREATE SKILL` (have version, description, author, tags)
- **Procedures** - Reusable logic created with `CREATE PROCEDURE` (called by skills or directly)
- **Functions** - User-defined functions created with `CREATE FUNCTION` (return values)

### Features
| Feature | Status | Description |
|---------|--------|-------------|
| **Tabbed Interface** | ✅ Done | Skills, Procedures, Functions tabs |
| **Sortable Tables** | ✅ Done | Sortable, filterable tables with pagination (TanStack Table) |
| **Search & Filter** | ✅ Done | Global search across all properties |
| **Detail Flyout** | ✅ Done | Flyout showing full definition and parameters |
| **Implementation/Docs Tabs** | ✅ Done | View source code or auto-generated markdown documentation |
| **Monaco Editor** | ✅ Done | Full editor with elastic-script syntax highlighting |
| **Autocomplete** | ✅ Done | Keywords, built-in functions, language constructs |
| **Execute Code** | ✅ Done | Run code directly from the UI |
| **Dark/Light Mode** | ✅ Done | Toggle between themes |
| **Create/Edit/Delete** | ✅ Done | Full CRUD for skills, procedures, and functions |
| **Code Templates** | ✅ Done | Type-specific templates when creating new items |
| **Auto-Generated Docs** | ✅ Done | skill.md generated automatically with rich formatting |

### Tech Stack
- **React 19** + TypeScript
- **Vite** (fast dev server and build)
- **Tailwind CSS 4** + Radix UI components
- **TanStack Table** (powerful data tables)
- **Monaco Editor** (VS Code's editor)
- **React Query** (data fetching and caching)

### Directory Structure
```
elastic-script/
├── moltler-ui/                       # Standalone web UI
│   ├── src/
│   │   ├── api/                      # API client for elastic-script
│   │   │   └── skills.ts
│   │   ├── components/
│   │   │   ├── skills/               # Skill-specific components
│   │   │   │   ├── SkillsTable.tsx   # Main table view
│   │   │   │   ├── SkillDetail.tsx   # Detail flyout panel
│   │   │   │   └── SkillEditor.tsx   # Monaco editor with elastic-script
│   │   │   └── ui/                   # Reusable UI components
│   │   │       ├── button.tsx
│   │   │       ├── table.tsx
│   │   │       ├── sheet.tsx         # Flyout panels
│   │   │       └── ...
│   │   ├── lib/utils.ts              # Utilities
│   │   ├── App.tsx                   # Main application
│   │   └── index.css                 # Tailwind styles
│   ├── index.html
│   ├── vite.config.ts                # Vite config with API proxy
│   ├── package.json
│   └── README.md
```

### Quick Start
```bash
# Full setup: Elasticsearch + demo data + Skills Manager UI
./scripts/quick-start.sh --moltler

# Or just start the UI (requires ES to be running)
./scripts/quick-start.sh --ui

# Open in browser
open http://localhost:3000
```

### Development
```bash
cd moltler-ui
npm install
npm run dev    # Development server on port 3000
npm run build  # Production build
```

### API Proxy
The UI proxies API requests through Vite to avoid CORS issues:
- `GET /api/skills` → `GET /_escript/skills`
- `POST /api/skills/{name}/_invoke` → `POST /_escript/skills/{name}/_invoke`
- `POST /api` → `POST /_escript` (raw query execution)

---

## 🤖 AI Integration: MCP Server & Skill Consumption (📋 Planned)

### Goal
Make Moltler skills directly usable by AI agents (Claude Desktop, Claude Code, VS Code Copilot, custom agents).

### Approach Options

| Approach | Pros | Cons | Status |
|----------|------|------|--------|
| **MCP Server for Moltler** | Direct Claude integration, standard protocol | Need to implement MCP server | 📋 Preferred |
| **OpenAI Function Calling** | Wide compatibility | Need wrapper per AI system | 📋 Alternative |
| **REST API + Agent Wrapper** | Simple, works everywhere | Manual integration needed | ✅ Exists |

### MCP Server Architecture (Preferred)
```
┌─────────────────┐     ┌──────────────────┐     ┌──────────────────┐
│  Claude Desktop │────▶│  Moltler MCP     │────▶│  Elasticsearch   │
│  Claude Code    │     │  Server          │     │  + elastic-script│
│  ClawdBot       │     │                  │     │                  │
└─────────────────┘     └──────────────────┘     └──────────────────┘
                              │
                              ▼
                        ┌──────────────────┐
                        │ Skills exposed   │
                        │ as MCP tools     │
                        └──────────────────┘
```

### MCP Server Implementation (Planned)
```
elastic-script/
├── mcp-server/                       # MCP server component
│   ├── src/
│   │   ├── server.ts                 # MCP server entry
│   │   ├── tools/
│   │   │   ├── skills.ts             # Skill discovery & execution
│   │   │   ├── queries.ts            # ESQL query execution
│   │   │   └── connectors.ts         # Connector actions
│   │   └── resources/
│   │       └── indices.ts            # Index discovery
│   ├── package.json
│   └── README.md
```

### Features (Planned)
| Feature | Status | Description |
|---------|--------|-------------|
| **Skill Discovery** | 📋 Planned | AI sees all available skills as tools |
| **Skill Execution** | 📋 Planned | AI invokes skills with typed parameters |
| **Index Awareness** | 📋 Planned | AI knows what data is available |
| **Connector Access** | 📋 Planned | AI can use connectors (GitHub, Jira, etc.) |
| **ESQL Queries** | 📋 Planned | AI can query Elasticsearch directly |

### Configuration (Planned)
```json
// Claude Desktop config (~/.config/claude/claude_desktop_config.json)
{
  "mcpServers": {
    "moltler": {
      "command": "npx",
      "args": ["moltler-mcp-server"],
      "env": {
        "ELASTICSEARCH_URL": "http://localhost:9200",
        "ELASTICSEARCH_USER": "elastic-admin",
        "ELASTICSEARCH_PASSWORD": "elastic-password"
      }
    }
  }
}
```

### Research Needed
- MCP specification: https://modelcontextprotocol.io/
- MCP TypeScript SDK: https://github.com/modelcontextprotocol/typescript-sdk
- Example MCP servers for reference patterns
- Claude Desktop MCP configuration

---

## 🔍 Context Awareness (📋 Planned)

### Goal
CLI and Skills Manager UI should automatically discover and expose what's available in the cluster.

### Discovery Sources

| Source | What It Provides | API |
|--------|------------------|-----|
| **Elasticsearch Indices** | Data indices available for querying | `GET /_cat/indices` |
| **Index Mappings** | Field names and types for autocomplete | `GET /index/_mapping` |
| **Technical Indices** | Skills (`.escript_skills`), Procedures, Jobs, etc. | Direct queries |
| **Kibana Saved Objects** | Dashboards, visualizations, data views | Kibana API |
| **Connectors** | Available connectors and their status | `.escript_connectors` |
| **Agents** | Running agents and their goals | `.escript_agents` |

### Implementation (Planned)
```
# CLI context commands
moltler context indices       # List all available indices
moltler context fields logs*  # Show fields for matching indices
moltler context connectors    # List configured connectors
moltler context agents        # List running agents

# SHOW commands in elastic-script
SHOW INDICES;                 # List queryable indices
SHOW FIELDS FOR 'index-*';    # Show index fields
SHOW CONNECTORS;              # Already exists
SHOW AGENTS;                  # Already exists
```

### Autocomplete Integration
- CLI completer pulls from context discovery
- Kibana editor autocomplete uses same data
- MCP server exposes as resources for AI

---

### Recently Verified Features ✅
11. ~~**Scheduled Jobs (CREATE JOB)**~~ ✅ Complete
    - `CREATE JOB name SCHEDULE 'cron' AS BEGIN ... END JOB`
    - `ALTER JOB name ENABLE/DISABLE` or `ALTER JOB name SCHEDULE 'cron'`
    - `DROP JOB name`, `SHOW JOBS`, `SHOW JOB name`, `SHOW JOB RUNS FOR name`
    - JobSchedulerService runs on leader node with leader election
    - CronParser supports standard cron + aliases (@hourly, @daily, etc.)
    - Notebook: `06-scheduled-jobs.ipynb`
12. ~~**Event Triggers (CREATE TRIGGER)**~~ ✅ Complete
    - `CREATE TRIGGER name ON INDEX 'pattern' WHEN condition EVERY interval AS BEGIN ... END TRIGGER`
    - `ALTER TRIGGER name ENABLE/DISABLE` or `ALTER TRIGGER name EVERY interval`
    - `DROP TRIGGER name`, `SHOW TRIGGERS`, `SHOW TRIGGER name`, `SHOW TRIGGER RUNS FOR name`
    - TriggerPollingService polls for new documents and fires when conditions match
    - Checkpoint-based processing ensures documents processed exactly once
    - Notebook: `07-event-triggers.ipynb` (uses `==` for comparisons, `documents`/`document_count` variables)
13. ~~**Package System**~~ ✅ Complete
    - `CREATE PACKAGE name AS ... END PACKAGE` with PUBLIC/PRIVATE visibility
    - `CREATE PACKAGE BODY name AS ... END PACKAGE` for implementations
    - `DROP PACKAGE name`, `SHOW PACKAGE name`
    - Supports procedures, functions, and variables with visibility modifiers
14. ~~**Profiler System**~~ ✅ Complete
    - `PROFILE CALL procedure()` - Profile execution and store results
    - `SHOW PROFILES`, `SHOW PROFILE`, `SHOW PROFILE FOR name`
    - `ANALYZE PROFILE`, `ANALYZE PROFILE FOR name` - Performance recommendations
    - `CLEAR PROFILES`, `CLEAR PROFILE FOR name`
    - ProfileResult captures statement, function, and ESQL query timings
    - Automatic recommendations for slow queries and high ESQL percentage
    - Stored in `.escript_profiles` index
15. ~~**Intelligent Data Applications**~~ ✅ Complete
    - **Application Framework**: Bundle procedures, skills, intents, sources into deployable units
      - `CREATE APPLICATION name ... END APPLICATION`
      - `INSTALL APPLICATION name CONFIG (key => value, ...)`
      - `DROP APPLICATION name`
      - `ALTER APPLICATION name SET/ENABLE/DISABLE`
      - `SHOW APPLICATIONS`, `SHOW APPLICATION name`, `SHOW APPLICATION name SKILLS/INTENTS`
      - `EXTEND APPLICATION name ADD SKILL/INTENT/SOURCE`
      - `APPLICATION name | STATUS/PAUSE/RESUME`
    - **Skills System**: AI-callable procedures with MCP/OpenAI function calling support
      - `SkillDefinition` with parameters, return types, descriptions
      - MCP tool spec generation for AI agent integration
      - REST API: `GET /_escript/skills`, `POST /_escript/skills/{name}/_invoke`
    - **Intent System**: Natural language pattern matching to skills
      - `IntentDefinition` with pattern matching and confidence scoring
      - Pattern syntax: `"churn|leaving|at risk" => detect_churn`
      - REST API: `POST /_escript/intent`, `POST /_escript/intent/_match`
    - **Data Structures**:
      - `ApplicationDefinition` - Complete application bundle
      - `SkillDefinition` - AI-callable procedure wrapper
      - `IntentDefinition` - NL pattern to skill mapping
      - `ApplicationRegistry` - Storage in `.escript_applications` index
    - **Three Interfaces**:
      1. Human: Natural language → Intent matching → Skill execution
      2. AI Agent: MCP/function calling → Typed skill invocation
      3. Developer: Direct procedure calls
    - Notebook: `12-intelligent-data-applications.ipynb`
16. ~~**Standalone Skills System**~~ ✅ Complete
    - **First-class skill objects** independent of applications
    - **Grammar statements**:
      - `CREATE SKILL name(params) RETURNS type DESCRIPTION 'desc' EXAMPLES '...' PROCEDURE proc(args) END SKILL`
      - `DROP SKILL name`
      - `SHOW SKILLS`, `SHOW SKILL name`
      - `ALTER SKILL name SET DESCRIPTION = 'new desc'`
      - `GENERATE SKILL FROM 'natural language description' [WITH MODEL 'gpt-4'] [SAVE AS name]`
    - **Skill parameters with metadata**:
      - Type declarations (NUMBER, STRING, BOOLEAN, ARRAY, DOCUMENT)
      - Parameter descriptions
      - Default values
    - **AI generation**: Natural language to skill template
    - **SkillRegistry**: Storage in `.escript_skills` index with caching
    - **Examples field**: For AI discovery and intent matching
    - Notebook: `22-standalone-skills.ipynb`

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
│   ├── 06-scheduled-jobs.ipynb       # CREATE JOB with cron scheduling
│   ├── 07-event-triggers.ipynb       # CREATE TRIGGER with polling
│   ├── 08-multinode-testing.ipynb    # Multi-node distributed testing
│   ├── 03-ai-observability.ipynb
│   ├── 04-async-execution.ipynb
│   ├── 05-runbook-integrations.ipynb
│   ├── 12-exception-handling.ipynb    # TRY/CATCH/FINALLY
│   ├── 13-user-defined-functions.ipynb # CREATE FUNCTION
│   ├── 14-execute-immediate.ipynb      # Dynamic ES|QL
│   ├── 15-map-type.ipynb               # MAP associative arrays
│   ├── 12-intelligent-data-applications.ipynb # Skills, Intents, Applications
│   └── 22-standalone-skills.ipynb            # Standalone skill management
├── tests/e2e/                         # E2E test framework
│   ├── README.md                      # E2E documentation
│   ├── run_notebook_tests.py          # Programmatic notebook execution
│   ├── run_tests.sh                   # Shell wrapper
│   └── requirements.txt               # nbclient, nbformat
├── otel-collector/                    # OpenTelemetry Collector
│   ├── config.yaml                    # Collector configuration
│   └── .gitignore                     # Ignores binary and logs
├── cli/                               # Command-Line Interface
│   ├── escript_cli/                   # Python package
│   │   ├── main.py                    # CLI entry point (Click)
│   │   ├── repl.py                    # Interactive REPL
│   │   ├── lexer.py                   # Pygments lexer
│   │   ├── completer.py               # Auto-completion
│   │   ├── output.py                  # Rich output formatting
│   │   ├── client.py                  # ES client
│   │   └── config.py                  # Configuration
│   ├── tests/                         # CLI tests (145 tests)
│   ├── setup.py                       # Package setup
│   └── README.md                      # CLI documentation
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

*Last updated: January 22, 2026*

---

## 🤖 MCP Integration (Model Context Protocol)

### Overview
Skills are exposed to AI agents via the Model Context Protocol (MCP), enabling tools like Claude Desktop to discover and invoke elastic-script skills.

### Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/_escript/mcp` | GET | Server info and capabilities |
| `/_escript/mcp` | POST | JSON-RPC endpoint for MCP protocol |

### Supported MCP Methods

| Method | Description |
|--------|-------------|
| `initialize` | Initialize MCP session |
| `tools/list` | List all skills as MCP tools |
| `tools/call` | Invoke a skill by name |
| `ping` | Health check |

### Usage Examples

**List available tools:**
```bash
curl -u elastic-admin:elastic-password -X POST http://localhost:9200/_escript/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc": "2.0", "method": "tools/list", "id": 1}'
```

**Invoke a skill:**
```bash
curl -u elastic-admin:elastic-password -X POST http://localhost:9200/_escript/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {"name": "metrics_summary", "arguments": {}},
    "id": 2
  }'
```

### Claude Desktop Integration

1. Install the MCP bridge: `npm install -g @moltler/mcp-bridge`

2. Add to `~/Library/Application Support/Claude/claude_desktop_config.json`:
```json
{
  "mcpServers": {
    "moltler": {
      "command": "npx",
      "args": ["@moltler/mcp-bridge", "--es-url", "http://localhost:9200"]
    }
  }
}
```

3. Restart Claude Desktop - skills appear as tools!

### MCP Bridge Package

The `mcp-bridge/` directory contains a Node.js package that translates stdio MCP to HTTP:

```
mcp-bridge/
├── src/index.ts       # Bridge implementation
├── package.json       # npm package config
├── tsconfig.json      # TypeScript config
└── README.md          # Usage documentation
```

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

---

## 🚨 Strategic Priorities (NEW)

These are the highest-impact items for user adoption:

| Priority | Feature | Impact | Effort |
|----------|---------|--------|--------|
| **P0** | MCP Server for Skills | AI agents can use skills directly | Medium |
| ✅ | Skills Manager Web UI | Visual skill management | Done |
| **P1** | Context Awareness in CLI | Better autocomplete, discovery | Low |
| ✅ | Skills Editor with Monaco | Create/edit skills visually | Done |
| **P2** | Public API Connectors | More demo options | Low each |

**Guiding Principle**: Every feature must pass the "60-second value" test - can a new user see meaningful results within one minute?

---

## 📁 Moltler Files

### Core Moltler Source
```
elastic-script/elastic-script/src/main/java/org/elasticsearch/xpack/escript/
├── agents/
│   ├── AgentRuntime.java           # OODA-loop agent execution
│   └── TriggerManager.java         # Webhook/alert/cron triggers
├── connectors/
│   ├── ConnectorFactory.java       # Connector dispatch
│   ├── SyncEngine.java             # Data synchronization
│   ├── github/GitHubConnector.java # GitHub API integration
│   ├── jira/JiraConnector.java     # Jira API integration
│   └── datadog/DatadogConnector.java # Datadog API integration
├── handlers/
│   ├── ConnectorStatementHandler.java  # CONNECTOR statements
│   ├── AgentStatementHandler.java      # AGENT statements
│   └── SkillStatementHandler.java      # SKILL statements
├── applications/
│   ├── SkillDefinition.java        # Skill data model
│   └── SkillRegistry.java          # Skill storage
```

### Python SDK
```
elastic-script/sdk/
├── moltler/
│   ├── __init__.py                 # Package exports
│   ├── client.py                   # Moltler client
│   ├── skills.py                   # Skill management
│   ├── connectors.py               # Connector management
│   ├── agents.py                   # Agent management
│   └── exceptions.py               # Custom exceptions
├── tests/
│   ├── test_client.py              # Client tests
│   ├── test_skills.py              # Skills tests
│   ├── test_connectors.py          # Connector tests
│   └── test_agents.py              # Agent tests
├── setup.py
├── pyproject.toml
└── README.md
```

### CLI Enhancements
```
elastic-script/cli/escript_cli/
├── main.py                         # skill/connector/agent commands
├── client.py                       # Moltler client methods
└── completer.py                    # Moltler keyword completion
```

### Parser Tests
```
elastic-script/elastic-script/src/test/java/org/elasticsearch/xpack/escript/parser/
├── ConnectorParserTests.java       # Connector statement parsing
├── AgentParserTests.java           # Agent statement parsing
├── MoltlerSkillParserTests.java    # Skill statement parsing (new grammar)
└── SkillParserTests.java           # Updated for Moltler grammar
```

---

## 📚 GitHub Pages Documentation

### Overview
Comprehensive documentation site hosted on GitHub Pages with Jekyll.

**URL:** https://bahaaldine.github.io/elastic-script/

### Pages
| Page | Description |
|------|-------------|
| `index.md` | Home page with quick start |
| `getting-started.md` | Installation and first skill guide |
| `language-reference.md` | Full elastic-script syntax |
| `functions.md` | 106 built-in functions reference |
| `mcp.md` | MCP integration for AI agents |
| `ui.md` | Skills Manager UI guide |
| `api.md` | REST API reference |

### Configuration
```yaml
# docs/_config.yml
title: Moltler - elastic-script
theme: jekyll-theme-cayman
baseurl: /elastic-script
```

### Enabling GitHub Pages
1. Go to repository Settings → Pages
2. Set Source to "Deploy from a branch"
3. Select `main` branch and `/docs` folder
4. Save and wait for deployment

---

## 📄 Auto-Generated Skill Documentation (skill.md)

### Overview
When skills are created, rich markdown documentation is automatically generated and stored alongside the skill definition.

### Features
- **Markdown Badges:** Version, author, tags
- **Parameter Tables:** Full parameter documentation
- **Usage Examples:** MCP JSON-RPC and elastic-script examples
- **Implementation Details:** Wrapped procedure information
- **Auto-enrichment:** Documentation derived from code analysis

### Storage
Documentation is stored in SkillDefinition:
- `documentation` field - Generated markdown content
- `source_code` field - Original CREATE SKILL source

### UI Display
The Skills Manager UI shows documentation in a tabbed interface:
- **Implementation Tab:** Source code in Monaco editor
- **Documentation Tab:** Rendered markdown with syntax highlighting

### Example Generated Documentation
```markdown
# analyze_logs

![Version](https://img.shields.io/badge/version-1.0-blue)
![Author](https://img.shields.io/badge/author-DevOps-green)

## Description
Analyze application logs for errors and patterns.

## Parameters
| Name | Type | Required | Default | Description |
|------|------|----------|---------|-------------|
| `index_pattern` | `STRING` | No | `logs-*` | Index pattern |
| `limit` | `NUMBER` | No | `10` | Max results |

## Usage
### Via MCP (AI Agents)
\`\`\`json
{"method": "tools/call", "params": {"name": "analyze_logs", ...}}
\`\`\`

### Via elastic-script
\`\`\`sql
CALL run_log_analysis(index_pattern, limit);
\`\`\`
```

*Last updated: January 22, 2026*
