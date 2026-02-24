CREATE SKILL list_synthetics_monitors
  VERSION '1.0.0'
  DESCRIPTION 'List all Synthetics uptime monitors'
  AUTHOR 'elastic'
  TAGS ['kibana', 'synthetics', 'uptime', 'monitoring']
  (
    per_page INTEGER DESCRIPTION 'Results per page' DEFAULT 25
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  SET result = SYNTHETICS_MONITOR_LIST(1, per_page, []);
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'success',
      'monitors': result.data.monitors,
      'total': result.data.total
    };
  ELSE
    RETURN {
      'status': 'error',
      'error': result.error
    };
  END IF;
END SKILL;
