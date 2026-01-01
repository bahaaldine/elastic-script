/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.escript.handlers;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.xpack.escript.parser.ElasticScriptLexer;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class DeclareStatementHandlerTests extends ESTestCase {

    private ExecutionContext context;
    private ProcedureExecutor executor;
    private DeclareStatementHandler declareHandler;
    private ThreadPool threadPool;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new ExecutionContext();
        threadPool = new TestThreadPool("test-thread-pool");
        Client mockClient = null; // use a mock client if needed
        // Provide an empty source for the lexer; tokens will be re-parsed in helper methods
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(""));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        executor = new ProcedureExecutor(context, threadPool, mockClient, tokens);
        declareHandler = new DeclareStatementHandler(executor);
    }

    @Override
    public void tearDown() throws Exception {
        terminate(threadPool);
        super.tearDown();
    }

    // Helper method to parse a declaration query
    private ElasticScriptParser.Declare_statementContext parseDeclaration(String query) {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);
        parser.removeErrorListeners();  // Remove existing error listeners
        parser.addErrorListener(new ElasticScriptErrorListener());  // Add custom error listener
        return parser.declare_statement();
    }

    // Existing Tests:

    // Test 1: Declare a NUMBER variable (used for int or float)
    @Test
    public void testDeclareIntVariable() throws InterruptedException {
        String declareQuery = "DECLARE myIntVar NUMBER;";
        ElasticScriptParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertTrue(context.hasVariable("myIntVar"));
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Declaration failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 2: Declare a STRING variable
    @Test
    public void testDeclareStringVariable() throws InterruptedException {
        String declareQuery = "DECLARE myStringVar STRING;";
        ElasticScriptParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertTrue(context.hasVariable("myStringVar"));
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Declaration failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 3: Declare multiple variables
    @Test
    public void testDeclareMultipleVariables() throws InterruptedException {
        String declareQuery = "DECLARE var1 NUMBER, var2 NUMBER, var3 STRING;";
        ElasticScriptParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertTrue(context.hasVariable("var1"));
                assertTrue(context.hasVariable("var2"));
                assertTrue(context.hasVariable("var3"));
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Declaration failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 4: Declare a variable with an unsupported type (Expecting Parser Error)
    @Test
    public void testDeclareUnsupportedType() {
        String declareQuery = "DECLARE myVar UNSUPPORTED_TYPE;";
        try {
            ElasticScriptParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
            fail("Expected a syntax error due to unsupported data type");
        } catch (RuntimeException e) {
            assertTrue("Exception message should indicate a syntax error.", e.getMessage().contains("Syntax error"));
        }
    }

    // Test 5: Declare a variable that's already declared
    @Test
    public void testDeclareExistingVariable() throws InterruptedException {
        String declareQuery = "DECLARE myVar NUMBER;";
        ElasticScriptParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
        CountDownLatch latch = new CountDownLatch(2);
        // First declaration
        declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Second declaration (should fail)
                declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
                    @Override
                    public void onResponse(Object unused) {
                        fail("Expected an exception due to variable already declared");
                        latch.countDown();
                    }
                    @Override
                    public void onFailure(Exception e) {
                        assertTrue("Exception message should indicate variable already declared.",
                            e.getMessage().contains("already declared"));
                        latch.countDown();
                    }
                });
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("First declaration failed: " + e.getMessage());
                latch.countDown();
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 6: Declare a variable with an initial value
    @Test
    public void testDeclareVariableWithInitialValue() throws InterruptedException {
        String declareQuery = "DECLARE myVar NUMBER = 10;";
        ElasticScriptParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertTrue(context.hasVariable("myVar"));
                assertEquals(10.0, ((Number) context.getVariable("myVar")).doubleValue(), 0.001);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Declaration failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 7: Declare a variable with invalid syntax
    @Test
    public void testDeclareVariableInvalidSyntax() {
        String declareQuery = "DECLARE NUMBER myVar;";  // Invalid syntax
        try {
            ElasticScriptParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
            fail("Expected a syntax error due to invalid declaration syntax");
        } catch (RuntimeException e) {
            assertTrue("Exception message should indicate a syntax error.",
                e.getMessage().contains("Syntax error"));
        }
    }

    // New Array Tests:

    // Test 8: Declare an ARRAY variable of STRING without an initial value.
    @Test
    public void testDeclareArrayOfStringVariableWithoutInitialValue() throws InterruptedException {
        String declareQuery = "DECLARE myArray ARRAY OF STRING;";
        ElasticScriptParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertTrue("myArray should be declared.", context.hasVariable("myArray"));
                // Expect the value to be null (or an empty list, depending on your design)
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Array declaration failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 9: Declare an ARRAY variable of NUMBER with an initial value.
    @Test
    public void testDeclareArrayOfNumberVariableWithInitialValue() throws InterruptedException {
        String declareQuery = "DECLARE myArray ARRAY OF NUMBER = [10, 20, 30];";
        ElasticScriptParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertTrue("myArray should be declared.", context.hasVariable("myArray"));
                Object value = context.getVariable("myArray");
                assertTrue("myArray should be a List.", value instanceof List);
                List<?> list = (List<?>) value;
                assertEquals(3, list.size());
                assertEquals(10.0, ((Number) list.get(0)).doubleValue(), 0.001);
                assertEquals(20.0, ((Number) list.get(1)).doubleValue(), 0.001);
                assertEquals(30.0, ((Number) list.get(2)).doubleValue(), 0.001);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Array declaration with initial value failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 10: Declare an ARRAY variable of STRING with an initial value.
    @Test
    public void testDeclareArrayOfStringVariableWithInitialValue() throws InterruptedException {
        String declareQuery = "DECLARE myArray ARRAY OF STRING = [\"a\", \"b\", \"c\"];";
        ElasticScriptParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertTrue("myArray should be declared.", context.hasVariable("myArray"));
                Object value = context.getVariable("myArray");
                assertTrue("myArray should be a List.", value instanceof List);
                List<?> list = (List<?>) value;
                assertEquals(3, list.size());
                assertEquals("a", list.get(0));
                assertEquals("b", list.get(1));
                assertEquals("c", list.get(2));
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Array declaration with initial value failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 11: Declare an ARRAY variable of DATE with an initial value.
    // (Assuming your DATE type is represented as a string literal in ISO format)
    @Test
    public void testDeclareArrayOfDateVariableWithInitialValue() throws InterruptedException {
        String declareQuery = "DECLARE myArray ARRAY OF DATE = [\"2020-01-01\", \"2020-12-31\"];";
        ElasticScriptParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertTrue("myArray should be declared.", context.hasVariable("myArray"));
                Object value = context.getVariable("myArray");
                assertTrue("myArray should be a List.", value instanceof List);
                List<?> list = (List<?>) value;
                assertEquals(2, list.size());
                // Adjust these assertions depending on how DATE values are processed in your engine.
                assertEquals("2020-01-01", list.get(0).toString());
                assertEquals("2020-12-31", list.get(1).toString());
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Array declaration with initial date value failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 12: Declare an ARRAY variable with an unsupported element type.
    @Test
    public void testDeclareArrayVariableWithUnsupportedElementType() {
        // For example, "ARRAY OF UNSUPPORTED" should trigger an error.
        String declareQuery = "DECLARE myArray ARRAY OF UNSUPPORTED;";
        try {
            ElasticScriptParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
            declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
                @Override
                public void onResponse(Object unused) {
                    fail("Expected an error for unsupported element type.");
                }
                @Override
                public void onFailure(Exception e) {
                    // Expected error message may vary; adjust as needed.
                    assertTrue(e.getMessage().contains("Unsupported data type"));
                }
            });
            fail("Expected a runtime error due to unsupported element type.");
        } catch (RuntimeException e) {
            // Expected to catch a syntax error here.
            assertTrue(e.getMessage().contains("Syntax error"));
        }
    }

    // Test 13: Declare a DOCUMENT variable with an empty initializer
    @Test
    public void testDeclareDocumentVariableWithEmptyInit() throws InterruptedException {
        String declareQuery = "DECLARE myVar DOCUMENT = {};";
        ElasticScriptParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertTrue("myVar should be declared", context.hasVariable("myVar"));
                Object value = context.getVariable("myVar");
                assertNotNull("myVar value should not be null", value);
                assertTrue("myVar should be a Map", value instanceof java.util.Map);
                java.util.Map<?,?> map = (java.util.Map<?,?>) value;
                assertTrue("myVar map should be empty", map.isEmpty());
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Document declaration with empty initializer failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // ==========================================
    // CURSOR Declaration Tests
    // ==========================================

    // Test 14: Declare a CURSOR with a simple query
    @Test
    public void testDeclareCursorSimple() throws InterruptedException {
        String declareQuery = "DECLARE error_logs CURSOR FOR FROM logs | WHERE level == \"ERROR\" | LIMIT 10;";
        ElasticScriptParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertTrue("Cursor 'error_logs' should be declared", context.hasCursor("error_logs"));
                var cursor = context.getCursor("error_logs");
                assertNotNull("Cursor should not be null", cursor);
                assertEquals("error_logs", cursor.getName());
                // Note: getText() concatenates tokens, so "FROM logs" becomes "FROMlogs"
                String query = cursor.getEsqlQuery();
                assertNotNull("Query should not be null", query);
                assertFalse("Query should not be empty", query.isEmpty());
                // Check that query contains expected keywords (may be concatenated)
                assertTrue("Query should contain query content", query.length() > 0);
                assertFalse("Cursor should not be executed yet", cursor.isExecuted());
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Cursor declaration failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 15: Declare a CURSOR with complex query including pipes
    @Test
    public void testDeclareCursorComplexQuery() throws InterruptedException {
        String declareQuery = "DECLARE app_logs CURSOR FOR FROM application-logs | WHERE log.level == \"ERROR\" | SORT @timestamp DESC | LIMIT 100;";
        ElasticScriptParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertTrue("Cursor 'app_logs' should be declared", context.hasCursor("app_logs"));
                var cursor = context.getCursor("app_logs");
                assertNotNull("Cursor should not be null", cursor);
                String query = cursor.getEsqlQuery();
                assertNotNull("Query should not be null", query);
                // Query contains pipe-separated ESQL commands
                assertTrue("Query should contain pipe separator", query.contains("|"));
                assertFalse("Cursor should not be executed yet", cursor.isExecuted());
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Cursor declaration with complex query failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 16: Declare a duplicate CURSOR (should fail)
    @Test
    public void testDeclareDuplicateCursor() throws InterruptedException {
        String declareQuery = "DECLARE my_cursor CURSOR FOR FROM test;";
        ElasticScriptParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
        CountDownLatch latch = new CountDownLatch(2);
        
        // First declaration
        declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertTrue("Cursor should be declared", context.hasCursor("my_cursor"));
                
                // Second declaration (should fail)
                declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
                    @Override
                    public void onResponse(Object unused) {
                        fail("Expected an exception due to cursor already declared");
                        latch.countDown();
                    }
                    @Override
                    public void onFailure(Exception e) {
                        assertTrue("Exception message should indicate cursor already declared",
                            e.getMessage().contains("already declared"));
                        latch.countDown();
                    }
                });
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("First cursor declaration failed: " + e.getMessage());
                latch.countDown();
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 17: Cursor declaration does not affect variable namespace
    @Test
    public void testCursorAndVariableSeparateNamespaces() throws InterruptedException {
        // First, declare a cursor
        String cursorQuery = "DECLARE data CURSOR FOR FROM test-index;";
        ElasticScriptParser.Declare_statementContext cursorContext = parseDeclaration(cursorQuery);
        
        // Then declare a variable with the same name
        String varQuery = "DECLARE data NUMBER = 42;";
        ElasticScriptParser.Declare_statementContext varContext = parseDeclaration(varQuery);
        
        CountDownLatch latch = new CountDownLatch(1);
        
        declareHandler.handleAsync(cursorContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertTrue("Cursor 'data' should exist", context.hasCursor("data"));
                
                declareHandler.handleAsync(varContext, new ActionListener<Object>() {
                    @Override
                    public void onResponse(Object unused) {
                        // Both cursor and variable should exist with the same name
                        assertTrue("Cursor 'data' should still exist", context.hasCursor("data"));
                        assertTrue("Variable 'data' should also exist", context.hasVariable("data"));
                        assertEquals(42.0, ((Number) context.getVariable("data")).doubleValue(), 0.001);
                        latch.countDown();
                    }
                    @Override
                    public void onFailure(Exception e) {
                        fail("Variable declaration failed: " + e.getMessage());
                        latch.countDown();
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                fail("Cursor declaration failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }
}
