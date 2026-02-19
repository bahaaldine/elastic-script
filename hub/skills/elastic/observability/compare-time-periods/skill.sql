CREATE SKILL compare_time_periods
  VERSION '1.0.0'
  DESCRIPTION 'Compare metrics between two time periods'
  AUTHOR 'elastic'
  TAGS ['observability,comparison,analysis']
  (index_pattern STRING DESCRIPTION 'Index to compare' DEFAULT 'metrics-*', metric STRING DESCRIPTION 'Metric to compare')
  RETURNS DOCUMENT
BEGIN
  DECLARE current_result ARRAY;
  SET current_result = ESQL_QUERY('FROM ' || index_pattern || ' | WHERE metric_name == "' || metric || '" | STATS avg_value = AVG(value)');
  RETURN {
    'metric': metric,
    'current_avg': current_result[0]['avg_value'],
    'comparison': 'Use time range parameters for full comparison'
  };
END SKILL;
