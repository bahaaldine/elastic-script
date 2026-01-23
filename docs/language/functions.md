# User-Defined Functions

User-defined functions allow you to create reusable code that returns values and can be used in expressions.

## Functions vs Procedures

| Aspect | PROCEDURE | FUNCTION |
|--------|-----------|----------|
| Returns value | No (uses OUT params) | Yes (RETURN statement) |
| Use in expressions | No | Yes |
| Side effects | Expected | Discouraged |
| Call syntax | `CALL proc()` | `func()` in expressions |
| Storage | `.elastic_script_procedures` | `.elastic_script_functions` |

## Creating Functions

### Basic Syntax

```sql
CREATE FUNCTION function_name(parameters)
RETURNS return_type AS
BEGIN
    -- statements
    RETURN value;
END FUNCTION;
```

### Example

```sql
CREATE FUNCTION calculate_severity(error_count NUMBER, warn_count NUMBER)
RETURNS STRING AS
BEGIN
    DECLARE score NUMBER = error_count * 10 + warn_count;
    
    IF score > 100 THEN
        RETURN 'critical';
    ELSIF score > 50 THEN
        RETURN 'high';
    ELSIF score > 20 THEN
        RETURN 'medium';
    ELSE
        RETURN 'low';
    END IF;
END FUNCTION;
```

## Using Functions

Functions can be used anywhere an expression is expected:

```sql
-- In variable assignment
DECLARE severity STRING = calculate_severity(15, 5);

-- In string concatenation
PRINT 'Status: ' || calculate_severity(errors, warnings);

-- In conditions
IF calculate_severity(err_count, warn_count) = 'critical' THEN
    CALL alert_team();
END IF;

-- As procedure arguments
CALL send_alert(calculate_severity(e, w), 'System Alert');
```

## Return Types

Functions must declare their return type:

```sql
-- Return STRING
CREATE FUNCTION get_greeting(name STRING)
RETURNS STRING AS
BEGIN
    RETURN 'Hello, ' || name || '!';
END FUNCTION;

-- Return NUMBER
CREATE FUNCTION calculate_total(price NUMBER, quantity NUMBER)
RETURNS NUMBER AS
BEGIN
    RETURN price * quantity;
END FUNCTION;

-- Return BOOLEAN
CREATE FUNCTION is_valid_email(email STRING)
RETURNS BOOLEAN AS
BEGIN
    RETURN INSTR(email, '@') > 0;
END FUNCTION;

-- Return DOCUMENT
CREATE FUNCTION create_user(name STRING, email STRING)
RETURNS DOCUMENT AS
BEGIN
    RETURN {
        "name": name,
        "email": email,
        "created_at": CURRENT_TIMESTAMP()
    };
END FUNCTION;

-- Return ARRAY
CREATE FUNCTION get_error_levels()
RETURNS ARRAY AS
BEGIN
    RETURN ['DEBUG', 'INFO', 'WARN', 'ERROR', 'FATAL'];
END FUNCTION;
```

## Parameters

### Parameter Modes

Functions support the same parameter modes as procedures:

```sql
CREATE FUNCTION process_data(
    IN input_data STRING,      -- Input only (default)
    OUT processed_count NUMBER, -- Output only
    INOUT buffer STRING         -- Both input and output
)
RETURNS BOOLEAN AS
BEGIN
    -- Process data
    SET processed_count = LENGTH(input_data);
    SET buffer = buffer || ' processed';
    RETURN true;
END FUNCTION;
```

### Default Values

```sql
CREATE FUNCTION format_date(
    date_val DATE,
    format STRING DEFAULT 'YYYY-MM-DD'
)
RETURNS STRING AS
BEGIN
    -- Format logic here
    RETURN date_val;  -- Simplified
END FUNCTION;

-- Call with default
DECLARE formatted = format_date(CURRENT_DATE());

-- Call with custom format
DECLARE custom = format_date(CURRENT_DATE(), 'MM/DD/YYYY');
```

## Recursive Functions

Functions can call themselves:

```sql
CREATE FUNCTION factorial(n NUMBER)
RETURNS NUMBER AS
BEGIN
    IF n <= 1 THEN
        RETURN 1;
    ELSE
        RETURN n * factorial(n - 1);
    END IF;
END FUNCTION;

PRINT factorial(5);  -- 120
```

## Functions Calling Functions

```sql
CREATE FUNCTION double(n NUMBER)
RETURNS NUMBER AS
BEGIN
    RETURN n * 2;
END FUNCTION;

CREATE FUNCTION quadruple(n NUMBER)
RETURNS NUMBER AS
BEGIN
    RETURN double(double(n));
END FUNCTION;

PRINT quadruple(5);  -- 20
```

## Error Handling in Functions

```sql
CREATE FUNCTION safe_divide(a NUMBER, b NUMBER)
RETURNS NUMBER AS
BEGIN
    TRY
        IF b = 0 THEN
            THROW 'Division by zero' WITH CODE 'MATH_001';
        END IF;
        RETURN a / b;
    CATCH
        PRINT 'Error in safe_divide: ' || error['message'];
        RETURN NULL;
    END TRY;
END FUNCTION;
```

## Deleting Functions

```sql
DELETE FUNCTION function_name;
```

## Listing Functions

```sql
-- List all user-defined functions
DECLARE funcs ARRAY = ESCRIPT_FUNCTIONS();

-- Get specific function info
DECLARE info DOCUMENT = ESCRIPT_FUNCTION('calculate_severity');
```

## Stored Functions

Functions are stored in the `.elastic_script_functions` index:

```json
{
  "_id": "calculate_severity",
  "_source": {
    "id": "calculate_severity",
    "body": "CREATE FUNCTION calculate_severity(...) RETURNS STRING AS BEGIN ... END FUNCTION;",
    "return_type": "STRING",
    "created_at": "2026-01-22T10:00:00Z"
  }
}
```

## Practical Examples

### Validation Function

```sql
CREATE FUNCTION validate_email(email STRING)
RETURNS BOOLEAN AS
BEGIN
    IF email = NULL OR LENGTH(email) = 0 THEN
        RETURN false;
    END IF;
    
    IF INSTR(email, '@') = 0 THEN
        RETURN false;
    END IF;
    
    IF INSTR(email, '.') = 0 THEN
        RETURN false;
    END IF;
    
    RETURN true;
END FUNCTION;

-- Usage
IF validate_email(user_email) THEN
    CALL send_welcome_email(user_email);
END IF;
```

### Formatting Function

```sql
CREATE FUNCTION format_bytes(bytes NUMBER)
RETURNS STRING AS
BEGIN
    IF bytes < 1024 THEN
        RETURN bytes || ' B';
    ELSIF bytes < 1024 * 1024 THEN
        RETURN ROUND(bytes / 1024, 2) || ' KB';
    ELSIF bytes < 1024 * 1024 * 1024 THEN
        RETURN ROUND(bytes / (1024 * 1024), 2) || ' MB';
    ELSE
        RETURN ROUND(bytes / (1024 * 1024 * 1024), 2) || ' GB';
    END IF;
END FUNCTION;

-- Usage
PRINT format_bytes(1536);      -- '1.5 KB'
PRINT format_bytes(2097152);   -- '2 MB'
```

### Business Logic Function

```sql
CREATE FUNCTION calculate_discount(
    subtotal NUMBER,
    customer_tier STRING
)
RETURNS NUMBER AS
BEGIN
    DECLARE discount_rate NUMBER = 0;
    
    IF customer_tier = 'gold' THEN
        SET discount_rate = 0.15;
    ELSIF customer_tier = 'silver' THEN
        SET discount_rate = 0.10;
    ELSIF customer_tier = 'bronze' THEN
        SET discount_rate = 0.05;
    END IF;
    
    -- Volume discount
    IF subtotal > 1000 THEN
        SET discount_rate = discount_rate + 0.05;
    END IF;
    
    RETURN subtotal * discount_rate;
END FUNCTION;

-- Usage
DECLARE discount = calculate_discount(1500, 'gold');
PRINT 'Discount: $' || discount;  -- 'Discount: $300'
```

## Best Practices

!!! tip "Pure Functions"
    Prefer functions without side effects. They're easier to test and reason about.

!!! tip "Single Responsibility"
    Each function should do one thing well.

!!! tip "Meaningful Names"
    Use descriptive names: `calculate_tax`, `validate_email`, `format_currency`.

!!! warning "Avoid Side Effects"
    Functions should compute and return values, not modify external state.

```sql
-- ❌ Bad: Function with side effects
CREATE FUNCTION get_and_increment_counter()
RETURNS NUMBER AS
BEGIN
    DECLARE val = counter;
    SET counter = counter + 1;  -- Side effect!
    RETURN val;
END FUNCTION;

-- ✅ Good: Pure function
CREATE FUNCTION increment(n NUMBER)
RETURNS NUMBER AS
BEGIN
    RETURN n + 1;
END FUNCTION;
```

## See Also

- [Procedures](procedures.md) - Procedures for side effects
- [Variables & Types](variables-types.md) - Data types
- [Control Flow](control-flow.md) - IF/ELSE, loops
- [Error Handling](error-handling.md) - TRY/CATCH
