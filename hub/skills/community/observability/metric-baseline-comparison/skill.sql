CREATE SKILL metric_baseline_comparison
  VERSION '1.0.0'
  DESCRIPTION 'Compare metric values to a computed baseline using INLINE STATS. Computes overall AVG and STD_DEV inline, then evaluates each time bucket against the baseline to flag deviations. Single-query alternative to multi-query period-over-period comparisons.'
  AUTHOR 'observability-team'
  TAGS ['observability', 'baseline', 'comparison', 'inlinestats', 'advanced']
  (
    index_pattern STRING DESCRIPTION 'Index pattern to analyze' DEFAULT 'metrics-*',
    metric_field STRING DESCRIPTION 'Numeric field to compare against baseline' DEFAULT 'system.cpu.total.pct',
    time_range STRING DESCRIPTION 'Time range to analyze' DEFAULT '7d',
    bucket_interval STRING DESCRIPTION 'Time bucket interval' DEFAULT '1h'
  )
  RETURNS ARRAY
BEGIN
  DECLARE results ARRAY;

  -- First aggregate into time buckets, then use INLINE STATS to compute
  -- the overall baseline across all buckets. Each bucket then gets compared
  -- to the baseline in a single pipeline — no second query needed.
  SET results = ESQL_QUERY('
    FROM ' || index_pattern || '
    | WHERE @timestamp >= NOW() - ' || time_range || '
    | STATS
        avg_value = AVG(' || metric_field || '),
        max_value = MAX(' || metric_field || '),
        doc_count = COUNT(*)
      BY bucket = BUCKET(@timestamp, ' || bucket_interval || ')
    | INLINE STATS baseline_avg = AVG(avg_value), baseline_stddev = STD_DEV(avg_value)
    | EVAL
        deviation_pct = ROUND((avg_value - baseline_avg) * 100.0 / baseline_avg, 2),
        z_score = ROUND((avg_value - baseline_avg) / COALESCE(baseline_stddev, 1), 2),
        status = CASE(
          ABS(z_score) > 2, "ANOMALOUS",
          ABS(z_score) > 1, "WARNING",
          "NORMAL"
        )
    | KEEP bucket, avg_value, max_value, doc_count, baseline_avg, deviation_pct, z_score, status
    | SORT bucket
  ');

  RETURN results;
END SKILL;
