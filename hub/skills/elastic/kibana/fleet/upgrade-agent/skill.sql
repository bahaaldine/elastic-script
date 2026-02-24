CREATE SKILL upgrade_fleet_agent
  VERSION '1.0.0'
  DESCRIPTION 'Upgrade a Fleet agent to a specific version'
  AUTHOR 'elastic'
  TAGS ['kibana', 'fleet', 'agents', 'upgrade']
  (
    agent_id STRING DESCRIPTION 'The ID of the agent to upgrade',
    version STRING DESCRIPTION 'Target version (e.g., 8.12.0)'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  SET result = AGENT_UPGRADE(agent_id, version);
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'upgrading',
      'agent_id': agent_id,
      'target_version': version,
      'message': 'Agent upgrade initiated'
    };
  ELSE
    RETURN {
      'status': 'error',
      'error': result.error
    };
  END IF;
END SKILL;
