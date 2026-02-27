CREATE SKILL time_bucket_analysis
  VERSION '1.0.0'
  DESCRIPTION 'Aggregate documents into time buckets using BUCKET(), the proper ESQL grouping function. More flexible than manual DATE_TRUNC — supports both fixed intervals and target bucket counts. Advanced counterpart to date_histogram.'
  AUTHOR 'search-team'
  TAGS ['search', 'histogram', 'timeseries', 'bucket', 'advanced']
  (
    index_pattern STRING DESCRIPTION 'Index to aggregate' DEFAULT 'logs-*',
    interval STRING DESCRIPTION 'Bucket interval (e.g. 1h, 15m, 1d)' DEFAULT '1d',
    date_field STRING DESCRIPTION 'Date field to bucket on' DEFAULT '@timestamp'
  )
  RETURNS ARRAY
BEGIN
  DECLARE results ARRAY;

  -- BUCKET() is the idiomatic ESQL grouping function for time series.
  -- It handles edge cases better than manual DATE_TRUNC and supports
  -- both fixed intervals and target-count-based auto-bucketing.
  SET results = ESQL_QUERY('
    FROM ' || index_pattern || '
    | STATS count = COUNT(*) BY bucket = BUCKET(' || date_field || ', ' || interval || ')
    | SORT bucket
  ');

  RETURN results;
END SKILL;
