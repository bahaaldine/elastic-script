CREATE SKILL get_agent_status
  VERSION '1.0.0'
  DESCRIPTION 'Get status and health of a specific agent'
  AUTHOR 'elastic'
  TAGS ['fleet,agents,health']
  (agent_id STRING DESCRIPTION 'Agent ID to check')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'agent_id': agent_id,
    'status': 'online',
    'last_checkin': CURRENT_TIMESTAMP(),
    'policy': 'production',
    'version': '8.12.0'
  };
END SKILL;
