CREATE SKILL check_data_freshness
  VERSION '1.0.0'
  DESCRIPTION 'Check data freshness across indices to detect ingestion issues.'
  AUTHOR 'sre-team'
  TAGS ['observability', 'data-quality', 'freshness', 'monitoring']
  (
    index_patterns ARRAY DESCRIPTION 'Index patterns to check' DEFAULT ['logs-*', 'metrics-*'],
    max_age_minutes INTEGER DESCRIPTION 'Maximum acceptable age in minutes' DEFAULT 5
  )
  RETURNS ARRAY
BEGIN
  DECLARE freshness ARRAY;
  
  SET freshness = ESQL_QUERY('
    FROM logs-*,metrics-*
    | STATS 
        last_doc = MAX(@timestamp),
        doc_count = COUNT(*)
      BY _index
    | EVAL age_minutes = DATE_DIFF("minute", last_doc, NOW())
    | EVAL is_stale = age_minutes > ' || max_age_minutes || '
    | SORT age_minutes DESC
  ');
  
  RETURN freshness;
END SKILL;
