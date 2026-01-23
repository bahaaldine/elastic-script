# Error Handling

elastic-script provides comprehensive exception handling with TRY/CATCH/FINALLY blocks, named exceptions, and structured error information.

## TRY / CATCH / FINALLY

Handle errors gracefully with structured exception handling:

```sql
TRY
    -- Code that might fail
    DECLARE result = risky_operation();
CATCH
    -- Handle error
    PRINT 'Error: ' || error['message'];
FINALLY
    -- Always executed (cleanup)
    CALL cleanup();
END TRY;
```

## The @error Document

When an exception is caught, the `error` variable is available as a DOCUMENT with these fields:

| Field | Type | Description |
|-------|------|-------------|
| `message` | STRING | Error message |
| `code` | STRING | Error code (if provided) |
| `type` | STRING | Exception type |
| `stack_trace` | STRING | Stack trace |
| `cause` | STRING | Original cause |

```sql
TRY
    CALL risky_operation();
CATCH
    PRINT 'Message: ' || error['message'];
    PRINT 'Code: ' || error['code'];
    PRINT 'Type: ' || error['type'];
END TRY;
```

## Named Exceptions

Catch specific exception types with named CATCH blocks:

```sql
TRY
    DECLARE result = HTTP_GET('https://api.example.com/data');
    SET parsed = JSON_PARSE(result);
CATCH http_error
    PRINT 'HTTP call failed: ' || error['message'];
    CALL log_error(error);
CATCH parse_error
    PRINT 'JSON parsing failed';
    SET parsed = {};
CATCH timeout_error
    PRINT 'Request timed out';
    CALL retry_later();
CATCH
    -- Catch-all for any other errors
    PRINT 'Unexpected error: ' || error['message'];
FINALLY
    -- Always runs (cleanup)
    CALL close_connections();
END TRY;
```

### Exception Types

| Type | Description |
|------|-------------|
| `error` | Generic (catch-all) |
| `http_error` | HTTP/network errors |
| `timeout_error` | Timeout errors |
| `division_error` | Division by zero |
| `null_reference_error` | Null pointer errors |
| `type_error` | Type mismatch |
| `validation_error` | Validation failures |
| `not_found_error` | Resource not found |
| `permission_error` | Auth/permission errors |
| `esql_error` | ES\|QL query errors |
| `function_error` | Built-in function errors |

## THROW and RAISE

Raise exceptions with `THROW` or `RAISE` (they are aliases):

### Simple Throw

```sql
THROW 'Something went wrong!';
RAISE 'Validation failed';
```

### Throw with Expression

```sql
DECLARE msg STRING = 'User ' || user_id || ' not found';
THROW msg;
```

### Throw with Error Code

Use `WITH CODE` to provide an error code:

```sql
THROW 'Resource not found' WITH CODE 'HTTP_404';
RAISE 'Invalid input' WITH CODE 'VALIDATION_001';

-- With expressions
DECLARE error_code STRING = 'ERR_' || error_type;
THROW error_message WITH CODE error_code;
```

## Basic Examples

### Simple Error Handling

```sql
CREATE PROCEDURE safe_divide(a NUMBER, b NUMBER)
BEGIN
    TRY
        IF b = 0 THEN
            THROW 'Division by zero!' WITH CODE 'MATH_001';
        END IF;
        RETURN a / b;
    CATCH division_error
        PRINT 'Math error: ' || error['message'];
        RETURN NULL;
    CATCH
        PRINT 'Unexpected error';
        RETURN NULL;
    END TRY;
END PROCEDURE;
```

### Validation with Errors

```sql
CREATE PROCEDURE validate_order(order DOCUMENT)
BEGIN
    IF order['quantity'] <= 0 THEN
        THROW 'Quantity must be positive' WITH CODE 'VAL_001';
    END IF;
    
    IF order['price'] < 0 THEN
        THROW 'Price cannot be negative' WITH CODE 'VAL_002';
    END IF;
    
    IF order['customer_id'] = NULL THEN
        THROW 'Customer ID is required' WITH CODE 'VAL_003';
    END IF;
    
    RETURN true;
END PROCEDURE;
```

### HTTP Error Handling

```sql
CREATE PROCEDURE fetch_data(url STRING)
BEGIN
    TRY
        DECLARE response = HTTP_GET(url);
        RETURN response;
    CATCH http_error
        PRINT 'HTTP failed: ' || error['message'];
        IF error['code'] = 'HTTP_404' THEN
            RETURN NULL;
        ELSE
            THROW error['message'] WITH CODE error['code'];
        END IF;
    CATCH timeout_error
        PRINT 'Request timed out, retrying...';
        -- Retry logic here
        RETURN NULL;
    END TRY;
END PROCEDURE;
```

## FINALLY Block

The FINALLY block always executes, even if an error occurred:

```sql
TRY
    -- Open resources
    DECLARE conn = open_connection();
    -- Use connection
    CALL process_data(conn);
CATCH
    PRINT 'Error: ' || error['message'];
FINALLY
    -- Always close - runs even if error occurred
    CALL close_connection(conn);
END TRY;
```

## Nested TRY Blocks

```sql
TRY
    TRY
        -- Inner operation
        CALL risky_call();
    CATCH
        PRINT 'Inner error handled: ' || error['message'];
    END TRY;
    
    -- Continue outer block
    CALL another_call();
CATCH
    PRINT 'Outer error: ' || error['message'];
END TRY;
```

## Error Propagation

Errors propagate up if not caught:

```sql
CREATE PROCEDURE inner_proc()
BEGIN
    THROW 'Something went wrong!' WITH CODE 'INNER_001';
END PROCEDURE;

CREATE PROCEDURE outer_proc()
BEGIN
    TRY
        CALL inner_proc();  -- Error propagates here
    CATCH
        PRINT 'Caught from inner: ' || error['message'];
        PRINT 'Error code: ' || error['code'];
    END TRY;
END PROCEDURE;
```

## Retry Pattern

```sql
CREATE PROCEDURE with_retry(max_attempts NUMBER)
BEGIN
    DECLARE attempt NUMBER = 0;
    
    WHILE attempt < max_attempts LOOP
        SET attempt = attempt + 1;
        
        TRY
            DECLARE result = call_api();
            RETURN result;  -- Success, exit
        CATCH timeout_error
            PRINT 'Attempt ' || attempt || ' timed out';
            IF attempt >= max_attempts THEN
                THROW 'Max retries exceeded' WITH CODE 'RETRY_EXHAUSTED';
            END IF;
        CATCH
            -- Non-retryable error
            THROW error['message'] WITH CODE error['code'];
        END TRY;
    END LOOP;
END PROCEDURE;
```

## Re-throwing Errors

Re-throw an error after logging or partial handling:

```sql
TRY
    CALL risky_operation();
CATCH
    -- Log the error
    CALL log_error(error);
    
    -- Re-throw to caller
    THROW error['message'] WITH CODE error['code'];
END TRY;
```

## Best Practices

!!! tip "Be Specific with Exception Types"
    Use named exceptions to handle different error types appropriately.

!!! tip "Use Error Codes"
    Provide error codes with `WITH CODE` for programmatic error handling.

!!! tip "Always Clean Up"
    Use FINALLY blocks for resource cleanup.

!!! warning "Don't Swallow Errors"
    Empty CATCH blocks hide problems. Always log or handle errors.

```sql
-- ❌ Bad: swallowing errors
TRY
    risky_call();
CATCH
    -- Silent failure - BAD!
END TRY;

-- ✅ Good: handle or log
TRY
    risky_call();
CATCH
    PRINT 'Error: ' || error['message'];
    CALL log_error(error);
    -- Take appropriate action or re-throw
END TRY;
```

!!! tip "Provide Context"
    Include relevant context in error messages.

```sql
-- ❌ Bad: vague message
THROW 'Error occurred';

-- ✅ Good: specific context
THROW 'Failed to process order ' || order_id || ': invalid quantity' 
    WITH CODE 'ORDER_INVALID';
```

## See Also

- [Control Flow](control-flow.md) - IF/ELSE, loops
- [Procedures](procedures.md) - Creating procedures
- [Variables & Types](variables-types.md) - Data types
