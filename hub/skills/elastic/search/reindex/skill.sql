CREATE SKILL reindex
  VERSION '1.0.0'
  DESCRIPTION 'Copy documents from one index to another'
  AUTHOR 'elastic'
  TAGS ['search,reindex,management']
  (source_index STRING DESCRIPTION 'Source index', dest_index STRING DESCRIPTION 'Destination index')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'result': 'reindexing',
    'source': source_index,
    'dest': dest_index,
    'status': 'started'
  };
END SKILL;
