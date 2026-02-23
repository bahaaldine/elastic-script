# Quick Start

Get Moltler running and execute your first skill in 5 minutes.

---

## Prerequisites

- Git
- Java 17+ (for building the Elasticsearch plugin)
- Node.js 18+ (for MoltlerHub)

---

## Step 1: Start Elasticsearch with the Plugin

```bash
git clone --recurse-submodules https://github.com/bahaaldine/moltler.git
cd moltler
./scripts/quick-start.sh
```

This builds the elastic-script plugin and starts Elasticsearch on port 9200.

**Verify it's running:**

```bash
curl -u elastic-admin:elastic-password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "PRINT '\''Hello Moltler!'\''"}'
```

---

## Step 2: Install Skills

```bash
cd hub
./moltler-cli.sh install --all
```

This installs all 155+ skills into your Elasticsearch cluster.

**Verify skills are installed:**

```bash
./moltler-cli.sh installed
```

---

## Step 3: Run Your First Skill

```bash
# Via CLI
./moltler-cli.sh run get-recent-errors

# Or via curl
curl -u elastic-admin:elastic-password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "RUN SKILL get_recent_errors()"}'
```

---

## Step 4: Browse Skills on MoltlerHub

```bash
./scripts/quick-start.sh --hub
# Open http://localhost:3000
```

MoltlerHub lets you:
- Browse all 155+ skills
- Search and filter by category
- View documentation and parameters
- Get install commands

---

## Step 5 (Optional): Connect AI Agents

Skills are exposed via the Model Context Protocol (MCP).

**For Cursor IDE**, add to `.cursor/mcp.json`:

```json
{
  "mcpServers": {
    "moltler": {
      "url": "http://localhost:9200/_escript/mcp",
      "headers": {
        "Authorization": "Basic ZWxhc3RpYy1hZG1pbjplbGFzdGljLXBhc3N3b3Jk"
      }
    }
  }
}
```

Now your AI assistant can run skills using natural language.

---

## What Just Happened?

```
┌─────────────────────────────────────────────────────────────────┐
│                        YOU / AI AGENT                           │
└─────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│  MoltlerHub   │    │  Moltler CLI  │    │  Moltler MCP  │
│  (Web Portal) │    │  (Terminal)   │    │  (AI Bridge)  │
└───────────────┘    └───────────────┘    └───────────────┘
        │                     │                     │
        └─────────────────────┼─────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│               Elasticsearch + elastic-script plugin             │
│                        (Skills Runtime)                         │
└─────────────────────────────────────────────────────────────────┘
```

You now have:
- **Elasticsearch** running with the elastic-script plugin
- **155+ skills** installed and ready to use
- **MoltlerHub** to browse and discover skills
- **MCP endpoint** for AI agents

---

## Common Skills by Solution

### Observability

```bash
# Find recent errors
curl -u elastic-admin:elastic-password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "RUN SKILL get_recent_errors()"}'

# Get slow transactions
curl -u elastic-admin:elastic-password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "RUN SKILL get_slow_transactions()"}'
```

### Security

```bash
# Hunt for an IOC
curl -u elastic-admin:elastic-password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "RUN SKILL hunt_ioc WITH ioc = '\''192.168.1.100'\''"}'

# Get risky users
curl -u elastic-admin:elastic-password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "RUN SKILL get_risky_users()"}'
```

### Search

```bash
# Semantic search
curl -u elastic-admin:elastic-password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "RUN SKILL semantic_search WITH query = '\''pricing information'\''"}'

# Top values
curl -u elastic-admin:elastic-password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "RUN SKILL top_values WITH field = '\''category'\''"}'
```

---

## Next Steps

| I want to... | Go here |
|--------------|---------|
| Browse all skills | [MoltlerHub](../moltlerhub/index.md) or `./moltler-cli.sh list` |
| Build my own skill | [Creating Skills](../skills/creating-skills.md) |
| Learn the language | [Language Reference](../language/overview.md) |
| Connect AI agents | [MCP Integration](../mcp.md) |
| Contribute a skill | [Contributing Guide](../moltlerhub/contributing.md) |
