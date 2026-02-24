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
 * Kibana Security Detection Engine API functions for elastic-script.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.RUNBOOK,
    description = "Security detection rules, signals, and timeline functions."
)
public class DetectionFunctions {

    public static void registerAll(ExecutionContext context) {
        // Detection Rules
        registerDetectionRuleList(context);
        registerDetectionRuleGet(context);
        registerDetectionRuleCreate(context);
        registerDetectionRuleUpdate(context);
        registerDetectionRuleDelete(context);
        registerDetectionRuleEnable(context);
        registerDetectionRuleDisable(context);
        registerDetectionRuleBulkAction(context);
        
        // Signals/Alerts
        registerSignalQuery(context);
        registerSignalStatusUpdate(context);
        
        // Exceptions
        registerExceptionListCreate(context);
        registerExceptionListGet(context);
        registerExceptionItemAdd(context);
        
        // Timelines
        registerTimelineList(context);
        registerTimelineGet(context);
        registerTimelineCreate(context);
        
        // Prebuilt Rules
        registerPrebuiltRulesInstall(context);
        registerPrebuiltRulesStatus(context);
    }

    // ========================================================================
    // Detection Rules
    // ========================================================================

    public static void registerDetectionRuleList(ExecutionContext context) {
        context.declareFunction("DETECTION_RULE_LIST",
            List.of(
                new Parameter("per_page", "INTEGER", ParameterMode.IN),
                new Parameter("page", "INTEGER", ParameterMode.IN),
                new Parameter("filter", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("DETECTION_RULE_LIST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    int perPage = args.size() > 0 && args.get(0) != null ? ((Number) args.get(0)).intValue() : 20;
                    int page = args.size() > 1 && args.get(1) != null ? ((Number) args.get(1)).intValue() : 1;
                    String filter = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : "";
                    
                    StringBuilder path = new StringBuilder("/api/detection_engine/rules/_find?per_page=" + perPage + "&page=" + page);
                    if (!filter.isEmpty()) {
                        path.append("&filter=").append(java.net.URLEncoder.encode(filter, "UTF-8"));
                    }
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", path.toString(), null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("DETECTION_RULE_LIST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerDetectionRuleGet(ExecutionContext context) {
        context.declareFunction("DETECTION_RULE_GET",
            List.of(new Parameter("rule_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("DETECTION_RULE_GET", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String ruleId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/detection_engine/rules?rule_id=" + ruleId, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("DETECTION_RULE_GET failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerDetectionRuleCreate(ExecutionContext context) {
        context.declareFunction("DETECTION_RULE_CREATE",
            List.of(new Parameter("rule_config", "DOCUMENT", ParameterMode.IN)),
            new BuiltInFunctionDefinition("DETECTION_RULE_CREATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    Map<String, Object> ruleConfig = toMap(args.get(0));
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/detection_engine/rules", ruleConfig);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("DETECTION_RULE_CREATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerDetectionRuleUpdate(ExecutionContext context) {
        context.declareFunction("DETECTION_RULE_UPDATE",
            List.of(
                new Parameter("rule_id", "STRING", ParameterMode.IN),
                new Parameter("updates", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("DETECTION_RULE_UPDATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String ruleId = args.get(0).toString();
                    Map<String, Object> updates = new HashMap<>(toMap(args.get(1)));
                    updates.put("rule_id", ruleId);
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "PUT", "/api/detection_engine/rules", updates);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("DETECTION_RULE_UPDATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerDetectionRuleDelete(ExecutionContext context) {
        context.declareFunction("DETECTION_RULE_DELETE",
            List.of(new Parameter("rule_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("DETECTION_RULE_DELETE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String ruleId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "DELETE", "/api/detection_engine/rules?rule_id=" + ruleId, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("DETECTION_RULE_DELETE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerDetectionRuleEnable(ExecutionContext context) {
        context.declareFunction("DETECTION_RULE_ENABLE",
            List.of(new Parameter("rule_ids", "ARRAY", ParameterMode.IN)),
            new BuiltInFunctionDefinition("DETECTION_RULE_ENABLE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    List<Object> ruleIds = toList(args.get(0));
                    Map<String, Object> body = new HashMap<>();
                    body.put("action", "enable");
                    body.put("ids", ruleIds);
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/detection_engine/rules/_bulk_action", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("DETECTION_RULE_ENABLE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerDetectionRuleDisable(ExecutionContext context) {
        context.declareFunction("DETECTION_RULE_DISABLE",
            List.of(new Parameter("rule_ids", "ARRAY", ParameterMode.IN)),
            new BuiltInFunctionDefinition("DETECTION_RULE_DISABLE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    List<Object> ruleIds = toList(args.get(0));
                    Map<String, Object> body = new HashMap<>();
                    body.put("action", "disable");
                    body.put("ids", ruleIds);
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/detection_engine/rules/_bulk_action", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("DETECTION_RULE_DISABLE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerDetectionRuleBulkAction(ExecutionContext context) {
        context.declareFunction("DETECTION_RULE_BULK_ACTION",
            List.of(
                new Parameter("action", "STRING", ParameterMode.IN),
                new Parameter("rule_ids", "ARRAY", ParameterMode.IN),
                new Parameter("query", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("DETECTION_RULE_BULK_ACTION", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String action = args.get(0).toString();
                    List<Object> ruleIds = args.size() > 1 && args.get(1) != null ? toList(args.get(1)) : null;
                    String query = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : null;
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("action", action);
                    if (ruleIds != null && !ruleIds.isEmpty()) {
                        body.put("ids", ruleIds);
                    }
                    if (query != null && !query.isEmpty()) {
                        body.put("query", query);
                    }
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/detection_engine/rules/_bulk_action", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("DETECTION_RULE_BULK_ACTION failed: " + e.getMessage(), e));
                }
            })
        );
    }

    // ========================================================================
    // Signals/Alerts
    // ========================================================================

    public static void registerSignalQuery(ExecutionContext context) {
        context.declareFunction("SIGNAL_QUERY",
            List.of(new Parameter("query", "DOCUMENT", ParameterMode.IN)),
            new BuiltInFunctionDefinition("SIGNAL_QUERY", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    Map<String, Object> query = toMap(args.get(0));
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/detection_engine/signals/search", query);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SIGNAL_QUERY failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerSignalStatusUpdate(ExecutionContext context) {
        context.declareFunction("SIGNAL_STATUS_UPDATE",
            List.of(
                new Parameter("signal_ids", "ARRAY", ParameterMode.IN),
                new Parameter("status", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SIGNAL_STATUS_UPDATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    List<Object> signalIds = toList(args.get(0));
                    String status = args.get(1).toString();
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("signal_ids", signalIds);
                    body.put("status", status);
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/detection_engine/signals/status", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("SIGNAL_STATUS_UPDATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    // ========================================================================
    // Exception Lists
    // ========================================================================

    public static void registerExceptionListCreate(ExecutionContext context) {
        context.declareFunction("EXCEPTION_LIST_CREATE",
            List.of(
                new Parameter("list_id", "STRING", ParameterMode.IN),
                new Parameter("name", "STRING", ParameterMode.IN),
                new Parameter("description", "STRING", ParameterMode.IN),
                new Parameter("type", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("EXCEPTION_LIST_CREATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String listId = args.get(0).toString();
                    String name = args.get(1).toString();
                    String description = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : "";
                    String type = args.size() > 3 && args.get(3) != null ? args.get(3).toString() : "detection";
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("list_id", listId);
                    body.put("name", name);
                    body.put("description", description);
                    body.put("type", type);
                    body.put("namespace_type", "single");
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/exception_lists", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("EXCEPTION_LIST_CREATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerExceptionListGet(ExecutionContext context) {
        context.declareFunction("EXCEPTION_LIST_GET",
            List.of(new Parameter("list_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("EXCEPTION_LIST_GET", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String listId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/exception_lists?list_id=" + listId, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("EXCEPTION_LIST_GET failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerExceptionItemAdd(ExecutionContext context) {
        context.declareFunction("EXCEPTION_ITEM_ADD",
            List.of(
                new Parameter("list_id", "STRING", ParameterMode.IN),
                new Parameter("name", "STRING", ParameterMode.IN),
                new Parameter("entries", "ARRAY", ParameterMode.IN),
                new Parameter("description", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("EXCEPTION_ITEM_ADD", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String listId = args.get(0).toString();
                    String name = args.get(1).toString();
                    List<Object> entries = toList(args.get(2));
                    String description = args.size() > 3 && args.get(3) != null ? args.get(3).toString() : "";
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("list_id", listId);
                    body.put("name", name);
                    body.put("entries", entries);
                    body.put("description", description);
                    body.put("type", "simple");
                    body.put("namespace_type", "single");
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/exception_lists/items", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("EXCEPTION_ITEM_ADD failed: " + e.getMessage(), e));
                }
            })
        );
    }

    // ========================================================================
    // Timelines
    // ========================================================================

    public static void registerTimelineList(ExecutionContext context) {
        context.declareFunction("TIMELINE_LIST",
            List.of(new Parameter("page_size", "INTEGER", ParameterMode.IN)),
            new BuiltInFunctionDefinition("TIMELINE_LIST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    int pageSize = args.size() > 0 && args.get(0) != null ? ((Number) args.get(0)).intValue() : 20;
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("page_size", pageSize);
                    body.put("page_index", 1);
                    body.put("sort_field", "updated");
                    body.put("sort_order", "desc");
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/timelines?page_size=" + pageSize, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("TIMELINE_LIST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerTimelineGet(ExecutionContext context) {
        context.declareFunction("TIMELINE_GET",
            List.of(new Parameter("timeline_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("TIMELINE_GET", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String timelineId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/timeline?id=" + timelineId, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("TIMELINE_GET failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerTimelineCreate(ExecutionContext context) {
        context.declareFunction("TIMELINE_CREATE",
            List.of(new Parameter("timeline", "DOCUMENT", ParameterMode.IN)),
            new BuiltInFunctionDefinition("TIMELINE_CREATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    Map<String, Object> timeline = toMap(args.get(0));
                    Map<String, Object> body = Map.of("timeline", timeline);
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/timeline", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("TIMELINE_CREATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    // ========================================================================
    // Prebuilt Rules
    // ========================================================================

    public static void registerPrebuiltRulesInstall(ExecutionContext context) {
        context.declareFunction("PREBUILT_RULES_INSTALL",
            List.of(),
            new BuiltInFunctionDefinition("PREBUILT_RULES_INSTALL", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "PUT", "/api/detection_engine/rules/prepackaged", null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("PREBUILT_RULES_INSTALL failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerPrebuiltRulesStatus(ExecutionContext context) {
        context.declareFunction("PREBUILT_RULES_STATUS",
            List.of(),
            new BuiltInFunctionDefinition("PREBUILT_RULES_STATUS", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/detection_engine/rules/prepackaged/_status", null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("PREBUILT_RULES_STATUS failed: " + e.getMessage(), e));
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
