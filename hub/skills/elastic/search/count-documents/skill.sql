CREATE SKILL count_documents
  VERSION '1.0.0'
  DESCRIPTION 'Count documents in an index, optionally filtered by a condition. Use this to get quick counts, verify data ingestion, or check index sizes.'
  AUTHOR 'elastic'
  TAGS ['search', 'count', 'aggregation']
  (
    index_pattern STRING DESCRIPTION 'Index pattern to count' DEFAULT '*'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE query STRING;
  DECLARE count_result ARRAY;
  DECLARE result INT;
  
  SET query = 'FROM ' || index_pattern || ' | STATS count = COUNT(*)';
  SET count_result = ESQL_QUERY(query);
  SET result = count_result[0]['count'];
  
  RETURN {
    'index_pattern': index_pattern,
    'count': result
  };
END SKILL;
