CREATE SKILL enable_alert_rule
  VERSION '1.0.0'
  DESCRIPTION 'Enable a Kibana alert rule'
  AUTHOR 'elastic'
  TAGS ['kibana', 'alerting', 'rules', 'enable']
  (
    rule_id STRING DESCRIPTION 'The ID of the alert rule to enable'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  SET result = ALERT_RULE_ENABLE(rule_id);
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'enabled',
      'rule_id': rule_id,
      'message': 'Alert rule enabled successfully'
    };
  ELSE
    RETURN {
      'status': 'error',
      'error': result.error
    };
  END IF;
END SKILL;
