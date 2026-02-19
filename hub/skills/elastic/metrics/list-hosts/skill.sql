CREATE SKILL list_hosts
  VERSION '1.0.0'
  DESCRIPTION 'List all monitored hosts with their current status'
  AUTHOR 'elastic'
  TAGS ['metrics,hosts,infrastructure']
  (status STRING DESCRIPTION 'Filter by status' DEFAULT NULL)
  RETURNS ARRAY
BEGIN
  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM metrics-* | STATS last_seen = MAX(@timestamp) BY host | SORT last_seen DESC | LIMIT 100');
  RETURN result;
END SKILL;
