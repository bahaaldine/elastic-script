CREATE SKILL get_trace
  VERSION '1.0.0'
  DESCRIPTION 'Get full distributed trace by trace ID'
  AUTHOR 'elastic'
  TAGS ['apm,traces,distributed']
  (trace_id STRING DESCRIPTION 'Trace ID to retrieve')
  RETURNS ARRAY
BEGIN
  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM logs-* | WHERE trace_id == "' || trace_id || '" | SORT @timestamp');
  RETURN result;
END SKILL;
