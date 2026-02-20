# MoltlerHub

The official skill repository for Moltler - exposing Elasticsearch capabilities to AI agents.

## Overview

**155 skills** across **13 categories** providing comprehensive Elasticsearch automation:

| Category | Skills | Examples |
|----------|--------|----------|
| **Meta** | 8 | `list_all_skills`, `explain_skill`, `recommend_skills` |
| **Search** | 28 | `search_documents`, `count_documents`, `top_values`, `semantic_search` |
| **Observability** | 22 | `get_recent_errors`, `count_logs_by_level`, `get_slo_status` |
| **APM** | 11 | `list_services`, `get_slow_transactions`, `get_trace` |
| **Metrics** | 6 | `list_hosts`, `get_host_metrics`, `get_memory_pressure` |
| **Security** | 20 | `suspicious_activity`, `hunt_ioc`, `get_user_risk_score` |
| **ML** | 11 | `get_anomalies`, `embed_text`, `classify_text` |
| **Alerting** | 8 | `list_alert_rules`, `get_active_alerts`, `acknowledge_alert` |
| **Cluster** | 10 | `cluster_health`, `list_nodes`, `list_snapshots` |
| **Integrations** | 10 | `send_slack_message`, `create_jira_issue`, `trigger_pagerduty` |
| **Fleet** | 6 | `list_agents`, `get_agent_status`, `list_integrations` |
| **Agent Builder** | 13 | `list_agents`, `create_agent`, `chat`, `list_tools` |
| **Enterprise Search** | 4 | `list_search_apps`, `get_search_analytics`, `get_top_queries` |

## Quick Start

### Install All Skills

```bash
./moltler-cli.sh install --all
```

### Test Skills

```bash
./tests/test_all_skills.sh
```

### List Installed Skills

```bash
./moltler-cli.sh list
```

## Directory Structure

```
hub/
├── skills/
│   └── elastic/
│       ├── meta/           # Discovery & navigation skills
│       ├── observability/  # Log & metrics skills
│       ├── search/         # Query & document skills
│       ├── security/       # SIEM & threat hunting skills
│       ├── apm/            # APM & tracing skills
│       ├── metrics/        # Infrastructure metrics
│       ├── ml/             # Machine learning skills
│       ├── alerting/       # Alert management skills
│       ├── cluster/        # Cluster operations
│       ├── integrations/   # External service integrations
│       ├── fleet/          # Agent management
│       └── enterprise-search/ # Search applications
├── tests/
│   └── test_all_skills.sh  # Comprehensive test suite
├── moltler-cli.sh          # CLI for skill management
├── SKILL_FORMAT.md         # Skill package format spec
└── README.md               # This file
```

## Skill Package Format

Each skill is a directory containing:

```
skill-name/
├── skill.yaml    # Metadata
├── skill.sql     # Implementation
└── README.md     # Documentation (optional)
```

See [SKILL_FORMAT.md](SKILL_FORMAT.md) for full specification.

## CLI Reference

```bash
# Install all skills
./moltler-cli.sh install --all

# Install skills from a category
./moltler-cli.sh install --all --category observability

# Install specific skill
./moltler-cli.sh install get-recent-errors

# Uninstall a skill
./moltler-cli.sh uninstall get-recent-errors

# List available skills in the hub
./moltler-cli.sh list
./moltler-cli.sh list --category security

# List installed skills
./moltler-cli.sh installed

# Search skills
./moltler-cli.sh search "error"

# Run a skill
./moltler-cli.sh run get-recent-errors
./moltler-cli.sh run hunt-ioc "'192.168.1.100'"

# Skill packs
./moltler-cli.sh pack list
./moltler-cli.sh pack show observability_pack

# Test MCP endpoint
./moltler-cli.sh mcp

# Check connection status
./moltler-cli.sh status

# Run skill tests
./moltler-cli.sh test
```

## MCP Integration

Skills are automatically exposed via the MCP endpoint:

```bash
# List available skills
curl -u elastic-admin:elastic-password http://localhost:9200/_escript/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc": "2.0", "id": 1, "method": "tools/list"}'

# Call a skill
curl -u elastic-admin:elastic-password http://localhost:9200/_escript/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/call",
    "params": {
      "name": "get_recent_errors",
      "arguments": {"index_pattern": "logs-*", "limit": 5}
    }
  }'
```

## Example Skills

### Search for Errors
```json
{
  "name": "get_recent_errors",
  "arguments": {
    "index_pattern": "logs-*",
    "limit": 10
  }
}
```

### Get Service Health
```json
{
  "name": "service_health",
  "arguments": {
    "service": "api-gateway"
  }
}
```

### Hunt for IOC
```json
{
  "name": "hunt_ioc",
  "arguments": {
    "ioc": "192.168.1.100",
    "ioc_type": "ip"
  }
}
```

### Create Jira Ticket
```json
{
  "name": "create_jira_issue",
  "arguments": {
    "project": "OPS",
    "summary": "High error rate detected",
    "priority": "high"
  }
}
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for the full guide. Quick steps:

1. Fork the repository
2. Create skill directory: `hub/skills/elastic/<category>/<skill-name>/`
3. Add required files:
   - `skill.yaml` - Metadata (name, version, description, tags)
   - `skill.sql` - EScript implementation with `CREATE SKILL ... END SKILL;`
   - `README.md` - Usage documentation
4. Test locally: `./moltler-cli.sh install <skill-name>`
5. Submit a pull request

## License

Elastic-2.0
