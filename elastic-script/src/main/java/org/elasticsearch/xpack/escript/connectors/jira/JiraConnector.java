/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.connectors.jira;

import org.elasticsearch.action.ActionListener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Jira Connector for Moltler.
 * 
 * Provides integration with Atlassian Jira API for:
 * - Issues: List, create, update, transition issues
 * - Projects: List projects, get details
 * - Sprints: List sprints, get sprint issues
 * - Boards: List boards, get board details
 * 
 * Configuration:
 * - url: Jira Cloud or Server URL (e.g., https://yourcompany.atlassian.net)
 * - email: User email (for Cloud) or username (for Server)
 * - api_token: API token (for Cloud) or password (for Server)
 * - project: Default project key (optional)
 */
public class JiraConnector {
    
    private final String baseUrl;
    private final String email;
    private final String apiToken;
    private final String defaultProject;
    private final String authHeader;
    
    public JiraConnector(Map<String, Object> config) {
        this.baseUrl = (String) config.get("url");
        this.email = (String) config.get("email");
        this.apiToken = (String) config.get("api_token");
        this.defaultProject = (String) config.get("project");
        
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalArgumentException("Jira connector requires 'url' in config");
        }
        if (email == null || apiToken == null) {
            throw new IllegalArgumentException("Jira connector requires 'email' and 'api_token' in config");
        }
        
        // Create Basic Auth header
        String auth = email + ":" + apiToken;
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Test the connection to Jira.
     */
    public void testConnection(ActionListener<Map<String, Object>> listener) {
        try {
            Map<String, Object> result = makeRequest("/rest/api/3/myself");
            Map<String, Object> response = new HashMap<>();
            response.put("status", "connected");
            response.put("user", result.get("displayName"));
            response.put("email", result.get("emailAddress"));
            listener.onResponse(response);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    // ==================== Issues ====================
    
    /**
     * Search for issues using JQL.
     */
    public void searchIssues(String jql, int maxResults,
                            ActionListener<List<Map<String, Object>>> listener) {
        try {
            String encodedJql = java.net.URLEncoder.encode(jql, StandardCharsets.UTF_8);
            String path = "/rest/api/3/search?jql=" + encodedJql + "&maxResults=" + maxResults;
            
            Map<String, Object> response = makeRequest(path);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> issues = (List<Map<String, Object>>) response.get("issues");
            listener.onResponse(issues != null ? issues : List.of());
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * List issues for a project.
     */
    public void listIssues(String projectKey, String status, int maxResults,
                          ActionListener<List<Map<String, Object>>> listener) {
        String project = projectKey != null ? projectKey : defaultProject;
        if (project == null) {
            listener.onFailure(new IllegalArgumentException("Project key required"));
            return;
        }
        
        StringBuilder jql = new StringBuilder("project = ").append(project);
        if (status != null && !status.isEmpty()) {
            jql.append(" AND status = '").append(status).append("'");
        }
        jql.append(" ORDER BY created DESC");
        
        searchIssues(jql.toString(), maxResults, listener);
    }
    
    /**
     * Get a specific issue.
     */
    public void getIssue(String issueKey,
                        ActionListener<Map<String, Object>> listener) {
        try {
            String path = "/rest/api/3/issue/" + issueKey;
            Map<String, Object> issue = makeRequest(path);
            listener.onResponse(issue);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Create an issue.
     */
    public void createIssue(String projectKey, String issueType, String summary, String description,
                           Map<String, Object> additionalFields,
                           ActionListener<Map<String, Object>> listener) {
        try {
            String project = projectKey != null ? projectKey : defaultProject;
            if (project == null) {
                listener.onFailure(new IllegalArgumentException("Project key required"));
                return;
            }
            
            Map<String, Object> fields = new HashMap<>();
            fields.put("project", Map.of("key", project));
            fields.put("issuetype", Map.of("name", issueType != null ? issueType : "Task"));
            fields.put("summary", summary);
            if (description != null) {
                // Jira Cloud uses ADF (Atlassian Document Format) for description
                fields.put("description", Map.of(
                    "type", "doc",
                    "version", 1,
                    "content", List.of(Map.of(
                        "type", "paragraph",
                        "content", List.of(Map.of(
                            "type", "text",
                            "text", description
                        ))
                    ))
                ));
            }
            if (additionalFields != null) {
                fields.putAll(additionalFields);
            }
            
            Map<String, Object> payload = Map.of("fields", fields);
            Map<String, Object> result = makePostRequest("/rest/api/3/issue", payload);
            listener.onResponse(result);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Transition an issue (change status).
     */
    public void transitionIssue(String issueKey, String transitionName,
                               ActionListener<Map<String, Object>> listener) {
        try {
            // First, get available transitions
            String path = "/rest/api/3/issue/" + issueKey + "/transitions";
            Map<String, Object> transitionsResponse = makeRequest(path);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> transitions = (List<Map<String, Object>>) transitionsResponse.get("transitions");
            
            // Find the matching transition
            String transitionId = null;
            for (Map<String, Object> transition : transitions) {
                if (transitionName.equalsIgnoreCase((String) transition.get("name"))) {
                    transitionId = String.valueOf(transition.get("id"));
                    break;
                }
            }
            
            if (transitionId == null) {
                listener.onFailure(new IllegalArgumentException("Transition not found: " + transitionName));
                return;
            }
            
            // Perform the transition
            Map<String, Object> payload = Map.of("transition", Map.of("id", transitionId));
            makePostRequest("/rest/api/3/issue/" + issueKey + "/transitions", payload);
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "transitioned");
            result.put("issue", issueKey);
            result.put("transition", transitionName);
            listener.onResponse(result);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Add a comment to an issue.
     */
    public void addComment(String issueKey, String commentBody,
                          ActionListener<Map<String, Object>> listener) {
        try {
            Map<String, Object> payload = Map.of(
                "body", Map.of(
                    "type", "doc",
                    "version", 1,
                    "content", List.of(Map.of(
                        "type", "paragraph",
                        "content", List.of(Map.of(
                            "type", "text",
                            "text", commentBody
                        ))
                    ))
                )
            );
            
            Map<String, Object> result = makePostRequest("/rest/api/3/issue/" + issueKey + "/comment", payload);
            listener.onResponse(result);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    // ==================== Projects ====================
    
    /**
     * List projects.
     */
    public void listProjects(ActionListener<List<Map<String, Object>>> listener) {
        try {
            String path = "/rest/api/3/project/search";
            Map<String, Object> response = makeRequest(path);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> projects = (List<Map<String, Object>>) response.get("values");
            listener.onResponse(projects != null ? projects : List.of());
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Get project details.
     */
    public void getProject(String projectKey,
                          ActionListener<Map<String, Object>> listener) {
        try {
            String project = projectKey != null ? projectKey : defaultProject;
            String path = "/rest/api/3/project/" + project;
            Map<String, Object> projectInfo = makeRequest(path);
            listener.onResponse(projectInfo);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    // ==================== Sprints ====================
    
    /**
     * List sprints for a board.
     */
    public void listSprints(int boardId, String state,
                           ActionListener<List<Map<String, Object>>> listener) {
        try {
            String path = "/rest/agile/1.0/board/" + boardId + "/sprint";
            if (state != null && !state.isEmpty()) {
                path += "?state=" + state;
            }
            
            Map<String, Object> response = makeRequest(path);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sprints = (List<Map<String, Object>>) response.get("values");
            listener.onResponse(sprints != null ? sprints : List.of());
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Get sprint issues.
     */
    public void getSprintIssues(int sprintId,
                               ActionListener<List<Map<String, Object>>> listener) {
        try {
            String path = "/rest/agile/1.0/sprint/" + sprintId + "/issue";
            Map<String, Object> response = makeRequest(path);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> issues = (List<Map<String, Object>>) response.get("issues");
            listener.onResponse(issues != null ? issues : List.of());
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    // ==================== Boards ====================
    
    /**
     * List boards.
     */
    public void listBoards(String projectKey,
                          ActionListener<List<Map<String, Object>>> listener) {
        try {
            String path = "/rest/agile/1.0/board";
            if (projectKey != null || defaultProject != null) {
                path += "?projectKeyOrId=" + (projectKey != null ? projectKey : defaultProject);
            }
            
            Map<String, Object> response = makeRequest(path);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> boards = (List<Map<String, Object>>) response.get("values");
            listener.onResponse(boards != null ? boards : List.of());
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    // ==================== HTTP Helpers ====================
    
    private Map<String, Object> makeRequest(String path) throws Exception {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", authHeader);
        conn.setRequestProperty("Accept", "application/json");
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("Jira API error: " + responseCode);
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
    
    private Map<String, Object> makePostRequest(String path, Map<String, Object> payload) throws Exception {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", authHeader);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        String jsonPayload = toJson(payload);
        conn.getOutputStream().write(jsonPayload.getBytes(StandardCharsets.UTF_8));
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200 && responseCode != 201 && responseCode != 204) {
            throw new RuntimeException("Jira API error: " + responseCode);
        }
        
        if (responseCode == 204) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return result;
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
    
    // Simple JSON parsing (in production, use a proper JSON library)
    private Map<String, Object> parseJson(String json) {
        Map<String, Object> result = new HashMap<>();
        result.put("_raw", json);
        
        // Basic extraction of common fields
        if (json.contains("\"displayName\"")) {
            result.put("displayName", extractJsonValue(json, "displayName"));
        }
        if (json.contains("\"emailAddress\"")) {
            result.put("emailAddress", extractJsonValue(json, "emailAddress"));
        }
        if (json.contains("\"key\"")) {
            result.put("key", extractJsonValue(json, "key"));
        }
        if (json.contains("\"id\"")) {
            result.put("id", extractJsonValue(json, "id"));
        }
        
        return result;
    }
    
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;
        
        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return null;
        
        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && 
               (json.charAt(valueStart) == ' ' || json.charAt(valueStart) == '"')) {
            valueStart++;
        }
        
        int valueEnd = valueStart;
        while (valueEnd < json.length() && 
               json.charAt(valueEnd) != '"' && 
               json.charAt(valueEnd) != ',' && 
               json.charAt(valueEnd) != '}') {
            valueEnd++;
        }
        
        return json.substring(valueStart, valueEnd);
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
                sb.append("\"").append(value).append("\"");
            } else if (value instanceof Number) {
                sb.append(value);
            } else if (value instanceof List) {
                sb.append("[");
                boolean listFirst = true;
                for (Object item : (List<?>) value) {
                    if (!listFirst) sb.append(",");
                    listFirst = false;
                    if (item instanceof String) {
                        sb.append("\"").append(item).append("\"");
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
}
