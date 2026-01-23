/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;
import org.elasticsearch.xpack.escript.context.ExecutionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handler for EXECUTE IMMEDIATE statements.
 * 
 * EXECUTE IMMEDIATE allows building and executing ES|QL queries dynamically at runtime.
 * This is similar to PL/SQL's EXECUTE IMMEDIATE for dynamic SQL.
 * 
 * <h2>Syntax</h2>
 * <pre>
 * EXECUTE IMMEDIATE query_expression [INTO var1, var2, ...] [USING bind1, bind2, ...];
 * </pre>
 * 
 * <h2>Examples</h2>
 * <pre>
 * -- Simple dynamic query
 * DECLARE query STRING = 'FROM logs-* | LIMIT 10';
 * EXECUTE IMMEDIATE query INTO results;
 * 
 * -- Building query dynamically
 * DECLARE index_name STRING = 'logs-' || CURRENT_DATE;
 * EXECUTE IMMEDIATE 'FROM ' || index_name || ' | WHERE level = "ERROR"' INTO errors;
 * 
 * -- Using bind variables (positional :1, :2, etc.)
 * EXECUTE IMMEDIATE 'FROM logs-* | WHERE status = :1 AND level = :2' 
 *     INTO results 
 *     USING 500, 'ERROR';
 * </pre>
 * 
 * <h2>Bind Variables</h2>
 * Bind variables use positional syntax (:1, :2, :3, etc.) and are replaced with
 * values from the USING clause before execution. String values are automatically quoted.
 */
public class ExecuteImmediateStatementHandler {

    private final ProcedureExecutor executor;
    
    // Pattern to match bind variables like :1, :2, :3, etc.
    private static final Pattern BIND_VAR_PATTERN = Pattern.compile(":(\\d+)");

    public ExecuteImmediateStatementHandler(ProcedureExecutor executor) {
        this.executor = executor;
    }

    /**
     * Handles the EXECUTE IMMEDIATE statement asynchronously.
     *
     * @param ctx      The Execute_immediate_statementContext from the parser
     * @param listener The ActionListener for async completion
     */
    public void handleAsync(ElasticScriptParser.Execute_immediate_statementContext ctx, 
                           ActionListener<Object> listener) {
        try {
            // Step 1: Evaluate the query expression
            executor.evaluateExpressionAsync(ctx.expression(), ActionListener.wrap(
                queryExprResult -> {
                    try {
                        String queryString = String.valueOf(queryExprResult);
                        
                        // Step 2: Handle USING clause if present (bind variables)
                        if (ctx.expression_list() != null) {
                            evaluateBindVariables(ctx.expression_list(), bindValues -> {
                                try {
                                    String substitutedQuery = substituteBindVariables(queryString, bindValues);
                                    executeQuery(substitutedQuery, ctx, listener);
                                } catch (Exception e) {
                                    listener.onFailure(e);
                                }
                            }, listener::onFailure);
                        } else {
                            executeQuery(queryString, ctx, listener);
                        }
                    } catch (Exception e) {
                        listener.onFailure(e);
                    }
                },
                listener::onFailure
            ));
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    /**
     * Evaluates all expressions in the USING clause.
     */
    private void evaluateBindVariables(ElasticScriptParser.Expression_listContext exprListCtx,
                                       java.util.function.Consumer<List<Object>> onSuccess,
                                       java.util.function.Consumer<Exception> onFailure) {
        List<ElasticScriptParser.ExpressionContext> expressions = exprListCtx.expression();
        List<Object> bindValues = new ArrayList<>();
        
        evaluateBindVariablesRecursive(expressions, 0, bindValues, onSuccess, onFailure);
    }

    private void evaluateBindVariablesRecursive(List<ElasticScriptParser.ExpressionContext> expressions,
                                                int index,
                                                List<Object> bindValues,
                                                java.util.function.Consumer<List<Object>> onSuccess,
                                                java.util.function.Consumer<Exception> onFailure) {
        if (index >= expressions.size()) {
            onSuccess.accept(bindValues);
            return;
        }
        
        executor.evaluateExpressionAsync(expressions.get(index), ActionListener.wrap(
            value -> {
                bindValues.add(value);
                evaluateBindVariablesRecursive(expressions, index + 1, bindValues, onSuccess, onFailure);
            },
            e -> onFailure.accept(e)
        ));
    }

    /**
     * Substitutes bind variables (:1, :2, etc.) with actual values.
     */
    private String substituteBindVariables(String query, List<Object> bindValues) {
        Matcher matcher = BIND_VAR_PATTERN.matcher(query);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            if (index < 1 || index > bindValues.size()) {
                throw new RuntimeException("Bind variable :" + index + " not provided. " +
                    "Expected " + bindValues.size() + " bind values.");
            }
            
            Object value = bindValues.get(index - 1); // :1 is index 0
            String replacement = formatBindValue(value);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    /**
     * Formats a bind value for insertion into the query.
     * Strings are quoted, numbers and booleans are used as-is.
     */
    private String formatBindValue(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            // Escape any quotes in the string and wrap in quotes
            String escaped = ((String) value).replace("\"", "\\\"");
            return "\"" + escaped + "\"";
        } else if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        } else {
            // For other types, convert to string and quote
            return "\"" + String.valueOf(value).replace("\"", "\\\"") + "\"";
        }
    }

    /**
     * Executes the ES|QL query and handles the INTO clause if present.
     */
    private void executeQuery(String query, 
                             ElasticScriptParser.Execute_immediate_statementContext ctx,
                             ActionListener<Object> listener) {
        // Execute the query using ProcedureExecutor's ESQL capability
        executor.executeEsqlQueryAsync(query, ActionListener.wrap(
            result -> {
                try {
                    // Handle INTO clause if present
                    if (ctx.id_list() != null) {
                        handleIntoClause(ctx.id_list(), result, listener);
                    } else {
                        // No INTO clause, just complete successfully
                        listener.onResponse(result);
                    }
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            },
            listener::onFailure
        ));
    }

    /**
     * Handles the INTO clause by assigning query results to variables.
     */
    @SuppressWarnings("unchecked")
    private void handleIntoClause(ElasticScriptParser.Id_listContext idListCtx,
                                  Object result,
                                  ActionListener<Object> listener) {
        List<String> varNames = new ArrayList<>();
        for (var id : idListCtx.ID()) {
            varNames.add(id.getText());
        }
        
        ExecutionContext context = executor.getContext();
        
        if (varNames.size() == 1) {
            // Single variable - assign the entire result
            String varName = varNames.get(0);
            ensureVariableDeclared(context, varName, result);
            context.setVariable(varName, result);
        } else if (result instanceof List) {
            // Multiple variables - try to assign from first row
            List<?> rows = (List<?>) result;
            if (rows.isEmpty()) {
                // No data - set all to null
                for (String varName : varNames) {
                    ensureVariableDeclared(context, varName, null);
                    context.setVariable(varName, null);
                }
            } else {
                Object firstRow = rows.get(0);
                if (firstRow instanceof Map) {
                    // Row is a map - assign by position from values
                    Map<String, Object> rowMap = (Map<String, Object>) firstRow;
                    List<Object> values = new ArrayList<>(rowMap.values());
                    
                    for (int i = 0; i < varNames.size(); i++) {
                        String varName = varNames.get(i);
                        Object value = (i < values.size()) ? values.get(i) : null;
                        ensureVariableDeclared(context, varName, value);
                        context.setVariable(varName, value);
                    }
                } else {
                    // Single value in first row
                    if (varNames.size() > 1) {
                        listener.onFailure(new RuntimeException(
                            "INTO clause has " + varNames.size() + " variables but query returned a single value"));
                        return;
                    }
                    ensureVariableDeclared(context, varNames.get(0), firstRow);
                    context.setVariable(varNames.get(0), firstRow);
                }
            }
        } else {
            // Single value result with multiple INTO variables
            if (varNames.size() > 1) {
                listener.onFailure(new RuntimeException(
                    "INTO clause has " + varNames.size() + " variables but query returned a single value"));
                return;
            }
            ensureVariableDeclared(context, varNames.get(0), result);
            context.setVariable(varNames.get(0), result);
        }
        
        listener.onResponse(result);
    }

    /**
     * Ensures a variable is declared before assignment.
     */
    private void ensureVariableDeclared(ExecutionContext context, String varName, Object value) {
        if (!context.hasVariable(varName)) {
            // Auto-declare with inferred type
            String type = inferType(value);
            context.declareVariable(varName, type);
        }
    }

    private String inferType(Object value) {
        if (value == null) return "ANY";
        if (value instanceof Number) return "NUMBER";
        if (value instanceof String) return "STRING";
        if (value instanceof Boolean) return "BOOLEAN";
        if (value instanceof List) return "ARRAY";
        if (value instanceof Map) return "DOCUMENT";
        return "ANY";
    }
}
