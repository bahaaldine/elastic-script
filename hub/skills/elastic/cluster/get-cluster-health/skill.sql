CREATE SKILL get_cluster_health
  VERSION '1.0.0'
  DESCRIPTION 'Get Elasticsearch cluster health status and statistics'
  AUTHOR 'elastic'
  TAGS ['cluster,health,status']
  ()
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'cluster_name': 'production',
    'status': 'green',
    'number_of_nodes': 3,
    'number_of_data_nodes': 3,
    'active_primary_shards': 50,
    'active_shards': 100,
    'relocating_shards': 0,
    'unassigned_shards': 0
  };
END SKILL;
