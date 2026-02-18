---
layout: default
title: MCP Integration
---

# MCP Integration

Moltler exposes skills via the **Model Context Protocol (MCP)**, a standard for AI tool integration.

## Overview

The MCP endpoint (`/_escript/mcp`) allows AI agents like Claude to:
- **Discover** available skills via `tools/list`
- **Invoke** skills via `tools/call`

## Endpoint

```
POST http://localhost:9200/_escript/mcp
Content-Type: application/json
Authorization: Basic <credentials>
```

## Supported Methods

### `initialize`

Initialize an MCP session:

```json
{
  "jsonrpc": "2.0",
  "method": "initialize",
  "params": {
    "protocolVersion": "2024-11-05",
    "clientInfo": {"name": "claude", "version": "1.0"}
  },
  "id": 1
}
```

Response:

```json
{
  "jsonrpc": "2.0",
  "result": {
    "protocolVersion": "2024-11-05",
    "serverInfo": {"name": "moltler", "version": "1.0"},
    "capabilities": {"tools": {}}
  },
  "id": 1
}
```

### `tools/list`

List all available skills as MCP tools:

```json
{
  "jsonrpc": "2.0",
  "method": "tools/list",
  "id": 2
}
```

Response:

```json
{
  "jsonrpc": "2.0",
  "result": {
    "tools": [
      {
        "name": "analyze_logs",
        "description": "Analyze application logs for errors and patterns",
        "inputSchema": {
          "type": "object",
          "properties": {
            "index_name": {"type": "string", "description": "Index pattern to search"}
          },
          "required": []
        }
      }
    ]
  },
  "id": 2
}
```

### `tools/call`

Invoke a skill:

```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "analyze_logs",
    "arguments": {
      "index_name": "logs-*"
    }
  },
  "id": 3
}
```

Response:

```json
{
  "jsonrpc": "2.0",
  "result": {
    "content": [
      {
        "type": "text",
        "text": "{\"errors\": [...], \"count\": 5}"
      }
    ]
  },
  "id": 3
}
```

## MCP Bridge for AI Agents

For MCP clients that use stdio transport (Claude Desktop, Cursor, etc.), use the included MCP bridge.

### Quick Setup

```bash
cd mcp-bridge
./setup.sh
```

The setup wizard will configure your preferred AI agent (Claude Desktop, Cursor, or manual).

### Manual Installation

```bash
# Install dependencies
pip3 install httpx

# Run the MCP server
python3 mcp-bridge/moltler_mcp_server.py
```

### Environment Variables

```bash
export ES_URL=http://localhost:9200
export ES_USER=elastic-admin
export ES_PASSWORD=elastic-password
python3 mcp-bridge/moltler_mcp_server.py
```

## Claude Desktop Integration

### Configuration

Run the setup wizard or manually edit `~/Library/Application Support/Claude/claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "moltler-skills": {
      "command": "python3",
      "args": ["/path/to/elastic-script/mcp-bridge/moltler_mcp_server.py"],
      "env": {
        "ES_URL": "http://localhost:9200",
        "ES_USER": "elastic-admin",
        "ES_PASSWORD": "elastic-password"
      }
    }
  }
}
```

### Verification

1. Restart Claude Desktop
2. Look for the hammer icon (🔨) - your Moltler skills will appear there
3. Ask Claude: "What Moltler skills do you have available?"

## Cursor Integration

### Configuration

The setup wizard creates `~/.cursor/mcp.json`:

```json
{
  "mcpServers": {
    "moltler-skills": {
      "command": "python3",
      "args": ["/path/to/elastic-script/mcp-bridge/moltler_mcp_server.py"],
      "env": {
        "ES_URL": "http://localhost:9200",
        "ES_USER": "elastic-admin",
        "ES_PASSWORD": "elastic-password"
      }
    }
  }
}
```

## Example Prompts

Once configured, you can ask your AI agent:

- "Use moltler to check the cluster health"
- "Call the count_logs_by_level skill to see log distribution"
- "Get recent errors using moltler"
- "What metrics are available? Run the metrics_summary skill"

## Creating MCP-Ready Skills

For best results with AI agents:

1. **Clear Descriptions**: Write descriptions that explain what the skill does
2. **Parameter Documentation**: Describe each parameter's purpose
3. **Meaningful Return Types**: Use DOCUMENT for complex returns
4. **Examples**: Provide usage examples in the description

```sql
CREATE SKILL search_products
  VERSION '1.0'
  DESCRIPTION 'Search the product catalog by keyword. Returns matching products with name, price, and stock status. Use this to help users find products or answer questions about inventory.'
  (
    keyword STRING DESCRIPTION 'Search term to match against product names and descriptions',
    max_results NUMBER DEFAULT 10 DESCRIPTION 'Maximum number of products to return'
  )
  RETURNS ARRAY
BEGIN
  RETURN ESQL_QUERY(
    'FROM products | WHERE name LIKE "*' || keyword || '*" | LIMIT ' || max_results
  );
END SKILL;
```

## Error Handling

MCP errors follow JSON-RPC error format:

```json
{
  "jsonrpc": "2.0",
  "error": {
    "code": -32602,
    "message": "Unknown tool: nonexistent_skill"
  },
  "id": 1
}
```

Error codes:
- `-32700`: Parse error
- `-32600`: Invalid request
- `-32601`: Method not found
- `-32602`: Invalid params
- `-32603`: Internal error
