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
 * Tests for parsing JOB statements (CREATE JOB, ALTER JOB, DROP JOB, SHOW JOBS).
 */
public class JobStatementParserTests extends ESTestCase {

    private ElasticScriptParser createParser(String input) {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new ElasticScriptErrorListener());
        return parser;
    }

    // ==================== CREATE JOB ====================

    public void testCreateJobBasic() {
        String input = """
            CREATE JOB my_job
            SCHEDULE '0 2 * * *'
            AS BEGIN
                PRINT 'Hello';
            END JOB
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull(program);
        assertNotNull(program.job_statement());
        assertNotNull(program.job_statement().create_job_statement());
        
        ElasticScriptParser.Create_job_statementContext ctx = program.job_statement().create_job_statement();
        assertEquals("my_job", ctx.ID().getText());
        assertEquals("'0 2 * * *'", ctx.STRING(0).getText());
    }

    public void testCreateJobWithTimezone() {
        String input = """
            CREATE JOB tz_job
            SCHEDULE '@daily'
            TIMEZONE 'America/New_York'
            AS BEGIN
                PRINT 'Running in NY timezone';
            END JOB
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull(program.job_statement());
        ElasticScriptParser.Create_job_statementContext ctx = program.job_statement().create_job_statement();
        
        assertEquals("tz_job", ctx.ID().getText());
        assertNotNull(ctx.TIMEZONE());
        assertEquals("'America/New_York'", ctx.STRING(1).getText());
    }

    public void testCreateJobWithEnabled() {
        String input = """
            CREATE JOB disabled_job
            SCHEDULE '*/5 * * * *'
            ENABLED false
            AS BEGIN
                PRINT 'This job is disabled';
            END JOB
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        ElasticScriptParser.Create_job_statementContext ctx = program.job_statement().create_job_statement();
        assertEquals("disabled_job", ctx.ID().getText());
        assertNotNull(ctx.ENABLED());
        assertNotNull(ctx.BOOLEAN());
    }

    public void testCreateJobWithDescription() {
        String input = """
            CREATE JOB documented_job
            SCHEDULE '@hourly'
            DESCRIPTION 'This job runs every hour'
            AS BEGIN
                PRINT 'Running';
            END JOB
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        ElasticScriptParser.Create_job_statementContext ctx = program.job_statement().create_job_statement();
        assertEquals("documented_job", ctx.ID().getText());
        assertNotNull(ctx.DESCRIPTION());
    }

    public void testCreateJobWithAllOptions() {
        String input = """
            CREATE JOB full_job
            SCHEDULE '0 9 * * 1-5'
            TIMEZONE 'Europe/London'
            ENABLED true
            DESCRIPTION 'Runs weekdays at 9 AM London time'
            AS BEGIN
                DECLARE msg STRING = 'Good morning!';
                PRINT msg;
                CALL SLACK_SEND('#general', msg);
            END JOB
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull(program.job_statement());
        ElasticScriptParser.Create_job_statementContext ctx = program.job_statement().create_job_statement();
        
        assertEquals("full_job", ctx.ID().getText());
        assertNotNull(ctx.TIMEZONE());
        assertNotNull(ctx.ENABLED());
        assertNotNull(ctx.DESCRIPTION());
        assertTrue(ctx.statement().size() >= 3);
    }

    public void testCreateJobComplexBody() {
        String input = """
            CREATE JOB complex_job
            SCHEDULE '0 * * * *'
            AS BEGIN
                DECLARE errors NUMBER;
                SET errors = 50;

                IF errors > 100 THEN
                    PRINT 'High error count';
                END IF

                FOR i IN 1..5 LOOP
                    PRINT 'Iteration ' || i;
                END LOOP
            END JOB
            """;

        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull(program.job_statement());
        assertNotNull(program.job_statement().create_job_statement());
    }

    // ==================== ALTER JOB ====================

    public void testAlterJobEnable() {
        String input = "ALTER JOB my_job ENABLE";
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull(program.job_statement());
        assertNotNull(program.job_statement().alter_job_statement());
        assertTrue(program.job_statement().alter_job_statement() instanceof ElasticScriptParser.AlterJobEnableDisableContext);
        
        ElasticScriptParser.AlterJobEnableDisableContext ctx = 
            (ElasticScriptParser.AlterJobEnableDisableContext) program.job_statement().alter_job_statement();
        assertEquals("my_job", ctx.ID().getText());
        assertNotNull(ctx.ENABLE());
    }

    public void testAlterJobDisable() {
        String input = "ALTER JOB my_job DISABLE";
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        ElasticScriptParser.AlterJobEnableDisableContext ctx = 
            (ElasticScriptParser.AlterJobEnableDisableContext) program.job_statement().alter_job_statement();
        assertEquals("my_job", ctx.ID().getText());
        assertNotNull(ctx.DISABLE());
    }

    public void testAlterJobSchedule() {
        String input = "ALTER JOB my_job SCHEDULE '0 3 * * *'";
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertTrue(program.job_statement().alter_job_statement() instanceof ElasticScriptParser.AlterJobScheduleContext);
        
        ElasticScriptParser.AlterJobScheduleContext ctx = 
            (ElasticScriptParser.AlterJobScheduleContext) program.job_statement().alter_job_statement();
        assertEquals("my_job", ctx.ID().getText());
        assertEquals("'0 3 * * *'", ctx.STRING().getText());
    }

    // ==================== DROP JOB ====================

    public void testDropJob() {
        String input = "DROP JOB my_job";
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull(program.job_statement());
        assertNotNull(program.job_statement().drop_job_statement());
        assertEquals("my_job", program.job_statement().drop_job_statement().ID().getText());
    }

    // ==================== SHOW JOBS ====================

    public void testShowJobs() {
        String input = "SHOW JOBS";
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull(program.job_statement());
        assertNotNull(program.job_statement().show_jobs_statement());
        assertTrue(program.job_statement().show_jobs_statement() instanceof ElasticScriptParser.ShowAllJobsContext);
    }

    public void testShowJobDetail() {
        String input = "SHOW JOB my_job";
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertTrue(program.job_statement().show_jobs_statement() instanceof ElasticScriptParser.ShowJobDetailContext);
        
        ElasticScriptParser.ShowJobDetailContext ctx = 
            (ElasticScriptParser.ShowJobDetailContext) program.job_statement().show_jobs_statement();
        assertEquals("my_job", ctx.ID().getText());
    }

    public void testShowJobRuns() {
        String input = "SHOW JOB RUNS FOR my_job";
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertTrue(program.job_statement().show_jobs_statement() instanceof ElasticScriptParser.ShowJobRunsContext);
        
        ElasticScriptParser.ShowJobRunsContext ctx = 
            (ElasticScriptParser.ShowJobRunsContext) program.job_statement().show_jobs_statement();
        assertEquals("my_job", ctx.ID().getText());
    }
}
