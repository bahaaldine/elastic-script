CREATE SKILL search_documents_cmd
  VERSION '1.0.0'
  DESCRIPTION 'Search documents using the first-class SEARCH command with Elasticsearch Query DSL.'
  AUTHOR 'moltler'
  TAGS ['elasticsearch', 'search', 'first-class', 'command', 'query']
  (
    index_name STRING DESCRIPTION 'Index pattern to search',
    query DOCUMENT DESCRIPTION 'Elasticsearch Query DSL document'
  )
  RETURNS ARRAY
BEGIN
  -- Use first-class SEARCH command
  DECLARE results ARRAY;
  
  SET results = SEARCH index_name QUERY query;
  
  RETURN results;
END SKILL;
