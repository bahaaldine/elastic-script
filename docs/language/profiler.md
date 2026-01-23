# Profiler

The profiler helps you analyze and optimize procedure performance by collecting execution timing data.

## Overview

Use the profiler to:

- **Measure execution time** - Total and per-statement timing
- **Identify bottlenecks** - Find slow statements and queries
- **Get recommendations** - Automated performance suggestions
- **Track performance over time** - Store and compare profiles

## Profiling a Procedure

Use `PROFILE CALL` to execute a procedure while collecting timing information:

```sql
PROFILE CALL my_procedure()
```

With arguments:

```sql
PROFILE CALL calculate_metrics('logs-*', 100)
```

The profiler captures:

- Start and end time
- Total execution duration
- Individual statement timing
- Function call timing
- ESQL query timing and row counts

## Viewing Profile Results

### Show Last Profile

```sql
SHOW PROFILE
```

Shows detailed results from the most recent profiled execution, including:
- Execution summary (duration, counts)
- Slowest statements
- Slowest ESQL queries

### Show All Profiles

```sql
SHOW PROFILES
```

Lists all stored profile results, sorted by most recent first.

### Show Profile for Specific Procedure

```sql
SHOW PROFILE FOR procedure_name
```

Shows the most recent profile for a specific procedure.

## Analyzing Performance

### Basic Analysis

```sql
ANALYZE PROFILE
```

Provides analysis of the last profile, including:
- **Time breakdown** - Percentage spent in ESQL, functions, and other operations
- **Recommendations** - Actionable suggestions for optimization

### Analyze Specific Procedure

```sql
ANALYZE PROFILE FOR procedure_name
```

## Analysis Recommendations

The analyzer provides recommendations such as:

| Issue | Recommendation |
|-------|----------------|
| >70% time in ESQL | Optimize queries or add indices |
| Many ESQL queries | Use BULK COLLECT or batching |
| Slow individual queries | Review query structure, add indices |
| Slow statements | Consider refactoring or caching |

## Clearing Profile Data

### Clear All Profiles

```sql
CLEAR PROFILES
```

Removes all stored profile data.

### Clear Profiles for Procedure

```sql
CLEAR PROFILE FOR procedure_name
```

## Profile Data Structure

Profile results include:

| Field | Description |
|-------|-------------|
| `procedure_name` | Name of the profiled procedure |
| `start_time` | Execution start timestamp |
| `end_time` | Execution end timestamp |
| `total_duration_ms` | Total execution time in milliseconds |
| `total_esql_time_ms` | Time spent in ESQL queries |
| `total_function_time_ms` | Time spent in function calls |
| `statement_count` | Number of statements executed |
| `function_call_count` | Number of function calls |
| `esql_query_count` | Number of ESQL queries |
| `statements` | Array of statement profiles |
| `function_calls` | Array of function call profiles |
| `esql_queries` | Array of ESQL query profiles |

## Best Practices

1. **Profile in realistic conditions** - Test with production-like data volumes
2. **Profile multiple times** - Account for variance in execution times
3. **Focus on hot paths** - Optimize code that runs frequently
4. **Use ANALYZE** - Let the system identify issues automatically
5. **Clear old profiles** - Remove outdated data periodically

## Example: Optimizing a Slow Procedure

```sql
-- Create a procedure
CREATE PROCEDURE slow_procedure()
BEGIN
    FOR i IN 1..1000 LOOP
        ESQL_QUERY 'FROM logs-* | LIMIT 1';
    END LOOP;
END PROCEDURE;

-- Profile it
PROFILE CALL slow_procedure()

-- Analyze results
ANALYZE PROFILE
-- Result: "High number of ESQL queries (1000). Consider batching or using BULK COLLECT."

-- Optimized version
CREATE PROCEDURE fast_procedure()
BEGIN
    BULK COLLECT INTO results FROM FROM logs-* | LIMIT 1000;
    FOR doc IN results LOOP
        -- Process doc
    END LOOP;
END PROCEDURE;

-- Profile the optimized version
PROFILE CALL fast_procedure()

-- Compare results
SHOW PROFILES
```

## Storage

Profile data is stored in the `.escript_profiles` index and persists across restarts. Use `CLEAR PROFILES` to manage storage usage.

## See Also

- [Procedures](procedures.md) - Creating stored procedures
- [Bulk Operations](bulk-operations.md) - FORALL and BULK COLLECT for performance
- [ESQL Integration](esql.md) - Optimizing ESQL queries
