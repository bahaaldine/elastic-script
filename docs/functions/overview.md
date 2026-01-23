# Functions Overview

elastic-script includes **118 built-in functions** across 13 categories.

## Function Categories

| Category | Count | Description |
|----------|-------|-------------|
| [String](string.md) | 18 | Text manipulation |
| [Number](number.md) | 11 | Mathematical operations |
| [Array](array.md) | 18 | List operations |
| [Date](date.md) | 8 | Date/time handling |
| [Document](document.md) | 6 | JSON object manipulation |
| [MAP](map.md) | 12 | Key-value associative arrays |
| [Elasticsearch](elasticsearch.md) | 5 | ES operations |
| [AI & LLM](ai-llm.md) | 14 | AI/ML functions |
| [Integrations](integrations.md) | 26 | External services |

## Quick Reference

### String Functions

```sql
LENGTH(str)              -- String length
SUBSTR(str, start, len)  -- Substring
UPPER(str)               -- Uppercase
LOWER(str)               -- Lowercase
TRIM(str)                -- Remove whitespace
REPLACE(str, old, new)   -- Replace text
CONCAT(str1, str2, ...)  -- Concatenate
SPLIT(str, delimiter)    -- Split to array
```

### Array Functions

```sql
ARRAY_LENGTH(arr)        -- Array size
ARRAY_APPEND(arr, val)   -- Add element
ARRAY_CONTAINS(arr, val) -- Check membership
ARRAY_FILTER(arr, fn)    -- Filter elements
ARRAY_MAP(arr, fn)       -- Transform elements
ARRAY_JOIN(arr, sep)     -- Join to string
```

### MAP Functions

```sql
MAP_GET(map, key)           -- Get value by key
MAP_PUT(map, key, value)    -- Add/update entry
MAP_KEYS(map)               -- Get all keys
MAP_VALUES(map)             -- Get all values
MAP_SIZE(map)               -- Count entries
MAP_CONTAINS_KEY(map, key)  -- Check key exists
MAP_MERGE(map1, map2)       -- Combine maps
```

### Elasticsearch Functions

```sql
ESQL_QUERY(query)        -- Execute ES|QL
INDEX_DOCUMENT(idx, doc) -- Index document
GET_DOCUMENT(idx, id)    -- Get by ID
REFRESH_INDEX(idx)       -- Refresh index
```

### AI/LLM Functions

```sql
LLM_COMPLETE(prompt)     -- Generate text
LLM_SUMMARIZE(text)      -- Summarize text
LLM_CLASSIFY(text, labels) -- Classify text
INFERENCE(endpoint, input) -- ES Inference API
```

### Integration Functions

```sql
SLACK_SEND(channel, msg) -- Send to Slack
PAGERDUTY_TRIGGER(...)   -- Create incident
K8S_SCALE(ns, deploy, n) -- Scale deployment
AWS_LAMBDA_INVOKE(...)   -- Invoke Lambda
```

## Calling Functions

Functions are called like in SQL:

```sql
DECLARE len NUMBER = LENGTH('Hello');
DECLARE upper STRING = UPPER(name);
DECLARE logs ARRAY = ESQL_QUERY('FROM logs | LIMIT 10');
```

## Chaining Functions

```sql
DECLARE result = UPPER(TRIM(SUBSTR(text, 1, 10)));
```

## Function Discovery

List all available functions:

```sql
DECLARE functions ARRAY = ESCRIPT_FUNCTIONS();

FOR func IN functions LOOP
    PRINT func.name || ': ' || func.description;
END LOOP;
```

Get details for a specific function:

```sql
DECLARE info DOCUMENT = ESCRIPT_FUNCTION('ARRAY_MAP');
PRINT info.signature;
PRINT info.description;
PRINT info.examples;
```

## Next Steps

Explore each category:

- [String Functions](string.md) - Text manipulation
- [Array Functions](array.md) - List operations
- [Elasticsearch Functions](elasticsearch.md) - ES integration
- [AI & LLM Functions](ai-llm.md) - AI capabilities
