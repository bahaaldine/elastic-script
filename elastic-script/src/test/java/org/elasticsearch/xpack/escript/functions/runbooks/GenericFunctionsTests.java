/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.runbooks;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.FunctionDefinition;
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.GenericFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.http.HttpClientHolder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for Generic HTTP/Webhook integration functions.
 */
public class GenericFunctionsTests extends ESTestCase {

    private ExecutionContext context;
    private MockHttpClient mockClient;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new ExecutionContext();
        GenericFunctions.registerAll(context);
        
        // Set up mock HTTP client
        mockClient = new MockHttpClient();
        HttpClientHolder.set(mockClient);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        HttpClientHolder.resetToDefault();
    }

    // ==================== Function Registration Tests ====================

    public void testWebhookFunctionRegistered() {
        FunctionDefinition func = context.getFunction("WEBHOOK");
        assertThat(func, notNullValue());
        assertEquals(1, func.getParameters().size());
        assertEquals("url", func.getParameters().get(0).getName());
    }

    public void testHttpGetFunctionRegistered() {
        FunctionDefinition func = context.getFunction("HTTP_GET");
        assertThat(func, notNullValue());
        assertEquals(1, func.getParameters().size());
        assertEquals("url", func.getParameters().get(0).getName());
    }

    public void testHttpPostFunctionRegistered() {
        FunctionDefinition func = context.getFunction("HTTP_POST");
        assertThat(func, notNullValue());
        assertEquals(2, func.getParameters().size());
        assertEquals("url", func.getParameters().get(0).getName());
        assertEquals("body", func.getParameters().get(1).getName());
    }

    // ==================== Mocked HTTP Tests ====================

    public void testHttpGetWithMockedResponse() throws Exception {
        // Set up the mock response
        mockClient.when("GET", ".*example.com.*")
            .thenReturn("{\"message\":\"Hello, World!\",\"status\":\"ok\"}");

        FunctionDefinition func = context.getFunction("HTTP_GET");
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        func.execute(List.of("https://api.example.com/data"), new ActionListener<>() {
            @Override
            public void onResponse(Object o) {
                result.set(o);
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                error.set(e);
                latch.countDown();
            }
        });
        
        latch.await();
        assertNull("Expected success but got error: " + error.get(), error.get());
        assertNotNull(result.get());
        assertTrue(result.get().toString().contains("Hello"));
    }

    public void testHttpPostWithMockedResponse() throws Exception {
        // Set up the mock response
        mockClient.when("POST", ".*webhook.example.com.*")
            .thenReturn("{\"received\":true,\"id\":\"12345\"}");

        FunctionDefinition func = context.getFunction("HTTP_POST");
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        func.execute(List.of("https://webhook.example.com/notify", "{\"event\":\"alert\",\"severity\":\"high\"}"), new ActionListener<>() {
            @Override
            public void onResponse(Object o) {
                result.set(o);
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                error.set(e);
                latch.countDown();
            }
        });
        
        latch.await();
        assertNull("Expected success but got error: " + error.get(), error.get());
        assertNotNull(result.get());
        assertTrue(result.get().toString().contains("received"));
        
        // Verify the request was made correctly
        MockHttpClient.RecordedRequest lastRequest = mockClient.getLastRequest();
        assertNotNull(lastRequest);
        assertEquals("POST", lastRequest.method);
        assertTrue(lastRequest.bodyContains("alert"));
    }

    public void testWebhookWithMockedResponse() throws Exception {
        // Set up the mock response
        mockClient.when("POST", ".*slack.com.*")
            .thenReturn("{\"ok\":true}");

        FunctionDefinition func = context.getFunction("WEBHOOK");
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        func.execute(List.of("https://hooks.slack.com/services/xxx/yyy/zzz"), new ActionListener<>() {
            @Override
            public void onResponse(Object o) {
                result.set(o);
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                error.set(e);
                latch.countDown();
            }
        });
        
        latch.await();
        assertNull("Expected success but got error: " + error.get(), error.get());
        assertNotNull(result.get());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> response = (Map<String, Object>) result.get();
        assertTrue((Boolean) response.get("success"));
    }

    // ==================== Parameter Type Tests ====================

    public void testWebhookParameterTypes() {
        FunctionDefinition func = context.getFunction("WEBHOOK");
        assertEquals("STRING", func.getParameters().get(0).getType());
    }

    public void testHttpPostParameterTypes() {
        FunctionDefinition func = context.getFunction("HTTP_POST");
        assertEquals("STRING", func.getParameters().get(0).getType());
        assertEquals("STRING", func.getParameters().get(1).getType());
    }
}
