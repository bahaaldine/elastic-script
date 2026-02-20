CREATE SKILL list_detection_rules
  VERSION '1.0.0'
  DESCRIPTION 'List all security detection rules'
  AUTHOR 'elastic'
  TAGS ['security', 'rules', 'detection']
  (enabled STRING DESCRIPTION 'Filter: true or false' DEFAULT NULL)
  RETURNS ARRAY
BEGIN
  RETURN [
    {'rule_id': 'brute_force', 'name': 'Brute Force Detection', 'enabled': true, 'severity': 'high'},
    {'rule_id': 'malware', 'name': 'Malware Detection', 'enabled': true, 'severity': 'critical'},
    {'rule_id': 'data_exfil', 'name': 'Data Exfiltration', 'enabled': true, 'severity': 'critical'}
  ];
END SKILL;
