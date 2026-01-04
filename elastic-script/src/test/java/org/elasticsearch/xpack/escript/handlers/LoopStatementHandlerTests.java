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
import org.elasticsearch.xpack.escript.utils.TestUtils;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class LoopStatementHandlerTests extends ESTestCase {

    private ExecutionContext context;
    private ProcedureExecutor executor;
    private ThreadPool threadPool;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new ExecutionContext();
        threadPool = new TestThreadPool("test-thread-pool");
        Client mockClient = null; // For tests, use null or a mocked client.
        // Create a dummy lexer to provide a token stream; the actual source is provided per test.
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(""));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        executor = new ProcedureExecutor(context, threadPool, mockClient, tokens);
    }

    @Override
    public void tearDown() throws Exception {
        terminate(threadPool);
        super.tearDown();
    }

    // Test 1: Simple FOR-range loop.
    @Test
    public void testSimpleForRangeLoop() throws InterruptedException {
        String blockQuery =
            "PROCEDURE dummy_function(INOUT x NUMBER) " +
                "BEGIN DECLARE j NUMBER, i NUMBER; " +
                "FOR i IN 1..3 LOOP " +
                " SET j = i + 1; " +
                "END LOOP " +
            "END PROCEDURE";
        ElasticScriptParser.ProcedureContext blockContext = TestUtils.parseBlock(blockQuery);
        CountDownLatch latch = new CountDownLatch(1);
        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertNotNull("j should be declared", context.getVariable("j"));
                // For i = 3, j = 3 + 1 = 4.
                assertEquals(4.0, context.getVariable("j"));
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 2: Simple WHILE loop.
    @Test
    public void testSimpleWhileLoop() throws InterruptedException {
        String blockQuery = " " +
            "PROCEDURE dummy_function(INOUT x NUMBER) +" +
                "BEGIN DECLARE i NUMBER = 1; WHILE i < 4 LOOP SET i = i + 1; END LOOP " +
            "END PROCEDURE";
        ElasticScriptParser.ProcedureContext blockContext = TestUtils.parseBlock(blockQuery);
        CountDownLatch latch = new CountDownLatch(1);
        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertNotNull("i should be declared", context.getVariable("i"));
                // i increments until 4 is reached.
                assertEquals(4.0, context.getVariable("i"));
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 3: Reverse FOR-range loop.
    @Test
    public void testReverseForRangeLoop() throws InterruptedException {
        String blockQuery = " " +
            "PROCEDURE dummy_function(INOUT x NUMBER) " +
                "BEGIN DECLARE j NUMBER = 0, i NUMBER; FOR i IN 5..3 LOOP SET j = j + i; END LOOP " +
            "END PROCEDURE";
        ElasticScriptParser.ProcedureContext blockContext = TestUtils.parseBlock(blockQuery);
        CountDownLatch latch = new CountDownLatch(1);
        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertNotNull("j should be declared", context.getVariable("j"));
                // Sum of 5, 4, 3 equals 12.
                assertEquals(12.0, context.getVariable("j"));
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 4: For-array loop (new rule). Iterates over an array literal.
    @Test
    public void testForArrayLoop() throws InterruptedException {
        // In this test, we declare an array and iterate over its elements.
        // We compute the sum of the elements.
        String blockQuery =
            "PROCEDURE dummy_function(INOUT x NUMBER) " +
            "BEGIN " +
                "DECLARE sum NUMBER = 0; " +
                "DECLARE arr ARRAY OF NUMBER = [10, 20, 30]; " +
                "FOR element IN arr LOOP " +
                " SET sum = sum + element; " +
                "END LOOP " +
                "RETURN sum; " +
            "END PROCEDURE ";
        ElasticScriptParser.ProcedureContext blockContext = TestUtils.parseBlock(blockQuery);
        CountDownLatch latch = new CountDownLatch(1);
        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                // We expect the procedure to return 60.
                Object sumValue = context.getVariable("sum");
                assertNotNull("sum should be declared", sumValue);
                assertEquals(60.0, ((Number) sumValue).doubleValue(), 0.001);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 5: Nested FOR loop.
    @Test
    public void testNestedForLoop() throws InterruptedException {
        String blockQuery =
            "PROCEDURE dummy_function(INOUT x NUMBER) " +
            "BEGIN " +
                "DECLARE i NUMBER; DECLARE j NUMBER; " +
                "FOR i IN 1..2 LOOP " +
                " FOR j IN 1..2 LOOP " +
                "  SET j = j + 1; " +
                " END LOOP " +
                "END LOOP " +
             "END PROCEDURE ";
        ElasticScriptParser.ProcedureContext blockContext = TestUtils.parseBlock(blockQuery);
        CountDownLatch latch = new CountDownLatch(1);
        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertNotNull("i should be declared", context.getVariable("i"));
                assertNotNull("j should be declared", context.getVariable("j"));
                // Outer loop runs twice so i should be 2.
                assertEquals(2.0, context.getVariable("i"));
                // Inner loop increments j: for each iteration, j becomes j+1.
                // With two iterations, if j started at 0 then j should be 2+1=3.
                assertEquals(3.0, context.getVariable("j"));
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 6: For-array loop with strings.
    @Test
    public void testForArrayLoopWithStrings() throws InterruptedException {
        String blockQuery =
            "PROCEDURE dummy_function(INOUT x NUMBER) " +
            "BEGIN " +
                "DECLARE last STRING = ''; " +
                "DECLARE arr ARRAY OF STRING = [\"alpha\", \"beta\", \"gamma\"  ]; " +
                "FOR element IN arr LOOP " +
                " SET last = element; " +
                "END LOOP " +
                "RETURN last; " +
            "END PROCEDURE";
        ElasticScriptParser.ProcedureContext blockContext = TestUtils.parseBlock(blockQuery);
        CountDownLatch latch = new CountDownLatch(1);
        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                Object lastValue = context.getVariable("last");
                assertNotNull("last should be declared", lastValue);
                assertEquals("gamma", lastValue);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

//    // Test 7: For-array loop with nested arrays.
//    @Test
//    public void testForArrayLoopWithNestedArrays() throws InterruptedException {
//        String blockQuery =
//            "BEGIN " +
//                "DECLARE sum NUMBER = 0; " +
//                "DECLARE arr ARRAY OF ARRAY OF NUMBER = [[1,2], [3,4]]; " +
//                "FOR innerArr IN arr LOOP " +
//                " SET sum = sum + innerArr[0]; " +
//                "END LOOP " +
//                "RETURN sum; " +
//                "END";
//        ElasticScriptParser.ProcedureContext blockContext = TestUtils.parseBlock(blockQuery);
//        CountDownLatch latch = new CountDownLatch(1);
//        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
//            @Override
//            public void onResponse(Object result) {
//                Object sumValue = context.getVariable("sum");
//                assertNotNull("sum should be declared", sumValue);
//                assertEquals(4.0, ((Number) sumValue).doubleValue(), 0.001);
//                latch.countDown();
//            }
//            @Override
//            public void onFailure(Exception e) {
//                fail("Execution failed: " + e.getMessage());
//                latch.countDown();
//            }
//        });
//        latch.await();
//    }

    // Test 8: For-array loop with documents.
    @Test
    public void testForArrayLoopWithDocuments() throws InterruptedException {
        String blockQuery =
            "PROCEDURE dummy_function(INOUT x NUMBER) " +
            "BEGIN " +
                "DECLARE sum NUMBER = 0; " +
                "DECLARE arr ARRAY OF DOCUMENT = [{\"value\":10}, {\"value\":20}]; " +
                "FOR doc IN arr LOOP " +
                " SET sum = sum + doc['value']; " +
                "END LOOP " +
                "RETURN sum; " +
            "END PROCEDURE";
        ElasticScriptParser.ProcedureContext blockContext = TestUtils.parseBlock(blockQuery);
        CountDownLatch latch = new CountDownLatch(1);
        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                Object sumValue = context.getVariable("sum");
                assertNotNull("sum should be declared", sumValue);
                assertEquals(30.0, ((Number) sumValue).doubleValue(), 0.001);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testForArrayLoopWithMixedDocumentFields() throws InterruptedException {
        String blockQuery =
            "PROCEDURE dummy_function(INOUT x NUMBER) " +
            "BEGIN " +
                "  DECLARE sum NUMBER = 0; " +
                "  DECLARE texts STRING = ''; " +
                "  DECLARE flags STRING = ''; " +
                "  DECLARE arr ARRAY OF DOCUMENT = [" +
                "    {\"value\": 10, \"text\": \"alpha\", \"flag\": true}, " +
                "    {\"value\": 20, \"text\": \"beta\", \"flag\": false}" +
                "  ]; " +
                "  FOR doc IN arr LOOP " +
                "    SET sum = sum + doc['value']; " +
                "    SET texts = texts + doc['text'] + ' '; " +
                "    IF doc['flag'] THEN " +
                "       SET flags = flags + 'T '; " +
                "    ELSE " +
                "       SET flags = flags + 'F '; " +
                "    END IF; " +
                "  END LOOP " +
                "  RETURN sum; " +
            "END PROCEDURE";
        ElasticScriptParser.ProcedureContext blockContext = TestUtils.parseBlock(blockQuery);
        CountDownLatch latch = new CountDownLatch(1);
        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                Object sumValue = context.getVariable("sum");
                Object textsValue = context.getVariable("texts");
                Object flagsValue = context.getVariable("flags");
                assertNotNull("sum should be declared", sumValue);
                assertNotNull("texts should be declared", textsValue);
                assertNotNull("flags should be declared", flagsValue);
                // Expected sum is 10 + 20 = 30.
                assertEquals(30.0, ((Number) sumValue).doubleValue(), 0.001);
                // Expected concatenated texts is "alpha beta " (may vary if trailing space matters)
                assertEquals("alpha beta ", textsValue);
                // Expected flags string is "T F " (using 'T' for true, 'F' for false)
                assertEquals("T F ", flagsValue);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 9: For-array loop with an empty array.
    @Test
    public void testForArrayLoopEmptyArray() throws InterruptedException {
        String blockQuery =
            "PROCEDURE dummy_function(INOUT x NUMBER) " +
            "BEGIN " +
                "DECLARE count NUMBER = 0; " +
                "DECLARE arr ARRAY OF NUMBER = []; " +
                "FOR element IN arr LOOP " +
                " SET count = count + 1; " +
                "END LOOP " +
                "RETURN count; " +
            "END PROCEDURE";
        ElasticScriptParser.ProcedureContext blockContext = TestUtils.parseBlock(blockQuery);
        CountDownLatch latch = new CountDownLatch(1);
        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                Object countValue = context.getVariable("count");
                assertNotNull("count should be declared", countValue);
                assertEquals(0.0, ((Number) countValue).doubleValue(), 0.001);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

/*
    // Test 6: Infinite WHILE loop with break condition.
    @Test
    public void testInfiniteWhileLoopWithBreak() throws InterruptedException {
        String blockQuery =
            "BEGIN " +
                "DECLARE i NUMBER = 1; " +
                "WHILE 1 = 1 LOOP " +
                " SET i = i + 1; " +
                " IF i > 1000 THEN BREAK; END IF; " +
                "END LOOP " +
                "RETURN i; " +
                "END";
        ElasticScriptParser.ProcedureContext blockContext = TestUtils.parseBlock(blockQuery);
        CountDownLatch latch = new CountDownLatch(1);
        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                // Expect i to be 1001
                Object iValue = context.getVariable("i");
                assertNotNull("i should be declared", iValue);
                assertEquals(1001.0, ((Number) iValue).doubleValue(), 0.001);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }*/

    // ==========================================
    // CURSOR Loop Tests
    // ==========================================

    // Test 10: FOR-cursor loop grammar parsing (verifies parser accepts cursor loop syntax)
    @Test
    public void testForCursorLoopGrammarParsing() {
        String blockQuery =
            "PROCEDURE test_cursor_loop() " +
            "BEGIN " +
                "DECLARE error_logs CURSOR FOR FROM logs | WHERE level == \"ERROR\" | LIMIT 10; " +
                "FOR log_entry IN error_logs LOOP " +
                " PRINT log_entry; " +
                "END LOOP " +
            "END PROCEDURE";
        
        // Verify that the procedure parses correctly
        ElasticScriptParser.ProcedureContext blockContext = TestUtils.parseBlock(blockQuery);
        assertNotNull("Procedure context should not be null", blockContext);
        assertEquals("Should have 2 statements (DECLARE CURSOR and FOR loop)", 
            2, blockContext.statement().size());
    }

    // Test 11: Cursor declaration and loop structure are correctly parsed
    @Test
    public void testCursorDeclarationInLoop() throws InterruptedException {
        String blockQuery =
            "PROCEDURE test_cursor() " +
            "BEGIN " +
                "DECLARE my_cursor CURSOR FOR FROM test-index | LIMIT 5; " +
            "END PROCEDURE";
        
        // Create executor with proper token stream for getRawText() to work
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(blockQuery));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);
        ElasticScriptParser.ProcedureContext blockContext = parser.procedure();
        ProcedureExecutor cursorExecutor = new ProcedureExecutor(context, threadPool, null, tokens);
        
        CountDownLatch latch = new CountDownLatch(1);
        
        cursorExecutor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                // Verify cursor was declared
                assertTrue("Cursor 'my_cursor' should be declared", context.hasCursor("my_cursor"));
                var cursor = context.getCursor("my_cursor");
                assertNotNull("Cursor should exist", cursor);
                assertEquals("my_cursor", cursor.getName());
                // Note: getText() concatenates tokens, verify query is captured
                String query = cursor.getEsqlQuery();
                assertNotNull("Query should not be null", query);
                assertFalse("Query should not be empty", query.isEmpty());
                assertTrue("Query should contain pipe", query.contains("|"));
                assertFalse("Cursor should not be executed until iterated", cursor.isExecuted());
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

    // Test 12: FOR-cursor loop with undeclared cursor (should fail)
    // This test verifies that iterating over an undeclared cursor/variable throws an exception.
    @Test
    public void testForCursorLoopWithUndeclaredCursor() {
        String blockQuery =
            "PROCEDURE test_undeclared_cursor() " +
            "BEGIN " +
                "FOR item IN non_existent_cursor LOOP " +
                " PRINT item; " +
                "END LOOP " +
            "END PROCEDURE";
        
        ElasticScriptParser.ProcedureContext blockContext = TestUtils.parseBlock(blockQuery);
        
        // The exception should be thrown when trying to iterate over an undeclared identifier
        RuntimeException exception = expectThrows(RuntimeException.class, () -> {
            CountDownLatch latch = new CountDownLatch(1);
            executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
                @Override
                public void onResponse(Object result) {
                    latch.countDown();
                }
                @Override
                public void onFailure(Exception e) {
                    latch.countDown();
                    throw new RuntimeException(e);
                }
            });
            latch.await();
        });
        
        assertTrue("Error should indicate undefined identifier: " + exception.getMessage(), 
            exception.getMessage().contains("not declared") || exception.getMessage().contains("not defined"));
    }

    // Note: Full cursor execution tests require an Elasticsearch cluster with ESQL support.
    // These tests verify that the grammar and handler infrastructure is correctly set up.
    // Integration tests for actual cursor execution should be in integration test suites.
}
