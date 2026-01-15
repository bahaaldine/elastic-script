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
 * Tests for parsing type-aware ES|QL binding syntax:
 * - DECLARE variable ARRAY FROM esql_query;
 * - DECLARE variable DOCUMENT FROM esql_query;
 * - DECLARE variable NUMBER FROM esql_query;
 * - DECLARE variable STRING FROM esql_query;
 * - DECLARE variable DATE FROM esql_query;
 * - DECLARE variable BOOLEAN FROM esql_query;
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

    private ElasticScriptParser.Declare_statementContext parseDeclaration(String declareStmt) {
        // Wrap in a procedure to parse as a statement
        String input = "CREATE PROCEDURE test() BEGIN " + declareStmt + " END PROCEDURE";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        assertNotNull("Program should not be null", program);
        assertNotNull("Create procedure statement should not be null", program.create_procedure_statement());
        assertNotNull("Procedure should not be null", program.create_procedure_statement().procedure());
        assertTrue("Should have at least one statement", 
            program.create_procedure_statement().procedure().statement().size() >= 1);
        
        ElasticScriptParser.StatementContext stmt = program.create_procedure_statement().procedure().statement(0);
        assertNotNull("Statement should not be null", stmt);
        assertNotNull("Declare statement should not be null", stmt.declare_statement());
        
        return stmt.declare_statement();
    }

    // ==================== ARRAY Binding ====================

    public void testDeclareArrayFromSimpleQuery() {
        ElasticScriptParser.Declare_statementContext ctx = parseDeclaration(
            "DECLARE logs ARRAY FROM logs-*;"
        );
        
        assertEquals("logs", ctx.ID().getText());
        assertNotNull("esql_binding_type should not be null", ctx.esql_binding_type());
        assertEquals("ARRAY", ctx.esql_binding_type().getText());
        assertNotNull("esql_binding_query should not be null", ctx.esql_binding_query());
        assertTrue(ctx.esql_binding_query().getText().contains("logs"));
    }

    public void testDeclareArrayFromQueryWithPipe() {
        ElasticScriptParser.Declare_statementContext ctx = parseDeclaration(
            "DECLARE errors ARRAY FROM logs-* | WHERE level == 'ERROR' | LIMIT 100;"
        );
        
        assertEquals("errors", ctx.ID().getText());
        assertEquals("ARRAY", ctx.esql_binding_type().getText());
        String query = ctx.esql_binding_query().getText();
        assertTrue("Query should contain 'logs-*'", query.contains("logs"));
        assertTrue("Query should contain 'WHERE'", query.contains("WHERE"));
        assertTrue("Query should contain 'LIMIT'", query.contains("LIMIT"));
    }

    public void testDeclareArrayFromQueryWithStats() {
        ElasticScriptParser.Declare_statementContext ctx = parseDeclaration(
            "DECLARE stats ARRAY FROM metrics-* | STATS avg_cpu = AVG(cpu) BY host;"
        );
        
        assertEquals("stats", ctx.ID().getText());
        assertEquals("ARRAY", ctx.esql_binding_type().getText());
    }

    // ==================== DOCUMENT Binding ====================

    public void testDeclareDocumentFromQuery() {
        ElasticScriptParser.Declare_statementContext ctx = parseDeclaration(
            "DECLARE single_log DOCUMENT FROM logs-* | WHERE _id == 'abc123' | LIMIT 1;"
        );
        
        assertEquals("single_log", ctx.ID().getText());
        assertEquals("DOCUMENT", ctx.esql_binding_type().getText());
    }

    public void testDeclareDocumentFromStatsQuery() {
        ElasticScriptParser.Declare_statementContext ctx = parseDeclaration(
            "DECLARE stats DOCUMENT FROM logs-* | STATS count = COUNT(*), errors = COUNT(*) | WHERE level == 'ERROR';"
        );
        
        assertEquals("stats", ctx.ID().getText());
        assertEquals("DOCUMENT", ctx.esql_binding_type().getText());
    }

    // ==================== NUMBER Binding ====================

    public void testDeclareNumberFromCountQuery() {
        ElasticScriptParser.Declare_statementContext ctx = parseDeclaration(
            "DECLARE total NUMBER FROM logs-* | STATS c = COUNT(*);"
        );
        
        assertEquals("total", ctx.ID().getText());
        assertEquals("NUMBER", ctx.esql_binding_type().getText());
    }

    public void testDeclareNumberFromSumQuery() {
        ElasticScriptParser.Declare_statementContext ctx = parseDeclaration(
            "DECLARE total_bytes NUMBER FROM metrics-* | STATS total = SUM(bytes_sent);"
        );
        
        assertEquals("total_bytes", ctx.ID().getText());
        assertEquals("NUMBER", ctx.esql_binding_type().getText());
    }

    public void testDeclareNumberFromAvgQuery() {
        ElasticScriptParser.Declare_statementContext ctx = parseDeclaration(
            "DECLARE avg_latency NUMBER FROM apm-* | WHERE service == 'api' | STATS avg = AVG(latency);"
        );
        
        assertEquals("avg_latency", ctx.ID().getText());
        assertEquals("NUMBER", ctx.esql_binding_type().getText());
    }

    // ==================== STRING Binding ====================

    public void testDeclareStringFromQuery() {
        ElasticScriptParser.Declare_statementContext ctx = parseDeclaration(
            "DECLARE service_name STRING FROM services | WHERE id == 'svc-001' | KEEP name | LIMIT 1;"
        );
        
        assertEquals("service_name", ctx.ID().getText());
        assertEquals("STRING", ctx.esql_binding_type().getText());
    }

    public void testDeclareStringFromMaxQuery() {
        ElasticScriptParser.Declare_statementContext ctx = parseDeclaration(
            "DECLARE latest_version STRING FROM deployments | STATS latest = MAX(version);"
        );
        
        assertEquals("latest_version", ctx.ID().getText());
        assertEquals("STRING", ctx.esql_binding_type().getText());
    }

    // ==================== DATE Binding ====================

    public void testDeclareDateFromMaxQuery() {
        ElasticScriptParser.Declare_statementContext ctx = parseDeclaration(
            "DECLARE latest_event DATE FROM logs-* | STATS latest = MAX(@timestamp);"
        );
        
        assertEquals("latest_event", ctx.ID().getText());
        assertEquals("DATE", ctx.esql_binding_type().getText());
    }

    public void testDeclareDateFromMinQuery() {
        ElasticScriptParser.Declare_statementContext ctx = parseDeclaration(
            "DECLARE first_seen DATE FROM users | WHERE user_id == 'u123' | STATS first = MIN(created_at);"
        );
        
        assertEquals("first_seen", ctx.ID().getText());
        assertEquals("DATE", ctx.esql_binding_type().getText());
    }

    // ==================== BOOLEAN Binding ====================

    public void testDeclareBooleanFromQuery() {
        ElasticScriptParser.Declare_statementContext ctx = parseDeclaration(
            "DECLARE has_errors BOOLEAN FROM logs-* | WHERE level == 'ERROR' | STATS has = COUNT(*) > 0;"
        );
        
        assertEquals("has_errors", ctx.ID().getText());
        assertEquals("BOOLEAN", ctx.esql_binding_type().getText());
    }

    // ==================== Complex Queries ====================

    public void testDeclareWithComplexPipeline() {
        ElasticScriptParser.Declare_statementContext ctx = parseDeclaration(
            "DECLARE top_services ARRAY FROM logs-* | WHERE level == 'ERROR' | STATS count = COUNT(*) BY service | SORT count DESC | LIMIT 10;"
        );
        
        assertEquals("top_services", ctx.ID().getText());
        assertEquals("ARRAY", ctx.esql_binding_type().getText());
        String query = ctx.esql_binding_query().getText();
        assertTrue(query.contains("SORT"));
    }

    public void testDeclareWithEvalAndKeep() {
        ElasticScriptParser.Declare_statementContext ctx = parseDeclaration(
            "DECLARE enriched ARRAY FROM logs-* | EVAL severity = CASE(level, 'ERROR', 3, 'WARN', 2, 1) | KEEP @timestamp, message, severity | LIMIT 50;"
        );
        
        assertEquals("enriched", ctx.ID().getText());
        assertEquals("ARRAY", ctx.esql_binding_type().getText());
    }

    // ==================== CURSOR syntax (existing, for comparison) ====================

    public void testDeclareCursorStillWorks() {
        ElasticScriptParser.Declare_statementContext ctx = parseDeclaration(
            "DECLARE logs CURSOR FOR FROM logs-* | WHERE level == 'ERROR' | LIMIT 100;"
        );
        
        assertEquals("logs", ctx.ID().getText());
        assertNotNull("CURSOR keyword should be present", ctx.CURSOR());
        assertNotNull("cursor_query should be present", ctx.cursor_query());
        // esql_binding_type should be null for CURSOR
        assertNull("esql_binding_type should be null for CURSOR", ctx.esql_binding_type());
    }

    // ==================== Regular declaration (existing, for comparison) ====================

    public void testRegularDeclarationStillWorks() {
        ElasticScriptParser.Declare_statementContext ctx = parseDeclaration(
            "DECLARE count NUMBER = 0;"
        );
        
        assertNotNull("variable_declaration_list should be present", ctx.variable_declaration_list());
        assertNull("esql_binding_type should be null for regular declaration", ctx.esql_binding_type());
        assertNull("CURSOR should be null for regular declaration", ctx.CURSOR());
    }
}
