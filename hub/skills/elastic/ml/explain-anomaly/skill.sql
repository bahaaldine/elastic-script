CREATE SKILL explain_anomaly
  VERSION '1.0.0'
  DESCRIPTION 'Get explanation for a specific anomaly'
  AUTHOR 'elastic'
  TAGS ['ml,anomaly,explanation']
  (job_id STRING DESCRIPTION 'ML job ID', anomaly_id STRING DESCRIPTION 'Anomaly record ID')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'anomaly_id': anomaly_id,
    'job_id': job_id,
    'score': 85,
    'explanation': 'Unusual spike in request count compared to historical baseline',
    'typical_value': 100,
    'actual_value': 500,
    'contributing_factors': ['Time of day', 'Day of week', 'Recent deployment']
  };
END SKILL;
