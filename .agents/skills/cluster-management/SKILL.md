---
name: cluster-management
description: Monitor and manage Elasticsearch cluster health, nodes, tasks, and settings. Use when the user asks about cluster status, node issues, shard allocation, or performance.
---

# Cluster Management

This skill enables you to monitor and manage Elasticsearch clusters, including health checks, node management, and task operations.

## When to Use

- User asks about **cluster health** or status
- User needs to check **node** status or resources
- User wants to view or cancel **running tasks**
- User asks about **shard allocation** problems
- User needs to update **cluster settings**
- User wants to troubleshoot **cluster issues**

## Health & Status Functions

| Function | Description | Example |
|----------|-------------|---------|
| `ES_CLUSTER_HEALTH(index?)` | Get cluster health | `ES_CLUSTER_HEALTH()` |
| `ES_CLUSTER_STATE(metrics)` | Get cluster state | `ES_CLUSTER_STATE('nodes,routing_table')` |
| `ES_CLUSTER_STATS()` | Get cluster statistics | `ES_CLUSTER_STATS()` |
| `ES_CLUSTER_INFO()` | Basic cluster info | `ES_CLUSTER_INFO()` |

## Node Management

| Function | Description | Example |
|----------|-------------|---------|
| `ES_NODES_INFO(node_id?, metrics?)` | Get node info | `ES_NODES_INFO('_all', 'os,jvm')` |
| `ES_NODES_STATS(node_id?, metrics?)` | Get node stats | `ES_NODES_STATS('_all', 'fs,jvm')` |
| `ES_NODE_HOT_THREADS(node_id?)` | Get hot threads | `ES_NODE_HOT_THREADS('_all')` |

## Task Management

| Function | Description | Example |
|----------|-------------|---------|
| `ES_LIST_TASKS(detailed?)` | List running tasks | `ES_LIST_TASKS(TRUE)` |
| `ES_PENDING_TASKS()` | Get pending tasks | `ES_PENDING_TASKS()` |
| `ES_CANCEL_TASK(task_id)` | Cancel a task | `ES_CANCEL_TASK('node1:12345')` |

## Shard Allocation

| Function | Description | Example |
|----------|-------------|---------|
| `ES_ALLOCATION_EXPLAIN(index?, shard?, primary?)` | Explain allocation | `ES_ALLOCATION_EXPLAIN()` |

## Cluster Settings

| Function | Description | Example |
|----------|-------------|---------|
| `ES_CLUSTER_SETTINGS(include_defaults?)` | Get settings | `ES_CLUSTER_SETTINGS(TRUE)` |
| `ES_UPDATE_CLUSTER_SETTINGS(settings)` | Update settings | See below |

## Common Patterns

### Comprehensive Health Check

```sql
DECLARE health DOCUMENT;
DECLARE stats DOCUMENT;

SET health = ES_CLUSTER_HEALTH();
SET stats = ES_CLUSTER_STATS();

PRINT 'Cluster: ' || health['cluster_name'];
PRINT 'Status: ' || health['status'];
PRINT 'Nodes: ' || health['number_of_nodes'];
PRINT 'Active Shards: ' || health['active_shards'];
PRINT 'Unassigned Shards: ' || health['unassigned_shards'];

IF health['status'] != 'green' THEN
    PRINT 'WARNING: Cluster is not healthy!';
    
    -- Get allocation explanation
    DECLARE explain DOCUMENT;
    SET explain = ES_ALLOCATION_EXPLAIN();
    PRINT 'Allocation issue: ' || explain['explanation'];
END IF;
```

### Monitor Node Resources

```sql
DECLARE node_stats DOCUMENT;
SET node_stats = ES_NODES_STATS('_all', 'fs,jvm,os');

FOR node_name IN DOCUMENT_KEYS(node_stats['nodes']) LOOP
    DECLARE node DOCUMENT;
    SET node = node_stats['nodes'][node_name];
    
    PRINT 'Node: ' || node['name'];
    PRINT '  JVM Heap: ' || node['jvm']['mem']['heap_used_percent'] || '%';
    PRINT '  Disk Free: ' || node['fs']['total']['free_in_bytes'];
END LOOP;
```

### Enable/Disable Shard Allocation

```sql
-- Disable allocation (for maintenance)
DECLARE result DOCUMENT;
SET result = ES_UPDATE_CLUSTER_SETTINGS({
    'persistent': {
        'cluster.routing.allocation.enable': 'primaries'
    }
});

-- Re-enable allocation
SET result = ES_UPDATE_CLUSTER_SETTINGS({
    'persistent': {
        'cluster.routing.allocation.enable': 'all'
    }
});
```

### Check Long-Running Tasks

```sql
DECLARE tasks DOCUMENT;
SET tasks = ES_LIST_TASKS(TRUE);

FOR node_id IN DOCUMENT_KEYS(tasks['nodes']) LOOP
    DECLARE node_tasks DOCUMENT;
    SET node_tasks = tasks['nodes'][node_id]['tasks'];
    
    FOR task_id IN DOCUMENT_KEYS(node_tasks) LOOP
        DECLARE task DOCUMENT;
        SET task = node_tasks[task_id];
        
        IF task['running_time_in_nanos'] > 60000000000 THEN  -- > 60 seconds
            PRINT 'Long task: ' || task['action'] || ' running for ' || task['running_time'];
        END IF;
    END LOOP;
END LOOP;
```

## Pre-built Skills (Moltler)

| Skill | Description |
|-------|-------------|
| `RUN SKILL cluster_health_check(index?)` | Comprehensive health report |
| `RUN SKILL node_stats_summary()` | Node resource overview |
| `RUN SKILL explain_allocation()` | Debug shard allocation |

## Troubleshooting Guide

### Red Cluster Status
1. Check `ES_CLUSTER_HEALTH()` for unassigned shards
2. Run `ES_ALLOCATION_EXPLAIN()` to understand why
3. Check disk space with `ES_NODES_STATS('_all', 'fs')`

### Yellow Cluster Status
- Usually means replicas can't be allocated
- Check if you have enough nodes for replica count
- Consider reducing replica count for dev clusters

### High JVM Heap
1. Check heap with `ES_NODES_STATS('_all', 'jvm')`
2. Look for nodes with `heap_used_percent > 85%`
3. Consider scaling or optimizing queries
