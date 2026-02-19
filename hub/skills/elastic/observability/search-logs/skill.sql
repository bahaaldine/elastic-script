CREATE SKILL search_logs
  VERSION '1.0.0'
  DESCRIPTION 'Search logs by keyword or phrase. Use this for full-text search across log messages to find specific events, trace requests, or investigate issues mentioned in alerts.'
  AUTHOR 'elastic'
  TAGS ['observability', 'logs', 'search', 'debugging']
  (
    query STRING DESCRIPTION 'Search query - keyword or phrase to find in logs',
    index_pattern STRING DESCRIPTION 'Index pattern to search' DEFAULT 'logs-*',
    limit INT DESCRIPTION 'Maximum results to return' DEFAULT 50
  )
  RETURNS ARRAY
BEGIN
  DECLARE esql_query STRING;
  DECLARE result ARRAY;
  
  SET esql_query = 'FROM ' || index_pattern || ' | WHERE message LIKE "*' || query || '*" | SORT @timestamp DESC | LIMIT ' || limit;
  SET result = ESQL_QUERY(esql_query);
  
  RETURN result;
END SKILL;
