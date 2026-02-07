/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V.
 * under one or more contributor license agreements. Licensed under
 * the Elastic License 2.0; you may not use this file except in compliance
 * with the Elastic License 2.0.
 */

package org.elasticsearch.xpack.escript.primitives;

import org.elasticsearch.test.ESTestCase;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link ExecutionResult}.
 */
public class ExecutionResultTests extends ESTestCase {

    public void testBasicResult() {
        ExecutionResult result = new ExecutionResult("hello", List.of(), "abc123", 100, "my_proc");
        
        assertEquals("hello", result.getResult());
        assertEquals("abc123", result.getExecutionId());
        assertEquals(100, result.getDurationMs());
        assertEquals("my_proc", result.getProcedureName());
        assertFalse(result.hasPrintOutput());
    }

    public void testWithPrintOutput() {
        List<String> output = Arrays.asList("line 1", "line 2", "line 3");
        ExecutionResult result = new ExecutionResult("result", output, "def456", 50, "test_proc");
        
        assertTrue(result.hasPrintOutput());
        assertEquals(3, result.getPrintOutput().size());
        assertEquals("line 1", result.getPrintOutput().get(0));
        assertEquals("line 2", result.getPrintOutput().get(1));
        assertEquals("line 3", result.getPrintOutput().get(2));
    }

    public void testNullResult() {
        ExecutionResult result = new ExecutionResult(null, List.of("printed"), "ghi789", 10, null);
        
        assertNull(result.getResult());
        assertTrue(result.hasPrintOutput());
        assertNull(result.getProcedureName());
    }

    public void testReturnValueUnwrapping() {
        ReturnValue wrapped = new ReturnValue(42);
        ExecutionResult result = new ExecutionResult(wrapped, List.of(), "xyz", 5, "proc");
        
        // getResult should unwrap ReturnValue
        assertEquals(42, result.getResult());
        // getRawResult should return the original
        assertTrue(result.getRawResult() instanceof ReturnValue);
    }

    public void testOfFactory() {
        ExecutionResult result = ExecutionResult.of("simple");
        
        assertEquals("simple", result.getResult());
        assertFalse(result.hasPrintOutput());
        assertNull(result.getExecutionId());
        assertEquals(0, result.getDurationMs());
    }

    public void testToMap() {
        List<String> output = Arrays.asList("output1");
        ExecutionResult result = new ExecutionResult("data", output, "exec123", 25, "my_procedure");
        
        Map<String, Object> map = result.toMap();
        
        assertEquals("data", map.get("result"));
        assertTrue(map.containsKey("output"));
        assertEquals(output, map.get("output"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> meta = (Map<String, Object>) map.get("_meta");
        assertEquals("exec123", meta.get("execution_id"));
        assertEquals(25L, meta.get("duration_ms"));
        assertEquals("my_procedure", meta.get("procedure_name"));
    }

    public void testToMapWithoutOutput() {
        ExecutionResult result = new ExecutionResult(123, List.of(), "id", 10, null);
        
        Map<String, Object> map = result.toMap();
        
        assertEquals(123, map.get("result"));
        assertFalse(map.containsKey("output"));  // No output field when empty
    }

    public void testPrintOutputIsImmutable() {
        List<String> output = Arrays.asList("line1");
        ExecutionResult result = new ExecutionResult("test", output, "id", 0, null);
        
        expectThrows(UnsupportedOperationException.class, () -> {
            result.getPrintOutput().add("new line");
        });
    }
}
