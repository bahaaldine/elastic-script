CREATE SKILL bulk_index
  VERSION '1.0.0'
  DESCRIPTION 'Index multiple documents in bulk'
  AUTHOR 'elastic'
  TAGS ['search,documents,bulk']
  (index_name STRING DESCRIPTION 'Index name', documents STRING DESCRIPTION 'JSON array of documents')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'result': 'indexed',
    'index': index_name,
    'items': 10,
    'errors': false
  };
END SKILL;
