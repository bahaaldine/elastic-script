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
 * Elasticsearch Data Stream API functions using REST HTTP calls.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.DATASOURCE,
    description = "Elasticsearch Data Stream API functions."
)
public class DataStreamApiFunctions extends ElasticsearchHttpFunctions {

    public static void registerAll(ExecutionContext context) {
        registerCreateDataStream(context);
        registerDeleteDataStream(context);
        registerGetDataStream(context);
        registerDataStreamStats(context);
        registerMigrateToDataStream(context);
        registerModifyDataStream(context);
    }

    @FunctionSpec(
        name = "ES_CREATE_DATA_STREAM",
        description = "Create a data stream",
        parameters = {
            @FunctionParam(name = "name", type = "STRING", description = "Data stream name")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Acknowledgment"),
        examples = {"ES_CREATE_DATA_STREAM('logs-my-app')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerCreateDataStream(ExecutionContext context) {
        context.declareFunction("ES_CREATE_DATA_STREAM",
            List.of(new Parameter("name", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_CREATE_DATA_STREAM", (args, listener) -> {
                try {
                    String name = toString(args.get(0));
                    Map<String, Object> result = esRequest("PUT", "/_data_stream/" + name, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_CREATE_DATA_STREAM failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_DELETE_DATA_STREAM",
        description = "Delete a data stream",
        parameters = {
            @FunctionParam(name = "name", type = "STRING", description = "Data stream name")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Acknowledgment"),
        examples = {"ES_DELETE_DATA_STREAM('logs-my-app')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDeleteDataStream(ExecutionContext context) {
        context.declareFunction("ES_DELETE_DATA_STREAM",
            List.of(new Parameter("name", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_DELETE_DATA_STREAM", (args, listener) -> {
                try {
                    String name = toString(args.get(0));
                    Map<String, Object> result = esRequest("DELETE", "/_data_stream/" + name, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_DELETE_DATA_STREAM failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_GET_DATA_STREAM",
        description = "Get data stream information",
        parameters = {
            @FunctionParam(name = "name", type = "STRING", description = "Data stream name or pattern")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Data stream information"),
        examples = {"ES_GET_DATA_STREAM('logs-*')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetDataStream(ExecutionContext context) {
        context.declareFunction("ES_GET_DATA_STREAM",
            List.of(new Parameter("name", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_GET_DATA_STREAM", (args, listener) -> {
                try {
                    String name = args.size() > 0 && args.get(0) != null ? toString(args.get(0)) : "*";
                    Map<String, Object> result = esRequest("GET", "/_data_stream/" + name, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_GET_DATA_STREAM failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_DATA_STREAM_STATS",
        description = "Get data stream statistics",
        parameters = {
            @FunctionParam(name = "name", type = "STRING", description = "Data stream name or pattern")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Data stream statistics"),
        examples = {"ES_DATA_STREAM_STATS('logs-*')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDataStreamStats(ExecutionContext context) {
        context.declareFunction("ES_DATA_STREAM_STATS",
            List.of(new Parameter("name", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_DATA_STREAM_STATS", (args, listener) -> {
                try {
                    String name = args.size() > 0 && args.get(0) != null ? toString(args.get(0)) : "*";
                    Map<String, Object> result = esRequest("GET", "/_data_stream/" + name + "/_stats", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_DATA_STREAM_STATS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_MIGRATE_TO_DATA_STREAM",
        description = "Migrate an index alias to a data stream",
        parameters = {
            @FunctionParam(name = "name", type = "STRING", description = "Alias name to migrate")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Acknowledgment"),
        examples = {"ES_MIGRATE_TO_DATA_STREAM('my-alias')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerMigrateToDataStream(ExecutionContext context) {
        context.declareFunction("ES_MIGRATE_TO_DATA_STREAM",
            List.of(new Parameter("name", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_MIGRATE_TO_DATA_STREAM", (args, listener) -> {
                try {
                    String name = toString(args.get(0));
                    Map<String, Object> result = esRequest("POST", "/_data_stream/_migrate/" + name, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_MIGRATE_TO_DATA_STREAM failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_MODIFY_DATA_STREAM",
        description = "Modify a data stream (add/remove backing indices)",
        parameters = {
            @FunctionParam(name = "actions", type = "DOCUMENT", description = "Actions to perform")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Acknowledgment"),
        examples = {"ES_MODIFY_DATA_STREAM({'actions': [{'add_backing_index': {'data_stream': 'logs', 'index': '.ds-logs-000001'}}]})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerModifyDataStream(ExecutionContext context) {
        context.declareFunction("ES_MODIFY_DATA_STREAM",
            List.of(new Parameter("actions", "DOCUMENT", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_MODIFY_DATA_STREAM", (args, listener) -> {
                try {
                    Map<String, Object> actions = toMap(args.get(0));
                    Map<String, Object> result = esRequest("POST", "/_data_stream/_modify", actions);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_MODIFY_DATA_STREAM failed: " + e.getMessage(), e));
                }
            })
        );
    }
}
