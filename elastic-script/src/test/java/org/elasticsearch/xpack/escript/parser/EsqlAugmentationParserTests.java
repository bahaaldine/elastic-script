/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.parser;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.elasticsearch.test.ESTestCase;

/**
 * Tests for ES|QL augmentation parsing (INTO and PROCESS WITH statements).
 */
public class EsqlAugmentationParserTests extends ESTestCase {

    private ElasticScriptParser createParser(String input) {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new ElasticScriptParser(tokens);
    }

    // ========== INTO Variable Tests ==========

    public void testSimpleIntoVariable() {
        String input = "FROM logs-* INTO my_results;";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Esql_into_statementContext ctx = parser.esql_into_statement();
        
        assertNotNull("Should parse INTO statement", ctx);
        assertNotNull("Should have esql_query", ctx.esql_query());
        assertNotNull("Should have into_target", ctx.into_target());
        assertTrue("Target should be a variable", ctx.into_target() instanceof ElasticScriptParser.IntoVariableContext);
    }

    public void testIntoVariableWithWhere() {
        String input = "FROM logs-* | WHERE level == 'ERROR' INTO error_logs;";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Esql_into_statementContext ctx = parser.esql_into_statement();
        
        assertNotNull("Should parse INTO statement with WHERE", ctx);
        assertNotNull("Should have into_target", ctx.into_target());
        
        ElasticScriptParser.IntoVariableContext varTarget = (ElasticScriptParser.IntoVariableContext) ctx.into_target();
        assertEquals("error_logs", varTarget.ID().getText());
    }

    public void testIntoVariableWithMultiplePipes() {
        String input = "FROM logs-* | WHERE level == 'ERROR' | STATS count = COUNT(*) | SORT count DESC INTO results;";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Esql_into_statementContext ctx = parser.esql_into_statement();
        
        assertNotNull("Should parse complex INTO statement", ctx);
    }

    public void testIntoVariableWithLimit() {
        String input = "FROM logs-* | LIMIT 100 INTO sample;";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Esql_into_statementContext ctx = parser.esql_into_statement();
        
        assertNotNull("Should parse INTO with LIMIT", ctx);
    }

    // ========== INTO Index Tests ==========

    public void testSimpleIntoIndex() {
        String input = "FROM logs-* INTO 'processed-logs';";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Esql_into_statementContext ctx = parser.esql_into_statement();
        
        assertNotNull("Should parse INTO index statement", ctx);
        assertTrue("Target should be an index", ctx.into_target() instanceof ElasticScriptParser.IntoIndexContext);
        
        ElasticScriptParser.IntoIndexContext indexTarget = (ElasticScriptParser.IntoIndexContext) ctx.into_target();
        assertEquals("'processed-logs'", indexTarget.STRING().getText());
    }

    public void testIntoIndexWithTransform() {
        String input = "FROM logs-* | WHERE level == 'ERROR' | EVAL processed_at = NOW() INTO 'error-archive';";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Esql_into_statementContext ctx = parser.esql_into_statement();
        
        assertNotNull("Should parse INTO index with transform", ctx);
    }

    public void testIntoIndexDoubleQuotes() {
        String input = "FROM logs-* INTO \"processed-logs\";";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Esql_into_statementContext ctx = parser.esql_into_statement();
        
        assertNotNull("Should parse INTO index with double quotes", ctx);
        assertTrue("Target should be an index", ctx.into_target() instanceof ElasticScriptParser.IntoIndexContext);
    }

    // ========== PROCESS WITH Tests ==========

    public void testSimpleProcessWith() {
        String input = "FROM logs-* PROCESS WITH handle_log;";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Esql_process_statementContext ctx = parser.esql_process_statement();
        
        assertNotNull("Should parse PROCESS WITH statement", ctx);
        assertEquals("handle_log", ctx.ID().getText());
        assertNull("Should not have BATCH", ctx.BATCH());
    }

    public void testProcessWithWhere() {
        String input = "FROM logs-* | WHERE level == 'ERROR' PROCESS WITH handle_error;";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Esql_process_statementContext ctx = parser.esql_process_statement();
        
        assertNotNull("Should parse PROCESS WITH WHERE", ctx);
        assertEquals("handle_error", ctx.ID().getText());
    }

    public void testProcessWithBatch() {
        String input = "FROM logs-* | WHERE level == 'ERROR' PROCESS WITH bulk_process BATCH 50;";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Esql_process_statementContext ctx = parser.esql_process_statement();
        
        assertNotNull("Should parse PROCESS WITH BATCH", ctx);
        assertEquals("bulk_process", ctx.ID().getText());
        assertNotNull("Should have BATCH", ctx.BATCH());
        assertEquals("50", ctx.INT().getText());
    }

    public void testProcessWithBatch1() {
        String input = "FROM logs-* PROCESS WITH process_one BATCH 1;";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Esql_process_statementContext ctx = parser.esql_process_statement();
        
        assertNotNull("Should parse PROCESS WITH BATCH 1", ctx);
        assertEquals("1", ctx.INT().getText());
    }

    public void testProcessWithLargeBatch() {
        String input = "FROM logs-* PROCESS WITH bulk_index BATCH 1000;";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Esql_process_statementContext ctx = parser.esql_process_statement();
        
        assertNotNull("Should parse PROCESS WITH large BATCH", ctx);
        assertEquals("1000", ctx.INT().getText());
    }

    public void testProcessWithComplexQuery() {
        String input = "FROM logs-* | WHERE level == 'ERROR' AND service == 'payment' | LIMIT 500 PROCESS WITH analyze_payment_errors BATCH 25;";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Esql_process_statementContext ctx = parser.esql_process_statement();
        
        assertNotNull("Should parse complex PROCESS WITH", ctx);
        assertEquals("analyze_payment_errors", ctx.ID().getText());
        assertEquals("25", ctx.INT().getText());
    }

    // ========== Edge Cases ==========

    public void testIntoWithIndexPattern() {
        String input = "FROM logs-2024.* INTO archived_logs;";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Esql_into_statementContext ctx = parser.esql_into_statement();
        
        assertNotNull("Should parse INTO with index pattern", ctx);
    }

    public void testProcessWithStatsQuery() {
        String input = "FROM logs-* | STATS count = COUNT(*) BY service PROCESS WITH alert_on_high_count;";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.Esql_process_statementContext ctx = parser.esql_process_statement();
        
        assertNotNull("Should parse PROCESS WITH after STATS", ctx);
    }
}
