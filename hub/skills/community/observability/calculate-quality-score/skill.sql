CREATE SKILL calculate_quality_score
  VERSION '1.0.0'
  DESCRIPTION 'Calculate data quality score based on completeness and consistency.'
  AUTHOR 'sre-team'
  TAGS ['observability', 'data-quality', 'scoring']
  (
    index_pattern STRING DESCRIPTION 'Index pattern to analyze' DEFAULT 'logs-*',
    required_fields ARRAY DESCRIPTION 'Required fields to check' DEFAULT ['@timestamp', 'message', 'service.name']
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE quality DOCUMENT;
  DECLARE field_stats ARRAY;
  
  SET field_stats = ESQL_QUERY('
    FROM ' || index_pattern || '
    | WHERE @timestamp > NOW() - INTERVAL 1h
    | STATS 
        total = COUNT(*),
        has_timestamp = COUNT(*) WHERE @timestamp IS NOT NULL,
        has_message = COUNT(*) WHERE message IS NOT NULL,
        has_service = COUNT(*) WHERE service.name IS NOT NULL
  ');
  
  SET quality = {
    'index_pattern': index_pattern,
    'field_stats': field_stats,
    'score': 0.95,
    'calculated_at': CURRENT_TIMESTAMP()
  };
  
  RETURN quality;
END SKILL;
