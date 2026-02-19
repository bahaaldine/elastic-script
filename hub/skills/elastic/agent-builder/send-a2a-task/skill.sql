CREATE SKILL ab_send_a2a_task
  VERSION '1.0.0'
  DESCRIPTION 'Send a task to an Agent Builder agent using A2A (Agent-to-Agent) protocol. This enables agent-to-agent communication.'
  AUTHOR 'elastic'
  TAGS ['agent-builder', 'a2a', 'ai', 'task']
  (
    agent_id STRING DESCRIPTION 'Target agent ID',
    task STRING DESCRIPTION 'Task description to send',
    context STRING DESCRIPTION 'Additional context as JSON' DEFAULT NULL
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE payload DOCUMENT;
  DECLARE result DOCUMENT;
  
  SET payload = {
    'task': task
  };
  
  IF context IS NOT NULL THEN
    SET payload['context'] = context;
  END IF;
  
  SET result = HTTP_POST('/api/agent_builder/a2a/' || agent_id, payload, {
    'kbn-xsrf': 'true',
    'Content-Type': 'application/json'
  });
  RETURN result;
END SKILL;
