# Moltler MCP Bridge

Connect AI agents (Claude Desktop, Cursor, etc.) to your Moltler skills via the Model Context Protocol (MCP).

## Quick Start

```bash
# 1. Make sure Elasticsearch is running with skills loaded
./scripts/quick-start.sh

# 2. Run the setup wizard
cd mcp-bridge
./setup.sh
```

The setup wizard will configure your preferred AI agent.

## What is MCP?

[Model Context Protocol (MCP)](https://modelcontextprotocol.io/) is a standard for connecting AI assistants to external tools and data sources. This bridge exposes your Moltler skills as MCP tools that any compatible AI agent can discover and use.

## Supported AI Agents

### Claude Desktop

1. Run `./setup.sh` and choose option 1
2. Restart Claude Desktop
3. Look for the hammer icon (🔨) - your Moltler skills will appear there

### Cursor

1. Run `./setup.sh` and choose option 2  
2. Restart Cursor
3. Skills are available as tools in the AI chat

### Other MCP Clients

Any MCP-compatible client can connect using:

```json
{
  "mcpServers": {
    "moltler-skills": {
      "command": "python3",
      "args": ["/path/to/mcp-bridge/moltler_mcp_server.py"],
      "env": {
        "ES_URL": "http://localhost:9200",
        "ES_USER": "elastic-admin",
        "ES_PASSWORD": "elastic-password"
      }
    }
  }
}
```

## Usage Examples

Once configured, you can ask your AI agent:

- "Use moltler to check the cluster health"
- "Call the count_logs_by_level skill to see log distribution"
- "Get the recent errors from my system"
- "Run the metrics_summary skill"

The AI will automatically discover available skills and call them with the appropriate arguments.

## Testing

```bash
# Test the MCP endpoint directly
./setup.sh  # Choose option 4

# Or manually:
curl -s -u elastic-admin:elastic-password \
  -X POST http://localhost:9200/_escript/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/list","id":1}' | jq .
```

## Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  Claude/Cursor  │────▶│  MCP Bridge      │────▶│  Elasticsearch  │
│  (AI Agent)     │     │  (Python stdio)  │     │  /_escript/mcp  │
└─────────────────┘     └──────────────────┘     └─────────────────┘
                              │
                              ▼
                        ┌─────────────────┐
                        │  Your Skills    │
                        │  - hello_moltler│
                        │  - metrics_sum  │
                        │  - log_analyzer │
                        └─────────────────┘
```

## Troubleshooting

**Skills not showing up?**
- Make sure Elasticsearch is running: `curl http://localhost:9200`
- Check that skills are loaded: `./scripts/quick-start.sh --load-data`

**Connection refused?**
- Verify ES credentials in the config
- Check firewall settings

**Claude doesn't see the tools?**
- Restart Claude Desktop after configuration
- Check `~/Library/Application Support/Claude/claude_desktop_config.json`
