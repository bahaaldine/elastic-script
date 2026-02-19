CREATE SKILL list_ml_jobs
  VERSION '1.0.0'
  DESCRIPTION 'List all machine learning anomaly detection jobs with their status'
  AUTHOR 'elastic'
  TAGS ['ml,anomaly,jobs']
  (status STRING DESCRIPTION 'Filter by status: started, stopped, closed' DEFAULT NULL)
  RETURNS ARRAY
BEGIN
  DECLARE query STRING;
  DECLARE result ARRAY;
  SET query = 'FROM .ml-anomalies-* | STATS count = COUNT(*) BY job_id | LIMIT 50';
  SET result = ESQL_QUERY(query);
  RETURN result;
END SKILL;
