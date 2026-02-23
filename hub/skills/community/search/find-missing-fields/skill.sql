CREATE SKILL find_missing_fields
  VERSION '1.0.0'
  DESCRIPTION 'Find documents with missing required fields for data quality analysis.'
  AUTHOR 'search-team'
  TAGS ['search', 'data-quality', 'validation']
  (
    index_pattern STRING DESCRIPTION 'Index pattern to check' DEFAULT 'content-*',
    required_fields ARRAY DESCRIPTION 'Fields that should be present'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE quality DOCUMENT;
  DECLARE missing_counts ARRAY;
  
  -- Check for missing fields
  SET missing_counts = ESQL_QUERY('
    FROM ' || index_pattern || '
    | STATS 
        total = COUNT(*),
        has_title = COUNT(*) WHERE title IS NOT NULL,
        has_content = COUNT(*) WHERE content IS NOT NULL
  ');
  
  SET quality = {
    'index_pattern': index_pattern,
    'field_stats': missing_counts,
    'checked_at': CURRENT_TIMESTAMP()
  };
  
  RETURN quality;
END SKILL;
