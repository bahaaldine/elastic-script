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
 * Kibana Alerting API functions for elastic-script.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.RUNBOOK,
    description = "Kibana Alerting functions for managing alert rules and alerts."
)
public class AlertingFunctions {

    public static void registerAll(ExecutionContext context) {
        registerAlertRuleList(context);
        registerAlertRuleGet(context);
        registerAlertRuleCreate(context);
        registerAlertRuleUpdate(context);
        registerAlertRuleDelete(context);
        registerAlertRuleEnable(context);
        registerAlertRuleDisable(context);
        registerAlertRuleMute(context);
        registerAlertRuleUnmute(context);
        registerAlertFind(context);
        registerAlertStatusSet(context);
        registerAlertRuleTypes(context);
    }

    public static void registerAlertRuleList(ExecutionContext context) {
        context.declareFunction("ALERT_RULE_LIST",
            List.of(
                new Parameter("filter", "STRING", ParameterMode.IN),
                new Parameter("per_page", "INTEGER", ParameterMode.IN),
                new Parameter("page", "INTEGER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ALERT_RULE_LIST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String filter = args.size() > 0 && args.get(0) != null ? args.get(0).toString() : "";
                    int perPage = args.size() > 1 && args.get(1) != null ? ((Number) args.get(1)).intValue() : 20;
                    int page = args.size() > 2 && args.get(2) != null ? ((Number) args.get(2)).intValue() : 1;
                    
                    String path = "/api/alerting/rules/_find?per_page=" + perPage + "&page=" + page;
                    if (!filter.isEmpty()) {
                        path += "&filter=" + java.net.URLEncoder.encode(filter, "UTF-8");
                    }
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", path, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ALERT_RULE_LIST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAlertRuleGet(ExecutionContext context) {
        context.declareFunction("ALERT_RULE_GET",
            List.of(new Parameter("rule_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ALERT_RULE_GET", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String ruleId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/alerting/rule/" + ruleId, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ALERT_RULE_GET failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAlertRuleCreate(ExecutionContext context) {
        context.declareFunction("ALERT_RULE_CREATE",
            List.of(new Parameter("config", "DOCUMENT", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ALERT_RULE_CREATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    Map<String, Object> config = toMap(args.get(0));
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/alerting/rule", config);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ALERT_RULE_CREATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAlertRuleUpdate(ExecutionContext context) {
        context.declareFunction("ALERT_RULE_UPDATE",
            List.of(
                new Parameter("rule_id", "STRING", ParameterMode.IN),
                new Parameter("config", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ALERT_RULE_UPDATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String ruleId = args.get(0).toString();
                    Map<String, Object> config = toMap(args.get(1));
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "PUT", "/api/alerting/rule/" + ruleId, config);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ALERT_RULE_UPDATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAlertRuleDelete(ExecutionContext context) {
        context.declareFunction("ALERT_RULE_DELETE",
            List.of(new Parameter("rule_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ALERT_RULE_DELETE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String ruleId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "DELETE", "/api/alerting/rule/" + ruleId, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ALERT_RULE_DELETE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAlertRuleEnable(ExecutionContext context) {
        context.declareFunction("ALERT_RULE_ENABLE",
            List.of(new Parameter("rule_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ALERT_RULE_ENABLE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String ruleId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/alerting/rule/" + ruleId + "/_enable", null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ALERT_RULE_ENABLE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAlertRuleDisable(ExecutionContext context) {
        context.declareFunction("ALERT_RULE_DISABLE",
            List.of(new Parameter("rule_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ALERT_RULE_DISABLE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String ruleId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/alerting/rule/" + ruleId + "/_disable", null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ALERT_RULE_DISABLE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAlertRuleMute(ExecutionContext context) {
        context.declareFunction("ALERT_RULE_MUTE",
            List.of(new Parameter("rule_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ALERT_RULE_MUTE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String ruleId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/alerting/rule/" + ruleId + "/_mute_all", null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ALERT_RULE_MUTE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAlertRuleUnmute(ExecutionContext context) {
        context.declareFunction("ALERT_RULE_UNMUTE",
            List.of(new Parameter("rule_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ALERT_RULE_UNMUTE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String ruleId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/alerting/rule/" + ruleId + "/_unmute_all", null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ALERT_RULE_UNMUTE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAlertFind(ExecutionContext context) {
        context.declareFunction("ALERT_FIND",
            List.of(new Parameter("query", "DOCUMENT", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ALERT_FIND", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    Map<String, Object> query = args.size() > 0 && args.get(0) != null ? toMap(args.get(0)) : new HashMap<>();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/alerting/rules/_find", query);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ALERT_FIND failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAlertStatusSet(ExecutionContext context) {
        context.declareFunction("ALERT_STATUS_SET",
            List.of(
                new Parameter("rule_id", "STRING", ParameterMode.IN),
                new Parameter("alert_id", "STRING", ParameterMode.IN),
                new Parameter("action", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ALERT_STATUS_SET", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String ruleId = args.get(0).toString();
                    String alertId = args.get(1).toString();
                    String action = args.get(2).toString().toLowerCase();
                    
                    String path = "/api/alerting/rule/" + ruleId + "/alert/" + alertId + "/_" + action;
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", path, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ALERT_STATUS_SET failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAlertRuleTypes(ExecutionContext context) {
        context.declareFunction("ALERT_RULE_TYPES",
            List.of(),
            new BuiltInFunctionDefinition("ALERT_RULE_TYPES", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/alerting/rule_types", null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ALERT_RULE_TYPES failed: " + e.getMessage(), e));
                }
            })
        );
    }
}
