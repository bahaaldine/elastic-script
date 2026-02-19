CREATE SKILL get_alert_history
  VERSION '1.0.0'
  DESCRIPTION 'Get historical alerts for analysis'
  AUTHOR 'elastic'
  TAGS ['alerting,history,analysis']
  (rule_id STRING DESCRIPTION 'Filter by rule ID' DEFAULT NULL, days INT DESCRIPTION 'Number of days of history' DEFAULT 7, limit INT DESCRIPTION 'Max results' DEFAULT 100)
  RETURNS ARRAY
BEGIN
  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM .alerts* | SORT @timestamp DESC | LIMIT ' || limit);
  RETURN result;
END SKILL;
