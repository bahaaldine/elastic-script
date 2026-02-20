CREATE SKILL get_risky_users
  VERSION '1.0.0'
  DESCRIPTION 'Get users with highest risk scores'
  AUTHOR 'elastic'
  TAGS ['security', 'risk', 'ueba']
  (min_score INT DESCRIPTION 'Minimum risk score' DEFAULT 50, limit INT DESCRIPTION 'Max results' DEFAULT 20)
  RETURNS ARRAY
BEGIN
  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM security-* | STATS event_count = COUNT(*) BY user | SORT event_count DESC | LIMIT ' || limit);
  RETURN result;
END SKILL;
