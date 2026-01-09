/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.builtin.runbooks.http;

/**
 * Static holder for the HttpClient instance.
 * Allows tests to inject a mock implementation.
 */
public class HttpClientHolder {
    
    private static volatile HttpClient instance = new DefaultHttpClient();

    /**
     * Get the current HttpClient instance.
     */
    public static HttpClient get() {
        return instance;
    }

    /**
     * Set a custom HttpClient (for testing).
     * Call resetToDefault() after tests to restore the default client.
     */
    public static void set(HttpClient client) {
        instance = client;
    }

    /**
     * Reset to the default HttpClient implementation.
     */
    public static void resetToDefault() {
        instance = new DefaultHttpClient();
    }
}




