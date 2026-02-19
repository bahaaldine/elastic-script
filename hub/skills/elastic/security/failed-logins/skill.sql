CREATE SKILL failed_logins
  VERSION '1.0.0'
  DESCRIPTION 'Get failed login attempts, grouped by user or source IP. Use this to detect brute force attacks, credential stuffing, or compromised accounts.'
  AUTHOR 'elastic'
  TAGS ['security', 'authentication', 'brute-force']
  (
    index_pattern STRING DESCRIPTION 'Security events index pattern' DEFAULT 'security-*',
    group_by STRING DESCRIPTION 'Group by: user, source_ip, or both' DEFAULT 'user',
    limit INT DESCRIPTION 'Maximum results' DEFAULT 20
  )
  RETURNS ARRAY
BEGIN
  DECLARE query STRING;
  DECLARE result ARRAY;
  
  IF group_by == 'source_ip' THEN
    SET query = 'FROM ' || index_pattern || ' | WHERE event_type == "authentication" AND outcome == "failure" | STATS count = COUNT(*) BY source_ip | SORT count DESC | LIMIT ' || limit;
  ELSE
    SET query = 'FROM ' || index_pattern || ' | WHERE event_type == "authentication" AND outcome == "failure" | STATS count = COUNT(*) BY user | SORT count DESC | LIMIT ' || limit;
  END IF;
  
  SET result = ESQL_QUERY(query);
  RETURN result;
END SKILL;
