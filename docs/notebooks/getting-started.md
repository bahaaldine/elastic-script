# Getting Started

!!! tip "Interactive Notebook"
    This page is generated from a Jupyter notebook. For the best experience, 
    run it interactively in Jupyter:
    
    ```bash
    ./scripts/quick-start.sh
    # Then open http://localhost:8888/notebooks/01-getting-started.ipynb
    ```
    
    [:fontawesome-brands-github: View on GitHub](https://github.com/bahaaldine/elastic-script/blob/main/notebooks/01-getting-started.ipynb){ .md-button }

---


Welcome to elastic-script! This notebook will guide you through the basics of writing procedures that run directly in Elasticsearch.

## What is elastic-script?

elastic-script (also known as PL|ESQL) is a **procedural extension to ESQL** that adds:
- Variables and control flow (IF, WHILE, FOR)
- Stored procedures
- Built-in functions for common operations
- Integration with external services (Kubernetes, PagerDuty, AWS, etc.)
- AI/LLM capabilities for intelligent automation

## Prerequisites

Make sure Elasticsearch is running with elastic-script:
```bash
cd /path/to/elastic-script
./scripts/quick-start.sh --start
```


## Your First Procedure

Let's start with a simple "Hello, World!" procedure:


```sql
CREATE PROCEDURE hello_world()
BEGIN
    RETURN 'Hello, World!';
END PROCEDURE

```

```sql
CALL hello_world()

```

## Procedures with Parameters

Let's create a procedure that takes a parameter:


```sql
CREATE PROCEDURE greet(name STRING)
BEGIN
    RETURN 'Hello, ' || name || '!';
END PROCEDURE

```

```sql
CALL greet('Elastic User')

```

## Variables and Types

elastic-script supports several data types:
- `STRING` - Text values
- `NUMBER` - Numeric values (integers or decimals)
- `BOOLEAN` - TRUE or FALSE
- `DOCUMENT` - JSON-like key-value structure
- `ARRAY` - List of values
- `DATE` - Date/time values


```sql
CREATE PROCEDURE demo_types()
BEGIN
    -- Declare variables with DECLARE
    DECLARE message STRING = 'Hello';
    DECLARE count NUMBER = 42;
    DECLARE is_active BOOLEAN = TRUE;
    DECLARE user DOCUMENT = { "name": "Alice", "role": "admin" };
    DECLARE items ARRAY = ['apple', 'banana', 'cherry'];

    -- Use VAR for type inference
    VAR inferred_type = 'This is a string';

    -- Use CONST for immutable values
    CONST PI = 3.14159;

    PRINT 'Message: ' || message;
    PRINT 'Count: ' || count;
    PRINT 'User: ' || user['name'];
    PRINT 'First item: ' || items[0];

    RETURN 'Types demo complete!';
END PROCEDURE

```

```sql
CALL demo_types()

```

## Control Flow

### IF/ELSE


```sql
CREATE PROCEDURE check_severity(error_count NUMBER)
BEGIN
    DECLARE status STRING;

    IF error_count == 0 THEN
        SET status = 'Healthy';
    ELSEIF error_count < 10 THEN
        SET status = 'Warning';
    ELSE
        SET status = 'Critical';
    END IF

    RETURN status;
END PROCEDURE

```

```sql
CALL check_severity(5)

```

### FOR Loops


```sql
CREATE PROCEDURE count_to(n NUMBER)
BEGIN
    DECLARE result STRING = '';

    FOR i IN 1..n LOOP
        SET result = result || i || ' ';
    END LOOP

    RETURN TRIM(result);
END PROCEDURE

```

```sql
CALL count_to(5)

```

### Error Handling with TRY/CATCH


```sql
CREATE PROCEDURE safe_divide(a NUMBER, b NUMBER)
BEGIN
    TRY
        IF b == 0 THEN
            THROW 'Division by zero!';
        END IF
        RETURN a / b;
    CATCH
        RETURN 'Error: Cannot divide by zero';
    END TRY
END PROCEDURE

```

```sql
CALL safe_divide(10, 0)

```

## Next Steps

Now that you know the basics, explore:

1. **02-esql-integration.ipynb** - Query Elasticsearch data with ESQL
2. **03-ai-observability.ipynb** - AI-powered log analysis
3. **04-async-execution.ipynb** - Pipe-driven async workflows
4. **05-runbook-integrations.ipynb** - K8s, AWS, PagerDuty integrations


