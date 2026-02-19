CREATE SKILL get_process_events
  VERSION '1.0.0'
  DESCRIPTION 'Get process execution events for threat hunting'
  AUTHOR 'elastic'
  TAGS ['security,process,hunting']
  (hostname STRING DESCRIPTION 'Host to analyze' DEFAULT NULL, process_name STRING DESCRIPTION 'Process name filter' DEFAULT NULL, limit INT DESCRIPTION 'Max results' DEFAULT 50)
  RETURNS ARRAY
BEGIN
  DECLARE query STRING;
  DECLARE result ARRAY;
  SET query = 'FROM security-* | WHERE event_type == "process" | SORT @timestamp DESC | LIMIT ' || limit;
  SET result = ESQL_QUERY(query);
  RETURN result;
END SKILL;
