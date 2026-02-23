CREATE SKILL notify_oncall
  VERSION '2.0.0'
  DESCRIPTION 'Send notifications to on-call engineers via PagerDuty, Slack, or other configured channels.'
  AUTHOR 'sre-team'
  TAGS ['alerting', 'notification', 'oncall', 'pagerduty']
  (
    message STRING DESCRIPTION 'Message to send to on-call',
    severity STRING DESCRIPTION 'Severity level: critical, high, medium, low' DEFAULT 'high',
    channel STRING DESCRIPTION 'Notification channel: slack, pagerduty, email' DEFAULT 'slack',
    service_name STRING DESCRIPTION 'Affected service name' DEFAULT NULL
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE notification DOCUMENT;
  DECLARE oncall_info DOCUMENT;
  
  -- In production, this would integrate with PagerDuty/Slack APIs
  -- For now, we return a notification object that can be processed
  
  SET notification = {
    'id': 'notif-' || SUBSTR(MD5(message || CURRENT_TIMESTAMP()), 1, 8),
    'message': message,
    'severity': severity,
    'channel': channel,
    'service_name': service_name,
    'status': 'queued',
    'created_at': CURRENT_TIMESTAMP()
  };
  
  -- Log the notification
  PRINT 'Notification queued: ' || message;
  
  RETURN notification;
END SKILL;
