CREATE SKILL delete_document_cmd
  VERSION '1.0.0'
  DESCRIPTION 'Delete a document by ID using the first-class DELETE command.'
  AUTHOR 'moltler'
  TAGS ['elasticsearch', 'delete', 'first-class', 'command', 'document']
  (
    index_name STRING DESCRIPTION 'Index containing the document',
    doc_id STRING DESCRIPTION 'Document ID to delete'
  )
  RETURNS DOCUMENT
BEGIN
  -- Use first-class DELETE command
  DELETE FROM index_name WHERE doc_id;
  
  RETURN {
    'status': 'deleted',
    'index': index_name,
    'id': doc_id
  };
END SKILL;
