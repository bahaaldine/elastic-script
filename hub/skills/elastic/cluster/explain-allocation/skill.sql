CREATE SKILL explain_allocation
  VERSION '1.0.0'
  DESCRIPTION 'Explain why a shard is assigned or unassigned'
  AUTHOR 'elastic'
  TAGS ['cluster', 'allocation', 'debugging']
  (index STRING DESCRIPTION 'Index name', shard INT DESCRIPTION 'Shard number' DEFAULT 0)
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'index': index,
    'shard': shard,
    'explanation': 'Shard is assigned to node node-1',
    'status': 'assigned'
  };
END SKILL;
