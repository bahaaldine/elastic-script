CREATE SKILL update_document
  VERSION '1.0.0'
  DESCRIPTION 'Update an existing document'
  AUTHOR 'elastic'
  TAGS ['search', 'documents', 'update']
  (index_name STRING DESCRIPTION 'Index name', doc_id STRING DESCRIPTION 'Document ID', updates STRING DESCRIPTION 'JSON with field updates')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'result': 'updated',
    'index': index_name,
    'id': doc_id,
    'version': 2
  };
END SKILL;
