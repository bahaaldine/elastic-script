/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.builtin.runbooks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.common.Strings;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.XContentParserConfiguration;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;
import org.elasticsearch.xpack.escript.functions.api.FunctionCategory;
import org.elasticsearch.xpack.escript.functions.api.FunctionCollectionSpec;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.http.HttpClient;
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.http.HttpClientHolder;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kubernetes integration functions for elastic-script.
 * Provides operations for managing Kubernetes resources.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.RUNBOOK,
    description = "Kubernetes integration functions for container orchestration operations."
)
public class KubernetesFunctions {

    private static final Logger LOGGER = LogManager.getLogger(KubernetesFunctions.class);

    public static void registerAll(ExecutionContext context) {
        LOGGER.info("Registering Kubernetes built-in functions");
        registerK8sScale(context);
        registerK8sRestart(context);
        registerK8sGet(context);
        registerK8sGetPods(context);
        registerK8sDelete(context);
        registerK8sLogs(context);
        registerK8sDescribe(context);
    }

    private static K8sConfig getConfig(List<Object> args, int apiServerIndex, int tokenIndex, int namespaceIndex) {
        String apiServer = null;
        String token = null;
        String namespace = "default";

        if (args.size() > apiServerIndex && args.get(apiServerIndex) != null) {
            apiServer = args.get(apiServerIndex).toString();
        }
        if (args.size() > tokenIndex && args.get(tokenIndex) != null) {
            token = args.get(tokenIndex).toString();
        }
        if (args.size() > namespaceIndex && args.get(namespaceIndex) != null) {
            String ns = args.get(namespaceIndex).toString();
            if (!ns.isEmpty()) {
                namespace = ns;
            }
        }

        if (apiServer == null || apiServer.isEmpty()) {
            apiServer = System.getenv("K8S_API_SERVER");
        }
        if (token == null || token.isEmpty()) {
            token = System.getenv("K8S_TOKEN");
        }

        if (apiServer == null || apiServer.isEmpty()) {
            throw new RuntimeException("K8S_API_SERVER not configured");
        }
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("K8S_TOKEN not configured");
        }

        return new K8sConfig(apiServer, token, namespace);
    }

    private static String k8sRequest(K8sConfig config, String method, String path, String body) throws Exception {
        String urlStr = config.apiServer + path;
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + config.token);
        headers.put("Accept", "application/json");
        if (body != null && !body.isEmpty()) {
            headers.put("Content-Type", "application/json");
        }
        
        HttpClient client = HttpClientHolder.get();
        return client.request(method, urlStr, headers, body);
    }

    public static void registerK8sScale(ExecutionContext context) {
        context.declareFunction("K8S_SCALE",
            List.of(
                new Parameter("deployment", "STRING", ParameterMode.IN),
                new Parameter("replicas", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("K8S_SCALE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String deployment = args.get(0).toString();
                    int replicas = ((Number) args.get(1)).intValue();
                    String namespace = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : "default";
                    
                    K8sConfig config = getConfig(args, 3, 4, 2);
                    
                    String path = "/apis/apps/v1/namespaces/" + namespace + "/deployments/" + deployment + "/scale";
                    XContentBuilder builder = XContentFactory.jsonBuilder();
                    builder.startObject();
                    builder.field("kind", "Scale");
                    builder.field("apiVersion", "autoscaling/v1");
                    builder.startObject("metadata").field("name", deployment).field("namespace", namespace).endObject();
                    builder.startObject("spec").field("replicas", replicas).endObject();
                    builder.endObject();
                    
                    k8sRequest(config, "PUT", path, Strings.toString(builder));
                    listener.onResponse(true);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("K8S_SCALE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerK8sRestart(ExecutionContext context) {
        context.declareFunction("K8S_RESTART",
            List.of(new Parameter("deployment", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("K8S_RESTART", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String deployment = args.get(0).toString();
                    String namespace = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : "default";
                    
                    K8sConfig config = getConfig(args, 2, 3, 1);
                    
                    String path = "/apis/apps/v1/namespaces/" + namespace + "/deployments/" + deployment;
                    String timestamp = java.time.Instant.now().toString();
                    String body = "{\"spec\":{\"template\":{\"metadata\":{\"annotations\":{\"kubectl.kubernetes.io/restartedAt\":\"" + timestamp + "\"}}}}}";
                    k8sRequest(config, "PATCH", path, body);
                    listener.onResponse(true);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("K8S_RESTART failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerK8sGet(ExecutionContext context) {
        context.declareFunction("K8S_GET",
            List.of(
                new Parameter("resource_type", "STRING", ParameterMode.IN),
                new Parameter("name", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("K8S_GET", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String resourceType = args.get(0).toString().toLowerCase();
                    String name = args.get(1).toString();
                    String namespace = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : "default";
                    
                    K8sConfig config = getConfig(args, 3, 4, 2);
                    
                    String path = getResourcePath(resourceType, namespace, name);
                    String response = k8sRequest(config, "GET", path, null);
                    listener.onResponse(parseJsonToMap(response));
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("K8S_GET failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerK8sGetPods(ExecutionContext context) {
        context.declareFunction("K8S_GET_PODS",
            List.of(),
            new BuiltInFunctionDefinition("K8S_GET_PODS", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String labelSelector = args.size() > 0 && args.get(0) != null ? args.get(0).toString() : "";
                    String namespace = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : "default";
                    
                    K8sConfig config = getConfig(args, 2, 3, 1);
                    
                    String path = "/api/v1/namespaces/" + namespace + "/pods";
                    if (!labelSelector.isEmpty()) {
                        path += "?labelSelector=" + java.net.URLEncoder.encode(labelSelector, "UTF-8");
                    }
                    String response = k8sRequest(config, "GET", path, null);
                    Map<String, Object> result = parseJsonToMap(response);
                    listener.onResponse(result.get("items"));
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("K8S_GET_PODS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerK8sDelete(ExecutionContext context) {
        context.declareFunction("K8S_DELETE",
            List.of(
                new Parameter("resource_type", "STRING", ParameterMode.IN),
                new Parameter("name", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("K8S_DELETE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String resourceType = args.get(0).toString().toLowerCase();
                    String name = args.get(1).toString();
                    String namespace = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : "default";
                    
                    K8sConfig config = getConfig(args, 3, 4, 2);
                    
                    String path = getResourcePath(resourceType, namespace, name);
                    k8sRequest(config, "DELETE", path, null);
                    listener.onResponse(true);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("K8S_DELETE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerK8sLogs(ExecutionContext context) {
        context.declareFunction("K8S_LOGS",
            List.of(new Parameter("pod", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("K8S_LOGS", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String pod = args.get(0).toString();
                    String container = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : null;
                    int tailLines = args.size() > 2 && args.get(2) != null ? ((Number) args.get(2)).intValue() : 100;
                    String namespace = args.size() > 3 && args.get(3) != null ? args.get(3).toString() : "default";
                    
                    K8sConfig config = getConfig(args, 4, 5, 3);
                    
                    StringBuilder path = new StringBuilder("/api/v1/namespaces/" + namespace + "/pods/" + pod + "/log");
                    path.append("?tailLines=").append(tailLines);
                    if (container != null && !container.isEmpty()) {
                        path.append("&container=").append(java.net.URLEncoder.encode(container, "UTF-8"));
                    }
                    listener.onResponse(k8sRequest(config, "GET", path.toString(), null));
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("K8S_LOGS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerK8sDescribe(ExecutionContext context) {
        context.declareFunction("K8S_DESCRIBE",
            List.of(
                new Parameter("resource_type", "STRING", ParameterMode.IN),
                new Parameter("name", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("K8S_DESCRIBE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String resourceType = args.get(0).toString().toLowerCase();
                    String name = args.get(1).toString();
                    String namespace = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : "default";
                    
                    K8sConfig config = getConfig(args, 3, 4, 2);
                    
                    String path = getResourcePath(resourceType, namespace, name);
                    String response = k8sRequest(config, "GET", path, null);
                    listener.onResponse(parseJsonToMap(response));
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("K8S_DESCRIBE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    private static String getResourcePath(String resourceType, String namespace, String name) {
        switch (resourceType) {
            case "pod": case "pods": return "/api/v1/namespaces/" + namespace + "/pods/" + name;
            case "service": case "services": case "svc": return "/api/v1/namespaces/" + namespace + "/services/" + name;
            case "deployment": case "deployments": case "deploy": return "/apis/apps/v1/namespaces/" + namespace + "/deployments/" + name;
            case "configmap": case "configmaps": case "cm": return "/api/v1/namespaces/" + namespace + "/configmaps/" + name;
            case "secret": case "secrets": return "/api/v1/namespaces/" + namespace + "/secrets/" + name;
            default: throw new RuntimeException("Unknown resource type: " + resourceType);
        }
    }

    private static Map<String, Object> parseJsonToMap(String json) throws Exception {
        try (XContentParser parser = XContentType.JSON.xContent().createParser(
                XContentParserConfiguration.EMPTY, 
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))) {
            return parser.map();
        }
    }

    private static class K8sConfig {
        final String apiServer;
        final String token;
        final String namespace;

        K8sConfig(String apiServer, String token, String namespace) {
            this.apiServer = apiServer;
            this.token = token;
            this.namespace = namespace;
        }
    }
}
