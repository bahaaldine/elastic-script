/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.builtin.runbooks.http;

/**
 * Exception thrown by HttpClient operations.
 */
public class HttpClientException extends Exception {
    
    private final int statusCode;
    private final String responseBody;

    public HttpClientException(String message) {
        super(message);
        this.statusCode = -1;
        this.responseBody = null;
    }

    public HttpClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.responseBody = null;
    }

    public HttpClientException(int statusCode, String responseBody) {
        super("HTTP " + statusCode + ": " + responseBody);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public boolean isHttpError() {
        return statusCode > 0;
    }
}

