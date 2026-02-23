CREATE SKILL detect_performance_regression
  VERSION '1.0.0'
  DESCRIPTION 'Detect performance regressions by comparing current metrics to baseline.'
  AUTHOR 'apm-team'
  TAGS ['apm', 'regression', 'performance', 'detection']
  (
    service_name STRING DESCRIPTION 'Service name to check',
    threshold_pct DOUBLE DESCRIPTION 'Regression threshold percentage' DEFAULT 20.0
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE regression DOCUMENT;
  DECLARE current_metrics ARRAY;
  DECLARE baseline_metrics ARRAY;
  
  -- Get current metrics (last 1 hour)
  SET current_metrics = ESQL_QUERY('
    FROM apm-*
    | WHERE @timestamp > NOW() - INTERVAL 1h
    | WHERE service.name == "' || service_name || '"
    | STATS 
        avg_latency = AVG(transaction.duration.us),
        p99_latency = PERCENTILE(transaction.duration.us, 99)
  ');
  
  -- Get baseline metrics (previous 24 hours)
  SET baseline_metrics = ESQL_QUERY('
    FROM apm-*
    | WHERE @timestamp > NOW() - INTERVAL 25h AND @timestamp < NOW() - INTERVAL 1h
    | WHERE service.name == "' || service_name || '"
    | STATS 
        avg_latency = AVG(transaction.duration.us),
        p99_latency = PERCENTILE(transaction.duration.us, 99)
  ');
  
  SET regression = {
    'service_name': service_name,
    'current_metrics': current_metrics,
    'baseline_metrics': baseline_metrics,
    'threshold_pct': threshold_pct,
    'checked_at': CURRENT_TIMESTAMP()
  };
  
  RETURN regression;
END SKILL;
