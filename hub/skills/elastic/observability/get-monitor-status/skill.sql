CREATE SKILL get_monitor_status
  VERSION '1.0.0'
  DESCRIPTION 'Get current status of a monitor'
  AUTHOR 'elastic'
  TAGS ['observability', 'uptime', 'status']
  (monitor_id STRING DESCRIPTION 'Monitor ID')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'monitor_id': monitor_id,
    'status': 'up',
    'last_check': CURRENT_TIMESTAMP(),
    'response_time_ms': 150,
    'uptime_percent': 99.95
  };
END SKILL;
