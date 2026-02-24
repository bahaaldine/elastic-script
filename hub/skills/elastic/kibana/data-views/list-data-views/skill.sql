CREATE SKILL list_data_views
  VERSION '1.0.0'
  DESCRIPTION 'List all Kibana data views (index patterns)'
  AUTHOR 'elastic'
  TAGS ['kibana', 'data-views', 'index-patterns']
  ()
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  SET result = DATA_VIEW_LIST();
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'success',
      'data_views': result.data.data_view,
      'count': ARRAY_LENGTH(result.data.data_view)
    };
  ELSE
    RETURN {
      'status': 'error',
      'error': result.error
    };
  END IF;
END SKILL;
