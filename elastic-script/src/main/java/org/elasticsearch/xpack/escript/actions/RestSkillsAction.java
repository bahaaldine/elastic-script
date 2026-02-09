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
import org.elasticsearch.xpack.escript.applications.SkillDefinition;
import org.elasticsearch.xpack.escript.executors.ElasticScriptExecutor;
import org.elasticsearch.xpack.escript.primitives.ExecutionResult;
import org.elasticsearch.xpack.escript.primitives.ReturnValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.POST;

/**
 * REST action for AI agent skill invocation.
 * 
 * Endpoints:
 * - GET /_escript/skills - List all available skills (for MCP tool registration)
 * - GET /_escript/skills/{skill_name} - Get skill details
 * - POST /_escript/skills/{skill_name}/_invoke - Invoke a skill with parameters
 * 
 * MCP Tool Format:
 * <pre>
 * {
 *   "tools": [
 *     {
 *       "name": "detect_churn",
 *       "description": "Identifies customers likely to churn",
 *       "inputSchema": {
 *         "type": "object",
 *         "properties": {
 *           "threshold": {"type": "number", "description": "Churn probability threshold"},
 *           "timeframe": {"type": "string", "description": "Analysis timeframe"}
 *         },
 *         "required": ["threshold"]
 *       }
 *     }
 *   ]
 * }
 * </pre>
 */
public class RestSkillsAction extends BaseRestHandler {

    private static final Logger LOGGER = LogManager.getLogger(RestSkillsAction.class);

    private final ApplicationRegistry registry;
    private final ElasticScriptExecutor executor;

    public RestSkillsAction(ApplicationRegistry registry, ElasticScriptExecutor executor) {
        this.registry = registry;
        this.executor = executor;
    }

    @Override
    public List<Route> routes() {
        return List.of(
            Route.builder(GET, "/_escript/skills").build(),
            Route.builder(GET, "/_escript/skills/{skill_name}").build(),
            Route.builder(POST, "/_escript/skills/{skill_name}/_invoke").build()
        );
    }

    @Override
    public String getName() {
        return "elastic_script_skills";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        String skillName = request.param("skill_name");
        
        if (request.method() == GET && skillName == null) {
            // List all skills
            return channel -> listAllSkills(channel);
        } else if (request.method() == GET && skillName != null) {
            // Get skill details
            return channel -> getSkillDetails(skillName, channel);
        } else if (request.method() == POST && skillName != null) {
            // Invoke skill
            Map<String, Object> params = parseParams(request);
            return channel -> invokeSkill(skillName, params, channel);
        }
        
        throw new IllegalArgumentException("Invalid request");
    }

    private Map<String, Object> parseParams(RestRequest request) throws IOException {
        if (!request.hasContentOrSourceParam()) {
            return Map.of();
        }
        
        XContentParser parser = request.contentParser();
        return parser.map();
    }

    private void listAllSkills(org.elasticsearch.rest.RestChannel channel) {
        registry.getAllSkills(new ActionListener<>() {
            @Override
            public void onResponse(List<SkillDefinition> skills) {
                try {
                    XContentBuilder builder = XContentFactory.jsonBuilder();
                    builder.startObject();
                    
                    // MCP tools format
                    builder.startArray("tools");
                    for (SkillDefinition skill : skills) {
                        builder.map(skill.toMcpToolSpec());
                    }
                    builder.endArray();
                    
                    // Also include a simple list for non-MCP consumers
                    builder.startArray("skills");
                    for (SkillDefinition skill : skills) {
                        builder.startObject();
                        builder.field("name", skill.getName());
                        builder.field("description", skill.getDescription());
                        builder.field("return_type", skill.getReturnType());
                        builder.field("procedure", skill.getProcedureName());
                        builder.endObject();
                    }
                    builder.endArray();
                    
                    builder.field("count", skills.size());
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

    private void getSkillDetails(String skillName, org.elasticsearch.rest.RestChannel channel) {
        registry.listApplications(new ActionListener<>() {
            @Override
            public void onResponse(List<ApplicationDefinition> apps) {
                try {
                    SkillDefinition found = null;
                    String appName = null;
                    
                    for (ApplicationDefinition app : apps) {
                        if (app.getStatus() == ApplicationDefinition.Status.RUNNING) {
                            SkillDefinition skill = app.findSkill(skillName);
                            if (skill != null) {
                                found = skill;
                                appName = app.getName();
                                break;
                            }
                        }
                    }
                    
                    if (found == null) {
                        XContentBuilder builder = XContentFactory.jsonBuilder();
                        builder.startObject();
                        builder.field("error", "Skill not found: " + skillName);
                        builder.endObject();
                        channel.sendResponse(new RestResponse(RestStatus.NOT_FOUND, builder));
                        return;
                    }
                    
                    XContentBuilder builder = XContentFactory.jsonBuilder();
                    builder.startObject();
                    builder.field("name", found.getName());
                    builder.field("description", found.getDescription());
                    builder.field("return_type", found.getReturnType());
                    builder.field("procedure", found.getProcedureName());
                    builder.field("application", appName);
                    
                    builder.startArray("parameters");
                    for (SkillDefinition.SkillParameter param : found.getParameters()) {
                        builder.startObject();
                        builder.field("name", param.getName());
                        builder.field("type", param.getType());
                        builder.field("description", param.getDescription());
                        builder.field("required", param.isRequired());
                        if (param.getDefaultValue() != null) {
                            builder.field("default", param.getDefaultValue());
                        }
                        builder.endObject();
                    }
                    builder.endArray();
                    
                    // Include MCP spec
                    builder.field("mcp_spec", found.toMcpToolSpec());
                    
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

    private void invokeSkill(String skillName, Map<String, Object> params, 
                            org.elasticsearch.rest.RestChannel channel) {
        LOGGER.debug("Invoking skill: {} with params: {}", skillName, params);
        
        registry.listApplications(new ActionListener<>() {
            @Override
            public void onResponse(List<ApplicationDefinition> apps) {
                SkillDefinition found = null;
                
                for (ApplicationDefinition app : apps) {
                    if (app.getStatus() == ApplicationDefinition.Status.RUNNING) {
                        SkillDefinition skill = app.findSkill(skillName);
                        if (skill != null) {
                            found = skill;
                            break;
                        }
                    }
                }
                
                if (found == null) {
                    try {
                        XContentBuilder builder = XContentFactory.jsonBuilder();
                        builder.startObject();
                        builder.field("error", "Skill not found: " + skillName);
                        builder.endObject();
                        channel.sendResponse(new RestResponse(RestStatus.NOT_FOUND, builder));
                    } catch (Exception e) {
                        sendError(channel, e);
                    }
                    return;
                }
                
                // Build procedure call
                String procedureName = found.getProcedureName();
                List<String> procArgs = found.getProcedureArgs();
                
                // Build argument list from params
                List<Object> argValues = new ArrayList<>();
                for (String argName : procArgs) {
                    Object value = params.get(argName);
                    if (value == null) {
                        // Check for matching parameter with default
                        for (SkillDefinition.SkillParameter p : found.getParameters()) {
                            if (p.getName().equals(argName) && p.getDefaultValue() != null) {
                                value = p.getDefaultValue();
                                break;
                            }
                        }
                    }
                    argValues.add(value);
                }
                
                // Build CALL statement
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
                LOGGER.debug("Executing skill via: {}", callStatement);
                
                executor.executeProcedure(callStatement, Map.of(), new ActionListener<>() {
                    @Override
                    public void onResponse(Object result) {
                        try {
                            XContentBuilder builder = XContentFactory.jsonBuilder();
                            builder.startObject();
                            builder.field("skill", skillName);
                            builder.field("success", true);
                            
                            // Extract the actual result
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
                        LOGGER.error("Skill invocation failed: {}", e.getMessage(), e);
                        try {
                            XContentBuilder builder = XContentFactory.jsonBuilder();
                            builder.startObject();
                            builder.field("skill", skillName);
                            builder.field("success", false);
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
