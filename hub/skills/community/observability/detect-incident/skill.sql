CREATE SKILL detect_incident
  VERSION '1.0.0'
  DESCRIPTION 'Automatically detect incidents from logs, metrics, and alerts. Identifies patterns indicative of service degradation or failures.'
  AUTHOR 'sre-team'
  TAGS ['observability', 'incident', 'detection', 'sre']
  (
    severity_threshold STRING DESCRIPTION 'Minimum severity to detect: critical, high, medium, low' DEFAULT 'medium',
    time_range STRING DESCRIPTION 'Time range to analyze' DEFAULT '15m',
    index_pattern STRING DESCRIPTION 'Index pattern to search' DEFAULT 'logs-*'
  )
  RETURNS ARRAY
BEGIN
  DECLARE incidents ARRAY;
  DECLARE error_spike ARRAY;
  DECLARE latency_spike ARRAY;
  
  -- Check for error rate spikes
  SET error_spike = ESQL_QUERY('
    FROM ' || index_pattern || '
    | WHERE @timestamp > NOW() - INTERVAL ' || time_range || '
    | WHERE log.level == "ERROR" OR log.level == "FATAL"
    | STATS error_count = COUNT(*) BY service.name
    | WHERE error_count > 10
    | SORT error_count DESC
    | LIMIT 10
  ');
  
  -- Check for latency spikes in APM data
  SET latency_spike = ESQL_QUERY('
    FROM apm-*
    | WHERE @timestamp > NOW() - INTERVAL ' || time_range || '
    | STATS avg_duration = AVG(transaction.duration.us), p99 = PERCENTILE(transaction.duration.us, 99) BY service.name
    | WHERE p99 > 5000000
    | LIMIT 10
  ');
  
  -- Combine into incidents
  SET incidents = ARRAY_CONCAT(error_spike, latency_spike);
  
  RETURN incidents;
END SKILL;
