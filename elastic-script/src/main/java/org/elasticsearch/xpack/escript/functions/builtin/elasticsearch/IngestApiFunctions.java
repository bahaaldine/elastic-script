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
 * Elasticsearch Ingest Pipeline API functions using REST HTTP calls.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.DATASOURCE,
    description = "Elasticsearch Ingest Pipeline API functions."
)
public class IngestApiFunctions extends ElasticsearchHttpFunctions {

    public static void registerAll(ExecutionContext context) {
        registerGetPipeline(context);
        registerPutPipeline(context);
        registerDeletePipeline(context);
        registerSimulatePipeline(context);
        registerGrokPatterns(context);
    }

    @FunctionSpec(
        name = "ES_GET_PIPELINE",
        description = "Get ingest pipeline definition",
        parameters = {
            @FunctionParam(name = "pipeline_id", type = "STRING", description = "Pipeline ID or empty for all")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Pipeline definition"),
        examples = {"ES_GET_PIPELINE('my-pipeline')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetPipeline(ExecutionContext context) {
        context.declareFunction("ES_GET_PIPELINE",
            List.of(new Parameter("pipeline_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_GET_PIPELINE", (args, listener) -> {
                try {
                    String pipelineId = args.size() > 0 && args.get(0) != null ? toString(args.get(0)) : "";
                    String path = pipelineId.isEmpty() ? "/_ingest/pipeline" : "/_ingest/pipeline/" + pipelineId;
                    Map<String, Object> result = esRequest("GET", path, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_GET_PIPELINE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_PUT_PIPELINE",
        description = "Create or update an ingest pipeline",
        parameters = {
            @FunctionParam(name = "pipeline_id", type = "STRING", description = "Pipeline ID"),
            @FunctionParam(name = "pipeline", type = "DOCUMENT", description = "Pipeline definition")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Acknowledgment"),
        examples = {"ES_PUT_PIPELINE('my-pipeline', {'description': 'My pipeline', 'processors': [...]})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerPutPipeline(ExecutionContext context) {
        context.declareFunction("ES_PUT_PIPELINE",
            List.of(
                new Parameter("pipeline_id", "STRING", ParameterMode.IN),
                new Parameter("pipeline", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_PUT_PIPELINE", (args, listener) -> {
                try {
                    String pipelineId = toString(args.get(0));
                    Map<String, Object> pipeline = toMap(args.get(1));
                    Map<String, Object> result = esRequest("PUT", "/_ingest/pipeline/" + pipelineId, pipeline);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_PUT_PIPELINE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_DELETE_PIPELINE",
        description = "Delete an ingest pipeline",
        parameters = {
            @FunctionParam(name = "pipeline_id", type = "STRING", description = "Pipeline ID to delete")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Acknowledgment"),
        examples = {"ES_DELETE_PIPELINE('my-pipeline')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDeletePipeline(ExecutionContext context) {
        context.declareFunction("ES_DELETE_PIPELINE",
            List.of(new Parameter("pipeline_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_DELETE_PIPELINE", (args, listener) -> {
                try {
                    String pipelineId = toString(args.get(0));
                    Map<String, Object> result = esRequest("DELETE", "/_ingest/pipeline/" + pipelineId, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_DELETE_PIPELINE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_SIMULATE_PIPELINE",
        description = "Simulate an ingest pipeline",
        parameters = {
            @FunctionParam(name = "pipeline_id", type = "STRING", description = "Pipeline ID (or null for inline)"),
            @FunctionParam(name = "docs", type = "ARRAY", description = "Documents to simulate"),
            @FunctionParam(name = "pipeline", type = "DOCUMENT", description = "Inline pipeline definition (if no ID)")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Simulation result"),
        examples = {"ES_SIMULATE_PIPELINE('my-pipeline', [{'_source': {'field': 'value'}}], null)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerSimulatePipeline(ExecutionContext context) {
        context.declareFunction("ES_SIMULATE_PIPELINE",
            List.of(
                new Parameter("pipeline_id", "STRING", ParameterMode.IN),
                new Parameter("docs", "ARRAY", ParameterMode.IN),
                new Parameter("pipeline", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_SIMULATE_PIPELINE", (args, listener) -> {
                try {
                    String pipelineId = args.get(0) != null ? toString(args.get(0)) : null;
                    List<?> docs = (List<?>) args.get(1);
                    Map<String, Object> inlinePipeline = args.size() > 2 && args.get(2) != null ? toMap(args.get(2)) : null;
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("docs", docs);
                    if (inlinePipeline != null) {
                        body.put("pipeline", inlinePipeline);
                    }
                    
                    String path = pipelineId != null && !pipelineId.isEmpty() 
                        ? "/_ingest/pipeline/" + pipelineId + "/_simulate"
                        : "/_ingest/pipeline/_simulate";
                    
                    Map<String, Object> result = esRequest("POST", path, body);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_SIMULATE_PIPELINE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_GROK_PATTERNS",
        description = "Get available grok patterns",
        parameters = {},
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Grok patterns"),
        examples = {"ES_GROK_PATTERNS()"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGrokPatterns(ExecutionContext context) {
        context.declareFunction("ES_GROK_PATTERNS",
            List.of(),
            new BuiltInFunctionDefinition("ES_GROK_PATTERNS", (args, listener) -> {
                try {
                    Map<String, Object> result = esRequest("GET", "/_ingest/processor/grok", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_GROK_PATTERNS failed: " + e.getMessage(), e));
                }
            })
        );
    }
}
