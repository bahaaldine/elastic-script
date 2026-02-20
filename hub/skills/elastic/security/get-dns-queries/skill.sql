CREATE SKILL get_dns_queries
  VERSION '1.0.0'
  DESCRIPTION 'Get DNS query events'
  AUTHOR 'elastic'
  TAGS ['security', 'dns', 'network']
  (domain STRING DESCRIPTION 'Domain filter' DEFAULT NULL, limit INT DESCRIPTION 'Max results' DEFAULT 50)
  RETURNS ARRAY
BEGIN
  DECLARE query STRING;
  DECLARE result ARRAY;
  SET query = 'FROM security-* | WHERE event_type == "dns" | SORT @timestamp DESC | LIMIT ' || limit;
  SET result = ESQL_QUERY(query);
  RETURN result;
END SKILL;
