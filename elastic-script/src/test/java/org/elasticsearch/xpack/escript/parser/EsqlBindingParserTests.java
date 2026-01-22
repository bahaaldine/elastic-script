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
import org.elasticsearch.xpack.escript.handlers.ElasticScriptErrorListener;

/**
 * Tests for parsing type-aware ES|QL bindings:
 * {@code DECLARE x ARRAY FROM esql_query;}
 * {@code DECLARE x DOCUMENT FROM esql_query;}
 * {@code DECLARE x NUMBER FROM esql_query;}
 * {@code DECLARE x STRING FROM esql_query;}
 * {@code DECLARE x DATE FROM esql_query;}
 * {@code DECLARE x BOOLEAN FROM esql_query;}
 */
public class EsqlBindingParserTests extends ESTestCase {

    private ElasticScriptParser createParser(String input) {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new ElasticScriptErrorListener());
        return parser;
    }

    private ElasticScriptParser.Declare_statementContext parseDeclare(String stmt) {
        String input = "CREATE PROCEDURE test() BEGIN " + stmt + " END PROCEDURE";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        assertNotNull("Program should not be null", program);
        assertNotNull("Create procedure statement should not be null", program.create_procedure_statement());
        assertNotNull("Procedure should not be null", program.create_procedure_statement().procedure());
        assertTrue("Should have at least one statement",
            program.create_procedure_statement().procedure().statement().size() >= 1);
        
        ElasticScriptParser.StatementContext stmt0 = program.create_procedure_statement().procedure().statement(0);
        assertNotNull("Statement should not be null", stmt0);
        return stmt0.declare_statement();
    }

    // ==================== ARRAY Binding ====================

    public void testArrayBindingSimple() {
        ElasticScriptParser.Declare_statementContext ctx = parseDeclare(
            "DECLARE errors ARRAY FROM FROM logs-* | WHERE level = 'ERROR';"
        );
        
        assertNotNull("declare_statement should not be null", ctx);
        assertNotNull("ID should not be null", ctx.ID());
        assertEquals("errors", ctx.ID().getText());
        assertNotNull("esql_binding_type should not be null", ctx.esql_binding_type());
        assertEquals("ARRAY", ctx.esql_binding_type().getText());
        assertNotNull("esql_binding_query should not be null", ctx.esql_binding_query());
    }

    public void testArrayBindingWithLimit() {
        ElasticScriptParser.Declare_statementContext ctx = parseDeclare(
            "DECLARE recent_logs ARRAY FROM FROM logs-* | LIMIT 100;"
        );
        
        assertNotNull(ctx);
        assertEquals("recent_logs", ctx.ID().getText());
        assertEquals("ARRAY", ctx.esql_binding_type().getText());
    }

    public void testArrayBindingWithStats() {
        ElasticScriptParser.Declare_statementContext ctx = parseDeclare(
            "DECLARE stats ARRAY FROM FROM logs-* | STATS count = COUNT(*) BY level;"
        );
        
        assertNotNull(ctx);
        assertEquals("stats", ctx.ID().getText());
        assertEquals("ARRAY", ctx.esql_binding_type().getText());
    }

    // ==================== DOCUMENT Binding ====================

    public void testDocumentBindingSingleRow() {
        ElasticScriptParser.Declare_statementContext ctx = parseDeclare(
            "DECLARE user DOCUMENT FROM FROM users | WHERE id = 'john.doe' | LIMIT 1;"
        );
        
        assertNotNull(ctx);
        assertEquals("user", ctx.ID().getText());
        assertEquals("DOCUMENT", ctx.esql_binding_type().getText());
    }

    public void testDocumentBindingFirst() {
        ElasticScriptParser.Declare_statementContext ctx = parseDeclare(
            "DECLARE latest_error DOCUMENT FROM FROM logs-* | SORT @timestamp DESC | LIMIT 1;"
        );
        
        assertNotNull(ctx);
        assertEquals("latest_error", ctx.ID().getText());
        assertEquals("DOCUMENT", ctx.esql_binding_type().getText());
    }

    // ==================== NUMBER Binding ====================

    public void testNumberBindingCount() {
        ElasticScriptParser.Declare_statementContext ctx = parseDeclare(
            "DECLARE total NUMBER FROM FROM logs-* | STATS count = COUNT(*);"
        );
        
        assertNotNull(ctx);
        assertEquals("total", ctx.ID().getText());
        assertEquals("NUMBER", ctx.esql_binding_type().getText());
    }

    public void testNumberBindingSum() {
        ElasticScriptParser.Declare_statementContext ctx = parseDeclare(
            "DECLARE total_sales NUMBER FROM FROM transactions | STATS total = SUM(amount);"
        );
        
        assertNotNull(ctx);
        assertEquals("total_sales", ctx.ID().getText());
        assertEquals("NUMBER", ctx.esql_binding_type().getText());
    }

    public void testNumberBindingAvg() {
        ElasticScriptParser.Declare_statementContext ctx = parseDeclare(
            "DECLARE avg_price NUMBER FROM FROM products | STATS avg = AVG(price);"
        );
        
        assertNotNull(ctx);
        assertEquals("avg_price", ctx.ID().getText());
        assertEquals("NUMBER", ctx.esql_binding_type().getText());
    }

    // ==================== STRING Binding ====================

    public void testStringBinding() {
        ElasticScriptParser.Declare_statementContext ctx = parseDeclare(
            "DECLARE cluster STRING FROM SHOW INFO | KEEP cluster_name;"
        );
        
        assertNotNull(ctx);
        assertEquals("cluster", ctx.ID().getText());
        assertEquals("STRING", ctx.esql_binding_type().getText());
    }

    // ==================== BOOLEAN Binding ====================

    public void testBooleanBinding() {
        ElasticScriptParser.Declare_statementContext ctx = parseDeclare(
            "DECLARE has_errors BOOLEAN FROM FROM logs-* | WHERE level = 'ERROR' | STATS has = COUNT(*) > 0;"
        );
        
        assertNotNull(ctx);
        assertEquals("has_errors", ctx.ID().getText());
        assertEquals("BOOLEAN", ctx.esql_binding_type().getText());
    }

    // ==================== DATE Binding ====================

    public void testDateBinding() {
        ElasticScriptParser.Declare_statementContext ctx = parseDeclare(
            "DECLARE last_login DATE FROM FROM users | WHERE id = 'john' | KEEP last_login_time | LIMIT 1;"
        );
        
        assertNotNull(ctx);
        assertEquals("last_login", ctx.ID().getText());
        assertEquals("DATE", ctx.esql_binding_type().getText());
    }

    // ==================== Complex Queries ====================

    public void testBindingWithPipes() {
        ElasticScriptParser.Declare_statementContext ctx = parseDeclare(
            "DECLARE filtered ARRAY FROM FROM logs-* | WHERE level = 'ERROR' | EVAL message_len = LENGTH(message) | LIMIT 50;"
        );
        
        assertNotNull(ctx);
        assertEquals("filtered", ctx.ID().getText());
        assertEquals("ARRAY", ctx.esql_binding_type().getText());
    }

    public void testBindingWithMultipleConditions() {
        ElasticScriptParser.Declare_statementContext ctx = parseDeclare(
            "DECLARE critical ARRAY FROM FROM logs-* | WHERE level = 'ERROR' AND service = 'payment' | LIMIT 10;"
        );
        
        assertNotNull(ctx);
        assertEquals("critical", ctx.ID().getText());
        assertEquals("ARRAY", ctx.esql_binding_type().getText());
    }

    // ==================== Multiple Bindings in Procedure ====================

    public void testMultipleBindingsInProcedure() {
        String input = """
            CREATE PROCEDURE analyze_logs()
            BEGIN
                DECLARE errors ARRAY FROM FROM logs-* | WHERE level = 'ERROR';
                DECLARE error_count NUMBER FROM FROM logs-* | WHERE level = 'ERROR' | STATS count = COUNT(*);
                DECLARE latest_error DOCUMENT FROM FROM logs-* | WHERE level = 'ERROR' | SORT @timestamp DESC | LIMIT 1;
            END PROCEDURE
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull(program);
        assertNotNull(program.create_procedure_statement());
        
        var statements = program.create_procedure_statement().procedure().statement();
        assertEquals(3, statements.size());
        
        // First: ARRAY binding
        ElasticScriptParser.Declare_statementContext decl1 = statements.get(0).declare_statement();
        assertNotNull(decl1);
        assertEquals("errors", decl1.ID().getText());
        assertEquals("ARRAY", decl1.esql_binding_type().getText());
        
        // Second: NUMBER binding
        ElasticScriptParser.Declare_statementContext decl2 = statements.get(1).declare_statement();
        assertNotNull(decl2);
        assertEquals("error_count", decl2.ID().getText());
        assertEquals("NUMBER", decl2.esql_binding_type().getText());
        
        // Third: DOCUMENT binding
        ElasticScriptParser.Declare_statementContext decl3 = statements.get(2).declare_statement();
        assertNotNull(decl3);
        assertEquals("latest_error", decl3.ID().getText());
        assertEquals("DOCUMENT", decl3.esql_binding_type().getText());
    }
}
