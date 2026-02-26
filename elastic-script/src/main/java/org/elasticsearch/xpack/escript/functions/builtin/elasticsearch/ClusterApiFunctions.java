/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.builtin.elasticsearch;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;
import org.elasticsearch.xpack.escript.functions.api.FunctionCategory;
import org.elasticsearch.xpack.escript.functions.api.FunctionCollectionSpec;
import org.elasticsearch.xpack.escript.functions.api.FunctionParam;
import org.elasticsearch.xpack.escript.functions.api.FunctionReturn;
import org.elasticsearch.xpack.escript.functions.api.FunctionSpec;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch Cluster API functions using REST HTTP calls.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.DATASOURCE,
    description = "Elasticsearch Cluster API functions for cluster management and monitoring."
)
public class ClusterApiFunctions extends ElasticsearchHttpFunctions {

    public static void registerAll(ExecutionContext context) {
        registerClusterHealth(context);
        registerClusterState(context);
        registerClusterStats(context);
        registerClusterSettings(context);
        registerUpdateClusterSettings(context);
        registerNodesInfo(context);
        registerNodesStats(context);
        registerNodeHotThreads(context);
        registerPendingTasks(context);
        registerListTasks(context);
        registerCancelTask(context);
        registerAllocationExplain(context);
        registerRemoteInfo(context);
        registerClusterInfo(context);
    }

    @FunctionSpec(
        name = "ES_CLUSTER_HEALTH",
        description = "Get cluster health status",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Optional index to check health for")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Cluster health information"),
        examples = {"ES_CLUSTER_HEALTH()", "ES_CLUSTER_HEALTH('my-index')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerClusterHealth(ExecutionContext context) {
        context.declareFunction("ES_CLUSTER_HEALTH",
            List.of(new Parameter("index", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_CLUSTER_HEALTH", (args, listener) -> {
                try {
                    String index = args.size() > 0 && args.get(0) != null ? toString(args.get(0)) : null;
                    String path = index != null && !index.isEmpty() 
                        ? "/_cluster/health/" + index 
                        : "/_cluster/health";
                    
                    Map<String, Object> result = esRequest("GET", path, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_CLUSTER_HEALTH failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_CLUSTER_STATE",
        description = "Get cluster state information",
        parameters = {
            @FunctionParam(name = "metrics", type = "STRING", description = "Metrics to return (e.g., 'nodes,routing_table')")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Cluster state"),
        examples = {"ES_CLUSTER_STATE('nodes')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerClusterState(ExecutionContext context) {
        context.declareFunction("ES_CLUSTER_STATE",
            List.of(new Parameter("metrics", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_CLUSTER_STATE", (args, listener) -> {
                try {
                    String metrics = args.size() > 0 && args.get(0) != null ? toString(args.get(0)) : "_all";
                    Map<String, Object> result = esRequest("GET", "/_cluster/state/" + metrics, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_CLUSTER_STATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_CLUSTER_STATS",
        description = "Get cluster-wide statistics",
        parameters = {},
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Cluster statistics"),
        examples = {"ES_CLUSTER_STATS()"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerClusterStats(ExecutionContext context) {
        context.declareFunction("ES_CLUSTER_STATS",
            List.of(),
            new BuiltInFunctionDefinition("ES_CLUSTER_STATS", (args, listener) -> {
                try {
                    Map<String, Object> result = esRequest("GET", "/_cluster/stats", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_CLUSTER_STATS failed: " + e.getMessage(), e));
                }
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
    public static void registerClusterSettings(ExecutionContext context) {
        context.declareFunction("ES_CLUSTER_SETTINGS",
            List.of(new Parameter("include_defaults", "BOOLEAN", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_CLUSTER_SETTINGS", (args, listener) -> {
                try {
                    boolean includeDefaults = args.size() > 0 ? toBoolean(args.get(0), false) : false;
                    String path = "/_cluster/settings" + (includeDefaults ? "?include_defaults=true" : "");
                    Map<String, Object> result = esRequest("GET", path, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_CLUSTER_SETTINGS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_UPDATE_CLUSTER_SETTINGS",
        description = "Update cluster settings",
        parameters = {
            @FunctionParam(name = "settings", type = "DOCUMENT", description = "Settings to update (persistent/transient)")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Updated settings"),
        examples = {"ES_UPDATE_CLUSTER_SETTINGS({'persistent': {'cluster.routing.allocation.enable': 'all'}})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerUpdateClusterSettings(ExecutionContext context) {
        context.declareFunction("ES_UPDATE_CLUSTER_SETTINGS",
            List.of(new Parameter("settings", "DOCUMENT", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_UPDATE_CLUSTER_SETTINGS", (args, listener) -> {
                try {
                    Map<String, Object> settings = toMap(args.get(0));
                    Map<String, Object> result = esRequest("PUT", "/_cluster/settings", settings);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_UPDATE_CLUSTER_SETTINGS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_NODES_INFO",
        description = "Get information about cluster nodes",
        parameters = {
            @FunctionParam(name = "node_id", type = "STRING", description = "Node ID or _all"),
            @FunctionParam(name = "metrics", type = "STRING", description = "Metrics to return")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Nodes information"),
        examples = {"ES_NODES_INFO('_all', 'os,jvm')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerNodesInfo(ExecutionContext context) {
        context.declareFunction("ES_NODES_INFO",
            List.of(
                new Parameter("node_id", "STRING", ParameterMode.IN),
                new Parameter("metrics", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_NODES_INFO", (args, listener) -> {
                try {
                    String nodeId = args.size() > 0 && args.get(0) != null ? toString(args.get(0)) : "_all";
                    String metrics = args.size() > 1 && args.get(1) != null ? toString(args.get(1)) : "";
                    String path = "/_nodes/" + nodeId + (metrics.isEmpty() ? "" : "/" + metrics);
                    Map<String, Object> result = esRequest("GET", path, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_NODES_INFO failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_NODES_STATS",
        description = "Get statistics about cluster nodes",
        parameters = {
            @FunctionParam(name = "node_id", type = "STRING", description = "Node ID or _all"),
            @FunctionParam(name = "metrics", type = "STRING", description = "Metrics to return")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Nodes statistics"),
        examples = {"ES_NODES_STATS('_all', 'os,jvm')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerNodesStats(ExecutionContext context) {
        context.declareFunction("ES_NODES_STATS",
            List.of(
                new Parameter("node_id", "STRING", ParameterMode.IN),
                new Parameter("metrics", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_NODES_STATS", (args, listener) -> {
                try {
                    String nodeId = args.size() > 0 && args.get(0) != null ? toString(args.get(0)) : "_all";
                    String metrics = args.size() > 1 && args.get(1) != null ? toString(args.get(1)) : "";
                    String path = "/_nodes/" + nodeId + "/stats" + (metrics.isEmpty() ? "" : "/" + metrics);
                    Map<String, Object> result = esRequest("GET", path, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_NODES_STATS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_NODE_HOT_THREADS",
        description = "Get hot threads information for nodes",
        parameters = {
            @FunctionParam(name = "node_id", type = "STRING", description = "Node ID or _all")
        },
        returnType = @FunctionReturn(type = "STRING", description = "Hot threads output"),
        examples = {"ES_NODE_HOT_THREADS('_all')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerNodeHotThreads(ExecutionContext context) {
        context.declareFunction("ES_NODE_HOT_THREADS",
            List.of(new Parameter("node_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_NODE_HOT_THREADS", (args, listener) -> {
                try {
                    String nodeId = args.size() > 0 && args.get(0) != null ? toString(args.get(0)) : "_all";
                    Map<String, Object> result = esRequest("GET", "/_nodes/" + nodeId + "/hot_threads", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_NODE_HOT_THREADS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_PENDING_TASKS",
        description = "Get pending cluster tasks",
        parameters = {},
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Pending tasks"),
        examples = {"ES_PENDING_TASKS()"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerPendingTasks(ExecutionContext context) {
        context.declareFunction("ES_PENDING_TASKS",
            List.of(),
            new BuiltInFunctionDefinition("ES_PENDING_TASKS", (args, listener) -> {
                try {
                    Map<String, Object> result = esRequest("GET", "/_cluster/pending_tasks", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_PENDING_TASKS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_LIST_TASKS",
        description = "List all running tasks",
        parameters = {
            @FunctionParam(name = "detailed", type = "BOOLEAN", description = "Include detailed information")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Running tasks"),
        examples = {"ES_LIST_TASKS(true)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerListTasks(ExecutionContext context) {
        context.declareFunction("ES_LIST_TASKS",
            List.of(new Parameter("detailed", "BOOLEAN", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_LIST_TASKS", (args, listener) -> {
                try {
                    boolean detailed = args.size() > 0 ? toBoolean(args.get(0), false) : false;
                    String path = "/_tasks" + (detailed ? "?detailed=true" : "");
                    Map<String, Object> result = esRequest("GET", path, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_LIST_TASKS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_CANCEL_TASK",
        description = "Cancel a running task",
        parameters = {
            @FunctionParam(name = "task_id", type = "STRING", description = "Task ID to cancel")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Cancellation result"),
        examples = {"ES_CANCEL_TASK('node1:12345')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerCancelTask(ExecutionContext context) {
        context.declareFunction("ES_CANCEL_TASK",
            List.of(new Parameter("task_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_CANCEL_TASK", (args, listener) -> {
                try {
                    String taskId = toString(args.get(0));
                    Map<String, Object> result = esRequest("POST", "/_tasks/" + taskId + "/_cancel", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_CANCEL_TASK failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_ALLOCATION_EXPLAIN",
        description = "Explain shard allocation",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "shard", type = "NUMBER", description = "Shard number"),
            @FunctionParam(name = "primary", type = "BOOLEAN", description = "Primary shard")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Allocation explanation"),
        examples = {"ES_ALLOCATION_EXPLAIN('my-index', 0, true)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerAllocationExplain(ExecutionContext context) {
        context.declareFunction("ES_ALLOCATION_EXPLAIN",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("shard", "NUMBER", ParameterMode.IN),
                new Parameter("primary", "BOOLEAN", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_ALLOCATION_EXPLAIN", (args, listener) -> {
                try {
                    Map<String, Object> body = new HashMap<>();
                    if (args.size() > 0 && args.get(0) != null) {
                        body.put("index", toString(args.get(0)));
                        body.put("shard", toInt(args.get(1), 0));
                        body.put("primary", toBoolean(args.get(2), true));
                    }
                    Map<String, Object> result = esRequest("GET", "/_cluster/allocation/explain", 
                        body.isEmpty() ? null : body);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_ALLOCATION_EXPLAIN failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_REMOTE_INFO",
        description = "Get remote cluster information",
        parameters = {},
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Remote cluster info"),
        examples = {"ES_REMOTE_INFO()"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerRemoteInfo(ExecutionContext context) {
        context.declareFunction("ES_REMOTE_INFO",
            List.of(),
            new BuiltInFunctionDefinition("ES_REMOTE_INFO", (args, listener) -> {
                try {
                    Map<String, Object> result = esRequest("GET", "/_remote/info", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_REMOTE_INFO failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_CLUSTER_INFO",
        description = "Get basic cluster information",
        parameters = {},
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Cluster info (name, version, etc.)"),
        examples = {"ES_CLUSTER_INFO()"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerClusterInfo(ExecutionContext context) {
        context.declareFunction("ES_CLUSTER_INFO",
            List.of(),
            new BuiltInFunctionDefinition("ES_CLUSTER_INFO", (args, listener) -> {
                try {
                    Map<String, Object> result = esRequest("GET", "/", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_CLUSTER_INFO failed: " + e.getMessage(), e));
                }
            })
        );
    }
}
