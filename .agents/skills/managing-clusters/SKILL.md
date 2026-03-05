---
name: managing-clusters
description: >
  Monitor and manage cluster health, nodes, tasks, and settings. Use when checking
  cluster status, troubleshooting node issues, investigating shard allocation problems,
  or managing cluster settings.
---

# Cluster Management

## Health & Status

| Function | Example |
|----------|---------|
| `ES_CLUSTER_HEALTH(index?)` | `ES_CLUSTER_HEALTH()` |
| `ES_CLUSTER_STATE(metrics)` | `ES_CLUSTER_STATE('nodes,routing_table')` |
| `ES_CLUSTER_STATS()` | `ES_CLUSTER_STATS()` |
| `ES_CLUSTER_INFO()` | `ES_CLUSTER_INFO()` |

## Node Management

| Function | Example |
|----------|---------|
| `ES_NODES_INFO(node_id?, metrics?)` | `ES_NODES_INFO('_all', 'os,jvm')` |
| `ES_NODES_STATS(node_id?, metrics?)` | `ES_NODES_STATS('_all', 'fs,jvm')` |
| `ES_NODE_HOT_THREADS(node_id?)` | `ES_NODE_HOT_THREADS('_all')` |

## Task Management

| Function | Example |
|----------|---------|
| `ES_LIST_TASKS(detailed?)` | `ES_LIST_TASKS(TRUE)` |
| `ES_PENDING_TASKS()` | `ES_PENDING_TASKS()` |
| `ES_CANCEL_TASK(task_id)` | `ES_CANCEL_TASK('node1:12345')` |

## Shard Allocation

| Function | Example |
|----------|---------|
| `ES_ALLOCATION_EXPLAIN(index?, shard?, primary?)` | Debug allocation |

## Settings

| Function | Example |
|----------|---------|
| `ES_CLUSTER_SETTINGS(include_defaults?)` | `ES_CLUSTER_SETTINGS(TRUE)` |
| `ES_UPDATE_CLUSTER_SETTINGS(settings)` | Update settings |

## Health Check Pattern

```sql
DECLARE health DOCUMENT;
SET health = ES_CLUSTER_HEALTH();

PRINT 'Status: ' || health['status'];
PRINT 'Nodes: ' || health['number_of_nodes'];
PRINT 'Unassigned: ' || health['unassigned_shards'];

IF health['status'] != 'green' THEN
    DECLARE explain DOCUMENT;
    SET explain = ES_ALLOCATION_EXPLAIN();
    PRINT 'Issue: ' || explain['explanation'];
END IF;
```

## Pre-built Skills

| Skill | Description |
|-------|-------------|
| `RUN SKILL cluster_health_check(index?)` | Full health report |
| `RUN SKILL node_stats_summary()` | Resource overview |
| `RUN SKILL explain_allocation()` | Debug shards |

## Troubleshooting

**Red cluster:** Check `ES_CLUSTER_HEALTH()` → `ES_ALLOCATION_EXPLAIN()` → check disk with `ES_NODES_STATS('_all', 'fs')`

**Yellow cluster:** Usually replica allocation - check node count vs replica settings

**High heap:** Check `ES_NODES_STATS('_all', 'jvm')` for nodes with `heap_used_percent > 85%`
