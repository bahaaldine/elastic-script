CREATE SKILL get_network_events
  VERSION '1.0.0'
  DESCRIPTION 'Get network connection events'
  AUTHOR 'elastic'
  TAGS ['security,network,connections']
  (source_ip STRING DESCRIPTION 'Source IP filter' DEFAULT NULL, destination_ip STRING DESCRIPTION 'Destination IP filter' DEFAULT NULL, limit INT DESCRIPTION 'Max results' DEFAULT 50)
  RETURNS ARRAY
BEGIN
  DECLARE query STRING;
  DECLARE result ARRAY;
  SET query = 'FROM security-* | WHERE event_type LIKE "*network*" | SORT @timestamp DESC | LIMIT ' || limit;
  SET result = ESQL_QUERY(query);
  RETURN result;
END SKILL;
