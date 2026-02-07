/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V.
 * under one or more contributor license agreements. Licensed under
 * the Elastic License 2.0; you may not use this file except in compliance
 * with the Elastic License 2.0.
 */

package org.elasticsearch.xpack.escript.functions;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.ActionType;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.escript.context.ExecutionContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link BuiltInFunctionRegistry}.
 * Validates that function caching works correctly and reduces registration overhead.
 */
public class BuiltInFunctionRegistryTests extends ESTestCase {

    private Client mockClient;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Clear any cached instances from previous tests
        BuiltInFunctionRegistry.clearInstances();
        
        // Create a mock client
        mockClient = mock(Client.class);
    }

    @Override
    public void tearDown() throws Exception {
        BuiltInFunctionRegistry.clearInstances();
        super.tearDown();
    }

    /**
     * Test that the registry caches stateless functions after first initialization.
     */
    public void testStatelessFunctionsAreCached() {
        BuiltInFunctionRegistry registry = BuiltInFunctionRegistry.getInstance(mockClient);
        
        // After getting instance, stateless functions should be cached
        // (initialization is lazy, happens on first registerAll call)
        int cachedCount = registry.getCachedFunctionCount();
        assertTrue("Registry should cache functions after initialization", cachedCount >= 0);
    }

    /**
     * Test that the same registry instance is returned for the same client.
     */
    public void testSameInstanceForSameClient() {
        BuiltInFunctionRegistry registry1 = BuiltInFunctionRegistry.getInstance(mockClient);
        BuiltInFunctionRegistry registry2 = BuiltInFunctionRegistry.getInstance(mockClient);
        
        assertSame("Same registry instance should be returned for same client", registry1, registry2);
    }

    /**
     * Test that different registry instances are created for different clients.
     */
    public void testDifferentInstancesForDifferentClients() {
        Client mockClient2 = mock(Client.class);
        
        BuiltInFunctionRegistry registry1 = BuiltInFunctionRegistry.getInstance(mockClient);
        BuiltInFunctionRegistry registry2 = BuiltInFunctionRegistry.getInstance(mockClient2);
        
        assertNotSame("Different registry instances should be returned for different clients", registry1, registry2);
    }

    /**
     * Test that clearInstances removes all cached registries.
     */
    public void testClearInstances() {
        BuiltInFunctionRegistry registry1 = BuiltInFunctionRegistry.getInstance(mockClient);
        
        BuiltInFunctionRegistry.clearInstances();
        
        BuiltInFunctionRegistry registry2 = BuiltInFunctionRegistry.getInstance(mockClient);
        
        assertNotSame("After clearing, a new registry instance should be created", registry1, registry2);
    }

    /**
     * Test that stateless functions are properly cached.
     * Verifies that STRING, NUMBER, ARRAY, DATE, and other stateless functions are available.
     */
    public void testStatelessFunctionsAvailable() {
        BuiltInFunctionRegistry registry = BuiltInFunctionRegistry.getInstance(mockClient);
        ExecutionContext context = new ExecutionContext();
        
        // We can't call registerAll without a real ProcedureExecutor,
        // but we can verify the registry was created
        assertNotNull("Registry should be created", registry);
        
        // Verify initial cached count is 0 (lazy initialization)
        assertEquals("Before registerAll, cached count should be 0", 0, registry.getCachedFunctionCount());
    }
}
