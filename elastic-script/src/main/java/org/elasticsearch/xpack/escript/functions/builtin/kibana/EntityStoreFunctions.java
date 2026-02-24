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
 * Kibana Entity Store API functions for elastic-script.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.RUNBOOK,
    description = "Entity store functions for security entity management."
)
public class EntityStoreFunctions {

    public static void registerAll(ExecutionContext context) {
        registerEntityStoreInit(context);
        registerEntityStoreStatus(context);
        registerEntityStoreDelete(context);
        registerEntityList(context);
        registerEntityGet(context);
        registerAssetCriticalityList(context);
        registerAssetCriticalitySet(context);
        registerAssetCriticalityDelete(context);
        registerRiskScoreList(context);
        registerRiskScorePreview(context);
    }

    public static void registerEntityStoreInit(ExecutionContext context) {
        context.declareFunction("ENTITY_STORE_INIT",
            List.of(new Parameter("entity_types", "ARRAY", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ENTITY_STORE_INIT", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    Map<String, Object> body = new HashMap<>();
                    if (args.size() > 0 && args.get(0) != null) {
                        body.put("entityTypes", args.get(0));
                    }
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/entity_store/enable", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ENTITY_STORE_INIT failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerEntityStoreStatus(ExecutionContext context) {
        context.declareFunction("ENTITY_STORE_STATUS",
            List.of(),
            new BuiltInFunctionDefinition("ENTITY_STORE_STATUS", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/entity_store/status", null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ENTITY_STORE_STATUS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerEntityStoreDelete(ExecutionContext context) {
        context.declareFunction("ENTITY_STORE_DELETE",
            List.of(new Parameter("data_view_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ENTITY_STORE_DELETE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String dataViewId = args.size() > 0 && args.get(0) != null ? args.get(0).toString() : "";
                    String path = "/api/entity_store";
                    if (!dataViewId.isEmpty()) {
                        path += "?dataViewId=" + dataViewId;
                    }
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "DELETE", path, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ENTITY_STORE_DELETE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerEntityList(ExecutionContext context) {
        context.declareFunction("ENTITY_LIST",
            List.of(
                new Parameter("entity_type", "STRING", ParameterMode.IN),
                new Parameter("page", "INTEGER", ParameterMode.IN),
                new Parameter("per_page", "INTEGER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ENTITY_LIST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String entityType = args.get(0).toString();
                    int page = args.size() > 1 && args.get(1) != null ? ((Number) args.get(1)).intValue() : 1;
                    int perPage = args.size() > 2 && args.get(2) != null ? ((Number) args.get(2)).intValue() : 10;
                    
                    String path = "/api/entity_store/entities/list/" + entityType + "?page=" + page + "&per_page=" + perPage;
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", path, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ENTITY_LIST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerEntityGet(ExecutionContext context) {
        context.declareFunction("ENTITY_GET",
            List.of(
                new Parameter("entity_type", "STRING", ParameterMode.IN),
                new Parameter("entity_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ENTITY_GET", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String entityType = args.get(0).toString();
                    String entityId = args.get(1).toString();
                    
                    String path = "/api/entity_store/entities/" + entityType + "/" + entityId;
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", path, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ENTITY_GET failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAssetCriticalityList(ExecutionContext context) {
        context.declareFunction("ASSET_CRITICALITY_LIST",
            List.of(
                new Parameter("page", "INTEGER", ParameterMode.IN),
                new Parameter("per_page", "INTEGER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ASSET_CRITICALITY_LIST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    int page = args.size() > 0 && args.get(0) != null ? ((Number) args.get(0)).intValue() : 1;
                    int perPage = args.size() > 1 && args.get(1) != null ? ((Number) args.get(1)).intValue() : 10;
                    
                    String path = "/api/asset_criticality/list?page=" + page + "&per_page=" + perPage;
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", path, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ASSET_CRITICALITY_LIST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAssetCriticalitySet(ExecutionContext context) {
        context.declareFunction("ASSET_CRITICALITY_SET",
            List.of(
                new Parameter("id_field", "STRING", ParameterMode.IN),
                new Parameter("id_value", "STRING", ParameterMode.IN),
                new Parameter("criticality_level", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ASSET_CRITICALITY_SET", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String idField = args.get(0).toString();
                    String idValue = args.get(1).toString();
                    String criticalityLevel = args.get(2).toString();
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("id_field", idField);
                    body.put("id_value", idValue);
                    body.put("criticality_level", criticalityLevel);
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/asset_criticality", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ASSET_CRITICALITY_SET failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAssetCriticalityDelete(ExecutionContext context) {
        context.declareFunction("ASSET_CRITICALITY_DELETE",
            List.of(
                new Parameter("id_field", "STRING", ParameterMode.IN),
                new Parameter("id_value", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ASSET_CRITICALITY_DELETE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String idField = args.get(0).toString();
                    String idValue = args.get(1).toString();
                    
                    String path = "/api/asset_criticality?id_field=" + idField + "&id_value=" + idValue;
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "DELETE", path, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ASSET_CRITICALITY_DELETE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerRiskScoreList(ExecutionContext context) {
        context.declareFunction("RISK_SCORE_LIST",
            List.of(
                new Parameter("entity_type", "STRING", ParameterMode.IN),
                new Parameter("page", "INTEGER", ParameterMode.IN),
                new Parameter("per_page", "INTEGER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("RISK_SCORE_LIST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String entityType = args.get(0).toString();
                    int page = args.size() > 1 && args.get(1) != null ? ((Number) args.get(1)).intValue() : 1;
                    int perPage = args.size() > 2 && args.get(2) != null ? ((Number) args.get(2)).intValue() : 10;
                    
                    String path = "/api/risk_score/scores/" + entityType + "?page=" + page + "&per_page=" + perPage;
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", path, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("RISK_SCORE_LIST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerRiskScorePreview(ExecutionContext context) {
        context.declareFunction("RISK_SCORE_PREVIEW",
            List.of(new Parameter("config", "DOCUMENT", ParameterMode.IN)),
            new BuiltInFunctionDefinition("RISK_SCORE_PREVIEW", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    Map<String, Object> config = args.size() > 0 && args.get(0) != null ? toMap(args.get(0)) : new HashMap<>();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/risk_score/preview", config);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("RISK_SCORE_PREVIEW failed: " + e.getMessage(), e));
                }
            })
        );
    }
}
