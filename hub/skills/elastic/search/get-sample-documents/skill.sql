CREATE SKILL get_sample_documents
  VERSION '1.0.0'
  DESCRIPTION 'Get sample documents from an index to understand its structure. Use this when exploring a new index, understanding field mappings, or seeing what data looks like.'
  AUTHOR 'elastic'
  TAGS ['search', 'exploration', 'data']
  (
    index_pattern STRING DESCRIPTION 'Index pattern to sample from',
    limit INT DESCRIPTION 'Number of sample documents' DEFAULT 5
  )
  RETURNS ARRAY
BEGIN
  DECLARE query STRING;
  DECLARE result ARRAY;
  
  SET query = 'FROM ' || index_pattern || ' | LIMIT ' || limit;
  SET result = ESQL_QUERY(query);
  
  RETURN result;
END SKILL;
