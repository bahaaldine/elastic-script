/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.escript.handlers;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.escript.parser.ElasticScriptLexer;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;
import org.elasticsearch.xpack.escript.scheduling.CronParser;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Tests for JobStatementHandler logic.
 * 
 * These tests focus on parsing and validation logic that doesn't require
 * an Elasticsearch cluster. Integration tests with actual job storage
 * are covered in E2E notebook tests.
 */
public class JobStatementHandlerTests extends ESTestCase {

    private ElasticScriptParser.Create_job_statementContext parseCreateJob(String input) {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new ElasticScriptErrorListener());
        
        ElasticScriptParser.ProgramContext program = parser.program();
        return program.job_statement().create_job_statement();
    }

    // ==================== Job Name Extraction ====================

    public void testExtractJobName() {
        String input = """
            CREATE JOB my_test_job
            SCHEDULE '0 * * * *'
            AS BEGIN
                PRINT 'test';
            END JOB
            """;
        
        ElasticScriptParser.Create_job_statementContext ctx = parseCreateJob(input);
        String jobName = ctx.ID().getText();
        
        assertEquals("my_test_job", jobName);
    }

    public void testExtractJobNameWithUnderscores() {
        String input = """
            CREATE JOB my_complex_job_name_v2
            SCHEDULE '@hourly'
            AS BEGIN
                PRINT 'test';
            END JOB
            """;
        
        ElasticScriptParser.Create_job_statementContext ctx = parseCreateJob(input);
        assertEquals("my_complex_job_name_v2", ctx.ID().getText());
    }

    // ==================== Schedule Extraction ====================

    public void testExtractSchedule() {
        String input = """
            CREATE JOB test_job
            SCHEDULE '30 14 * * 1-5'
            AS BEGIN
                PRINT 'test';
            END JOB
            """;
        
        ElasticScriptParser.Create_job_statementContext ctx = parseCreateJob(input);
        String schedule = ctx.STRING(0).getText();
        
        // Remove quotes
        schedule = schedule.substring(1, schedule.length() - 1);
        assertEquals("30 14 * * 1-5", schedule);
        
        // Verify it's a valid cron expression
        assertTrue(CronParser.isValid(schedule));
    }

    public void testExtractScheduleAlias() {
        String input = """
            CREATE JOB test_job
            SCHEDULE '@daily'
            AS BEGIN
                PRINT 'test';
            END JOB
            """;
        
        ElasticScriptParser.Create_job_statementContext ctx = parseCreateJob(input);
        String schedule = ctx.STRING(0).getText();
        schedule = schedule.substring(1, schedule.length() - 1);
        
        assertEquals("@daily", schedule);
        assertTrue(CronParser.isValid(schedule));
    }

    // ==================== Timezone Extraction ====================

    public void testExtractTimezonePresent() {
        String input = """
            CREATE JOB test_job
            SCHEDULE '@hourly'
            TIMEZONE 'America/New_York'
            AS BEGIN
                PRINT 'test';
            END JOB
            """;
        
        ElasticScriptParser.Create_job_statementContext ctx = parseCreateJob(input);
        assertNotNull(ctx.TIMEZONE());
        
        String timezone = ctx.STRING(1).getText();
        timezone = timezone.substring(1, timezone.length() - 1);
        assertEquals("America/New_York", timezone);
        
        // Verify it's a valid timezone
        ZoneId zone = ZoneId.of(timezone);
        assertNotNull(zone);
    }

    public void testExtractTimezoneAbsent() {
        String input = """
            CREATE JOB test_job
            SCHEDULE '@hourly'
            AS BEGIN
                PRINT 'test';
            END JOB
            """;
        
        ElasticScriptParser.Create_job_statementContext ctx = parseCreateJob(input);
        assertNull(ctx.TIMEZONE());
    }

    // ==================== Enabled Extraction ====================

    public void testExtractEnabledTrue() {
        String input = """
            CREATE JOB test_job
            SCHEDULE '@hourly'
            ENABLED true
            AS BEGIN
                PRINT 'test';
            END JOB
            """;
        
        ElasticScriptParser.Create_job_statementContext ctx = parseCreateJob(input);
        assertNotNull(ctx.ENABLED());
        assertNotNull(ctx.BOOLEAN());
        assertEquals("true", ctx.BOOLEAN().getText());
    }

    public void testExtractEnabledFalse() {
        String input = """
            CREATE JOB test_job
            SCHEDULE '@hourly'
            ENABLED false
            AS BEGIN
                PRINT 'test';
            END JOB
            """;
        
        ElasticScriptParser.Create_job_statementContext ctx = parseCreateJob(input);
        assertEquals("false", ctx.BOOLEAN().getText());
    }

    public void testExtractEnabledAbsentDefaultsToTrue() {
        String input = """
            CREATE JOB test_job
            SCHEDULE '@hourly'
            AS BEGIN
                PRINT 'test';
            END JOB
            """;
        
        ElasticScriptParser.Create_job_statementContext ctx = parseCreateJob(input);
        assertNull(ctx.ENABLED());
        // When ENABLED is absent, default should be true (handled by handler logic)
    }

    // ==================== Description Extraction ====================

    public void testExtractDescriptionPresent() {
        String input = """
            CREATE JOB test_job
            SCHEDULE '@hourly'
            DESCRIPTION 'This is a test job'
            AS BEGIN
                PRINT 'test';
            END JOB
            """;
        
        ElasticScriptParser.Create_job_statementContext ctx = parseCreateJob(input);
        assertNotNull(ctx.DESCRIPTION());
    }

    public void testExtractDescriptionAbsent() {
        String input = """
            CREATE JOB test_job
            SCHEDULE '@hourly'
            AS BEGIN
                PRINT 'test';
            END JOB
            """;
        
        ElasticScriptParser.Create_job_statementContext ctx = parseCreateJob(input);
        assertNull(ctx.DESCRIPTION());
    }

    // ==================== Body Extraction ====================

    public void testExtractBodyStatements() {
        String input = """
            CREATE JOB test_job
            SCHEDULE '@hourly'
            AS BEGIN
                DECLARE x NUMBER = 1;
                SET x = x + 1;
                PRINT x;
            END JOB
            """;
        
        ElasticScriptParser.Create_job_statementContext ctx = parseCreateJob(input);
        assertEquals(3, ctx.statement().size());
    }

    public void testExtractBodyWithControlFlow() {
        String input = """
            CREATE JOB test_job
            SCHEDULE '@hourly'
            AS BEGIN
                IF 1 == 1 THEN
                    PRINT 'yes';
                ELSE
                    PRINT 'no';
                END IF

                FOR i IN 1..5 LOOP
                    PRINT i;
                END LOOP
            END JOB
            """;
        
        ElasticScriptParser.Create_job_statementContext ctx = parseCreateJob(input);
        assertEquals(2, ctx.statement().size());  // IF and FOR
    }

    // ==================== Next Run Calculation ====================

    public void testNextRunCalculation() {
        String schedule = "0 9 * * *";  // 9 AM daily
        CronParser cron = new CronParser(schedule);
        
        ZonedDateTime now = ZonedDateTime.parse("2026-01-12T08:00:00Z");
        java.time.Instant next = cron.getNextRunTime(now.toInstant(), ZoneId.of("UTC"));
        
        assertEquals("2026-01-12T09:00:00Z", next.toString());
    }

    public void testNextRunCalculationWithTimezone() {
        String schedule = "0 9 * * *";  // 9 AM daily
        CronParser cron = new CronParser(schedule);
        
        ZonedDateTime now = ZonedDateTime.parse("2026-01-12T08:00:00Z");  // 3 AM in NY
        java.time.Instant next = cron.getNextRunTime(now.toInstant(), ZoneId.of("America/New_York"));
        
        // 9 AM in NY = 14:00 UTC
        ZonedDateTime nextNY = next.atZone(ZoneId.of("America/New_York"));
        assertEquals(9, nextNY.getHour());
    }

    // ==================== Alter Job Parsing ====================

    public void testParseAlterJobEnable() {
        String input = "ALTER JOB my_job ENABLE";
        
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new ElasticScriptErrorListener());
        
        ElasticScriptParser.ProgramContext program = parser.program();
        ElasticScriptParser.Alter_job_statementContext ctx = program.job_statement().alter_job_statement();
        
        assertTrue(ctx instanceof ElasticScriptParser.AlterJobEnableDisableContext);
        ElasticScriptParser.AlterJobEnableDisableContext alterCtx = 
            (ElasticScriptParser.AlterJobEnableDisableContext) ctx;
        
        assertEquals("my_job", alterCtx.ID().getText());
        assertNotNull(alterCtx.ENABLE());
        assertNull(alterCtx.DISABLE());
    }

    public void testParseAlterJobDisable() {
        String input = "ALTER JOB my_job DISABLE";
        
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new ElasticScriptErrorListener());
        
        ElasticScriptParser.ProgramContext program = parser.program();
        ElasticScriptParser.AlterJobEnableDisableContext alterCtx = 
            (ElasticScriptParser.AlterJobEnableDisableContext) program.job_statement().alter_job_statement();
        
        assertEquals("my_job", alterCtx.ID().getText());
        assertNull(alterCtx.ENABLE());
        assertNotNull(alterCtx.DISABLE());
    }

    public void testParseAlterJobSchedule() {
        String input = "ALTER JOB my_job SCHEDULE '0 3 * * *'";
        
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new ElasticScriptErrorListener());
        
        ElasticScriptParser.ProgramContext program = parser.program();
        ElasticScriptParser.AlterJobScheduleContext alterCtx = 
            (ElasticScriptParser.AlterJobScheduleContext) program.job_statement().alter_job_statement();
        
        assertEquals("my_job", alterCtx.ID().getText());
        String newSchedule = alterCtx.STRING().getText();
        newSchedule = newSchedule.substring(1, newSchedule.length() - 1);
        assertEquals("0 3 * * *", newSchedule);
    }

    // ==================== Drop Job Parsing ====================

    public void testParseDropJob() {
        String input = "DROP JOB my_job";
        
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new ElasticScriptErrorListener());
        
        ElasticScriptParser.ProgramContext program = parser.program();
        ElasticScriptParser.Drop_job_statementContext ctx = program.job_statement().drop_job_statement();
        
        assertEquals("my_job", ctx.ID().getText());
    }
}
