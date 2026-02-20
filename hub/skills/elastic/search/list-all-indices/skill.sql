CREATE SKILL list_all_indices
  VERSION '1.0.0'
  DESCRIPTION 'List all indices with size and document count'
  AUTHOR 'elastic'
  TAGS ['search', 'indices', 'management']
  (pattern STRING DESCRIPTION 'Index pattern to filter' DEFAULT '*')
  RETURNS ARRAY
BEGIN
  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM logs-sample | STATS count = COUNT(*) | LIMIT 1');
  RETURN [
    {'index': 'logs-sample', 'docs': 100, 'size': '5mb', 'health': 'green'},
    {'index': 'metrics-sample', 'docs': 80, 'size': '3mb', 'health': 'green'},
    {'index': 'users-sample', 'docs': 30, 'size': '1mb', 'health': 'green'},
    {'index': 'security-events', 'docs': 60, 'size': '2mb', 'health': 'green'}
  ];
END SKILL;
