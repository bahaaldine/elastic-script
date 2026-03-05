# Moltler Agent Skills

Agent Skills for AI assistants (Claude, Cursor, etc.) to interact with Elasticsearch via Moltler/elastic-script.

## Installation

Add to your AI agent:

```
https://github.com/bahaaldine/moltler.git
```

Path: `.agents/skills/`

## Available Skills

| Skill | Description |
|-------|-------------|
| `discovering-skills` | Master index - use first when uncertain |
| `getting-started` | Quick start with examples |
| `managing-documents` | Document CRUD, index management |
| `searching-data` | Search, aggregations, ES\|QL |
| `querying-metrics-promql` | PromQL syntax for metrics (rate, histograms) |
| `managing-clusters` | Cluster health, nodes, tasks |
| `analyzing-observability` | Logs, metrics, traces, APM |
| `managing-security` | Users, roles, API keys |
| `using-ml-inference` | ML, embeddings, LLMs |
| `managing-data-lifecycle` | ILM, snapshots, pipelines |
| `alerting-and-responding` | Alerts, Slack, PagerDuty |
| `integrating-services` | AWS, K8s, CI/CD, webhooks |

## Quick Start

After installation, try:

- "Check my cluster health"
- "Find errors in the last hour"
- "What services are throwing exceptions?"
- "Create an alert for high error rate"

## Requirements

Environment variables:

```bash
ES_URL=http://localhost:9200
ES_USERNAME=elastic
ES_PASSWORD=changeme
```

## Skill Structure

Each skill follows the [Claude Skills format](https://platform.claude.com/docs/en/agents-and-tools/agent-skills/best-practices):

```
skill-name/
└── SKILL.md      # Frontmatter (name, description) + instructions
```

## Relationship to Moltler Skills

These Agent Skills guide AI assistants to use:
- **elastic-script functions** (`ES_CLUSTER_HEALTH()`, `ES_SEARCH()`, etc.)
- **Moltler Skills** (`RUN SKILL cluster_health_check()`, etc.)
- **ES|QL queries** for data analysis
