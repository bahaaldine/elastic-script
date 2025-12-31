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
import org.elasticsearch.xpack.escript.functions.builtin.thirdparty.OpenAIFunctions;
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
 * Tests for OpenAI integration functions.
 * 
 * Note: Tests that require actual API calls are marked with @AwaitsFix or require
 * the OPENAI_API_KEY environment variable to be set.
 */
public class OpenAIFunctionsTests extends ESTestCase {

    private ExecutionContext context;
    private ThreadPool threadPool;
    private ProcedureExecutor executor;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new ExecutionContext();
        threadPool = new TestThreadPool("openai-test-pool");
        Client mockClient = null;
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(""));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        executor = new ProcedureExecutor(context, threadPool, mockClient, tokens);
        OpenAIFunctions.registerAll(context);
    }

    @Override
    public void tearDown() throws Exception {
        terminate(threadPool);
        super.tearDown();
    }

    @Test
    public void testFunctionsAreRegistered() {
        // Verify all OpenAI functions are registered
        assertTrue("LLM_COMPLETE should be registered", context.hasFunction("LLM_COMPLETE"));
        assertTrue("LLM_CHAT should be registered", context.hasFunction("LLM_CHAT"));
        assertTrue("LLM_EMBED should be registered", context.hasFunction("LLM_EMBED"));
        assertTrue("LLM_SUMMARIZE should be registered", context.hasFunction("LLM_SUMMARIZE"));
        assertTrue("LLM_CLASSIFY should be registered", context.hasFunction("LLM_CLASSIFY"));
        assertTrue("LLM_EXTRACT should be registered", context.hasFunction("LLM_EXTRACT"));
    }

    @Test
    public void testLlmCompleteRequiresPrompt() throws Exception {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("LLM_COMPLETE");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();

        fn.execute(Collections.emptyList(), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                fail("Should have failed with missing prompt");
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
        assertTrue("Error should mention prompt", error.get().getMessage().contains("prompt"));
    }

    @Test
    public void testLlmChatRequiresMessages() throws Exception {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("LLM_CHAT");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();

        fn.execute(Collections.emptyList(), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                fail("Should have failed with missing messages");
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
        assertTrue("Error should mention messages", error.get().getMessage().contains("messages"));
    }

    @Test
    public void testLlmEmbedRequiresText() throws Exception {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("LLM_EMBED");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();

        fn.execute(Collections.emptyList(), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                fail("Should have failed with missing text");
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
        assertTrue("Error should mention text", error.get().getMessage().contains("text"));
    }

    @Test
    public void testLlmClassifyRequiresTextAndCategories() throws Exception {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("LLM_CLASSIFY");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();

        // Only provide text, no categories
        fn.execute(Arrays.asList("Some text"), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                fail("Should have failed with missing categories");
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
        assertTrue("Error should mention categories", error.get().getMessage().contains("categories"));
    }

    @Test
    public void testLlmExtractRequiresTextAndFields() throws Exception {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("LLM_EXTRACT");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();

        // Only provide text, no fields
        fn.execute(Arrays.asList("Some text"), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                fail("Should have failed with missing fields");
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
        assertTrue("Error should mention fields", error.get().getMessage().contains("fields"));
    }

    @Test
    public void testApiKeyFromEnvironment() throws Exception {
        // This test verifies the API key check without making actual API calls
        // When no API key is available, it should fail with appropriate message
        
        String envKey = System.getenv("OPENAI_API_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            // If API key is set, skip this test as it would make an actual API call
            return;
        }

        BuiltInFunctionDefinition fn = context.getBuiltInFunction("LLM_COMPLETE");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();

        fn.execute(Arrays.asList("Test prompt", null, null), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                fail("Should have failed without API key");
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
        assertTrue("Error should mention API key", 
            error.get().getMessage().contains("API key") || error.get().getMessage().contains("OPENAI_API_KEY"));
    }

    // ================================================================
    // Integration tests - require OPENAI_API_KEY environment variable
    // These tests make actual API calls and should be run manually
    // ================================================================

    /**
     * Integration test for LLM_COMPLETE.
     * Requires OPENAI_API_KEY environment variable to be set.
     */
    @Test
    public void testLlmCompleteIntegration() throws Exception {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            // Skip test if no API key
            return;
        }

        BuiltInFunctionDefinition fn = context.getBuiltInFunction("LLM_COMPLETE");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();

        fn.execute(Arrays.asList("Say 'hello' and nothing else", "gpt-4o-mini", apiKey), new ActionListener<Object>() {
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
            fail("LLM_COMPLETE failed: " + error.get().getMessage());
        }
        
        assertNotNull("Should have a result", result.get());
        assertTrue("Result should be a string", result.get() instanceof String);
        String response = (String) result.get();
        assertTrue("Response should contain 'hello'", response.toLowerCase().contains("hello"));
    }

    /**
     * Integration test for LLM_EMBED.
     * Requires OPENAI_API_KEY environment variable to be set.
     */
    @Test
    public void testLlmEmbedIntegration() throws Exception {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            // Skip test if no API key
            return;
        }

        BuiltInFunctionDefinition fn = context.getBuiltInFunction("LLM_EMBED");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();

        fn.execute(Arrays.asList("Elasticsearch is a search engine", "text-embedding-3-small", apiKey), 
            new ActionListener<Object>() {
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
            fail("LLM_EMBED failed: " + error.get().getMessage());
        }
        
        assertNotNull("Should have a result", result.get());
        assertTrue("Result should be a list", result.get() instanceof List);
        
        @SuppressWarnings("unchecked")
        List<Double> embedding = (List<Double>) result.get();
        assertTrue("Embedding should have multiple dimensions", embedding.size() > 100);
    }

    /**
     * Integration test for LLM_CLASSIFY.
     * Requires OPENAI_API_KEY environment variable to be set.
     */
    @Test
    public void testLlmClassifyIntegration() throws Exception {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            // Skip test if no API key
            return;
        }

        BuiltInFunctionDefinition fn = context.getBuiltInFunction("LLM_CLASSIFY");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();

        List<String> categories = Arrays.asList("positive", "negative", "neutral");
        fn.execute(Arrays.asList("I love this product! It's amazing!", categories, apiKey), 
            new ActionListener<Object>() {
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
            fail("LLM_CLASSIFY failed: " + error.get().getMessage());
        }
        
        assertNotNull("Should have a result", result.get());
        assertTrue("Result should be a string", result.get() instanceof String);
        String classification = ((String) result.get()).toLowerCase();
        assertTrue("Classification should be 'positive'", classification.contains("positive"));
    }

    /**
     * Integration test for LLM_EXTRACT.
     * Requires OPENAI_API_KEY environment variable to be set.
     */
    @Test
    public void testLlmExtractIntegration() throws Exception {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            // Skip test if no API key
            return;
        }

        BuiltInFunctionDefinition fn = context.getBuiltInFunction("LLM_EXTRACT");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();

        List<String> fields = Arrays.asList("name", "age", "city");
        fn.execute(Arrays.asList("John Smith is 35 years old and lives in San Francisco.", fields, apiKey), 
            new ActionListener<Object>() {
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
            fail("LLM_EXTRACT failed: " + error.get().getMessage());
        }
        
        assertNotNull("Should have a result", result.get());
        assertTrue("Result should be a map", result.get() instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> extracted = (Map<String, Object>) result.get();
        assertTrue("Should have 'name' field", extracted.containsKey("name"));
        assertTrue("Name should contain 'John'", extracted.get("name").toString().contains("John"));
    }

    /**
     * Integration test for LLM_CHAT with conversation history.
     * Requires OPENAI_API_KEY environment variable to be set.
     */
    @Test
    public void testLlmChatIntegration() throws Exception {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            // Skip test if no API key
            return;
        }

        BuiltInFunctionDefinition fn = context.getBuiltInFunction("LLM_CHAT");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();

        List<Map<String, Object>> messages = Arrays.asList(
            Map.of("role", "system", "content", "You are a helpful assistant that only responds with 'YES' or 'NO'."),
            Map.of("role", "user", "content", "Is the sky blue?")
        );

        fn.execute(Arrays.asList(messages, "gpt-4o-mini", apiKey), new ActionListener<Object>() {
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
            fail("LLM_CHAT failed: " + error.get().getMessage());
        }
        
        assertNotNull("Should have a result", result.get());
        assertTrue("Result should be a string", result.get() instanceof String);
        String response = ((String) result.get()).toUpperCase();
        assertTrue("Response should be YES or NO", response.contains("YES") || response.contains("NO"));
    }
}

