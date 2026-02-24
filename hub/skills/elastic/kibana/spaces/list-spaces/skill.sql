CREATE SKILL list_spaces
  VERSION '1.0.0'
  DESCRIPTION 'List all Kibana spaces'
  AUTHOR 'elastic'
  TAGS ['kibana', 'spaces', 'multi-tenancy']
  ()
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  SET result = SPACE_LIST();
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'success',
      'spaces': result.data,
      'count': ARRAY_LENGTH(result.data)
    };
  ELSE
    RETURN {
      'status': 'error',
      'error': result.error
    };
  END IF;
END SKILL;
