/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.connectors.datadog;

import org.elasticsearch.action.ActionListener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Datadog Connector for Moltler.
 * 
 * Provides integration with Datadog API for:
 * - Metrics: Query metrics, list metrics
 * - Monitors: List, mute, unmute monitors
 * - Events: List events, post events
 * - Logs: Query logs
 * - Dashboards: List dashboards
 * 
 * Configuration:
 * - api_key: Datadog API key
 * - app_key: Datadog Application key
 * - site: Datadog site (us1, us3, us5, eu1, ap1) - defaults to us1
 */
public class DatadogConnector {
    
    private static final Map<String, String> SITE_URLS = Map.of(
        "us1", "https://api.datadoghq.com",
        "us3", "https://api.us3.datadoghq.com",
        "us5", "https://api.us5.datadoghq.com",
        "eu1", "https://api.datadoghq.eu",
        "ap1", "https://api.ap1.datadoghq.com"
    );
    
    private final String apiKey;
    private final String appKey;
    private final String baseUrl;
    
    public DatadogConnector(Map<String, Object> config) {
        this.apiKey = (String) config.get("api_key");
        this.appKey = (String) config.get("app_key");
        
        String site = (String) config.getOrDefault("site", "us1");
        this.baseUrl = SITE_URLS.getOrDefault(site, SITE_URLS.get("us1"));
        
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("Datadog connector requires 'api_key' in config");
        }
        if (appKey == null || appKey.isEmpty()) {
            throw new IllegalArgumentException("Datadog connector requires 'app_key' in config");
        }
    }
    
    /**
     * Test the connection to Datadog.
     */
    public void testConnection(ActionListener<Map<String, Object>> listener) {
        try {
            Map<String, Object> result = makeRequest("/api/v1/validate");
            Map<String, Object> response = new HashMap<>();
            response.put("status", "connected");
            response.put("valid", result.get("valid"));
            listener.onResponse(response);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    // ==================== Metrics ====================
    
    /**
     * Query metrics.
     * 
     * @param query The metrics query (e.g., "avg:system.cpu.user{*}")
     * @param from Start time (Unix epoch seconds)
     * @param to End time (Unix epoch seconds)
     */
    public void queryMetrics(String query, long from, long to,
                            ActionListener<Map<String, Object>> listener) {
        try {
            String encodedQuery = java.net.URLEncoder.encode(query, StandardCharsets.UTF_8);
            String path = "/api/v1/query?query=" + encodedQuery + "&from=" + from + "&to=" + to;
            Map<String, Object> result = makeRequest(path);
            listener.onResponse(result);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * List active metrics.
     */
    public void listMetrics(long from, String host,
                           ActionListener<List<String>> listener) {
        try {
            String path = "/api/v1/metrics?from=" + from;
            if (host != null && !host.isEmpty()) {
                path += "&host=" + host;
            }
            Map<String, Object> result = makeRequest(path);
            @SuppressWarnings("unchecked")
            List<String> metrics = (List<String>) result.get("metrics");
            listener.onResponse(metrics != null ? metrics : List.of());
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    // ==================== Monitors ====================
    
    /**
     * List monitors.
     */
    public void listMonitors(String groupStates, String name, String tags,
                            ActionListener<List<Map<String, Object>>> listener) {
        try {
            StringBuilder path = new StringBuilder("/api/v1/monitor?");
            List<String> params = new ArrayList<>();
            if (groupStates != null) params.add("group_states=" + groupStates);
            if (name != null) params.add("name=" + java.net.URLEncoder.encode(name, StandardCharsets.UTF_8));
            if (tags != null) params.add("tags=" + tags);
            path.append(String.join("&", params));
            
            List<Map<String, Object>> monitors = makeRequestAsList(path.toString());
            listener.onResponse(monitors);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Get a specific monitor.
     */
    public void getMonitor(long monitorId,
                          ActionListener<Map<String, Object>> listener) {
        try {
            String path = "/api/v1/monitor/" + monitorId;
            Map<String, Object> monitor = makeRequest(path);
            listener.onResponse(monitor);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Mute a monitor.
     */
    public void muteMonitor(long monitorId, String scope, long end,
                           ActionListener<Map<String, Object>> listener) {
        try {
            Map<String, Object> payload = new HashMap<>();
            if (scope != null) payload.put("scope", scope);
            if (end > 0) payload.put("end", end);
            
            String path = "/api/v1/monitor/" + monitorId + "/mute";
            Map<String, Object> result = makePostRequest(path, payload);
            listener.onResponse(result);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Unmute a monitor.
     */
    public void unmuteMonitor(long monitorId, String scope,
                             ActionListener<Map<String, Object>> listener) {
        try {
            Map<String, Object> payload = new HashMap<>();
            if (scope != null) payload.put("scope", scope);
            
            String path = "/api/v1/monitor/" + monitorId + "/unmute";
            Map<String, Object> result = makePostRequest(path, payload);
            listener.onResponse(result);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    // ==================== Events ====================
    
    /**
     * List events.
     */
    public void listEvents(long start, long end, String priority, String sources, String tags,
                          ActionListener<Map<String, Object>> listener) {
        try {
            StringBuilder path = new StringBuilder("/api/v1/events?start=").append(start).append("&end=").append(end);
            if (priority != null) path.append("&priority=").append(priority);
            if (sources != null) path.append("&sources=").append(sources);
            if (tags != null) path.append("&tags=").append(tags);
            
            Map<String, Object> result = makeRequest(path.toString());
            listener.onResponse(result);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Post an event.
     */
    public void postEvent(String title, String text, String priority, String host,
                         List<String> tags, String alertType,
                         ActionListener<Map<String, Object>> listener) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("title", title);
            payload.put("text", text);
            if (priority != null) payload.put("priority", priority);
            if (host != null) payload.put("host", host);
            if (tags != null && !tags.isEmpty()) payload.put("tags", tags);
            if (alertType != null) payload.put("alert_type", alertType);
            
            Map<String, Object> result = makePostRequest("/api/v1/events", payload);
            listener.onResponse(result);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    // ==================== Logs ====================
    
    /**
     * Query logs.
     */
    public void queryLogs(String query, long from, long to, int limit,
                         ActionListener<Map<String, Object>> listener) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("query", query);
            payload.put("time", Map.of("from", from * 1000, "to", to * 1000)); // milliseconds
            payload.put("limit", limit > 0 ? limit : 50);
            
            Map<String, Object> result = makePostRequest("/api/v2/logs/events/search", payload);
            listener.onResponse(result);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    // ==================== Dashboards ====================
    
    /**
     * List dashboards.
     */
    public void listDashboards(ActionListener<Map<String, Object>> listener) {
        try {
            Map<String, Object> result = makeRequest("/api/v1/dashboard");
            listener.onResponse(result);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Get a specific dashboard.
     */
    public void getDashboard(String dashboardId,
                            ActionListener<Map<String, Object>> listener) {
        try {
            String path = "/api/v1/dashboard/" + dashboardId;
            Map<String, Object> dashboard = makeRequest(path);
            listener.onResponse(dashboard);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    // ==================== Hosts ====================
    
    /**
     * List hosts.
     */
    public void listHosts(String filter, String sortField, String sortDir, int count,
                         ActionListener<Map<String, Object>> listener) {
        try {
            StringBuilder path = new StringBuilder("/api/v1/hosts?");
            List<String> params = new ArrayList<>();
            if (filter != null) params.add("filter=" + java.net.URLEncoder.encode(filter, StandardCharsets.UTF_8));
            if (sortField != null) params.add("sort_field=" + sortField);
            if (sortDir != null) params.add("sort_dir=" + sortDir);
            if (count > 0) params.add("count=" + count);
            path.append(String.join("&", params));
            
            Map<String, Object> result = makeRequest(path.toString());
            listener.onResponse(result);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Mute a host.
     */
    public void muteHost(String hostName, String message, long end, boolean override,
                        ActionListener<Map<String, Object>> listener) {
        try {
            Map<String, Object> payload = new HashMap<>();
            if (message != null) payload.put("message", message);
            if (end > 0) payload.put("end", end);
            payload.put("override", override);
            
            String path = "/api/v1/host/" + hostName + "/mute";
            Map<String, Object> result = makePostRequest(path, payload);
            listener.onResponse(result);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Unmute a host.
     */
    public void unmuteHost(String hostName,
                          ActionListener<Map<String, Object>> listener) {
        try {
            String path = "/api/v1/host/" + hostName + "/unmute";
            Map<String, Object> result = makePostRequest(path, Map.of());
            listener.onResponse(result);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    // ==================== HTTP Helpers ====================
    
    private Map<String, Object> makeRequest(String path) throws Exception {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("DD-API-KEY", apiKey);
        conn.setRequestProperty("DD-APPLICATION-KEY", appKey);
        conn.setRequestProperty("Accept", "application/json");
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("Datadog API error: " + responseCode);
        }
        
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        return parseJson(response.toString());
    }
    
    private List<Map<String, Object>> makeRequestAsList(String path) throws Exception {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("DD-API-KEY", apiKey);
        conn.setRequestProperty("DD-APPLICATION-KEY", appKey);
        conn.setRequestProperty("Accept", "application/json");
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("Datadog API error: " + responseCode);
        }
        
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        // Return as list - simplified parsing
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("_raw", response.toString());
        result.add(item);
        return result;
    }
    
    private Map<String, Object> makePostRequest(String path, Map<String, Object> payload) throws Exception {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("DD-API-KEY", apiKey);
        conn.setRequestProperty("DD-APPLICATION-KEY", appKey);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        String jsonPayload = toJson(payload);
        conn.getOutputStream().write(jsonPayload.getBytes(StandardCharsets.UTF_8));
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200 && responseCode != 201 && responseCode != 202) {
            throw new RuntimeException("Datadog API error: " + responseCode);
        }
        
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        return parseJson(response.toString());
    }
    
    // Simple JSON parsing
    private Map<String, Object> parseJson(String json) {
        Map<String, Object> result = new HashMap<>();
        result.put("_raw", json);
        
        // Basic extraction of common fields
        if (json.contains("\"valid\"")) {
            result.put("valid", json.contains("\"valid\":true") || json.contains("\"valid\": true"));
        }
        
        return result;
    }
    
    @SuppressWarnings("unchecked")
    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else if (value instanceof List) {
                sb.append("[");
                boolean listFirst = true;
                for (Object item : (List<?>) value) {
                    if (!listFirst) sb.append(",");
                    listFirst = false;
                    if (item instanceof String) {
                        sb.append("\"").append(escapeJson((String) item)).append("\"");
                    } else if (item instanceof Map) {
                        sb.append(toJson((Map<String, Object>) item));
                    } else {
                        sb.append(item);
                    }
                }
                sb.append("]");
            } else if (value instanceof Map) {
                sb.append(toJson((Map<String, Object>) value));
            } else {
                sb.append("\"").append(value).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }
    
    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
