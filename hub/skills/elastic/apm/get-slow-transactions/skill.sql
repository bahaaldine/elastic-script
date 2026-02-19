CREATE SKILL get_slow_transactions
  VERSION '1.0.0'
  DESCRIPTION 'Find the slowest transactions for a service or across all services'
  AUTHOR 'elastic'
  TAGS ['apm,latency,performance']
  (service STRING DESCRIPTION 'Service name (optional)' DEFAULT NULL, threshold_ms INT DESCRIPTION 'Minimum duration in ms' DEFAULT 1000, limit INT DESCRIPTION 'Max results' DEFAULT 20)
  RETURNS ARRAY
BEGIN
  DECLARE query STRING;
  DECLARE result ARRAY;
  IF service IS NOT NULL THEN
    SET query = 'FROM logs-* | WHERE service == "' || service || '" AND duration_ms > ' || threshold_ms || ' | SORT duration_ms DESC | LIMIT ' || limit;
  ELSE
    SET query = 'FROM logs-* | WHERE duration_ms > ' || threshold_ms || ' | SORT duration_ms DESC | LIMIT ' || limit;
  END IF;
  SET result = ESQL_QUERY(query);
  RETURN result;
END SKILL;
