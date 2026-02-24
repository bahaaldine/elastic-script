# Moltler

**The skills framework for Elasticsearch.** Build, share, and run skills on your data.

[![Skills](https://img.shields.io/badge/skills-180+-blue)](https://hub.moltler.dev/skills)
[![Skills Validated](https://img.shields.io/badge/validated-100%25-brightgreen)](tests/skills/)
[![Skill Tests](https://github.com/bahaaldine/moltler/actions/workflows/skill-tests.yml/badge.svg)](https://github.com/bahaaldine/moltler/actions/workflows/skill-tests.yml)
[![MoltlerHub](https://img.shields.io/badge/hub-hub.moltler.dev-purple)](https://hub.moltler.dev)

### Connect to your favorite AI tools

[![Cursor](https://img.shields.io/badge/Cursor-Connect-blue?logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZmlsbD0id2hpdGUiIGQ9Ik0xMiAyTDIgN2wxMCA1IDEwLTV6Ii8+PC9zdmc+)](https://hub.moltler.dev/connect?client=cursor)
[![Claude](https://img.shields.io/badge/Claude-Connect-orange)](https://hub.moltler.dev/connect?client=claude-desktop)
[![VS Code](https://img.shields.io/badge/VS%20Code-Connect-007ACC?logo=visualstudiocode&logoColor=white)](https://hub.moltler.dev/connect?client=vscode)
[![Windsurf](https://img.shields.io/badge/Windsurf-Connect-00D4AA)](https://hub.moltler.dev/connect?client=windsurf)
[![Cline](https://img.shields.io/badge/Cline-Connect-green)](https://hub.moltler.dev/connect?client=cline)
[![Zed](https://img.shields.io/badge/Zed-Connect-yellow)](https://hub.moltler.dev/connect?client=zed)

---

## What is Moltler?

Moltler is a **framework for building skills** that run directly on Elasticsearch. Skills are reusable operations that leverage Elasticsearch's full power - search, aggregations, semantic search, and ML.

- **Build skills** using elastic-script
- **Share skills** via MoltlerHub with the community
- **Run skills** via REST API or AI agents (MCP)

It ships with **155+ ready-to-use skills** for Observability, Security, and Search - because we know Elasticsearch.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        USER / AI AGENT                          │
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

| Component | Purpose |
|-----------|---------|
| **elastic-script plugin** | Elasticsearch plugin that executes skills |
| **Moltler CLI** | Install and manage skills from your terminal |
| **MoltlerHub** | Web portal to browse, search, and discover skills |
| **Moltler MCP** | Bridge for AI agents (Claude, Cursor, etc.) |

---

## Installation

Choose your path:

| I want to... | Do this |
|--------------|---------|
| **Try it out** (demo/dev) | [Quick Start](#quick-start-tryevaluation) - 5 minutes |
| **Install on existing cluster** | [Production Install](docs/getting-started/installation.md#path-b-existing-elasticsearch-cluster) |

---

## Quick Start (Try/Evaluation)

```bash
# 1. Clone and start (builds plugin + starts ES)
git clone --recurse-submodules https://github.com/bahaaldine/moltler.git
cd moltler && ./scripts/quick-start.sh

# 2. Install skills
cd hub && ./moltler-cli.sh install --all

# 3. Run your first skill
./moltler-cli.sh run get-recent-errors

# 4. (Optional) Browse skills on MoltlerHub
cd ../moltler-hub && npm install && npm run dev
```

**Result:** Elasticsearch on `localhost:9200` with 155+ skills installed.

[**Full Installation Guide →**](docs/getting-started/installation.md)

---

## For Existing Clusters

Already have Elasticsearch running? Install the plugin on your nodes:

```bash
# Build the plugin
cd moltler/elastic-script/elasticsearch
./gradlew :x-pack:plugin:elastic-script:build -x test

# Install on each ES node
elasticsearch-plugin install file:///path/to/elastic-script-*.zip

# Restart ES, then install skills
cd ../../hub
export ES_URL="https://your-cluster:9200"
export ES_USER="elastic"
export ES_PASSWORD="your-password"
./moltler-cli.sh install --all
```

[**Full Production Install Guide →**](docs/getting-started/installation.md#path-b-existing-elasticsearch-cluster)

---

## Connect AI Agents (MCP)

Give your AI assistant access to all 180+ Moltler skills. Works with Cursor, Claude Desktop, VS Code, Windsurf, Cline, Zed, and any MCP-compatible client.

**[Open Connect Page →](https://hub.moltler.dev/connect)**

Or manually configure - add to your MCP config:

```json
{
  "mcpServers": {
    "moltler": {
      "command": "python",
      "args": ["<path-to-moltler>/mcp-bridge/moltler_mcp_server.py"],
      "env": {
        "ES_URL": "http://localhost:9200",
        "ES_USER": "elastic-admin",
        "ES_PASSWORD": "elastic-password"
      }
    }
  }
}
```

| Client | Config Location |
|--------|-----------------|
| **Cursor** | `.cursor/mcp.json` |
| **Claude Desktop** | `~/Library/Application Support/Claude/claude_desktop_config.json` |
| **VS Code** | `.vscode/mcp.json` |
| **Claude Code** | `claude mcp add moltler ...` |

After connecting, ask your AI: *"What Moltler skills are available?"*

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
  -d '{"query": "RUN SKILL hunt_ioc WITH ioc = '\''192.168.1.100'\''"}'
```

**Get top search queries:**
```bash
curl -u elastic-admin:elastic-password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "RUN SKILL get_top_queries()"}'
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

| For Users | For Builders |
|-----------|--------------|
| Run skills without learning ES|QL | Build with elastic-script |
| 155+ skills ready to use | Full Elasticsearch power |
| AI agents via MCP | Publish to MoltlerHub |
| Solve real problems fast | Build once, share everywhere |

**Your data is already in Elasticsearch.** Logs, metrics, traces, security events - they're all there.

**Build skills in minutes.** Simple syntax, full power of ES|QL, aggregations, semantic search, and ML.

**Share with the community.** Publish your skills to MoltlerHub. Help others, get feedback, iterate.

**AI-ready by default.** Every skill is automatically exposed to AI agents via MCP.

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

## Quality Assurance

Every skill is validated before release:

```bash
# Validate all skills (no ES needed)
python tests/skills/validate_syntax.py

# Run full test suite (requires ES + plugin)
./tests/skills/run_tests.sh

# Test specific category
./tests/skills/run_tests.sh --category security
```

**All 155 skills pass syntax validation.** The CI/CD pipeline ensures:
- All skills are validated on every PR
- Releases require 100% validation pass rate
- Test results are published with each release

[**Testing Documentation →**](tests/skills/README.md)

---

## Resources

| Resource | Description |
|----------|-------------|
| [**Documentation**](docs/index.md) | Full documentation |
| [**MoltlerHub**](moltler-hub/) | Web portal to browse 155+ skills |
| [**CLI Reference**](hub/) | Moltler CLI for terminal |
| [**Skill Testing**](tests/skills/README.md) | Test framework and validation |
| [**Contributing**](hub/CONTRIBUTING.md) | Add your skill |
| [**Language Reference**](LANGUAGE_REFERENCE.md) | elastic-script syntax |

---

## License

Elastic-2.0
