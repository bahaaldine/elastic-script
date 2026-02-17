---
layout: default
title: Language Reference
---

# elastic-script Language Reference

elastic-script is a procedural scripting language designed for Elasticsearch operations.

## Statements

### CREATE PROCEDURE

```sql
CREATE PROCEDURE procedure_name(
  param1 TYPE,
  param2 TYPE DEFAULT value,
  IN input_param TYPE,
  OUT output_param TYPE,
  INOUT both_param TYPE
)
BEGIN
  -- statements
END PROCEDURE;
```

### CREATE FUNCTION

```sql
CREATE FUNCTION function_name(param1 TYPE, param2 TYPE)
RETURNS TYPE
BEGIN
  -- statements
  RETURN value;
END FUNCTION;
```

### CREATE SKILL

```sql
CREATE SKILL skill_name
  VERSION 'semver'
  DESCRIPTION 'description for AI agents'
  [AUTHOR 'author name']
  [TAGS ['tag1', 'tag2']]
  [REQUIRES ['dependency1']]
  (param1 TYPE [DEFAULT value], ...)
  [RETURNS TYPE]
BEGIN
  -- skill body
END SKILL;
```

### CALL

```sql
CALL procedure_name(arg1, arg2);
```

### DECLARE / VAR / CONST

```sql
DECLARE variable_name TYPE;
DECLARE variable_name TYPE := initial_value;
VAR variable_name := value;
CONST constant_name := value;
```

### SET

```sql
SET variable_name = expression;
SET variable_name := expression;
```

### IF/THEN/ELSE

```sql
IF condition THEN
  -- statements
ELSEIF other_condition THEN
  -- statements
ELSE
  -- statements
END IF;
```

### FOR Loop

```sql
FOR i IN 1..10 LOOP
  -- statements
END LOOP;
```

### WHILE Loop

```sql
WHILE condition LOOP
  -- statements
END LOOP;
```

### TRY/CATCH

```sql
TRY
  -- statements that might fail
CATCH exception_variable
  -- handle error
FINALLY
  -- cleanup
END TRY;
```

### PRINT

```sql
PRINT 'Message';
PRINT variable;
PRINT 'Value is: ' || variable;
```

### RETURN

```sql
RETURN value;
RETURN {"key": "value"};
```

## Data Types

| Type | Description | Example |
|------|-------------|---------|
| `STRING` | Text | `'hello'` |
| `NUMBER` | Numeric | `42`, `3.14` |
| `BOOLEAN` | True/false | `TRUE`, `FALSE` |
| `ARRAY` | List | `[1, 2, 3]` |
| `DOCUMENT` | Object/Map | `{"key": "value"}` |

## Operators

### Arithmetic
- `+` Addition
- `-` Subtraction
- `*` Multiplication
- `/` Division
- `%` Modulo

### Comparison
- `=` Equal
- `!=`, `<>` Not equal
- `<`, `<=`, `>`, `>=` Comparisons

### Logical
- `AND`
- `OR`
- `NOT`

### String
- `||` Concatenation

### Special
- `??` Null coalescing
- `?.` Safe navigation
- `..` Range (e.g., `1..10`)

## Expressions

### Array Literals

```sql
['a', 'b', 'c']
[1, 2, 3]
```

### Document Literals

```sql
{"name": "John", "age": 30}
```

### Function Calls

```sql
UPPER('hello')
ARRAY_LENGTH([1, 2, 3])
```

## ES|QL Integration

### ESQL_QUERY

Execute ES|QL queries and get results:

```sql
DECLARE results ARRAY;
SET results = ESQL_QUERY('FROM logs-* | WHERE level = "ERROR" | LIMIT 10');
```

### Cursors (Streaming)

```sql
DECLARE log_cursor CURSOR FOR 'FROM logs-* | LIMIT 1000';

FOR record IN log_cursor LOOP
  PRINT record.message;
END LOOP;
```

## Async Execution

### TRACK / ON_DONE / ON_FAIL

```sql
CALL long_running_procedure()
  TRACK 'job-1'
  TIMEOUT 300
  ON_DONE
    PRINT 'Completed: ' || @result;
  ON_FAIL
    PRINT 'Failed: ' || @error;
  FINALLY
    PRINT 'Cleanup';
;
```

### PARALLEL

```sql
PARALLEL [
  CALL procedure1(),
  CALL procedure2(),
  CALL procedure3()
]
ON_ALL_DONE
  PRINT 'All completed: ' || @results;
;
```

## Comments

```sql
-- Single line comment

/* 
 * Multi-line
 * comment
 */
```

## Examples

### Simple Procedure

```sql
CREATE PROCEDURE greet(name STRING DEFAULT 'World')
BEGIN
  PRINT 'Hello, ' || name || '!';
END PROCEDURE;
```

### Error Analysis Skill

```sql
CREATE SKILL analyze_errors
  VERSION '1.0'
  DESCRIPTION 'Analyze error logs and return summary'
  (
    index_pattern STRING DEFAULT 'logs-*',
    hours NUMBER DEFAULT 24
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE errors ARRAY;
  SET errors = ESQL_QUERY(
    'FROM ' || index_pattern ||
    ' | WHERE level = "ERROR"' ||
    ' | WHERE @timestamp > NOW() - ' || hours || 'h' ||
    ' | STATS count = COUNT(*) BY error_type' ||
    ' | SORT count DESC'
  );
  
  RETURN {
    "total_errors": ARRAY_LENGTH(errors),
    "by_type": errors,
    "analyzed_at": CURRENT_TIMESTAMP()
  };
END SKILL;
```

### Conditional Logic

```sql
CREATE PROCEDURE check_health()
BEGIN
  DECLARE cpu_usage NUMBER;
  SET cpu_usage = ESQL_QUERY('FROM metrics-* | STATS avg = AVG(cpu) | LIMIT 1')[0].avg;
  
  IF cpu_usage > 90 THEN
    PRINT 'CRITICAL: CPU at ' || cpu_usage || '%';
  ELSEIF cpu_usage > 70 THEN
    PRINT 'WARNING: CPU at ' || cpu_usage || '%';
  ELSE
    PRINT 'OK: CPU at ' || cpu_usage || '%';
  END IF;
END PROCEDURE;
```
