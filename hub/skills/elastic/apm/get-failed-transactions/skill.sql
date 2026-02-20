CREATE SKILL get_failed_transactions
  VERSION '1.0.0'
  DESCRIPTION 'Get failed/errored transactions'
  AUTHOR 'elastic'
  TAGS ['apm', 'errors', 'transactions']
  (service STRING DESCRIPTION 'Service name' DEFAULT NULL, limit INT DESCRIPTION 'Max results' DEFAULT 20)
  RETURNS ARRAY
BEGIN
  DECLARE query STRING;
  DECLARE result ARRAY;
  IF service IS NOT NULL THEN
    SET query = 'FROM logs-* | WHERE service == "' || service || '" AND status_code >= 400 | SORT @timestamp DESC | LIMIT ' || limit;
  ELSE
    SET query = 'FROM logs-* | WHERE status_code >= 400 | SORT @timestamp DESC | LIMIT ' || limit;
  END IF;
  SET result = ESQL_QUERY(query);
  RETURN result;
END SKILL;
