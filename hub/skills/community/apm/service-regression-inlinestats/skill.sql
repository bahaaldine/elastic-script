CREATE SKILL service_regression_inlinestats
  VERSION '1.0.0'
  DESCRIPTION 'Detect service performance regressions in a single query using INLINE STATS. Computes baseline latency inline, then calculates per-bucket deviation to flag regressions. No need for two separate queries. Advanced counterpart to detect_performance_regression.'
  AUTHOR 'apm-team'
  TAGS ['apm', 'regression', 'performance', 'inlinestats', 'advanced']
  (
    index_pattern STRING DESCRIPTION 'APM index pattern' DEFAULT 'apm-*',
    service_name STRING DESCRIPTION 'Service to analyze' DEFAULT 'api-gateway',
    threshold_pct DOUBLE DESCRIPTION 'Percentage above baseline to flag as regression' DEFAULT 20.0
  )
  RETURNS ARRAY
BEGIN
  DECLARE results ARRAY;

  -- INLINE STATS computes the overall baseline AVG and STD_DEV while preserving
  -- every row, so we can bucket by hour and compare each bucket to the baseline
  -- in a single query pipeline.
  SET results = ESQL_QUERY('
    FROM ' || index_pattern || '
    | WHERE @timestamp >= NOW() - 7d
    | WHERE service.name == "' || service_name || '"
    | STATS
        avg_latency = AVG(transaction.duration.us),
        p95_latency = PERCENTILE(transaction.duration.us, 95),
        request_count = COUNT(*)
      BY bucket = BUCKET(@timestamp, 1h)
    | INLINE STATS baseline_avg = AVG(avg_latency), baseline_stddev = STD_DEV(avg_latency)
    | EVAL
        deviation_pct = ROUND((avg_latency - baseline_avg) * 100.0 / baseline_avg, 2),
        z_score = ROUND((avg_latency - baseline_avg) / COALESCE(baseline_stddev, 1), 2),
        is_regression = CASE(avg_latency > baseline_avg * (1 + ' || threshold_pct || ' / 100.0), "YES", "NO")
    | KEEP bucket, avg_latency, p95_latency, request_count, baseline_avg, deviation_pct, z_score, is_regression
    | SORT bucket DESC
  ');

  RETURN results;
END SKILL;
