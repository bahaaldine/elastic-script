CREATE SKILL list_cases
  VERSION '1.0.0'
  DESCRIPTION 'List Kibana cases with optional status filter'
  AUTHOR 'elastic'
  TAGS ['kibana', 'cases', 'incident-management', 'security']
  (
    status STRING DESCRIPTION 'Filter by status: open, in-progress, closed' DEFAULT NULL,
    per_page INTEGER DESCRIPTION 'Results per page' DEFAULT 20
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  SET result = CASE_LIST(status, NULL, per_page);
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'success',
      'cases': result.data.cases,
      'total': result.data.total,
      'count_open': result.data.count_open_cases,
      'count_in_progress': result.data.count_in_progress_cases,
      'count_closed': result.data.count_closed_cases
    };
  ELSE
    RETURN {
      'status': 'error',
      'error': result.error
    };
  END IF;
END SKILL;
