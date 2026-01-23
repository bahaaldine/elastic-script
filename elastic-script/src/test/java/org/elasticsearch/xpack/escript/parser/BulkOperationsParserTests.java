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
import org.junit.Test;

/**
 * Parser tests for FORALL and BULK COLLECT syntax.
 *
 * Tests:
 * - FORALL element IN collection action
 * - FORALL with SAVE EXCEPTIONS
 * - BULK COLLECT INTO variable FROM query
 */
public class BulkOperationsParserTests extends ESTestCase {

    private ElasticScriptParser createParser(String input) {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new ElasticScriptParser(tokens);
    }

    // ==================== FORALL Tests ====================

    @Test
    public void testParseForallWithCall() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE docs ARRAY = [{'a': 1}, {'a': 2}];
                FORALL doc IN docs CALL process(doc);
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseForallWithFunctionCall() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE docs ARRAY = [{'name': 'test'}];
                FORALL doc IN docs INDEX_DOCUMENT('my-index', doc);
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseForallWithSaveExceptions() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE docs ARRAY = [{'a': 1}, {'a': 2}];
                FORALL doc IN docs CALL process(doc) SAVE EXCEPTIONS;
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseForallWithComplexCollection() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE docs ARRAY = ESQL_QUERY('FROM logs-* | LIMIT 100');
                FORALL doc IN docs CALL archive_document(doc) SAVE EXCEPTIONS;
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== BULK COLLECT Tests ====================

    @Test
    public void testParseBulkCollect() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                BULK COLLECT INTO all_errors FROM logs-* | WHERE level = 'ERROR' | LIMIT 1000;
                PRINT 'Collected ' || ARRAY_LENGTH(all_errors) || ' errors';
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseBulkCollectWithComplexQuery() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                BULK COLLECT INTO metrics
                    FROM metrics-*
                    | WHERE cpu_percent > 90
                    | SORT @timestamp DESC
                    | LIMIT 5000;
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== Combined Tests ====================

    @Test
    public void testParseBulkCollectThenForall() {
        String input = """
            CREATE PROCEDURE process_all_errors()
            BEGIN
                BULK COLLECT INTO errors FROM logs-* | WHERE level = 'ERROR' | LIMIT 5000;

                FORALL err IN errors CALL notify_admin(err) SAVE EXCEPTIONS;

                PRINT 'Processed ' || ARRAY_LENGTH(errors) || ' errors';
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseForallInLoop() {
        String input = """
            CREATE PROCEDURE batch_process()
            BEGIN
                DECLARE log_cursor CURSOR FOR FROM logs-* | LIMIT 10000;
                DECLARE batch ARRAY;

                OPEN log_cursor;
                FETCH log_cursor LIMIT 1000 INTO batch;

                WHILE ARRAY_LENGTH(batch) > 0 LOOP
                    FORALL doc IN batch CALL process_doc(doc) SAVE EXCEPTIONS;
                    FETCH log_cursor LIMIT 1000 INTO batch;
                END LOOP;

                CLOSE log_cursor;
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }
}
