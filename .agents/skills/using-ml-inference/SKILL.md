---
name: using-ml-inference
description: >
  Use ML and AI capabilities including anomaly detection, trained models, embeddings,
  and LLM integration. Use when needing anomaly detection, text classification,
  NER, embeddings, semantic search, or AI-powered analysis.
---

# ML & Inference

## Anomaly Detection Jobs

| Function | Example |
|----------|---------|
| `ES_ML_GET_JOBS(job_id?)` | `ES_ML_GET_JOBS('_all')` |
| `ES_ML_GET_JOB_STATS(job_id?)` | Job statistics |
| `ES_ML_OPEN_JOB(job_id)` / `ES_ML_CLOSE_JOB(job_id)` | Control jobs |
| `ES_ML_GET_RECORDS(job_id, start?, end?)` | Anomaly records |
| `ES_ML_GET_BUCKETS(job_id, start?, end?)` | Result buckets |

```sql
SET anomalies = ES_ML_GET_RECORDS('response-time-job', 'now-1d', 'now');
FOR record IN anomalies['records'] LOOP
    IF record['record_score'] > 75 THEN
        PRINT 'High anomaly: ' || record['function'] || ' = ' || record['actual'];
    END IF;
END LOOP;
```

## Trained Models (NLP)

| Function | Example |
|----------|---------|
| `ES_ML_GET_TRAINED_MODELS(model_id?)` | List models |
| `ES_ML_GET_TRAINED_MODEL_STATS(model_id?)` | Model stats |
| `ES_ML_INFER(model_id, docs)` | Run inference |

```sql
-- Text classification
SET result = ES_ML_INFER('sentiment-model', [
    {'text_field': 'Great product!'},
    {'text_field': 'Terrible experience.'}
]);

-- NER
SET result = ES_ML_INFER('ner-model', [
    {'text_field': 'John Smith works at Microsoft.'}
]);
```

## Inference API

| Function | Example |
|----------|---------|
| `INFERENCE(model, input)` | Generic inference |
| `INFERENCE_EMBED(model, text)` | Embeddings |
| `INFERENCE_CHAT(model, messages)` | Chat completion |
| `INFERENCE_RERANK(model, query, docs)` | Rerank documents |

```sql
-- Embeddings for vector search
SET embedding = INFERENCE_EMBED('my-model', 'What is Kubernetes?');
SET similar = ES_KNN_SEARCH('knowledge-base', embedding, 5);
```

## OpenAI/Azure OpenAI

| Function | Description |
|----------|-------------|
| `LLM_COMPLETE(prompt)` | Text completion |
| `LLM_CHAT(messages)` | Chat completion |
| `LLM_EMBED(text)` | Embeddings |
| `LLM_SUMMARIZE(text)` | Summarization |
| `LLM_CLASSIFY(text, labels)` | Classification |
| `LLM_EXTRACT(text, schema)` | Structured extraction |

```sql
-- Summarize errors
ESQL FROM logs-* | WHERE @timestamp > NOW() - 1 HOUR AND level = 'ERROR' | KEEP message | LIMIT 20 INTO errors;
SET summary = LLM_SUMMARIZE('Summarize these errors:\n' || ARRAY_JOIN(ARRAY_MAP(errors, x -> x['message']), '\n'));
```

## Pre-built Skills

| Skill | Description |
|-------|-------------|
| `RUN SKILL get_anomalies(job_id, threshold)` | High anomalies |
| `RUN SKILL embed_text(model, text)` | Embeddings |
| `RUN SKILL summarize_errors(index, minutes)` | AI summarization |
| `RUN SKILL semantic_search(query, index)` | Semantic search |

## Best Practices

- Use appropriate model sizes
- Batch inference calls
- Cache embeddings in ES
- Monitor ML job health
