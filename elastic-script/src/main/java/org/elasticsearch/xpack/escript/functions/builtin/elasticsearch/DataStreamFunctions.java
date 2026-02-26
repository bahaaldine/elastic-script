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
import org.elasticsearch.action.datastreams.CreateDataStreamAction;
import org.elasticsearch.action.datastreams.DeleteDataStreamAction;
import org.elasticsearch.action.datastreams.GetDataStreamAction;
import org.elasticsearch.action.datastreams.DataStreamsStatsAction;
import org.elasticsearch.action.datastreams.ModifyDataStreamsAction;
import org.elasticsearch.action.datastreams.MigrateToDataStreamAction;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch Data Stream API functions.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.DATASOURCE,
    description = "Elasticsearch Data Stream API functions"
)
public class DataStreamFunctions {

    public static void registerAll(ExecutionContext context, Client client) {
        registerCreateDataStream(context, client);
        registerDeleteDataStream(context, client);
        registerGetDataStream(context, client);
        registerDataStreamStats(context, client);
        registerMigrateToDataStream(context, client);
        registerPromoteDataStream(context, client);
    }

    @FunctionSpec(
        name = "ES_CREATE_DATA_STREAM",
        description = "Create a data stream",
        parameters = {
            @FunctionParam(name = "name", type = "STRING", description = "Data stream name")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if acknowledged"),
        examples = {"ES_CREATE_DATA_STREAM('logs-myapp-default')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerCreateDataStream(ExecutionContext context, Client client) {
        context.declareFunction("ES_CREATE_DATA_STREAM",
            List.of(new Parameter("name", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_CREATE_DATA_STREAM", (args, listener) -> {
                String name = args.get(0).toString();
                
                CreateDataStreamAction.Request request = new CreateDataStreamAction.Request(name);
                client.execute(CreateDataStreamAction.INSTANCE, request, new ActionListener<AcknowledgedResponse>() {
                    @Override
                    public void onResponse(AcknowledgedResponse response) {
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
        name = "ES_DELETE_DATA_STREAM",
        description = "Delete a data stream",
        parameters = {
            @FunctionParam(name = "name", type = "STRING", description = "Data stream name or pattern")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if acknowledged"),
        examples = {"ES_DELETE_DATA_STREAM('logs-myapp-*')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDeleteDataStream(ExecutionContext context, Client client) {
        context.declareFunction("ES_DELETE_DATA_STREAM",
            List.of(new Parameter("name", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_DELETE_DATA_STREAM", (args, listener) -> {
                String name = args.get(0).toString();
                
                DeleteDataStreamAction.Request request = new DeleteDataStreamAction.Request(new String[]{name});
                client.execute(DeleteDataStreamAction.INSTANCE, request, new ActionListener<AcknowledgedResponse>() {
                    @Override
                    public void onResponse(AcknowledgedResponse response) {
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
        name = "ES_GET_DATA_STREAM",
        description = "Get data stream information",
        parameters = {
            @FunctionParam(name = "name", type = "STRING", description = "Data stream name or pattern")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Data stream details"),
        examples = {"ES_GET_DATA_STREAM('logs-*')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetDataStream(ExecutionContext context, Client client) {
        context.declareFunction("ES_GET_DATA_STREAM",
            List.of(new Parameter("name", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_GET_DATA_STREAM", (args, listener) -> {
                String name = args.size() > 0 && args.get(0) != null ? args.get(0).toString() : "*";
                
                GetDataStreamAction.Request request = new GetDataStreamAction.Request(new String[]{name});
                client.execute(GetDataStreamAction.INSTANCE, request, new ActionListener<GetDataStreamAction.Response>() {
                    @Override
                    public void onResponse(GetDataStreamAction.Response response) {
                        List<Map<String, Object>> result = new java.util.ArrayList<>();
                        for (var ds : response.getDataStreams()) {
                            Map<String, Object> dsInfo = new HashMap<>();
                            dsInfo.put("name", ds.getDataStream().getName());
                            dsInfo.put("timestamp_field", ds.getDataStream().getTimeStampField().getName());
                            dsInfo.put("generation", ds.getDataStream().getGeneration());
                            dsInfo.put("status", ds.getDataStreamStatus().name());
                            dsInfo.put("template", ds.getIndexTemplate());
                            dsInfo.put("ilm_policy", ds.getIlmPolicyName());
                            
                            List<String> indices = new java.util.ArrayList<>();
                            for (var idx : ds.getDataStream().getIndices()) {
                                indices.add(idx.getName());
                            }
                            dsInfo.put("indices", indices);
                            result.add(dsInfo);
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
        name = "ES_DATA_STREAM_STATS",
        description = "Get data stream statistics",
        parameters = {
            @FunctionParam(name = "name", type = "STRING", description = "Data stream name or pattern")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Data stream stats"),
        examples = {"ES_DATA_STREAM_STATS('logs-*')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDataStreamStats(ExecutionContext context, Client client) {
        context.declareFunction("ES_DATA_STREAM_STATS",
            List.of(new Parameter("name", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_DATA_STREAM_STATS", (args, listener) -> {
                String name = args.size() > 0 && args.get(0) != null ? args.get(0).toString() : "*";
                
                DataStreamsStatsAction.Request request = new DataStreamsStatsAction.Request();
                request.indices(name);
                
                client.execute(DataStreamsStatsAction.INSTANCE, request, new ActionListener<DataStreamsStatsAction.Response>() {
                    @Override
                    public void onResponse(DataStreamsStatsAction.Response response) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("data_stream_count", response.getDataStreamCount());
                        result.put("backing_indices", response.getBackingIndices());
                        result.put("total_store_size_bytes", response.getTotalStoreSize().getBytes());
                        
                        List<Map<String, Object>> streams = new java.util.ArrayList<>();
                        for (var stat : response.getDataStreams()) {
                            Map<String, Object> s = new HashMap<>();
                            s.put("data_stream", stat.getDataStream());
                            s.put("backing_indices", stat.getBackingIndices());
                            s.put("store_size_bytes", stat.getStoreSize().getBytes());
                            s.put("maximum_timestamp", stat.getMaximumTimestamp());
                            streams.add(s);
                        }
                        result.put("data_streams", streams);
                        
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
        name = "ES_MIGRATE_TO_DATA_STREAM",
        description = "Migrate index alias to data stream",
        parameters = {
            @FunctionParam(name = "name", type = "STRING", description = "Alias name to migrate")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if acknowledged"),
        examples = {"ES_MIGRATE_TO_DATA_STREAM('logs-alias')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerMigrateToDataStream(ExecutionContext context, Client client) {
        context.declareFunction("ES_MIGRATE_TO_DATA_STREAM",
            List.of(new Parameter("name", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_MIGRATE_TO_DATA_STREAM", (args, listener) -> {
                String name = args.get(0).toString();
                
                MigrateToDataStreamAction.Request request = new MigrateToDataStreamAction.Request(name);
                client.execute(MigrateToDataStreamAction.INSTANCE, request, new ActionListener<AcknowledgedResponse>() {
                    @Override
                    public void onResponse(AcknowledgedResponse response) {
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
        name = "ES_PROMOTE_DATA_STREAM",
        description = "Promote a replicated data stream to regular",
        parameters = {
            @FunctionParam(name = "name", type = "STRING", description = "Data stream name")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if acknowledged"),
        examples = {"ES_PROMOTE_DATA_STREAM('logs-replicated')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerPromoteDataStream(ExecutionContext context, Client client) {
        context.declareFunction("ES_PROMOTE_DATA_STREAM",
            List.of(new Parameter("name", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_PROMOTE_DATA_STREAM", (args, listener) -> {
                String name = args.get(0).toString();
                
                org.elasticsearch.action.datastreams.PromoteDataStreamAction.Request request = 
                    new org.elasticsearch.action.datastreams.PromoteDataStreamAction.Request(name);
                client.execute(org.elasticsearch.action.datastreams.PromoteDataStreamAction.INSTANCE, request, 
                    new ActionListener<AcknowledgedResponse>() {
                        @Override
                        public void onResponse(AcknowledgedResponse response) {
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
