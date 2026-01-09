/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.language;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.ArrayBuiltInFunctions;
import org.elasticsearch.xpack.escript.parser.ElasticScriptLexer;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;
import org.elasticsearch.xpack.escript.primitives.ReturnValue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Unit tests for Phase 7: Lambda Expressions.
 */
public class LambdaExpressionTests extends ESTestCase {

    private ThreadPool threadPool;
    private ExecutionContext context;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        threadPool = new TestThreadPool("test");
        context = new ExecutionContext();
        // Register array functions that support lambdas
        ArrayBuiltInFunctions.registerAll(context);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        ThreadPool.terminate(threadPool, 10, TimeUnit.SECONDS);
    }

    private Object executeScript(String script) throws Exception {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(script));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);
        
        ProcedureExecutor executor = new ProcedureExecutor(context, threadPool, null, tokens);
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();
        
        ElasticScriptParser.ProcedureContext procedureCtx = parser.procedure();
        executor.visitProcedureAsync(procedureCtx, new ActionListener<>() {
            @Override
            public void onResponse(Object o) {
                result.set(o);
                latch.countDown();
            }
            
            @Override
            public void onFailure(Exception e) {
                error.set(e);
                latch.countDown();
            }
        });
        
        assertTrue("Execution timed out", latch.await(10, TimeUnit.SECONDS));
        if (error.get() != null) {
            throw error.get();
        }
        Object rawResult = result.get();
        if (rawResult instanceof ReturnValue returnValue) {
            return returnValue.getValue();
        }
        return rawResult;
    }

    // ==================== Basic Lambda Tests ====================

    public void testLambdaMapDouble() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE numbers ARRAY = [1, 2, 3, 4, 5];
                DECLARE doubled ARRAY = ARRAY_MAP(numbers, (x) => x * 2);
                RETURN doubled;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertTrue(result instanceof List);
        List<?> list = (List<?>) result;
        assertEquals(5, list.size());
        assertEquals(2.0, ((Number) list.get(0)).doubleValue(), 0.001);
        assertEquals(4.0, ((Number) list.get(1)).doubleValue(), 0.001);
        assertEquals(6.0, ((Number) list.get(2)).doubleValue(), 0.001);
        assertEquals(8.0, ((Number) list.get(3)).doubleValue(), 0.001);
        assertEquals(10.0, ((Number) list.get(4)).doubleValue(), 0.001);
    }

    public void testLambdaMapSquare() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE numbers ARRAY = [1, 2, 3];
                DECLARE squared ARRAY = ARRAY_MAP(numbers, (n) => n * n);
                RETURN squared;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertTrue(result instanceof List);
        List<?> list = (List<?>) result;
        assertEquals(3, list.size());
        assertEquals(1.0, ((Number) list.get(0)).doubleValue(), 0.001);
        assertEquals(4.0, ((Number) list.get(1)).doubleValue(), 0.001);
        assertEquals(9.0, ((Number) list.get(2)).doubleValue(), 0.001);
    }

    public void testLambdaFilterEven() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE numbers ARRAY = [1, 2, 3, 4, 5, 6];
                DECLARE evens ARRAY = ARRAY_FILTER(numbers, (x) => x % 2 == 0);
                RETURN evens;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertTrue(result instanceof List);
        List<?> list = (List<?>) result;
        assertEquals(3, list.size());
        assertEquals(2.0, ((Number) list.get(0)).doubleValue(), 0.001);
        assertEquals(4.0, ((Number) list.get(1)).doubleValue(), 0.001);
        assertEquals(6.0, ((Number) list.get(2)).doubleValue(), 0.001);
    }

    public void testLambdaFilterGreaterThan() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE numbers ARRAY = [1, 5, 10, 15, 20];
                DECLARE large ARRAY = ARRAY_FILTER(numbers, (n) => n > 8);
                RETURN large;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertTrue(result instanceof List);
        List<?> list = (List<?>) result;
        assertEquals(3, list.size());
        assertEquals(10.0, ((Number) list.get(0)).doubleValue(), 0.001);
        assertEquals(15.0, ((Number) list.get(1)).doubleValue(), 0.001);
        assertEquals(20.0, ((Number) list.get(2)).doubleValue(), 0.001);
    }

    // ==================== Lambda with Property Access ====================

    public void testLambdaMapPropertyAccess() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE users ARRAY = [
                    {"name": "Alice", "age": 30},
                    {"name": "Bob", "age": 25}
                ];
                DECLARE names ARRAY = ARRAY_MAP(users, (u) => u['name']);
                RETURN names;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertTrue(result instanceof List);
        List<?> list = (List<?>) result;
        assertEquals(2, list.size());
        assertEquals("Alice", list.get(0));
        assertEquals("Bob", list.get(1));
    }

    public void testLambdaFilterByAge() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE users ARRAY = [
                    {"name": "Alice", "age": 30},
                    {"name": "Bob", "age": 17},
                    {"name": "Carol", "age": 25}
                ];
                DECLARE adults ARRAY = ARRAY_FILTER(users, (u) => u['age'] >= 18);
                RETURN ARRAY_LENGTH(adults);
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertEquals(2, ((Number) result).intValue());
    }

    // ==================== Combining Lambda Operations ====================

    public void testLambdaChain() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE numbers ARRAY = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
                -- Filter evens, then double them
                DECLARE evens ARRAY = ARRAY_FILTER(numbers, (x) => x % 2 == 0);
                DECLARE doubled ARRAY = ARRAY_MAP(evens, (x) => x * 2);
                RETURN doubled;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertTrue(result instanceof List);
        List<?> list = (List<?>) result;
        assertEquals(5, list.size());
        // Evens: 2, 4, 6, 8, 10 -> doubled: 4, 8, 12, 16, 20
        assertEquals(4.0, ((Number) list.get(0)).doubleValue(), 0.001);
        assertEquals(8.0, ((Number) list.get(1)).doubleValue(), 0.001);
        assertEquals(12.0, ((Number) list.get(2)).doubleValue(), 0.001);
        assertEquals(16.0, ((Number) list.get(3)).doubleValue(), 0.001);
        assertEquals(20.0, ((Number) list.get(4)).doubleValue(), 0.001);
    }

    // ==================== Edge Cases ====================

    public void testLambdaEmptyArray() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE empty ARRAY = [];
                DECLARE result ARRAY = ARRAY_MAP(empty, (x) => x * 2);
                RETURN ARRAY_LENGTH(result);
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertEquals(0, ((Number) result).intValue());
    }

    public void testLambdaSingleElement() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE single ARRAY = [5];
                DECLARE result ARRAY = ARRAY_MAP(single, (x) => x + 10);
                RETURN result;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertTrue(result instanceof List);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());
        assertEquals(15.0, ((Number) list.get(0)).doubleValue(), 0.001);
    }

    // ==================== Using Variables from Outer Scope ====================

    public void testLambdaWithOuterVariable() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE multiplier NUMBER = 10;
                DECLARE numbers ARRAY = [1, 2, 3];
                DECLARE scaled ARRAY = ARRAY_MAP(numbers, (x) => x * multiplier);
                RETURN scaled;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertTrue(result instanceof List);
        List<?> list = (List<?>) result;
        assertEquals(3, list.size());
        assertEquals(10.0, ((Number) list.get(0)).doubleValue(), 0.001);
        assertEquals(20.0, ((Number) list.get(1)).doubleValue(), 0.001);
        assertEquals(30.0, ((Number) list.get(2)).doubleValue(), 0.001);
    }
}



