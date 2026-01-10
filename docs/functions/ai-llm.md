# AI & LLM Functions

Integration with OpenAI and Elasticsearch Inference APIs for AI-powered automation.

## OpenAI Functions

### LLM_COMPLETE

Sends a prompt to OpenAI and returns the completion.

```sql
DECLARE prompt STRING = 'Explain what Elasticsearch is in one sentence.';
DECLARE response STRING = LLM_COMPLETE(prompt);
PRINT response;
-- Output: Elasticsearch is a distributed search and analytics engine...
```

**Syntax:** `LLM_COMPLETE(prompt)`

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| prompt | STRING | The prompt to send to the LLM |

**Returns:** STRING - The model's response

!!! note "API Key Required"
    Set the `OPENAI_API_KEY` environment variable before starting Elasticsearch.

---

### LLM_COMPLETE with Model Selection

Specify a particular OpenAI model:

```sql
DECLARE response STRING = LLM_COMPLETE(prompt, 'gpt-4');
```

**Syntax:** `LLM_COMPLETE(prompt, model)`

**Available Models:**

| Model | Description |
|-------|-------------|
| `gpt-4` | Most capable model |
| `gpt-4-turbo` | Faster GPT-4 variant |
| `gpt-3.5-turbo` | Fast and cost-effective |

---

### LLM_SUMMARIZE

Summarizes text content.

```sql
DECLARE long_text STRING = '... lengthy document content ...';
DECLARE summary STRING = LLM_SUMMARIZE(long_text);
PRINT summary;
```

**Syntax:** `LLM_SUMMARIZE(text)`

---

## Elasticsearch Inference API

### ES_INFERENCE

Uses Elasticsearch's built-in inference endpoints.

```sql
DECLARE text STRING = 'The server is experiencing high latency';
DECLARE embedding ARRAY = ES_INFERENCE('text-embedding', text);
```

**Syntax:** `ES_INFERENCE(endpoint_id, input)`

!!! tip "Inference Endpoints"
    Configure inference endpoints in Elasticsearch using the 
    [Inference API](https://www.elastic.co/guide/en/elasticsearch/reference/current/inference-apis.html).

---

## Example: Log Summarization

```sql
CREATE PROCEDURE summarize_errors()
BEGIN
    -- Fetch recent errors
    DECLARE errors CURSOR FOR ESQL_QUERY('
        FROM logs-*
        | WHERE level = "ERROR"
        | SORT @timestamp DESC
        | LIMIT 20
    ');
    
    -- Collect error messages
    DECLARE messages ARRAY = [];
    FOR error IN errors LOOP
        SET messages = ARRAY_APPEND(messages, DOCUMENT_GET(error, 'message'));
    END LOOP;
    
    IF ARRAY_LENGTH(messages) = 0 THEN
        PRINT 'No errors found';
        RETURN 'No errors to summarize';
    END IF;
    
    -- Create summary prompt
    DECLARE prompt STRING = 'Summarize these error messages and identify the root cause:\n\n' 
        || ARRAY_JOIN(messages, '\n');
    
    -- Get AI summary
    DECLARE summary STRING = LLM_COMPLETE(prompt);
    
    PRINT '=== Error Summary ===';
    PRINT summary;
    
    RETURN summary;
END PROCEDURE;
```

---

## Example: Intelligent Alert Classification

```sql
CREATE PROCEDURE classify_alert(alert_message STRING)
BEGIN
    DECLARE prompt STRING = 'Classify this alert into one of: CRITICAL, HIGH, MEDIUM, LOW.
Only respond with the classification level.

Alert: ' || alert_message;
    
    DECLARE classification STRING = LLM_COMPLETE(prompt);
    DECLARE level STRING = TRIM(UPPER(classification));
    
    -- Validate response
    IF level NOT IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW') THEN
        SET level = 'MEDIUM';  -- Default
    END IF;
    
    PRINT 'Alert classified as: ' || level;
    RETURN level;
END PROCEDURE;
```

---

## Example: Automated Incident Response

```sql
CREATE PROCEDURE generate_runbook(incident DOCUMENT)
BEGIN
    DECLARE service STRING = DOCUMENT_GET(incident, 'service');
    DECLARE error STRING = DOCUMENT_GET(incident, 'error');
    
    DECLARE prompt STRING = 'Generate a step-by-step runbook to resolve this incident:

Service: ' || service || '
Error: ' || error || '

Provide numbered steps that an on-call engineer can follow.';
    
    DECLARE runbook STRING = LLM_COMPLETE(prompt);
    
    -- Store runbook
    DECLARE doc DOCUMENT = {
        "incident_id": DOCUMENT_GET(incident, 'id'),
        "generated_runbook": runbook,
        "generated_at": CURRENT_TIMESTAMP()
    };
    ES_INDEX('incident-runbooks', NULL, doc);
    
    RETURN runbook;
END PROCEDURE;
```

---

## Example: Semantic Search Enhancement

```sql
CREATE PROCEDURE semantic_search(query STRING)
BEGIN
    -- Generate embedding for query
    DECLARE query_embedding ARRAY = ES_INFERENCE('text-embedding', query);
    
    -- Search using KNN
    DECLARE results CURSOR FOR ESQL_QUERY('
        FROM knowledge-base
        | WHERE knn(embedding, ' || query_embedding || ', 10)
        | LIMIT 5
    ');
    
    FOR result IN results LOOP
        PRINT DOCUMENT_GET(result, 'title');
        PRINT '  ' || DOCUMENT_GET(result, 'summary');
    END LOOP;
END PROCEDURE;
```

---

## Best Practices

!!! tip "Prompt Engineering"
    - Be specific in your prompts
    - Include context about the expected output format
    - Use examples when needed (few-shot prompting)

!!! warning "Cost Considerations"
    - OpenAI API calls incur costs
    - Cache results when possible
    - Use smaller models for simple tasks

!!! tip "Error Handling"
    ```sql
    TRY
        DECLARE response STRING = LLM_COMPLETE(prompt);
    CATCH error
        PRINT 'AI request failed: ' || error;
        -- Fallback logic
    END TRY;
    ```
