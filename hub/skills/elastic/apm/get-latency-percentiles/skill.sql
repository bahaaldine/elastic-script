CREATE SKILL get_latency_percentiles
  VERSION '1.0.0'
  DESCRIPTION 'Get latency percentiles (p50, p95, p99) for a service'
  AUTHOR 'elastic'
  TAGS ['apm,latency,percentiles']
  (service STRING DESCRIPTION 'Service name to analyze', index_pattern STRING DESCRIPTION 'Index pattern' DEFAULT 'logs-*')
  RETURNS ARRAY
BEGIN
  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM ' || index_pattern || ' | WHERE service == "' || service || '" | STATS p50 = PERCENTILE(duration_ms, 50), p95 = PERCENTILE(duration_ms, 95), p99 = PERCENTILE(duration_ms, 99)');
  RETURN result;
END SKILL;
