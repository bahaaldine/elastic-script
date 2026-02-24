/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.builtin.kibana;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;
import org.elasticsearch.xpack.escript.functions.api.FunctionCategory;
import org.elasticsearch.xpack.escript.functions.api.FunctionCollectionSpec;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.xpack.escript.functions.builtin.kibana.KibanaFunctions.*;

/**
 * Kibana SLO API functions for elastic-script.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.RUNBOOK,
    description = "SLO management functions for Service Level Objectives."
)
public class SloFunctions {

    public static void registerAll(ExecutionContext context) {
        registerSloList(context);
        registerSloGet(context);
        registerSloCreate(context);
        registerSloUpdate(context);
        registerSloDelete(context);
        registerSloEnable(context);
        registerSloDisable(context);
        registerSloReset(context);
        registerSloBurnRates(context);
    }

    public static void registerSloList(ExecutionContext context) {
        context.declareFunction("SLO_LIST",
            List.of(
                new Parameter("name", "STRING", ParameterMode.IN),
                new Parameter("page", "INTEGER", ParameterMode.IN),
                new Parameter("per_page", "INTEGER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SLO_LIST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String name = args.size() > 0 && args.get(0) != null ? args.get(0).toString() : "";
                    int page = args.size() > 1 && args.get(1) != null ? ((Number) args.get(1)).intValue() : 1;
                    int perPage = args.size() > 2 && args.get(2) != null ? ((Number) args.get(2)).intValue() : 25;
                    
                    StringBuilder path = new StringBuilder("/api/observability/slos?page=" + page + "&perPage=" + perPage);
                    if (!name.isEmpty()) {
                        path.append("&kqlQuery=").append(java.net.URLEncoder.encode("slo.name: *" + name + "*", "UTF-8"));
                    }
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", path.toString(), null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SLO_LIST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerSloGet(ExecutionContext context) {
        context.declareFunction("SLO_GET",
            List.of(new Parameter("slo_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("SLO_GET", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String sloId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/observability/slos/" + sloId, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SLO_GET failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerSloCreate(ExecutionContext context) {
        context.declareFunction("SLO_CREATE",
            List.of(new Parameter("config", "DOCUMENT", ParameterMode.IN)),
            new BuiltInFunctionDefinition("SLO_CREATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    Map<String, Object> config = toMap(args.get(0));
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/observability/slos", config);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SLO_CREATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerSloUpdate(ExecutionContext context) {
        context.declareFunction("SLO_UPDATE",
            List.of(
                new Parameter("slo_id", "STRING", ParameterMode.IN),
                new Parameter("updates", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SLO_UPDATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String sloId = args.get(0).toString();
                    Map<String, Object> updates = toMap(args.get(1));
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "PUT", "/api/observability/slos/" + sloId, updates);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SLO_UPDATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerSloDelete(ExecutionContext context) {
        context.declareFunction("SLO_DELETE",
            List.of(new Parameter("slo_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("SLO_DELETE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String sloId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "DELETE", "/api/observability/slos/" + sloId, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SLO_DELETE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerSloEnable(ExecutionContext context) {
        context.declareFunction("SLO_ENABLE",
            List.of(new Parameter("slo_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("SLO_ENABLE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String sloId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/observability/slos/" + sloId + "/enable", new HashMap<>());
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SLO_ENABLE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerSloDisable(ExecutionContext context) {
        context.declareFunction("SLO_DISABLE",
            List.of(new Parameter("slo_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("SLO_DISABLE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String sloId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/observability/slos/" + sloId + "/disable", new HashMap<>());
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SLO_DISABLE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerSloReset(ExecutionContext context) {
        context.declareFunction("SLO_RESET",
            List.of(new Parameter("slo_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("SLO_RESET", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String sloId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/observability/slos/" + sloId + "/_reset", new HashMap<>());
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SLO_RESET failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerSloBurnRates(ExecutionContext context) {
        context.declareFunction("SLO_BURN_RATES",
            List.of(new Parameter("slo_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("SLO_BURN_RATES", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String sloId = args.get(0).toString();
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("sloIds", List.of(sloId));
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/internal/observability/slos/burn_rates", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SLO_BURN_RATES failed: " + e.getMessage(), e));
                }
            })
        );
    }
}
