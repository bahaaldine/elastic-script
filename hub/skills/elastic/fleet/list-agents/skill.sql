CREATE SKILL list_agents
  VERSION '1.0.0'
  DESCRIPTION 'List all Elastic Agents'
  AUTHOR 'elastic'
  TAGS ['fleet', 'agents', 'management']
  (status STRING DESCRIPTION 'Filter by status: online, offline, updating' DEFAULT NULL)
  RETURNS ARRAY
BEGIN
  RETURN [
    {'agent_id': 'agent-001', 'hostname': 'prod-web-01', 'status': 'online', 'policy': 'production'},
    {'agent_id': 'agent-002', 'hostname': 'prod-web-02', 'status': 'online', 'policy': 'production'},
    {'agent_id': 'agent-003', 'hostname': 'dev-server', 'status': 'offline', 'policy': 'development'}
  ];
END SKILL;
