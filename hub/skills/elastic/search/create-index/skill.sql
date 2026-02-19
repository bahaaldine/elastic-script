CREATE SKILL create_index
  VERSION '1.0.0'
  DESCRIPTION 'Create a new index with settings and mappings'
  AUTHOR 'elastic'
  TAGS ['search,indices,create']
  (index_name STRING DESCRIPTION 'Index name', shards INT DESCRIPTION 'Number of primary shards' DEFAULT 1, replicas INT DESCRIPTION 'Number of replicas' DEFAULT 1)
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'acknowledged': true,
    'index': index_name,
    'shards_acknowledged': true
  };
END SKILL;
