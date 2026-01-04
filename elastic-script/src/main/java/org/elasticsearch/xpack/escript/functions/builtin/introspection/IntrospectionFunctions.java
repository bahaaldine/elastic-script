/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.builtin.introspection;

import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.FunctionDefinition;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;
import org.elasticsearch.xpack.escript.functions.api.FunctionCategory;
import org.elasticsearch.xpack.escript.functions.api.FunctionCollectionSpec;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Introspection functions for elastic-script.
 * 
 * These functions allow agents and users to discover what capabilities
 * are available in the current execution context:
 * - ESCRIPT_FUNCTIONS() - List all available built-in functions
 * - ESCRIPT_FUNCTION(name) - Get details about a specific function
 * - ESCRIPT_PROCEDURES() - List all stored procedures
 * - ESCRIPT_PROCEDURE(name) - Get details about a specific procedure
 */
@FunctionCollectionSpec(
    category = FunctionCategory.UTILITY,
    description = "Introspection functions for discovering available capabilities."
)
public class IntrospectionFunctions {

    /**
     * Registers all introspection functions in the given ExecutionContext.
     *
     * @param context the ExecutionContext to register functions in
     */
    public static void registerAll(ExecutionContext context) {
        registerEscriptFunctions(context);
        registerEscriptFunction(context);
        registerEscriptVariables(context);
    }

    /**
     * ESCRIPT_FUNCTIONS() - Returns an array of all registered functions with their metadata.
     * 
     * Each element in the returned array is a DOCUMENT with:
     * - name: The function name
     * - parameter_count: Number of parameters
     * - parameters: Array of parameter definitions
     * - is_builtin: Whether this is a built-in function
     */
    public static void registerEscriptFunctions(ExecutionContext context) {
        context.declareFunction("ESCRIPT_FUNCTIONS",
            List.of(), // No parameters
            new BuiltInFunctionDefinition("ESCRIPT_FUNCTIONS", (List<Object> args, org.elasticsearch.action.ActionListener<Object> listener) -> {
                try {
                    List<Map<String, Object>> result = new ArrayList<>();
                    
                    // Get all functions from the context
                    Map<String, FunctionDefinition> functions = context.getFunctions();
                    
                    for (Map.Entry<String, FunctionDefinition> entry : functions.entrySet()) {
                        String name = entry.getKey();
                        FunctionDefinition funcDef = entry.getValue();
                        
                        Map<String, Object> funcInfo = new HashMap<>();
                        funcInfo.put("name", name);
                        funcInfo.put("is_builtin", funcDef instanceof BuiltInFunctionDefinition);
                        
                        // Get parameters
                        List<Parameter> params;
                        if (funcDef instanceof BuiltInFunctionDefinition) {
                            params = ((BuiltInFunctionDefinition) funcDef).getParameters();
                        } else {
                            params = funcDef.getParameters();
                        }
                        
                        funcInfo.put("parameter_count", params != null ? params.size() : 0);
                        
                        // Build parameter details
                        List<Map<String, Object>> paramList = new ArrayList<>();
                        if (params != null) {
                            for (Parameter param : params) {
                                Map<String, Object> paramInfo = new HashMap<>();
                                paramInfo.put("name", param.getName());
                                paramInfo.put("type", param.getType());
                                paramInfo.put("mode", param.getMode().toString());
                                paramList.add(paramInfo);
                            }
                        }
                        funcInfo.put("parameters", paramList);
                        
                        result.add(funcInfo);
                    }
                    
                    // Sort by name for consistent output
                    result.sort((a, b) -> ((String) a.get("name")).compareTo((String) b.get("name")));
                    
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            })
        );
    }

    /**
     * ESCRIPT_FUNCTION(name) - Returns detailed information about a specific function.
     * 
     * Returns a DOCUMENT with:
     * - name: The function name
     * - exists: Whether the function exists
     * - is_builtin: Whether this is a built-in function
     * - parameters: Array of parameter definitions with name, type, and mode
     */
    public static void registerEscriptFunction(ExecutionContext context) {
        context.declareFunction("ESCRIPT_FUNCTION",
            List.of(new Parameter("name", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ESCRIPT_FUNCTION", (List<Object> args, org.elasticsearch.action.ActionListener<Object> listener) -> {
                try {
                    if (args.isEmpty() || args.get(0) == null) {
                        listener.onFailure(new IllegalArgumentException("ESCRIPT_FUNCTION requires a function name"));
                        return;
                    }
                    
                    String funcName = args.get(0).toString();
                    Map<String, Object> result = new HashMap<>();
                    result.put("name", funcName);
                    
                    if (!context.hasFunction(funcName)) {
                        result.put("exists", false);
                        listener.onResponse(result);
                        return;
                    }
                    
                    result.put("exists", true);
                    FunctionDefinition funcDef = context.getFunction(funcName);
                    result.put("is_builtin", funcDef instanceof BuiltInFunctionDefinition);
                    
                    // Get parameters
                    List<Parameter> params;
                    if (funcDef instanceof BuiltInFunctionDefinition) {
                        params = ((BuiltInFunctionDefinition) funcDef).getParameters();
                    } else {
                        params = funcDef.getParameters();
                    }
                    
                    result.put("parameter_count", params != null ? params.size() : 0);
                    
                    // Build parameter details
                    List<Map<String, Object>> paramList = new ArrayList<>();
                    if (params != null) {
                        for (Parameter param : params) {
                            Map<String, Object> paramInfo = new HashMap<>();
                            paramInfo.put("name", param.getName());
                            paramInfo.put("type", param.getType());
                            paramInfo.put("mode", param.getMode().toString());
                            paramList.add(paramInfo);
                        }
                    }
                    result.put("parameters", paramList);
                    
                    // Build signature string for convenience
                    StringBuilder sig = new StringBuilder();
                    sig.append(funcName).append("(");
                    if (params != null && !params.isEmpty()) {
                        for (int i = 0; i < params.size(); i++) {
                            if (i > 0) sig.append(", ");
                            Parameter p = params.get(i);
                            sig.append(p.getName()).append(" ").append(p.getType());
                        }
                    }
                    sig.append(")");
                    result.put("signature", sig.toString());
                    
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            })
        );
    }

    /**
     * ESCRIPT_VARIABLES() - Returns an array of all declared variables in scope.
     * 
     * Each element in the returned array is a DOCUMENT with:
     * - name: The variable name
     * - type: The variable type
     * - value: The current value (may be null)
     */
    public static void registerEscriptVariables(ExecutionContext context) {
        context.declareFunction("ESCRIPT_VARIABLES",
            List.of(), // No parameters
            new BuiltInFunctionDefinition("ESCRIPT_VARIABLES", (List<Object> args, org.elasticsearch.action.ActionListener<Object> listener) -> {
                try {
                    List<Map<String, Object>> result = new ArrayList<>();
                    
                    // Get all variable names (includes parent context variables)
                    for (String varName : context.getVariableNames()) {
                        Map<String, Object> varInfo = new HashMap<>();
                        varInfo.put("name", varName);
                        varInfo.put("type", context.getVariableType(varName));
                        
                        Object value = context.getVariable(varName);
                        // For complex types, we might want to truncate or summarize
                        if (value instanceof List && ((List<?>) value).size() > 10) {
                            varInfo.put("value", "ARRAY[" + ((List<?>) value).size() + " items]");
                        } else if (value instanceof Map && ((Map<?, ?>) value).size() > 10) {
                            varInfo.put("value", "DOCUMENT[" + ((Map<?, ?>) value).size() + " fields]");
                        } else {
                            varInfo.put("value", value);
                        }
                        
                        result.add(varInfo);
                    }
                    
                    // Sort by name
                    result.sort((a, b) -> ((String) a.get("name")).compareTo((String) b.get("name")));
                    
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            })
        );
    }
}

