/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.xpack.escript.exceptions.BreakException;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;
import org.elasticsearch.xpack.escript.primitives.ReturnValue;
import org.elasticsearch.xpack.escript.utils.ActionListenerUtils;

import java.util.List;

/**
 * The SwitchStatementHandler class handles SWITCH/CASE statements within the procedural SQL execution context.
 * It evaluates a switch expression and matches it against case values, executing the corresponding statement block.
 */
public class SwitchStatementHandler {
    private final ProcedureExecutor executor;

    /**
     * Constructs a SwitchStatementHandler with the given ProcedureExecutor.
     *
     * @param executor The ProcedureExecutor instance responsible for executing procedures.
     */
    public SwitchStatementHandler(ProcedureExecutor executor) {
        this.executor = executor;
    }

    /**
     * Handles the SWITCH statement by evaluating the switch expression and matching against cases.
     *
     * @param ctx      The Switch_statementContext representing the SWITCH statement.
     * @param listener The ActionListener to handle asynchronous callbacks.
     */
    public void handleAsync(ElasticScriptParser.Switch_statementContext ctx, ActionListener<Object> listener) {
        // First evaluate the switch expression
        executor.evaluateExpressionAsync(ctx.expression(), ActionListener.wrap(
            switchValue -> {
                // Now match against cases
                handleCasesAsync(ctx.case_clause(), ctx.default_clause(), switchValue, 0, listener);
            },
            listener::onFailure
        ));
    }

    /**
     * Handles the CASE clauses asynchronously.
     *
     * @param caseClauses   The list of case clauses.
     * @param defaultClause The optional default clause.
     * @param switchValue   The evaluated switch value to match against.
     * @param index         The current index of the case being processed.
     * @param listener      The ActionListener to handle asynchronous callbacks.
     */
    private void handleCasesAsync(List<ElasticScriptParser.Case_clauseContext> caseClauses,
                                  ElasticScriptParser.Default_clauseContext defaultClause,
                                  Object switchValue, int index, ActionListener<Object> listener) {
        if (index >= caseClauses.size()) {
            // No more cases, check for default
            if (defaultClause != null && defaultClause.statement() != null && !defaultClause.statement().isEmpty()) {
                executeStatementsAsync(defaultClause.statement(), 0, listener);
            } else {
                // No default, switch completes without execution
                listener.onResponse(null);
            }
            return;
        }

        ElasticScriptParser.Case_clauseContext caseClause = caseClauses.get(index);

        // Evaluate the case expression
        executor.evaluateExpressionAsync(caseClause.expression(), ActionListener.wrap(
            caseValue -> {
                // Check if switch value matches case value
                if (valuesEqual(switchValue, caseValue)) {
                    // Match found, execute case statements
                    if (caseClause.statement() != null && !caseClause.statement().isEmpty()) {
                        executeStatementsAsync(caseClause.statement(), 0, listener);
                    } else {
                        listener.onResponse(null);
                    }
                } else {
                    // No match, proceed to next case
                    handleCasesAsync(caseClauses, defaultClause, switchValue, index + 1, listener);
                }
            },
            listener::onFailure
        ));
    }

    /**
     * Executes a list of statements asynchronously.
     *
     * @param stmtCtxList The list of StatementContext representing the statements to execute.
     * @param index       The current index of the statement being executed.
     * @param listener    The ActionListener to handle asynchronous callbacks.
     */
    private void executeStatementsAsync(List<ElasticScriptParser.StatementContext> stmtCtxList, int index,
                                        ActionListener<Object> listener) {
        if (index >= stmtCtxList.size()) {
            listener.onResponse(null); // All statements executed
            return;
        }

        ElasticScriptParser.StatementContext stmtCtx = stmtCtxList.get(index);

        ActionListener<Object> stmtExecutionListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object o) {
                if (o instanceof ReturnValue) {
                    listener.onResponse(o);
                } else {
                    executeStatementsAsync(stmtCtxList, index + 1, listener);
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (e instanceof ReturnValue) {
                    listener.onFailure(e);
                } else if (e instanceof BreakException) {
                    // BREAK in switch means exit the switch (not propagate)
                    listener.onResponse(null);
                } else {
                    listener.onFailure(e);
                }
            }
        };

        ActionListener<Object> stmtExecutionLogger = ActionListenerUtils.withLogging(stmtExecutionListener,
            this.getClass().getName(),
            "Switch-Case-Statement-Execution: " + stmtCtx.getText());

        // Visit the statement asynchronously
        executor.visitStatementAsync(stmtCtx, stmtExecutionLogger);
    }

    /**
     * Compares two values for equality, handling type coercion for numbers.
     */
    private boolean valuesEqual(Object a, Object b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        // Handle numeric comparison with type coercion
        if (a instanceof Number && b instanceof Number) {
            return ((Number) a).doubleValue() == ((Number) b).doubleValue();
        }
        return a.equals(b);
    }
}


