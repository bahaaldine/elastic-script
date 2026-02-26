---
name: ml-inference
description: Use machine learning and AI capabilities including ML jobs, trained models, embeddings, and LLM integration. Use when the user needs anomaly detection, NLP, text classification, or AI-powered analysis.
---

# Machine Learning & Inference

This skill enables you to use Elasticsearch's machine learning and inference capabilities, including anomaly detection, NLP, and LLM integration.

## When to Use

- User wants **anomaly detection** or **outlier analysis**
- User needs **text embeddings** or **semantic search**
- User asks for **text classification** or **NER**
- User wants to use **LLMs** for summarization or analysis
- User needs **ML-powered insights**

## ML Jobs (Anomaly Detection)

| Function | Description | Example |
|----------|-------------|---------|
| `ES_ML_GET_JOBS(job_id?)` | List ML jobs | `ES_ML_GET_JOBS('_all')` |
| `ES_ML_GET_JOB_STATS(job_id?)` | Get job statistics | `ES_ML_GET_JOB_STATS('my-job')` |
| `ES_ML_OPEN_JOB(job_id)` | Open a job | `ES_ML_OPEN_JOB('my-job')` |
| `ES_ML_CLOSE_JOB(job_id)` | Close a job | `ES_ML_CLOSE_JOB('my-job')` |
| `ES_ML_GET_RECORDS(job_id, start?, end?)` | Get anomaly records | See below |
| `ES_ML_GET_BUCKETS(job_id, start?, end?)` | Get result buckets | See below |

### Get Anomalies

```sql
DECLARE anomalies DOCUMENT;
SET anomalies = ES_ML_GET_RECORDS('response-time-job', 'now-1d', 'now');

FOR record IN anomalies['records'] LOOP
    IF record['record_score'] > 75 THEN
        PRINT 'High anomaly: ' || record['function'] || ' = ' || record['actual'];
        PRINT '  Score: ' || record['record_score'];
        PRINT '  Typical: ' || record['typical'];
    END IF;
END LOOP;
```

## Datafeeds

| Function | Description | Example |
|----------|-------------|---------|
| `ES_ML_GET_DATAFEEDS(id?)` | List datafeeds | `ES_ML_GET_DATAFEEDS('_all')` |
| `ES_ML_START_DATAFEED(id)` | Start datafeed | `ES_ML_START_DATAFEED('feed-1')` |
| `ES_ML_STOP_DATAFEED(id)` | Stop datafeed | `ES_ML_STOP_DATAFEED('feed-1')` |

## Trained Models (NLP)

| Function | Description | Example |
|----------|-------------|---------|
| `ES_ML_GET_TRAINED_MODELS(model_id?)` | List models | `ES_ML_GET_TRAINED_MODELS('_all')` |
| `ES_ML_GET_TRAINED_MODEL_STATS(model_id?)` | Get model stats | See below |
| `ES_ML_INFER(model_id, docs)` | Run inference | See below |

### Text Classification

```sql
DECLARE result DOCUMENT;
SET result = ES_ML_INFER('sentiment-model', [
    {'text_field': 'The product is amazing, I love it!'},
    {'text_field': 'Terrible experience, would not recommend.'}
]);

FOR prediction IN result['inference_results'] LOOP
    PRINT 'Predicted: ' || prediction['predicted_value'];
    PRINT 'Confidence: ' || prediction['prediction_probability'];
END LOOP;
```

### Named Entity Recognition

```sql
DECLARE result DOCUMENT;
SET result = ES_ML_INFER('ner-model', [
    {'text_field': 'John Smith works at Microsoft in Seattle.'}
]);

FOR entity IN result['inference_results'][0]['entities'] LOOP
    PRINT entity['class_name'] || ': ' || entity['entity'];
END LOOP;
```

## Elasticsearch Inference API

| Function | Description | Example |
|----------|-------------|---------|
| `INFERENCE(model, input)` | Generic inference | `INFERENCE('my-model', 'text')` |
| `INFERENCE_EMBED(model, text)` | Get embeddings | `INFERENCE_EMBED('e5', 'query text')` |
| `INFERENCE_CHAT(model, messages)` | Chat completion | See below |
| `INFERENCE_RERANK(model, query, docs)` | Rerank documents | See below |

### Generate Embeddings

```sql
DECLARE embedding ARRAY;
SET embedding = INFERENCE_EMBED('my-embedding-model', 'What is Kubernetes?');

-- Use for vector search
DECLARE similar DOCUMENT;
SET similar = ES_KNN_SEARCH('knowledge-base', embedding, 5);
```

### Chat with LLM

```sql
DECLARE response DOCUMENT;
SET response = INFERENCE_CHAT('openai-gpt4', [
    {'role': 'system', 'content': 'You are a helpful assistant.'},
    {'role': 'user', 'content': 'Explain Elasticsearch in simple terms.'}
]);

PRINT response['choices'][0]['message']['content'];
```

### Rerank Search Results

```sql
-- First, get search results
DECLARE results DOCUMENT;
SET results = ES_SEARCH('docs', {'query': {'match': {'content': 'kubernetes pods'}}});

-- Then rerank with a cross-encoder
DECLARE docs ARRAY;
SET docs = ARRAY_MAP(results['hits']['hits'], x -> x['_source']['content']);

DECLARE reranked DOCUMENT;
SET reranked = INFERENCE_RERANK('cross-encoder', 'how do kubernetes pods work?', docs);
```

## OpenAI Integration

| Function | Description |
|----------|-------------|
| `LLM_COMPLETE(prompt)` | Text completion |
| `LLM_CHAT(messages)` | Chat completion |
| `LLM_EMBED(text)` | Get embeddings |
| `LLM_SUMMARIZE(text)` | Summarize text |
| `LLM_CLASSIFY(text, labels)` | Classify text |
| `LLM_EXTRACT(text, schema)` | Extract structured data |

### Summarize Logs

```sql
DECLARE errors ARRAY;
ESQL FROM logs-* 
| WHERE @timestamp > NOW() - 1 HOUR AND level = 'ERROR'
| KEEP message
| LIMIT 20
INTO errors;

DECLARE error_text STRING;
SET error_text = ARRAY_JOIN(ARRAY_MAP(errors, x -> x['message']), '\n');

DECLARE summary STRING;
SET summary = LLM_SUMMARIZE('Summarize these error messages and identify root cause:\n' || error_text);

PRINT summary;
```

## Pre-built Skills (Moltler)

| Skill | Description |
|-------|-------------|
| `RUN SKILL get_anomalies(job_id, threshold)` | Get high-scoring anomalies |
| `RUN SKILL embed_text(model, text)` | Generate embeddings |
| `RUN SKILL classify_text(text, categories)` | Classify text |
| `RUN SKILL summarize_errors(index, minutes)` | AI summarize errors |
| `RUN SKILL semantic_search(query, index)` | Semantic search |

## Best Practices

1. **Use appropriate models** - Choose models sized for your use case
2. **Batch inference** - Send multiple docs in one call when possible
3. **Cache embeddings** - Store embeddings in ES for reuse
4. **Monitor ML job health** - Check datafeed lag and job state
5. **Set anomaly thresholds** - Focus on high-scoring anomalies
