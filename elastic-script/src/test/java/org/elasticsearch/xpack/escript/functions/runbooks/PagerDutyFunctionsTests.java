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
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.PagerDutyFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.http.HttpClientHolder;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for PagerDuty integration functions.
 */
public class PagerDutyFunctionsTests extends ESTestCase {

    private ExecutionContext context;
    private MockHttpClient mockClient;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new ExecutionContext();
        PagerDutyFunctions.registerAll(context);
        
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

    public void testPagerDutyTriggerFunctionRegistered() {
        FunctionDefinition func = context.getFunction("PAGERDUTY_TRIGGER");
        assertThat(func, notNullValue());
        assertEquals(1, func.getParameters().size());
        assertEquals("summary", func.getParameters().get(0).getName());
    }

    public void testPagerDutyResolveFunctionRegistered() {
        FunctionDefinition func = context.getFunction("PAGERDUTY_RESOLVE");
        assertThat(func, notNullValue());
        assertEquals(1, func.getParameters().size());
        assertEquals("dedup_key", func.getParameters().get(0).getName());
    }

    public void testPagerDutyAcknowledgeFunctionRegistered() {
        FunctionDefinition func = context.getFunction("PAGERDUTY_ACKNOWLEDGE");
        assertThat(func, notNullValue());
        assertEquals(1, func.getParameters().size());
        assertEquals("dedup_key", func.getParameters().get(0).getName());
    }

    public void testPagerDutyAddNoteFunctionRegistered() {
        FunctionDefinition func = context.getFunction("PAGERDUTY_ADD_NOTE");
        assertThat(func, notNullValue());
        assertEquals(3, func.getParameters().size());
        assertEquals("incident_id", func.getParameters().get(0).getName());
        assertEquals("content", func.getParameters().get(1).getName());
        assertEquals("user_email", func.getParameters().get(2).getName());
    }

    public void testPagerDutyGetIncidentFunctionRegistered() {
        FunctionDefinition func = context.getFunction("PAGERDUTY_GET_INCIDENT");
        assertThat(func, notNullValue());
        assertEquals(1, func.getParameters().size());
        assertEquals("incident_id", func.getParameters().get(0).getName());
    }

    public void testPagerDutyListIncidentsFunctionRegistered() {
        FunctionDefinition func = context.getFunction("PAGERDUTY_LIST_INCIDENTS");
        assertThat(func, notNullValue());
        assertEquals(0, func.getParameters().size());
    }

    // ==================== Configuration Tests ====================

    public void testPagerDutyTriggerRequiresConfiguration() throws Exception {
        FunctionDefinition func = context.getFunction("PAGERDUTY_TRIGGER");
        assertThat(func, notNullValue());
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        func.execute(List.of("Test incident"), new ActionListener<>() {
            @Override
            public void onResponse(Object o) {
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                error.set(e);
                latch.countDown();
            }
        });
        
        latch.await();
        assertNotNull(error.get());
        assertTrue(error.get().getMessage().contains("ROUTING_KEY") || 
                   error.get().getMessage().contains("not configured"));
    }

    // ==================== Mocked HTTP Tests ====================

    public void testPagerDutyTriggerWithMockedResponse() throws Exception {
        // Set up the mock response
        mockClient.when("POST", ".*events.pagerduty.com.*")
            .thenReturn("{\"status\":\"success\",\"message\":\"Event processed\",\"dedup_key\":\"test-dedup-key-123\"}");

        FunctionDefinition func = context.getFunction("PAGERDUTY_TRIGGER");
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        // Call with summary, severity, dedup_key, and routing_key (index 4)
        // Use Arrays.asList() because List.of() doesn't allow null values
        func.execute(Arrays.asList("Database connection failed", "critical", null, null, "test-routing-key"), new ActionListener<>() {
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
        assertEquals("test-dedup-key-123", result.get());
        
        // Verify the request was made correctly
        MockHttpClient.RecordedRequest lastRequest = mockClient.getLastRequest();
        assertNotNull(lastRequest);
        assertEquals("POST", lastRequest.method);
        assertTrue(lastRequest.bodyContains("trigger"));
        assertTrue(lastRequest.bodyContains("Database connection failed"));
    }

    public void testPagerDutyResolveWithMockedResponse() throws Exception {
        // Set up the mock response
        mockClient.when("POST", ".*events.pagerduty.com.*")
            .thenReturn("{\"status\":\"success\",\"message\":\"Event processed\",\"dedup_key\":\"my-incident-key\"}");

        FunctionDefinition func = context.getFunction("PAGERDUTY_RESOLVE");
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        // Call with dedup_key and routing_key (via index 1 which is used for both api_key and routing_key)
        func.execute(List.of("my-incident-key", "test-routing-key"), new ActionListener<>() {
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
        assertEquals(true, result.get());
        
        // Verify the request included "resolve" action
        MockHttpClient.RecordedRequest lastRequest = mockClient.getLastRequest();
        assertTrue(lastRequest.bodyContains("resolve"));
    }

    // ==================== Parameter Type Tests ====================

    public void testPagerDutyTriggerParameterTypes() {
        FunctionDefinition func = context.getFunction("PAGERDUTY_TRIGGER");
        assertEquals("STRING", func.getParameters().get(0).getType());
    }

    public void testPagerDutyAddNoteParameterTypes() {
        FunctionDefinition func = context.getFunction("PAGERDUTY_ADD_NOTE");
        assertEquals("STRING", func.getParameters().get(0).getType());
        assertEquals("STRING", func.getParameters().get(1).getType());
        assertEquals("STRING", func.getParameters().get(2).getType());
    }
}
