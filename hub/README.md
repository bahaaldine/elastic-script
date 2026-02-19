# MoltlerHub

The central repository for Moltler skills - pre-built capabilities that AI agents can use to interact with Elasticsearch.

## Quick Start

```bash
# Install all skills
./moltler-cli.sh install --all

# List available skills
./moltler-cli.sh list

# Test skills
./moltler-cli.sh test
```

## What's Inside

### Skills by Category

| Category | Count | Description |
|----------|-------|-------------|
| **Meta** | 5 | Skills that help agents discover and use other skills |
| **Observability** | 10 | Logs, metrics, APM, and service health |
| **Search** | 10 | Document search, aggregations, and exploration |
| **Security** | 5 | Security alerts, threat detection, and investigation |

**Total: 30 skills**

### Meta Skills (Discovery)

| Skill | Description |
|-------|-------------|
| `list_all_skills` | List all available skills |
| `search_skills` | Search for skills by keyword |
| `explain_skill` | Get detailed info about a skill |
| `recommend_skills` | Get recommendations based on goal |
| `get_related_skills` | Find related skills |

### Observability Skills

| Skill | Description |
|-------|-------------|
| `count_logs_by_level` | Count logs by severity (DEBUG, INFO, WARN, ERROR) |
| `get_recent_errors` | Get recent error logs |
| `error_rate` | Calculate error rate percentage |
| `top_error_messages` | Get most frequent errors |
| `logs_by_service` | Log volume by service |
| `search_logs` | Full-text log search |
| `get_metrics_summary` | Metrics statistics |
| `high_cpu_hosts` | Find high CPU hosts |
| `slow_requests` | Find slow API calls |
| `service_health` | Service health summary |

### Search Skills

| Skill | Description |
|-------|-------------|
| `search_documents` | Full-text search |
| `get_document` | Get document by ID |
| `count_documents` | Count documents |
| `aggregate_by_field` | Group and count |
| `get_field_stats` | Field statistics |
| `get_sample_documents` | Sample data |
| `get_unique_values` | Unique field values |
| `recent_documents` | Latest documents |
| `cluster_health` | Cluster status |
| `list_indices` | Available indices |

### Security Skills

| Skill | Description |
|-------|-------------|
| `get_security_alerts` | Recent security alerts |
| `failed_logins` | Failed login attempts |
| `suspicious_activity` | High-risk events |
| `user_activity` | User timeline |
| `threat_summary` | Security posture |

## Skill Package Format

Each skill follows a standard format:

```
skill-name/
├── skill.yaml       # Metadata
├── skill.sql        # Implementation
└── README.md        # Documentation
```

See [SKILL_FORMAT.md](SKILL_FORMAT.md) for full specification.

## CLI Reference

```bash
# Install
moltler-cli.sh install --all              # All skills
moltler-cli.sh install count-logs-by-level # Specific skill

# Explore
moltler-cli.sh list                       # List all
moltler-cli.sh list --category observability
moltler-cli.sh search "error"             # Search

# Test
moltler-cli.sh test                       # Run tests
moltler-cli.sh status                     # Check connection
```

## For AI Agents

All skills are exposed via MCP (Model Context Protocol) at:

```
/_escript/mcp
```

Example using Claude Desktop or Cursor:

```json
{
  "mcpServers": {
    "moltler": {
      "command": "python3",
      "args": ["/path/to/moltler_mcp_server.py"]
    }
  }
}
```

Skills are designed with rich descriptions that help AI agents understand when and how to use them.

## Testing

```bash
# Run comprehensive tests
./tests/test_all_skills.sh
```

## Contributing

1. Create a new skill directory under `skills/elastic/<category>/`
2. Add `skill.yaml`, `skill.sql`, and `README.md`
3. Test with `moltler-cli.sh install <skill-name>`
4. Submit a PR
