CREATE SKILL ml_job_status
VERSION '1.0.0'
DESCRIPTION 'Get ML anomaly detection job status and statistics'
AUTHOR 'Moltler'
TAGS ['ml', 'anomaly-detection', 'monitoring']
(
    job_id IN STRING DEFAULT '*'
)
RETURNS DOCUMENT
BEGIN
    DECLARE jobs ARRAY;
    DECLARE stats ARRAY;
    
    -- Get job configurations
    SET jobs = ES_ML_GET_JOBS(job_id);
    
    -- Get job statistics
    SET stats = ES_ML_GET_JOB_STATS(job_id);
    
    RETURN {
        'job_count': ARRAY_LENGTH(jobs),
        'jobs': jobs,
        'stats': stats
    };
END SKILL;
