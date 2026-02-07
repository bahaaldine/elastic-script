# Intents

**Intents** are declarative operations that encode SRE expertise with built-in guardrails. They're designed for safe execution by AI/LLM agents.

## Overview

An intent encapsulates:
- **Description** - What the intent does
- **Parameters** - Input values with types
- **REQUIRES** - Pre-conditions that must be true
- **ACTIONS** - Steps to execute
- **ON_FAILURE** - Error handling

## Syntax

### Defining an Intent

```sql
DEFINE INTENT intent_name(param1 TYPE, param2 TYPE)
DESCRIPTION 'What this intent does'
REQUIRES
    condition1,
    condition2
ACTIONS
    statement1;
    statement2;
ON_FAILURE
    error_handling_statement;
END INTENT
```

### Invoking an Intent

```sql
-- Positional arguments
INTENT intent_name(value1, value2);

-- Named arguments
INTENT intent_name WITH param1 = value1, param2 = value2;
```

## Examples

### Simple Intent

```sql
DEFINE INTENT greet(name STRING)
DESCRIPTION 'Greets a user'
ACTIONS
    PRINT 'Hello, ' || name || '!';
END INTENT

-- Invoke
INTENT greet('Alice');
INTENT greet WITH name = 'Bob';
```

### Intent with Pre-conditions

```sql
DEFINE INTENT scale_pods(replicas NUMBER)
DESCRIPTION 'Scale Kubernetes pods'
REQUIRES
    replicas > 0,
    replicas <= 100
ACTIONS
    PRINT 'Scaling to ' || replicas || ' replicas';
    -- K8S_SCALE('my-deployment', replicas);
END INTENT

-- Will succeed
INTENT scale_pods(5);

-- Will fail REQUIRES
INTENT scale_pods(0);  -- Error: REQUIRES conditions not met
```

### Intent with Error Handling

```sql
DEFINE INTENT restart_service(service STRING)
DESCRIPTION 'Safely restart a service'
REQUIRES
    LENGTH(service) > 0
ACTIONS
    PRINT 'Stopping ' || service;
    -- Stop logic here
    PRINT 'Starting ' || service;
    -- Start logic here - might fail
ON_FAILURE
    PRINT 'Restart failed: ' || ERROR_MESSAGE;
    PRINT 'Rolling back...';
    -- Rollback logic here
END INTENT
```

## REQUIRES Clause

Pre-conditions are evaluated before ACTIONS execute. If any condition is false, the intent fails without executing ACTIONS.

```sql
DEFINE INTENT update_config(key STRING, value STRING, environment STRING)
REQUIRES
    LENGTH(key) > 0,
    environment IN ('dev', 'staging', 'prod'),
    NOT (environment = 'prod' AND key = 'debug')  -- Never enable debug in prod
ACTIONS
    PRINT 'Updating ' || key || ' = ' || value;
END INTENT
```

Conditions support:
- Boolean expressions
- Comparison operators (`>`, `<`, `=`, `!=`, `>=`, `<=`)
- Logical operators (`AND`, `OR`, `NOT`)
- Function calls (`LENGTH()`, `INSTR()`, etc.)

## ON_FAILURE Clause

When ACTIONS fail (throw an exception), ON_FAILURE statements execute. The `ERROR_MESSAGE` variable contains the error text.

```sql
DEFINE INTENT deploy(version STRING)
ACTIONS
    PRINT 'Deploying ' || version;
    IF version = 'bad' THEN
        RAISE 'Deployment failed: invalid version';
    END IF;
ON_FAILURE
    PRINT 'Deployment error: ' || ERROR_MESSAGE;
    PRINT 'Initiating rollback...';
    -- Rollback to previous version
END INTENT
```

## Introspection

Use introspection functions to discover and inspect intents:

```sql
-- List all intents
DECLARE intents ARRAY = ESCRIPT_INTENTS();
FOR intent IN intents LOOP
    PRINT intent['name'] || ': ' || intent['description'];
END LOOP;

-- Get details about a specific intent
DECLARE info DOCUMENT = ESCRIPT_INTENT('restart_service');
PRINT 'Signature: ' || info['signature'];
PRINT 'Has REQUIRES: ' || info['has_requires'];
```

### ESCRIPT_INTENTS() Return Format

Returns an array of documents with:

| Field | Type | Description |
|-------|------|-------------|
| `name` | STRING | Intent name |
| `description` | STRING | Description text |
| `signature` | STRING | Human-readable signature |
| `parameter_count` | NUMBER | Number of parameters |
| `has_requires` | BOOLEAN | Has pre-conditions |
| `has_on_failure` | BOOLEAN | Has error handling |

### ESCRIPT_INTENT(name) Return Format

Returns a document with:

| Field | Type | Description |
|-------|------|-------------|
| `name` | STRING | Intent name |
| `exists` | BOOLEAN | Whether intent exists |
| `description` | STRING | Description text |
| `signature` | STRING | Human-readable signature |
| `parameters` | ARRAY | Parameter definitions |
| `has_requires` | BOOLEAN | Has pre-conditions |
| `requires_count` | NUMBER | Number of pre-conditions |
| `has_on_failure` | BOOLEAN | Has error handling |

## Use Cases

### AI Agent Integration

Intents provide a safe interface for LLM agents to execute operational tasks:

```sql
DEFINE INTENT investigate_alert(alert_id STRING)
DESCRIPTION 'Investigate an alert and provide analysis'
REQUIRES
    LENGTH(alert_id) > 0
ACTIONS
    PRINT 'Investigating alert: ' || alert_id;
    
    -- Query related logs
    DECLARE logs ARRAY = ESQL_QUERY('
        FROM logs-*
        | WHERE alert_id = "' || alert_id || '"
        | LIMIT 100
    ');
    
    -- Analyze and summarize
    PRINT 'Found ' || ARRAY_LENGTH(logs) || ' related log entries';
    -- LLM_SUMMARIZE(logs);
END INTENT
```

### Runbook Automation

Encode operational procedures as intents:

```sql
DEFINE INTENT database_failover(primary STRING, secondary STRING)
DESCRIPTION 'Safely failover database to secondary'
REQUIRES
    primary != secondary,
    LENGTH(primary) > 0,
    LENGTH(secondary) > 0
ACTIONS
    PRINT '1. Verifying secondary health...';
    PRINT '2. Stopping writes to primary...';
    PRINT '3. Syncing final transactions...';
    PRINT '4. Promoting secondary to primary...';
    PRINT '5. Updating DNS/routing...';
    PRINT 'Failover complete: ' || secondary || ' is now primary';
ON_FAILURE
    PRINT 'CRITICAL: Failover failed!';
    PRINT 'Error: ' || ERROR_MESSAGE;
    PRINT 'Manual intervention required.';
    -- PAGERDUTY_TRIGGER('database-failover-failed', ERROR_MESSAGE);
END INTENT
```

## Best Practices

1. **Always include DESCRIPTION** - Helps LLMs understand what the intent does
2. **Use REQUIRES for safety** - Validate inputs before execution
3. **Handle failures gracefully** - Include ON_FAILURE for critical operations
4. **Keep intents focused** - One intent = one operation
5. **Use named arguments for clarity** - `INTENT foo WITH x = 1` is clearer than `INTENT foo(1)`

## See Also

- [Procedures](procedures.md) - For complex logic
- [Functions](functions.md) - For reusable computations
- [Error Handling](error-handling.md) - TRY/CATCH/FINALLY
