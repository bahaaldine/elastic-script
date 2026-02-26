/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.builtin.elasticsearch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.XContentParserConfiguration;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.http.HttpClient;
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.http.HttpClientException;
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.http.HttpClientHolder;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class providing HTTP utilities for Elasticsearch REST API functions.
 * Uses stable REST API instead of internal Java client APIs.
 */
public abstract class ElasticsearchHttpFunctions {

    private static final Logger LOGGER = LogManager.getLogger(ElasticsearchHttpFunctions.class);
    private static final String DEFAULT_ES_URL = "http://localhost:9200";

    /**
     * Get the Elasticsearch base URL from environment or use default.
     */
    protected static String getElasticsearchUrl() {
        String url = System.getenv("ELASTICSEARCH_URL");
        if (url == null || url.isEmpty()) {
            url = System.getenv("ES_URL");
        }
        return url != null && !url.isEmpty() ? url : DEFAULT_ES_URL;
    }

    /**
     * Get authentication headers for Elasticsearch.
     */
    protected static Map<String, String> getElasticsearchHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");

        String apiKey = System.getenv("ELASTICSEARCH_API_KEY");
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.put("Authorization", "ApiKey " + apiKey);
        } else {
            String user = System.getenv("ELASTIC_USER");
            String pass = System.getenv("ELASTIC_PASSWORD");
            if (user == null || user.isEmpty()) user = "elastic-admin";
            if (pass == null || pass.isEmpty()) pass = "elastic-password";
            String credentials = Base64.getEncoder()
                .encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
            headers.put("Authorization", "Basic " + credentials);
        }

        return headers;
    }

    /**
     * Make an HTTP request to the Elasticsearch REST API.
     * 
     * @param method HTTP method (GET, POST, PUT, DELETE)
     * @param path API path (e.g., "/my-index/_doc/1")
     * @param body Request body (can be null)
     * @return Response as Map
     */
    protected static Map<String, Object> esRequest(String method, String path, Map<String, Object> body) throws Exception {
        return esRequest(getElasticsearchUrl(), method, path, body);
    }

    /**
     * Make an HTTP request to the Elasticsearch REST API with custom URL.
     */
    protected static Map<String, Object> esRequest(String esUrl, String method, String path, Map<String, Object> body) 
            throws Exception {
        HttpClient client = HttpClientHolder.get();
        String url = esUrl + (path.startsWith("/") ? path : "/" + path);

        String bodyStr = null;
        if (body != null && !body.isEmpty()) {
            bodyStr = mapToJson(body);
        }

        try {
            String response = client.request(method, url, getElasticsearchHeaders(), bodyStr);
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

    /**
     * Make an HTTP request with a raw JSON string body.
     */
    protected static Map<String, Object> esRequestRaw(String method, String path, String bodyJson) throws Exception {
        HttpClient client = HttpClientHolder.get();
        String url = getElasticsearchUrl() + (path.startsWith("/") ? path : "/" + path);

        try {
            String response = client.request(method, url, getElasticsearchHeaders(), bodyJson);
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

    /**
     * Convert a Map to JSON string.
     */
    protected static String mapToJson(Map<String, Object> map) throws Exception {
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.map(map);
        return org.elasticsearch.common.Strings.toString(builder);
    }

    /**
     * Convert a List to JSON string.
     */
    protected static String listToJson(List<?> list) throws Exception {
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startArray();
        for (Object item : list) {
            builder.value(item);
        }
        builder.endArray();
        return org.elasticsearch.common.Strings.toString(builder);
    }

    /**
     * Parse JSON string to Map.
     */
    @SuppressWarnings("unchecked")
    protected static Map<String, Object> parseJsonToMap(String json) throws Exception {
        try (XContentParser parser = XContentType.JSON.xContent().createParser(
                XContentParserConfiguration.EMPTY,
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))) {
            return parser.map();
        }
    }

    /**
     * Parse JSON string to List.
     */
    @SuppressWarnings("unchecked")
    protected static List<Object> parseJsonToList(String json) throws Exception {
        try (XContentParser parser = XContentType.JSON.xContent().createParser(
                XContentParserConfiguration.EMPTY,
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))) {
            return parser.list();
        }
    }

    /**
     * Convert Object to Map safely.
     */
    @SuppressWarnings("unchecked")
    protected static Map<String, Object> toMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return new HashMap<>();
    }

    /**
     * Convert Object to String safely.
     */
    protected static String toString(Object obj) {
        return obj != null ? obj.toString() : null;
    }

    /**
     * Convert Object to int safely.
     */
    protected static int toInt(Object obj, int defaultValue) {
        if (obj == null) return defaultValue;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Convert Object to boolean safely.
     */
    protected static boolean toBoolean(Object obj, boolean defaultValue) {
        if (obj == null) return defaultValue;
        if (obj instanceof Boolean) return (Boolean) obj;
        String s = obj.toString().toLowerCase();
        if ("true".equals(s) || "yes".equals(s) || "1".equals(s)) return true;
        if ("false".equals(s) || "no".equals(s) || "0".equals(s)) return false;
        return defaultValue;
    }
}
