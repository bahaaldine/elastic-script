CREATE SKILL create_dashboard
  VERSION '1.0.0'
  DESCRIPTION 'Create a new Kibana dashboard'
  AUTHOR 'elastic'
  TAGS ['kibana', 'dashboards', 'visualization', 'create']
  (
    title STRING DESCRIPTION 'Dashboard title',
    panels ARRAY DESCRIPTION 'Array of panel configurations' DEFAULT [],
    options DOCUMENT DESCRIPTION 'Dashboard options' DEFAULT {}
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  SET result = DASHBOARD_CREATE(title, panels, options, NULL);
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'created',
      'dashboard_id': result.data.id,
      'title': title,
      'url': '/app/dashboards#/view/' || result.data.id,
      'message': 'Dashboard created successfully'
    };
  ELSE
    RETURN {
      'status': 'error',
      'error': result.error
    };
  END IF;
END SKILL;
