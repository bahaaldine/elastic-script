CREATE SKILL multi_field_search
  VERSION '1.0.0'
  DESCRIPTION 'Search across multiple fields simultaneously'
  AUTHOR 'elastic'
  TAGS ['search,multi-field,query']
  (query STRING DESCRIPTION 'Search query', index_pattern STRING DESCRIPTION 'Index pattern' DEFAULT '*', fields STRING DESCRIPTION 'Comma-separated fields to search' DEFAULT 'message,title,description')
  RETURNS ARRAY
BEGIN
  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM ' || index_pattern || ' | WHERE message LIKE "*' || query || '*" | LIMIT 20');
  RETURN result;
END SKILL;
