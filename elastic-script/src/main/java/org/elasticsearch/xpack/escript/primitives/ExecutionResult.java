/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V.
 * under one or more contributor license agreements. Licensed under
 * the Elastic License 2.0; you may not use this file except in compliance
 * with the Elastic License 2.0.
 */

package org.elasticsearch.xpack.escript.primitives;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents the complete result of an elastic-script execution.
 * 
 * This class encapsulates:
 * - The actual return value (if any)
 * - All PRINT output captured during execution
 * - Execution metadata (execution ID, duration, etc.)
 * 
 * This enables the API to return structured responses with observability data.
 */
public class ExecutionResult {
    
    private final Object result;
    private final List<String> printOutput;
    private final String executionId;
    private final long durationMs;
    private final String procedureName;
    
    /**
     * Creates a new ExecutionResult.
     * 
     * @param result The return value of the execution (may be null)
     * @param printOutput List of PRINT statement outputs captured during execution
     * @param executionId The unique execution ID for tracing
     * @param durationMs Execution duration in milliseconds
     * @param procedureName The name of the executed procedure (may be null for ad-hoc scripts)
     */
    public ExecutionResult(Object result, List<String> printOutput, String executionId, long durationMs, String procedureName) {
        this.result = result;
        this.printOutput = printOutput != null ? List.copyOf(printOutput) : Collections.emptyList();
        this.executionId = executionId;
        this.durationMs = durationMs;
        this.procedureName = procedureName;
    }
    
    /**
     * Creates a simple ExecutionResult with just a result value.
     * Used for backwards compatibility when metadata is not needed.
     */
    public static ExecutionResult of(Object result) {
        return new ExecutionResult(result, Collections.emptyList(), null, 0, null);
    }
    
    /**
     * Creates an ExecutionResult from context and raw result.
     */
    public static ExecutionResult from(Object rawResult, String executionId, List<String> printOutput, 
                                       long startTimeMs, String procedureName) {
        long durationMs = System.currentTimeMillis() - startTimeMs;
        return new ExecutionResult(rawResult, printOutput, executionId, durationMs, procedureName);
    }
    
    /**
     * Gets the actual return value of the execution.
     * This unwraps ReturnValue if necessary.
     */
    public Object getResult() {
        if (result instanceof ReturnValue) {
            return ((ReturnValue) result).getValue();
        }
        return result;
    }
    
    /**
     * Gets the raw result without unwrapping.
     */
    public Object getRawResult() {
        return result;
    }
    
    /**
     * Gets all PRINT output captured during execution.
     */
    public List<String> getPrintOutput() {
        return printOutput;
    }
    
    /**
     * Checks if there is any PRINT output.
     */
    public boolean hasPrintOutput() {
        return !printOutput.isEmpty();
    }
    
    /**
     * Gets the execution ID for tracing correlation.
     */
    public String getExecutionId() {
        return executionId;
    }
    
    /**
     * Gets the execution duration in milliseconds.
     */
    public long getDurationMs() {
        return durationMs;
    }
    
    /**
     * Gets the procedure name, if known.
     */
    public String getProcedureName() {
        return procedureName;
    }
    
    /**
     * Converts this result to a map for JSON serialization.
     * Includes result, output, and metadata.
     */
    public Map<String, Object> toMap() {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        
        // Always include result
        map.put("result", getResult());
        
        // Include output only if present
        if (hasPrintOutput()) {
            map.put("output", printOutput);
        }
        
        // Include metadata
        java.util.Map<String, Object> meta = new java.util.LinkedHashMap<>();
        if (executionId != null) {
            meta.put("execution_id", executionId);
        }
        meta.put("duration_ms", durationMs);
        if (procedureName != null) {
            meta.put("procedure_name", procedureName);
        }
        
        if (!meta.isEmpty()) {
            map.put("_meta", meta);
        }
        
        return map;
    }
}
