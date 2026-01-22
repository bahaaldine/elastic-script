/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.escript.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.xpack.escript.logging.EScriptLogger;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;
import org.elasticsearch.xpack.escript.evaluators.ExpressionEvaluator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for first-class Elasticsearch commands.
 * These are language-level statements that provide cleaner syntax for core ES operations.
 * 
 * Supported commands:
 * - INDEX document INTO 'index-name';
 * - DELETE FROM 'index-name' WHERE condition;
 * - SEARCH 'index-name' QUERY {...};
 * - REFRESH 'index-name';
 * - CREATE INDEX 'name' WITH {...};
 */
public class FirstClassCommandsHandler {

    private final ProcedureExecutor executor;
    private final Client client;
    private final ExpressionEvaluator evaluator;

    public FirstClassCommandsHandler(ProcedureExecutor executor, Client client) {
        this.executor = executor;
        this.client = client;
        this.evaluator = new ExpressionEvaluator(executor);
    }

    /**
     * Handles INDEX document INTO 'index-name';
     */
    @SuppressWarnings("unchecked")
    public void handleIndexCommand(ElasticScriptParser.Index_commandContext ctx, ActionListener<Object> listener) {
        String executionId = executor.getContext().getExecutionId();
        
        // Evaluate the document expression
        evaluator.evaluateExpressionAsync(ctx.expression(), ActionListener.wrap(
            docResult -> {
                if (!(docResult instanceof Map)) {
                    listener.onFailure(new RuntimeException(
                        "INDEX command requires a document (Map), got: " + 
                        (docResult != null ? docResult.getClass().getSimpleName() : "null")));
                    return;
                }
                
                Map<String, Object> document = (Map<String, Object>) docResult;
                
                // Get the index name
                String indexName = getIndexName(ctx.index_target());
                
                EScriptLogger.statementExec(executionId, "INDEX", "indexing document into '" + indexName + "'");
                
                client.prepareIndex(indexName).setSource(document).execute(ActionListener.wrap(
                    resp -> {
                        Map<String, Object> resultMap = Map.of(
                            "id", resp.getId(),
                            "index", resp.getIndex(),
                            "result", resp.getResult().getLowercase()
                        );
                        EScriptLogger.statementExec(executionId, "INDEX", "document indexed with id=" + resp.getId());
                        listener.onResponse(resultMap);
                    },
                    listener::onFailure
                ));
            },
            listener::onFailure
        ));
    }

    /**
     * Handles DELETE FROM 'index-name' WHERE condition;
     * Currently supports DELETE by _id only for safety.
     */
    @SuppressWarnings("unchecked")
    public void handleDeleteCommand(ElasticScriptParser.Delete_commandContext ctx, ActionListener<Object> listener) {
        String executionId = executor.getContext().getExecutionId();
        String indexName = getIndexName(ctx.index_target());
        
        // Evaluate the WHERE condition expression
        evaluator.evaluateExpressionAsync(ctx.expression(), ActionListener.wrap(
            conditionResult -> {
                // The condition should evaluate to a document ID string for now
                // In the future, we can support more complex query-based deletes
                String docId;
                if (conditionResult instanceof String) {
                    docId = (String) conditionResult;
                } else if (conditionResult instanceof Boolean && (Boolean) conditionResult) {
                    // If the condition is just 'true', we need a different approach
                    listener.onFailure(new RuntimeException(
                        "DELETE command requires a specific document ID or condition that yields an ID. " +
                        "Use: DELETE FROM 'index' WHERE _id == 'doc-id';"));
                    return;
                } else {
                    listener.onFailure(new RuntimeException(
                        "DELETE WHERE condition should resolve to a document ID (STRING), got: " +
                        (conditionResult != null ? conditionResult.getClass().getSimpleName() : "null")));
                    return;
                }
                
                EScriptLogger.statementExec(executionId, "DELETE", "deleting document '" + docId + "' from '" + indexName + "'");
                
                DeleteRequest deleteRequest = new DeleteRequest(indexName, docId);
                client.delete(deleteRequest, ActionListener.wrap(
                    resp -> {
                        Map<String, Object> resultMap = Map.of(
                            "id", resp.getId(),
                            "index", resp.getIndex(),
                            "result", resp.getResult().getLowercase()
                        );
                        EScriptLogger.statementExec(executionId, "ES_CMD", "DELETE command: document deleted, result=" + resp.getResult().getLowercase());
                        listener.onResponse(resultMap);
                    },
                    listener::onFailure
                ));
            },
            listener::onFailure
        ));
    }

    /**
     * Handles SEARCH 'index-name' QUERY {...};
     */
    @SuppressWarnings("unchecked")
    public void handleSearchCommand(ElasticScriptParser.Search_commandContext ctx, ActionListener<Object> listener) {
        String executionId = executor.getContext().getExecutionId();
        String indexName = getIndexName(ctx.index_target());
        
        // Evaluate the query expression (should be a document/map)
        evaluator.evaluateExpressionAsync(ctx.expression(), ActionListener.wrap(
            queryResult -> {
                if (!(queryResult instanceof Map)) {
                    listener.onFailure(new RuntimeException(
                        "SEARCH QUERY requires a query document (Map), got: " +
                        (queryResult != null ? queryResult.getClass().getSimpleName() : "null")));
                    return;
                }
                
                Map<String, Object> queryDoc = (Map<String, Object>) queryResult;
                
                EScriptLogger.statementExec(executionId, "ES_CMD", "SEARCH command: searching '" + indexName + "'");
                
                try {
                    // Convert the query document to JSON and use it as the query
                    SearchRequest searchRequest = new SearchRequest(indexName);
                    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
                    
                    // Use the query document directly with wrapperQuery
                    String queryJson = convertMapToJson(queryDoc);
                    sourceBuilder.query(QueryBuilders.wrapperQuery(queryJson));
                    searchRequest.source(sourceBuilder);
                    
                    client.search(searchRequest, ActionListener.wrap(
                        resp -> {
                            List<Map<String, Object>> hits = new ArrayList<>();
                            resp.getHits().forEach(hit -> {
                                Map<String, Object> doc = new HashMap<>(hit.getSourceAsMap());
                                doc.put("_id", hit.getId());
                                doc.put("_index", hit.getIndex());
                                doc.put("_score", hit.getScore());
                                hits.add(doc);
                            });
                            
                            EScriptLogger.statementExec(executionId, "ES_CMD", "SEARCH command: found " + hits.size() + " documents");
                            listener.onResponse(hits);
                        },
                        listener::onFailure
                    ));
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("Failed to execute search: " + e.getMessage(), e));
                }
            },
            listener::onFailure
        ));
    }

    /**
     * Handles REFRESH 'index-name';
     */
    public void handleRefreshCommand(ElasticScriptParser.Refresh_commandContext ctx, ActionListener<Object> listener) {
        String executionId = executor.getContext().getExecutionId();
        String indexName = getIndexName(ctx.index_target());
        
        EScriptLogger.statementExec(executionId, "ES_CMD", "REFRESH command: refreshing '" + indexName + "'");
        
        RefreshRequest refreshRequest = new RefreshRequest(indexName);
        client.admin().indices().refresh(refreshRequest, ActionListener.wrap(
            resp -> {
                Map<String, Object> resultMap = Map.of(
                    "shards", resp.getTotalShards(),
                    "successful", resp.getSuccessfulShards(),
                    "failed", resp.getFailedShards()
                );
                EScriptLogger.statementExec(executionId, "ES_CMD", "REFRESH command: completed, " + resp.getSuccessfulShards() + " shards successful");
                listener.onResponse(resultMap);
            },
            listener::onFailure
        ));
    }

    /**
     * Handles CREATE INDEX 'name' WITH {...};
     */
    @SuppressWarnings("unchecked")
    public void handleCreateIndexCommand(ElasticScriptParser.Create_index_commandContext ctx, ActionListener<Object> listener) {
        String executionId = executor.getContext().getExecutionId();
        String indexName = getIndexName(ctx.index_target());
        
        EScriptLogger.statementExec(executionId, "ES_CMD", "CREATE INDEX command: creating index '" + indexName + "'");
        
        CreateIndexRequest createRequest = new CreateIndexRequest(indexName);
        
        // Check if we have options (WITH clause)
        if (ctx.create_index_options() != null) {
            ElasticScriptParser.Create_index_optionsContext opts = ctx.create_index_options();
            
            // Determine which expressions to evaluate based on the option type
            if (opts.MAPPINGS() != null && opts.SETTINGS() != null) {
                // WITH SETTINGS {...} MAPPINGS {...}
                evaluateTwoExpressions(opts.expression(0), opts.expression(1), ActionListener.wrap(
                    results -> {
                        Map<String, Object> settings = (Map<String, Object>) results[0];
                        Map<String, Object> mappings = (Map<String, Object>) results[1];
                        Map<String, Object> fullBody = Map.of("settings", settings, "mappings", mappings);
                        createRequest.source(convertMapToJson(fullBody), XContentType.JSON);
                        executeCreateIndex(createRequest, executionId, listener);
                    },
                    listener::onFailure
                ));
            } else if (opts.SETTINGS() != null) {
                // WITH SETTINGS {...}
                evaluator.evaluateExpressionAsync(opts.expression(0), ActionListener.wrap(
                    settingsResult -> {
                        Map<String, Object> settings = (Map<String, Object>) settingsResult;
                        Map<String, Object> fullBody = Map.of("settings", settings);
                        createRequest.source(convertMapToJson(fullBody), XContentType.JSON);
                        executeCreateIndex(createRequest, executionId, listener);
                    },
                    listener::onFailure
                ));
            } else if (opts.MAPPINGS() != null) {
                // WITH MAPPINGS {...}
                evaluator.evaluateExpressionAsync(opts.expression(0), ActionListener.wrap(
                    mappingsResult -> {
                        Map<String, Object> mappings = (Map<String, Object>) mappingsResult;
                        Map<String, Object> fullBody = Map.of("mappings", mappings);
                        createRequest.source(convertMapToJson(fullBody), XContentType.JSON);
                        executeCreateIndex(createRequest, executionId, listener);
                    },
                    listener::onFailure
                ));
            } else {
                // WITH {...} - full body (may contain mappings, settings, aliases, etc.)
                evaluator.evaluateExpressionAsync(opts.expression(0), ActionListener.wrap(
                    bodyResult -> {
                        Map<String, Object> body = (Map<String, Object>) bodyResult;
                        createRequest.source(convertMapToJson(body), XContentType.JSON);
                        executeCreateIndex(createRequest, executionId, listener);
                    },
                    listener::onFailure
                ));
            }
        } else {
            // No options, just create empty index
            executeCreateIndex(createRequest, executionId, listener);
        }
    }

    private void executeCreateIndex(CreateIndexRequest request, String executionId, ActionListener<Object> listener) {
        client.admin().indices().create(request, ActionListener.wrap(
            resp -> {
                Map<String, Object> resultMap = Map.of(
                    "acknowledged", resp.isAcknowledged(),
                    "index", resp.index()
                );
                EScriptLogger.statementExec(executionId, "ES_CMD", "CREATE INDEX command: index created, acknowledged=" + resp.isAcknowledged());
                listener.onResponse(resultMap);
            },
            listener::onFailure
        ));
    }

    /**
     * Helper to evaluate two expressions in sequence.
     */
    private void evaluateTwoExpressions(
            ElasticScriptParser.ExpressionContext expr1,
            ElasticScriptParser.ExpressionContext expr2,
            ActionListener<Object[]> listener) {
        
        evaluator.evaluateExpressionAsync(expr1, ActionListener.wrap(
            result1 -> evaluator.evaluateExpressionAsync(expr2, ActionListener.wrap(
                result2 -> listener.onResponse(new Object[]{result1, result2}),
                listener::onFailure
            )),
            listener::onFailure
        ));
    }

    /**
     * Extracts the index name from an index_target context.
     */
    private String getIndexName(ElasticScriptParser.Index_targetContext ctx) {
        if (ctx.STRING() != null) {
            // Remove quotes from string literal
            String quoted = ctx.STRING().getText();
            return quoted.substring(1, quoted.length() - 1);
        } else if (ctx.ID() != null) {
            // It's a variable reference - get value from context
            String varName = ctx.ID().getText();
            Object value = executor.getContext().getVariable(varName);
            if (value instanceof String) {
                return (String) value;
            } else {
                throw new RuntimeException("Variable '" + varName + "' does not contain a string index name");
            }
        }
        throw new RuntimeException("Invalid index target");
    }

    /**
     * Converts a Map to a JSON string.
     */
    private String convertMapToJson(Map<String, Object> map) {
        try {
            StringBuilder sb = new StringBuilder();
            appendJsonObject(sb, map);
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert map to JSON: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private void appendJsonObject(StringBuilder sb, Map<String, Object> map) {
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
            appendJsonValue(sb, entry.getValue());
        }
        sb.append("}");
    }

    @SuppressWarnings("unchecked")
    private void appendJsonValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            sb.append("\"").append(escapeJson((String) value)).append("\"");
        } else if (value instanceof Number || value instanceof Boolean) {
            sb.append(value);
        } else if (value instanceof Map) {
            appendJsonObject(sb, (Map<String, Object>) value);
        } else if (value instanceof List) {
            sb.append("[");
            boolean first = true;
            for (Object item : (List<?>) value) {
                if (!first) sb.append(",");
                first = false;
                appendJsonValue(sb, item);
            }
            sb.append("]");
        } else {
            sb.append("\"").append(escapeJson(value.toString())).append("\"");
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
