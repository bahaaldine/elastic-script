CREATE SKILL list_slos
  VERSION '1.0.0'
  DESCRIPTION 'List all Service Level Objectives'
  AUTHOR 'elastic'
  TAGS ['kibana', 'observability', 'slo', 'monitoring']
  (
    name STRING DESCRIPTION 'Filter by SLO name' DEFAULT NULL,
    per_page INTEGER DESCRIPTION 'Results per page' DEFAULT 25
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  SET result = SLO_LIST(name, 1, per_page);
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'success',
      'slos': result.data.results,
      'total': result.data.total
    };
  ELSE
    RETURN {
      'status': 'error',
      'error': result.error
    };
  END IF;
END SKILL;
