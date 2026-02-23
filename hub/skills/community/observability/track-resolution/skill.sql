CREATE SKILL track_resolution
  VERSION '1.0.0'
  DESCRIPTION 'Track incident resolution progress, update status, and record resolution actions.'
  AUTHOR 'sre-team'
  TAGS ['observability', 'incident', 'resolution', 'tracking']
  (
    incident_id STRING DESCRIPTION 'Incident ID to track',
    status STRING DESCRIPTION 'Resolution status: investigating, identified, monitoring, resolved' DEFAULT 'investigating',
    notes STRING DESCRIPTION 'Resolution notes' DEFAULT NULL
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE resolution DOCUMENT;
  DECLARE current_errors ARRAY;
  DECLARE error_trend ARRAY;
  
  -- Get current error state
  SET current_errors = ESQL_QUERY('
    FROM logs-*
    | WHERE @timestamp > NOW() - INTERVAL 5m
    | WHERE log.level == "ERROR"
    | STATS error_count = COUNT(*) BY service.name
  ');
  
  -- Get error trend (is it improving?)
  SET error_trend = ESQL_QUERY('
    FROM logs-*
    | WHERE @timestamp > NOW() - INTERVAL 30m
    | WHERE log.level == "ERROR"
    | EVAL time_bucket = DATE_TRUNC(5 minutes, @timestamp)
    | STATS error_count = COUNT(*) BY time_bucket
    | SORT time_bucket ASC
  ');
  
  SET resolution = {
    'incident_id': incident_id,
    'status': status,
    'notes': notes,
    'current_errors': current_errors,
    'error_trend': error_trend,
    'is_improving': ARRAY_LENGTH(current_errors) == 0,
    'updated_at': CURRENT_TIMESTAMP()
  };
  
  RETURN resolution;
END SKILL;
