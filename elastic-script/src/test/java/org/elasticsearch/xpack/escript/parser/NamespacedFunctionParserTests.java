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
 * Tests for parsing namespaced function calls.
 * Supports both simple function calls (MY_FUNC) and namespaced calls (NAMESPACE.METHOD).
 */
public class NamespacedFunctionParserTests extends ESTestCase {

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

    // ==================== Simple Function Calls ====================

    public void testSimpleFunctionCallNoArgs() {
        ElasticScriptParser.StatementContext stmt = parseStatement(
            "MY_FUNCTION();"
        );
        
        assertNotNull(stmt.function_call_statement());
        ElasticScriptParser.Function_callContext ctx = stmt.function_call_statement().function_call();
        assertNotNull("simple_function_call should not be null", ctx.simple_function_call());
        assertNull("namespaced_function_call should be null", ctx.namespaced_function_call());
        assertEquals("MY_FUNCTION", ctx.simple_function_call().ID().getText());
    }

    public void testSimpleFunctionCallWithArgs() {
        ElasticScriptParser.StatementContext stmt = parseStatement(
            "PRINT_VALUE(42, 'hello');"
        );
        
        assertNotNull(stmt.function_call_statement());
        ElasticScriptParser.Function_callContext ctx = stmt.function_call_statement().function_call();
        assertNotNull(ctx.simple_function_call());
        assertEquals("PRINT_VALUE", ctx.simple_function_call().ID().getText());
        assertNotNull(ctx.simple_function_call().argument_list());
        assertEquals(2, ctx.simple_function_call().argument_list().expression().size());
    }

    // ==================== Namespaced Function Calls ====================

    public void testNamespacedFunctionCallNoArgs() {
        ElasticScriptParser.StatementContext stmt = parseStatement(
            "ARRAY.LENGTH();"
        );
        
        assertNotNull(stmt.function_call_statement());
        ElasticScriptParser.Function_callContext ctx = stmt.function_call_statement().function_call();
        assertNotNull("namespaced_function_call should not be null", ctx.namespaced_function_call());
        assertNull("simple_function_call should be null", ctx.simple_function_call());
        
        ElasticScriptParser.Namespaced_function_callContext nsCtx = ctx.namespaced_function_call();
        assertEquals("ARRAY", nsCtx.namespace_id().getText());
        assertEquals("LENGTH", nsCtx.ID().getText());
    }

    public void testNamespacedFunctionCallWithArgs() {
        ElasticScriptParser.StatementContext stmt = parseStatement(
            "STRING.REPLACE(text, 'old', 'new');"
        );
        
        assertNotNull(stmt.function_call_statement());
        ElasticScriptParser.Function_callContext ctx = stmt.function_call_statement().function_call();
        assertNotNull(ctx.namespaced_function_call());
        
        ElasticScriptParser.Namespaced_function_callContext nsCtx = ctx.namespaced_function_call();
        assertEquals("STRING", nsCtx.namespace_id().getText());
        assertEquals("REPLACE", nsCtx.ID().getText());
        assertNotNull(nsCtx.argument_list());
        assertEquals(3, nsCtx.argument_list().expression().size());
    }

    public void testArrayMapNamespaced() {
        ElasticScriptParser.StatementContext stmt = parseStatement(
            "ARRAY.MAP(items, x => x * 2);"
        );
        
        assertNotNull(stmt.function_call_statement());
        ElasticScriptParser.Namespaced_function_callContext nsCtx = 
            stmt.function_call_statement().function_call().namespaced_function_call();
        assertNotNull(nsCtx);
        assertEquals("ARRAY", nsCtx.namespace_id().getText());
        assertEquals("MAP", nsCtx.ID().getText());
        assertEquals(2, nsCtx.argument_list().expression().size());
    }

    public void testStringUpperNamespaced() {
        ElasticScriptParser.StatementContext stmt = parseStatement(
            "STRING.UPPER(my_text);"
        );
        
        assertNotNull(stmt.function_call_statement());
        ElasticScriptParser.Namespaced_function_callContext nsCtx = 
            stmt.function_call_statement().function_call().namespaced_function_call();
        assertNotNull(nsCtx);
        assertEquals("STRING", nsCtx.namespace_id().getText());
        assertEquals("UPPER", nsCtx.ID().getText());
    }

    public void testK8sGetPodsNamespaced() {
        ElasticScriptParser.StatementContext stmt = parseStatement(
            "K8S.GET_PODS('default');"
        );
        
        assertNotNull(stmt.function_call_statement());
        ElasticScriptParser.Namespaced_function_callContext nsCtx = 
            stmt.function_call_statement().function_call().namespaced_function_call();
        assertNotNull(nsCtx);
        assertEquals("K8S", nsCtx.namespace_id().getText());
        assertEquals("GET_PODS", nsCtx.ID().getText());
        assertEquals(1, nsCtx.argument_list().expression().size());
    }

    public void testAwsS3GetNamespaced() {
        ElasticScriptParser.StatementContext stmt = parseStatement(
            "AWS.S3_GET('my-bucket', 'path/to/file.json');"
        );
        
        assertNotNull(stmt.function_call_statement());
        ElasticScriptParser.Namespaced_function_callContext nsCtx = 
            stmt.function_call_statement().function_call().namespaced_function_call();
        assertNotNull(nsCtx);
        assertEquals("AWS", nsCtx.namespace_id().getText());
        assertEquals("S3_GET", nsCtx.ID().getText());
        assertEquals(2, nsCtx.argument_list().expression().size());
    }

    public void testHttpGetNamespaced() {
        ElasticScriptParser.StatementContext stmt = parseStatement(
            "HTTP.GET('https://api.example.com/data');"
        );
        
        assertNotNull(stmt.function_call_statement());
        ElasticScriptParser.Namespaced_function_callContext nsCtx = 
            stmt.function_call_statement().function_call().namespaced_function_call();
        assertNotNull(nsCtx);
        assertEquals("HTTP", nsCtx.namespace_id().getText());
        assertEquals("GET", nsCtx.ID().getText());
    }

    public void testDocumentKeysNamespaced() {
        ElasticScriptParser.StatementContext stmt = parseStatement(
            "DOCUMENT.KEYS(my_doc);"
        );
        
        assertNotNull(stmt.function_call_statement());
        ElasticScriptParser.Namespaced_function_callContext nsCtx = 
            stmt.function_call_statement().function_call().namespaced_function_call();
        assertNotNull(nsCtx);
        assertEquals("DOCUMENT", nsCtx.namespace_id().getText());
        assertEquals("KEYS", nsCtx.ID().getText());
    }

    public void testDateAddNamespaced() {
        ElasticScriptParser.StatementContext stmt = parseStatement(
            "DATE.ADD(CURRENT_DATE(), '1d');"
        );
        
        assertNotNull(stmt.function_call_statement());
        ElasticScriptParser.Namespaced_function_callContext nsCtx = 
            stmt.function_call_statement().function_call().namespaced_function_call();
        assertNotNull(nsCtx);
        assertEquals("DATE", nsCtx.namespace_id().getText());
        assertEquals("ADD", nsCtx.ID().getText());
        assertEquals(2, nsCtx.argument_list().expression().size());
    }

    // ==================== Mixed Usage ====================

    public void testMixedFunctionCallsInProcedure() {
        String input = """
            CREATE PROCEDURE data_transform()
            BEGIN
                DECLARE items ARRAY = [1, 2, 3];
                DECLARE doubled ARRAY;
                SET doubled = ARRAY.MAP(items, x => x * 2);
                DECLARE result STRING;
                SET result = STRING.JOIN(doubled, ', ');
                PRINT(result);
            END PROCEDURE
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull(program);
        assertNotNull(program.create_procedure_statement());
    }

    // ==================== In Expressions ====================

    public void testNamespacedFunctionInExpression() {
        String input = """
            CREATE PROCEDURE test()
            BEGIN
                DECLARE length NUMBER;
                SET length = ARRAY.LENGTH(my_array);
            END PROCEDURE
            """;
        
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();
        
        assertNotNull(program);
        // The SET statement should contain an assignment with ARRAY.LENGTH in the expression
        var statements = program.create_procedure_statement().procedure().statement();
        assertEquals(2, statements.size());
        
        // Second statement is the SET
        var setStmt = statements.get(1).assignment_statement();
        assertNotNull(setStmt);
    }
}
