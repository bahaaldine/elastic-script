# AI & Observability

!!! tip "Interactive Notebook"
    This page is generated from a Jupyter notebook. For the best experience, 
    run it interactively in Jupyter:
    
    ```bash
    ./scripts/quick-start.sh
    # Then open http://localhost:8888/notebooks/03-ai-observability.ipynb
    ```
    
    [:fontawesome-brands-github: View on GitHub](https://github.com/bahaaldine/elastic-script/blob/main/notebooks/03-ai-observability.ipynb){ .md-button }

---

# 🤖 AI-Powered Observability

This notebook demonstrates how to use elastic-script with AI/LLM capabilities for intelligent log analysis and observability automation.

## Features Covered
- Using LLM_COMPLETE() for natural language analysis
- Semantic search with embeddings
- Intelligent incident investigation
- Automated report generation


## Error Log Summarization

Query Elasticsearch for error logs and use an LLM to analyze patterns:


```sql
CREATE PROCEDURE summarize_errors()
BEGIN
    DECLARE error_logs CURSOR FOR
        FROM logs-sample
        | WHERE level == "ERROR"
        | LIMIT 50;

    DECLARE error_messages STRING = '';
    DECLARE error_count NUMBER = 0;

    FOR log_entry IN error_logs LOOP
        SET error_messages = error_messages || log_entry['message'] || '\n';
        SET error_count = error_count + 1;
    END LOOP

    IF error_count > 0 THEN
        DECLARE prompt STRING = 'Analyze these error logs and provide a summary:\n\n' || error_messages;
        DECLARE summary STRING = LLM_COMPLETE(prompt);

        PRINT 'Found ' || error_count || ' errors. Summary:';
        PRINT summary;
        RETURN summary;
    ELSE
        PRINT 'No errors found in the logs.';
        RETURN 'No errors found';
    END IF
END PROCEDURE

```

```sql
CALL summarize_errors()

```

## Anomaly Detection

Scan for unusual patterns and get AI-powered analysis:


```sql
CREATE PROCEDURE detect_anomalies()
BEGIN
    DECLARE recent_errors CURSOR FOR
        FROM logs-sample
        | WHERE level == "ERROR"
        | SORT @timestamp DESC
        | LIMIT 30;

    DECLARE error_timeline STRING = 'Recent errors:\n';
    DECLARE count NUMBER = 0;

    FOR err IN recent_errors LOOP
        SET error_timeline = error_timeline || err['service'] || ': ' || err['message'] || '\n';
        SET count = count + 1;
    END LOOP

    IF count > 10 THEN
        DECLARE prompt STRING = 'Analyze this error timeline for anomalies. Provide urgency level (LOW/MEDIUM/HIGH/CRITICAL):\n\n' || error_timeline;
        DECLARE analysis STRING = LLM_COMPLETE(prompt);

        PRINT '⚠️ ANOMALY DETECTION ALERT';
        PRINT 'Error count: ' || count;
        PRINT analysis;
        RETURN analysis;
    ELSE
        PRINT '✓ System appears healthy. Only ' || count || ' errors detected.';
        RETURN 'Healthy';
    END IF
END PROCEDURE

```

```sql
CALL detect_anomalies()

```

## Semantic Search

Use embeddings for semantic log search:


```sql
CREATE PROCEDURE semantic_search(query STRING)
BEGIN
    DECLARE embedding ARRAY = INFERENCE_EMBED('.multilingual-e5-small-elasticsearch', query);
    PRINT 'Query: ' || query;
    PRINT 'Embedding dimensions: ' || ARRAY_LENGTH(embedding);
    RETURN embedding;
END PROCEDURE

```

```sql
CALL semantic_search('database connection timeout error')

```

