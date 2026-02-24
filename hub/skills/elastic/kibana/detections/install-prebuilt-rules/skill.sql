CREATE SKILL install_prebuilt_rules
  VERSION '1.0.0'
  DESCRIPTION 'Install Elastic prebuilt detection rules'
  AUTHOR 'elastic'
  TAGS ['kibana', 'security', 'detections', 'rules', 'prebuilt']
  ()
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  DECLARE status DOCUMENT;
  
  -- First check current status
  SET status = PREBUILT_RULES_STATUS();
  
  -- Install rules
  SET result = PREBUILT_RULES_INSTALL();
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'installed',
      'rules_installed': result.data.rules_installed,
      'rules_updated': result.data.rules_updated,
      'message': 'Prebuilt rules installed successfully'
    };
  ELSE
    RETURN {
      'status': 'error',
      'error': result.error
    };
  END IF;
END SKILL;
