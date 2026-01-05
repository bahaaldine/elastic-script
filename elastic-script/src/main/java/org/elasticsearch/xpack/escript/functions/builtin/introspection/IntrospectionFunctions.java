/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.builtin.introspection;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.FunctionDefinition;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;
import org.elasticsearch.xpack.escript.functions.api.FunctionCategory;
import org.elasticsearch.xpack.escript.functions.api.FunctionCollectionSpec;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;
import org.elasticsearch.xpack.escript.intent.IntentDefinition;
import org.elasticsearch.xpack.escript.intent.IntentRegistry;

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
 * - ESCRIPT_VARIABLES() - List all declared variables in scope
 */
@FunctionCollectionSpec(
    category = FunctionCategory.UTILITY,
    description = "Introspection functions for discovering available capabilities."
)
public class IntrospectionFunctions {

    private static final String PROCEDURES_INDEX = ".elastic_script_procedures";

    /**
     * Registers all introspection functions in the given ExecutionContext.
     * This version does not register procedure introspection (requires ES client).
     *
     * @param context the ExecutionContext to register functions in
     */
    public static void registerAll(ExecutionContext context) {
        registerEscriptFunctions(context);
        registerEscriptFunction(context);
        registerEscriptVariables(context);
        registerEscriptIntents(context);
        registerEscriptIntent(context);
        registerEscriptCapabilities(context, null);
    }

    /**
     * Registers all introspection functions including procedure introspection.
     *
     * @param context the ExecutionContext to register functions in
     * @param client the Elasticsearch client for procedure access
     */
    public static void registerAll(ExecutionContext context, Client client) {
        registerEscriptFunctions(context);
        registerEscriptFunction(context);
        registerEscriptVariables(context);
        registerEscriptIntents(context);
        registerEscriptIntent(context);
        registerEscriptProcedures(context, client);
        registerEscriptProcedure(context, client);
        registerEscriptCapabilities(context, client);
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
            new BuiltInFunctionDefinition("ESCRIPT_FUNCTIONS", (List<Object> args, ActionListener<Object> listener) -> {
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
            new BuiltInFunctionDefinition("ESCRIPT_FUNCTION", (List<Object> args, ActionListener<Object> listener) -> {
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
            new BuiltInFunctionDefinition("ESCRIPT_VARIABLES", (List<Object> args, ActionListener<Object> listener) -> {
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

    /**
     * ESCRIPT_PROCEDURES() - Returns an array of all stored procedures.
     * 
     * Each element in the returned array is a DOCUMENT with:
     * - name: The procedure name (document ID)
     * - parameter_count: Number of parameters
     * - parameters: Array of parameter definitions (name, type)
     */
    public static void registerEscriptProcedures(ExecutionContext context, Client client) {
        context.declareFunction("ESCRIPT_PROCEDURES",
            List.of(), // No parameters
            new BuiltInFunctionDefinition("ESCRIPT_PROCEDURES", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    SearchRequest searchRequest = new SearchRequest(PROCEDURES_INDEX);
                    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
                    sourceBuilder.query(QueryBuilders.matchAllQuery());
                    sourceBuilder.size(1000); // Reasonable limit
                    sourceBuilder.fetchSource(new String[]{"parameters"}, null); // Only fetch parameters
                    searchRequest.source(sourceBuilder);
                    
                    client.search(searchRequest, new ActionListener<>() {
                        @Override
                        public void onResponse(org.elasticsearch.action.search.SearchResponse response) {
                            try {
                                List<Map<String, Object>> result = new ArrayList<>();
                                
                                for (SearchHit hit : response.getHits().getHits()) {
                                    Map<String, Object> procInfo = new HashMap<>();
                                    procInfo.put("name", hit.getId());
                                    
                                    Map<String, Object> source = hit.getSourceAsMap();
                                    @SuppressWarnings("unchecked")
                                    List<Map<String, Object>> params = (List<Map<String, Object>>) source.get("parameters");
                                    
                                    procInfo.put("parameter_count", params != null ? params.size() : 0);
                                    procInfo.put("parameters", params != null ? params : List.of());
                                    
                                    // Build signature
                                    StringBuilder sig = new StringBuilder();
                                    sig.append(hit.getId()).append("(");
                                    if (params != null && !params.isEmpty()) {
                                        for (int i = 0; i < params.size(); i++) {
                                            if (i > 0) sig.append(", ");
                                            Map<String, Object> p = params.get(i);
                                            sig.append(p.get("name")).append(" ").append(p.get("type"));
                                        }
                                    }
                                    sig.append(")");
                                    procInfo.put("signature", sig.toString());
                                    
                                    result.add(procInfo);
                                }
                                
                                // Sort by name
                                result.sort((a, b) -> ((String) a.get("name")).compareTo((String) b.get("name")));
                                
                                listener.onResponse(result);
                            } catch (Exception e) {
                                listener.onFailure(e);
                            }
                        }
                        
                        @Override
                        public void onFailure(Exception e) {
                            // If index doesn't exist, return empty array
                            if (e.getMessage() != null && e.getMessage().contains("index_not_found")) {
                                listener.onResponse(List.of());
                            } else {
                                listener.onFailure(e);
                            }
                        }
                    });
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            })
        );
    }

    /**
     * ESCRIPT_PROCEDURE(name) - Returns detailed information about a specific stored procedure.
     * 
     * Returns a DOCUMENT with:
     * - name: The procedure name
     * - exists: Whether the procedure exists
     * - parameters: Array of parameter definitions
     * - signature: Human-readable signature string
     * - source: The procedure source code (if exists)
     */
    public static void registerEscriptProcedure(ExecutionContext context, Client client) {
        context.declareFunction("ESCRIPT_PROCEDURE",
            List.of(new Parameter("name", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ESCRIPT_PROCEDURE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    if (args.isEmpty() || args.get(0) == null) {
                        listener.onFailure(new IllegalArgumentException("ESCRIPT_PROCEDURE requires a procedure name"));
                        return;
                    }
                    
                    String procName = args.get(0).toString();
                    
                    client.prepareGet(PROCEDURES_INDEX, procName)
                        .execute(new ActionListener<>() {
                            @Override
                            public void onResponse(org.elasticsearch.action.get.GetResponse response) {
                                Map<String, Object> result = new HashMap<>();
                                result.put("name", procName);
                                
                                if (!response.isExists()) {
                                    result.put("exists", false);
                                    listener.onResponse(result);
                                    return;
                                }
                                
                                result.put("exists", true);
                                
                                Map<String, Object> source = response.getSourceAsMap();
                                
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> params = (List<Map<String, Object>>) source.get("parameters");
                                result.put("parameter_count", params != null ? params.size() : 0);
                                result.put("parameters", params != null ? params : List.of());
                                
                                // Include the procedure source
                                result.put("source", source.get("procedure"));
                                
                                // Build signature
                                StringBuilder sig = new StringBuilder();
                                sig.append(procName).append("(");
                                if (params != null && !params.isEmpty()) {
                                    for (int i = 0; i < params.size(); i++) {
                                        if (i > 0) sig.append(", ");
                                        Map<String, Object> p = params.get(i);
                                        sig.append(p.get("name")).append(" ").append(p.get("type"));
                                    }
                                }
                                sig.append(")");
                                result.put("signature", sig.toString());
                                
                                listener.onResponse(result);
                            }
                            
                            @Override
                            public void onFailure(Exception e) {
                                // If index doesn't exist, procedure doesn't exist
                                if (e.getMessage() != null && e.getMessage().contains("index_not_found")) {
                                    Map<String, Object> result = new HashMap<>();
                                    result.put("name", procName);
                                    result.put("exists", false);
                                    listener.onResponse(result);
                                } else {
                                    listener.onFailure(e);
                                }
                            }
                        });
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            })
        );
    }

    /**
     * ESCRIPT_CAPABILITIES(category?) - Returns all capabilities in a table-friendly format.
     * 
     * Each element in the returned array is a DOCUMENT with:
     * - type: "FUNCTION" or "PROCEDURE"
     * - name: The name
     * - signature: Human-readable signature
     * - parameter_count: Number of parameters
     * - category: Category filter used (if any)
     * 
     * Optional parameter allows filtering by category prefix (e.g., "K8S", "AWS", "PAGERDUTY")
     */
    public static void registerEscriptCapabilities(ExecutionContext context, Client client) {
        context.declareFunction("ESCRIPT_CAPABILITIES",
            List.of(new Parameter("category", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ESCRIPT_CAPABILITIES", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String categoryFilter = null;
                    if (!args.isEmpty() && args.get(0) != null) {
                        categoryFilter = args.get(0).toString().toUpperCase();
                    }
                    final String filter = categoryFilter;
                    
                    List<Map<String, Object>> results = new ArrayList<>();
                    
                    // Add all functions
                    Map<String, FunctionDefinition> functions = context.getFunctions();
                    for (Map.Entry<String, FunctionDefinition> entry : functions.entrySet()) {
                        String name = entry.getKey();
                        
                        // Skip introspection functions themselves for cleaner output
                        if (name.startsWith("ESCRIPT_")) continue;
                        
                        // Apply category filter if specified
                        if (filter != null && !name.startsWith(filter)) continue;
                        
                        FunctionDefinition funcDef = entry.getValue();
                        Map<String, Object> row = new HashMap<>();
                        row.put("type", "FUNCTION");
                        row.put("name", name);
                        
                        // Get parameters
                        List<Parameter> params;
                        if (funcDef instanceof BuiltInFunctionDefinition) {
                            params = ((BuiltInFunctionDefinition) funcDef).getParameters();
                        } else {
                            params = funcDef.getParameters();
                        }
                        
                        row.put("parameter_count", params != null ? params.size() : 0);
                        
                        // Build signature
                        StringBuilder sig = new StringBuilder();
                        sig.append(name).append("(");
                        if (params != null && !params.isEmpty()) {
                            for (int i = 0; i < params.size(); i++) {
                                if (i > 0) sig.append(", ");
                                Parameter p = params.get(i);
                                sig.append(p.getName()).append(" ").append(p.getType());
                            }
                        }
                        sig.append(")");
                        row.put("signature", sig.toString());
                        
                        // Determine category from name prefix
                        String category = "OTHER";
                        if (name.startsWith("K8S_")) category = "KUBERNETES";
                        else if (name.startsWith("AWS_")) category = "AWS";
                        else if (name.startsWith("PAGERDUTY_")) category = "PAGERDUTY";
                        else if (name.startsWith("TF_")) category = "TERRAFORM";
                        else if (name.startsWith("SLACK_")) category = "SLACK";
                        else if (name.startsWith("S3_")) category = "S3";
                        else if (name.startsWith("JENKINS_") || name.startsWith("GITHUB_") 
                                || name.startsWith("GITLAB_") || name.startsWith("ARGOCD_")) category = "CICD";
                        else if (name.startsWith("LLM_") || name.startsWith("INFERENCE_")) category = "AI";
                        else if (name.startsWith("ARRAY_")) category = "ARRAY";
                        else if (name.startsWith("DOC_") || name.startsWith("FROM_JSON") || name.startsWith("TO_JSON")) category = "DOCUMENT";
                        else if (name.startsWith("DATE_") || name.equals("NOW") || name.equals("CURRENT_DATE")) category = "DATE";
                        else if (name.startsWith("HTTP_") || name.equals("WEBHOOK")) category = "HTTP";
                        else if (name.matches("^(LENGTH|UPPER|LOWER|TRIM|SUBSTR|REPLACE|CONCAT|INSTR|LPAD|RPAD|INITCAP|REVERSE|LTRIM|RTRIM)$")) category = "STRING";
                        else if (name.matches("^(ABS|CEIL|FLOOR|ROUND|TRUNC|MOD|POWER|SQRT|SIGN|GREATEST|LEAST|RANDOM)$")) category = "NUMBER";
                        row.put("category", category);
                        
                        results.add(row);
                    }
                    
                    // Add all intents
                    if (filter == null || "INTENT".startsWith(filter)) {
                        IntentRegistry registry = IntentRegistry.getInstance();
                        for (IntentDefinition intent : registry.getAll()) {
                            Map<String, Object> row = new HashMap<>();
                            row.put("type", "INTENT");
                            row.put("name", intent.getName());
                            row.put("category", "INTENT");
                            row.put("signature", intent.getSignature());
                            row.put("parameter_count", intent.getParameters().size());
                            row.put("description", intent.getDescription());
                            row.put("has_requires", intent.hasRequires());
                            results.add(row);
                        }
                    }
                    
                    // If we have a client, also fetch procedures
                    if (client != null && (filter == null || "PROCEDURE".startsWith(filter))) {
                        SearchRequest searchRequest = new SearchRequest(PROCEDURES_INDEX);
                        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
                        sourceBuilder.query(QueryBuilders.matchAllQuery());
                        sourceBuilder.size(1000);
                        sourceBuilder.fetchSource(new String[]{"parameters"}, null);
                        searchRequest.source(sourceBuilder);
                        
                        client.search(searchRequest, new ActionListener<>() {
                            @Override
                            public void onResponse(org.elasticsearch.action.search.SearchResponse response) {
                                try {
                                    for (SearchHit hit : response.getHits().getHits()) {
                                        Map<String, Object> row = new HashMap<>();
                                        row.put("type", "PROCEDURE");
                                        row.put("name", hit.getId());
                                        row.put("category", "STORED_PROCEDURE");
                                        
                                        Map<String, Object> source = hit.getSourceAsMap();
                                        @SuppressWarnings("unchecked")
                                        List<Map<String, Object>> params = (List<Map<String, Object>>) source.get("parameters");
                                        
                                        row.put("parameter_count", params != null ? params.size() : 0);
                                        
                                        // Build signature
                                        StringBuilder sig = new StringBuilder();
                                        sig.append(hit.getId()).append("(");
                                        if (params != null && !params.isEmpty()) {
                                            for (int i = 0; i < params.size(); i++) {
                                                if (i > 0) sig.append(", ");
                                                Map<String, Object> p = params.get(i);
                                                sig.append(p.get("name")).append(" ").append(p.get("type"));
                                            }
                                        }
                                        sig.append(")");
                                        row.put("signature", sig.toString());
                                        
                                        results.add(row);
                                    }
                                    
                                    // Sort by category, then name
                                    results.sort((a, b) -> {
                                        int catCmp = ((String) a.get("category")).compareTo((String) b.get("category"));
                                        if (catCmp != 0) return catCmp;
                                        return ((String) a.get("name")).compareTo((String) b.get("name"));
                                    });
                                    
                                    listener.onResponse(results);
                                } catch (Exception e) {
                                    listener.onFailure(e);
                                }
                            }
                            
                            @Override
                            public void onFailure(Exception e) {
                                // If index doesn't exist, just return functions
                                results.sort((a, b) -> {
                                    int catCmp = ((String) a.get("category")).compareTo((String) b.get("category"));
                                    if (catCmp != 0) return catCmp;
                                    return ((String) a.get("name")).compareTo((String) b.get("name"));
                                });
                                listener.onResponse(results);
                            }
                        });
                    } else {
                        // No client, just return functions
                        results.sort((a, b) -> {
                            int catCmp = ((String) a.get("category")).compareTo((String) b.get("category"));
                            if (catCmp != 0) return catCmp;
                            return ((String) a.get("name")).compareTo((String) b.get("name"));
                        });
                        listener.onResponse(results);
                    }
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            })
        );
    }

    /**
     * ESCRIPT_INTENTS() - Returns an array of all defined intents.
     * 
     * Each element in the returned array is a DOCUMENT with:
     * - name: The intent name
     * - description: What the intent does
     * - signature: Human-readable signature
     * - parameter_count: Number of parameters
     * - has_requires: Whether the intent has pre-conditions
     * - has_on_failure: Whether the intent has failure handling
     */
    public static void registerEscriptIntents(ExecutionContext context) {
        context.declareFunction("ESCRIPT_INTENTS",
            List.of(), // No parameters
            new BuiltInFunctionDefinition("ESCRIPT_INTENTS", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    List<Map<String, Object>> result = new ArrayList<>();
                    
                    IntentRegistry registry = IntentRegistry.getInstance();
                    for (IntentDefinition intent : registry.getAll()) {
                        Map<String, Object> intentInfo = new HashMap<>();
                        intentInfo.put("name", intent.getName());
                        intentInfo.put("description", intent.getDescription());
                        intentInfo.put("signature", intent.getSignature());
                        intentInfo.put("parameter_count", intent.getParameters().size());
                        intentInfo.put("has_requires", intent.hasRequires());
                        intentInfo.put("has_on_failure", intent.hasOnFailure());
                        
                        // Build parameters list
                        List<Map<String, Object>> paramList = new ArrayList<>();
                        for (Parameter param : intent.getParameters()) {
                            Map<String, Object> paramInfo = new HashMap<>();
                            paramInfo.put("name", param.getName());
                            paramInfo.put("type", param.getType());
                            paramInfo.put("mode", param.getMode().toString());
                            paramList.add(paramInfo);
                        }
                        intentInfo.put("parameters", paramList);
                        
                        result.add(intentInfo);
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

    /**
     * ESCRIPT_INTENT(name) - Returns detailed information about a specific intent.
     * 
     * Returns a DOCUMENT with:
     * - name: The intent name
     * - exists: Whether the intent exists
     * - description: What the intent does
     * - signature: Human-readable signature
     * - parameters: Array of parameter definitions
     * - has_requires: Whether the intent has pre-conditions
     * - requires_count: Number of pre-conditions
     * - has_on_failure: Whether the intent has failure handling
     */
    public static void registerEscriptIntent(ExecutionContext context) {
        context.declareFunction("ESCRIPT_INTENT",
            List.of(new Parameter("name", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ESCRIPT_INTENT", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    if (args.isEmpty() || args.get(0) == null) {
                        listener.onFailure(new IllegalArgumentException("ESCRIPT_INTENT requires an intent name"));
                        return;
                    }
                    
                    String intentName = args.get(0).toString();
                    Map<String, Object> result = new HashMap<>();
                    result.put("name", intentName);
                    
                    IntentRegistry registry = IntentRegistry.getInstance();
                    IntentDefinition intent = registry.get(intentName);
                    
                    if (intent == null) {
                        result.put("exists", false);
                        listener.onResponse(result);
                        return;
                    }
                    
                    result.put("exists", true);
                    result.put("description", intent.getDescription());
                    result.put("signature", intent.getSignature());
                    result.put("parameter_count", intent.getParameters().size());
                    result.put("has_requires", intent.hasRequires());
                    result.put("requires_count", intent.getRequiresConditions().size());
                    result.put("has_on_failure", intent.hasOnFailure());
                    
                    // Build parameters list
                    List<Map<String, Object>> paramList = new ArrayList<>();
                    for (Parameter param : intent.getParameters()) {
                        Map<String, Object> paramInfo = new HashMap<>();
                        paramInfo.put("name", param.getName());
                        paramInfo.put("type", param.getType());
                        paramInfo.put("mode", param.getMode().toString());
                        paramList.add(paramInfo);
                    }
                    result.put("parameters", paramList);
                    
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            })
        );
    }
}
