/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.builtin.elasticsearch;

import org.elasticsearch.xpack.escript.functions.api.FunctionCollectionSpec;
import org.elasticsearch.xpack.escript.functions.api.FunctionCategory;
import org.elasticsearch.xpack.escript.functions.api.FunctionParam;
import org.elasticsearch.xpack.escript.functions.api.FunctionReturn;
import org.elasticsearch.xpack.escript.functions.api.FunctionSpec;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequest;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsRequest;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsResponse;
import org.elasticsearch.action.admin.cluster.settings.ClusterGetSettingsRequest;
import org.elasticsearch.action.admin.cluster.settings.ClusterGetSettingsResponse;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsRequest;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsResponse;
import org.elasticsearch.action.admin.cluster.allocation.ClusterAllocationExplainRequest;
import org.elasticsearch.action.admin.cluster.allocation.ClusterAllocationExplainResponse;
import org.elasticsearch.action.admin.cluster.allocation.TransportClusterAllocationExplainAction;
import org.elasticsearch.action.admin.cluster.reroute.ClusterRerouteRequest;
import org.elasticsearch.action.admin.cluster.reroute.ClusterRerouteResponse;
import org.elasticsearch.action.admin.cluster.node.tasks.list.ListTasksRequest;
import org.elasticsearch.action.admin.cluster.node.tasks.list.ListTasksResponse;
import org.elasticsearch.action.admin.cluster.node.tasks.cancel.CancelTasksRequest;
import org.elasticsearch.action.admin.cluster.node.tasks.cancel.CancelTasksResponse;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch Cluster API functions.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.DATASOURCE,
    description = "Elasticsearch Cluster API functions for monitoring and managing the cluster"
)
public class ClusterFunctions {

    public static void registerAll(ExecutionContext context, Client client) {
        registerClusterHealth(context, client);
        registerClusterState(context, client);
        registerClusterStats(context, client);
        registerClusterSettings(context, client);
        registerUpdateClusterSettings(context, client);
        registerNodesInfo(context, client);
        registerNodesStats(context, client);
        registerNodeHotThreads(context, client);
        registerPendingTasks(context, client);
        registerListTasks(context, client);
        registerCancelTask(context, client);
        registerClusterAllocationExplain(context, client);
        registerClusterReroute(context, client);
        registerRemoteInfo(context, client);
        registerClusterInfo(context, client);
    }

    @FunctionSpec(
        name = "ES_CLUSTER_HEALTH",
        description = "Get cluster health status",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Optional index to check")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Cluster health details"),
        examples = {"ES_CLUSTER_HEALTH()", "ES_CLUSTER_HEALTH('logs-*')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerClusterHealth(ExecutionContext context, Client client) {
        context.declareFunction("ES_CLUSTER_HEALTH",
            List.of(new Parameter("index", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_CLUSTER_HEALTH", (args, listener) -> {
                ClusterHealthRequest request = new ClusterHealthRequest();
                if (args.size() > 0 && args.get(0) != null) {
                    request.indices(args.get(0).toString());
                }
                
                client.admin().cluster().health(request, new ActionListener<ClusterHealthResponse>() {
                    @Override
                    public void onResponse(ClusterHealthResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("cluster_name", response.getClusterName());
                        result.put("status", response.getStatus().name().toLowerCase());
                        result.put("number_of_nodes", response.getNumberOfNodes());
                        result.put("number_of_data_nodes", response.getNumberOfDataNodes());
                        result.put("active_primary_shards", response.getActivePrimaryShards());
                        result.put("active_shards", response.getActiveShards());
                        result.put("relocating_shards", response.getRelocatingShards());
                        result.put("initializing_shards", response.getInitializingShards());
                        result.put("unassigned_shards", response.getUnassignedShards());
                        result.put("active_shards_percent", response.getActiveShardsPercent());
                        result.put("timed_out", response.isTimedOut());
                        listener.onResponse(result);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_CLUSTER_STATE",
        description = "Get cluster state",
        parameters = {
            @FunctionParam(name = "metrics", type = "STRING", description = "Comma-separated metrics (nodes,routing_table,metadata)")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Cluster state"),
        examples = {"ES_CLUSTER_STATE('nodes,metadata')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerClusterState(ExecutionContext context, Client client) {
        context.declareFunction("ES_CLUSTER_STATE",
            List.of(new Parameter("metrics", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_CLUSTER_STATE", (args, listener) -> {
                ClusterStateRequest request = new ClusterStateRequest();
                
                client.admin().cluster().state(request, new ActionListener<ClusterStateResponse>() {
                    @Override
                    public void onResponse(ClusterStateResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("cluster_name", response.getClusterName().value());
                        result.put("cluster_uuid", response.getState().metadata().clusterUUID());
                        result.put("version", response.getState().version());
                        
                        List<Map<String, Object>> nodes = new java.util.ArrayList<>();
                        for (var node : response.getState().nodes()) {
                            Map<String, Object> nodeInfo = new HashMap<>();
                            nodeInfo.put("id", node.getId());
                            nodeInfo.put("name", node.getName());
                            nodeInfo.put("address", node.getAddress().toString());
                            nodeInfo.put("roles", node.getRoles().toString());
                            nodes.add(nodeInfo);
                        }
                        result.put("nodes", nodes);
                        result.put("master_node", response.getState().nodes().getMasterNodeId());
                        
                        listener.onResponse(result);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_CLUSTER_STATS",
        description = "Get cluster statistics",
        parameters = {},
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Cluster statistics"),
        examples = {"ES_CLUSTER_STATS()"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerClusterStats(ExecutionContext context, Client client) {
        context.declareFunction("ES_CLUSTER_STATS",
            List.of(),
            new BuiltInFunctionDefinition("ES_CLUSTER_STATS", (args, listener) -> {
                ClusterStatsRequest request = new ClusterStatsRequest();
                
                client.admin().cluster().clusterStats(request, new ActionListener<ClusterStatsResponse>() {
                    @Override
                    public void onResponse(ClusterStatsResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("cluster_name", response.getClusterName().value());
                        result.put("cluster_uuid", response.getClusterUUID());
                        result.put("status", response.getStatus().name().toLowerCase());
                        result.put("timestamp", response.getTimestamp());
                        
                        Map<String, Object> indices = new HashMap<>();
                        indices.put("count", response.getIndicesStats().getIndexCount());
                        indices.put("shards_total", response.getIndicesStats().getShards().getTotal());
                        indices.put("docs_count", response.getIndicesStats().getDocs().getCount());
                        indices.put("store_size_bytes", response.getIndicesStats().getStore().sizeInBytes());
                        result.put("indices", indices);
                        
                        Map<String, Object> nodes = new HashMap<>();
                        nodes.put("total", response.getNodesStats().getCounts().getTotal());
                        nodes.put("successful", response.getNodesStats().getCounts().getTotal());
                        result.put("nodes", nodes);
                        
                        listener.onResponse(result);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_CLUSTER_SETTINGS",
        description = "Get cluster settings",
        parameters = {
            @FunctionParam(name = "include_defaults", type = "BOOLEAN", description = "Include default settings")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Cluster settings"),
        examples = {"ES_CLUSTER_SETTINGS(true)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerClusterSettings(ExecutionContext context, Client client) {
        context.declareFunction("ES_CLUSTER_SETTINGS",
            List.of(new Parameter("include_defaults", "BOOLEAN", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_CLUSTER_SETTINGS", (args, listener) -> {
                ClusterGetSettingsRequest request = new ClusterGetSettingsRequest();
                if (args.size() > 0 && args.get(0) != null) {
                    request.includeDefaults(Boolean.TRUE.equals(args.get(0)));
                }
                
                client.admin().cluster().getSettings(request, new ActionListener<ClusterGetSettingsResponse>() {
                    @Override
                    public void onResponse(ClusterGetSettingsResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        
                        Map<String, Object> persistent = new HashMap<>();
                        for (String key : response.persistentSettings().keySet()) {
                            persistent.put(key, response.persistentSettings().get(key));
                        }
                        result.put("persistent", persistent);
                        
                        Map<String, Object> transientSettings = new HashMap<>();
                        for (String key : response.transientSettings().keySet()) {
                            transientSettings.put(key, response.transientSettings().get(key));
                        }
                        result.put("transient", transientSettings);
                        
                        listener.onResponse(result);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_UPDATE_CLUSTER_SETTINGS",
        description = "Update cluster settings",
        parameters = {
            @FunctionParam(name = "persistent", type = "DOCUMENT", description = "Persistent settings"),
            @FunctionParam(name = "transient_settings", type = "DOCUMENT", description = "Transient settings")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if acknowledged"),
        examples = {"ES_UPDATE_CLUSTER_SETTINGS({'cluster.routing.allocation.enable': 'all'}, {})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerUpdateClusterSettings(ExecutionContext context, Client client) {
        context.declareFunction("ES_UPDATE_CLUSTER_SETTINGS",
            Arrays.asList(
                new Parameter("persistent", "DOCUMENT", ParameterMode.IN),
                new Parameter("transient_settings", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_UPDATE_CLUSTER_SETTINGS", (args, listener) -> {
                ClusterUpdateSettingsRequest request = new ClusterUpdateSettingsRequest();
                
                if (args.size() > 0 && args.get(0) != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> persistent = (Map<String, Object>) args.get(0);
                    Settings.Builder builder = Settings.builder();
                    for (Map.Entry<String, Object> entry : persistent.entrySet()) {
                        builder.put(entry.getKey(), entry.getValue().toString());
                    }
                    request.persistentSettings(builder);
                }
                
                if (args.size() > 1 && args.get(1) != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> transientSettings = (Map<String, Object>) args.get(1);
                    Settings.Builder builder = Settings.builder();
                    for (Map.Entry<String, Object> entry : transientSettings.entrySet()) {
                        builder.put(entry.getKey(), entry.getValue().toString());
                    }
                    request.transientSettings(builder);
                }
                
                client.admin().cluster().updateSettings(request, new ActionListener<org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsResponse>() {
                    @Override
                    public void onResponse(org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsResponse response) {
                        listener.onResponse(response.isAcknowledged());
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_NODES_INFO",
        description = "Get nodes information",
        parameters = {
            @FunctionParam(name = "node_id", type = "STRING", description = "Optional node ID filter")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Array of node info"),
        examples = {"ES_NODES_INFO()", "ES_NODES_INFO('node-1')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerNodesInfo(ExecutionContext context, Client client) {
        context.declareFunction("ES_NODES_INFO",
            List.of(new Parameter("node_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_NODES_INFO", (args, listener) -> {
                NodesInfoRequest request = new NodesInfoRequest();
                if (args.size() > 0 && args.get(0) != null) {
                    request.nodesIds(args.get(0).toString());
                }
                
                client.admin().cluster().nodesInfo(request, new ActionListener<NodesInfoResponse>() {
                    @Override
                    public void onResponse(NodesInfoResponse response) {
                        List<Map<String, Object>> nodes = new java.util.ArrayList<>();
                        for (var nodeInfo : response.getNodes()) {
                            Map<String, Object> node = new HashMap<>();
                            node.put("id", nodeInfo.getNode().getId());
                            node.put("name", nodeInfo.getNode().getName());
                            node.put("address", nodeInfo.getNode().getAddress().toString());
                            node.put("version", nodeInfo.getVersion().toString());
                            node.put("roles", nodeInfo.getNode().getRoles().toString());
                            node.put("os_name", nodeInfo.getInfo(org.elasticsearch.monitor.os.OsInfo.class) != null ? 
                                nodeInfo.getInfo(org.elasticsearch.monitor.os.OsInfo.class).getPrettyName() : "unknown");
                            nodes.add(node);
                        }
                        listener.onResponse(nodes);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_NODES_STATS",
        description = "Get nodes statistics",
        parameters = {
            @FunctionParam(name = "node_id", type = "STRING", description = "Optional node ID filter"),
            @FunctionParam(name = "metrics", type = "STRING", description = "Comma-separated metrics")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Array of node stats"),
        examples = {"ES_NODES_STATS()", "ES_NODES_STATS('node-1', 'jvm,os')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerNodesStats(ExecutionContext context, Client client) {
        context.declareFunction("ES_NODES_STATS",
            Arrays.asList(
                new Parameter("node_id", "STRING", ParameterMode.IN),
                new Parameter("metrics", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_NODES_STATS", (args, listener) -> {
                NodesStatsRequest request = new NodesStatsRequest();
                if (args.size() > 0 && args.get(0) != null) {
                    request.nodesIds(args.get(0).toString());
                }
                
                client.admin().cluster().nodesStats(request, new ActionListener<NodesStatsResponse>() {
                    @Override
                    public void onResponse(NodesStatsResponse response) {
                        List<Map<String, Object>> nodes = new java.util.ArrayList<>();
                        for (var nodeStats : response.getNodes()) {
                            Map<String, Object> node = new HashMap<>();
                            node.put("name", nodeStats.getNode().getName());
                            node.put("id", nodeStats.getNode().getId());
                            
                            if (nodeStats.getOs() != null) {
                                Map<String, Object> os = new HashMap<>();
                                os.put("cpu_percent", nodeStats.getOs().getCpu().getPercent());
                                os.put("mem_used_percent", nodeStats.getOs().getMem().getUsedPercent());
                                os.put("mem_free_bytes", nodeStats.getOs().getMem().getFree().getBytes());
                                node.put("os", os);
                            }
                            
                            if (nodeStats.getJvm() != null) {
                                Map<String, Object> jvm = new HashMap<>();
                                jvm.put("heap_used_bytes", nodeStats.getJvm().getMem().getHeapUsed().getBytes());
                                jvm.put("heap_max_bytes", nodeStats.getJvm().getMem().getHeapMax().getBytes());
                                jvm.put("heap_used_percent", nodeStats.getJvm().getMem().getHeapUsedPercent());
                                jvm.put("uptime_ms", nodeStats.getJvm().getUptime().millis());
                                node.put("jvm", jvm);
                            }
                            
                            if (nodeStats.getIndices() != null) {
                                Map<String, Object> indices = new HashMap<>();
                                indices.put("docs_count", nodeStats.getIndices().getDocs().getCount());
                                indices.put("store_size_bytes", nodeStats.getIndices().getStore().sizeInBytes());
                                node.put("indices", indices);
                            }
                            
                            nodes.add(node);
                        }
                        listener.onResponse(nodes);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_NODE_HOT_THREADS",
        description = "Get hot threads from nodes",
        parameters = {
            @FunctionParam(name = "node_id", type = "STRING", description = "Optional node ID")
        },
        returnType = @FunctionReturn(type = "STRING", description = "Hot threads output"),
        examples = {"ES_NODE_HOT_THREADS()"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerNodeHotThreads(ExecutionContext context, Client client) {
        context.declareFunction("ES_NODE_HOT_THREADS",
            List.of(new Parameter("node_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_NODE_HOT_THREADS", (args, listener) -> {
                org.elasticsearch.action.admin.cluster.node.hotthreads.NodesHotThreadsRequest request = 
                    new org.elasticsearch.action.admin.cluster.node.hotthreads.NodesHotThreadsRequest();
                if (args.size() > 0 && args.get(0) != null) {
                    request.nodesIds(args.get(0).toString());
                }
                
                client.admin().cluster().nodesHotThreads(request, new ActionListener<org.elasticsearch.action.admin.cluster.node.hotthreads.NodesHotThreadsResponse>() {
                    @Override
                    public void onResponse(org.elasticsearch.action.admin.cluster.node.hotthreads.NodesHotThreadsResponse response) {
                        StringBuilder sb = new StringBuilder();
                        for (var nodeHotThreads : response.getNodes()) {
                            sb.append("=== Node: ").append(nodeHotThreads.getNode().getName()).append(" ===\n");
                            sb.append(nodeHotThreads.getHotThreads()).append("\n");
                        }
                        listener.onResponse(sb.toString());
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_PENDING_TASKS",
        description = "Get pending cluster tasks",
        parameters = {},
        returnType = @FunctionReturn(type = "ARRAY", description = "Pending tasks"),
        examples = {"ES_PENDING_TASKS()"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerPendingTasks(ExecutionContext context, Client client) {
        context.declareFunction("ES_PENDING_TASKS",
            List.of(),
            new BuiltInFunctionDefinition("ES_PENDING_TASKS", (args, listener) -> {
                org.elasticsearch.action.admin.cluster.tasks.PendingClusterTasksRequest request = 
                    new org.elasticsearch.action.admin.cluster.tasks.PendingClusterTasksRequest();
                
                client.admin().cluster().pendingClusterTasks(request, new ActionListener<org.elasticsearch.action.admin.cluster.tasks.PendingClusterTasksResponse>() {
                    @Override
                    public void onResponse(org.elasticsearch.action.admin.cluster.tasks.PendingClusterTasksResponse response) {
                        List<Map<String, Object>> tasks = new java.util.ArrayList<>();
                        for (var task : response.pendingTasks()) {
                            Map<String, Object> t = new HashMap<>();
                            t.put("insert_order", task.getInsertOrder());
                            t.put("priority", task.getPriority().toString());
                            t.put("source", task.getSource().toString());
                            t.put("time_in_queue_ms", task.getTimeInQueueInMillis());
                            tasks.add(t);
                        }
                        listener.onResponse(tasks);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_LIST_TASKS",
        description = "List running tasks",
        parameters = {
            @FunctionParam(name = "actions", type = "STRING", description = "Optional action filter")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Running tasks"),
        examples = {"ES_LIST_TASKS()", "ES_LIST_TASKS('*search*')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerListTasks(ExecutionContext context, Client client) {
        context.declareFunction("ES_LIST_TASKS",
            List.of(new Parameter("actions", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_LIST_TASKS", (args, listener) -> {
                ListTasksRequest request = new ListTasksRequest();
                if (args.size() > 0 && args.get(0) != null) {
                    request.setActions(args.get(0).toString());
                }
                request.setDetailed(true);
                
                client.admin().cluster().listTasks(request, new ActionListener<ListTasksResponse>() {
                    @Override
                    public void onResponse(ListTasksResponse response) {
                        List<Map<String, Object>> tasks = new java.util.ArrayList<>();
                        for (var task : response.getTasks()) {
                            Map<String, Object> t = new HashMap<>();
                            t.put("id", task.taskId().toString());
                            t.put("action", task.action());
                            t.put("type", task.type());
                            t.put("node", task.taskId().getNodeId());
                            t.put("running_time_ns", task.runningTimeNanos());
                            t.put("cancellable", task.cancellable());
                            tasks.add(t);
                        }
                        listener.onResponse(tasks);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_CANCEL_TASK",
        description = "Cancel a running task",
        parameters = {
            @FunctionParam(name = "task_id", type = "STRING", description = "Task ID to cancel")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if cancelled"),
        examples = {"ES_CANCEL_TASK('node1:12345')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerCancelTask(ExecutionContext context, Client client) {
        context.declareFunction("ES_CANCEL_TASK",
            List.of(new Parameter("task_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_CANCEL_TASK", (args, listener) -> {
                String taskId = args.get(0).toString();
                
                CancelTasksRequest request = new CancelTasksRequest();
                request.setTargetTaskId(org.elasticsearch.tasks.TaskId.readFromString(taskId));
                
                client.admin().cluster().cancelTasks(request, new ActionListener<CancelTasksResponse>() {
                    @Override
                    public void onResponse(CancelTasksResponse response) {
                        listener.onResponse(response.getTasks().size() > 0);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_ALLOCATION_EXPLAIN",
        description = "Explain shard allocation",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "shard", type = "NUMBER", description = "Shard number"),
            @FunctionParam(name = "primary", type = "BOOLEAN", description = "Primary shard?")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Allocation explanation"),
        examples = {"ES_ALLOCATION_EXPLAIN('my-index', 0, true)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerClusterAllocationExplain(ExecutionContext context, Client client) {
        context.declareFunction("ES_ALLOCATION_EXPLAIN",
            Arrays.asList(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("shard", "NUMBER", ParameterMode.IN),
                new Parameter("primary", "BOOLEAN", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_ALLOCATION_EXPLAIN", (args, listener) -> {
                ClusterAllocationExplainRequest request = new ClusterAllocationExplainRequest();
                if (args.size() > 0 && args.get(0) != null) {
                    request.setIndex(args.get(0).toString());
                }
                if (args.size() > 1 && args.get(1) != null) {
                    request.setShard(((Number) args.get(1)).intValue());
                }
                if (args.size() > 2 && args.get(2) != null) {
                    request.setPrimary(Boolean.TRUE.equals(args.get(2)));
                }
                
                client.execute(TransportClusterAllocationExplainAction.TYPE, request, 
                    new ActionListener<ClusterAllocationExplainResponse>() {
                        @Override
                        public void onResponse(ClusterAllocationExplainResponse response) {
                            Map<String, Object> result = new HashMap<>();
                            var explain = response.getExplanation();
                            result.put("index", explain.getShard().getIndexName());
                            result.put("shard", explain.getShard().id());
                            result.put("primary", explain.isPrimary());
                            result.put("current_state", explain.getShardState().name());
                            if (explain.getCurrentNode() != null) {
                                result.put("current_node", explain.getCurrentNode().getName());
                            }
                            result.put("can_allocate", explain.getShardAllocationDecision().getAllocateDecision() != null ?
                                explain.getShardAllocationDecision().getAllocateDecision().getAllocationDecision().name() : "N/A");
                            listener.onResponse(result);
                        }
                        @Override
                        public void onFailure(Exception e) {
                            listener.onFailure(e);
                        }
                    });
            })
        );
    }

    @FunctionSpec(
        name = "ES_CLUSTER_REROUTE",
        description = "Manually reroute shards",
        parameters = {
            @FunctionParam(name = "commands", type = "ARRAY", description = "Reroute commands")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if acknowledged"),
        examples = {"ES_CLUSTER_REROUTE([{'allocate_replica': {'index': 'test', 'shard': 0, 'node': 'node1'}}])"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerClusterReroute(ExecutionContext context, Client client) {
        context.declareFunction("ES_CLUSTER_REROUTE",
            List.of(new Parameter("commands", "ARRAY", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_CLUSTER_REROUTE", (args, listener) -> {
                ClusterRerouteRequest request = new ClusterRerouteRequest();
                
                client.admin().cluster().reroute(request, new ActionListener<ClusterRerouteResponse>() {
                    @Override
                    public void onResponse(ClusterRerouteResponse response) {
                        listener.onResponse(response.isAcknowledged());
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_REMOTE_INFO",
        description = "Get remote cluster info",
        parameters = {},
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Remote clusters info"),
        examples = {"ES_REMOTE_INFO()"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerRemoteInfo(ExecutionContext context, Client client) {
        context.declareFunction("ES_REMOTE_INFO",
            List.of(),
            new BuiltInFunctionDefinition("ES_REMOTE_INFO", (args, listener) -> {
                org.elasticsearch.action.admin.cluster.remote.RemoteInfoRequest request = 
                    new org.elasticsearch.action.admin.cluster.remote.RemoteInfoRequest();
                
                client.execute(org.elasticsearch.action.admin.cluster.remote.TransportRemoteInfoAction.TYPE, request,
                    new ActionListener<org.elasticsearch.action.admin.cluster.remote.RemoteInfoResponse>() {
                        @Override
                        public void onResponse(org.elasticsearch.action.admin.cluster.remote.RemoteInfoResponse response) {
                            Map<String, Object> result = new HashMap<>();
                            for (var info : response.getInfos()) {
                                Map<String, Object> clusterInfo = new HashMap<>();
                                clusterInfo.put("connected", info.isConnected());
                                clusterInfo.put("mode", info.getModeInfo().modeName());
                                clusterInfo.put("initial_connect_timeout", info.getInitialConnectionTimeout().toString());
                                result.put(info.getClusterAlias(), clusterInfo);
                            }
                            listener.onResponse(result);
                        }
                        @Override
                        public void onFailure(Exception e) {
                            listener.onFailure(e);
                        }
                    });
            })
        );
    }

    @FunctionSpec(
        name = "ES_CLUSTER_INFO",
        description = "Get basic cluster info",
        parameters = {},
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Cluster name, version, UUID"),
        examples = {"ES_CLUSTER_INFO()"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerClusterInfo(ExecutionContext context, Client client) {
        context.declareFunction("ES_CLUSTER_INFO",
            List.of(),
            new BuiltInFunctionDefinition("ES_CLUSTER_INFO", (args, listener) -> {
                ClusterStateRequest request = new ClusterStateRequest();
                request.clear().metadata(true);
                
                client.admin().cluster().state(request, new ActionListener<ClusterStateResponse>() {
                    @Override
                    public void onResponse(ClusterStateResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("cluster_name", response.getClusterName().value());
                        result.put("cluster_uuid", response.getState().metadata().clusterUUID());
                        listener.onResponse(result);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }
}
