/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.builtin.elasticsearch.DocumentFunctions;

import static org.mockito.Mockito.mock;
import org.elasticsearch.client.internal.Client;

import java.util.Set;

/**
 * Tests that Elasticsearch API functions are properly registered.
 * 
 * Note: Many ES API function classes were removed due to Elasticsearch internal API changes.
 * This test only covers DocumentFunctions which uses stable APIs.
 */
public class ElasticsearchApiFunctionsTests extends ESTestCase {

    private ExecutionContext context;
    private Client mockClient;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new ExecutionContext();
        mockClient = mock(Client.class);
    }

    public void testDocumentFunctionsRegistered() {
        DocumentFunctions.registerAll(context, mockClient);
        
        Set<String> expectedFunctions = Set.of(
            "ES_INDEX", "ES_GET", "ES_EXISTS", "ES_DELETE", "ES_UPDATE",
            "ES_BULK", "ES_MGET", "ES_REINDEX", "ES_UPDATE_BY_QUERY",
            "ES_DELETE_BY_QUERY", "ES_GET_SOURCE", "ES_CREATE"
        );
        
        for (String funcName : expectedFunctions) {
            assertNotNull("Function " + funcName + " should be registered", 
                context.getFunction(funcName));
        }
    }

    public void testDocumentFunctionCount() {
        DocumentFunctions.registerAll(context, mockClient);
        
        int totalFunctions = context.getAllFunctions().size();
        assertTrue("Should have at least 10 Document API functions registered, got " + totalFunctions, 
            totalFunctions >= 10);
    }
}
