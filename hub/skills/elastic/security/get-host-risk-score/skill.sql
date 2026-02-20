CREATE SKILL get_host_risk_score
  VERSION '1.0.0'
  DESCRIPTION 'Get risk score for a host based on security events'
  AUTHOR 'elastic'
  TAGS ['security', 'risk', 'hosts']
  (hostname STRING DESCRIPTION 'Hostname to check')
  RETURNS DOCUMENT
BEGIN
  DECLARE events_result ARRAY;
  DECLARE event_count INT;
  DECLARE risk_score INT;
  DECLARE risk_level STRING;
  SET events_result = ESQL_QUERY('FROM security-* | WHERE host == "' || hostname || '" | STATS count = COUNT(*)');
  SET event_count = events_result[0]['count'];
  
  IF event_count > 50 THEN
    SET risk_score = 75;
    SET risk_level = 'high';
  ELSEIF event_count > 20 THEN
    SET risk_score = 50;
    SET risk_level = 'medium';
  ELSE
    SET risk_score = 25;
    SET risk_level = 'low';
  END IF;
  
  RETURN {
    'hostname': hostname,
    'risk_score': risk_score,
    'risk_level': risk_level,
    'event_count': event_count
  };
END SKILL;
