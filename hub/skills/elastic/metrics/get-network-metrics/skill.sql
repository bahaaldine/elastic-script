CREATE SKILL get_network_metrics
  VERSION '1.0.0'
  DESCRIPTION 'Get network throughput metrics (bytes in/out)'
  AUTHOR 'elastic'
  TAGS ['metrics,network,throughput']
  (hostname STRING DESCRIPTION 'Host name (optional)' DEFAULT NULL, limit INT DESCRIPTION 'Max results' DEFAULT 20)
  RETURNS ARRAY
BEGIN
  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM metrics-* | WHERE metric_name LIKE "network*" | STATS total = SUM(value) BY host, metric_name | LIMIT ' || limit);
  RETURN result;
END SKILL;
