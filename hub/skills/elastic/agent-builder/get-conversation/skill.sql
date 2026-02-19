CREATE SKILL ab_get_conversation
  VERSION '1.0.0'
  DESCRIPTION 'Get full conversation history by ID. Returns all messages exchanged with the agent.'
  AUTHOR 'elastic'
  TAGS ['agent-builder', 'conversations', 'ai', 'history']
  (
    conversation_id STRING DESCRIPTION 'Conversation ID to retrieve'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  SET result = HTTP_GET('/api/agent_builder/conversations/' || conversation_id, {
    'kbn-xsrf': 'true'
  });
  RETURN result;
END SKILL;
