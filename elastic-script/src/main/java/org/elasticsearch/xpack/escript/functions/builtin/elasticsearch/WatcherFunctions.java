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
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;
import org.elasticsearch.xpack.core.watcher.transport.actions.get.GetWatchAction;
import org.elasticsearch.xpack.core.watcher.transport.actions.get.GetWatchRequest;
import org.elasticsearch.xpack.core.watcher.transport.actions.get.GetWatchResponse;
import org.elasticsearch.xpack.core.watcher.transport.actions.delete.DeleteWatchAction;
import org.elasticsearch.xpack.core.watcher.transport.actions.delete.DeleteWatchRequest;
import org.elasticsearch.xpack.core.watcher.transport.actions.delete.DeleteWatchResponse;
import org.elasticsearch.xpack.core.watcher.transport.actions.execute.ExecuteWatchAction;
import org.elasticsearch.xpack.core.watcher.transport.actions.execute.ExecuteWatchRequest;
import org.elasticsearch.xpack.core.watcher.transport.actions.execute.ExecuteWatchResponse;
import org.elasticsearch.xpack.core.watcher.transport.actions.activate.ActivateWatchAction;
import org.elasticsearch.xpack.core.watcher.transport.actions.activate.ActivateWatchRequest;
import org.elasticsearch.xpack.core.watcher.transport.actions.activate.ActivateWatchResponse;
import org.elasticsearch.xpack.core.watcher.transport.actions.ack.AckWatchAction;
import org.elasticsearch.xpack.core.watcher.transport.actions.ack.AckWatchRequest;
import org.elasticsearch.xpack.core.watcher.transport.actions.ack.AckWatchResponse;
import org.elasticsearch.xpack.core.watcher.transport.actions.stats.WatcherStatsAction;
import org.elasticsearch.xpack.core.watcher.transport.actions.stats.WatcherStatsRequest;
import org.elasticsearch.xpack.core.watcher.transport.actions.stats.WatcherStatsResponse;
import org.elasticsearch.xpack.core.watcher.transport.actions.service.WatcherServiceAction;
import org.elasticsearch.xpack.core.watcher.transport.actions.service.WatcherServiceRequest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch Watcher API functions.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.DATASOURCE,
    description = "Elasticsearch Watcher API functions for alerting"
)
public class WatcherFunctions {

    public static void registerAll(ExecutionContext context, Client client) {
        registerGetWatch(context, client);
        registerDeleteWatch(context, client);
        registerExecuteWatch(context, client);
        registerActivateWatch(context, client);
        registerDeactivateWatch(context, client);
        registerAckWatch(context, client);
        registerWatcherStats(context, client);
        registerStartWatcher(context, client);
        registerStopWatcher(context, client);
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
    public static void registerGetWatch(ExecutionContext context, Client client) {
        context.declareFunction("ES_GET_WATCH",
            List.of(new Parameter("watch_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_GET_WATCH", (args, listener) -> {
                String watchId = args.get(0).toString();
                
                GetWatchRequest request = new GetWatchRequest(watchId);
                client.execute(GetWatchAction.INSTANCE, request, new ActionListener<GetWatchResponse>() {
                    @Override
                    public void onResponse(GetWatchResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("found", response.isFound());
                        result.put("id", response.getId());
                        result.put("status", response.getStatus() != null ? response.getStatus().state().toString() : null);
                        if (response.getSource() != null) {
                            result.put("watch", response.getSource().getAsMap());
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
        name = "ES_DELETE_WATCH",
        description = "Delete a watch",
        parameters = {
            @FunctionParam(name = "watch_id", type = "STRING", description = "Watch ID")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if found and deleted"),
        examples = {"ES_DELETE_WATCH('my-watch')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDeleteWatch(ExecutionContext context, Client client) {
        context.declareFunction("ES_DELETE_WATCH",
            List.of(new Parameter("watch_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_DELETE_WATCH", (args, listener) -> {
                String watchId = args.get(0).toString();
                
                DeleteWatchRequest request = new DeleteWatchRequest(watchId);
                client.execute(DeleteWatchAction.INSTANCE, request, new ActionListener<DeleteWatchResponse>() {
                    @Override
                    public void onResponse(DeleteWatchResponse response) {
                        listener.onResponse(response.isFound());
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
        name = "ES_EXECUTE_WATCH",
        description = "Execute a watch manually",
        parameters = {
            @FunctionParam(name = "watch_id", type = "STRING", description = "Watch ID"),
            @FunctionParam(name = "record_execution", type = "BOOLEAN", description = "Record in watch history")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Execution result"),
        examples = {"ES_EXECUTE_WATCH('my-watch', false)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerExecuteWatch(ExecutionContext context, Client client) {
        context.declareFunction("ES_EXECUTE_WATCH",
            Arrays.asList(
                new Parameter("watch_id", "STRING", ParameterMode.IN),
                new Parameter("record_execution", "BOOLEAN", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_EXECUTE_WATCH", (args, listener) -> {
                String watchId = args.get(0).toString();
                boolean recordExecution = args.size() > 1 && Boolean.TRUE.equals(args.get(1));
                
                ExecuteWatchRequest request = new ExecuteWatchRequest(watchId);
                request.setRecordExecution(recordExecution);
                
                client.execute(ExecuteWatchAction.INSTANCE, request, new ActionListener<ExecuteWatchResponse>() {
                    @Override
                    public void onResponse(ExecuteWatchResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("id", response.getRecordId());
                        var record = response.getRecordSource();
                        if (record != null) {
                            result.put("watch_record", record.getAsMap());
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
        name = "ES_ACTIVATE_WATCH",
        description = "Activate a watch",
        parameters = {
            @FunctionParam(name = "watch_id", type = "STRING", description = "Watch ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Activation status"),
        examples = {"ES_ACTIVATE_WATCH('my-watch')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerActivateWatch(ExecutionContext context, Client client) {
        context.declareFunction("ES_ACTIVATE_WATCH",
            List.of(new Parameter("watch_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_ACTIVATE_WATCH", (args, listener) -> {
                String watchId = args.get(0).toString();
                
                ActivateWatchRequest request = new ActivateWatchRequest(watchId, true);
                client.execute(ActivateWatchAction.INSTANCE, request, new ActionListener<ActivateWatchResponse>() {
                    @Override
                    public void onResponse(ActivateWatchResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("status", response.getStatus().state().toString());
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
        name = "ES_DEACTIVATE_WATCH",
        description = "Deactivate a watch",
        parameters = {
            @FunctionParam(name = "watch_id", type = "STRING", description = "Watch ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Deactivation status"),
        examples = {"ES_DEACTIVATE_WATCH('my-watch')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDeactivateWatch(ExecutionContext context, Client client) {
        context.declareFunction("ES_DEACTIVATE_WATCH",
            List.of(new Parameter("watch_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_DEACTIVATE_WATCH", (args, listener) -> {
                String watchId = args.get(0).toString();
                
                ActivateWatchRequest request = new ActivateWatchRequest(watchId, false);
                client.execute(ActivateWatchAction.INSTANCE, request, new ActionListener<ActivateWatchResponse>() {
                    @Override
                    public void onResponse(ActivateWatchResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("status", response.getStatus().state().toString());
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
        name = "ES_ACK_WATCH",
        description = "Acknowledge a watch",
        parameters = {
            @FunctionParam(name = "watch_id", type = "STRING", description = "Watch ID"),
            @FunctionParam(name = "action_ids", type = "ARRAY", description = "Action IDs to acknowledge")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Ack status"),
        examples = {"ES_ACK_WATCH('my-watch', ['email_action'])"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerAckWatch(ExecutionContext context, Client client) {
        context.declareFunction("ES_ACK_WATCH",
            Arrays.asList(
                new Parameter("watch_id", "STRING", ParameterMode.IN),
                new Parameter("action_ids", "ARRAY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_ACK_WATCH", (args, listener) -> {
                String watchId = args.get(0).toString();
                @SuppressWarnings("unchecked")
                List<String> actionIdsList = args.size() > 1 && args.get(1) != null ? 
                    (List<String>) args.get(1) : List.of();
                String[] actionIds = actionIdsList.toArray(new String[0]);
                
                AckWatchRequest request = new AckWatchRequest(watchId, actionIds);
                client.execute(AckWatchAction.INSTANCE, request, new ActionListener<AckWatchResponse>() {
                    @Override
                    public void onResponse(AckWatchResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("status", response.getStatus().state().toString());
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
        name = "ES_WATCHER_STATS",
        description = "Get Watcher statistics",
        parameters = {},
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Watcher stats"),
        examples = {"ES_WATCHER_STATS()"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerWatcherStats(ExecutionContext context, Client client) {
        context.declareFunction("ES_WATCHER_STATS",
            List.of(),
            new BuiltInFunctionDefinition("ES_WATCHER_STATS", (args, listener) -> {
                WatcherStatsRequest request = new WatcherStatsRequest();
                client.execute(WatcherStatsAction.INSTANCE, request, new ActionListener<WatcherStatsResponse>() {
                    @Override
                    public void onResponse(WatcherStatsResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("watcher_state", response.getWatcherMetadata().manuallyStopped() ? "stopped" : "started");
                        result.put("watch_count", response.getWatchesCount());
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
        name = "ES_START_WATCHER",
        description = "Start Watcher service",
        parameters = {},
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if acknowledged"),
        examples = {"ES_START_WATCHER()"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerStartWatcher(ExecutionContext context, Client client) {
        context.declareFunction("ES_START_WATCHER",
            List.of(),
            new BuiltInFunctionDefinition("ES_START_WATCHER", (args, listener) -> {
                WatcherServiceRequest request = new WatcherServiceRequest();
                request.start();
                
                client.execute(WatcherServiceAction.INSTANCE, request, new ActionListener<org.elasticsearch.action.support.master.AcknowledgedResponse>() {
                    @Override
                    public void onResponse(org.elasticsearch.action.support.master.AcknowledgedResponse response) {
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
        name = "ES_STOP_WATCHER",
        description = "Stop Watcher service",
        parameters = {},
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if acknowledged"),
        examples = {"ES_STOP_WATCHER()"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerStopWatcher(ExecutionContext context, Client client) {
        context.declareFunction("ES_STOP_WATCHER",
            List.of(),
            new BuiltInFunctionDefinition("ES_STOP_WATCHER", (args, listener) -> {
                WatcherServiceRequest request = new WatcherServiceRequest();
                request.stop();
                
                client.execute(WatcherServiceAction.INSTANCE, request, new ActionListener<org.elasticsearch.action.support.master.AcknowledgedResponse>() {
                    @Override
                    public void onResponse(org.elasticsearch.action.support.master.AcknowledgedResponse response) {
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
}
