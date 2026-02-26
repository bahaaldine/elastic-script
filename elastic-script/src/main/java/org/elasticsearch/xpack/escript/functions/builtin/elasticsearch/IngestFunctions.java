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
import org.elasticsearch.action.ingest.DeletePipelineRequest;
import org.elasticsearch.action.ingest.GetPipelineRequest;
import org.elasticsearch.action.ingest.GetPipelineResponse;
import org.elasticsearch.action.ingest.PutPipelineRequest;
import org.elasticsearch.action.ingest.SimulatePipelineRequest;
import org.elasticsearch.action.ingest.SimulatePipelineResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch Ingest Pipeline API functions.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.DATASOURCE,
    description = "Elasticsearch Ingest Pipeline API functions"
)
public class IngestFunctions {

    public static void registerAll(ExecutionContext context, Client client) {
        registerGetPipeline(context, client);
        registerPutPipeline(context, client);
        registerDeletePipeline(context, client);
        registerSimulatePipeline(context, client);
        registerGrokProcessor(context, client);
    }

    @FunctionSpec(
        name = "ES_GET_PIPELINE",
        description = "Get ingest pipeline definitions",
        parameters = {
            @FunctionParam(name = "pipeline_id", type = "STRING", description = "Pipeline ID or pattern")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Pipeline configurations"),
        examples = {"ES_GET_PIPELINE('*')", "ES_GET_PIPELINE('my-pipeline')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetPipeline(ExecutionContext context, Client client) {
        context.declareFunction("ES_GET_PIPELINE",
            List.of(new Parameter("pipeline_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_GET_PIPELINE", (args, listener) -> {
                String pipelineId = args.size() > 0 && args.get(0) != null ? args.get(0).toString() : "*";
                
                GetPipelineRequest request = new GetPipelineRequest(pipelineId);
                client.admin().cluster().getPipeline(request, new ActionListener<GetPipelineResponse>() {
                    @Override
                    public void onResponse(GetPipelineResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        for (var pipeline : response.pipelines()) {
                            Map<String, Object> p = new HashMap<>();
                            p.put("id", pipeline.getId());
                            p.put("config", pipeline.getConfigAsMap());
                            result.put(pipeline.getId(), p);
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
        name = "ES_PUT_PIPELINE",
        description = "Create or update an ingest pipeline",
        parameters = {
            @FunctionParam(name = "pipeline_id", type = "STRING", description = "Pipeline ID"),
            @FunctionParam(name = "description", type = "STRING", description = "Pipeline description"),
            @FunctionParam(name = "processors", type = "ARRAY", description = "Array of processor definitions")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if acknowledged"),
        examples = {"ES_PUT_PIPELINE('my-pipeline', 'Description', [{'set': {'field': 'foo', 'value': 'bar'}}])"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerPutPipeline(ExecutionContext context, Client client) {
        context.declareFunction("ES_PUT_PIPELINE",
            Arrays.asList(
                new Parameter("pipeline_id", "STRING", ParameterMode.IN),
                new Parameter("description", "STRING", ParameterMode.IN),
                new Parameter("processors", "ARRAY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_PUT_PIPELINE", (args, listener) -> {
                String pipelineId = args.get(0).toString();
                String description = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : "";
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> processors = (List<Map<String, Object>>) args.get(2);
                
                Map<String, Object> pipelineConfig = new HashMap<>();
                pipelineConfig.put("description", description);
                pipelineConfig.put("processors", processors);
                
                String json;
                try {
                    var builder = org.elasticsearch.xcontent.XContentFactory.jsonBuilder();
                    builder.map(pipelineConfig);
                    json = org.elasticsearch.common.Strings.toString(builder);
                } catch (Exception e) {
                    listener.onFailure(e);
                    return;
                }
                
                PutPipelineRequest request = new PutPipelineRequest(pipelineId, 
                    new BytesArray(json.getBytes(StandardCharsets.UTF_8)), XContentType.JSON);
                
                client.admin().cluster().putPipeline(request, new ActionListener<AcknowledgedResponse>() {
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
        name = "ES_DELETE_PIPELINE",
        description = "Delete an ingest pipeline",
        parameters = {
            @FunctionParam(name = "pipeline_id", type = "STRING", description = "Pipeline ID")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if acknowledged"),
        examples = {"ES_DELETE_PIPELINE('old-pipeline')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDeletePipeline(ExecutionContext context, Client client) {
        context.declareFunction("ES_DELETE_PIPELINE",
            List.of(new Parameter("pipeline_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_DELETE_PIPELINE", (args, listener) -> {
                String pipelineId = args.get(0).toString();
                
                DeletePipelineRequest request = new DeletePipelineRequest(pipelineId);
                client.admin().cluster().deletePipeline(request, new ActionListener<AcknowledgedResponse>() {
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
        name = "ES_SIMULATE_PIPELINE",
        description = "Simulate running a pipeline on documents",
        parameters = {
            @FunctionParam(name = "pipeline_id", type = "STRING", description = "Pipeline ID (null for inline)"),
            @FunctionParam(name = "docs", type = "ARRAY", description = "Documents to simulate"),
            @FunctionParam(name = "pipeline", type = "DOCUMENT", description = "Inline pipeline definition")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Simulated results"),
        examples = {"ES_SIMULATE_PIPELINE('my-pipeline', [{'_source': {'foo': 'bar'}}], null)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerSimulatePipeline(ExecutionContext context, Client client) {
        context.declareFunction("ES_SIMULATE_PIPELINE",
            Arrays.asList(
                new Parameter("pipeline_id", "STRING", ParameterMode.IN),
                new Parameter("docs", "ARRAY", ParameterMode.IN),
                new Parameter("pipeline", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_SIMULATE_PIPELINE", (args, listener) -> {
                String pipelineId = args.size() > 0 && args.get(0) != null ? args.get(0).toString() : null;
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> docs = (List<Map<String, Object>>) args.get(1);
                @SuppressWarnings("unchecked")
                Map<String, Object> inlinePipeline = args.size() > 2 ? (Map<String, Object>) args.get(2) : null;
                
                Map<String, Object> body = new HashMap<>();
                body.put("docs", docs);
                if (inlinePipeline != null) {
                    body.put("pipeline", inlinePipeline);
                }
                
                String json;
                try {
                    var builder = org.elasticsearch.xcontent.XContentFactory.jsonBuilder();
                    builder.map(body);
                    json = org.elasticsearch.common.Strings.toString(builder);
                } catch (Exception e) {
                    listener.onFailure(e);
                    return;
                }
                
                SimulatePipelineRequest request = new SimulatePipelineRequest(
                    new BytesArray(json.getBytes(StandardCharsets.UTF_8)), XContentType.JSON);
                if (pipelineId != null) {
                    request.setId(pipelineId);
                }
                
                client.admin().cluster().simulatePipeline(request, new ActionListener<SimulatePipelineResponse>() {
                    @Override
                    public void onResponse(SimulatePipelineResponse response) {
                        List<Map<String, Object>> results = new java.util.ArrayList<>();
                        for (var result : response.getResults()) {
                            Map<String, Object> r = new HashMap<>();
                            if (result instanceof org.elasticsearch.action.ingest.SimulateDocumentBaseResult) {
                                var baseResult = (org.elasticsearch.action.ingest.SimulateDocumentBaseResult) result;
                                if (baseResult.getIngestDocument() != null) {
                                    r.put("doc", baseResult.getIngestDocument().getSource());
                                }
                                if (baseResult.getFailure() != null) {
                                    r.put("error", baseResult.getFailure().getMessage());
                                }
                            }
                            results.add(r);
                        }
                        listener.onResponse(results);
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
        name = "ES_GROK_PATTERNS",
        description = "Get built-in Grok patterns",
        parameters = {},
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Available Grok patterns"),
        examples = {"ES_GROK_PATTERNS()"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGrokProcessor(ExecutionContext context, Client client) {
        context.declareFunction("ES_GROK_PATTERNS",
            List.of(),
            new BuiltInFunctionDefinition("ES_GROK_PATTERNS", (args, listener) -> {
                org.elasticsearch.action.ingest.GetGrokPatternsAction.Request request = 
                    new org.elasticsearch.action.ingest.GetGrokPatternsAction.Request();
                
                client.execute(org.elasticsearch.action.ingest.GetGrokPatternsAction.INSTANCE, request,
                    new ActionListener<org.elasticsearch.action.ingest.GetGrokPatternsAction.Response>() {
                        @Override
                        public void onResponse(org.elasticsearch.action.ingest.GetGrokPatternsAction.Response response) {
                            listener.onResponse(response.getGrokPatterns());
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
