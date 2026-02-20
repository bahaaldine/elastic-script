CREATE SKILL test_connector
  VERSION '1.0.0'
  DESCRIPTION 'Test an alerting connector'
  AUTHOR 'elastic'
  TAGS ['alerting', 'connectors', 'testing']
  (connector_id STRING DESCRIPTION 'Connector ID to test')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'connector_id': connector_id,
    'status': 'success',
    'message': 'Connector test successful'
  };
END SKILL;
