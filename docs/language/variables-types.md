# Variables & Types

## Declaring Variables

Use `DECLARE` to create variables:

```sql
DECLARE name STRING = 'Alice';
DECLARE count NUMBER = 42;
DECLARE is_active BOOLEAN = TRUE;
```

Or use `VAR` for shorthand:

```sql
VAR name = 'Alice';  -- Type inferred as STRING
```

## Data Types

### STRING

Text values with single or double quotes:

```sql
DECLARE message STRING = 'Hello, World!';
DECLARE alt STRING = "Also valid";
```

**Operations:**
```sql
-- Concatenation
SET full_name = first_name || ' ' || last_name;

-- Length
SET len = LENGTH(message);

-- Substring
SET part = SUBSTR(message, 1, 5);  -- 'Hello'
```

### NUMBER

Integer and floating-point values:

```sql
DECLARE count NUMBER = 42;
DECLARE price NUMBER = 19.99;
DECLARE negative NUMBER = -5;
```

**Operations:**
```sql
SET total = price * quantity;
SET average = total / count;
SET remainder = count % 10;
```

### BOOLEAN

True or false values (case-insensitive):

```sql
DECLARE is_active BOOLEAN = TRUE;
DECLARE is_deleted BOOLEAN = false;
DECLARE flag BOOLEAN = True;
```

**Operations:**
```sql
IF is_active AND NOT is_deleted THEN
    PRINT 'Active record';
END IF;
```

### ARRAY

Ordered list of values:

```sql
DECLARE numbers ARRAY = [1, 2, 3, 4, 5];
DECLARE fruits ARRAY = ['apple', 'banana', 'cherry'];
DECLARE mixed ARRAY = [1, 'two', TRUE];
```

**Access:**
```sql
SET first = numbers[0];    -- 1
SET last = numbers[4];     -- 5
```

**Operations:**
```sql
SET len = ARRAY_LENGTH(fruits);            -- 3
SET extended = ARRAY_APPEND(fruits, 'date');
SET found = ARRAY_CONTAINS(fruits, 'apple');  -- TRUE
```

### DOCUMENT

JSON objects:

```sql
DECLARE user DOCUMENT = {
    "name": "John",
    "age": 30,
    "email": "john@example.com"
};
```

**Access:**
```sql
SET name = user.name;           -- Dot notation
SET age = DOCUMENT_GET(user, 'age');
```

**Operations:**
```sql
SET keys = DOCUMENT_KEYS(user);      -- ['name', 'age', 'email']
SET values = DOCUMENT_VALUES(user);   -- ['John', 30, 'john@...']
SET has_name = DOCUMENT_CONTAINS(user, 'name');  -- TRUE
```

### DATE

Date and timestamp values:

```sql
DECLARE today DATE = CURRENT_DATE();
DECLARE now DATE = CURRENT_TIMESTAMP();
```

**Operations:**
```sql
SET tomorrow = DATE_ADD(today, 1, 'DAY');
SET year = EXTRACT_YEAR(today);
SET month = EXTRACT_MONTH(today);
```

### CURSOR

Iterator for query results:

```sql
DECLARE logs CURSOR FOR
    FROM logs-sample
    | WHERE level == "ERROR"
    | LIMIT 100;

FOR log IN logs LOOP
    PRINT log.message;
END LOOP;
```

## Assigning Values

Use `SET` to assign values:

```sql
DECLARE x NUMBER = 0;
SET x = 10;
SET x = x + 5;
```

## Constants

Use `CONST` for immutable values:

```sql
CONST MAX_RETRIES = 3;
CONST API_URL = 'https://api.example.com';
```

## Type Conversion

Implicit conversion happens in many cases:

```sql
DECLARE num NUMBER = 42;
PRINT 'Value: ' || num;  -- NUMBER converted to STRING
```

## Null Handling

```sql
DECLARE maybe_null STRING;  -- NULL by default

-- Null coalescing
SET value = maybe_null ?? 'default';

-- Safe navigation
SET name = user?.profile?.name;
```

## Scope

Variables are scoped to their procedure:

```sql
CREATE PROCEDURE outer()
BEGIN
    DECLARE x NUMBER = 10;
    
    -- x is visible here
    CALL inner();  -- inner() cannot see x
    
    RETURN x;
END PROCEDURE;
```
