CREATE SKILL get_mapping
  VERSION '1.0.0'
  DESCRIPTION 'Get field mappings for an index'
  AUTHOR 'elastic'
  TAGS ['search', 'mappings', 'schema']
  (index_name STRING DESCRIPTION 'Index name')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'index': index_name,
    'mappings': {
      '@timestamp': 'date',
      'message': 'text',
      'level': 'keyword',
      'service': 'keyword'
    }
  };
END SKILL;
