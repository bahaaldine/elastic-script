/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;

import java.util.List;

/**
 * Represents a user-defined function stored in Elasticsearch.
 * 
 * Stored functions are created with CREATE FUNCTION and persist in the
 * .elastic_script_functions index. They differ from inline functions in that:
 * 
 * <ul>
 *   <li>They have an explicit RETURNS clause declaring the return type</li>
 *   <li>They are stored persistently and can be called across sessions</li>
 *   <li>They can be used directly in expressions</li>
 * </ul>
 * 
 * Example:
 * <pre>
 * CREATE FUNCTION calculate_score(x NUMBER, y NUMBER) RETURNS NUMBER AS
 * BEGIN
 *     RETURN x * 10 + y;
 * END FUNCTION
 * </pre>
 */
public class StoredFunctionDefinition extends FunctionDefinition {
    
    private final String returnType;
    
    /**
     * Creates a stored function definition.
     *
     * @param name       The function name
     * @param parameters The function parameters
     * @param body       The function body statements
     * @param returnType The declared return type (e.g., "NUMBER", "STRING")
     */
    public StoredFunctionDefinition(String name, List<Parameter> parameters,
                                    List<ElasticScriptParser.StatementContext> body,
                                    String returnType) {
        super(name, parameters, body);
        this.returnType = returnType != null ? returnType.toUpperCase() : "ANY";
    }
    
    /**
     * Returns the declared return type of this function.
     *
     * @return The return type (e.g., "NUMBER", "STRING", "DOCUMENT")
     */
    public String getReturnType() {
        return returnType;
    }
    
    @Override
    public void execute(List<Object> args, ActionListener<Object> listener) {
        // Execution is handled by FunctionDefinitionHandler.executeFunctionAsync
        // which creates a child context and executes the body
        listener.onFailure(new UnsupportedOperationException(
            "StoredFunctionDefinition.execute should not be called directly. " +
            "Use FunctionDefinitionHandler.executeFunctionAsync instead."));
    }
    
    @Override
    public String toString() {
        return "StoredFunction[" + getName() + "(" + getParameters().size() + " params) RETURNS " + returnType + "]";
    }
}
