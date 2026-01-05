/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;

import java.util.List;

/**
 * Handler for VAR statements - variable declarations with type inference.
 * Example: VAR name = 'John';
 */
public class VarStatementHandler {
    private final ProcedureExecutor executor;

    public VarStatementHandler(ProcedureExecutor executor) {
        this.executor = executor;
    }

    public void handleAsync(ElasticScriptParser.Var_statementContext ctx, ActionListener<Object> listener) {
        List<ElasticScriptParser.Var_declarationContext> varDecls =
            ctx.var_declaration_list().var_declaration();
        processVarDeclarations(varDecls, 0, listener);
    }

    private void processVarDeclarations(List<ElasticScriptParser.Var_declarationContext> varDecls,
                                        int index, ActionListener<Object> listener) {
        if (index >= varDecls.size()) {
            listener.onResponse(null); // All declarations succeeded
            return;
        }

        ElasticScriptParser.Var_declarationContext varCtx = varDecls.get(index);
        String varName = varCtx.ID().getText();

        // VAR requires an initial value for type inference
        if (varCtx.expression() == null) {
            listener.onFailure(new RuntimeException(
                "VAR declaration for '" + varName + "' requires an initial value for type inference."));
            return;
        }

        // Evaluate the expression to get the value and infer type
        executor.evaluateExpressionAsync(varCtx.expression(), ActionListener.wrap(
            value -> {
                try {
                    executor.getContext().declareVariableWithInferredType(varName, value);
                    processVarDeclarations(varDecls, index + 1, listener);
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            },
            listener::onFailure
        ));
    }
}

