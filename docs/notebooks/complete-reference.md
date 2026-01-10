# Complete Reference

!!! tip "Interactive Notebook"
    This page is generated from a Jupyter notebook. For the best experience, 
    run it interactively in Jupyter:
    
    ```bash
    ./scripts/quick-start.sh
    # Then open http://localhost:8888/notebooks/00-complete-reference.ipynb
    ```
    
    [:fontawesome-brands-github: View on GitHub](https://github.com/bahaaldine/elastic-script/blob/main/notebooks/00-complete-reference.ipynb){ .md-button }

---


This notebook provides a comprehensive reference for **all** elastic-script features.

## Table of Contents
1. [Language Basics](#1-language-basics)
2. [String Functions (18)](#2-string-functions)
3. [Number Functions (11)](#3-number-functions)
4. [Array Functions (18)](#4-array-functions)
5. [Date Functions (8)](#5-date-functions)
6. [Document Functions (6)](#6-document-functions)
7. [Elasticsearch Functions (5)](#7-elasticsearch-functions)
8. [OpenAI/LLM Functions (6)](#8-openai-functions)
9. [Introspection Functions (8)](#9-introspection-functions)
10. [Integration Functions](#10-integration-functions)

---
# 1. Language Basics

## 1.1 Variables and Types

```sql
-- Variable declarations with all supported types
CREATE PROCEDURE demo_types()
BEGIN
    -- String types
    DECLARE name STRING = 'Alice';
    DECLARE greeting STRING = "Hello, World!";
    
    -- Numeric types
    DECLARE count INT = 42;
    DECLARE price FLOAT = 19.99;
    DECLARE total NUMBER = 1000.50;
    
    -- Boolean (case-insensitive)
    DECLARE is_active BOOLEAN = TRUE;
    DECLARE is_deleted BOOLEAN = false;
    DECLARE is_pending BOOLEAN = True;
    
    -- Arrays (single or double quotes)
    DECLARE tags ARRAY = ['prod', 'urgent', "critical"];
    DECLARE numbers ARRAY = [1, 2, 3, 4, 5];
    
    -- Documents (JSON objects)
    DECLARE config DOCUMENT = {"host": "localhost", "port": 9200};
    
    -- Output
    PRINT 'Name: ' || name;
    PRINT 'Count: ' || count;
    PRINT 'Is Active: ' || is_active;
    PRINT 'Tags: ' || tags;
    PRINT 'Config: ' || config;
END PROCEDURE;
```

```sql
CALL demo_types()
```

## 1.2 Control Flow - IF/THEN/ELSE

```sql
CREATE PROCEDURE demo_if(severity STRING)
BEGIN
    DECLARE action STRING;
    
    IF severity == 'critical' THEN
        SET action = 'PAGE_ONCALL';
    ELSEIF severity == 'high' THEN
        SET action = 'SEND_ALERT';
    ELSEIF severity == 'medium' THEN
        SET action = 'LOG_WARNING';
    ELSE
        SET action = 'LOG_INFO';
    END IF;
    
    PRINT 'Severity: ' || severity || ' -> Action: ' || action;
END PROCEDURE;
```

```sql
CALL demo_if('critical')
```

```sql
CALL demo_if('low')
```

## 1.3 FOR Loops

```sql
CREATE PROCEDURE demo_for_loop()
BEGIN
    -- Range-based loop
    PRINT '--- Counting 1 to 5 ---';
    FOR i IN 1..5 LOOP
        PRINT 'Count: ' || i;
    END LOOP;
    
    -- Array iteration
    PRINT '--- Iterating array ---';
    DECLARE fruits ARRAY = ['apple', 'banana', 'cherry'];
    FOR fruit IN fruits LOOP
        PRINT 'Fruit: ' || fruit;
    END LOOP;
END PROCEDURE;
```

```sql
CALL demo_for_loop()
```



```sql
CREATE PROCEDURE demo_string_functions()
BEGIN
    DECLARE text STRING = '  Hello, World!  ';
    
    -- Basic string operations
    PRINT 'Original: [' || text || ']';
    PRINT 'LENGTH: ' || LENGTH(text);
    PRINT 'TRIM: [' || TRIM(text) || ']';
    PRINT 'LTRIM: [' || LTRIM(text) || ']';
    PRINT 'RTRIM: [' || RTRIM(text) || ']';
    PRINT 'UPPER: ' || UPPER(TRIM(text));
    PRINT 'LOWER: ' || LOWER(TRIM(text));
    PRINT 'INITCAP: ' || INITCAP('hello world');
    PRINT 'REVERSE: ' || REVERSE('hello');
    
    -- Substring operations
    PRINT 'SUBSTR(text, 3, 5): ' || SUBSTR(TRIM(text), 3, 5);
    PRINT 'INSTR(text, o): ' || INSTR(text, 'o');
    
    -- Padding
    PRINT 'LPAD(42, 5, 0): ' || LPAD('42', 5, '0');
    PRINT 'RPAD(42, 5, 0): ' || RPAD('42', 5, '0');
    
    -- Replace and split
    PRINT 'REPLACE: ' || REPLACE('hello', 'l', 'L');
    PRINT 'SPLIT: ' || SPLIT('a,b,c', ',');
    PRINT 'CONCAT: ' || 'Hello' || ' ' || 'World';
    
    -- Regex operations
    PRINT 'REGEXP_REPLACE: ' || REGEXP_REPLACE('abc123def', '[0-9]+', 'NUM');
END PROCEDURE;
```

```sql
CALL demo_string_functions()
```

---
# 3. Number Functions (11 functions)

Mathematical operations.

```sql
CREATE PROCEDURE demo_number_functions()
BEGIN
    DECLARE num NUMBER = -15.7;
    
    PRINT 'Number: ' || num;
    PRINT 'ABS: ' || ABS(num);
    PRINT 'CEIL: ' || CEIL(num);
    PRINT 'FLOOR: ' || FLOOR(num);
    PRINT 'ROUND: ' || ROUND(15.678, 2);
    PRINT 'TRUNC: ' || TRUNC(15.678, 1);
    PRINT 'MOD(17, 5): ' || MOD(17, 5);
    PRINT 'POWER(2, 10): ' || POWER(2, 10);
    PRINT 'SQRT(144): ' || SQRT(144);
    PRINT 'EXP(1): ' || EXP(1);
    PRINT 'LOG(100, 10): ' || LOG(100, 10);
    PRINT 'SIGN(-42): ' || SIGN(-42);
END PROCEDURE;
```

```sql
CALL demo_number_functions()
```

---
# 4. Array Functions (18 functions)

Array manipulation including functional programming patterns.

```sql
CREATE PROCEDURE demo_array_basic()
BEGIN
    DECLARE arr ARRAY = [1, 2, 3, 4, 5];
    
    PRINT 'Array: ' || arr;
    PRINT 'ARRAY_LENGTH: ' || ARRAY_LENGTH(arr);
    PRINT 'ARRAY_APPEND(arr, 6): ' || ARRAY_APPEND(arr, 6);
    PRINT 'ARRAY_PREPEND(arr, 0): ' || ARRAY_PREPEND(arr, 0);
    PRINT 'ARRAY_REMOVE(arr, 3): ' || ARRAY_REMOVE(arr, 3);
    PRINT 'ARRAY_CONTAINS(arr, 3): ' || ARRAY_CONTAINS(arr, 3);
    PRINT 'ARRAY_REVERSE: ' || ARRAY_REVERSE(arr);
    PRINT 'ARRAY_SLICE(arr, 1, 3): ' || ARRAY_SLICE(arr, 1, 3);
    
    -- Distinct
    DECLARE with_dups ARRAY = [1, 2, 2, 3, 3, 3];
    PRINT 'ARRAY_DISTINCT: ' || ARRAY_DISTINCT(with_dups);
    
    -- Join
    DECLARE words ARRAY = ['hello', 'world'];
    PRINT 'ARRAY_JOIN: ' || ARRAY_JOIN(words, ', ');
END PROCEDURE;
```

```sql
CALL demo_array_basic()
```

---
# 5. Date Functions (8 functions)

```sql
CREATE PROCEDURE demo_date_functions()
BEGIN
    DECLARE today DATE = CURRENT_DATE();
    DECLARE now DATE = CURRENT_TIMESTAMP();
    
    PRINT 'CURRENT_DATE: ' || today;
    PRINT 'CURRENT_TIMESTAMP: ' || now;
    PRINT 'DATE_ADD (7 days): ' || DATE_ADD(today, 7);
    PRINT 'DATE_SUB (30 days): ' || DATE_SUB(today, 30);
    PRINT 'EXTRACT_YEAR: ' || EXTRACT_YEAR(now);
    PRINT 'EXTRACT_MONTH: ' || EXTRACT_MONTH(now);
    PRINT 'EXTRACT_DAY: ' || EXTRACT_DAY(now);
END PROCEDURE;
```

```sql
CALL demo_date_functions()
```

---
# 6. Document Functions (6 functions)

Working with JSON documents.

```sql
CREATE PROCEDURE demo_document_functions()
BEGIN
    DECLARE doc DOCUMENT = {"name": "John", "age": 30, "city": "NYC"};
    
    PRINT 'Document: ' || doc;
    PRINT 'DOCUMENT_GET(name): ' || DOCUMENT_GET(doc, 'name');
    PRINT 'DOCUMENT_KEYS: ' || DOCUMENT_KEYS(doc);
    PRINT 'DOCUMENT_VALUES: ' || DOCUMENT_VALUES(doc);
    PRINT 'DOCUMENT_CONTAINS(age): ' || DOCUMENT_CONTAINS(doc, 'age');
    
    DECLARE extra DOCUMENT = {"email": "john@test.com"};
    PRINT 'DOCUMENT_MERGE: ' || DOCUMENT_MERGE(doc, extra);
    PRINT 'DOCUMENT_REMOVE(city): ' || DOCUMENT_REMOVE(doc, 'city');
END PROCEDURE;
```

```sql
CALL demo_document_functions()
```

---
# 7. Elasticsearch Functions

Query and index data directly.

```sql
-- Query with ESQL_QUERY and CURSOR
CREATE PROCEDURE demo_esql_query()
BEGIN
    DECLARE results CURSOR FOR
        FROM logs-sample
        | WHERE level == "ERROR"
        | KEEP @timestamp, level, message, service
        | SORT @timestamp DESC
        | LIMIT 5;
    
    PRINT '=== Recent Error Logs ===';
    FOR row IN results LOOP
        PRINT DOCUMENT_GET(row, 'service') || ': ' || DOCUMENT_GET(row, 'message');
    END LOOP;
END PROCEDURE;
```

```sql
CALL demo_esql_query()
```

```sql
-- Aggregations
CREATE PROCEDURE demo_aggregation()
BEGIN
    DECLARE stats CURSOR FOR
        FROM logs-sample
        | STATS count = COUNT(*) BY level
        | SORT count DESC;
    
    PRINT '=== Log Level Distribution ===';
    FOR stat IN stats LOOP
        PRINT DOCUMENT_GET(stat, 'level') || ': ' || DOCUMENT_GET(stat, 'count');
    END LOOP;
END PROCEDURE;
```

```sql
CALL demo_aggregation()
```

---
# 8. OpenAI/LLM Functions (requires OPENAI_API_KEY)

AI-powered text processing.

```sql
-- LLM_COMPLETE - Text completion
CREATE PROCEDURE demo_llm_complete()
BEGIN
    DECLARE prompt STRING = 'Explain Elasticsearch in one sentence.';
    DECLARE response STRING = LLM_COMPLETE(prompt);
    
    PRINT 'Prompt: ' || prompt;
    PRINT 'Response: ' || response;
END PROCEDURE;
```

```sql
CALL demo_llm_complete()
```

```sql
-- LLM_SUMMARIZE - Summarize errors from logs
CREATE PROCEDURE demo_llm_summarize()
BEGIN
    DECLARE error_logs CURSOR FOR
        FROM logs-sample
        | WHERE level == "ERROR"
        | LIMIT 10;
    
    DECLARE messages STRING = '';
    FOR log IN error_logs LOOP
        SET messages = messages || DOCUMENT_GET(log, 'message') || '\n';
    END LOOP;
    
    DECLARE summary STRING = LLM_SUMMARIZE(messages);
    PRINT '=== Error Summary ===';
    PRINT summary;
END PROCEDURE;
```

```sql
CALL demo_llm_summarize()
```

---
# 9. Introspection Functions

Discover available capabilities.

```sql
-- List all available functions
CREATE PROCEDURE demo_introspection()
BEGIN
    DECLARE functions ARRAY = ESCRIPT_FUNCTIONS();
    DECLARE count NUMBER = ARRAY_LENGTH(functions);
    DECLARE func DOCUMENT;
    
    PRINT 'Total functions available: ' || count;
    
    -- Show first 10
    PRINT '=== Sample Functions ===';
    FOR i IN 0..9 LOOP
        SET func = functions[i];
        PRINT DOCUMENT_GET(func, 'name');
    END LOOP;
END PROCEDURE;
```

```sql
CALL demo_introspection()
```

---
# Summary

**elastic-script** provides **106+ built-in functions** across these categories:

| Category | Count | Examples |
|----------|-------|----------|
| String | 18 | LENGTH, SUBSTR, UPPER, REGEX_REPLACE |
| Number | 11 | ABS, ROUND, POWER, SQRT |
| Array | 18 | ARRAY_MAP, ARRAY_FILTER, ARRAY_REDUCE |
| Date | 8 | DATE_ADD, DATE_DIFF, EXTRACT_YEAR |
| Document | 6 | DOCUMENT_GET, DOCUMENT_MERGE |
| Elasticsearch | 5 | ESQL_QUERY, INDEX_DOCUMENT |
| OpenAI/LLM | 6 | LLM_COMPLETE, LLM_SUMMARIZE, LLM_CLASSIFY |
| Inference API | 8 | INFERENCE, INFERENCE_EMBED |
| Introspection | 8 | ESCRIPT_FUNCTIONS, ESCRIPT_PROCEDURES |
| Integrations | ~30 | Slack, PagerDuty, AWS, K8s, HTTP |

Plus full language support for:
- Variables & Types (STRING, INT, FLOAT, BOOLEAN, ARRAY, DOCUMENT)
- Control Flow (IF/THEN/ELSE, FOR, WHILE, TRY/CATCH)
- Procedures & Functions with IN/OUT/INOUT parameters
- Async Execution with Continuations (ON_DONE, ON_FAIL)

