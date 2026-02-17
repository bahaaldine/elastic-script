---
layout: default
title: Getting Started
---

# Getting Started with Moltler

## Prerequisites

- **Node.js** 18+ (for Skills Manager UI and MCP bridge)
- **Java** 21+ (for Elasticsearch plugin)
- **Git**

## Installation

### 1. Clone the Repository

```bash
git clone https://github.com/bahaaldine/elastic-script.git
cd elastic-script
```

### 2. Start the Full Stack

```bash
./scripts/quick-start.sh --moltler
```

This starts:
- **Elasticsearch** with elastic-script plugin on port 9200
- **Skills Manager UI** on port 3000
- Loads demo data and example skills

### 3. Verify Installation

```bash
# Check Elasticsearch
curl -u elastic-admin:elastic-password http://localhost:9200

# List skills
curl -u elastic-admin:elastic-password \
  -X POST http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "SHOW SKILLS"}'
```

## Your First Skill

### 1. Create a Procedure

Procedures contain the actual logic:

```sql
CREATE PROCEDURE hello_user(name STRING)
BEGIN
  PRINT 'Hello, ' || name || '!';
  RETURN {"greeting": "Hello, " || name || "!", "timestamp": CURRENT_TIMESTAMP()};
END PROCEDURE;
```

### 2. Create a Skill

Skills wrap procedures with AI-friendly metadata:

```sql
CREATE SKILL greet_user
  VERSION '1.0'
  DESCRIPTION 'Greet a user by name with a friendly message'
  AUTHOR 'Your Name'
  TAGS ['demo', 'greeting']
  (name STRING DEFAULT 'World')
  RETURNS DOCUMENT
BEGIN
  CALL hello_user(name);
END SKILL;
```

### 3. Invoke via MCP

```bash
curl -u elastic-admin:elastic-password \
  -X POST http://localhost:9200/_escript/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {"name": "greet_user", "arguments": {"name": "Alice"}},
    "id": 1
  }'
```

## Using the Skills Manager UI

1. Open http://localhost:3000
2. Browse skills in the **Skills** tab
3. Click a skill to see its documentation
4. Click **Edit** to modify or **Execute** to run

## Connecting Claude Desktop

1. Install the MCP bridge:

```bash
npm install -g @moltler/mcp-bridge
```

2. Add to Claude Desktop config (`~/Library/Application Support/Claude/claude_desktop_config.json`):

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

3. Restart Claude Desktop

4. Ask Claude: "What tools do you have available?"

## Next Steps

- [Language Reference](./language-reference.md) - Full syntax documentation
- [Built-in Functions](./functions.md) - 106 built-in functions
- [MCP Integration](./mcp.md) - AI agent integration details
