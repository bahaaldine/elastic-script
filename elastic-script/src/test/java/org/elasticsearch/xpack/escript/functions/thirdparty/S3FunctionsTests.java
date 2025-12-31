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
import org.elasticsearch.xpack.escript.functions.builtin.thirdparty.S3Functions;
import org.elasticsearch.xpack.escript.parser.ElasticScriptLexer;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests for S3 integration functions.
 * 
 * Note: Tests that require actual API calls require AWS credentials:
 * - AWS_ACCESS_KEY_ID
 * - AWS_SECRET_ACCESS_KEY
 * - AWS_TEST_BUCKET (optional, for integration tests)
 */
public class S3FunctionsTests extends ESTestCase {

    private ExecutionContext context;
    private ThreadPool threadPool;
    private ProcedureExecutor executor;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new ExecutionContext();
        threadPool = new TestThreadPool("s3-test-pool");
        Client mockClient = null;
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(""));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        executor = new ProcedureExecutor(context, threadPool, mockClient, tokens);
        S3Functions.registerAll(context);
    }

    @Override
    public void tearDown() throws Exception {
        terminate(threadPool);
        super.tearDown();
    }

    @Test
    public void testFunctionsAreRegistered() {
        // Verify all S3 functions are registered
        assertTrue("S3_GET should be registered", context.hasFunction("S3_GET"));
        assertTrue("S3_PUT should be registered", context.hasFunction("S3_PUT"));
        assertTrue("S3_DELETE should be registered", context.hasFunction("S3_DELETE"));
        assertTrue("S3_LIST should be registered", context.hasFunction("S3_LIST"));
        assertTrue("S3_EXISTS should be registered", context.hasFunction("S3_EXISTS"));
    }

    @Test
    public void testS3GetRequiresBucketAndKey() throws Exception {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("S3_GET");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();

        // Missing key
        fn.execute(Arrays.asList("my-bucket"), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                fail("Should have failed with missing key");
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
            error.get().getMessage().contains("bucket") || error.get().getMessage().contains("key"));
    }

    @Test
    public void testS3PutRequiresBucketKeyAndContent() throws Exception {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("S3_PUT");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();

        // Missing content
        fn.execute(Arrays.asList("my-bucket", "test-key"), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                fail("Should have failed with missing content");
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
        assertTrue("Error should mention arguments", error.get().getMessage().contains("content"));
    }

    @Test
    public void testS3DeleteRequiresBucketAndKey() throws Exception {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("S3_DELETE");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();

        // Missing key
        fn.execute(Arrays.asList("my-bucket"), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                fail("Should have failed with missing key");
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
            error.get().getMessage().contains("bucket") || error.get().getMessage().contains("key"));
    }

    @Test
    public void testS3ListRequiresBucket() throws Exception {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("S3_LIST");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();

        // Empty args
        fn.execute(Arrays.asList(), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                fail("Should have failed with missing bucket");
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
        assertTrue("Error should mention bucket", error.get().getMessage().contains("bucket"));
    }

    @Test
    public void testS3ExistsRequiresBucketAndKey() throws Exception {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("S3_EXISTS");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();

        // Missing key
        fn.execute(Arrays.asList("my-bucket"), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                fail("Should have failed with missing key");
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
            error.get().getMessage().contains("bucket") || error.get().getMessage().contains("key"));
    }

    @Test
    public void testCredentialsFromEnvironment() throws Exception {
        // This test verifies the credentials check without making actual API calls
        String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
        String secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        
        if (accessKey != null && !accessKey.isEmpty() && secretKey != null && !secretKey.isEmpty()) {
            // If credentials are set, skip this test as it would attempt an actual API call
            return;
        }

        BuiltInFunctionDefinition fn = context.getBuiltInFunction("S3_GET");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();

        fn.execute(Arrays.asList("my-bucket", "test-key", null, null, null), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                fail("Should have failed without credentials");
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
        assertTrue("Error should mention credentials", 
            error.get().getMessage().contains("credentials") || 
            error.get().getMessage().contains("AWS_ACCESS_KEY_ID"));
    }

    // ================================================================
    // Integration tests - require AWS credentials
    // These tests make actual API calls and should be run manually
    // Set AWS_TEST_BUCKET environment variable to run these tests
    // ================================================================

    /**
     * Integration test for S3 PUT, GET, EXISTS, and DELETE.
     * Requires AWS credentials and AWS_TEST_BUCKET environment variable.
     */
    @Test
    public void testS3PutGetDeleteIntegration() throws Exception {
        String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
        String secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        String bucket = System.getenv("AWS_TEST_BUCKET");
        
        if (accessKey == null || accessKey.isEmpty() || 
            secretKey == null || secretKey.isEmpty() ||
            bucket == null || bucket.isEmpty()) {
            // Skip test if credentials or bucket not set
            return;
        }

        String testKey = "escript-test/test-" + System.currentTimeMillis() + ".txt";
        String testContent = "Hello from elastic-script test at " + System.currentTimeMillis();
        
        // Test PUT
        {
            BuiltInFunctionDefinition fn = context.getBuiltInFunction("S3_PUT");
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Object> result = new AtomicReference<>();
            AtomicReference<Exception> error = new AtomicReference<>();

            fn.execute(Arrays.asList(bucket, testKey, testContent, "text/plain", accessKey, secretKey, null), 
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
                fail("S3_PUT failed: " + error.get().getMessage());
            }
            assertTrue("PUT should return true", Boolean.TRUE.equals(result.get()));
        }

        // Test EXISTS (should be true after PUT)
        {
            BuiltInFunctionDefinition fn = context.getBuiltInFunction("S3_EXISTS");
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Object> result = new AtomicReference<>();
            AtomicReference<Exception> error = new AtomicReference<>();

            fn.execute(Arrays.asList(bucket, testKey, accessKey, secretKey, null), 
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
                fail("S3_EXISTS failed: " + error.get().getMessage());
            }
            assertTrue("EXISTS should return true", Boolean.TRUE.equals(result.get()));
        }

        // Test GET
        {
            BuiltInFunctionDefinition fn = context.getBuiltInFunction("S3_GET");
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Object> result = new AtomicReference<>();
            AtomicReference<Exception> error = new AtomicReference<>();

            fn.execute(Arrays.asList(bucket, testKey, accessKey, secretKey, null), 
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
                fail("S3_GET failed: " + error.get().getMessage());
            }
            assertNotNull("GET should return content", result.get());
            assertEquals("GET should return the content we PUT", testContent, result.get().toString());
        }

        // Test DELETE
        {
            BuiltInFunctionDefinition fn = context.getBuiltInFunction("S3_DELETE");
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Object> result = new AtomicReference<>();
            AtomicReference<Exception> error = new AtomicReference<>();

            fn.execute(Arrays.asList(bucket, testKey, accessKey, secretKey, null), 
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
                fail("S3_DELETE failed: " + error.get().getMessage());
            }
            assertTrue("DELETE should return true", Boolean.TRUE.equals(result.get()));
        }

        // Test EXISTS again (should be false after DELETE)
        {
            BuiltInFunctionDefinition fn = context.getBuiltInFunction("S3_EXISTS");
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Object> result = new AtomicReference<>();
            AtomicReference<Exception> error = new AtomicReference<>();

            fn.execute(Arrays.asList(bucket, testKey, accessKey, secretKey, null), 
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
                fail("S3_EXISTS failed: " + error.get().getMessage());
            }
            assertFalse("EXISTS should return false after DELETE", Boolean.TRUE.equals(result.get()));
        }
    }

    /**
     * Integration test for S3_LIST.
     * Requires AWS credentials and AWS_TEST_BUCKET environment variable.
     */
    @Test
    public void testS3ListIntegration() throws Exception {
        String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
        String secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        String bucket = System.getenv("AWS_TEST_BUCKET");
        
        if (accessKey == null || accessKey.isEmpty() || 
            secretKey == null || secretKey.isEmpty() ||
            bucket == null || bucket.isEmpty()) {
            // Skip test if credentials or bucket not set
            return;
        }

        BuiltInFunctionDefinition fn = context.getBuiltInFunction("S3_LIST");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();

        fn.execute(Arrays.asList(bucket, "", 10, accessKey, secretKey, null), 
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
            fail("S3_LIST failed: " + error.get().getMessage());
        }
        assertNotNull("LIST should return a result", result.get());
        assertTrue("LIST should return a list", result.get() instanceof List);
    }
}

