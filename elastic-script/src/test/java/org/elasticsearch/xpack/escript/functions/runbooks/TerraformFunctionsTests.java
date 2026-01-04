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
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.TerraformFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.http.HttpClientHolder;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for Terraform Cloud integration functions.
 */
public class TerraformFunctionsTests extends ESTestCase {

    private ExecutionContext context;
    private MockHttpClient mockClient;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new ExecutionContext();
        TerraformFunctions.registerAll(context);
        
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

    public void testTfCloudRunFunctionRegistered() {
        FunctionDefinition func = context.getFunction("TF_CLOUD_RUN");
        assertThat(func, notNullValue());
        assertEquals(1, func.getParameters().size());
        assertEquals("workspace", func.getParameters().get(0).getName());
    }

    public void testTfCloudStatusFunctionRegistered() {
        FunctionDefinition func = context.getFunction("TF_CLOUD_STATUS");
        assertThat(func, notNullValue());
        assertEquals(1, func.getParameters().size());
        assertEquals("run_id", func.getParameters().get(0).getName());
    }

    public void testTfCloudWaitFunctionRegistered() {
        FunctionDefinition func = context.getFunction("TF_CLOUD_WAIT");
        assertThat(func, notNullValue());
        assertEquals(1, func.getParameters().size());
        assertEquals("run_id", func.getParameters().get(0).getName());
    }

    public void testTfCloudOutputsFunctionRegistered() {
        FunctionDefinition func = context.getFunction("TF_CLOUD_OUTPUTS");
        assertThat(func, notNullValue());
        assertEquals(1, func.getParameters().size());
        assertEquals("workspace", func.getParameters().get(0).getName());
    }

    // ==================== Configuration Tests ====================

    public void testTfCloudRunRequiresConfiguration() throws Exception {
        FunctionDefinition func = context.getFunction("TF_CLOUD_RUN");
        assertThat(func, notNullValue());
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        func.execute(List.of("my-workspace"), new ActionListener<>() {
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
        assertTrue(error.get().getMessage().contains("TFC_TOKEN") || 
                   error.get().getMessage().contains("not configured"));
    }

    // ==================== Mocked HTTP Tests ====================

    public void testTfCloudStatusWithMockedResponse() throws Exception {
        // Set up the mock response
        mockClient.when("GET", ".*/runs/run-.*")
            .thenReturn("{\"data\":{\"id\":\"run-abc123\",\"attributes\":{\"status\":\"applied\"}}}");

        FunctionDefinition func = context.getFunction("TF_CLOUD_STATUS");
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        // Call with run_id and token
        func.execute(List.of("run-abc123", "test-token"), new ActionListener<>() {
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
        assertEquals("applied", result.get());
    }

    // ==================== Parameter Type Tests ====================

    public void testTfCloudRunParameterTypes() {
        FunctionDefinition func = context.getFunction("TF_CLOUD_RUN");
        assertEquals("STRING", func.getParameters().get(0).getType());
    }

    public void testTfCloudStatusParameterTypes() {
        FunctionDefinition func = context.getFunction("TF_CLOUD_STATUS");
        assertEquals("STRING", func.getParameters().get(0).getType());
    }
}
