CREATE SKILL detect_anomalies
  VERSION '1.0.0'
  DESCRIPTION 'Detect anomalies in time-series data using statistical methods.'
  AUTHOR 'ml-team'
  TAGS ['ml', 'anomaly-detection', 'timeseries']
  (
    index_pattern STRING DESCRIPTION 'Index pattern to analyze' DEFAULT 'metrics-*',
    metric_field STRING DESCRIPTION 'Field containing the metric' DEFAULT 'value',
    time_range STRING DESCRIPTION 'Time range to analyze' DEFAULT '24h',
    std_dev_threshold DOUBLE DESCRIPTION 'Standard deviation threshold' DEFAULT 3.0
  )
  RETURNS ARRAY
BEGIN
  DECLARE anomalies ARRAY;
  DECLARE stats ARRAY;
  
  -- Calculate baseline statistics
  SET stats = ESQL_QUERY('
    FROM ' || index_pattern || '
    | WHERE @timestamp > NOW() - INTERVAL ' || time_range || '
    | STATS 
        mean_value = AVG(' || metric_field || '),
        std_dev = STDDEV(' || metric_field || '),
        min_value = MIN(' || metric_field || '),
        max_value = MAX(' || metric_field || ')
  ');
  
  -- Find anomalous data points
  SET anomalies = ESQL_QUERY('
    FROM ' || index_pattern || '
    | WHERE @timestamp > NOW() - INTERVAL ' || time_range || '
    | EVAL time_bucket = DATE_TRUNC(5 minutes, @timestamp)
    | STATS avg_value = AVG(' || metric_field || ') BY time_bucket, host.name
    | SORT avg_value DESC
    | LIMIT 20
  ');
  
  RETURN anomalies;
END SKILL;
