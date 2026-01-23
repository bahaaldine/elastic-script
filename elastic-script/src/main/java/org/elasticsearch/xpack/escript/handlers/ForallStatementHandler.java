/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.evaluators.ExpressionEvaluator;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for FORALL statement - bulk operations over collections.
 *
 * Syntax:
 *   FORALL element IN collection action [SAVE EXCEPTIONS];
 *
 * Where action can be:
 *   - CALL procedure(element, ...)
 *   - function_call(element, ...)
 *
 * With SAVE EXCEPTIONS, errors are collected in @bulk_errors instead of
 * stopping execution on first failure.
 *
 * Example:
 *   FORALL doc IN documents
 *       CALL process_document(doc)
 *       SAVE EXCEPTIONS;
 *
 *   IF ARRAY_LENGTH(@bulk_errors) > 0 THEN
 *       PRINT 'Errors occurred: ' || ARRAY_LENGTH(@bulk_errors);
 *   END IF;
 */
public class ForallStatementHandler {

    private final ProcedureExecutor executor;
    private final ExpressionEvaluator evaluator;

    public ForallStatementHandler(ProcedureExecutor executor) {
        this.executor = executor;
        this.evaluator = new ExpressionEvaluator(executor);
    }

    /**
     * Handle FORALL statement asynchronously.
     */
    public void handleAsync(ElasticScriptParser.Forall_statementContext ctx,
                            ActionListener<Object> listener) {
        String elementVar = ctx.ID().getText();
        boolean saveExceptions = ctx.save_exceptions_clause() != null;

        ExecutionContext context = executor.getContext();

        // Evaluate the collection expression
        evaluator.evaluateExpressionAsync(ctx.expression(), ActionListener.wrap(
            collectionValue -> {
                if (!(collectionValue instanceof List)) {
                    listener.onFailure(new RuntimeException(
                        "FORALL requires an ARRAY, got: " +
                        (collectionValue != null ? collectionValue.getClass().getSimpleName() : "null")));
                    return;
                }

                @SuppressWarnings("unchecked")
                List<Object> collection = (List<Object>) collectionValue;

                if (collection.isEmpty()) {
                    // Empty collection - nothing to do
                    if (saveExceptions) {
                        context.declareVariable("bulk_errors", "ARRAY");
                        context.setVariable("bulk_errors", new ArrayList<>());
                    }
                    listener.onResponse(null);
                    return;
                }

                // Initialize bulk errors tracking if SAVE EXCEPTIONS
                List<Map<String, Object>> bulkErrors = new ArrayList<>();
                if (saveExceptions) {
                    context.declareVariable("bulk_errors", "ARRAY");
                    context.setVariable("bulk_errors", bulkErrors);
                }

                // Process elements sequentially
                processElementsAsync(ctx, elementVar, collection, 0, bulkErrors, saveExceptions, listener);
            },
            listener::onFailure
        ));
    }

    /**
     * Process collection elements sequentially.
     */
    private void processElementsAsync(ElasticScriptParser.Forall_statementContext ctx,
                                       String elementVar,
                                       List<Object> collection,
                                       int index,
                                       List<Map<String, Object>> bulkErrors,
                                       boolean saveExceptions,
                                       ActionListener<Object> listener) {
        if (index >= collection.size()) {
            // All elements processed
            listener.onResponse(null);
            return;
        }

        Object element = collection.get(index);
        ExecutionContext context = executor.getContext();

        // Bind the element variable
        if (!context.hasVariable(elementVar)) {
            context.declareVariable(elementVar, "ANY");
        }
        context.setVariable(elementVar, element);

        // Execute the action
        executeActionAsync(ctx, ActionListener.wrap(
            result -> {
                // Success - continue to next element
                processElementsAsync(ctx, elementVar, collection, index + 1,
                    bulkErrors, saveExceptions, listener);
            },
            error -> {
                if (saveExceptions) {
                    // Record error and continue
                    Map<String, Object> errorRecord = new HashMap<>();
                    errorRecord.put("index", index);
                    errorRecord.put("element", element);
                    errorRecord.put("error", error.getMessage());
                    errorRecord.put("type", error.getClass().getSimpleName());
                    bulkErrors.add(errorRecord);

                    // Update bulk_errors
                    context.setVariable("bulk_errors", bulkErrors);

                    // Continue to next element
                    processElementsAsync(ctx, elementVar, collection, index + 1,
                        bulkErrors, saveExceptions, listener);
                } else {
                    // Fail immediately
                    listener.onFailure(new RuntimeException(
                        "FORALL failed at index " + index + ": " + error.getMessage(), error));
                }
            }
        ));
    }

    /**
     * Execute the FORALL action (procedure call or function call).
     */
    private void executeActionAsync(ElasticScriptParser.Forall_statementContext ctx,
                                     ActionListener<Object> listener) {
        ElasticScriptParser.Forall_actionContext actionCtx = ctx.forall_action();

        if (actionCtx.call_procedure_statement() != null) {
            // CALL procedure(...)
            executor.visitCallProcedureAsync(actionCtx.call_procedure_statement(), listener);
        } else if (actionCtx.function_call() != null) {
            // function_call(...)
            executor.visitFunctionCallAsync(actionCtx.function_call(), listener);
        } else {
            listener.onFailure(new RuntimeException("Invalid FORALL action"));
        }
    }
}
