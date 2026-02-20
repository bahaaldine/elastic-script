CREATE SKILL mute_alert
  VERSION '1.0.0'
  DESCRIPTION 'Mute an alert temporarily'
  AUTHOR 'elastic'
  TAGS ['alerting', 'mute', 'management']
  (alert_id STRING DESCRIPTION 'Alert ID to mute', duration_minutes INT DESCRIPTION 'Minutes to mute' DEFAULT 60)
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'alert_id': alert_id,
    'muted': true,
    'muted_until': 'Now + ' || duration_minutes || ' minutes'
  };
END SKILL;
