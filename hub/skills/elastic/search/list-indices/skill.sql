CREATE SKILL list_indices
  VERSION '1.0.0'
  DESCRIPTION 'List sample indices that are available for querying. Use this to explore what data is available in the cluster.'
  AUTHOR 'elastic'
  TAGS ['search', 'indices', 'management']
  (
    pattern STRING DESCRIPTION 'Index name pattern (informational only)' DEFAULT '*'
  )
  RETURNS ARRAY
BEGIN
  DECLARE indices ARRAY;
  
  SET indices = [
    {'name': 'logs-sample', 'description': 'Application logs with errors'},
    {'name': 'metrics-sample', 'description': 'System metrics (CPU, memory)'},
    {'name': 'users-sample', 'description': 'User profiles'},
    {'name': 'orders-sample', 'description': 'E-commerce orders'},
    {'name': 'products-sample', 'description': 'Product catalog'},
    {'name': 'security-events', 'description': 'Security audit events'}
  ];
  
  RETURN indices;
END SKILL;
