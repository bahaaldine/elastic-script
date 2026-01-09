/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.tracing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

/**
 * APM tracing utilities for elastic-script.
 * 
 * This class provides a lightweight abstraction for APM tracing that can work with
 * or without the Elastic APM agent. When the APM agent is not present, tracing
 * operations are no-ops.
 * 
 * Tracing Hierarchy:
 * - Transaction: One per procedure execution
 * - Spans: Nested for statements, function calls, external calls
 * 
 * To enable full APM tracing, add the Elastic APM Java agent to the classpath
 * and configure it to point to your APM server.
 */
public final class EScriptTracer {

    private static final Logger LOGGER = LogManager.getLogger(EScriptTracer.class);
    
    // Flag to check if APM is available at runtime
    private static final boolean APM_AVAILABLE = isApmAvailable();
    
    private EScriptTracer() {
        // Utility class
    }

    /**
     * Check if Elastic APM agent is available on the classpath.
     */
    private static boolean isApmAvailable() {
        try {
            Class.forName("co.elastic.apm.api.ElasticApm");
            LOGGER.info("Elastic APM agent detected - tracing enabled");
            return true;
        } catch (ClassNotFoundException e) {
            LOGGER.debug("Elastic APM agent not found - tracing disabled");
            return false;
        }
    }

    /**
     * Generate a new execution ID for correlation.
     * Format: 8 character hex string (e.g., "a1b2c3d4")
     */
    public static String generateExecutionId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Start a new transaction for procedure execution.
     * Returns a TracingContext that should be ended when the procedure completes.
     */
    public static TracingContext startProcedure(String executionId, String procedureName) {
        if (APM_AVAILABLE) {
            return ApmTracingContext.startTransaction(executionId, procedureName);
        }
        return new NoOpTracingContext(executionId, procedureName);
    }

    /**
     * Start a span for a statement execution.
     */
    public static SpanContext startStatement(String executionId, String statementType, String details) {
        if (APM_AVAILABLE) {
            return ApmSpanContext.startSpan(executionId, "statement", statementType, details);
        }
        return NoOpSpanContext.INSTANCE;
    }

    /**
     * Start a span for a function call.
     */
    public static SpanContext startFunctionCall(String executionId, String functionName, int argCount) {
        if (APM_AVAILABLE) {
            return ApmSpanContext.startSpan(executionId, "function", functionName, 
                functionName + "() with " + argCount + " args");
        }
        return NoOpSpanContext.INSTANCE;
    }

    /**
     * Start a span for an ESQL query.
     */
    public static SpanContext startEsqlQuery(String executionId, String query) {
        if (APM_AVAILABLE) {
            return ApmSpanContext.startDbSpan(executionId, "elasticsearch", "esql", query);
        }
        return NoOpSpanContext.INSTANCE;
    }

    /**
     * Start a span for an external HTTP call.
     */
    public static SpanContext startExternalCall(String executionId, String serviceName, 
                                                  String method, String url) {
        if (APM_AVAILABLE) {
            return ApmSpanContext.startExternalSpan(executionId, serviceName, method, url);
        }
        return NoOpSpanContext.INSTANCE;
    }

    /**
     * Start a span for a loop iteration.
     */
    public static SpanContext startLoopIteration(String executionId, String loopType, int iteration) {
        if (APM_AVAILABLE) {
            return ApmSpanContext.startSpan(executionId, "loop", loopType, 
                loopType + " iteration " + iteration);
        }
        return NoOpSpanContext.INSTANCE;
    }

    // ========================================================================
    // TRACING CONTEXT INTERFACES
    // ========================================================================

    /**
     * Context for a transaction (procedure execution).
     */
    public interface TracingContext extends AutoCloseable {
        String getExecutionId();
        void setLabel(String key, String value);
        void setLabel(String key, long value);
        void setResult(String result);
        void captureException(Exception e);
        void end();
        @Override
        default void close() { end(); }
    }

    /**
     * Context for a span (sub-operation).
     */
    public interface SpanContext extends AutoCloseable {
        void setLabel(String key, String value);
        void setLabel(String key, long value);
        void captureException(Exception e);
        void end();
        @Override
        default void close() { end(); }
    }

    // ========================================================================
    // NO-OP IMPLEMENTATIONS (when APM is not available)
    // ========================================================================

    /**
     * No-op implementation of TracingContext.
     */
    private static class NoOpTracingContext implements TracingContext {
        private final String executionId;
        private final String procedureName;

        NoOpTracingContext(String executionId, String procedureName) {
            this.executionId = executionId;
            this.procedureName = procedureName;
        }

        @Override public String getExecutionId() { return executionId; }
        @Override public void setLabel(String key, String value) { }
        @Override public void setLabel(String key, long value) { }
        @Override public void setResult(String result) { }
        @Override public void captureException(Exception e) { }
        @Override public void end() { }
    }

    /**
     * No-op implementation of SpanContext.
     */
    private static class NoOpSpanContext implements SpanContext {
        static final NoOpSpanContext INSTANCE = new NoOpSpanContext();
        
        @Override public void setLabel(String key, String value) { }
        @Override public void setLabel(String key, long value) { }
        @Override public void captureException(Exception e) { }
        @Override public void end() { }
    }

    // ========================================================================
    // APM IMPLEMENTATIONS (when APM agent is available)
    // ========================================================================

    /**
     * APM-backed implementation of TracingContext.
     * Uses reflection to avoid compile-time dependency on APM classes.
     */
    private static class ApmTracingContext implements TracingContext {
        private final String executionId;
        private Object transaction; // co.elastic.apm.api.Transaction

        private ApmTracingContext(String executionId, Object transaction) {
            this.executionId = executionId;
            this.transaction = transaction;
        }

        static TracingContext startTransaction(String executionId, String procedureName) {
            try {
                Class<?> elasticApmClass = Class.forName("co.elastic.apm.api.ElasticApm");
                Object transaction = elasticApmClass.getMethod("startTransaction").invoke(null);
                
                // Set transaction name and type
                Class<?> txClass = transaction.getClass();
                txClass.getMethod("setName", String.class).invoke(transaction, procedureName);
                txClass.getMethod("setType", String.class).invoke(transaction, "escript");
                txClass.getMethod("addLabel", String.class, String.class)
                    .invoke(transaction, "execution.id", executionId);
                txClass.getMethod("addLabel", String.class, String.class)
                    .invoke(transaction, "procedure.name", procedureName);
                
                return new ApmTracingContext(executionId, transaction);
            } catch (Exception e) {
                LOGGER.debug("Failed to start APM transaction: {}", e.getMessage());
                return new NoOpTracingContext(executionId, procedureName);
            }
        }

        @Override
        public String getExecutionId() {
            return executionId;
        }

        @Override
        public void setLabel(String key, String value) {
            try {
                transaction.getClass().getMethod("addLabel", String.class, String.class)
                    .invoke(transaction, key, value);
            } catch (Exception e) {
                LOGGER.trace("Failed to set label: {}", e.getMessage());
            }
        }

        @Override
        public void setLabel(String key, long value) {
            try {
                transaction.getClass().getMethod("addLabel", String.class, Number.class)
                    .invoke(transaction, key, value);
            } catch (Exception e) {
                LOGGER.trace("Failed to set label: {}", e.getMessage());
            }
        }

        @Override
        public void setResult(String result) {
            try {
                transaction.getClass().getMethod("setResult", String.class)
                    .invoke(transaction, result);
            } catch (Exception e) {
                LOGGER.trace("Failed to set result: {}", e.getMessage());
            }
        }

        @Override
        public void captureException(Exception e) {
            try {
                transaction.getClass().getMethod("captureException", Throwable.class)
                    .invoke(transaction, e);
            } catch (Exception ex) {
                LOGGER.trace("Failed to capture exception: {}", ex.getMessage());
            }
        }

        @Override
        public void end() {
            try {
                transaction.getClass().getMethod("end").invoke(transaction);
            } catch (Exception e) {
                LOGGER.trace("Failed to end transaction: {}", e.getMessage());
            }
        }
    }

    /**
     * APM-backed implementation of SpanContext.
     */
    private static class ApmSpanContext implements SpanContext {
        private Object span; // co.elastic.apm.api.Span

        private ApmSpanContext(Object span) {
            this.span = span;
        }

        static SpanContext startSpan(String executionId, String type, String subtype, String name) {
            try {
                Class<?> elasticApmClass = Class.forName("co.elastic.apm.api.ElasticApm");
                Object currentSpan = elasticApmClass.getMethod("currentSpan").invoke(null);
                Object newSpan = currentSpan.getClass()
                    .getMethod("startSpan", String.class, String.class, String.class)
                    .invoke(currentSpan, type, subtype, "execute");
                
                newSpan.getClass().getMethod("setName", String.class).invoke(newSpan, name);
                newSpan.getClass().getMethod("addLabel", String.class, String.class)
                    .invoke(newSpan, "execution.id", executionId);
                
                return new ApmSpanContext(newSpan);
            } catch (Exception e) {
                LOGGER.trace("Failed to start span: {}", e.getMessage());
                return NoOpSpanContext.INSTANCE;
            }
        }

        static SpanContext startDbSpan(String executionId, String dbType, String dbAction, String statement) {
            try {
                Class<?> elasticApmClass = Class.forName("co.elastic.apm.api.ElasticApm");
                Object currentSpan = elasticApmClass.getMethod("currentSpan").invoke(null);
                Object newSpan = currentSpan.getClass()
                    .getMethod("startSpan", String.class, String.class, String.class)
                    .invoke(currentSpan, "db", dbType, dbAction);
                
                String truncatedStatement = statement.length() > 200 
                    ? statement.substring(0, 200) + "..." : statement;
                newSpan.getClass().getMethod("setName", String.class)
                    .invoke(newSpan, "ESQL: " + truncatedStatement);
                newSpan.getClass().getMethod("addLabel", String.class, String.class)
                    .invoke(newSpan, "db.statement", statement);
                newSpan.getClass().getMethod("addLabel", String.class, String.class)
                    .invoke(newSpan, "execution.id", executionId);
                
                return new ApmSpanContext(newSpan);
            } catch (Exception e) {
                LOGGER.trace("Failed to start db span: {}", e.getMessage());
                return NoOpSpanContext.INSTANCE;
            }
        }

        static SpanContext startExternalSpan(String executionId, String serviceName, 
                                              String method, String url) {
            try {
                Class<?> elasticApmClass = Class.forName("co.elastic.apm.api.ElasticApm");
                Object currentSpan = elasticApmClass.getMethod("currentSpan").invoke(null);
                Object newSpan = currentSpan.getClass()
                    .getMethod("startSpan", String.class, String.class, String.class)
                    .invoke(currentSpan, "external", "http", serviceName);
                
                newSpan.getClass().getMethod("setName", String.class)
                    .invoke(newSpan, method + " " + extractHost(url));
                newSpan.getClass().getMethod("addLabel", String.class, String.class)
                    .invoke(newSpan, "http.method", method);
                newSpan.getClass().getMethod("addLabel", String.class, String.class)
                    .invoke(newSpan, "http.url", url);
                newSpan.getClass().getMethod("addLabel", String.class, String.class)
                    .invoke(newSpan, "execution.id", executionId);
                
                return new ApmSpanContext(newSpan);
            } catch (Exception e) {
                LOGGER.trace("Failed to start external span: {}", e.getMessage());
                return NoOpSpanContext.INSTANCE;
            }
        }

        @Override
        public void setLabel(String key, String value) {
            try {
                span.getClass().getMethod("addLabel", String.class, String.class)
                    .invoke(span, key, value);
            } catch (Exception e) {
                LOGGER.trace("Failed to set label: {}", e.getMessage());
            }
        }

        @Override
        public void setLabel(String key, long value) {
            try {
                span.getClass().getMethod("addLabel", String.class, Number.class)
                    .invoke(span, key, value);
            } catch (Exception e) {
                LOGGER.trace("Failed to set label: {}", e.getMessage());
            }
        }

        @Override
        public void captureException(Exception e) {
            try {
                span.getClass().getMethod("captureException", Throwable.class)
                    .invoke(span, e);
            } catch (Exception ex) {
                LOGGER.trace("Failed to capture exception: {}", ex.getMessage());
            }
        }

        @Override
        public void end() {
            try {
                span.getClass().getMethod("end").invoke(span);
            } catch (Exception e) {
                LOGGER.trace("Failed to end span: {}", e.getMessage());
            }
        }
    }

    /**
     * Extract hostname from URL for span naming.
     */
    private static String extractHost(String url) {
        try {
            if (url == null) return "unknown";
            // Simple extraction - remove protocol and path
            String withoutProtocol = url.replaceFirst("^https?://", "");
            int slashIndex = withoutProtocol.indexOf('/');
            if (slashIndex > 0) {
                return withoutProtocol.substring(0, slashIndex);
            }
            return withoutProtocol;
        } catch (Exception e) {
            return "unknown";
        }
    }
}
