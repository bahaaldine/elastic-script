CREATE SKILL list_apm_services
  VERSION '1.0.0'
  DESCRIPTION 'List all APM monitored services'
  AUTHOR 'elastic'
  TAGS ['kibana', 'apm', 'observability', 'services']
  (
    start STRING DESCRIPTION 'Start time (e.g., now-24h)' DEFAULT 'now-24h',
    end_time STRING DESCRIPTION 'End time (e.g., now)' DEFAULT 'now',
    environment STRING DESCRIPTION 'Filter by environment' DEFAULT NULL
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  SET result = APM_SERVICE_LIST(start, end_time, environment);
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'success',
      'services': result.data.items,
      'count': ARRAY_LENGTH(result.data.items)
    };
  ELSE
    RETURN {
      'status': 'error',
      'error': result.error
    };
  END IF;
END SKILL;
