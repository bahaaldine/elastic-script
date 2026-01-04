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
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.KubernetesFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.http.HttpClientHolder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for Kubernetes integration functions.
 */
public class KubernetesFunctionsTests extends ESTestCase {

    private ExecutionContext context;
    private MockHttpClient mockClient;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new ExecutionContext();
        KubernetesFunctions.registerAll(context);
        
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

    public void testK8sScaleFunctionRegistered() {
        FunctionDefinition func = context.getFunction("K8S_SCALE");
        assertThat(func, notNullValue());
        assertEquals(2, func.getParameters().size());
        assertEquals("deployment", func.getParameters().get(0).getName());
        assertEquals("replicas", func.getParameters().get(1).getName());
    }

    public void testK8sRestartFunctionRegistered() {
        FunctionDefinition func = context.getFunction("K8S_RESTART");
        assertThat(func, notNullValue());
        assertEquals(1, func.getParameters().size());
        assertEquals("deployment", func.getParameters().get(0).getName());
    }

    public void testK8sGetFunctionRegistered() {
        FunctionDefinition func = context.getFunction("K8S_GET");
        assertThat(func, notNullValue());
        assertEquals(2, func.getParameters().size());
        assertEquals("resource_type", func.getParameters().get(0).getName());
        assertEquals("name", func.getParameters().get(1).getName());
    }

    public void testK8sGetPodsFunctionRegistered() {
        FunctionDefinition func = context.getFunction("K8S_GET_PODS");
        assertThat(func, notNullValue());
        assertEquals(0, func.getParameters().size());
    }

    public void testK8sDeleteFunctionRegistered() {
        FunctionDefinition func = context.getFunction("K8S_DELETE");
        assertThat(func, notNullValue());
        assertEquals(2, func.getParameters().size());
        assertEquals("resource_type", func.getParameters().get(0).getName());
        assertEquals("name", func.getParameters().get(1).getName());
    }

    public void testK8sLogsFunctionRegistered() {
        FunctionDefinition func = context.getFunction("K8S_LOGS");
        assertThat(func, notNullValue());
        assertEquals(1, func.getParameters().size());
        assertEquals("pod", func.getParameters().get(0).getName());
    }

    public void testK8sDescribeFunctionRegistered() {
        FunctionDefinition func = context.getFunction("K8S_DESCRIBE");
        assertThat(func, notNullValue());
        assertEquals(2, func.getParameters().size());
        assertEquals("resource_type", func.getParameters().get(0).getName());
        assertEquals("name", func.getParameters().get(1).getName());
    }

    // ==================== Configuration Tests ====================

    public void testK8sScaleRequiresConfiguration() throws Exception {
        FunctionDefinition func = context.getFunction("K8S_SCALE");
        assertThat(func, notNullValue());
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        func.execute(List.of("my-deployment", 3), new ActionListener<>() {
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
        assertTrue(error.get().getMessage().contains("not configured") || 
                   error.get().getMessage().contains("K8S_API_SERVER"));
    }

    // ==================== Mocked HTTP Tests ====================

    public void testK8sScaleWithMockedResponse() throws Exception {
        // Set up the mock response
        mockClient.when("PUT", ".*/deployments/.*/scale")
            .thenReturn("{\"kind\":\"Scale\",\"apiVersion\":\"autoscaling/v1\",\"metadata\":{\"name\":\"my-deployment\"},\"spec\":{\"replicas\":3}}");

        FunctionDefinition func = context.getFunction("K8S_SCALE");
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        // Call with all required params including api_server and token
        func.execute(List.of("my-deployment", 3, "default", "https://k8s.example.com", "test-token"), new ActionListener<>() {
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
        
        // Verify the request was made correctly
        MockHttpClient.RecordedRequest lastRequest = mockClient.getLastRequest();
        assertNotNull(lastRequest);
        assertEquals("PUT", lastRequest.method);
        assertTrue(lastRequest.url.contains("/deployments/my-deployment/scale"));
        assertTrue(lastRequest.hasHeader("Authorization"));
        assertEquals("Bearer test-token", lastRequest.getHeader("Authorization"));
    }

    public void testK8sGetPodsWithMockedResponse() throws Exception {
        // Set up the mock response
        String mockResponse = "{\"kind\":\"PodList\",\"items\":[{\"metadata\":{\"name\":\"pod-1\"}},{\"metadata\":{\"name\":\"pod-2\"}}]}";
        mockClient.when("GET", ".*/pods.*")
            .thenReturn(mockResponse);

        FunctionDefinition func = context.getFunction("K8S_GET_PODS");
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        // Call with label selector, namespace, api_server, and token
        func.execute(List.of("app=myapp", "default", "https://k8s.example.com", "test-token"), new ActionListener<>() {
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
        assertTrue(result.get() instanceof List);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pods = (List<Map<String, Object>>) result.get();
        assertEquals(2, pods.size());
    }

    public void testK8sRestartWithMockedResponse() throws Exception {
        // Set up the mock response
        mockClient.when("PATCH", ".*/deployments/.*")
            .thenReturn("{\"kind\":\"Deployment\",\"metadata\":{\"name\":\"my-deployment\"}}");

        FunctionDefinition func = context.getFunction("K8S_RESTART");
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        func.execute(List.of("my-deployment", "default", "https://k8s.example.com", "test-token"), new ActionListener<>() {
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
        
        // Verify the restart annotation was included in the request body
        MockHttpClient.RecordedRequest lastRequest = mockClient.getLastRequest();
        assertTrue(lastRequest.bodyContains("restartedAt"));
    }

    // ==================== Parameter Type Tests ====================

    public void testK8sScaleParameterTypes() {
        FunctionDefinition func = context.getFunction("K8S_SCALE");
        assertEquals("STRING", func.getParameters().get(0).getType());
        assertEquals("NUMBER", func.getParameters().get(1).getType());
    }

    public void testK8sGetParameterTypes() {
        FunctionDefinition func = context.getFunction("K8S_GET");
        assertEquals("STRING", func.getParameters().get(0).getType());
        assertEquals("STRING", func.getParameters().get(1).getType());
    }
}
