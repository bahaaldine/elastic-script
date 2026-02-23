CREATE SKILL get_service_latency
  VERSION '1.0.0'
  DESCRIPTION 'Get service latency metrics including p50, p95, and p99 percentiles.'
  AUTHOR 'apm-team'
  TAGS ['apm', 'latency', 'performance', 'metrics']
  (
    service_name STRING DESCRIPTION 'Service name to analyze',
    time_range STRING DESCRIPTION 'Time range to analyze' DEFAULT '1h'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE latency DOCUMENT;
  DECLARE metrics ARRAY;
  DECLARE trend ARRAY;
  
  -- Get latency percentiles
  SET metrics = ESQL_QUERY('
    FROM apm-*
    | WHERE @timestamp > NOW() - INTERVAL ' || time_range || '
    | WHERE service.name == "' || service_name || '"
    | STATS 
        avg_latency = AVG(transaction.duration.us),
        p50 = PERCENTILE(transaction.duration.us, 50),
        p95 = PERCENTILE(transaction.duration.us, 95),
        p99 = PERCENTILE(transaction.duration.us, 99),
        max_latency = MAX(transaction.duration.us),
        request_count = COUNT(*)
  ');
  
  -- Get trend over time
  SET trend = ESQL_QUERY('
    FROM apm-*
    | WHERE @timestamp > NOW() - INTERVAL ' || time_range || '
    | WHERE service.name == "' || service_name || '"
    | EVAL time_bucket = DATE_TRUNC(5 minutes, @timestamp)
    | STATS avg_latency = AVG(transaction.duration.us) BY time_bucket
    | SORT time_bucket ASC
  ');
  
  SET latency = {
    'service_name': service_name,
    'time_range': time_range,
    'metrics': metrics,
    'trend': trend,
    'analyzed_at': CURRENT_TIMESTAMP()
  };
  
  RETURN latency;
END SKILL;
