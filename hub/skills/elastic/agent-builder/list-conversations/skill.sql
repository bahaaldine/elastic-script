CREATE SKILL ab_list_conversations
  VERSION '1.0.0'
  DESCRIPTION 'List all conversations with Agent Builder agents. Returns conversation IDs, titles, and metadata.'
  AUTHOR 'elastic'
  TAGS ['agent-builder', 'conversations', 'ai']
  ()
  RETURNS ARRAY
BEGIN
  DECLARE result DOCUMENT;
  SET result = HTTP_GET('/api/agent_builder/conversations', {
    'kbn-xsrf': 'true'
  });
  RETURN result['conversations'];
END SKILL;
