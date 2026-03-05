---
name: discovering-skills
description: >
  Master index of all Moltler/elastic-script capabilities. Use first when uncertain
  which skill applies, when the user asks "what can you do", or to find the right
  skill for a task involving Elasticsearch, observability, security, ML, or integrations.
---

# Skills Index

Quick reference for selecting the right skill.

| Task | Skill |
|------|-------|
| Index/Get/Update/Delete documents | `managing-documents` |
| Search, query, aggregate data | `searching-data` |
| PromQL metrics (rate, histograms, percentiles) | `querying-metrics-promql` |
| Cluster health, nodes, tasks | `managing-clusters` |
| Logs, metrics, traces, APM | `analyzing-observability` |
| Users, roles, API keys, security events | `managing-security` |
| ML models, embeddings, LLMs | `using-ml-inference` |
| ILM, snapshots, pipelines | `managing-data-lifecycle` |
| Alerts, notifications, SLOs | `alerting-and-responding` |
| AWS, K8s, CI/CD, webhooks | `integrating-services` |

## Execution Methods

```bash
# Direct API
curl -X POST "localhost:9200/_escript" \
  -H "Content-Type: application/json" \
  -d '{"query": "ES_CLUSTER_HEALTH()"}'

# Pre-built skills
RUN SKILL cluster_health_check();
RUN SKILL get_recent_errors(60, 50);
```

## Environment Variables

| Variable | Purpose |
|----------|---------|
| `ES_URL` | Elasticsearch URL |
| `ES_USERNAME` / `ES_PASSWORD` | Auth |
| `ES_API_KEY` | Alternative auth |
| `KIBANA_URL` | Kibana API access |
| `OPENAI_API_KEY` | LLM functions |
| `SLACK_TOKEN` | Slack integration |
| `PAGERDUTY_API_KEY` | PagerDuty integration |
