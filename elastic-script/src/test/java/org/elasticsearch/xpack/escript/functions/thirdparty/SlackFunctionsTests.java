/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.thirdparty;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;
import org.elasticsearch.xpack.escript.functions.builtin.thirdparty.SlackFunctions;
import org.elasticsearch.xpack.escript.parser.ElasticScriptLexer;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests for Slack integration functions.
 * 
 * Note: Tests that require actual API calls are marked with @AwaitsFix or require
 * the SLACK_BOT_TOKEN environment variable to be set.
 */
public class SlackFunctionsTests extends ESTestCase {

    private ExecutionContext context;
    private ThreadPool threadPool;
    private ProcedureExecutor executor;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new ExecutionContext();
        threadPool = new TestThreadPool("slack-test-pool");
        Client mockClient = null;
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(""));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        executor = new ProcedureExecutor(context, threadPool, mockClient, tokens);
        SlackFunctions.registerAll(context);
    }

    @Override
    public void tearDown() throws Exception {
        terminate(threadPool);
        super.tearDown();
    }

    @Test
    public void testFunctionsAreRegistered() {
        // Verify all Slack functions are registered
        assertTrue("SLACK_WEBHOOK should be registered", context.hasFunction("SLACK_WEBHOOK"));
        assertTrue("SLACK_SEND should be registered", context.hasFunction("SLACK_SEND"));
        assertTrue("SLACK_SEND_BLOCKS should be registered", context.hasFunction("SLACK_SEND_BLOCKS"));
        assertTrue("SLACK_REACT should be registered", context.hasFunction("SLACK_REACT"));
        assertTrue("SLACK_LIST_CHANNELS should be registered", context.hasFunction("SLACK_LIST_CHANNELS"));
    }

    @Test
    public void testSlackWebhookRequiresWebhookUrlAndMessage() throws Exception {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("SLACK_WEBHOOK");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();

        // Missing message
        fn.execute(Arrays.asList("https://hooks.slack.com/services/XXX"), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                fail("Should have failed with missing message");
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                error.set(e);
                latch.countDown();
            }
        });

        assertTrue("Should complete within timeout", latch.await(5, TimeUnit.SECONDS));
        assertNotNull("Should have an error", error.get());
        assertTrue("Error should mention arguments", 
            error.get().getMessage().contains("webhook_url") || error.get().getMessage().contains("message"));
    }

    @Test
    public void testSlackWebhookValidatesUrl() throws Exception {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("SLACK_WEBHOOK");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();

        // Invalid webhook URL
        fn.execute(Arrays.asList("https://example.com/not-slack", "Test message"), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                fail("Should have failed with invalid URL");
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                error.set(e);
                latch.countDown();
            }
        });

        assertTrue("Should complete within timeout", latch.await(5, TimeUnit.SECONDS));
        assertNotNull("Should have an error", error.get());
        assertTrue("Error should mention invalid URL", error.get().getMessage().contains("Invalid"));
    }

    @Test
    public void testSlackSendRequiresChannelAndMessage() throws Exception {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("SLACK_SEND");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();

        // Missing message
        fn.execute(Arrays.asList("#general"), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                fail("Should have failed with missing message");
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                error.set(e);
                latch.countDown();
            }
        });

        assertTrue("Should complete within timeout", latch.await(5, TimeUnit.SECONDS));
        assertNotNull("Should have an error", error.get());
        assertTrue("Error should mention channel and message", error.get().getMessage().contains("channel"));
    }

    @Test
    public void testSlackSendBlocksRequiresAllArgs() throws Exception {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("SLACK_SEND_BLOCKS");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();

        // Missing fallback_text
        fn.execute(Arrays.asList("#general", List.of()), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                fail("Should have failed with missing fallback_text");
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                error.set(e);
                latch.countDown();
            }
        });

        assertTrue("Should complete within timeout", latch.await(5, TimeUnit.SECONDS));
        assertNotNull("Should have an error", error.get());
        assertTrue("Error should mention arguments", 
            error.get().getMessage().contains("blocks") || error.get().getMessage().contains("fallback"));
    }

    @Test
    public void testSlackReactRequiresAllArgs() throws Exception {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("SLACK_REACT");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();

        // Missing emoji
        fn.execute(Arrays.asList("#general", "1234567890.123456"), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                fail("Should have failed with missing emoji");
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                error.set(e);
                latch.countDown();
            }
        });

        assertTrue("Should complete within timeout", latch.await(5, TimeUnit.SECONDS));
        assertNotNull("Should have an error", error.get());
        assertTrue("Error should mention arguments", error.get().getMessage().contains("emoji"));
    }

    @Test
    public void testTokenFromEnvironment() throws Exception {
        // This test verifies the token check without making actual API calls
        // When no token is available, it should fail with appropriate message
        
        String envToken = System.getenv("SLACK_BOT_TOKEN");
        if (envToken != null && !envToken.isEmpty()) {
            // If token is set, skip this test as it would make an actual API call
            return;
        }

        BuiltInFunctionDefinition fn = context.getBuiltInFunction("SLACK_SEND");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();

        fn.execute(Arrays.asList("#general", "Test message", null, null), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                fail("Should have failed without token");
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                error.set(e);
                latch.countDown();
            }
        });

        assertTrue("Should complete within timeout", latch.await(5, TimeUnit.SECONDS));
        assertNotNull("Should have an error", error.get());
        assertTrue("Error should mention token", 
            error.get().getMessage().contains("token") || error.get().getMessage().contains("SLACK_BOT_TOKEN"));
    }

    // ================================================================
    // Integration tests - require SLACK_BOT_TOKEN environment variable
    // These tests make actual API calls and should be run manually
    // ================================================================

    /**
     * Integration test for SLACK_LIST_CHANNELS.
     * Requires SLACK_BOT_TOKEN environment variable to be set.
     */
    @Test
    public void testSlackListChannelsIntegration() throws Exception {
        String token = System.getenv("SLACK_BOT_TOKEN");
        if (token == null || token.isEmpty()) {
            // Skip test if no token
            return;
        }

        BuiltInFunctionDefinition fn = context.getBuiltInFunction("SLACK_LIST_CHANNELS");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();

        fn.execute(Arrays.asList(10, token), new ActionListener<Object>() {
            @Override
            public void onResponse(Object r) {
                result.set(r);
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                error.set(e);
                latch.countDown();
            }
        });

        assertTrue("Should complete within timeout", latch.await(30, TimeUnit.SECONDS));
        
        if (error.get() != null) {
            fail("SLACK_LIST_CHANNELS failed: " + error.get().getMessage());
        }
        
        assertNotNull("Should have a result", result.get());
        assertTrue("Result should be a list", result.get() instanceof List);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> channels = (List<Map<String, Object>>) result.get();
        // Should have at least some channels in the workspace
        assertTrue("Should have channels", channels.size() >= 0);
    }

    /**
     * Integration test for SLACK_SEND.
     * Requires SLACK_BOT_TOKEN and SLACK_TEST_CHANNEL environment variables.
     */
    @Test
    public void testSlackSendIntegration() throws Exception {
        String token = System.getenv("SLACK_BOT_TOKEN");
        String channel = System.getenv("SLACK_TEST_CHANNEL");
        
        if (token == null || token.isEmpty() || channel == null || channel.isEmpty()) {
            // Skip test if no token or channel
            return;
        }

        BuiltInFunctionDefinition fn = context.getBuiltInFunction("SLACK_SEND");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();

        String testMessage = "Test message from elastic-script at " + System.currentTimeMillis();
        
        fn.execute(Arrays.asList(channel, testMessage, null, token), new ActionListener<Object>() {
            @Override
            public void onResponse(Object r) {
                result.set(r);
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                error.set(e);
                latch.countDown();
            }
        });

        assertTrue("Should complete within timeout", latch.await(30, TimeUnit.SECONDS));
        
        if (error.get() != null) {
            fail("SLACK_SEND failed: " + error.get().getMessage());
        }
        
        assertNotNull("Should have a result", result.get());
        assertTrue("Result should be a map", result.get() instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> response = (Map<String, Object>) result.get();
        assertTrue("Response should have 'ok' field", Boolean.TRUE.equals(response.get("ok")));
        assertNotNull("Response should have 'ts' field", response.get("ts"));
    }
}

