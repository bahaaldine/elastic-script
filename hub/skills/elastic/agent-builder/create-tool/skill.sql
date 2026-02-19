CREATE SKILL ab_create_tool
  VERSION '1.0.0'
  DESCRIPTION 'Create a new tool in Agent Builder. Tools define capabilities that agents can invoke.'
  AUTHOR 'elastic'
  TAGS ['agent-builder', 'tools', 'ai', 'create']
  (
    name STRING DESCRIPTION 'Tool name',
    description STRING DESCRIPTION 'Tool description for AI understanding',
    type STRING DESCRIPTION 'Tool type: elasticsearch, http, mcp' DEFAULT 'elasticsearch',
    configuration STRING DESCRIPTION 'JSON configuration for the tool'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE payload DOCUMENT;
  DECLARE result DOCUMENT;
  
  SET payload = {
    'name': name,
    'description': description,
    'type': type,
    'configuration': configuration
  };
  
  SET result = HTTP_POST('/api/agent_builder/tools', payload, {
    'kbn-xsrf': 'true',
    'Content-Type': 'application/json'
  });
  RETURN result;
END SKILL;
