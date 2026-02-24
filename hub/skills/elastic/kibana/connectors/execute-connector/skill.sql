CREATE SKILL execute_connector
  VERSION '1.0.0'
  DESCRIPTION 'Execute an action connector (send message, create ticket, etc.)'
  AUTHOR 'elastic'
  TAGS ['kibana', 'connectors', 'actions', 'execute']
  (
    connector_id STRING DESCRIPTION 'ID of the connector to execute',
    params DOCUMENT DESCRIPTION 'Parameters for the connector action'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  SET result = CONNECTOR_EXECUTE(connector_id, params);
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'executed',
      'connector_id': connector_id,
      'response': result.data,
      'message': 'Connector action executed successfully'
    };
  ELSE
    RETURN {
      'status': 'error',
      'error': result.error
    };
  END IF;
END SKILL;
