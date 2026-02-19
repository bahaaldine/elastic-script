CREATE SKILL hunt_ioc
  VERSION '1.0.0'
  DESCRIPTION 'Hunt for an Indicator of Compromise (IP, hash, domain)'
  AUTHOR 'elastic'
  TAGS ['security,threat,hunting']
  (ioc STRING DESCRIPTION 'IOC value to hunt for (IP, hash, domain)', ioc_type STRING DESCRIPTION 'Type: ip, hash, domain' DEFAULT 'ip')
  RETURNS ARRAY
BEGIN
  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM security-* | WHERE source_ip == "' || ioc || '" OR destination_ip == "' || ioc || '" | LIMIT 50');
  RETURN result;
END SKILL;
