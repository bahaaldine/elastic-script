/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.builtin.runbooks.http;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Default HTTP client implementation using HttpURLConnection.
 */
public class DefaultHttpClient implements HttpClient {
    
    private static final Logger LOGGER = LogManager.getLogger(DefaultHttpClient.class);
    
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public DefaultHttpClient() {
        this(30000, 30000);
    }

    public DefaultHttpClient(int connectTimeoutMs, int readTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    @Override
    public String request(String method, String urlStr, Map<String, String> headers, String body) throws HttpClientException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            
            conn.setRequestMethod(method);
            conn.setConnectTimeout(connectTimeoutMs);
            conn.setReadTimeout(readTimeoutMs);
            
            // Set headers
            if (headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    conn.setRequestProperty(header.getKey(), header.getValue());
                }
            }
            
            // Write body if present
            if (body != null && !body.isEmpty()) {
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }
            
            int responseCode = conn.getResponseCode();
            String responseBody = readResponse(conn, responseCode);
            
            if (responseCode >= 400) {
                throw new HttpClientException(responseCode, responseBody);
            }
            
            return responseBody;
            
        } catch (HttpClientException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("HTTP request failed: {} {}", method, urlStr, e);
            throw new HttpClientException("HTTP request failed: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String readResponse(HttpURLConnection conn, int responseCode) throws Exception {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                    StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (response.length() > 0) {
                    response.append("\n");
                }
                response.append(line);
            }
        }
        return response.toString();
    }
}



