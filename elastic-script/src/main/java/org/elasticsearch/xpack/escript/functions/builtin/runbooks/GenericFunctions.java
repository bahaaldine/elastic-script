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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic HTTP/Webhook functions for elastic-script.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.RUNBOOK,
    description = "Generic integration functions for webhooks and HTTP calls."
)
public class GenericFunctions {

    private static final Logger LOGGER = LogManager.getLogger(GenericFunctions.class);

    public static void registerAll(ExecutionContext context) {
        LOGGER.info("Registering Generic built-in functions");
        registerWebhook(context);
        registerHttpGet(context);
        registerHttpPost(context);
    }

    public static void registerWebhook(ExecutionContext context) {
        context.declareFunction("WEBHOOK",
            List.of(new Parameter("url", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("WEBHOOK", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String urlStr = args.get(0).toString();
                    String method = args.size() > 1 && args.get(1) != null ? args.get(1).toString().toUpperCase() : "POST";
                    String headersJson = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : null;
                    String body = args.size() > 3 && args.get(3) != null ? args.get(3).toString() : null;
                    int timeout = args.size() > 4 && args.get(4) != null ? ((Number) args.get(4)).intValue() : 30;
                    
                    listener.onResponse(makeHttpRequest(urlStr, method, headersJson, body, timeout));
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("WEBHOOK failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerHttpGet(ExecutionContext context) {
        context.declareFunction("HTTP_GET",
            List.of(new Parameter("url", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("HTTP_GET", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String urlStr = args.get(0).toString();
                    String headersJson = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : null;
                    
                    Map<String, Object> result = makeHttpRequest(urlStr, "GET", headersJson, null, 30);
                    listener.onResponse(result.get("body"));
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("HTTP_GET failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerHttpPost(ExecutionContext context) {
        context.declareFunction("HTTP_POST",
            List.of(
                new Parameter("url", "STRING", ParameterMode.IN),
                new Parameter("body", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("HTTP_POST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String urlStr = args.get(0).toString();
                    String body = args.get(1).toString();
                    String headersJson = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : null;
                    
                    Map<String, Object> result = makeHttpRequest(urlStr, "POST", headersJson, body, 30);
                    listener.onResponse(result.get("body"));
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("HTTP_POST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    private static Map<String, Object> makeHttpRequest(String urlStr, String method, String headersJson, 
            String body, int timeoutSeconds) throws Exception {
        
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "elastic-script/1.0");
        
        if (headersJson != null && !headersJson.isEmpty()) {
            Map<String, Object> parsedHeaders = parseJsonToMap(headersJson);
            for (Map.Entry<String, Object> entry : parsedHeaders.entrySet()) {
                headers.put(entry.getKey(), entry.getValue().toString());
            }
        }
        
        if (body != null && !body.isEmpty() && !headers.containsKey("Content-Type")) {
            if (body.trim().startsWith("{") || body.trim().startsWith("[")) {
                headers.put("Content-Type", "application/json");
            } else {
                headers.put("Content-Type", "text/plain");
            }
        }
        
        HttpClient client = HttpClientHolder.get();
        Map<String, Object> result = new HashMap<>();
        
        try {
            String responseBody = client.request(method, urlStr, headers, body);
            result.put("status_code", 200);
            result.put("status_message", "OK");
            result.put("body", responseBody != null ? responseBody.trim() : "");
            result.put("success", true);
            
            if (responseBody != null && !responseBody.isEmpty()) {
                String bodyStr = responseBody.trim();
                if (bodyStr.startsWith("{") || bodyStr.startsWith("[")) {
                    try {
                        if (bodyStr.startsWith("{")) {
                            result.put("json", parseJsonToMap(bodyStr));
                        } else {
                            result.put("json", parseJsonToList(bodyStr));
                        }
                    } catch (Exception e) {
                        // Not valid JSON
                    }
                }
            }
        } catch (HttpClientException e) {
            if (e.isHttpError()) {
                result.put("status_code", e.getStatusCode());
                result.put("body", e.getResponseBody() != null ? e.getResponseBody() : "");
                result.put("success", false);
            } else {
                throw e;
            }
        }
        
        return result;
    }

    private static Map<String, Object> parseJsonToMap(String json) throws Exception {
        try (XContentParser parser = XContentType.JSON.xContent().createParser(
                XContentParserConfiguration.EMPTY, 
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))) {
            return parser.map();
        }
    }

    private static List<Object> parseJsonToList(String json) throws Exception {
        try (XContentParser parser = XContentType.JSON.xContent().createParser(
                XContentParserConfiguration.EMPTY, 
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))) {
            return parser.list();
        }
    }
}
