/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.execution;

import org.elasticsearch.test.ESTestCase;

import java.util.List;

/**
 * Tests for continuation bindings (@result, @results, @error).
 * These tests ensure that special variables are properly recognized
 * in continuation handlers.
 */
public class ContinuationBindingsTests extends ESTestCase {

    public void testContinuationTypeOnDone() {
        Continuation continuation = new Continuation(Continuation.Type.ON_DONE, "handler", List.of("@result"));
        
        assertEquals(Continuation.Type.ON_DONE, continuation.getType());
        assertEquals("handler", continuation.getHandlerName());
        assertEquals(1, continuation.getArgumentBindings().size());
        assertEquals("@result", continuation.getArgumentBindings().get(0));
    }

    public void testContinuationTypeOnFail() {
        Continuation continuation = new Continuation(Continuation.Type.ON_FAIL, "errorHandler", List.of("@error"));
        
        assertEquals(Continuation.Type.ON_FAIL, continuation.getType());
        assertEquals("errorHandler", continuation.getHandlerName());
        assertEquals(1, continuation.getArgumentBindings().size());
        assertEquals("@error", continuation.getArgumentBindings().get(0));
    }

    public void testContinuationWithResultsBinding() {
        // @results is used for parallel execution
        Continuation continuation = new Continuation(Continuation.Type.ON_DONE, "mergeHandler", List.of("@results"));
        
        assertEquals(Continuation.Type.ON_DONE, continuation.getType());
        assertEquals("mergeHandler", continuation.getHandlerName());
        assertEquals(1, continuation.getArgumentBindings().size());
        assertEquals("@results", continuation.getArgumentBindings().get(0));
    }

    public void testContinuationWithMultipleBindings() {
        Continuation continuation = new Continuation(
            Continuation.Type.ON_DONE, 
            "complexHandler", 
            List.of("@result", "extraArg", "@progress")
        );
        
        assertEquals(3, continuation.getArgumentBindings().size());
        assertEquals("@result", continuation.getArgumentBindings().get(0));
        assertEquals("extraArg", continuation.getArgumentBindings().get(1));
        assertEquals("@progress", continuation.getArgumentBindings().get(2));
    }

    public void testInlineLambdaContinuation() {
        Continuation continuation = new Continuation(Continuation.Type.ON_DONE, "(@result) => { PRINT @result; }");
        
        assertTrue(continuation.isInlineLambda());
        assertEquals("(@result) => { PRINT @result; }", continuation.getInlineLambda());
    }

    public void testExecutionPipelineBuilding() {
        ExecutionPipeline pipeline = ExecutionPipeline.builder()
            .addOnDone(new Continuation(Continuation.Type.ON_DONE, "successHandler", List.of("@result")))
            .addOnFail(new Continuation(Continuation.Type.ON_FAIL, "errorHandler", List.of("@error")))
            .trackAs("my-execution")
            .timeout(300)
            .build();
        
        assertTrue(pipeline.hasOnDone());
        assertTrue(pipeline.hasOnFail());
        assertTrue(pipeline.getTrackName().isPresent());
        assertEquals("my-execution", pipeline.getTrackName().get());
        assertTrue(pipeline.getTimeoutSeconds().isPresent());
        assertEquals(Integer.valueOf(300), pipeline.getTimeoutSeconds().get());
        assertEquals(1, pipeline.getOnDoneHandlers().size());
        assertEquals(1, pipeline.getOnFailHandlers().size());
    }

    public void testExecutionPipelineWithResultsForParallel() {
        // Parallel execution uses @results (plural)
        ExecutionPipeline pipeline = ExecutionPipeline.builder()
            .addOnDone(new Continuation(Continuation.Type.ON_DONE, "mergeResults", List.of("@results")))
            .addOnFail(new Continuation(Continuation.Type.ON_FAIL, "handleError", List.of("@error")))
            .trackAs("parallel-fetch")
            .build();
        
        assertTrue(pipeline.hasOnDone());
        assertTrue(pipeline.getTrackName().isPresent());
        assertEquals("parallel-fetch", pipeline.getTrackName().get());
        
        Continuation onDone = pipeline.getOnDoneHandlers().get(0);
        assertEquals("mergeResults", onDone.getHandlerName());
        assertEquals("@results", onDone.getArgumentBindings().get(0));
    }

    public void testExecutionStatusEnum() {
        assertEquals(5, ExecutionStatus.values().length);
        assertEquals(ExecutionStatus.PENDING, ExecutionStatus.valueOf("PENDING"));
        assertEquals(ExecutionStatus.RUNNING, ExecutionStatus.valueOf("RUNNING"));
        assertEquals(ExecutionStatus.COMPLETED, ExecutionStatus.valueOf("COMPLETED"));
        assertEquals(ExecutionStatus.FAILED, ExecutionStatus.valueOf("FAILED"));
        assertEquals(ExecutionStatus.CANCELLED, ExecutionStatus.valueOf("CANCELLED"));
    }

    public void testExecutionProgress() {
        ExecutionProgress progress = new ExecutionProgress(50, "Processing records...");
        
        assertEquals(50, progress.getPercent());
        assertEquals("Processing records...", progress.getMessage());
    }

    public void testExecutionProgressZero() {
        ExecutionProgress progress = new ExecutionProgress(0, "Starting...");
        
        assertEquals(0, progress.getPercent());
        assertEquals("Starting...", progress.getMessage());
    }

    public void testExecutionProgressComplete() {
        ExecutionProgress progress = new ExecutionProgress(100, "Done");
        
        assertEquals(100, progress.getPercent());
        assertEquals("Done", progress.getMessage());
    }
}
