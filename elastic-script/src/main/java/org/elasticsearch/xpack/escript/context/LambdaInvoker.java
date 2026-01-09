/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.context;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.escript.primitives.LambdaExpression;

import java.util.List;

/**
 * Functional interface for invoking lambda expressions.
 * This allows built-in functions to invoke lambda expressions passed as arguments.
 */
@FunctionalInterface
public interface LambdaInvoker {
    /**
     * Invokes a lambda expression with the given arguments.
     *
     * @param lambda The lambda expression to invoke
     * @param args The arguments to pass to the lambda
     * @param listener The listener to receive the result
     */
    void invoke(LambdaExpression lambda, List<Object> args, ActionListener<Object> listener);
}



