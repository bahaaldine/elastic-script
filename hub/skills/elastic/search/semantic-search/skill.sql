CREATE SKILL semantic_search
  VERSION '1.1.0'
  DESCRIPTION 'Full-text search using MATCH() for relevant results. Works on text and keyword fields. For semantic_text fields, MATCH() automatically performs semantic search.'
  AUTHOR 'elastic'
  TAGS ['search', 'semantic', 'fulltext', 'match']
  (query STRING DESCRIPTION 'Search query text', index_pattern STRING DESCRIPTION 'Index to search' DEFAULT 'content-*', field STRING DESCRIPTION 'Field to search (text, keyword, or semantic_text)' DEFAULT 'content', limit INT DESCRIPTION 'Max results' DEFAULT 10)
  RETURNS ARRAY
BEGIN
  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM ' || index_pattern || ' | WHERE MATCH(' || field || ', "' || query || '") | KEEP ' || field || ', @timestamp | LIMIT ' || limit);
  RETURN result;
END SKILL;
