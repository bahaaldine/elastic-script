/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.primitives;

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the result of an elastic-script execution, including:
 * - The procedure's return value
 * - Captured PRINT output
 * - Execution metadata (ID, duration)
 */
public class ExecutionResult {

    private final Object result;
    private final List<String> output;
    private final String executionId;
    private final long durationMs;

    public ExecutionResult(Object result, List<String> output, String executionId, long durationMs) {
        this.result = result;
        this.output = output != null ? output : Collections.emptyList();
        this.executionId = executionId;
        this.durationMs = durationMs;
    }

    /**
     * The procedure's return value.
     */
    public Object getResult() {
        return result;
    }

    /**
     * Captured PRINT statement output.
     */
    public List<String> getOutput() {
        return output;
    }

    /**
     * The execution ID for log/trace correlation.
     */
    public String getExecutionId() {
        return executionId;
    }

    /**
     * The execution duration in milliseconds.
     */
    public long getDurationMs() {
        return durationMs;
    }

    /**
     * Returns true if there is PRINT output to display.
     */
    public boolean hasOutput() {
        return output != null && !output.isEmpty();
    }

    @Override
    public String toString() {
        return "ExecutionResult{" +
            "executionId='" + executionId + '\'' +
            ", durationMs=" + durationMs +
            ", outputLines=" + output.size() +
            ", result=" + result +
            '}';
    }
}
