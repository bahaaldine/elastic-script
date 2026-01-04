/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions;
/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License 2.0.
 */

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.StringBuiltInFunctions;
import org.elasticsearch.xpack.escript.handlers.FunctionDefinitionHandler;
import org.elasticsearch.xpack.escript.parser.ElasticScriptLexer;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

public class StringBuiltInFunctionsTests extends ESTestCase {

    private ExecutionContext context;
    private ProcedureExecutor dummyExecutor;
    private FunctionDefinitionHandler handler;
    private ThreadPool threadPool;
    private ProcedureExecutor executor;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new ExecutionContext();
        threadPool = new TestThreadPool("array-test-pool");
        Client mockClient = null; // or a proper mock
        // Create a lexer with an empty source as a placeholder (adjust if needed)
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(""));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        executor = new ProcedureExecutor(context, threadPool, mockClient, tokens);
        // Initialize the FunctionDefinitionHandler as done in other test classes
        handler = new FunctionDefinitionHandler(executor);
        // Register the array built-in functions so that they are available for testing.
        StringBuiltInFunctions.registerAll(context);
    }

    @Override
    public void tearDown() throws Exception {
        // Properly terminate the thread pool to avoid thread leaks.
        terminate(threadPool);
        super.tearDown();
    }

    @Test
    public void testLength() throws Exception {
        BuiltInFunctionDefinition lengthFn = context.getBuiltInFunction("LENGTH");
        CountDownLatch latch = new CountDownLatch(1);
        lengthFn.execute(Arrays.asList("hello world"), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                // "hello world" is 11 characters.
                assertEquals(11, result);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail(e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testSubstr() throws Exception {
        BuiltInFunctionDefinition substrFn = context.getBuiltInFunction("SUBSTR");
        CountDownLatch latch = new CountDownLatch(1);
        substrFn.execute(Arrays.asList("hello world", 7, 5), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                // Expected substring is "world"
                assertEquals("world", result);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail(e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testUpper() throws Exception {
        BuiltInFunctionDefinition upperFn = context.getBuiltInFunction("UPPER");
        CountDownLatch latch = new CountDownLatch(1);
        upperFn.execute(Arrays.asList("hello"), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertEquals("HELLO", result);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail(e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testLower() throws Exception {
        BuiltInFunctionDefinition lowerFn = context.getBuiltInFunction("LOWER");
        CountDownLatch latch = new CountDownLatch(1);
        lowerFn.execute(Arrays.asList("HELLO"), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertEquals("hello", result);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail(e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testTrim() throws Exception {
        BuiltInFunctionDefinition trimFn = context.getBuiltInFunction("TRIM");
        CountDownLatch latch = new CountDownLatch(1);
        trimFn.execute(Arrays.asList("  hello  "), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertEquals("hello", result);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail(e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testLTrim() throws Exception {
        BuiltInFunctionDefinition ltrimFn = context.getBuiltInFunction("LTRIM");
        CountDownLatch latch = new CountDownLatch(1);
        ltrimFn.execute(Arrays.asList("   hello"), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertEquals("hello", result);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail(e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testRTrim() throws Exception {
        BuiltInFunctionDefinition rtrimFn = context.getBuiltInFunction("RTRIM");
        CountDownLatch latch = new CountDownLatch(1);
        rtrimFn.execute(Arrays.asList("hello   "), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertEquals("hello", result);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail(e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testReplace() throws Exception {
        BuiltInFunctionDefinition replaceFn = context.getBuiltInFunction("REPLACE");
        CountDownLatch latch = new CountDownLatch(1);
        replaceFn.execute(Arrays.asList("hello world", "world", "Elastic Script"), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertEquals("hello Elastic Script", result);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail(e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testInstr() throws Exception {
        BuiltInFunctionDefinition instrFn = context.getBuiltInFunction("INSTR");
        CountDownLatch latch = new CountDownLatch(1);
        instrFn.execute(Arrays.asList("hello world", "world"), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                // Expecting 7 (1-indexed)
                assertEquals(7, result);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail(e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testLPad() throws Exception {
        BuiltInFunctionDefinition lpadFn = context.getBuiltInFunction("LPAD");
        CountDownLatch latch = new CountDownLatch(1);
        lpadFn.execute(Arrays.asList("hello", 10, "*"), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertEquals("*****hello", result);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail(e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testRPad() throws Exception {
        BuiltInFunctionDefinition rpadFn = context.getBuiltInFunction("RPAD");
        CountDownLatch latch = new CountDownLatch(1);
        rpadFn.execute(Arrays.asList("hello", 10, "*"), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertEquals("hello*****", result);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail(e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testSplit() throws Exception {
        BuiltInFunctionDefinition splitFn = context.getBuiltInFunction("SPLIT");
        CountDownLatch latch = new CountDownLatch(1);
        splitFn.execute(Arrays.asList("a,b,c", ","), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertEquals(Arrays.asList("a", "b", "c"), result);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail(e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testConcat() throws Exception {
        BuiltInFunctionDefinition concatFn = context.getBuiltInFunction("||");
        CountDownLatch latch = new CountDownLatch(1);
        concatFn.execute(Arrays.asList("hello ", "world"), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertEquals("hello world", result);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail(e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testRegexpReplace() throws Exception {
        BuiltInFunctionDefinition regexpReplaceFn = context.getBuiltInFunction("REGEXP_REPLACE");
        CountDownLatch latch = new CountDownLatch(1);
        regexpReplaceFn.execute(Arrays.asList("abc123def", "[0-9]+", ""), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertEquals("abcdef", result);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail(e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testRegexpSubstr() throws Exception {
        BuiltInFunctionDefinition regexpSubstrFn = context.getBuiltInFunction("REGEXP_SUBSTR");
        CountDownLatch latch = new CountDownLatch(1);
        regexpSubstrFn.execute(Arrays.asList("abc123def", "[0-9]+"), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertEquals("123", result);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail(e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testReverse() throws Exception {
        BuiltInFunctionDefinition reverseFn = context.getBuiltInFunction("REVERSE");
        CountDownLatch latch = new CountDownLatch(1);
        reverseFn.execute(Arrays.asList("hello"), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertEquals("olleh", result);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail(e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testInitCap() throws Exception {
        BuiltInFunctionDefinition initcapFn = context.getBuiltInFunction("INITCAP");
        CountDownLatch latch = new CountDownLatch(1);
        initcapFn.execute(Arrays.asList("hello world"), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertEquals("Hello World", result);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail(e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testLengthWithString() throws Exception {
        BuiltInFunctionDefinition lengthFn = context.getBuiltInFunction("LENGTH");
        CountDownLatch latch = new CountDownLatch(1);
        lengthFn.execute(Arrays.asList("Elasticsearch"), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertEquals(13, result);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail(e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testLengthWithArray() throws Exception {
        BuiltInFunctionDefinition lengthFn = context.getBuiltInFunction("LENGTH");
        CountDownLatch latch = new CountDownLatch(1);
        java.util.List<String> testArray = Arrays.asList("a", "b", "c", "d", "e");
        lengthFn.execute(Arrays.asList(testArray), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertEquals(5, result);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail(e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testLengthWithEmptyArray() throws Exception {
        BuiltInFunctionDefinition lengthFn = context.getBuiltInFunction("LENGTH");
        CountDownLatch latch = new CountDownLatch(1);
        java.util.List<String> emptyArray = java.util.Collections.emptyList();
        lengthFn.execute(Arrays.asList(emptyArray), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertEquals(0, result);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail(e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testEnvFunctionRegistered() {
        FunctionDefinition envFn = context.getFunction("ENV");
        assertNotNull("ENV function should be registered", envFn);
        assertEquals("ENV should have 1 required parameter", 1, envFn.getParameters().size());
        assertEquals("Parameter should be of type STRING", "STRING", envFn.getParameters().get(0).getType());
    }

    @Test
    public void testEnvWithDefaultValue() throws Exception {
        BuiltInFunctionDefinition envFn = context.getBuiltInFunction("ENV");
        CountDownLatch latch = new CountDownLatch(1);
        // Test with a non-existent env var and a default value
        envFn.execute(Arrays.asList("NONEXISTENT_VAR_12345", "default_value"), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertEquals("default_value", result);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail(e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testEnvWithExistingVar() throws Exception {
        BuiltInFunctionDefinition envFn = context.getBuiltInFunction("ENV");
        CountDownLatch latch = new CountDownLatch(1);
        // PATH should exist on most systems
        envFn.execute(Arrays.asList("PATH"), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertNotNull("PATH env var should not be null", result);
                assertTrue("PATH should contain something", result.toString().length() > 0);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail(e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }
}
