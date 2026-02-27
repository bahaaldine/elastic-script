CREATE SKILL field_cardinality_report
  VERSION '1.0.0'
  DESCRIPTION 'Report field cardinality, sample values, and document counts per group. Uses COUNT_DISTINCT for approximate cardinality and SAMPLE for representative values. Useful for data exploration, quality assessment, and understanding field distributions.'
  AUTHOR 'search-team'
  TAGS ['search', 'analytics', 'cardinality', 'data-quality', 'exploration', 'advanced']
  (
    index_pattern STRING DESCRIPTION 'Index pattern to analyze' DEFAULT 'logs-*',
    group_field STRING DESCRIPTION 'Field to report cardinality for' DEFAULT 'service.name',
    sample_field STRING DESCRIPTION 'Field to sample values from' DEFAULT 'message',
    limit INT DESCRIPTION 'Max groups to return' DEFAULT 20
  )
  RETURNS ARRAY
BEGIN
  DECLARE results ARRAY;

  -- COUNT_DISTINCT uses HyperLogLog++ for approximate but fast cardinality.
  -- SAMPLE collects representative values without pulling all data.
  SET results = ESQL_QUERY('
    FROM ' || index_pattern || '
    | STATS
        doc_count = COUNT(*),
        unique_values = COUNT_DISTINCT(' || sample_field || '),
        samples = SAMPLE(' || sample_field || ', 3)
      BY ' || group_field || '
    | SORT doc_count DESC
    | LIMIT ' || limit || '
  ');

  RETURN results;
END SKILL;
