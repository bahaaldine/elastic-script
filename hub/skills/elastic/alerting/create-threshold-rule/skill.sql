CREATE SKILL create_threshold_rule
  VERSION '1.0.0'
  DESCRIPTION 'Create a threshold-based alert rule'
  AUTHOR 'elastic'
  TAGS ['alerting', 'rules', 'threshold']
  (name STRING DESCRIPTION 'Rule name', index_pattern STRING DESCRIPTION 'Index to monitor', field STRING DESCRIPTION 'Field to check', threshold INT DESCRIPTION 'Threshold value', condition STRING DESCRIPTION 'Condition: above, below, equals' DEFAULT 'above')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'rule_id': 'rule-' || SUBSTRING(name, 1, 8),
    'name': name,
    'type': 'threshold',
    'enabled': true,
    'created_at': CURRENT_TIMESTAMP()
  };
END SKILL;
