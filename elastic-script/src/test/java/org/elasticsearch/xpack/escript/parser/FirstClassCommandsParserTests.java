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
 * Tests for parsing first-class Elasticsearch commands:
 * - INDEX document INTO 'index-name';
 * - DELETE FROM 'index-name' WHERE condition;
 * - SEARCH 'index-name' QUERY {...};
 * - REFRESH 'index-name';
 * - CREATE INDEX 'name' WITH {...};
 */
public class FirstClassCommandsParserTests extends ESTestCase {

    private ElasticScriptParser createParser(String input) {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new ElasticScriptErrorListener());
        return parser;
    }

    private ElasticScriptParser.StatementContext parseStatement(String stmt) {
        String input = "CREATE PROCEDURE test() BEGIN " + stmt + " END PROCEDURE";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        assertNotNull("Program should not be null", program);
        assertNotNull("Create procedure statement should not be null", program.create_procedure_statement());
        assertNotNull("Procedure should not be null", program.create_procedure_statement().procedure());
        assertTrue("Should have at least one statement",
            program.create_procedure_statement().procedure().statement().size() >= 1);
        
        return program.create_procedure_statement().procedure().statement(0);
    }

    // ==================== INDEX Command ====================

    public void testIndexCommandWithLiteralDocument() {
        ElasticScriptParser.StatementContext stmt = parseStatement(
            "INDEX {'name': 'John', 'age': 30} INTO 'users';"
        );
        
        assertNotNull("Statement should not be null", stmt);
        assertNotNull("index_command should not be null", stmt.index_command());
        
        ElasticScriptParser.Index_commandContext ctx = stmt.index_command();
        assertNotNull("expression should not be null", ctx.expression());
        assertNotNull("index_target should not be null", ctx.index_target());
        assertEquals("'users'", ctx.index_target().getText());
    }

    public void testIndexCommandWithVariable() {
        ElasticScriptParser.StatementContext stmt = parseStatement(
            "INDEX my_document INTO 'my-index';"
        );
        
        assertNotNull(stmt.index_command());
        ElasticScriptParser.Index_commandContext ctx = stmt.index_command();
        assertEquals("my_document", ctx.expression().getText());
        assertEquals("'my-index'", ctx.index_target().getText());
    }

    public void testIndexCommandWithVariableTarget() {
        ElasticScriptParser.StatementContext stmt = parseStatement(
            "INDEX doc INTO index_name;"
        );
        
        assertNotNull(stmt.index_command());
        ElasticScriptParser.Index_commandContext ctx = stmt.index_command();
        assertNotNull("ID target should be present", ctx.index_target().ID());
        assertEquals("index_name", ctx.index_target().ID().getText());
    }

    // ==================== DELETE Command ====================

    public void testDeleteCommandBasic() {
        ElasticScriptParser.StatementContext stmt = parseStatement(
            "DELETE FROM 'my-index' WHERE 'doc-123';"
        );
        
        assertNotNull(stmt.delete_command());
        ElasticScriptParser.Delete_commandContext ctx = stmt.delete_command();
        assertEquals("'my-index'", ctx.index_target().getText());
        assertNotNull("expression should not be null", ctx.expression());
    }

    public void testDeleteCommandWithCondition() {
        ElasticScriptParser.StatementContext stmt = parseStatement(
            "DELETE FROM 'logs-*' WHERE doc_id;"
        );
        
        assertNotNull(stmt.delete_command());
        ElasticScriptParser.Delete_commandContext ctx = stmt.delete_command();
        assertEquals("'logs-*'", ctx.index_target().getText());
        assertEquals("doc_id", ctx.expression().getText());
    }

    // ==================== SEARCH Command ====================

    public void testSearchCommandBasic() {
        ElasticScriptParser.StatementContext stmt = parseStatement(
            "SEARCH 'my-index' QUERY {'match_all': {}};"
        );
        
        assertNotNull(stmt.search_command());
        ElasticScriptParser.Search_commandContext ctx = stmt.search_command();
        assertEquals("'my-index'", ctx.index_target().getText());
        assertNotNull("QUERY expression should not be null", ctx.expression());
    }

    public void testSearchCommandWithMatchQuery() {
        ElasticScriptParser.StatementContext stmt = parseStatement(
            "SEARCH 'logs-*' QUERY {'match': {'level': 'ERROR'}};"
        );
        
        assertNotNull(stmt.search_command());
        ElasticScriptParser.Search_commandContext ctx = stmt.search_command();
        assertEquals("'logs-*'", ctx.index_target().getText());
    }

    public void testSearchCommandWithVariableQuery() {
        ElasticScriptParser.StatementContext stmt = parseStatement(
            "SEARCH index_name QUERY query_doc;"
        );
        
        assertNotNull(stmt.search_command());
        ElasticScriptParser.Search_commandContext ctx = stmt.search_command();
        assertEquals("index_name", ctx.index_target().ID().getText());
        assertEquals("query_doc", ctx.expression().getText());
    }

    // ==================== REFRESH Command ====================

    public void testRefreshCommandBasic() {
        ElasticScriptParser.StatementContext stmt = parseStatement(
            "REFRESH 'my-index';"
        );
        
        assertNotNull(stmt.refresh_command());
        ElasticScriptParser.Refresh_commandContext ctx = stmt.refresh_command();
        assertEquals("'my-index'", ctx.index_target().getText());
    }

    public void testRefreshCommandWithVariable() {
        ElasticScriptParser.StatementContext stmt = parseStatement(
            "REFRESH index_var;"
        );
        
        assertNotNull(stmt.refresh_command());
        ElasticScriptParser.Refresh_commandContext ctx = stmt.refresh_command();
        assertEquals("index_var", ctx.index_target().ID().getText());
    }

    // ==================== CREATE INDEX Command ====================

    public void testCreateIndexBasic() {
        ElasticScriptParser.StatementContext stmt = parseStatement(
            "CREATE INDEX 'new-index';"
        );
        
        assertNotNull(stmt.create_index_command());
        ElasticScriptParser.Create_index_commandContext ctx = stmt.create_index_command();
        assertEquals("'new-index'", ctx.index_target().getText());
        assertNull("Should have no options", ctx.create_index_options());
    }

    public void testCreateIndexWithMappings() {
        ElasticScriptParser.StatementContext stmt = parseStatement(
            "CREATE INDEX 'users' WITH MAPPINGS {'properties': {'name': {'type': 'text'}}};"
        );
        
        assertNotNull(stmt.create_index_command());
        ElasticScriptParser.Create_index_commandContext ctx = stmt.create_index_command();
        assertEquals("'users'", ctx.index_target().getText());
        assertNotNull("create_index_options should not be null", ctx.create_index_options());
        assertNotNull("MAPPINGS should be present", ctx.create_index_options().MAPPINGS());
    }

    public void testCreateIndexWithSettings() {
        ElasticScriptParser.StatementContext stmt = parseStatement(
            "CREATE INDEX 'logs' WITH SETTINGS {'number_of_shards': 3};"
        );
        
        assertNotNull(stmt.create_index_command());
        ElasticScriptParser.Create_index_commandContext ctx = stmt.create_index_command();
        assertNotNull("create_index_options should not be null", ctx.create_index_options());
        assertNotNull("SETTINGS should be present", ctx.create_index_options().SETTINGS());
    }

    public void testCreateIndexWithSettingsAndMappings() {
        ElasticScriptParser.StatementContext stmt = parseStatement(
            "CREATE INDEX 'data' WITH SETTINGS {'number_of_replicas': 2} MAPPINGS {'properties': {'ts': {'type': 'date'}}};"
        );
        
        assertNotNull(stmt.create_index_command());
        ElasticScriptParser.Create_index_commandContext ctx = stmt.create_index_command();
        assertNotNull("create_index_options should not be null", ctx.create_index_options());
        assertNotNull("SETTINGS should be present", ctx.create_index_options().SETTINGS());
        assertNotNull("MAPPINGS should be present", ctx.create_index_options().MAPPINGS());
        assertEquals(2, ctx.create_index_options().expression().size());
    }

    public void testCreateIndexWithFullBody() {
        ElasticScriptParser.StatementContext stmt = parseStatement(
            "CREATE INDEX 'full-index' WITH {'settings': {'number_of_shards': 1}, 'mappings': {'properties': {}}};"
        );
        
        assertNotNull(stmt.create_index_command());
        ElasticScriptParser.Create_index_commandContext ctx = stmt.create_index_command();
        assertNotNull("create_index_options should not be null", ctx.create_index_options());
        // Full body mode - no SETTINGS or MAPPINGS keywords
        assertNull("SETTINGS keyword should be null for full body", ctx.create_index_options().SETTINGS());
        assertNull("MAPPINGS keyword should be null for full body", ctx.create_index_options().MAPPINGS());
    }

    // ==================== Multiple Commands in Procedure ====================

    public void testMultipleCommandsInProcedure() {
        String input = """
            CREATE PROCEDURE data_pipeline()
            BEGIN
                CREATE INDEX 'output-index' WITH MAPPINGS {'properties': {'value': {'type': 'integer'}}};
                INDEX {'value': 42} INTO 'output-index';
                REFRESH 'output-index';
                SEARCH 'output-index' QUERY {'match_all': {}};
            END PROCEDURE
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull(program);
        assertNotNull(program.create_procedure_statement());
        
        var statements = program.create_procedure_statement().procedure().statement();
        assertEquals(4, statements.size());
        
        assertNotNull("First statement should be CREATE INDEX", statements.get(0).create_index_command());
        assertNotNull("Second statement should be INDEX", statements.get(1).index_command());
        assertNotNull("Third statement should be REFRESH", statements.get(2).refresh_command());
        assertNotNull("Fourth statement should be SEARCH", statements.get(3).search_command());
    }
}
