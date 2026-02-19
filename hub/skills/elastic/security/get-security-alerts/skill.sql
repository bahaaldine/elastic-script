CREATE SKILL get_security_alerts
  VERSION '1.0.0'
  DESCRIPTION 'Get recent security alerts and detections. Use this to review threats, check for active incidents, or respond to security events.'
  AUTHOR 'elastic'
  TAGS ['security', 'alerts', 'siem', 'detection']
  (
    severity STRING DESCRIPTION 'Filter by severity: critical, high, medium, low' DEFAULT NULL,
    limit INT DESCRIPTION 'Maximum alerts to return' DEFAULT 20,
    index_pattern STRING DESCRIPTION 'Security events index pattern' DEFAULT 'security-*'
  )
  RETURNS ARRAY
BEGIN
  DECLARE query STRING;
  DECLARE result ARRAY;
  
  IF severity IS NOT NULL THEN
    SET query = 'FROM ' || index_pattern || ' | WHERE event_type == "alert" AND severity == "' || severity || '" | SORT @timestamp DESC | LIMIT ' || limit;
  ELSE
    SET query = 'FROM ' || index_pattern || ' | WHERE event_type == "alert" | SORT @timestamp DESC | LIMIT ' || limit;
  END IF;
  
  SET result = ESQL_QUERY(query);
  RETURN result;
END SKILL;
