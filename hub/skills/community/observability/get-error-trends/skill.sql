CREATE SKILL get_error_trends
  VERSION '1.0.0'
  DESCRIPTION 'Analyze error trends over time to identify emerging issues.'
  AUTHOR 'sre-team'
  TAGS ['observability', 'errors', 'trends', 'analysis']
  (
    service_name STRING DESCRIPTION 'Service to analyze' DEFAULT NULL,
    time_range STRING DESCRIPTION 'Time range to analyze' DEFAULT '24h',
    bucket_size STRING DESCRIPTION 'Time bucket size' DEFAULT '1h'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE trends DOCUMENT;
  DECLARE hourly_errors ARRAY;
  DECLARE error_types ARRAY;
  DECLARE query STRING;
  
  SET query = 'FROM logs-* | WHERE @timestamp > NOW() - INTERVAL ' || time_range;
  SET query = query || ' | WHERE log.level == "ERROR"';
  
  IF service_name IS NOT NULL THEN
    SET query = query || ' | WHERE service.name == "' || service_name || '"';
  END IF;
  
  -- Get hourly error counts
  SET hourly_errors = ESQL_QUERY(query || '
    | EVAL time_bucket = DATE_TRUNC(' || bucket_size || ', @timestamp)
    | STATS error_count = COUNT(*) BY time_bucket
    | SORT time_bucket ASC
  ');
  
  -- Get error type breakdown
  SET error_types = ESQL_QUERY(query || '
    | STATS count = COUNT(*) BY error.type
    | SORT count DESC
    | LIMIT 10
  ');
  
  SET trends = {
    'service_name': service_name,
    'time_range': time_range,
    'hourly_errors': hourly_errors,
    'error_types': error_types,
    'analyzed_at': CURRENT_TIMESTAMP()
  };
  
  RETURN trends;
END SKILL;
