CREATE SKILL ab_delete_agent
  VERSION '1.0.0'
  DESCRIPTION 'Delete an Agent Builder agent by ID.'
  AUTHOR 'elastic'
  TAGS ['agent-builder', 'agents', 'ai', 'delete']
  (
    agent_id STRING DESCRIPTION 'Agent ID to delete'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  SET result = HTTP_DELETE('/api/agent_builder/agents/' || agent_id, {
    'kbn-xsrf': 'true'
  });
  RETURN {'deleted': true, 'agent_id': agent_id};
END SKILL;
