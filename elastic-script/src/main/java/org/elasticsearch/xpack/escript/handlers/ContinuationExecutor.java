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
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.execution.Continuation;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Executes continuation handlers after async procedure completion.
 *
 * Handles binding of special variables (@result, @error) and invoking handlers.
 */
public class ContinuationExecutor {

    private static final Logger LOGGER = LogManager.getLogger(ContinuationExecutor.class);

    private final ProcedureExecutor executor;

    public ContinuationExecutor(ProcedureExecutor executor) {
        this.executor = executor;
    }

    /**
     * Executes a list of continuations sequentially.
     *
     * @param continuations The continuations to execute
     * @param result The result from the procedure (for @result binding)
     * @param error The error if procedure failed (for @error binding)
     * @param listener Callback when all continuations complete
     */
    public void executeContinuationsAsync(
        List<Continuation> continuations,
        Object result,
        Exception error,
        ActionListener<Object> listener
    ) {
        if (continuations.isEmpty()) {
            listener.onResponse(null);
            return;
        }

        executeContinuationChainAsync(continuations, 0, result, error, listener);
    }

    private void executeContinuationChainAsync(
        List<Continuation> continuations,
        int index,
        Object result,
        Exception error,
        ActionListener<Object> listener
    ) {
        if (index >= continuations.size()) {
            listener.onResponse(result);
            return;
        }

        Continuation continuation = continuations.get(index);
        
        executeSingleContinuationAsync(continuation, result, error, ActionListener.wrap(
            continuationResult -> {
                // Use continuation result as the new result for chaining
                Object nextResult = continuationResult != null ? continuationResult : result;
                executeContinuationChainAsync(continuations, index + 1, nextResult, error, listener);
            },
            e -> {
                LOGGER.warn("Continuation {} failed: {}", continuation, e.getMessage());
                listener.onFailure(e);
            }
        ));
    }

    private void executeSingleContinuationAsync(
        Continuation continuation,
        Object result,
        Exception error,
        ActionListener<Object> listener
    ) {
        LOGGER.debug("Executing continuation: {}", continuation);

        if (continuation.isInlineLambda()) {
            executeInlineLambda(continuation.getInlineLambda(), result, error, listener);
        } else {
            executeNamedHandler(continuation, result, error, listener);
        }
    }

    private void executeNamedHandler(
        Continuation continuation,
        Object result,
        Exception error,
        ActionListener<Object> listener
    ) {
        String handlerName = continuation.getHandlerName();
        List<Object> args = buildArgumentList(continuation.getArgumentBindings(), result, error);

        LOGGER.debug("Calling handler: {}({})", handlerName, args.size());

        // Try to call as a procedure first
        executor.callProcedureByNameAsync(handlerName, args, ActionListener.wrap(
            listener::onResponse,
            procError -> {
                // If procedure not found, try as a function
                try {
                    executor.callFunctionAsync(handlerName, args, listener);
                } catch (Exception funcError) {
                    listener.onFailure(new RuntimeException(
                        "Handler '" + handlerName + "' not found as procedure or function", funcError));
                }
            }
        ));
    }

    private void executeInlineLambda(
        String lambda,
        Object result,
        Exception error,
        ActionListener<Object> listener
    ) {
        // For inline lambdas, we need to:
        // 1. Set up special variables (@result, @error)
        // 2. Parse and execute the lambda body

        ExecutionContext context = executor.getContext();
        
        // Bind special variables
        if (result != null) {
            try {
                context.declareVariable("result", inferType(result));
                context.setVariable("result", result);
            } catch (Exception e) {
                // Variable might already exist
                try {
                    context.setVariable("result", result);
                } catch (Exception ignored) {}
            }
        }
        
        if (error != null) {
            try {
                context.declareVariable("error", "STRING");
                context.setVariable("error", error.getMessage());
            } catch (Exception e) {
                try {
                    context.setVariable("error", error.getMessage());
                } catch (Exception ignored) {}
            }
        }

        // Parse and execute the lambda
        // The lambda syntax is: (@result) => { statements }
        // We need to extract and execute the statements
        
        // For now, we'll support simple lambda expressions
        // More complex lambda execution would require parsing the lambda body
        
        LOGGER.debug("Executing inline lambda: {}", lambda);
        
        // Extract the body between { and }
        int braceStart = lambda.indexOf('{');
        int braceEnd = lambda.lastIndexOf('}');
        
        if (braceStart != -1 && braceEnd != -1 && braceEnd > braceStart) {
            String body = lambda.substring(braceStart + 1, braceEnd).trim();
            
            // Execute the body as a mini-script
            executor.executeStatementBlockAsync(body, listener);
        } else {
            // Simple expression lambda
            // Extract expression after =>
            int arrowIndex = lambda.indexOf("=>");
            if (arrowIndex != -1) {
                String expression = lambda.substring(arrowIndex + 2).trim();
                executor.getExpressionEvaluator().evaluateExpressionStringAsync(expression, listener);
            } else {
                listener.onFailure(new RuntimeException("Invalid lambda syntax: " + lambda));
            }
        }
    }

    private List<Object> buildArgumentList(List<String> bindings, Object result, Exception error) {
        List<Object> args = new ArrayList<>();
        
        for (String binding : bindings) {
            if ("@result".equals(binding)) {
                args.add(result);
            } else if ("@error".equals(binding)) {
                args.add(error != null ? error.getMessage() : null);
            } else if ("@progress".equals(binding)) {
                // Progress is typically passed during execution, not at completion
                args.add(null);
            } else {
                // Regular expression - evaluate it
                try {
                    Object value = executor.getContext().getVariable(binding);
                    args.add(value);
                } catch (Exception e) {
                    // If not a variable, pass as literal
                    args.add(binding);
                }
            }
        }
        
        return args;
    }

    private String inferType(Object value) {
        if (value == null) {
            return "STRING";
        } else if (value instanceof String) {
            return "STRING";
        } else if (value instanceof Number) {
            return "NUMBER";
        } else if (value instanceof Boolean) {
            return "BOOLEAN";
        } else if (value instanceof List) {
            return "ARRAY";
        } else if (value instanceof Map) {
            return "DOCUMENT";
        } else {
            return "STRING";
        }
    }
}

