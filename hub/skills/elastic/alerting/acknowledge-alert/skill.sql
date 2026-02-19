CREATE SKILL acknowledge_alert
  VERSION '1.0.0'
  DESCRIPTION 'Acknowledge an active alert'
  AUTHOR 'elastic'
  TAGS ['alerting,acknowledge,response']
  (alert_id STRING DESCRIPTION 'Alert ID to acknowledge', comment STRING DESCRIPTION 'Acknowledgement comment' DEFAULT 'Acknowledged via Moltler')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'alert_id': alert_id,
    'status': 'acknowledged',
    'acknowledged_at': CURRENT_TIMESTAMP(),
    'comment': comment
  };
END SKILL;
