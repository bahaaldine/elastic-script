CREATE SKILL top_error_messages
  VERSION '1.0.0'
  DESCRIPTION 'Get the most frequent error messages. Use this to identify recurring issues, prioritize bug fixes, or understand the most impactful errors affecting your system.'
  AUTHOR 'elastic'
  TAGS ['observability', 'logs', 'errors', 'analysis']
  (
    index_pattern STRING DESCRIPTION 'Index pattern to search' DEFAULT 'logs-*',
    limit INT DESCRIPTION 'Number of top errors to return' DEFAULT 10
  )
  RETURNS ARRAY
BEGIN
  DECLARE query STRING;
  DECLARE result ARRAY;
  
  SET query = 'FROM ' || index_pattern || ' | WHERE level == "ERROR" | STATS count = COUNT(*) BY message | SORT count DESC | LIMIT ' || limit;
  SET result = ESQL_QUERY(query);
  
  RETURN result;
END SKILL;
