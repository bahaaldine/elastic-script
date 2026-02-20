CREATE SKILL get_node_stats
  VERSION '1.0.0'
  DESCRIPTION 'Get detailed statistics for a specific node'
  AUTHOR 'elastic'
  TAGS ['cluster', 'nodes', 'stats']
  (node_name STRING DESCRIPTION 'Node name to get stats for')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'name': node_name,
    'jvm_heap_used_percent': 55,
    'cpu_percent': 25,
    'disk_used_percent': 60,
    'indexing_rate': 1000,
    'search_rate': 5000,
    'gc_old_count': 10
  };
END SKILL;
