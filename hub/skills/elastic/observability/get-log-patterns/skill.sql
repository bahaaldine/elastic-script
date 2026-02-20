CREATE SKILL get_log_patterns
  VERSION '1.0.0'
  DESCRIPTION 'Identify common log patterns using ML categorization'
  AUTHOR 'elastic'
  TAGS ['observability', 'logs', 'patterns']
  (index_pattern STRING DESCRIPTION 'Index to analyze' DEFAULT 'logs-*', limit INT DESCRIPTION 'Max patterns' DEFAULT 20)
  RETURNS ARRAY
BEGIN
  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM ' || index_pattern || ' | STATS count = COUNT(*) BY message | SORT count DESC | LIMIT ' || limit);
  RETURN result;
END SKILL;
