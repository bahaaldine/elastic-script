/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xcontent.DeprecationHandler;
import org.elasticsearch.xcontent.NamedXContentRegistry;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;
import org.elasticsearch.xpack.escript.primitives.ElasticScriptDataType;
import org.elasticsearch.xpack.escript.utils.ActionListenerUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class DeclareStatementHandler {
    private final ProcedureExecutor executor;

    public DeclareStatementHandler(ProcedureExecutor executor) {
        this.executor = executor;
    }

    /**
     * Checks whether the provided variable type string represents a supported data type.
     * Supported types include NUMBER, STRING, DATE, and ARRAY.
     * For ARRAY types, the syntax "ARRAY" or "ARRAY OF elementType" is allowed.
     * This method also handles cases where the input may lack the expected spaces (e.g. "ARRAYOFSTRING").
     *
     * @param varType the variable type string (e.g. "NUMBER", "ARRAY", "ARRAY OF STRING")
     * @return true if the type is supported, false otherwise.
     */
    private boolean isSupportedDataType(String varType) {
        // Normalize and trim the type string.
        String normalizedType = varType.trim().toUpperCase();
        // If the type starts with "ARRAY", check for the "OF" clause.
        if (normalizedType.startsWith("ARRAY")) {
            // Just "ARRAY" without element type is valid (untyped array)
            if (normalizedType.equals("ARRAY")) {
                return true;
            }
            // If the token comes in as "ARRAYOF..." without a space, insert the space.
            if (normalizedType.startsWith("ARRAYOF")) {
                normalizedType = "ARRAY OF " + normalizedType.substring("ARRAYOF".length());
                String elementType = normalizedType.substring("ARRAY OF ".length()).trim();
                try {
                    // Verify that the element type is a valid base type.
                    ElasticScriptDataType.valueOf(elementType);
                    return true;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }
            // Handle "ARRAY OF <type>" with space
            if (normalizedType.contains(" OF ")) {
                String elementType = normalizedType.substring(normalizedType.indexOf(" OF ") + 4).trim();
                try {
                    ElasticScriptDataType.valueOf(elementType);
                    return true;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }
            return true; // Default: accept ARRAY types
        } else {
            try {
                ElasticScriptDataType type = ElasticScriptDataType.valueOf(normalizedType);
                // For non-array types, allow INT, FLOAT, NUMBER, STRING, DATE, BOOLEAN, and DOCUMENT.
                return ( type == ElasticScriptDataType.INT
                    || type == ElasticScriptDataType.FLOAT
                    || type == ElasticScriptDataType.NUMBER
                    || type == ElasticScriptDataType.STRING
                    || type == ElasticScriptDataType.DATE
                    || type == ElasticScriptDataType.BOOLEAN
                    || type == ElasticScriptDataType.DOCUMENT);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }

    public void handleAsync(ElasticScriptParser.Declare_statementContext ctx, ActionListener<Object> listener) {
        // Check if this is a cursor declaration (DECLARE name CURSOR FOR query)
        if (ctx.CURSOR() != null) {
            handleCursorDeclaration(ctx, listener);
            return;
        }
        
        // Check if this is a type-aware ES|QL binding (DECLARE name TYPE FROM esql_query)
        if (ctx.esql_binding_type() != null) {
            handleEsqlBinding(ctx, listener);
            return;
        }
        
        // Otherwise, it's a regular variable declaration
        List<ElasticScriptParser.Variable_declarationContext> varDecls =
            ctx.variable_declaration_list().variable_declaration();
        processVariableDeclarations(varDecls, 0, listener);
    }

    /**
     * Handles cursor declaration: DECLARE name CURSOR FOR query;
     */
    private void handleCursorDeclaration(ElasticScriptParser.Declare_statementContext ctx, ActionListener<Object> listener) {
        String cursorName = ctx.ID().getText();
        
        // Extract the ESQL query from the cursor_query context
        ElasticScriptParser.Cursor_queryContext queryCtx = ctx.cursor_query();
        if (queryCtx == null || queryCtx.cursor_query_content() == null) {
            listener.onFailure(new RuntimeException("Cursor '" + cursorName + "' must have a query defined"));
            return;
        }
        
        // Get the query text from the cursor_query_content using getRawText to preserve whitespace
        // getText() concatenates tokens without spaces which breaks ESQL parsing
        String esqlQuery = executor.getRawText(queryCtx.cursor_query_content()).trim();
        
        try {
            executor.getContext().declareCursor(cursorName, esqlQuery);
            listener.onResponse(null);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    /**
     * Handles type-aware ES|QL binding: DECLARE name TYPE FROM esql_query;
     * 
     * Supported binding types:
     * - ARRAY: Captures all rows as an array of documents
     * - DOCUMENT: Single row as a document (validates row count = 1)
     * - NUMBER/STRING/DATE/BOOLEAN: Scalar value (validates single row, single column)
     */
    @SuppressWarnings("unchecked")
    private void handleEsqlBinding(ElasticScriptParser.Declare_statementContext ctx, ActionListener<Object> listener) {
        String varName = ctx.ID().getText();
        String bindingType = ctx.esql_binding_type().getText().toUpperCase();
        
        // Extract the ESQL query
        ElasticScriptParser.Esql_binding_queryContext queryCtx = ctx.esql_binding_query();
        if (queryCtx == null || queryCtx.esql_binding_content() == null) {
            listener.onFailure(new RuntimeException("Variable '" + varName + "' must have an ES|QL query defined"));
            return;
        }
        
        String esqlQuery = executor.getRawText(queryCtx.esql_binding_content()).trim();
        
        // Declare the variable with appropriate type
        try {
            if ("ARRAY".equals(bindingType)) {
                executor.getContext().declareVariable(varName, "ARRAY", "DOCUMENT");
            } else {
                executor.getContext().declareVariable(varName, bindingType);
            }
        } catch (Exception e) {
            listener.onFailure(e);
            return;
        }
        
        // Execute the ES|QL query and bind the result
        executor.executeEsqlQueryAsync(esqlQuery, ActionListener.wrap(
            result -> {
                try {
                    if (!(result instanceof List)) {
                        listener.onFailure(new RuntimeException(
                            "ES|QL query for '" + varName + "' did not return a result set"));
                        return;
                    }
                    
                    List<Map<String, Object>> rows = (List<Map<String, Object>>) result;
                    Object value = extractTypedValue(varName, bindingType, rows);
                    executor.getContext().setVariable(varName, value);
                    listener.onResponse(null);
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            },
            listener::onFailure
        ));
    }

    /**
     * Extracts a typed value from ES|QL query results based on the binding type.
     * 
     * @param varName The variable name (for error messages)
     * @param bindingType The declared type (ARRAY, DOCUMENT, NUMBER, STRING, DATE, BOOLEAN)
     * @param rows The query results
     * @return The extracted value
     */
    private Object extractTypedValue(String varName, String bindingType, List<Map<String, Object>> rows) {
        switch (bindingType) {
            case "ARRAY":
                // Return all rows as an array of documents
                return rows;
                
            case "DOCUMENT":
                // Validate single row, return as document
                if (rows.isEmpty()) {
                    throw new RuntimeException(
                        "ES|QL query for DOCUMENT variable '" + varName + "' returned no rows (expected exactly 1)");
                }
                if (rows.size() > 1) {
                    throw new RuntimeException(
                        "ES|QL query for DOCUMENT variable '" + varName + "' returned " + rows.size() + 
                        " rows (expected exactly 1). Use ARRAY type to capture multiple rows.");
                }
                return rows.get(0);
                
            case "NUMBER":
            case "STRING":
            case "DATE":
            case "BOOLEAN":
                // Validate single row, single column, extract scalar
                return extractScalar(varName, bindingType, rows);
                
            default:
                throw new RuntimeException("Unsupported ES|QL binding type: " + bindingType);
        }
    }

    /**
     * Extracts a scalar value from single-row, single-column query results.
     */
    private Object extractScalar(String varName, String bindingType, List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            throw new RuntimeException(
                "ES|QL query for " + bindingType + " variable '" + varName + "' returned no rows (expected exactly 1)");
        }
        if (rows.size() > 1) {
            throw new RuntimeException(
                "ES|QL query for " + bindingType + " variable '" + varName + "' returned " + rows.size() + 
                " rows (expected exactly 1). Use ARRAY type to capture multiple rows.");
        }
        
        Map<String, Object> row = rows.get(0);
        
        if (row.isEmpty()) {
            throw new RuntimeException(
                "ES|QL query for " + bindingType + " variable '" + varName + "' returned no columns");
        }
        if (row.size() > 1) {
            throw new RuntimeException(
                "ES|QL query for " + bindingType + " variable '" + varName + "' returned " + row.size() + 
                " columns (expected exactly 1). Use DOCUMENT type to capture multiple columns.");
        }
        
        // Extract the single value
        Object value = row.values().iterator().next();
        
        // Type validation and conversion
        switch (bindingType) {
            case "NUMBER":
                if (value == null) return null;
                if (value instanceof Number) return value;
                try {
                    return Double.parseDouble(value.toString());
                } catch (NumberFormatException e) {
                    throw new RuntimeException(
                        "Cannot convert value '" + value + "' to NUMBER for variable '" + varName + "'");
                }
                
            case "STRING":
                if (value == null) return null;
                return value.toString();
                
            case "BOOLEAN":
                if (value == null) return null;
                if (value instanceof Boolean) return value;
                String strVal = value.toString().toLowerCase();
                if ("true".equals(strVal)) return true;
                if ("false".equals(strVal)) return false;
                throw new RuntimeException(
                    "Cannot convert value '" + value + "' to BOOLEAN for variable '" + varName + "'");
                
            case "DATE":
                // Return as-is for now (dates are typically ISO strings from ES|QL)
                return value;
                
            default:
                return value;
        }
    }

    private void processVariableDeclarations(List<ElasticScriptParser.Variable_declarationContext> varDecls,
                                             int index, ActionListener<Object> listener) {
        if (index >= varDecls.size()) {
            listener.onResponse(null); // All declarations succeeded
            return;
        }

        ElasticScriptParser.Variable_declarationContext varCtx = varDecls.get(index);
        String varName = varCtx.ID().getText();
        String varType = varCtx.datatype().getText();
        String elementType = null;

        // Check if the datatype is supported.
        if ( isSupportedDataType(varType) == false ) {
            listener.onFailure(new RuntimeException("Unsupported data type: " + varType + " + for variable " + varName));
            return;
        }

        // If the type indicates an array, try to extract the element type.
        if (varType.toUpperCase().startsWith("ARRAY")) {
            // For a declaration like "ARRAY OF NUMBER", split on "OF" (case-insensitive)
            String[] parts = varType.split("(?i)OF");
            if (parts.length == 2) {
                elementType = parts[1].trim();
            } else {
                // Optionally default to ANY if no element type is specified.
                elementType = "ANY";
            }
        }

        // Now, declare the variable.
        try {
            // Assume ExecutionContext now has declareVariable(String, String, String)
            if (elementType != null) {
                executor.getContext().declareVariable(varName, varType, elementType);
            } else {
                executor.getContext().declareVariable(varName, varType);
            }
        } catch (Exception e) {
            listener.onFailure(e);
            return;
        }

        ActionListener<Object> variableDeclarationListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object value) {
                executor.getContext().setVariable(varName, value);
                processVariableDeclarations(varDecls, index + 1, listener);
            }

            @Override
            public void onFailure(Exception e) {
                if ("Null expression context".equals(e.getMessage())) {
                    executor.getContext().setVariable(varName, null);
                    processVariableDeclarations(varDecls, index + 1, listener);
                } else {
                    listener.onFailure(e);
                }
            }
        };

        ActionListener<Object> variableDeclarationLogger =
            ActionListenerUtils.withLogging(variableDeclarationListener,
                this.getClass().getName(),
                "Variable-Declaration: " + varName + " - " + varType +
                    (elementType != null ? " (Element Type: " + elementType + ")" : ""));

        // Process the initializer expression (if any)
        if (varCtx.expression() != null) {
            // Let the expression evaluator handle all types including array literals
            // The ExpressionEvaluator properly handles array literals with single-quoted strings
            executor.evaluateExpressionAsync(varCtx.expression(), variableDeclarationLogger);
        } else {
            variableDeclarationLogger.onFailure(new RuntimeException("Null expression context"));
        }
    }
}
