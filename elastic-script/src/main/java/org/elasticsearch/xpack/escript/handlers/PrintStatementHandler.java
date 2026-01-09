/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License 2.0.
 */
package org.elasticsearch.xpack.escript.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.xpack.escript.logging.EScriptLogger;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;
import org.elasticsearch.xpack.escript.evaluators.ExpressionEvaluator;

/**
 * Handler for PRINT statements.
 * Evaluates the expression asynchronously using ExpressionEvaluator.evaluateExpressionAsync,
 * then:
 * 1. Captures the output in ExecutionContext for API response
 * 2. Logs to the dedicated OUTPUT logger for visibility
 */
public class PrintStatementHandler {

    private final ProcedureExecutor executor;

    public PrintStatementHandler(ProcedureExecutor executor) {
        this.executor = executor;
    }

    /**
     * Executes a PRINT statement.
     *
     * The print statement is defined as:
     *   PRINT expression (COMMA severity)? SEMICOLON
     *
     * This method evaluates the expression asynchronously and then:
     * - Captures the output in the ExecutionContext for API response
     * - Logs the output using EScriptLogger with the appropriate severity
     *
     * @param ctx the parse tree context for the print_statement.
     * @param listener the ActionListener to signal completion.
     */
    public void execute(ElasticScriptParser.Print_statementContext ctx, ActionListener<Object> listener) {
        // Use the expression() method provided by the parser
        ElasticScriptParser.ExpressionContext exprCtx = ctx.expression();
        ExecutionContext execContext = executor.getContext();
        String executionId = execContext.getExecutionId();

        new ExpressionEvaluator(executor)
            .evaluateExpressionAsync(exprCtx, new ActionListener<Object>() {
                @Override
                public void onResponse(Object result) {
                    String output = (result != null) ? result.toString() : "null";
                    
                    // 1. Capture output for API response
                    execContext.addPrintOutput(output);
                    
                    // 2. Log to dedicated OUTPUT logger with appropriate severity
                    if (ctx.severity() != null) {
                        String severity = ctx.severity().getText().toUpperCase();
                        switch (severity) {
                            case "DEBUG":
                                EScriptLogger.userDebug(executionId, output);
                                break;
                            case "WARN":
                                EScriptLogger.userWarn(executionId, output);
                                break;
                            case "ERROR":
                                EScriptLogger.userError(executionId, output);
                                break;
                            case "INFO":
                            default:
                                EScriptLogger.userOutput(executionId, output);
                                break;
                        }
                    } else {
                        EScriptLogger.userOutput(executionId, output);
                    }
                    
                    listener.onResponse(null);
                }
                
                @Override
                public void onFailure(Exception e) {
                    EScriptLogger.error(executionId, "PRINT failed: " + e.getMessage(), e);
                    listener.onFailure(e);
                }
            });
    }
}
