# Moltler Skills

Skills are the building blocks that AI agents use to interact with Elasticsearch. Each skill is a self-contained capability with a clear description, typed parameters, and structured output.

## Available Skills

Moltler ships with **30 pre-built skills** across 4 categories:

| Category | Count | Description |
|----------|-------|-------------|
| [Meta Skills](#meta-skills) | 5 | Help agents discover and use other skills |
| [Observability](#observability-skills) | 10 | Logs, metrics, and service health |
| [Search](#search-skills) | 10 | Document search and data exploration |
| [Security](#security-skills) | 5 | Threat detection and investigation |

## Installing Skills

Use the Moltler CLI to install skills:

```bash
cd hub

# Install all skills
./moltler-cli.sh install --all

# Install a specific skill
./moltler-cli.sh install count-logs-by-level

# List available skills
./moltler-cli.sh list

# Search for skills
./moltler-cli.sh search "error"
```

## Meta Skills

Skills that help AI agents discover and navigate other skills.

| Skill | Description |
|-------|-------------|
| `list_all_skills` | List all available skills with descriptions |
| `search_skills` | Search for skills by keyword or capability |
| `explain_skill` | Get detailed explanation of a specific skill |
| `recommend_skills` | Get recommendations based on your goal |
| `get_related_skills` | Find skills related to another skill |

### Example: Using Meta Skills

```
User: "I need to investigate a production incident"

Agent uses: recommend_skills(goal => "investigate production incident")
Returns: [count_logs_by_level, get_recent_errors, error_rate, service_health]

Agent uses: explain_skill(skill_name => "get_recent_errors")
Returns: Full parameter details and usage
```

## Observability Skills

Skills for logs, metrics, and application monitoring.

| Skill | Description | Key Parameters |
|-------|-------------|----------------|
| `count_logs_by_level` | Count logs by severity level | `index_pattern` |
| `get_recent_errors` | Get recent error logs | `limit`, `service` |
| `error_rate` | Calculate error rate percentage | `index_pattern`, `service` |
| `top_error_messages` | Most frequent error messages | `limit` |
| `logs_by_service` | Log volume breakdown by service | `index_pattern` |
| `search_logs` | Full-text search in logs | `query`, `limit` |
| `get_metrics_summary` | Statistics for system metrics | `metric_name` |
| `high_cpu_hosts` | Find hosts with high CPU | `threshold` |
| `slow_requests` | Find slow API calls | `threshold_ms` |
| `service_health` | Health summary for a service | `service` |

### Example: Error Investigation

```
User: "Show me what's wrong with the system"

Agent uses: count_logs_by_level()
Returns: {INFO: 40, DEBUG: 25, ERROR: 20, WARN: 15}

Agent uses: error_rate()
Returns: {total_logs: 100, error_count: 20, error_rate_percent: 20.0}

Agent uses: top_error_messages(limit => 5)
Returns: Top 5 most frequent errors
```

## Search Skills

Skills for searching, aggregating, and exploring data.

| Skill | Description | Key Parameters |
|-------|-------------|----------------|
| `search_documents` | Full-text search across indices | `query`, `index_pattern` |
| `get_document` | Get document by ID | `index_name`, `doc_id` |
| `count_documents` | Count documents in index | `index_pattern` |
| `aggregate_by_field` | Group and count by field | `field`, `limit` |
| `get_field_stats` | Min/max/avg for numeric field | `field` |
| `get_sample_documents` | Sample documents from index | `limit` |
| `get_unique_values` | Unique values for a field | `field` |
| `recent_documents` | Most recently indexed docs | `limit` |
| `cluster_health` | Cluster health status | - |
| `list_indices` | Available indices | - |

### Example: Data Exploration

```
User: "What data do we have?"

Agent uses: list_indices()
Returns: [logs-sample, metrics-sample, users-sample, ...]

Agent uses: get_sample_documents(index_pattern => "logs-sample", limit => 3)
Returns: Sample documents showing structure

Agent uses: get_unique_values(index_pattern => "logs-sample", field => "service")
Returns: All unique service names
```

## Security Skills

Skills for security monitoring and investigation.

| Skill | Description | Key Parameters |
|-------|-------------|----------------|
| `get_security_alerts` | Recent security alerts | `severity`, `limit` |
| `failed_logins` | Failed login attempts | `group_by` |
| `suspicious_activity` | High-risk events | `limit` |
| `user_activity` | Activity timeline for user | `username` |
| `threat_summary` | Security posture summary | - |

### Example: Security Investigation

```
User: "Are there any security issues?"

Agent uses: threat_summary()
Returns: {total_alerts: 5, critical_alerts: 0, failed_auth: 12}

Agent uses: failed_logins(group_by => "source_ip")
Returns: IPs with most failed login attempts

Agent uses: user_activity(username => "admin")
Returns: All activity for the admin user
```

## Creating Custom Skills

See [Skills Roadmap](skills-roadmap.md) for planned skills and the [Skill Format](../hub/SKILL_FORMAT.md) for creating your own.

## MCP Integration

All skills are automatically exposed via the Model Context Protocol. Configure your AI agent to connect to the Moltler MCP bridge:

```json
{
  "mcpServers": {
    "moltler": {
      "command": "python3",
      "args": ["/path/to/mcp-bridge/moltler_mcp_server.py"]
    }
  }
}
```

The AI agent can then discover and invoke skills using natural language.
