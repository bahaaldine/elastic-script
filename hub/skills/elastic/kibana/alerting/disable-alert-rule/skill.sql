CREATE SKILL disable_alert_rule
  VERSION '1.0.0'
  DESCRIPTION 'Disable a Kibana alert rule'
  AUTHOR 'elastic'
  TAGS ['kibana', 'alerting', 'rules', 'disable']
  (
    rule_id STRING DESCRIPTION 'The ID of the alert rule to disable'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  SET result = ALERT_RULE_DISABLE(rule_id);
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'disabled',
      'rule_id': rule_id,
      'message': 'Alert rule disabled successfully'
    };
  ELSE
    RETURN {
      'status': 'error',
      'error': result.error
    };
  END IF;
END SKILL;
