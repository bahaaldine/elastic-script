CREATE SKILL create_slo
  VERSION '1.0.0'
  DESCRIPTION 'Create a new Service Level Objective'
  AUTHOR 'elastic'
  TAGS ['kibana', 'observability', 'slo', 'create']
  (
    config DOCUMENT DESCRIPTION 'SLO configuration including name, indicator, budgetingMethod, objective'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  SET result = SLO_CREATE(config);
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'created',
      'slo_id': result.data.id,
      'name': result.data.name,
      'message': 'SLO created successfully'
    };
  ELSE
    RETURN {
      'status': 'error',
      'error': result.error
    };
  END IF;
END SKILL;
