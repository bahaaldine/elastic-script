# Moltler

**Pre-built skills for Elasticsearch.** Solve real problems in seconds.

[![Skills](https://img.shields.io/badge/skills-155-blue)](hub/)
[![Solutions](https://img.shields.io/badge/solutions-observability%20%7C%20security%20%7C%20search-green)]()
[![MCP](https://img.shields.io/badge/MCP-compatible-purple)]()

---

## What is Moltler?

Moltler is a collection of **skills** that run directly on your Elasticsearch data. Instead of writing queries from scratch, use pre-built solutions that leverage Elasticsearch's full power - search, aggregations, semantic search, and ML.

```
┌─────────────────────────────────────────────────────────────────┐
│  "Find all errors in production"                                │
│                          ↓                                      │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  RUN SKILL get_recent_errors(service => 'api-gateway')  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                          ↓                                      │
│  [{"level": "ERROR", "message": "Connection timeout", ...}]    │
└─────────────────────────────────────────────────────────────────┘
```

---

## Quick Start

```bash
# 1. Clone and start
git clone --recurse-submodules https://github.com/bahaaldine/elastic-script.git
cd elastic-script
./scripts/quick-start.sh

# 2. Install skills (155 available)
cd hub && ./moltler-cli.sh install --all

# 3. Run the demo
./scripts/demo.sh
```

---

## See It Work

**Find recent errors:**
```bash
curl -u elastic-admin:elastic-password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "RUN SKILL get_recent_errors()"}'
```

**Hunt for a suspicious IP:**
```bash
curl -u elastic-admin:elastic-password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "RUN SKILL hunt_ioc(ioc => '\''192.168.1.100'\'')"}'
```

**Get top search queries:**
```bash
curl -u elastic-admin:elastic-password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "RUN SKILL get_top_queries(limit => 10)"}'
```

---

## Solutions

### Observability
Investigate incidents, analyze logs, monitor services.

| Skill | What it does |
|-------|--------------|
| `get_recent_errors` | Find ERROR logs with context |
| `get_slow_transactions` | Find slow APM transactions |
| `correlate_logs` | Trace logs across services |
| `count_logs_by_level` | Error rate trends |

[**Full Observability Guide →**](docs/solutions/observability.md)

### Security
Hunt threats, investigate alerts, assess risk.

| Skill | What it does |
|-------|--------------|
| `hunt_ioc` | Search for IP, hash, domain |
| `get_risky_users` | Find high-risk users |
| `get_process_events` | Host process activity |
| `search_security_events` | Full-text security search |

[**Full Security Guide →**](docs/solutions/security.md)

### Search
Query documents, aggregations, semantic search.

| Skill | What it does |
|-------|--------------|
| `semantic_search` | Search by meaning |
| `top_values` | Terms aggregation |
| `fuzzy_search` | Typo-tolerant search |
| `get_search_analytics` | Query analytics |

[**Full Search Guide →**](docs/solutions/search.md)

---

## Why Moltler?

**Your data is already in Elasticsearch.** Logs, metrics, traces, security events - they're all there.

**Elasticsearch's power is built-in.** Skills leverage ES|QL, aggregations, semantic search, and ML - you don't need to learn it all.

**AI agents can use skills.** The MCP endpoint (`/_escript/mcp`) exposes all skills to AI assistants.

**The community can contribute.** Built a useful skill? Share it with everyone via MoltlerHub.

---

## CLI Reference

```bash
# List available skills in the hub
./hub/moltler-cli.sh list
./hub/moltler-cli.sh list --category security

# Install skills
./hub/moltler-cli.sh install --all
./hub/moltler-cli.sh install get-recent-errors

# List installed skills
./hub/moltler-cli.sh installed

# Run a skill
./hub/moltler-cli.sh run get-recent-errors

# Search for skills
./hub/moltler-cli.sh search "error"
```

---

## For AI Agents (MCP)

Skills are exposed via the Model Context Protocol:

```bash
# List available tools
curl http://localhost:9200/_escript/mcp \
  -d '{"jsonrpc": "2.0", "method": "tools/list", "id": 1}'

# Call a tool
curl http://localhost:9200/_escript/mcp \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {"name": "get_recent_errors", "arguments": {"limit": 5}},
    "id": 1
  }'
```

Your AI assistant can now query your Elasticsearch data using natural language.

---

## Create Your Own Skill

```sql
CREATE SKILL my_custom_skill
  VERSION '1.0.0'
  DESCRIPTION 'Describe what it does and when to use it'
  (param1 STRING DEFAULT 'logs-*')
  RETURNS ARRAY
BEGIN
  RETURN ESQL_QUERY('FROM ' || param1 || ' | WHERE level == "ERROR" | LIMIT 10');
END SKILL;
```

[**Creating Skills Guide →**](docs/skills/creating-skills.md)

---

## Resources

| Resource | Description |
|----------|-------------|
| [**Documentation**](docs/index.md) | Full documentation |
| [**MoltlerHub**](hub/) | Browse 155 skills |
| [**Contributing**](hub/CONTRIBUTING.md) | Add your skill |
| [**Language Reference**](LANGUAGE_REFERENCE.md) | EScript syntax |

---

## License

Elastic-2.0
