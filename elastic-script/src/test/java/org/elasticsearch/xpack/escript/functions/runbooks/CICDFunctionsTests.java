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
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.CICDFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.http.HttpClientHolder;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for CI/CD integration functions.
 */
public class CICDFunctionsTests extends ESTestCase {

    private ExecutionContext context;
    private MockHttpClient mockClient;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new ExecutionContext();
        CICDFunctions.registerAll(context);
        
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

    public void testJenkinsBuildFunctionRegistered() {
        FunctionDefinition func = context.getFunction("JENKINS_BUILD");
        assertThat(func, notNullValue());
        assertEquals(1, func.getParameters().size());
        assertEquals("job", func.getParameters().get(0).getName());
    }

    public void testJenkinsStatusFunctionRegistered() {
        FunctionDefinition func = context.getFunction("JENKINS_STATUS");
        assertThat(func, notNullValue());
        assertEquals(1, func.getParameters().size());
        assertEquals("job", func.getParameters().get(0).getName());
    }

    public void testGitHubWorkflowFunctionRegistered() {
        FunctionDefinition func = context.getFunction("GITHUB_WORKFLOW");
        assertThat(func, notNullValue());
        assertEquals(2, func.getParameters().size());
        assertEquals("repo", func.getParameters().get(0).getName());
        assertEquals("workflow", func.getParameters().get(1).getName());
    }

    public void testGitHubWorkflowStatusFunctionRegistered() {
        FunctionDefinition func = context.getFunction("GITHUB_WORKFLOW_STATUS");
        assertThat(func, notNullValue());
        assertEquals(2, func.getParameters().size());
        assertEquals("repo", func.getParameters().get(0).getName());
        assertEquals("run_id", func.getParameters().get(1).getName());
    }

    public void testGitLabPipelineFunctionRegistered() {
        FunctionDefinition func = context.getFunction("GITLAB_PIPELINE");
        assertThat(func, notNullValue());
        assertEquals(2, func.getParameters().size());
        assertEquals("project", func.getParameters().get(0).getName());
        assertEquals("ref", func.getParameters().get(1).getName());
    }

    public void testArgoCDSyncFunctionRegistered() {
        FunctionDefinition func = context.getFunction("ARGOCD_SYNC");
        assertThat(func, notNullValue());
        assertEquals(1, func.getParameters().size());
        assertEquals("app", func.getParameters().get(0).getName());
    }

    // ==================== Configuration Tests ====================

    public void testJenkinsBuildRequiresConfiguration() throws Exception {
        FunctionDefinition func = context.getFunction("JENKINS_BUILD");
        assertThat(func, notNullValue());
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        func.execute(List.of("my-job"), new ActionListener<>() {
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
        assertTrue(error.get().getMessage().contains("JENKINS") || 
                   error.get().getMessage().contains("not configured"));
    }

    public void testGitHubWorkflowRequiresConfiguration() throws Exception {
        FunctionDefinition func = context.getFunction("GITHUB_WORKFLOW");
        assertThat(func, notNullValue());
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        func.execute(List.of("owner/repo", "workflow.yml"), new ActionListener<>() {
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
        assertTrue(error.get().getMessage().contains("GITHUB_TOKEN") || 
                   error.get().getMessage().contains("not configured"));
    }

    // ==================== Parameter Type Tests ====================

    public void testJenkinsBuildParameterTypes() {
        FunctionDefinition func = context.getFunction("JENKINS_BUILD");
        assertEquals("STRING", func.getParameters().get(0).getType());
    }

    public void testGitHubWorkflowParameterTypes() {
        FunctionDefinition func = context.getFunction("GITHUB_WORKFLOW");
        assertEquals("STRING", func.getParameters().get(0).getType());
        assertEquals("STRING", func.getParameters().get(1).getType());
    }

    public void testGitHubWorkflowStatusParameterTypes() {
        FunctionDefinition func = context.getFunction("GITHUB_WORKFLOW_STATUS");
        assertEquals("STRING", func.getParameters().get(0).getType());
        assertEquals("NUMBER", func.getParameters().get(1).getType());
    }
}
