/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.intent;

import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;

import java.util.Collections;
import java.util.List;

/**
 * Represents an INTENT definition in elastic-script.
 * 
 * An INTENT encapsulates:
 * - Name and description
 * - Parameters with optional defaults
 * - REQUIRES clause (pre-conditions that must be true)
 * - ACTIONS clause (statements to execute)
 * - ON_FAILURE clause (statements to execute on failure)
 * 
 * INTENTs encode SRE expertise and provide guardrails for AI agents.
 */
public class IntentDefinition {
    
    private final String name;
    private final String description;
    private final List<Parameter> parameters;
    private final List<ElasticScriptParser.Requires_conditionContext> requiresConditions;
    private final List<ElasticScriptParser.StatementContext> actions;
    private final List<ElasticScriptParser.StatementContext> onFailureActions;
    
    /**
     * Constructs an IntentDefinition.
     *
     * @param name The intent name
     * @param description Optional description of what the intent does
     * @param parameters List of parameters the intent accepts
     * @param requiresConditions Pre-conditions that must be true before execution
     * @param actions Statements to execute when the intent is invoked
     * @param onFailureActions Statements to execute if the intent fails
     */
    public IntentDefinition(
            String name,
            String description,
            List<Parameter> parameters,
            List<ElasticScriptParser.Requires_conditionContext> requiresConditions,
            List<ElasticScriptParser.StatementContext> actions,
            List<ElasticScriptParser.StatementContext> onFailureActions) {
        this.name = name;
        this.description = description != null ? description : "";
        this.parameters = parameters != null ? parameters : Collections.emptyList();
        this.requiresConditions = requiresConditions != null ? requiresConditions : Collections.emptyList();
        this.actions = actions != null ? actions : Collections.emptyList();
        this.onFailureActions = onFailureActions != null ? onFailureActions : Collections.emptyList();
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public List<Parameter> getParameters() {
        return parameters;
    }
    
    public List<ElasticScriptParser.Requires_conditionContext> getRequiresConditions() {
        return requiresConditions;
    }
    
    public List<ElasticScriptParser.StatementContext> getActions() {
        return actions;
    }
    
    public List<ElasticScriptParser.StatementContext> getOnFailureActions() {
        return onFailureActions;
    }
    
    public boolean hasRequires() {
        return !requiresConditions.isEmpty();
    }
    
    public boolean hasOnFailure() {
        return !onFailureActions.isEmpty();
    }
    
    /**
     * Builds a human-readable signature for this intent.
     */
    public String getSignature() {
        StringBuilder sig = new StringBuilder();
        sig.append(name).append("(");
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) sig.append(", ");
            Parameter p = parameters.get(i);
            sig.append(p.getName()).append(" ").append(p.getType());
        }
        sig.append(")");
        return sig.toString();
    }
    
    @Override
    public String toString() {
        return "IntentDefinition{" +
            "name='" + name + '\'' +
            ", description='" + description + '\'' +
            ", parameters=" + parameters.size() +
            ", requires=" + requiresConditions.size() +
            ", actions=" + actions.size() +
            ", onFailure=" + onFailureActions.size() +
            '}';
    }
}


