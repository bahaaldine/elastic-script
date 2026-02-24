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
 * Kibana Synthetics API functions for elastic-script.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.RUNBOOK,
    description = "Synthetics functions for uptime monitoring."
)
public class SyntheticsFunctions {

    public static void registerAll(ExecutionContext context) {
        registerMonitorList(context);
        registerMonitorGet(context);
        registerMonitorCreate(context);
        registerMonitorUpdate(context);
        registerMonitorDelete(context);
        registerMonitorEnable(context);
        registerMonitorDisable(context);
        registerPrivateLocationList(context);
        registerPrivateLocationCreate(context);
        registerPrivateLocationDelete(context);
        registerSyntheticsParams(context);
    }

    public static void registerMonitorList(ExecutionContext context) {
        context.declareFunction("SYNTHETICS_MONITOR_LIST",
            List.of(
                new Parameter("page", "INTEGER", ParameterMode.IN),
                new Parameter("per_page", "INTEGER", ParameterMode.IN),
                new Parameter("tags", "ARRAY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SYNTHETICS_MONITOR_LIST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    int page = args.size() > 0 && args.get(0) != null ? ((Number) args.get(0)).intValue() : 1;
                    int perPage = args.size() > 1 && args.get(1) != null ? ((Number) args.get(1)).intValue() : 25;
                    
                    String path = "/api/synthetics/monitors?page=" + page + "&perPage=" + perPage;
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", path, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SYNTHETICS_MONITOR_LIST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerMonitorGet(ExecutionContext context) {
        context.declareFunction("SYNTHETICS_MONITOR_GET",
            List.of(new Parameter("monitor_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("SYNTHETICS_MONITOR_GET", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String monitorId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/synthetics/monitors/" + monitorId, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SYNTHETICS_MONITOR_GET failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerMonitorCreate(ExecutionContext context) {
        context.declareFunction("SYNTHETICS_MONITOR_CREATE",
            List.of(new Parameter("config", "DOCUMENT", ParameterMode.IN)),
            new BuiltInFunctionDefinition("SYNTHETICS_MONITOR_CREATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    Map<String, Object> config = toMap(args.get(0));
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/synthetics/monitors", config);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SYNTHETICS_MONITOR_CREATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerMonitorUpdate(ExecutionContext context) {
        context.declareFunction("SYNTHETICS_MONITOR_UPDATE",
            List.of(
                new Parameter("monitor_id", "STRING", ParameterMode.IN),
                new Parameter("config", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SYNTHETICS_MONITOR_UPDATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String monitorId = args.get(0).toString();
                    Map<String, Object> config = toMap(args.get(1));
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "PUT", "/api/synthetics/monitors/" + monitorId, config);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SYNTHETICS_MONITOR_UPDATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerMonitorDelete(ExecutionContext context) {
        context.declareFunction("SYNTHETICS_MONITOR_DELETE",
            List.of(new Parameter("monitor_ids", "ARRAY", ParameterMode.IN)),
            new BuiltInFunctionDefinition("SYNTHETICS_MONITOR_DELETE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    List<Object> monitorIds = toList(args.get(0));
                    Map<String, Object> body = Map.of("ids", monitorIds);
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "DELETE", "/api/synthetics/monitors", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SYNTHETICS_MONITOR_DELETE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerMonitorEnable(ExecutionContext context) {
        context.declareFunction("SYNTHETICS_MONITOR_ENABLE",
            List.of(new Parameter("monitor_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("SYNTHETICS_MONITOR_ENABLE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String monitorId = args.get(0).toString();
                    Map<String, Object> body = Map.of("enabled", true);
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "PUT", "/api/synthetics/monitors/" + monitorId, body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SYNTHETICS_MONITOR_ENABLE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerMonitorDisable(ExecutionContext context) {
        context.declareFunction("SYNTHETICS_MONITOR_DISABLE",
            List.of(new Parameter("monitor_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("SYNTHETICS_MONITOR_DISABLE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String monitorId = args.get(0).toString();
                    Map<String, Object> body = Map.of("enabled", false);
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "PUT", "/api/synthetics/monitors/" + monitorId, body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SYNTHETICS_MONITOR_DISABLE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerPrivateLocationList(ExecutionContext context) {
        context.declareFunction("SYNTHETICS_PRIVATE_LOCATION_LIST",
            List.of(),
            new BuiltInFunctionDefinition("SYNTHETICS_PRIVATE_LOCATION_LIST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/synthetics/private_locations", null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SYNTHETICS_PRIVATE_LOCATION_LIST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerPrivateLocationCreate(ExecutionContext context) {
        context.declareFunction("SYNTHETICS_PRIVATE_LOCATION_CREATE",
            List.of(
                new Parameter("label", "STRING", ParameterMode.IN),
                new Parameter("agent_policy_id", "STRING", ParameterMode.IN),
                new Parameter("tags", "ARRAY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SYNTHETICS_PRIVATE_LOCATION_CREATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String label = args.get(0).toString();
                    String agentPolicyId = args.get(1).toString();
                    List<Object> tags = args.size() > 2 && args.get(2) != null ? toList(args.get(2)) : new ArrayList<>();
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("label", label);
                    body.put("agentPolicyId", agentPolicyId);
                    body.put("tags", tags);
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/synthetics/private_locations", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SYNTHETICS_PRIVATE_LOCATION_CREATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerPrivateLocationDelete(ExecutionContext context) {
        context.declareFunction("SYNTHETICS_PRIVATE_LOCATION_DELETE",
            List.of(new Parameter("location_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("SYNTHETICS_PRIVATE_LOCATION_DELETE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String locationId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "DELETE", "/api/synthetics/private_locations/" + locationId, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SYNTHETICS_PRIVATE_LOCATION_DELETE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerSyntheticsParams(ExecutionContext context) {
        context.declareFunction("SYNTHETICS_PARAMS",
            List.of(
                new Parameter("action", "STRING", ParameterMode.IN),
                new Parameter("params", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SYNTHETICS_PARAMS", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String action = args.get(0).toString().toLowerCase();
                    Map<String, Object> params = args.size() > 1 && args.get(1) != null ? toMap(args.get(1)) : new HashMap<>();
                    
                    Map<String, Object> result;
                    switch (action) {
                        case "get":
                            result = kibanaRequest(getKibanaUrl(), "GET", "/api/synthetics/params", null);
                            break;
                        case "add":
                            result = kibanaRequest(getKibanaUrl(), "POST", "/api/synthetics/params", params);
                            break;
                        case "delete":
                            String id = params.containsKey("id") ? params.get("id").toString() : "";
                            result = kibanaRequest(getKibanaUrl(), "DELETE", "/api/synthetics/params/" + id, null);
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown action: " + action + ". Use 'get', 'add', or 'delete'.");
                    }
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SYNTHETICS_PARAMS failed: " + e.getMessage(), e));
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
