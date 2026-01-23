# EXECUTE IMMEDIATE

EXECUTE IMMEDIATE allows you to build and execute ES|QL queries dynamically at runtime.

## Overview

Dynamic queries are useful when:

- Query structure depends on runtime conditions
- Building search queries from user input
- Creating flexible reporting procedures
- Constructing queries programmatically

## Basic Syntax

```sql
EXECUTE IMMEDIATE query_expression INTO result_variable;
```

Or with bind variables:

```sql
EXECUTE IMMEDIATE query_expression
    USING bind_value1, bind_value2, ...
    INTO result_variable;
```

## Simple Examples

### Basic Dynamic Query

```sql
DECLARE query STRING = 'FROM logs-* | LIMIT 10';
DECLARE results ARRAY;

EXECUTE IMMEDIATE query INTO results;

PRINT 'Found ' || ARRAY_LENGTH(results) || ' records';
```

### Building Query with Conditions

```sql
CREATE PROCEDURE search_logs(
    level_filter STRING,
    service_filter STRING,
    max_results NUMBER
)
BEGIN
    DECLARE query STRING = 'FROM logs-*';
    
    -- Add WHERE clause based on parameters
    IF level_filter IS NOT NULL THEN
        SET query = query || ' | WHERE level = ''' || level_filter || '''';
    END IF;
    
    IF service_filter IS NOT NULL THEN
        SET query = query || ' | WHERE service = ''' || service_filter || '''';
    END IF;
    
    SET query = query || ' | LIMIT ' || max_results;
    
    DECLARE results ARRAY;
    EXECUTE IMMEDIATE query INTO results;
    
    RETURN results;
END PROCEDURE;

-- Usage
DECLARE errors = CALL search_logs('ERROR', 'payment', 100);
```

## Bind Variables

Use bind variables (`:1`, `:2`, etc.) for safer query construction:

```sql
EXECUTE IMMEDIATE 
    'FROM logs-* | WHERE service = :1 AND level = :2 | LIMIT :3'
    USING 'payment-service', 'ERROR', 100
    INTO results;
```

### Benefits of Bind Variables

!!! tip "SQL Injection Prevention"
    Bind variables prevent injection attacks by properly escaping values.

!!! tip "Type Safety"
    Values are automatically formatted based on their type.

!!! tip "Readability"
    Separates query structure from data values.

### Bind Variable Types

| Type | Formatting |
|------|------------|
| STRING | Quoted with single quotes |
| NUMBER | Unquoted numeric value |
| BOOLEAN | `true` or `false` |
| NULL | `null` |

```sql
-- String value - gets quoted
EXECUTE IMMEDIATE 'FROM users | WHERE name = :1' 
    USING 'John' 
    INTO result;
-- Becomes: FROM users | WHERE name = 'John'

-- Number value - no quotes
EXECUTE IMMEDIATE 'FROM orders | WHERE total > :1'
    USING 1000
    INTO result;
-- Becomes: FROM orders | WHERE total > 1000

-- Boolean value
EXECUTE IMMEDIATE 'FROM users | WHERE active = :1'
    USING true
    INTO result;
-- Becomes: FROM users | WHERE active = true
```

## INTO Clause

The INTO clause captures query results into variables.

### Single Variable

```sql
DECLARE all_results ARRAY;
EXECUTE IMMEDIATE 'FROM logs-* | LIMIT 100' INTO all_results;
```

### Multiple Variables

Capture multiple columns from a single-row result:

```sql
DECLARE total_count NUMBER;
DECLARE error_count NUMBER;
DECLARE avg_duration NUMBER;

EXECUTE IMMEDIATE 
    'FROM metrics-* | STATS total = COUNT(*), errors = SUM(error_count), avg_dur = AVG(duration)'
    INTO total_count, error_count, avg_duration;

PRINT 'Total: ' || total_count;
PRINT 'Errors: ' || error_count;
PRINT 'Avg Duration: ' || avg_duration;
```

### Auto-Declaration

If variables don't exist, they're automatically declared with inferred types:

```sql
-- 'results' is auto-declared as ARRAY
EXECUTE IMMEDIATE 'FROM logs-* | LIMIT 10' INTO results;
PRINT ARRAY_LENGTH(results);
```

## Practical Examples

### Flexible Search Procedure

```sql
CREATE PROCEDURE dynamic_search(
    index_pattern STRING,
    filters ARRAY,      -- Array of {field, operator, value} documents
    sort_field STRING,
    limit_count NUMBER
)
BEGIN
    DECLARE query STRING = 'FROM ' || index_pattern;
    
    -- Add filters
    FOR i IN 1..ARRAY_LENGTH(filters) LOOP
        DECLARE f DOCUMENT = filters[i];
        SET query = query || ' | WHERE ' || f['field'] || ' ' || 
                    f['operator'] || ' ''' || f['value'] || '''';
    END LOOP;
    
    -- Add sorting
    IF sort_field IS NOT NULL THEN
        SET query = query || ' | SORT ' || sort_field || ' DESC';
    END IF;
    
    -- Add limit
    SET query = query || ' | LIMIT ' || limit_count;
    
    PRINT 'Executing: ' || query;
    
    DECLARE results ARRAY;
    EXECUTE IMMEDIATE query INTO results;
    RETURN results;
END PROCEDURE;
```

### Dynamic Aggregation

```sql
CREATE PROCEDURE get_stats_by_field(
    index STRING,
    group_by_field STRING,
    metric_field STRING
)
BEGIN
    DECLARE query STRING = 
        'FROM ' || index || 
        ' | STATS count = COUNT(*), ' ||
        'total = SUM(' || metric_field || '), ' ||
        'average = AVG(' || metric_field || ') ' ||
        'BY ' || group_by_field;
    
    DECLARE stats ARRAY;
    EXECUTE IMMEDIATE query INTO stats;
    RETURN stats;
END PROCEDURE;

-- Usage
DECLARE order_stats = CALL get_stats_by_field(
    'orders-*', 
    'category', 
    'total_amount'
);
```

### Time-Based Query Builder

```sql
CREATE PROCEDURE query_time_range(
    index STRING,
    start_time STRING,
    end_time STRING,
    additional_filter STRING
)
BEGIN
    DECLARE query STRING = 'FROM ' || index ||
        ' | WHERE @timestamp >= ''' || start_time || '''' ||
        ' AND @timestamp <= ''' || end_time || '''';
    
    IF additional_filter IS NOT NULL AND LENGTH(additional_filter) > 0 THEN
        SET query = query || ' AND (' || additional_filter || ')';
    END IF;
    
    SET query = query || ' | SORT @timestamp DESC | LIMIT 1000';
    
    DECLARE results ARRAY;
    EXECUTE IMMEDIATE query INTO results;
    RETURN results;
END PROCEDURE;
```

### Report Generator

```sql
CREATE PROCEDURE generate_report(report_type STRING)
BEGIN
    DECLARE query STRING;
    
    IF report_type = 'daily_summary' THEN
        SET query = 'FROM logs-* | WHERE @timestamp > NOW() - 1 DAY ' ||
                    '| STATS count = COUNT(*) BY level';
    ELSIF report_type = 'error_details' THEN
        SET query = 'FROM logs-* | WHERE level = ''ERROR'' ' ||
                    '| SORT @timestamp DESC | LIMIT 100';
    ELSIF report_type = 'service_health' THEN
        SET query = 'FROM metrics-* | STATS avg_cpu = AVG(cpu), ' ||
                    'avg_mem = AVG(memory) BY service';
    ELSE
        THROW 'Unknown report type: ' || report_type WITH CODE 'REPORT_001';
    END IF;
    
    DECLARE results ARRAY;
    EXECUTE IMMEDIATE query INTO results;
    RETURN results;
END PROCEDURE;
```

## Error Handling

Always wrap dynamic queries in TRY/CATCH:

```sql
CREATE PROCEDURE safe_query(query STRING)
BEGIN
    TRY
        DECLARE results ARRAY;
        EXECUTE IMMEDIATE query INTO results;
        RETURN results;
    CATCH esql_error
        PRINT 'Query syntax error: ' || error['message'];
        PRINT 'Query was: ' || query;
        RETURN [];
    CATCH
        PRINT 'Unexpected error: ' || error['message'];
        THROW error['message'] WITH CODE 'QUERY_FAILED';
    END TRY;
END PROCEDURE;
```

## Security Considerations

!!! warning "SQL Injection"
    Be careful when building queries from user input!

### Unsafe Pattern

```sql
-- ❌ DANGEROUS: Direct string concatenation with user input
DECLARE query = 'FROM users | WHERE name = ''' || user_input || '''';
EXECUTE IMMEDIATE query INTO results;
-- User could input: ' OR 1=1 --
```

### Safe Patterns

```sql
-- ✅ SAFE: Use bind variables
EXECUTE IMMEDIATE 'FROM users | WHERE name = :1'
    USING user_input
    INTO results;

-- ✅ SAFE: Validate input
IF NOT validate_identifier(user_input) THEN
    THROW 'Invalid input' WITH CODE 'SEC_001';
END IF;

-- ✅ SAFE: Use allowlist for field names
DECLARE allowed_fields ARRAY = ['name', 'email', 'status'];
IF NOT ARRAY_CONTAINS(allowed_fields, field_name) THEN
    THROW 'Invalid field' WITH CODE 'SEC_002';
END IF;
```

## Best Practices

!!! tip "Use Bind Variables"
    Always use bind variables for data values.

!!! tip "Validate Identifiers"
    Validate field names and index patterns against allowlists.

!!! tip "Log Queries"
    Log dynamic queries for debugging and auditing.

!!! tip "Handle Errors"
    Always wrap EXECUTE IMMEDIATE in TRY/CATCH.

!!! warning "Avoid Building Queries from Untrusted Input"
    If you must use user input, sanitize and validate thoroughly.

## See Also

- [Procedures](procedures.md) - Creating procedures
- [Error Handling](error-handling.md) - TRY/CATCH
- [Variables & Types](variables-types.md) - Data types
