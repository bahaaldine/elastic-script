CREATE SKILL list_monitors
  VERSION '1.0.0'
  DESCRIPTION 'List all uptime/synthetic monitors'
  AUTHOR 'elastic'
  TAGS ['observability,uptime,monitors']
  (status STRING DESCRIPTION 'Filter by status: up, down' DEFAULT NULL)
  RETURNS ARRAY
BEGIN
  RETURN [
    {'monitor_id': 'api-health', 'name': 'API Health Check', 'status': 'up', 'url': 'https://api.example.com/health'},
    {'monitor_id': 'website', 'name': 'Website Monitor', 'status': 'up', 'url': 'https://www.example.com'},
    {'monitor_id': 'database', 'name': 'Database Check', 'status': 'up', 'host': 'db.example.com:5432'}
  ];
END SKILL;
