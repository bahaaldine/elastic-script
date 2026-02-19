CREATE SKILL ab_get_agent_card
  VERSION '1.0.0'
  DESCRIPTION 'Get the A2A (Agent-to-Agent) card for an agent. This JSON-LD document describes the agent capabilities for A2A protocol.'
  AUTHOR 'elastic'
  TAGS ['agent-builder', 'a2a', 'ai', 'interop']
  (
    agent_id STRING DESCRIPTION 'Agent ID to get A2A card for'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  SET result = HTTP_GET('/api/agent_builder/a2a/' || agent_id || '.json', {
    'kbn-xsrf': 'true'
  });
  RETURN result;
END SKILL;
