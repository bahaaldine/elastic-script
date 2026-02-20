CREATE SKILL get_job_status
  VERSION '1.0.0'
  DESCRIPTION 'Get detailed status of an ML job'
  AUTHOR 'elastic'
  TAGS ['ml', 'jobs', 'status']
  (job_id STRING DESCRIPTION 'ML job ID')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'job_id': job_id,
    'state': 'opened',
    'assignment_explanation': 'Job is assigned to node',
    'data_counts': {'processed_record_count': 10000},
    'model_size_stats': {'model_bytes': 1048576}
  };
END SKILL;
