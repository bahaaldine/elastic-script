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
 * Parser tests for exception handling syntax:
 * - TRY/CATCH/FINALLY blocks
 * - Named exceptions (CATCH http_error)
 * - THROW/RAISE with expressions
 * - WITH CODE clause
 */
public class ExceptionHandlingParserTests extends ESTestCase {

    private ElasticScriptParser createParser(String input) {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new ElasticScriptParser(tokens);
    }

    // ==================== THROW Statement Tests ====================

    @Test
    public void testParseBasicThrow() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                THROW 'Error message';
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseThrowWithCode() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                THROW 'Not found' WITH CODE 'HTTP_404';
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseThrowWithVariableMessage() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE msg STRING = 'Error';
                THROW msg;
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseThrowWithConcatenation() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE id STRING = '123';
                THROW 'Item not found: ' || id;
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseThrowWithExpressionCode() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE code STRING = 'ERR_001';
                THROW 'Error' WITH CODE code;
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== RAISE Statement Tests ====================

    @Test
    public void testParseRaiseBasic() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                RAISE 'Error message';
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseRaiseWithCode() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                RAISE 'Timeout' WITH CODE 'TIMEOUT_001';
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== TRY/CATCH Block Tests ====================

    @Test
    public void testParseBasicTryCatch() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                TRY
                    PRINT 'trying';
                CATCH
                    PRINT 'caught';
                END TRY
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseTryCatchFinally() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                TRY
                    PRINT 'trying';
                CATCH
                    PRINT 'caught';
                FINALLY
                    PRINT 'finally';
                END TRY
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseTryFinally() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                TRY
                    PRINT 'trying';
                FINALLY
                    PRINT 'cleanup';
                END TRY
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseNamedExceptionCatch() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                TRY
                    PRINT 'trying';
                CATCH http_error
                    PRINT 'HTTP error caught';
                END TRY
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseMultipleNamedCatchBlocks() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                TRY
                    PRINT 'trying';
                CATCH http_error
                    PRINT 'HTTP error';
                CATCH timeout_error
                    PRINT 'Timeout error';
                CATCH
                    PRINT 'Other error';
                END TRY
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseNamedCatchWithFinally() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                TRY
                    PRINT 'trying';
                CATCH validation_error
                    PRINT 'Validation failed';
                CATCH
                    PRINT 'Other error';
                FINALLY
                    PRINT 'Cleanup';
                END TRY
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== @error Variable Access Tests ====================

    @Test
    public void testParseErrorVariableAccess() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                TRY
                    THROW 'Error';
                CATCH
                    PRINT error['message'];
                END TRY
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseErrorVariableMultipleFields() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                TRY
                    THROW 'Error' WITH CODE 'ERR_001';
                CATCH
                    PRINT error['message'];
                    PRINT error['code'];
                    PRINT error['type'];
                END TRY
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== Nested TRY/CATCH Tests ====================

    @Test
    public void testParseNestedTryCatch() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                TRY
                    TRY
                        THROW 'Inner';
                    CATCH
                        THROW 'Outer';
                    END TRY
                CATCH
                    PRINT 'Caught outer';
                END TRY
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== Complex Scenarios ====================

    @Test
    public void testParseCompleteExceptionHandling() {
        String input = """
            CREATE PROCEDURE complete_exception_handling()
            BEGIN
                DECLARE result STRING;
                DECLARE err_code STRING;
                TRY
                    THROW 'Validation failed' WITH CODE 'VAL_001';
                CATCH validation_error
                    SET result = 'Handled validation error';
                    SET err_code = error['code'];
                CATCH http_error
                    SET result = 'Handled HTTP error';
                CATCH
                    SET result = 'Handled unknown error: ' || error['message'];
                FINALLY
                    PRINT 'Cleanup completed';
                END TRY
                PRINT result;
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }
}
