CREATE SKILL detect_log_patterns
  VERSION '1.0.0'
  DESCRIPTION 'Detect common log patterns and categorize log messages.'
  AUTHOR 'sre-team'
  TAGS ['observability', 'logs', 'patterns', 'categorization']
  (
    index_pattern STRING DESCRIPTION 'Log index pattern' DEFAULT 'logs-*',
    time_range STRING DESCRIPTION 'Time range to analyze' DEFAULT '1h',
    min_count INTEGER DESCRIPTION 'Minimum occurrences' DEFAULT 10
  )
  RETURNS ARRAY
BEGIN
  DECLARE patterns ARRAY;
  
  SET patterns = ESQL_QUERY('
    FROM ' || index_pattern || '
    | WHERE @timestamp > NOW() - INTERVAL ' || time_range || '
    | STATS 
        count = COUNT(*),
        services = VALUES(service.name),
        first_seen = MIN(@timestamp),
        last_seen = MAX(@timestamp)
      BY log.level, error.type
    | WHERE count >= ' || min_count || '
    | SORT count DESC
    | LIMIT 50
  ');
  
  RETURN patterns;
END SKILL;
