CREATE SKILL get_anomalies
  VERSION '1.0.0'
  DESCRIPTION 'Get detected anomalies from ML jobs. Use for anomaly detection and alerting.'
  AUTHOR 'elastic'
  TAGS ['ml,anomaly,detection']
  (job_id STRING DESCRIPTION 'ML job ID to get anomalies for' DEFAULT NULL, min_score INT DESCRIPTION 'Minimum anomaly score (0-100)' DEFAULT 50, limit INT DESCRIPTION 'Maximum results' DEFAULT 20)
  RETURNS ARRAY
BEGIN
  DECLARE query STRING;
  DECLARE result ARRAY;
  SET query = 'FROM .ml-anomalies-* | WHERE record_score > ' || min_score || ' | SORT record_score DESC | LIMIT ' || limit;
  SET result = ESQL_QUERY(query);
  RETURN result;
END SKILL;
