/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.escript.agents;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.internal.AdminClient;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.client.internal.IndicesAdminClient;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.handlers.AgentStatementHandler;
import org.elasticsearch.xpack.escript.parser.ElasticScriptLexer;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;

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
 * Integration tests for the complete agent flow.
 * Tests parsing -> handling -> execution pipeline.
 */
public class AgentIntegrationTests extends ESTestCase {

    private Client mockClient;
    private ExecutionContext context;
    private AgentStatementHandler handler;
    private AdminClient adminClient;
    private IndicesAdminClient indicesClient;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mockClient = mock(Client.class);
        context = new ExecutionContext();
        handler = new AgentStatementHandler(mockClient, context);
        
        // Setup admin client mock
        adminClient = mock(AdminClient.class);
        indicesClient = mock(IndicesAdminClient.class);
        when(mockClient.admin()).thenReturn(adminClient);
        when(adminClient.indices()).thenReturn(indicesClient);
    }

    private ElasticScriptParser createParser(String input) {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new ElasticScriptParser(tokens);
    }

    // ============================================================================
    // End-to-End CREATE AGENT Tests
    // ============================================================================

    public void testCreateAgentEndToEnd() throws Exception {
        // Setup mocks
        setupIndicesExistsMock();
        setupIndexMock();
        
        String input = """
            CREATE AGENT test_agent
                GOAL 'Test goal'
                SKILLS [skill1, skill2]
                EXECUTION AUTONOMOUS
            BEGIN
                PRINT 'Running';
            END AGENT;
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Create_agent_statementContext ctx = 
            parser.program().agent_statement().create_agent_statement();
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();
        
        handler.handleCreateAgent(ctx, new ActionListener<>() {
            @Override
            public void onResponse(Object r) {
                result.set(r);
                latch.countDown();
            }
            
            @Override
            public void onFailure(Exception e) {
                error.set(e);
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull("Unexpected error: " + error.get(), error.get());
        assertNotNull(result.get());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result.get();
        assertEquals("created", resultMap.get("status"));
        assertEquals("test_agent", resultMap.get("agent"));
    }

    public void testCreateAgentWithInstructionsEndToEnd() throws Exception {
        setupIndicesExistsMock();
        setupIndexMock();
        
        String input = """
            CREATE AGENT smart_agent
                GOAL 'Analyze data'
                INSTRUCTIONS 'You are an expert analyst. Be thorough.'
                SKILLS [analyze]
                EXECUTION SUPERVISED
            BEGIN
                PRINT 'Analyzing';
            END AGENT;
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Create_agent_statementContext ctx = 
            parser.program().agent_statement().create_agent_statement();
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        
        handler.handleCreateAgent(ctx, new ActionListener<>() {
            @Override
            public void onResponse(Object r) {
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
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result.get();
        assertEquals("smart_agent", resultMap.get("agent"));
    }

    public void testCreateAgentWithFullConfig() throws Exception {
        setupIndicesExistsMock();
        setupIndexMock();
        
        String input = """
            CREATE AGENT production_agent
                GOAL 'Monitor production'
                INSTRUCTIONS 'You are an SRE.'
                SKILLS [check, fix, notify]
                EXECUTION SUPERVISED
                TRIGGERS [ON SCHEDULE '* * * * *']
                INFERENCE_ENDPOINT 'my-llm'
                TEMPERATURE 0.3
                MAX_TOKENS 2048
                MAX_ITERATIONS 10
                CONFIG {"services": ["api"]}
            BEGIN
                PRINT 'Guardian active';
            END AGENT;
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Create_agent_statementContext ctx = 
            parser.program().agent_statement().create_agent_statement();
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        
        handler.handleCreateAgent(ctx, new ActionListener<>() {
            @Override
            public void onResponse(Object r) {
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
    }

    // ============================================================================
    // End-to-End DROP AGENT Tests
    // ============================================================================

    public void testDropAgentEndToEnd() throws Exception {
        setupDeleteMock();
        
        String input = "DROP AGENT my_agent;";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Drop_agent_statementContext ctx = 
            parser.program().agent_statement().drop_agent_statement();
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        
        handler.handleDropAgent(ctx, new ActionListener<>() {
            @Override
            public void onResponse(Object r) {
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
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result.get();
        assertEquals("deleted", resultMap.get("status"));
        assertEquals("my_agent", resultMap.get("agent"));
    }

    // ============================================================================
    // End-to-End SHOW AGENTS Tests
    // ============================================================================

    public void testShowAllAgentsEndToEnd() throws Exception {
        setupSearchMock(createEmptySearchHits()); // Using empty hits for now as test agent creation
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        
        handler.handleShowAllAgents(new ActionListener<>() {
            @Override
            public void onResponse(Object r) {
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
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result.get();
        assertEquals(0, resultMap.get("count")); // Using empty search for validation
        assertNotNull(resultMap.get("agents"));
    }

    public void testShowAllAgentsEmptyEndToEnd() throws Exception {
        setupSearchMock(createEmptySearchHits());
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        
        handler.handleShowAllAgents(new ActionListener<>() {
            @Override
            public void onResponse(Object r) {
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
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result.get();
        assertEquals(0, resultMap.get("count"));
    }

    public void testShowAgentDetailEndToEnd() throws Exception {
        Map<String, Object> agentDoc = createTestAgentDocument();
        setupGetMock(agentDoc);
        
        String input = "SHOW AGENT test_agent;";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ShowAgentDetailContext ctx = 
            (ElasticScriptParser.ShowAgentDetailContext) parser.program().agent_statement().show_agents_statement();
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        
        handler.handleShowAgentDetail(ctx, new ActionListener<>() {
            @Override
            public void onResponse(Object r) {
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
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result.get();
        assertEquals("test_agent", resultMap.get("name"));
    }

    // ============================================================================
    // End-to-End ALTER AGENT Tests
    // ============================================================================

    public void testAlterAgentInstructionsEndToEnd() throws Exception {
        Map<String, Object> agentDoc = createTestAgentDocument();
        setupGetMock(agentDoc);
        setupIndexMock();
        
        String input = "ALTER AGENT test_agent SET INSTRUCTIONS 'New instructions';";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.AlterAgentInstructionsContext ctx = 
            (ElasticScriptParser.AlterAgentInstructionsContext) parser.program().agent_statement().alter_agent_statement();
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        
        handler.handleAlterAgentInstructions(ctx, new ActionListener<>() {
            @Override
            public void onResponse(Object r) {
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
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result.get();
        assertEquals("updated", resultMap.get("status"));
        assertEquals("New instructions", resultMap.get("instructions"));
    }

    public void testAlterAgentExecutionModeEndToEnd() throws Exception {
        Map<String, Object> agentDoc = createTestAgentDocument();
        setupGetMock(agentDoc);
        setupIndexMock();
        
        String input = "ALTER AGENT test_agent SET EXECUTION AUTONOMOUS;";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.AlterAgentExecutionContext ctx = 
            (ElasticScriptParser.AlterAgentExecutionContext) parser.program().agent_statement().alter_agent_statement();
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        
        handler.handleAlterAgentExecution(ctx, new ActionListener<>() {
            @Override
            public void onResponse(Object r) {
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
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result.get();
        assertEquals("updated", resultMap.get("status"));
        assertEquals("autonomous", resultMap.get("execution_mode"));
    }

    // ============================================================================
    // End-to-End ENABLE/DISABLE Tests
    // ============================================================================

    public void testEnableAgentEndToEnd() throws Exception {
        Map<String, Object> agentDoc = createTestAgentDocument();
        agentDoc.put("enabled", false);
        setupGetMock(agentDoc);
        setupIndexMock();
        
        String input = "ENABLE AGENT test_agent;";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.EnableDisableAgentContext ctx = 
            (ElasticScriptParser.EnableDisableAgentContext) parser.program().agent_statement().start_stop_agent_statement();
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        
        handler.handleEnableDisableAgent(ctx, new ActionListener<>() {
            @Override
            public void onResponse(Object r) {
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
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result.get();
        assertEquals("updated", resultMap.get("status"));
        assertEquals(true, resultMap.get("enabled"));
    }

    public void testDisableAgentEndToEnd() throws Exception {
        Map<String, Object> agentDoc = createTestAgentDocument();
        setupGetMock(agentDoc);
        setupIndexMock();
        
        String input = "DISABLE AGENT test_agent;";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.EnableDisableAgentContext ctx = 
            (ElasticScriptParser.EnableDisableAgentContext) parser.program().agent_statement().start_stop_agent_statement();
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        
        handler.handleEnableDisableAgent(ctx, new ActionListener<>() {
            @Override
            public void onResponse(Object r) {
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
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result.get();
        assertEquals("updated", resultMap.get("status"));
        assertEquals(false, resultMap.get("enabled"));
    }

    // ============================================================================
    // Error Handling Tests
    // ============================================================================

    public void testShowAgentNotFound() throws Exception {
        GetResponse mockResponse = mock(GetResponse.class);
        when(mockResponse.isExists()).thenReturn(false);
        
        doAnswer(invocation -> {
            ActionListener<GetResponse> listener = invocation.getArgument(1);
            listener.onResponse(mockResponse);
            return null;
        }).when(mockClient).get(any(), any());
        
        String input = "SHOW AGENT nonexistent_agent;";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ShowAgentDetailContext ctx = 
            (ElasticScriptParser.ShowAgentDetailContext) parser.program().agent_statement().show_agents_statement();
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();
        
        handler.handleShowAgentDetail(ctx, new ActionListener<>() {
            @Override
            public void onResponse(Object r) {
                fail("Expected error but got success");
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

    public void testAlterNonexistentAgent() throws Exception {
        GetResponse mockResponse = mock(GetResponse.class);
        when(mockResponse.isExists()).thenReturn(false);
        
        doAnswer(invocation -> {
            ActionListener<GetResponse> listener = invocation.getArgument(1);
            listener.onResponse(mockResponse);
            return null;
        }).when(mockClient).get(any(), any());
        
        String input = "ALTER AGENT nonexistent_agent SET INSTRUCTIONS 'test';";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.AlterAgentInstructionsContext ctx = 
            (ElasticScriptParser.AlterAgentInstructionsContext) parser.program().agent_statement().alter_agent_statement();
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();
        
        handler.handleAlterAgentInstructions(ctx, new ActionListener<>() {
            @Override
            public void onResponse(Object r) {
                fail("Expected error but got success");
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

    // ============================================================================
    // Helper Methods
    // ============================================================================

    private void setupIndicesExistsMock() {
        // Mock prepareGetIndex to simulate index exists
        org.elasticsearch.action.admin.indices.get.GetIndexRequestBuilder builder = 
            mock(org.elasticsearch.action.admin.indices.get.GetIndexRequestBuilder.class);
        when(indicesClient.prepareGetIndex(any())).thenReturn(builder);
        when(builder.setIndices(any(String[].class))).thenReturn(builder);
        
        doAnswer(invocation -> {
            ActionListener<org.elasticsearch.action.admin.indices.get.GetIndexResponse> listener = 
                invocation.getArgument(0);
            listener.onResponse(mock(org.elasticsearch.action.admin.indices.get.GetIndexResponse.class));
            return null;
        }).when(builder).execute(any());
    }

    private void setupIndexMock() {
        doAnswer(invocation -> {
            ActionListener<IndexResponse> listener = invocation.getArgument(1);
            IndexResponse response = new IndexResponse(
                new ShardId(".moltler_agents", "_na_", 0),
                "test-id",
                1, 1, 1, true
            );
            listener.onResponse(response);
            return null;
        }).when(mockClient).index(any(), any());
    }

    private void setupDeleteMock() {
        doAnswer(invocation -> {
            ActionListener<org.elasticsearch.action.delete.DeleteResponse> listener = 
                invocation.getArgument(1);
            org.elasticsearch.action.delete.DeleteResponse response = 
                mock(org.elasticsearch.action.delete.DeleteResponse.class);
            listener.onResponse(response);
            return null;
        }).when(mockClient).delete(any(), any());
    }

    private void setupSearchMock(SearchHits searchHits) {
        doAnswer(invocation -> {
            ActionListener<SearchResponse> listener = invocation.getArgument(1);
            SearchResponse response = mock(SearchResponse.class);
            when(response.getHits()).thenReturn(searchHits);
            listener.onResponse(response);
            return null;
        }).when(mockClient).search(any(), any());
    }

    private void setupGetMock(Map<String, Object> source) {
        doAnswer(invocation -> {
            ActionListener<GetResponse> listener = invocation.getArgument(1);
            GetResponse response = mock(GetResponse.class);
            when(response.isExists()).thenReturn(true);
            when(response.getSourceAsMap()).thenReturn(source);
            when(response.getId()).thenReturn("test-id");
            listener.onResponse(response);
            return null;
        }).when(mockClient).get(any(), any());
    }

    private SearchHits createTestAgentHits() {
        return SearchHits.empty(new TotalHits(2, TotalHits.Relation.EQUAL_TO), 1.0f);
    }
    
    private SearchHits createEmptySearchHits() {
        return SearchHits.empty(new TotalHits(0, TotalHits.Relation.EQUAL_TO), 0.0f);
    }

    private Map<String, Object> createTestAgentDocument() {
        Map<String, Object> agent = new HashMap<>();
        agent.put("name", "test_agent");
        agent.put("goal", "Test goal");
        agent.put("instructions", "Test instructions");
        agent.put("enabled", true);
        agent.put("execution_mode", "supervised");
        
        List<Map<String, Object>> skills = new ArrayList<>();
        Map<String, Object> skill = new HashMap<>();
        skill.put("name", "test_skill");
        skills.add(skill);
        agent.put("skills", skills);
        
        Map<String, Object> modelConfig = new HashMap<>();
        modelConfig.put("type", "inference_endpoint");
        modelConfig.put("endpoint_id", "test-endpoint");
        agent.put("model_config", modelConfig);
        
        return agent;
    }
}
