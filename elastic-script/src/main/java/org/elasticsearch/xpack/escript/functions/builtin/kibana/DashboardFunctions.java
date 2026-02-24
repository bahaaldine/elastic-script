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
 * Kibana Dashboard API functions for elastic-script.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.RUNBOOK,
    description = "Dashboard management functions for creating and managing dashboards."
)
public class DashboardFunctions {

    public static void registerAll(ExecutionContext context) {
        registerDashboardList(context);
        registerDashboardGet(context);
        registerDashboardCreate(context);
        registerDashboardUpdate(context);
        registerDashboardDelete(context);
        registerDashboardDuplicate(context);
        registerVisualizationList(context);
        registerVisualizationCreate(context);
        registerLensList(context);
        registerLensCreate(context);
    }

    public static void registerDashboardList(ExecutionContext context) {
        context.declareFunction("DASHBOARD_LIST",
            List.of(
                new Parameter("search", "STRING", ParameterMode.IN),
                new Parameter("per_page", "INTEGER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("DASHBOARD_LIST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String search = args.size() > 0 && args.get(0) != null ? args.get(0).toString() : "";
                    int perPage = args.size() > 1 && args.get(1) != null ? ((Number) args.get(1)).intValue() : 20;
                    
                    StringBuilder path = new StringBuilder("/api/saved_objects/_find?type=dashboard&per_page=" + perPage);
                    if (!search.isEmpty()) {
                        path.append("&search=").append(java.net.URLEncoder.encode(search, "UTF-8"));
                    }
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", path.toString(), null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("DASHBOARD_LIST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerDashboardGet(ExecutionContext context) {
        context.declareFunction("DASHBOARD_GET",
            List.of(new Parameter("dashboard_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("DASHBOARD_GET", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String dashboardId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/saved_objects/dashboard/" + dashboardId, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("DASHBOARD_GET failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerDashboardCreate(ExecutionContext context) {
        context.declareFunction("DASHBOARD_CREATE",
            List.of(
                new Parameter("title", "STRING", ParameterMode.IN),
                new Parameter("panels", "ARRAY", ParameterMode.IN),
                new Parameter("options", "DOCUMENT", ParameterMode.IN),
                new Parameter("id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("DASHBOARD_CREATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String title = args.get(0).toString();
                    List<Object> panels = args.size() > 1 && args.get(1) != null ? toList(args.get(1)) : new ArrayList<>();
                    Map<String, Object> options = args.size() > 2 && args.get(2) != null ? toMap(args.get(2)) : new HashMap<>();
                    String id = args.size() > 3 && args.get(3) != null ? args.get(3).toString() : null;
                    
                    Map<String, Object> attributes = new HashMap<>();
                    attributes.put("title", title);
                    attributes.put("panelsJSON", mapToJson(Map.of("panels", panels)));
                    attributes.put("optionsJSON", mapToJson(options.isEmpty() ? Map.of("useMargins", true, "hidePanelTitles", false) : options));
                    attributes.put("version", 1);
                    attributes.put("timeRestore", false);
                    attributes.put("kibanaSavedObjectMeta", Map.of("searchSourceJSON", "{}"));
                    
                    Map<String, Object> body = Map.of("attributes", attributes);
                    
                    String path = id != null ? "/api/saved_objects/dashboard/" + id : "/api/saved_objects/dashboard";
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", path, body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("DASHBOARD_CREATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerDashboardUpdate(ExecutionContext context) {
        context.declareFunction("DASHBOARD_UPDATE",
            List.of(
                new Parameter("dashboard_id", "STRING", ParameterMode.IN),
                new Parameter("attributes", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("DASHBOARD_UPDATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String dashboardId = args.get(0).toString();
                    Map<String, Object> attributes = toMap(args.get(1));
                    
                    Map<String, Object> body = Map.of("attributes", attributes);
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "PUT", "/api/saved_objects/dashboard/" + dashboardId, body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("DASHBOARD_UPDATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerDashboardDelete(ExecutionContext context) {
        context.declareFunction("DASHBOARD_DELETE",
            List.of(new Parameter("dashboard_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("DASHBOARD_DELETE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String dashboardId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "DELETE", "/api/saved_objects/dashboard/" + dashboardId, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("DASHBOARD_DELETE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerDashboardDuplicate(ExecutionContext context) {
        context.declareFunction("DASHBOARD_DUPLICATE",
            List.of(
                new Parameter("dashboard_id", "STRING", ParameterMode.IN),
                new Parameter("new_title", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("DASHBOARD_DUPLICATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String dashboardId = args.get(0).toString();
                    String newTitle = args.get(1).toString();
                    
                    // Get existing dashboard
                    Map<String, Object> existing = kibanaRequest(getKibanaUrl(), "GET", "/api/saved_objects/dashboard/" + dashboardId, null);
                    
                    if (existing.containsKey("data") && existing.get("data") instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = (Map<String, Object>) existing.get("data");
                        if (data.containsKey("attributes") && data.get("attributes") instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> attributes = new HashMap<>((Map<String, Object>) data.get("attributes"));
                            attributes.put("title", newTitle);
                            
                            Map<String, Object> body = Map.of("attributes", attributes);
                            Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/saved_objects/dashboard", body);
                            listener.onResponse(result);
                            return;
                        }
                    }
                    
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("error", "Could not find dashboard to duplicate");
                    listener.onResponse(error);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("DASHBOARD_DUPLICATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerVisualizationList(ExecutionContext context) {
        context.declareFunction("VISUALIZATION_LIST",
            List.of(
                new Parameter("search", "STRING", ParameterMode.IN),
                new Parameter("per_page", "INTEGER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("VISUALIZATION_LIST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String search = args.size() > 0 && args.get(0) != null ? args.get(0).toString() : "";
                    int perPage = args.size() > 1 && args.get(1) != null ? ((Number) args.get(1)).intValue() : 20;
                    
                    StringBuilder path = new StringBuilder("/api/saved_objects/_find?type=visualization&per_page=" + perPage);
                    if (!search.isEmpty()) {
                        path.append("&search=").append(java.net.URLEncoder.encode(search, "UTF-8"));
                    }
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", path.toString(), null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("VISUALIZATION_LIST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerVisualizationCreate(ExecutionContext context) {
        context.declareFunction("VISUALIZATION_CREATE",
            List.of(
                new Parameter("title", "STRING", ParameterMode.IN),
                new Parameter("vis_type", "STRING", ParameterMode.IN),
                new Parameter("vis_state", "DOCUMENT", ParameterMode.IN),
                new Parameter("references", "ARRAY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("VISUALIZATION_CREATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String title = args.get(0).toString();
                    String visType = args.get(1).toString();
                    Map<String, Object> visState = args.size() > 2 && args.get(2) != null ? toMap(args.get(2)) : new HashMap<>();
                    List<Object> references = args.size() > 3 && args.get(3) != null ? toList(args.get(3)) : new ArrayList<>();
                    
                    visState.put("type", visType);
                    visState.put("title", title);
                    
                    Map<String, Object> attributes = new HashMap<>();
                    attributes.put("title", title);
                    attributes.put("visState", mapToJson(visState));
                    attributes.put("kibanaSavedObjectMeta", Map.of("searchSourceJSON", "{}"));
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("attributes", attributes);
                    body.put("references", references);
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/saved_objects/visualization", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("VISUALIZATION_CREATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerLensList(ExecutionContext context) {
        context.declareFunction("LENS_LIST",
            List.of(
                new Parameter("search", "STRING", ParameterMode.IN),
                new Parameter("per_page", "INTEGER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("LENS_LIST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String search = args.size() > 0 && args.get(0) != null ? args.get(0).toString() : "";
                    int perPage = args.size() > 1 && args.get(1) != null ? ((Number) args.get(1)).intValue() : 20;
                    
                    StringBuilder path = new StringBuilder("/api/saved_objects/_find?type=lens&per_page=" + perPage);
                    if (!search.isEmpty()) {
                        path.append("&search=").append(java.net.URLEncoder.encode(search, "UTF-8"));
                    }
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", path.toString(), null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("LENS_LIST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerLensCreate(ExecutionContext context) {
        context.declareFunction("LENS_CREATE",
            List.of(
                new Parameter("title", "STRING", ParameterMode.IN),
                new Parameter("state", "DOCUMENT", ParameterMode.IN),
                new Parameter("references", "ARRAY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("LENS_CREATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String title = args.get(0).toString();
                    Map<String, Object> state = args.size() > 1 && args.get(1) != null ? toMap(args.get(1)) : new HashMap<>();
                    List<Object> references = args.size() > 2 && args.get(2) != null ? toList(args.get(2)) : new ArrayList<>();
                    
                    Map<String, Object> attributes = new HashMap<>();
                    attributes.put("title", title);
                    attributes.put("state", state);
                    attributes.put("visualizationType", state.getOrDefault("visualization", Map.of()).toString());
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("attributes", attributes);
                    body.put("references", references);
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/saved_objects/lens", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("LENS_CREATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @SuppressWarnings("unchecked")
    private static List<Object> toList(Object obj) {
        if (obj instanceof List) {
            return (List<Object>) obj;
        }
        return new ArrayList<>();
    }
}
