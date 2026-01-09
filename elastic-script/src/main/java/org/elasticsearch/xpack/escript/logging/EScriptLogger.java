/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Centralized, structured logging for elastic-script.
 * 
 * Log Level Guidelines:
 * - ERROR: Exceptions, failures, unrecoverable errors
 * - WARN:  Deprecated APIs, retries, recoverable issues
 * - INFO:  Procedure start/end, high-level operations
 * - DEBUG: Variable assignments, function calls, loop iterations
 * - TRACE: Expression evaluation, handler internals, context state
 * 
 * All log messages include an execution ID for correlation with APM traces.
 */
public final class EScriptLogger {

    // Main logger for system messages
    private static final Logger SYSTEM_LOGGER = LogManager.getLogger("o.e.x.e.EScript");
    
    // Separate logger for user output (PRINT statements) - can be configured independently
    private static final Logger OUTPUT_LOGGER = LogManager.getLogger("o.e.x.e.EScript.OUTPUT");
    
    // Maximum length for truncated values in logs
    private static final int MAX_VALUE_LENGTH = 100;
    private static final int MAX_QUERY_LENGTH = 200;

    private EScriptLogger() {
        // Utility class
    }

    // ========================================================================
    // PROCEDURE LIFECYCLE (INFO level)
    // ========================================================================

    /**
     * Log procedure execution start.
     */
    public static void procedureStart(String executionId, String procedureName) {
        SYSTEM_LOGGER.info("[{}] Starting: {}()", executionId, procedureName);
    }

    /**
     * Log procedure execution start with arguments.
     */
    public static void procedureStart(String executionId, String procedureName, int argCount) {
        SYSTEM_LOGGER.info("[{}] Starting: {}() with {} args", executionId, procedureName, argCount);
    }

    /**
     * Log procedure execution completion.
     */
    public static void procedureEnd(String executionId, String procedureName, long durationMs) {
        SYSTEM_LOGGER.info("[{}] Completed: {}() ({}ms)", executionId, procedureName, durationMs);
    }

    /**
     * Log procedure execution failure.
     */
    public static void procedureFailed(String executionId, String procedureName, String error) {
        SYSTEM_LOGGER.error("[{}] Failed: {}() - {}", executionId, procedureName, error);
    }

    /**
     * Log procedure execution failure with exception.
     */
    public static void procedureFailed(String executionId, String procedureName, Exception e) {
        SYSTEM_LOGGER.error("[{}] Failed: {}() - {}", executionId, procedureName, e.getMessage(), e);
    }

    // ========================================================================
    // USER OUTPUT (INFO level - separate logger)
    // ========================================================================

    /**
     * Log PRINT statement output. Uses a separate logger so it can be filtered independently.
     */
    public static void userOutput(String executionId, String message) {
        OUTPUT_LOGGER.info("[{}] {}", executionId, message);
    }

    /**
     * Log DEBUG statement output.
     */
    public static void userDebug(String executionId, String message) {
        OUTPUT_LOGGER.debug("[{}] [DEBUG] {}", executionId, message);
    }

    /**
     * Log WARN statement output.
     */
    public static void userWarn(String executionId, String message) {
        OUTPUT_LOGGER.warn("[{}] [WARN] {}", executionId, message);
    }

    /**
     * Log ERROR statement output.
     */
    public static void userError(String executionId, String message) {
        OUTPUT_LOGGER.error("[{}] [ERROR] {}", executionId, message);
    }

    // ========================================================================
    // VARIABLE OPERATIONS (DEBUG level)
    // ========================================================================

    /**
     * Log variable declaration.
     */
    public static void variableDeclare(String executionId, String name, String type, Object value) {
        if (SYSTEM_LOGGER.isDebugEnabled()) {
            SYSTEM_LOGGER.debug("[{}] DECLARE {} {} = {}", executionId, type, name, truncate(value));
        }
    }

    /**
     * Log variable assignment (SET).
     */
    public static void variableSet(String executionId, String name, Object value) {
        if (SYSTEM_LOGGER.isDebugEnabled()) {
            SYSTEM_LOGGER.debug("[{}] SET {} = {}", executionId, name, truncate(value));
        }
    }

    // ========================================================================
    // CONTROL FLOW (DEBUG level)
    // ========================================================================

    /**
     * Log IF statement evaluation.
     */
    public static void ifStatement(String executionId, boolean conditionResult) {
        SYSTEM_LOGGER.debug("[{}] IF condition = {}", executionId, conditionResult);
    }

    /**
     * Log FOR loop start.
     */
    public static void forLoopStart(String executionId, String variable, int iterationCount) {
        SYSTEM_LOGGER.debug("[{}] FOR {} IN ({}  iterations)", executionId, variable, iterationCount);
    }

    /**
     * Log FOR loop iteration.
     */
    public static void forLoopIteration(String executionId, String variable, int index, Object value) {
        if (SYSTEM_LOGGER.isTraceEnabled()) {
            SYSTEM_LOGGER.trace("[{}] FOR {} iteration {}: {}", executionId, variable, index, truncate(value));
        }
    }

    /**
     * Log WHILE loop start.
     */
    public static void whileLoopStart(String executionId) {
        SYSTEM_LOGGER.debug("[{}] WHILE loop starting", executionId);
    }

    /**
     * Log WHILE loop iteration.
     */
    public static void whileLoopIteration(String executionId, int iteration) {
        if (SYSTEM_LOGGER.isTraceEnabled()) {
            SYSTEM_LOGGER.trace("[{}] WHILE iteration {}", executionId, iteration);
        }
    }

    // ========================================================================
    // FUNCTION CALLS (DEBUG level)
    // ========================================================================

    /**
     * Log function call.
     */
    public static void functionCall(String executionId, String functionName, int argCount) {
        SYSTEM_LOGGER.debug("[{}] CALL {}() with {} args", executionId, functionName, argCount);
    }

    /**
     * Log function call result.
     */
    public static void functionResult(String executionId, String functionName, Object result) {
        if (SYSTEM_LOGGER.isTraceEnabled()) {
            SYSTEM_LOGGER.trace("[{}] {}() returned: {}", executionId, functionName, truncate(result));
        }
    }

    /**
     * Log built-in function registration (at startup).
     */
    public static void functionRegistered(String functionName) {
        SYSTEM_LOGGER.debug("Registered function: {}", functionName);
    }

    // ========================================================================
    // ESQL QUERIES (DEBUG level)
    // ========================================================================

    /**
     * Log ESQL query execution.
     */
    public static void esqlQuery(String executionId, String query) {
        if (SYSTEM_LOGGER.isDebugEnabled()) {
            SYSTEM_LOGGER.debug("[{}] ESQL: {}", executionId, truncateQuery(query));
        }
    }

    /**
     * Log ESQL query result.
     */
    public static void esqlResult(String executionId, int rowCount) {
        SYSTEM_LOGGER.debug("[{}] ESQL returned {} rows", executionId, rowCount);
    }

    // ========================================================================
    // EXTERNAL CALLS (DEBUG level)
    // ========================================================================

    /**
     * Log external HTTP call.
     */
    public static void externalCall(String executionId, String service, String method, String url) {
        SYSTEM_LOGGER.debug("[{}] {} {} {}", executionId, service, method, url);
    }

    /**
     * Log external call result.
     */
    public static void externalCallResult(String executionId, String service, int statusCode, long durationMs) {
        SYSTEM_LOGGER.debug("[{}] {} responded {} ({}ms)", executionId, service, statusCode, durationMs);
    }

    /**
     * Log external call failure.
     */
    public static void externalCallFailed(String executionId, String service, String error) {
        SYSTEM_LOGGER.warn("[{}] {} failed: {}", executionId, service, error);
    }

    // ========================================================================
    // ASYNC CHAIN TRACING (TRACE level)
    // ========================================================================

    /**
     * Log async handler entry - shows the flow between async handlers.
     */
    public static void asyncEnter(String executionId, String handlerName) {
        if (SYSTEM_LOGGER.isTraceEnabled()) {
            SYSTEM_LOGGER.trace("[{}] → {}", executionId, handlerName);
        }
    }

    /**
     * Log async handler exit with result.
     */
    public static void asyncExit(String executionId, String handlerName, Object result) {
        if (SYSTEM_LOGGER.isTraceEnabled()) {
            SYSTEM_LOGGER.trace("[{}] ← {} ({})", executionId, handlerName, truncate(result));
        }
    }

    /**
     * Log async handler failure.
     */
    public static void asyncFailed(String executionId, String handlerName, String error) {
        if (SYSTEM_LOGGER.isTraceEnabled()) {
            SYSTEM_LOGGER.trace("[{}] ✗ {} failed: {}", executionId, handlerName, error);
        }
    }

    /**
     * Log async chain step with indentation for nested calls.
     */
    public static void asyncStep(String executionId, int depth, String step) {
        if (SYSTEM_LOGGER.isTraceEnabled()) {
            String indent = "  ".repeat(depth);
            String prefix = depth > 0 ? "├─ " : "→ ";
            SYSTEM_LOGGER.trace("[{}] {}{}{}", executionId, indent, prefix, step);
        }
    }

    /**
     * Log async callback between handlers.
     */
    public static void asyncCallback(String executionId, String from, String to) {
        if (SYSTEM_LOGGER.isTraceEnabled()) {
            SYSTEM_LOGGER.trace("[{}] {} → {}", executionId, from, to);
        }
    }

    // ========================================================================
    // EXPRESSION EVALUATION (TRACE level)
    // ========================================================================

    /**
     * Log expression evaluation (very verbose - TRACE only).
     */
    public static void expressionEval(String executionId, String expression, Object result) {
        if (SYSTEM_LOGGER.isTraceEnabled()) {
            SYSTEM_LOGGER.trace("[{}] Eval: {} -> {}", executionId, truncate(expression), truncate(result));
        }
    }

    /**
     * Log statement execution (very verbose - TRACE only).
     */
    public static void statementExec(String executionId, String statementType, String details) {
        if (SYSTEM_LOGGER.isTraceEnabled()) {
            SYSTEM_LOGGER.trace("[{}] {} {}", executionId, statementType, truncate(details));
        }
    }

    // ========================================================================
    // ERRORS AND WARNINGS
    // ========================================================================

    /**
     * Log an error with exception.
     */
    public static void error(String executionId, String message, Exception e) {
        SYSTEM_LOGGER.error("[{}] {}", executionId, message, e);
    }

    /**
     * Log an error without exception.
     */
    public static void error(String executionId, String message) {
        SYSTEM_LOGGER.error("[{}] {}", executionId, message);
    }

    /**
     * Log a warning.
     */
    public static void warn(String executionId, String message) {
        SYSTEM_LOGGER.warn("[{}] {}", executionId, message);
    }

    // ========================================================================
    // UTILITIES
    // ========================================================================

    /**
     * Truncate a value for logging to avoid huge log messages.
     */
    private static String truncate(Object value) {
        if (value == null) {
            return "null";
        }
        String str = value.toString();
        if (str.length() > MAX_VALUE_LENGTH) {
            return str.substring(0, MAX_VALUE_LENGTH) + "...(" + str.length() + " chars)";
        }
        return str;
    }

    /**
     * Truncate a query for logging.
     */
    private static String truncateQuery(String query) {
        if (query == null) {
            return "null";
        }
        // Remove newlines for cleaner logs
        String cleaned = query.replaceAll("\\s+", " ").trim();
        if (cleaned.length() > MAX_QUERY_LENGTH) {
            return cleaned.substring(0, MAX_QUERY_LENGTH) + "...";
        }
        return cleaned;
    }

    /**
     * Check if debug logging is enabled (for performance optimization).
     */
    public static boolean isDebugEnabled() {
        return SYSTEM_LOGGER.isDebugEnabled();
    }

    /**
     * Check if trace logging is enabled (for performance optimization).
     */
    public static boolean isTraceEnabled() {
        return SYSTEM_LOGGER.isTraceEnabled();
    }
}
