CREATE SKILL create_case
  VERSION '1.0.0'
  DESCRIPTION 'Create a new Kibana case for incident management'
  AUTHOR 'elastic'
  TAGS ['kibana', 'cases', 'incident-management', 'security', 'create']
  (
    title STRING DESCRIPTION 'Case title',
    description STRING DESCRIPTION 'Case description' DEFAULT '',
    tags ARRAY DESCRIPTION 'Tags for the case' DEFAULT [],
    severity STRING DESCRIPTION 'Severity: low, medium, high, critical' DEFAULT 'low'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  SET result = CASE_CREATE(title, description, tags, severity, NULL);
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'created',
      'case_id': result.data.id,
      'title': result.data.title,
      'severity': result.data.severity,
      'url': '/app/security/cases/' || result.data.id,
      'message': 'Case created successfully'
    };
  ELSE
    RETURN {
      'status': 'error',
      'error': result.error
    };
  END IF;
END SKILL;
