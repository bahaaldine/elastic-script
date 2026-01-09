/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.execution;

import org.elasticsearch.test.ESTestCase;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Tests for the pipe-driven async execution data structures.
 */
public class ExecutionDataStructuresTests extends ESTestCase {

    // ============ ExecutionStatus Tests ============

    public void testExecutionStatusTerminalStates() {
        assertTrue(ExecutionStatus.COMPLETED.isTerminal());
        assertTrue(ExecutionStatus.FAILED.isTerminal());
        assertTrue(ExecutionStatus.CANCELLED.isTerminal());
        assertFalse(ExecutionStatus.PENDING.isTerminal());
        assertFalse(ExecutionStatus.RUNNING.isTerminal());
    }

    public void testExecutionStatusCanRetry() {
        assertTrue(ExecutionStatus.FAILED.canRetry());
        assertTrue(ExecutionStatus.CANCELLED.canRetry());
        assertFalse(ExecutionStatus.COMPLETED.canRetry());
        assertFalse(ExecutionStatus.PENDING.canRetry());
        assertFalse(ExecutionStatus.RUNNING.canRetry());
    }

    // ============ ExecutionProgress Tests ============

    public void testExecutionProgressBasic() {
        ExecutionProgress progress = new ExecutionProgress(50, "Halfway done");
        assertEquals(50, progress.getPercent());
        assertEquals("Halfway done", progress.getMessage());
    }

    public void testExecutionProgressBoundaries() {
        ExecutionProgress zero = new ExecutionProgress(0, "Starting");
        assertEquals(0, zero.getPercent());

        ExecutionProgress hundred = new ExecutionProgress(100, "Complete");
        assertEquals(100, hundred.getPercent());
    }

    public void testExecutionProgressInvalidPercent() {
        expectThrows(IllegalArgumentException.class, () -> new ExecutionProgress(-1, "Invalid"));
        expectThrows(IllegalArgumentException.class, () -> new ExecutionProgress(101, "Invalid"));
    }

    public void testExecutionProgressFactoryMethods() {
        ExecutionProgress starting = ExecutionProgress.starting();
        assertEquals(0, starting.getPercent());
        
        ExecutionProgress complete = ExecutionProgress.complete();
        assertEquals(100, complete.getPercent());
    }

    // ============ Continuation Tests ============

    public void testContinuationNamedHandler() {
        Continuation continuation = new Continuation(
            Continuation.Type.ON_DONE,
            "process_result",
            List.of("@result")
        );
        
        assertEquals(Continuation.Type.ON_DONE, continuation.getType());
        assertEquals("process_result", continuation.getHandlerName());
        assertEquals(List.of("@result"), continuation.getArgumentBindings());
        assertFalse(continuation.isInlineLambda());
        assertNull(continuation.getInlineLambda());
    }

    public void testContinuationInlineLambda() {
        Continuation continuation = new Continuation(
            Continuation.Type.ON_FAIL,
            "(@error) => { PRINT @error; }"
        );
        
        assertEquals(Continuation.Type.ON_FAIL, continuation.getType());
        assertTrue(continuation.isInlineLambda());
        assertEquals("(@error) => { PRINT @error; }", continuation.getInlineLambda());
        assertNull(continuation.getHandlerName());
    }

    public void testContinuationEquality() {
        Continuation a = new Continuation(Continuation.Type.ON_DONE, "handler", List.of("@result"));
        Continuation b = new Continuation(Continuation.Type.ON_DONE, "handler", List.of("@result"));
        Continuation c = new Continuation(Continuation.Type.ON_FAIL, "handler", List.of("@error"));
        
        assertEquals(a, b);
        assertNotEquals(a, c);
    }

    // ============ ExecutionPipeline Tests ============

    public void testExecutionPipelineBuilder() {
        ExecutionPipeline pipeline = ExecutionPipeline.builder()
            .addOnDone(new Continuation(Continuation.Type.ON_DONE, "process", List.of("@result")))
            .addOnFail(new Continuation(Continuation.Type.ON_FAIL, "handle_error", List.of("@error")))
            .addFinally(new Continuation(Continuation.Type.FINALLY, "cleanup", List.of()))
            .trackAs("daily-analysis")
            .timeout(300)
            .build();
        
        assertEquals(1, pipeline.getOnDoneHandlers().size());
        assertEquals(1, pipeline.getOnFailHandlers().size());
        assertEquals(1, pipeline.getFinallyHandlers().size());
        assertTrue(pipeline.isTracked());
        assertEquals("daily-analysis", pipeline.getTrackName().orElse(null));
        assertTrue(pipeline.hasTimeout());
        assertEquals(Integer.valueOf(300), pipeline.getTimeoutSeconds().orElse(null));
    }

    public void testExecutionPipelineMultipleContinuations() {
        ExecutionPipeline pipeline = ExecutionPipeline.builder()
            .addOnDone(new Continuation(Continuation.Type.ON_DONE, "step1", List.of("@result")))
            .addOnDone(new Continuation(Continuation.Type.ON_DONE, "step2", List.of("@result")))
            .addOnDone(new Continuation(Continuation.Type.ON_DONE, "step3", List.of("@result")))
            .build();
        
        assertEquals(3, pipeline.getOnDoneHandlers().size());
        assertEquals("step1", pipeline.getOnDoneHandlers().get(0).getHandlerName());
        assertEquals("step2", pipeline.getOnDoneHandlers().get(1).getHandlerName());
        assertEquals("step3", pipeline.getOnDoneHandlers().get(2).getHandlerName());
    }

    public void testExecutionPipelineFireAndForget() {
        ExecutionPipeline pipeline = ExecutionPipeline.builder().build();
        
        assertTrue(pipeline.isFireAndForget());
        assertFalse(pipeline.hasOnDone());
        assertFalse(pipeline.hasOnFail());
        assertFalse(pipeline.hasFinally());
    }

    // ============ ExecutionState Tests ============

    public void testExecutionStateBuilder() {
        ExecutionState state = ExecutionState.builder()
            .executionId("exec-12345")
            .name("daily-analysis")
            .procedure("analyze_logs")
            .parameters(Map.of("index", "logs-*"))
            .status(ExecutionStatus.PENDING)
            .node("node-1")
            .startedAt(Instant.now())
            .build();
        
        assertEquals("exec-12345", state.getExecutionId());
        assertEquals("daily-analysis", state.getName());
        assertEquals("daily-analysis", state.getEffectiveName()); // Uses name when present
        assertEquals("analyze_logs", state.getProcedure());
        assertEquals("logs-*", state.getParameters().get("index"));
        assertEquals(ExecutionStatus.PENDING, state.getStatus());
        assertEquals("node-1", state.getNode());
        assertNotNull(state.getStartedAt());
        assertNull(state.getCompletedAt());
        assertNull(state.getResult());
        assertNull(state.getError());
    }

    public void testExecutionStateWithoutName() {
        ExecutionState state = ExecutionState.builder()
            .executionId("exec-67890")
            .procedure("my_proc")
            .status(ExecutionStatus.RUNNING)
            .build();
        
        assertNull(state.getName());
        assertEquals("exec-67890", state.getEffectiveName()); // Falls back to ID
    }

    public void testExecutionStateWithCompletion() {
        ExecutionState original = ExecutionState.builder()
            .executionId("exec-12345")
            .procedure("analyze_logs")
            .status(ExecutionStatus.RUNNING)
            .build();
        
        ExecutionState completed = original.withCompletion("analysis-result");
        
        assertEquals(ExecutionStatus.COMPLETED, completed.getStatus());
        assertEquals("analysis-result", completed.getResult());
        assertNotNull(completed.getCompletedAt());
        assertEquals(original.getExecutionId(), completed.getExecutionId());
    }

    public void testExecutionStateWithFailure() {
        ExecutionState original = ExecutionState.builder()
            .executionId("exec-12345")
            .procedure("analyze_logs")
            .status(ExecutionStatus.RUNNING)
            .build();
        
        ExecutionState failed = original.withFailure("Something went wrong");
        
        assertEquals(ExecutionStatus.FAILED, failed.getStatus());
        assertEquals("Something went wrong", failed.getError());
        assertNotNull(failed.getCompletedAt());
    }

    public void testExecutionStateWithProgress() {
        ExecutionState original = ExecutionState.builder()
            .executionId("exec-12345")
            .procedure("analyze_logs")
            .status(ExecutionStatus.RUNNING)
            .build();
        
        ExecutionState updated = original.withProgress(50, "Processing batch 5/10");
        
        assertNotNull(updated.getProgress());
        assertEquals(50, updated.getProgress().getPercent());
        assertEquals("Processing batch 5/10", updated.getProgress().getMessage());
    }

    public void testExecutionStateIsTerminal() {
        ExecutionState running = ExecutionState.builder()
            .executionId("exec-1")
            .procedure("proc")
            .status(ExecutionStatus.RUNNING)
            .build();
        assertFalse(running.isTerminal());
        
        ExecutionState completed = running.withCompletion("result");
        assertTrue(completed.isTerminal());
    }

    public void testExecutionStateEquality() {
        ExecutionState a = ExecutionState.builder()
            .executionId("exec-12345")
            .procedure("proc")
            .status(ExecutionStatus.PENDING)
            .build();
        
        ExecutionState b = ExecutionState.builder()
            .executionId("exec-12345")
            .procedure("different_proc")  // Different proc but same ID
            .status(ExecutionStatus.RUNNING)
            .build();
        
        // States are equal if they have the same execution ID
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}



