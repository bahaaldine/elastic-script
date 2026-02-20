# Moltler

**The skills framework for Elasticsearch.** Build, share, and run skills on your data.

---

## What is Moltler?

Moltler is a **framework for building skills** - reusable operations that run directly on your Elasticsearch data. Create your own skills, share them with the community, and leverage 155+ ready-to-use skills.

```
┌─────────────────────────────────────────────────────────────────┐
│                        MOLTLER FRAMEWORK                         │
│                                                                  │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐       │
│  │    BUILD     │    │    SHARE     │    │     RUN      │       │
│  │              │    │              │    │              │       │
│  │ CREATE SKILL │───▶│  MoltlerHub  │───▶│  RUN SKILL   │       │
│  │ simple SQL   │    │  community   │    │  REST / MCP  │       │
│  └──────────────┘    └──────────────┘    └──────────────┘       │
│                              │                                   │
│                              ▼                                   │
│                      ┌──────────────┐                           │
│                      │ Elasticsearch │                           │
│                      │  your data    │                           │
│                      └──────────────┘                           │
└─────────────────────────────────────────────────────────────────┘
```

**Why build skills on Elasticsearch?**

- **Your data is already there** - Logs, metrics, traces, security events
- **Elasticsearch's power** - Search, aggregations, semantic, ML built-in
- **No data movement** - Skills run where the data lives
- **AI-ready** - Skills are discoverable by AI agents via MCP
- **Community** - Share skills, learn from others, contribute back

---

## Pick Your Solution

<div class="grid cards" markdown>

-   :material-eye:{ .lg .middle } **Observability**

    Investigate incidents, analyze logs, monitor services.

    [:octicons-arrow-right-24: Get Started](solutions/observability.md)

    ```sql
    RUN SKILL get_recent_errors();
    RUN SKILL get_slow_transactions(service => 'api');
    RUN SKILL correlate_logs(trace_id => 'abc123');
    ```

-   :material-shield:{ .lg .middle } **Security**

    Hunt threats, investigate alerts, assess risk.

    [:octicons-arrow-right-24: Get Started](solutions/security.md)

    ```sql
    RUN SKILL hunt_ioc(ioc => '192.168.1.100');
    RUN SKILL get_risky_users(threshold => 70);
    RUN SKILL search_security_events(query => 'failed login');
    ```

-   :material-magnify:{ .lg .middle } **Search**

    Query documents, build aggregations, semantic search.

    [:octicons-arrow-right-24: Get Started](solutions/search.md)

    ```sql
    RUN SKILL semantic_search(query => 'pricing info');
    RUN SKILL top_values(field => 'category', limit => 10);
    RUN SKILL get_search_analytics();
    ```

</div>

---

## 30-Second Demo

**1. Start Moltler** (one command)
```bash
./scripts/quick-start.sh
```

**2. Run a skill** (instant value)
```bash
curl -s -u elastic-admin:elastic-password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "RUN SKILL get_recent_errors()"}' | jq
```

**3. See results**
```json
{
  "result": [
    {"level": "ERROR", "service": "api-gateway", "message": "Connection timeout"},
    {"level": "ERROR", "service": "payment", "message": "Invalid card number"}
  ]
}
```

That's it. No configuration, no learning curve, just answers.

---

## How It Works

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   You/AI     │────▶│    Skill     │────▶│ Elasticsearch│
│              │     │              │     │              │
│ "Find errors"│     │ get_recent_  │     │ ES|QL query  │
│              │     │ errors()     │     │ on your data │
└──────────────┘     └──────────────┘     └──────────────┘
```

**Skills are:**
- Pre-built ES|QL queries with parameters
- Stored in Elasticsearch (`.escript_skills` index)
- Callable via REST API or MCP (for AI agents)
- Shareable via MoltlerHub (155+ skills available)

---

## Install Skills

```bash
# Install all 155 skills
cd hub && ./moltler-cli.sh install --all

# Or install by solution
./moltler-cli.sh install --all --category observability
./moltler-cli.sh install --all --category security
./moltler-cli.sh install --all --category search
```

---

## For AI Agents (MCP)

Skills are automatically exposed to AI agents via the Model Context Protocol:

```bash
# List available skills
curl -s http://localhost:9200/_escript/mcp \
  -d '{"jsonrpc": "2.0", "method": "tools/list", "id": 1}'

# Call a skill
curl -s http://localhost:9200/_escript/mcp \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {"name": "get_recent_errors", "arguments": {"limit": 5}},
    "id": 1
  }'
```

Your AI assistant can now query your Elasticsearch data using natural language.

---

## Build Your Own Skills

Once you've used the pre-built skills, create your own:

```sql
CREATE SKILL my_custom_skill
  VERSION '1.0.0'
  DESCRIPTION 'Describe what it does and when to use it'
  (param1 STRING DEFAULT 'logs-*')
  RETURNS ARRAY
BEGIN
  RETURN ESQL_QUERY('FROM ' || param1 || ' | LIMIT 10');
END SKILL;
```

Share with your team or the community via [MoltlerHub](moltlerhub/index.md).

---

---

## The Golden Path

```
┌─────────────────────────────────────────────────────────────────────┐
│                         YOUR JOURNEY                                 │
│                                                                      │
│  ① RUN existing skills                                              │
│     └── 155+ skills for O11y, Security, Search                       │
│                     ↓                                                │
│  ② BUILD your own skills                                            │
│     └── Simple SQL-like syntax, full ES power                        │
│                     ↓                                                │
│  ③ SHARE with the community                                         │
│     └── Publish to MoltlerHub                                        │
│                     ↓                                                │
│  ④ CONNECT to AI assistants                                         │
│     └── Every skill is MCP-ready                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Next Steps

| I want to... | Go here |
|--------------|---------|
| Investigate an incident | [Observability Guide](solutions/observability.md) |
| Hunt for threats | [Security Guide](solutions/security.md) |
| Query my data | [Search Guide](solutions/search.md) |
| Browse all skills | [MoltlerHub](moltlerhub/index.md) |
| Build my own skill | [Creating Skills](skills/creating-skills.md) |
| Connect an AI agent | [MCP Integration](mcp.md) |
| Run the demo | `./scripts/demo.sh` |
