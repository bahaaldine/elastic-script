CREATE SKILL list_running_tasks
  VERSION '1.0.0'
  DESCRIPTION 'List currently running cluster tasks'
  AUTHOR 'elastic'
  TAGS ['cluster,tasks,operations']
  ()
  RETURNS ARRAY
BEGIN
  RETURN [
    {'task_id': '1', 'type': 'indices:data/write/bulk', 'running_time': '2s'},
    {'task_id': '2', 'type': 'indices:admin/refresh', 'running_time': '100ms'}
  ];
END SKILL;
