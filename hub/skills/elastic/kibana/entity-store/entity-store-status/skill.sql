CREATE SKILL entity_store_status
  VERSION '1.0.0'
  DESCRIPTION 'Get Security Entity Store status'
  AUTHOR 'elastic'
  TAGS ['kibana', 'security', 'entity-store', 'status']
  ()
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  SET result = ENTITY_STORE_STATUS();
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'success',
      'enabled': result.data.enabled,
      'entity_types': result.data.entity_types,
      'stats': result.data.stats
    };
  ELSE
    RETURN {
      'status': 'error',
      'error': result.error
    };
  END IF;
END SKILL;
