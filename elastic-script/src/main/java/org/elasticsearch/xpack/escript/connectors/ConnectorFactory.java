/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.connectors;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.escript.connectors.datadog.DatadogConnector;
import org.elasticsearch.xpack.escript.connectors.github.GitHubConnector;
import org.elasticsearch.xpack.escript.connectors.jira.JiraConnector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory for creating and managing connector instances.
 * 
 * Supported connector types:
 * - github: GitHub API integration
 * - jira: Atlassian Jira integration (planned)
 * - datadog: Datadog metrics/monitoring (planned)
 * - slack: Slack messaging (planned)
 * - pagerduty: PagerDuty incident management (planned)
 */
public class ConnectorFactory {
    
    /**
     * Create a connector instance based on type.
     */
    public static Object createConnector(String type, Map<String, Object> config) {
        return switch (type.toLowerCase()) {
            case "github" -> new GitHubConnector(config);
            case "jira" -> new JiraConnector(config);
            case "datadog" -> new DatadogConnector(config);
            // Add more connector types as they are implemented
            // case "slack" -> new SlackConnector(config);
            // case "pagerduty" -> new PagerDutyConnector(config);
            default -> throw new IllegalArgumentException("Unknown connector type: " + type);
        };
    }
    
    /**
     * Execute an action on a connector.
     */
    public static void executeAction(String type, String action, Map<String, Object> config,
                                     Map<String, Object> args, List<Object> positionalArgs,
                                     ActionListener<Object> listener) {
        try {
            Object connector = createConnector(type, config);
            
            if (connector instanceof GitHubConnector github) {
                executeGitHubAction(github, action, args, listener);
            } else if (connector instanceof JiraConnector jira) {
                executeJiraAction(jira, action, args, listener);
            } else if (connector instanceof DatadogConnector datadog) {
                executeDatadogAction(datadog, action, args, listener);
            } else {
                listener.onFailure(new IllegalArgumentException("Connector type not supported: " + type));
            }
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Query data from a connector entity.
     */
    public static void queryEntity(String type, String entity, Map<String, Object> config,
                                   String whereCondition, Integer limit,
                                   ActionListener<List<Map<String, Object>>> listener) {
        try {
            Object connector = createConnector(type, config);
            
            if (connector instanceof GitHubConnector github) {
                queryGitHubEntity(github, entity, whereCondition, limit, listener);
            } else if (connector instanceof JiraConnector jira) {
                queryJiraEntity(jira, entity, whereCondition, limit, listener);
            } else if (connector instanceof DatadogConnector datadog) {
                queryDatadogEntity(datadog, entity, whereCondition, limit, listener);
            } else {
                listener.onFailure(new IllegalArgumentException("Connector type not supported: " + type));
            }
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Fetch data from a connector for sync operations.
     */
    public static void fetchForSync(String type, String entity, Map<String, Object> config,
                                    String incrementalField, Object lastValue,
                                    ActionListener<List<Map<String, Object>>> listener) {
        try {
            Object connector = createConnector(type, config);
            
            if (connector instanceof GitHubConnector github) {
                fetchGitHubForSync(github, entity, incrementalField, lastValue, listener);
            } else {
                // Return empty list for unsupported connectors (framework placeholder)
                listener.onResponse(List.of());
            }
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Test a connector connection.
     */
    public static void testConnection(String type, Map<String, Object> config,
                                      ActionListener<Map<String, Object>> listener) {
        try {
            Object connector = createConnector(type, config);
            
            if (connector instanceof GitHubConnector github) {
                github.testConnection(listener);
            } else if (connector instanceof JiraConnector jira) {
                jira.testConnection(listener);
            } else if (connector instanceof DatadogConnector datadog) {
                datadog.testConnection(listener);
            } else {
                Map<String, Object> result = new HashMap<>();
                result.put("status", "unknown");
                result.put("message", "Connection testing not implemented for type: " + type);
                listener.onResponse(result);
            }
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    // ==================== GitHub-specific handlers ====================
    
    private static void executeGitHubAction(GitHubConnector github, String action,
                                            Map<String, Object> args,
                                            ActionListener<Object> listener) {
        switch (action.toLowerCase()) {
            case "list_issues" -> {
                String repo = (String) args.get("repo");
                String owner = (String) args.get("owner");
                Map<String, Object> filters = new HashMap<>();
                if (args.containsKey("state")) filters.put("state", args.get("state"));
                if (args.containsKey("labels")) filters.put("labels", args.get("labels"));
                github.listIssues(owner, repo, filters, ActionListener.wrap(
                    issues -> listener.onResponse(issues),
                    listener::onFailure
                ));
            }
            case "get_issue" -> {
                String repo = (String) args.get("repo");
                String owner = (String) args.get("owner");
                int issueNumber = ((Number) args.get("issue_number")).intValue();
                github.getIssue(owner, repo, issueNumber, ActionListener.wrap(
                    issue -> listener.onResponse(issue),
                    listener::onFailure
                ));
            }
            case "create_issue" -> {
                String repo = (String) args.get("repo");
                String owner = (String) args.get("owner");
                String title = (String) args.get("title");
                String body = (String) args.get("body");
                @SuppressWarnings("unchecked")
                List<String> labels = (List<String>) args.get("labels");
                @SuppressWarnings("unchecked")
                List<String> assignees = (List<String>) args.get("assignees");
                github.createIssue(owner, repo, title, body, labels, assignees, ActionListener.wrap(
                    issue -> listener.onResponse(issue),
                    listener::onFailure
                ));
            }
            case "list_pull_requests", "list_prs" -> {
                String repo = (String) args.get("repo");
                String owner = (String) args.get("owner");
                String state = (String) args.get("state");
                github.listPullRequests(owner, repo, state, ActionListener.wrap(
                    prs -> listener.onResponse(prs),
                    listener::onFailure
                ));
            }
            case "list_repositories", "list_repos" -> {
                github.listRepositories(ActionListener.wrap(
                    repos -> listener.onResponse(repos),
                    listener::onFailure
                ));
            }
            case "list_workflow_runs" -> {
                String repo = (String) args.get("repo");
                String owner = (String) args.get("owner");
                String status = (String) args.get("status");
                github.listWorkflowRuns(owner, repo, status, ActionListener.wrap(
                    runs -> listener.onResponse(runs),
                    listener::onFailure
                ));
            }
            case "trigger_workflow" -> {
                String repo = (String) args.get("repo");
                String owner = (String) args.get("owner");
                String workflowId = (String) args.get("workflow_id");
                String ref = (String) args.get("ref");
                @SuppressWarnings("unchecked")
                Map<String, Object> inputs = (Map<String, Object>) args.get("inputs");
                github.triggerWorkflow(owner, repo, workflowId, ref, inputs, ActionListener.wrap(
                    result -> listener.onResponse(result),
                    listener::onFailure
                ));
            }
            case "list_commits" -> {
                String repo = (String) args.get("repo");
                String owner = (String) args.get("owner");
                String sha = (String) args.get("sha");
                String path = (String) args.get("path");
                github.listCommits(owner, repo, sha, path, ActionListener.wrap(
                    commits -> listener.onResponse(commits),
                    listener::onFailure
                ));
            }
            default -> listener.onFailure(new IllegalArgumentException("Unknown GitHub action: " + action));
        }
    }
    
    private static void queryGitHubEntity(GitHubConnector github, String entity,
                                          String whereCondition, Integer limit,
                                          ActionListener<List<Map<String, Object>>> listener) {
        // Parse where condition to extract filters
        Map<String, Object> filters = parseWhereCondition(whereCondition);
        if (limit != null) {
            filters.put("per_page", limit);
        }
        
        switch (entity.toLowerCase()) {
            case "issues" -> github.listIssues(null, null, filters, listener);
            case "pull_requests", "prs" -> github.listPullRequests(null, null, 
                (String) filters.get("state"), listener);
            case "repositories", "repos" -> github.listRepositories(ActionListener.wrap(
                repos -> listener.onResponse(repos),
                listener::onFailure
            ));
            default -> listener.onFailure(new IllegalArgumentException("Unknown GitHub entity: " + entity));
        }
    }
    
    private static void fetchGitHubForSync(GitHubConnector github, String entity,
                                           String incrementalField, Object lastValue,
                                           ActionListener<List<Map<String, Object>>> listener) {
        Map<String, Object> filters = new HashMap<>();
        
        // Add since filter for incremental sync
        if (incrementalField != null && lastValue != null) {
            filters.put("since", lastValue.toString());
        }
        
        switch (entity.toLowerCase()) {
            case "issues" -> github.listIssues(null, null, filters, listener);
            case "pull_requests", "prs" -> github.listPullRequests(null, null, "all", listener);
            default -> listener.onResponse(List.of());
        }
    }
    
    // ==================== Jira-specific handlers ====================
    
    private static void executeJiraAction(JiraConnector jira, String action,
                                          Map<String, Object> args,
                                          ActionListener<Object> listener) {
        switch (action.toLowerCase()) {
            case "list_issues", "search_issues" -> {
                String jql = (String) args.get("jql");
                if (jql != null) {
                    int maxResults = args.containsKey("max_results") ? 
                        ((Number) args.get("max_results")).intValue() : 50;
                    jira.searchIssues(jql, maxResults, ActionListener.wrap(
                        issues -> listener.onResponse(issues),
                        listener::onFailure
                    ));
                } else {
                    String project = (String) args.get("project");
                    String status = (String) args.get("status");
                    int maxResults = args.containsKey("max_results") ? 
                        ((Number) args.get("max_results")).intValue() : 50;
                    jira.listIssues(project, status, maxResults, ActionListener.wrap(
                        issues -> listener.onResponse(issues),
                        listener::onFailure
                    ));
                }
            }
            case "get_issue" -> {
                String issueKey = (String) args.get("issue_key");
                jira.getIssue(issueKey, ActionListener.wrap(
                    issue -> listener.onResponse(issue),
                    listener::onFailure
                ));
            }
            case "create_issue" -> {
                String project = (String) args.get("project");
                String issueType = (String) args.get("issue_type");
                String summary = (String) args.get("summary");
                String description = (String) args.get("description");
                @SuppressWarnings("unchecked")
                Map<String, Object> additionalFields = (Map<String, Object>) args.get("fields");
                jira.createIssue(project, issueType, summary, description, additionalFields,
                    ActionListener.wrap(
                        issue -> listener.onResponse(issue),
                        listener::onFailure
                    ));
            }
            case "transition_issue" -> {
                String issueKey = (String) args.get("issue_key");
                String transition = (String) args.get("transition");
                jira.transitionIssue(issueKey, transition, ActionListener.wrap(
                    result -> listener.onResponse(result),
                    listener::onFailure
                ));
            }
            case "add_comment" -> {
                String issueKey = (String) args.get("issue_key");
                String comment = (String) args.get("comment");
                jira.addComment(issueKey, comment, ActionListener.wrap(
                    result -> listener.onResponse(result),
                    listener::onFailure
                ));
            }
            case "list_projects" -> {
                jira.listProjects(ActionListener.wrap(
                    projects -> listener.onResponse(projects),
                    listener::onFailure
                ));
            }
            case "get_project" -> {
                String projectKey = (String) args.get("project");
                jira.getProject(projectKey, ActionListener.wrap(
                    project -> listener.onResponse(project),
                    listener::onFailure
                ));
            }
            case "list_sprints" -> {
                int boardId = ((Number) args.get("board_id")).intValue();
                String state = (String) args.get("state");
                jira.listSprints(boardId, state, ActionListener.wrap(
                    sprints -> listener.onResponse(sprints),
                    listener::onFailure
                ));
            }
            case "list_boards" -> {
                String project = (String) args.get("project");
                jira.listBoards(project, ActionListener.wrap(
                    boards -> listener.onResponse(boards),
                    listener::onFailure
                ));
            }
            default -> listener.onFailure(new IllegalArgumentException("Unknown Jira action: " + action));
        }
    }
    
    private static void queryJiraEntity(JiraConnector jira, String entity,
                                        String whereCondition, Integer limit,
                                        ActionListener<List<Map<String, Object>>> listener) {
        int maxResults = limit != null ? limit : 50;
        
        switch (entity.toLowerCase()) {
            case "issues" -> {
                // Convert WHERE condition to JQL
                String jql = convertToJql(whereCondition);
                jira.searchIssues(jql, maxResults, listener);
            }
            case "projects" -> jira.listProjects(listener);
            default -> listener.onFailure(new IllegalArgumentException("Unknown Jira entity: " + entity));
        }
    }
    
    /**
     * Convert a simple WHERE condition to JQL.
     */
    private static String convertToJql(String whereCondition) {
        if (whereCondition == null || whereCondition.isEmpty()) {
            return "ORDER BY created DESC";
        }
        
        // Simple conversion - replace = with =
        // whereCondition like: project = 'PROJ' AND status = 'Open'
        String jql = whereCondition
            .replaceAll("'", "\"")  // JQL uses double quotes
            .replaceAll("(?i)\\bAND\\b", "AND")
            .replaceAll("(?i)\\bOR\\b", "OR");
        
        return jql + " ORDER BY created DESC";
    }
    
    // ==================== Datadog-specific handlers ====================
    
    private static void executeDatadogAction(DatadogConnector datadog, String action,
                                             Map<String, Object> args,
                                             ActionListener<Object> listener) {
        // Default time range: last hour
        long now = System.currentTimeMillis() / 1000;
        long oneHourAgo = now - 3600;
        
        switch (action.toLowerCase()) {
            case "query_metrics" -> {
                String query = (String) args.get("query");
                long from = args.containsKey("from") ? ((Number) args.get("from")).longValue() : oneHourAgo;
                long to = args.containsKey("to") ? ((Number) args.get("to")).longValue() : now;
                datadog.queryMetrics(query, from, to, ActionListener.wrap(
                    result -> listener.onResponse(result),
                    listener::onFailure
                ));
            }
            case "list_monitors" -> {
                String groupStates = (String) args.get("group_states");
                String name = (String) args.get("name");
                String tags = (String) args.get("tags");
                datadog.listMonitors(groupStates, name, tags, ActionListener.wrap(
                    monitors -> listener.onResponse(monitors),
                    listener::onFailure
                ));
            }
            case "get_monitor" -> {
                long monitorId = ((Number) args.get("monitor_id")).longValue();
                datadog.getMonitor(monitorId, ActionListener.wrap(
                    monitor -> listener.onResponse(monitor),
                    listener::onFailure
                ));
            }
            case "mute_monitor" -> {
                long monitorId = ((Number) args.get("monitor_id")).longValue();
                String scope = (String) args.get("scope");
                long end = args.containsKey("end") ? ((Number) args.get("end")).longValue() : 0;
                datadog.muteMonitor(monitorId, scope, end, ActionListener.wrap(
                    result -> listener.onResponse(result),
                    listener::onFailure
                ));
            }
            case "unmute_monitor" -> {
                long monitorId = ((Number) args.get("monitor_id")).longValue();
                String scope = (String) args.get("scope");
                datadog.unmuteMonitor(monitorId, scope, ActionListener.wrap(
                    result -> listener.onResponse(result),
                    listener::onFailure
                ));
            }
            case "list_events" -> {
                long start = args.containsKey("start") ? ((Number) args.get("start")).longValue() : oneHourAgo;
                long end = args.containsKey("end") ? ((Number) args.get("end")).longValue() : now;
                String priority = (String) args.get("priority");
                String sources = (String) args.get("sources");
                String tags = (String) args.get("tags");
                datadog.listEvents(start, end, priority, sources, tags, ActionListener.wrap(
                    events -> listener.onResponse(events),
                    listener::onFailure
                ));
            }
            case "post_event" -> {
                String title = (String) args.get("title");
                String text = (String) args.get("text");
                String priority = (String) args.get("priority");
                String host = (String) args.get("host");
                @SuppressWarnings("unchecked")
                List<String> tags = (List<String>) args.get("tags");
                String alertType = (String) args.get("alert_type");
                datadog.postEvent(title, text, priority, host, tags, alertType, ActionListener.wrap(
                    result -> listener.onResponse(result),
                    listener::onFailure
                ));
            }
            case "query_logs" -> {
                String query = (String) args.get("query");
                long from = args.containsKey("from") ? ((Number) args.get("from")).longValue() : oneHourAgo;
                long to = args.containsKey("to") ? ((Number) args.get("to")).longValue() : now;
                int limit = args.containsKey("limit") ? ((Number) args.get("limit")).intValue() : 50;
                datadog.queryLogs(query, from, to, limit, ActionListener.wrap(
                    logs -> listener.onResponse(logs),
                    listener::onFailure
                ));
            }
            case "list_dashboards" -> {
                datadog.listDashboards(ActionListener.wrap(
                    dashboards -> listener.onResponse(dashboards),
                    listener::onFailure
                ));
            }
            case "list_hosts" -> {
                String filter = (String) args.get("filter");
                String sortField = (String) args.get("sort_field");
                String sortDir = (String) args.get("sort_dir");
                int count = args.containsKey("count") ? ((Number) args.get("count")).intValue() : 100;
                datadog.listHosts(filter, sortField, sortDir, count, ActionListener.wrap(
                    hosts -> listener.onResponse(hosts),
                    listener::onFailure
                ));
            }
            case "mute_host" -> {
                String hostName = (String) args.get("host");
                String message = (String) args.get("message");
                long end = args.containsKey("end") ? ((Number) args.get("end")).longValue() : 0;
                boolean override = args.containsKey("override") ? (Boolean) args.get("override") : false;
                datadog.muteHost(hostName, message, end, override, ActionListener.wrap(
                    result -> listener.onResponse(result),
                    listener::onFailure
                ));
            }
            case "unmute_host" -> {
                String hostName = (String) args.get("host");
                datadog.unmuteHost(hostName, ActionListener.wrap(
                    result -> listener.onResponse(result),
                    listener::onFailure
                ));
            }
            default -> listener.onFailure(new IllegalArgumentException("Unknown Datadog action: " + action));
        }
    }
    
    private static void queryDatadogEntity(DatadogConnector datadog, String entity,
                                           String whereCondition, Integer limit,
                                           ActionListener<List<Map<String, Object>>> listener) {
        long now = System.currentTimeMillis() / 1000;
        long oneHourAgo = now - 3600;
        
        switch (entity.toLowerCase()) {
            case "monitors" -> datadog.listMonitors(null, null, null, listener);
            case "events" -> datadog.listEvents(oneHourAgo, now, null, null, null,
                ActionListener.wrap(
                    result -> {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> events = (List<Map<String, Object>>) result.get("events");
                        listener.onResponse(events != null ? events : List.of());
                    },
                    listener::onFailure
                ));
            default -> listener.onFailure(new IllegalArgumentException("Unknown Datadog entity: " + entity));
        }
    }
    
    /**
     * Parse a simple WHERE condition into a filter map.
     * Supports: field = 'value' AND field2 = 'value2'
     */
    private static Map<String, Object> parseWhereCondition(String whereCondition) {
        Map<String, Object> filters = new HashMap<>();
        if (whereCondition == null || whereCondition.isEmpty()) {
            return filters;
        }
        
        // Simple parsing for conditions like: state = 'open' AND labels = 'bug'
        String[] parts = whereCondition.split("(?i)\\s+AND\\s+");
        for (String part : parts) {
            String[] keyValue = part.split("\\s*=\\s*");
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim().replaceAll("^['\"]|['\"]$", "");
                filters.put(key, value);
            }
        }
        
        return filters;
    }
    
    /**
     * Get list of available connector types.
     */
    public static List<String> getAvailableTypes() {
        return List.of(
            "github",
            "jira",      // planned
            "datadog",   // planned
            "slack",     // planned
            "pagerduty", // planned
            "salesforce", // planned
            "zendesk"    // planned
        );
    }
    
    /**
     * Get entities available for a connector type.
     */
    public static List<String> getAvailableEntities(String type) {
        return switch (type.toLowerCase()) {
            case "github" -> List.of("issues", "pull_requests", "repositories", "commits", "workflow_runs");
            case "jira" -> List.of("issues", "projects", "sprints", "boards");
            case "datadog" -> List.of("metrics", "logs", "events", "monitors");
            case "slack" -> List.of("channels", "messages", "users");
            case "pagerduty" -> List.of("incidents", "services", "escalation_policies");
            default -> List.of();
        };
    }
    
    /**
     * Get actions available for a connector type.
     */
    public static List<String> getAvailableActions(String type) {
        return switch (type.toLowerCase()) {
            case "github" -> List.of(
                "list_issues", "get_issue", "create_issue",
                "list_pull_requests", "get_pull_request",
                "list_repositories", "get_repository",
                "list_workflow_runs", "trigger_workflow",
                "list_commits"
            );
            case "jira" -> List.of(
                "list_issues", "get_issue", "create_issue", "update_issue",
                "list_projects", "list_sprints"
            );
            case "datadog" -> List.of(
                "query_metrics", "list_monitors", "mute_monitor", "unmute_monitor"
            );
            default -> List.of();
        };
    }
}
