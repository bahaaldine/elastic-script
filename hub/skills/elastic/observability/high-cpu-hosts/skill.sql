CREATE SKILL high_cpu_hosts
  VERSION '1.0.0'
  DESCRIPTION 'Find hosts with high CPU usage. Use this to identify overloaded servers, detect resource contention, or prioritize scaling decisions.'
  AUTHOR 'elastic'
  TAGS ['observability', 'metrics', 'cpu', 'alerts']
  (
    index_pattern STRING DESCRIPTION 'Index pattern for metrics data' DEFAULT 'metrics-*',
    threshold INT DESCRIPTION 'CPU usage threshold percentage (0-100)' DEFAULT 80
  )
  RETURNS ARRAY
BEGIN
  DECLARE query STRING;
  DECLARE result ARRAY;
  
  SET query = 'FROM ' || index_pattern || ' | WHERE metric_name == "cpu" AND value > ' || threshold || ' | STATS avg_cpu = AVG(value), max_cpu = MAX(value), samples = COUNT(*) BY host | SORT avg_cpu DESC | LIMIT 20';
  SET result = ESQL_QUERY(query);
  
  RETURN result;
END SKILL;
