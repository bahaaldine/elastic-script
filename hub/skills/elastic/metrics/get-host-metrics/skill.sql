CREATE SKILL get_host_metrics
  VERSION '1.0.0'
  DESCRIPTION 'Get current CPU, memory, and disk metrics for a host'
  AUTHOR 'elastic'
  TAGS ['metrics', 'hosts', 'resources']
  (hostname STRING DESCRIPTION 'Host name to check')
  RETURNS ARRAY
BEGIN
  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM metrics-* | WHERE host == "' || hostname || '" | STATS avg_value = AVG(value) BY metric_name | LIMIT 10');
  RETURN result;
END SKILL;
