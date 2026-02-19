/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.parser;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.test.ESTestCase;

/**
 * Tests for Moltler agent statement parsing.
 */
public class AgentParserTests extends ESTestCase {
    
    private ElasticScriptParser createParser(String input) {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new ElasticScriptParser(tokens);
    }
    
    public void testCreateAgentBasic() {
        String input = """
            CREATE AGENT incident_responder
                GOAL 'Respond to production incidents'
                SKILLS [diagnose_issue, notify_team]
                EXECUTION SUPERVISED
            BEGIN
                PRINT 'Agent body';
            END AGENT;
            """;
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.agent_statement());
        assertNotNull(ctx.agent_statement().create_agent_statement());
        assertEquals("incident_responder", ctx.agent_statement().create_agent_statement().ID().getText());
    }
    
    public void testCreateAgentWithTriggers() {
        String input = """
            CREATE AGENT scheduled_agent
                GOAL 'Run on schedule'
                SKILLS [check_health]
                EXECUTION AUTONOMOUS
                TRIGGERS [ON SCHEDULE 'every 5 minutes', ON ALERT 'HighCPU']
            BEGIN
                PRINT 'Running';
            END AGENT;
            """;
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.agent_statement().create_agent_statement());
        assertNotNull(ctx.agent_statement().create_agent_statement().agent_trigger_list());
    }
    
    public void testCreateAgentWithModel() {
        String input = """
            CREATE AGENT ai_agent
                GOAL 'Use AI for decisions'
                SKILLS [analyze]
                EXECUTION HUMAN_APPROVAL
                MODEL 'gpt-4'
            BEGIN
                PRINT 'AI agent';
            END AGENT;
            """;
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.agent_statement().create_agent_statement());
        assertNotNull(ctx.agent_statement().create_agent_statement().agent_model_config());
        assertNotNull(ctx.agent_statement().create_agent_statement().agent_model_config().MODEL());
    }
    
    public void testCreateAgentWithConfig() {
        String input = """
            CREATE AGENT config_agent
                GOAL 'Has config'
                SKILLS [run]
                EXECUTION DRY_RUN
                CONFIG {"max_retries": 3, "timeout": 60}
            BEGIN
                PRINT 'Config agent';
            END AGENT;
            """;
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.agent_statement().create_agent_statement());
        assertNotNull(ctx.agent_statement().create_agent_statement().documentLiteral());
    }
    
    public void testDropAgent() {
        String input = "DROP AGENT incident_responder;";
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.agent_statement());
        assertNotNull(ctx.agent_statement().drop_agent_statement());
        assertEquals("incident_responder", ctx.agent_statement().drop_agent_statement().ID().getText());
    }
    
    public void testShowAgents() {
        String input = "SHOW AGENTS;";
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.agent_statement());
        assertNotNull(ctx.agent_statement().show_agents_statement());
    }
    
    public void testShowAgentDetail() {
        String input = "SHOW AGENT my_agent;";
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.agent_statement());
        assertNotNull(ctx.agent_statement().show_agents_statement());
    }
    
    public void testShowAgentHistory() {
        String input = "SHOW AGENT my_agent HISTORY;";
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.agent_statement());
        assertNotNull(ctx.agent_statement().show_agents_statement());
    }
    
    public void testTriggerAgent() {
        String input = "TRIGGER AGENT my_agent;";
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.agent_statement());
        assertNotNull(ctx.agent_statement().trigger_agent_statement());
    }
    
    public void testTriggerAgentWithContext() {
        String input = "TRIGGER AGENT my_agent WITH {\"priority\": \"high\"};";
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.agent_statement().trigger_agent_statement());
        assertNotNull(ctx.agent_statement().trigger_agent_statement().documentLiteral());
    }
    
    public void testEnableAgent() {
        String input = "ENABLE AGENT my_agent;";
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.agent_statement());
        assertNotNull(ctx.agent_statement().start_stop_agent_statement());
    }
    
    public void testDisableAgent() {
        String input = "DISABLE AGENT my_agent;";
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.agent_statement());
        assertNotNull(ctx.agent_statement().start_stop_agent_statement());
    }
    
    public void testAlterAgentExecution() {
        String input = "ALTER AGENT my_agent EXECUTION AUTONOMOUS;";
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.agent_statement());
        assertNotNull(ctx.agent_statement().alter_agent_statement());
    }
    
    public void testExecutionModes() {
        // Test all execution modes parse correctly
        String[] modes = {"AUTONOMOUS", "SUPERVISED", "HUMAN_APPROVAL", "DRY_RUN"};
        
        for (String mode : modes) {
            String input = String.format("""
                CREATE AGENT test_agent
                    GOAL 'Test'
                    SKILLS [test]
                    EXECUTION %s
                BEGIN
                    PRINT 'test';
                END AGENT;
                """, mode);
            
            ElasticScriptParser parser = createParser(input);
            ElasticScriptParser.ProgramContext ctx = parser.program();
            assertNotNull("Failed to parse execution mode: " + mode, ctx.agent_statement().create_agent_statement());
        }
    }
    
    public void testCreateAgentWithInstructions() {
        String input = """
            CREATE AGENT smart_agent
                GOAL 'Respond to incidents'
                INSTRUCTIONS 'You are an SRE expert. Always explain your reasoning.'
                SKILLS [diagnose, fix, notify]
                EXECUTION SUPERVISED
            BEGIN
                PRINT 'Agent running';
            END AGENT;
            """;
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.agent_statement().create_agent_statement());
        assertNotNull(ctx.agent_statement().create_agent_statement().INSTRUCTIONS());
    }
    
    public void testCreateAgentWithInferenceEndpoint() {
        String input = """
            CREATE AGENT llm_agent
                GOAL 'AI-powered analysis'
                INSTRUCTIONS 'Be concise and precise.'
                SKILLS [analyze_logs, summarize]
                EXECUTION AUTONOMOUS
                INFERENCE_ENDPOINT 'my-openai-endpoint'
            BEGIN
                PRINT 'LLM agent';
            END AGENT;
            """;
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.agent_statement().create_agent_statement());
        assertNotNull(ctx.agent_statement().create_agent_statement().agent_model_config());
        assertNotNull(ctx.agent_statement().create_agent_statement().agent_model_config().INFERENCE_ENDPOINT());
    }
    
    public void testCreateAgentWithTemperature() {
        String input = """
            CREATE AGENT precise_agent
                GOAL 'Make deterministic decisions'
                SKILLS [analyze]
                EXECUTION SUPERVISED
                INFERENCE_ENDPOINT 'my-llm'
                TEMPERATURE 0.3
            BEGIN
                PRINT 'Precise agent';
            END AGENT;
            """;
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.agent_statement().create_agent_statement());
        assertNotNull(ctx.agent_statement().create_agent_statement().TEMPERATURE());
    }
    
    public void testCreateAgentWithMaxTokens() {
        String input = """
            CREATE AGENT verbose_agent
                GOAL 'Provide detailed analysis'
                SKILLS [analyze]
                EXECUTION SUPERVISED
                INFERENCE_ENDPOINT 'my-llm'
                MAX_TOKENS 4096
            BEGIN
                PRINT 'Verbose agent';
            END AGENT;
            """;
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.agent_statement().create_agent_statement());
        assertNotNull(ctx.agent_statement().create_agent_statement().MAX_TOKENS());
    }
    
    public void testCreateAgentWithMaxIterations() {
        String input = """
            CREATE AGENT bounded_agent
                GOAL 'Limited iterations'
                SKILLS [analyze]
                EXECUTION SUPERVISED
                MAX_ITERATIONS 5
            BEGIN
                PRINT 'Bounded agent';
            END AGENT;
            """;
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.agent_statement().create_agent_statement());
        assertNotNull(ctx.agent_statement().create_agent_statement().MAX_ITERATIONS());
    }
    
    public void testCreateAgentFullConfiguration() {
        String input = """
            CREATE AGENT production_guardian
                GOAL 'Monitor and protect production systems'
                INSTRUCTIONS 'You are an expert SRE. Minimize user impact. Explain your reasoning.'
                SKILLS [check_health, analyze_metrics, restart_service, notify_team]
                EXECUTION SUPERVISED
                TRIGGERS [ON SCHEDULE '*/5 * * * *', ON ALERT 'critical-alert']
                INFERENCE_ENDPOINT 'my-openai'
                TEMPERATURE 0.3
                MAX_TOKENS 2048
                MAX_ITERATIONS 10
                CONFIG {"services": ["api", "web"], "escalation_timeout": "10m"}
            BEGIN
                PRINT 'Guardian running';
            END AGENT;
            """;
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        ElasticScriptParser.Create_agent_statementContext agentCtx = ctx.agent_statement().create_agent_statement();
        assertNotNull(agentCtx);
        assertEquals("production_guardian", agentCtx.ID().getText());
        assertNotNull(agentCtx.INSTRUCTIONS());
        assertNotNull(agentCtx.agent_model_config());
        assertNotNull(agentCtx.TEMPERATURE());
        assertNotNull(agentCtx.MAX_TOKENS());
        assertNotNull(agentCtx.MAX_ITERATIONS());
        assertNotNull(agentCtx.documentLiteral());
    }
    
    public void testChatAgent() {
        String input = "CHAT AGENT my_agent 'What is the current health status?';";
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.agent_statement());
        assertNotNull(ctx.agent_statement().chat_agent_statement());
    }
    
    public void testChatAgentWithContext() {
        String input = "CHAT AGENT my_agent 'Analyze this alert' WITH {\"alert_id\": \"123\", \"severity\": \"critical\"};";
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.agent_statement().chat_agent_statement());
        // Check that documentLiteral is present
        ElasticScriptParser.Chat_agent_statementContext chatCtx = ctx.agent_statement().chat_agent_statement();
        assertTrue(chatCtx instanceof ElasticScriptParser.ChatWithAgentContext);
    }
    
    public void testAlterAgentInstructions() {
        String input = "ALTER AGENT my_agent SET INSTRUCTIONS 'New system prompt for the agent';";
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.agent_statement());
        assertNotNull(ctx.agent_statement().alter_agent_statement());
    }
}
