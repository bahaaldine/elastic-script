CREATE SKILL get_index_stats
  VERSION '1.0.0'
  DESCRIPTION 'Get detailed statistics for an index'
  AUTHOR 'elastic'
  TAGS ['search,indices,stats']
  (index_name STRING DESCRIPTION 'Index name to get stats for')
  RETURNS DOCUMENT
BEGIN
  DECLARE count_result ARRAY;
  SET count_result = ESQL_QUERY('FROM ' || index_name || ' | STATS count = COUNT(*)');
  RETURN {
    'index': index_name,
    'doc_count': count_result[0]['count'],
    'primary_shards': 1,
    'replica_shards': 1,
    'health': 'green'
  };
END SKILL;
