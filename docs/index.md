# elastic-script

<div class="grid cards" markdown>

-   :material-robot:{ .lg .middle } __Agent-Native Language__

    ---

    A language designed for AI agents to generate, execute, and debug procedures on Elasticsearch data.

    [:octicons-arrow-right-24: Why Agents?](#why-agents)

-   :material-shield-check:{ .lg .middle } __Transparent & Debuggable__

    ---

    Every agent action is a procedure you can inspect, trace, and understand.

    [:octicons-arrow-right-24: Observability](#full-observability)

-   :material-database:{ .lg .middle } __Native ES|QL Integration__

    ---

    Query your data with ES|QL and process results with cursors and bulk operations.

    [:octicons-arrow-right-24: Elasticsearch Functions](functions/elasticsearch.md)

-   :material-lightning-bolt:{ .lg .middle } __Distributed & Scalable__

    ---

    Runs inside Elasticsearch with async execution, scheduled jobs, and event triggers.

    [:octicons-arrow-right-24: Async Guide](language/async-execution.md)

</div>

---

## What is elastic-script?

**elastic-script** is a **procedural language for AI agents** that runs **inside Elasticsearch**.

Unlike general-purpose code generation where agents produce arbitrary Python, JavaScript, or shell scripts, elastic-script provides a **constrained, purpose-built language** for data operations. When an AI agent uses elastic-script:

- **Actions are transparent** — Every operation is visible, auditable, and logged
- **Behavior is debuggable** — Procedures can be inspected, traced, and replayed
- **Capabilities are bounded** — Agents can only do what elastic-script permits
- **Execution is distributed** — Runs natively in Elasticsearch at scale

---

## Why Agents?

### The Problem with Arbitrary Code Generation

When AI agents generate arbitrary code (Python, SQL, shell scripts), you face:

1. **Security risks** — Unbounded access to file systems, networks, credentials
2. **Debugging nightmares** — Hard to trace what the agent actually did
3. **Inconsistent behavior** — Different execution environments, dependencies
4. **Audit challenges** — No structured record of actions taken

### The elastic-script Solution

elastic-script is **designed from the ground up for AI agents**:

| Challenge | elastic-script Solution |
|-----------|------------------------|
| **Security** | Sandboxed execution inside Elasticsearch; no file/network access outside defined functions |
| **Transparency** | Every procedure is stored, versioned, and inspectable |
| **Debugging** | Full execution traces, APM integration, structured logging |
| **Auditing** | Execution history in `.escript_executions` index |
| **Consistency** | Same language, same runtime, everywhere |

### How It Works

```
┌─────────────┐     generates      ┌──────────────────┐
│   AI Agent  │ ─────────────────► │  elastic-script  │
│  (LLM/MCP)  │                    │    Procedure     │
└─────────────┘                    └────────┬─────────┘
                                            │
                                   executes │
                                            ▼
                               ┌────────────────────────┐
                               │     Elasticsearch      │
                               │  ┌──────────────────┐  │
                               │  │  Logs, Metrics,  │  │
                               │  │  Events, Alerts  │  │
                               │  └──────────────────┘  │
                               └────────────────────────┘
```

The agent generates elastic-script code → the code runs inside Elasticsearch → the user can inspect exactly what happened.

---

## What Can Agents Do?

With elastic-script, AI agents can:

- **Runbook Automation** - Codify operational procedures that respond to alerts
- **Data Processing** - Transform and enrich data with complex logic
- **AI/ML Integration** - Leverage LLMs for log analysis and anomaly detection
- **Cross-System Orchestration** - Connect to Slack, PagerDuty, AWS, Kubernetes, and more

## Quick Example

```sql
CREATE PROCEDURE analyze_errors()
BEGIN
    -- Query recent errors
    DECLARE errors CURSOR FOR
        FROM logs-*
        | WHERE level == "ERROR"
        | WHERE @timestamp > NOW() - 1 HOUR
        | LIMIT 100;
    
    -- Count and summarize
    DECLARE error_count NUMBER = 0;
    DECLARE messages STRING = '';
    
    FOR error IN errors LOOP
        SET error_count = error_count + 1;
        SET messages = messages || error.message || '\n';
    END LOOP;
    
    -- Use AI to summarize if many errors
    IF error_count > 10 THEN
        DECLARE summary STRING = LLM_SUMMARIZE(messages);
        PRINT 'Error Summary: ' || summary;
    END IF;
    
    RETURN error_count;
END PROCEDURE;
```

## The Language

elastic-script is a **procedural language** with familiar SQL-like syntax:

- **Variables** - `DECLARE`, `VAR`, `CONST` with strong typing
- **Control Flow** - `IF/THEN/ELSE`, `FOR`, `WHILE`, `SWITCH/CASE`
- **Error Handling** - `TRY/CATCH/FINALLY` with structured exceptions
- **Functions** - `CREATE FUNCTION` with `IN`, `OUT`, `INOUT` parameters
- **Cursors** - Stream large result sets with `OPEN`, `FETCH`, `CLOSE`
- **Bulk Operations** - `FORALL` with `SAVE EXCEPTIONS` for batch processing
- **Dynamic SQL** - `EXECUTE IMMEDIATE` with bind variables

All designed to be **easy for agents to generate** and **easy for humans to read**.

---

## Key Features

### 🔧 118 Built-in Functions

| Category | Functions |
|----------|-----------|
| **String** | `LENGTH`, `SUBSTR`, `UPPER`, `LOWER`, `TRIM`, `REPLACE`, `REGEXP_REPLACE`... |
| **Array** | `ARRAY_LENGTH`, `ARRAY_APPEND`, `ARRAY_FILTER`, `ARRAY_MAP`, `ARRAY_REDUCE`... |
| **MAP** | `MAP_GET`, `MAP_PUT`, `MAP_KEYS`, `MAP_VALUES`, `MAP_MERGE`... |
| **Elasticsearch** | `ESQL_QUERY`, `INDEX_DOCUMENT`, `GET_DOCUMENT`, `REFRESH_INDEX`... |
| **AI/LLM** | `LLM_COMPLETE`, `LLM_SUMMARIZE`, `LLM_CLASSIFY`, `INFERENCE`... |
| **Integrations** | `SLACK_SEND`, `PAGERDUTY_TRIGGER`, `K8S_SCALE`, `AWS_LAMBDA_INVOKE`... |

### ⏰ Scheduled Jobs & Triggers

```sql
-- Run every day at 2 AM
CREATE JOB daily_cleanup
SCHEDULE '0 2 * * *'
AS BEGIN
    CALL archive_old_logs(30);
END JOB;

-- React to new documents
CREATE TRIGGER on_critical_error
ON INDEX 'logs-*'
WHEN level = 'ERROR' AND severity = 'critical'
EVERY 5 SECONDS
AS BEGIN
    CALL notify_oncall(@document);
END TRIGGER;
```

### ⚡ Async Execution

```sql
-- Chain procedures with continuations
analyze_logs()
    | ON_DONE notify_team(@result)
    | ON_FAIL alert_oncall(@error)
    | TRACK AS 'daily-analysis';

-- Run in parallel
PARALLEL [fetch_logs(), fetch_metrics()]
    | ON_ALL_DONE merge_results(@results);
```

### 🔍 Full Observability

- **Distributed Tracing** via integrated OTEL Collector (out of the box)
- Structured logging with execution IDs
- Traces visible in Kibana APM at http://localhost:5601/app/apm
- Execution state persisted in `.escript_executions`
- Full procedure history for debugging and auditing

[:octicons-arrow-right-24: OpenTelemetry Tracing](observability/opentelemetry.md)

## Installation

=== "Quick Start"

    ```bash
    git clone https://github.com/bahaaldine/elastic-script.git
    cd elastic-script
    ./scripts/quick-start.sh
    ```
    
    This starts **everything**:
    
    - Elasticsearch on port 9200
    - OTEL Collector on ports 4317/4318 (for distributed tracing)
    - Kibana on port 5601 (view traces at /app/apm)
    - Jupyter on port 8888

=== "Manual Setup"

    ```bash
    # Build the plugin
    cd elasticsearch
    ./gradlew :x-pack:plugin:elastic-script:assemble
    
    # Install in your ES cluster
    bin/elasticsearch-plugin install file:///path/to/elastic-script.zip
    ```

## Try It Now

Once running, create and call procedures:

```bash
# Create a procedure
curl -u elastic-admin:elastic-password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "CREATE PROCEDURE hello() BEGIN RETURN '\''Hello, World!'\''; END PROCEDURE;"}'

# Call it
curl -u elastic-admin:elastic-password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "CALL hello()"}'

# Response: {"result": "Hello, World!"}
```

Or use the **Jupyter notebook** interface for interactive development!

---

<div class="grid cards" markdown>

-   :material-book-open-page-variant:{ .lg .middle } __Read the Docs__

    [:octicons-arrow-right-24: Language Guide](language/overview.md)

-   :material-function:{ .lg .middle } __Function Reference__

    [:octicons-arrow-right-24: All 106 Functions](functions/overview.md)

-   :material-flask:{ .lg .middle } __Examples__

    [:octicons-arrow-right-24: Real-world Use Cases](examples/log-analysis.md)

</div>
