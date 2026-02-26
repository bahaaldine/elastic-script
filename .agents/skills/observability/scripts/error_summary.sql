-- Error Summary Report
-- Usage: Execute to get a summary of recent errors
-- Parameters: Set @minutes variable before calling (default: 60)

DECLARE minutes NUMBER;
SET minutes = COALESCE(@minutes, 60);

DECLARE errors ARRAY;
DECLARE summary DOCUMENT;

-- Get error counts by service
ESQL FROM logs-*
| WHERE @timestamp > NOW() - minutes MINUTES
| WHERE level = 'ERROR' OR log.level = 'error'
| STATS error_count = COUNT(*) BY service.name
| SORT error_count DESC
| LIMIT 20
INTO errors;

-- Get total error count
DECLARE total_errors NUMBER;
ESQL FROM logs-*
| WHERE @timestamp > NOW() - minutes MINUTES
| WHERE level = 'ERROR' OR log.level = 'error'
| STATS count = COUNT(*)
INTO total_errors;

-- Get top error messages
DECLARE top_messages ARRAY;
ESQL FROM logs-*
| WHERE @timestamp > NOW() - minutes MINUTES
| WHERE level = 'ERROR' OR log.level = 'error'
| STATS count = COUNT(*) BY message
| SORT count DESC
| LIMIT 10
INTO top_messages;

-- Build summary
SET summary = {
    'time_range_minutes': minutes,
    'total_errors': total_errors,
    'errors_by_service': errors,
    'top_error_messages': top_messages,
    'generated_at': CURRENT_TIMESTAMP()
};

-- Print report
PRINT '=== Error Summary (Last ' || minutes || ' minutes) ===';
PRINT 'Total Errors: ' || total_errors;
PRINT '';
PRINT '=== Errors by Service ===';

FOR svc IN errors LOOP
    PRINT svc['service.name'] || ': ' || svc['error_count'] || ' errors';
END LOOP;

PRINT '';
PRINT '=== Top Error Messages ===';

FOR msg IN top_messages LOOP
    PRINT '[' || msg['count'] || '] ' || SUBSTR(msg['message'], 1, 100);
END LOOP;

RETURN summary;
