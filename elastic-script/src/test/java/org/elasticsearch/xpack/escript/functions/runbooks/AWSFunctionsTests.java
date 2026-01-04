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
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.AWSFunctions;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for AWS integration functions.
 * Note: AWS functions use custom SigV4 signing and are not yet refactored to use HttpClientHolder.
 */
public class AWSFunctionsTests extends ESTestCase {

    private ExecutionContext context;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new ExecutionContext();
        AWSFunctions.registerAll(context);
    }

    // ==================== Function Registration Tests ====================

    public void testAwsSsmRunFunctionRegistered() {
        FunctionDefinition func = context.getFunction("AWS_SSM_RUN");
        assertThat(func, notNullValue());
        assertEquals(2, func.getParameters().size());
        assertEquals("instance_ids", func.getParameters().get(0).getName());
        assertEquals("command", func.getParameters().get(1).getName());
    }

    public void testAwsSsmStatusFunctionRegistered() {
        FunctionDefinition func = context.getFunction("AWS_SSM_STATUS");
        assertThat(func, notNullValue());
        assertEquals(2, func.getParameters().size());
        assertEquals("command_id", func.getParameters().get(0).getName());
        assertEquals("instance_id", func.getParameters().get(1).getName());
    }

    public void testAwsLambdaInvokeFunctionRegistered() {
        FunctionDefinition func = context.getFunction("AWS_LAMBDA_INVOKE");
        assertThat(func, notNullValue());
        assertEquals(1, func.getParameters().size());
        assertEquals("function_name", func.getParameters().get(0).getName());
    }

    public void testAwsEc2RebootFunctionRegistered() {
        FunctionDefinition func = context.getFunction("AWS_EC2_REBOOT");
        assertThat(func, notNullValue());
        assertEquals(1, func.getParameters().size());
        assertEquals("instance_ids", func.getParameters().get(0).getName());
    }

    public void testAwsEc2StartFunctionRegistered() {
        FunctionDefinition func = context.getFunction("AWS_EC2_START");
        assertThat(func, notNullValue());
        assertEquals(1, func.getParameters().size());
        assertEquals("instance_ids", func.getParameters().get(0).getName());
    }

    public void testAwsEc2StopFunctionRegistered() {
        FunctionDefinition func = context.getFunction("AWS_EC2_STOP");
        assertThat(func, notNullValue());
        assertEquals(1, func.getParameters().size());
        assertEquals("instance_ids", func.getParameters().get(0).getName());
    }

    public void testAwsAsgSetCapacityFunctionRegistered() {
        FunctionDefinition func = context.getFunction("AWS_ASG_SET_CAPACITY");
        assertThat(func, notNullValue());
        assertEquals(2, func.getParameters().size());
        assertEquals("asg_name", func.getParameters().get(0).getName());
        assertEquals("desired_capacity", func.getParameters().get(1).getName());
    }

    public void testAwsAsgDescribeFunctionRegistered() {
        FunctionDefinition func = context.getFunction("AWS_ASG_DESCRIBE");
        assertThat(func, notNullValue());
        assertEquals(1, func.getParameters().size());
        assertEquals("asg_name", func.getParameters().get(0).getName());
    }

    // ==================== Configuration Tests ====================

    public void testAwsSsmRunRequiresConfiguration() throws Exception {
        FunctionDefinition func = context.getFunction("AWS_SSM_RUN");
        assertThat(func, notNullValue());
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        func.execute(List.of("i-1234567890", "echo hello"), new ActionListener<>() {
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
        assertTrue(error.get().getMessage().contains("AWS_ACCESS_KEY_ID") || 
                   error.get().getMessage().contains("not configured"));
    }

    public void testAwsLambdaInvokeRequiresConfiguration() throws Exception {
        FunctionDefinition func = context.getFunction("AWS_LAMBDA_INVOKE");
        assertThat(func, notNullValue());
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        func.execute(List.of("my-function"), new ActionListener<>() {
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
        assertTrue(error.get().getMessage().contains("AWS_ACCESS_KEY_ID") || 
                   error.get().getMessage().contains("not configured"));
    }

    // ==================== Parameter Type Tests ====================

    public void testAwsSsmRunParameterTypes() {
        FunctionDefinition func = context.getFunction("AWS_SSM_RUN");
        assertEquals("STRING", func.getParameters().get(0).getType());
        assertEquals("STRING", func.getParameters().get(1).getType());
    }

    public void testAwsAsgSetCapacityParameterTypes() {
        FunctionDefinition func = context.getFunction("AWS_ASG_SET_CAPACITY");
        assertEquals("STRING", func.getParameters().get(0).getType());
        assertEquals("NUMBER", func.getParameters().get(1).getType());
    }
}
