/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.builtin.runbooks.http;

import java.util.Map;

/**
 * HTTP client interface for runbook integrations.
 * This abstraction allows for easy mocking in tests.
 */
public interface HttpClient {
    
    /**
     * Perform an HTTP request.
     *
     * @param method HTTP method (GET, POST, PUT, PATCH, DELETE)
     * @param url Full URL to request
     * @param headers Map of headers to include
     * @param body Request body (null for GET/DELETE)
     * @return Response body as string
     * @throws HttpClientException on any HTTP or connection error
     */
    String request(String method, String url, Map<String, String> headers, String body) throws HttpClientException;

    /**
     * Convenience method for GET requests.
     */
    default String get(String url, Map<String, String> headers) throws HttpClientException {
        return request("GET", url, headers, null);
    }

    /**
     * Convenience method for POST requests.
     */
    default String post(String url, Map<String, String> headers, String body) throws HttpClientException {
        return request("POST", url, headers, body);
    }

    /**
     * Convenience method for PUT requests.
     */
    default String put(String url, Map<String, String> headers, String body) throws HttpClientException {
        return request("PUT", url, headers, body);
    }

    /**
     * Convenience method for PATCH requests.
     */
    default String patch(String url, Map<String, String> headers, String body) throws HttpClientException {
        return request("PATCH", url, headers, body);
    }

    /**
     * Convenience method for DELETE requests.
     */
    default String delete(String url, Map<String, String> headers) throws HttpClientException {
        return request("DELETE", url, headers, null);
    }
}



