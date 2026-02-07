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
import org.elasticsearch.xpack.escript.executors.ElasticScriptExecutor;
import org.elasticsearch.xpack.escript.primitives.ExecutionResult;
import org.elasticsearch.xpack.escript.primitives.ReturnValue;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.elasticsearch.rest.RestRequest.Method.POST;

/**
 * REST action for executing Elastic Script procedures.
 * 
 * Response format when executing a procedure:
 * <pre>
 * {
 *   "result": [the return value],
 *   "output": ["print line 1", "print line 2"],
 *   "_meta": {
 *     "execution_id": "abc123",
 *     "duration_ms": 42,
 *     "procedure_name": "my_proc"
 *   }
 * }
 * </pre>
 */
public class RestRunEScriptAction extends BaseRestHandler {

    private static final Logger LOGGER = LogManager.getLogger(RestRunEScriptAction.class);

    private final ElasticScriptExecutor elasticScriptExecutor;

    public RestRunEScriptAction(ElasticScriptExecutor elasticScriptExecutor) {
        this.elasticScriptExecutor = elasticScriptExecutor;
    }

    @Override
    public List<Route> routes() {
        return List.of(
            Route.builder(POST, "/_escript").build()
        );
    }

    @Override
    public String getName() {
        return "elastic_script_run_procedure";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        if (request.hasContentOrSourceParam() == false) {
            throw new IllegalArgumentException("Request body is required");
        }

        XContentParser parser = request.contentParser();
        String elasticScriptQuery = null;
        Map<String, Object> args = null;

        XContentParser.Token token = parser.nextToken();
        while (token != null) {
            if (token == XContentParser.Token.FIELD_NAME) {
                String fieldName = parser.currentName();
                parser.nextToken();
                if ("query".equals(fieldName)) {
                    elasticScriptQuery = parser.text();
                } else if ("args".equals(fieldName)) {
                    args = parser.map();
                }
            }
            token = parser.nextToken();
        }

        if (elasticScriptQuery == null) {
            throw new IllegalArgumentException("Field [query] must be provided in the body");
        }

        if (args == null) {
            args = new HashMap<>();
        }

        final String finalQuery = elasticScriptQuery;
        final Map<String, Object> finalArgs = args;

        return channel -> {
            elasticScriptExecutor.executeProcedure(finalQuery, finalArgs, new ActionListener<>() {
                @Override
                public void onResponse(Object result) {
                    try {
                        XContentBuilder builder = XContentFactory.jsonBuilder();
                        builder.startObject();
                        
                        // Check if this is an enriched ExecutionResult
                        if (result instanceof ExecutionResult) {
                            ExecutionResult execResult = (ExecutionResult) result;
                            Object finalValue = execResult.getResult();
                            
                            // Add the result field
                            addResultField(builder, finalValue);
                            
                            // Add PRINT output if present
                            if (execResult.hasPrintOutput()) {
                                builder.field("output", execResult.getPrintOutput());
                            }
                            
                            // Add metadata
                            builder.startObject("_meta");
                            if (execResult.getExecutionId() != null) {
                                builder.field("execution_id", execResult.getExecutionId());
                            }
                            builder.field("duration_ms", execResult.getDurationMs());
                            if (execResult.getProcedureName() != null) {
                                builder.field("procedure_name", execResult.getProcedureName());
                            }
                            builder.endObject();
                        } else {
                            // Handle legacy/non-enriched results (CREATE PROCEDURE, etc.)
                            Object finalValue = result;
                            if (result == null) {
                                LOGGER.debug("Result is null (no RETURN statement)");
                            } else {
                                LOGGER.debug("Object instance type: {}", result.getClass().getName());
                                if (result instanceof ReturnValue) {
                                    LOGGER.debug("This is a ReturnValue, extracting getValue()");
                                    finalValue = ((ReturnValue) result).getValue();
                                }
                            }
                            addResultField(builder, finalValue);
                        }

                        builder.endObject();
                        channel.sendResponse(new RestResponse(RestStatus.OK, builder));

                    } catch (Exception e) {
                        channel.sendResponse(new RestResponse(RestStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    LOGGER.error("Elastic-script execution failed: {}", e.getMessage());
                    LOGGER.error("Stack trace:", e);
                    
                    try {
                        XContentBuilder errorBuilder = XContentFactory.jsonBuilder();
                        errorBuilder.startObject();
                        errorBuilder.field("error", e.getMessage());
                        errorBuilder.field("type", e.getClass().getSimpleName());
                        if (e.getCause() != null) {
                            errorBuilder.field("cause", e.getCause().getMessage());
                        }
                        errorBuilder.endObject();
                        channel.sendResponse(new RestResponse(RestStatus.INTERNAL_SERVER_ERROR, errorBuilder));
                    } catch (Exception jsonError) {
                        channel.sendResponse(new RestResponse(RestStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
                    }
                }
                
                private void addResultField(XContentBuilder builder, Object value) throws IOException {
                    if (value == null) {
                        builder.nullField("result");
                    } else if (value instanceof String) {
                        builder.field("result", (String) value);
                    } else if (value instanceof Map) {
                        builder.field("result", value);
                    } else if (value instanceof List) {
                        builder.field("result", value);
                    } else if (value instanceof Number) {
                        builder.field("result", value);
                    } else if (value instanceof Boolean) {
                        builder.field("result", value);
                    } else {
                        builder.field("result", value.toString());
                    }
                }
            });
        };
    }
}
