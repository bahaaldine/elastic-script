/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;

import java.util.List;
import java.util.Map;

/**
 * Handler for BULK COLLECT statement - collect ES|QL query results into an array.
 *
 * Syntax:
 *   BULK COLLECT INTO variable FROM esql_query;
 *
 * This is similar to DECLARE ... ARRAY FROM ... but with explicit BULK COLLECT semantics
 * for clarity when working with large result sets.
 *
 * Example:
 *   BULK COLLECT INTO all_errors
 *       FROM logs-*
 *       | WHERE level = 'ERROR'
 *       | LIMIT 5000;
 *
 *   PRINT 'Collected ' || ARRAY_LENGTH(all_errors) || ' errors';
 */
public class BulkCollectHandler {

    private final ProcedureExecutor executor;

    public BulkCollectHandler(ProcedureExecutor executor) {
        this.executor = executor;
    }

    /**
     * Handle BULK COLLECT statement asynchronously.
     */
    public void handleAsync(ElasticScriptParser.Bulk_collect_statementContext ctx,
                            ActionListener<Object> listener) {
        String targetVar = ctx.ID().getText();

        // Get the ES|QL query
        ElasticScriptParser.Esql_binding_queryContext queryCtx = ctx.esql_binding_query();
        if (queryCtx == null) {
            listener.onFailure(new RuntimeException("BULK COLLECT requires an ES|QL query"));
            return;
        }

        // Get raw query text preserving whitespace
        String esqlQuery = executor.getRawText(queryCtx).trim();

        ExecutionContext context = executor.getContext();

        // Execute the ES|QL query
        executor.executeEsqlQueryAsync(esqlQuery, ActionListener.wrap(
            results -> {
                try {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> rows = (List<Map<String, Object>>) results;

                    // Declare or set the target variable
                    if (!context.hasVariable(targetVar)) {
                        context.declareVariable(targetVar, "ARRAY");
                    }
                    context.setVariable(targetVar, rows);

                    listener.onResponse(null);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException(
                        "BULK COLLECT failed: " + e.getMessage(), e));
                }
            },
            e -> listener.onFailure(new RuntimeException(
                "BULK COLLECT query failed: " + e.getMessage(), e))
        ));
    }
}
