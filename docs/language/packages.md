# Packages

Packages group related procedures, functions, and variables into a single namespace. They provide:

- **Encapsulation** - Hide implementation details with private members
- **Organization** - Group related functionality together
- **State** - Share variables across package procedures
- **Reusability** - Define once, use everywhere

## Package Structure

A package consists of two parts:

1. **Specification** (`CREATE PACKAGE`) - Declares the public interface
2. **Body** (`CREATE PACKAGE BODY`) - Implements the declared members

## Creating a Package Specification

The specification declares what procedures, functions, and variables are available:

```sql
CREATE PACKAGE package_name AS
    -- Procedure declarations
    PROCEDURE procedure_name(params);
    
    -- Function declarations
    FUNCTION function_name(params) RETURNS type;
    
    -- Variable declarations
    variable_name TYPE [= default_value];
END PACKAGE
```

### Example

```sql
CREATE PACKAGE string_utils AS
    -- Public procedures
    PROCEDURE log_message(msg STRING, level STRING);
    
    -- Public functions
    FUNCTION capitalize(s STRING) RETURNS STRING;
    FUNCTION is_empty(s STRING) RETURNS BOOLEAN;
    FUNCTION truncate(s STRING, max_len NUMBER) RETURNS STRING;
    
    -- Package constants
    max_length NUMBER = 1000;
    default_separator STRING = ', ';
END PACKAGE
```

## Creating a Package Body

The body implements the procedures and functions declared in the specification:

```sql
CREATE PACKAGE BODY package_name AS
    -- Procedure implementations
    PROCEDURE procedure_name(params)
    BEGIN
        -- statements
    END PROCEDURE;
    
    -- Function implementations
    FUNCTION function_name(params) RETURNS type AS
    BEGIN
        RETURN value;
    END FUNCTION;
END PACKAGE
```

### Example

```sql
CREATE PACKAGE BODY string_utils AS
    PROCEDURE log_message(msg STRING, level STRING)
    BEGIN
        PRINT '[' || level || '] ' || msg;
    END PROCEDURE;
    
    FUNCTION capitalize(s STRING) RETURNS STRING AS
    BEGIN
        IF is_empty(s) THEN
            RETURN s;
        END IF;
        RETURN UPPER(SUBSTR(s, 1, 1)) || LOWER(SUBSTR(s, 2));
    END FUNCTION;
    
    FUNCTION is_empty(s STRING) RETURNS BOOLEAN AS
    BEGIN
        RETURN s IS NULL OR LENGTH(TRIM(s)) = 0;
    END FUNCTION;
    
    FUNCTION truncate(s STRING, max_len NUMBER) RETURNS STRING AS
    BEGIN
        IF LENGTH(s) <= max_len THEN
            RETURN s;
        END IF;
        RETURN SUBSTR(s, 1, max_len - 3) || '...';
    END FUNCTION;
END PACKAGE
```

## Public and Private Members

Control visibility with `PUBLIC` and `PRIVATE` keywords. By default, all members are public.

```sql
CREATE PACKAGE counter AS
    -- Public: accessible from outside
    PUBLIC PROCEDURE increment();
    PUBLIC PROCEDURE decrement();
    PUBLIC FUNCTION get_value() RETURNS NUMBER;
    
    -- Private: internal use only
    PRIVATE current_value NUMBER = 0;
    PRIVATE PROCEDURE validate();
END PACKAGE
```

### Visibility Rules

| Modifier | Description |
|----------|-------------|
| `PUBLIC` | Accessible from outside the package (default) |
| `PRIVATE` | Only accessible within the package body |

## Managing Packages

### View Package Details

```sql
SHOW PACKAGE package_name
```

Returns:
- Package name
- List of procedures with parameters
- List of functions with parameters and return types
- List of variables with types
- Whether body is implemented
- Created/updated timestamps

### Drop a Package

```sql
DROP PACKAGE package_name
```

Removes both the specification and body.

## Real-World Example: Alert Manager

```sql
-- Specification
CREATE PACKAGE alert_manager AS
    -- Configuration (public)
    PUBLIC max_alerts_per_hour NUMBER = 100;
    PUBLIC cooldown_seconds NUMBER = 300;
    
    -- Public API
    PUBLIC PROCEDURE send_alert(message STRING, severity STRING);
    PUBLIC PROCEDURE send_slack_alert(channel STRING, message STRING);
    PUBLIC PROCEDURE clear_alerts();
    PUBLIC FUNCTION get_alert_count() RETURNS NUMBER;
    PUBLIC FUNCTION should_throttle() RETURNS BOOLEAN;
    
    -- Internal state (private)
    PRIVATE alert_count NUMBER = 0;
    PRIVATE last_alert_time STRING;
    PRIVATE PROCEDURE increment_counter();
END PACKAGE
```

```sql
-- Implementation
CREATE PACKAGE BODY alert_manager AS
    PROCEDURE send_alert(message STRING, severity STRING)
    BEGIN
        IF should_throttle() THEN
            PRINT 'Alert throttled: too many alerts';
            RETURN;
        END IF;
        
        increment_counter();
        PRINT 'ALERT [' || severity || ']: ' || message;
    END PROCEDURE;
    
    PROCEDURE send_slack_alert(channel STRING, message STRING)
    BEGIN
        IF should_throttle() THEN
            RETURN;
        END IF;
        
        SLACK_SEND(channel, message);
        increment_counter();
    END PROCEDURE;
    
    PROCEDURE clear_alerts()
    BEGIN
        SET alert_count = 0;
        PRINT 'Alert counter reset';
    END PROCEDURE;
    
    FUNCTION get_alert_count() RETURNS NUMBER AS
    BEGIN
        RETURN alert_count;
    END FUNCTION;
    
    FUNCTION should_throttle() RETURNS BOOLEAN AS
    BEGIN
        RETURN alert_count >= max_alerts_per_hour;
    END FUNCTION;
    
    PROCEDURE increment_counter()
    BEGIN
        SET alert_count = alert_count + 1;
    END PROCEDURE;
END PACKAGE
```

## Best Practices

1. **Separate specification and body** - Define the interface first, then implement
2. **Use private for internal state** - Hide implementation details
3. **Group related functionality** - One package per domain/concern
4. **Document public members** - Make the API self-explanatory
5. **Use meaningful names** - Package names should describe their purpose

## Package Storage

Packages are stored in the `.escript_packages` index with:
- Specification text (for recreation)
- Body text (for recreation)
- Parsed member metadata
- Creation/update timestamps

## See Also

- [Procedures](procedures.md) - Creating standalone procedures
- [User-Defined Functions](functions.md) - Creating standalone functions
- [Variables & Types](variables-types.md) - Data types reference
