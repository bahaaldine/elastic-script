/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.agents;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages triggers for Moltler agents.
 * 
 * Triggers cause agents to execute based on:
 * - CRON schedules (periodic execution)
 * - ES|QL alerts (Elasticsearch query conditions)
 * - Webhooks (external HTTP calls)
 * - Manual invocation
 * 
 * The TriggerManager evaluates triggers and dispatches to AgentRuntime.
 */
public class TriggerManager {
    
    private static final String AGENTS_INDEX = ".moltler_agents";
    
    private final Client client;
    private final AgentRuntime agentRuntime;
    
    // Active scheduled triggers
    private final Map<String, ScheduledTrigger> scheduledTriggers = new ConcurrentHashMap<>();
    
    public TriggerManager(Client client, AgentRuntime agentRuntime) {
        this.client = client;
        this.agentRuntime = agentRuntime;
    }
    
    /**
     * Handle an incoming webhook trigger.
     * 
     * @param webhookId The webhook identifier
     * @param payload The webhook payload
     * @param listener Callback with triggered agents
     */
    public void handleWebhook(String webhookId, Map<String, Object> payload,
                             ActionListener<List<Map<String, Object>>> listener) {
        // Find agents with webhook triggers matching this ID
        findAgentsWithTrigger("webhook", webhookId, ActionListener.wrap(
            agents -> {
                if (agents.isEmpty()) {
                    listener.onResponse(List.of());
                    return;
                }
                
                // Trigger all matching agents
                List<Map<String, Object>> results = new ArrayList<>();
                triggerAgents(agents, payload, results, listener);
            },
            listener::onFailure
        ));
    }
    
    /**
     * Handle an ES|QL alert trigger.
     * 
     * @param alertName The alert name
     * @param alertContext Context from the alert
     * @param listener Callback with triggered agents
     */
    public void handleAlert(String alertName, Map<String, Object> alertContext,
                           ActionListener<List<Map<String, Object>>> listener) {
        // Find agents with alert triggers matching this alert
        findAgentsWithTrigger("alert", alertName, ActionListener.wrap(
            agents -> {
                if (agents.isEmpty()) {
                    listener.onResponse(List.of());
                    return;
                }
                
                // Add alert-specific context
                Map<String, Object> triggerContext = new HashMap<>(alertContext);
                triggerContext.put("trigger_type", "alert");
                triggerContext.put("alert_name", alertName);
                
                // Trigger all matching agents
                List<Map<String, Object>> results = new ArrayList<>();
                triggerAgents(agents, triggerContext, results, listener);
            },
            listener::onFailure
        ));
    }
    
    /**
     * Handle manual trigger of an agent.
     * 
     * @param agentName The agent to trigger
     * @param context Optional context
     * @param listener Callback with execution result
     */
    public void triggerManually(String agentName, Map<String, Object> context,
                               ActionListener<Map<String, Object>> listener) {
        Map<String, Object> triggerContext = context != null ? new HashMap<>(context) : new HashMap<>();
        triggerContext.put("trigger_type", "manual");
        triggerContext.put("triggered_at", System.currentTimeMillis());
        
        agentRuntime.execute(agentName, triggerContext, listener);
    }
    
    /**
     * Evaluate all CRON triggers.
     * Called periodically by a scheduler.
     */
    public void evaluateCronTriggers(ActionListener<List<Map<String, Object>>> listener) {
        // Find all agents with CRON triggers
        findAgentsWithTriggerType("cron", ActionListener.wrap(
            agents -> {
                List<Map<String, Object>> results = new ArrayList<>();
                long now = System.currentTimeMillis();
                
                for (Map<String, Object> agent : agents) {
                    String agentName = (String) agent.get("name");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> trigger = (Map<String, Object>) agent.get("trigger");
                    String cronExpr = (String) trigger.get("schedule");
                    
                    // Check if should run now
                    if (shouldRunCron(agentName, cronExpr, now)) {
                        Map<String, Object> triggerContext = new HashMap<>();
                        triggerContext.put("trigger_type", "cron");
                        triggerContext.put("schedule", cronExpr);
                        triggerContext.put("triggered_at", now);
                        
                        // Execute asynchronously
                        agentRuntime.execute(agentName, triggerContext, ActionListener.wrap(
                            result -> {
                                results.add(result);
                                recordLastRun(agentName, now);
                            },
                            e -> {
                                Map<String, Object> errorResult = new HashMap<>();
                                errorResult.put("agent", agentName);
                                errorResult.put("error", e.getMessage());
                                results.add(errorResult);
                            }
                        ));
                    }
                }
                
                listener.onResponse(results);
            },
            listener::onFailure
        ));
    }
    
    private void findAgentsWithTrigger(String triggerType, String triggerValue,
                                       ActionListener<List<Map<String, Object>>> listener) {
        SearchRequest searchRequest = new SearchRequest(AGENTS_INDEX);
        searchRequest.source(new SearchSourceBuilder()
            .query(QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("enabled", true))
                .must(QueryBuilders.termQuery("trigger.type", triggerType))
                .must(QueryBuilders.termQuery("trigger.value", triggerValue)))
            .size(100));
        
        client.search(searchRequest, ActionListener.wrap(
            response -> {
                List<Map<String, Object>> agents = new ArrayList<>();
                for (SearchHit hit : response.getHits().getHits()) {
                    Map<String, Object> agent = hit.getSourceAsMap();
                    agent.put("name", hit.getId());
                    agents.add(agent);
                }
                listener.onResponse(agents);
            },
            e -> {
                if (e instanceof org.elasticsearch.index.IndexNotFoundException) {
                    listener.onResponse(List.of());
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }
    
    private void findAgentsWithTriggerType(String triggerType,
                                           ActionListener<List<Map<String, Object>>> listener) {
        SearchRequest searchRequest = new SearchRequest(AGENTS_INDEX);
        searchRequest.source(new SearchSourceBuilder()
            .query(QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("enabled", true))
                .must(QueryBuilders.termQuery("trigger.type", triggerType)))
            .size(1000));
        
        client.search(searchRequest, ActionListener.wrap(
            response -> {
                List<Map<String, Object>> agents = new ArrayList<>();
                for (SearchHit hit : response.getHits().getHits()) {
                    Map<String, Object> agent = hit.getSourceAsMap();
                    agent.put("name", hit.getId());
                    agents.add(agent);
                }
                listener.onResponse(agents);
            },
            e -> {
                if (e instanceof org.elasticsearch.index.IndexNotFoundException) {
                    listener.onResponse(List.of());
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }
    
    private void triggerAgents(List<Map<String, Object>> agents, Map<String, Object> context,
                              List<Map<String, Object>> results,
                              ActionListener<List<Map<String, Object>>> listener) {
        if (agents.isEmpty()) {
            listener.onResponse(results);
            return;
        }
        
        Map<String, Object> agent = agents.remove(0);
        String agentName = (String) agent.get("name");
        
        agentRuntime.execute(agentName, context, ActionListener.wrap(
            result -> {
                results.add(result);
                triggerAgents(agents, context, results, listener);
            },
            e -> {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("agent", agentName);
                errorResult.put("error", e.getMessage());
                results.add(errorResult);
                triggerAgents(agents, context, results, listener);
            }
        ));
    }
    
    private boolean shouldRunCron(String agentName, String cronExpr, long now) {
        // Simple CRON evaluation
        // For production, use a proper CRON library
        ScheduledTrigger scheduled = scheduledTriggers.get(agentName);
        if (scheduled == null) {
            // First run
            scheduledTriggers.put(agentName, new ScheduledTrigger(cronExpr, now));
            return true;
        }
        
        // Parse interval from simple CRON-like expression
        long intervalMs = parseInterval(cronExpr);
        if (intervalMs <= 0) {
            return false;
        }
        
        // Check if enough time has passed
        if (now - scheduled.lastRun >= intervalMs) {
            return true;
        }
        
        return false;
    }
    
    private long parseInterval(String cronExpr) {
        // Simple interval parsing for expressions like:
        // "every 5 minutes", "every 1 hour", "every 30 seconds"
        cronExpr = cronExpr.toLowerCase().trim();
        
        if (cronExpr.startsWith("every ")) {
            String rest = cronExpr.substring(6);
            String[] parts = rest.split(" ");
            if (parts.length >= 2) {
                try {
                    int value = Integer.parseInt(parts[0]);
                    String unit = parts[1];
                    
                    if (unit.startsWith("second")) {
                        return value * 1000L;
                    } else if (unit.startsWith("minute")) {
                        return value * 60 * 1000L;
                    } else if (unit.startsWith("hour")) {
                        return value * 60 * 60 * 1000L;
                    } else if (unit.startsWith("day")) {
                        return value * 24 * 60 * 60 * 1000L;
                    }
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        
        // Standard CRON format would need a proper parser
        return -1;
    }
    
    private void recordLastRun(String agentName, long timestamp) {
        ScheduledTrigger scheduled = scheduledTriggers.get(agentName);
        if (scheduled != null) {
            scheduled.lastRun = timestamp;
        }
    }
    
    /**
     * Tracks scheduled trigger state.
     */
    private static class ScheduledTrigger {
        final String cronExpr;
        long lastRun;
        
        ScheduledTrigger(String cronExpr, long initialRun) {
            this.cronExpr = cronExpr;
            this.lastRun = initialRun;
        }
    }
    
    /**
     * Parse trigger specification from grammar.
     * 
     * @param triggerType "cron", "esql", "webhook", "manual"
     * @param triggerSpec The trigger specification
     * @return Parsed trigger configuration
     */
    public static Map<String, Object> parseTrigger(String triggerType, String triggerSpec) {
        Map<String, Object> trigger = new HashMap<>();
        trigger.put("type", triggerType);
        
        switch (triggerType.toLowerCase()) {
            case "cron":
                trigger.put("schedule", triggerSpec);
                break;
            case "esql":
            case "alert":
                trigger.put("query", triggerSpec);
                break;
            case "webhook":
                trigger.put("value", triggerSpec);
                break;
            case "manual":
            default:
                trigger.put("value", "manual");
                break;
        }
        
        return trigger;
    }
}
