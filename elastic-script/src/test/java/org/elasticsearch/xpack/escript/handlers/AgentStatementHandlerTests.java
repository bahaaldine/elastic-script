/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.escript.handlers;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.internal.AdminClient;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.client.internal.IndicesAdminClient;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.parser.ElasticScriptLexer;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;

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
 * Tests for AgentStatementHandler.
 */
public class AgentStatementHandlerTests extends ESTestCase {

    private Client mockClient;
    private ExecutionContext context;
    private AgentStatementHandler handler;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mockClient = mock(Client.class);
        context = new ExecutionContext();
        handler = new AgentStatementHandler(mockClient, context);
        
        // Setup admin client mock
        AdminClient adminClient = mock(AdminClient.class);
        IndicesAdminClient indicesClient = mock(IndicesAdminClient.class);
        when(mockClient.admin()).thenReturn(adminClient);
        when(adminClient.indices()).thenReturn(indicesClient);
    }

    private ElasticScriptParser createParser(String input) {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new ElasticScriptParser(tokens);
    }

    // ============================================================================
    // CREATE AGENT Tests
    // ============================================================================

    public void testCreateAgentBasic() throws Exception {
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
        
        // Verify parsing
        assertNotNull(ctx);
        assertEquals("test_agent", ctx.ID().getText());
        assertEquals("'Test goal'", ctx.STRING(0).getText());
        assertNotNull(ctx.agent_skill_list());
        assertEquals(2, ctx.agent_skill_list().agent_skill_ref().size());
    }

    public void testCreateAgentWithInstructions() throws Exception {
        String input = """
            CREATE AGENT smart_agent
                GOAL 'Analyze data'
                INSTRUCTIONS 'You are an expert analyst. Be precise.'
                SKILLS [analyze, report]
                EXECUTION SUPERVISED
            BEGIN
                PRINT 'Analyzing';
            END AGENT;
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Create_agent_statementContext ctx = 
            parser.program().agent_statement().create_agent_statement();
        
        assertNotNull(ctx);
        assertNotNull(ctx.INSTRUCTIONS());
        // The second STRING should be the instructions
        assertTrue(ctx.STRING().size() >= 2);
    }

    public void testCreateAgentWithInferenceEndpoint() throws Exception {
        String input = """
            CREATE AGENT llm_agent
                GOAL 'Use LLM'
                SKILLS [think]
                EXECUTION AUTONOMOUS
                INFERENCE_ENDPOINT 'my-openai-endpoint'
            BEGIN
                PRINT 'Thinking';
            END AGENT;
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Create_agent_statementContext ctx = 
            parser.program().agent_statement().create_agent_statement();
        
        assertNotNull(ctx);
        assertNotNull(ctx.agent_model_config());
        assertNotNull(ctx.agent_model_config().INFERENCE_ENDPOINT());
    }

    public void testCreateAgentWithTemperature() throws Exception {
        String input = """
            CREATE AGENT precise_agent
                GOAL 'Be precise'
                SKILLS [calculate]
                EXECUTION SUPERVISED
                TEMPERATURE 0.1
            BEGIN
                PRINT 'Calculating';
            END AGENT;
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Create_agent_statementContext ctx = 
            parser.program().agent_statement().create_agent_statement();
        
        assertNotNull(ctx);
        assertNotNull(ctx.TEMPERATURE());
    }

    public void testCreateAgentWithMaxTokens() throws Exception {
        String input = """
            CREATE AGENT verbose_agent
                GOAL 'Explain in detail'
                SKILLS [explain]
                EXECUTION SUPERVISED
                MAX_TOKENS 4096
            BEGIN
                PRINT 'Explaining';
            END AGENT;
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Create_agent_statementContext ctx = 
            parser.program().agent_statement().create_agent_statement();
        
        assertNotNull(ctx);
        assertNotNull(ctx.MAX_TOKENS());
    }

    public void testCreateAgentWithMaxIterations() throws Exception {
        String input = """
            CREATE AGENT bounded_agent
                GOAL 'Limited loops'
                SKILLS [iterate]
                EXECUTION SUPERVISED
                MAX_ITERATIONS 10
            BEGIN
                PRINT 'Iterating';
            END AGENT;
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Create_agent_statementContext ctx = 
            parser.program().agent_statement().create_agent_statement();
        
        assertNotNull(ctx);
        assertNotNull(ctx.MAX_ITERATIONS());
    }

    public void testCreateAgentWithConfig() throws Exception {
        String input = """
            CREATE AGENT config_agent
                GOAL 'Has config'
                SKILLS [run]
                EXECUTION DRY_RUN
                CONFIG {"timeout": 60, "retries": 3}
            BEGIN
                PRINT 'Configured';
            END AGENT;
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Create_agent_statementContext ctx = 
            parser.program().agent_statement().create_agent_statement();
        
        assertNotNull(ctx);
        assertNotNull(ctx.documentLiteral());
    }

    public void testCreateAgentWithTriggers() throws Exception {
        String input = """
            CREATE AGENT scheduled_agent
                GOAL 'Run on schedule'
                SKILLS [check]
                EXECUTION AUTONOMOUS
                TRIGGERS [ON SCHEDULE '*/5 * * * *', ON ALERT 'high-cpu']
            BEGIN
                PRINT 'Triggered';
            END AGENT;
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Create_agent_statementContext ctx = 
            parser.program().agent_statement().create_agent_statement();
        
        assertNotNull(ctx);
        assertNotNull(ctx.agent_trigger_list());
        assertEquals(2, ctx.agent_trigger_list().agent_trigger_def().size());
    }

    public void testCreateAgentFullConfiguration() throws Exception {
        String input = """
            CREATE AGENT production_guardian
                GOAL 'Monitor production'
                INSTRUCTIONS 'You are an SRE expert. Minimize downtime.'
                SKILLS [check_health, restart_service, notify_team]
                EXECUTION SUPERVISED
                TRIGGERS [ON SCHEDULE '* * * * *', ON ALERT 'critical']
                INFERENCE_ENDPOINT 'my-llm'
                TEMPERATURE 0.3
                MAX_TOKENS 2048
                MAX_ITERATIONS 5
                CONFIG {"services": ["api", "web"]}
            BEGIN
                PRINT 'Guardian active';
            END AGENT;
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Create_agent_statementContext ctx = 
            parser.program().agent_statement().create_agent_statement();
        
        assertNotNull(ctx);
        assertEquals("production_guardian", ctx.ID().getText());
        assertNotNull(ctx.INSTRUCTIONS());
        assertNotNull(ctx.agent_skill_list());
        assertNotNull(ctx.agent_execution_mode());
        assertNotNull(ctx.agent_trigger_list());
        assertNotNull(ctx.agent_model_config());
        assertNotNull(ctx.TEMPERATURE());
        assertNotNull(ctx.MAX_TOKENS());
        assertNotNull(ctx.MAX_ITERATIONS());
        assertNotNull(ctx.documentLiteral());
    }

    // ============================================================================
    // Execution Mode Tests
    // ============================================================================

    public void testExecutionModeAutonomous() throws Exception {
        String input = """
            CREATE AGENT auto_agent
                GOAL 'Auto mode'
                SKILLS [run]
                EXECUTION AUTONOMOUS
            BEGIN
                PRINT 'Auto';
            END AGENT;
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Create_agent_statementContext ctx = 
            parser.program().agent_statement().create_agent_statement();
        
        assertNotNull(ctx.agent_execution_mode().AUTONOMOUS());
    }

    public void testExecutionModeSupervised() throws Exception {
        String input = """
            CREATE AGENT supervised_agent
                GOAL 'Supervised mode'
                SKILLS [run]
                EXECUTION SUPERVISED
            BEGIN
                PRINT 'Supervised';
            END AGENT;
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Create_agent_statementContext ctx = 
            parser.program().agent_statement().create_agent_statement();
        
        assertNotNull(ctx.agent_execution_mode().SUPERVISED());
    }

    public void testExecutionModeHumanApproval() throws Exception {
        String input = """
            CREATE AGENT approval_agent
                GOAL 'Human approval mode'
                SKILLS [run]
                EXECUTION HUMAN_APPROVAL
            BEGIN
                PRINT 'Awaiting approval';
            END AGENT;
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Create_agent_statementContext ctx = 
            parser.program().agent_statement().create_agent_statement();
        
        assertNotNull(ctx.agent_execution_mode().HUMAN_APPROVAL());
    }

    public void testExecutionModeDryRun() throws Exception {
        String input = """
            CREATE AGENT dryrun_agent
                GOAL 'Dry run mode'
                SKILLS [run]
                EXECUTION DRY_RUN
            BEGIN
                PRINT 'Dry run';
            END AGENT;
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Create_agent_statementContext ctx = 
            parser.program().agent_statement().create_agent_statement();
        
        assertNotNull(ctx.agent_execution_mode().DRY_RUN());
    }

    // ============================================================================
    // DROP AGENT Tests
    // ============================================================================

    public void testDropAgent() throws Exception {
        String input = "DROP AGENT my_agent;";
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Drop_agent_statementContext ctx = 
            parser.program().agent_statement().drop_agent_statement();
        
        assertNotNull(ctx);
        assertEquals("my_agent", ctx.ID().getText());
    }

    // ============================================================================
    // SHOW AGENTS Tests
    // ============================================================================

    public void testShowAgents() throws Exception {
        String input = "SHOW AGENTS;";
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Show_agents_statementContext ctx = 
            parser.program().agent_statement().show_agents_statement();
        
        assertNotNull(ctx);
        assertTrue(ctx instanceof ElasticScriptParser.ShowAllAgentsContext);
    }

    public void testShowAgentDetail() throws Exception {
        String input = "SHOW AGENT my_agent;";
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Show_agents_statementContext ctx = 
            parser.program().agent_statement().show_agents_statement();
        
        assertNotNull(ctx);
        assertTrue(ctx instanceof ElasticScriptParser.ShowAgentDetailContext);
    }

    public void testShowAgentHistory() throws Exception {
        String input = "SHOW AGENT my_agent HISTORY;";
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Show_agents_statementContext ctx = 
            parser.program().agent_statement().show_agents_statement();
        
        assertNotNull(ctx);
        assertTrue(ctx instanceof ElasticScriptParser.ShowAgentHistoryContext);
    }

    public void testShowAgentExecution() throws Exception {
        String input = "SHOW AGENT my_agent EXECUTION 'exec-123';";
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Show_agents_statementContext ctx = 
            parser.program().agent_statement().show_agents_statement();
        
        assertNotNull(ctx);
        assertTrue(ctx instanceof ElasticScriptParser.ShowAgentExecutionContext);
    }

    // ============================================================================
    // ALTER AGENT Tests
    // ============================================================================

    public void testAlterAgentConfig() throws Exception {
        String input = "ALTER AGENT my_agent SET CONFIG {\"timeout\": 120};";
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Alter_agent_statementContext ctx = 
            parser.program().agent_statement().alter_agent_statement();
        
        assertNotNull(ctx);
        assertTrue(ctx instanceof ElasticScriptParser.AlterAgentConfigContext);
    }

    public void testAlterAgentExecution() throws Exception {
        String input = "ALTER AGENT my_agent SET EXECUTION AUTONOMOUS;";
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Alter_agent_statementContext ctx = 
            parser.program().agent_statement().alter_agent_statement();
        
        assertNotNull(ctx);
        assertTrue(ctx instanceof ElasticScriptParser.AlterAgentExecutionContext);
    }

    public void testAlterAgentInstructions() throws Exception {
        String input = "ALTER AGENT my_agent SET INSTRUCTIONS 'New system prompt';";
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Alter_agent_statementContext ctx = 
            parser.program().agent_statement().alter_agent_statement();
        
        assertNotNull(ctx);
        assertTrue(ctx instanceof ElasticScriptParser.AlterAgentInstructionsContext);
    }

    // ============================================================================
    // ENABLE/DISABLE AGENT Tests
    // ============================================================================

    public void testEnableAgent() throws Exception {
        String input = "ENABLE AGENT my_agent;";
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Start_stop_agent_statementContext ctx = 
            parser.program().agent_statement().start_stop_agent_statement();
        
        assertNotNull(ctx);
        assertTrue(ctx instanceof ElasticScriptParser.EnableDisableAgentContext);
        ElasticScriptParser.EnableDisableAgentContext enableCtx = 
            (ElasticScriptParser.EnableDisableAgentContext) ctx;
        assertNotNull(enableCtx.ENABLE());
    }

    public void testDisableAgent() throws Exception {
        String input = "DISABLE AGENT my_agent;";
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Start_stop_agent_statementContext ctx = 
            parser.program().agent_statement().start_stop_agent_statement();
        
        assertNotNull(ctx);
        assertTrue(ctx instanceof ElasticScriptParser.EnableDisableAgentContext);
        ElasticScriptParser.EnableDisableAgentContext disableCtx = 
            (ElasticScriptParser.EnableDisableAgentContext) ctx;
        assertNotNull(disableCtx.DISABLE());
    }

    // ============================================================================
    // TRIGGER AGENT Tests
    // ============================================================================

    public void testTriggerAgent() throws Exception {
        String input = "TRIGGER AGENT my_agent;";
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Trigger_agent_statementContext ctx = 
            parser.program().agent_statement().trigger_agent_statement();
        
        assertNotNull(ctx);
        assertEquals("my_agent", ctx.ID().getText());
        assertNull(ctx.documentLiteral());
    }

    public void testTriggerAgentWithContext() throws Exception {
        String input = "TRIGGER AGENT my_agent WITH {\"priority\": \"high\", \"source\": \"manual\"};";
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Trigger_agent_statementContext ctx = 
            parser.program().agent_statement().trigger_agent_statement();
        
        assertNotNull(ctx);
        assertEquals("my_agent", ctx.ID().getText());
        assertNotNull(ctx.documentLiteral());
    }

    // ============================================================================
    // CHAT AGENT Tests
    // ============================================================================

    public void testChatAgent() throws Exception {
        String input = "CHAT AGENT my_agent 'What is the current status?';";
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Chat_agent_statementContext ctx = 
            parser.program().agent_statement().chat_agent_statement();
        
        assertNotNull(ctx);
        assertTrue(ctx instanceof ElasticScriptParser.ChatWithAgentContext);
        ElasticScriptParser.ChatWithAgentContext chatCtx = 
            (ElasticScriptParser.ChatWithAgentContext) ctx;
        assertEquals("my_agent", chatCtx.ID().getText());
    }

    public void testChatAgentWithContext() throws Exception {
        String input = "CHAT AGENT my_agent 'Analyze this alert' WITH {\"alert_id\": \"123\"};";
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Chat_agent_statementContext ctx = 
            parser.program().agent_statement().chat_agent_statement();
        
        assertNotNull(ctx);
        assertTrue(ctx instanceof ElasticScriptParser.ChatWithAgentContext);
        ElasticScriptParser.ChatWithAgentContext chatCtx = 
            (ElasticScriptParser.ChatWithAgentContext) ctx;
        assertNotNull(chatCtx.documentLiteral());
    }

    // ============================================================================
    // Skill Reference Tests
    // ============================================================================

    public void testSkillWithVersion() throws Exception {
        String input = """
            CREATE AGENT versioned_agent
                GOAL 'Use versioned skill'
                SKILLS [analyze@v1]
                EXECUTION AUTONOMOUS
            BEGIN
                PRINT 'Versioned';
            END AGENT;
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Create_agent_statementContext ctx = 
            parser.program().agent_statement().create_agent_statement();
        
        assertNotNull(ctx);
        ElasticScriptParser.Agent_skill_refContext skillRef = 
            ctx.agent_skill_list().agent_skill_ref(0);
        assertEquals(2, skillRef.ID().size()); // analyze and v1
    }

    public void testSkillWithApprovalMode() throws Exception {
        String input = """
            CREATE AGENT restricted_agent
                GOAL 'Restricted skill'
                SKILLS [dangerous_action[approval]]
                EXECUTION AUTONOMOUS
            BEGIN
                PRINT 'Restricted';
            END AGENT;
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Create_agent_statementContext ctx = 
            parser.program().agent_statement().create_agent_statement();
        
        assertNotNull(ctx);
        ElasticScriptParser.Agent_skill_refContext skillRef = 
            ctx.agent_skill_list().agent_skill_ref(0);
        assertNotNull(skillRef.LBRACKET());
    }

    // ============================================================================
    // Trigger Definition Tests
    // ============================================================================

    public void testTriggerOnSchedule() throws Exception {
        String input = """
            CREATE AGENT scheduled
                GOAL 'Scheduled'
                SKILLS [run]
                EXECUTION AUTONOMOUS
                TRIGGERS [ON SCHEDULE '*/5 * * * *']
            BEGIN
                PRINT 'Scheduled';
            END AGENT;
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Create_agent_statementContext ctx = 
            parser.program().agent_statement().create_agent_statement();
        
        ElasticScriptParser.Agent_trigger_defContext trigger = 
            ctx.agent_trigger_list().agent_trigger_def(0);
        assertNotNull(trigger.SCHEDULE());
    }

    public void testTriggerOnAlert() throws Exception {
        String input = """
            CREATE AGENT alert_responder
                GOAL 'Respond to alerts'
                SKILLS [respond]
                EXECUTION AUTONOMOUS
                TRIGGERS [ON ALERT 'high-cpu-usage']
            BEGIN
                PRINT 'Alert';
            END AGENT;
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Create_agent_statementContext ctx = 
            parser.program().agent_statement().create_agent_statement();
        
        ElasticScriptParser.Agent_trigger_defContext trigger = 
            ctx.agent_trigger_list().agent_trigger_def(0);
        assertNotNull(trigger.ALERT());
    }

    public void testTriggerOnManual() throws Exception {
        String input = """
            CREATE AGENT manual_agent
                GOAL 'Manual trigger'
                SKILLS [run]
                EXECUTION AUTONOMOUS
                TRIGGERS [ON MANUAL]
            BEGIN
                PRINT 'Manual';
            END AGENT;
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Create_agent_statementContext ctx = 
            parser.program().agent_statement().create_agent_statement();
        
        ElasticScriptParser.Agent_trigger_defContext trigger = 
            ctx.agent_trigger_list().agent_trigger_def(0);
        assertNotNull(trigger.ID());
        assertEquals("MANUAL", trigger.ID().getText());
    }

    // ============================================================================
    // Model Configuration Tests
    // ============================================================================

    public void testModelConfigWithInferenceEndpoint() throws Exception {
        String input = """
            CREATE AGENT inference_agent
                GOAL 'Use inference'
                SKILLS [analyze]
                EXECUTION AUTONOMOUS
                INFERENCE_ENDPOINT 'my-azure-openai'
            BEGIN
                PRINT 'Inference';
            END AGENT;
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Create_agent_statementContext ctx = 
            parser.program().agent_statement().create_agent_statement();
        
        assertNotNull(ctx.agent_model_config());
        assertNotNull(ctx.agent_model_config().INFERENCE_ENDPOINT());
        assertEquals("'my-azure-openai'", ctx.agent_model_config().STRING().getText());
    }

    public void testModelConfigWithModel() throws Exception {
        String input = """
            CREATE AGENT model_agent
                GOAL 'Use model'
                SKILLS [analyze]
                EXECUTION AUTONOMOUS
                MODEL 'gpt-4'
            BEGIN
                PRINT 'Model';
            END AGENT;
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Create_agent_statementContext ctx = 
            parser.program().agent_statement().create_agent_statement();
        
        assertNotNull(ctx.agent_model_config());
        assertNotNull(ctx.agent_model_config().MODEL());
        assertEquals("'gpt-4'", ctx.agent_model_config().STRING().getText());
    }
}
