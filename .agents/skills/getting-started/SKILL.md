---
name: getting-started
description: >
  Quick start guide for Elasticsearch capabilities. Use when the user is new,
  asks "what can you do with Elasticsearch", wants to see available functions,
  or needs help getting oriented with Moltler skills.
---

# Getting Started

320+ Elasticsearch functions available. Just ask.

## Try These

| Ask | Result |
|-----|--------|
| "Check cluster health" | `ES_CLUSTER_HEALTH()` |
| "Find errors in the last hour" | ES|QL query on logs |
| "Hunt for IP 192.168.1.100" | Security event search |
| "Summarize today's incidents" | LLM-powered analysis |
| "Send Slack alert" | `SLACK_SEND()` |

## Capability Overview

| Category | Examples |
|----------|----------|
| Documents | Index, Get, Update, Delete, Bulk, Reindex |
| Search | ES\|QL, Full-text, Aggregations, Vector search |
| Cluster | Health, Stats, Nodes, Tasks, Settings |
| Observability | Logs, Metrics, Traces, APM |
| Security | Users, Roles, API Keys, Threat Hunting |
| ML | Anomaly Detection, Inference, Embeddings |
| Data | ILM, Snapshots, Data Streams, Pipelines |
| Alerting | Rules, Connectors, Actions |
| Integrations | Slack, PagerDuty, AWS, Kubernetes, CI/CD |

## Connection

```bash
ES_URL=http://localhost:9200
ES_USERNAME=elastic
ES_PASSWORD=your-password
# Or: ES_API_KEY=...
```

## Next Steps

See specific skills: `managing-documents`, `searching-data`, `analyzing-observability`, etc.
