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
                ON SCHEDULE 'every 5 minutes', ALERT 'HighCPU'
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
        assertNotNull(ctx.agent_statement().create_agent_statement().MODEL());
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
        assertNotNull(ctx.agent_statement().alter_agent_statement());
    }
    
    public void testDisableAgent() {
        String input = "DISABLE AGENT my_agent;";
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.agent_statement());
        assertNotNull(ctx.agent_statement().alter_agent_statement());
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
}
