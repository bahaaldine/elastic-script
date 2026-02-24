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
 * Kibana Cases API functions for elastic-script.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.RUNBOOK,
    description = "Kibana Cases functions for incident management."
)
public class CaseFunctions {

    public static void registerAll(ExecutionContext context) {
        registerCaseList(context);
        registerCaseGet(context);
        registerCaseCreate(context);
        registerCaseUpdate(context);
        registerCaseDelete(context);
        registerCaseCommentAdd(context);
        registerCaseCommentList(context);
        registerCaseAlerts(context);
        registerCasePush(context);
        registerCaseTags(context);
    }

    public static void registerCaseList(ExecutionContext context) {
        context.declareFunction("CASE_LIST",
            List.of(
                new Parameter("status", "STRING", ParameterMode.IN),
                new Parameter("tags", "ARRAY", ParameterMode.IN),
                new Parameter("per_page", "INTEGER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("CASE_LIST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String status = args.size() > 0 && args.get(0) != null ? args.get(0).toString() : "";
                    int perPage = args.size() > 2 && args.get(2) != null ? ((Number) args.get(2)).intValue() : 20;
                    
                    StringBuilder path = new StringBuilder("/api/cases/_find?perPage=" + perPage);
                    if (!status.isEmpty()) {
                        path.append("&status=").append(status);
                    }
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", path.toString(), null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("CASE_LIST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerCaseGet(ExecutionContext context) {
        context.declareFunction("CASE_GET",
            List.of(new Parameter("case_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("CASE_GET", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String caseId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/cases/" + caseId, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("CASE_GET failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerCaseCreate(ExecutionContext context) {
        context.declareFunction("CASE_CREATE",
            List.of(
                new Parameter("title", "STRING", ParameterMode.IN),
                new Parameter("description", "STRING", ParameterMode.IN),
                new Parameter("tags", "ARRAY", ParameterMode.IN),
                new Parameter("severity", "STRING", ParameterMode.IN),
                new Parameter("connector", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("CASE_CREATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String title = args.get(0).toString();
                    String description = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : "";
                    List<String> tags = args.size() > 2 && args.get(2) != null ? toStringList(args.get(2)) : new ArrayList<>();
                    String severity = args.size() > 3 && args.get(3) != null ? args.get(3).toString() : "low";
                    Map<String, Object> connector = args.size() > 4 && args.get(4) != null ? toMap(args.get(4)) : getDefaultConnector();
                    
                    Map<String, Object> caseData = new HashMap<>();
                    caseData.put("title", title);
                    caseData.put("description", description);
                    caseData.put("tags", tags);
                    caseData.put("severity", severity);
                    caseData.put("connector", connector);
                    caseData.put("settings", Map.of("syncAlerts", true));
                    caseData.put("owner", "cases");
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/cases", caseData);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("CASE_CREATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerCaseUpdate(ExecutionContext context) {
        context.declareFunction("CASE_UPDATE",
            List.of(
                new Parameter("case_id", "STRING", ParameterMode.IN),
                new Parameter("version", "STRING", ParameterMode.IN),
                new Parameter("updates", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("CASE_UPDATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String caseId = args.get(0).toString();
                    String version = args.get(1).toString();
                    Map<String, Object> updates = toMap(args.get(2));
                    
                    Map<String, Object> caseUpdate = new HashMap<>(updates);
                    caseUpdate.put("id", caseId);
                    caseUpdate.put("version", version);
                    
                    Map<String, Object> body = Map.of("cases", List.of(caseUpdate));
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "PATCH", "/api/cases", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("CASE_UPDATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerCaseDelete(ExecutionContext context) {
        context.declareFunction("CASE_DELETE",
            List.of(new Parameter("case_ids", "ARRAY", ParameterMode.IN)),
            new BuiltInFunctionDefinition("CASE_DELETE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    List<String> caseIds = toStringList(args.get(0));
                    String idsParam = String.join("&ids=", caseIds);
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "DELETE", "/api/cases?ids=" + idsParam, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("CASE_DELETE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerCaseCommentAdd(ExecutionContext context) {
        context.declareFunction("CASE_COMMENT_ADD",
            List.of(
                new Parameter("case_id", "STRING", ParameterMode.IN),
                new Parameter("comment", "STRING", ParameterMode.IN),
                new Parameter("owner", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("CASE_COMMENT_ADD", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String caseId = args.get(0).toString();
                    String comment = args.get(1).toString();
                    String owner = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : "cases";
                    
                    Map<String, Object> commentData = new HashMap<>();
                    commentData.put("comment", comment);
                    commentData.put("type", "user");
                    commentData.put("owner", owner);
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/cases/" + caseId + "/comments", commentData);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("CASE_COMMENT_ADD failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerCaseCommentList(ExecutionContext context) {
        context.declareFunction("CASE_COMMENT_LIST",
            List.of(new Parameter("case_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("CASE_COMMENT_LIST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String caseId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/cases/" + caseId + "/comments/_find", null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("CASE_COMMENT_LIST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerCaseAlerts(ExecutionContext context) {
        context.declareFunction("CASE_ALERTS",
            List.of(new Parameter("case_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("CASE_ALERTS", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String caseId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/cases/" + caseId + "/alerts", null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("CASE_ALERTS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerCasePush(ExecutionContext context) {
        context.declareFunction("CASE_PUSH",
            List.of(
                new Parameter("case_id", "STRING", ParameterMode.IN),
                new Parameter("connector_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("CASE_PUSH", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String caseId = args.get(0).toString();
                    String connectorId = args.get(1).toString();
                    String path = "/api/cases/" + caseId + "/connector/" + connectorId + "/_push";
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", path, new HashMap<>());
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("CASE_PUSH failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerCaseTags(ExecutionContext context) {
        context.declareFunction("CASE_TAGS",
            List.of(),
            new BuiltInFunctionDefinition("CASE_TAGS", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/cases/tags", null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("CASE_TAGS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @SuppressWarnings("unchecked")
    private static List<String> toStringList(Object obj) {
        if (obj instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) obj) {
                result.add(item.toString());
            }
            return result;
        }
        return new ArrayList<>();
    }

    private static Map<String, Object> getDefaultConnector() {
        Map<String, Object> connector = new HashMap<>();
        connector.put("id", "none");
        connector.put("name", "none");
        connector.put("type", ".none");
        connector.put("fields", null);
        return connector;
    }
}
