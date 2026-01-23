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
 * Parser tests for AUTHID clause syntax.
 *
 * Tests:
 * - Procedures with AUTHID DEFINER
 * - Procedures with AUTHID CURRENT_USER
 * - Functions with AUTHID
 * - Default behavior (no AUTHID clause)
 */
public class AuthIdParserTests extends ESTestCase {

    private ElasticScriptParser createParser(String input) {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new ElasticScriptParser(tokens);
    }

    // ==================== Procedure AUTHID Tests ====================

    @Test
    public void testParseProcedureWithoutAuthId() {
        String input = """
            CREATE PROCEDURE simple_proc()
            BEGIN
                PRINT 'Hello';
            END PROCEDURE;
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseProcedureWithAuthIdDefiner() {
        String input = """
            CREATE PROCEDURE secured_proc()
            AUTHID DEFINER
            BEGIN
                PRINT 'Running with definer privileges';
            END PROCEDURE;
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseProcedureWithAuthIdCurrentUser() {
        String input = """
            CREATE PROCEDURE invoker_proc()
            AUTHID CURRENT_USER
            BEGIN
                PRINT 'Running with invoker privileges';
            END PROCEDURE;
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseProcedureWithParamsAndAuthId() {
        String input = """
            CREATE PROCEDURE process_data(data DOCUMENT, user_id STRING)
            AUTHID DEFINER
            BEGIN
                PRINT 'Processing for: ' || user_id;
            END PROCEDURE;
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== Function AUTHID Tests ====================

    @Test
    public void testParseFunctionWithoutAuthId() {
        String input = """
            CREATE FUNCTION add_numbers(a NUMBER, b NUMBER)
            RETURNS NUMBER AS
            BEGIN
                RETURN a + b;
            END FUNCTION
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseFunctionWithAuthIdDefiner() {
        String input = """
            CREATE FUNCTION secure_lookup(key STRING)
            RETURNS STRING
            AUTHID DEFINER AS
            BEGIN
                RETURN 'secret_value';
            END FUNCTION
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseFunctionWithAuthIdCurrentUser() {
        String input = """
            CREATE FUNCTION user_data()
            RETURNS DOCUMENT
            AUTHID CURRENT_USER AS
            BEGIN
                RETURN {'user': 'current'};
            END FUNCTION
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }
}
