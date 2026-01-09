/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.utils;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.escript.logging.EScriptLogger;

/**
 * Utility class for wrapping an ActionListener in logging calls.
 * 
 * Uses TRACE level logging to trace async execution flow without polluting normal logs.
 * Enable with: logger.o.e.x.e.EScript: TRACE
 */
public final class ActionListenerUtils {

    private ActionListenerUtils() {
        // Prevent instantiation
    }

    /**
     * Wraps the given delegate ActionListener with TRACE-level async flow logging.
     *
     * @param delegate    The real listener to be notified eventually
     * @param executionId The execution ID for correlation
     * @param operation   A short label for logs (e.g. "ForLoop", "PrintStatement")
     * @return A wrapped listener that logs at TRACE level
     */
    public static <T> ActionListener<T> withLogging(
        ActionListener<T> delegate, 
        String executionId, 
        String operation
    ) {
        // Skip wrapping if TRACE is not enabled for performance
        if (!EScriptLogger.isTraceEnabled()) {
            return delegate;
        }
        
        return new ActionListener<>() {
            @Override
            public void onResponse(T response) {
                EScriptLogger.asyncExit(executionId, operation, response);
                delegate.onResponse(response);
            }

            @Override
            public void onFailure(Exception e) {
                EScriptLogger.asyncFailed(executionId, operation, e.getMessage());
                delegate.onFailure(e);
            }
        };
    }

    /**
     * Legacy method for backwards compatibility - logs without execution ID context.
     * 
     * @deprecated Use {@link #withLogging(ActionListener, String, String)} instead
     */
    @Deprecated
    public static <T> ActionListener<T> withLogging(
        ActionListener<T> delegate, 
        String className, 
        String operation,
        @SuppressWarnings("unused") boolean legacyMode
    ) {
        // No-op wrapper - just return delegate since we don't have execution context
        return delegate;
    }
}
