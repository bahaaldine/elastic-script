CREATE SKILL get_throughput_stats
  VERSION '1.0.0'
  DESCRIPTION 'Get service throughput statistics including requests per minute.'
  AUTHOR 'apm-team'
  TAGS ['apm', 'throughput', 'performance', 'metrics']
  (
    service_name STRING DESCRIPTION 'Service name to analyze' DEFAULT NULL,
    time_range STRING DESCRIPTION 'Time range to analyze' DEFAULT '1h'
  )
  RETURNS ARRAY
BEGIN
  DECLARE throughput ARRAY;
  DECLARE query STRING;
  
  SET query = 'FROM apm-* | WHERE @timestamp > NOW() - INTERVAL ' || time_range;
  
  IF service_name IS NOT NULL THEN
    SET query = query || ' | WHERE service.name == "' || service_name || '"';
  END IF;
  
  SET throughput = ESQL_QUERY(query || '
    | STATS 
        total_requests = COUNT(*),
        successful = COUNT(*) WHERE transaction.result == "success",
        failed = COUNT(*) WHERE transaction.result == "failure",
        avg_duration = AVG(transaction.duration.us)
      BY service.name
    | EVAL rpm = total_requests / 60
    | EVAL success_rate = successful * 100.0 / total_requests
    | SORT total_requests DESC
  ');
  
  RETURN throughput;
END SKILL;
