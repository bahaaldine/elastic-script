CREATE SKILL threat_summary
  VERSION '1.0.0'
  DESCRIPTION 'Get a summary of threats and security posture. Use this for security dashboards, executive briefings, or quick health checks of your security status.'
  AUTHOR 'elastic'
  TAGS ['security', 'dashboard', 'summary', 'posture']
  (
    index_pattern STRING DESCRIPTION 'Security events index pattern' DEFAULT 'security-*'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE total_result ARRAY;
  DECLARE alert_result ARRAY;
  DECLARE critical_result ARRAY;
  DECLARE auth_result ARRAY;
  DECLARE total_events INT;
  DECLARE alerts INT;
  DECLARE critical_alerts INT;
  DECLARE failed_auth INT;
  
  SET total_result = ESQL_QUERY('FROM ' || index_pattern || ' | STATS count = COUNT(*)');
  SET alert_result = ESQL_QUERY('FROM ' || index_pattern || ' | WHERE event_type == "alert" | STATS count = COUNT(*)');
  SET critical_result = ESQL_QUERY('FROM ' || index_pattern || ' | WHERE event_type == "alert" AND severity == "critical" | STATS count = COUNT(*)');
  SET auth_result = ESQL_QUERY('FROM ' || index_pattern || ' | WHERE event_type == "authentication" AND outcome == "failure" | STATS count = COUNT(*)');
  
  SET total_events = total_result[0]['count'];
  SET alerts = alert_result[0]['count'];
  SET critical_alerts = critical_result[0]['count'];
  SET failed_auth = auth_result[0]['count'];
  
  RETURN {
    'total_security_events': total_events,
    'total_alerts': alerts,
    'critical_alerts': critical_alerts,
    'failed_authentications': failed_auth
  };
END SKILL;
