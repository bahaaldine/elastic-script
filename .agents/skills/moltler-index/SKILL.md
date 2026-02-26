---
name: moltler-index
description: Index of all Moltler/elastic-script capabilities. Use this skill first to understand what skills are available and which one to use for a given task.
---

# Moltler Skills Index

This is the master index of all Moltler/elastic-script skills. Use this to find the right skill for your task.

## Quick Reference

| Task Category | Skill to Use |
|--------------|--------------|
| Index/Get/Update/Delete documents | `elasticsearch-ops` |
| Search, query, or aggregate data | `search-query` |
| Check cluster health or manage nodes | `cluster-management` |
| Find errors, analyze logs/metrics | `observability` |
| Manage users, roles, API keys | `security-ops` |
| Use ML models or LLMs | `ml-inference` |
| Manage ILM, snapshots, pipelines | `data-management` |
| Create alerts, send notifications | `alerting-response` |
| AWS, K8s, CI/CD integrations | `integrations` |

## Available Skills

### elasticsearch-ops
**When to use:** CRUD operations on documents, index management, bulk operations
- Index, get, update, delete documents
- Create/delete indices
- Bulk operations and reindexing
- [View skill →](./elasticsearch-ops/SKILL.md)

### search-query
**When to use:** Finding data, running searches, analytics
- ES|QL queries
- Full-text search
- Aggregations and analytics
- Vector/semantic search
- [View skill →](./search-query/SKILL.md)

### cluster-management
**When to use:** Cluster health, node management, troubleshooting
- Cluster health and stats
- Node information
- Task management
- Shard allocation issues
- [View skill →](./cluster-management/SKILL.md)

### observability
**When to use:** Troubleshooting, log analysis, performance issues
- Find errors in logs
- Analyze metrics
- Trace requests
- APM data analysis
- [View skill →](./observability/SKILL.md)

### security-ops
**When to use:** Access control, authentication, security events
- User and role management
- API key creation/revocation
- Permission checking
- Security event investigation
- [View skill →](./security-ops/SKILL.md)

### ml-inference
**When to use:** AI/ML capabilities, embeddings, LLM integration
- Anomaly detection
- Text embeddings
- NLP (classification, NER)
- LLM chat and summarization
- [View skill →](./ml-inference/SKILL.md)

### data-management
**When to use:** Data lifecycle, backups, data processing
- ILM policies
- Data streams
- Snapshots and restore
- Ingest pipelines
- [View skill →](./data-management/SKILL.md)

### alerting-response
**When to use:** Monitoring, notifications, incident response
- Create alert rules
- Send Slack/PagerDuty notifications
- SLO management
- Automated incident response
- [View skill →](./alerting-response/SKILL.md)

### integrations
**When to use:** External service integration, automation
- AWS (Lambda, SSM, ASG)
- Kubernetes
- CI/CD (GitHub, Jenkins)
- Terraform Cloud
- [View skill →](./integrations/SKILL.md)

## How to Execute

All skills use elastic-script, which runs inside Elasticsearch. There are several ways to execute:

### 1. Direct API Call

```bash
curl -X POST "localhost:9200/_escript" \
  -H "Content-Type: application/json" \
  -d '{"query": "ES_CLUSTER_HEALTH()"}'
```

### 2. Jupyter Notebook

The project includes a Jupyter kernel. Start the notebook server and use the `plesql` kernel.

### 3. Pre-built Skills (RUN SKILL)

```sql
RUN SKILL cluster_health_check();
RUN SKILL get_recent_errors(60, 50);
RUN SKILL search_documents('logs-*', 'error', 10);
```

### 4. Execute Scripts

Skills include scripts in their `scripts/` directories that can be executed directly.

## Environment Setup

Required environment variables for full functionality:

| Variable | Purpose |
|----------|---------|
| `ES_URL` | Elasticsearch URL (default: http://localhost:9200) |
| `ES_USERNAME` | Elasticsearch username |
| `ES_PASSWORD` | Elasticsearch password |
| `ES_API_KEY` | Alternative: API key auth |
| `KIBANA_URL` | Kibana URL for Kibana API functions |
| `OPENAI_API_KEY` | For LLM functions |
| `SLACK_TOKEN` | For Slack notifications |
| `PAGERDUTY_API_KEY` | For PagerDuty integration |

## Getting Help

- **Function reference:** Use `ESCRIPT_FUNCTIONS()` to list all available functions
- **Skill details:** Use `RUN SKILL explain_skill('skill_name')` for details
- **Documentation:** See CLAUDE.md in the project root
