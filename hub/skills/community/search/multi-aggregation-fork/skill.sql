CREATE SKILL multi_aggregation_fork
  VERSION '1.0.0'
  DESCRIPTION 'Run multiple different aggregations on the same data in a single query using FORK. Returns results from all branches tagged by _fork column. Useful for dashboards that need several views of the same dataset without multiple round-trips.'
  AUTHOR 'search-team'
  TAGS ['search', 'aggregation', 'fork', 'analytics', 'advanced']
  (
    index_pattern STRING DESCRIPTION 'Index pattern to analyze' DEFAULT 'logs-*',
    group_field STRING DESCRIPTION 'Primary field to group by' DEFAULT 'service.name'
  )
  RETURNS ARRAY
BEGIN
  DECLARE results ARRAY;

  -- FORK creates multiple execution branches on the same input data.
  -- Each branch can run different aggregations or filters.
  -- The _fork column identifies which branch each row came from.
  SET results = ESQL_QUERY('
    FROM ' || index_pattern || '
    | FORK
      ( STATS count = COUNT(*) BY ' || group_field || ' | SORT count DESC | LIMIT 10 )
      ( STATS count = COUNT(*) BY bucket = BUCKET(@timestamp, 1d) | SORT bucket )
      ( STATS count = COUNT(*) BY log.level | SORT count DESC )
    | KEEP _fork, ' || group_field || ', bucket, log.level, count
  ');

  RETURN results;
END SKILL;
