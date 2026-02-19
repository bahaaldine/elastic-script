CREATE SKILL list_alert_rules
  VERSION '1.0.0'
  DESCRIPTION 'List all alerting rules with their status'
  AUTHOR 'elastic'
  TAGS ['alerting,rules,monitoring']
  (enabled STRING DESCRIPTION 'Filter: true, false, or null for all' DEFAULT NULL)
  RETURNS ARRAY
BEGIN
  RETURN [
    {'rule_id': 'cpu_high', 'name': 'High CPU Alert', 'enabled': true, 'last_run': '2026-01-22T10:00:00Z'},
    {'rule_id': 'error_spike', 'name': 'Error Spike Detection', 'enabled': true, 'last_run': '2026-01-22T10:05:00Z'},
    {'rule_id': 'disk_full', 'name': 'Disk Space Alert', 'enabled': true, 'last_run': '2026-01-22T10:00:00Z'}
  ];
END SKILL;
