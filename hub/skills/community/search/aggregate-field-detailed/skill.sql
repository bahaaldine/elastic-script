CREATE SKILL aggregate_field_detailed
  VERSION '1.0.0'
  DESCRIPTION 'Rich field aggregation with count, distinct count, and top sample values per group. Goes beyond simple COUNT(*) BY field to provide cardinality insights and representative values. Advanced counterpart to aggregate_by_field.'
  AUTHOR 'search-team'
  TAGS ['search', 'aggregation', 'analytics', 'cardinality', 'advanced']
  (
    index_pattern STRING DESCRIPTION 'Index pattern to aggregate' DEFAULT 'logs-*',
    group_field STRING DESCRIPTION 'Field to group by' DEFAULT 'service.name',
    value_field STRING DESCRIPTION 'Field to compute stats on' DEFAULT 'host.name',
    limit INT DESCRIPTION 'Maximum number of groups' DEFAULT 20
  )
  RETURNS ARRAY
BEGIN
  DECLARE results ARRAY;

  -- Richer aggregation: count, distinct count, and top sample values per group.
  -- TOP() returns the N most frequent values including repeats.
  SET results = ESQL_QUERY('
    FROM ' || index_pattern || '
    | STATS
        count = COUNT(*),
        distinct_count = COUNT_DISTINCT(' || value_field || '),
        top_values = TOP(' || value_field || ', 3, "desc")
      BY ' || group_field || '
    | SORT count DESC
    | LIMIT ' || limit || '
  ');

  RETURN results;
END SKILL;
