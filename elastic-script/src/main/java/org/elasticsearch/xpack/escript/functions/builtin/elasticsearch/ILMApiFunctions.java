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
 * Elasticsearch ILM (Index Lifecycle Management) API functions using REST HTTP calls.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.DATASOURCE,
    description = "Elasticsearch ILM API functions for lifecycle policy management."
)
public class ILMApiFunctions extends ElasticsearchHttpFunctions {

    public static void registerAll(ExecutionContext context) {
        registerGetPolicy(context);
        registerPutPolicy(context);
        registerDeletePolicy(context);
        registerExplain(context);
        registerMoveToStep(context);
        registerRetry(context);
        registerRemovePolicy(context);
        registerStatus(context);
        registerStart(context);
        registerStop(context);
    }

    @FunctionSpec(
        name = "ES_ILM_GET_POLICY",
        description = "Get ILM policy definition",
        parameters = {
            @FunctionParam(name = "policy_id", type = "STRING", description = "Policy ID or empty for all")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "ILM policy"),
        examples = {"ES_ILM_GET_POLICY('my-policy')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetPolicy(ExecutionContext context) {
        context.declareFunction("ES_ILM_GET_POLICY",
            List.of(new Parameter("policy_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_ILM_GET_POLICY", (args, listener) -> {
                try {
                    String policyId = args.size() > 0 && args.get(0) != null ? toString(args.get(0)) : "";
                    String path = policyId.isEmpty() ? "/_ilm/policy" : "/_ilm/policy/" + policyId;
                    Map<String, Object> result = esRequest("GET", path, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_ILM_GET_POLICY failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_ILM_PUT_POLICY",
        description = "Create or update an ILM policy",
        parameters = {
            @FunctionParam(name = "policy_id", type = "STRING", description = "Policy ID"),
            @FunctionParam(name = "policy", type = "DOCUMENT", description = "Policy definition")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Acknowledgment"),
        examples = {"ES_ILM_PUT_POLICY('my-policy', {'policy': {'phases': {...}}})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerPutPolicy(ExecutionContext context) {
        context.declareFunction("ES_ILM_PUT_POLICY",
            List.of(
                new Parameter("policy_id", "STRING", ParameterMode.IN),
                new Parameter("policy", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_ILM_PUT_POLICY", (args, listener) -> {
                try {
                    String policyId = toString(args.get(0));
                    Map<String, Object> policy = toMap(args.get(1));
                    Map<String, Object> result = esRequest("PUT", "/_ilm/policy/" + policyId, policy);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_ILM_PUT_POLICY failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_ILM_DELETE_POLICY",
        description = "Delete an ILM policy",
        parameters = {
            @FunctionParam(name = "policy_id", type = "STRING", description = "Policy ID to delete")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Acknowledgment"),
        examples = {"ES_ILM_DELETE_POLICY('my-policy')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDeletePolicy(ExecutionContext context) {
        context.declareFunction("ES_ILM_DELETE_POLICY",
            List.of(new Parameter("policy_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_ILM_DELETE_POLICY", (args, listener) -> {
                try {
                    String policyId = toString(args.get(0));
                    Map<String, Object> result = esRequest("DELETE", "/_ilm/policy/" + policyId, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_ILM_DELETE_POLICY failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_ILM_EXPLAIN",
        description = "Explain lifecycle state of an index",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name or pattern")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Lifecycle explanation"),
        examples = {"ES_ILM_EXPLAIN('my-index-*')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerExplain(ExecutionContext context) {
        context.declareFunction("ES_ILM_EXPLAIN",
            List.of(new Parameter("index", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_ILM_EXPLAIN", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    Map<String, Object> result = esRequest("GET", "/" + index + "/_ilm/explain", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_ILM_EXPLAIN failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_ILM_MOVE_TO_STEP",
        description = "Move an index to a specific ILM step",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "current_step", type = "DOCUMENT", description = "Current step"),
            @FunctionParam(name = "next_step", type = "DOCUMENT", description = "Next step")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Acknowledgment"),
        examples = {"ES_ILM_MOVE_TO_STEP('my-index', {'phase':'hot','action':'rollover','name':'check-rollover-ready'}, {'phase':'delete','action':'delete','name':'delete'})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerMoveToStep(ExecutionContext context) {
        context.declareFunction("ES_ILM_MOVE_TO_STEP",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("current_step", "DOCUMENT", ParameterMode.IN),
                new Parameter("next_step", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_ILM_MOVE_TO_STEP", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    Map<String, Object> currentStep = toMap(args.get(1));
                    Map<String, Object> nextStep = toMap(args.get(2));
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("current_step", currentStep);
                    body.put("next_step", nextStep);
                    
                    Map<String, Object> result = esRequest("POST", "/_ilm/move/" + index, body);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_ILM_MOVE_TO_STEP failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_ILM_RETRY",
        description = "Retry a failed ILM step",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Acknowledgment"),
        examples = {"ES_ILM_RETRY('my-index')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerRetry(ExecutionContext context) {
        context.declareFunction("ES_ILM_RETRY",
            List.of(new Parameter("index", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_ILM_RETRY", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    Map<String, Object> result = esRequest("POST", "/" + index + "/_ilm/retry", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_ILM_RETRY failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_ILM_REMOVE_POLICY",
        description = "Remove ILM policy from an index",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Acknowledgment"),
        examples = {"ES_ILM_REMOVE_POLICY('my-index')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerRemovePolicy(ExecutionContext context) {
        context.declareFunction("ES_ILM_REMOVE_POLICY",
            List.of(new Parameter("index", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_ILM_REMOVE_POLICY", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    Map<String, Object> result = esRequest("POST", "/" + index + "/_ilm/remove", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_ILM_REMOVE_POLICY failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_ILM_STATUS",
        description = "Get ILM operation status",
        parameters = {},
        returnType = @FunctionReturn(type = "DOCUMENT", description = "ILM status"),
        examples = {"ES_ILM_STATUS()"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerStatus(ExecutionContext context) {
        context.declareFunction("ES_ILM_STATUS",
            List.of(),
            new BuiltInFunctionDefinition("ES_ILM_STATUS", (args, listener) -> {
                try {
                    Map<String, Object> result = esRequest("GET", "/_ilm/status", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_ILM_STATUS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_ILM_START",
        description = "Start the ILM plugin",
        parameters = {},
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Acknowledgment"),
        examples = {"ES_ILM_START()"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerStart(ExecutionContext context) {
        context.declareFunction("ES_ILM_START",
            List.of(),
            new BuiltInFunctionDefinition("ES_ILM_START", (args, listener) -> {
                try {
                    Map<String, Object> result = esRequest("POST", "/_ilm/start", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_ILM_START failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_ILM_STOP",
        description = "Stop the ILM plugin",
        parameters = {},
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Acknowledgment"),
        examples = {"ES_ILM_STOP()"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerStop(ExecutionContext context) {
        context.declareFunction("ES_ILM_STOP",
            List.of(),
            new BuiltInFunctionDefinition("ES_ILM_STOP", (args, listener) -> {
                try {
                    Map<String, Object> result = esRequest("POST", "/_ilm/stop", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_ILM_STOP failed: " + e.getMessage(), e));
                }
            })
        );
    }
}
