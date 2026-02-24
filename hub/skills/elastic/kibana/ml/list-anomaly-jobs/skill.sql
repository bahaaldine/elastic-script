CREATE SKILL list_ml_anomaly_jobs
  VERSION '1.0.0'
  DESCRIPTION 'List all ML anomaly detection jobs'
  AUTHOR 'elastic'
  TAGS ['kibana', 'ml', 'anomaly-detection', 'machine-learning']
  ()
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  SET result = ML_ANOMALY_JOB_LIST();
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'success',
      'jobs': result.data.jobs,
      'count': result.data.count
    };
  ELSE
    RETURN {
      'status': 'error',
      'error': result.error
    };
  END IF;
END SKILL;
