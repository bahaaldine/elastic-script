CREATE SKILL get_error_context
  VERSION '1.0.0'
  DESCRIPTION 'Get logs before and after an error for context'
  AUTHOR 'elastic'
  TAGS ['observability', 'logs', 'debugging']
  (timestamp STRING DESCRIPTION 'Error timestamp', service STRING DESCRIPTION 'Service name', window_minutes INT DESCRIPTION 'Minutes before/after' DEFAULT 5)
  RETURNS ARRAY
BEGIN
  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM logs-* | WHERE service == "' || service || '" | SORT @timestamp | LIMIT 50');
  RETURN result;
END SKILL;
