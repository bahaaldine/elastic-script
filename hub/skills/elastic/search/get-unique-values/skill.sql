CREATE SKILL get_unique_values
  VERSION '1.0.0'
  DESCRIPTION 'Get unique values for a field in an index. Use this to understand possible values, find categories, or check data cardinality.'
  AUTHOR 'elastic'
  TAGS ['search', 'cardinality', 'exploration']
  (
    index_pattern STRING DESCRIPTION 'Index pattern to analyze',
    field STRING DESCRIPTION 'Field to get unique values for',
    limit INT DESCRIPTION 'Maximum unique values to return' DEFAULT 50
  )
  RETURNS ARRAY
BEGIN
  DECLARE query STRING;
  DECLARE result ARRAY;
  
  SET query = 'FROM ' || index_pattern || ' | STATS count = COUNT(*) BY ' || field || ' | SORT count DESC | LIMIT ' || limit;
  SET result = ESQL_QUERY(query);
  
  RETURN result;
END SKILL;
