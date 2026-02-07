/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V.
 * under one or more contributor license agreements. Licensed under
 * the Elastic License 2.0; you may not use this file except in compliance
 * with the Elastic License 2.0.
 */

package org.elasticsearch.xpack.escript.functions;

import org.elasticsearch.client.internal.Client;
import org.elasticsearch.logging.LogManager;
import org.elasticsearch.logging.Logger;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.xpack.escript.functions.builtin.datasources.ESFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.datasources.EsqlBuiltInFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.ArrayBuiltInFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.DateBuiltInFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.DocumentBuiltInFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.MapBuiltInFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.NumberBuiltInFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.StringBuiltInFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.inference.InferenceFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.introspection.IntrospectionFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.AWSFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.CICDFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.GenericFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.KubernetesFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.PagerDutyFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.TerraformFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.thirdparty.OpenAIFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.thirdparty.S3Functions;
import org.elasticsearch.xpack.escript.functions.builtin.thirdparty.SlackFunctions;
import org.elasticsearch.xpack.escript.functions.community.FunctionLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A registry that caches built-in function definitions to avoid re-registration on every execution.
 * 
 * This registry separates functions into two categories:
 * 1. Stateless functions (String, Number, Array, Date, etc.) - registered once at startup, cached permanently
 * 2. Executor-dependent functions (ESQL) - registered per request since they capture executor for variable substitution
 * 3. Client-dependent functions (ES, Inference) - registered once per Client instance
 * 
 * The registry is thread-safe and uses lazy initialization.
 */
public class BuiltInFunctionRegistry {
    
    private static final Logger LOGGER = LogManager.getLogger(BuiltInFunctionRegistry.class);
    
    // Singleton instance per Client
    private static final Map<Client, BuiltInFunctionRegistry> INSTANCES = new HashMap<>();
    
    // Cached function definitions (function name -> FunctionDefinition)
    private final Map<String, FunctionDefinition> statelessFunctions = new HashMap<>();
    private final Map<String, FunctionDefinition> clientFunctions = new HashMap<>();
    
    private final AtomicBoolean statelessInitialized = new AtomicBoolean(false);
    private final AtomicBoolean clientInitialized = new AtomicBoolean(false);
    
    private final Client client;
    
    private BuiltInFunctionRegistry(Client client) {
        this.client = client;
    }
    
    /**
     * Gets or creates the registry instance for the given Client.
     * 
     * @param client The Elasticsearch client
     * @return The registry instance
     */
    public static synchronized BuiltInFunctionRegistry getInstance(Client client) {
        return INSTANCES.computeIfAbsent(client, BuiltInFunctionRegistry::new);
    }
    
    /**
     * Registers all built-in functions into the given ExecutionContext.
     * Uses cached function definitions where possible to minimize overhead.
     * 
     * Performance optimization breakdown:
     * - ~70 stateless functions: cached, copy reference only
     * - ~20 client-dependent functions: cached, copy reference only
     * - ~1 executor-dependent function (ESQL_QUERY): registered per request
     * 
     * This reduces per-request overhead from ~106 function registrations to ~1.
     * 
     * @param context The execution context to register functions into
     * @param executor The procedure executor (needed for ESQL functions)
     */
    public void registerAll(ExecutionContext context, ProcedureExecutor executor) {
        // Initialize stateless functions once (cached permanently)
        initializeStatelessFunctions();
        
        // Initialize client-dependent functions once per Client (cached)
        initializeClientFunctions();
        
        // Copy all cached functions to the context (fast - just reference copies)
        copyFunctionsToContext(context);
        
        // Register executor-dependent functions per request (cannot be cached)
        // ESQL_QUERY needs executor for variable substitution at runtime
        EsqlBuiltInFunctions.registerAll(context, executor, client);
        
        // Load community functions (may be dynamic, so always reload)
        FunctionLoader.loadCommunityFunctions(context);
    }
    
    /**
     * Initializes stateless built-in functions (String, Number, Array, Date, Document, Map, etc.)
     * These don't depend on Client or ProcedureExecutor and can be cached permanently.
     */
    private void initializeStatelessFunctions() {
        if (statelessInitialized.compareAndSet(false, true)) {
            LOGGER.debug("Initializing stateless built-in functions (one-time)");
            
            // Create a temporary context to collect function definitions
            ExecutionContext tempContext = new ExecutionContext();
            
            // Data type functions (pure transformations, no external dependencies)
            StringBuiltInFunctions.registerAll(tempContext);
            NumberBuiltInFunctions.registerAll(tempContext);
            ArrayBuiltInFunctions.registerAll(tempContext);
            DateBuiltInFunctions.registerAll(tempContext);
            DocumentBuiltInFunctions.registerAll(tempContext);
            MapBuiltInFunctions.registerAll(tempContext);
            
            // Third-party integrations (stateless HTTP-based, no captured state)
            OpenAIFunctions.registerAll(tempContext);
            SlackFunctions.registerAll(tempContext);
            S3Functions.registerAll(tempContext);
            
            // Runbook integrations (stateless HTTP-based)
            KubernetesFunctions.registerAll(tempContext);
            PagerDutyFunctions.registerAll(tempContext);
            TerraformFunctions.registerAll(tempContext);
            CICDFunctions.registerAll(tempContext);
            AWSFunctions.registerAll(tempContext);
            GenericFunctions.registerAll(tempContext);
            
            // Copy registered functions to cache
            statelessFunctions.putAll(tempContext.getAllFunctions());
            
            LOGGER.debug("Cached {} stateless built-in functions", statelessFunctions.size());
        }
    }
    
    /**
     * Initializes client-dependent built-in functions (ES document functions, Inference).
     * These require Client but not ProcedureExecutor, so they can be cached per Client.
     */
    private void initializeClientFunctions() {
        if (clientInitialized.compareAndSet(false, true)) {
            LOGGER.debug("Initializing client-dependent built-in functions (one-time per Client)");
            
            // Create a temporary context to collect function definitions
            ExecutionContext tempContext = new ExecutionContext();
            
            // ES document functions (only need client, not executor)
            ESFunctions.registerGetDocumentFunction(tempContext, client);
            ESFunctions.registerUpdateDocumentFunction(tempContext, client);
            ESFunctions.registerIndexBulkFunction(tempContext, client);
            ESFunctions.registerIndexDocumentFunction(tempContext, client);
            ESFunctions.registerRefreshIndexFunction(tempContext, client);
            
            // Inference functions (only need client)
            InferenceFunctions.registerAll(tempContext, client);
            
            // Introspection functions (only need client for stored procedures lookup)
            IntrospectionFunctions.registerAll(tempContext, client);
            
            // Copy registered functions to cache
            clientFunctions.putAll(tempContext.getAllFunctions());
            
            LOGGER.debug("Cached {} client-dependent built-in functions", clientFunctions.size());
        }
    }
    
    /**
     * Copies all cached functions to the given ExecutionContext.
     * This is a fast operation - just copying references to immutable function definitions.
     */
    private void copyFunctionsToContext(ExecutionContext context) {
        // Copy stateless functions (fast - reference copy only)
        for (Map.Entry<String, FunctionDefinition> entry : statelessFunctions.entrySet()) {
            context.declareFunctionIfAbsent(entry.getKey(), entry.getValue());
        }
        
        // Copy client-dependent functions (fast - reference copy only)
        for (Map.Entry<String, FunctionDefinition> entry : clientFunctions.entrySet()) {
            context.declareFunctionIfAbsent(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Returns the total number of cached functions.
     */
    public int getCachedFunctionCount() {
        return statelessFunctions.size() + clientFunctions.size();
    }
    
    /**
     * Clears all cached instances (useful for testing).
     */
    public static synchronized void clearInstances() {
        INSTANCES.clear();
    }
}
