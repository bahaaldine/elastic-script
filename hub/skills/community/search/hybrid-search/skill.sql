CREATE SKILL hybrid_search
  VERSION '1.0.0'
  DESCRIPTION 'Perform hybrid search combining BM25 text search with vector similarity.'
  AUTHOR 'search-team'
  TAGS ['search', 'hybrid', 'semantic', 'vector']
  (
    query STRING DESCRIPTION 'Search query',
    index_pattern STRING DESCRIPTION 'Index pattern to search' DEFAULT 'content-*',
    vector_field STRING DESCRIPTION 'Vector field name' DEFAULT 'embedding',
    text_field STRING DESCRIPTION 'Text field name' DEFAULT 'content',
    k INTEGER DESCRIPTION 'Number of results' DEFAULT 10
  )
  RETURNS ARRAY
BEGIN
  DECLARE results ARRAY;
  
  -- Perform text search
  SET results = ESQL_QUERY('
    FROM ' || index_pattern || '
    | WHERE ' || text_field || ' LIKE "*' || query || '*"
    | KEEP _id, title, ' || text_field || ', @timestamp
    | LIMIT ' || k || '
  ');
  
  RETURN results;
END SKILL;
