# ES|QL Integration

!!! tip "Interactive Notebook"
    This page is generated from a Jupyter notebook. For the best experience, 
    run it interactively in Jupyter:
    
    ```bash
    ./scripts/quick-start.sh
    # Then open http://localhost:8888/notebooks/02-esql-integration.ipynb
    ```
    
    [:fontawesome-brands-github: View on GitHub](https://github.com/bahaaldine/elastic-script/blob/main/notebooks/02-esql-integration.ipynb){ .md-button }

---

# 📊 ESQL Integration

elastic-script is built on top of ESQL, Elasticsearch's query language. This notebook shows how to combine procedural logic with powerful ESQL queries.

## Prerequisites

This notebook uses sample data. Load it with:
```bash
./scripts/quick-start.sh --load-data
```


## ESQL_QUERY() Function

Use `ESQL_QUERY()` to run any ESQL query and get results as an array:


```sql
CREATE PROCEDURE get_recent_logs()
BEGIN
    DECLARE logs ARRAY = ESQL_QUERY('FROM logs-sample | LIMIT 10');
    PRINT 'Found ' || ARRAY_LENGTH(logs) || ' logs';
    RETURN logs;
END PROCEDURE

```

```sql
CALL get_recent_logs()

```

## CURSOR - Iterate Over Query Results

For large result sets, use a CURSOR to iterate row by row:


```sql
CREATE PROCEDURE analyze_log_levels()
BEGIN
    DECLARE logs CURSOR FOR FROM logs-sample | LIMIT 50;

    DECLARE info_count NUMBER = 0;
    DECLARE warn_count NUMBER = 0;
    DECLARE error_count NUMBER = 0;

    FOR log IN logs LOOP
        IF log['level'] == 'INFO' THEN
            SET info_count = info_count + 1;
        ELSEIF log['level'] == 'WARN' THEN
            SET warn_count = warn_count + 1;
        ELSEIF log['level'] == 'ERROR' THEN
            SET error_count = error_count + 1;
        END IF
    END LOOP

    RETURN {
        "info": info_count,
        "warn": warn_count,
        "error": error_count
    };
END PROCEDURE

```

```sql
CALL analyze_log_levels()

```

## Aggregations with STATS


```sql
CREATE PROCEDURE get_service_stats()
BEGIN
    DECLARE stats ARRAY = ESQL_QUERY(
        'FROM logs-sample | STATS count = COUNT(*) BY service'
    );

    PRINT 'Service distribution:';
    FOR stat IN stats LOOP
        PRINT stat['service'] || ': ' || stat['count'] || ' logs';
    END LOOP

    RETURN stats;
END PROCEDURE

```

```sql
CALL get_service_stats()

```

