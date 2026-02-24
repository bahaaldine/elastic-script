CREATE SKILL list_connectors
  VERSION '1.0.0'
  DESCRIPTION 'List all Kibana action connectors'
  AUTHOR 'elastic'
  TAGS ['kibana', 'connectors', 'actions', 'integrations']
  ()
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  SET result = CONNECTOR_LIST();
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'success',
      'connectors': result.data,
      'count': ARRAY_LENGTH(result.data)
    };
  ELSE
    RETURN {
      'status': 'error',
      'error': result.error
    };
  END IF;
END SKILL;
