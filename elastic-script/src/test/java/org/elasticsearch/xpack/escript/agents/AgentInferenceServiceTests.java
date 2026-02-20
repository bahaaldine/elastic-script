/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.escript.agents;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.test.ESTestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.mock;

/**
 * Tests for AgentInferenceService.
 */
public class AgentInferenceServiceTests extends ESTestCase {

    private Client mockClient;
    private AgentInferenceService service;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mockClient = mock(Client.class);
        service = new AgentInferenceService(mockClient);
    }

    // ============================================================================
    // Reasoning Prompt Building Tests
    // ============================================================================

    public void testBuildReasoningPromptWithGoal() {
        Map<String, Object> agent = new HashMap<>();
        agent.put("goal", "Monitor production systems");
        agent.put("skills", createTestSkills("check_health", "notify_team"));
        
        Map<String, Object> observations = new HashMap<>();
        
        String prompt = service.buildReasoningPrompt(agent, observations, "Check system status");
        
        assertNotNull(prompt);
        assertTrue(prompt.contains("Monitor production systems"));
        assertTrue(prompt.contains("check_health"));
        assertTrue(prompt.contains("notify_team"));
        assertTrue(prompt.contains("Check system status"));
    }

    public void testBuildReasoningPromptWithMultipleSkills() {
        Map<String, Object> agent = new HashMap<>();
        agent.put("goal", "Incident response");
        agent.put("skills", createTestSkills("diagnose", "fix", "notify", "escalate"));
        
        Map<String, Object> observations = new HashMap<>();
        
        String prompt = service.buildReasoningPrompt(agent, observations, "Handle this incident");
        
        assertNotNull(prompt);
        assertTrue(prompt.contains("diagnose"));
        assertTrue(prompt.contains("fix"));
        assertTrue(prompt.contains("notify"));
        assertTrue(prompt.contains("escalate"));
    }

    public void testBuildReasoningPromptWithEmptySkills() {
        Map<String, Object> agent = new HashMap<>();
        agent.put("goal", "No skills agent");
        agent.put("skills", new ArrayList<>());
        
        Map<String, Object> observations = new HashMap<>();
        
        String prompt = service.buildReasoningPrompt(agent, observations, "Test message");
        
        assertNotNull(prompt);
        assertTrue(prompt.contains("No skills agent"));
        assertTrue(prompt.contains("AVAILABLE SKILLS"));
    }

    public void testBuildReasoningPromptWithNullSkills() {
        Map<String, Object> agent = new HashMap<>();
        agent.put("goal", "Null skills agent");
        agent.put("skills", null);
        
        Map<String, Object> observations = new HashMap<>();
        
        String prompt = service.buildReasoningPrompt(agent, observations, "Test message");
        
        assertNotNull(prompt);
    }

    public void testBuildReasoningPromptWithObservations() {
        Map<String, Object> agent = new HashMap<>();
        agent.put("goal", "Analyze data");
        agent.put("skills", createTestSkills("analyze"));
        
        Map<String, Object> observations = new HashMap<>();
        observations.put("cpu_usage", 85);
        observations.put("memory_usage", 70);
        
        String prompt = service.buildReasoningPrompt(agent, observations, "What should we do?");
        
        assertNotNull(prompt);
        assertTrue(prompt.contains("CURRENT CONTEXT"));
    }

    public void testBuildReasoningPromptJSONFormat() {
        Map<String, Object> agent = new HashMap<>();
        agent.put("goal", "Test JSON output");
        agent.put("skills", createTestSkills("test"));
        
        String prompt = service.buildReasoningPrompt(agent, null, "Test");
        
        assertNotNull(prompt);
        assertTrue(prompt.contains("Respond in JSON format"));
        assertTrue(prompt.contains("\"reasoning\""));
        assertTrue(prompt.contains("\"skill\""));
        assertTrue(prompt.contains("\"parameters\""));
        assertTrue(prompt.contains("\"confidence\""));
    }

    // ============================================================================
    // Skill Decision Parsing Tests
    // ============================================================================

    public void testParseSkillDecisionValidJSON() {
        String llmResponse = """
            {
                "reasoning": "Based on high CPU usage, we should restart the service",
                "skill": "restart_service",
                "parameters": {"service_name": "api"},
                "confidence": 0.85
            }
            """;
        
        Map<String, Object> decision = service.parseSkillDecision(llmResponse);
        
        assertNotNull(decision);
        assertEquals("restart_service", decision.get("skill"));
        assertEquals(0.85, decision.get("confidence"));
        assertNotNull(decision.get("reasoning"));
    }

    public void testParseSkillDecisionWithPrefixText() {
        String llmResponse = """
            Based on my analysis, here is my decision:
            
            {
                "reasoning": "Need to scale up due to high load",
                "skill": "scale_up",
                "parameters": {"replicas": 5},
                "confidence": 0.9
            }
            """;
        
        Map<String, Object> decision = service.parseSkillDecision(llmResponse);
        
        assertNotNull(decision);
        assertEquals("scale_up", decision.get("skill"));
    }

    public void testParseSkillDecisionWithSuffixText() {
        String llmResponse = """
            {
                "reasoning": "Service is healthy",
                "skill": null,
                "parameters": {},
                "confidence": 0.95
            }
            
            Let me know if you need anything else!
            """;
        
        Map<String, Object> decision = service.parseSkillDecision(llmResponse);
        
        assertNotNull(decision);
        assertNull(decision.get("skill"));
    }

    public void testParseSkillDecisionInvalidJSON() {
        String llmResponse = "This is not valid JSON at all";
        
        Map<String, Object> decision = service.parseSkillDecision(llmResponse);
        
        assertNotNull(decision);
        assertNull(decision.get("skill"));
        assertEquals(0.0, decision.get("confidence"));
        assertTrue(decision.get("reasoning").toString().contains("not valid JSON"));
    }

    public void testParseSkillDecisionEmptyResponse() {
        String llmResponse = "";
        
        Map<String, Object> decision = service.parseSkillDecision(llmResponse);
        
        assertNotNull(decision);
        assertNull(decision.get("skill"));
    }

    public void testParseSkillDecisionNullResponse() {
        Map<String, Object> decision = service.parseSkillDecision(null);
        
        assertNotNull(decision);
        assertNull(decision.get("skill"));
    }

    public void testParseSkillDecisionMalformedJSON() {
        String llmResponse = "{\"skill\": \"test\", \"reasoning\": }";
        
        Map<String, Object> decision = service.parseSkillDecision(llmResponse);
        
        assertNotNull(decision);
        // Should have parse_error field
        assertTrue(decision.containsKey("parse_error") || decision.get("skill") == null);
    }

    public void testParseSkillDecisionNestedJSON() {
        String llmResponse = """
            {
                "reasoning": "Complex decision with nested params",
                "skill": "deploy",
                "parameters": {
                    "service": "api",
                    "config": {
                        "replicas": 3,
                        "memory": "512Mi"
                    }
                },
                "confidence": 0.75
            }
            """;
        
        Map<String, Object> decision = service.parseSkillDecision(llmResponse);
        
        assertNotNull(decision);
        assertEquals("deploy", decision.get("skill"));
        assertNotNull(decision.get("parameters"));
    }

    // ============================================================================
    // Generate Response Tests (Model Config Validation)
    // ============================================================================

    public void testGenerateResponseWithoutModelConfig() throws Exception {
        Map<String, Object> agent = new HashMap<>();
        agent.put("goal", "Test agent");
        // No model_config
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();
        
        service.generateResponse(agent, "System prompt", "User message", null,
            new ActionListener<>() {
                @Override
                public void onResponse(String response) {
                    latch.countDown();
                }
                
                @Override
                public void onFailure(Exception e) {
                    error.set(e);
                    latch.countDown();
                }
            });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNotNull(error.get());
        assertTrue(error.get().getMessage().contains("no model configuration"));
    }

    public void testGenerateResponseWithEmptyModelConfig() throws Exception {
        Map<String, Object> agent = new HashMap<>();
        agent.put("goal", "Test agent");
        agent.put("model_config", new HashMap<>());
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();
        
        service.generateResponse(agent, "System prompt", "User message", null,
            new ActionListener<>() {
                @Override
                public void onResponse(String response) {
                    latch.countDown();
                }
                
                @Override
                public void onFailure(Exception e) {
                    error.set(e);
                    latch.countDown();
                }
            });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNotNull(error.get());
        assertTrue(error.get().getMessage().contains("no model configuration"));
    }

    public void testGenerateResponseWithUnsupportedModelType() throws Exception {
        Map<String, Object> agent = new HashMap<>();
        agent.put("goal", "Test agent");
        
        Map<String, Object> modelConfig = new HashMap<>();
        modelConfig.put("type", "unsupported_type");
        agent.put("model_config", modelConfig);
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();
        
        service.generateResponse(agent, "System prompt", "User message", null,
            new ActionListener<>() {
                @Override
                public void onResponse(String response) {
                    latch.countDown();
                }
                
                @Override
                public void onFailure(Exception e) {
                    error.set(e);
                    latch.countDown();
                }
            });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNotNull(error.get());
        assertTrue(error.get().getMessage().contains("Unsupported model config type"));
    }

    // ============================================================================
    // Edge Cases
    // ============================================================================

    public void testBuildReasoningPromptWithNullUserMessage() {
        Map<String, Object> agent = new HashMap<>();
        agent.put("goal", "Test goal");
        agent.put("skills", createTestSkills("test"));
        
        String prompt = service.buildReasoningPrompt(agent, null, null);
        
        assertNotNull(prompt);
        assertTrue(prompt.contains("Test goal"));
    }

    public void testBuildReasoningPromptWithEmptyUserMessage() {
        Map<String, Object> agent = new HashMap<>();
        agent.put("goal", "Test goal");
        agent.put("skills", createTestSkills("test"));
        
        String prompt = service.buildReasoningPrompt(agent, null, "");
        
        assertNotNull(prompt);
        assertTrue(prompt.contains("Test goal"));
    }

    public void testParseSkillDecisionWithOnlyBraces() {
        String llmResponse = "{}";
        
        Map<String, Object> decision = service.parseSkillDecision(llmResponse);
        
        assertNotNull(decision);
        // Empty JSON should parse but have no skill
        assertFalse(decision.containsKey("skill") && decision.get("skill") != null);
    }

    public void testParseSkillDecisionWithCodeFences() {
        String llmResponse = """
            Here's my decision:
            
            ```json
            {
                "reasoning": "Analysis complete",
                "skill": "report",
                "parameters": {},
                "confidence": 0.8
            }
            ```
            """;
        
        Map<String, Object> decision = service.parseSkillDecision(llmResponse);
        
        assertNotNull(decision);
        // Should still extract JSON from within code fences
        assertEquals("report", decision.get("skill"));
    }

    // ============================================================================
    // Helper Methods
    // ============================================================================

    private List<Map<String, Object>> createTestSkills(String... skillNames) {
        List<Map<String, Object>> skills = new ArrayList<>();
        for (String name : skillNames) {
            Map<String, Object> skill = new HashMap<>();
            skill.put("name", name);
            skills.add(skill);
        }
        return skills;
    }
}
