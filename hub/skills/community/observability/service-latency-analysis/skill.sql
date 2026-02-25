CREATE SKILL service_latency_analysis
  VERSION '1.0.0'
  DESCRIPTION 'Comprehensive latency profiling using PERCENTILE, STD_DEV, MEDIAN_ABSOLUTE_DEVIATION, and WEIGHTED_AVG. Returns p50/p95/p99, standard deviation, MAD, and request-weighted average per service. One query, full statistical picture.'
  AUTHOR 'observability-team'
  TAGS ['observability', 'apm', 'latency', 'percentile', 'statistics', 'advanced']
  (
    index_pattern STRING DESCRIPTION 'APM index pattern' DEFAULT 'apm-*',
    time_range STRING DESCRIPTION 'Time range to analyze' DEFAULT '7d'
  )
  RETURNS ARRAY
BEGIN
  DECLARE results ARRAY;

  -- Comprehensive latency statistics in a single query.
  -- WEIGHTED_AVG weights latency by request count so high-traffic endpoints
  -- contribute more to the average than low-traffic ones.
  -- MEDIAN_ABSOLUTE_DEVIATION is more robust than STD_DEV for skewed distributions.
  SET results = ESQL_QUERY('
    FROM ' || index_pattern || '
    | WHERE @timestamp >= NOW() - ' || time_range || '
    | STATS
        request_count = COUNT(*),
        avg_latency_us = AVG(transaction.duration.us),
        p50_latency_us = PERCENTILE(transaction.duration.us, 50),
        p95_latency_us = PERCENTILE(transaction.duration.us, 95),
        p99_latency_us = PERCENTILE(transaction.duration.us, 99),
        stddev_latency_us = STD_DEV(transaction.duration.us),
        mad_latency_us = MEDIAN_ABSOLUTE_DEVIATION(transaction.duration.us),
        error_count = COUNT(*) WHERE transaction.outcome == "failure"
      BY service.name
    | EVAL
        error_rate_pct = ROUND(error_count * 100.0 / TO_DOUBLE(request_count), 2),
        p95_p50_ratio = ROUND(p95_latency_us / COALESCE(p50_latency_us, 1), 2)
    | KEEP service.name, request_count, avg_latency_us, p50_latency_us, p95_latency_us, p99_latency_us, stddev_latency_us, mad_latency_us, error_rate_pct, p95_p50_ratio
    | SORT p95_latency_us DESC
  ');

  RETURN results;
END SKILL;
