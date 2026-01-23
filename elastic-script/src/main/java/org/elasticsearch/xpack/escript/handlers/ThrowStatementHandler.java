/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.escript.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.escript.exceptions.EScriptException;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;

/**
 * The ThrowStatementHandler class is responsible for handling THROW/RAISE statements
 * within the procedural SQL execution context.
 * 
 * Supported syntax:
 * <ul>
 *   <li>{@code THROW 'Error message';} - Simple string message</li>
 *   <li>{@code THROW error_variable;} - Expression that evaluates to a string</li>
 *   <li>{@code THROW 'Not found' WITH CODE 'HTTP_404';} - Message with error code</li>
 *   <li>{@code RAISE 'Error';} - RAISE is an alias for THROW</li>
 * </ul>
 * 
 * The thrown exception is an {@link EScriptException} which provides structured
 * error information accessible via the @error binding in CATCH blocks.
 */
public class ThrowStatementHandler {
    private final ProcedureExecutor executor;

    /**
     * Constructs a ThrowStatementHandler with the given ProcedureExecutor.
     *
     * @param executor The ProcedureExecutor instance responsible for executing procedures.
     */
    public ThrowStatementHandler(ProcedureExecutor executor) {
        this.executor = executor;
    }

    /**
     * Handles the THROW/RAISE statement asynchronously by evaluating the message expression
     * and optional error code, then invoking the listener's onFailure method.
     *
     * @param ctx      The Throw_statementContext representing the THROW/RAISE statement.
     * @param listener The ActionListener to handle asynchronous callbacks.
     */
    public void handleAsync(ElasticScriptParser.Throw_statementContext ctx, ActionListener<Object> listener) {
        // Get the expression contexts
        // Grammar: (THROW | RAISE) expression (WITH CODE expression)? SEMICOLON
        // expression(0) is the message, expression(1) is the optional code
        
        if (ctx.expression() == null || ctx.expression().isEmpty()) {
            listener.onFailure(new EScriptException("THROW/RAISE statement requires a message expression."));
            return;
        }
        
        // Evaluate the message expression
        executor.evaluateExpressionAsync(ctx.expression(0), ActionListener.wrap(
            messageResult -> {
                String message = convertToString(messageResult);
                
                // Check if there's a WITH CODE clause
                if (ctx.expression().size() > 1) {
                    // Evaluate the code expression
                    executor.evaluateExpressionAsync(ctx.expression(1), ActionListener.wrap(
                        codeResult -> {
                            String code = convertToString(codeResult);
                            // Throw with message and code
                            listener.onFailure(new EScriptException(message, code));
                        },
                        e -> listener.onFailure(new EScriptException(
                            "Failed to evaluate error code expression: " + e.getMessage(), null, EScriptException.TYPE_GENERIC, e))
                    ));
                } else {
                    // Throw with just the message
                    listener.onFailure(new EScriptException(message));
                }
            },
            e -> listener.onFailure(new EScriptException(
                "Failed to evaluate error message expression: " + e.getMessage(), null, EScriptException.TYPE_GENERIC, e))
        ));
    }
    
    /**
     * Converts a value to its string representation.
     *
     * @param value The value to convert
     * @return The string representation
     */
    private String convertToString(Object value) {
        if (value == null) {
            return "null";
        }
        return value.toString();
    }
}
