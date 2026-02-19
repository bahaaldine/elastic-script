CREATE SKILL user_activity
  VERSION '1.0.0'
  DESCRIPTION 'Get activity timeline for a specific user. Use this during user investigations, access reviews, or when tracking potentially compromised accounts.'
  AUTHOR 'elastic'
  TAGS ['security', 'user', 'investigation', 'audit']
  (
    username STRING DESCRIPTION 'Username to investigate',
    index_pattern STRING DESCRIPTION 'Security events index pattern' DEFAULT 'security-*',
    limit INT DESCRIPTION 'Maximum events to return' DEFAULT 50
  )
  RETURNS ARRAY
BEGIN
  DECLARE query STRING;
  DECLARE result ARRAY;
  
  SET query = 'FROM ' || index_pattern || ' | WHERE user == "' || username || '" | SORT @timestamp DESC | LIMIT ' || limit;
  SET result = ESQL_QUERY(query);
  
  RETURN result;
END SKILL;
