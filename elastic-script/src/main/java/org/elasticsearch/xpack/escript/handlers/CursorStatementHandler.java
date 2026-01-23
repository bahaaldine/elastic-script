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
import org.elasticsearch.xpack.escript.primitives.CursorDefinition;

import java.util.List;
import java.util.Map;

/**
 * Handler for cursor operations: OPEN, CLOSE, and FETCH.
 * 
 * Cursor lifecycle:
 * 1. DECLARE cursor CURSOR FOR query;  -- declares cursor (handled by DeclareStatementHandler)
 * 2. OPEN cursor;                       -- executes query, makes results available
 * 3. FETCH cursor INTO variable;        -- retrieves next row(s)
 * 4. CLOSE cursor;                      -- releases resources
 * 
 * Cursor attributes:
 * - cursor%NOTFOUND - true when last FETCH returned no rows
 * - cursor%ROWCOUNT - number of rows fetched so far
 */
public class CursorStatementHandler {

    private final ProcedureExecutor executor;
    private final ExpressionEvaluator evaluator;

    public CursorStatementHandler(ProcedureExecutor executor) {
        this.executor = executor;
        this.evaluator = new ExpressionEvaluator(executor);
    }

    /**
     * Handle OPEN cursor;
     * 
     * Executes the cursor's ES|QL query and makes results available for fetching.
     */
    public void handleOpenAsync(ElasticScriptParser.Open_cursor_statementContext ctx, 
                                 ActionListener<Object> listener) {
        String cursorName = ctx.ID().getText();
        ExecutionContext context = executor.getContext();
        
        try {
            CursorDefinition cursor = context.getCursor(cursorName);
            
            if (cursor == null) {
                listener.onFailure(new RuntimeException(
                    "Cursor '" + cursorName + "' not found. Did you declare it?"));
                return;
            }
            
            if (cursor.isOpen()) {
                listener.onFailure(new RuntimeException(
                    "Cursor '" + cursorName + "' is already open. Close it first."));
                return;
            }
            
            // Execute the ES|QL query
            String esqlQuery = cursor.getEsqlQuery();
            executor.executeEsqlQueryAsync(esqlQuery, ActionListener.wrap(
                results -> {
                    try {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> rows = (List<Map<String, Object>>) results;
                        cursor.openWithResults(rows);
                        listener.onResponse(null);
                    } catch (Exception e) {
                        listener.onFailure(new RuntimeException(
                            "Failed to open cursor '" + cursorName + "': " + e.getMessage(), e));
                    }
                },
                e -> listener.onFailure(new RuntimeException(
                    "Failed to execute cursor query for '" + cursorName + "': " + e.getMessage(), e))
            ));
            
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    /**
     * Handle CLOSE cursor;
     * 
     * Closes the cursor and releases resources.
     */
    public void handleCloseAsync(ElasticScriptParser.Close_cursor_statementContext ctx,
                                  ActionListener<Object> listener) {
        String cursorName = ctx.ID().getText();
        ExecutionContext context = executor.getContext();
        
        try {
            CursorDefinition cursor = context.getCursor(cursorName);
            
            if (cursor == null) {
                listener.onFailure(new RuntimeException(
                    "Cursor '" + cursorName + "' not found. Did you declare it?"));
                return;
            }
            
            if (!cursor.isOpen()) {
                // Silently ignore closing a closed cursor (common pattern)
                listener.onResponse(null);
                return;
            }
            
            cursor.close();
            listener.onResponse(null);
            
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    /**
     * Handle FETCH cursor INTO variable;
     * or    FETCH cursor LIMIT n INTO variable;
     * 
     * Fetches the next row(s) from the cursor into the target variable.
     */
    public void handleFetchAsync(ElasticScriptParser.Fetch_cursor_statementContext ctx,
                                  ActionListener<Object> listener) {
        // Get cursor name (first ID)
        String cursorName = ctx.ID(0).getText();
        // Get target variable name (second ID, which is after INTO)
        String targetVarName = ctx.ID(1).getText();
        
        ExecutionContext context = executor.getContext();
        
        try {
            CursorDefinition cursor = context.getCursor(cursorName);
            
            if (cursor == null) {
                listener.onFailure(new RuntimeException(
                    "Cursor '" + cursorName + "' not found. Did you declare it?"));
                return;
            }
            
            if (!cursor.isOpen()) {
                listener.onFailure(new RuntimeException(
                    "Cursor '" + cursorName + "' is not open. Use OPEN " + cursorName + "; first."));
                return;
            }
            
            // Check if this is a batch fetch (FETCH cursor LIMIT n INTO var)
            if (ctx.LIMIT() != null && ctx.expression() != null) {
                // Batch fetch - evaluate the limit expression
                evaluator.evaluateExpressionAsync(ctx.expression(), ActionListener.wrap(
                    limitValue -> {
                        try {
                            int limit = ((Number) limitValue).intValue();
                            List<Map<String, Object>> rows = cursor.fetchBatch(limit);
                            
                            // Declare or set the target variable
                            if (!context.hasVariable(targetVarName)) {
                                context.declareVariable(targetVarName, "ARRAY");
                            }
                            context.setVariable(targetVarName, rows);
                            
                            listener.onResponse(null);
                        } catch (Exception e) {
                            listener.onFailure(new RuntimeException(
                                "Failed to fetch from cursor '" + cursorName + "': " + e.getMessage(), e));
                        }
                    },
                    listener::onFailure
                ));
            } else {
                // Single row fetch
                Map<String, Object> row = cursor.fetchNext();
                
                // Declare or set the target variable
                if (!context.hasVariable(targetVarName)) {
                    context.declareVariable(targetVarName, "DOCUMENT");
                }
                context.setVariable(targetVarName, row);
                
                listener.onResponse(null);
            }
            
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
}
