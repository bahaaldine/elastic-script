# MoltlerHub

The official skill repository for Moltler - exposing Elasticsearch capabilities to AI agents.

## Overview

**130 skills** across **10 categories** providing comprehensive Elasticsearch automation:

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

# Install specific skill
./moltler-cli.sh install observability/get-recent-errors

# List skills
./moltler-cli.sh list

# Search skills
./moltler-cli.sh search "error"

# Check status
./moltler-cli.sh status

# Run tests
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

1. Create a new skill directory under the appropriate category
2. Add `skill.yaml`, `skill.sql`, and optionally `README.md`
3. Test with `./moltler-cli.sh test`
4. Submit a pull request

## License

Elastic-2.0
