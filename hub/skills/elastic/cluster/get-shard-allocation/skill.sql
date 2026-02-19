CREATE SKILL get_shard_allocation
  VERSION '1.0.0'
  DESCRIPTION 'Get shard allocation across nodes'
  AUTHOR 'elastic'
  TAGS ['cluster,shards,allocation']
  (index STRING DESCRIPTION 'Index to check allocation for' DEFAULT NULL)
  RETURNS ARRAY
BEGIN
  RETURN [
    {'index': 'logs-sample', 'shard': 0, 'primary': true, 'node': 'node-1', 'state': 'STARTED'},
    {'index': 'logs-sample', 'shard': 0, 'primary': false, 'node': 'node-2', 'state': 'STARTED'},
    {'index': 'logs-sample', 'shard': 1, 'primary': true, 'node': 'node-2', 'state': 'STARTED'}
  ];
END SKILL;
