# MCP Bridge

The Model Context Protocol (MCP) bridge enables AI agents like Claude Desktop and Cursor to use Moltler skills as tools.

## Overview

MCP is an open standard for connecting AI models to external tools and data sources. The Moltler MCP bridge translates MCP requests into Elasticsearch `/_escript/mcp` API calls.

```
┌─────────────────┐     stdio      ┌──────────────┐     HTTP      ┌───────────────┐
│   AI Agent      │ ◄────────────► │  MCP Bridge  │ ◄───────────► │ Elasticsearch │
│ (Claude/Cursor) │   JSON-RPC     │   (Python)   │   /_escript   │   + Moltler   │
└─────────────────┘                └──────────────┘               └───────────────┘
```

## Quick Setup

Run the interactive setup wizard:

```bash
cd mcp-bridge
./setup.sh
```

This will guide you through configuring:

- **Claude Desktop** - Adds to `claude_desktop_config.json`
- **Cursor** - Adds to `~/.cursor/mcp.json`

## Manual Installation

### Prerequisites

- Python 3.8+
- httpx library

```bash
pip install httpx
```

### Configuration

#### Claude Desktop

Edit `~/Library/Application Support/Claude/claude_desktop_config.json`:

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

#### Cursor

Edit `~/.cursor/mcp.json`:

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

## Using Skills from AI Agents

Once configured, your AI agent can discover and invoke Moltler skills:

### Example Prompts

- "What Moltler skills do you have available?"
- "Use moltler to check the cluster health"
- "Call the count_logs_by_level skill"
- "Get recent errors using the get_recent_errors skill"

### Tool Discovery

The AI agent sees your skills as callable tools:

```
Tools available:
- hello_moltler: Say hello from Moltler
- cluster_health: Check Elasticsearch cluster health
- count_logs_by_level: Count logs grouped by level
- get_recent_errors: Get recent error log entries
```

## How Natural Language Works

You don't need to remember skill names. Just describe what you want in plain English:

| You Say | AI Understands | Skill Called |
|---------|----------------|--------------|
| "How are my logs distributed by severity?" | Wants log level breakdown | `count_logs_by_level` |
| "Is my cluster healthy?" | Wants health status | `check_cluster_health` |
| "Show me recent errors" | Wants error logs | `get_recent_errors` |
| "What metrics do I have?" | Wants metrics summary | `metrics_summary` |

### The Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         You (Human)                             │
│         "How are my logs distributed by severity?"              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    AI Agent (Claude/GPT)                        │
│                                                                 │
│  1. Sees available tools via MCP:                               │
│     - count_logs_by_level: "Count log entries grouped by        │
│                             severity level"                     │
│     - check_cluster_health: "Check cluster health status"       │
│                                                                 │
│  2. Matches your intent to the best tool:                       │
│     "logs distributed by severity" → count_logs_by_level ✓      │
│                                                                 │
│  3. Calls it via MCP automatically                              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    MCP Bridge (Python)                          │
│         Translates MCP JSON-RPC → HTTP to Elasticsearch         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Elasticsearch + Moltler                      │
│                    Executes: RUN SKILL count_logs_by_level      │
└─────────────────────────────────────────────────────────────────┘
```

### The Secret: Good Descriptions

The AI matches your intent to skills based on their **descriptions**. The better your descriptions, the better the AI understands when to use each skill:

```sql
-- Basic description (works, but limited)
CREATE SKILL analyze_logs
  DESCRIPTION 'Analyze logs'
  ...

-- Rich description (AI understands context and use cases)
CREATE SKILL analyze_logs
  DESCRIPTION 'Analyze application logs for errors and patterns. 
               Use this skill when users report issues, ask about 
               error rates, or want to investigate incidents. 
               Returns error counts, trends, and sample messages.'
  ...
```

### Writing AI-Friendly Descriptions

| Element | Example |
|---------|---------|
| **What it does** | "Counts log entries grouped by severity level" |
| **When to use it** | "Use when investigating log distribution or error rates" |
| **What it returns** | "Returns counts for ERROR, WARN, INFO, DEBUG levels" |
| **Example queries** | "How many errors? What's my log breakdown?" |

**Full example:**

```sql
CREATE SKILL count_logs_by_level
  DESCRIPTION 'Count log entries grouped by severity level (ERROR, WARN, INFO, DEBUG). Use this skill when users ask about log distribution, error rates, or want to understand their logging patterns. Returns an array of counts per level.'
  (
    index_pattern STRING DEFAULT 'logs-*' 
      DESCRIPTION 'Index pattern to search (e.g., logs-production-*)'
  )
  RETURNS ARRAY
BEGIN
  RETURN ESQL_QUERY('FROM ' || index_pattern || ' | STATS count=COUNT() BY level');
END SKILL;
```

With this description, the AI will correctly invoke this skill when you ask:

- "How many errors do I have?"
- "What's the breakdown of my logs?"
- "Show me log levels distribution"
- "Are there more warnings than errors?"

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `ES_URL` | Elasticsearch URL | `http://localhost:9200` |
| `ES_USER` | Username | `elastic-admin` |
| `ES_PASSWORD` | Password | `elastic-password` |

## Testing the Bridge

### Test ES Connection

```bash
curl -u elastic-admin:elastic-password http://localhost:9200/_escript/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/list","id":1}'
```

### Test via stdio

```bash
cd mcp-bridge
echo '{"jsonrpc":"2.0","method":"tools/list","id":1}' | python3 moltler_mcp_server.py
```

### Run E2E Tests

```bash
./tests/e2e/test_mcp_bridge.sh
```

## How It Works

### MCP Protocol Flow

1. **Initialize** - AI agent establishes connection
2. **tools/list** - Agent discovers available skills
3. **tools/call** - Agent invokes a specific skill

### Request Translation

The bridge converts MCP `tools/call` to Moltler `RUN SKILL`:

```
MCP Request:                        Elasticsearch:
{                                   POST /_escript/mcp
  "method": "tools/call",           {
  "params": {                         "jsonrpc": "2.0",
    "name": "cluster_health",         "method": "tools/call",
    "arguments": {}                   "params": {
  }                                     "name": "cluster_health",
}                                       "arguments": {}
                                      }
                                    }
```

### Response Format

Skill results are wrapped in MCP content format:

```json
{
  "jsonrpc": "2.0",
  "result": {
    "content": [
      {
        "type": "text",
        "text": "{\"status\": \"green\", \"cluster_name\": \"elasticsearch\"}"
      }
    ]
  },
  "id": 1
}
```

## Creating AI-Friendly Skills

Best practices for skills that work well with AI agents:

```sql
CREATE SKILL analyze_logs
  VERSION '1.0'
  DESCRIPTION 'Analyze application logs for errors and patterns. Use this skill to investigate issues, find error trends, or understand log distribution.'
  (
    index_name STRING DEFAULT 'logs-*' 
      DESCRIPTION 'The index pattern to search (e.g., logs-production-*)',
    hours_back INT DEFAULT 24 
      DESCRIPTION 'How many hours back to search'
  )
  RETURNS DOCUMENT
BEGIN
  -- Clear, structured return value
  RETURN DOCUMENT_MERGE(
    {'analyzed_at': CURRENT_TIMESTAMP()},
    ESQL_QUERY('FROM ' || index_name || ' | STATS count=COUNT() BY level')
  );
END SKILL;
```

### Tips

1. **Descriptive names** - Use clear, action-oriented names
2. **Rich descriptions** - Explain what the skill does and when to use it
3. **Parameter docs** - Document each parameter's purpose
4. **Sensible defaults** - Provide defaults so skills work without arguments
5. **Structured output** - Return documents/arrays that AI can understand

## Troubleshooting

### "Connection refused"

Elasticsearch is not running:

```bash
./scripts/quick-start.sh
```

### "Authentication failed"

Check credentials in your MCP config match Elasticsearch:

```bash
curl -u elastic-admin:elastic-password http://localhost:9200
```

### "No tools found"

Skills may not be loaded. Check the skills endpoint:

```bash
curl -u elastic-admin:elastic-password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "SHOW SKILLS"}'
```

### "Tool not found"

The skill name may not match. List available skills:

```bash
curl -u elastic-admin:elastic-password http://localhost:9200/_escript/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/list","id":1}'
```

## See Also

- [MCP Protocol Specification](https://modelcontextprotocol.io/)
- [Skills Overview](../skills/overview.md)
- [Creating Skills](../skills/creating-skills.md)
