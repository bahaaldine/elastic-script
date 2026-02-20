CREATE SKILL get_throughput
  VERSION '1.0.0'
  DESCRIPTION 'Get request throughput for a service over time'
  AUTHOR 'elastic'
  TAGS ['apm', 'throughput', 'metrics']
  (service STRING DESCRIPTION 'Service name', interval STRING DESCRIPTION 'Time interval' DEFAULT '1h')
  RETURNS ARRAY
BEGIN
  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM logs-* | WHERE service == "' || service || '" | STATS count = COUNT(*)');
  RETURN result;
END SKILL;
