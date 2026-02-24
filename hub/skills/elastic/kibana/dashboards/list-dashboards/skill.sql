CREATE SKILL list_dashboards
  VERSION '1.0.0'
  DESCRIPTION 'List all Kibana dashboards'
  AUTHOR 'elastic'
  TAGS ['kibana', 'dashboards', 'visualization']
  (
    search STRING DESCRIPTION 'Search term to filter dashboards' DEFAULT NULL,
    per_page INTEGER DESCRIPTION 'Results per page' DEFAULT 20
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  SET result = DASHBOARD_LIST(search, per_page);
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'success',
      'dashboards': result.data.saved_objects,
      'total': result.data.total
    };
  ELSE
    RETURN {
      'status': 'error',
      'error': result.error
    };
  END IF;
END SKILL;
