CREATE SKILL get_service_health
  VERSION '1.0.0'
  DESCRIPTION 'Get health metrics for a specific service including error rate and latency'
  AUTHOR 'elastic'
  TAGS ['apm,health,services']
  (service STRING DESCRIPTION 'Service name to check')
  RETURNS DOCUMENT
BEGIN
  DECLARE total_result ARRAY;
  DECLARE error_result ARRAY;
  DECLARE total INT;
  DECLARE errors INT;
  SET total_result = ESQL_QUERY('FROM logs-* | WHERE service == "' || service || '" | STATS count = COUNT(*)');
  SET error_result = ESQL_QUERY('FROM logs-* | WHERE service == "' || service || '" AND level == "ERROR" | STATS count = COUNT(*)');
  SET total = total_result[0]['count'];
  SET errors = error_result[0]['count'];
  RETURN {
    'service': service,
    'total_requests': total,
    'errors': errors,
    'error_rate': ROUND((errors * 100.0) / CASE WHEN total > 0 THEN total ELSE 1 END, 2)
  };
END SKILL;
