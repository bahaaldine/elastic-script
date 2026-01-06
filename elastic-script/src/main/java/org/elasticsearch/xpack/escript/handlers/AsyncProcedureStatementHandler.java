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
import org.elasticsearch.xpack.escript.execution.ExecutionStatus;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles async procedure statements with pipe continuations.
 *
 * Syntax: {@code procedure_name(args) | ON_DONE handler(@result) | ON_FAIL handler(@error) | TRACK AS 'name';}
 */
public class AsyncProcedureStatementHandler {

    private static final Logger LOGGER = LogManager.getLogger(AsyncProcedureStatementHandler.class);

    private final ProcedureExecutor executor;
    private final ExecutionRegistry registry;
    private final ContinuationExecutor continuationExecutor;

    public AsyncProcedureStatementHandler(
        ProcedureExecutor executor,
        ExecutionRegistry registry,
        ContinuationExecutor continuationExecutor
    ) {
        this.executor = executor;
        this.registry = registry;
        this.continuationExecutor = continuationExecutor;
    }

    /**
     * Handles an async procedure statement.
     */
    public void handleAsync(
        ElasticScriptParser.Async_procedure_statementContext ctx,
        ActionListener<Object> listener
    ) {
        try {
            // 1. Extract procedure name
            String procedureName = ctx.ID().getText();
            LOGGER.info("Executing async procedure: {}", procedureName);

            // 2. Evaluate arguments
            List<Object> args = new ArrayList<>();
            if (ctx.argument_list() != null) {
                evaluateArgumentsAsync(ctx.argument_list(), args, ActionListener.wrap(
                    evaluatedArgs -> {
                        executeProcedureWithPipeline(procedureName, evaluatedArgs, ctx, listener);
                    },
                    listener::onFailure
                ));
            } else {
                executeProcedureWithPipeline(procedureName, args, ctx, listener);
            }
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    private void executeProcedureWithPipeline(
        String procedureName,
        List<Object> args,
        ElasticScriptParser.Async_procedure_statementContext ctx,
        ActionListener<Object> listener
    ) {
        // 3. Build execution pipeline from continuations
        ExecutionPipeline pipeline = buildPipeline(ctx.pipe_continuation());

        // 4. Convert args to parameters map
        Map<String, Object> parameters = new HashMap<>();
        for (int i = 0; i < args.size(); i++) {
            parameters.put("arg" + i, args.get(i));
        }

        // 5. Create execution in registry
        registry.createExecution(procedureName, parameters, pipeline, ActionListener.wrap(
            executionState -> {
                String executionId = executionState.getExecutionId();
                LOGGER.info("Created execution: {} for procedure: {}", executionId, procedureName);

                // 6. Mark as running
                registry.markRunning(executionId, ActionListener.wrap(
                    runningState -> {
                        // 7. Execute the procedure asynchronously
                        executeProcedureAsync(procedureName, args, executionId, pipeline, listener);
                    },
                    listener::onFailure
                ));
            },
            listener::onFailure
        ));
    }

    private void executeProcedureAsync(
        String procedureName,
        List<Object> args,
        String executionId,
        ExecutionPipeline pipeline,
        ActionListener<Object> listener
    ) {
        // Call the procedure
        executor.callProcedureByNameAsync(procedureName, args, ActionListener.wrap(
            result -> {
                // Procedure succeeded
                LOGGER.info("Procedure {} completed successfully, result type: {}", 
                    procedureName, result != null ? result.getClass().getSimpleName() : "null");

                // Mark as completed
                registry.markCompleted(executionId, result, ActionListener.wrap(
                    completedState -> {
                        // Execute ON_DONE continuations
                        if (pipeline.hasOnDone()) {
                            continuationExecutor.executeContinuationsAsync(
                                pipeline.getOnDoneHandlers(),
                                result,
                                null,
                                ActionListener.wrap(
                                    continuationResult -> executeFinally(executionId, pipeline, listener, null),
                                    e -> executeFinally(executionId, pipeline, listener, e)
                                )
                            );
                        } else {
                            executeFinally(executionId, pipeline, listener, null);
                        }
                    },
                    listener::onFailure
                ));
            },
            error -> {
                // Procedure failed
                LOGGER.warn("Procedure {} failed: {}", procedureName, error.getMessage());

                // Mark as failed
                registry.markFailed(executionId, error.getMessage(), ActionListener.wrap(
                    failedState -> {
                        // Execute ON_FAIL continuations
                        if (pipeline.hasOnFail()) {
                            continuationExecutor.executeContinuationsAsync(
                                pipeline.getOnFailHandlers(),
                                null,
                                error,
                                ActionListener.wrap(
                                    continuationResult -> executeFinally(executionId, pipeline, listener, error),
                                    e -> executeFinally(executionId, pipeline, listener, e)
                                )
                            );
                        } else {
                            executeFinally(executionId, pipeline, listener, error);
                        }
                    },
                    e -> listener.onFailure(new RuntimeException("Failed to mark execution as failed", e))
                ));
            }
        ));
    }

    private void executeFinally(
        String executionId,
        ExecutionPipeline pipeline,
        ActionListener<Object> listener,
        Exception previousError
    ) {
        if (pipeline.hasFinally()) {
            continuationExecutor.executeContinuationsAsync(
                pipeline.getFinallyHandlers(),
                null,
                null,
                ActionListener.wrap(
                    result -> completeExecution(executionId, listener, previousError),
                    e -> {
                        LOGGER.warn("FINALLY handler failed: {}", e.getMessage());
                        completeExecution(executionId, listener, previousError != null ? previousError : e);
                    }
                )
            );
        } else {
            completeExecution(executionId, listener, previousError);
        }
    }

    private void completeExecution(String executionId, ActionListener<Object> listener, Exception error) {
        // Return the execution ID to the caller
        // The caller can use this to query status later
        if (error != null) {
            // Even if there was an error, we consider the async statement itself successful
            // The error is stored in the execution state
            LOGGER.debug("Execution {} completed with error stored", executionId);
        }
        listener.onResponse(executionId);
    }

    private ExecutionPipeline buildPipeline(List<ElasticScriptParser.Pipe_continuationContext> continuations) {
        ExecutionPipeline.Builder builder = ExecutionPipeline.builder();

        for (ElasticScriptParser.Pipe_continuationContext continuation : continuations) {
            if (continuation instanceof ElasticScriptParser.OnDoneContinuationContext onDone) {
                builder.addOnDone(buildContinuation(Continuation.Type.ON_DONE, onDone.continuation_handler()));
            } else if (continuation instanceof ElasticScriptParser.OnFailContinuationContext onFail) {
                builder.addOnFail(buildContinuation(Continuation.Type.ON_FAIL, onFail.continuation_handler()));
            } else if (continuation instanceof ElasticScriptParser.FinallyContinuationContext finallyCtx) {
                builder.addFinally(buildContinuation(Continuation.Type.FINALLY, finallyCtx.continuation_handler()));
            } else if (continuation instanceof ElasticScriptParser.TrackAsContinuationContext trackAs) {
                String trackName = trackAs.STRING().getText();
                // Remove quotes
                trackName = trackName.substring(1, trackName.length() - 1);
                builder.trackAs(trackName);
            } else if (continuation instanceof ElasticScriptParser.TimeoutContinuationContext timeout) {
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
            // Inline lambda
            String lambda = handler.lambda_continuation().getText();
            return new Continuation(type, lambda);
        } else {
            // Named handler
            String handlerName = handler.ID().getText();
            List<String> bindings = new ArrayList<>();
            
            if (handler.continuation_arg_list() != null) {
                for (ElasticScriptParser.Continuation_argContext arg : handler.continuation_arg_list().continuation_arg()) {
                    if (arg.AT() != null) {
                        // @result, @error, etc.
                        bindings.add("@" + arg.ID().getText());
                    } else {
                        // Regular expression - for now, just capture the text
                        bindings.add(arg.expression().getText());
                    }
                }
            }
            
            return new Continuation(type, handlerName, bindings);
        }
    }

    private void evaluateArgumentsAsync(
        ElasticScriptParser.Argument_listContext argList,
        List<Object> results,
        ActionListener<List<Object>> listener
    ) {
        List<ElasticScriptParser.ExpressionContext> expressions = argList.expression();
        evaluateExpressionsRecursive(expressions, 0, results, listener);
    }

    private void evaluateExpressionsRecursive(
        List<ElasticScriptParser.ExpressionContext> expressions,
        int index,
        List<Object> results,
        ActionListener<List<Object>> listener
    ) {
        if (index >= expressions.size()) {
            listener.onResponse(results);
            return;
        }

        executor.getExpressionEvaluator().evaluateExpressionAsync(expressions.get(index), ActionListener.wrap(
            value -> {
                results.add(value);
                evaluateExpressionsRecursive(expressions, index + 1, results, listener);
            },
            listener::onFailure
        ));
    }
}

