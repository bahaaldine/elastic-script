CREATE SKILL ab_create_agent
  VERSION '1.0.0'
  DESCRIPTION 'Create a new Agent Builder agent in Kibana with specified name, description, instructions, and tools.'
  AUTHOR 'elastic'
  TAGS ['agent-builder', 'agents', 'ai', 'create']
  (
    name STRING DESCRIPTION 'Agent name',
    description STRING DESCRIPTION 'Agent description',
    instructions STRING DESCRIPTION 'System instructions for the agent',
    model STRING DESCRIPTION 'LLM model to use' DEFAULT 'gpt-4',
    tools ARRAY DESCRIPTION 'Array of tool IDs to enable' DEFAULT NULL
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE payload DOCUMENT;
  DECLARE result DOCUMENT;
  
  SET payload = {
    'name': name,
    'description': description,
    'instructions': instructions,
    'model': model
  };
  
  IF tools IS NOT NULL THEN
    SET payload['tools'] = tools;
  END IF;
  
  SET result = HTTP_POST('/api/agent_builder/agents', payload, {
    'kbn-xsrf': 'true',
    'Content-Type': 'application/json'
  });
  RETURN result;
END SKILL;
