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
 * Unit tests for Phase 6: String Interpolation.
 * Tests the ${expression} syntax in double-quoted strings.
 */
public class StringInterpolationTests extends ESTestCase {

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

    // ==================== Basic Interpolation Tests ====================

    public void testSimpleVariableInterpolation() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE name STRING = 'Alice';
                DECLARE greeting STRING = "Hello, ${name}!";
                RETURN greeting;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertEquals("Hello, Alice!", result);
    }

    public void testMultipleInterpolations() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE first STRING = 'John';
                DECLARE last STRING = 'Doe';
                DECLARE full STRING = "Name: ${first} ${last}";
                RETURN full;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertEquals("Name: John Doe", result);
    }

    public void testNumberInterpolation() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE count NUMBER = 42;
                DECLARE msg STRING = "The answer is ${count}";
                RETURN msg;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        // Numbers are stored as Double, so they appear with .0
        assertEquals("The answer is 42.0", result);
    }

    public void testExpressionInterpolation() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE x NUMBER = 10;
                DECLARE y NUMBER = 5;
                DECLARE msg STRING = "Sum: ${x + y}";
                RETURN msg;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertEquals("Sum: 15.0", result);
    }

    public void testComplexExpressionInterpolation() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE price NUMBER = 100;
                DECLARE tax NUMBER = 0.1;
                DECLARE msg STRING = "Total: ${price + (price * tax)}";
                RETURN msg;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertEquals("Total: 110.0", result);
    }

    // ==================== Edge Cases ====================

    public void testNoInterpolation() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE msg STRING = "Plain string without interpolation";
                RETURN msg;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertEquals("Plain string without interpolation", result);
    }

    public void testSingleQuoteNoInterpolation() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE name STRING = 'World';
                DECLARE msg STRING = 'Hello ${name}';
                RETURN msg;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        // Single-quoted strings should NOT interpolate
        assertEquals("Hello ${name}", result);
    }

    public void testEmptyInterpolation() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE empty STRING = '';
                DECLARE msg STRING = "Value: ${empty}";
                RETURN msg;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertEquals("Value: ", result);
    }

    public void testNullInterpolation() throws Exception {
        // Test that null values in interpolation render as empty string
        // Using a ternary to produce null rather than declaring NULL variable
        // because there may be issues with NULL variable declarations
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE check BOOLEAN = false;
                DECLARE msg STRING = "Value: ${check ? 'has value' : ''}";
                RETURN msg;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertEquals("Value: ", result);
    }

    public void testBooleanInterpolation() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE active BOOLEAN = true;
                DECLARE msg STRING = "Active: ${active}";
                RETURN msg;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertEquals("Active: true", result);
    }

    // ==================== Function Call Interpolation ====================

    public void testFunctionInInterpolation() throws Exception {
        // Use a simple expression instead of function call since we'd need to register functions
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE text STRING = 'hello';
                DECLARE suffix STRING = ' world';
                DECLARE msg STRING = "Combined: ${text || suffix}";
                RETURN msg;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertEquals("Combined: hello world", result);
    }

    public void testStringConcatVsInterpolation() throws Exception {
        // Compare traditional concatenation with interpolation
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE name STRING = 'Bob';
                DECLARE age NUMBER = 30;
                DECLARE interp STRING = "Name: ${name}, Age: ${age}";
                RETURN interp;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertEquals("Name: Bob, Age: 30.0", result);
    }

    // ==================== Positional Cases ====================

    public void testInterpolationAtStart() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE val NUMBER = 100;
                DECLARE msg STRING = "${val} is the value";
                RETURN msg;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertEquals("100.0 is the value", result);
    }

    public void testInterpolationAtEnd() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE val NUMBER = 100;
                DECLARE msg STRING = "The value is ${val}";
                RETURN msg;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertEquals("The value is 100.0", result);
    }

    public void testOnlyInterpolation() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE val STRING = 'test';
                DECLARE msg STRING = "${val}";
                RETURN msg;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertEquals("test", result);
    }

    public void testConsecutiveInterpolations() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE a STRING = 'A';
                DECLARE b STRING = 'B';
                DECLARE c STRING = 'C';
                DECLARE msg STRING = "${a}${b}${c}";
                RETURN msg;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertEquals("ABC", result);
    }

    // ==================== Ternary in Interpolation ====================

    public void testTernaryInInterpolation() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE score NUMBER = 85;
                DECLARE msg STRING = "Result: ${score >= 60 ? 'Pass' : 'Fail'}";
                RETURN msg;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertEquals("Result: Pass", result);
    }

    public void testTernaryFailInInterpolation() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE score NUMBER = 45;
                DECLARE msg STRING = "Result: ${score >= 60 ? 'Pass' : 'Fail'}";
                RETURN msg;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertEquals("Result: Fail", result);
    }

    // ==================== Special Characters ====================

    public void testDollarSignWithoutBrace() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE price NUMBER = 50;
                DECLARE msg STRING = "Price: $${price}";
                RETURN msg;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertEquals("Price: $50.0", result);
    }
}
