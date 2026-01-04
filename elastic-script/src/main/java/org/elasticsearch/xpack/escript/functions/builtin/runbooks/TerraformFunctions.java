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
 * Terraform Cloud/Enterprise integration functions for elastic-script.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.RUNBOOK,
    description = "Terraform Cloud/Enterprise integration functions for infrastructure automation."
)
public class TerraformFunctions {

    private static final Logger LOGGER = LogManager.getLogger(TerraformFunctions.class);
    private static final String TFC_API_URL = "https://app.terraform.io/api/v2";

    public static void registerAll(ExecutionContext context) {
        LOGGER.info("Registering Terraform Cloud built-in functions");
        registerTfCloudRun(context);
        registerTfCloudStatus(context);
        registerTfCloudWait(context);
        registerTfCloudCancel(context);
        registerTfCloudOutputs(context);
        registerTfCloudListWorkspaces(context);
    }

    private static TfcConfig getConfig(List<Object> args, int tokenIndex, int orgIndex) {
        String token = args.size() > tokenIndex && args.get(tokenIndex) != null 
            ? args.get(tokenIndex).toString() : System.getenv("TFC_TOKEN");
        String org = args.size() > orgIndex && args.get(orgIndex) != null 
            ? args.get(orgIndex).toString() : System.getenv("TFC_ORG");
        
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("TFC_TOKEN not configured");
        }
        return new TfcConfig(token, org);
    }

    private static String tfcRequest(String token, String method, String path, String body) throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("Content-Type", "application/vnd.api+json");
        
        HttpClient client = HttpClientHolder.get();
        return client.request(method, TFC_API_URL + path, headers, body);
    }

    private static String getWorkspaceId(String token, String org, String workspaceName) throws Exception {
        String response = tfcRequest(token, "GET", "/organizations/" + org + "/workspaces/" + workspaceName, null);
        Map<String, Object> result = parseJsonToMap(response);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        return (String) data.get("id");
    }

    public static void registerTfCloudRun(ExecutionContext context) {
        context.declareFunction("TF_CLOUD_RUN",
            List.of(new Parameter("workspace", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("TF_CLOUD_RUN", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String workspace = args.get(0).toString();
                    String message = args.size() > 1 && args.get(1) != null 
                        ? args.get(1).toString() : "Triggered by elastic-script";
                    boolean autoApply = args.size() > 2 && args.get(2) != null 
                        && Boolean.parseBoolean(args.get(2).toString());
                    
                    TfcConfig config = getConfig(args, 3, 4);
                    if (config.organization == null || config.organization.isEmpty()) {
                        listener.onFailure(new RuntimeException("TFC_ORG not configured"));
                        return;
                    }
                    
                    String workspaceId = getWorkspaceId(config.token, config.organization, workspace);
                    
                    XContentBuilder builder = XContentFactory.jsonBuilder();
                    builder.startObject().startObject("data");
                    builder.field("type", "runs");
                    builder.startObject("attributes").field("message", message).field("auto-apply", autoApply).endObject();
                    builder.startObject("relationships").startObject("workspace").startObject("data")
                        .field("type", "workspaces").field("id", workspaceId).endObject().endObject().endObject();
                    builder.endObject().endObject();
                    
                    String response = tfcRequest(config.token, "POST", "/runs", Strings.toString(builder));
                    Map<String, Object> result = parseJsonToMap(response);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) result.get("data");
                    listener.onResponse(data.get("id"));
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("TF_CLOUD_RUN failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerTfCloudStatus(ExecutionContext context) {
        context.declareFunction("TF_CLOUD_STATUS",
            List.of(new Parameter("run_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("TF_CLOUD_STATUS", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String runId = args.get(0).toString();
                    TfcConfig config = getConfig(args, 1, 2);
                    
                    String response = tfcRequest(config.token, "GET", "/runs/" + runId, null);
                    Map<String, Object> result = parseJsonToMap(response);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) result.get("data");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> attrs = (Map<String, Object>) data.get("attributes");
                    listener.onResponse(attrs.get("status"));
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("TF_CLOUD_STATUS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerTfCloudWait(ExecutionContext context) {
        context.declareFunction("TF_CLOUD_WAIT",
            List.of(new Parameter("run_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("TF_CLOUD_WAIT", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String runId = args.get(0).toString();
                    TfcConfig config = getConfig(args, 2, 3);
                    long timeout = 30 * 60 * 1000; // 30 minutes default
                    long start = System.currentTimeMillis();
                    
                    while (System.currentTimeMillis() - start < timeout) {
                        String response = tfcRequest(config.token, "GET", "/runs/" + runId, null);
                        Map<String, Object> result = parseJsonToMap(response);
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = (Map<String, Object>) result.get("data");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> attrs = (Map<String, Object>) data.get("attributes");
                        String status = (String) attrs.get("status");
                        
                        if (isTerminalStatus(status)) {
                            Map<String, Object> runResult = new HashMap<>();
                            runResult.put("run_id", runId);
                            runResult.put("status", status);
                            listener.onResponse(runResult);
                            return;
                        }
                        Thread.sleep(5000);
                    }
                    listener.onFailure(new RuntimeException("Terraform run did not complete within timeout"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    listener.onFailure(new RuntimeException("Wait interrupted", e));
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("TF_CLOUD_WAIT failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerTfCloudCancel(ExecutionContext context) {
        context.declareFunction("TF_CLOUD_CANCEL",
            List.of(new Parameter("run_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("TF_CLOUD_CANCEL", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String runId = args.get(0).toString();
                    TfcConfig config = getConfig(args, 2, 3);
                    
                    tfcRequest(config.token, "POST", "/runs/" + runId + "/actions/cancel", 
                        "{\"comment\":\"Cancelled by elastic-script\"}");
                    listener.onResponse(true);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("TF_CLOUD_CANCEL failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerTfCloudOutputs(ExecutionContext context) {
        context.declareFunction("TF_CLOUD_OUTPUTS",
            List.of(new Parameter("workspace", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("TF_CLOUD_OUTPUTS", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String workspace = args.get(0).toString();
                    TfcConfig config = getConfig(args, 1, 2);
                    if (config.organization == null || config.organization.isEmpty()) {
                        listener.onFailure(new RuntimeException("TFC_ORG not configured"));
                        return;
                    }
                    
                    String workspaceId = getWorkspaceId(config.token, config.organization, workspace);
                    String response = tfcRequest(config.token, "GET", 
                        "/workspaces/" + workspaceId + "/current-state-version?include=outputs", null);
                    Map<String, Object> result = parseJsonToMap(response);
                    
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> included = (List<Map<String, Object>>) result.get("included");
                    Map<String, Object> outputs = new HashMap<>();
                    if (included != null) {
                        for (Map<String, Object> item : included) {
                            if ("state-version-outputs".equals(item.get("type"))) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> attrs = (Map<String, Object>) item.get("attributes");
                                outputs.put((String) attrs.get("name"), attrs.get("value"));
                            }
                        }
                    }
                    listener.onResponse(outputs);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("TF_CLOUD_OUTPUTS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerTfCloudListWorkspaces(ExecutionContext context) {
        context.declareFunction("TF_CLOUD_LIST_WORKSPACES",
            List.of(),
            new BuiltInFunctionDefinition("TF_CLOUD_LIST_WORKSPACES", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String search = args.size() > 0 && args.get(0) != null ? args.get(0).toString() : null;
                    TfcConfig config = getConfig(args, 1, 2);
                    if (config.organization == null || config.organization.isEmpty()) {
                        listener.onFailure(new RuntimeException("TFC_ORG not configured"));
                        return;
                    }
                    
                    String path = "/organizations/" + config.organization + "/workspaces";
                    if (search != null && !search.isEmpty()) {
                        path += "?search[name]=" + java.net.URLEncoder.encode(search, "UTF-8");
                    }
                    String response = tfcRequest(config.token, "GET", path, null);
                    Map<String, Object> result = parseJsonToMap(response);
                    listener.onResponse(result.get("data"));
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("TF_CLOUD_LIST_WORKSPACES failed: " + e.getMessage(), e));
                }
            })
        );
    }

    private static boolean isTerminalStatus(String status) {
        return status != null && (status.equals("applied") || status.equals("errored") || 
            status.equals("canceled") || status.equals("discarded") || status.equals("planned_and_finished"));
    }

    private static Map<String, Object> parseJsonToMap(String json) throws Exception {
        try (XContentParser parser = XContentType.JSON.xContent().createParser(
                XContentParserConfiguration.EMPTY, 
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))) {
            return parser.map();
        }
    }

    private static class TfcConfig {
        final String token;
        final String organization;
        TfcConfig(String token, String organization) {
            this.token = token;
            this.organization = organization;
        }
    }
}
