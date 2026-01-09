# Error Handling

## TRY / CATCH / FINALLY

Handle errors gracefully:

```sql
TRY
    -- Code that might fail
    DECLARE result = risky_operation();
CATCH
    -- Handle error
    PRINT 'An error occurred';
FINALLY
    -- Always executed
    cleanup();
END TRY;
```

## Basic Example

```sql
CREATE PROCEDURE safe_divide(a NUMBER, b NUMBER)
BEGIN
    TRY
        IF b == 0 THEN
            THROW 'Division by zero!';
        END IF;
        RETURN a / b;
    CATCH
        RETURN NULL;
    END TRY;
END PROCEDURE;
```

## THROW

Raise an error:

```sql
THROW 'Error message';
```

With context:

```sql
IF user == NULL THEN
    THROW 'User not found: ' || user_id;
END IF;
```

## Catching Specific Errors

The `@error` variable contains the error message:

```sql
TRY
    -- Code
CATCH
    PRINT 'Caught error: ' || @error;
    
    IF @error LIKE '%timeout%' THEN
        -- Handle timeout
    ELSEIF @error LIKE '%not found%' THEN
        -- Handle not found
    ELSE
        -- Re-throw unknown errors
        THROW @error;
    END IF;
END TRY;
```

## FINALLY Block

Always executed, even if an error occurred:

```sql
TRY
    -- Open resources
    DECLARE conn = open_connection();
    -- Use connection
CATCH
    PRINT 'Error: ' || @error;
FINALLY
    -- Always close
    close_connection(conn);
END TRY;
```

## Nested TRY Blocks

```sql
TRY
    TRY
        -- Inner operation
        risky_call();
    CATCH
        PRINT 'Inner error handled';
    END TRY;
    
    -- Continue outer block
    another_call();
CATCH
    PRINT 'Outer error: ' || @error;
END TRY;
```

## Error Propagation

Errors propagate up if not caught:

```sql
CREATE PROCEDURE inner_proc()
BEGIN
    THROW 'Something went wrong!';
END PROCEDURE;

CREATE PROCEDURE outer_proc()
BEGIN
    TRY
        CALL inner_proc();  -- Error propagates here
    CATCH
        PRINT 'Caught from inner: ' || @error;
    END TRY;
END PROCEDURE;
```

## Validation Pattern

```sql
CREATE PROCEDURE validate_order(order DOCUMENT)
BEGIN
    IF order.quantity <= 0 THEN
        THROW 'Quantity must be positive';
    END IF;
    
    IF order.price < 0 THEN
        THROW 'Price cannot be negative';
    END IF;
    
    IF order.customer_id == NULL THEN
        THROW 'Customer ID is required';
    END IF;
    
    RETURN TRUE;
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
        CATCH
            PRINT 'Attempt ' || attempt || ' failed: ' || @error;
            
            IF attempt >= max_attempts THEN
                THROW 'Max retries exceeded: ' || @error;
            END IF;
            
            -- Wait before retry (if available)
            -- SLEEP(1000);
        END TRY;
    END LOOP;
END PROCEDURE;
```

## Best Practices

!!! tip "Be Specific"
    Catch specific error types when possible.

!!! tip "Log Errors"
    Always log errors for debugging.

!!! warning "Don't Swallow Errors"
    Empty CATCH blocks hide problems.

```sql
-- ❌ Bad: swallowing errors
TRY
    risky_call();
CATCH
    -- Silent failure
END TRY;

-- ✅ Good: handle or log
TRY
    risky_call();
CATCH
    PRINT 'Error in risky_call: ' || @error;
    -- Take appropriate action
END TRY;
```
