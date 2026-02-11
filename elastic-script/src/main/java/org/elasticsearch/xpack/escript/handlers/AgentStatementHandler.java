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
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xpack.escript.agents.AgentRuntime;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handler for agent statements in Moltler.
 * Manages autonomous agent creation, execution, and monitoring.
 */
public class AgentStatementHandler {
    
    private static final String AGENTS_INDEX = ".moltler_agents";
    private static final String AGENT_EXECUTIONS_INDEX = ".moltler_agent_executions";
    
    private final Client client;
    private final ExecutionContext context;
    
    public AgentStatementHandler(Client client, ExecutionContext context) {
        this.client = client;
        this.context = context;
    }
    
    /**
     * Handles CREATE AGENT statement.
     */
    public void handleCreateAgent(ElasticScriptParser.Create_agent_statementContext ctx,
                                  ActionListener<Object> listener) {
        try {
            String agentName = ctx.ID().getText();
            String goal = unquoteString(ctx.STRING(0).getText());
            
            // Parse skills list
            List<Map<String, Object>> skills = new ArrayList<>();
            for (ElasticScriptParser.Agent_skill_refContext skillRef : ctx.agent_skill_list().agent_skill_ref()) {
                Map<String, Object> skill = new HashMap<>();
                skill.put("name", skillRef.ID(0).getText());
                if (skillRef.ID().size() > 1) {
                    skill.put("version", skillRef.ID(1).getText());
                }
                if (skillRef.LBRACKET() != null && skillRef.ID().size() > 1) {
                    skill.put("mode", skillRef.ID(1).getText()); // approval, forbidden, etc.
                }
                skills.add(skill);
            }
            
            // Parse execution mode
            final String executionMode;
            if (ctx.agent_execution_mode() != null) {
                if (ctx.agent_execution_mode().AUTONOMOUS() != null) {
                    executionMode = "autonomous";
                } else if (ctx.agent_execution_mode().SUPERVISED() != null) {
                    executionMode = "supervised";
                } else if (ctx.agent_execution_mode().DRY_RUN() != null) {
                    executionMode = "dry_run";
                } else {
                    executionMode = "human_approval";
                }
            } else {
                executionMode = "human_approval"; // default
            }
            
            // Parse triggers
            List<Map<String, Object>> triggers = new ArrayList<>();
            if (ctx.agent_trigger_list() != null) {
                for (ElasticScriptParser.Agent_trigger_defContext triggerDef : ctx.agent_trigger_list().agent_trigger_def()) {
                    Map<String, Object> trigger = new HashMap<>();
                    if (triggerDef.SCHEDULE() != null) {
                        trigger.put("type", "schedule");
                        trigger.put("schedule", unquoteString(triggerDef.STRING().getText()));
                    } else if (triggerDef.ALERT() != null) {
                        trigger.put("type", "alert");
                        if (triggerDef.STRING() != null) {
                            trigger.put("alert", unquoteString(triggerDef.STRING().getText()));
                        } else if (triggerDef.expression() != null) {
                            trigger.put("condition", triggerDef.expression().getText());
                        }
                    } else if (triggerDef.ID() != null) {
                        trigger.put("type", triggerDef.ID().getText().toLowerCase());
                    }
                    triggers.add(trigger);
                }
            }
            
            // Parse model
            String model = null;
            if (ctx.MODEL() != null && ctx.STRING().size() > 1) {
                model = unquoteString(ctx.STRING(1).getText());
            }
            
            // Parse config
            Map<String, Object> config = new HashMap<>();
            if (ctx.documentLiteral() != null) {
                config = parseDocumentLiteral(ctx.documentLiteral());
            }
            
            // Get the agent body (statements)
            String body = ctx.statement().stream()
                .map(s -> s.getText())
                .collect(Collectors.joining("\n"));
            
            // Build agent document
            Map<String, Object> agentDoc = new HashMap<>();
            agentDoc.put("name", agentName);
            agentDoc.put("goal", goal);
            agentDoc.put("skills", skills);
            agentDoc.put("execution_mode", executionMode);
            agentDoc.put("triggers", triggers);
            agentDoc.put("model", model);
            agentDoc.put("config", config);
            agentDoc.put("body", body);
            agentDoc.put("status", "created");
            agentDoc.put("enabled", true);
            agentDoc.put("created_at", System.currentTimeMillis());
            agentDoc.put("updated_at", System.currentTimeMillis());
            
            // Ensure index exists
            ensureAgentsIndexExists(ActionListener.wrap(
                indexCreated -> {
                    // Index the agent
                    IndexRequest indexRequest = new IndexRequest(AGENTS_INDEX)
                        .id(agentName)
                        .source(agentDoc, XContentType.JSON);
                    
                    client.index(indexRequest, ActionListener.wrap(
                        indexResponse -> {
                            Map<String, Object> result = new HashMap<>();
                            result.put("status", "created");
                            result.put("agent", agentName);
                            result.put("goal", goal);
                            result.put("skills_count", skills.size());
                            result.put("execution_mode", executionMode);
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
     * Handles DROP AGENT statement.
     */
    public void handleDropAgent(ElasticScriptParser.Drop_agent_statementContext ctx,
                                ActionListener<Object> listener) {
        try {
            String agentName = ctx.ID().getText();
            
            client.delete(
                new org.elasticsearch.action.delete.DeleteRequest(AGENTS_INDEX, agentName),
                ActionListener.wrap(
                    deleteResponse -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("status", "deleted");
                        result.put("agent", agentName);
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
     * Handles SHOW AGENTS statement.
     */
    public void handleShowAllAgents(ActionListener<Object> listener) {
        SearchRequest searchRequest = new SearchRequest(AGENTS_INDEX);
        searchRequest.source(new SearchSourceBuilder()
            .query(QueryBuilders.matchAllQuery())
            .size(1000));
        
        client.search(searchRequest, ActionListener.wrap(
            response -> {
                List<Map<String, Object>> agents = new ArrayList<>();
                for (SearchHit hit : response.getHits().getHits()) {
                    Map<String, Object> agent = new HashMap<>();
                    Map<String, Object> source = hit.getSourceAsMap();
                    agent.put("name", source.get("name"));
                    agent.put("goal", source.get("goal"));
                    agent.put("status", source.get("status"));
                    agent.put("enabled", source.get("enabled"));
                    agent.put("execution_mode", source.get("execution_mode"));
                    agents.add(agent);
                }
                
                Map<String, Object> result = new HashMap<>();
                result.put("count", agents.size());
                result.put("agents", agents);
                listener.onResponse(result);
            },
            e -> {
                // Return empty list if index doesn't exist
                if (e.getMessage() != null && e.getMessage().contains("no such index")) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("count", 0);
                    result.put("agents", new ArrayList<>());
                    listener.onResponse(result);
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }
    
    /**
     * Handles SHOW AGENT name statement.
     */
    public void handleShowAgentDetail(ElasticScriptParser.ShowAgentDetailContext ctx,
                                      ActionListener<Object> listener) {
        String agentName = ctx.ID().getText();
        
        client.get(
            new org.elasticsearch.action.get.GetRequest(AGENTS_INDEX, agentName),
            ActionListener.wrap(
                getResponse -> {
                    if (getResponse.isExists()) {
                        Map<String, Object> agent = getResponse.getSourceAsMap();
                        agent.put("id", getResponse.getId());
                        listener.onResponse(agent);
                    } else {
                        listener.onFailure(new RuntimeException("Agent not found: " + agentName));
                    }
                },
                listener::onFailure
            )
        );
    }
    
    /**
     * Handles SHOW AGENT name EXECUTION id statement.
     */
    public void handleShowAgentExecution(ElasticScriptParser.ShowAgentExecutionContext ctx,
                                         ActionListener<Object> listener) {
        String agentName = ctx.ID().getText();
        String executionId = unquoteString(ctx.STRING().getText());
        
        client.get(
            new org.elasticsearch.action.get.GetRequest(AGENT_EXECUTIONS_INDEX, executionId),
            ActionListener.wrap(
                getResponse -> {
                    if (getResponse.isExists()) {
                        Map<String, Object> execution = getResponse.getSourceAsMap();
                        execution.put("id", getResponse.getId());
                        listener.onResponse(execution);
                    } else {
                        listener.onFailure(new RuntimeException("Execution not found: " + executionId));
                    }
                },
                listener::onFailure
            )
        );
    }
    
    /**
     * Handles SHOW AGENT name HISTORY statement.
     */
    public void handleShowAgentHistory(ElasticScriptParser.ShowAgentHistoryContext ctx,
                                       ActionListener<Object> listener) {
        String agentName = ctx.ID().getText();
        
        SearchRequest searchRequest = new SearchRequest(AGENT_EXECUTIONS_INDEX);
        searchRequest.source(new SearchSourceBuilder()
            .query(QueryBuilders.termQuery("agent", agentName))
            .size(100)
            .sort("started_at", org.elasticsearch.search.sort.SortOrder.DESC));
        
        client.search(searchRequest, ActionListener.wrap(
            response -> {
                List<Map<String, Object>> executions = new ArrayList<>();
                for (SearchHit hit : response.getHits().getHits()) {
                    Map<String, Object> execution = new HashMap<>();
                    Map<String, Object> source = hit.getSourceAsMap();
                    execution.put("id", hit.getId());
                    execution.put("status", source.get("status"));
                    execution.put("started_at", source.get("started_at"));
                    execution.put("ended_at", source.get("ended_at"));
                    execution.put("duration_ms", source.get("duration_ms"));
                    execution.put("actions_count", source.get("actions_count"));
                    executions.add(execution);
                }
                
                Map<String, Object> result = new HashMap<>();
                result.put("agent", agentName);
                result.put("count", executions.size());
                result.put("executions", executions);
                listener.onResponse(result);
            },
            e -> {
                if (e.getMessage() != null && e.getMessage().contains("no such index")) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("agent", agentName);
                    result.put("count", 0);
                    result.put("executions", new ArrayList<>());
                    listener.onResponse(result);
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }
    
    /**
     * Handles ALTER AGENT statement for config changes.
     */
    public void handleAlterAgentConfig(ElasticScriptParser.AlterAgentConfigContext ctx,
                                       ActionListener<Object> listener) {
        try {
            String agentName = ctx.ID().getText();
            Map<String, Object> newConfig = parseDocumentLiteral(ctx.documentLiteral());
            
            updateAgent(agentName, "config", newConfig, listener);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Handles ALTER AGENT statement for execution mode changes.
     */
    public void handleAlterAgentExecution(ElasticScriptParser.AlterAgentExecutionContext ctx,
                                          ActionListener<Object> listener) {
        try {
            String agentName = ctx.ID().getText();
            String executionMode = getExecutionModeString(ctx.agent_execution_mode());
            
            updateAgent(agentName, "execution_mode", executionMode, listener);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Handles ENABLE/DISABLE AGENT statement.
     */
    public void handleEnableDisableAgent(ElasticScriptParser.EnableDisableAgentContext ctx,
                                         ActionListener<Object> listener) {
        try {
            String agentName = ctx.ID().getText();
            boolean enabled = ctx.ENABLE() != null;
            
            updateAgent(agentName, "enabled", enabled, listener);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Handles TRIGGER AGENT statement.
     * Uses the AgentRuntime to execute the OODA loop.
     */
    public void handleTriggerAgent(ElasticScriptParser.Trigger_agent_statementContext ctx,
                                   ActionListener<Object> listener) {
        try {
            String agentName = ctx.ID().getText();
            
            Map<String, Object> triggerContext = new HashMap<>();
            if (ctx.documentLiteral() != null) {
                triggerContext = parseDocumentLiteral(ctx.documentLiteral());
            }
            
            // Add manual trigger metadata
            triggerContext.put("trigger_type", "manual");
            triggerContext.put("triggered_at", System.currentTimeMillis());
            
            // Use AgentRuntime for execution
            AgentRuntime runtime = new AgentRuntime(client);
            runtime.execute(agentName, triggerContext, ActionListener.wrap(
                result -> listener.onResponse(result),
                listener::onFailure
            ));
            
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    // Helper methods
    
    private void ensureAgentsIndexExists(ActionListener<Boolean> listener) {
        client.admin().indices().prepareGetIndex(TimeValue.timeValueSeconds(30)).setIndices(AGENTS_INDEX).execute(ActionListener.wrap(
            response -> listener.onResponse(true),
            e -> {
                if (e instanceof IndexNotFoundException) {
                    CreateIndexRequest createRequest = new CreateIndexRequest(AGENTS_INDEX);
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
    
    private void updateAgent(String agentName, String field, Object value, ActionListener<Object> listener) {
        client.get(
            new org.elasticsearch.action.get.GetRequest(AGENTS_INDEX, agentName),
            ActionListener.wrap(
                getResponse -> {
                    if (getResponse.isExists()) {
                        Map<String, Object> agent = new HashMap<>(getResponse.getSourceAsMap());
                        agent.put(field, value);
                        agent.put("updated_at", System.currentTimeMillis());
                        
                        IndexRequest indexRequest = new IndexRequest(AGENTS_INDEX)
                            .id(agentName)
                            .source(agent, XContentType.JSON);
                        
                        client.index(indexRequest, ActionListener.wrap(
                            indexResponse -> {
                                Map<String, Object> result = new HashMap<>();
                                result.put("status", "updated");
                                result.put("agent", agentName);
                                result.put(field, value);
                                listener.onResponse(result);
                            },
                            listener::onFailure
                        ));
                    } else {
                        listener.onFailure(new RuntimeException("Agent not found: " + agentName));
                    }
                },
                listener::onFailure
            )
        );
    }
    
    private String getExecutionModeString(ElasticScriptParser.Agent_execution_modeContext ctx) {
        if (ctx.AUTONOMOUS() != null) return "autonomous";
        if (ctx.SUPERVISED() != null) return "supervised";
        if (ctx.DRY_RUN() != null) return "dry_run";
        return "human_approval";
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
        String text = ctx.getText();
        
        if ((text.startsWith("'") && text.endsWith("'")) || 
            (text.startsWith("\"") && text.endsWith("\""))) {
            return unquoteString(text);
        }
        
        if (text.equalsIgnoreCase("true")) return true;
        if (text.equalsIgnoreCase("false")) return false;
        
        try {
            if (text.contains(".")) {
                return Double.parseDouble(text);
            } else {
                return Long.parseLong(text);
            }
        } catch (NumberFormatException e) {
            // Not a number
        }
        
        if (text.equalsIgnoreCase("null")) return null;
        
        return text;
    }
}
