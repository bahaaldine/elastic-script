CREATE SKILL create_space
  VERSION '1.0.0'
  DESCRIPTION 'Create a new Kibana space for multi-tenancy'
  AUTHOR 'elastic'
  TAGS ['kibana', 'spaces', 'multi-tenancy', 'create']
  (
    id STRING DESCRIPTION 'Unique space ID (lowercase, no spaces)',
    name STRING DESCRIPTION 'Display name for the space',
    description STRING DESCRIPTION 'Description of the space' DEFAULT '',
    color STRING DESCRIPTION 'Color for the space avatar' DEFAULT '#00bfb3'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  SET result = SPACE_CREATE(id, name, description, color, NULL, []);
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'created',
      'space_id': id,
      'name': name,
      'url': '/s/' || id,
      'message': 'Space created successfully'
    };
  ELSE
    RETURN {
      'status': 'error',
      'error': result.error
    };
  END IF;
END SKILL;
