CREATE SKILL slow_requests
  VERSION '1.0.0'
  DESCRIPTION 'Find slow HTTP requests or API calls. Use this to identify performance bottlenecks, slow endpoints, or services that need optimization.'
  AUTHOR 'elastic'
  TAGS ['observability', 'apm', 'latency', 'performance']
  (
    index_pattern STRING DESCRIPTION 'Index pattern to search' DEFAULT 'logs-*',
    threshold_ms INT DESCRIPTION 'Duration threshold in milliseconds' DEFAULT 1000,
    limit INT DESCRIPTION 'Maximum results' DEFAULT 20
  )
  RETURNS ARRAY
BEGIN
  DECLARE query STRING;
  DECLARE result ARRAY;
  
  SET query = 'FROM ' || index_pattern || ' | WHERE duration_ms > ' || threshold_ms || ' | SORT duration_ms DESC | LIMIT ' || limit;
  SET result = ESQL_QUERY(query);
  
  RETURN result;
END SKILL;
