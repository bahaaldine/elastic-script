# MCP Integration

**Connect AI assistants to your Elasticsearch data.** Skills become tools that any MCP-compatible AI can use.

---

## What is MCP?

The [Model Context Protocol](https://modelcontextprotocol.io/) (MCP) is an open standard for AI assistants to discover and use tools. Moltler exposes all installed skills as MCP tools, allowing AI agents to:

- **Discover skills** - List available operations with descriptions
- **Understand parameters** - Get input schemas with validation
- **Call skills** - Execute skills and receive structured results

---

## Quick Start

### 1. List Available Tools

```bash
curl -u elastic-admin:elastic-password http://localhost:9200/_escript/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc": "2.0", "method": "tools/list", "id": 1}'
```

Response:
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "tools": [
      {
        "name": "get_recent_errors",
        "description": "Get recent ERROR level logs from the specified index pattern",
        "inputSchema": {
          "type": "object",
          "properties": {
            "index_pattern": {"type": "string", "default": "logs-*"},
            "limit": {"type": "integer", "default": 20}
          }
        }
      }
    ]
  }
}
```

### 2. Call a Tool

```bash
curl -u elastic-admin:elastic-password http://localhost:9200/_escript/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
      "name": "get_recent_errors",
      "arguments": {"limit": 5}
    },
    "id": 1
  }'
```

Response:
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "[{\"level\": \"ERROR\", \"service\": \"api\", \"message\": \"Connection timeout\"}]"
      }
    ]
  }
}
```

---

## MCP Protocol Support

| Method | Supported | Description |
|--------|-----------|-------------|
| `initialize` | ✅ | Handshake and capability exchange |
| `tools/list` | ✅ | List available skills as tools |
| `tools/call` | ✅ | Execute a skill |
| `resources/list` | ❌ | Not yet implemented |
| `prompts/list` | ❌ | Not yet implemented |

---

## Connecting AI Assistants

### Cursor IDE

Add to your Cursor MCP config (`.cursor/mcp.json`):

```json
{
  "mcpServers": {
    "moltler": {
      "command": "curl",
      "args": [
        "-X", "POST",
        "-u", "elastic-admin:elastic-password",
        "-H", "Content-Type: application/json",
        "http://localhost:9200/_escript/mcp"
      ]
    }
  }
}
```

### Claude Desktop

Add to Claude's MCP settings:

```json
{
  "mcpServers": {
    "moltler": {
      "type": "http",
      "url": "http://localhost:9200/_escript/mcp",
      "headers": {
        "Authorization": "Basic ZWxhc3RpYy1hZG1pbjplbGFzdGljLXBhc3N3b3Jk"
      }
    }
  }
}
```

### Custom Integration

For any MCP-compatible client:

```python
import requests
import json

class MoltlerMCPClient:
    def __init__(self, url="http://localhost:9200/_escript/mcp", auth=("elastic-admin", "elastic-password")):
        self.url = url
        self.auth = auth
    
    def call(self, method, params=None):
        payload = {
            "jsonrpc": "2.0",
            "method": method,
            "id": 1
        }
        if params:
            payload["params"] = params
        
        response = requests.post(self.url, json=payload, auth=self.auth)
        return response.json()
    
    def list_tools(self):
        return self.call("tools/list")
    
    def call_tool(self, name, arguments=None):
        return self.call("tools/call", {"name": name, "arguments": arguments or {}})

# Usage
client = MoltlerMCPClient()

# List skills
tools = client.list_tools()
for tool in tools["result"]["tools"]:
    print(f"- {tool['name']}: {tool['description']}")

# Call a skill
result = client.call_tool("get_recent_errors", {"limit": 5})
print(result["result"]["content"][0]["text"])
```

---

## How AI Agents Use Skills

When connected, an AI assistant can:

**1. Understand available capabilities**
```
AI: "I can query your Elasticsearch data using these skills:
     - get_recent_errors: Find ERROR logs
     - get_slow_transactions: Find slow APM transactions
     - hunt_ioc: Search for suspicious IPs
     ..."
```

**2. Choose the right skill for the task**
```
User: "Are there any errors in production?"

AI: [Internally selects get_recent_errors skill]
    [Calls with arguments: {service: "production"}]
```

**3. Interpret results naturally**
```
AI: "I found 3 errors in the last hour:
     1. API timeout connecting to database (5 occurrences)
     2. Invalid authentication token (2 occurrences)
     3. Rate limit exceeded (1 occurrence)
     
     The database timeout looks like the most critical issue."
```

---

## Security

### Authentication

The MCP endpoint uses the same authentication as other Elasticsearch APIs:

```bash
# Basic auth
curl -u username:password http://localhost:9200/_escript/mcp

# API key
curl -H "Authorization: ApiKey base64_encoded_key" http://localhost:9200/_escript/mcp
```

### Access Control

Skills respect Elasticsearch security:
- Index-level permissions apply to ES|QL queries within skills
- Users only see skills they have permission to execute
- All skill executions are logged

---

## Skill Discovery

Skills include AI-friendly metadata:

```sql
CREATE SKILL analyze_errors
  VERSION '1.0.0'
  DESCRIPTION 'Analyze error patterns and trends. Use when investigating incidents or monitoring error rates.'
  TAGS ['observability', 'logs', 'errors']
  (
    service STRING DESCRIPTION 'Service name to analyze',
    time_range STRING DEFAULT '1h' DESCRIPTION 'Time range (e.g., 1h, 24h, 7d)'
  )
  RETURNS DOCUMENT
BEGIN
  -- skill implementation
END SKILL;
```

The description and parameter descriptions help AI agents understand when and how to use each skill.

---

## Next Steps

- [Browse available skills](moltlerhub/index.md)
- [Create custom skills](skills/creating-skills.md)
- [Solution guides](solutions/observability.md)
