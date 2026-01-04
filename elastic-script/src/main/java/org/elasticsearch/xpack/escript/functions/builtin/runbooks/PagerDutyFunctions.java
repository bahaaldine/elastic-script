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
 * PagerDuty integration functions for elastic-script.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.RUNBOOK,
    description = "PagerDuty integration functions for incident management."
)
public class PagerDutyFunctions {

    private static final Logger LOGGER = LogManager.getLogger(PagerDutyFunctions.class);
    private static final String EVENTS_API_URL = "https://events.pagerduty.com/v2/enqueue";
    private static final String REST_API_URL = "https://api.pagerduty.com";

    public static void registerAll(ExecutionContext context) {
        LOGGER.info("Registering PagerDuty built-in functions");
        registerPagerDutyTrigger(context);
        registerPagerDutyResolve(context);
        registerPagerDutyAcknowledge(context);
        registerPagerDutyAddNote(context);
        registerPagerDutyGetIncident(context);
        registerPagerDutyListIncidents(context);
    }

    private static PagerDutyConfig getConfig(List<Object> args, int apiKeyIndex, int routingKeyIndex) {
        String apiKey = args.size() > apiKeyIndex && args.get(apiKeyIndex) != null 
            ? args.get(apiKeyIndex).toString() : System.getenv("PAGERDUTY_API_KEY");
        String routingKey = args.size() > routingKeyIndex && args.get(routingKeyIndex) != null 
            ? args.get(routingKeyIndex).toString() : System.getenv("PAGERDUTY_ROUTING_KEY");
        return new PagerDutyConfig(apiKey, routingKey);
    }

    private static Map<String, Object> eventsApiRequest(String routingKey, String eventAction, String dedupKey, 
            String summary, String severity) throws Exception {
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();
        builder.field("routing_key", routingKey);
        builder.field("event_action", eventAction);
        if (dedupKey != null) builder.field("dedup_key", dedupKey);
        if (eventAction.equals("trigger")) {
            builder.startObject("payload");
            builder.field("summary", summary);
            builder.field("severity", severity != null ? severity : "error");
            builder.field("source", "elastic-script");
            builder.endObject();
        }
        builder.endObject();
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        
        HttpClient client = HttpClientHolder.get();
        String response = client.post(EVENTS_API_URL, headers, Strings.toString(builder));
        return parseJsonToMap(response);
    }
    
    private static String restApiRequest(String apiKey, String method, String path, String body) throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Token token=" + apiKey);
        headers.put("Accept", "application/json");
        if (body != null) {
            headers.put("Content-Type", "application/json");
        }
        
        HttpClient client = HttpClientHolder.get();
        return client.request(method, REST_API_URL + path, headers, body);
    }

    public static void registerPagerDutyTrigger(ExecutionContext context) {
        context.declareFunction("PAGERDUTY_TRIGGER",
            List.of(new Parameter("summary", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("PAGERDUTY_TRIGGER", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String summary = args.get(0).toString();
                    String severity = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : "error";
                    String dedupKey = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : null;
                    
                    PagerDutyConfig config = getConfig(args, 4, 4);
                    if (config.routingKey == null || config.routingKey.isEmpty()) {
                        listener.onFailure(new RuntimeException("PAGERDUTY_ROUTING_KEY not configured"));
                        return;
                    }
                    
                    Map<String, Object> response = eventsApiRequest(config.routingKey, "trigger", dedupKey, summary, severity);
                    listener.onResponse(response.get("dedup_key"));
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("PAGERDUTY_TRIGGER failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerPagerDutyResolve(ExecutionContext context) {
        context.declareFunction("PAGERDUTY_RESOLVE",
            List.of(new Parameter("dedup_key", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("PAGERDUTY_RESOLVE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String dedupKey = args.get(0).toString();
                    PagerDutyConfig config = getConfig(args, 1, 1);
                    if (config.routingKey == null || config.routingKey.isEmpty()) {
                        listener.onFailure(new RuntimeException("PAGERDUTY_ROUTING_KEY not configured"));
                        return;
                    }
                    
                    eventsApiRequest(config.routingKey, "resolve", dedupKey, null, null);
                    listener.onResponse(true);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("PAGERDUTY_RESOLVE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerPagerDutyAcknowledge(ExecutionContext context) {
        context.declareFunction("PAGERDUTY_ACKNOWLEDGE",
            List.of(new Parameter("dedup_key", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("PAGERDUTY_ACKNOWLEDGE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String dedupKey = args.get(0).toString();
                    PagerDutyConfig config = getConfig(args, 1, 1);
                    if (config.routingKey == null || config.routingKey.isEmpty()) {
                        listener.onFailure(new RuntimeException("PAGERDUTY_ROUTING_KEY not configured"));
                        return;
                    }
                    
                    eventsApiRequest(config.routingKey, "acknowledge", dedupKey, null, null);
                    listener.onResponse(true);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("PAGERDUTY_ACKNOWLEDGE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerPagerDutyAddNote(ExecutionContext context) {
        context.declareFunction("PAGERDUTY_ADD_NOTE",
            List.of(
                new Parameter("incident_id", "STRING", ParameterMode.IN),
                new Parameter("content", "STRING", ParameterMode.IN),
                new Parameter("user_email", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("PAGERDUTY_ADD_NOTE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String incidentId = args.get(0).toString();
                    String content = args.get(1).toString();
                    String userEmail = args.get(2).toString();
                    
                    PagerDutyConfig config = getConfig(args, 3, 4);
                    if (config.apiKey == null || config.apiKey.isEmpty()) {
                        listener.onFailure(new RuntimeException("PAGERDUTY_API_KEY not configured"));
                        return;
                    }
                    
                    XContentBuilder builder = XContentFactory.jsonBuilder();
                    builder.startObject().startObject("note").field("content", content).endObject().endObject();
                    
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Token token=" + config.apiKey);
                    headers.put("Content-Type", "application/json");
                    headers.put("From", userEmail);
                    
                    HttpClient client = HttpClientHolder.get();
                    client.post(REST_API_URL + "/incidents/" + incidentId + "/notes", headers, Strings.toString(builder));
                    listener.onResponse(true);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("PAGERDUTY_ADD_NOTE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerPagerDutyGetIncident(ExecutionContext context) {
        context.declareFunction("PAGERDUTY_GET_INCIDENT",
            List.of(new Parameter("incident_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("PAGERDUTY_GET_INCIDENT", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String incidentId = args.get(0).toString();
                    PagerDutyConfig config = getConfig(args, 1, 2);
                    if (config.apiKey == null || config.apiKey.isEmpty()) {
                        listener.onFailure(new RuntimeException("PAGERDUTY_API_KEY not configured"));
                        return;
                    }
                    
                    String response = restApiRequest(config.apiKey, "GET", "/incidents/" + incidentId, null);
                    Map<String, Object> result = parseJsonToMap(response);
                    listener.onResponse(result.get("incident"));
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("PAGERDUTY_GET_INCIDENT failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerPagerDutyListIncidents(ExecutionContext context) {
        context.declareFunction("PAGERDUTY_LIST_INCIDENTS",
            List.of(),
            new BuiltInFunctionDefinition("PAGERDUTY_LIST_INCIDENTS", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String statuses = args.size() > 0 && args.get(0) != null 
                        ? args.get(0).toString() : "triggered,acknowledged";
                    int limit = args.size() > 1 && args.get(1) != null 
                        ? ((Number) args.get(1)).intValue() : 25;
                    
                    PagerDutyConfig config = getConfig(args, 2, 3);
                    if (config.apiKey == null || config.apiKey.isEmpty()) {
                        listener.onFailure(new RuntimeException("PAGERDUTY_API_KEY not configured"));
                        return;
                    }
                    
                    StringBuilder path = new StringBuilder("/incidents?limit=" + limit);
                    for (String status : statuses.split(",")) {
                        path.append("&statuses[]=").append(status.trim());
                    }
                    
                    String response = restApiRequest(config.apiKey, "GET", path.toString(), null);
                    Map<String, Object> result = parseJsonToMap(response);
                    listener.onResponse(result.get("incidents"));
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("PAGERDUTY_LIST_INCIDENTS failed: " + e.getMessage(), e));
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

    private static class PagerDutyConfig {
        final String apiKey;
        final String routingKey;
        PagerDutyConfig(String apiKey, String routingKey) {
            this.apiKey = apiKey;
            this.routingKey = routingKey;
        }
    }
}
