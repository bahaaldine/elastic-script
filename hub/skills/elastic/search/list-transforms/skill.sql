CREATE SKILL list_transforms
  VERSION '1.0.0'
  DESCRIPTION 'List all data transforms'
  AUTHOR 'elastic'
  TAGS ['search,transforms,management']
  ()
  RETURNS ARRAY
BEGIN
  RETURN [
    {'transform_id': 'logs-summary', 'source': 'logs-*', 'dest': 'logs-summary', 'state': 'started'},
    {'transform_id': 'metrics-hourly', 'source': 'metrics-*', 'dest': 'metrics-hourly', 'state': 'started'}
  ];
END SKILL;
