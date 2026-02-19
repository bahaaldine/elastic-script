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
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.inference.TaskType;
import org.elasticsearch.xpack.core.inference.action.InferenceAction;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.XContentParserConfiguration;
import org.elasticsearch.xcontent.XContentType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for handling LLM inference calls for agent reasoning.
 * Supports both Elasticsearch Inference endpoints and direct model calls.
 */
public class AgentInferenceService {
    
    private static final Logger LOGGER = LogManager.getLogger(AgentInferenceService.class);
    private static final TimeValue DEFAULT_TIMEOUT = TimeValue.timeValueSeconds(60);
    
    private final Client client;
    
    public AgentInferenceService(Client client) {
        this.client = client;
    }
    
    /**
     * Generate a response using LLM for agent reasoning.
     * 
     * @param agent The agent configuration
     * @param systemPrompt The system prompt (instructions)
     * @param userMessage The user message or context
     * @param conversationHistory Previous messages in the conversation
     * @param listener Callback with the LLM response
     */
    public void generateResponse(Map<String, Object> agent,
                                 String systemPrompt,
                                 String userMessage,
                                 List<Map<String, String>> conversationHistory,
                                 ActionListener<String> listener) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> modelConfig = (Map<String, Object>) agent.get("model_config");
            
            if (modelConfig == null || modelConfig.isEmpty()) {
                listener.onFailure(new RuntimeException(
                    "Agent has no model configuration. Use INFERENCE_ENDPOINT or MODEL in CREATE AGENT."));
                return;
            }
            
            String configType = (String) modelConfig.get("type");
            
            if ("inference_endpoint".equals(configType)) {
                String endpointId = (String) modelConfig.get("endpoint_id");
                callInferenceEndpoint(endpointId, systemPrompt, userMessage, 
                    conversationHistory, agent, listener);
            } else {
                listener.onFailure(new RuntimeException(
                    "Unsupported model config type: " + configType + 
                    ". Use INFERENCE_ENDPOINT with an Elasticsearch inference endpoint."));
            }
            
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Call an Elasticsearch Inference endpoint for chat completion.
     */
    private void callInferenceEndpoint(String endpointId,
                                       String systemPrompt,
                                       String userMessage,
                                       List<Map<String, String>> conversationHistory,
                                       Map<String, Object> agent,
                                       ActionListener<String> listener) {
        try {
            // Build messages array
            List<Map<String, String>> messages = new ArrayList<>();
            
            // Add system message
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                Map<String, String> sysMsg = new HashMap<>();
                sysMsg.put("role", "system");
                sysMsg.put("content", systemPrompt);
                messages.add(sysMsg);
            }
            
            // Add conversation history
            if (conversationHistory != null) {
                messages.addAll(conversationHistory);
            }
            
            // Add current user message
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);
            
            // Build the request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("messages", messages);
            
            // Add temperature if configured
            Object temperature = agent.get("temperature");
            if (temperature != null) {
                requestBody.put("temperature", temperature);
            }
            
            // Add max_tokens if configured
            Object maxTokens = agent.get("max_tokens");
            if (maxTokens != null) {
                requestBody.put("max_tokens", maxTokens);
            }
            
            // Convert to JSON
            String inputJson = convertToJson(requestBody);
            
            LOGGER.debug("Calling inference endpoint [{}] with {} messages", endpointId, messages.size());
            
            // Create inference request using builder pattern
            InferenceAction.Request request = InferenceAction.Request.builder(endpointId, TaskType.CHAT_COMPLETION)
                .setInput(List.of(inputJson))
                .setInferenceTimeout(DEFAULT_TIMEOUT)
                .build();
            
            client.execute(InferenceAction.INSTANCE, request, new ActionListener<InferenceAction.Response>() {
                @Override
                public void onResponse(InferenceAction.Response response) {
                    try {
                        String result = extractChatResponse(response);
                        LOGGER.debug("Inference response received: {} chars", result.length());
                        listener.onResponse(result);
                    } catch (Exception e) {
                        listener.onFailure(new RuntimeException(
                            "Failed to parse inference response: " + e.getMessage(), e));
                    }
                }
                
                @Override
                public void onFailure(Exception e) {
                    LOGGER.error("Inference failed for endpoint [{}]: {}", endpointId, e.getMessage());
                    listener.onFailure(new RuntimeException(
                        "Agent inference failed: " + e.getMessage(), e));
                }
            });
            
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Build a reasoning prompt for the agent to decide which skill to execute.
     */
    public String buildReasoningPrompt(Map<String, Object> agent,
                                       Map<String, Object> observations,
                                       String userMessage) {
        StringBuilder prompt = new StringBuilder();
        
        String goal = (String) agent.get("goal");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> skills = (List<Map<String, Object>>) agent.get("skills");
        
        prompt.append("You are an AI agent with a specific goal and a set of skills.\n\n");
        prompt.append("GOAL: ").append(goal).append("\n\n");
        
        prompt.append("AVAILABLE SKILLS:\n");
        if (skills != null) {
            for (Map<String, Object> skill : skills) {
                String skillName = (String) skill.get("name");
                prompt.append("- ").append(skillName).append("\n");
            }
        }
        prompt.append("\n");
        
        if (userMessage != null && !userMessage.isEmpty()) {
            prompt.append("USER REQUEST: ").append(userMessage).append("\n\n");
        }
        
        if (observations != null && !observations.isEmpty()) {
            prompt.append("CURRENT CONTEXT:\n");
            prompt.append(convertToJson(observations)).append("\n\n");
        }
        
        prompt.append("Based on the goal and user request, decide which skill(s) to execute.\n");
        prompt.append("Respond in JSON format:\n");
        prompt.append("{\n");
        prompt.append("  \"reasoning\": \"Your reasoning process\",\n");
        prompt.append("  \"skill\": \"skill_name_to_execute\",\n");
        prompt.append("  \"parameters\": { \"param1\": \"value1\" },\n");
        prompt.append("  \"confidence\": 0.0 to 1.0\n");
        prompt.append("}\n");
        prompt.append("\nIf no skill is appropriate, set \"skill\": null and explain in reasoning.");
        
        return prompt.toString();
    }
    
    /**
     * Parse the LLM's skill decision response.
     */
    public Map<String, Object> parseSkillDecision(String llmResponse) {
        Map<String, Object> decision = new HashMap<>();
        
        try {
            // Try to extract JSON from the response
            String json = extractJson(llmResponse);
            if (json != null) {
                try (XContentParser parser = XContentType.JSON.xContent().createParser(
                        XContentParserConfiguration.EMPTY,
                        new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))) {
                    decision = parser.map();
                }
            } else {
                // Fallback: no valid JSON found
                decision.put("reasoning", llmResponse);
                decision.put("skill", null);
                decision.put("confidence", 0.0);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to parse LLM decision response: {}", e.getMessage());
            decision.put("reasoning", llmResponse);
            decision.put("skill", null);
            decision.put("confidence", 0.0);
            decision.put("parse_error", e.getMessage());
        }
        
        return decision;
    }
    
    /**
     * Extract JSON from a response that might contain additional text.
     */
    private String extractJson(String text) {
        if (text == null) return null;
        
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        
        return null;
    }
    
    /**
     * Extract the assistant response from an inference response.
     */
    private String extractChatResponse(InferenceAction.Response response) {
        // The response structure depends on the inference endpoint type
        // For OpenAI-compatible endpoints, extract content from choices[0].message.content
        Object results = response.getResults();
        
        if (results != null) {
            String resultsStr = results.toString();
            
            // Try to find content in the response
            if (resultsStr.contains("content=")) {
                int contentStart = resultsStr.indexOf("content=");
                if (contentStart >= 0) {
                    int valueStart = contentStart + 8;
                    int valueEnd = resultsStr.indexOf(",", valueStart);
                    if (valueEnd < 0) valueEnd = resultsStr.indexOf("}", valueStart);
                    if (valueEnd > valueStart) {
                        return resultsStr.substring(valueStart, valueEnd).trim();
                    }
                }
            }
            
            // Fallback: return the whole result
            return resultsStr;
        }
        
        return "";
    }
    
    /**
     * Convert a map to JSON string.
     */
    private String convertToJson(Map<String, ?> map) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.map(new HashMap<>(map));
            return builder.toString();
        } catch (IOException e) {
            LOGGER.error("Failed to convert map to JSON", e);
            return "{}";
        }
    }
}
