CREATE SKILL get_document
  VERSION '1.0.0'
  DESCRIPTION 'Get a single document by ID from an index. Use this when you know the exact document ID and need to retrieve its full contents.'
  AUTHOR 'elastic'
  TAGS ['search', 'document', 'get']
  (
    index_name STRING DESCRIPTION 'Name of the index containing the document',
    doc_id STRING DESCRIPTION 'Document ID to retrieve'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  SET result = GET_DOCUMENT(index_name, doc_id);
  RETURN result;
END SKILL;
