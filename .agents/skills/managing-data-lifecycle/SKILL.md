---
name: managing-data-lifecycle
description: >
  Manage ILM policies, data streams, snapshots, and ingest pipelines. Use when
  configuring data retention, creating backups, restoring data, setting up
  data streams, or configuring data processing pipelines.
---

# Data Lifecycle Management

## ILM (Index Lifecycle Management)

| Function | Example |
|----------|---------|
| `ES_ILM_GET_POLICY(policy?)` | `ES_ILM_GET_POLICY('logs-policy')` |
| `ES_ILM_PUT_POLICY(name, policy)` | Create policy |
| `ES_ILM_DELETE_POLICY(name)` | Delete policy |
| `ES_ILM_EXPLAIN(index)` | `ES_ILM_EXPLAIN('logs-*')` |
| `ES_ILM_RETRY(index)` | Retry failed step |
| `ES_ILM_STATUS()` / `ES_ILM_START()` / `ES_ILM_STOP()` | Control ILM |

### Create Policy

```sql
SET result = ES_ILM_PUT_POLICY('logs-30d', {
    'policy': {
        'phases': {
            'hot': {'actions': {'rollover': {'max_primary_shard_size': '50gb', 'max_age': '1d'}}},
            'warm': {'min_age': '7d', 'actions': {'shrink': {'number_of_shards': 1}}},
            'delete': {'min_age': '30d', 'actions': {'delete': {}}}
        }
    }
});
```

## Data Streams

| Function | Example |
|----------|---------|
| `ES_CREATE_DATA_STREAM(name)` | Create stream |
| `ES_DELETE_DATA_STREAM(name)` | Delete stream |
| `ES_GET_DATA_STREAM(name?)` | Get info |
| `ES_DATA_STREAM_STATS(name?)` | Statistics |

## Snapshots

| Function | Example |
|----------|---------|
| `ES_GET_REPOSITORIES(repo?)` | List repos |
| `ES_CREATE_REPOSITORY(name, config)` | Create repo |
| `ES_GET_SNAPSHOTS(repo, snapshot?)` | List snapshots |
| `ES_CREATE_SNAPSHOT(repo, name, config?)` | Create snapshot |
| `ES_DELETE_SNAPSHOT(repo, name)` | Delete snapshot |
| `ES_RESTORE_SNAPSHOT(repo, name, config?)` | Restore |
| `ES_SNAPSHOT_STATUS(repo?, snapshot?)` | Status |

```sql
-- Create snapshot
SET snapshot = ES_CREATE_SNAPSHOT('s3-backup', 'daily-' || CURRENT_DATE(), {
    'indices': 'logs-*,metrics-*',
    'ignore_unavailable': TRUE
});
```

## Ingest Pipelines

| Function | Example |
|----------|---------|
| `ES_GET_PIPELINE(id?)` | Get pipelines |
| `ES_PUT_PIPELINE(id, config)` | Create pipeline |
| `ES_DELETE_PIPELINE(id)` | Delete pipeline |
| `ES_SIMULATE_PIPELINE(id?, docs, pipeline?)` | Test |
| `ES_GROK_PATTERNS()` | List grok patterns |

## Pre-built Skills

| Skill | Description |
|-------|-------------|
| `RUN SKILL ilm_explain_status(index)` | ILM status |
| `RUN SKILL create_snapshot_backup(repo, indices)` | Create backup |
| `RUN SKILL list_snapshots(repo)` | List snapshots |

## Best Practices

- Test ILM policies on small datasets first
- Use data streams for time-series
- Schedule snapshots regularly
- Test restores periodically
