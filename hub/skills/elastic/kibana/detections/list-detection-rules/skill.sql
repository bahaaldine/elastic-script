CREATE SKILL list_detection_rules
  VERSION '1.0.0'
  DESCRIPTION 'List security detection rules'
  AUTHOR 'elastic'
  TAGS ['kibana', 'security', 'detections', 'rules', 'siem']
  (
    per_page INTEGER DESCRIPTION 'Results per page' DEFAULT 20,
    filter STRING DESCRIPTION 'Filter query' DEFAULT NULL
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  SET result = DETECTION_RULE_LIST(per_page, 1, filter);
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'success',
      'rules': result.data.data,
      'total': result.data.total
    };
  ELSE
    RETURN {
      'status': 'error',
      'error': result.error
    };
  END IF;
END SKILL;
