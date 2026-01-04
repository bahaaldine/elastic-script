/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.runbooks;

import org.elasticsearch.xpack.escript.functions.builtin.runbooks.http.HttpClient;
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.http.HttpClientException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Mock HTTP client for testing runbook functions.
 * Allows setting up expected responses for specific URL patterns.
 */
public class MockHttpClient implements HttpClient {

    private final List<MockResponse> mockResponses = new ArrayList<>();
    private final List<RecordedRequest> recordedRequests = new ArrayList<>();
    private String defaultResponse = "{}";
    private HttpClientException defaultException = null;

    /**
     * Add a mock response for a specific URL pattern.
     */
    public MockHttpClient whenUrl(String urlPattern) {
        MockResponse response = new MockResponse(Pattern.compile(urlPattern));
        mockResponses.add(response);
        return this;
    }

    /**
     * Add a mock response for a specific method and URL pattern.
     */
    public MockHttpClient when(String method, String urlPattern) {
        MockResponse response = new MockResponse(method, Pattern.compile(urlPattern));
        mockResponses.add(response);
        return this;
    }

    /**
     * Set the response for the last added mock.
     */
    public MockHttpClient thenReturn(String responseBody) {
        if (mockResponses.isEmpty()) {
            throw new IllegalStateException("Call when() or whenUrl() first");
        }
        mockResponses.get(mockResponses.size() - 1).responseBody = responseBody;
        return this;
    }

    /**
     * Set a dynamic response generator for the last added mock.
     */
    public MockHttpClient thenAnswer(Function<RecordedRequest, String> responseGenerator) {
        if (mockResponses.isEmpty()) {
            throw new IllegalStateException("Call when() or whenUrl() first");
        }
        mockResponses.get(mockResponses.size() - 1).responseGenerator = responseGenerator;
        return this;
    }

    /**
     * Set an exception to throw for the last added mock.
     */
    public MockHttpClient thenThrow(HttpClientException exception) {
        if (mockResponses.isEmpty()) {
            throw new IllegalStateException("Call when() or whenUrl() first");
        }
        mockResponses.get(mockResponses.size() - 1).exception = exception;
        return this;
    }

    /**
     * Set an HTTP error response for the last added mock.
     */
    public MockHttpClient thenError(int statusCode, String errorBody) {
        return thenThrow(new HttpClientException(statusCode, errorBody));
    }

    /**
     * Set the default response when no patterns match.
     */
    public MockHttpClient setDefaultResponse(String response) {
        this.defaultResponse = response;
        this.defaultException = null;
        return this;
    }

    /**
     * Set the default exception when no patterns match.
     */
    public MockHttpClient setDefaultException(HttpClientException exception) {
        this.defaultException = exception;
        return this;
    }

    /**
     * Get all recorded requests.
     */
    public List<RecordedRequest> getRecordedRequests() {
        return new ArrayList<>(recordedRequests);
    }

    /**
     * Get the last recorded request.
     */
    public RecordedRequest getLastRequest() {
        if (recordedRequests.isEmpty()) {
            return null;
        }
        return recordedRequests.get(recordedRequests.size() - 1);
    }

    /**
     * Clear recorded requests.
     */
    public void clearRecordedRequests() {
        recordedRequests.clear();
    }

    /**
     * Verify that a request was made with the specified method and URL pattern.
     */
    public boolean wasRequestMade(String method, String urlPattern) {
        Pattern pattern = Pattern.compile(urlPattern);
        return recordedRequests.stream().anyMatch(r -> 
            r.method.equals(method) && pattern.matcher(r.url).matches()
        );
    }

    /**
     * Count how many requests were made with the specified method.
     */
    public int countRequests(String method) {
        return (int) recordedRequests.stream().filter(r -> r.method.equals(method)).count();
    }

    @Override
    public String request(String method, String url, Map<String, String> headers, String body) throws HttpClientException {
        // Record the request
        RecordedRequest recorded = new RecordedRequest(method, url, headers, body);
        recordedRequests.add(recorded);

        // Find matching mock response
        for (MockResponse mock : mockResponses) {
            if (mock.matches(method, url)) {
                if (mock.exception != null) {
                    throw mock.exception;
                }
                if (mock.responseGenerator != null) {
                    return mock.responseGenerator.apply(recorded);
                }
                return mock.responseBody;
            }
        }

        // No match - use default
        if (defaultException != null) {
            throw defaultException;
        }
        return defaultResponse;
    }

    /**
     * Represents a recorded HTTP request.
     */
    public static class RecordedRequest {
        public final String method;
        public final String url;
        public final Map<String, String> headers;
        public final String body;

        public RecordedRequest(String method, String url, Map<String, String> headers, String body) {
            this.method = method;
            this.url = url;
            this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>();
            this.body = body;
        }

        public boolean hasHeader(String name) {
            return headers.containsKey(name);
        }

        public String getHeader(String name) {
            return headers.get(name);
        }

        public boolean bodyContains(String text) {
            return body != null && body.contains(text);
        }

        @Override
        public String toString() {
            return method + " " + url + (body != null ? " [body=" + body.length() + " chars]" : "");
        }
    }

    /**
     * Represents a mock response configuration.
     */
    private static class MockResponse {
        final String method;
        final Pattern urlPattern;
        String responseBody = "{}";
        HttpClientException exception = null;
        Function<RecordedRequest, String> responseGenerator = null;

        MockResponse(Pattern urlPattern) {
            this.method = null;
            this.urlPattern = urlPattern;
        }

        MockResponse(String method, Pattern urlPattern) {
            this.method = method;
            this.urlPattern = urlPattern;
        }

        boolean matches(String requestMethod, String requestUrl) {
            if (method != null && !method.equals(requestMethod)) {
                return false;
            }
            return urlPattern.matcher(requestUrl).find();
        }
    }
}


