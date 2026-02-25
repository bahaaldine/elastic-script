CREATE SKILL compare_time_periods
  VERSION '1.1.0'
  DESCRIPTION 'Compare a numeric metric between a recent window and a previous baseline window. Returns averages for both periods plus the absolute and percentage change.'
  AUTHOR 'elastic'
  TAGS ['observability', 'comparison', 'analysis', 'metrics']
  (
    index_pattern STRING DESCRIPTION 'Index to query' DEFAULT 'metrics-*',
    metric_field STRING DESCRIPTION 'Numeric field to compare' DEFAULT 'system.cpu.total.pct',
    recent_window STRING DESCRIPTION 'Recent period duration (e.g. 1h, 6h, 1d)' DEFAULT '1d',
    baseline_window STRING DESCRIPTION 'Baseline period duration before the recent window' DEFAULT '7d'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE current_result ARRAY;
  DECLARE baseline_result ARRAY;
  DECLARE current_avg FLOAT;
  DECLARE baseline_avg FLOAT;
  DECLARE change_abs FLOAT;
  DECLARE change_pct FLOAT;

  -- Current window: last N time units
  SET current_result = ESQL_QUERY('FROM ' || index_pattern || ' | WHERE @timestamp >= NOW() - ' || recent_window || ' | STATS avg_value = AVG(' || metric_field || '), min_value = MIN(' || metric_field || '), max_value = MAX(' || metric_field || '), doc_count = COUNT(*)');

  -- Baseline window: from (recent + baseline) ago to recent ago
  SET baseline_result = ESQL_QUERY('FROM ' || index_pattern || ' | WHERE @timestamp >= NOW() - ' || baseline_window || ' AND @timestamp < NOW() - ' || recent_window || ' | STATS avg_value = AVG(' || metric_field || '), min_value = MIN(' || metric_field || '), max_value = MAX(' || metric_field || '), doc_count = COUNT(*)');

  SET current_avg = current_result[0]['avg_value'];
  SET baseline_avg = baseline_result[0]['avg_value'];

  IF baseline_avg IS NOT NULL AND baseline_avg != 0 THEN
    SET change_abs = current_avg - baseline_avg;
    SET change_pct = ROUND(((current_avg - baseline_avg) * 100.0) / baseline_avg, 2);
  ELSE
    SET change_abs = 0.0;
    SET change_pct = 0.0;
  END IF;

  RETURN {
    'metric_field': metric_field,
    'current_window': recent_window,
    'baseline_window': baseline_window,
    'current_avg': current_avg,
    'current_min': current_result[0]['min_value'],
    'current_max': current_result[0]['max_value'],
    'current_docs': current_result[0]['doc_count'],
    'baseline_avg': baseline_avg,
    'baseline_min': baseline_result[0]['min_value'],
    'baseline_max': baseline_result[0]['max_value'],
    'baseline_docs': baseline_result[0]['doc_count'],
    'change_absolute': change_abs,
    'change_percent': change_pct
  };
END SKILL;
