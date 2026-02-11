/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xpack.escript.connectors.ConnectorFactory;
import org.elasticsearch.xpack.escript.connectors.SyncEngine;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for connector statements in Moltler.
 * Manages external data source connections (GitHub, Jira, Datadog, etc.)
 */
public class ConnectorStatementHandler {
    
    private static final String CONNECTORS_INDEX = ".moltler_connectors";
    
    private final Client client;
    private final ExecutionContext context;
    
    public ConnectorStatementHandler(Client client, ExecutionContext context) {
        this.client = client;
        this.context = context;
    }
    
    /**
     * Handles CREATE CONNECTOR statement.
     * Creates a new connector configuration in Elasticsearch.
     */
    public void handleCreateConnector(ElasticScriptParser.Create_connector_statementContext ctx, 
                                      ActionListener<Object> listener) {
        try {
            String connectorName = ctx.ID().getText();
            String connectorType = unquoteString(ctx.STRING().getText());
            
            // Parse CONFIG document
            Map<String, Object> config = parseDocumentLiteral(ctx.documentLiteral(0));
            
            // Parse OPTIONS if present
            Map<String, Object> options = new HashMap<>();
            if (ctx.documentLiteral().size() > 1) {
                options = parseDocumentLiteral(ctx.documentLiteral(1));
            }
            
            // Build connector document
            Map<String, Object> connectorDoc = new HashMap<>();
            connectorDoc.put("name", connectorName);
            connectorDoc.put("type", connectorType);
            connectorDoc.put("config", config);
            connectorDoc.put("options", options);
            connectorDoc.put("status", "created");
            connectorDoc.put("created_at", System.currentTimeMillis());
            connectorDoc.put("updated_at", System.currentTimeMillis());
            
            // Ensure index exists
            ensureConnectorsIndexExists(ActionListener.wrap(
                indexCreated -> {
                    // Index the connector
                    IndexRequest indexRequest = new IndexRequest(CONNECTORS_INDEX)
                        .id(connectorName)
                        .source(connectorDoc, XContentType.JSON);
                    
                    client.index(indexRequest, ActionListener.wrap(
                        indexResponse -> {
                            Map<String, Object> result = new HashMap<>();
                            result.put("status", "created");
                            result.put("connector", connectorName);
                            result.put("type", connectorType);
                            listener.onResponse(result);
                        },
                        listener::onFailure
                    ));
                },
                listener::onFailure
            ));
            
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Handles DROP CONNECTOR statement.
     */
    public void handleDropConnector(ElasticScriptParser.Drop_connector_statementContext ctx,
                                    ActionListener<Object> listener) {
        try {
            String connectorName = ctx.ID().getText();
            
            client.delete(
                new org.elasticsearch.action.delete.DeleteRequest(CONNECTORS_INDEX, connectorName),
                ActionListener.wrap(
                    deleteResponse -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("status", "deleted");
                        result.put("connector", connectorName);
                        listener.onResponse(result);
                    },
                    listener::onFailure
                )
            );
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Handles SHOW CONNECTORS statement.
     */
    public void handleShowAllConnectors(ActionListener<Object> listener) {
        SearchRequest searchRequest = new SearchRequest(CONNECTORS_INDEX);
        searchRequest.source(new SearchSourceBuilder()
            .query(QueryBuilders.matchAllQuery())
            .size(1000));
        
        client.search(searchRequest, ActionListener.wrap(
            response -> {
                List<Map<String, Object>> connectors = new ArrayList<>();
                for (SearchHit hit : response.getHits().getHits()) {
                    Map<String, Object> connector = hit.getSourceAsMap();
                    connector.put("id", hit.getId());
                    connectors.add(connector);
                }
                
                Map<String, Object> result = new HashMap<>();
                result.put("count", connectors.size());
                result.put("connectors", connectors);
                listener.onResponse(result);
            },
            e -> {
                // Return empty list if index doesn't exist
                if (e.getMessage() != null && e.getMessage().contains("no such index")) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("count", 0);
                    result.put("connectors", new ArrayList<>());
                    listener.onResponse(result);
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }
    
    /**
     * Handles SHOW CONNECTOR name statement.
     */
    public void handleShowConnectorDetail(ElasticScriptParser.ShowConnectorDetailContext ctx,
                                          ActionListener<Object> listener) {
        String connectorName = ctx.ID().getText();
        
        client.get(
            new org.elasticsearch.action.get.GetRequest(CONNECTORS_INDEX, connectorName),
            ActionListener.wrap(
                getResponse -> {
                    if (getResponse.isExists()) {
                        Map<String, Object> connector = getResponse.getSourceAsMap();
                        connector.put("id", getResponse.getId());
                        listener.onResponse(connector);
                    } else {
                        listener.onFailure(new RuntimeException("Connector not found: " + connectorName));
                    }
                },
                listener::onFailure
            )
        );
    }
    
    /**
     * Handles SHOW CONNECTOR name STATUS statement.
     */
    public void handleShowConnectorStatus(ElasticScriptParser.ShowConnectorStatusContext ctx,
                                          ActionListener<Object> listener) {
        String connectorName = ctx.ID().getText();
        
        client.get(
            new org.elasticsearch.action.get.GetRequest(CONNECTORS_INDEX, connectorName),
            ActionListener.wrap(
                getResponse -> {
                    if (getResponse.isExists()) {
                        Map<String, Object> source = getResponse.getSourceAsMap();
                        Map<String, Object> status = new HashMap<>();
                        status.put("connector", connectorName);
                        status.put("type", source.get("type"));
                        status.put("status", source.getOrDefault("status", "unknown"));
                        status.put("last_sync", source.get("last_sync"));
                        status.put("error", source.get("last_error"));
                        listener.onResponse(status);
                    } else {
                        listener.onFailure(new RuntimeException("Connector not found: " + connectorName));
                    }
                },
                listener::onFailure
            )
        );
    }
    
    /**
     * Handles TEST CONNECTOR statement.
     */
    public void handleTestConnector(ElasticScriptParser.Test_connector_statementContext ctx,
                                    ActionListener<Object> listener) {
        String connectorName = ctx.ID().getText();
        
        // Get connector config and test connection
        client.get(
            new org.elasticsearch.action.get.GetRequest(CONNECTORS_INDEX, connectorName),
            ActionListener.wrap(
                getResponse -> {
                    if (getResponse.isExists()) {
                        Map<String, Object> connector = getResponse.getSourceAsMap();
                        String connectorType = (String) connector.get("type");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> config = (Map<String, Object>) connector.get("config");
                        
                        // Use ConnectorFactory to test the connection
                        ConnectorFactory.testConnection(connectorType, config, ActionListener.wrap(
                            testResult -> {
                                testResult.put("connector", connectorName);
                                listener.onResponse(testResult);
                            },
                            listener::onFailure
                        ));
                    } else {
                        listener.onFailure(new RuntimeException("Connector not found: " + connectorName));
                    }
                },
                e -> {
                    if (e instanceof IndexNotFoundException) {
                        listener.onFailure(new RuntimeException("Connector not found: " + connectorName));
                    } else {
                        listener.onFailure(e);
                    }
                }
            )
        );
    }
    
    /**
     * Handles SYNC CONNECTOR statement.
     */
    public void handleSyncConnector(ElasticScriptParser.Sync_connector_statementContext ctx,
                                    ActionListener<Object> listener) {
        try {
            // Parse connector.entity reference
            ElasticScriptParser.Connector_entity_refContext refCtx = ctx.connector_entity_ref();
            String connectorName = refCtx.ID(0).getText();
            String entity = refCtx.ID().size() > 1 ? refCtx.ID(1).getText() : null;
            
            String targetIndex = unquoteString(ctx.STRING(0).getText());
            
            // Check for incremental sync
            String incrementalField = null;
            if (ctx.INCREMENTAL() != null && ctx.ID() != null) {
                incrementalField = ctx.ID().getText();
            }
            
            // Check for schedule
            String schedule = null;
            if (ctx.SCHEDULE() != null && ctx.STRING().size() > 1) {
                schedule = unquoteString(ctx.STRING(1).getText());
            }
            
            // Look up connector configuration
            final String finalEntity = entity;
            final String finalIncrementalField = incrementalField;
            final String finalSchedule = schedule;
            
            client.get(
                new org.elasticsearch.action.get.GetRequest(CONNECTORS_INDEX, connectorName),
                ActionListener.wrap(
                    getResponse -> {
                        if (getResponse.isExists()) {
                            Map<String, Object> connector = getResponse.getSourceAsMap();
                            String connectorType = (String) connector.get("type");
                            @SuppressWarnings("unchecked")
                            Map<String, Object> config = (Map<String, Object>) connector.get("config");
                            
                            // Use the SyncEngine to perform the sync
                            SyncEngine syncEngine = new SyncEngine(client);
                            syncEngine.sync(
                                connectorName, 
                                finalEntity != null ? finalEntity : "default", 
                                connectorType, 
                                config, 
                                targetIndex, 
                                finalIncrementalField,
                                ActionListener.wrap(
                                    syncResult -> {
                                        // Add schedule info to result
                                        if (finalSchedule != null) {
                                            syncResult.put("schedule", finalSchedule);
                                            syncResult.put("message", "Sync completed. Schedule configured for future runs.");
                                        }
                                        listener.onResponse(syncResult);
                                    },
                                    listener::onFailure
                                )
                            );
                        } else {
                            listener.onFailure(new RuntimeException("Connector not found: " + connectorName));
                        }
                    },
                    e -> {
                        if (e instanceof IndexNotFoundException) {
                            listener.onFailure(new RuntimeException("Connector not found: " + connectorName));
                        } else {
                            listener.onFailure(e);
                        }
                    }
                )
            );
            
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Handles ALTER CONNECTOR statement.
     */
    public void handleAlterConnectorOptions(ElasticScriptParser.AlterConnectorOptionsContext ctx,
                                            ActionListener<Object> listener) {
        try {
            String connectorName = ctx.ID().getText();
            Map<String, Object> newOptions = parseDocumentLiteral(ctx.documentLiteral());
            
            // Get existing connector and update options
            client.get(
                new org.elasticsearch.action.get.GetRequest(CONNECTORS_INDEX, connectorName),
                ActionListener.wrap(
                    getResponse -> {
                        if (getResponse.isExists()) {
                            Map<String, Object> connector = new HashMap<>(getResponse.getSourceAsMap());
                            connector.put("options", newOptions);
                            connector.put("updated_at", System.currentTimeMillis());
                            
                            IndexRequest indexRequest = new IndexRequest(CONNECTORS_INDEX)
                                .id(connectorName)
                                .source(connector, XContentType.JSON);
                            
                            client.index(indexRequest, ActionListener.wrap(
                                indexResponse -> {
                                    Map<String, Object> result = new HashMap<>();
                                    result.put("status", "updated");
                                    result.put("connector", connectorName);
                                    listener.onResponse(result);
                                },
                                listener::onFailure
                            ));
                        } else {
                            listener.onFailure(new RuntimeException("Connector not found: " + connectorName));
                        }
                    },
                    listener::onFailure
                )
            );
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Handles ALTER CONNECTOR name ENABLE/DISABLE statement.
     */
    public void handleAlterConnectorEnableDisable(ElasticScriptParser.AlterConnectorEnableDisableContext ctx,
                                                   ActionListener<Object> listener) {
        try {
            String connectorName = ctx.ID().getText();
            boolean enabled = ctx.ENABLE() != null;
            
            // Get existing connector and update status
            client.get(
                new org.elasticsearch.action.get.GetRequest(CONNECTORS_INDEX, connectorName),
                ActionListener.wrap(
                    getResponse -> {
                        if (getResponse.isExists()) {
                            Map<String, Object> connector = new HashMap<>(getResponse.getSourceAsMap());
                            connector.put("status", enabled ? "enabled" : "disabled");
                            connector.put("updated_at", System.currentTimeMillis());
                            
                            IndexRequest indexRequest = new IndexRequest(CONNECTORS_INDEX)
                                .id(connectorName)
                                .source(connector, XContentType.JSON);
                            
                            client.index(indexRequest, ActionListener.wrap(
                                indexResponse -> {
                                    Map<String, Object> result = new HashMap<>();
                                    result.put("status", enabled ? "enabled" : "disabled");
                                    result.put("connector", connectorName);
                                    listener.onResponse(result);
                                },
                                listener::onFailure
                            ));
                        } else {
                            listener.onFailure(new RuntimeException("Connector not found: " + connectorName));
                        }
                    },
                    listener::onFailure
                )
            );
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Handles EXEC connector_name.action(args) statement.
     * Executes a connector action and returns the result.
     */
    public void handleExecConnector(ElasticScriptParser.Exec_connector_statementContext ctx,
                                    ActionListener<Object> listener) {
        try {
            ElasticScriptParser.Connector_action_callContext actionCall = ctx.connector_action_call();
            String connectorName = actionCall.ID(0).getText();
            String actionName = actionCall.ID(1).getText();
            
            // Parse arguments
            Map<String, Object> args = new HashMap<>();
            List<Object> positionalArgs = new ArrayList<>();
            
            if (actionCall.connector_args() != null) {
                for (ElasticScriptParser.Connector_argContext argCtx : actionCall.connector_args().connector_arg()) {
                    if (argCtx.ID() != null) {
                        // Named argument
                        String argName = argCtx.ID().getText();
                        Object argValue = evaluateExpression(argCtx.expression());
                        args.put(argName, argValue);
                    } else {
                        // Positional argument
                        positionalArgs.add(evaluateExpression(argCtx.expression()));
                    }
                }
            }
            
            // Look up connector configuration
            client.get(
                new org.elasticsearch.action.get.GetRequest(CONNECTORS_INDEX, connectorName),
                ActionListener.wrap(
                    getResponse -> {
                        if (getResponse.isExists()) {
                            Map<String, Object> connector = getResponse.getSourceAsMap();
                            String connectorType = (String) connector.get("type");
                            @SuppressWarnings("unchecked")
                            Map<String, Object> config = (Map<String, Object>) connector.get("config");
                            
                            // Execute the connector action
                            executeConnectorAction(connectorType, actionName, config, args, positionalArgs, listener);
                        } else {
                            listener.onFailure(new RuntimeException("Connector not found: " + connectorName));
                        }
                    },
                    e -> {
                        if (e instanceof IndexNotFoundException) {
                            listener.onFailure(new RuntimeException("Connector not found: " + connectorName));
                        } else {
                            listener.onFailure(e);
                        }
                    }
                )
            );
            
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Handles QUERY connector_name.entity WHERE condition statement.
     * Queries data from a connector entity.
     */
    public void handleQueryConnector(ElasticScriptParser.Query_connector_statementContext ctx,
                                     ActionListener<Object> listener) {
        try {
            String connectorName = ctx.ID(0).getText();
            String entityName = ctx.ID(1).getText();
            
            // Parse WHERE condition if present
            String whereCondition = null;
            if (ctx.WHERE_CMD() != null && ctx.expression() != null && !ctx.expression().isEmpty()) {
                whereCondition = ctx.expression(0).getText();
            }
            
            // Parse LIMIT if present
            Integer limit = null;
            if (ctx.LIMIT() != null && ctx.INT() != null) {
                limit = Integer.parseInt(ctx.INT().getText());
            }
            
            final String finalWhereCondition = whereCondition;
            final Integer finalLimit = limit;
            
            // Look up connector configuration
            client.get(
                new org.elasticsearch.action.get.GetRequest(CONNECTORS_INDEX, connectorName),
                ActionListener.wrap(
                    getResponse -> {
                        if (getResponse.isExists()) {
                            Map<String, Object> connector = getResponse.getSourceAsMap();
                            String connectorType = (String) connector.get("type");
                            @SuppressWarnings("unchecked")
                            Map<String, Object> config = (Map<String, Object>) connector.get("config");
                            
                            // Query the connector entity
                            queryConnectorEntity(connectorType, entityName, config, finalWhereCondition, finalLimit, listener);
                        } else {
                            listener.onFailure(new RuntimeException("Connector not found: " + connectorName));
                        }
                    },
                    e -> {
                        if (e instanceof IndexNotFoundException) {
                            listener.onFailure(new RuntimeException("Connector not found: " + connectorName));
                        } else {
                            listener.onFailure(e);
                        }
                    }
                )
            );
            
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Execute a connector action based on connector type.
     * Uses ConnectorFactory to dispatch to the appropriate connector implementation.
     */
    private void executeConnectorAction(String connectorType, String actionName, 
                                        Map<String, Object> config,
                                        Map<String, Object> namedArgs,
                                        List<Object> positionalArgs,
                                        ActionListener<Object> listener) {
        // Use ConnectorFactory to execute the action
        ConnectorFactory.executeAction(connectorType, actionName, config, namedArgs, positionalArgs, listener);
    }
    
    /**
     * Query a connector entity based on connector type.
     * Uses ConnectorFactory to dispatch to the appropriate connector implementation.
     */
    private void queryConnectorEntity(String connectorType, String entityName,
                                      Map<String, Object> config,
                                      String whereCondition, Integer limit,
                                      ActionListener<Object> listener) {
        // Use ConnectorFactory to query the entity
        ConnectorFactory.queryEntity(connectorType, entityName, config, whereCondition, limit,
            ActionListener.wrap(
                data -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("status", "success");
                    result.put("connector_type", connectorType);
                    result.put("entity", entityName);
                    result.put("count", data.size());
                    result.put("data", data);
                    listener.onResponse(result);
                },
                listener::onFailure
            )
        );
    }
    
    // Helper methods
    
    private void ensureConnectorsIndexExists(ActionListener<Boolean> listener) {
        client.admin().indices().prepareGetIndex(TimeValue.timeValueSeconds(30)).setIndices(CONNECTORS_INDEX).execute(ActionListener.wrap(
            response -> listener.onResponse(true),
            e -> {
                if (e instanceof IndexNotFoundException) {
                    CreateIndexRequest createRequest = new CreateIndexRequest(CONNECTORS_INDEX);
                    client.admin().indices().create(createRequest, ActionListener.wrap(
                        response -> listener.onResponse(true),
                        listener::onFailure
                    ));
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }
    
    private String unquoteString(String s) {
        if (s == null) return null;
        if ((s.startsWith("'") && s.endsWith("'")) || (s.startsWith("\"") && s.endsWith("\""))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
    
    private Map<String, Object> parseDocumentLiteral(ElasticScriptParser.DocumentLiteralContext ctx) {
        Map<String, Object> doc = new HashMap<>();
        if (ctx != null && ctx.documentField() != null) {
            for (ElasticScriptParser.DocumentFieldContext field : ctx.documentField()) {
                String key = unquoteString(field.STRING().getText());
                Object value = evaluateExpression(field.expression());
                doc.put(key, value);
            }
        }
        return doc;
    }
    
    private Object evaluateExpression(ElasticScriptParser.ExpressionContext ctx) {
        // Simple expression evaluation for literals
        String text = ctx.getText();
        
        // String literal
        if ((text.startsWith("'") && text.endsWith("'")) || 
            (text.startsWith("\"") && text.endsWith("\""))) {
            return unquoteString(text);
        }
        
        // Boolean
        if (text.equalsIgnoreCase("true")) return true;
        if (text.equalsIgnoreCase("false")) return false;
        
        // Number
        try {
            if (text.contains(".")) {
                return Double.parseDouble(text);
            } else {
                return Long.parseLong(text);
            }
        } catch (NumberFormatException e) {
            // Not a number
        }
        
        // Null
        if (text.equalsIgnoreCase("null")) return null;
        
        // Return as string for complex expressions
        return text;
    }
}
