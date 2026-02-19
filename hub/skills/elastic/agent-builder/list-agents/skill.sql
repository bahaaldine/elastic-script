CREATE SKILL ab_list_agents
  VERSION '1.0.0'
  DESCRIPTION 'List all Agent Builder agents configured in Kibana. Returns agent IDs, names, descriptions, and configurations.'
  AUTHOR 'elastic'
  TAGS ['agent-builder', 'agents', 'ai']
  (
    space_id STRING DESCRIPTION 'Kibana space ID' DEFAULT 'default'
  )
  RETURNS ARRAY
BEGIN
  DECLARE result DOCUMENT;
  SET result = HTTP_GET('/api/agent_builder/agents', {
    'kbn-xsrf': 'true'
  });
  RETURN result['agents'];
END SKILL;
