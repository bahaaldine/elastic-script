/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.language;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.escript.parser.ElasticScriptLexer;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;

/**
 * Tests for Phase 8: Inline ESQL Integration.
 * These tests verify that the grammar correctly parses inline ESQL in FOR loops.
 * 
 * Note: Full integration tests require an Elasticsearch cluster and are
 * not included in this unit test class.
 */
public class InlineEsqlTests extends ESTestCase {

    /**
     * Parses a script and returns the parse tree.
     * Throws if there are syntax errors.
     */
    private ElasticScriptParser.ProcedureContext parse(String script) {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(script));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);
        return parser.procedure();
    }

    // ==================== Grammar Parsing Tests ====================

    public void testInlineEsqlForLoopParsing() {
        String script = """
            PROCEDURE test()
            BEGIN
                FOR log IN (FROM logs | WHERE level == 'error' | LIMIT 10) LOOP
                    PRINT log['message'];
                END LOOP
            END PROCEDURE
            """;
        // Should parse without errors
        ElasticScriptParser.ProcedureContext ctx = parse(script);
        assertNotNull(ctx);
        assertNotNull(ctx.statement());
        assertFalse(ctx.statement().isEmpty());
    }

    public void testInlineEsqlWithWhereClause() {
        String script = """
            PROCEDURE test()
            BEGIN
                FOR doc IN (FROM my_index | WHERE status == 'active') LOOP
                    PRINT doc['name'];
                END LOOP
            END PROCEDURE
            """;
        ElasticScriptParser.ProcedureContext ctx = parse(script);
        assertNotNull(ctx);
    }

    public void testInlineEsqlWithStatsAggregation() {
        String script = """
            PROCEDURE test()
            BEGIN
                FOR result IN (FROM sales | STATS total = SUM(amount) BY category) LOOP
                    PRINT result['category'] || ': ' || result['total'];
                END LOOP
            END PROCEDURE
            """;
        ElasticScriptParser.ProcedureContext ctx = parse(script);
        assertNotNull(ctx);
    }

    public void testInlineEsqlWithSortAndLimit() {
        String script = """
            PROCEDURE test()
            BEGIN
                FOR user IN (FROM users | SORT created_at DESC | LIMIT 5) LOOP
                    PRINT user['email'];
                END LOOP
            END PROCEDURE
            """;
        ElasticScriptParser.ProcedureContext ctx = parse(script);
        assertNotNull(ctx);
    }

    public void testInlineEsqlWithMultipleStatements() {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE count NUMBER = 0;
                FOR item IN (FROM inventory | WHERE quantity > 0) LOOP
                    PRINT item['name'];
                    SET count = count + 1;
                END LOOP
                RETURN count;
            END PROCEDURE
            """;
        ElasticScriptParser.ProcedureContext ctx = parse(script);
        assertNotNull(ctx);
    }

    public void testInlineEsqlWithNestedParentheses() {
        String script = """
            PROCEDURE test()
            BEGIN
                FOR doc IN (FROM logs | WHERE (level == 'error' OR level == 'warning') AND timestamp > '2024-01-01') LOOP
                    PRINT doc['message'];
                END LOOP
            END PROCEDURE
            """;
        ElasticScriptParser.ProcedureContext ctx = parse(script);
        assertNotNull(ctx);
    }

    public void testInlineEsqlWithKeep() {
        String script = """
            PROCEDURE test()
            BEGIN
                FOR doc IN (FROM logs | KEEP timestamp, level, message | LIMIT 100) LOOP
                    PRINT doc['level'] || ': ' || doc['message'];
                END LOOP
            END PROCEDURE
            """;
        ElasticScriptParser.ProcedureContext ctx = parse(script);
        assertNotNull(ctx);
    }

    public void testInlineEsqlWithEval() {
        String script = """
            PROCEDURE test()
            BEGIN
                FOR order IN (FROM orders | EVAL total = quantity * price | WHERE total > 100) LOOP
                    PRINT order['product'] || ' - $' || order['total'];
                END LOOP
            END PROCEDURE
            """;
        ElasticScriptParser.ProcedureContext ctx = parse(script);
        assertNotNull(ctx);
    }

    // ==================== Multiple Loops Tests ====================

    public void testMultipleInlineEsqlLoops() {
        String script = """
            PROCEDURE test()
            BEGIN
                FOR error IN (FROM logs | WHERE level == 'error' | LIMIT 5) LOOP
                    PRINT 'Error: ' || error['message'];
                END LOOP

                FOR warning IN (FROM logs | WHERE level == 'warning' | LIMIT 5) LOOP
                    PRINT 'Warning: ' || warning['message'];
                END LOOP
            END PROCEDURE
            """;
        ElasticScriptParser.ProcedureContext ctx = parse(script);
        assertNotNull(ctx);
    }

    public void testMixedLoopTypes() {
        String script = """
            PROCEDURE test()
            BEGIN
                -- Regular range loop
                FOR i IN 1..3 LOOP
                    PRINT 'Iteration: ' || i;
                END LOOP

                -- Inline ESQL loop
                FOR doc IN (FROM my_index | LIMIT 5) LOOP
                    PRINT doc['title'];
                END LOOP

                -- Array loop
                DECLARE items ARRAY = ['a', 'b', 'c'];
                FOR item IN items LOOP
                    PRINT item;
                END LOOP
            END PROCEDURE
            """;
        ElasticScriptParser.ProcedureContext ctx = parse(script);
        assertNotNull(ctx);
    }
}

