/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.tracing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * OpenTelemetry-compliant tracing for elastic-script.
 * 
 * This tracer provides distributed tracing using OpenTelemetry standards via reflection,
 * enabling zero-dependency integration with the OTEL Java agent for auto-instrumentation.
 * 
 * <h2>Quick Start with OTEL Java Agent</h2>
 * <pre>
 * # 1. Download the OTEL Java agent
 * curl -L -o opentelemetry-javaagent.jar \
 *   https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar
 * 
 * # 2. Configure environment variables
 * export OTEL_SERVICE_NAME=elasticsearch
 * export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317
 * export OTEL_TRACES_EXPORTER=otlp
 * 
 * # 3. Start Elasticsearch with the agent
 * ES_JAVA_OPTS="-javaagent:/path/to/opentelemetry-javaagent.jar" ./bin/elasticsearch
 * </pre>
 * 
 * <h2>Sending Traces to Jaeger</h2>
 * <pre>
 * # Run Jaeger (all-in-one)
 * docker run -d --name jaeger \
 *   -p 16686:16686 \
 *   -p 4317:4317 \
 *   jaegertracing/all-in-one:latest
 * 
 * # View traces at http://localhost:16686
 * </pre>
 * 
 * <h2>Sending Traces to Elastic APM via OTEL</h2>
 * <pre>
 * export OTEL_EXPORTER_OTLP_ENDPOINT=https://your-apm-server:8200
 * export OTEL_EXPORTER_OTLP_HEADERS="Authorization=Bearer YOUR_SECRET_TOKEN"
 * </pre>
 * 
 * <h2>Tracing Hierarchy</h2>
 * <ul>
 *   <li>Root Span: Procedure execution (escript.procedure.execute)</li>
 *   <li>Child Spans: Statements, function calls, ESQL queries, external calls</li>
 * </ul>
 * 
 * <h2>Semantic Conventions</h2>
 * Uses OpenTelemetry semantic conventions for:
 * <ul>
 *   <li>db.* - Database operations (ESQL queries)</li>
 *   <li>http.* - External HTTP calls</li>
 *   <li>code.* - Code execution context</li>
 * </ul>
 */
public final class EScriptTracer {

    private static final Logger LOGGER = LogManager.getLogger(EScriptTracer.class);
    
    // OTEL availability flag
    private static final boolean OTEL_AVAILABLE = isOtelAvailable();
    
    // Cached reflection objects for performance
    private static Object globalOpenTelemetry;
    private static Method getTracerMethod;
    private static Method spanBuilderMethod;
    private static Method setAttributeStringMethod;
    private static Method setAttributeLongMethod;
    private static Method startSpanMethod;
    private static Method endSpanMethod;
    private static Method makeCurrentMethod;
    private static Method closeScopeMethod;
    private static Method recordExceptionMethod;
    private static Method setStatusMethod;
    private static Object statusCodeOk;
    private static Object statusCodeError;
    private static Object spanKindInternal;
    private static Object spanKindClient;
    
    static {
        if (OTEL_AVAILABLE) {
            initializeOtelReflection();
        }
    }

    private EScriptTracer() {
        // Utility class
    }

    /**
     * Check if OpenTelemetry is available on the classpath (via OTEL Java agent).
     */
    private static boolean isOtelAvailable() {
        try {
            Class.forName("io.opentelemetry.api.GlobalOpenTelemetry");
            LOGGER.info("OpenTelemetry detected - distributed tracing enabled");
            return true;
        } catch (ClassNotFoundException e) {
            LOGGER.debug("OpenTelemetry not found - tracing disabled. " +
                "Attach the OTEL Java agent for distributed tracing support.");
            return false;
        }
    }

    /**
     * Initialize reflection objects for OTEL API access.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void initializeOtelReflection() {
        try {
            // GlobalOpenTelemetry
            Class<?> globalOtelClass = Class.forName("io.opentelemetry.api.GlobalOpenTelemetry");
            Method getMethod = globalOtelClass.getMethod("get");
            globalOpenTelemetry = getMethod.invoke(null);
            
            // Tracer
            Class<?> otelClass = Class.forName("io.opentelemetry.api.OpenTelemetry");
            getTracerMethod = otelClass.getMethod("getTracer", String.class, String.class);
            
            // SpanBuilder
            Class<?> tracerClass = Class.forName("io.opentelemetry.api.trace.Tracer");
            spanBuilderMethod = tracerClass.getMethod("spanBuilder", String.class);
            
            Class<?> spanBuilderClass = Class.forName("io.opentelemetry.api.trace.SpanBuilder");
            Class<?> spanKindClass = Class.forName("io.opentelemetry.api.trace.SpanKind");
            Method setSpanKindMethod = spanBuilderClass.getMethod("setSpanKind", spanKindClass);
            setAttributeStringMethod = spanBuilderClass.getMethod("setAttribute", String.class, String.class);
            
            // Get AttributeKey for long attributes
            Class<?> attributeKeyClass = Class.forName("io.opentelemetry.api.common.AttributeKey");
            Method longKeyMethod = attributeKeyClass.getMethod("longKey", String.class);
            
            startSpanMethod = spanBuilderClass.getMethod("startSpan");
            
            // Span
            Class<?> spanClass = Class.forName("io.opentelemetry.api.trace.Span");
            endSpanMethod = spanClass.getMethod("end");
            makeCurrentMethod = spanClass.getMethod("makeCurrent");
            recordExceptionMethod = spanClass.getMethod("recordException", Throwable.class);
            
            // StatusCode
            Class<?> statusCodeClass = Class.forName("io.opentelemetry.api.trace.StatusCode");
            statusCodeOk = Enum.valueOf((Class<Enum>) statusCodeClass, "OK");
            statusCodeError = Enum.valueOf((Class<Enum>) statusCodeClass, "ERROR");
            setStatusMethod = spanClass.getMethod("setStatus", statusCodeClass, String.class);
            
            // SpanKind
            spanKindInternal = Enum.valueOf((Class<Enum>) spanKindClass, "INTERNAL");
            spanKindClient = Enum.valueOf((Class<Enum>) spanKindClass, "CLIENT");
            
            // Scope
            Class<?> scopeClass = Class.forName("io.opentelemetry.context.Scope");
            closeScopeMethod = scopeClass.getMethod("close");
            
            LOGGER.debug("OpenTelemetry reflection initialized successfully");
        } catch (Exception e) {
            LOGGER.warn("Failed to initialize OpenTelemetry reflection: {}", e.getMessage());
        }
    }

    /**
     * Generate a new execution ID for correlation.
     */
    public static String generateExecutionId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Start a new root span for procedure execution.
     */
    public static TracingContext startProcedure(String executionId, String procedureName) {
        if (OTEL_AVAILABLE) {
            return OtelTracingContext.start(executionId, procedureName);
        }
        return new NoOpTracingContext(executionId, procedureName);
    }

    /**
     * Start a span for a statement execution.
     */
    public static SpanContext startStatement(String executionId, String statementType, String details) {
        if (OTEL_AVAILABLE) {
            return OtelSpanContext.start(
                "escript.statement." + statementType.toLowerCase(),
                executionId,
                "escript.statement.type", statementType,
                false
            );
        }
        return NoOpSpanContext.INSTANCE;
    }

    /**
     * Start a span for a function call.
     */
    public static SpanContext startFunctionCall(String executionId, String functionName, int argCount) {
        if (OTEL_AVAILABLE) {
            return OtelSpanContext.start(
                "escript.function." + functionName.toLowerCase(),
                executionId,
                "escript.function.name", functionName,
                false
            );
        }
        return NoOpSpanContext.INSTANCE;
    }

    /**
     * Start a span for an ESQL query.
     */
    public static SpanContext startEsqlQuery(String executionId, String query) {
        if (OTEL_AVAILABLE) {
            String truncated = query.length() > 200 ? query.substring(0, 200) + "..." : query;
            return OtelSpanContext.startDb(
                "ESQL: " + truncated,
                executionId,
                "elasticsearch", "esql", query
            );
        }
        return NoOpSpanContext.INSTANCE;
    }

    /**
     * Start a span for an external HTTP call.
     */
    public static SpanContext startExternalCall(String executionId, String serviceName, 
                                                 String method, String url) {
        if (OTEL_AVAILABLE) {
            return OtelSpanContext.startHttp(
                method + " " + extractHost(url),
                executionId,
                method, url, serviceName
            );
        }
        return NoOpSpanContext.INSTANCE;
    }

    /**
     * Start a span for a loop iteration.
     */
    public static SpanContext startLoopIteration(String executionId, String loopType, int iteration) {
        if (OTEL_AVAILABLE) {
            return OtelSpanContext.start(
                "escript.loop." + loopType.toLowerCase() + "[" + iteration + "]",
                executionId,
                "escript.loop.type", loopType,
                false
            );
        }
        return NoOpSpanContext.INSTANCE;
    }

    /**
     * Start a span for an async operation.
     */
    public static SpanContext startAsyncOperation(String executionId, String operationType, String details) {
        if (OTEL_AVAILABLE) {
            return OtelSpanContext.start(
                "escript.async." + operationType.toLowerCase(),
                executionId,
                "escript.async.type", operationType,
                false
            );
        }
        return NoOpSpanContext.INSTANCE;
    }

    // ========================================================================
    // INTERFACES
    // ========================================================================

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

    public interface SpanContext extends AutoCloseable {
        void setLabel(String key, String value);
        void setLabel(String key, long value);
        void captureException(Exception e);
        void end();
        @Override
        default void close() { end(); }
    }

    // ========================================================================
    // NO-OP IMPLEMENTATIONS
    // ========================================================================

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

    private static class NoOpSpanContext implements SpanContext {
        static final NoOpSpanContext INSTANCE = new NoOpSpanContext();
        
        @Override public void setLabel(String key, String value) { }
        @Override public void setLabel(String key, long value) { }
        @Override public void captureException(Exception e) { }
        @Override public void end() { }
    }

    // ========================================================================
    // OTEL IMPLEMENTATIONS (via reflection)
    // ========================================================================

    private static class OtelTracingContext implements TracingContext {
        private final String executionId;
        private final String procedureName;
        private final Object span;
        private final Object scope;

        private OtelTracingContext(String executionId, String procedureName, Object span, Object scope) {
            this.executionId = executionId;
            this.procedureName = procedureName;
            this.span = span;
            this.scope = scope;
        }

        static TracingContext start(String executionId, String procedureName) {
            try {
                Object tracer = getTracerMethod.invoke(globalOpenTelemetry, "elastic-script", "1.0.0");
                Object spanBuilder = spanBuilderMethod.invoke(tracer, "escript.procedure.execute " + procedureName);
                
                // Set attributes
                setAttributeStringMethod.invoke(spanBuilder, "escript.execution.id", executionId);
                setAttributeStringMethod.invoke(spanBuilder, "escript.procedure.name", procedureName);
                setAttributeStringMethod.invoke(spanBuilder, "code.function", procedureName);
                setAttributeStringMethod.invoke(spanBuilder, "code.namespace", "escript");
                
                Object span = startSpanMethod.invoke(spanBuilder);
                Object scope = makeCurrentMethod.invoke(span);
                
                LOGGER.debug("Started OTEL trace: {} (execution={})", procedureName, executionId);
                return new OtelTracingContext(executionId, procedureName, span, scope);
            } catch (Exception e) {
                LOGGER.trace("Failed to start OTEL trace: {}", e.getMessage());
                return new NoOpTracingContext(executionId, procedureName);
            }
        }

        @Override public String getExecutionId() { return executionId; }

        @Override
        public void setLabel(String key, String value) {
            try {
                span.getClass().getMethod("setAttribute", String.class, String.class)
                    .invoke(span, key, value);
            } catch (Exception e) {
                LOGGER.trace("Failed to set label: {}", e.getMessage());
            }
        }

        @Override
        public void setLabel(String key, long value) {
            try {
                Class<?> attributeKeyClass = Class.forName("io.opentelemetry.api.common.AttributeKey");
                Method longKeyMethod = attributeKeyClass.getMethod("longKey", String.class);
                Object attrKey = longKeyMethod.invoke(null, key);
                span.getClass().getMethod("setAttribute", attributeKeyClass, Object.class)
                    .invoke(span, attrKey, value);
            } catch (Exception e) {
                LOGGER.trace("Failed to set long label: {}", e.getMessage());
            }
        }

        @Override
        public void setResult(String result) {
            try {
                setLabel("escript.result", result);
                if ("success".equalsIgnoreCase(result)) {
                    setStatusMethod.invoke(span, statusCodeOk, "");
                }
            } catch (Exception e) {
                LOGGER.trace("Failed to set result: {}", e.getMessage());
            }
        }

        @Override
        public void captureException(Exception e) {
            try {
                recordExceptionMethod.invoke(span, e);
                setStatusMethod.invoke(span, statusCodeError, e.getMessage());
            } catch (Exception ex) {
                LOGGER.trace("Failed to capture exception: {}", ex.getMessage());
            }
        }

        @Override
        public void end() {
            try {
                closeScopeMethod.invoke(scope);
                endSpanMethod.invoke(span);
                LOGGER.debug("Ended OTEL trace: {} (execution={})", procedureName, executionId);
            } catch (Exception e) {
                LOGGER.trace("Failed to end trace: {}", e.getMessage());
            }
        }
    }

    private static class OtelSpanContext implements SpanContext {
        private final Object span;
        private final Object scope;

        private OtelSpanContext(Object span, Object scope) {
            this.span = span;
            this.scope = scope;
        }

        static SpanContext start(String name, String executionId, String attrKey, String attrValue, boolean isClient) {
            try {
                Object tracer = getTracerMethod.invoke(globalOpenTelemetry, "elastic-script", "1.0.0");
                Object spanBuilder = spanBuilderMethod.invoke(tracer, name);
                
                setAttributeStringMethod.invoke(spanBuilder, "escript.execution.id", executionId);
                setAttributeStringMethod.invoke(spanBuilder, attrKey, attrValue);
                
                Object span = startSpanMethod.invoke(spanBuilder);
                Object scope = makeCurrentMethod.invoke(span);
                
                return new OtelSpanContext(span, scope);
            } catch (Exception e) {
                LOGGER.trace("Failed to start span: {}", e.getMessage());
                return NoOpSpanContext.INSTANCE;
            }
        }

        static SpanContext startDb(String name, String executionId, String dbSystem, String dbOp, String statement) {
            try {
                Object tracer = getTracerMethod.invoke(globalOpenTelemetry, "elastic-script", "1.0.0");
                Object spanBuilder = spanBuilderMethod.invoke(tracer, name);
                
                setAttributeStringMethod.invoke(spanBuilder, "escript.execution.id", executionId);
                setAttributeStringMethod.invoke(spanBuilder, "db.system", dbSystem);
                setAttributeStringMethod.invoke(spanBuilder, "db.operation", dbOp);
                setAttributeStringMethod.invoke(spanBuilder, "db.statement", statement);
                
                Object span = startSpanMethod.invoke(spanBuilder);
                Object scope = makeCurrentMethod.invoke(span);
                
                return new OtelSpanContext(span, scope);
            } catch (Exception e) {
                LOGGER.trace("Failed to start db span: {}", e.getMessage());
                return NoOpSpanContext.INSTANCE;
            }
        }

        static SpanContext startHttp(String name, String executionId, String method, String url, String service) {
            try {
                Object tracer = getTracerMethod.invoke(globalOpenTelemetry, "elastic-script", "1.0.0");
                Object spanBuilder = spanBuilderMethod.invoke(tracer, name);
                
                setAttributeStringMethod.invoke(spanBuilder, "escript.execution.id", executionId);
                setAttributeStringMethod.invoke(spanBuilder, "http.method", method);
                setAttributeStringMethod.invoke(spanBuilder, "http.url", url);
                setAttributeStringMethod.invoke(spanBuilder, "peer.service", service);
                
                Object span = startSpanMethod.invoke(spanBuilder);
                Object scope = makeCurrentMethod.invoke(span);
                
                return new OtelSpanContext(span, scope);
            } catch (Exception e) {
                LOGGER.trace("Failed to start http span: {}", e.getMessage());
                return NoOpSpanContext.INSTANCE;
            }
        }

        @Override
        public void setLabel(String key, String value) {
            try {
                span.getClass().getMethod("setAttribute", String.class, String.class)
                    .invoke(span, key, value);
            } catch (Exception e) {
                LOGGER.trace("Failed to set label: {}", e.getMessage());
            }
        }

        @Override
        public void setLabel(String key, long value) {
            try {
                Class<?> attributeKeyClass = Class.forName("io.opentelemetry.api.common.AttributeKey");
                Method longKeyMethod = attributeKeyClass.getMethod("longKey", String.class);
                Object attrKey = longKeyMethod.invoke(null, key);
                span.getClass().getMethod("setAttribute", attributeKeyClass, Object.class)
                    .invoke(span, attrKey, value);
            } catch (Exception e) {
                LOGGER.trace("Failed to set long label: {}", e.getMessage());
            }
        }

        @Override
        public void captureException(Exception e) {
            try {
                recordExceptionMethod.invoke(span, e);
                setStatusMethod.invoke(span, statusCodeError, e.getMessage());
            } catch (Exception ex) {
                LOGGER.trace("Failed to capture exception: {}", ex.getMessage());
            }
        }

        @Override
        public void end() {
            try {
                closeScopeMethod.invoke(scope);
                endSpanMethod.invoke(span);
            } catch (Exception e) {
                LOGGER.trace("Failed to end span: {}", e.getMessage());
            }
        }
    }

    private static String extractHost(String url) {
        try {
            if (url == null) return "unknown";
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
