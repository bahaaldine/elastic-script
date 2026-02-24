CREATE SKILL create_synthetics_monitor
  VERSION '1.0.0'
  DESCRIPTION 'Create a new Synthetics uptime monitor'
  AUTHOR 'elastic'
  TAGS ['kibana', 'synthetics', 'uptime', 'monitoring', 'create']
  (
    config DOCUMENT DESCRIPTION 'Monitor configuration including name, type, urls, schedule, locations'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  SET result = SYNTHETICS_MONITOR_CREATE(config);
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'created',
      'monitor_id': result.data.id,
      'name': result.data.name,
      'type': result.data.type,
      'message': 'Monitor created successfully'
    };
  ELSE
    RETURN {
      'status': 'error',
      'error': result.error
    };
  END IF;
END SKILL;
