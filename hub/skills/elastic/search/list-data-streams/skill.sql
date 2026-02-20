CREATE SKILL list_data_streams
  VERSION '1.0.0'
  DESCRIPTION 'List all data streams'
  AUTHOR 'elastic'
  TAGS ['search', 'datastreams', 'management']
  ()
  RETURNS ARRAY
BEGIN
  RETURN [
    {'name': 'logs-nginx-default', 'backing_indices': 5, 'generation': 5},
    {'name': 'metrics-system-default', 'backing_indices': 3, 'generation': 3},
    {'name': 'traces-apm-default', 'backing_indices': 7, 'generation': 7}
  ];
END SKILL;
