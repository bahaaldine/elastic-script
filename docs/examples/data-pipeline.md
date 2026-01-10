# Data Pipeline Examples

ETL, data transformation, and processing workflows with elastic-script.

## Basic Data Transformation

Transform and enrich documents:

```sql
CREATE PROCEDURE enrich_logs(source_index STRING, target_index STRING)
BEGIN
    PRINT '=== Enriching logs from ' || source_index || ' ===';
    
    DECLARE logs CURSOR FOR ESQL_QUERY('
        FROM ' || source_index || '
        | LIMIT 1000
    ');
    
    DECLARE processed NUMBER = 0;
    DECLARE errors NUMBER = 0;
    
    FOR log IN logs LOOP
        TRY
            -- Get original fields
            DECLARE message STRING = DOCUMENT_GET(log, 'message');
            DECLARE level STRING = DOCUMENT_GET(log, 'level');
            DECLARE timestamp DATE = DOCUMENT_GET(log, '@timestamp');
            
            -- Enrich with computed fields
            DECLARE enriched DOCUMENT = DOCUMENT_MERGE(log, {
                "message_length": LENGTH(message),
                "level_normalized": UPPER(level),
                "day_of_week": EXTRACT_DAY(timestamp),
                "processed_at": CURRENT_TIMESTAMP(),
                "processed_by": "elastic-script-pipeline"
            });
            
            -- Classify severity
            DECLARE severity NUMBER;
            IF level = 'ERROR' THEN
                SET severity = 1;
            ELSEIF level = 'WARN' THEN
                SET severity = 2;
            ELSEIF level = 'INFO' THEN
                SET severity = 3;
            ELSE
                SET severity = 4;
            END IF;
            SET enriched = DOCUMENT_MERGE(enriched, {"severity_score": severity});
            
            -- Index to target
            ES_INDEX(target_index, NULL, enriched);
            SET processed = processed + 1;
            
        CATCH error
            SET errors = errors + 1;
            PRINT 'Error processing document: ' || error;
        END TRY;
    END LOOP;
    
    PRINT 'Processed: ' || processed || ', Errors: ' || errors;
    RETURN processed;
END PROCEDURE;
```

---

## Aggregation Pipeline

Compute and store aggregated metrics:

```sql
CREATE PROCEDURE aggregate_metrics(time_window STRING)
BEGIN
    PRINT '=== Computing Metrics (' || time_window || ') ===';
    
    -- Aggregate by service
    DECLARE service_metrics CURSOR FOR ESQL_QUERY('
        FROM logs-*
        | WHERE @timestamp > NOW() - ' || time_window || '
        | STATS 
            request_count = COUNT(*),
            error_count = COUNT(*) WHERE level = "ERROR",
            avg_response_time = AVG(response_time_ms)
          BY service.name
    ');
    
    DECLARE timestamp DATE = CURRENT_TIMESTAMP();
    
    FOR svc IN service_metrics LOOP
        DECLARE service STRING = DOCUMENT_GET(svc, 'service.name');
        DECLARE requests NUMBER = DOCUMENT_GET(svc, 'request_count');
        DECLARE errors NUMBER = DOCUMENT_GET(svc, 'error_count');
        DECLARE avg_time NUMBER = DOCUMENT_GET(svc, 'avg_response_time');
        
        -- Calculate error rate
        DECLARE error_rate NUMBER = 0;
        IF requests > 0 THEN
            SET error_rate = ROUND((errors * 100.0) / requests, 2);
        END IF;
        
        -- Create metric document
        DECLARE metric DOCUMENT = {
            "@timestamp": timestamp,
            "service": service,
            "time_window": time_window,
            "metrics": {
                "request_count": requests,
                "error_count": errors,
                "error_rate_percent": error_rate,
                "avg_response_time_ms": ROUND(avg_time, 2)
            }
        };
        
        -- Store metric
        ES_INDEX('service-metrics', NULL, metric);
        
        PRINT service || ': ' || requests || ' requests, ' || error_rate || '% errors';
    END LOOP;
END PROCEDURE;
```

---

## Data Migration

Migrate data between indices with transformation:

```sql
CREATE PROCEDURE migrate_index(
    source_index STRING, 
    target_index STRING,
    batch_size NUMBER
)
BEGIN
    PRINT '=== Migrating ' || source_index || ' to ' || target_index || ' ===';
    
    DECLARE total_migrated NUMBER = 0;
    DECLARE batch_number NUMBER = 0;
    DECLARE continue_migration BOOLEAN = TRUE;
    
    WHILE continue_migration LOOP
        SET batch_number = batch_number + 1;
        DECLARE offset NUMBER = (batch_number - 1) * batch_size;
        
        PRINT 'Processing batch ' || batch_number || ' (offset: ' || offset || ')';
        
        DECLARE docs CURSOR FOR ESQL_QUERY('
            FROM ' || source_index || '
            | SORT @timestamp
            | LIMIT ' || batch_size || '
        ');
        
        DECLARE batch_count NUMBER = 0;
        
        FOR doc IN docs LOOP
            -- Apply any transformations here
            DECLARE transformed DOCUMENT = DOCUMENT_MERGE(doc, {
                "migrated_at": CURRENT_TIMESTAMP(),
                "source_index": source_index
            });
            
            -- Remove any fields you don't want
            SET transformed = DOCUMENT_REMOVE(transformed, '_score');
            
            ES_INDEX(target_index, NULL, transformed);
            SET batch_count = batch_count + 1;
        END LOOP;
        
        SET total_migrated = total_migrated + batch_count;
        
        -- Check if we've processed all documents
        IF batch_count < batch_size THEN
            SET continue_migration = FALSE;
        END IF;
        
        PRINT '  Migrated ' || batch_count || ' documents (total: ' || total_migrated || ')';
    END LOOP;
    
    PRINT '';
    PRINT 'Migration complete: ' || total_migrated || ' documents';
    RETURN total_migrated;
END PROCEDURE;
```

---

## Data Quality Checks

Validate data quality and generate reports:

```sql
CREATE PROCEDURE data_quality_check(index_name STRING)
BEGIN
    PRINT '╔══════════════════════════════════════════╗';
    PRINT '║        DATA QUALITY REPORT               ║';
    PRINT '║        Index: ' || RPAD(index_name, 26, ' ') || '║';
    PRINT '╚══════════════════════════════════════════╝';
    PRINT '';
    
    DECLARE issues ARRAY = [];
    
    -- Check 1: Document count
    DECLARE count_result CURSOR FOR ESQL_QUERY('
        FROM ' || index_name || ' | STATS count = COUNT(*)
    ');
    DECLARE total_docs NUMBER = 0;
    FOR row IN count_result LOOP
        SET total_docs = DOCUMENT_GET(row, 'count');
    END LOOP;
    PRINT '📊 Total documents: ' || total_docs;
    
    -- Check 2: Missing required fields
    DECLARE missing_timestamp CURSOR FOR ESQL_QUERY('
        FROM ' || index_name || '
        | WHERE @timestamp IS NULL
        | STATS count = COUNT(*)
    ');
    FOR row IN missing_timestamp LOOP
        DECLARE missing NUMBER = DOCUMENT_GET(row, 'count');
        IF missing > 0 THEN
            PRINT '⚠️  Missing @timestamp: ' || missing || ' documents';
            SET issues = ARRAY_APPEND(issues, 'Missing @timestamp: ' || missing);
        ELSE
            PRINT '✅ All documents have @timestamp';
        END IF;
    END LOOP;
    
    -- Check 3: Date range
    DECLARE date_range CURSOR FOR ESQL_QUERY('
        FROM ' || index_name || '
        | STATS 
            oldest = MIN(@timestamp),
            newest = MAX(@timestamp)
    ');
    FOR row IN date_range LOOP
        PRINT '📅 Date range: ' || DOCUMENT_GET(row, 'oldest') 
            || ' to ' || DOCUMENT_GET(row, 'newest');
    END LOOP;
    
    -- Check 4: Field cardinality
    DECLARE level_dist CURSOR FOR ESQL_QUERY('
        FROM ' || index_name || '
        | STATS count = COUNT(*) BY level
    ');
    PRINT '';
    PRINT '📊 Level distribution:';
    FOR row IN level_dist LOOP
        DECLARE level STRING = DOCUMENT_GET(row, 'level');
        DECLARE count NUMBER = DOCUMENT_GET(row, 'count');
        DECLARE pct NUMBER = ROUND((count * 100.0) / total_docs, 1);
        PRINT '   ' || RPAD(level, 10, ' ') || count || ' (' || pct || '%)';
    END LOOP;
    
    -- Summary
    PRINT '';
    IF ARRAY_LENGTH(issues) = 0 THEN
        PRINT '✅ No data quality issues found';
    ELSE
        PRINT '⚠️  Found ' || ARRAY_LENGTH(issues) || ' issues:';
        FOR i IN 0..(ARRAY_LENGTH(issues)-1) LOOP
            PRINT '   • ' || issues[i];
        END LOOP;
    END IF;
    
    RETURN issues;
END PROCEDURE;
```

---

## ETL with External APIs

Fetch external data and load into Elasticsearch:

```sql
CREATE PROCEDURE load_external_data(api_url STRING, target_index STRING)
BEGIN
    PRINT '=== Loading data from external API ===';
    PRINT 'Source: ' || api_url;
    
    TRY
        -- Fetch data from API
        DECLARE response STRING = HTTP_GET(api_url);
        
        -- Parse JSON response (assuming array of objects)
        -- Note: Actual JSON parsing implementation may vary
        DECLARE items ARRAY = JSON_PARSE(response);
        
        PRINT 'Received ' || ARRAY_LENGTH(items) || ' items';
        
        DECLARE loaded NUMBER = 0;
        FOR i IN 0..(ARRAY_LENGTH(items)-1) LOOP
            DECLARE item DOCUMENT = items[i];
            
            -- Add metadata
            SET item = DOCUMENT_MERGE(item, {
                "loaded_at": CURRENT_TIMESTAMP(),
                "source_url": api_url
            });
            
            ES_INDEX(target_index, NULL, item);
            SET loaded = loaded + 1;
        END LOOP;
        
        PRINT 'Loaded ' || loaded || ' documents to ' || target_index;
        RETURN loaded;
        
    CATCH error
        PRINT 'Failed to load data: ' || error;
        RETURN 0;
    END TRY;
END PROCEDURE;
```

---

## Scheduled Cleanup

Remove old data based on retention policy:

```sql
CREATE PROCEDURE cleanup_old_data(index_pattern STRING, retention_days NUMBER)
BEGIN
    DECLARE cutoff DATE = DATE_SUB(CURRENT_DATE(), retention_days);
    
    PRINT '=== Data Cleanup ===';
    PRINT 'Index pattern: ' || index_pattern;
    PRINT 'Retention: ' || retention_days || ' days';
    PRINT 'Cutoff date: ' || cutoff;
    
    -- Find old documents
    DECLARE old_docs CURSOR FOR ESQL_QUERY('
        FROM ' || index_pattern || '
        | WHERE @timestamp < "' || cutoff || '"
        | STATS count = COUNT(*), indices = VALUES(_index)
    ');
    
    FOR row IN old_docs LOOP
        DECLARE count NUMBER = DOCUMENT_GET(row, 'count');
        DECLARE indices ARRAY = DOCUMENT_GET(row, 'indices');
        
        IF count > 0 THEN
            PRINT 'Found ' || count || ' documents to clean up';
            PRINT 'Affected indices: ' || ARRAY_JOIN(indices, ', ');
            
            -- In production, you would delete these documents
            -- ES_DELETE_BY_QUERY(index_pattern, '@timestamp < "' || cutoff || '"');
            
            PRINT '⚠️  Cleanup would delete ' || count || ' documents';
            PRINT '   (Dry run - no documents deleted)';
        ELSE
            PRINT '✅ No documents older than retention period';
        END IF;
    END LOOP;
END PROCEDURE;
```

---

## Usage

```sql
-- Enrich and transform logs
CALL enrich_logs('raw-logs', 'enriched-logs');

-- Compute hourly metrics
CALL aggregate_metrics('1 hour');

-- Migrate index with transformation
CALL migrate_index('logs-v1', 'logs-v2', 500);

-- Run data quality checks
CALL data_quality_check('logs-sample');

-- Cleanup old data (dry run)
CALL cleanup_old_data('logs-*', 90);
```
