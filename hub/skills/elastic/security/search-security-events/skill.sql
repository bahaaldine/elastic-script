CREATE SKILL search_security_events
  VERSION '1.0.0'
  DESCRIPTION 'Search security events with full-text query'
  AUTHOR 'elastic'
  TAGS ['security', 'search', 'events']
  (query STRING DESCRIPTION 'Search query', index_pattern STRING DESCRIPTION 'Index pattern' DEFAULT 'security-*', limit INT DESCRIPTION 'Max results' DEFAULT 50)
  RETURNS ARRAY
BEGIN
  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM ' || index_pattern || ' | WHERE message LIKE "*' || query || '*" | SORT @timestamp DESC | LIMIT ' || limit);
  RETURN result;
END SKILL;
