CREATE SKILL detect_duplicates
  VERSION '1.0.0'
  DESCRIPTION 'Detect duplicate or near-duplicate documents based on content similarity.'
  AUTHOR 'search-team'
  TAGS ['search', 'deduplication', 'quality']
  (
    index_pattern STRING DESCRIPTION 'Index pattern to check' DEFAULT 'content-*',
    field STRING DESCRIPTION 'Field to check for duplicates' DEFAULT 'title',
    threshold DOUBLE DESCRIPTION 'Similarity threshold' DEFAULT 0.9
  )
  RETURNS ARRAY
BEGIN
  DECLARE duplicates ARRAY;
  
  -- Find documents with identical field values
  SET duplicates = ESQL_QUERY('
    FROM ' || index_pattern || '
    | STATS doc_count = COUNT(*), docs = VALUES(_id) BY ' || field || '
    | WHERE doc_count > 1
    | SORT doc_count DESC
    | LIMIT 50
  ');
  
  RETURN duplicates;
END SKILL;
