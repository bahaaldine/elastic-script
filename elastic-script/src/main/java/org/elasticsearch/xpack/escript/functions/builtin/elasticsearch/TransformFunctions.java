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
import org.elasticsearch.xpack.core.transform.action.GetTransformAction;
import org.elasticsearch.xpack.core.transform.action.GetTransformStatsAction;
import org.elasticsearch.xpack.core.transform.action.StartTransformAction;
import org.elasticsearch.xpack.core.transform.action.StopTransformAction;
import org.elasticsearch.xpack.core.transform.action.DeleteTransformAction;
import org.elasticsearch.xpack.core.transform.action.PreviewTransformAction;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch Transform API functions.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.DATASOURCE,
    description = "Elasticsearch Transform API functions"
)
public class TransformFunctions {

    public static void registerAll(ExecutionContext context, Client client) {
        registerGetTransform(context, client);
        registerGetTransformStats(context, client);
        registerStartTransform(context, client);
        registerStopTransform(context, client);
        registerDeleteTransform(context, client);
        registerPreviewTransform(context, client);
    }

    @FunctionSpec(
        name = "ES_GET_TRANSFORM",
        description = "Get transform configurations",
        parameters = {
            @FunctionParam(name = "transform_id", type = "STRING", description = "Transform ID or pattern")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Transform configurations"),
        examples = {"ES_GET_TRANSFORM('*')", "ES_GET_TRANSFORM('my-transform')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetTransform(ExecutionContext context, Client client) {
        context.declareFunction("ES_GET_TRANSFORM",
            List.of(new Parameter("transform_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_GET_TRANSFORM", (args, listener) -> {
                String transformId = args.size() > 0 && args.get(0) != null ? args.get(0).toString() : "*";
                
                GetTransformAction.Request request = new GetTransformAction.Request(transformId);
                client.execute(GetTransformAction.INSTANCE, request, new ActionListener<GetTransformAction.Response>() {
                    @Override
                    public void onResponse(GetTransformAction.Response response) {
                        List<Map<String, Object>> transforms = new java.util.ArrayList<>();
                        for (var t : response.getTransformConfigurations()) {
                            Map<String, Object> transform = new HashMap<>();
                            transform.put("id", t.getId());
                            transform.put("source_index", t.getSource().getIndex());
                            transform.put("dest_index", t.getDestination().getIndex());
                            transform.put("description", t.getDescription());
                            transforms.add(transform);
                        }
                        listener.onResponse(transforms);
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
        name = "ES_GET_TRANSFORM_STATS",
        description = "Get transform statistics",
        parameters = {
            @FunctionParam(name = "transform_id", type = "STRING", description = "Transform ID or pattern")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Transform statistics"),
        examples = {"ES_GET_TRANSFORM_STATS('my-transform')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetTransformStats(ExecutionContext context, Client client) {
        context.declareFunction("ES_GET_TRANSFORM_STATS",
            List.of(new Parameter("transform_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_GET_TRANSFORM_STATS", (args, listener) -> {
                String transformId = args.size() > 0 && args.get(0) != null ? args.get(0).toString() : "*";
                
                GetTransformStatsAction.Request request = new GetTransformStatsAction.Request(transformId);
                client.execute(GetTransformStatsAction.INSTANCE, request, new ActionListener<GetTransformStatsAction.Response>() {
                    @Override
                    public void onResponse(GetTransformStatsAction.Response response) {
                        List<Map<String, Object>> stats = new java.util.ArrayList<>();
                        for (var ts : response.getTransformsStats()) {
                            Map<String, Object> s = new HashMap<>();
                            s.put("id", ts.getId());
                            s.put("state", ts.getState().value());
                            if (ts.getTransformStats() != null) {
                                s.put("documents_processed", ts.getTransformStats().getNumDocuments());
                                s.put("pages_processed", ts.getTransformStats().getNumPages());
                            }
                            stats.add(s);
                        }
                        listener.onResponse(stats);
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
        name = "ES_START_TRANSFORM",
        description = "Start a transform",
        parameters = {
            @FunctionParam(name = "transform_id", type = "STRING", description = "Transform ID")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if acknowledged"),
        examples = {"ES_START_TRANSFORM('my-transform')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerStartTransform(ExecutionContext context, Client client) {
        context.declareFunction("ES_START_TRANSFORM",
            List.of(new Parameter("transform_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_START_TRANSFORM", (args, listener) -> {
                String transformId = args.get(0).toString();
                
                StartTransformAction.Request request = new StartTransformAction.Request(transformId);
                client.execute(StartTransformAction.INSTANCE, request, new ActionListener<StartTransformAction.Response>() {
                    @Override
                    public void onResponse(StartTransformAction.Response response) {
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
        name = "ES_STOP_TRANSFORM",
        description = "Stop a transform",
        parameters = {
            @FunctionParam(name = "transform_id", type = "STRING", description = "Transform ID"),
            @FunctionParam(name = "force", type = "BOOLEAN", description = "Force stop")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if acknowledged"),
        examples = {"ES_STOP_TRANSFORM('my-transform', false)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerStopTransform(ExecutionContext context, Client client) {
        context.declareFunction("ES_STOP_TRANSFORM",
            Arrays.asList(
                new Parameter("transform_id", "STRING", ParameterMode.IN),
                new Parameter("force", "BOOLEAN", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_STOP_TRANSFORM", (args, listener) -> {
                String transformId = args.get(0).toString();
                boolean force = args.size() > 1 && Boolean.TRUE.equals(args.get(1));
                
                StopTransformAction.Request request = new StopTransformAction.Request(transformId, true, force, null, true, false);
                client.execute(StopTransformAction.INSTANCE, request, new ActionListener<StopTransformAction.Response>() {
                    @Override
                    public void onResponse(StopTransformAction.Response response) {
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
        name = "ES_DELETE_TRANSFORM",
        description = "Delete a transform",
        parameters = {
            @FunctionParam(name = "transform_id", type = "STRING", description = "Transform ID"),
            @FunctionParam(name = "force", type = "BOOLEAN", description = "Force delete")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if acknowledged"),
        examples = {"ES_DELETE_TRANSFORM('my-transform', false)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDeleteTransform(ExecutionContext context, Client client) {
        context.declareFunction("ES_DELETE_TRANSFORM",
            Arrays.asList(
                new Parameter("transform_id", "STRING", ParameterMode.IN),
                new Parameter("force", "BOOLEAN", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_DELETE_TRANSFORM", (args, listener) -> {
                String transformId = args.get(0).toString();
                boolean force = args.size() > 1 && Boolean.TRUE.equals(args.get(1));
                
                DeleteTransformAction.Request request = new DeleteTransformAction.Request(transformId, force, false);
                client.execute(DeleteTransformAction.INSTANCE, request, new ActionListener<DeleteTransformAction.Response>() {
                    @Override
                    public void onResponse(DeleteTransformAction.Response response) {
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
        name = "ES_PREVIEW_TRANSFORM",
        description = "Preview transform results",
        parameters = {
            @FunctionParam(name = "transform_id", type = "STRING", description = "Transform ID")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Preview documents"),
        examples = {"ES_PREVIEW_TRANSFORM('my-transform')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerPreviewTransform(ExecutionContext context, Client client) {
        context.declareFunction("ES_PREVIEW_TRANSFORM",
            List.of(new Parameter("transform_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_PREVIEW_TRANSFORM", (args, listener) -> {
                String transformId = args.get(0).toString();
                
                PreviewTransformAction.Request request = new PreviewTransformAction.Request(transformId);
                client.execute(PreviewTransformAction.INSTANCE, request, new ActionListener<PreviewTransformAction.Response>() {
                    @Override
                    public void onResponse(PreviewTransformAction.Response response) {
                        listener.onResponse(response.getDocs());
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
