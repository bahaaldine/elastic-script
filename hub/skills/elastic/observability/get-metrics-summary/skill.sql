CREATE SKILL get_metrics_summary
  VERSION '1.0.0'
  DESCRIPTION 'Get summary statistics for system metrics (CPU, memory, latency). Use this for health checks, capacity planning, or identifying resource bottlenecks.'
  AUTHOR 'elastic'
  TAGS ['observability', 'metrics', 'analysis', 'monitoring']
  (
    index_pattern STRING DESCRIPTION 'Index pattern for metrics data' DEFAULT 'metrics-*',
    metric_name STRING DESCRIPTION 'Specific metric to analyze (cpu, memory, latency) or NULL for all' DEFAULT NULL
  )
  RETURNS ARRAY
BEGIN
  DECLARE query STRING;
  DECLARE result ARRAY;
  
  IF metric_name IS NOT NULL THEN
    SET query = 'FROM ' || index_pattern || ' | WHERE metric_name == "' || metric_name || '" | STATS avg_value = AVG(value), max_value = MAX(value), min_value = MIN(value) BY host | LIMIT 20';
  ELSE
    SET query = 'FROM ' || index_pattern || ' | STATS avg_value = AVG(value), max_value = MAX(value), min_value = MIN(value) BY metric_name | LIMIT 20';
  END IF;
  
  SET result = ESQL_QUERY(query);
  RETURN result;
END SKILL;
