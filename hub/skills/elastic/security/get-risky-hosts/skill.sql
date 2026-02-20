CREATE SKILL get_risky_hosts
  VERSION '1.0.0'
  DESCRIPTION 'Get hosts with highest risk scores'
  AUTHOR 'elastic'
  TAGS ['security', 'risk', 'hosts']
  (min_score INT DESCRIPTION 'Minimum risk score' DEFAULT 50, limit INT DESCRIPTION 'Max results' DEFAULT 20)
  RETURNS ARRAY
BEGIN
  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM security-* | STATS event_count = COUNT(*) BY host | SORT event_count DESC | LIMIT ' || limit);
  RETURN result;
END SKILL;
