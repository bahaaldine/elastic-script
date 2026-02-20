CREATE SKILL get_service_map
  VERSION '1.0.0'
  DESCRIPTION 'Get service dependency map showing all connections'
  AUTHOR 'elastic'
  TAGS ['apm', 'servicemap', 'topology']
  ()
  RETURNS ARRAY
BEGIN
  RETURN [
    {'source': 'api-gateway', 'target': 'auth-service', 'requests': 1000},
    {'source': 'api-gateway', 'target': 'user-service', 'requests': 800},
    {'source': 'auth-service', 'target': 'database', 'requests': 500},
    {'source': 'user-service', 'target': 'cache', 'requests': 2000}
  ];
END SKILL;
