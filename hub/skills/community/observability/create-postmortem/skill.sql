CREATE SKILL create_postmortem
  VERSION '1.0.0'
  DESCRIPTION 'Generate a postmortem document from incident data including timeline, impact analysis, and action items.'
  AUTHOR 'sre-team'
  TAGS ['observability', 'incident', 'postmortem', 'documentation']
  (
    incident_id STRING DESCRIPTION 'Incident ID to generate postmortem for',
    start_time STRING DESCRIPTION 'Incident start time' DEFAULT NULL,
    end_time STRING DESCRIPTION 'Incident end time' DEFAULT NULL
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE postmortem DOCUMENT;
  DECLARE timeline ARRAY;
  DECLARE affected_services ARRAY;
  DECLARE error_summary ARRAY;
  
  -- Build timeline from logs
  SET timeline = ESQL_QUERY('
    FROM logs-*
    | WHERE @timestamp > NOW() - INTERVAL 2h
    | WHERE log.level IN ("ERROR", "WARN", "FATAL")
    | KEEP @timestamp, service.name, log.level, message
    | SORT @timestamp ASC
    | LIMIT 50
  ');
  
  -- Get affected services summary
  SET affected_services = ESQL_QUERY('
    FROM logs-*
    | WHERE @timestamp > NOW() - INTERVAL 2h
    | WHERE log.level == "ERROR"
    | STATS error_count = COUNT(*), first_error = MIN(@timestamp), last_error = MAX(@timestamp) BY service.name
    | SORT error_count DESC
  ');
  
  -- Error summary
  SET error_summary = ESQL_QUERY('
    FROM logs-*
    | WHERE @timestamp > NOW() - INTERVAL 2h
    | WHERE log.level == "ERROR"
    | STATS count = COUNT(*) BY error.type
    | SORT count DESC
    | LIMIT 10
  ');
  
  SET postmortem = {
    'incident_id': incident_id,
    'title': 'Postmortem for Incident ' || incident_id,
    'status': 'draft',
    'timeline': timeline,
    'affected_services': affected_services,
    'error_summary': error_summary,
    'impact': 'To be filled',
    'root_cause': 'To be determined',
    'action_items': ['Review timeline', 'Identify root cause', 'Document remediation steps'],
    'generated_at': CURRENT_TIMESTAMP()
  };
  
  RETURN postmortem;
END SKILL;
