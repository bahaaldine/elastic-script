CREATE SKILL aggregate_by_field
  VERSION '1.0.0'
  DESCRIPTION 'Aggregate documents by a specific field and get counts. Use this for grouping, categorization, or getting distribution of values in your data.'
  AUTHOR 'elastic'
  TAGS ['search', 'aggregation', 'analytics']
  (
    index_pattern STRING DESCRIPTION 'Index pattern to aggregate',
    field STRING DESCRIPTION 'Field name to group by',
    limit INT DESCRIPTION 'Maximum number of buckets' DEFAULT 20
  )
  RETURNS ARRAY
BEGIN
  DECLARE query STRING;
  DECLARE result ARRAY;
  
  SET query = 'FROM ' || index_pattern || ' | STATS count = COUNT(*) BY ' || field || ' | SORT count DESC | LIMIT ' || limit;
  SET result = ESQL_QUERY(query);
  
  RETURN result;
END SKILL;
