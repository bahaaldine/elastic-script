/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.connectors.github;

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
import java.util.concurrent.CompletableFuture;

/**
 * GitHub Connector for Moltler.
 * 
 * Provides integration with GitHub API for:
 * - Issues: List, create, update issues
 * - Pull Requests: List PRs, check status
 * - Repositories: List repos, get details
 * - Actions: List workflows, trigger workflows
 * - Commits: List commits, compare branches
 * 
 * Configuration:
 * - token: GitHub Personal Access Token or OAuth token
 * - org: Organization name (optional)
 * - repo: Repository name (optional)
 * - base_url: API base URL (default: https://api.github.com)
 */
public class GitHubConnector {
    
    private static final String DEFAULT_BASE_URL = "https://api.github.com";
    
    private final String token;
    private final String org;
    private final String repo;
    private final String baseUrl;
    
    public GitHubConnector(Map<String, Object> config) {
        this.token = (String) config.get("token");
        this.org = (String) config.get("org");
        this.repo = (String) config.get("repo");
        this.baseUrl = config.containsKey("base_url") ? 
            (String) config.get("base_url") : DEFAULT_BASE_URL;
        
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("GitHub connector requires 'token' in config");
        }
    }
    
    /**
     * Test the connection to GitHub.
     */
    public void testConnection(ActionListener<Map<String, Object>> listener) {
        try {
            // Get authenticated user to verify token
            Map<String, Object> result = makeRequest("/user");
            Map<String, Object> response = new HashMap<>();
            response.put("status", "connected");
            response.put("user", result.get("login"));
            response.put("rate_limit", getRateLimit());
            listener.onResponse(response);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Get rate limit information.
     */
    public Map<String, Object> getRateLimit() {
        try {
            return makeRequest("/rate_limit");
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }
    
    // ==================== Issues ====================
    
    /**
     * List issues for a repository.
     */
    public void listIssues(String repoOwner, String repoName, Map<String, Object> filters,
                          ActionListener<List<Map<String, Object>>> listener) {
        try {
            String owner = repoOwner != null ? repoOwner : org;
            String repository = repoName != null ? repoName : repo;
            
            if (owner == null || repository == null) {
                listener.onFailure(new IllegalArgumentException("Repository owner and name required"));
                return;
            }
            
            StringBuilder path = new StringBuilder("/repos/").append(owner).append("/").append(repository).append("/issues");
            
            // Add query parameters
            List<String> params = new ArrayList<>();
            if (filters != null) {
                if (filters.containsKey("state")) {
                    params.add("state=" + filters.get("state"));
                }
                if (filters.containsKey("labels")) {
                    params.add("labels=" + filters.get("labels"));
                }
                if (filters.containsKey("assignee")) {
                    params.add("assignee=" + filters.get("assignee"));
                }
                if (filters.containsKey("since")) {
                    params.add("since=" + filters.get("since"));
                }
                if (filters.containsKey("per_page")) {
                    params.add("per_page=" + filters.get("per_page"));
                }
            }
            if (!params.isEmpty()) {
                path.append("?").append(String.join("&", params));
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> issues = (List<Map<String, Object>>) makeRequest(path.toString());
            listener.onResponse(issues);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Get a specific issue.
     */
    public void getIssue(String repoOwner, String repoName, int issueNumber,
                        ActionListener<Map<String, Object>> listener) {
        try {
            String owner = repoOwner != null ? repoOwner : org;
            String repository = repoName != null ? repoName : repo;
            
            String path = "/repos/" + owner + "/" + repository + "/issues/" + issueNumber;
            Map<String, Object> issue = makeRequest(path);
            listener.onResponse(issue);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Create an issue.
     */
    public void createIssue(String repoOwner, String repoName, String title, String body,
                           List<String> labels, List<String> assignees,
                           ActionListener<Map<String, Object>> listener) {
        try {
            String owner = repoOwner != null ? repoOwner : org;
            String repository = repoName != null ? repoName : repo;
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("title", title);
            if (body != null) payload.put("body", body);
            if (labels != null && !labels.isEmpty()) payload.put("labels", labels);
            if (assignees != null && !assignees.isEmpty()) payload.put("assignees", assignees);
            
            String path = "/repos/" + owner + "/" + repository + "/issues";
            Map<String, Object> issue = makePostRequest(path, payload);
            listener.onResponse(issue);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    // ==================== Pull Requests ====================
    
    /**
     * List pull requests for a repository.
     */
    public void listPullRequests(String repoOwner, String repoName, String state,
                                 ActionListener<List<Map<String, Object>>> listener) {
        try {
            String owner = repoOwner != null ? repoOwner : org;
            String repository = repoName != null ? repoName : repo;
            
            String path = "/repos/" + owner + "/" + repository + "/pulls";
            if (state != null) {
                path += "?state=" + state;
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> prs = (List<Map<String, Object>>) makeRequest(path);
            listener.onResponse(prs);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Get a specific pull request.
     */
    public void getPullRequest(String repoOwner, String repoName, int prNumber,
                              ActionListener<Map<String, Object>> listener) {
        try {
            String owner = repoOwner != null ? repoOwner : org;
            String repository = repoName != null ? repoName : repo;
            
            String path = "/repos/" + owner + "/" + repository + "/pulls/" + prNumber;
            Map<String, Object> pr = makeRequest(path);
            listener.onResponse(pr);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    // ==================== Repositories ====================
    
    /**
     * List repositories for the configured organization.
     */
    public void listRepositories(ActionListener<List<Map<String, Object>>> listener) {
        try {
            String path;
            if (org != null) {
                path = "/orgs/" + org + "/repos";
            } else {
                path = "/user/repos";
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> repos = (List<Map<String, Object>>) makeRequest(path);
            listener.onResponse(repos);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Get repository details.
     */
    public void getRepository(String repoOwner, String repoName,
                             ActionListener<Map<String, Object>> listener) {
        try {
            String owner = repoOwner != null ? repoOwner : org;
            String repository = repoName != null ? repoName : repo;
            
            String path = "/repos/" + owner + "/" + repository;
            Map<String, Object> repoInfo = makeRequest(path);
            listener.onResponse(repoInfo);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    // ==================== Actions/Workflows ====================
    
    /**
     * List workflow runs for a repository.
     */
    public void listWorkflowRuns(String repoOwner, String repoName, String status,
                                ActionListener<Map<String, Object>> listener) {
        try {
            String owner = repoOwner != null ? repoOwner : org;
            String repository = repoName != null ? repoName : repo;
            
            String path = "/repos/" + owner + "/" + repository + "/actions/runs";
            if (status != null) {
                path += "?status=" + status;
            }
            
            Map<String, Object> runs = makeRequest(path);
            listener.onResponse(runs);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Trigger a workflow.
     */
    public void triggerWorkflow(String repoOwner, String repoName, String workflowId,
                               String ref, Map<String, Object> inputs,
                               ActionListener<Map<String, Object>> listener) {
        try {
            String owner = repoOwner != null ? repoOwner : org;
            String repository = repoName != null ? repoName : repo;
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("ref", ref != null ? ref : "main");
            if (inputs != null && !inputs.isEmpty()) {
                payload.put("inputs", inputs);
            }
            
            String path = "/repos/" + owner + "/" + repository + "/actions/workflows/" + workflowId + "/dispatches";
            makePostRequest(path, payload);
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "triggered");
            result.put("workflow", workflowId);
            result.put("ref", ref);
            listener.onResponse(result);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    // ==================== Commits ====================
    
    /**
     * List commits for a repository.
     */
    public void listCommits(String repoOwner, String repoName, String sha, String path,
                           ActionListener<List<Map<String, Object>>> listener) {
        try {
            String owner = repoOwner != null ? repoOwner : org;
            String repository = repoName != null ? repoName : repo;
            
            StringBuilder apiPath = new StringBuilder("/repos/").append(owner).append("/").append(repository).append("/commits");
            
            List<String> params = new ArrayList<>();
            if (sha != null) params.add("sha=" + sha);
            if (path != null) params.add("path=" + path);
            if (!params.isEmpty()) {
                apiPath.append("?").append(String.join("&", params));
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> commits = (List<Map<String, Object>>) makeRequest(apiPath.toString());
            listener.onResponse(commits);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    // ==================== HTTP Helpers ====================
    
    private Map<String, Object> makeRequest(String path) throws Exception {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("GitHub API error: " + responseCode);
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
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        conn.setDoOutput(true);
        
        String jsonPayload = toJson(payload);
        conn.getOutputStream().write(jsonPayload.getBytes(StandardCharsets.UTF_8));
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200 && responseCode != 201 && responseCode != 204) {
            throw new RuntimeException("GitHub API error: " + responseCode);
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
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        // This is a simplified JSON parser - in real code, use XContentFactory
        // For now, return a placeholder
        Map<String, Object> result = new HashMap<>();
        result.put("_raw", json);
        
        // Basic extraction of common fields
        if (json.contains("\"login\"")) {
            result.put("login", extractJsonValue(json, "login"));
        }
        if (json.contains("\"id\"")) {
            result.put("id", extractJsonValue(json, "id"));
        }
        if (json.contains("\"name\"")) {
            result.put("name", extractJsonValue(json, "name"));
        }
        
        return result;
    }
    
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;
        
        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return null;
        
        // Skip whitespace and quotes
        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && 
               (json.charAt(valueStart) == ' ' || json.charAt(valueStart) == '"')) {
            valueStart++;
        }
        
        // Find end of value
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
            } else if (value instanceof List) {
                sb.append("[");
                boolean listFirst = true;
                for (Object item : (List<?>) value) {
                    if (!listFirst) sb.append(",");
                    listFirst = false;
                    if (item instanceof String) {
                        sb.append("\"").append(item).append("\"");
                    } else {
                        sb.append(item);
                    }
                }
                sb.append("]");
            } else if (value instanceof Map) {
                sb.append(toJson((Map<String, Object>) value));
            } else {
                sb.append(value);
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
