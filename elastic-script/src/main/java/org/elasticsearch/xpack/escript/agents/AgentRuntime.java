/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.agents;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.util.stream.Collectors;

/**
 * Agent Runtime for Moltler.
 * 
 * Executes autonomous agents based on the OODA loop:
 * - Observe: Gather context and data
 * - Orient: Analyze situation and determine appropriate action (using LLM)
 * - Decide: Select skill(s) to execute (using LLM reasoning)
 * - Act: Execute the skill and record results
 * 
 * Execution Modes:
 * - AUTONOMOUS: Execute without human intervention
 * - SUPERVISED: Execute with logging, can be stopped
 * - HUMAN_APPROVAL: Pause before each action for approval
 * - DRY_RUN: Show what would happen without executing
 */
public class AgentRuntime {
    
    private static final Logger LOGGER = LogManager.getLogger(AgentRuntime.class);
    private static final String AGENTS_INDEX = ".moltler_agents";
    private static final String EXECUTIONS_INDEX = ".moltler_agent_executions";
    private static final String CONVERSATIONS_INDEX = ".moltler_agent_conversations";
    
    private final Client client;
    private final AgentInferenceService inferenceService;
    
    public AgentRuntime(Client client) {
        this.client = client;
        this.inferenceService = new AgentInferenceService(client);
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
    
    /**
     * Chat with an agent - interactive conversation mode.
     * 
     * @param agentName The name of the agent
     * @param message The user's message
     * @param context Additional context for the conversation
     * @param listener Callback with the agent's response
     */
    public void chat(String agentName, String message, Map<String, Object> context,
                    ActionListener<Map<String, Object>> listener) {
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
                
                // Get conversation ID from context or create new
                String conversationId = context != null ? 
                    (String) context.get("conversation_id") : null;
                if (conversationId == null) {
                    conversationId = UUID.randomUUID().toString();
                }
                
                final String convId = conversationId;
                
                // Load conversation history
                loadConversationHistory(convId, ActionListener.wrap(
                    history -> {
                        // Get instructions (system prompt)
                        String instructions = (String) agent.get("instructions");
                        if (instructions == null || instructions.isEmpty()) {
                            instructions = "You are a helpful AI assistant. Goal: " + agent.get("goal");
                        }
                        
                        // Build reasoning prompt if agent has skills
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> skills = (List<Map<String, Object>>) agent.get("skills");
                        
                        String effectiveMessage = message;
                        if (skills != null && !skills.isEmpty()) {
                            // Add skill information to the prompt
                            effectiveMessage = inferenceService.buildReasoningPrompt(agent, context, message);
                        }
                        
                        // Call LLM
                        inferenceService.generateResponse(agent, instructions, effectiveMessage, history,
                            ActionListener.wrap(
                                response -> {
                                    // Save conversation turn
                                    saveConversationTurn(convId, agentName, message, response, ActionListener.wrap(
                                        saved -> {
                                            Map<String, Object> result = new HashMap<>();
                                            result.put("agent", agentName);
                                            result.put("conversation_id", convId);
                                            result.put("message", message);
                                            result.put("response", response);
                                            result.put("timestamp", System.currentTimeMillis());
                                            
                                            // If the response contains a skill decision, parse and include it
                                            if (skills != null && !skills.isEmpty()) {
                                                Map<String, Object> decision = inferenceService.parseSkillDecision(response);
                                                result.put("decision", decision);
                                            }
                                            
                                            listener.onResponse(result);
                                        },
                                        e -> {
                                            // Still return response even if save fails
                                            LOGGER.warn("Failed to save conversation: {}", e.getMessage());
                                            Map<String, Object> result = new HashMap<>();
                                            result.put("agent", agentName);
                                            result.put("conversation_id", convId);
                                            result.put("message", message);
                                            result.put("response", response);
                                            result.put("timestamp", System.currentTimeMillis());
                                            listener.onResponse(result);
                                        }
                                    ));
                                },
                                listener::onFailure
                            ));
                    },
                    listener::onFailure
                ));
            },
            listener::onFailure
        ));
    }
    
    /**
     * Load conversation history for a given conversation ID.
     */
    private void loadConversationHistory(String conversationId,
                                        ActionListener<List<Map<String, String>>> listener) {
        SearchRequest searchRequest = new SearchRequest(CONVERSATIONS_INDEX);
        searchRequest.source(new SearchSourceBuilder()
            .query(QueryBuilders.termQuery("conversation_id", conversationId))
            .size(50)  // Last 50 messages
            .sort("timestamp", org.elasticsearch.search.sort.SortOrder.ASC));
        
        client.search(searchRequest, ActionListener.wrap(
            response -> {
                List<Map<String, String>> history = new ArrayList<>();
                for (SearchHit hit : response.getHits().getHits()) {
                    Map<String, Object> source = hit.getSourceAsMap();
                    
                    // Add user message
                    Map<String, String> userMsg = new HashMap<>();
                    userMsg.put("role", "user");
                    userMsg.put("content", (String) source.get("user_message"));
                    history.add(userMsg);
                    
                    // Add assistant response
                    Map<String, String> assistantMsg = new HashMap<>();
                    assistantMsg.put("role", "assistant");
                    assistantMsg.put("content", (String) source.get("assistant_response"));
                    history.add(assistantMsg);
                }
                listener.onResponse(history);
            },
            e -> {
                // Return empty history if index doesn't exist
                if (e instanceof org.elasticsearch.index.IndexNotFoundException) {
                    listener.onResponse(new ArrayList<>());
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }
    
    /**
     * Save a conversation turn.
     */
    private void saveConversationTurn(String conversationId, String agentName,
                                     String userMessage, String assistantResponse,
                                     ActionListener<Boolean> listener) {
        Map<String, Object> turn = new HashMap<>();
        turn.put("conversation_id", conversationId);
        turn.put("agent_name", agentName);
        turn.put("user_message", userMessage);
        turn.put("assistant_response", assistantResponse);
        turn.put("timestamp", System.currentTimeMillis());
        
        String turnId = conversationId + "_" + System.currentTimeMillis();
        
        IndexRequest indexRequest = new IndexRequest(CONVERSATIONS_INDEX)
            .id(turnId)
            .source(turn, XContentType.JSON);
        
        client.index(indexRequest, ActionListener.wrap(
            response -> listener.onResponse(true),
            e -> {
                // Try to create index if it doesn't exist
                if (e instanceof org.elasticsearch.index.IndexNotFoundException) {
                    client.admin().indices().prepareCreate(CONVERSATIONS_INDEX)
                        .execute(ActionListener.wrap(
                            created -> {
                                // Retry the index operation
                                client.index(indexRequest, ActionListener.wrap(
                                    r -> listener.onResponse(true),
                                    listener::onFailure
                                ));
                            },
                            listener::onFailure
                        ));
                } else {
                    listener.onFailure(e);
                }
            }
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
     * ORIENT phase: Analyze situation and context using LLM.
     * If the agent has an inference endpoint configured, use it for reasoning.
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
        
        // Check if agent has LLM configured for reasoning
        @SuppressWarnings("unchecked")
        Map<String, Object> modelConfig = (Map<String, Object>) ctx.agent.get("model_config");
        
        if (modelConfig != null && !modelConfig.isEmpty()) {
            // Use LLM for orientation analysis
            String instructions = (String) ctx.agent.get("instructions");
            if (instructions == null) {
                instructions = "You are an AI agent analyzing a situation to determine the best course of action.";
            }
            
            String contextSummary = buildContextSummary(ctx, observations);
            
            inferenceService.generateResponse(ctx.agent, instructions, 
                "Analyze this situation and provide orientation:\n" + contextSummary,
                null,  // no conversation history for OODA
                ActionListener.wrap(
                    llmResponse -> {
                        orientation.put("llm_analysis", llmResponse);
                        orientation.put("priority", "normal");
                        listener.onResponse(orientation);
                    },
                    e -> {
                        // Fall back to basic orientation if LLM fails
                        LOGGER.warn("LLM orientation failed, using basic analysis: {}", e.getMessage());
                        orientation.put("priority", "normal");
                        orientation.put("llm_error", e.getMessage());
                        listener.onResponse(orientation);
                    }
                ));
        } else {
            // No LLM configured, use basic orientation
            orientation.put("priority", "normal");
            listener.onResponse(orientation);
        }
    }
    
    /**
     * Build a context summary for LLM reasoning.
     */
    private String buildContextSummary(AgentExecutionContext ctx, Map<String, Object> observations) {
        StringBuilder sb = new StringBuilder();
        sb.append("Goal: ").append(ctx.agent.get("goal")).append("\n");
        sb.append("Execution Mode: ").append(ctx.executionMode).append("\n");
        
        if (ctx.triggerContext != null && !ctx.triggerContext.isEmpty()) {
            sb.append("Trigger Context: ").append(ctx.triggerContext).append("\n");
        }
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> skills = (List<Map<String, Object>>) observations.get("available_skills");
        if (skills != null && !skills.isEmpty()) {
            sb.append("Available Skills: ");
            sb.append(skills.stream()
                .map(s -> (String) s.get("name"))
                .collect(java.util.stream.Collectors.joining(", ")));
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * DECIDE phase: Select skill(s) to execute using LLM reasoning.
     */
    private void decide(AgentExecutionContext ctx, Map<String, Object> orientation,
                       ActionListener<Map<String, Object>> listener) {
        @SuppressWarnings("unchecked")
        List<String> applicableSkills = (List<String>) orientation.get("applicable_skills");
        
        if (applicableSkills == null || applicableSkills.isEmpty()) {
            Map<String, Object> decision = new HashMap<>();
            decision.put("action", "no_action");
            decision.put("reason", "No applicable skills found");
            listener.onResponse(decision);
            return;
        }
        
        // Check if agent has LLM configured
        @SuppressWarnings("unchecked")
        Map<String, Object> modelConfig = (Map<String, Object>) ctx.agent.get("model_config");
        
        if (modelConfig != null && !modelConfig.isEmpty()) {
            // Use LLM to decide which skill to execute
            String reasoningPrompt = inferenceService.buildReasoningPrompt(
                ctx.agent, orientation, 
                ctx.triggerContext != null ? ctx.triggerContext.toString() : "Manual trigger");
            
            String instructions = (String) ctx.agent.get("instructions");
            if (instructions == null) {
                instructions = "You are an AI agent that must decide which skill to execute based on the goal and context.";
            }
            
            inferenceService.generateResponse(ctx.agent, instructions, reasoningPrompt, null,
                ActionListener.wrap(
                    llmResponse -> {
                        Map<String, Object> decision = inferenceService.parseSkillDecision(llmResponse);
                        
                        // Validate the selected skill exists
                        String selectedSkill = (String) decision.get("skill");
                        if (selectedSkill != null && applicableSkills.contains(selectedSkill)) {
                            decision.put("action", "execute_skill");
                        } else if (selectedSkill != null) {
                            // LLM suggested a skill not in the list, use first available
                            LOGGER.warn("LLM suggested unavailable skill [{}], using [{}]", 
                                selectedSkill, applicableSkills.get(0));
                            decision.put("skill", applicableSkills.get(0));
                            decision.put("action", "execute_skill");
                            decision.put("llm_skill_override", selectedSkill);
                        } else {
                            decision.put("action", "no_action");
                        }
                        
                        // Check if approval is needed
                        if ("human_approval".equals(ctx.executionMode)) {
                            decision.put("requires_approval", true);
                            decision.put("approval_status", "pending");
                        } else {
                            decision.put("requires_approval", false);
                        }
                        
                        listener.onResponse(decision);
                    },
                    e -> {
                        // Fall back to basic decision if LLM fails
                        LOGGER.warn("LLM decision failed, using basic selection: {}", e.getMessage());
                        Map<String, Object> decision = new HashMap<>();
                        decision.put("action", "execute_skill");
                        decision.put("skill", applicableSkills.get(0));
                        decision.put("reason", "Fallback selection (LLM unavailable)");
                        decision.put("parameters", Map.of());
                        decision.put("llm_error", e.getMessage());
                        
                        if ("human_approval".equals(ctx.executionMode)) {
                            decision.put("requires_approval", true);
                            decision.put("approval_status", "pending");
                        } else {
                            decision.put("requires_approval", false);
                        }
                        
                        listener.onResponse(decision);
                    }
                ));
        } else {
            // No LLM configured, use basic decision (first skill)
            Map<String, Object> decision = new HashMap<>();
            decision.put("action", "execute_skill");
            decision.put("skill", applicableSkills.get(0));
            decision.put("reason", "Selected first available skill (no LLM configured)");
            decision.put("parameters", Map.of());
            
            if ("human_approval".equals(ctx.executionMode)) {
                decision.put("requires_approval", true);
                decision.put("approval_status", "pending");
            } else {
                decision.put("requires_approval", false);
            }
            
            listener.onResponse(decision);
        }
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
            actionResult.put("parameters", decision.get("parameters"));
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
                actionResult.put("parameters", decision.get("parameters"));
                actionResult.put("message", "Execution paused - waiting for human approval");
                listener.onResponse(actionResult);
                return;
            }
        }
        
        // Execute the skill via MCP endpoint
        String skillName = (String) decision.get("skill");
        @SuppressWarnings("unchecked")
        Map<String, Object> rawParams = (Map<String, Object>) decision.get("parameters");
        final Map<String, Object> parameters = (rawParams != null) ? rawParams : new HashMap<>();
        
        LOGGER.info("Executing skill [{}] with parameters: {}", skillName, parameters);
        
        // Execute skill using the skill registry
        executeSkill(skillName, parameters, ActionListener.wrap(
            skillResult -> {
                actionResult.put("status", "executed");
                actionResult.put("skill", skillName);
                actionResult.put("parameters", parameters);
                actionResult.put("result", skillResult);
                actionResult.put("executed_at", System.currentTimeMillis());
                listener.onResponse(actionResult);
            },
            e -> {
                LOGGER.error("Skill execution failed for [{}]: {}", skillName, e.getMessage());
                actionResult.put("status", "failed");
                actionResult.put("skill", skillName);
                actionResult.put("parameters", parameters);
                actionResult.put("error", e.getMessage());
                actionResult.put("failed_at", System.currentTimeMillis());
                listener.onResponse(actionResult);
            }
        ));
    }
    
    /**
     * Execute a skill by calling the MCP endpoint internally.
     */
    private void executeSkill(String skillName, Map<String, Object> parameters,
                             ActionListener<Object> listener) {
        // Build the skill execution request
        // This would typically call the skill registry or MCP handler
        // For now, we'll look up the skill and execute it
        
        SearchRequest searchRequest = new SearchRequest(".moltler_skills");
        searchRequest.source(new SearchSourceBuilder()
            .query(QueryBuilders.termQuery("name", skillName))
            .size(1));
        
        client.search(searchRequest, ActionListener.wrap(
            response -> {
                if (response.getHits().getTotalHits().value() > 0) {
                    Map<String, Object> skill = response.getHits().getHits()[0].getSourceAsMap();
                    String body = (String) skill.get("body");
                    
                    // Execute the skill body
                    // For production, this would use the ProcedureExecutor
                    Map<String, Object> result = new HashMap<>();
                    result.put("skill_name", skillName);
                    result.put("executed", true);
                    result.put("body_preview", body != null && body.length() > 100 ? 
                        body.substring(0, 100) + "..." : body);
                    result.put("message", "Skill found and ready for execution. " +
                        "Full execution requires ProcedureExecutor integration.");
                    
                    listener.onResponse(result);
                } else {
                    listener.onFailure(new RuntimeException("Skill not found: " + skillName));
                }
            },
            e -> {
                if (e instanceof org.elasticsearch.index.IndexNotFoundException) {
                    listener.onFailure(new RuntimeException(
                        "Skills index not found. Install skills using: moltler-cli.sh install <skill>"));
                } else {
                    listener.onFailure(e);
                }
            }
        ));
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
