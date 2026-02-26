---
name: data-management
description: Manage data lifecycle including ILM policies, data streams, snapshots, and ingest pipelines. Use when the user needs to configure data retention, backup data, or set up data processing.
---

# Data Management

This skill enables you to manage the entire data lifecycle in Elasticsearch including ILM, data streams, snapshots, and ingest pipelines.

## When to Use

- User needs to configure **data retention** or **ILM policies**
- User wants to **backup** or **restore** data
- User needs to set up **data streams**
- User wants to configure **ingest pipelines**
- User asks about **snapshot** or **restore** operations

## Index Lifecycle Management (ILM)

| Function | Description | Example |
|----------|-------------|---------|
| `ES_ILM_GET_POLICY(policy?)` | Get ILM policies | `ES_ILM_GET_POLICY('logs-policy')` |
| `ES_ILM_PUT_POLICY(name, policy)` | Create/update policy | See below |
| `ES_ILM_DELETE_POLICY(name)` | Delete policy | `ES_ILM_DELETE_POLICY('old-policy')` |
| `ES_ILM_EXPLAIN(index)` | Explain index state | `ES_ILM_EXPLAIN('logs-*')` |
| `ES_ILM_RETRY(index)` | Retry failed step | `ES_ILM_RETRY('logs-000001')` |
| `ES_ILM_STATUS()` | Get ILM status | `ES_ILM_STATUS()` |
| `ES_ILM_START()` | Start ILM | `ES_ILM_START()` |
| `ES_ILM_STOP()` | Stop ILM | `ES_ILM_STOP()` |

### Create ILM Policy

```sql
DECLARE result DOCUMENT;
SET result = ES_ILM_PUT_POLICY('logs-30-day-retention', {
    'policy': {
        'phases': {
            'hot': {
                'min_age': '0ms',
                'actions': {
                    'rollover': {
                        'max_primary_shard_size': '50gb',
                        'max_age': '1d'
                    }
                }
            },
            'warm': {
                'min_age': '7d',
                'actions': {
                    'shrink': {'number_of_shards': 1},
                    'forcemerge': {'max_num_segments': 1}
                }
            },
            'cold': {
                'min_age': '14d',
                'actions': {
                    'searchable_snapshot': {
                        'snapshot_repository': 'my-repo'
                    }
                }
            },
            'delete': {
                'min_age': '30d',
                'actions': {
                    'delete': {}
                }
            }
        }
    }
});
```

### Check Index Lifecycle Status

```sql
DECLARE explain DOCUMENT;
SET explain = ES_ILM_EXPLAIN('logs-*');

FOR index_name IN DOCUMENT_KEYS(explain['indices']) LOOP
    DECLARE idx DOCUMENT;
    SET idx = explain['indices'][index_name];
    
    PRINT 'Index: ' || index_name;
    PRINT '  Phase: ' || idx['phase'];
    PRINT '  Age: ' || idx['age'];
    
    IF idx['step'] = 'ERROR' THEN
        PRINT '  ERROR: ' || idx['step_info']['reason'];
    END IF;
END LOOP;
```

## Data Streams

| Function | Description | Example |
|----------|-------------|---------|
| `ES_CREATE_DATA_STREAM(name)` | Create data stream | `ES_CREATE_DATA_STREAM('logs-app')` |
| `ES_DELETE_DATA_STREAM(name)` | Delete data stream | `ES_DELETE_DATA_STREAM('logs-app')` |
| `ES_GET_DATA_STREAM(name?)` | Get data stream info | `ES_GET_DATA_STREAM('logs-*')` |
| `ES_DATA_STREAM_STATS(name?)` | Get statistics | `ES_DATA_STREAM_STATS('logs-*')` |

### List Data Streams with Stats

```sql
DECLARE streams DOCUMENT;
SET streams = ES_GET_DATA_STREAM('*');

FOR ds IN streams['data_streams'] LOOP
    PRINT 'Data Stream: ' || ds['name'];
    PRINT '  Backing Indices: ' || ARRAY_LENGTH(ds['indices']);
    PRINT '  Generation: ' || ds['generation'];
    PRINT '  Status: ' || ds['status'];
END LOOP;
```

## Snapshots & Restore

| Function | Description | Example |
|----------|-------------|---------|
| `ES_GET_REPOSITORIES(repo?)` | List repositories | `ES_GET_REPOSITORIES('_all')` |
| `ES_CREATE_REPOSITORY(name, config)` | Create repository | See below |
| `ES_DELETE_REPOSITORY(name)` | Delete repository | `ES_DELETE_REPOSITORY('old-repo')` |
| `ES_GET_SNAPSHOTS(repo, snapshot?)` | List snapshots | `ES_GET_SNAPSHOTS('my-repo', '_all')` |
| `ES_CREATE_SNAPSHOT(repo, name, config?)` | Create snapshot | See below |
| `ES_DELETE_SNAPSHOT(repo, name)` | Delete snapshot | `ES_DELETE_SNAPSHOT('repo', 'snap-1')` |
| `ES_RESTORE_SNAPSHOT(repo, name, config?)` | Restore snapshot | See below |
| `ES_SNAPSHOT_STATUS(repo?, snapshot?)` | Get status | `ES_SNAPSHOT_STATUS('repo', 'snap-1')` |

### Create Snapshot Repository (S3)

```sql
DECLARE result DOCUMENT;
SET result = ES_CREATE_REPOSITORY('s3-backup', {
    'type': 's3',
    'settings': {
        'bucket': 'my-es-backups',
        'region': 'us-east-1',
        'base_path': 'production'
    }
});
```

### Create a Snapshot

```sql
DECLARE snapshot DOCUMENT;
SET snapshot = ES_CREATE_SNAPSHOT('s3-backup', 'daily-' || CURRENT_DATE(), {
    'indices': 'logs-*,metrics-*',
    'ignore_unavailable': TRUE,
    'include_global_state': FALSE
});

PRINT 'Snapshot state: ' || snapshot['snapshot']['state'];
```

### Restore from Snapshot

```sql
DECLARE result DOCUMENT;
SET result = ES_RESTORE_SNAPSHOT('s3-backup', 'daily-2024-01-15', {
    'indices': 'logs-production-*',
    'rename_pattern': '(.+)',
    'rename_replacement': 'restored_$1'
});
```

## Ingest Pipelines

| Function | Description | Example |
|----------|-------------|---------|
| `ES_GET_PIPELINE(id?)` | Get pipelines | `ES_GET_PIPELINE('logs-pipeline')` |
| `ES_PUT_PIPELINE(id, config)` | Create/update pipeline | See below |
| `ES_DELETE_PIPELINE(id)` | Delete pipeline | `ES_DELETE_PIPELINE('old-pipe')` |
| `ES_SIMULATE_PIPELINE(id?, docs, pipeline?)` | Test pipeline | See below |
| `ES_GROK_PATTERNS()` | Get grok patterns | `ES_GROK_PATTERNS()` |

### Create Ingest Pipeline

```sql
DECLARE result DOCUMENT;
SET result = ES_PUT_PIPELINE('logs-enrichment', {
    'description': 'Enrich log data',
    'processors': [
        {
            'grok': {
                'field': 'message',
                'patterns': ['%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} %{GREEDYDATA:msg}']
            }
        },
        {
            'date': {
                'field': 'timestamp',
                'formats': ['ISO8601']
            }
        },
        {
            'geoip': {
                'field': 'client.ip',
                'target_field': 'client.geo'
            }
        },
        {
            'user_agent': {
                'field': 'user_agent.original'
            }
        }
    ]
});
```

### Test Pipeline

```sql
DECLARE test_result DOCUMENT;
SET test_result = ES_SIMULATE_PIPELINE('logs-enrichment', [
    {'_source': {'message': '2024-01-15T10:30:00Z ERROR Connection failed'}}
], NULL);

PRINT test_result['docs'][0]['doc']['_source'];
```

## Pre-built Skills (Moltler)

| Skill | Description |
|-------|-------------|
| `RUN SKILL ilm_explain_status(index)` | ILM status report |
| `RUN SKILL create_snapshot_backup(repo, indices)` | Create backup |
| `RUN SKILL list_snapshots(repo)` | List all snapshots |
| `RUN SKILL cleanup_old_snapshots(repo, keep_count)` | Retention cleanup |

## Best Practices

1. **Always test ILM policies** on a small dataset first
2. **Use data streams** for time-series data
3. **Schedule snapshots** regularly (SLM is preferred)
4. **Test restores** periodically to verify backups work
5. **Use pipeline versioning** to track changes
