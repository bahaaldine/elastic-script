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
 * Parser tests for cursor syntax.
 *
 * Tests:
 * - Cursor declaration (DECLARE ... CURSOR FOR ...)
 * - OPEN cursor
 * - CLOSE cursor
 * - FETCH cursor INTO variable
 * - FETCH cursor LIMIT n INTO variable
 * - Cursor attributes (%NOTFOUND, %ROWCOUNT)
 */
public class CursorParserTests extends ESTestCase {

    private ElasticScriptParser createParser(String input) {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new ElasticScriptParser(tokens);
    }

    // ==================== Cursor Declaration Tests ====================

    @Test
    public void testParseCursorDeclaration() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE log_cursor CURSOR FOR FROM logs-* | WHERE level = 'ERROR' | LIMIT 100;
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseCursorWithComplexQuery() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE error_cursor CURSOR FOR
                    FROM logs-*
                    | WHERE level = 'ERROR' AND service = 'payment'
                    | SORT @timestamp DESC
                    | LIMIT 1000;
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== OPEN Cursor Tests ====================

    @Test
    public void testParseOpenCursor() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE my_cursor CURSOR FOR FROM logs-* | LIMIT 10;
                OPEN my_cursor;
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== CLOSE Cursor Tests ====================

    @Test
    public void testParseCloseCursor() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE my_cursor CURSOR FOR FROM logs-* | LIMIT 10;
                OPEN my_cursor;
                CLOSE my_cursor;
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== FETCH Tests ====================

    @Test
    public void testParseFetchSingleRow() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE my_cursor CURSOR FOR FROM logs-* | LIMIT 10;
                DECLARE row DOCUMENT;
                OPEN my_cursor;
                FETCH my_cursor INTO row;
                CLOSE my_cursor;
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseFetchBatch() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE my_cursor CURSOR FOR FROM logs-* | LIMIT 100;
                DECLARE batch ARRAY;
                OPEN my_cursor;
                FETCH my_cursor LIMIT 10 INTO batch;
                CLOSE my_cursor;
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseFetchBatchWithExpression() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE batch_size NUMBER = 50;
                DECLARE my_cursor CURSOR FOR FROM logs-* | LIMIT 1000;
                DECLARE batch ARRAY;
                OPEN my_cursor;
                FETCH my_cursor LIMIT batch_size INTO batch;
                CLOSE my_cursor;
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== Cursor Attributes Tests ====================

    @Test
    public void testParseCursorNotFound() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE my_cursor CURSOR FOR FROM logs-* | LIMIT 10;
                DECLARE row DOCUMENT;
                OPEN my_cursor;
                FETCH my_cursor INTO row;
                IF my_cursor%NOTFOUND THEN
                    PRINT 'No more rows';
                END IF;
                CLOSE my_cursor;
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseCursorRowCount() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE my_cursor CURSOR FOR FROM logs-* | LIMIT 100;
                DECLARE row DOCUMENT;
                OPEN my_cursor;
                WHILE NOT my_cursor%NOTFOUND LOOP
                    FETCH my_cursor INTO row;
                    PRINT 'Processed row ' || my_cursor%ROWCOUNT;
                END LOOP;
                PRINT 'Total rows: ' || my_cursor%ROWCOUNT;
                CLOSE my_cursor;
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== Full Cursor Workflow Tests ====================

    @Test
    public void testParseFullCursorWorkflow() {
        String input = """
            CREATE PROCEDURE process_logs()
            BEGIN
                DECLARE log_cursor CURSOR FOR
                    FROM logs-*
                    | WHERE level = 'ERROR'
                    | SORT @timestamp DESC
                    | LIMIT 1000;
                DECLARE batch ARRAY;
                DECLARE processed NUMBER = 0;

                OPEN log_cursor;

                WHILE NOT log_cursor%NOTFOUND LOOP
                    FETCH log_cursor LIMIT 100 INTO batch;

                    IF ARRAY_LENGTH(batch) > 0 THEN
                        DECLARE i NUMBER;
                        FOR i IN 1..ARRAY_LENGTH(batch) LOOP
                            PRINT 'Processing: ' || batch[i]['message'];
                        END LOOP;

                        SET processed = processed + ARRAY_LENGTH(batch);
                    END IF;
                END LOOP;

                PRINT 'Total processed: ' || log_cursor%ROWCOUNT;
                CLOSE log_cursor;
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseNestedCursors() {
        String input = """
            CREATE PROCEDURE nested_cursors()
            BEGIN
                DECLARE service_cursor CURSOR FOR FROM services-* | LIMIT 10;
                DECLARE service DOCUMENT;

                OPEN service_cursor;
                FETCH service_cursor INTO service;

                WHILE NOT service_cursor%NOTFOUND LOOP
                    DECLARE log_cursor CURSOR FOR FROM logs-* | WHERE service = 'test' | LIMIT 10;
                    DECLARE log_entry DOCUMENT;

                    OPEN log_cursor;
                    FETCH log_cursor INTO log_entry;

                    WHILE NOT log_cursor%NOTFOUND LOOP
                        PRINT log_entry['message'];
                        FETCH log_cursor INTO log_entry;
                    END LOOP;

                    CLOSE log_cursor;
                    FETCH service_cursor INTO service;
                END LOOP;

                CLOSE service_cursor;
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }
}
