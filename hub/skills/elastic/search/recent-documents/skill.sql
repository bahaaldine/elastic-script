CREATE SKILL recent_documents
  VERSION '1.0.0'
  DESCRIPTION 'Get the most recently indexed documents from an index. Use this to see latest data, verify real-time ingestion, or check recent activity.'
  AUTHOR 'elastic'
  TAGS ['search', 'recent', 'real-time']
  (
    index_pattern STRING DESCRIPTION 'Index pattern to query' DEFAULT '*',
    limit INT DESCRIPTION 'Number of documents to return' DEFAULT 10
  )
  RETURNS ARRAY
BEGIN
  DECLARE query STRING;
  DECLARE result ARRAY;
  
  SET query = 'FROM ' || index_pattern || ' | SORT @timestamp DESC | LIMIT ' || limit;
  SET result = ESQL_QUERY(query);
  
  RETURN result;
END SKILL;
