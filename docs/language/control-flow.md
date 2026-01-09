# Control Flow

## IF / THEN / ELSE

Conditional execution:

```sql
IF condition THEN
    -- statements
END IF;
```

With ELSE:

```sql
IF error_count == 0 THEN
    SET status = 'Healthy';
ELSE
    SET status = 'Has errors';
END IF;
```

With ELSEIF:

```sql
IF error_count == 0 THEN
    SET status = 'Healthy';
ELSEIF error_count < 10 THEN
    SET status = 'Warning';
ELSEIF error_count < 100 THEN
    SET status = 'Critical';
ELSE
    SET status = 'Emergency';
END IF;
```

## FOR Loop

### Range-based

Iterate over a numeric range:

```sql
FOR i IN 1..10 LOOP
    PRINT 'Count: ' || i;
END LOOP;
```

### Array iteration

Iterate over array elements:

```sql
DECLARE fruits ARRAY = ['apple', 'banana', 'cherry'];

FOR fruit IN fruits LOOP
    PRINT 'Processing: ' || fruit;
END LOOP;
```

### CURSOR iteration

Iterate over query results:

```sql
DECLARE logs CURSOR FOR
    FROM logs-sample
    | WHERE level == "ERROR"
    | LIMIT 100;

FOR log IN logs LOOP
    PRINT log.message;
END LOOP;
```

## WHILE Loop

Repeat while condition is true:

```sql
DECLARE i NUMBER = 0;

WHILE i < 10 LOOP
    PRINT 'i = ' || i;
    SET i = i + 1;
END LOOP;
```

!!! warning "Avoid infinite loops"
    Always ensure the condition will eventually become false.

## LOOP with EXIT

Manual loop control:

```sql
DECLARE i NUMBER = 0;

LOOP
    SET i = i + 1;
    
    IF i > 10 THEN
        EXIT;  -- Break out of loop
    END IF;
    
    PRINT 'i = ' || i;
END LOOP;
```

## CONTINUE

Skip to next iteration:

```sql
FOR i IN 1..10 LOOP
    IF i % 2 == 0 THEN
        CONTINUE;  -- Skip even numbers
    END IF;
    
    PRINT 'Odd: ' || i;
END LOOP;
```

## Nested Loops

```sql
FOR i IN 1..3 LOOP
    FOR j IN 1..3 LOOP
        PRINT i || ' x ' || j || ' = ' || (i * j);
    END LOOP;
END LOOP;
```

## CASE Expression

Pattern matching (in expressions):

```sql
DECLARE status STRING;
DECLARE severity NUMBER = 5;

SET status = CASE severity
    WHEN 1 THEN 'Low'
    WHEN 2 THEN 'Medium'
    WHEN 3 THEN 'High'
    ELSE 'Unknown'
END;
```

## Early Return

Exit procedure early:

```sql
CREATE PROCEDURE find_user(id NUMBER)
BEGIN
    IF id <= 0 THEN
        RETURN NULL;  -- Early exit
    END IF;
    
    -- Continue processing...
    RETURN user;
END PROCEDURE;
```
