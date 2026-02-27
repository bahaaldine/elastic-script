CREATE SKILL detect_change_points
  VERSION '1.0.0'
  DESCRIPTION 'Detect spikes, dips, step changes, and trend changes in a numeric metric using the CHANGE_POINT command. Returns the change type and statistical p-value (lower = more significant). Requires at least 22 data points. Advanced counterpart to detect_anomalies.'
  AUTHOR 'observability-team'
  TAGS ['observability', 'anomaly-detection', 'change-point', 'timeseries', 'advanced']
  (
    index_pattern STRING DESCRIPTION 'Index pattern to analyze' DEFAULT 'metrics-*',
    metric_field STRING DESCRIPTION 'Numeric field to detect changes in' DEFAULT 'system.cpu.total.pct',
    time_bucket STRING DESCRIPTION 'Time bucket size for aggregation (e.g. 1h, 15m, 1d)' DEFAULT '1h',
    time_range STRING DESCRIPTION 'How far back to look' DEFAULT '7d'
  )
  RETURNS ARRAY
BEGIN
  DECLARE results ARRAY;

  -- Aggregate into time buckets, then run CHANGE_POINT on the aggregated values.
  -- CHANGE_POINT needs at least 22 values to work; 1h buckets over 7d = 168 points.
  SET results = ESQL_QUERY('
    FROM ' || index_pattern || '
    | WHERE @timestamp >= NOW() - ' || time_range || '
    | STATS metric_value = AVG(' || metric_field || ') BY bucket = BUCKET(@timestamp, ' || time_bucket || ')
    | CHANGE_POINT metric_value ON bucket
    | KEEP bucket, metric_value, type, pvalue
    | SORT bucket
  ');

  RETURN results;
END SKILL;
