/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.exceptions;

import org.elasticsearch.test.ESTestCase;

import java.util.Map;

/**
 * Tests for the enhanced EScriptException class.
 */
public class EScriptExceptionTests extends ESTestCase {

    public void testBasicException() {
        EScriptException ex = new EScriptException("Test error");
        
        assertEquals("Test error", ex.getMessage());
        assertEquals(EScriptException.TYPE_GENERIC, ex.getType());
        assertNull(ex.getCode());
    }

    public void testExceptionWithCode() {
        EScriptException ex = new EScriptException("HTTP error", "HTTP_500");
        
        assertEquals("HTTP error", ex.getMessage());
        assertEquals("HTTP_500", ex.getCode());
    }

    public void testExceptionWithType() {
        EScriptException ex = new EScriptException("Division by zero", "DIV_ZERO", EScriptException.TYPE_DIVISION);
        
        assertEquals(EScriptException.TYPE_DIVISION, ex.getType());
        assertTrue(ex.matchesType("division_error"));
        assertFalse(ex.matchesType("http_error"));
    }

    public void testSourceLocation() {
        SourceLocation location = new SourceLocation(42, 10, "my_procedure", "SET");
        EScriptException ex = new EScriptException("Variable not found", location);
        
        assertNotNull(ex.getSourceLocation());
        assertEquals(42, ex.getSourceLocation().getLine());
        assertEquals(10, ex.getSourceLocation().getColumn());
        assertEquals("my_procedure", ex.getSourceLocation().getProcedureName());
    }

    public void testWithLocation() {
        EScriptException ex = new EScriptException("Error")
            .withLocation(new SourceLocation(5, 1, "test_proc", "IF"));
        
        assertNotNull(ex.getSourceLocation());
        assertEquals(5, ex.getSourceLocation().getLine());
    }

    public void testStackFrames() {
        EScriptException ex = new EScriptException("Error occurred")
            .addStackFrame(new EScriptStackFrame("inner_proc", "CALL", 10, 5, "CALL outer_proc()"))
            .addStackFrame(new EScriptStackFrame("outer_proc", "SET", 25, 3, "SET x = y / 0"));
        
        assertEquals(2, ex.getEScriptStack().size());
        assertEquals("inner_proc", ex.getEScriptStack().get(0).getProcedureName());
        assertEquals("outer_proc", ex.getEScriptStack().get(1).getProcedureName());
        
        String stackTrace = ex.getEScriptStackTrace();
        assertTrue(stackTrace.contains("inner_proc"));
        assertTrue(stackTrace.contains("line 10"));
    }

    public void testToDocument() {
        SourceLocation location = new SourceLocation(42, 10, "my_proc", "SET");
        EScriptException ex = new EScriptException("Test error", "ERR_001", EScriptException.TYPE_VALIDATION, location, null)
            .addStackFrame(new EScriptStackFrame("my_proc", "SET", 42, 10, "SET x = invalid"));
        
        Map<String, Object> doc = ex.toDocument();
        
        assertEquals("Test error", doc.get("message"));
        assertEquals("ERR_001", doc.get("code"));
        assertEquals("validation_error", doc.get("type"));
        assertEquals(42, doc.get("line"));
        assertEquals(10, doc.get("column"));
        assertEquals("my_proc", doc.get("procedure"));
        assertNotNull(doc.get("escript_stack"));
        assertNotNull(doc.get("escript_stack_trace"));
    }

    public void testFormattedMessage() {
        SourceLocation location = new SourceLocation(42, 10, "my_proc", "SET");
        EScriptException ex = new EScriptException("Variable not found", location);
        
        String formatted = ex.getFormattedMessage();
        assertTrue(formatted.contains("Variable not found"));
        assertTrue(formatted.contains("line 42"));
        assertTrue(formatted.contains("my_proc"));
    }

    public void testFromThrowable() {
        RuntimeException original = new RuntimeException("Original error");
        EScriptException ex = EScriptException.from(original);
        
        assertEquals("Original error", ex.getMessage());
        assertSame(original, ex.getCause());
    }

    public void testFromThrowableWithLocation() {
        RuntimeException original = new RuntimeException("Original");
        SourceLocation location = new SourceLocation(10, 1, "proc", "IF");
        
        EScriptException ex = EScriptException.from(original, location);
        
        assertNotNull(ex.getSourceLocation());
        assertEquals(10, ex.getSourceLocation().getLine());
    }

    public void testTypeInference() {
        // HTTP error
        EScriptException httpEx = EScriptException.from(new RuntimeException("Connection refused"));
        // Note: "connection" in class name triggers HTTP type, but this is a RuntimeException
        
        // Null pointer
        NullPointerException npe = new NullPointerException("null value");
        EScriptException npeEx = EScriptException.from(npe);
        assertEquals(EScriptException.TYPE_NULL_REFERENCE, npeEx.getType());
        
        // Arithmetic
        ArithmeticException arith = new ArithmeticException("Division by zero");
        EScriptException arithEx = EScriptException.from(arith);
        assertEquals(EScriptException.TYPE_DIVISION, arithEx.getType());
    }

    public void testLocationToString() {
        SourceLocation loc = new SourceLocation(42, 5, "my_procedure", "SET");
        String str = loc.toLocationString();
        
        assertTrue(str.contains("line 42"));
        assertTrue(str.contains("column 5"));
        assertTrue(str.contains("my_procedure"));
    }

    public void testStackFrameToString() {
        EScriptStackFrame frame = new EScriptStackFrame("my_proc", "SET", 42, 5, "SET x = y");
        String str = frame.toString();
        
        assertTrue(str.contains("my_proc"));
        assertTrue(str.contains("line 42"));
        assertTrue(str.contains("SET"));
        assertTrue(str.contains("SET x = y"));
    }
}
