CREATE SKILL get_field_stats
  VERSION '1.0.0'
  DESCRIPTION 'Get statistics (min, max, avg, sum) for a numeric field. Use this for data analysis, understanding value distributions, or calculating metrics.'
  AUTHOR 'elastic'
  TAGS ['search', 'statistics', 'analytics']
  (
    index_pattern STRING DESCRIPTION 'Index pattern to analyze',
    field STRING DESCRIPTION 'Numeric field to calculate stats for'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE query STRING;
  DECLARE result ARRAY;
  
  SET query = 'FROM ' || index_pattern || ' | STATS min_val = MIN(' || field || '), max_val = MAX(' || field || '), avg_val = AVG(' || field || '), sum_val = SUM(' || field || '), count = COUNT(*)';
  SET result = ESQL_QUERY(query);
  
  RETURN result[0];
END SKILL;
