/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.primitives;

import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;

import java.util.List;

/**
 * Represents a lambda expression in elastic-script.
 * 
 * Lambda expressions have the form:
 * - (param1, param2) => expression
 * - param => expression (single parameter without parens)
 * 
 * When invoked, the lambda creates a temporary scope with the parameters
 * bound to the provided arguments, then evaluates the body expression.
 */
public class LambdaExpression {
    
    private final List<String> parameters;
    private final ElasticScriptParser.ExpressionContext bodyExpression;
    
    public LambdaExpression(List<String> parameters, ElasticScriptParser.ExpressionContext bodyExpression) {
        this.parameters = parameters;
        this.bodyExpression = bodyExpression;
    }
    
    /**
     * Returns the list of parameter names for this lambda.
     */
    public List<String> getParameters() {
        return parameters;
    }
    
    /**
     * Returns the number of parameters this lambda expects.
     */
    public int getParameterCount() {
        return parameters.size();
    }
    
    /**
     * Returns the body expression that will be evaluated when the lambda is invoked.
     */
    public ElasticScriptParser.ExpressionContext getBodyExpression() {
        return bodyExpression;
    }
    
    @Override
    public String toString() {
        if (parameters.size() == 1) {
            return parameters.get(0) + " => <expression>";
        }
        return "(" + String.join(", ", parameters) + ") => <expression>";
    }
}


