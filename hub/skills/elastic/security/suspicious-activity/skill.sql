CREATE SKILL suspicious_activity
  VERSION '1.0.0'
  DESCRIPTION 'Get recent suspicious or anomalous activity from security events. Use this during incident response, threat hunting, or when investigating potential breaches.'
  AUTHOR 'elastic'
  TAGS ['security', 'threats', 'anomaly', 'hunting']
  (
    index_pattern STRING DESCRIPTION 'Security events index pattern' DEFAULT 'security-*',
    limit INT DESCRIPTION 'Maximum events to return' DEFAULT 30
  )
  RETURNS ARRAY
BEGIN
  DECLARE query STRING;
  DECLARE result ARRAY;
  
  SET query = 'FROM ' || index_pattern || ' | WHERE severity == "critical" OR severity == "high" | SORT @timestamp DESC | LIMIT ' || limit;
  SET result = ESQL_QUERY(query);
  
  RETURN result;
END SKILL;
