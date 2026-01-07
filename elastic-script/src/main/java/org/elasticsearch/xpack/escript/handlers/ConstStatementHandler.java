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
 * Handler for CONST statements - immutable constant declarations.
 * Example: CONST MAX_RETRIES NUMBER = 3;
 * Example: CONST API_URL = 'https://api.example.com'; (type inferred)
 */
public class ConstStatementHandler {
    private final ProcedureExecutor executor;

    public ConstStatementHandler(ProcedureExecutor executor) {
        this.executor = executor;
    }

    public void handleAsync(ElasticScriptParser.Const_statementContext ctx, ActionListener<Object> listener) {
        List<ElasticScriptParser.Const_declarationContext> constDecls =
            ctx.const_declaration_list().const_declaration();
        processConstDeclarations(constDecls, 0, listener);
    }

    private void processConstDeclarations(List<ElasticScriptParser.Const_declarationContext> constDecls,
                                          int index, ActionListener<Object> listener) {
        if (index >= constDecls.size()) {
            listener.onResponse(null); // All declarations succeeded
            return;
        }

        ElasticScriptParser.Const_declarationContext constCtx = constDecls.get(index);
        String constName = constCtx.ID().getText();

        // CONST requires an initial value
        if (constCtx.expression() == null) {
            listener.onFailure(new RuntimeException(
                "CONST declaration for '" + constName + "' requires an initial value."));
            return;
        }

        // Check if type is explicitly specified
        String explicitType = null;
        if (constCtx.datatype() != null) {
            explicitType = constCtx.datatype().getText();
        }

        final String finalExplicitType = explicitType;

        // Evaluate the expression to get the value
        executor.evaluateExpressionAsync(constCtx.expression(), ActionListener.wrap(
            value -> {
                try {
                    if (finalExplicitType != null) {
                        // Use explicit type
                        executor.getContext().declareConstant(constName, finalExplicitType, value);
                    } else {
                        // Infer type from value
                        executor.getContext().declareConstant(constName, value);
                    }
                    processConstDeclarations(constDecls, index + 1, listener);
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            },
            listener::onFailure
        ));
    }
}


