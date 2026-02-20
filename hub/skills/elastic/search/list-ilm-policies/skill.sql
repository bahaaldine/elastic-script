CREATE SKILL list_ilm_policies
  VERSION '1.0.0'
  DESCRIPTION 'List Index Lifecycle Management policies'
  AUTHOR 'elastic'
  TAGS ['search', 'ilm', 'lifecycle']
  ()
  RETURNS ARRAY
BEGIN
  RETURN [
    {'name': 'logs-policy', 'phases': ['hot', 'warm', 'cold', 'delete'], 'indices': 50},
    {'name': 'metrics-policy', 'phases': ['hot', 'delete'], 'indices': 30}
  ];
END SKILL;
