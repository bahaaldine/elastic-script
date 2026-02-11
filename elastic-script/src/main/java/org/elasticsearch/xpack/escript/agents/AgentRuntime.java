/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.agents;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.XContentType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agent Runtime for Moltler.
 * 
 * Executes autonomous agents based on the OODA loop:
 * - Observe: Gather context and data
 * - Orient: Analyze situation and determine appropriate action
 * - Decide: Select skill(s) to execute
 * - Act: Execute the skill and record results
 * 
 * Execution Modes:
 * - AUTONOMOUS: Execute without human intervention
 * - SUPERVISED: Execute with logging, can be stopped
 * - HUMAN_APPROVAL: Pause before each action for approval
 * - DRY_RUN: Show what would happen without executing
 */
public class AgentRuntime {
    
    private static final String AGENTS_INDEX = ".moltler_agents";
    private static final String EXECUTIONS_INDEX = ".moltler_agent_executions";
    
    private final Client client;
    
    public AgentRuntime(Client client) {
        this.client = client;
    }
    
    /**
     * Execute an agent based on a trigger.
     * 
     * @param agentName The name of the agent to execute
     * @param triggerContext Optional context from the trigger
     * @param listener Callback with execution results
     */
    public void execute(String agentName, Map<String, Object> triggerContext,
                       ActionListener<Map<String, Object>> listener) {
        // Load agent definition
        loadAgent(agentName, ActionListener.wrap(
            agent -> {
                if (agent == null) {
                    listener.onFailure(new RuntimeException("Agent not found: " + agentName));
                    return;
                }
                
                // Check if agent is enabled
                Boolean enabled = (Boolean) agent.get("enabled");
                if (enabled == null || !enabled) {
                    listener.onFailure(new RuntimeException("Agent is disabled: " + agentName));
                    return;
                }
                
                // Create execution context
                String executionId = UUID.randomUUID().toString();
                AgentExecutionContext ctx = new AgentExecutionContext(
                    executionId,
                    agentName,
                    agent,
                    triggerContext,
                    (String) agent.get("execution_mode")
                );
                
                // Start execution
                startExecution(ctx, listener);
            },
            listener::onFailure
        ));
    }
    
    private void loadAgent(String agentName, ActionListener<Map<String, Object>> listener) {
        client.get(
            new org.elasticsearch.action.get.GetRequest(AGENTS_INDEX, agentName),
            ActionListener.wrap(
                response -> {
                    if (response.isExists()) {
                        listener.onResponse(response.getSourceAsMap());
                    } else {
                        listener.onResponse(null);
                    }
                },
                e -> {
                    if (e instanceof org.elasticsearch.index.IndexNotFoundException) {
                        listener.onResponse(null);
                    } else {
                        listener.onFailure(e);
                    }
                }
            )
        );
    }
    
    private void startExecution(AgentExecutionContext ctx,
                               ActionListener<Map<String, Object>> listener) {
        // Record execution start
        Map<String, Object> execution = new HashMap<>();
        execution.put("execution_id", ctx.executionId);
        execution.put("agent_name", ctx.agentName);
        execution.put("goal", ctx.agent.get("goal"));
        execution.put("execution_mode", ctx.executionMode);
        execution.put("trigger_context", ctx.triggerContext);
        execution.put("status", "started");
        execution.put("started_at", System.currentTimeMillis());
        execution.put("steps", new ArrayList<Map<String, Object>>());
        
        // Record to index
        IndexRequest indexRequest = new IndexRequest(EXECUTIONS_INDEX)
            .id(ctx.executionId)
            .source(execution, XContentType.JSON);
        
        client.index(indexRequest, ActionListener.wrap(
            indexResponse -> {
                // Execute the OODA loop
                executeOODALoop(ctx, execution, listener);
            },
            listener::onFailure
        ));
    }
    
    private void executeOODALoop(AgentExecutionContext ctx, Map<String, Object> execution,
                                ActionListener<Map<String, Object>> listener) {
        // Phase 1: OBSERVE
        observe(ctx, ActionListener.wrap(
            observations -> {
                // Record observation step
                recordStep(ctx, execution, "observe", observations);
                
                // Phase 2: ORIENT
                orient(ctx, observations, ActionListener.wrap(
                    orientation -> {
                        recordStep(ctx, execution, "orient", orientation);
                        
                        // Phase 3: DECIDE
                        decide(ctx, orientation, ActionListener.wrap(
                            decision -> {
                                recordStep(ctx, execution, "decide", decision);
                                
                                // Phase 4: ACT
                                act(ctx, decision, ActionListener.wrap(
                                    actionResult -> {
                                        recordStep(ctx, execution, "act", actionResult);
                                        
                                        // Complete execution
                                        completeExecution(ctx, execution, actionResult, listener);
                                    },
                                    e -> failExecution(ctx, execution, "act", e, listener)
                                ));
                            },
                            e -> failExecution(ctx, execution, "decide", e, listener)
                        ));
                    },
                    e -> failExecution(ctx, execution, "orient", e, listener)
                ));
            },
            e -> failExecution(ctx, execution, "observe", e, listener)
        ));
    }
    
    /**
     * OBSERVE phase: Gather context and data.
     */
    private void observe(AgentExecutionContext ctx,
                        ActionListener<Map<String, Object>> listener) {
        Map<String, Object> observations = new HashMap<>();
        
        // Add trigger context
        if (ctx.triggerContext != null) {
            observations.put("trigger", ctx.triggerContext);
        }
        
        // Add agent goal
        observations.put("goal", ctx.agent.get("goal"));
        
        // Add available skills
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> skills = (List<Map<String, Object>>) ctx.agent.get("skills");
        observations.put("available_skills", skills);
        
        // Add execution mode
        observations.put("execution_mode", ctx.executionMode);
        
        // Add timestamp
        observations.put("observed_at", System.currentTimeMillis());
        
        // For DRY_RUN, mark as simulated
        if ("dry_run".equals(ctx.executionMode)) {
            observations.put("simulated", true);
        }
        
        listener.onResponse(observations);
    }
    
    /**
     * ORIENT phase: Analyze situation and context.
     */
    private void orient(AgentExecutionContext ctx, Map<String, Object> observations,
                       ActionListener<Map<String, Object>> listener) {
        Map<String, Object> orientation = new HashMap<>();
        
        String goal = (String) observations.get("goal");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> skills = (List<Map<String, Object>>) observations.get("available_skills");
        
        // Analyze which skills might be applicable
        List<String> applicableSkills = new ArrayList<>();
        if (skills != null) {
            for (Map<String, Object> skill : skills) {
                applicableSkills.add((String) skill.get("name"));
            }
        }
        
        orientation.put("goal_analysis", "Goal: " + goal);
        orientation.put("applicable_skills", applicableSkills);
        orientation.put("context_summary", "Agent oriented based on " + 
            (ctx.triggerContext != null ? "trigger context" : "manual invocation"));
        
        // Determine urgency/priority
        orientation.put("priority", "normal");
        
        listener.onResponse(orientation);
    }
    
    /**
     * DECIDE phase: Select skill(s) to execute.
     */
    private void decide(AgentExecutionContext ctx, Map<String, Object> orientation,
                       ActionListener<Map<String, Object>> listener) {
        Map<String, Object> decision = new HashMap<>();
        
        @SuppressWarnings("unchecked")
        List<String> applicableSkills = (List<String>) orientation.get("applicable_skills");
        
        if (applicableSkills == null || applicableSkills.isEmpty()) {
            decision.put("action", "no_action");
            decision.put("reason", "No applicable skills found");
        } else {
            // For now, select the first applicable skill
            String selectedSkill = applicableSkills.get(0);
            
            decision.put("action", "execute_skill");
            decision.put("skill", selectedSkill);
            decision.put("reason", "Selected skill based on goal alignment");
            decision.put("parameters", Map.of()); // TODO: Determine parameters
            
            // Check if approval is needed
            if ("human_approval".equals(ctx.executionMode)) {
                decision.put("requires_approval", true);
                decision.put("approval_status", "pending");
            } else {
                decision.put("requires_approval", false);
            }
        }
        
        listener.onResponse(decision);
    }
    
    /**
     * ACT phase: Execute the selected skill.
     */
    private void act(AgentExecutionContext ctx, Map<String, Object> decision,
                    ActionListener<Map<String, Object>> listener) {
        Map<String, Object> actionResult = new HashMap<>();
        
        String action = (String) decision.get("action");
        
        if ("no_action".equals(action)) {
            actionResult.put("status", "skipped");
            actionResult.put("reason", decision.get("reason"));
            listener.onResponse(actionResult);
            return;
        }
        
        // Check for dry run mode
        if ("dry_run".equals(ctx.executionMode)) {
            actionResult.put("status", "simulated");
            actionResult.put("skill", decision.get("skill"));
            actionResult.put("message", "DRY_RUN mode - skill would be executed: " + decision.get("skill"));
            actionResult.put("simulated_result", "Success (simulated)");
            listener.onResponse(actionResult);
            return;
        }
        
        // Check for human approval
        Boolean requiresApproval = (Boolean) decision.get("requires_approval");
        if (requiresApproval != null && requiresApproval) {
            String approvalStatus = (String) decision.get("approval_status");
            if ("pending".equals(approvalStatus)) {
                actionResult.put("status", "awaiting_approval");
                actionResult.put("skill", decision.get("skill"));
                actionResult.put("message", "Execution paused - waiting for human approval");
                listener.onResponse(actionResult);
                return;
            }
        }
        
        // Execute the skill
        String skillName = (String) decision.get("skill");
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) decision.get("parameters");
        
        // TODO: Actually execute the skill using SkillRegistry and ProcedureExecutor
        // For now, return a placeholder result
        actionResult.put("status", "executed");
        actionResult.put("skill", skillName);
        actionResult.put("parameters", parameters);
        actionResult.put("result", "Skill execution framework ready - " +
            "actual execution requires skill procedure implementation");
        actionResult.put("executed_at", System.currentTimeMillis());
        
        listener.onResponse(actionResult);
    }
    
    @SuppressWarnings("unchecked")
    private void recordStep(AgentExecutionContext ctx, Map<String, Object> execution,
                           String phase, Map<String, Object> data) {
        Map<String, Object> step = new HashMap<>();
        step.put("phase", phase);
        step.put("timestamp", System.currentTimeMillis());
        step.put("data", data);
        
        List<Map<String, Object>> steps = (List<Map<String, Object>>) execution.get("steps");
        if (steps == null) {
            steps = new ArrayList<>();
            execution.put("steps", steps);
        }
        steps.add(step);
    }
    
    private void completeExecution(AgentExecutionContext ctx, Map<String, Object> execution,
                                  Map<String, Object> finalResult,
                                  ActionListener<Map<String, Object>> listener) {
        execution.put("status", "completed");
        execution.put("completed_at", System.currentTimeMillis());
        execution.put("result", finalResult);
        
        // Update execution record
        IndexRequest updateRequest = new IndexRequest(EXECUTIONS_INDEX)
            .id(ctx.executionId)
            .source(execution, XContentType.JSON);
        
        client.index(updateRequest, ActionListener.wrap(
            response -> {
                Map<String, Object> result = new HashMap<>();
                result.put("execution_id", ctx.executionId);
                result.put("agent", ctx.agentName);
                result.put("status", "completed");
                result.put("execution_mode", ctx.executionMode);
                result.put("result", finalResult);
                listener.onResponse(result);
            },
            listener::onFailure
        ));
    }
    
    private void failExecution(AgentExecutionContext ctx, Map<String, Object> execution,
                              String failedPhase, Exception error,
                              ActionListener<Map<String, Object>> listener) {
        execution.put("status", "failed");
        execution.put("failed_at", System.currentTimeMillis());
        execution.put("failed_phase", failedPhase);
        execution.put("error", error.getMessage());
        
        // Update execution record
        IndexRequest updateRequest = new IndexRequest(EXECUTIONS_INDEX)
            .id(ctx.executionId)
            .source(execution, XContentType.JSON);
        
        client.index(updateRequest, ActionListener.wrap(
            response -> {
                Map<String, Object> result = new HashMap<>();
                result.put("execution_id", ctx.executionId);
                result.put("agent", ctx.agentName);
                result.put("status", "failed");
                result.put("failed_phase", failedPhase);
                result.put("error", error.getMessage());
                listener.onResponse(result);
            },
            listener::onFailure
        ));
    }
    
    /**
     * Context for agent execution.
     */
    private static class AgentExecutionContext {
        final String executionId;
        final String agentName;
        final Map<String, Object> agent;
        final Map<String, Object> triggerContext;
        final String executionMode;
        
        AgentExecutionContext(String executionId, String agentName, Map<String, Object> agent,
                             Map<String, Object> triggerContext, String executionMode) {
            this.executionId = executionId;
            this.agentName = agentName;
            this.agent = agent;
            this.triggerContext = triggerContext;
            this.executionMode = executionMode != null ? executionMode : "human_approval";
        }
    }
    
    /**
     * Get execution history for an agent.
     */
    public void getExecutionHistory(String agentName, int limit,
                                   ActionListener<List<Map<String, Object>>> listener) {
        SearchRequest searchRequest = new SearchRequest(EXECUTIONS_INDEX);
        searchRequest.source(new SearchSourceBuilder()
            .query(QueryBuilders.termQuery("agent_name", agentName))
            .size(limit)
            .sort("started_at", org.elasticsearch.search.sort.SortOrder.DESC));
        
        client.search(searchRequest, ActionListener.wrap(
            response -> {
                List<Map<String, Object>> history = new ArrayList<>();
                for (SearchHit hit : response.getHits().getHits()) {
                    history.add(hit.getSourceAsMap());
                }
                listener.onResponse(history);
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
    
    /**
     * Get a specific execution.
     */
    public void getExecution(String executionId,
                            ActionListener<Map<String, Object>> listener) {
        client.get(
            new org.elasticsearch.action.get.GetRequest(EXECUTIONS_INDEX, executionId),
            ActionListener.wrap(
                response -> {
                    if (response.isExists()) {
                        listener.onResponse(response.getSourceAsMap());
                    } else {
                        listener.onResponse(null);
                    }
                },
                listener::onFailure
            )
        );
    }
}
