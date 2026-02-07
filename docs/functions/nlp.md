# Nlp Functions

## Quick Reference

| Function | Description |
|----------|-------------|
| [`INFERENCE`](#inference) | Performs inference using a configured Elasticsearch inferenc... |
| [`INFERENCE_CHAT`](#inference-chat) | Performs chat completion using the Unified Completion API (O... |
| [`INFERENCE_EMBED`](#inference-embed) | Generates embeddings for the input text using a configured e... |
| [`INFERENCE_RERANK`](#inference-rerank) | Reranks a list of documents based on relevance to a query us... |
| [`LLM_CHAT`](#llm-chat) | Sends a chat conversation to OpenAI and returns the assistan... |
| [`LLM_CLASSIFY`](#llm-classify) | Classifies the given text into one of the provided categorie... |
| [`LLM_COMPLETE`](#llm-complete) | Generates a text completion using OpenAI's API. Returns the ... |
| [`LLM_EMBED`](#llm-embed) | Generates an embedding vector for the given text using OpenA... |
| [`LLM_EXTRACT`](#llm-extract) | Extracts structured information from text based on a schema.... |
| [`LLM_SUMMARIZE`](#llm-summarize) | Summarizes the given text using an LLM. Returns a concise su... |

---

## Function Details

### INFERENCE

```
INFERENCE(endpoint_id STRING, input STRING, timeout STRING) -> STRING
```

Performs inference using a configured Elasticsearch inference endpoint. 

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `endpoint_id` | STRING | The inference endpoint ID |
| `input` | STRING | The input text for inference |
| `timeout` | STRING | Timeout (e.g., '30s'). Optional, defaults to 30s |

**Returns:** `STRING`
 - The inference result as text


**Examples:**

```sql
INFERENCE('my-openai-endpoint', 'Explain Elasticsearch')
INFERENCE('elser-endpoint', 'search query', '60s')
```

---

### INFERENCE_CHAT

```
INFERENCE_CHAT(endpoint_id STRING) -> STRING
```

Performs chat completion using the Unified Completion API (OpenAI-compatible). 

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `endpoint_id` | STRING | The chat completion inference endpoint ID |

**Returns:** `STRING`
 - The assistant's response text


**Examples:**

```sql
INFERENCE_CHAT('my-openai-endpoint', '[{\
:\
,\
:\
```

---

### INFERENCE_EMBED

```
INFERENCE_EMBED(endpoint_id STRING, input STRING, timeout STRING) -> ARRAY
```

Generates embeddings for the input text using a configured embedding endpoint. 

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `endpoint_id` | STRING | The embedding inference endpoint ID |
| `input` | STRING | The text to embed |
| `timeout` | STRING | Timeout (e.g., '30s'). Optional |

**Returns:** `ARRAY`
 - The embedding vector as an array of numbers


**Examples:**

```sql
INFERENCE_EMBED('my-embedding-endpoint', 'text to embed')
INFERENCE_EMBED('elser-endpoint', 'search query')
```

---

### INFERENCE_RERANK

```
INFERENCE_RERANK(endpoint_id STRING, query STRING, documents ARRAY, top_n NUMBER, timeout STRING) -> ARRAY
```

Reranks a list of documents based on relevance to a query using a reranking endpoint.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `endpoint_id` | STRING | The reranking inference endpoint ID |
| `query` | STRING | The query to rank against |
| `documents` | ARRAY | Array of document strings to rerank |
| `top_n` | NUMBER | Number of top results to return. Optional |
| `timeout` | STRING | Timeout (e.g., '30s'). Optional |

**Returns:** `ARRAY`
 - Reranked documents with scores


**Examples:**

```sql
INFERENCE_RERANK('my-reranker', 'elasticsearch query', doc_array)
INFERENCE_RERANK('cohere-rerank', 'best database', docs, 5)
```

---

### LLM_CHAT

```
LLM_CHAT(messages ARRAY OF DOCUMENT, model STRING, api_key STRING) -> STRING
```

Sends a chat conversation to OpenAI and returns the assistant's response. 

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `messages` | ARRAY OF DOCUMENT | Array of message objects with 'role' and 'content' |
| `model` | STRING | The model to use (default: gpt-4o-mini) |
| `api_key` | STRING | OpenAI API key (optional) |

**Returns:** `STRING`
 - The assistant's response


---

### LLM_CLASSIFY

```
LLM_CLASSIFY(text STRING, categories ARRAY OF STRING, api_key STRING) -> STRING
```

Classifies the given text into one of the provided categories using an LLM.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `text` | STRING | The text to classify |
| `categories` | ARRAY OF STRING | List of possible categories |
| `api_key` | STRING | OpenAI API key (optional) |

**Returns:** `STRING`
 - The selected category


**Examples:**

```sql
LLM_CLASSIFY('Server is down!', ['critical', 'warning', 'info'])
LLM_CLASSIFY(email_body, ['spam', 'not_spam'])
```

---

### LLM_COMPLETE

```
LLM_COMPLETE(prompt STRING, model STRING, api_key STRING) -> STRING
```

Generates a text completion using OpenAI's API. Returns the generated text.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `prompt` | STRING | The prompt to complete |
| `model` | STRING | The model to use (default: gpt-4o-mini) |
| `api_key` | STRING | OpenAI API key (optional, uses env var if not provided) |

**Returns:** `STRING`
 - The generated completion text


**Examples:**

```sql
LLM_COMPLETE('Explain Elasticsearch in one sentence')
LLM_COMPLETE('Translate to French: Hello', 'gpt-4o')
```

---

### LLM_EMBED

```
LLM_EMBED(text STRING, model STRING, api_key STRING) -> ARRAY OF FLOAT
```

Generates an embedding vector for the given text using OpenAI's embedding API. 

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `text` | STRING | The text to embed |
| `model` | STRING | The embedding model (default: text-embedding-3-small) |
| `api_key` | STRING | OpenAI API key (optional) |

**Returns:** `ARRAY OF FLOAT`
 - The embedding vector


**Examples:**

```sql
LLM_EMBED('Elasticsearch is a search engine')
LLM_EMBED('Hello world', 'text-embedding-3-large')
```

---

### LLM_EXTRACT

```
LLM_EXTRACT(text STRING, fields ARRAY OF STRING, api_key STRING) -> DOCUMENT
```

Extracts structured information from text based on a schema. 

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `text` | STRING | The text to extract from |
| `fields` | ARRAY OF STRING | List of field names to extract |
| `api_key` | STRING | OpenAI API key (optional) |

**Returns:** `DOCUMENT`
 - Document with extracted fields


**Examples:**

```sql
LLM_EXTRACT('John Doe, age 30, from NYC', ['name', 'age', 'city'])
LLM_EXTRACT(error_log, ['error_type', 'timestamp', 'severity'])
```

---

### LLM_SUMMARIZE

```
LLM_SUMMARIZE(text STRING, max_words NUMBER, api_key STRING) -> STRING
```

Summarizes the given text using an LLM. Returns a concise summary.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `text` | STRING | The text to summarize |
| `max_words` | NUMBER | Maximum words in summary (default: 100) |
| `api_key` | STRING | OpenAI API key (optional) |

**Returns:** `STRING`
 - The summarized text


**Examples:**

```sql
LLM_SUMMARIZE(long_document)
LLM_SUMMARIZE(log_output, 50)
```

---
