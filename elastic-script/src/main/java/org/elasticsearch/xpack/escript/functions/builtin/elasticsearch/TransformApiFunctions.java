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

import java.util.List;
import java.util.Map;

/**
 * Elasticsearch Transform API functions using REST HTTP calls.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.DATASOURCE,
    description = "Elasticsearch Transform API functions."
)
public class TransformApiFunctions extends ElasticsearchHttpFunctions {

    public static void registerAll(ExecutionContext context) {
        registerGetTransform(context);
        registerGetTransformStats(context);
        registerStartTransform(context);
        registerStopTransform(context);
        registerDeleteTransform(context);
        registerPreviewTransform(context);
        registerPutTransform(context);
        registerResetTransform(context);
    }

    @FunctionSpec(
        name = "ES_GET_TRANSFORM",
        description = "Get transform definition",
        parameters = {
            @FunctionParam(name = "transform_id", type = "STRING", description = "Transform ID or _all")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Transform definition"),
        examples = {"ES_GET_TRANSFORM('my-transform')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetTransform(ExecutionContext context) {
        context.declareFunction("ES_GET_TRANSFORM",
            List.of(new Parameter("transform_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_GET_TRANSFORM", (args, listener) -> {
                try {
                    String transformId = args.size() > 0 && args.get(0) != null ? toString(args.get(0)) : "_all";
                    Map<String, Object> result = esRequest("GET", "/_transform/" + transformId, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_GET_TRANSFORM failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_GET_TRANSFORM_STATS",
        description = "Get transform statistics",
        parameters = {
            @FunctionParam(name = "transform_id", type = "STRING", description = "Transform ID or _all")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Transform statistics"),
        examples = {"ES_GET_TRANSFORM_STATS('my-transform')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetTransformStats(ExecutionContext context) {
        context.declareFunction("ES_GET_TRANSFORM_STATS",
            List.of(new Parameter("transform_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_GET_TRANSFORM_STATS", (args, listener) -> {
                try {
                    String transformId = args.size() > 0 && args.get(0) != null ? toString(args.get(0)) : "_all";
                    Map<String, Object> result = esRequest("GET", "/_transform/" + transformId + "/_stats", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_GET_TRANSFORM_STATS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_START_TRANSFORM",
        description = "Start a transform",
        parameters = {
            @FunctionParam(name = "transform_id", type = "STRING", description = "Transform ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Start result"),
        examples = {"ES_START_TRANSFORM('my-transform')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerStartTransform(ExecutionContext context) {
        context.declareFunction("ES_START_TRANSFORM",
            List.of(new Parameter("transform_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_START_TRANSFORM", (args, listener) -> {
                try {
                    String transformId = toString(args.get(0));
                    Map<String, Object> result = esRequest("POST", "/_transform/" + transformId + "/_start", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_START_TRANSFORM failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_STOP_TRANSFORM",
        description = "Stop a transform",
        parameters = {
            @FunctionParam(name = "transform_id", type = "STRING", description = "Transform ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Stop result"),
        examples = {"ES_STOP_TRANSFORM('my-transform')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerStopTransform(ExecutionContext context) {
        context.declareFunction("ES_STOP_TRANSFORM",
            List.of(new Parameter("transform_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_STOP_TRANSFORM", (args, listener) -> {
                try {
                    String transformId = toString(args.get(0));
                    Map<String, Object> result = esRequest("POST", "/_transform/" + transformId + "/_stop", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_STOP_TRANSFORM failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_DELETE_TRANSFORM",
        description = "Delete a transform",
        parameters = {
            @FunctionParam(name = "transform_id", type = "STRING", description = "Transform ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Delete result"),
        examples = {"ES_DELETE_TRANSFORM('my-transform')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDeleteTransform(ExecutionContext context) {
        context.declareFunction("ES_DELETE_TRANSFORM",
            List.of(new Parameter("transform_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_DELETE_TRANSFORM", (args, listener) -> {
                try {
                    String transformId = toString(args.get(0));
                    Map<String, Object> result = esRequest("DELETE", "/_transform/" + transformId, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_DELETE_TRANSFORM failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_PREVIEW_TRANSFORM",
        description = "Preview a transform",
        parameters = {
            @FunctionParam(name = "transform_id", type = "STRING", description = "Transform ID or null for inline"),
            @FunctionParam(name = "config", type = "DOCUMENT", description = "Transform config for inline preview")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Preview result"),
        examples = {"ES_PREVIEW_TRANSFORM('my-transform', null)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerPreviewTransform(ExecutionContext context) {
        context.declareFunction("ES_PREVIEW_TRANSFORM",
            List.of(
                new Parameter("transform_id", "STRING", ParameterMode.IN),
                new Parameter("config", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_PREVIEW_TRANSFORM", (args, listener) -> {
                try {
                    String transformId = args.get(0) != null ? toString(args.get(0)) : null;
                    Map<String, Object> config = args.size() > 1 && args.get(1) != null ? toMap(args.get(1)) : null;
                    
                    String path = transformId != null && !transformId.isEmpty() 
                        ? "/_transform/" + transformId + "/_preview"
                        : "/_transform/_preview";
                    
                    Map<String, Object> result = esRequest("POST", path, config);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_PREVIEW_TRANSFORM failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_PUT_TRANSFORM",
        description = "Create a transform",
        parameters = {
            @FunctionParam(name = "transform_id", type = "STRING", description = "Transform ID"),
            @FunctionParam(name = "config", type = "DOCUMENT", description = "Transform configuration")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Create result"),
        examples = {"ES_PUT_TRANSFORM('my-transform', {'source': {...}, 'dest': {...}, 'pivot': {...}})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerPutTransform(ExecutionContext context) {
        context.declareFunction("ES_PUT_TRANSFORM",
            List.of(
                new Parameter("transform_id", "STRING", ParameterMode.IN),
                new Parameter("config", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_PUT_TRANSFORM", (args, listener) -> {
                try {
                    String transformId = toString(args.get(0));
                    Map<String, Object> config = toMap(args.get(1));
                    Map<String, Object> result = esRequest("PUT", "/_transform/" + transformId, config);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_PUT_TRANSFORM failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_RESET_TRANSFORM",
        description = "Reset a transform",
        parameters = {
            @FunctionParam(name = "transform_id", type = "STRING", description = "Transform ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Reset result"),
        examples = {"ES_RESET_TRANSFORM('my-transform')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerResetTransform(ExecutionContext context) {
        context.declareFunction("ES_RESET_TRANSFORM",
            List.of(new Parameter("transform_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_RESET_TRANSFORM", (args, listener) -> {
                try {
                    String transformId = toString(args.get(0));
                    Map<String, Object> result = esRequest("POST", "/_transform/" + transformId + "/_reset", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_RESET_TRANSFORM failed: " + e.getMessage(), e));
                }
            })
        );
    }
}
