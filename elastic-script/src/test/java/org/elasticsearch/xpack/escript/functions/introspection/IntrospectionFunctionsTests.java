/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.introspection;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.FunctionDefinition;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;
import org.elasticsearch.xpack.escript.functions.builtin.introspection.IntrospectionFunctions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Unit tests for IntrospectionFunctions.
 */
public class IntrospectionFunctionsTests extends ESTestCase {

    private ExecutionContext context;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new ExecutionContext();
        
        // Register a test function with parameters
        context.declareFunction("TEST_FUNCTION",
            List.of(
                new Parameter("input", "STRING", ParameterMode.IN),
                new Parameter("count", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("TEST_FUNCTION", (List<Object> args, ActionListener<Object> listener) -> {
                listener.onResponse("test_result");
            })
        );
        
        // Register introspection functions
        IntrospectionFunctions.registerAll(context);
    }

    public void testEscriptFunctionsRegistered() {
        assertTrue("ESCRIPT_FUNCTIONS should be registered", context.hasFunction("ESCRIPT_FUNCTIONS"));
        assertTrue("ESCRIPT_FUNCTION should be registered", context.hasFunction("ESCRIPT_FUNCTION"));
        assertTrue("ESCRIPT_VARIABLES should be registered", context.hasFunction("ESCRIPT_VARIABLES"));
    }

    public void testEscriptFunctionsReturnsAllFunctions() throws Exception {
        FunctionDefinition func = context.getFunction("ESCRIPT_FUNCTIONS");
        assertNotNull(func);
        
        AtomicReference<Object> resultRef = new AtomicReference<>();
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        func.execute(List.of(), new ActionListener<>() {
            @Override
            public void onResponse(Object result) {
                resultRef.set(result);
                latch.countDown();
            }
            
            @Override
            public void onFailure(Exception e) {
                errorRef.set(e);
                latch.countDown();
            }
        });
        
        assertTrue("Should complete within timeout", latch.await(5, TimeUnit.SECONDS));
        assertNull("Should not have error", errorRef.get());
        
        Object result = resultRef.get();
        assertNotNull("Should have result", result);
        assertTrue("Result should be a list", result instanceof List);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> functions = (List<Map<String, Object>>) result;
        assertTrue("Should have at least 4 functions", functions.size() >= 4);
        
        // Find TEST_FUNCTION in the result
        Map<String, Object> testFunc = functions.stream()
            .filter(f -> "TEST_FUNCTION".equals(f.get("name")))
            .findFirst()
            .orElse(null);
        
        assertNotNull("TEST_FUNCTION should be in the list", testFunc);
        assertEquals("Should be a built-in function", true, testFunc.get("is_builtin"));
        assertEquals("Should have 2 parameters", 2, testFunc.get("parameter_count"));
    }

    public void testEscriptFunctionReturnsDetails() throws Exception {
        FunctionDefinition func = context.getFunction("ESCRIPT_FUNCTION");
        assertNotNull(func);
        
        AtomicReference<Object> resultRef = new AtomicReference<>();
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        func.execute(List.of("TEST_FUNCTION"), new ActionListener<>() {
            @Override
            public void onResponse(Object result) {
                resultRef.set(result);
                latch.countDown();
            }
            
            @Override
            public void onFailure(Exception e) {
                errorRef.set(e);
                latch.countDown();
            }
        });
        
        assertTrue("Should complete within timeout", latch.await(5, TimeUnit.SECONDS));
        assertNull("Should not have error", errorRef.get());
        
        Object result = resultRef.get();
        assertNotNull("Should have result", result);
        assertTrue("Result should be a map", result instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> funcInfo = (Map<String, Object>) result;
        
        assertEquals("Name should match", "TEST_FUNCTION", funcInfo.get("name"));
        assertEquals("Should exist", true, funcInfo.get("exists"));
        assertEquals("Should be built-in", true, funcInfo.get("is_builtin"));
        assertEquals("Should have 2 parameters", 2, funcInfo.get("parameter_count"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> params = (List<Map<String, Object>>) funcInfo.get("parameters");
        assertEquals("Should have 2 parameters", 2, params.size());
        
        assertEquals("First param name", "input", params.get(0).get("name"));
        assertEquals("First param type", "STRING", params.get(0).get("type"));
        
        assertEquals("Second param name", "count", params.get(1).get("name"));
        assertEquals("Second param type", "NUMBER", params.get(1).get("type"));
        
        String signature = (String) funcInfo.get("signature");
        assertEquals("Signature should match", "TEST_FUNCTION(input STRING, count NUMBER)", signature);
    }

    public void testEscriptFunctionNonExistent() throws Exception {
        FunctionDefinition func = context.getFunction("ESCRIPT_FUNCTION");
        
        AtomicReference<Object> resultRef = new AtomicReference<>();
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        func.execute(List.of("NON_EXISTENT_FUNCTION"), new ActionListener<>() {
            @Override
            public void onResponse(Object result) {
                resultRef.set(result);
                latch.countDown();
            }
            
            @Override
            public void onFailure(Exception e) {
                errorRef.set(e);
                latch.countDown();
            }
        });
        
        assertTrue("Should complete within timeout", latch.await(5, TimeUnit.SECONDS));
        assertNull("Should not have error", errorRef.get());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) resultRef.get();
        
        assertEquals("Name should match", "NON_EXISTENT_FUNCTION", result.get("name"));
        assertEquals("Should not exist", false, result.get("exists"));
    }

    public void testEscriptVariables() throws Exception {
        // Declare some test variables
        context.declareVariable("test_string", "STRING");
        context.setVariable("test_string", "hello");
        
        context.declareVariable("test_number", "NUMBER");
        context.setVariable("test_number", 42);
        
        context.declareVariable("test_array", "ARRAY");
        context.setVariable("test_array", List.of(1, 2, 3));
        
        FunctionDefinition func = context.getFunction("ESCRIPT_VARIABLES");
        
        AtomicReference<Object> resultRef = new AtomicReference<>();
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        func.execute(List.of(), new ActionListener<>() {
            @Override
            public void onResponse(Object result) {
                resultRef.set(result);
                latch.countDown();
            }
            
            @Override
            public void onFailure(Exception e) {
                errorRef.set(e);
                latch.countDown();
            }
        });
        
        assertTrue("Should complete within timeout", latch.await(5, TimeUnit.SECONDS));
        assertNull("Should not have error", errorRef.get());
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> variables = (List<Map<String, Object>>) resultRef.get();
        
        assertEquals("Should have 3 variables", 3, variables.size());
        
        // Find test_string
        Map<String, Object> strVar = variables.stream()
            .filter(v -> "test_string".equals(v.get("name")))
            .findFirst()
            .orElse(null);
        
        assertNotNull("test_string should be in list", strVar);
        assertEquals("Type should be STRING", "STRING", strVar.get("type"));
        assertEquals("Value should be hello", "hello", strVar.get("value"));
    }

    public void testEscriptFunctionMissingArgument() throws Exception {
        FunctionDefinition func = context.getFunction("ESCRIPT_FUNCTION");
        
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        func.execute(List.of(), new ActionListener<>() {
            @Override
            public void onResponse(Object result) {
                latch.countDown();
            }
            
            @Override
            public void onFailure(Exception e) {
                errorRef.set(e);
                latch.countDown();
            }
        });
        
        assertTrue("Should complete within timeout", latch.await(5, TimeUnit.SECONDS));
        assertNotNull("Should have error for missing argument", errorRef.get());
        assertTrue("Should be IllegalArgumentException", errorRef.get() instanceof IllegalArgumentException);
    }

    public void testFunctionsSortedByName() throws Exception {
        // Register functions with names that would be out of alphabetical order
        context.declareFunction("ZEBRA_FUNCTION",
            List.of(),
            new BuiltInFunctionDefinition("ZEBRA_FUNCTION", (args, listener) -> listener.onResponse(null))
        );
        context.declareFunction("ALPHA_FUNCTION",
            List.of(),
            new BuiltInFunctionDefinition("ALPHA_FUNCTION", (args, listener) -> listener.onResponse(null))
        );
        
        FunctionDefinition func = context.getFunction("ESCRIPT_FUNCTIONS");
        
        AtomicReference<Object> resultRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        func.execute(List.of(), new ActionListener<>() {
            @Override
            public void onResponse(Object result) {
                resultRef.set(result);
                latch.countDown();
            }
            
            @Override
            public void onFailure(Exception e) {
                latch.countDown();
            }
        });
        
        assertTrue("Should complete within timeout", latch.await(5, TimeUnit.SECONDS));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> functions = (List<Map<String, Object>>) resultRef.get();
        
        // Verify sorted order
        String prevName = "";
        for (Map<String, Object> f : functions) {
            String name = (String) f.get("name");
            assertTrue("Functions should be sorted: " + prevName + " < " + name, 
                prevName.compareTo(name) <= 0);
            prevName = name;
        }
    }
}

