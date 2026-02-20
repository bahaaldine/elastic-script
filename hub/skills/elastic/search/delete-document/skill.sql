CREATE SKILL delete_document
  VERSION '1.0.0'
  DESCRIPTION 'Delete a document by ID'
  AUTHOR 'elastic'
  TAGS ['search', 'documents', 'delete']
  (index_name STRING DESCRIPTION 'Index name', doc_id STRING DESCRIPTION 'Document ID')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'result': 'deleted',
    'index': index_name,
    'id': doc_id
  };
END SKILL;
