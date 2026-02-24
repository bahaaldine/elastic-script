CREATE SKILL create_alert_rule
  VERSION '1.0.0'
  DESCRIPTION 'Create a new Kibana alert rule'
  AUTHOR 'elastic'
  TAGS ['kibana', 'alerting', 'rules', 'create']
  (
    rule_config DOCUMENT DESCRIPTION 'Alert rule configuration including name, rule_type_id, schedule, params, actions'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  SET result = ALERT_RULE_CREATE(rule_config);
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'created',
      'rule_id': result.data.id,
      'name': result.data.name,
      'enabled': result.data.enabled,
      'message': 'Alert rule created successfully'
    };
  ELSE
    RETURN {
      'status': 'error',
      'error': result.error
    };
  END IF;
END SKILL;
