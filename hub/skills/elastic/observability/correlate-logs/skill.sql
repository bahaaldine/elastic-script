CREATE SKILL correlate_logs
  VERSION '1.0.0'
  DESCRIPTION 'Find correlated log events across services'
  AUTHOR 'elastic'
  TAGS ['observability,logs,correlation']
  (trace_id STRING DESCRIPTION 'Trace ID to correlate', index_pattern STRING DESCRIPTION 'Index pattern' DEFAULT 'logs-*')
  RETURNS ARRAY
BEGIN
  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM ' || index_pattern || ' | WHERE trace_id == "' || trace_id || '" | SORT @timestamp');
  RETURN result;
END SKILL;
