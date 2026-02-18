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
import org.elasticsearch.xpack.escript.applications.SkillDefinition;
import org.elasticsearch.xpack.escript.applications.SkillRegistry;
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
 * REST action implementing Model Context Protocol (MCP) for AI agent integration.
 * 
 * MCP is a standard protocol for exposing tools to AI agents. This endpoint
 * implements the JSON-RPC based MCP specification.
 * 
 * Endpoints:
 * - POST /_escript/mcp - JSON-RPC endpoint for MCP protocol
 * - GET  /_escript/mcp - Returns MCP server info
 * 
 * Supported MCP Methods:
 * - initialize: Initialize the MCP session
 * - tools/list: List all available tools (skills)
 * - tools/call: Invoke a tool with arguments
 * 
 * Example request (tools/list):
 * <pre>
 * POST /_escript/mcp
 * {
 *   "jsonrpc": "2.0",
 *   "method": "tools/list",
 *   "id": 1
 * }
 * </pre>
 * 
 * Example request (tools/call):
 * <pre>
 * POST /_escript/mcp
 * {
 *   "jsonrpc": "2.0",
 *   "method": "tools/call",
 *   "params": {
 *     "name": "metrics_summary",
 *     "arguments": {}
 *   },
 *   "id": 2
 * }
 * </pre>
 */
public class RestMcpAction extends BaseRestHandler {

    private static final Logger LOGGER = LogManager.getLogger(RestMcpAction.class);
    private static final String MCP_VERSION = "2024-11-05";
    private static final String SERVER_NAME = "moltler-elasticsearch";
    private static final String SERVER_VERSION = "1.0.0";

    private final ElasticScriptExecutor executor;

    public RestMcpAction(ElasticScriptExecutor executor) {
        this.executor = executor;
    }

    @Override
    public List<Route> routes() {
        return List.of(
            Route.builder(GET, "/_escript/mcp").build(),
            Route.builder(POST, "/_escript/mcp").build()
        );
    }

    @Override
    public String getName() {
        return "elastic_script_mcp";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        if (request.method() == GET) {
            // Return server info
            return channel -> {
                try {
                    XContentBuilder builder = XContentFactory.jsonBuilder();
                    builder.startObject();
                    builder.field("name", SERVER_NAME);
                    builder.field("version", SERVER_VERSION);
                    builder.field("protocolVersion", MCP_VERSION);
                    builder.startObject("capabilities");
                    builder.startObject("tools");
                    builder.field("listChanged", false);
                    builder.endObject();
                    builder.endObject();
                    builder.endObject();
                    channel.sendResponse(new RestResponse(RestStatus.OK, builder));
                } catch (Exception e) {
                    sendError(channel, null, -32603, e.getMessage());
                }
            };
        }

        // POST - JSON-RPC request
        Map<String, Object> jsonRpc = parseJsonRpc(request);
        String method = (String) jsonRpc.get("method");
        Object id = jsonRpc.get("id");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) jsonRpc.getOrDefault("params", Map.of());

        return channel -> {
            switch (method) {
                case "initialize":
                    handleInitialize(channel, id, params);
                    break;
                case "tools/list":
                    handleToolsList(channel, id, client);
                    break;
                case "tools/call":
                    handleToolsCall(channel, id, params, client);
                    break;
                case "ping":
                    handlePing(channel, id);
                    break;
                default:
                    sendError(channel, id, -32601, "Method not found: " + method);
            }
        };
    }

    private Map<String, Object> parseJsonRpc(RestRequest request) throws IOException {
        if (!request.hasContentOrSourceParam()) {
            throw new IllegalArgumentException("Request body required");
        }
        XContentParser parser = request.contentParser();
        return parser.map();
    }

    /**
     * Handle MCP initialize request
     */
    private void handleInitialize(org.elasticsearch.rest.RestChannel channel, Object id, Map<String, Object> params) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            builder.field("jsonrpc", "2.0");
            builder.field("id", id);
            builder.startObject("result");
            builder.field("protocolVersion", MCP_VERSION);
            builder.startObject("serverInfo");
            builder.field("name", SERVER_NAME);
            builder.field("version", SERVER_VERSION);
            builder.endObject();
            builder.startObject("capabilities");
            builder.startObject("tools");
            builder.field("listChanged", false);
            builder.endObject();
            builder.endObject();
            builder.endObject();
            builder.endObject();
            channel.sendResponse(new RestResponse(RestStatus.OK, builder));
        } catch (Exception e) {
            sendError(channel, id, -32603, e.getMessage());
        }
    }

    /**
     * Handle MCP tools/list request - returns all skills as MCP tools
     */
    private void handleToolsList(org.elasticsearch.rest.RestChannel channel, Object id, NodeClient client) {
        SkillRegistry registry = new SkillRegistry(client);
        
        registry.listSkills(new ActionListener<>() {
            @Override
            public void onResponse(List<SkillDefinition> skills) {
                try {
                    XContentBuilder builder = XContentFactory.jsonBuilder();
                    builder.startObject();
                    builder.field("jsonrpc", "2.0");
                    builder.field("id", id);
                    builder.startObject("result");
                    builder.startArray("tools");
                    
                    for (SkillDefinition skill : skills) {
                        builder.startObject();
                        builder.field("name", skill.getName());
                        builder.field("description", skill.getDescription());
                        
                        // Build input schema
                        builder.startObject("inputSchema");
                        builder.field("type", "object");
                        builder.startObject("properties");
                        
                        List<String> required = new ArrayList<>();
                        for (SkillDefinition.SkillParameter param : skill.getParameters()) {
                            builder.startObject(param.getName());
                            builder.field("type", mapTypeToJsonSchema(param.getType()));
                            if (param.getDescription() != null) {
                                builder.field("description", param.getDescription());
                            }
                            builder.endObject();
                            
                            if (param.isRequired() && param.getDefaultValue() == null) {
                                required.add(param.getName());
                            }
                        }
                        
                        builder.endObject(); // properties
                        builder.array("required", required.toArray(new String[0]));
                        builder.endObject(); // inputSchema
                        
                        builder.endObject(); // tool
                    }
                    
                    builder.endArray(); // tools
                    builder.endObject(); // result
                    builder.endObject();
                    
                    channel.sendResponse(new RestResponse(RestStatus.OK, builder));
                } catch (Exception e) {
                    sendError(channel, id, -32603, e.getMessage());
                }
            }

            @Override
            public void onFailure(Exception e) {
                LOGGER.warn("Failed to list skills for MCP", e);
                sendError(channel, id, -32603, "Failed to list tools: " + e.getMessage());
            }
        });
    }

    /**
     * Handle MCP tools/call request - invokes a skill
     */
    private void handleToolsCall(org.elasticsearch.rest.RestChannel channel, Object id, 
                                  Map<String, Object> params, NodeClient client) {
        String toolName = (String) params.get("name");
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) params.getOrDefault("arguments", Map.of());
        
        if (toolName == null || toolName.isEmpty()) {
            sendError(channel, id, -32602, "Missing required parameter: name");
            return;
        }
        
        LOGGER.debug("MCP tools/call: {} with args: {}", toolName, arguments);
        
        // Look up the skill and invoke it
        SkillRegistry registry = new SkillRegistry(client);
        
        registry.getSkill(toolName, ActionListener.wrap(
            optSkill -> {
                if (optSkill.isEmpty()) {
                    sendError(channel, id, -32602, "Tool not found: " + toolName);
                    return;
                }
                
                SkillDefinition skill = optSkill.get();
                
                // Build CALL statement with arguments
                StringBuilder callBuilder = new StringBuilder();
                callBuilder.append("CALL ").append(skill.getProcedureName()).append("(");
                
                List<String> procArgs = skill.getProcedureArgs();
                for (int i = 0; i < procArgs.size(); i++) {
                    if (i > 0) callBuilder.append(", ");
                    String argName = procArgs.get(i);
                    Object value = arguments.get(argName);
                    
                    // Use default if not provided
                    boolean isDefaultValue = false;
                    if (value == null) {
                        for (SkillDefinition.SkillParameter p : skill.getParameters()) {
                            if (p.getName().equals(argName) && p.getDefaultValue() != null) {
                                value = p.getDefaultValue();
                                isDefaultValue = true;
                                break;
                            }
                        }
                    }
                    
                    if (value == null) {
                        callBuilder.append("NULL");
                    } else if (isDefaultValue) {
                        // Default values are already properly formatted (e.g., 'logs-sample' or 10)
                        callBuilder.append(value);
                    } else if (value instanceof String) {
                        callBuilder.append("'").append(value.toString().replace("'", "''")).append("'");
                    } else {
                        callBuilder.append(value);
                    }
                }
                callBuilder.append(")");
                
                String callStatement = callBuilder.toString();
                LOGGER.debug("MCP executing: {}", callStatement);
                
                executor.executeProcedure(callStatement, Map.of(), new ActionListener<>() {
                    @Override
                    public void onResponse(Object result) {
                        try {
                            Object finalValue = result;
                            if (result instanceof ExecutionResult) {
                                ExecutionResult execResult = (ExecutionResult) result;
                                finalValue = execResult.getResult();
                            } else if (result instanceof ReturnValue) {
                                finalValue = ((ReturnValue) result).getValue();
                            }
                            
                            XContentBuilder builder = XContentFactory.jsonBuilder();
                            builder.startObject();
                            builder.field("jsonrpc", "2.0");
                            builder.field("id", id);
                            builder.startObject("result");
                            builder.startArray("content");
                            builder.startObject();
                            builder.field("type", "text");
                            
                            // Convert result to text
                            String textResult;
                            if (finalValue == null) {
                                textResult = "null";
                            } else if (finalValue instanceof Map || finalValue instanceof List) {
                                XContentBuilder jsonBuilder = XContentFactory.jsonBuilder();
                                jsonBuilder.value(finalValue);
                                textResult = org.elasticsearch.common.Strings.toString(jsonBuilder);
                            } else {
                                textResult = finalValue.toString();
                            }
                            builder.field("text", textResult);
                            
                            builder.endObject();
                            builder.endArray(); // content
                            builder.field("isError", false);
                            builder.endObject(); // result
                            builder.endObject();
                            
                            channel.sendResponse(new RestResponse(RestStatus.OK, builder));
                        } catch (Exception e) {
                            sendError(channel, id, -32603, e.getMessage());
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        LOGGER.warn("MCP tool execution failed: {}", toolName, e);
                        try {
                            XContentBuilder builder = XContentFactory.jsonBuilder();
                            builder.startObject();
                            builder.field("jsonrpc", "2.0");
                            builder.field("id", id);
                            builder.startObject("result");
                            builder.startArray("content");
                            builder.startObject();
                            builder.field("type", "text");
                            builder.field("text", "Error: " + e.getMessage());
                            builder.endObject();
                            builder.endArray();
                            builder.field("isError", true);
                            builder.endObject();
                            builder.endObject();
                            channel.sendResponse(new RestResponse(RestStatus.OK, builder));
                        } catch (Exception jsonError) {
                            sendError(channel, id, -32603, e.getMessage());
                        }
                    }
                });
            },
            e -> sendError(channel, id, -32603, "Failed to get tool: " + e.getMessage())
        ));
    }

    /**
     * Handle MCP ping request
     */
    private void handlePing(org.elasticsearch.rest.RestChannel channel, Object id) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            builder.field("jsonrpc", "2.0");
            builder.field("id", id);
            builder.startObject("result");
            builder.endObject();
            builder.endObject();
            channel.sendResponse(new RestResponse(RestStatus.OK, builder));
        } catch (Exception e) {
            sendError(channel, id, -32603, e.getMessage());
        }
    }

    /**
     * Map elastic-script types to JSON Schema types
     */
    private String mapTypeToJsonSchema(String esType) {
        if (esType == null) return "string";
        switch (esType.toUpperCase()) {
            case "NUMBER":
            case "INTEGER":
            case "LONG":
            case "DOUBLE":
            case "FLOAT":
                return "number";
            case "BOOLEAN":
                return "boolean";
            case "ARRAY":
                return "array";
            case "DOCUMENT":
            case "OBJECT":
            case "MAP":
                return "object";
            default:
                return "string";
        }
    }

    /**
     * Send JSON-RPC error response
     */
    private void sendError(org.elasticsearch.rest.RestChannel channel, Object id, int code, String message) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            builder.field("jsonrpc", "2.0");
            if (id != null) {
                builder.field("id", id);
            }
            builder.startObject("error");
            builder.field("code", code);
            builder.field("message", message);
            builder.endObject();
            builder.endObject();
            channel.sendResponse(new RestResponse(RestStatus.OK, builder));
        } catch (Exception e) {
            channel.sendResponse(new RestResponse(RestStatus.INTERNAL_SERVER_ERROR, "Internal error"));
        }
    }
}
