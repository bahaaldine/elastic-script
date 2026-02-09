/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.actions;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.node.NodeClient;
import org.elasticsearch.logging.LogManager;
import org.elasticsearch.logging.Logger;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xpack.escript.applications.ApplicationDefinition;
import org.elasticsearch.xpack.escript.applications.ApplicationRegistry;
import org.elasticsearch.xpack.escript.applications.IntentDefinition;
import org.elasticsearch.xpack.escript.applications.SkillDefinition;
import org.elasticsearch.xpack.escript.executors.ElasticScriptExecutor;
import org.elasticsearch.xpack.escript.primitives.ExecutionResult;
import org.elasticsearch.xpack.escript.primitives.ReturnValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.POST;

/**
 * REST action for natural language intent matching and execution.
 * 
 * Endpoints:
 * - POST /_escript/intent - Match and execute intent from natural language query
 * - POST /_escript/intent/_match - Match intent without executing (returns the matched skill)
 * - GET /_escript/intents - List all registered intents
 * 
 * Request format:
 * <pre>
 * {
 *   "query": "Show me customers who might be leaving",
 *   "params": { "threshold": 0.7 }  // Optional additional params
 * }
 * </pre>
 * 
 * Response format for successful match and execution:
 * <pre>
 * {
 *   "intent": {
 *     "matched_pattern": "churn|leaving|at risk",
 *     "confidence": 0.75,
 *     "target_skill": "detect_churn"
 *   },
 *   "result": [...the skill result...],
 *   "success": true
 * }
 * </pre>
 */
public class RestIntentAction extends BaseRestHandler {

    private static final Logger LOGGER = LogManager.getLogger(RestIntentAction.class);

    private final ApplicationRegistry registry;
    private final ElasticScriptExecutor executor;

    public RestIntentAction(ApplicationRegistry registry, ElasticScriptExecutor executor) {
        this.registry = registry;
        this.executor = executor;
    }

    @Override
    public List<Route> routes() {
        return List.of(
            Route.builder(POST, "/_escript/intent").build(),
            Route.builder(POST, "/_escript/intent/_match").build(),
            Route.builder(GET, "/_escript/intents").build()
        );
    }

    @Override
    public String getName() {
        return "elastic_script_intent";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        if (request.method() == GET) {
            // List all intents
            return channel -> listAllIntents(channel);
        }
        
        String path = request.path();
        boolean matchOnly = path.endsWith("/_match");
        
        String query = null;
        Map<String, Object> params = Map.of();
        
        if (request.hasContentOrSourceParam()) {
            XContentParser parser = request.contentParser();
            XContentParser.Token token = parser.nextToken();
            while (token != null) {
                if (token == XContentParser.Token.FIELD_NAME) {
                    String fieldName = parser.currentName();
                    parser.nextToken();
                    if ("query".equals(fieldName)) {
                        query = parser.text();
                    } else if ("params".equals(fieldName)) {
                        params = parser.map();
                    }
                }
                token = parser.nextToken();
            }
        }
        
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Field [query] must be provided");
        }
        
        final String userQuery = query;
        final Map<String, Object> userParams = params;
        
        if (matchOnly) {
            return channel -> matchIntent(userQuery, channel);
        } else {
            return channel -> matchAndExecuteIntent(userQuery, userParams, channel);
        }
    }

    private void listAllIntents(org.elasticsearch.rest.RestChannel channel) {
        registry.listApplications(new ActionListener<>() {
            @Override
            public void onResponse(List<ApplicationDefinition> apps) {
                try {
                    XContentBuilder builder = XContentFactory.jsonBuilder();
                    builder.startObject();
                    
                    builder.startArray("intents");
                    for (ApplicationDefinition app : apps) {
                        if (app.getStatus() == ApplicationDefinition.Status.RUNNING) {
                            for (IntentDefinition intent : app.getIntents()) {
                                builder.startObject();
                                builder.field("pattern", intent.getPattern());
                                builder.field("target_skill", intent.getTargetSkill());
                                builder.field("description", intent.getDescription());
                                builder.field("application", app.getName());
                                builder.endObject();
                            }
                        }
                    }
                    builder.endArray();
                    
                    builder.endObject();
                    channel.sendResponse(new RestResponse(RestStatus.OK, builder));
                } catch (Exception e) {
                    sendError(channel, e);
                }
            }

            @Override
            public void onFailure(Exception e) {
                sendError(channel, e);
            }
        });
    }

    private void matchIntent(String userQuery, org.elasticsearch.rest.RestChannel channel) {
        LOGGER.debug("Matching intent for query: {}", userQuery);
        
        registry.matchIntent(userQuery, new ActionListener<>() {
            @Override
            public void onResponse(ApplicationDefinition.IntentMatch match) {
                try {
                    XContentBuilder builder = XContentFactory.jsonBuilder();
                    builder.startObject();
                    
                    if (match != null) {
                        builder.field("matched", true);
                        builder.startObject("intent");
                        builder.field("pattern", match.getIntent().getPattern());
                        builder.field("target_skill", match.getIntent().getTargetSkill());
                        builder.field("confidence", match.getConfidence());
                        if (match.getIntent().getDescription() != null) {
                            builder.field("description", match.getIntent().getDescription());
                        }
                        builder.endObject();
                    } else {
                        builder.field("matched", false);
                        builder.field("message", "No intent matched the query");
                    }
                    
                    builder.field("query", userQuery);
                    builder.endObject();
                    
                    channel.sendResponse(new RestResponse(RestStatus.OK, builder));
                } catch (Exception e) {
                    sendError(channel, e);
                }
            }

            @Override
            public void onFailure(Exception e) {
                sendError(channel, e);
            }
        });
    }

    private void matchAndExecuteIntent(String userQuery, Map<String, Object> params, 
                                       org.elasticsearch.rest.RestChannel channel) {
        LOGGER.debug("Matching and executing intent for query: {}", userQuery);
        
        registry.listApplications(new ActionListener<>() {
            @Override
            public void onResponse(List<ApplicationDefinition> apps) {
                // Find the best matching intent across all running applications
                ApplicationDefinition.IntentMatch bestMatch = null;
                ApplicationDefinition matchedApp = null;
                
                for (ApplicationDefinition app : apps) {
                    if (app.getStatus() == ApplicationDefinition.Status.RUNNING) {
                        ApplicationDefinition.IntentMatch match = app.matchIntent(userQuery);
                        if (match != null && (bestMatch == null || match.getConfidence() > bestMatch.getConfidence())) {
                            bestMatch = match;
                            matchedApp = app;
                        }
                    }
                }
                
                if (bestMatch == null) {
                    try {
                        XContentBuilder builder = XContentFactory.jsonBuilder();
                        builder.startObject();
                        builder.field("matched", false);
                        builder.field("message", "No intent matched the query: " + userQuery);
                        builder.field("query", userQuery);
                        builder.endObject();
                        channel.sendResponse(new RestResponse(RestStatus.OK, builder));
                    } catch (Exception e) {
                        sendError(channel, e);
                    }
                    return;
                }
                
                // Find the target skill
                IntentDefinition intent = bestMatch.getIntent();
                double confidence = bestMatch.getConfidence();
                SkillDefinition skill = matchedApp.findSkill(intent.getTargetSkill());
                
                if (skill == null) {
                    try {
                        XContentBuilder builder = XContentFactory.jsonBuilder();
                        builder.startObject();
                        builder.field("matched", true);
                        builder.field("error", "Target skill not found: " + intent.getTargetSkill());
                        builder.endObject();
                        channel.sendResponse(new RestResponse(RestStatus.INTERNAL_SERVER_ERROR, builder));
                    } catch (Exception e) {
                        sendError(channel, e);
                    }
                    return;
                }
                
                // Build and execute the procedure call
                String procedureName = skill.getProcedureName();
                List<String> procArgs = skill.getProcedureArgs();
                
                List<Object> argValues = new ArrayList<>();
                for (String argName : procArgs) {
                    Object value = params.get(argName);
                    if (value == null) {
                        for (SkillDefinition.SkillParameter p : skill.getParameters()) {
                            if (p.getName().equals(argName) && p.getDefaultValue() != null) {
                                value = p.getDefaultValue();
                                break;
                            }
                        }
                    }
                    argValues.add(value);
                }
                
                StringBuilder callBuilder = new StringBuilder();
                callBuilder.append("CALL ").append(procedureName).append("(");
                for (int i = 0; i < argValues.size(); i++) {
                    if (i > 0) callBuilder.append(", ");
                    Object val = argValues.get(i);
                    if (val instanceof String) {
                        callBuilder.append("'").append(val.toString().replace("'", "''")).append("'");
                    } else if (val == null) {
                        callBuilder.append("NULL");
                    } else {
                        callBuilder.append(val);
                    }
                }
                callBuilder.append(")");
                
                String callStatement = callBuilder.toString();
                LOGGER.debug("Executing intent via: {}", callStatement);
                
                final double finalConfidence = confidence;
                final String matchedPattern = intent.getPattern();
                final String targetSkill = intent.getTargetSkill();
                
                executor.executeProcedure(callStatement, Map.of(), new ActionListener<>() {
                    @Override
                    public void onResponse(Object result) {
                        try {
                            XContentBuilder builder = XContentFactory.jsonBuilder();
                            builder.startObject();
                            builder.field("matched", true);
                            builder.field("success", true);
                            builder.field("query", userQuery);
                            
                            builder.startObject("intent");
                            builder.field("pattern", matchedPattern);
                            builder.field("target_skill", targetSkill);
                            builder.field("confidence", finalConfidence);
                            builder.endObject();
                            
                            Object finalValue = result;
                            if (result instanceof ExecutionResult) {
                                ExecutionResult execResult = (ExecutionResult) result;
                                finalValue = execResult.getResult();
                                if (execResult.hasPrintOutput()) {
                                    builder.field("output", execResult.getPrintOutput());
                                }
                                builder.field("duration_ms", execResult.getDurationMs());
                            } else if (result instanceof ReturnValue) {
                                finalValue = ((ReturnValue) result).getValue();
                            }
                            
                            addResultField(builder, "result", finalValue);
                            builder.endObject();
                            
                            channel.sendResponse(new RestResponse(RestStatus.OK, builder));
                        } catch (Exception e) {
                            sendError(channel, e);
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        LOGGER.error("Intent execution failed: {}", e.getMessage(), e);
                        try {
                            XContentBuilder builder = XContentFactory.jsonBuilder();
                            builder.startObject();
                            builder.field("matched", true);
                            builder.field("success", false);
                            builder.field("query", userQuery);
                            builder.startObject("intent");
                            builder.field("pattern", matchedPattern);
                            builder.field("target_skill", targetSkill);
                            builder.endObject();
                            builder.field("error", e.getMessage());
                            builder.endObject();
                            channel.sendResponse(new RestResponse(RestStatus.INTERNAL_SERVER_ERROR, builder));
                        } catch (Exception jsonError) {
                            sendError(channel, e);
                        }
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                sendError(channel, e);
            }
        });
    }

    private void addResultField(XContentBuilder builder, String fieldName, Object value) throws IOException {
        if (value == null) {
            builder.nullField(fieldName);
        } else if (value instanceof String) {
            builder.field(fieldName, (String) value);
        } else if (value instanceof Map) {
            builder.field(fieldName, value);
        } else if (value instanceof List) {
            builder.field(fieldName, value);
        } else if (value instanceof Number) {
            builder.field(fieldName, value);
        } else if (value instanceof Boolean) {
            builder.field(fieldName, value);
        } else {
            builder.field(fieldName, value.toString());
        }
    }

    private void sendError(org.elasticsearch.rest.RestChannel channel, Exception e) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            builder.field("error", e.getMessage());
            builder.field("type", e.getClass().getSimpleName());
            builder.endObject();
            channel.sendResponse(new RestResponse(RestStatus.INTERNAL_SERVER_ERROR, builder));
        } catch (Exception jsonError) {
            channel.sendResponse(new RestResponse(RestStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }
}
