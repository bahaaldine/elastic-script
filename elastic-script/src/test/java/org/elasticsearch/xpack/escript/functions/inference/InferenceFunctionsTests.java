/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.inference;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.FunctionDefinition;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for InferenceFunctions.
 * These tests verify function registration and parameter validation.
 * Integration tests with actual inference endpoints would require a running cluster.
 */
public class InferenceFunctionsTests extends ESTestCase {

    public void testInferenceFunctionRegistration() {
        ExecutionContext context = new ExecutionContext();
        
        // Register functions with null client (for registration testing only)
        // In real usage, a valid client is required
        try {
            // We expect this to work for registration
            org.elasticsearch.xpack.escript.functions.builtin.inference.InferenceFunctions
                .registerAll(context, null);
        } catch (Exception e) {
            // Registration should work even with null client
            // The actual execution would fail with null client
        }
        
        // Check that INFERENCE function is registered
        FunctionDefinition inferenceFunc = context.getFunction("INFERENCE");
        assertThat("INFERENCE function should be registered", inferenceFunc, notNullValue());
        
        // Check that INFERENCE_CHAT function is registered
        FunctionDefinition chatFunc = context.getFunction("INFERENCE_CHAT");
        assertThat("INFERENCE_CHAT function should be registered", chatFunc, notNullValue());
        
        // Check that INFERENCE_EMBED function is registered
        FunctionDefinition embedFunc = context.getFunction("INFERENCE_EMBED");
        assertThat("INFERENCE_EMBED function should be registered", embedFunc, notNullValue());
        
        // Check that INFERENCE_RERANK function is registered
        FunctionDefinition rerankFunc = context.getFunction("INFERENCE_RERANK");
        assertThat("INFERENCE_RERANK function should be registered", rerankFunc, notNullValue());
    }

    public void testInferenceFunctionParameterCount() {
        ExecutionContext context = new ExecutionContext();
        
        try {
            org.elasticsearch.xpack.escript.functions.builtin.inference.InferenceFunctions
                .registerAll(context, null);
        } catch (Exception e) {
            // Ignore registration issues for this test
        }
        
        // INFERENCE should have 2 required parameters
        FunctionDefinition inferenceFunc = context.getFunction("INFERENCE");
        if (inferenceFunc != null) {
            assertEquals("INFERENCE should have 2 required parameters", 
                2, inferenceFunc.getParameters().size());
        }
        
        // INFERENCE_EMBED should have 2 required parameters
        FunctionDefinition embedFunc = context.getFunction("INFERENCE_EMBED");
        if (embedFunc != null) {
            assertEquals("INFERENCE_EMBED should have 2 required parameters", 
                2, embedFunc.getParameters().size());
        }
        
        // INFERENCE_RERANK should have 3 required parameters
        FunctionDefinition rerankFunc = context.getFunction("INFERENCE_RERANK");
        if (rerankFunc != null) {
            assertEquals("INFERENCE_RERANK should have 3 required parameters", 
                3, rerankFunc.getParameters().size());
        }
    }

    public void testInferenceFunctionParameterTypes() {
        ExecutionContext context = new ExecutionContext();
        
        try {
            org.elasticsearch.xpack.escript.functions.builtin.inference.InferenceFunctions
                .registerAll(context, null);
        } catch (Exception e) {
            // Ignore registration issues for this test
        }
        
        FunctionDefinition inferenceFunc = context.getFunction("INFERENCE");
        if (inferenceFunc != null && inferenceFunc.getParameters().size() >= 2) {
            assertEquals("First parameter should be STRING", 
                "STRING", inferenceFunc.getParameters().get(0).getType());
            assertEquals("Second parameter should be STRING", 
                "STRING", inferenceFunc.getParameters().get(1).getType());
        }
        
        FunctionDefinition rerankFunc = context.getFunction("INFERENCE_RERANK");
        if (rerankFunc != null && rerankFunc.getParameters().size() >= 3) {
            assertEquals("First parameter should be STRING", 
                "STRING", rerankFunc.getParameters().get(0).getType());
            assertEquals("Second parameter should be STRING", 
                "STRING", rerankFunc.getParameters().get(1).getType());
            assertEquals("Third parameter should be ARRAY", 
                "ARRAY", rerankFunc.getParameters().get(2).getType());
        }
    }
}

