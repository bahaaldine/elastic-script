/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.builtin.workflows;

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
 * Elastic Workflows integration functions for elastic-script.
 * 
 * Enables calling Kibana Workflows API from within elastic-script procedures and skills.
 * See: https://www.elastic.co/docs/explore-analyze/workflows
 */
@FunctionCollectionSpec(
    category = FunctionCategory.RUNBOOK,
    description = "Elastic Workflows integration functions for triggering and managing Kibana workflows."
)
public class WorkflowFunctions {

    private static final Logger LOGGER = LogManager.getLogger(WorkflowFunctions.class);
    
    private static final String DEFAULT_KIBANA_URL = "http://localhost:5601";

    public static void registerAll(ExecutionContext context) {
        registerWorkflowTrigger(context);
        registerWorkflowList(context);
        registerWorkflowGet(context);
        registerWorkflowStatus(context);
        registerWorkflowCreate(context);
        registerWorkflowDelete(context);
    }

    /**
     * WORKFLOW_TRIGGER - Trigger execution of a workflow by ID or name.
     * 
     * Usage:
     *   SET result = WORKFLOW_TRIGGER('workflow-id', {'ip_address': '8.8.8.8'});
     *   SET result = WORKFLOW_TRIGGER('IP Reputation Check', {'ip_address': '1.2.3.4'}, 'http://kibana:5601');
     */
    public static void registerWorkflowTrigger(ExecutionContext context) {
        context.declareFunction("WORKFLOW_TRIGGER",
            List.of(
                new Parameter("workflow_id", "STRING", ParameterMode.IN),
                new Parameter("inputs", "DOCUMENT", ParameterMode.IN),
                new Parameter("kibana_url", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("WORKFLOW_TRIGGER", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String workflowId = args.get(0).toString();
                    Map<String, Object> inputs = args.size() > 1 && args.get(1) != null 
                        ? toMap(args.get(1)) 
                        : new HashMap<>();
                    String kibanaUrl = args.size() > 2 && args.get(2) != null 
                        ? args.get(2).toString() 
                        : getKibanaUrl();
                    
                    Map<String, Object> result = triggerWorkflow(kibanaUrl, workflowId, inputs);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("WORKFLOW_TRIGGER failed: " + e.getMessage(), e));
                }
            })
        );
    }

    /**
     * WORKFLOW_LIST - List all available workflows.
     * 
     * Usage:
     *   SET workflows = WORKFLOW_LIST();
     *   SET workflows = WORKFLOW_LIST('http://kibana:5601');
     */
    public static void registerWorkflowList(ExecutionContext context) {
        context.declareFunction("WORKFLOW_LIST",
            List.of(
                new Parameter("kibana_url", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("WORKFLOW_LIST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String kibanaUrl = args.size() > 0 && args.get(0) != null 
                        ? args.get(0).toString() 
                        : getKibanaUrl();
                    
                    List<Map<String, Object>> workflows = listWorkflows(kibanaUrl);
                    listener.onResponse(workflows);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("WORKFLOW_LIST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    /**
     * WORKFLOW_GET - Get details of a specific workflow.
     * 
     * Usage:
     *   SET workflow = WORKFLOW_GET('workflow-id');
     */
    public static void registerWorkflowGet(ExecutionContext context) {
        context.declareFunction("WORKFLOW_GET",
            List.of(
                new Parameter("workflow_id", "STRING", ParameterMode.IN),
                new Parameter("kibana_url", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("WORKFLOW_GET", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String workflowId = args.get(0).toString();
                    String kibanaUrl = args.size() > 1 && args.get(1) != null 
                        ? args.get(1).toString() 
                        : getKibanaUrl();
                    
                    Map<String, Object> workflow = getWorkflow(kibanaUrl, workflowId);
                    listener.onResponse(workflow);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("WORKFLOW_GET failed: " + e.getMessage(), e));
                }
            })
        );
    }

    /**
     * WORKFLOW_STATUS - Get the status of a workflow execution.
     * 
     * Usage:
     *   SET status = WORKFLOW_STATUS('execution-id');
     */
    public static void registerWorkflowStatus(ExecutionContext context) {
        context.declareFunction("WORKFLOW_STATUS",
            List.of(
                new Parameter("execution_id", "STRING", ParameterMode.IN),
                new Parameter("kibana_url", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("WORKFLOW_STATUS", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String executionId = args.get(0).toString();
                    String kibanaUrl = args.size() > 1 && args.get(1) != null 
                        ? args.get(1).toString() 
                        : getKibanaUrl();
                    
                    Map<String, Object> status = getExecutionStatus(kibanaUrl, executionId);
                    listener.onResponse(status);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("WORKFLOW_STATUS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    /**
     * WORKFLOW_CREATE - Create a new workflow from YAML definition.
     * 
     * Usage:
     *   SET result = WORKFLOW_CREATE(yaml_content);
     */
    public static void registerWorkflowCreate(ExecutionContext context) {
        context.declareFunction("WORKFLOW_CREATE",
            List.of(
                new Parameter("yaml", "STRING", ParameterMode.IN),
                new Parameter("kibana_url", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("WORKFLOW_CREATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String yaml = args.get(0).toString();
                    String kibanaUrl = args.size() > 1 && args.get(1) != null 
                        ? args.get(1).toString() 
                        : getKibanaUrl();
                    
                    Map<String, Object> result = createWorkflow(kibanaUrl, yaml);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("WORKFLOW_CREATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    /**
     * WORKFLOW_DELETE - Delete a workflow by ID.
     * 
     * Usage:
     *   SET result = WORKFLOW_DELETE('workflow-id');
     */
    public static void registerWorkflowDelete(ExecutionContext context) {
        context.declareFunction("WORKFLOW_DELETE",
            List.of(
                new Parameter("workflow_id", "STRING", ParameterMode.IN),
                new Parameter("kibana_url", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("WORKFLOW_DELETE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String workflowId = args.get(0).toString();
                    String kibanaUrl = args.size() > 1 && args.get(1) != null 
                        ? args.get(1).toString() 
                        : getKibanaUrl();
                    
                    Map<String, Object> result = deleteWorkflow(kibanaUrl, workflowId);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("WORKFLOW_DELETE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    // ========================================================================
    // Implementation helpers
    // ========================================================================

    private static String getKibanaUrl() {
        String envUrl = System.getenv("KIBANA_URL");
        return envUrl != null && !envUrl.isEmpty() ? envUrl : DEFAULT_KIBANA_URL;
    }

    private static Map<String, String> getKibanaHeaders() {
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
            if (user != null && pass != null) {
                String credentials = java.util.Base64.getEncoder()
                    .encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
                headers.put("Authorization", "Basic " + credentials);
            }
        }
        
        return headers;
    }

    private static Map<String, Object> triggerWorkflow(String kibanaUrl, String workflowId, Map<String, Object> inputs) 
            throws Exception {
        HttpClient client = HttpClientHolder.get();
        String url = kibanaUrl + "/api/workflows/" + workflowId + "/run";
        
        String body = mapToJson(Map.of("inputs", inputs));
        
        try {
            String response = client.request("POST", url, getKibanaHeaders(), body);
            Map<String, Object> result = parseJsonToMap(response);
            result.put("success", true);
            result.put("workflow_id", workflowId);
            return result;
        } catch (HttpClientException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("workflow_id", workflowId);
            error.put("status_code", e.getStatusCode());
            error.put("error", e.getResponseBody());
            return error;
        }
    }

    private static List<Map<String, Object>> listWorkflows(String kibanaUrl) throws Exception {
        HttpClient client = HttpClientHolder.get();
        String url = kibanaUrl + "/api/workflows";
        
        try {
            String response = client.request("GET", url, getKibanaHeaders(), null);
            Map<String, Object> result = parseJsonToMap(response);
            
            Object workflows = result.get("workflows");
            if (workflows == null) {
                workflows = result.get("data");
            }
            if (workflows == null) {
                workflows = result.get("items");
            }
            
            if (workflows instanceof List) {
                List<Map<String, Object>> workflowList = new ArrayList<>();
                for (Object item : (List<?>) workflows) {
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> wf = (Map<String, Object>) item;
                        workflowList.add(wf);
                    }
                }
                return workflowList;
            }
            
            return new ArrayList<>();
        } catch (HttpClientException e) {
            throw new RuntimeException("Failed to list workflows: " + e.getMessage(), e);
        }
    }

    private static Map<String, Object> getWorkflow(String kibanaUrl, String workflowId) throws Exception {
        HttpClient client = HttpClientHolder.get();
        String url = kibanaUrl + "/api/workflows/" + workflowId;
        
        try {
            String response = client.request("GET", url, getKibanaHeaders(), null);
            return parseJsonToMap(response);
        } catch (HttpClientException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("found", false);
            error.put("workflow_id", workflowId);
            error.put("error", e.getMessage());
            return error;
        }
    }

    private static Map<String, Object> getExecutionStatus(String kibanaUrl, String executionId) throws Exception {
        HttpClient client = HttpClientHolder.get();
        String url = kibanaUrl + "/api/workflows/executions/" + executionId;
        
        try {
            String response = client.request("GET", url, getKibanaHeaders(), null);
            return parseJsonToMap(response);
        } catch (HttpClientException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("found", false);
            error.put("execution_id", executionId);
            error.put("error", e.getMessage());
            return error;
        }
    }

    private static Map<String, Object> createWorkflow(String kibanaUrl, String yaml) throws Exception {
        HttpClient client = HttpClientHolder.get();
        String url = kibanaUrl + "/api/workflows";
        
        String body = mapToJson(Map.of("yaml", yaml));
        
        try {
            String response = client.request("POST", url, getKibanaHeaders(), body);
            Map<String, Object> result = parseJsonToMap(response);
            result.put("success", true);
            return result;
        } catch (HttpClientException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("status_code", e.getStatusCode());
            error.put("error", e.getResponseBody());
            return error;
        }
    }

    private static Map<String, Object> deleteWorkflow(String kibanaUrl, String workflowId) throws Exception {
        HttpClient client = HttpClientHolder.get();
        String url = kibanaUrl + "/api/workflows/" + workflowId;
        
        try {
            String response = client.request("DELETE", url, getKibanaHeaders(), null);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("workflow_id", workflowId);
            result.put("deleted", true);
            return result;
        } catch (HttpClientException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("workflow_id", workflowId);
            error.put("status_code", e.getStatusCode());
            error.put("error", e.getResponseBody());
            return error;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return new HashMap<>();
    }

    private static String mapToJson(Map<String, Object> map) throws Exception {
        try (XContentBuilder builder = XContentFactory.jsonBuilder()) {
            builder.map(map);
            return Strings.toString(builder);
        }
    }

    private static Map<String, Object> parseJsonToMap(String json) throws Exception {
        if (json == null || json.isEmpty()) {
            return new HashMap<>();
        }
        try (XContentParser parser = XContentType.JSON.xContent().createParser(
                XContentParserConfiguration.EMPTY, 
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))) {
            return parser.map();
        }
    }
}
