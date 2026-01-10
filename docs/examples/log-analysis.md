# Log Analysis Examples

Real-world examples of log analysis and monitoring with elastic-script.

## Basic Error Counting

Count errors by service over the last hour:

```sql
CREATE PROCEDURE count_errors_by_service()
BEGIN
    DECLARE results CURSOR FOR ESQL_QUERY('
        FROM logs-*
        | WHERE level = "ERROR" AND @timestamp > NOW() - 1 hour
        | STATS error_count = COUNT(*) BY service.name
        | SORT error_count DESC
        | LIMIT 20
    ');
    
    PRINT '=== Errors by Service (Last Hour) ===';
    
    FOR row IN results LOOP
        DECLARE service STRING = DOCUMENT_GET(row, 'service.name');
        DECLARE count NUMBER = DOCUMENT_GET(row, 'error_count');
        PRINT service || ': ' || count || ' errors';
    END LOOP;
END PROCEDURE;
```

---

## Error Pattern Detection

Identify recurring error patterns:

```sql
CREATE PROCEDURE find_error_patterns()
BEGIN
    -- Get recent errors
    DECLARE errors CURSOR FOR ESQL_QUERY('
        FROM logs-*
        | WHERE level = "ERROR"
        | SORT @timestamp DESC
        | LIMIT 100
    ');
    
    -- Group by message pattern (first 50 chars)
    DECLARE patterns DOCUMENT = {};
    
    FOR error IN errors LOOP
        DECLARE msg STRING = DOCUMENT_GET(error, 'message');
        DECLARE pattern STRING = SUBSTR(msg, 1, 50);
        
        -- Count occurrences
        IF DOCUMENT_CONTAINS(patterns, pattern) THEN
            DECLARE current NUMBER = DOCUMENT_GET(patterns, pattern);
            SET patterns = DOCUMENT_MERGE(patterns, {pattern: current + 1});
        ELSE
            SET patterns = DOCUMENT_MERGE(patterns, {pattern: 1});
        END IF;
    END LOOP;
    
    PRINT '=== Error Patterns ===';
    DECLARE keys ARRAY = DOCUMENT_KEYS(patterns);
    FOR i IN 0..(ARRAY_LENGTH(keys)-1) LOOP
        DECLARE pattern STRING = keys[i];
        DECLARE count NUMBER = DOCUMENT_GET(patterns, pattern);
        IF count > 3 THEN  -- Only show repeated patterns
            PRINT count || 'x: ' || pattern || '...';
        END IF;
    END LOOP;
END PROCEDURE;
```

---

## AI-Powered Error Summarization

Use LLM to summarize and categorize errors:

```sql
CREATE PROCEDURE summarize_errors()
BEGIN
    -- Collect recent errors
    DECLARE errors CURSOR FOR ESQL_QUERY('
        FROM logs-*
        | WHERE level = "ERROR"
        | SORT @timestamp DESC
        | LIMIT 20
    ');
    
    DECLARE messages ARRAY = [];
    FOR error IN errors LOOP
        DECLARE msg STRING = DOCUMENT_GET(error, 'message');
        SET messages = ARRAY_APPEND(messages, msg);
    END LOOP;
    
    IF ARRAY_LENGTH(messages) = 0 THEN
        PRINT 'No errors found in the specified time range.';
        RETURN 'No errors';
    END IF;
    
    -- Create AI prompt
    DECLARE prompt STRING = 'Analyze these error messages and provide:
1. A brief summary of the main issues
2. Likely root causes
3. Recommended actions

Error messages:
' || ARRAY_JOIN(messages, '\n');
    
    DECLARE summary STRING = LLM_COMPLETE(prompt);
    
    PRINT '=== AI Error Analysis ===';
    PRINT summary;
    
    RETURN summary;
END PROCEDURE;
```

---

## Anomaly Detection

Detect unusual log patterns:

```sql
CREATE PROCEDURE detect_anomalies()
BEGIN
    -- Get hourly error rates for the past day
    DECLARE hourly CURSOR FOR ESQL_QUERY('
        FROM logs-*
        | WHERE @timestamp > NOW() - 24 hours
        | EVAL hour = DATE_TRUNC(1 hour, @timestamp)
        | STATS error_count = COUNT(*) BY hour, level
        | WHERE level = "ERROR"
        | SORT hour
    ');
    
    -- Calculate baseline
    DECLARE counts ARRAY = [];
    DECLARE hours ARRAY = [];
    
    FOR row IN hourly LOOP
        SET counts = ARRAY_APPEND(counts, DOCUMENT_GET(row, 'error_count'));
        SET hours = ARRAY_APPEND(hours, DOCUMENT_GET(row, 'hour'));
    END LOOP;
    
    -- Simple anomaly detection: flag if > 2x average
    DECLARE total NUMBER = 0;
    FOR i IN 0..(ARRAY_LENGTH(counts)-1) LOOP
        SET total = total + counts[i];
    END LOOP;
    DECLARE avg NUMBER = total / ARRAY_LENGTH(counts);
    DECLARE threshold NUMBER = avg * 2;
    
    PRINT '=== Anomaly Detection ===';
    PRINT 'Average hourly errors: ' || ROUND(avg, 1);
    PRINT 'Anomaly threshold: ' || ROUND(threshold, 1);
    PRINT '';
    
    FOR i IN 0..(ARRAY_LENGTH(counts)-1) LOOP
        IF counts[i] > threshold THEN
            PRINT '⚠️ ANOMALY at ' || hours[i] || ': ' || counts[i] || ' errors';
        END IF;
    END LOOP;
END PROCEDURE;
```

---

## Service Health Dashboard

Create a real-time health summary:

```sql
CREATE PROCEDURE service_health_check()
BEGIN
    PRINT '╔══════════════════════════════════════════╗';
    PRINT '║        SERVICE HEALTH DASHBOARD          ║';
    PRINT '╚══════════════════════════════════════════╝';
    PRINT '';
    
    -- Get service metrics
    DECLARE metrics CURSOR FOR ESQL_QUERY('
        FROM logs-*
        | WHERE @timestamp > NOW() - 5 minutes
        | STATS 
            total = COUNT(*),
            errors = COUNT(*) WHERE level = "ERROR",
            warnings = COUNT(*) WHERE level = "WARN"
          BY service.name
        | EVAL error_rate = ROUND(errors * 100.0 / total, 1)
        | SORT error_rate DESC
    ');
    
    FOR svc IN metrics LOOP
        DECLARE name STRING = DOCUMENT_GET(svc, 'service.name');
        DECLARE error_rate NUMBER = DOCUMENT_GET(svc, 'error_rate');
        DECLARE errors NUMBER = DOCUMENT_GET(svc, 'errors');
        
        -- Determine status icon
        DECLARE status STRING;
        IF error_rate > 10 THEN
            SET status = '🔴 CRITICAL';
        ELSEIF error_rate > 5 THEN
            SET status = '🟡 WARNING';
        ELSE
            SET status = '🟢 HEALTHY';
        END IF;
        
        PRINT status || ' ' || name;
        PRINT '    Error Rate: ' || error_rate || '% (' || errors || ' errors)';
    END LOOP;
END PROCEDURE;
```

---

## Scheduled Log Cleanup

Identify old indices for cleanup:

```sql
CREATE PROCEDURE identify_old_logs(retention_days NUMBER)
BEGIN
    DECLARE cutoff DATE = DATE_SUB(CURRENT_DATE(), retention_days);
    
    PRINT '=== Logs Older Than ' || retention_days || ' Days ===';
    PRINT 'Cutoff date: ' || cutoff;
    
    DECLARE old_logs CURSOR FOR ESQL_QUERY('
        FROM logs-*
        | STATS 
            doc_count = COUNT(*),
            oldest = MIN(@timestamp),
            newest = MAX(@timestamp)
          BY _index
        | WHERE oldest < "' || cutoff || '"
        | SORT oldest
    ');
    
    DECLARE total_docs NUMBER = 0;
    
    FOR idx IN old_logs LOOP
        DECLARE index_name STRING = DOCUMENT_GET(idx, '_index');
        DECLARE docs NUMBER = DOCUMENT_GET(idx, 'doc_count');
        DECLARE oldest DATE = DOCUMENT_GET(idx, 'oldest');
        
        PRINT index_name;
        PRINT '    Documents: ' || docs;
        PRINT '    Oldest: ' || oldest;
        
        SET total_docs = total_docs + docs;
    END LOOP;
    
    PRINT '';
    PRINT 'Total documents eligible for cleanup: ' || total_docs;
END PROCEDURE;
```

---

## Usage

Call these procedures to analyze your logs:

```sql
-- Count errors by service
CALL count_errors_by_service();

-- Find repeating patterns
CALL find_error_patterns();

-- Get AI summary of errors
CALL summarize_errors();

-- Check for anomalies
CALL detect_anomalies();

-- Service health dashboard
CALL service_health_check();
```
