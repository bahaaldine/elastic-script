CREATE SKILL ab_get_agent
  VERSION '1.0.0'
  DESCRIPTION 'Get details of a specific Agent Builder agent by ID. Returns full agent configuration including tools, instructions, and model settings.'
  AUTHOR 'elastic'
  TAGS ['agent-builder', 'agents', 'ai']
  (
    agent_id STRING DESCRIPTION 'Agent ID to retrieve'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  SET result = HTTP_GET('/api/agent_builder/agents/' || agent_id, {
    'kbn-xsrf': 'true'
  });
  RETURN result;
END SKILL;
