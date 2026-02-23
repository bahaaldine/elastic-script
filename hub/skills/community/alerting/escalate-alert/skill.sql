CREATE SKILL escalate_alert
  VERSION '1.0.0'
  DESCRIPTION 'Escalate an alert to the next tier based on escalation policies.'
  AUTHOR 'sre-team'
  TAGS ['alerting', 'escalation', 'oncall']
  (
    alert_id STRING DESCRIPTION 'Alert ID to escalate',
    reason STRING DESCRIPTION 'Reason for escalation' DEFAULT 'No response within SLA'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE escalation DOCUMENT;
  DECLARE alert_info ARRAY;
  
  -- Get alert details
  SET alert_info = ESQL_QUERY('
    FROM .alerts-*
    | WHERE kibana.alert.uuid == "' || alert_id || '"
    | LIMIT 1
  ');
  
  SET escalation = {
    'alert_id': alert_id,
    'reason': reason,
    'escalated_at': CURRENT_TIMESTAMP(),
    'escalation_level': 2,
    'status': 'escalated',
    'original_alert': alert_info
  };
  
  RETURN escalation;
END SKILL;
