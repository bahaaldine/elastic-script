CREATE SKILL get_error_groups
  VERSION '1.0.0'
  DESCRIPTION 'Get errors grouped by type/message for a service'
  AUTHOR 'elastic'
  TAGS ['apm', 'errors', 'analysis']
  (service STRING DESCRIPTION 'Service name to analyze', limit INT DESCRIPTION 'Max error groups' DEFAULT 20)
  RETURNS ARRAY
BEGIN
  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM logs-* | WHERE service == "' || service || '" AND level == "ERROR" | STATS count = COUNT(*) BY message | SORT count DESC | LIMIT ' || limit);
  RETURN result;
END SKILL;
