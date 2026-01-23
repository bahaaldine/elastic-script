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
 * Parser tests for CREATE FUNCTION syntax.
 * 
 * Tests the new top-level CREATE FUNCTION statement with RETURNS clause:
 * <pre>
 * CREATE FUNCTION name(params) RETURNS type AS
 * BEGIN
 *     ...
 *     RETURN expr;
 * END FUNCTION
 * </pre>
 */
public class CreateFunctionParserTests extends ESTestCase {

    private ElasticScriptParser createParser(String input) {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new ElasticScriptParser(tokens);
    }

    // ==================== Basic CREATE FUNCTION Tests ====================

    @Test
    public void testParseBasicCreateFunction() {
        String input = """
            CREATE FUNCTION add_numbers(a NUMBER, b NUMBER) RETURNS NUMBER AS
            BEGIN
                RETURN a + b;
            END FUNCTION
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
        assertNotNull("Should have create_function_statement", program.create_function_statement());
    }

    @Test
    public void testParseFunctionWithNoParameters() {
        String input = """
            CREATE FUNCTION get_timestamp() RETURNS DATE AS
            BEGIN
                RETURN CURRENT_TIMESTAMP;
            END FUNCTION
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseFunctionReturnsString() {
        String input = """
            CREATE FUNCTION greet(name STRING) RETURNS STRING AS
            BEGIN
                RETURN 'Hello, ' || name || '!';
            END FUNCTION
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseFunctionReturnsDocument() {
        String input = """
            CREATE FUNCTION create_user(name STRING, age NUMBER) RETURNS DOCUMENT AS
            BEGIN
                RETURN {'name': name, 'age': age, 'created': CURRENT_TIMESTAMP};
            END FUNCTION
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseFunctionReturnsArray() {
        String input = """
            CREATE FUNCTION get_range(start NUMBER, end_val NUMBER) RETURNS ARRAY AS
            BEGIN
                DECLARE result ARRAY = [];
                FOR i IN start..end_val LOOP
                    SET result = ARRAY_APPEND(result, i);
                END LOOP
                RETURN result;
            END FUNCTION
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseFunctionReturnsBoolean() {
        String input = """
            CREATE FUNCTION is_positive(n NUMBER) RETURNS BOOLEAN AS
            BEGIN
                IF n > 0 THEN
                    RETURN true;
                ELSE
                    RETURN false;
                END IF
            END FUNCTION
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== DELETE FUNCTION Tests ====================

    @Test
    public void testParseDeleteFunction() {
        String input = "DELETE FUNCTION my_function;";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
        assertNotNull("Should have delete_function_statement", program.delete_function_statement());
    }

    // ==================== Complex Function Tests ====================

    @Test
    public void testParseFunctionWithControlFlow() {
        String input = """
            CREATE FUNCTION calculate_grade(score NUMBER) RETURNS STRING AS
            BEGIN
                IF score >= 90 THEN
                    RETURN 'A';
                ELSEIF score >= 80 THEN
                    RETURN 'B';
                ELSEIF score >= 70 THEN
                    RETURN 'C';
                ELSEIF score >= 60 THEN
                    RETURN 'D';
                ELSE
                    RETURN 'F';
                END IF
            END FUNCTION
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseFunctionWithLocalVariables() {
        String input = """
            CREATE FUNCTION calculate_tax(price NUMBER, tax_rate NUMBER) RETURNS NUMBER AS
            BEGIN
                DECLARE tax_amount NUMBER;
                DECLARE total NUMBER;
                SET tax_amount = price * tax_rate / 100;
                SET total = price + tax_amount;
                RETURN total;
            END FUNCTION
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseFunctionWithTryCatch() {
        String input = """
            CREATE FUNCTION safe_divide(a NUMBER, b NUMBER) RETURNS NUMBER AS
            BEGIN
                TRY
                    IF b == 0 THEN
                        THROW 'Division by zero' WITH CODE 'DIV_ZERO';
                    END IF
                    RETURN a / b;
                CATCH
                    PRINT 'Error: ' || error['message'];
                    RETURN 0;
                END TRY
            END FUNCTION
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseFunctionWithLoop() {
        String input = """
            CREATE FUNCTION sum_array(numbers ARRAY) RETURNS NUMBER AS
            BEGIN
                DECLARE total NUMBER = 0;
                FOR num IN numbers LOOP
                    SET total = total + num;
                END LOOP
                RETURN total;
            END FUNCTION
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== Parameter Mode Tests ====================

    @Test
    public void testParseFunctionWithExplicitInParameters() {
        String input = """
            CREATE FUNCTION multiply(IN a NUMBER, IN b NUMBER) RETURNS NUMBER AS
            BEGIN
                RETURN a * b;
            END FUNCTION
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseFunctionWithMixedParameters() {
        String input = """
            CREATE FUNCTION process_data(IN input STRING, OUT result STRING) RETURNS BOOLEAN AS
            BEGIN
                SET result = UPPER(input);
                RETURN true;
            END FUNCTION
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }
}
