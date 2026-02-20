/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.escript.agents;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.internal.AdminClient;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.client.internal.IndicesAdminClient;
import org.elasticsearch.index.get.GetResult;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.test.ESTestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for AgentRuntime OODA loop execution.
 */
public class AgentRuntimeTests extends ESTestCase {

    private Client mockClient;
    private AgentRuntime runtime;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mockClient = mock(Client.class);
        runtime = new AgentRuntime(mockClient);
        
        // Setup admin client mock
        AdminClient adminClient = mock(AdminClient.class);
        IndicesAdminClient indicesClient = mock(IndicesAdminClient.class);
        when(mockClient.admin()).thenReturn(adminClient);
        when(adminClient.indices()).thenReturn(indicesClient);
    }

    // ============================================================================
    // Agent Loading Tests
    // ============================================================================

    public void testExecuteWithNonExistentAgent() throws Exception {
        // Mock get response for non-existent agent
        GetResponse mockGetResponse = mock(GetResponse.class);
        when(mockGetResponse.isExists()).thenReturn(false);
        
        doAnswer(invocation -> {
            ActionListener<GetResponse> listener = invocation.getArgument(1);
            listener.onResponse(mockGetResponse);
            return null;
        }).when(mockClient).get(any(), any());
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();
        
        runtime.execute("non_existent_agent", null, new ActionListener<>() {
            @Override
            public void onResponse(Map<String, Object> result) {
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
        assertTrue(error.get().getMessage().contains("not found"));
    }

    public void testExecuteWithDisabledAgent() throws Exception {
        // Mock agent document with enabled=false
        Map<String, Object> agentDoc = createTestAgentDocument();
        agentDoc.put("enabled", false);
        
        GetResponse mockGetResponse = createMockGetResponse(agentDoc);
        
        doAnswer(invocation -> {
            ActionListener<GetResponse> listener = invocation.getArgument(1);
            listener.onResponse(mockGetResponse);
            return null;
        }).when(mockClient).get(any(), any());
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();
        
        runtime.execute("test_agent", null, new ActionListener<>() {
            @Override
            public void onResponse(Map<String, Object> result) {
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
        assertTrue(error.get().getMessage().contains("disabled"));
    }

    // ============================================================================
    // OODA Loop Phase Tests
    // ============================================================================

    // Note: Full OODA loop tests require integration testing with a real Elasticsearch cluster.
    // These unit tests focus on error handling and simple scenarios.

    // ============================================================================
    // Chat Tests
    // ============================================================================

    public void testChatWithNonExistentAgent() throws Exception {
        GetResponse mockGetResponse = mock(GetResponse.class);
        when(mockGetResponse.isExists()).thenReturn(false);
        
        doAnswer(invocation -> {
            ActionListener<GetResponse> listener = invocation.getArgument(1);
            listener.onResponse(mockGetResponse);
            return null;
        }).when(mockClient).get(any(), any());
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();
        
        runtime.chat("non_existent_agent", "Hello", null, new ActionListener<>() {
            @Override
            public void onResponse(Map<String, Object> result) {
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
        assertTrue(error.get().getMessage().contains("not found"));
    }

    public void testChatWithDisabledAgent() throws Exception {
        Map<String, Object> agentDoc = createTestAgentDocument();
        agentDoc.put("enabled", false);
        
        GetResponse mockGetResponse = createMockGetResponse(agentDoc);
        
        doAnswer(invocation -> {
            ActionListener<GetResponse> listener = invocation.getArgument(1);
            listener.onResponse(mockGetResponse);
            return null;
        }).when(mockClient).get(any(), any());
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();
        
        runtime.chat("test_agent", "Hello", null, new ActionListener<>() {
            @Override
            public void onResponse(Map<String, Object> result) {
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
        assertTrue(error.get().getMessage().contains("disabled"));
    }

    // Note: testChatWithoutModelConfig requires full async mocking of conversation history loading.
    // This is better tested in integration tests with a real Elasticsearch cluster.

    // ============================================================================
    // Execution History Tests
    // ============================================================================

    public void testGetExecutionHistory() throws Exception {
        SearchResponse mockSearchResponse = createEmptySearchResponse();
        
        doAnswer(invocation -> {
            ActionListener<SearchResponse> listener = invocation.getArgument(1);
            listener.onResponse(mockSearchResponse);
            return null;
        }).when(mockClient).search(any(), any());
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<List<Map<String, Object>>> result = new AtomicReference<>();
        
        runtime.getExecutionHistory("test_agent", 10, new ActionListener<>() {
            @Override
            public void onResponse(List<Map<String, Object>> r) {
                result.set(r);
                latch.countDown();
            }
            
            @Override
            public void onFailure(Exception e) {
                fail("Unexpected failure: " + e.getMessage());
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNotNull(result.get());
        assertTrue(result.get().isEmpty());
    }

    public void testGetExecution() throws Exception {
        Map<String, Object> executionDoc = new HashMap<>();
        executionDoc.put("execution_id", "exec-123");
        executionDoc.put("status", "completed");
        
        GetResponse mockGetResponse = createMockGetResponse(executionDoc);
        
        doAnswer(invocation -> {
            ActionListener<GetResponse> listener = invocation.getArgument(1);
            listener.onResponse(mockGetResponse);
            return null;
        }).when(mockClient).get(any(), any());
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Map<String, Object>> result = new AtomicReference<>();
        
        runtime.getExecution("exec-123", new ActionListener<>() {
            @Override
            public void onResponse(Map<String, Object> r) {
                result.set(r);
                latch.countDown();
            }
            
            @Override
            public void onFailure(Exception e) {
                fail("Unexpected failure: " + e.getMessage());
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNotNull(result.get());
        assertEquals("completed", result.get().get("status"));
    }

    public void testGetNonExistentExecution() throws Exception {
        GetResponse mockGetResponse = mock(GetResponse.class);
        when(mockGetResponse.isExists()).thenReturn(false);
        
        doAnswer(invocation -> {
            ActionListener<GetResponse> listener = invocation.getArgument(1);
            listener.onResponse(mockGetResponse);
            return null;
        }).when(mockClient).get(any(), any());
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Map<String, Object>> result = new AtomicReference<>();
        
        runtime.getExecution("non-existent", new ActionListener<>() {
            @Override
            public void onResponse(Map<String, Object> r) {
                result.set(r);
                latch.countDown();
            }
            
            @Override
            public void onFailure(Exception e) {
                fail("Unexpected failure: " + e.getMessage());
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(result.get());
    }

    // ============================================================================
    // Helper Methods
    // ============================================================================

    private Map<String, Object> createTestAgentDocument() {
        Map<String, Object> agent = new HashMap<>();
        agent.put("name", "test_agent");
        agent.put("goal", "Test goal");
        agent.put("instructions", "You are a test agent.");
        agent.put("enabled", true);
        agent.put("execution_mode", "autonomous");
        
        List<Map<String, Object>> skills = new ArrayList<>();
        Map<String, Object> skill1 = new HashMap<>();
        skill1.put("name", "test_skill");
        skills.add(skill1);
        agent.put("skills", skills);
        
        Map<String, Object> modelConfig = new HashMap<>();
        modelConfig.put("type", "inference_endpoint");
        modelConfig.put("endpoint_id", "test-endpoint");
        agent.put("model_config", modelConfig);
        
        agent.put("temperature", 0.7);
        agent.put("max_tokens", 2048);
        agent.put("max_iterations", 5);
        
        return agent;
    }

    private GetResponse createMockGetResponse(Map<String, Object> source) {
        GetResponse mockResponse = mock(GetResponse.class);
        when(mockResponse.isExists()).thenReturn(true);
        when(mockResponse.getSourceAsMap()).thenReturn(source);
        return mockResponse;
    }

    private IndexResponse createMockIndexResponse() {
        return new IndexResponse(
            new ShardId(".moltler_agent_executions", "_na_", 0),
            "test-id",
            1, 1, 1,
            true
        );
    }

    private SearchResponse createEmptySearchResponse() {
        SearchResponse mockResponse = mock(SearchResponse.class);
        SearchHits searchHits = SearchHits.empty(new TotalHits(0, TotalHits.Relation.EQUAL_TO), 0.0f);
        when(mockResponse.getHits()).thenReturn(searchHits);
        return mockResponse;
    }
}
