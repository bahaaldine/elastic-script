/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.workflows;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.FunctionDefinition;
import org.elasticsearch.xpack.escript.functions.runbooks.MockHttpClient;
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.http.HttpClientHolder;
import org.elasticsearch.xpack.escript.functions.builtin.workflows.WorkflowFunctions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for Elastic Workflows integration functions.
 */
public class WorkflowFunctionsTests extends ESTestCase {

    private ExecutionContext context;
    private MockHttpClient mockClient;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new ExecutionContext();
        WorkflowFunctions.registerAll(context);
        
        mockClient = new MockHttpClient();
        HttpClientHolder.set(mockClient);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        HttpClientHolder.resetToDefault();
    }

    // ==================== Function Registration Tests ====================

    public void testWorkflowTriggerFunctionRegistered() {
        FunctionDefinition func = context.getFunction("WORKFLOW_TRIGGER");
        assertThat(func, notNullValue());
        assertEquals(3, func.getParameters().size());
        assertEquals("workflow_id", func.getParameters().get(0).getName());
        assertEquals("inputs", func.getParameters().get(1).getName());
        assertEquals("kibana_url", func.getParameters().get(2).getName());
    }

    public void testWorkflowListFunctionRegistered() {
        FunctionDefinition func = context.getFunction("WORKFLOW_LIST");
        assertThat(func, notNullValue());
        assertEquals(1, func.getParameters().size());
        assertEquals("kibana_url", func.getParameters().get(0).getName());
    }

    public void testWorkflowGetFunctionRegistered() {
        FunctionDefinition func = context.getFunction("WORKFLOW_GET");
        assertThat(func, notNullValue());
        assertEquals(2, func.getParameters().size());
        assertEquals("workflow_id", func.getParameters().get(0).getName());
    }

    public void testWorkflowStatusFunctionRegistered() {
        FunctionDefinition func = context.getFunction("WORKFLOW_STATUS");
        assertThat(func, notNullValue());
        assertEquals(2, func.getParameters().size());
        assertEquals("execution_id", func.getParameters().get(0).getName());
    }

    public void testWorkflowCreateFunctionRegistered() {
        FunctionDefinition func = context.getFunction("WORKFLOW_CREATE");
        assertThat(func, notNullValue());
        assertEquals(2, func.getParameters().size());
        assertEquals("yaml", func.getParameters().get(0).getName());
    }

    public void testWorkflowDeleteFunctionRegistered() {
        FunctionDefinition func = context.getFunction("WORKFLOW_DELETE");
        assertThat(func, notNullValue());
        assertEquals(2, func.getParameters().size());
        assertEquals("workflow_id", func.getParameters().get(0).getName());
    }

    // ==================== Mocked HTTP Tests ====================

    public void testWorkflowTriggerWithMockedResponse() throws Exception {
        mockClient.when("POST", ".*/api/workflows/.*/run")
            .thenReturn("{\"execution_id\":\"exec-123\",\"status\":\"running\"}");

        FunctionDefinition func = context.getFunction("WORKFLOW_TRIGGER");
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("ip_address", "8.8.8.8");
        
        func.execute(List.of("workflow-abc", inputs, "http://localhost:5601"), new ActionListener<>() {
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
        assertEquals("exec-123", response.get("execution_id"));
    }

    public void testWorkflowListWithMockedResponse() throws Exception {
        mockClient.when("GET", ".*/api/workflows$")
            .thenReturn("{\"workflows\":[{\"id\":\"wf-1\",\"name\":\"Test Workflow\",\"enabled\":true}]}");

        FunctionDefinition func = context.getFunction("WORKFLOW_LIST");
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        func.execute(List.of("http://localhost:5601"), new ActionListener<>() {
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
        List<Map<String, Object>> workflows = (List<Map<String, Object>>) result.get();
        assertEquals(1, workflows.size());
        assertEquals("wf-1", workflows.get(0).get("id"));
        assertEquals("Test Workflow", workflows.get(0).get("name"));
    }

    public void testWorkflowCreateWithMockedResponse() throws Exception {
        mockClient.when("POST", ".*/api/workflows$")
            .thenReturn("{\"id\":\"wf-new\",\"name\":\"New Workflow\",\"created\":true}");

        FunctionDefinition func = context.getFunction("WORKFLOW_CREATE");
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        String yaml = "name: Test\\nsteps:\\n  - name: step1\\n    type: console";
        
        func.execute(List.of(yaml, "http://localhost:5601"), new ActionListener<>() {
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
        assertEquals("wf-new", response.get("id"));
    }

    public void testWorkflowStatusWithMockedResponse() throws Exception {
        mockClient.when("GET", ".*/api/workflows/executions/.*")
            .thenReturn("{\"execution_id\":\"exec-123\",\"status\":\"completed\",\"duration_ms\":1234}");

        FunctionDefinition func = context.getFunction("WORKFLOW_STATUS");
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        func.execute(List.of("exec-123", "http://localhost:5601"), new ActionListener<>() {
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
        assertEquals("completed", response.get("status"));
        assertEquals(1234, response.get("duration_ms"));
    }

    // ==================== Parameter Type Tests ====================

    public void testWorkflowTriggerParameterTypes() {
        FunctionDefinition func = context.getFunction("WORKFLOW_TRIGGER");
        assertEquals("STRING", func.getParameters().get(0).getType());
        assertEquals("DOCUMENT", func.getParameters().get(1).getType());
        assertEquals("STRING", func.getParameters().get(2).getType());
    }

    public void testWorkflowCreateParameterTypes() {
        FunctionDefinition func = context.getFunction("WORKFLOW_CREATE");
        assertEquals("STRING", func.getParameters().get(0).getType());
        assertEquals("STRING", func.getParameters().get(1).getType());
    }
}
