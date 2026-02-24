# AI Skill Generation Guide

This guide is for AI agents (Claude, ChatGPT, Cursor, etc.) to help them generate valid Moltler skills from natural language descriptions.

## Skill Code Template

```sql
CREATE SKILL skill_name
  VERSION '1.0.0'
  DESCRIPTION 'What this skill does'
  AUTHOR 'author_name'
  TAGS ['tag1', 'tag2']
  (
    param_name STRING DESCRIPTION 'Parameter description' DEFAULT 'default_value'
  )
  RETURNS ARRAY  -- or DOCUMENT, STRING, INTEGER, BOOLEAN
BEGIN
  DECLARE result ARRAY;
  
  -- Implementation here
  
  RETURN result;
END SKILL;
```

## Available Data Types

- `STRING` - Text values
- `INTEGER` - Whole numbers
- `BOOLEAN` - TRUE or FALSE
- `ARRAY` - List of values
- `DOCUMENT` - Key-value object (like JSON)

## Core Functions

### Elasticsearch Queries
```sql
-- Execute ES|QL query (most common)
SET result = ESQL_QUERY('FROM logs-* | WHERE log.level == "ERROR" | LIMIT 100');

-- Common ES|QL patterns
FROM index-* | WHERE condition | SORT field DESC | LIMIT n
FROM index-* | STATS count=COUNT(*), avg=AVG(field) BY group_field
FROM index-* | WHERE field LIKE "*pattern*" | KEEP field1, field2
```

### Array Operations
```sql
ARRAY_LENGTH(arr)              -- Get array size
ARRAY_APPEND(arr, item)        -- Add item to array
ARRAY_FILTER(arr, condition)   -- Filter array
ARRAY_MAP(arr, transform)      -- Transform each item
ARRAY_FIND(arr, condition)     -- Find first match
ARRAY_SORT(arr)                -- Sort array
```

### Document Operations
```sql
DOCUMENT_GET(doc, 'key')       -- Get value by key
DOCUMENT_KEYS(doc)             -- Get all keys
DOCUMENT_MERGE(doc1, doc2)     -- Merge documents
DOCUMENT_CONTAINS(doc, 'key')  -- Check key exists
```

### String Operations
```sql
LENGTH(str)                    -- String length
UPPER(str), LOWER(str)         -- Case conversion
SUBSTR(str, start, len)        -- Substring
REPLACE(str, old, new)         -- Replace text
SPLIT(str, delimiter)          -- Split to array
```

### Date/Time Operations
```sql
CURRENT_TIMESTAMP()            -- Current time
DATE_ADD(date, 'interval')     -- Add time
DATE_DIFF('unit', d1, d2)      -- Difference
```

### External Calls
```sql
HTTP_GET(url)                  -- GET request
HTTP_POST(url, body)           -- POST request
INFERENCE(endpoint, type, input) -- Call AI model
```

### Call Other Skills
```sql
SET data = RUN SKILL other_skill(param1, param2);
```

## Control Flow

```sql
-- Conditionals
IF condition THEN
  -- statements
ELSEIF other_condition THEN
  -- statements
ELSE
  -- statements
END IF;

-- Loops
FOR i IN 1..10 LOOP
  -- statements
END LOOP;

FOR item IN array_var LOOP
  -- statements
END LOOP;

WHILE condition LOOP
  -- statements
END LOOP;
```

## Common Skill Patterns

### Pattern 1: Query and Filter
```sql
CREATE SKILL find_errors
  VERSION '1.0.0'
  DESCRIPTION 'Find error logs in a time range'
  AUTHOR 'sre'
  TAGS ['observability', 'logs']
  (
    time_range STRING DESCRIPTION 'Time range' DEFAULT '1h',
    index STRING DESCRIPTION 'Index pattern' DEFAULT 'logs-*'
  )
  RETURNS ARRAY
BEGIN
  DECLARE errors ARRAY;
  SET errors = ESQL_QUERY(
    'FROM ' || index || ' | WHERE log.level == "ERROR" | SORT @timestamp DESC | LIMIT 100'
  );
  RETURN errors;
END SKILL;
```

### Pattern 2: Aggregate and Summarize
```sql
CREATE SKILL top_services_by_errors
  VERSION '1.0.0'
  DESCRIPTION 'Get services with most errors'
  AUTHOR 'sre'
  TAGS ['observability', 'aggregation']
  (
    limit INTEGER DESCRIPTION 'Number of services' DEFAULT 10
  )
  RETURNS ARRAY
BEGIN
  DECLARE results ARRAY;
  SET results = ESQL_QUERY(
    'FROM logs-* | WHERE log.level == "ERROR" | STATS error_count = COUNT(*) BY service.name | SORT error_count DESC | LIMIT ' || limit
  );
  RETURN results;
END SKILL;
```

### Pattern 3: Multi-Step Analysis
```sql
CREATE SKILL incident_report
  VERSION '1.0.0'
  DESCRIPTION 'Generate incident report'
  AUTHOR 'sre'
  TAGS ['incident', 'report']
  (
    service STRING DESCRIPTION 'Service name'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE errors ARRAY;
  DECLARE metrics ARRAY;
  DECLARE report DOCUMENT;
  
  SET errors = ESQL_QUERY(
    'FROM logs-* | WHERE service.name == "' || service || '" AND log.level == "ERROR" | LIMIT 50'
  );
  
  SET metrics = ESQL_QUERY(
    'FROM metrics-* | WHERE service.name == "' || service || '" | STATS avg_cpu = AVG(system.cpu.percent)'
  );
  
  SET report = {
    'service': service,
    'error_count': ARRAY_LENGTH(errors),
    'recent_errors': errors,
    'avg_cpu': DOCUMENT_GET(ARRAY_FIND(metrics, TRUE), 'avg_cpu'),
    'generated_at': CURRENT_TIMESTAMP()
  };
  
  RETURN report;
END SKILL;
```

### Pattern 4: External Integration
```sql
CREATE SKILL notify_slack
  VERSION '1.0.0'
  DESCRIPTION 'Send Slack notification'
  AUTHOR 'platform'
  TAGS ['notification', 'slack']
  (
    channel STRING DESCRIPTION 'Slack channel',
    message STRING DESCRIPTION 'Message to send'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  SET result = HTTP_POST(
    'https://slack.com/api/chat.postMessage',
    {'channel': channel, 'text': message}
  );
  RETURN result;
END SKILL;
```

## Generation Rules

When generating skills, AI agents MUST:

1. **Always include all required clauses**: VERSION, DESCRIPTION, AUTHOR, TAGS, RETURNS
2. **Use meaningful parameter names** with DESCRIPTION and DEFAULT values
3. **Declare all variables** with DECLARE before use
4. **End with END SKILL;**
5. **Use single quotes** for strings in SQL, escape with double single quotes
6. **Handle edge cases** - check for empty results, null values
7. **Return consistent types** - match RETURNS clause

## Validation

Before installing, validate syntax with:
```
POST /_escript/validate
{"query": "<skill_code>"}
```

## Installation

To install a generated skill:
```
POST /_escript
{"query": "<skill_code>"}
```

To execute:
```
POST /_escript
{"query": "RUN SKILL skill_name(param => 'value')"}
```

## MCP Integration

AI agents with MCP access can use these tools:

1. `generate_skill` - Generate skill from description using ES Inference
2. `validate_skill_syntax` - Check if skill code is valid
3. `install_skill` - Install a skill into Elasticsearch

Example workflow:
1. User: "Create a skill to find slow API requests"
2. Agent: Uses `generate_skill` or generates code directly
3. Agent: Uses `validate_skill_syntax` to verify
4. Agent: Uses `install_skill` to deploy
5. Agent: Tests with `RUN SKILL slow_api_requests()`
