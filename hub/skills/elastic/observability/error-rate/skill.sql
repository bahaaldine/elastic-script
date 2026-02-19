CREATE SKILL error_rate
  VERSION '1.0.0'
  DESCRIPTION 'Calculate the error rate as a percentage of total logs. Use this to monitor application health, set up alerts, or compare error rates across different time periods or services.'
  AUTHOR 'elastic'
  TAGS ['observability', 'logs', 'errors', 'metrics', 'health']
  (
    index_pattern STRING DESCRIPTION 'Index pattern to search' DEFAULT 'logs-*',
    service STRING DESCRIPTION 'Filter by service name (optional)' DEFAULT NULL
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE total_result ARRAY;
  DECLARE error_result ARRAY;
  DECLARE total_count INT;
  DECLARE error_count INT;
  DECLARE rate FLOAT;
  
  IF service IS NOT NULL THEN
    SET total_result = ESQL_QUERY('FROM ' || index_pattern || ' | WHERE service == "' || service || '" | STATS count = COUNT(*)');
    SET error_result = ESQL_QUERY('FROM ' || index_pattern || ' | WHERE service == "' || service || '" AND level == "ERROR" | STATS count = COUNT(*)');
  ELSE
    SET total_result = ESQL_QUERY('FROM ' || index_pattern || ' | STATS count = COUNT(*)');
    SET error_result = ESQL_QUERY('FROM ' || index_pattern || ' | WHERE level == "ERROR" | STATS count = COUNT(*)');
  END IF;
  
  SET total_count = total_result[0]['count'];
  SET error_count = error_result[0]['count'];
  
  IF total_count > 0 THEN
    SET rate = ROUND((error_count * 100.0) / total_count, 2);
  ELSE
    SET rate = 0.0;
  END IF;
  
  RETURN {
    'total_logs': total_count,
    'error_count': error_count,
    'error_rate_percent': rate
  };
END SKILL;
