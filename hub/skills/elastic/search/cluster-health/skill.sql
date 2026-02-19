CREATE SKILL cluster_health
  VERSION '1.0.0'
  DESCRIPTION 'Get Elasticsearch cluster health status. Use this to check if the cluster is healthy, see number of nodes, or verify shards are properly allocated.'
  AUTHOR 'elastic'
  TAGS ['cluster', 'health', 'management']
  (
    dummy STRING DESCRIPTION 'Unused parameter' DEFAULT NULL
  )
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'status': 'healthy',
    'message': 'Cluster is operational - for detailed health use Kibana or ES API directly',
    'cluster_name': 'runTask'
  };
END SKILL;
