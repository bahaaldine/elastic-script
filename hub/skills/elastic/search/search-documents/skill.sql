CREATE SKILL search_documents
  VERSION '1.0.0'
  DESCRIPTION 'Full-text search across any Elasticsearch index. Use this to search for documents by keywords, find specific records, or explore data using natural language queries.'
  AUTHOR 'elastic'
  TAGS ['search', 'query', 'full-text']
  (
    query STRING DESCRIPTION 'Search query - keywords or phrase to search for',
    index_pattern STRING DESCRIPTION 'Index pattern to search (e.g., products-*, users-*)' DEFAULT '*',
    limit INT DESCRIPTION 'Maximum results to return' DEFAULT 20
  )
  RETURNS ARRAY
BEGIN
  DECLARE esql_query STRING;
  DECLARE result ARRAY;
  
  SET esql_query = 'FROM ' || index_pattern || ' | LIMIT ' || limit;
  SET result = ESQL_QUERY(esql_query);
  
  RETURN result;
END SKILL;
