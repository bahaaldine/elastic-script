CREATE SKILL get_disk_usage
  VERSION '1.0.0'
  DESCRIPTION 'Get disk usage for hosts showing available and used space'
  AUTHOR 'elastic'
  TAGS ['metrics', 'disk', 'storage']
  (hostname STRING DESCRIPTION 'Host name (optional)' DEFAULT NULL)
  RETURNS ARRAY
BEGIN
  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM metrics-* | WHERE metric_name == "disk" | STATS avg_usage = AVG(value) BY host | SORT avg_usage DESC | LIMIT 20');
  RETURN result;
END SKILL;
