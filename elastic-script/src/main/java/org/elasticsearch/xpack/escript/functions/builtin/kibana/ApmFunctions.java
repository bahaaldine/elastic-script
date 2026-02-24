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
 * Kibana APM API functions for elastic-script.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.RUNBOOK,
    description = "APM functions for service monitoring and management."
)
public class ApmFunctions {

    public static void registerAll(ExecutionContext context) {
        registerApmServiceList(context);
        registerApmServiceGet(context);
        registerApmTransactions(context);
        registerApmErrors(context);
        registerApmServiceMap(context);
        registerApmAgentConfigList(context);
        registerApmAgentConfigCreate(context);
        registerApmSourceMapUpload(context);
        registerApmAnnotationCreate(context);
    }

    public static void registerApmServiceList(ExecutionContext context) {
        context.declareFunction("APM_SERVICE_LIST",
            List.of(
                new Parameter("start", "STRING", ParameterMode.IN),
                new Parameter("end", "STRING", ParameterMode.IN),
                new Parameter("environment", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("APM_SERVICE_LIST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String start = args.size() > 0 && args.get(0) != null ? args.get(0).toString() : "now-24h";
                    String end = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : "now";
                    String environment = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : "";
                    
                    StringBuilder path = new StringBuilder("/api/apm/services?start=" + start + "&end=" + end);
                    if (!environment.isEmpty()) {
                        path.append("&environment=").append(environment);
                    }
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", path.toString(), null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("APM_SERVICE_LIST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerApmServiceGet(ExecutionContext context) {
        context.declareFunction("APM_SERVICE_GET",
            List.of(
                new Parameter("service_name", "STRING", ParameterMode.IN),
                new Parameter("start", "STRING", ParameterMode.IN),
                new Parameter("end", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("APM_SERVICE_GET", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String serviceName = args.get(0).toString();
                    String start = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : "now-24h";
                    String end = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : "now";
                    
                    String path = "/api/apm/services/" + serviceName + "?start=" + start + "&end=" + end;
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", path, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("APM_SERVICE_GET failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerApmTransactions(ExecutionContext context) {
        context.declareFunction("APM_TRANSACTIONS",
            List.of(
                new Parameter("service_name", "STRING", ParameterMode.IN),
                new Parameter("start", "STRING", ParameterMode.IN),
                new Parameter("end", "STRING", ParameterMode.IN),
                new Parameter("transaction_type", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("APM_TRANSACTIONS", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String serviceName = args.get(0).toString();
                    String start = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : "now-24h";
                    String end = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : "now";
                    String transactionType = args.size() > 3 && args.get(3) != null ? args.get(3).toString() : "";
                    
                    StringBuilder path = new StringBuilder("/api/apm/services/" + serviceName + "/transactions?start=" + start + "&end=" + end);
                    if (!transactionType.isEmpty()) {
                        path.append("&transactionType=").append(transactionType);
                    }
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", path.toString(), null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("APM_TRANSACTIONS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerApmErrors(ExecutionContext context) {
        context.declareFunction("APM_ERRORS",
            List.of(
                new Parameter("service_name", "STRING", ParameterMode.IN),
                new Parameter("start", "STRING", ParameterMode.IN),
                new Parameter("end", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("APM_ERRORS", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String serviceName = args.get(0).toString();
                    String start = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : "now-24h";
                    String end = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : "now";
                    
                    String path = "/api/apm/services/" + serviceName + "/errors?start=" + start + "&end=" + end;
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", path, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("APM_ERRORS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerApmServiceMap(ExecutionContext context) {
        context.declareFunction("APM_SERVICE_MAP",
            List.of(
                new Parameter("start", "STRING", ParameterMode.IN),
                new Parameter("end", "STRING", ParameterMode.IN),
                new Parameter("environment", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("APM_SERVICE_MAP", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String start = args.size() > 0 && args.get(0) != null ? args.get(0).toString() : "now-24h";
                    String end = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : "now";
                    String environment = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : "";
                    
                    StringBuilder path = new StringBuilder("/api/apm/service-map?start=" + start + "&end=" + end);
                    if (!environment.isEmpty()) {
                        path.append("&environment=").append(environment);
                    }
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", path.toString(), null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("APM_SERVICE_MAP failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerApmAgentConfigList(ExecutionContext context) {
        context.declareFunction("APM_AGENT_CONFIG_LIST",
            List.of(),
            new BuiltInFunctionDefinition("APM_AGENT_CONFIG_LIST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/apm/settings/agent-configuration", null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("APM_AGENT_CONFIG_LIST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerApmAgentConfigCreate(ExecutionContext context) {
        context.declareFunction("APM_AGENT_CONFIG_CREATE",
            List.of(
                new Parameter("service_name", "STRING", ParameterMode.IN),
                new Parameter("settings", "DOCUMENT", ParameterMode.IN),
                new Parameter("environment", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("APM_AGENT_CONFIG_CREATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String serviceName = args.get(0).toString();
                    Map<String, Object> settings = toMap(args.get(1));
                    String environment = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : "";
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("service", Map.of("name", serviceName, "environment", environment));
                    body.put("settings", settings);
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "PUT", "/api/apm/settings/agent-configuration", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("APM_AGENT_CONFIG_CREATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerApmSourceMapUpload(ExecutionContext context) {
        context.declareFunction("APM_SOURCEMAP_UPLOAD",
            List.of(
                new Parameter("service_name", "STRING", ParameterMode.IN),
                new Parameter("service_version", "STRING", ParameterMode.IN),
                new Parameter("bundle_filepath", "STRING", ParameterMode.IN),
                new Parameter("sourcemap", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("APM_SOURCEMAP_UPLOAD", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String serviceName = args.get(0).toString();
                    String serviceVersion = args.get(1).toString();
                    String bundleFilepath = args.get(2).toString();
                    String sourcemap = args.get(3).toString();
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("service_name", serviceName);
                    body.put("service_version", serviceVersion);
                    body.put("bundle_filepath", bundleFilepath);
                    body.put("sourcemap", sourcemap);
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/apm/sourcemaps", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("APM_SOURCEMAP_UPLOAD failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerApmAnnotationCreate(ExecutionContext context) {
        context.declareFunction("APM_ANNOTATION_CREATE",
            List.of(
                new Parameter("service_name", "STRING", ParameterMode.IN),
                new Parameter("message", "STRING", ParameterMode.IN),
                new Parameter("timestamp", "STRING", ParameterMode.IN),
                new Parameter("tags", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("APM_ANNOTATION_CREATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String serviceName = args.get(0).toString();
                    String message = args.get(1).toString();
                    String timestamp = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : null;
                    Map<String, Object> tags = args.size() > 3 && args.get(3) != null ? toMap(args.get(3)) : new HashMap<>();
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("service_name", serviceName);
                    body.put("message", message);
                    if (timestamp != null) {
                        body.put("@timestamp", timestamp);
                    }
                    if (!tags.isEmpty()) {
                        body.put("tags", tags);
                    }
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/apm/services/" + serviceName + "/annotation", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("APM_ANNOTATION_CREATE failed: " + e.getMessage(), e));
                }
            })
        );
    }
}
