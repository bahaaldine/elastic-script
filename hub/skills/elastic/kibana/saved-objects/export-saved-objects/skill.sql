CREATE SKILL export_saved_objects
  VERSION '1.0.0'
  DESCRIPTION 'Export Kibana saved objects (dashboards, visualizations, etc.)'
  AUTHOR 'elastic'
  TAGS ['kibana', 'saved-objects', 'export', 'backup']
  (
    types ARRAY DESCRIPTION 'Types to export (e.g., ["dashboard", "visualization"])' DEFAULT [],
    objects ARRAY DESCRIPTION 'Specific objects to export [{type, id}]' DEFAULT []
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  SET result = SAVED_OBJECT_EXPORT(types, objects);
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'success',
      'objects': result.data,
      'message': 'Objects exported successfully'
    };
  ELSE
    RETURN {
      'status': 'error',
      'error': result.error
    };
  END IF;
END SKILL;
