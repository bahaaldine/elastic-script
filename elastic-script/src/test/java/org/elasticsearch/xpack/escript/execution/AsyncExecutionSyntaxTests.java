/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.execution;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.escript.parser.ElasticScriptLexer;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;

/**
 * Tests for pipe-driven async execution syntax parsing.
 */
public class AsyncExecutionSyntaxTests extends ESTestCase {

    // ============ Async Procedure Statement Tests ============

    public void testBasicAsyncProcedure() {
        String script = """
            CREATE PROCEDURE test()
            BEGIN
                analyze_logs()
                | ON_DONE process(@result);
            END PROCEDURE
            """;
        
        ElasticScriptParser parser = createParser(script);
        ElasticScriptParser.ProcedureContext ctx = parser.procedure();
        
        assertNotNull("Procedure should parse", ctx);
        assertNotNull("Should have statements", ctx.statement());
        assertTrue("Should have async statement", 
            ctx.statement().stream().anyMatch(s -> s.async_procedure_statement() != null));
    }

    public void testAsyncProcedureWithMultiplePipes() {
        String script = """
            CREATE PROCEDURE test()
            BEGIN
                analyze_logs('logs-*')
                | ON_DONE transform(@result)
                | ON_DONE store(@result)
                | ON_FAIL alert(@error)
                | FINALLY cleanup()
                | TRACK AS 'daily-job'
                | TIMEOUT 300;
            END PROCEDURE
            """;
        
        ElasticScriptParser parser = createParser(script);
        ElasticScriptParser.ProcedureContext ctx = parser.procedure();
        
        assertNotNull("Procedure should parse", ctx);
    }

    public void testAsyncProcedureWithArguments() {
        String script = """
            CREATE PROCEDURE test()
            BEGIN
                process_data('index-*', 100, true)
                | ON_DONE handle_result(@result)
                | TRACK AS 'processing';
            END PROCEDURE
            """;
        
        ElasticScriptParser parser = createParser(script);
        ElasticScriptParser.ProcedureContext ctx = parser.procedure();
        
        assertNotNull("Procedure should parse", ctx);
    }

    // ============ Execution Control Tests ============

    public void testExecutionStatus() {
        String script = """
            CREATE PROCEDURE test()
            BEGIN
                EXECUTION('my-job') | STATUS;
            END PROCEDURE
            """;
        
        ElasticScriptParser parser = createParser(script);
        ElasticScriptParser.ProcedureContext ctx = parser.procedure();
        
        assertNotNull("Procedure should parse", ctx);
        assertTrue("Should have execution control statement",
            ctx.statement().stream().anyMatch(s -> s.execution_control_statement() != null));
    }

    public void testExecutionCancel() {
        String script = """
            CREATE PROCEDURE test()
            BEGIN
                EXECUTION('my-job') | CANCEL;
            END PROCEDURE
            """;
        
        ElasticScriptParser parser = createParser(script);
        ElasticScriptParser.ProcedureContext ctx = parser.procedure();
        
        assertNotNull("Procedure should parse", ctx);
    }

    public void testExecutionRetry() {
        String script = """
            CREATE PROCEDURE test()
            BEGIN
                EXECUTION('failed-job') | RETRY;
            END PROCEDURE
            """;
        
        ElasticScriptParser parser = createParser(script);
        ElasticScriptParser.ProcedureContext ctx = parser.procedure();
        
        assertNotNull("Procedure should parse", ctx);
    }

    public void testExecutionWait() {
        String script = """
            CREATE PROCEDURE test()
            BEGIN
                EXECUTION('my-job') | WAIT;
            END PROCEDURE
            """;
        
        ElasticScriptParser parser = createParser(script);
        ElasticScriptParser.ProcedureContext ctx = parser.procedure();
        
        assertNotNull("Procedure should parse", ctx);
    }

    public void testExecutionWaitWithTimeout() {
        String script = """
            CREATE PROCEDURE test()
            BEGIN
                EXECUTION('my-job') | WAIT TIMEOUT 60;
            END PROCEDURE
            """;
        
        ElasticScriptParser parser = createParser(script);
        ElasticScriptParser.ProcedureContext ctx = parser.procedure();
        
        assertNotNull("Procedure should parse", ctx);
    }

    // ============ Parallel Execution Tests ============

    public void testBasicParallelExecution() {
        String script = """
            CREATE PROCEDURE test()
            BEGIN
                PARALLEL [fetch_logs(), fetch_metrics()]
                | ON_ALL_DONE merge(@results);
            END PROCEDURE
            """;
        
        ElasticScriptParser parser = createParser(script);
        ElasticScriptParser.ProcedureContext ctx = parser.procedure();
        
        assertNotNull("Procedure should parse", ctx);
        assertTrue("Should have parallel statement",
            ctx.statement().stream().anyMatch(s -> s.parallel_statement() != null));
    }

    public void testParallelExecutionWithErrorHandling() {
        String script = """
            CREATE PROCEDURE test()
            BEGIN
                PARALLEL [proc1(), proc2(), proc3()]
                | ON_ALL_DONE success_handler(@results)
                | ON_ANY_FAIL error_handler(@error)
                | TRACK AS 'parallel-job'
                | TIMEOUT 120;
            END PROCEDURE
            """;
        
        ElasticScriptParser parser = createParser(script);
        ElasticScriptParser.ProcedureContext ctx = parser.procedure();
        
        assertNotNull("Procedure should parse", ctx);
    }

    public void testParallelWithArguments() {
        String script = """
            CREATE PROCEDURE test()
            BEGIN
                PARALLEL [fetch('source1'), fetch('source2'), fetch('source3')]
                | ON_ALL_DONE combine(@results);
            END PROCEDURE
            """;
        
        ElasticScriptParser parser = createParser(script);
        ElasticScriptParser.ProcedureContext ctx = parser.procedure();
        
        assertNotNull("Procedure should parse", ctx);
    }

    // ============ Combined Workflow Tests ============

    public void testCompleteAsyncWorkflow() {
        String script = """
            CREATE PROCEDURE async_analysis()
            BEGIN
                -- Start async analysis
                analyze_data('logs-*')
                | ON_DONE process_results(@result)
                | ON_FAIL notify_team(@error)
                | TRACK AS 'nightly-analysis';

                -- Check another job's status
                EXECUTION('other-job') | STATUS;

                -- Run parallel tasks
                PARALLEL [cleanup_old(), archive_results()]
                | ON_ALL_DONE finalize(@results)
                | TRACK AS 'cleanup';
            END PROCEDURE
            """;
        
        ElasticScriptParser parser = createParser(script);
        ElasticScriptParser.ProcedureContext ctx = parser.procedure();
        
        assertNotNull("Procedure should parse", ctx);
        assertEquals("Should have 3 statements", 3, 
            ctx.statement().stream().filter(s -> s.getText().length() > 1).count());
    }

    // ============ Helper Methods ============

    private ElasticScriptParser createParser(String input) {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new ElasticScriptParser(tokens);
    }
}

