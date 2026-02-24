CREATE SKILL create_data_view
  VERSION '1.0.0'
  DESCRIPTION 'Create a new Kibana data view (index pattern)'
  AUTHOR 'elastic'
  TAGS ['kibana', 'data-views', 'index-patterns', 'create']
  (
    title STRING DESCRIPTION 'Index pattern (e.g., logs-* or metrics-*)',
    name STRING DESCRIPTION 'Display name for the data view' DEFAULT NULL,
    time_field STRING DESCRIPTION 'Name of the time field (e.g., @timestamp)' DEFAULT NULL
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  DECLARE display_name STRING;
  
  IF name IS NULL THEN
    SET display_name = title;
  ELSE
    SET display_name = name;
  END IF;
  
  SET result = DATA_VIEW_CREATE(title, display_name, time_field, NULL);
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'created',
      'data_view_id': result.data.data_view.id,
      'title': result.data.data_view.title,
      'name': result.data.data_view.name,
      'message': 'Data view created successfully'
    };
  ELSE
    RETURN {
      'status': 'error',
      'error': result.error
    };
  END IF;
END SKILL;
