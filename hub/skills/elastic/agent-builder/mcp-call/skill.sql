CREATE SKILL ab_mcp_call
  VERSION '1.0.0'
  DESCRIPTION 'Call the Agent Builder MCP server endpoint. This enables MCP-based tool discovery and execution through Agent Builder.'
  AUTHOR 'elastic'
  TAGS ['agent-builder', 'mcp', 'ai', 'tools']
  (
    method STRING DESCRIPTION 'MCP method: tools/list, tools/call, etc.',
    params STRING DESCRIPTION 'JSON parameters for the MCP call' DEFAULT '{}'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE payload DOCUMENT;
  DECLARE result DOCUMENT;
  
  SET payload = {
    'jsonrpc': '2.0',
    'id': 1,
    'method': method,
    'params': params
  };
  
  SET result = HTTP_POST('/api/agent_builder/mcp', payload, {
    'kbn-xsrf': 'true',
    'Content-Type': 'application/json'
  });
  RETURN result;
END SKILL;
