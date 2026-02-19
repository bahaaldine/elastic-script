CREATE SKILL analyze_database_queries
  VERSION '1.0.0'
  DESCRIPTION 'Analyze slow database queries'
  AUTHOR 'elastic'
  TAGS ['apm,database,performance']
  (threshold_ms INT DESCRIPTION 'Minimum query duration in ms' DEFAULT 100, limit INT DESCRIPTION 'Max results' DEFAULT 20)
  RETURNS ARRAY
BEGIN
  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM logs-* | WHERE service LIKE "*database*" AND duration_ms > ' || threshold_ms || ' | SORT duration_ms DESC | LIMIT ' || limit);
  RETURN result;
END SKILL;
