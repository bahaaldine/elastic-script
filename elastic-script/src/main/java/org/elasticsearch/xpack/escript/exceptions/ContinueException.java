/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V.
 * under one or more contributor license agreements. Licensed under the Elastic
 * License 2.0; you may not use this file except in compliance with the Elastic
 * License 2.0.
 */

package org.elasticsearch.xpack.escript.exceptions;

/**
 * The ContinueException is a custom exception used to control loop execution flow.
 * When a CONTINUE statement is encountered within a loop, this exception is thrown
 * to skip to the next iteration of the loop.
 */
public class ContinueException extends RuntimeException {
    /**
     * Constructs a ContinueException with the specified message.
     *
     * @param message The detail message.
     */
    public ContinueException(String message) {
        super(message);
    }
}



