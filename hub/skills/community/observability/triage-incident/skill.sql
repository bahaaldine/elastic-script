CREATE SKILL triage_incident
  VERSION '1.0.0'
  DESCRIPTION 'Automatically triage and prioritize incidents based on impact, affected services, and historical patterns.'
  AUTHOR 'sre-team'
  TAGS ['observability', 'incident', 'triage', 'sre']
  (
    incident_id STRING DESCRIPTION 'Incident ID to triage',
    service_name STRING DESCRIPTION 'Affected service name' DEFAULT NULL
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE triage_result DOCUMENT;
  DECLARE affected_services ARRAY;
  DECLARE error_context ARRAY;
  DECLARE severity STRING;
  
  -- Get affected services
  SET affected_services = ESQL_QUERY('
    FROM logs-*
    | WHERE @timestamp > NOW() - INTERVAL 30m
    | WHERE log.level == "ERROR"
    | STATS error_count = COUNT(*) BY service.name
    | SORT error_count DESC
    | LIMIT 5
  ');
  
  -- Get error context
  SET error_context = ESQL_QUERY('
    FROM logs-*
    | WHERE @timestamp > NOW() - INTERVAL 15m
    | WHERE log.level == "ERROR"
    | KEEP @timestamp, service.name, message, error.message
    | SORT @timestamp DESC
    | LIMIT 10
  ');
  
  -- Determine severity based on affected services count
  IF ARRAY_LENGTH(affected_services) > 3 THEN
    SET severity = 'critical';
  ELSEIF ARRAY_LENGTH(affected_services) > 1 THEN
    SET severity = 'high';
  ELSE
    SET severity = 'medium';
  END IF;
  
  SET triage_result = {
    'incident_id': incident_id,
    'severity': severity,
    'affected_services': affected_services,
    'error_context': error_context,
    'recommended_action': 'Investigate ' || severity || ' priority incident'
  };
  
  RETURN triage_result;
END SKILL;
