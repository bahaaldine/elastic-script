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
import org.elasticsearch.xpack.escript.parser.ElasticScriptLexer;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;

import org.elasticsearch.xpack.escript.primitives.ReturnValue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests for new language primitives: NOT, !, IS NULL, IS NOT NULL, CONTINUE
 */
public class LanguagePrimitivesTests extends ESTestCase {

    private ThreadPool threadPool;
    private ExecutionContext context;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        threadPool = new TestThreadPool("test");
        context = new ExecutionContext();
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
        // Unwrap ReturnValue if present
        if (rawResult instanceof ReturnValue returnValue) {
            return returnValue.getValue();
        }
        return rawResult;
    }

    public void testNotOperatorWithBoolean() throws Exception {
        String script = """
            PROCEDURE test_not()
            BEGIN
                DECLARE active BOOLEAN = true;
                IF NOT active THEN
                    RETURN 'inactive';
                ELSE
                    RETURN 'active';
                END IF
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("active", result);
    }

    public void testNotOperatorNegation() throws Exception {
        String script = """
            PROCEDURE test_not_negation()
            BEGIN
                DECLARE active BOOLEAN = false;
                IF NOT active THEN
                    RETURN 'was false';
                ELSE
                    RETURN 'was true';
                END IF
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("was false", result);
    }

    public void testBangOperator() throws Exception {
        String script = """
            PROCEDURE test_bang()
            BEGIN
                DECLARE active BOOLEAN = true;
                IF !active THEN
                    RETURN 'inactive';
                ELSE
                    RETURN 'active';
                END IF
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("active", result);
    }

    public void testNotWithTruthyValue() throws Exception {
        String script = """
            PROCEDURE test_truthy()
            BEGIN
                DECLARE count NUMBER = 5;
                IF NOT count THEN
                    RETURN 'falsy';
                ELSE
                    RETURN 'truthy';
                END IF
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("truthy", result);
    }

    public void testNotWithZero() throws Exception {
        String script = """
            PROCEDURE test_zero()
            BEGIN
                DECLARE count NUMBER = 0;
                IF NOT count THEN
                    RETURN 'zero is falsy';
                ELSE
                    RETURN 'zero is truthy';
                END IF
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("zero is falsy", result);
    }

    public void testIsNull() throws Exception {
        // Test IS NULL with null literal
        String script = """
            PROCEDURE test_is_null()
            BEGIN
                IF null IS NULL THEN
                    RETURN 'is null';
                ELSE
                    RETURN 'not null';
                END IF
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("is null", result);
    }

    public void testIsNotNull() throws Exception {
        String script = """
            PROCEDURE test_is_not_null()
            BEGIN
                DECLARE value STRING = 'hello';
                IF value IS NOT NULL THEN
                    RETURN 'has value';
                ELSE
                    RETURN 'null';
                END IF
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("has value", result);
    }

    public void testContinueStatement() throws Exception {
        String script = """
            PROCEDURE test_continue()
            BEGIN
                DECLARE sum NUMBER = 0;
                FOR i IN 1..10 LOOP
                    IF i == 5 THEN
                        CONTINUE;
                    END IF
                    SET sum = sum + i;
                END LOOP
                RETURN sum;
            END PROCEDURE
            """;
        
        // Sum of 1..10 = 55, minus 5 = 50
        Object result = executeScript(script);
        assertEquals(50.0, result);
    }

    public void testContinueInWhileLoop() throws Exception {
        String script = """
            PROCEDURE test_while_continue()
            BEGIN
                DECLARE i NUMBER = 0;
                DECLARE sum NUMBER = 0;
                WHILE i < 10 LOOP
                    SET i = i + 1;
                    IF i == 3 OR i == 7 THEN
                        CONTINUE;
                    END IF
                    SET sum = sum + i;
                END LOOP
                RETURN sum;
            END PROCEDURE
            """;
        
        // Sum of 1..10 = 55, minus 3 and 7 = 45
        Object result = executeScript(script);
        assertEquals(45.0, result);
    }

    public void testContinueInArrayLoop() throws Exception {
        String script = """
            PROCEDURE test_array_continue()
            BEGIN
                DECLARE items ARRAY = [1, 2, 3, 4, 5];
                DECLARE sum NUMBER = 0;
                FOR item IN items LOOP
                    IF item == 3 THEN
                        CONTINUE;
                    END IF
                    SET sum = sum + item;
                END LOOP
                RETURN sum;
            END PROCEDURE
            """;
        
        // Sum of 1,2,4,5 = 12 (skipping 3)
        Object result = executeScript(script);
        assertEquals(12.0, result);
    }
}

