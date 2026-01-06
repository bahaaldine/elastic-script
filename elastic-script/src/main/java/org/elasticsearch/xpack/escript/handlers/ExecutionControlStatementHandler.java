/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.escript.execution.ExecutionRegistry;
import org.elasticsearch.xpack.escript.execution.ExecutionState;
import org.elasticsearch.xpack.escript.execution.ExecutionStatus;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Handles execution control statements.
 *
 * Syntax: {@code EXECUTION('name') | STATUS/CANCEL/RETRY/WAIT;}
 */
public class ExecutionControlStatementHandler {

    private static final Logger LOGGER = LogManager.getLogger(ExecutionControlStatementHandler.class);

    private final ExecutionRegistry registry;

    public ExecutionControlStatementHandler(ExecutionRegistry registry) {
        this.registry = registry;
    }

    /**
     * Handles an execution control statement.
     */
    public void handleAsync(
        ElasticScriptParser.Execution_control_statementContext ctx,
        ActionListener<Object> listener
    ) {
        try {
            // Extract the execution name
            String executionName = ctx.STRING().getText();
            // Remove quotes
            executionName = executionName.substring(1, executionName.length() - 1);
            
            LOGGER.debug("Execution control for: {}", executionName);

            // Get the operation
            ElasticScriptParser.Execution_operationContext operation = ctx.execution_operation();

            if (operation instanceof ElasticScriptParser.StatusOperationContext) {
                handleStatus(executionName, listener);
            } else if (operation instanceof ElasticScriptParser.CancelOperationContext) {
                handleCancel(executionName, listener);
            } else if (operation instanceof ElasticScriptParser.RetryOperationContext) {
                handleRetry(executionName, listener);
            } else if (operation instanceof ElasticScriptParser.WaitOperationContext waitOp) {
                int timeout = 60; // Default timeout
                if (waitOp.INT() != null) {
                    timeout = Integer.parseInt(waitOp.INT().getText());
                }
                handleWait(executionName, timeout, listener);
            } else {
                listener.onFailure(new RuntimeException("Unknown execution operation"));
            }
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    private void handleStatus(String executionName, ActionListener<Object> listener) {
        registry.getExecution(executionName, ActionListener.wrap(
            optState -> {
                if (optState.isEmpty()) {
                    listener.onResponse(Map.of(
                        "name", executionName,
                        "exists", false
                    ));
                } else {
                    ExecutionState state = optState.get();
                    listener.onResponse(Map.of(
                        "name", executionName,
                        "exists", true,
                        "status", state.getStatus().name(),
                        "procedure", state.getProcedure(),
                        "started_at", state.getStartedAt() != null ? state.getStartedAt().toString() : null,
                        "completed_at", state.getCompletedAt() != null ? state.getCompletedAt().toString() : null,
                        "progress", state.getProgress() != null ? state.getProgress().getPercent() : 0,
                        "error", state.getError() != null ? state.getError() : "",
                        "result", state.getResult() != null ? state.getResult() : ""
                    ));
                }
            },
            listener::onFailure
        ));
    }

    private void handleCancel(String executionName, ActionListener<Object> listener) {
        registry.markCancelled(executionName, ActionListener.wrap(
            state -> {
                LOGGER.info("Cancelled execution: {}", executionName);
                listener.onResponse(Map.of(
                    "name", executionName,
                    "status", "CANCELLED",
                    "success", true
                ));
            },
            e -> {
                LOGGER.warn("Failed to cancel execution {}: {}", executionName, e.getMessage());
                listener.onResponse(Map.of(
                    "name", executionName,
                    "success", false,
                    "error", e.getMessage()
                ));
            }
        ));
    }

    private void handleRetry(String executionName, ActionListener<Object> listener) {
        registry.getExecution(executionName, ActionListener.wrap(
            optState -> {
                if (optState.isEmpty()) {
                    listener.onFailure(new RuntimeException("Execution not found: " + executionName));
                    return;
                }
                
                ExecutionState state = optState.get();
                if (!state.getStatus().canRetry()) {
                    listener.onFailure(new RuntimeException("Cannot retry execution in state: " + state.getStatus()));
                    return;
                }

                // Mark as pending and re-run
                ExecutionState retryState = state.toBuilder()
                    .status(ExecutionStatus.PENDING)
                    .error(null)
                    .result(null)
                    .build();
                
                registry.updateExecution(retryState, ActionListener.wrap(
                    updated -> {
                        LOGGER.info("Retrying execution: {}", executionName);
                        // Note: The actual re-execution would need to be triggered by a listener
                        // For now, we just update the state
                        listener.onResponse(Map.of(
                            "name", executionName,
                            "status", "PENDING",
                            "retry", true
                        ));
                    },
                    listener::onFailure
                ));
            },
            listener::onFailure
        ));
    }

    private void handleWait(String executionName, int timeoutSeconds, ActionListener<Object> listener) {
        LOGGER.debug("Waiting for execution: {} (timeout: {}s)", executionName, timeoutSeconds);
        
        // Poll for completion
        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeoutSeconds * 1000L;
        
        pollForCompletion(executionName, startTime, timeoutMillis, listener);
    }

    private void pollForCompletion(
        String executionName,
        long startTime,
        long timeoutMillis,
        ActionListener<Object> listener
    ) {
        registry.getExecution(executionName, ActionListener.wrap(
            optState -> {
                if (optState.isEmpty()) {
                    listener.onFailure(new RuntimeException("Execution not found: " + executionName));
                    return;
                }
                
                ExecutionState state = optState.get();
                
                if (state.isTerminal()) {
                    // Execution has completed
                    if (state.getStatus() == ExecutionStatus.COMPLETED) {
                        listener.onResponse(state.getResult());
                    } else if (state.getStatus() == ExecutionStatus.FAILED) {
                        listener.onFailure(new RuntimeException("Execution failed: " + state.getError()));
                    } else if (state.getStatus() == ExecutionStatus.CANCELLED) {
                        listener.onFailure(new RuntimeException("Execution was cancelled"));
                    }
                } else {
                    // Check timeout
                    long elapsed = System.currentTimeMillis() - startTime;
                    if (elapsed >= timeoutMillis) {
                        listener.onFailure(new RuntimeException("Wait timeout exceeded for: " + executionName));
                        return;
                    }
                    
                    // Poll again after a short delay
                    try {
                        Thread.sleep(500); // Poll every 500ms
                        pollForCompletion(executionName, startTime, timeoutMillis, listener);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        listener.onFailure(e);
                    }
                }
            },
            listener::onFailure
        ));
    }
}

