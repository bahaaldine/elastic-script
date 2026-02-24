CREATE SKILL list_alert_rules
  VERSION '1.0.0'
  DESCRIPTION 'List all Kibana alert rules with optional filtering'
  AUTHOR 'elastic'
  TAGS ['kibana', 'alerting', 'rules', 'monitoring']
  (
    filter STRING DESCRIPTION 'KQL filter query' DEFAULT NULL,
    per_page INTEGER DESCRIPTION 'Results per page' DEFAULT 20,
    page INTEGER DESCRIPTION 'Page number' DEFAULT 1
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  SET result = ALERT_RULE_LIST(filter, per_page, page);
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'success',
      'rules': result.data.data,
      'total': result.data.total,
      'page': page,
      'per_page': per_page
    };
  ELSE
    RETURN {
      'status': 'error',
      'error': result.error
    };
  END IF;
END SKILL;
