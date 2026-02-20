CREATE SKILL create_document
  VERSION '1.0.0'
  DESCRIPTION 'Create a new document in an index'
  AUTHOR 'elastic'
  TAGS ['search', 'documents', 'create']
  (index_name STRING DESCRIPTION 'Index name', doc_id STRING DESCRIPTION 'Document ID' DEFAULT NULL, document STRING DESCRIPTION 'JSON document')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'result': 'created',
    'index': index_name,
    'id': CASE WHEN doc_id IS NOT NULL THEN doc_id ELSE 'auto-generated' END,
    'version': 1
  };
END SKILL;
