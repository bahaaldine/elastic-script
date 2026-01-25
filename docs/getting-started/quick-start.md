# Quick Start

This guide will have you running elastic-script procedures in 5 minutes.

## Hello World

Create your first procedure:

```bash
curl -u elastic-admin:elastic-password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{
    "query": "CREATE PROCEDURE hello_world() BEGIN RETURN '\''Hello, World!'\''; END PROCEDURE;"
  }'
```

Call it:

```bash
curl -u elastic-admin:elastic-password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "CALL hello_world()"}'
```

Response:
```json
{"result": "Hello, World!"}
```

## With Parameters

```sql
CREATE PROCEDURE greet(name STRING)
BEGIN
    RETURN 'Hello, ' || name || '!';
END PROCEDURE;
```

```bash
curl -u elastic-admin:elastic-password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "CALL greet('\''Alice'\'')"}'
```

Response:
```json
{"result": "Hello, Alice!"}
```

## Variables and Types

```sql
CREATE PROCEDURE demo_types()
BEGIN
    -- String
    DECLARE message STRING = 'Hello';
    
    -- Number
    DECLARE count NUMBER = 42;
    
    -- Boolean
    DECLARE is_active BOOLEAN = TRUE;
    
    -- Array
    DECLARE items ARRAY = ['apple', 'banana', 'cherry'];
    
    -- Document (JSON object)
    DECLARE user DOCUMENT = {"name": "John", "age": 30};
    
    PRINT 'Message: ' || message;
    PRINT 'Count: ' || count;
    PRINT 'Active: ' || is_active;
    PRINT 'First item: ' || items[0];
    PRINT 'User name: ' || user.name;
    
    RETURN 'Demo complete!';
END PROCEDURE;
```

## Control Flow

### IF/THEN/ELSE

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
    END IF;
    
    RETURN status;
END PROCEDURE;
```

### FOR Loop

```sql
CREATE PROCEDURE count_to(n NUMBER)
BEGIN
    FOR i IN 1..n LOOP
        PRINT 'Count: ' || i;
    END LOOP;
    
    RETURN 'Done!';
END PROCEDURE;
```

### Array Iteration

```sql
CREATE PROCEDURE process_items()
BEGIN
    DECLARE fruits ARRAY = ['apple', 'banana', 'cherry'];
    
    FOR fruit IN fruits LOOP
        PRINT 'Processing: ' || fruit;
    END LOOP;
    
    RETURN ARRAY_LENGTH(fruits) || ' items processed';
END PROCEDURE;
```

## Query Elasticsearch

```sql
CREATE PROCEDURE get_recent_logs()
BEGIN
    -- Execute ES|QL query
    DECLARE logs ARRAY = ESQL_QUERY('FROM logs-sample | LIMIT 10');
    
    PRINT 'Found ' || ARRAY_LENGTH(logs) || ' logs';
    
    RETURN logs;
END PROCEDURE;
```

## Error Handling

```sql
CREATE PROCEDURE safe_divide(a NUMBER, b NUMBER)
BEGIN
    TRY
        IF b == 0 THEN
            THROW 'Division by zero!';
        END IF;
        RETURN a / b;
    CATCH
        RETURN 'Error: Cannot divide by zero';
    END TRY;
END PROCEDURE;
```

## Distributed Tracing

Traces are collected automatically via the OTEL Collector. Any OTEL-instrumented app can send traces:

```bash
# Configure your app
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318
export OTEL_SERVICE_NAME=my-service

# View traces in Kibana APM
open http://localhost:5601/app/apm
```

See [OpenTelemetry Tracing](../observability/opentelemetry.md) for complete documentation.

## Next Steps

- [Jupyter Setup](jupyter-setup.md) - Interactive development
- [Language Overview](../language/overview.md) - Full syntax guide
- [Function Reference](../functions/overview.md) - All 118 functions
- [OpenTelemetry Tracing](../observability/opentelemetry.md) - Distributed tracing
