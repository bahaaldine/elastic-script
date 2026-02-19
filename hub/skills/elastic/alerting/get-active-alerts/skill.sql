CREATE SKILL get_active_alerts
  VERSION '1.0.0'
  DESCRIPTION 'Get all currently active/firing alerts'
  AUTHOR 'elastic'
  TAGS ['alerting,active,incidents']
  (severity STRING DESCRIPTION 'Filter by severity: critical, high, medium, low' DEFAULT NULL, limit INT DESCRIPTION 'Max results' DEFAULT 50)
  RETURNS ARRAY
BEGIN
  DECLARE query STRING;
  DECLARE result ARRAY;
  IF severity IS NOT NULL THEN
    SET query = 'FROM .alerts* | WHERE status == "active" AND severity == "' || severity || '" | SORT @timestamp DESC | LIMIT ' || limit;
  ELSE
    SET query = 'FROM .alerts* | WHERE status == "active" | SORT @timestamp DESC | LIMIT ' || limit;
  END IF;
  SET result = ESQL_QUERY(query);
  RETURN result;
END SKILL;
