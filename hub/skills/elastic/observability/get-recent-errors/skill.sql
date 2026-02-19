CREATE SKILL get_recent_errors
  VERSION '1.0.0'
  DESCRIPTION 'Get recent error logs with full details including message, timestamp, and service. Use this when investigating issues, debugging production problems, or responding to alerts about errors.'
  AUTHOR 'elastic'
  TAGS ['observability', 'logs', 'errors', 'debugging']
  (
    index_pattern STRING DESCRIPTION 'Index pattern to search' DEFAULT 'logs-*',
    limit INT DESCRIPTION 'Maximum number of errors to return' DEFAULT 20,
    service STRING DESCRIPTION 'Filter by service name (optional)' DEFAULT NULL
  )
  RETURNS ARRAY
BEGIN
  DECLARE query STRING;
  DECLARE result ARRAY;
  
  IF service IS NOT NULL THEN
    SET query = 'FROM ' || index_pattern || ' | WHERE level == "ERROR" AND service == "' || service || '" | SORT @timestamp DESC | LIMIT ' || limit;
  ELSE
    SET query = 'FROM ' || index_pattern || ' | WHERE level == "ERROR" | SORT @timestamp DESC | LIMIT ' || limit;
  END IF;
  
  SET result = ESQL_QUERY(query);
  RETURN result;
END SKILL;
