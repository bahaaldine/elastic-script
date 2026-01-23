/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.escript.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.exceptions.EScriptException;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;
import org.elasticsearch.xpack.escript.primitives.ReturnValue;
import org.elasticsearch.xpack.escript.utils.ActionListenerUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The TryCatchStatementHandler handles TRY-CATCH-FINALLY statements with support for
 * named exception types.
 * 
 * <h2>Syntax</h2>
 * <pre>
 * TRY
 *     -- statements that might throw
 * CATCH http_error
 *     -- handle HTTP errors specifically
 *     PRINT @error.message;
 * CATCH timeout_error
 *     -- handle timeouts specifically
 * CATCH
 *     -- catch-all for any other errors
 *     PRINT 'Unexpected error: ' || @error.message;
 * FINALLY
 *     -- cleanup code (always runs)
 * END TRY
 * </pre>
 * 
 * <h2>Exception Matching</h2>
 * <ul>
 *   <li>Named CATCH blocks (e.g., {@code CATCH http_error}) only match exceptions
 *       whose type equals the specified name (case-insensitive).</li>
 *   <li>A CATCH block without a name is a "catch-all" that matches any exception.</li>
 *   <li>CATCH blocks are evaluated in order; the first matching block is executed.</li>
 *   <li>If no CATCH block matches, the exception propagates after FINALLY runs.</li>
 * </ul>
 * 
 * <h2>The @error Variable</h2>
 * Within a CATCH block, the {@code @error} variable (accessed as {@code error}) is a
 * DOCUMENT containing:
 * <ul>
 *   <li>{@code message} - The error message</li>
 *   <li>{@code code} - Error code if provided (e.g., "HTTP_404")</li>
 *   <li>{@code type} - Exception type (e.g., "http_error", "timeout_error")</li>
 *   <li>{@code stack_trace} - Full Java stack trace</li>
 *   <li>{@code cause} - Cause message if present</li>
 * </ul>
 */
public class TryCatchStatementHandler {
    private final ProcedureExecutor executor;

    /**
     * Constructs a TryCatchStatementHandler with the given ProcedureExecutor.
     *
     * @param executor The ProcedureExecutor instance responsible for executing procedures.
     */
    public TryCatchStatementHandler(ProcedureExecutor executor) {
        this.executor = executor;
    }

    /**
     * Handles the TRY-CATCH-FINALLY statement asynchronously.
     *
     * @param ctx      The Try_catch_statementContext representing the TRY-CATCH-FINALLY statement.
     * @param listener The ActionListener to handle asynchronous callbacks.
     */
    public void handleAsync(ElasticScriptParser.Try_catch_statementContext ctx, ActionListener<Object> listener) {
        // Extract TRY statements (direct children of try_catch_statement before catch_block)
        List<ElasticScriptParser.StatementContext> tryStatements = ctx.statement();
        
        // Extract CATCH blocks
        List<ElasticScriptParser.Catch_blockContext> catchBlocks = ctx.catch_block();
        
        // Extract FINALLY statements (if present - they come after FINALLY keyword)
        List<ElasticScriptParser.StatementContext> finallyStatements = extractFinallyStatements(ctx);

        // Execute TRY block asynchronously
        ActionListener<Object> tryCatchListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object tryResult) {
                if (tryResult instanceof ReturnValue) {
                    // TRY block returned - run FINALLY then bubble up the ReturnValue
                    executeFinallyBlock(finallyStatements, ActionListener.wrap(
                        ignore -> listener.onResponse(tryResult),
                        listener::onFailure
                    ));
                } else {
                    // No ReturnValue - proceed to FINALLY
                    executeFinallyBlock(finallyStatements, listener);
                }
            }

            @Override
            public void onFailure(Exception e) {
                // Exception occurred in TRY block - find matching CATCH block
                handleException(e, catchBlocks, finallyStatements, listener);
            }
        };

        ActionListener<Object> tryCatchLogger = ActionListenerUtils.withLogging(
            tryCatchListener, 
            this.getClass().getName(),
            "Try-Catch:" + tryStatements.size() + " statements"
        );

        executeStatementsAsync(tryStatements, 0, tryCatchLogger);
    }

    /**
     * Handles an exception by finding and executing the appropriate CATCH block.
     * 
     * @param e The exception that was thrown
     * @param catchBlocks The list of CATCH blocks to search
     * @param finallyStatements The FINALLY statements to execute afterward
     * @param listener The completion listener
     */
    private void handleException(
            Exception e,
            List<ElasticScriptParser.Catch_blockContext> catchBlocks,
            List<ElasticScriptParser.StatementContext> finallyStatements,
            ActionListener<Object> listener) {
        
        // Convert to EScriptException for type matching
        EScriptException escriptException = EScriptException.from(e);
        
        // Find matching CATCH block
        CatchBlockMatch match = findMatchingCatchBlock(catchBlocks, escriptException);
        
        if (match != null) {
            // Bind @error variable before executing CATCH block
            bindErrorVariable(escriptException);
            
            // Execute the matching CATCH block
            executeStatementsAsync(match.statements, 0, new ActionListener<Object>() {
                @Override
                public void onResponse(Object catchResult) {
                    if (catchResult instanceof ReturnValue) {
                        executeFinallyBlock(finallyStatements, ActionListener.wrap(
                            ignore -> listener.onResponse(catchResult),
                            listener::onFailure
                        ));
                    } else {
                        executeFinallyBlock(finallyStatements, listener);
                    }
                }

                @Override
                public void onFailure(Exception ex) {
                    // Exception in CATCH block - run FINALLY then fail
                    executeFinallyBlock(finallyStatements, ActionListener.wrap(
                        unused -> listener.onFailure(ex),
                        listener::onFailure
                    ));
                }
            });
        } else {
            // No matching CATCH block - run FINALLY then rethrow
            executeFinallyBlock(finallyStatements, ActionListener.wrap(
                unused -> listener.onFailure(e),
                listener::onFailure
            ));
        }
    }

    /**
     * Represents a matched CATCH block with its statements.
     */
    private static class CatchBlockMatch {
        final String exceptionName; // null for catch-all
        final List<ElasticScriptParser.StatementContext> statements;
        
        CatchBlockMatch(String exceptionName, List<ElasticScriptParser.StatementContext> statements) {
            this.exceptionName = exceptionName;
            this.statements = statements;
        }
    }

    /**
     * Finds the first CATCH block that matches the given exception.
     * 
     * Named CATCH blocks match by exception type; catch-all blocks match everything.
     * Returns the first match found (blocks are checked in order).
     * 
     * @param catchBlocks The CATCH blocks to search
     * @param exception The exception to match
     * @return The matching CatchBlockMatch, or null if no match
     */
    private CatchBlockMatch findMatchingCatchBlock(
            List<ElasticScriptParser.Catch_blockContext> catchBlocks,
            EScriptException exception) {
        
        CatchBlockMatch catchAllMatch = null;
        
        for (ElasticScriptParser.Catch_blockContext catchBlock : catchBlocks) {
            // Check if this is a named catch (has ID) or catch-all
            if (catchBlock.ID() != null) {
                // Named exception: CATCH http_error
                String exceptionName = catchBlock.ID().getText();
                if (exception.matchesType(exceptionName)) {
                    return new CatchBlockMatch(exceptionName, catchBlock.statement());
                }
            } else {
                // Catch-all: CATCH (no name)
                // Save it but continue looking for a more specific match
                if (catchAllMatch == null) {
                    catchAllMatch = new CatchBlockMatch(null, catchBlock.statement());
                }
            }
        }
        
        // Return catch-all if no named match was found
        return catchAllMatch;
    }

    /**
     * Extracts FINALLY statements from the context.
     * 
     * The grammar is: TRY statement+ catch_block* (FINALLY statement+)? ENDTRY
     * The FINALLY statements are separate from TRY statements in the parse tree.
     */
    private List<ElasticScriptParser.StatementContext> extractFinallyStatements(
            ElasticScriptParser.Try_catch_statementContext ctx) {
        
        List<ElasticScriptParser.StatementContext> finallyStatements = new ArrayList<>();
        
        // Check if FINALLY keyword is present
        if (ctx.FINALLY() == null) {
            return finallyStatements;
        }
        
        // The statements after FINALLY are in the context
        // We need to find them by position - statements after the last catch_block
        // and before ENDTRY
        
        // Get all statements from the context
        List<ElasticScriptParser.StatementContext> allStatements = ctx.statement();
        
        // In the new grammar, statements directly in try_catch_statement are TRY statements.
        // FINALLY statements would be in a separate list if the grammar supported it.
        // For now, we need to parse the tree structure to find FINALLY statements.
        
        // Actually, looking at the grammar again:
        // try_catch_statement: TRY statement+ catch_block* (FINALLY statement+)? ENDTRY
        // 
        // The statement+ after FINALLY is a separate occurrence, but ANTLR combines
        // them into a single list via ctx.statement(). We need to identify which
        // statements belong to FINALLY.
        
        // For a cleaner implementation, let's check if there are any statements
        // between FINALLY token and ENDTRY token.
        // This requires walking the parse tree children.
        
        boolean inFinally = false;
        for (int i = 0; i < ctx.getChildCount(); i++) {
            var child = ctx.getChild(i);
            
            if (child instanceof org.antlr.v4.runtime.tree.TerminalNode) {
                String text = child.getText();
                if ("FINALLY".equalsIgnoreCase(text)) {
                    inFinally = true;
                    continue;
                }
                if ("END".equalsIgnoreCase(text) || "END TRY".equalsIgnoreCase(text)) {
                    break;
                }
            }
            
            if (inFinally && child instanceof ElasticScriptParser.StatementContext) {
                finallyStatements.add((ElasticScriptParser.StatementContext) child);
            }
        }
        
        return finallyStatements;
    }

    /**
     * Binds the @error variable in the current execution context.
     * 
     * @param e The exception that was caught
     */
    private void bindErrorVariable(EScriptException e) {
        ExecutionContext context = executor.getContext();
        Map<String, Object> errorDocument = e.toDocument();
        
        try {
            context.declareVariable("error", "DOCUMENT");
            context.setVariable("error", errorDocument);
        } catch (RuntimeException alreadyDeclared) {
            // Variable already exists from a previous catch, just update the value
            try {
                context.setVariable("error", errorDocument);
            } catch (Exception ignored) {
                // Shouldn't happen in normal operation
            }
        }
    }

    /**
     * Executes a list of statements asynchronously.
     */
    private void executeStatementsAsync(
            List<ElasticScriptParser.StatementContext> stmtCtxList, 
            int index,
            ActionListener<Object> listener) {
        
        if (index >= stmtCtxList.size()) {
            listener.onResponse(null);
            return;
        }

        ElasticScriptParser.StatementContext stmtCtx = stmtCtxList.get(index);

        ActionListener<Object> stmtListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                if (result instanceof ReturnValue) {
                    listener.onResponse(result);
                } else {
                    executeStatementsAsync(stmtCtxList, index + 1, listener);
                }
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        };

        executor.visitStatementAsync(stmtCtx, stmtListener);
    }

    /**
     * Executes the FINALLY block asynchronously.
     */
    private void executeFinallyBlock(
            List<ElasticScriptParser.StatementContext> finallyStatements, 
            ActionListener<Object> listener) {
        
        if (finallyStatements.isEmpty()) {
            listener.onResponse(null);
            return;
        }

        executeStatementsAsync(finallyStatements, 0, ActionListener.wrap(
            unused -> listener.onResponse(null),
            listener::onFailure
        ));
    }
}
