CREATE SKILL search_with_filters
  VERSION '1.0.0'
  DESCRIPTION 'Perform search with dynamic filtering based on facets.'
  AUTHOR 'search-team'
  TAGS ['search', 'filters', 'faceted']
  (
    query STRING DESCRIPTION 'Search query',
    index_pattern STRING DESCRIPTION 'Index pattern' DEFAULT 'content-*',
    filters DOCUMENT DESCRIPTION 'Filter conditions' DEFAULT NULL
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE search_result DOCUMENT;
  DECLARE results ARRAY;
  DECLARE facets ARRAY;
  
  -- Perform search
  SET results = ESQL_QUERY('
    FROM ' || index_pattern || '
    | WHERE content LIKE "*' || query || '*"
    | KEEP _id, title, content, category, @timestamp
    | LIMIT 20
  ');
  
  -- Get facets
  SET facets = ESQL_QUERY('
    FROM ' || index_pattern || '
    | STATS count = COUNT(*) BY category
    | SORT count DESC
  ');
  
  SET search_result = {
    'query': query,
    'results': results,
    'facets': facets,
    'total': ARRAY_LENGTH(results)
  };
  
  RETURN search_result;
END SKILL;
