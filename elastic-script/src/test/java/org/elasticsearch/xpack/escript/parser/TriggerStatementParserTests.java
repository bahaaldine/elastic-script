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
import org.elasticsearch.xpack.escript.handlers.ElasticScriptErrorListener;

/**
 * Tests for parsing TRIGGER statements (CREATE TRIGGER, ALTER TRIGGER, DROP TRIGGER, SHOW TRIGGERS).
 */
public class TriggerStatementParserTests extends ESTestCase {

    private ElasticScriptParser createParser(String input) {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new ElasticScriptErrorListener());
        return parser;
    }

    // ==================== CREATE TRIGGER ====================

    public void testCreateTriggerBasic() {
        String input = """
            CREATE TRIGGER error_alert
            ON INDEX 'logs-*'
            WHEN level == 'ERROR'
            AS BEGIN
                PRINT 'Error detected';
            END TRIGGER
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull(program);
        assertNotNull(program.trigger_statement());
        assertNotNull(program.trigger_statement().create_trigger_statement());
        
        ElasticScriptParser.Create_trigger_statementContext ctx = program.trigger_statement().create_trigger_statement();
        assertEquals("error_alert", ctx.ID().getText());
        assertEquals("'logs-*'", ctx.STRING(0).getText());
    }

    public void testCreateTriggerWithPolling() {
        String input = """
            CREATE TRIGGER frequent_check
            ON INDEX 'events-*'
            WHEN severity >= 5
            EVERY 30 SECONDS
            AS BEGIN
                CALL SLACK_SEND('#alerts', 'High severity event');
            END TRIGGER
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        ElasticScriptParser.Create_trigger_statementContext ctx = program.trigger_statement().create_trigger_statement();
        assertEquals("frequent_check", ctx.ID().getText());
        assertNotNull(ctx.interval_expression());
    }

    public void testCreateTriggerEveryMinute() {
        String input = """
            CREATE TRIGGER minute_check
            ON INDEX 'metrics-*'
            WHEN cpu_usage > 90
            EVERY 1 MINUTE
            AS BEGIN
                PRINT 'CPU alert';
            END TRIGGER
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull(program.trigger_statement().create_trigger_statement());
        ElasticScriptParser.Interval_expressionContext interval = 
            program.trigger_statement().create_trigger_statement().interval_expression();
        assertNotNull(interval);
        assertNotNull(interval.MINUTE());
    }

    public void testCreateTriggerEveryHours() {
        String input = """
            CREATE TRIGGER hourly_check
            ON INDEX 'data-*'
            WHEN status == 'pending'
            EVERY 2 HOURS
            AS BEGIN
                PRINT 'Processing pending items';
            END TRIGGER
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        ElasticScriptParser.Interval_expressionContext interval = 
            program.trigger_statement().create_trigger_statement().interval_expression();
        assertNotNull(interval);
        assertNotNull(interval.HOURS());
    }

    public void testCreateTriggerWithEnabled() {
        String input = """
            CREATE TRIGGER disabled_trigger
            ON INDEX 'logs-*'
            WHEN level == 'FATAL'
            ENABLED false
            AS BEGIN
                PRINT 'Fatal error';
            END TRIGGER
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        ElasticScriptParser.Create_trigger_statementContext ctx = program.trigger_statement().create_trigger_statement();
        assertNotNull(ctx.ENABLED());
        assertNotNull(ctx.BOOLEAN());
    }

    public void testCreateTriggerWithDescription() {
        String input = """
            CREATE TRIGGER documented_trigger
            ON INDEX 'events-*'
            WHEN type == 'alert'
            DESCRIPTION 'Handles alert events'
            AS BEGIN
                PRINT 'Alert received';
            END TRIGGER
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        ElasticScriptParser.Create_trigger_statementContext ctx = program.trigger_statement().create_trigger_statement();
        assertNotNull(ctx.DESCRIPTION());
    }

    public void testCreateTriggerWithAllOptions() {
        String input = """
            CREATE TRIGGER full_trigger
            ON INDEX 'security-logs-*'
            WHEN event_type == 'intrusion' AND severity >= 8
            EVERY 10 SECONDS
            ENABLED true
            DESCRIPTION 'Security intrusion detector'
            AS BEGIN
                DECLARE i NUMBER;
                FOR i IN 1..5 LOOP
                    PRINT 'Security alert';
                END LOOP
            END TRIGGER
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull(program.trigger_statement());
        ElasticScriptParser.Create_trigger_statementContext ctx = program.trigger_statement().create_trigger_statement();
        
        assertEquals("full_trigger", ctx.ID().getText());
        assertNotNull(ctx.interval_expression());
        assertNotNull(ctx.ENABLED());
        assertNotNull(ctx.DESCRIPTION());
    }

    public void testCreateTriggerComplexCondition() {
        String input = """
            CREATE TRIGGER complex_trigger
            ON INDEX 'logs-*'
            WHEN (level == 'ERROR' OR level == 'FATAL') AND service == 'payment'
            AS BEGIN
                PRINT 'Payment service error';
            END TRIGGER
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull(program.trigger_statement().create_trigger_statement());
    }

    public void testCreateTriggerDocumentsBinding() {
        String input = """
            CREATE TRIGGER docs_trigger
            ON INDEX 'events-*'
            WHEN processed == false
            AS BEGIN
                PRINT 'Processing documents';

                DECLARE i NUMBER;
                FOR i IN 1..5 LOOP
                    PRINT i;
                END LOOP
            END TRIGGER
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull(program.trigger_statement().create_trigger_statement());
    }

    // ==================== ALTER TRIGGER ====================

    public void testAlterTriggerEnable() {
        String input = "ALTER TRIGGER my_trigger ENABLE";
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull(program.trigger_statement());
        assertNotNull(program.trigger_statement().alter_trigger_statement());
        assertTrue(program.trigger_statement().alter_trigger_statement() instanceof ElasticScriptParser.AlterTriggerEnableDisableContext);
        
        ElasticScriptParser.AlterTriggerEnableDisableContext ctx = 
            (ElasticScriptParser.AlterTriggerEnableDisableContext) program.trigger_statement().alter_trigger_statement();
        assertEquals("my_trigger", ctx.ID().getText());
        assertNotNull(ctx.ENABLE());
    }

    public void testAlterTriggerDisable() {
        String input = "ALTER TRIGGER my_trigger DISABLE";
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        ElasticScriptParser.AlterTriggerEnableDisableContext ctx = 
            (ElasticScriptParser.AlterTriggerEnableDisableContext) program.trigger_statement().alter_trigger_statement();
        assertEquals("my_trigger", ctx.ID().getText());
        assertNotNull(ctx.DISABLE());
    }

    public void testAlterTriggerPolling() {
        String input = "ALTER TRIGGER my_trigger EVERY 45 SECONDS";
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertTrue(program.trigger_statement().alter_trigger_statement() instanceof ElasticScriptParser.AlterTriggerIntervalContext);
        
        ElasticScriptParser.AlterTriggerIntervalContext ctx = 
            (ElasticScriptParser.AlterTriggerIntervalContext) program.trigger_statement().alter_trigger_statement();
        assertEquals("my_trigger", ctx.ID().getText());
        assertNotNull(ctx.interval_expression());
    }

    // ==================== DROP TRIGGER ====================

    public void testDropTrigger() {
        String input = "DROP TRIGGER my_trigger";
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull(program.trigger_statement());
        assertNotNull(program.trigger_statement().drop_trigger_statement());
        assertEquals("my_trigger", program.trigger_statement().drop_trigger_statement().ID().getText());
    }

    // ==================== SHOW TRIGGERS ====================

    public void testShowTriggers() {
        String input = "SHOW TRIGGERS";
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull(program.trigger_statement());
        assertNotNull(program.trigger_statement().show_triggers_statement());
        assertTrue(program.trigger_statement().show_triggers_statement() instanceof ElasticScriptParser.ShowAllTriggersContext);
    }

    public void testShowTriggerDetail() {
        String input = "SHOW TRIGGER my_trigger";
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertTrue(program.trigger_statement().show_triggers_statement() instanceof ElasticScriptParser.ShowTriggerDetailContext);
        
        ElasticScriptParser.ShowTriggerDetailContext ctx = 
            (ElasticScriptParser.ShowTriggerDetailContext) program.trigger_statement().show_triggers_statement();
        assertEquals("my_trigger", ctx.ID().getText());
    }

    public void testShowTriggerRuns() {
        String input = "SHOW TRIGGER RUNS FOR my_trigger";
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertTrue(program.trigger_statement().show_triggers_statement() instanceof ElasticScriptParser.ShowTriggerRunsContext);
        
        ElasticScriptParser.ShowTriggerRunsContext ctx = 
            (ElasticScriptParser.ShowTriggerRunsContext) program.trigger_statement().show_triggers_statement();
        assertEquals("my_trigger", ctx.ID().getText());
    }

    // ==================== Multiple Index Patterns ====================

    public void testCreateTriggerWildcardIndex() {
        String input = """
            CREATE TRIGGER wildcard_trigger
            ON INDEX 'logs-*-2026.*'
            WHEN status == 'failed'
            AS BEGIN
                PRINT 'Failed job detected';
            END TRIGGER
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull(program.trigger_statement().create_trigger_statement());
    }

    // ==================== Edge Cases ====================

    public void testCreateTriggerMinimalBody() {
        String input = """
            CREATE TRIGGER minimal
            ON INDEX 'test'
            WHEN x == 1
            AS BEGIN
                PRINT 'x';
            END TRIGGER
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull(program.trigger_statement().create_trigger_statement());
    }

    public void testCreateTriggerNumericCondition() {
        String input = """
            CREATE TRIGGER numeric_trigger
            ON INDEX 'metrics-*'
            WHEN value > 100.5 AND count <= 1000
            AS BEGIN
                PRINT 'Threshold exceeded';
            END TRIGGER
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull(program.trigger_statement().create_trigger_statement());
    }
}
