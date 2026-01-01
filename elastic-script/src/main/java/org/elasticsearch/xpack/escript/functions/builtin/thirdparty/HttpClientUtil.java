/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.builtin.thirdparty;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Utility class for making HTTP requests to external services.
 * HTTP calls are made synchronously but the caller handles async via ActionListener.
 * The execution already happens on Elasticsearch's generic thread pool.
 */
public class HttpClientUtil {

    private static final Logger LOGGER = LogManager.getLogger(HttpClientUtil.class);

    private static final int DEFAULT_CONNECT_TIMEOUT = 30000; // 30 seconds
    private static final int DEFAULT_READ_TIMEOUT = 120000;   // 120 seconds for LLM responses

    /**
     * Performs an HTTP POST request with JSON body.
     * The call is synchronous but wrapped in ActionListener for consistency.
     *
     * @param url      The URL to POST to
     * @param headers  Map of header name to header value
     * @param jsonBody The JSON request body
     * @param listener Callback for the response or error
     */
    public static void postJson(String url, Map<String, String> headers, String jsonBody, ActionListener<String> listener) {
        HttpURLConnection connection = null;
        try {
            connection = createConnection(url, "POST", headers);
            
            // Write request body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Read response
            int responseCode = connection.getResponseCode();
            String responseBody = readResponse(connection, responseCode);

            if (responseCode >= 200 && responseCode < 300) {
                listener.onResponse(responseBody);
            } else {
                listener.onFailure(new RuntimeException(
                    "HTTP " + responseCode + ": " + responseBody
                ));
            }
        } catch (Exception e) {
            LOGGER.error("HTTP request failed", e);
            listener.onFailure(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Performs an HTTP GET request.
     * The call is synchronous but wrapped in ActionListener for consistency.
     *
     * @param url      The URL to GET
     * @param headers  Map of header name to header value
     * @param listener Callback for the response or error
     */
    public static void get(String url, Map<String, String> headers, ActionListener<String> listener) {
        HttpURLConnection connection = null;
        try {
            connection = createConnection(url, "GET", headers);

            int responseCode = connection.getResponseCode();
            String responseBody = readResponse(connection, responseCode);

            if (responseCode >= 200 && responseCode < 300) {
                listener.onResponse(responseBody);
            } else {
                listener.onFailure(new RuntimeException(
                    "HTTP " + responseCode + ": " + responseBody
                ));
            }
        } catch (Exception e) {
            LOGGER.error("HTTP request failed", e);
            listener.onFailure(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static HttpURLConnection createConnection(String url, String method, Map<String, String> headers) throws IOException {
        URL urlObj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
        connection.setReadTimeout(DEFAULT_READ_TIMEOUT);

        // Set default headers
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");

        // Set custom headers
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
        }

        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            connection.setDoOutput(true);
        }

        return connection;
    }

    private static String readResponse(HttpURLConnection connection, int responseCode) throws IOException {
        BufferedReader reader;
        if (responseCode >= 200 && responseCode < 300) {
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        } else {
            reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));
        }

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        return response.toString();
    }
}
