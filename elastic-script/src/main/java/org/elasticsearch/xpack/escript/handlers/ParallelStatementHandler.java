/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.escript.execution.Continuation;
import org.elasticsearch.xpack.escript.execution.ExecutionPipeline;
import org.elasticsearch.xpack.escript.execution.ExecutionRegistry;
import org.elasticsearch.xpack.escript.execution.ExecutionState;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Handles parallel execution statements.
 *
 * Syntax: {@code PARALLEL [proc1(), proc2()] | ON_ALL_DONE merge(@results) | ON_ANY_FAIL handle(@error);}
 */
public class ParallelStatementHandler {

    private static final Logger LOGGER = LogManager.getLogger(ParallelStatementHandler.class);

    private final ProcedureExecutor executor;
    private final ExecutionRegistry registry;
    private final ContinuationExecutor continuationExecutor;

    public ParallelStatementHandler(
        ProcedureExecutor executor,
        ExecutionRegistry registry,
        ContinuationExecutor continuationExecutor
    ) {
        this.executor = executor;
        this.registry = registry;
        this.continuationExecutor = continuationExecutor;
    }

    /**
     * Handles a parallel execution statement.
     */
    public void handleAsync(
        ElasticScriptParser.Parallel_statementContext ctx,
        ActionListener<Object> listener
    ) {
        try {
            // Extract procedure calls
            List<ElasticScriptParser.Parallel_procedure_callContext> procedures = 
                ctx.parallel_procedure_list().parallel_procedure_call();
            
            LOGGER.info("Executing {} procedures in parallel", procedures.size());

            // Build the pipeline from continuations
            ExecutionPipeline pipeline = buildParallelPipeline(ctx.parallel_continuation());

            // Track results and errors
            List<Object> results = new ArrayList<>(procedures.size());
            for (int i = 0; i < procedures.size(); i++) {
                results.add(null);
            }
            
            AtomicInteger completed = new AtomicInteger(0);
            AtomicBoolean anyFailed = new AtomicBoolean(false);
            AtomicReference<Exception> firstError = new AtomicReference<>(null);

            // Execute all procedures in parallel
            for (int i = 0; i < procedures.size(); i++) {
                final int index = i;
                ElasticScriptParser.Parallel_procedure_callContext procCall = procedures.get(i);
                String procedureName = procCall.ID().getText();
                
                // Evaluate arguments
                List<Object> args = new ArrayList<>();
                if (procCall.argument_list() != null) {
                    // For simplicity, evaluate synchronously for now
                    // A more robust implementation would evaluate asynchronously
                    evaluateArgumentsSync(procCall.argument_list(), args);
                }

                // Execute the procedure
                executor.callProcedureByNameAsync(procedureName, args, ActionListener.wrap(
                    result -> {
                        results.set(index, result);
                        int count = completed.incrementAndGet();
                        LOGGER.debug("Parallel procedure {} completed ({}/{})", 
                            procedureName, count, procedures.size());
                        
                        if (count == procedures.size()) {
                            // All completed
                            handleAllCompleted(results, firstError.get(), pipeline, listener);
                        }
                    },
                    error -> {
                        LOGGER.warn("Parallel procedure {} failed: {}", procedureName, error.getMessage());
                        if (firstError.compareAndSet(null, error)) {
                            anyFailed.set(true);
                        }
                        
                        int count = completed.incrementAndGet();
                        if (count == procedures.size()) {
                            // All completed (some with errors)
                            handleAllCompleted(results, firstError.get(), pipeline, listener);
                        }
                    }
                ));
            }
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    private void handleAllCompleted(
        List<Object> results,
        Exception firstError,
        ExecutionPipeline pipeline,
        ActionListener<Object> listener
    ) {
        if (firstError != null && pipeline.hasOnFail()) {
            // Execute ON_ANY_FAIL handlers
            continuationExecutor.executeContinuationsAsync(
                pipeline.getOnFailHandlers(),
                results,
                firstError,
                ActionListener.wrap(
                    r -> listener.onResponse(results),
                    listener::onFailure
                )
            );
        } else if (firstError == null && pipeline.hasOnDone()) {
            // Execute ON_ALL_DONE handlers
            continuationExecutor.executeContinuationsAsync(
                pipeline.getOnDoneHandlers(),
                results,
                null,
                ActionListener.wrap(
                    r -> listener.onResponse(r != null ? r : results),
                    listener::onFailure
                )
            );
        } else {
            // No handlers or mixed success
            if (firstError != null) {
                listener.onFailure(firstError);
            } else {
                listener.onResponse(results);
            }
        }
    }

    private ExecutionPipeline buildParallelPipeline(
        List<ElasticScriptParser.Parallel_continuationContext> continuations
    ) {
        ExecutionPipeline.Builder builder = ExecutionPipeline.builder();

        for (ElasticScriptParser.Parallel_continuationContext continuation : continuations) {
            if (continuation instanceof ElasticScriptParser.OnAllDoneContinuationContext onAllDone) {
                builder.addOnDone(buildContinuation(Continuation.Type.ON_DONE, onAllDone.continuation_handler()));
            } else if (continuation instanceof ElasticScriptParser.OnAnyFailContinuationContext onAnyFail) {
                builder.addOnFail(buildContinuation(Continuation.Type.ON_FAIL, onAnyFail.continuation_handler()));
            } else if (continuation instanceof ElasticScriptParser.ParallelTrackAsContinuationContext trackAs) {
                String trackName = trackAs.STRING().getText();
                trackName = trackName.substring(1, trackName.length() - 1);
                builder.trackAs(trackName);
            } else if (continuation instanceof ElasticScriptParser.ParallelTimeoutContinuationContext timeout) {
                int seconds = Integer.parseInt(timeout.INT().getText());
                builder.timeout(seconds);
            }
        }

        return builder.build();
    }

    private Continuation buildContinuation(
        Continuation.Type type,
        ElasticScriptParser.Continuation_handlerContext handler
    ) {
        if (handler.lambda_continuation() != null) {
            String lambda = handler.lambda_continuation().getText();
            return new Continuation(type, lambda);
        } else {
            String handlerName = handler.ID().getText();
            List<String> bindings = new ArrayList<>();
            
            if (handler.continuation_arg_list() != null) {
                for (ElasticScriptParser.Continuation_argContext arg : handler.continuation_arg_list().continuation_arg()) {
                    if (arg.AT() != null) {
                        bindings.add("@" + arg.ID().getText());
                    } else {
                        bindings.add(arg.expression().getText());
                    }
                }
            }
            
            return new Continuation(type, handlerName, bindings);
        }
    }

    private void evaluateArgumentsSync(
        ElasticScriptParser.Argument_listContext argList,
        List<Object> results
    ) {
        // For parallel execution, we evaluate arguments synchronously for simplicity
        // A more robust implementation would handle this asynchronously
        for (ElasticScriptParser.ExpressionContext expr : argList.expression()) {
            try {
                // Use a blocking approach for simplicity
                Object value = evaluateExpressionBlocking(expr);
                results.add(value);
            } catch (Exception e) {
                LOGGER.warn("Failed to evaluate argument: {}", e.getMessage());
                results.add(null);
            }
        }
    }

    private Object evaluateExpressionBlocking(ElasticScriptParser.ExpressionContext expr) {
        // Simple synchronous evaluation
        // This is a simplified implementation; a full implementation would need
        // proper async handling with blocking
        final Object[] result = new Object[1];
        final Exception[] error = new Exception[1];
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        
        executor.getExpressionEvaluator().evaluateExpressionAsync(expr, new ActionListener<>() {
            @Override
            public void onResponse(Object value) {
                result[0] = value;
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                error[0] = e;
                latch.countDown();
            }
        });
        
        try {
            latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        if (error[0] != null) {
            throw new RuntimeException(error[0]);
        }
        
        return result[0];
    }
}


