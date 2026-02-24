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
 * Kibana Saved Objects API functions for elastic-script.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.RUNBOOK,
    description = "Kibana Saved Objects functions for managing dashboards, visualizations, etc."
)
public class SavedObjectFunctions {

    public static void registerAll(ExecutionContext context) {
        registerSavedObjectFind(context);
        registerSavedObjectGet(context);
        registerSavedObjectCreate(context);
        registerSavedObjectUpdate(context);
        registerSavedObjectDelete(context);
        registerSavedObjectBulkGet(context);
        registerSavedObjectExport(context);
        registerSavedObjectImport(context);
    }

    public static void registerSavedObjectFind(ExecutionContext context) {
        context.declareFunction("SAVED_OBJECT_FIND",
            List.of(
                new Parameter("type", "STRING", ParameterMode.IN),
                new Parameter("search", "STRING", ParameterMode.IN),
                new Parameter("per_page", "INTEGER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SAVED_OBJECT_FIND", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String type = args.get(0).toString();
                    String search = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : "";
                    int perPage = args.size() > 2 && args.get(2) != null ? ((Number) args.get(2)).intValue() : 20;
                    
                    StringBuilder path = new StringBuilder("/api/saved_objects/_find?type=" + type + "&per_page=" + perPage);
                    if (!search.isEmpty()) {
                        path.append("&search=").append(java.net.URLEncoder.encode(search, "UTF-8"));
                    }
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", path.toString(), null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SAVED_OBJECT_FIND failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerSavedObjectGet(ExecutionContext context) {
        context.declareFunction("SAVED_OBJECT_GET",
            List.of(
                new Parameter("type", "STRING", ParameterMode.IN),
                new Parameter("id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SAVED_OBJECT_GET", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String type = args.get(0).toString();
                    String id = args.get(1).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/saved_objects/" + type + "/" + id, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SAVED_OBJECT_GET failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerSavedObjectCreate(ExecutionContext context) {
        context.declareFunction("SAVED_OBJECT_CREATE",
            List.of(
                new Parameter("type", "STRING", ParameterMode.IN),
                new Parameter("id", "STRING", ParameterMode.IN),
                new Parameter("attributes", "DOCUMENT", ParameterMode.IN),
                new Parameter("references", "ARRAY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SAVED_OBJECT_CREATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String type = args.get(0).toString();
                    String id = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : null;
                    Map<String, Object> attributes = args.size() > 2 && args.get(2) != null ? toMap(args.get(2)) : new HashMap<>();
                    List<Object> references = args.size() > 3 && args.get(3) != null ? toList(args.get(3)) : new ArrayList<>();
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("attributes", attributes);
                    body.put("references", references);
                    
                    String path = id != null ? "/api/saved_objects/" + type + "/" + id : "/api/saved_objects/" + type;
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", path, body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SAVED_OBJECT_CREATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerSavedObjectUpdate(ExecutionContext context) {
        context.declareFunction("SAVED_OBJECT_UPDATE",
            List.of(
                new Parameter("type", "STRING", ParameterMode.IN),
                new Parameter("id", "STRING", ParameterMode.IN),
                new Parameter("attributes", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SAVED_OBJECT_UPDATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String type = args.get(0).toString();
                    String id = args.get(1).toString();
                    Map<String, Object> attributes = toMap(args.get(2));
                    
                    Map<String, Object> body = Map.of("attributes", attributes);
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "PUT", "/api/saved_objects/" + type + "/" + id, body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SAVED_OBJECT_UPDATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerSavedObjectDelete(ExecutionContext context) {
        context.declareFunction("SAVED_OBJECT_DELETE",
            List.of(
                new Parameter("type", "STRING", ParameterMode.IN),
                new Parameter("id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SAVED_OBJECT_DELETE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String type = args.get(0).toString();
                    String id = args.get(1).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "DELETE", "/api/saved_objects/" + type + "/" + id, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SAVED_OBJECT_DELETE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerSavedObjectBulkGet(ExecutionContext context) {
        context.declareFunction("SAVED_OBJECT_BULK_GET",
            List.of(new Parameter("objects", "ARRAY", ParameterMode.IN)),
            new BuiltInFunctionDefinition("SAVED_OBJECT_BULK_GET", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    List<Object> objects = toList(args.get(0));
                    Map<String, Object> body = new HashMap<>();
                    body.put("objects", objects);
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/saved_objects/_bulk_get", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SAVED_OBJECT_BULK_GET failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerSavedObjectExport(ExecutionContext context) {
        context.declareFunction("SAVED_OBJECT_EXPORT",
            List.of(
                new Parameter("types", "ARRAY", ParameterMode.IN),
                new Parameter("objects", "ARRAY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SAVED_OBJECT_EXPORT", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    List<Object> types = args.size() > 0 && args.get(0) != null ? toList(args.get(0)) : new ArrayList<>();
                    List<Object> objects = args.size() > 1 && args.get(1) != null ? toList(args.get(1)) : new ArrayList<>();
                    
                    Map<String, Object> body = new HashMap<>();
                    if (!types.isEmpty()) {
                        body.put("type", types);
                    }
                    if (!objects.isEmpty()) {
                        body.put("objects", objects);
                    }
                    body.put("includeReferencesDeep", true);
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/saved_objects/_export", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SAVED_OBJECT_EXPORT failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerSavedObjectImport(ExecutionContext context) {
        context.declareFunction("SAVED_OBJECT_IMPORT",
            List.of(
                new Parameter("objects", "ARRAY", ParameterMode.IN),
                new Parameter("overwrite", "BOOLEAN", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SAVED_OBJECT_IMPORT", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    List<Object> objects = toList(args.get(0));
                    boolean overwrite = args.size() > 1 && args.get(1) != null ? (Boolean) args.get(1) : false;
                    
                    String path = "/api/saved_objects/_import?overwrite=" + overwrite;
                    Map<String, Object> body = Map.of("objects", objects);
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", path, body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SAVED_OBJECT_IMPORT failed: " + e.getMessage(), e));
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
