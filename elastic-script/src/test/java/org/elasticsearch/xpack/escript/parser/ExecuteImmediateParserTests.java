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
 * Parser tests for EXECUTE IMMEDIATE syntax.
 * 
 * Tests the dynamic ES|QL execution statement:
 * <pre>
 * EXECUTE IMMEDIATE query_expression [INTO var1, var2, ...] [USING bind1, bind2, ...];
 * </pre>
 */
public class ExecuteImmediateParserTests extends ESTestCase {

    private ElasticScriptParser createParser(String input) {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new ElasticScriptParser(tokens);
    }

    // ==================== Basic EXECUTE IMMEDIATE Tests ====================

    @Test
    public void testParseSimpleExecuteImmediate() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                EXECUTE IMMEDIATE 'FROM logs-* | LIMIT 10';
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseExecuteImmediateWithVariable() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE query STRING = 'FROM logs-* | LIMIT 10';
                EXECUTE IMMEDIATE query;
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseExecuteImmediateWithConcatenation() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE index_name STRING = 'logs-2024';
                EXECUTE IMMEDIATE 'FROM ' || index_name || ' | LIMIT 10';
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== INTO Clause Tests ====================

    @Test
    public void testParseExecuteImmediateIntoSingleVariable() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE results ARRAY;
                EXECUTE IMMEDIATE 'FROM logs-* | LIMIT 10' INTO results;
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseExecuteImmediateIntoMultipleVariables() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE name STRING;
                DECLARE age NUMBER;
                DECLARE email STRING;
                EXECUTE IMMEDIATE 'FROM users | LIMIT 1' INTO name, age, email;
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== USING Clause Tests ====================

    @Test
    public void testParseExecuteImmediateUsingSingleBind() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                EXECUTE IMMEDIATE 'FROM logs-* | WHERE level = :1' USING 'ERROR';
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseExecuteImmediateUsingMultipleBinds() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                EXECUTE IMMEDIATE 'FROM logs-* | WHERE level = :1 AND status = :2'
                    USING 'ERROR', 500;
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseExecuteImmediateUsingVariables() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE level STRING = 'ERROR';
                DECLARE status NUMBER = 500;
                EXECUTE IMMEDIATE 'FROM logs-* | WHERE level = :1 AND status = :2'
                    USING level, status;
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== Combined INTO and USING Tests ====================

    @Test
    public void testParseExecuteImmediateWithIntoAndUsing() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE results ARRAY;
                EXECUTE IMMEDIATE 'FROM logs-* | WHERE level = :1 | LIMIT :2'
                    INTO results
                    USING 'ERROR', 100;
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== Complex Expression Tests ====================

    @Test
    public void testParseExecuteImmediateWithComplexExpression() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE base STRING = 'FROM logs-';
                DECLARE date STRING = '2024-01-01';
                DECLARE suffix STRING = ' | LIMIT 100';
                EXECUTE IMMEDIATE base || date || suffix;
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseExecuteImmediateWithFunctionCall() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE index_pattern STRING;
                SET index_pattern = 'logs-' || SUBSTR(CURRENT_DATE, 1, 7);
                EXECUTE IMMEDIATE 'FROM ' || index_pattern || ' | LIMIT 10';
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== Real-World Scenario Tests ====================

    @Test
    public void testParseDynamicIndexQuery() {
        String input = """
            CREATE PROCEDURE query_by_date(date_suffix STRING)
            BEGIN
                DECLARE query STRING;
                DECLARE results ARRAY;
                SET query = 'FROM logs-' || date_suffix || ' | WHERE level = :1 | LIMIT 100';
                EXECUTE IMMEDIATE query INTO results USING 'ERROR';
                PRINT 'Found ' || ARRAY_LENGTH(results) || ' errors';
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseDynamicAggregation() {
        String input = """
            CREATE PROCEDURE count_by_field(index_name STRING, field_name STRING)
            BEGIN
                DECLARE query STRING;
                DECLARE result NUMBER;
                SET query = 'FROM ' || index_name || ' | STATS count = COUNT(' || field_name || ')';
                EXECUTE IMMEDIATE query INTO result;
                PRINT 'Count: ' || result;
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }
}
