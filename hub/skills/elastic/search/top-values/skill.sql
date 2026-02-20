CREATE SKILL top_values
  VERSION '1.0.0'
  DESCRIPTION 'Get top N most common values for a field'
  AUTHOR 'elastic'
  TAGS ['search', 'aggregation', 'analytics']
  (index_pattern STRING DESCRIPTION 'Index to analyze', field STRING DESCRIPTION 'Field to get top values for', limit INT DESCRIPTION 'Number of top values' DEFAULT 10)
  RETURNS ARRAY
BEGIN
  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM ' || index_pattern || ' | STATS count = COUNT(*) BY ' || field || ' | SORT count DESC | LIMIT ' || limit);
  RETURN result;
END SKILL;
