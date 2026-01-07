/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V.
 * under one or more contributor license agreements. Licensed under
 * the Elastic License 2.0; you may not use this file except in compliance
 * with the Elastic License 2.0.
 */

package org.elasticsearch.xpack.escript.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;
import org.elasticsearch.xpack.escript.intent.IntentDefinition;
import org.elasticsearch.xpack.escript.intent.IntentRegistry;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler for DEFINE INTENT statements.
 * 
 * Parses the intent definition from the AST and registers it in the IntentRegistry.
 * 
 * Syntax:
 * DEFINE INTENT name(params)
 * DESCRIPTION 'description'
 * REQUIRES
 *     condition1,
 *     condition2
 * ACTIONS
 *     statement+
 * ON_FAILURE
 *     statement+
 * END INTENT
 */
public class DefineIntentStatementHandler {

    private final ProcedureExecutor executor;

    /**
     * Constructs a DefineIntentStatementHandler with the given ProcedureExecutor.
     *
     * @param executor The ProcedureExecutor instance.
     */
    public DefineIntentStatementHandler(ProcedureExecutor executor) {
        this.executor = executor;
    }

    /**
     * Handles the DEFINE INTENT statement asynchronously.
     *
     * @param ctx      The Define_intent_statementContext from the parser.
     * @param listener The ActionListener to handle async callbacks.
     */
    public void handleAsync(ElasticScriptParser.Define_intent_statementContext ctx, ActionListener<Object> listener) {
        try {
            // Get the intent name
            String intentName = ctx.ID().getText();

            // Check if intent already exists
            IntentRegistry registry = IntentRegistry.getInstance();
            if (registry.exists(intentName)) {
                listener.onFailure(new RuntimeException("Intent '" + intentName + "' is already defined."));
                return;
            }

            // Parse description (optional)
            String description = null;
            if (ctx.STRING() != null) {
                description = ctx.STRING().getText();
                // Remove quotes
                if (description.startsWith("'") || description.startsWith("\"")) {
                    description = description.substring(1, description.length() - 1);
                }
            }

            // Parse parameters
            List<Parameter> parameters = new ArrayList<>();
            if (ctx.parameter_list() != null) {
                for (ElasticScriptParser.ParameterContext paramCtx : ctx.parameter_list().parameter()) {
                    ParameterMode mode = ParameterMode.IN; // default mode
                    String paramName;
                    String paramType;
                    
                    // Check if mode is specified
                    if (paramCtx.getChild(0).getText().equalsIgnoreCase("IN") ||
                        paramCtx.getChild(0).getText().equalsIgnoreCase("OUT") ||
                        paramCtx.getChild(0).getText().equalsIgnoreCase("INOUT")) {
                        String modeStr = paramCtx.getChild(0).getText().toUpperCase();
                        mode = ParameterMode.valueOf(modeStr);
                        paramName = paramCtx.getChild(1).getText();
                        paramType = paramCtx.getChild(2).getText().toUpperCase();
                    } else {
                        paramName = paramCtx.getChild(0).getText();
                        paramType = paramCtx.getChild(1).getText().toUpperCase();
                    }
                    
                    // Check for duplicate parameter names
                    for (Parameter existing : parameters) {
                        if (existing.getName().equals(paramName)) {
                            listener.onFailure(new RuntimeException("Duplicate parameter name '" +
                                paramName + "' in intent '" + intentName + "'."));
                            return;
                        }
                    }
                    parameters.add(new Parameter(paramName, paramType, mode));
                }
            }

            // Parse REQUIRES conditions
            List<ElasticScriptParser.Requires_conditionContext> requiresConditions = new ArrayList<>();
            if (ctx.requires_clause() != null) {
                requiresConditions.addAll(ctx.requires_clause().requires_condition());
            }

            // Parse ACTIONS statements
            List<ElasticScriptParser.StatementContext> actions = new ArrayList<>();
            if (ctx.actions_clause() != null) {
                actions.addAll(ctx.actions_clause().statement());
            }

            // Parse ON_FAILURE statements
            List<ElasticScriptParser.StatementContext> onFailureActions = new ArrayList<>();
            if (ctx.on_failure_clause() != null) {
                onFailureActions.addAll(ctx.on_failure_clause().statement());
            }

            // Create the IntentDefinition
            IntentDefinition intent = new IntentDefinition(
                intentName,
                description,
                parameters,
                requiresConditions,
                actions,
                onFailureActions
            );

            // Register the intent
            registry.register(intent);

            listener.onResponse("Intent '" + intentName + "' defined successfully.");
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
}


