CREATE SKILL ab_list_tools
  VERSION '1.0.0'
  DESCRIPTION 'List all tools available in Agent Builder. Tools are capabilities that agents can use.'
  AUTHOR 'elastic'
  TAGS ['agent-builder', 'tools', 'ai']
  ()
  RETURNS ARRAY
BEGIN
  DECLARE result DOCUMENT;
  SET result = HTTP_GET('/api/agent_builder/tools', {
    'kbn-xsrf': 'true'
  });
  RETURN result['tools'];
END SKILL;
