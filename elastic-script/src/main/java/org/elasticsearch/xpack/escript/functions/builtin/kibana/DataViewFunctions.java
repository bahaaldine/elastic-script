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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.xpack.escript.functions.builtin.kibana.KibanaFunctions.*;

/**
 * Kibana Data Views API functions for elastic-script.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.RUNBOOK,
    description = "Kibana Data Views (index patterns) functions."
)
public class DataViewFunctions {

    public static void registerAll(ExecutionContext context) {
        registerDataViewList(context);
        registerDataViewGet(context);
        registerDataViewCreate(context);
        registerDataViewUpdate(context);
        registerDataViewDelete(context);
        registerDataViewRefresh(context);
        registerDataViewFields(context);
        registerDataViewSetDefault(context);
        registerDataViewRuntimeFieldCreate(context);
        registerDataViewRuntimeFieldDelete(context);
    }

    public static void registerDataViewList(ExecutionContext context) {
        context.declareFunction("DATA_VIEW_LIST",
            List.of(),
            new BuiltInFunctionDefinition("DATA_VIEW_LIST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/data_views", null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("DATA_VIEW_LIST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerDataViewGet(ExecutionContext context) {
        context.declareFunction("DATA_VIEW_GET",
            List.of(new Parameter("view_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("DATA_VIEW_GET", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String viewId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/data_views/data_view/" + viewId, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("DATA_VIEW_GET failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerDataViewCreate(ExecutionContext context) {
        context.declareFunction("DATA_VIEW_CREATE",
            List.of(
                new Parameter("title", "STRING", ParameterMode.IN),
                new Parameter("name", "STRING", ParameterMode.IN),
                new Parameter("time_field", "STRING", ParameterMode.IN),
                new Parameter("id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("DATA_VIEW_CREATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String title = args.get(0).toString();
                    String name = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : title;
                    String timeField = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : null;
                    String id = args.size() > 3 && args.get(3) != null ? args.get(3).toString() : null;
                    
                    Map<String, Object> dataView = new HashMap<>();
                    dataView.put("title", title);
                    dataView.put("name", name);
                    if (timeField != null) {
                        dataView.put("timeFieldName", timeField);
                    }
                    if (id != null) {
                        dataView.put("id", id);
                    }
                    
                    Map<String, Object> body = Map.of("data_view", dataView);
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/data_views/data_view", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("DATA_VIEW_CREATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerDataViewUpdate(ExecutionContext context) {
        context.declareFunction("DATA_VIEW_UPDATE",
            List.of(
                new Parameter("view_id", "STRING", ParameterMode.IN),
                new Parameter("updates", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("DATA_VIEW_UPDATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String viewId = args.get(0).toString();
                    Map<String, Object> updates = toMap(args.get(1));
                    
                    Map<String, Object> body = Map.of("data_view", updates);
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/data_views/data_view/" + viewId, body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("DATA_VIEW_UPDATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerDataViewDelete(ExecutionContext context) {
        context.declareFunction("DATA_VIEW_DELETE",
            List.of(new Parameter("view_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("DATA_VIEW_DELETE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String viewId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "DELETE", "/api/data_views/data_view/" + viewId, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("DATA_VIEW_DELETE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerDataViewRefresh(ExecutionContext context) {
        context.declareFunction("DATA_VIEW_REFRESH",
            List.of(new Parameter("view_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("DATA_VIEW_REFRESH", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String viewId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/data_views/data_view/" + viewId + "/refresh", null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("DATA_VIEW_REFRESH failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerDataViewFields(ExecutionContext context) {
        context.declareFunction("DATA_VIEW_FIELDS",
            List.of(new Parameter("pattern", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("DATA_VIEW_FIELDS", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String pattern = args.get(0).toString();
                    String encodedPattern = java.net.URLEncoder.encode(pattern, "UTF-8");
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", 
                        "/api/index_patterns/_fields_for_wildcard?pattern=" + encodedPattern, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("DATA_VIEW_FIELDS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerDataViewSetDefault(ExecutionContext context) {
        context.declareFunction("DATA_VIEW_SET_DEFAULT",
            List.of(new Parameter("view_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("DATA_VIEW_SET_DEFAULT", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String viewId = args.get(0).toString();
                    Map<String, Object> body = Map.of("data_view_id", viewId);
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/data_views/default", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("DATA_VIEW_SET_DEFAULT failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerDataViewRuntimeFieldCreate(ExecutionContext context) {
        context.declareFunction("DATA_VIEW_RUNTIME_FIELD_CREATE",
            List.of(
                new Parameter("view_id", "STRING", ParameterMode.IN),
                new Parameter("name", "STRING", ParameterMode.IN),
                new Parameter("type", "STRING", ParameterMode.IN),
                new Parameter("script", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("DATA_VIEW_RUNTIME_FIELD_CREATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String viewId = args.get(0).toString();
                    String name = args.get(1).toString();
                    String type = args.get(2).toString();
                    String script = args.size() > 3 && args.get(3) != null ? args.get(3).toString() : null;
                    
                    Map<String, Object> runtimeField = new HashMap<>();
                    runtimeField.put("name", name);
                    runtimeField.put("runtimeField", Map.of(
                        "type", type,
                        "script", script != null ? Map.of("source", script) : Map.of()
                    ));
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", 
                        "/api/data_views/data_view/" + viewId + "/runtime_field", runtimeField);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("DATA_VIEW_RUNTIME_FIELD_CREATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerDataViewRuntimeFieldDelete(ExecutionContext context) {
        context.declareFunction("DATA_VIEW_RUNTIME_FIELD_DELETE",
            List.of(
                new Parameter("view_id", "STRING", ParameterMode.IN),
                new Parameter("field_name", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("DATA_VIEW_RUNTIME_FIELD_DELETE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String viewId = args.get(0).toString();
                    String fieldName = args.get(1).toString();
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "DELETE", 
                        "/api/data_views/data_view/" + viewId + "/runtime_field/" + fieldName, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("DATA_VIEW_RUNTIME_FIELD_DELETE failed: " + e.getMessage(), e));
                }
            })
        );
    }
}
