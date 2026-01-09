# Procedures

Procedures are the fundamental building blocks of elastic-script.

## Creating Procedures

```sql
CREATE PROCEDURE procedure_name(parameters)
BEGIN
    -- statements
END PROCEDURE;
```

### Example

```sql
CREATE PROCEDURE greet(name STRING)
BEGIN
    RETURN 'Hello, ' || name || '!';
END PROCEDURE;
```

## Parameters

### Parameter Modes

| Mode | Description |
|------|-------------|
| `IN` | Input only (default) |
| `OUT` | Output only |
| `INOUT` | Both input and output |

```sql
CREATE PROCEDURE calculate(IN x NUMBER, IN y NUMBER, OUT result NUMBER)
BEGIN
    SET result = x + y;
END PROCEDURE;
```

### Parameter Types

```sql
CREATE PROCEDURE example(
    name STRING,
    count NUMBER,
    is_active BOOLEAN,
    items ARRAY,
    config DOCUMENT
)
BEGIN
    -- Use parameters
END PROCEDURE;
```

### Default Values

```sql
CREATE PROCEDURE greet(name STRING DEFAULT 'World')
BEGIN
    RETURN 'Hello, ' || name || '!';
END PROCEDURE;
```

## Calling Procedures

```sql
CALL procedure_name(arguments)
```

### Examples

```sql
-- Simple call
CALL greet('Alice')

-- With multiple arguments
CALL calculate(10, 20)

-- With named arguments (future)
CALL send_email(to: 'user@example.com', subject: 'Hello')
```

## Return Values

### Single Value

```sql
CREATE PROCEDURE add(a NUMBER, b NUMBER)
BEGIN
    RETURN a + b;
END PROCEDURE;
```

### Document (Object)

```sql
CREATE PROCEDURE get_stats()
BEGIN
    RETURN {
        "count": 42,
        "average": 3.14,
        "status": "ok"
    };
END PROCEDURE;
```

### Array

```sql
CREATE PROCEDURE get_items()
BEGIN
    RETURN ['apple', 'banana', 'cherry'];
END PROCEDURE;
```

### No Return

Procedures without `RETURN` return `NULL`:

```sql
CREATE PROCEDURE log_event(message STRING)
BEGIN
    PRINT message;
    INDEX_DOCUMENT('audit-log', {"message": message});
END PROCEDURE;
```

## Deleting Procedures

```sql
DELETE PROCEDURE procedure_name;
```

## Listing Procedures

```sql
-- Get all procedures
DECLARE procs ARRAY = ESCRIPT_PROCEDURES();

-- Get specific procedure
DECLARE proc DOCUMENT = ESCRIPT_PROCEDURE('greet');
```

## Stored Procedures

Procedures are stored in `.elastic_script_procedures`:

```json
{
  "_id": "greet",
  "_source": {
    "id": "greet",
    "body": "CREATE PROCEDURE greet(name STRING) BEGIN RETURN 'Hello, ' || name || '!'; END PROCEDURE;",
    "created_at": "2026-01-09T10:00:00Z"
  }
}
```

## Calling Other Procedures

Procedures can call other procedures:

```sql
CREATE PROCEDURE helper()
BEGIN
    RETURN 'I helped!';
END PROCEDURE;

CREATE PROCEDURE main()
BEGIN
    DECLARE result STRING = CALL helper();
    PRINT 'Helper said: ' || result;
    RETURN result;
END PROCEDURE;
```

## Best Practices

!!! tip "Single Responsibility"
    Each procedure should do one thing well.

!!! tip "Meaningful Names"
    Use descriptive names: `calculate_order_total` not `proc1`.

!!! tip "Document Parameters"
    Use comments to explain what each parameter does.

```sql
CREATE PROCEDURE process_order(
    order_id STRING,     -- Unique order identifier
    apply_discount BOOLEAN DEFAULT FALSE  -- Apply loyalty discount
)
BEGIN
    -- Implementation
END PROCEDURE;
```
