CREATE SKILL send_opsgenie_alert
  VERSION '1.0.0'
  DESCRIPTION 'Create an OpsGenie alert'
  AUTHOR 'elastic'
  TAGS ['integrations,opsgenie,alerts']
  (message STRING DESCRIPTION 'Alert message', priority STRING DESCRIPTION 'Priority: P1-P5' DEFAULT 'P3')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'status': 'created',
    'platform': 'OpsGenie',
    'priority': priority,
    'timestamp': CURRENT_TIMESTAMP()
  };
END SKILL;
