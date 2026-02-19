CREATE SKILL ab_execute_tool
  VERSION '1.0.0'
  DESCRIPTION 'Execute an Agent Builder tool directly with provided arguments. Useful for testing tools or running them outside of agent context.'
  AUTHOR 'elastic'
  TAGS ['agent-builder', 'tools', 'ai', 'execute']
  (
    tool_id STRING DESCRIPTION 'Tool ID to execute',
    arguments STRING DESCRIPTION 'JSON arguments to pass to the tool'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE payload DOCUMENT;
  DECLARE result DOCUMENT;
  
  SET payload = {
    'toolId': tool_id,
    'arguments': arguments
  };
  
  SET result = HTTP_POST('/api/agent_builder/tools/_execute', payload, {
    'kbn-xsrf': 'true',
    'Content-Type': 'application/json'
  });
  RETURN result;
END SKILL;
