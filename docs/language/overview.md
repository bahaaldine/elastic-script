# Language Overview

elastic-script is a **procedural scripting language** designed to run inside Elasticsearch. It combines familiar SQL-like syntax with modern programming constructs.

## Key Concepts

### Procedures

Everything in elastic-script is organized into **procedures**:

```sql
CREATE PROCEDURE my_procedure(param1 STRING, param2 NUMBER)
BEGIN
    -- Your code here
    RETURN result;
END PROCEDURE;
```

Procedures are:
- Stored in the `.elastic_script_procedures` index
- Called with `CALL procedure_name(args)`
- Persistent across Elasticsearch restarts

### Statements

Procedures contain statements:

| Statement | Purpose |
|-----------|---------|
| `DECLARE` | Create variables |
| `SET` | Assign values |
| `PRINT` | Output to logs |
| `IF/THEN/ELSE` | Conditionals |
| `FOR/WHILE` | Loops |
| `TRY/CATCH` | Error handling |
| `RETURN` | Return value |
| `THROW` | Raise errors |
| `EXECUTE IMMEDIATE` | Dynamic ES\|QL |

### Expressions

Expressions evaluate to values:

```sql
-- Arithmetic
SET x = 10 + 5 * 2;

-- String concatenation
SET message = 'Hello, ' || name || '!';

-- Comparison
IF x > 10 THEN ...

-- Function calls
SET len = LENGTH(message);
```

## Data Types

| Type | Description | Example |
|------|-------------|---------|
| `STRING` | Text | `'Hello'` or `"World"` |
| `NUMBER` | Numeric | `42`, `3.14`, `-5` |
| `BOOLEAN` | True/False | `TRUE`, `FALSE` |
| `DATE` | Date/timestamp | `CURRENT_DATE()` |
| `ARRAY` | List | `[1, 2, 3]`, `['a', 'b']` |
| `DOCUMENT` | JSON object | `{"key": "value"}` |
| `MAP` | Associative array | `MAP { 'key' => value }` |
| `CURSOR` | Query result iterator | `CURSOR FOR FROM ...` |

## Operators

### Arithmetic
```sql
+   -- Addition
-   -- Subtraction
*   -- Multiplication
/   -- Division
%   -- Modulo
```

### Comparison
```sql
==  -- Equal
<>  -- Not equal
!=  -- Not equal (alias)
<   -- Less than
>   -- Greater than
<=  -- Less than or equal
>=  -- Greater than or equal
```

### Logical
```sql
AND -- Logical AND
OR  -- Logical OR
NOT -- Logical NOT
```

### String
```sql
||  -- Concatenation
```

### Special
```sql
??  -- Null coalescing
?.  -- Safe navigation
..  -- Range (for loops)
```

## Comments

```sql
-- Single line comment

/* Multi-line
   comment */
```

## API Endpoints

elastic-script exposes REST endpoints:

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/_escript` | POST | Execute queries |
| `/_escript/procedures/{id}` | GET | Get procedure |
| `/_escript/procedures/{id}` | DELETE | Delete procedure |
| `/_escript/procedures/{id}/_call` | POST | Call with params |

## Query Structure

All queries go through `POST /_escript`:

```bash
curl -u user:pass http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "YOUR QUERY HERE"}'
```

Valid top-level queries:
- `CREATE PROCEDURE ...`
- `DELETE PROCEDURE ...`
- `CALL procedure_name(...)`
- `DEFINE INTENT ...`

## Next Steps

- [Variables & Types](variables-types.md)
- [Control Flow](control-flow.md)
- [Procedures](procedures.md)
- [User-Defined Functions](functions.md)
- [Error Handling](error-handling.md)
- [EXECUTE IMMEDIATE](execute-immediate.md)
- [Async Execution](async-execution.md)
