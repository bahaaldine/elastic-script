CREATE SKILL statistical_outlier_detection
  VERSION '1.0.0'
  DESCRIPTION 'Detect statistical outliers using z-scores computed via INLINE STATS. Calculates group-level AVG and STD_DEV inline, then flags rows whose values deviate beyond a configurable threshold. The standard ESQL pattern for anomaly detection without window functions.'
  AUTHOR 'ml-team'
  TAGS ['ml', 'anomaly-detection', 'outlier', 'z-score', 'inlinestats', 'advanced']
  (
    index_pattern STRING DESCRIPTION 'Index pattern to analyze' DEFAULT 'metrics-*',
    metric_field STRING DESCRIPTION 'Numeric field to check for outliers' DEFAULT 'system.cpu.total.pct',
    group_field STRING DESCRIPTION 'Field to group by for per-group statistics' DEFAULT 'host.name',
    threshold DOUBLE DESCRIPTION 'Z-score threshold for outlier detection (default: 2.0)' DEFAULT 2.0,
    limit INT DESCRIPTION 'Max outliers to return' DEFAULT 50
  )
  RETURNS ARRAY
BEGIN
  DECLARE results ARRAY;

  -- INLINE STATS computes per-group AVG and STD_DEV while preserving every row.
  -- Then EVAL computes the z-score for each row, and WHERE filters to outliers.
  -- COALESCE(group_stddev, 1) prevents division by zero when a group has no variance.
  SET results = ESQL_QUERY('
    FROM ' || index_pattern || '
    | INLINE STATS group_avg = AVG(' || metric_field || '), group_stddev = STD_DEV(' || metric_field || ') BY ' || group_field || '
    | EVAL z_score = ROUND((' || metric_field || ' - group_avg) / COALESCE(group_stddev, 1), 2)
    | WHERE ABS(z_score) > ' || threshold || '
    | KEEP @timestamp, ' || group_field || ', ' || metric_field || ', group_avg, group_stddev, z_score
    | SORT z_score DESC
    | LIMIT ' || limit || '
  ');

  RETURN results;
END SKILL;
