CREATE SKILL get_user_risk_score
  VERSION '1.0.0'
  DESCRIPTION 'Get risk score for a user based on their activity'
  AUTHOR 'elastic'
  TAGS ['security', 'risk', 'ueba']
  (username STRING DESCRIPTION 'Username to check')
  RETURNS DOCUMENT
BEGIN
  DECLARE events_result ARRAY;
  DECLARE event_count INT;
  DECLARE risk_score INT;
  DECLARE risk_level STRING;
  SET events_result = ESQL_QUERY('FROM security-* | WHERE user == "' || username || '" | STATS count = COUNT(*)');
  SET event_count = events_result[0]['count'];
  
  IF event_count > 100 THEN
    SET risk_score = 85;
    SET risk_level = 'high';
  ELSEIF event_count > 50 THEN
    SET risk_score = 60;
    SET risk_level = 'medium';
  ELSEIF event_count > 20 THEN
    SET risk_score = 40;
    SET risk_level = 'medium';
  ELSE
    SET risk_score = 20;
    SET risk_level = 'low';
  END IF;
  
  RETURN {
    'username': username,
    'risk_score': risk_score,
    'risk_level': risk_level,
    'event_count': event_count
  };
END SKILL;
