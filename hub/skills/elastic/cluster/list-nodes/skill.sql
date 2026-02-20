CREATE SKILL list_nodes
  VERSION '1.0.0'
  DESCRIPTION 'List all nodes in the Elasticsearch cluster'
  AUTHOR 'elastic'
  TAGS ['cluster', 'nodes', 'infrastructure']
  (role STRING DESCRIPTION 'Filter by role: master, data, ingest' DEFAULT NULL)
  RETURNS ARRAY
BEGIN
  RETURN [
    {'name': 'node-1', 'role': 'master,data', 'heap_percent': 45, 'disk_percent': 60},
    {'name': 'node-2', 'role': 'data', 'heap_percent': 55, 'disk_percent': 65},
    {'name': 'node-3', 'role': 'data', 'heap_percent': 50, 'disk_percent': 58}
  ];
END SKILL;
