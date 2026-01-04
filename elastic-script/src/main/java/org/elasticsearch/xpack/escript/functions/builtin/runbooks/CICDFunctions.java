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
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CI/CD integration functions for Jenkins, GitHub Actions, GitLab CI, and ArgoCD.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.RUNBOOK,
    description = "CI/CD integration functions for Jenkins, GitHub Actions, GitLab CI, and ArgoCD."
)
public class CICDFunctions {

    private static final Logger LOGGER = LogManager.getLogger(CICDFunctions.class);

    public static void registerAll(ExecutionContext context) {
        LOGGER.info("Registering CI/CD built-in functions");
        registerJenkinsBuild(context);
        registerJenkinsStatus(context);
        registerGitHubWorkflow(context);
        registerGitHubWorkflowStatus(context);
        registerGitLabPipeline(context);
        registerGitLabPipelineStatus(context);
        registerArgoCDSync(context);
        registerArgoCDGetApp(context);
    }

    // ==================== JENKINS ====================

    public static void registerJenkinsBuild(ExecutionContext context) {
        context.declareFunction("JENKINS_BUILD",
            List.of(new Parameter("job", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("JENKINS_BUILD", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String job = args.get(0).toString();
                    String jenkinsUrl = System.getenv("JENKINS_URL");
                    String user = System.getenv("JENKINS_USER");
                    String token = System.getenv("JENKINS_TOKEN");
                    
                    if (jenkinsUrl == null || jenkinsUrl.isEmpty()) {
                        listener.onFailure(new RuntimeException("JENKINS_URL not configured"));
                        return;
                    }
                    if (user == null || user.isEmpty() || token == null || token.isEmpty()) {
                        listener.onFailure(new RuntimeException("JENKINS_USER and JENKINS_TOKEN must be configured"));
                        return;
                    }
                    
                    String jobPath = job.replace("/", "/job/");
                    String auth = Base64.getEncoder().encodeToString((user + ":" + token).getBytes(StandardCharsets.UTF_8));
                    
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Basic " + auth);
                    
                    HttpClient client = HttpClientHolder.get();
                    client.post(jenkinsUrl + "/job/" + jobPath + "/build", headers, null);
                    listener.onResponse(jenkinsUrl + "/job/" + jobPath);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("JENKINS_BUILD failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerJenkinsStatus(ExecutionContext context) {
        context.declareFunction("JENKINS_STATUS",
            List.of(new Parameter("job", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("JENKINS_STATUS", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String job = args.get(0).toString();
                    String jenkinsUrl = System.getenv("JENKINS_URL");
                    String user = System.getenv("JENKINS_USER");
                    String token = System.getenv("JENKINS_TOKEN");
                    
                    if (jenkinsUrl == null || jenkinsUrl.isEmpty()) {
                        listener.onFailure(new RuntimeException("JENKINS_URL not configured"));
                        return;
                    }
                    
                    String jobPath = job.replace("/", "/job/");
                    String auth = Base64.getEncoder().encodeToString((user + ":" + token).getBytes(StandardCharsets.UTF_8));
                    
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Basic " + auth);
                    
                    HttpClient client = HttpClientHolder.get();
                    String response = client.get(jenkinsUrl + "/job/" + jobPath + "/lastBuild/api/json", headers);
                    listener.onResponse(parseJsonToMap(response));
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("JENKINS_STATUS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    // ==================== GITHUB ACTIONS ====================

    public static void registerGitHubWorkflow(ExecutionContext context) {
        context.declareFunction("GITHUB_WORKFLOW",
            List.of(
                new Parameter("repo", "STRING", ParameterMode.IN),
                new Parameter("workflow", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("GITHUB_WORKFLOW", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String repo = args.get(0).toString();
                    String workflow = args.get(1).toString();
                    String ref = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : "main";
                    String inputsJson = args.size() > 3 && args.get(3) != null ? args.get(3).toString() : null;
                    String token = System.getenv("GITHUB_TOKEN");
                    
                    if (token == null || token.isEmpty()) {
                        listener.onFailure(new RuntimeException("GITHUB_TOKEN not configured"));
                        return;
                    }
                    
                    XContentBuilder builder = XContentFactory.jsonBuilder();
                    builder.startObject().field("ref", ref);
                    if (inputsJson != null && !inputsJson.isEmpty()) {
                        builder.field("inputs", parseJsonToMap(inputsJson));
                    }
                    builder.endObject();
                    
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + token);
                    headers.put("Accept", "application/vnd.github+json");
                    headers.put("Content-Type", "application/json");
                    
                    HttpClient client = HttpClientHolder.get();
                    client.post("https://api.github.com/repos/" + repo + "/actions/workflows/" + workflow + "/dispatches", 
                        headers, Strings.toString(builder));
                    listener.onResponse(true);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("GITHUB_WORKFLOW failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerGitHubWorkflowStatus(ExecutionContext context) {
        context.declareFunction("GITHUB_WORKFLOW_STATUS",
            List.of(
                new Parameter("repo", "STRING", ParameterMode.IN),
                new Parameter("run_id", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("GITHUB_WORKFLOW_STATUS", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String repo = args.get(0).toString();
                    long runId = ((Number) args.get(1)).longValue();
                    String token = System.getenv("GITHUB_TOKEN");
                    
                    if (token == null || token.isEmpty()) {
                        listener.onFailure(new RuntimeException("GITHUB_TOKEN not configured"));
                        return;
                    }
                    
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + token);
                    headers.put("Accept", "application/vnd.github+json");
                    
                    HttpClient client = HttpClientHolder.get();
                    String response = client.get("https://api.github.com/repos/" + repo + "/actions/runs/" + runId, headers);
                    listener.onResponse(parseJsonToMap(response));
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("GITHUB_WORKFLOW_STATUS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    // ==================== GITLAB CI ====================

    public static void registerGitLabPipeline(ExecutionContext context) {
        context.declareFunction("GITLAB_PIPELINE",
            List.of(
                new Parameter("project", "STRING", ParameterMode.IN),
                new Parameter("ref", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("GITLAB_PIPELINE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String project = args.get(0).toString();
                    String ref = args.get(1).toString();
                    String gitlabUrl = System.getenv("GITLAB_URL") != null ? System.getenv("GITLAB_URL") : "https://gitlab.com";
                    String token = System.getenv("GITLAB_TOKEN");
                    
                    if (token == null || token.isEmpty()) {
                        listener.onFailure(new RuntimeException("GITLAB_TOKEN not configured"));
                        return;
                    }
                    
                    String encodedProject = java.net.URLEncoder.encode(project, "UTF-8");
                    XContentBuilder builder = XContentFactory.jsonBuilder();
                    builder.startObject().field("ref", ref).endObject();
                    
                    Map<String, String> headers = new HashMap<>();
                    headers.put("PRIVATE-TOKEN", token);
                    headers.put("Content-Type", "application/json");
                    
                    HttpClient client = HttpClientHolder.get();
                    String response = client.post(gitlabUrl + "/api/v4/projects/" + encodedProject + "/pipeline", 
                        headers, Strings.toString(builder));
                    listener.onResponse(parseJsonToMap(response));
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("GITLAB_PIPELINE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerGitLabPipelineStatus(ExecutionContext context) {
        context.declareFunction("GITLAB_PIPELINE_STATUS",
            List.of(
                new Parameter("project", "STRING", ParameterMode.IN),
                new Parameter("pipeline_id", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("GITLAB_PIPELINE_STATUS", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String project = args.get(0).toString();
                    long pipelineId = ((Number) args.get(1)).longValue();
                    String gitlabUrl = System.getenv("GITLAB_URL") != null ? System.getenv("GITLAB_URL") : "https://gitlab.com";
                    String token = System.getenv("GITLAB_TOKEN");
                    
                    if (token == null || token.isEmpty()) {
                        listener.onFailure(new RuntimeException("GITLAB_TOKEN not configured"));
                        return;
                    }
                    
                    String encodedProject = java.net.URLEncoder.encode(project, "UTF-8");
                    
                    Map<String, String> headers = new HashMap<>();
                    headers.put("PRIVATE-TOKEN", token);
                    
                    HttpClient client = HttpClientHolder.get();
                    String response = client.get(gitlabUrl + "/api/v4/projects/" + encodedProject + "/pipelines/" + pipelineId, headers);
                    listener.onResponse(parseJsonToMap(response));
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("GITLAB_PIPELINE_STATUS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    // ==================== ARGOCD ====================

    public static void registerArgoCDSync(ExecutionContext context) {
        context.declareFunction("ARGOCD_SYNC",
            List.of(new Parameter("app", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ARGOCD_SYNC", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String app = args.get(0).toString();
                    boolean prune = args.size() > 1 && args.get(1) != null 
                        && Boolean.parseBoolean(args.get(1).toString());
                    String argoUrl = System.getenv("ARGOCD_URL");
                    String token = System.getenv("ARGOCD_TOKEN");
                    
                    if (argoUrl == null || argoUrl.isEmpty()) {
                        listener.onFailure(new RuntimeException("ARGOCD_URL not configured"));
                        return;
                    }
                    if (token == null || token.isEmpty()) {
                        listener.onFailure(new RuntimeException("ARGOCD_TOKEN not configured"));
                        return;
                    }
                    
                    XContentBuilder builder = XContentFactory.jsonBuilder();
                    builder.startObject().field("prune", prune).field("dryRun", false).endObject();
                    
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + token);
                    headers.put("Content-Type", "application/json");
                    
                    HttpClient client = HttpClientHolder.get();
                    String response = client.post(argoUrl + "/api/v1/applications/" + app + "/sync", 
                        headers, Strings.toString(builder));
                    listener.onResponse(parseJsonToMap(response));
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ARGOCD_SYNC failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerArgoCDGetApp(ExecutionContext context) {
        context.declareFunction("ARGOCD_GET_APP",
            List.of(new Parameter("app", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ARGOCD_GET_APP", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String app = args.get(0).toString();
                    String argoUrl = System.getenv("ARGOCD_URL");
                    String token = System.getenv("ARGOCD_TOKEN");
                    
                    if (argoUrl == null || argoUrl.isEmpty()) {
                        listener.onFailure(new RuntimeException("ARGOCD_URL not configured"));
                        return;
                    }
                    if (token == null || token.isEmpty()) {
                        listener.onFailure(new RuntimeException("ARGOCD_TOKEN not configured"));
                        return;
                    }
                    
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + token);
                    
                    HttpClient client = HttpClientHolder.get();
                    String response = client.get(argoUrl + "/api/v1/applications/" + app, headers);
                    listener.onResponse(parseJsonToMap(response));
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ARGOCD_GET_APP failed: " + e.getMessage(), e));
                }
            })
        );
    }

    private static Map<String, Object> parseJsonToMap(String json) throws Exception {
        try (XContentParser parser = XContentType.JSON.xContent().createParser(
                XContentParserConfiguration.EMPTY, 
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))) {
            return parser.map();
        }
    }
}
