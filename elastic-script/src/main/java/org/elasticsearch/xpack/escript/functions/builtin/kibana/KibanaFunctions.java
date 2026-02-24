/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.builtin.kibana;

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
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.http.HttpClientException;
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.http.HttpClientHolder;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Core Kibana API functions for elastic-script.
 * Provides generic Kibana API access and status functions.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.RUNBOOK,
    description = "Core Kibana API functions for status, features, and generic API calls."
)
public class KibanaFunctions {

    private static final Logger LOGGER = LogManager.getLogger(KibanaFunctions.class);
    private static final String DEFAULT_KIBANA_URL = "http://localhost:5601";

    public static void registerAll(ExecutionContext context) {
        registerKibanaRequest(context);
        registerKibanaStatus(context);
        registerKibanaFeatures(context);
    }

    /**
     * KIBANA_REQUEST - Make a generic Kibana API request.
     */
    public static void registerKibanaRequest(ExecutionContext context) {
        context.declareFunction("KIBANA_REQUEST",
            List.of(
                new Parameter("method", "STRING", ParameterMode.IN),
                new Parameter("path", "STRING", ParameterMode.IN),
                new Parameter("body", "DOCUMENT", ParameterMode.IN),
                new Parameter("kibana_url", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("KIBANA_REQUEST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String method = args.get(0).toString().toUpperCase();
                    String path = args.get(1).toString();
                    Map<String, Object> body = args.size() > 2 && args.get(2) != null ? toMap(args.get(2)) : null;
                    String kibanaUrl = args.size() > 3 && args.get(3) != null ? args.get(3).toString() : getKibanaUrl();
                    
                    Map<String, Object> result = kibanaRequest(kibanaUrl, method, path, body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("KIBANA_REQUEST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    /**
     * KIBANA_STATUS - Get Kibana status.
     */
    public static void registerKibanaStatus(ExecutionContext context) {
        context.declareFunction("KIBANA_STATUS",
            List.of(new Parameter("kibana_url", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("KIBANA_STATUS", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String kibanaUrl = args.size() > 0 && args.get(0) != null ? args.get(0).toString() : getKibanaUrl();
                    Map<String, Object> result = kibanaRequest(kibanaUrl, "GET", "/api/status", null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("KIBANA_STATUS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    /**
     * KIBANA_FEATURES - Get available Kibana features.
     */
    public static void registerKibanaFeatures(ExecutionContext context) {
        context.declareFunction("KIBANA_FEATURES",
            List.of(new Parameter("kibana_url", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("KIBANA_FEATURES", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String kibanaUrl = args.size() > 0 && args.get(0) != null ? args.get(0).toString() : getKibanaUrl();
                    Map<String, Object> result = kibanaRequest(kibanaUrl, "GET", "/api/features", null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("KIBANA_FEATURES failed: " + e.getMessage(), e));
                }
            })
        );
    }

    // ========================================================================
    // Shared utilities for all Kibana functions
    // ========================================================================

    public static String getKibanaUrl() {
        String envUrl = System.getenv("KIBANA_URL");
        return envUrl != null && !envUrl.isEmpty() ? envUrl : DEFAULT_KIBANA_URL;
    }

    public static Map<String, String> getKibanaHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("kbn-xsrf", "true");
        headers.put("x-elastic-internal-origin", "Kibana");
        
        String apiKey = System.getenv("KIBANA_API_KEY");
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.put("Authorization", "ApiKey " + apiKey);
        } else {
            String user = System.getenv("ELASTIC_USER");
            String pass = System.getenv("ELASTIC_PASSWORD");
            if (user == null) user = "elastic-admin";
            if (pass == null) pass = "elastic-password";
            String credentials = java.util.Base64.getEncoder()
                .encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
            headers.put("Authorization", "Basic " + credentials);
        }
        
        return headers;
    }

    public static Map<String, Object> kibanaRequest(String kibanaUrl, String method, String path, Map<String, Object> body) 
            throws Exception {
        HttpClient client = HttpClientHolder.get();
        String url = kibanaUrl + (path.startsWith("/") ? path : "/" + path);
        
        String bodyStr = null;
        if (body != null && !body.isEmpty()) {
            bodyStr = mapToJson(body);
        }
        
        try {
            String response = client.request(method, url, getKibanaHeaders(), bodyStr);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            
            if (response != null && !response.isEmpty()) {
                String trimmed = response.trim();
                if (trimmed.startsWith("{")) {
                    result.put("data", parseJsonToMap(trimmed));
                } else if (trimmed.startsWith("[")) {
                    result.put("data", parseJsonToList(trimmed));
                } else {
                    result.put("data", trimmed);
                }
            }
            return result;
        } catch (HttpClientException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("status_code", e.getStatusCode());
            error.put("error", e.getResponseBody());
            return error;
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return new HashMap<>();
    }

    public static String mapToJson(Map<String, Object> map) throws Exception {
        try (XContentBuilder builder = XContentFactory.jsonBuilder()) {
            builder.map(map);
            return Strings.toString(builder);
        }
    }

    public static Map<String, Object> parseJsonToMap(String json) throws Exception {
        if (json == null || json.isEmpty()) {
            return new HashMap<>();
        }
        try (XContentParser parser = XContentType.JSON.xContent().createParser(
                XContentParserConfiguration.EMPTY, 
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))) {
            return parser.map();
        }
    }

    public static List<Object> parseJsonToList(String json) throws Exception {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try (XContentParser parser = XContentType.JSON.xContent().createParser(
                XContentParserConfiguration.EMPTY, 
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))) {
            return parser.list();
        }
    }
}
