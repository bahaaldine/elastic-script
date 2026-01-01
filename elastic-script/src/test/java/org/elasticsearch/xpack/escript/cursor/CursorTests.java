/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.cursor;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.parser.ElasticScriptLexer;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;
import org.elasticsearch.xpack.escript.primitives.CursorDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for the CURSOR feature in elastic-script.
 */
public class CursorTests extends ESTestCase {

    /**
     * Tests that CursorDefinition can be created and stores the query properly.
     */
    public void testCursorDefinitionCreation() {
        String cursorName = "error_logs";
        String esqlQuery = "FROM application-logs | WHERE log.level == \"ERROR\" | LIMIT 10";
        
        CursorDefinition cursor = new CursorDefinition(cursorName, esqlQuery);
        
        assertEquals(cursorName, cursor.getName());
        assertEquals(esqlQuery, cursor.getEsqlQuery());
        assertFalse(cursor.isExecuted());
        assertNull(cursor.getResults());
    }

    /**
     * Tests that cursor results can be set and retrieved.
     */
    public void testCursorResultsStorage() {
        CursorDefinition cursor = new CursorDefinition("test_cursor", "FROM test | LIMIT 5");
        
        List<Map<String, Object>> mockResults = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", 1);
        row1.put("message", "Test message 1");
        mockResults.add(row1);
        
        Map<String, Object> row2 = new HashMap<>();
        row2.put("id", 2);
        row2.put("message", "Test message 2");
        mockResults.add(row2);
        
        cursor.setResults(mockResults);
        
        assertTrue(cursor.isExecuted());
        assertEquals(2, cursor.getResults().size());
        assertEquals("Test message 1", cursor.getResults().get(0).get("message"));
    }

    /**
     * Tests that cursor can be reset.
     */
    public void testCursorReset() {
        CursorDefinition cursor = new CursorDefinition("test_cursor", "FROM test");
        
        List<Map<String, Object>> mockResults = new ArrayList<>();
        mockResults.add(new HashMap<>());
        cursor.setResults(mockResults);
        
        assertTrue(cursor.isExecuted());
        
        cursor.reset();
        
        assertFalse(cursor.isExecuted());
        assertNull(cursor.getResults());
    }

    /**
     * Tests that ExecutionContext can declare and retrieve cursors.
     */
    public void testExecutionContextCursorSupport() {
        ExecutionContext context = new ExecutionContext();
        
        String cursorName = "my_cursor";
        String esqlQuery = "FROM data | LIMIT 100";
        
        context.declareCursor(cursorName, esqlQuery);
        
        assertTrue(context.hasCursor(cursorName));
        
        CursorDefinition cursor = context.getCursor(cursorName);
        assertNotNull(cursor);
        assertEquals(cursorName, cursor.getName());
        assertEquals(esqlQuery, cursor.getEsqlQuery());
    }

    /**
     * Tests that declaring a cursor with the same name throws an exception.
     */
    public void testDuplicateCursorDeclaration() {
        ExecutionContext context = new ExecutionContext();
        
        context.declareCursor("my_cursor", "FROM test1");
        
        RuntimeException exception = expectThrows(RuntimeException.class, () -> {
            context.declareCursor("my_cursor", "FROM test2");
        });
        
        assertTrue(exception.getMessage().contains("already declared"));
    }

    /**
     * Tests that child contexts can access parent context cursors.
     */
    public void testCursorInheritance() {
        ExecutionContext parent = new ExecutionContext();
        parent.declareCursor("parent_cursor", "FROM parent_data");
        
        ExecutionContext child = new ExecutionContext(parent);
        
        assertTrue(child.hasCursor("parent_cursor"));
        CursorDefinition cursor = child.getCursor("parent_cursor");
        assertNotNull(cursor);
        assertEquals("FROM parent_data", cursor.getEsqlQuery());
    }

    /**
     * Tests that the CURSOR grammar is properly parsed.
     */
    public void testCursorGrammarParsing() {
        String script = "DECLARE error_logs CURSOR FOR FROM application-logs | WHERE log.level == \"ERROR\" | LIMIT 10;";
        
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(script));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);
        
        ElasticScriptParser.Declare_statementContext declareCtx = parser.declare_statement();
        
        assertNotNull(declareCtx);
        assertNotNull(declareCtx.CURSOR());
        assertNotNull(declareCtx.ID());
        assertEquals("error_logs", declareCtx.ID().getText());
        assertNotNull(declareCtx.cursor_query());
    }

    /**
     * Tests that the FOR cursor loop grammar is properly parsed.
     */
    public void testForCursorLoopGrammarParsing() {
        String script = "FOR log_entry IN error_logs LOOP PRINT log_entry; END LOOP";
        
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(script));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);
        
        ElasticScriptParser.For_cursor_loopContext loopCtx = parser.for_cursor_loop();
        
        assertNotNull(loopCtx);
        assertEquals(2, loopCtx.ID().size());
        assertEquals("log_entry", loopCtx.ID(0).getText());
        assertEquals("error_logs", loopCtx.ID(1).getText());
        assertFalse(loopCtx.statement().isEmpty());
    }

    /**
     * Tests the full cursor workflow: declare + loop.
     */
    public void testFullCursorGrammarParsing() {
        String script = """
            PROCEDURE test_cursor_proc() BEGIN
                DECLARE error_logs CURSOR FOR FROM application-logs | WHERE log.level == "ERROR" | LIMIT 10;
                FOR log_entry IN error_logs LOOP
                    PRINT log_entry;
                END LOOP
            END PROCEDURE
            """;
        
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(script));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);
        
        ElasticScriptParser.Create_procedure_statementContext procCtx = parser.create_procedure_statement();
        
        assertNotNull(procCtx);
        assertNotNull(procCtx.procedure());
        assertFalse(procCtx.procedure().statement().isEmpty());
        assertEquals(2, procCtx.procedure().statement().size());
    }
}

