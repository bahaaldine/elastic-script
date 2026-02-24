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
 * Kibana Spaces API functions for elastic-script.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.RUNBOOK,
    description = "Kibana Spaces functions for multi-tenancy management."
)
public class SpaceFunctions {

    public static void registerAll(ExecutionContext context) {
        registerSpaceList(context);
        registerSpaceGet(context);
        registerSpaceCreate(context);
        registerSpaceUpdate(context);
        registerSpaceDelete(context);
        registerSpaceCopyObjects(context);
    }

    public static void registerSpaceList(ExecutionContext context) {
        context.declareFunction("SPACE_LIST",
            List.of(),
            new BuiltInFunctionDefinition("SPACE_LIST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/spaces/space", null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SPACE_LIST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerSpaceGet(ExecutionContext context) {
        context.declareFunction("SPACE_GET",
            List.of(new Parameter("space_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("SPACE_GET", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String spaceId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/spaces/space/" + spaceId, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SPACE_GET failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerSpaceCreate(ExecutionContext context) {
        context.declareFunction("SPACE_CREATE",
            List.of(
                new Parameter("id", "STRING", ParameterMode.IN),
                new Parameter("name", "STRING", ParameterMode.IN),
                new Parameter("description", "STRING", ParameterMode.IN),
                new Parameter("color", "STRING", ParameterMode.IN),
                new Parameter("initials", "STRING", ParameterMode.IN),
                new Parameter("disabled_features", "ARRAY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SPACE_CREATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String id = args.get(0).toString();
                    String name = args.get(1).toString();
                    String description = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : "";
                    String color = args.size() > 3 && args.get(3) != null ? args.get(3).toString() : "#00bfb3";
                    String initials = args.size() > 4 && args.get(4) != null ? args.get(4).toString() : null;
                    List<Object> disabledFeatures = args.size() > 5 && args.get(5) != null ? toList(args.get(5)) : new ArrayList<>();
                    
                    Map<String, Object> spaceData = new HashMap<>();
                    spaceData.put("id", id);
                    spaceData.put("name", name);
                    spaceData.put("description", description);
                    spaceData.put("color", color);
                    if (initials != null) {
                        spaceData.put("initials", initials);
                    }
                    spaceData.put("disabledFeatures", disabledFeatures);
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/spaces/space", spaceData);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SPACE_CREATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerSpaceUpdate(ExecutionContext context) {
        context.declareFunction("SPACE_UPDATE",
            List.of(
                new Parameter("space_id", "STRING", ParameterMode.IN),
                new Parameter("updates", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SPACE_UPDATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String spaceId = args.get(0).toString();
                    Map<String, Object> updates = toMap(args.get(1));
                    
                    updates.put("id", spaceId);
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "PUT", "/api/spaces/space/" + spaceId, updates);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SPACE_UPDATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerSpaceDelete(ExecutionContext context) {
        context.declareFunction("SPACE_DELETE",
            List.of(new Parameter("space_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("SPACE_DELETE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String spaceId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "DELETE", "/api/spaces/space/" + spaceId, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SPACE_DELETE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerSpaceCopyObjects(ExecutionContext context) {
        context.declareFunction("SPACE_COPY_OBJECTS",
            List.of(
                new Parameter("source_space", "STRING", ParameterMode.IN),
                new Parameter("dest_spaces", "ARRAY", ParameterMode.IN),
                new Parameter("objects", "ARRAY", ParameterMode.IN),
                new Parameter("include_references", "BOOLEAN", ParameterMode.IN),
                new Parameter("overwrite", "BOOLEAN", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SPACE_COPY_OBJECTS", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String sourceSpace = args.get(0).toString();
                    List<Object> destSpaces = toList(args.get(1));
                    List<Object> objects = toList(args.get(2));
                    boolean includeReferences = args.size() > 3 && args.get(3) != null ? (Boolean) args.get(3) : true;
                    boolean overwrite = args.size() > 4 && args.get(4) != null ? (Boolean) args.get(4) : false;
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("spaces", destSpaces);
                    body.put("objects", objects);
                    body.put("includeReferences", includeReferences);
                    body.put("overwrite", overwrite);
                    
                    String path = "/api/spaces/_copy_saved_objects";
                    if (!sourceSpace.equals("default")) {
                        path = "/s/" + sourceSpace + path;
                    }
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", path, body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SPACE_COPY_OBJECTS failed: " + e.getMessage(), e));
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
