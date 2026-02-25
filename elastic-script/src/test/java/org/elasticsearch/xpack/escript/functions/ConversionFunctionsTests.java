/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.ConversionFunctions;
import org.elasticsearch.action.ActionListener;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ConversionFunctionsTests extends ESTestCase {

    private ExecutionContext context;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new ExecutionContext();
        ConversionFunctions.registerAll(context);
    }

    // ============================================
    // TO_STRING Tests
    // ============================================

    public void testToStringFromNumber() throws Exception {
        Object result = invokeFunction("TO_STRING", 123);
        assertEquals("123", result);
    }

    public void testToStringFromFloat() throws Exception {
        Object result = invokeFunction("TO_STRING", 3.14);
        assertEquals("3.14", result);
    }

    public void testToStringFromBoolean() throws Exception {
        Object result = invokeFunction("TO_STRING", true);
        assertEquals("true", result);
    }

    public void testToStringFromNull() throws Exception {
        Object result = invokeFunction("TO_STRING", (Object) null);
        assertEquals("null", result);
    }

    public void testToStringFromString() throws Exception {
        Object result = invokeFunction("TO_STRING", "hello");
        assertEquals("hello", result);
    }

    // ============================================
    // TO_NUMBER Tests
    // ============================================

    public void testToNumberFromString() throws Exception {
        Object result = invokeFunction("TO_NUMBER", "42.5");
        assertEquals(42.5, result);
    }

    public void testToNumberFromInteger() throws Exception {
        Object result = invokeFunction("TO_NUMBER", 42);
        assertEquals(42.0, result);
    }

    public void testToNumberFromBoolean() throws Exception {
        Object result = invokeFunction("TO_NUMBER", true);
        assertEquals(1.0, result);
        
        Object resultFalse = invokeFunction("TO_NUMBER", false);
        assertEquals(0.0, resultFalse);
    }

    // ============================================
    // TO_INT Tests
    // ============================================

    public void testToIntFromString() throws Exception {
        Object result = invokeFunction("TO_INT", "42");
        assertEquals(42, result);
    }

    public void testToIntFromFloat() throws Exception {
        Object result = invokeFunction("TO_INT", 42.9);
        assertEquals(42, result);
    }

    public void testToIntFromStringWithDecimal() throws Exception {
        Object result = invokeFunction("TO_INT", "42.9");
        assertEquals(42, result);
    }

    public void testToIntFromBoolean() throws Exception {
        Object result = invokeFunction("TO_INT", true);
        assertEquals(1, result);
        
        Object resultFalse = invokeFunction("TO_INT", false);
        assertEquals(0, resultFalse);
    }

    // ============================================
    // TO_FLOAT Tests
    // ============================================

    public void testToFloatFromString() throws Exception {
        Object result = invokeFunction("TO_FLOAT", "3.14");
        assertEquals(3.14, result);
    }

    public void testToFloatFromInteger() throws Exception {
        Object result = invokeFunction("TO_FLOAT", 42);
        assertEquals(42.0, result);
    }

    // ============================================
    // TO_BOOLEAN Tests
    // ============================================

    public void testToBooleanFromString() throws Exception {
        assertEquals(true, invokeFunction("TO_BOOLEAN", "true"));
        assertEquals(true, invokeFunction("TO_BOOLEAN", "TRUE"));
        assertEquals(true, invokeFunction("TO_BOOLEAN", "yes"));
        assertEquals(true, invokeFunction("TO_BOOLEAN", "1"));
        assertEquals(true, invokeFunction("TO_BOOLEAN", "on"));
        assertEquals(false, invokeFunction("TO_BOOLEAN", "false"));
        assertEquals(false, invokeFunction("TO_BOOLEAN", "no"));
        assertEquals(false, invokeFunction("TO_BOOLEAN", "0"));
    }

    public void testToBooleanFromNumber() throws Exception {
        assertEquals(true, invokeFunction("TO_BOOLEAN", 1));
        assertEquals(true, invokeFunction("TO_BOOLEAN", 42));
        assertEquals(true, invokeFunction("TO_BOOLEAN", -1));
        assertEquals(false, invokeFunction("TO_BOOLEAN", 0));
    }

    public void testToBooleanFromBoolean() throws Exception {
        assertEquals(true, invokeFunction("TO_BOOLEAN", true));
        assertEquals(false, invokeFunction("TO_BOOLEAN", false));
    }

    // ============================================
    // CAST Tests
    // ============================================

    public void testCastToString() throws Exception {
        Object result = invokeFunction("CAST", 123, "STRING");
        assertEquals("123", result);
    }

    public void testCastToNumber() throws Exception {
        Object result = invokeFunction("CAST", "42.5", "NUMBER");
        assertEquals(42.5, result);
    }

    public void testCastToInt() throws Exception {
        Object result = invokeFunction("CAST", "42", "INT");
        assertEquals(42, result);
    }

    public void testCastToBoolean() throws Exception {
        Object result = invokeFunction("CAST", "true", "BOOLEAN");
        assertEquals(true, result);
    }

    public void testCastNullReturnsNull() throws Exception {
        Object result = invokeFunction("CAST", null, "STRING");
        assertNull(result);
    }

    public void testCastInvalidTypeThrows() throws Exception {
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        FunctionDefinition fn = context.getFunction("CAST");
        fn.execute(Arrays.asList("value", "INVALID_TYPE"), new ActionListener<Object>() {
            @Override
            public void onResponse(Object response) {
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                error.set(e);
                latch.countDown();
            }
        });
        
        assertTrue("Timed out waiting for function", latch.await(5, TimeUnit.SECONDS));
        assertNotNull("Expected an error for invalid type", error.get());
        assertTrue("Error message should mention unknown type", 
            error.get().getMessage().contains("Unknown target type"));
    }

    // ============================================
    // Helper Methods
    // ============================================

    private Object invokeFunction(String name, Object... args) throws Exception {
        FunctionDefinition fn = context.getFunction(name);
        assertNotNull("Function " + name + " should be registered", fn);
        
        AtomicReference<Object> result = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        fn.execute(Arrays.asList(args), new ActionListener<Object>() {
            @Override
            public void onResponse(Object response) {
                result.set(response);
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                error.set(e);
                latch.countDown();
            }
        });
        
        assertTrue("Timed out waiting for function " + name, latch.await(5, TimeUnit.SECONDS));
        if (error.get() != null) {
            throw error.get();
        }
        return result.get();
    }
}
