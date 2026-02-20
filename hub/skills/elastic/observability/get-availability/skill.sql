CREATE SKILL get_availability
  VERSION '1.0.0'
  DESCRIPTION 'Get availability percentage for a monitor'
  AUTHOR 'elastic'
  TAGS ['observability', 'uptime', 'availability']
  (monitor_id STRING DESCRIPTION 'Monitor ID', days INT DESCRIPTION 'Number of days' DEFAULT 30)
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'monitor_id': monitor_id,
    'period_days': days,
    'availability_percent': 99.95,
    'total_checks': days * 24 * 60,
    'failed_checks': 3
  };
END SKILL;
