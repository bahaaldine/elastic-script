CREATE SKILL semantic_search
  VERSION '1.0.0'
  DESCRIPTION 'Semantic/vector search using embeddings'
  AUTHOR 'elastic'
  TAGS ['search', 'semantic', 'vector']
  (query STRING DESCRIPTION 'Natural language query', index_pattern STRING DESCRIPTION 'Index with vectors' DEFAULT '*', limit INT DESCRIPTION 'Max results' DEFAULT 10)
  RETURNS ARRAY
BEGIN
  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM ' || index_pattern || ' | LIMIT ' || limit);
  RETURN result;
END SKILL;
