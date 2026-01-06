/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.execution;

/**
 * Status of an async procedure execution.
 */
public enum ExecutionStatus {
    /**
     * Execution is queued but not yet started.
     */
    PENDING,

    /**
     * Execution is currently running.
     */
    RUNNING,

    /**
     * Execution completed successfully.
     */
    COMPLETED,

    /**
     * Execution failed with an error.
     */
    FAILED,

    /**
     * Execution was cancelled by the user.
     */
    CANCELLED;

    /**
     * Returns true if this is a terminal state (no further transitions possible).
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }

    /**
     * Returns true if this execution can be retried.
     */
    public boolean canRetry() {
        return this == FAILED || this == CANCELLED;
    }
}

