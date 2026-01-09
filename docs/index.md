# elastic-script

<div class="grid cards" markdown>

-   :material-lightning-bolt:{ .lg .middle } __Procedural Power__

    ---

    Write complex logic with variables, loops, conditionals, and error handling - all inside Elasticsearch.

    [:octicons-arrow-right-24: Getting Started](getting-started/installation.md)

-   :material-database:{ .lg .middle } __Native ES|QL Integration__

    ---

    Query your data with ES|QL and process results with cursors and loops.

    [:octicons-arrow-right-24: Elasticsearch Functions](functions/elasticsearch.md)

-   :material-robot:{ .lg .middle } __AI & LLM Built-in__

    ---

    Summarize logs, classify events, and generate insights with OpenAI or Elasticsearch Inference.

    [:octicons-arrow-right-24: AI Functions](functions/ai-llm.md)

-   :material-run-fast:{ .lg .middle } __Async Execution__

    ---

    Chain procedures with `ON_DONE`, `ON_FAIL`, and run tasks in parallel.

    [:octicons-arrow-right-24: Async Guide](language/async-execution.md)

</div>

---

## What is elastic-script?

**elastic-script** is a procedural scripting language that runs **inside Elasticsearch**. It's designed for:

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

## Key Features

### 🔧 106 Built-in Functions

| Category | Functions |
|----------|-----------|
| **String** | `LENGTH`, `SUBSTR`, `UPPER`, `LOWER`, `TRIM`, `REPLACE`, `REGEXP_REPLACE`... |
| **Array** | `ARRAY_LENGTH`, `ARRAY_APPEND`, `ARRAY_FILTER`, `ARRAY_MAP`, `ARRAY_REDUCE`... |
| **Elasticsearch** | `ESQL_QUERY`, `INDEX_DOCUMENT`, `GET_DOCUMENT`, `REFRESH_INDEX`... |
| **AI/LLM** | `LLM_COMPLETE`, `LLM_SUMMARIZE`, `LLM_CLASSIFY`, `INFERENCE`... |
| **Integrations** | `SLACK_SEND`, `PAGERDUTY_TRIGGER`, `K8S_SCALE`, `AWS_LAMBDA_INVOKE`... |

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

- Structured logging with execution IDs
- Elastic APM tracing (optional)
- Execution state persisted in `.escript_executions`

## Installation

=== "Quick Start"

    ```bash
    git clone https://github.com/bahaaldine/elastic-script.git
    cd elastic-script
    ./scripts/quick-start.sh
    ```

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
