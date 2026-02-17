# Moltler MCP Bridge

Connect AI agents to Elasticsearch elastic-script skills via the Model Context Protocol (MCP).

## What is this?

This is a lightweight bridge that enables AI agents (like Claude Desktop) to use elastic-script skills as tools. It translates MCP's stdio protocol to HTTP calls against the Elasticsearch `/_escript/mcp` endpoint.

## Quick Start

### 1. Start Elasticsearch with elastic-script

```bash
cd elastic-script
./scripts/quick-start.sh --moltler
```

### 2. Configure Claude Desktop

Add to your Claude Desktop config (`~/Library/Application Support/Claude/claude_desktop_config.json` on macOS):

```json
{
  "mcpServers": {
    "moltler": {
      "command": "npx",
      "args": [
        "@moltler/mcp-bridge",
        "--es-url", "http://localhost:9200",
        "--username", "elastic-admin",
        "--password", "elastic-password"
      ]
    }
  }
}
```

### 3. Restart Claude Desktop

Your skills are now available as tools! Try asking Claude:
- "List the available tools"
- "Check the system health"
- "Get user statistics"

## Usage

```bash
# Using npx (recommended)
npx @moltler/mcp-bridge --es-url http://localhost:9200

# Or install globally
npm install -g @moltler/mcp-bridge
moltler-mcp --es-url http://localhost:9200
```

## Options

| Option | Default | Description |
|--------|---------|-------------|
| `--es-url` | `http://localhost:9200` | Elasticsearch URL |
| `--username` | `elastic-admin` | Elasticsearch username |
| `--password` | `elastic-password` | Elasticsearch password |

## Environment Variables

You can also configure via environment variables:

```bash
export ELASTICSEARCH_URL=http://localhost:9200
export ELASTICSEARCH_USERNAME=elastic-admin
export ELASTICSEARCH_PASSWORD=elastic-password
```

## How It Works

```
┌─────────────────┐      stdio       ┌─────────────────┐      HTTP       ┌─────────────────┐
│  Claude Desktop │ ◄──────────────► │  MCP Bridge     │ ◄─────────────► │  Elasticsearch  │
│  (AI Agent)     │      MCP         │  (this package) │   /_escript/mcp │  elastic-script │
└─────────────────┘                  └─────────────────┘                 └─────────────────┘
```

1. Claude Desktop starts the bridge as a subprocess
2. Bridge reads MCP messages from stdin, writes to stdout
3. Bridge forwards requests to Elasticsearch's MCP endpoint
4. Skills are exposed as MCP tools that Claude can invoke

## Creating Skills

Skills are created in Elasticsearch using elastic-script:

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

Then the skill automatically appears as a tool for AI agents.

## License

Elastic License 2.0
