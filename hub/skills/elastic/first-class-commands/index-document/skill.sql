CREATE SKILL index_document_cmd
  VERSION '1.0.0'
  DESCRIPTION 'Index a document using the first-class INDEX command. Cleaner syntax than function calls.'
  AUTHOR 'moltler'
  TAGS ['elasticsearch', 'index', 'first-class', 'command', 'document']
  (
    index_name STRING DESCRIPTION 'Target index name',
    document DOCUMENT DESCRIPTION 'Document to index'
  )
  RETURNS DOCUMENT
BEGIN
  -- Use first-class INDEX command (cleaner than INDEX_DOCUMENT function)
  DECLARE result DOCUMENT;
  
  INDEX document INTO index_name;
  
  SET result = {
    'status': 'indexed',
    'index': index_name
  };
  
  RETURN result;
END SKILL;
