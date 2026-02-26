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
 * Elasticsearch Watcher API functions using REST HTTP calls.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.DATASOURCE,
    description = "Elasticsearch Watcher API functions."
)
public class WatcherApiFunctions extends ElasticsearchHttpFunctions {

    public static void registerAll(ExecutionContext context) {
        registerGetWatch(context);
        registerPutWatch(context);
        registerDeleteWatch(context);
        registerExecuteWatch(context);
        registerActivateWatch(context);
        registerDeactivateWatch(context);
        registerAckWatch(context);
        registerWatcherStats(context);
        registerStartWatcher(context);
        registerStopWatcher(context);
    }

    @FunctionSpec(
        name = "ES_GET_WATCH",
        description = "Get a watch definition",
        parameters = {
            @FunctionParam(name = "watch_id", type = "STRING", description = "Watch ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Watch definition"),
        examples = {"ES_GET_WATCH('my-watch')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetWatch(ExecutionContext context) {
        context.declareFunction("ES_GET_WATCH",
            List.of(new Parameter("watch_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_GET_WATCH", (args, listener) -> {
                try {
                    String watchId = toString(args.get(0));
                    Map<String, Object> result = esRequest("GET", "/_watcher/watch/" + watchId, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_GET_WATCH failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_PUT_WATCH",
        description = "Create or update a watch",
        parameters = {
            @FunctionParam(name = "watch_id", type = "STRING", description = "Watch ID"),
            @FunctionParam(name = "watch", type = "DOCUMENT", description = "Watch definition")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Create/update result"),
        examples = {"ES_PUT_WATCH('my-watch', {'trigger': {...}, 'input': {...}, 'actions': {...}})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerPutWatch(ExecutionContext context) {
        context.declareFunction("ES_PUT_WATCH",
            List.of(
                new Parameter("watch_id", "STRING", ParameterMode.IN),
                new Parameter("watch", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_PUT_WATCH", (args, listener) -> {
                try {
                    String watchId = toString(args.get(0));
                    Map<String, Object> watch = toMap(args.get(1));
                    Map<String, Object> result = esRequest("PUT", "/_watcher/watch/" + watchId, watch);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_PUT_WATCH failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_DELETE_WATCH",
        description = "Delete a watch",
        parameters = {
            @FunctionParam(name = "watch_id", type = "STRING", description = "Watch ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Delete result"),
        examples = {"ES_DELETE_WATCH('my-watch')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDeleteWatch(ExecutionContext context) {
        context.declareFunction("ES_DELETE_WATCH",
            List.of(new Parameter("watch_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_DELETE_WATCH", (args, listener) -> {
                try {
                    String watchId = toString(args.get(0));
                    Map<String, Object> result = esRequest("DELETE", "/_watcher/watch/" + watchId, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_DELETE_WATCH failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_EXECUTE_WATCH",
        description = "Execute a watch manually",
        parameters = {
            @FunctionParam(name = "watch_id", type = "STRING", description = "Watch ID"),
            @FunctionParam(name = "options", type = "DOCUMENT", description = "Execution options")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Execution result"),
        examples = {"ES_EXECUTE_WATCH('my-watch', {'record_execution': true})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerExecuteWatch(ExecutionContext context) {
        context.declareFunction("ES_EXECUTE_WATCH",
            List.of(
                new Parameter("watch_id", "STRING", ParameterMode.IN),
                new Parameter("options", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_EXECUTE_WATCH", (args, listener) -> {
                try {
                    String watchId = toString(args.get(0));
                    Map<String, Object> options = args.size() > 1 && args.get(1) != null ? toMap(args.get(1)) : null;
                    Map<String, Object> result = esRequest("PUT", "/_watcher/watch/" + watchId + "/_execute", options);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_EXECUTE_WATCH failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_ACTIVATE_WATCH",
        description = "Activate a watch",
        parameters = {
            @FunctionParam(name = "watch_id", type = "STRING", description = "Watch ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Activation result"),
        examples = {"ES_ACTIVATE_WATCH('my-watch')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerActivateWatch(ExecutionContext context) {
        context.declareFunction("ES_ACTIVATE_WATCH",
            List.of(new Parameter("watch_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_ACTIVATE_WATCH", (args, listener) -> {
                try {
                    String watchId = toString(args.get(0));
                    Map<String, Object> result = esRequest("PUT", "/_watcher/watch/" + watchId + "/_activate", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_ACTIVATE_WATCH failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_DEACTIVATE_WATCH",
        description = "Deactivate a watch",
        parameters = {
            @FunctionParam(name = "watch_id", type = "STRING", description = "Watch ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Deactivation result"),
        examples = {"ES_DEACTIVATE_WATCH('my-watch')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDeactivateWatch(ExecutionContext context) {
        context.declareFunction("ES_DEACTIVATE_WATCH",
            List.of(new Parameter("watch_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_DEACTIVATE_WATCH", (args, listener) -> {
                try {
                    String watchId = toString(args.get(0));
                    Map<String, Object> result = esRequest("PUT", "/_watcher/watch/" + watchId + "/_deactivate", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_DEACTIVATE_WATCH failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_ACK_WATCH",
        description = "Acknowledge a watch",
        parameters = {
            @FunctionParam(name = "watch_id", type = "STRING", description = "Watch ID"),
            @FunctionParam(name = "action_id", type = "STRING", description = "Action ID to acknowledge")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Acknowledgment result"),
        examples = {"ES_ACK_WATCH('my-watch', 'my-action')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerAckWatch(ExecutionContext context) {
        context.declareFunction("ES_ACK_WATCH",
            List.of(
                new Parameter("watch_id", "STRING", ParameterMode.IN),
                new Parameter("action_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_ACK_WATCH", (args, listener) -> {
                try {
                    String watchId = toString(args.get(0));
                    String actionId = args.size() > 1 && args.get(1) != null ? toString(args.get(1)) : "_all";
                    Map<String, Object> result = esRequest("PUT", "/_watcher/watch/" + watchId + "/_ack/" + actionId, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_ACK_WATCH failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_WATCHER_STATS",
        description = "Get watcher statistics",
        parameters = {},
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Watcher stats"),
        examples = {"ES_WATCHER_STATS()"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerWatcherStats(ExecutionContext context) {
        context.declareFunction("ES_WATCHER_STATS",
            List.of(),
            new BuiltInFunctionDefinition("ES_WATCHER_STATS", (args, listener) -> {
                try {
                    Map<String, Object> result = esRequest("GET", "/_watcher/stats", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_WATCHER_STATS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_START_WATCHER",
        description = "Start the watcher service",
        parameters = {},
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Start result"),
        examples = {"ES_START_WATCHER()"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerStartWatcher(ExecutionContext context) {
        context.declareFunction("ES_START_WATCHER",
            List.of(),
            new BuiltInFunctionDefinition("ES_START_WATCHER", (args, listener) -> {
                try {
                    Map<String, Object> result = esRequest("POST", "/_watcher/_start", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_START_WATCHER failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_STOP_WATCHER",
        description = "Stop the watcher service",
        parameters = {},
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Stop result"),
        examples = {"ES_STOP_WATCHER()"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerStopWatcher(ExecutionContext context) {
        context.declareFunction("ES_STOP_WATCHER",
            List.of(),
            new BuiltInFunctionDefinition("ES_STOP_WATCHER", (args, listener) -> {
                try {
                    Map<String, Object> result = esRequest("POST", "/_watcher/_stop", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_STOP_WATCHER failed: " + e.getMessage(), e));
                }
            })
        );
    }
}
