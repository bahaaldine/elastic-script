CREATE SKILL ab_chat
  VERSION '1.0.0'
  DESCRIPTION 'Send a chat message to an Agent Builder agent and get a response. This is the main way to interact with agents programmatically.'
  AUTHOR 'elastic'
  TAGS ['agent-builder', 'chat', 'ai', 'converse']
  (
    agent_id STRING DESCRIPTION 'Agent ID to chat with',
    message STRING DESCRIPTION 'Message to send to the agent',
    conversation_id STRING DESCRIPTION 'Existing conversation ID to continue (optional)' DEFAULT NULL
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE payload DOCUMENT;
  DECLARE result DOCUMENT;
  
  SET payload = {
    'agentId': agent_id,
    'message': message
  };
  
  IF conversation_id IS NOT NULL THEN
    SET payload['conversationId'] = conversation_id;
  END IF;
  
  SET result = HTTP_POST('/api/agent_builder/converse', payload, {
    'kbn-xsrf': 'true',
    'Content-Type': 'application/json'
  });
  RETURN result;
END SKILL;
