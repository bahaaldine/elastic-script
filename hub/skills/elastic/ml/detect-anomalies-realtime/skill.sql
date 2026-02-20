CREATE SKILL detect_anomalies_realtime
  VERSION '1.0.0'
  DESCRIPTION 'Analyze data in real-time for anomalies using statistical methods'
  AUTHOR 'elastic'
  TAGS ['ml', 'anomaly', 'realtime']
  (index_pattern STRING DESCRIPTION 'Index to analyze', field STRING DESCRIPTION 'Numeric field to check for anomalies', threshold FLOAT DESCRIPTION 'Standard deviation threshold' DEFAULT 2.0)
  RETURNS ARRAY
BEGIN
  DECLARE stats_result ARRAY;
  DECLARE anomalies ARRAY;
  SET stats_result = ESQL_QUERY('FROM ' || index_pattern || ' | STATS avg_val = AVG(' || field || '), std_val = STDDEV(' || field || ')');
  SET anomalies = ESQL_QUERY('FROM ' || index_pattern || ' | WHERE ABS(' || field || ' - ' || stats_result[0]['avg_val'] || ') > ' || threshold || ' * ' || stats_result[0]['std_val'] || ' | LIMIT 20');
  RETURN anomalies;
END SKILL;
