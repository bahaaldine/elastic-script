/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.execution;

import org.elasticsearch.test.ESTestCase;
import org.junit.Before;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for ExecutionState and ExecutionPipeline.
 * These test the data structures used for multi-node execution tracking.
 */
public class ExecutionRegistryTests extends ESTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Test creating an ExecutionState with all fields.
     */
    public void testExecutionStateBuilder() {
        Instant now = Instant.now();
        
        ExecutionState state = ExecutionState.builder()
            .executionId("exec-12345")
            .name("my_task")
            .procedure("test_procedure")
            .parameters(Map.of("param1", "value1"))
            .status(ExecutionStatus.PENDING)
            .startedAt(now)
            .node("node-1")
            .build();

        assertThat(state.getExecutionId(), equalTo("exec-12345"));
        assertThat(state.getName(), equalTo("my_task"));
        assertThat(state.getProcedure(), equalTo("test_procedure"));
        assertThat(state.getParameters().get("param1"), equalTo("value1"));
        assertThat(state.getStatus(), equalTo(ExecutionStatus.PENDING));
        assertThat(state.getStartedAt(), equalTo(now));
        assertThat(state.getNode(), equalTo("node-1"));
        assertThat(state.isTerminal(), is(false));
    }

    /**
     * Test state transitions.
     */
    public void testExecutionStateTransitions() {
        ExecutionState initial = ExecutionState.builder()
            .executionId("exec-001")
            .procedure("test")
            .status(ExecutionStatus.PENDING)
            .startedAt(Instant.now())
            .node("node-1")
            .build();

        // Transition to RUNNING
        ExecutionState running = initial.withStatus(ExecutionStatus.RUNNING);
        assertThat(running.getStatus(), equalTo(ExecutionStatus.RUNNING));
        assertThat(running.isTerminal(), is(false));

        // Transition to COMPLETED
        ExecutionState completed = running.withCompletion("result data");
        assertThat(completed.getStatus(), equalTo(ExecutionStatus.COMPLETED));
        assertThat(completed.getResult(), equalTo("result data"));
        assertThat(completed.getCompletedAt(), notNullValue());
        assertThat(completed.isTerminal(), is(true));
    }

    /**
     * Test failure state.
     */
    public void testExecutionStateFailure() {
        ExecutionState running = ExecutionState.builder()
            .executionId("exec-002")
            .procedure("failing_test")
            .status(ExecutionStatus.RUNNING)
            .startedAt(Instant.now())
            .node("node-1")
            .build();

        ExecutionState failed = running.withFailure("Connection timeout");
        assertThat(failed.getStatus(), equalTo(ExecutionStatus.FAILED));
        assertThat(failed.getError(), equalTo("Connection timeout"));
        assertThat(failed.getCompletedAt(), notNullValue());
        assertThat(failed.isTerminal(), is(true));
    }

    /**
     * Test progress updates.
     */
    public void testExecutionStateProgress() {
        ExecutionState running = ExecutionState.builder()
            .executionId("exec-003")
            .procedure("long_running")
            .status(ExecutionStatus.RUNNING)
            .startedAt(Instant.now())
            .node("node-1")
            .build();

        ExecutionState withProgress = running.withProgress(50, "Processing step 5 of 10");
        assertThat(withProgress.getProgress(), notNullValue());
        assertThat(withProgress.getProgress().getPercent(), equalTo(50));
        assertThat(withProgress.getProgress().getMessage(), equalTo("Processing step 5 of 10"));
    }

    /**
     * Test ExecutionPipeline builder.
     */
    public void testExecutionPipelineBuilder() {
        ExecutionPipeline pipeline = ExecutionPipeline.builder()
            .trackAs("my_pipeline")
            .timeout(300)
            .build();

        assertThat(pipeline.getTrackName(), equalTo(Optional.of("my_pipeline")));
        assertThat(pipeline.getTimeoutSeconds(), equalTo(Optional.of(300)));
        assertThat(pipeline.isTracked(), is(true));
        assertThat(pipeline.hasTimeout(), is(true));
        assertThat(pipeline.isFireAndForget(), is(true));  // No handlers = fire and forget
    }

    /**
     * Test ExecutionPipeline with handlers.
     */
    public void testExecutionPipelineWithHandlers() {
        Continuation onDone = new Continuation(Continuation.Type.ON_DONE, "handler_proc", java.util.List.of("@result"));
        Continuation onFail = new Continuation(Continuation.Type.ON_FAIL, "error_handler", java.util.List.of("@error"));
        Continuation finallyHandler = new Continuation(Continuation.Type.FINALLY, "cleanup", java.util.List.of());

        ExecutionPipeline pipeline = ExecutionPipeline.builder()
            .addOnDone(onDone)
            .addOnFail(onFail)
            .addFinally(finallyHandler)
            .trackAs("complex_pipeline")
            .build();

        assertThat(pipeline.hasOnDone(), is(true));
        assertThat(pipeline.hasOnFail(), is(true));
        assertThat(pipeline.hasFinally(), is(true));
        assertThat(pipeline.isFireAndForget(), is(false));
        assertThat(pipeline.getOnDoneHandlers().size(), equalTo(1));
        assertThat(pipeline.getOnFailHandlers().size(), equalTo(1));
        assertThat(pipeline.getFinallyHandlers().size(), equalTo(1));
    }

    /**
     * Test terminal state detection.
     */
    public void testTerminalStates() {
        assertThat(ExecutionStatus.PENDING.isTerminal(), is(false));
        assertThat(ExecutionStatus.RUNNING.isTerminal(), is(false));
        assertThat(ExecutionStatus.COMPLETED.isTerminal(), is(true));
        assertThat(ExecutionStatus.FAILED.isTerminal(), is(true));
        assertThat(ExecutionStatus.CANCELLED.isTerminal(), is(true));
    }

    /**
     * Test execution ID generation.
     */
    public void testExecutionIdGeneration() {
        String id1 = ExecutionRegistry.generateExecutionId();
        String id2 = ExecutionRegistry.generateExecutionId();

        assertThat(id1.startsWith("exec-"), is(true));
        assertThat(id2.startsWith("exec-"), is(true));
        assertThat(id1.equals(id2), is(false));  // Should be unique
    }

    /**
     * Test Continuation creation.
     */
    public void testContinuationCreation() {
        java.util.List<String> bindings = java.util.List.of("@result", "@extra");
        Continuation continuation = new Continuation(Continuation.Type.ON_DONE, "my_handler", bindings);

        assertThat(continuation.getHandlerName(), equalTo("my_handler"));
        assertThat(continuation.getArgumentBindings().size(), equalTo(2));
        assertThat(continuation.getArgumentBindings().get(0), equalTo("@result"));
        assertThat(continuation.getType(), equalTo(Continuation.Type.ON_DONE));
    }

    /**
     * Test ExecutionProgress.
     */
    public void testExecutionProgress() {
        ExecutionProgress progress = new ExecutionProgress(75, "Almost done");
        assertThat(progress.getPercent(), equalTo(75));
        assertThat(progress.getMessage(), equalTo("Almost done"));
    }

    /**
     * Test state immutability - transitions return new objects.
     */
    public void testStateImmutability() {
        ExecutionState original = ExecutionState.builder()
            .executionId("exec-immutable")
            .procedure("test")
            .status(ExecutionStatus.PENDING)
            .startedAt(Instant.now())
            .node("node-1")
            .build();

        ExecutionState modified = original.withStatus(ExecutionStatus.RUNNING);

        // Original should be unchanged
        assertThat(original.getStatus(), equalTo(ExecutionStatus.PENDING));
        // Modified should have new status
        assertThat(modified.getStatus(), equalTo(ExecutionStatus.RUNNING));
        // Should be different objects
        assertThat(original == modified, is(false));
    }
}
