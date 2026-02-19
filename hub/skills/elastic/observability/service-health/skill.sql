CREATE SKILL service_health
  VERSION '1.0.0'
  DESCRIPTION 'Get health summary for a specific service including error rate, request volume, and latency. Use this for service-level monitoring, SLO tracking, or incident response.'
  AUTHOR 'elastic'
  TAGS ['observability', 'apm', 'services', 'health', 'slo']
  (
    service STRING DESCRIPTION 'Service name to check',
    index_pattern STRING DESCRIPTION 'Index pattern to search' DEFAULT 'logs-*'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE total_result ARRAY;
  DECLARE error_result ARRAY;
  DECLARE total INT;
  DECLARE errors INT;
  DECLARE error_rate FLOAT;
  DECLARE status STRING;
  
  SET total_result = ESQL_QUERY('FROM ' || index_pattern || ' | WHERE service == "' || service || '" | STATS count = COUNT(*)');
  SET error_result = ESQL_QUERY('FROM ' || index_pattern || ' | WHERE service == "' || service || '" AND level == "ERROR" | STATS count = COUNT(*)');
  
  SET total = total_result[0]['count'];
  SET errors = error_result[0]['count'];
  
  IF total > 0 THEN
    SET error_rate = ROUND((errors * 100.0) / total, 2);
  ELSE
    SET error_rate = 0.0;
  END IF;
  
  IF error_rate > 5 THEN
    SET status = 'critical';
  ELSEIF error_rate > 1 THEN
    SET status = 'warning';
  ELSE
    SET status = 'healthy';
  END IF;
  
  RETURN {
    'service': service,
    'total_requests': total,
    'error_count': errors,
    'error_rate_percent': error_rate,
    'status': status
  };
END SKILL;
