---
layout: default
title: Moltler - elastic-script
---

# Moltler / elastic-script

**Procedural scripting language that runs inside Elasticsearch**

Moltler enables you to create **AI-ready skills** that wrap Elasticsearch operations, making them easily discoverable and invocable by AI agents via the Model Context Protocol (MCP).

## Quick Start

```bash
# Clone and start
git clone https://github.com/bahaaldine/moltler.git
cd elastic-script
./scripts/quick-start.sh --moltler

# Access:
# - Skills Manager UI: http://localhost:3000
# - Elasticsearch: http://localhost:9200
# - MCP Endpoint: http://localhost:9200/_escript/mcp
```

## Features

### Skills for AI Agents

Create skills that AI agents can discover and invoke:

```sql
CREATE SKILL analyze_logs
  VERSION '1.0'
  DESCRIPTION 'Analyze application logs for errors and patterns'
  AUTHOR 'DevOps Team'
  TAGS ['logs', 'monitoring']
  (index_name STRING DEFAULT 'logs-*')
  RETURNS DOCUMENT
BEGIN
  DECLARE results ARRAY;
  SET results = ESQL_QUERY('FROM ' || index_name || ' | WHERE level = "ERROR" | LIMIT 10');
  RETURN {"errors": results, "count": ARRAY_LENGTH(results)};
END SKILL;
```

### MCP Integration

Skills are automatically exposed via the Model Context Protocol:

```bash
# List available tools
curl -X POST http://localhost:9200/_escript/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc": "2.0", "method": "tools/list", "id": 1}'

# Invoke a skill
curl -X POST http://localhost:9200/_escript/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {"name": "analyze_logs", "arguments": {"index_name": "logs-*"}},
    "id": 2
  }'
```

### Claude Desktop Integration

Connect Claude to your Elasticsearch skills:

```json
{
  "mcpServers": {
    "moltler": {
      "command": "npx",
      "args": ["@moltler/mcp-bridge", "--es-url", "http://localhost:9200"]
    }
  }
}
```

## Documentation

- [Getting Started](./getting-started.md)
- [Language Reference](./language-reference.md)
- [Built-in Functions](./functions.md)
- [MCP Integration](./mcp.md)
- [Skills Manager UI](./ui.md)
- [API Reference](./api.md)

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  AI Agent       │────▶│  MCP Endpoint   │────▶│  Skills         │
│  (Claude, etc.) │     │  /_escript/mcp  │     │  (Procedures)   │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                                                        │
                        ┌─────────────────┐             │
                        │  Skills Manager │             │
                        │  UI (React)     │             │
                        └─────────────────┘             ▼
                                                ┌─────────────────┐
                                                │  Elasticsearch  │
                                                │  (ES|QL, etc.)  │
                                                └─────────────────┘
```

## License

Elastic License 2.0
