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
 * Parser tests for MAP type syntax.
 *
 * Tests:
 * - MAP type declaration
 * - MAP literal syntax: MAP { key => value }
 * - MAP functions
 */
public class MapTypeParserTests extends ESTestCase {

    private ElasticScriptParser createParser(String input) {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new ElasticScriptParser(tokens);
    }

    // ==================== MAP Declaration Tests ====================

    @Test
    public void testParseMapDeclaration() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE my_map MAP;
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseTypedMapDeclaration() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE counts MAP OF STRING TO NUMBER;
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== MAP Literal Tests ====================

    @Test
    public void testParseEmptyMapLiteral() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE empty_map MAP;
                SET empty_map = MAP {};
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseMapLiteralWithStringKeys() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE my_map MAP;
                SET my_map = MAP { 'name' => 'John', 'age' => 30 };
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseMapLiteralWithExpressions() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE key STRING = 'key1';
                DECLARE value NUMBER = 42;
                DECLARE my_map MAP;
                SET my_map = MAP { key => value, 'literal' => 100 };
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseMapLiteralInline() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE config MAP = MAP { 'host' => 'localhost', 'port' => 9200 };
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== MAP Function Tests ====================

    @Test
    public void testParseMapGetFunction() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE my_map MAP = MAP { 'key' => 'value' };
                DECLARE val STRING;
                SET val = MAP_GET(my_map, 'key');
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseMapPutFunction() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE my_map MAP = MAP {};
                SET my_map = MAP_PUT(my_map, 'new_key', 'new_value');
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseMapKeysFunction() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE my_map MAP = MAP { 'a' => 1, 'b' => 2 };
                DECLARE keys ARRAY;
                SET keys = MAP_KEYS(my_map);
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseMapMergeFunction() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE map1 MAP = MAP { 'a' => 1 };
                DECLARE map2 MAP = MAP { 'b' => 2 };
                DECLARE merged MAP;
                SET merged = MAP_MERGE(map1, map2);
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== MAP Bracket Access Tests ====================

    @Test
    public void testParseMapBracketAccess() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE my_map MAP = MAP { 'name' => 'John' };
                DECLARE name STRING;
                SET name = my_map['name'];
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== MAP in Expressions Tests ====================

    @Test
    public void testParseMapInCondition() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE my_map MAP = MAP { 'enabled' => true };
                IF MAP_CONTAINS_KEY(my_map, 'enabled') THEN
                    PRINT 'Has enabled key';
                END IF
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseMapSizeInLoop() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE my_map MAP = MAP { 'a' => 1, 'b' => 2, 'c' => 3 };
                DECLARE i NUMBER;
                FOR i IN 1..MAP_SIZE(my_map) LOOP
                    PRINT 'Entry ' || i;
                END LOOP
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== Complex MAP Usage Tests ====================

    @Test
    public void testParseNestedMapLiteral() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE config MAP = MAP {
                    'database' => MAP { 'host' => 'localhost', 'port' => 5432 },
                    'cache' => MAP { 'ttl' => 3600, 'enabled' => true }
                };
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseMapFromArrays() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE keys ARRAY = ['a', 'b', 'c'];
                DECLARE values ARRAY = [1, 2, 3];
                DECLARE my_map MAP;
                SET my_map = MAP_FROM_ARRAYS(keys, values);
            END PROCEDURE
            """;
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }
}
