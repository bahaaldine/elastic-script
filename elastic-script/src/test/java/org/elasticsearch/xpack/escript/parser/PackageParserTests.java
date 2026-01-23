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
 * Parser tests for package syntax.
 *
 * Tests:
 * - CREATE PACKAGE with procedure, function, and variable declarations
 * - CREATE PACKAGE BODY with implementations
 * - Public/Private visibility
 * - DROP PACKAGE
 * - SHOW PACKAGE
 */
public class PackageParserTests extends ESTestCase {

    private ElasticScriptParser createParser(String input) {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new ElasticScriptParser(tokens);
    }

    // ==================== CREATE PACKAGE Tests ====================

    @Test
    public void testParseSimplePackage() {
        String input = """
            CREATE PACKAGE utils AS
                PROCEDURE log_message(msg STRING);
                FUNCTION add_numbers(a NUMBER, b NUMBER) RETURNS NUMBER;
            END PACKAGE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParsePackageWithVariables() {
        String input = """
            CREATE PACKAGE config AS
                max_retries NUMBER = 3;
                default_timeout NUMBER = 30000;
                PROCEDURE initialize();
            END PACKAGE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParsePackageWithPublicPrivate() {
        String input = """
            CREATE PACKAGE my_pkg AS
                PUBLIC PROCEDURE public_proc();
                PRIVATE PROCEDURE private_proc();
                PUBLIC counter NUMBER = 0;
                PRIVATE internal_state STRING;
            END PACKAGE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParsePackageWithFunctions() {
        String input = """
            CREATE PACKAGE math_utils AS
                FUNCTION factorial(n NUMBER) RETURNS NUMBER;
                FUNCTION fibonacci(n NUMBER) RETURNS NUMBER;
                FUNCTION is_prime(n NUMBER) RETURNS BOOLEAN;
            END PACKAGE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== CREATE PACKAGE BODY Tests ====================

    @Test
    public void testParsePackageBody() {
        String input = """
            CREATE PACKAGE BODY utils AS
                PROCEDURE log_message(msg STRING)
                BEGIN
                    PRINT msg;
                END PROCEDURE;

                FUNCTION add_numbers(a NUMBER, b NUMBER) RETURNS NUMBER AS
                BEGIN
                    RETURN a + b;
                END FUNCTION;
            END PACKAGE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParsePackageBodyWithMultipleProcedures() {
        String input = """
            CREATE PACKAGE BODY log_pkg AS
                PROCEDURE log_info(msg STRING)
                BEGIN
                    PRINT 'INFO: ' || msg;
                END PROCEDURE;

                PROCEDURE log_warn(msg STRING)
                BEGIN
                    PRINT 'WARN: ' || msg;
                END PROCEDURE;

                PROCEDURE log_error(msg STRING)
                BEGIN
                    PRINT 'ERROR: ' || msg;
                END PROCEDURE;
            END PACKAGE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== DROP PACKAGE Tests ====================

    @Test
    public void testParseDropPackage() {
        String input = "DROP PACKAGE my_package";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== SHOW PACKAGE Tests ====================

    @Test
    public void testParseShowPackage() {
        String input = "SHOW PACKAGE my_package";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== Complex Package Tests ====================

    @Test
    public void testParseCompletePackage() {
        String input = """
            CREATE PACKAGE alert_manager AS
                PUBLIC max_alerts NUMBER = 100;
                PRIVATE alert_count NUMBER = 0;

                PUBLIC PROCEDURE send_alert(message STRING, severity STRING);
                PUBLIC FUNCTION get_alert_count() RETURNS NUMBER;
                PRIVATE PROCEDURE increment_count();
            END PACKAGE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }
}
