/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.builtin.cloud.ServerlessFunctions;

/**
 * Tests for ServerlessFunctions registration.
 * Note: Actual API calls require valid credentials and are not tested here.
 */
public class ServerlessFunctionsTests extends ESTestCase {

    private ExecutionContext context;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new ExecutionContext();
        ServerlessFunctions.registerAll(context);
    }

    public void testAllFunctionsRegistered() {
        // Elasticsearch Projects (9)
        assertNotNull("SERVERLESS_LIST_ES_PROJECTS should be registered", context.getFunction("SERVERLESS_LIST_ES_PROJECTS"));
        assertNotNull("SERVERLESS_GET_ES_PROJECT should be registered", context.getFunction("SERVERLESS_GET_ES_PROJECT"));
        assertNotNull("SERVERLESS_CREATE_ES_PROJECT should be registered", context.getFunction("SERVERLESS_CREATE_ES_PROJECT"));
        assertNotNull("SERVERLESS_UPDATE_ES_PROJECT should be registered", context.getFunction("SERVERLESS_UPDATE_ES_PROJECT"));
        assertNotNull("SERVERLESS_DELETE_ES_PROJECT should be registered", context.getFunction("SERVERLESS_DELETE_ES_PROJECT"));
        assertNotNull("SERVERLESS_ES_PROJECT_STATUS should be registered", context.getFunction("SERVERLESS_ES_PROJECT_STATUS"));
        assertNotNull("SERVERLESS_ES_PROJECT_ROLES should be registered", context.getFunction("SERVERLESS_ES_PROJECT_ROLES"));
        assertNotNull("SERVERLESS_RESET_ES_CREDENTIALS should be registered", context.getFunction("SERVERLESS_RESET_ES_CREDENTIALS"));
        assertNotNull("SERVERLESS_RESUME_ES_PROJECT should be registered", context.getFunction("SERVERLESS_RESUME_ES_PROJECT"));

        // Observability Projects (9)
        assertNotNull("SERVERLESS_LIST_OBSERVABILITY_PROJECTS should be registered", context.getFunction("SERVERLESS_LIST_OBSERVABILITY_PROJECTS"));
        assertNotNull("SERVERLESS_GET_OBSERVABILITY_PROJECT should be registered", context.getFunction("SERVERLESS_GET_OBSERVABILITY_PROJECT"));
        assertNotNull("SERVERLESS_CREATE_OBSERVABILITY_PROJECT should be registered", context.getFunction("SERVERLESS_CREATE_OBSERVABILITY_PROJECT"));
        assertNotNull("SERVERLESS_UPDATE_OBSERVABILITY_PROJECT should be registered", context.getFunction("SERVERLESS_UPDATE_OBSERVABILITY_PROJECT"));
        assertNotNull("SERVERLESS_DELETE_OBSERVABILITY_PROJECT should be registered", context.getFunction("SERVERLESS_DELETE_OBSERVABILITY_PROJECT"));
        assertNotNull("SERVERLESS_OBSERVABILITY_PROJECT_STATUS should be registered", context.getFunction("SERVERLESS_OBSERVABILITY_PROJECT_STATUS"));
        assertNotNull("SERVERLESS_OBSERVABILITY_PROJECT_ROLES should be registered", context.getFunction("SERVERLESS_OBSERVABILITY_PROJECT_ROLES"));
        assertNotNull("SERVERLESS_RESET_OBSERVABILITY_CREDENTIALS should be registered", context.getFunction("SERVERLESS_RESET_OBSERVABILITY_CREDENTIALS"));
        assertNotNull("SERVERLESS_RESUME_OBSERVABILITY_PROJECT should be registered", context.getFunction("SERVERLESS_RESUME_OBSERVABILITY_PROJECT"));

        // Security Projects (9)
        assertNotNull("SERVERLESS_LIST_SECURITY_PROJECTS should be registered", context.getFunction("SERVERLESS_LIST_SECURITY_PROJECTS"));
        assertNotNull("SERVERLESS_GET_SECURITY_PROJECT should be registered", context.getFunction("SERVERLESS_GET_SECURITY_PROJECT"));
        assertNotNull("SERVERLESS_CREATE_SECURITY_PROJECT should be registered", context.getFunction("SERVERLESS_CREATE_SECURITY_PROJECT"));
        assertNotNull("SERVERLESS_UPDATE_SECURITY_PROJECT should be registered", context.getFunction("SERVERLESS_UPDATE_SECURITY_PROJECT"));
        assertNotNull("SERVERLESS_DELETE_SECURITY_PROJECT should be registered", context.getFunction("SERVERLESS_DELETE_SECURITY_PROJECT"));
        assertNotNull("SERVERLESS_SECURITY_PROJECT_STATUS should be registered", context.getFunction("SERVERLESS_SECURITY_PROJECT_STATUS"));
        assertNotNull("SERVERLESS_SECURITY_PROJECT_ROLES should be registered", context.getFunction("SERVERLESS_SECURITY_PROJECT_ROLES"));
        assertNotNull("SERVERLESS_RESET_SECURITY_CREDENTIALS should be registered", context.getFunction("SERVERLESS_RESET_SECURITY_CREDENTIALS"));
        assertNotNull("SERVERLESS_RESUME_SECURITY_PROJECT should be registered", context.getFunction("SERVERLESS_RESUME_SECURITY_PROJECT"));

        // Link Candidates (4)
        assertNotNull("SERVERLESS_ES_LINK_CANDIDATES should be registered", context.getFunction("SERVERLESS_ES_LINK_CANDIDATES"));
        assertNotNull("SERVERLESS_OBSERVABILITY_LINK_CANDIDATES should be registered", context.getFunction("SERVERLESS_OBSERVABILITY_LINK_CANDIDATES"));
        assertNotNull("SERVERLESS_SECURITY_LINK_CANDIDATES should be registered", context.getFunction("SERVERLESS_SECURITY_LINK_CANDIDATES"));
        assertNotNull("SERVERLESS_WORKPLACE_AI_LINK_CANDIDATES should be registered", context.getFunction("SERVERLESS_WORKPLACE_AI_LINK_CANDIDATES"));

        // Regions (2)
        assertNotNull("SERVERLESS_LIST_REGIONS should be registered", context.getFunction("SERVERLESS_LIST_REGIONS"));
        assertNotNull("SERVERLESS_GET_REGION should be registered", context.getFunction("SERVERLESS_GET_REGION"));

        // Traffic Filters (6)
        assertNotNull("SERVERLESS_LIST_TRAFFIC_FILTERS should be registered", context.getFunction("SERVERLESS_LIST_TRAFFIC_FILTERS"));
        assertNotNull("SERVERLESS_GET_TRAFFIC_FILTER should be registered", context.getFunction("SERVERLESS_GET_TRAFFIC_FILTER"));
        assertNotNull("SERVERLESS_CREATE_TRAFFIC_FILTER should be registered", context.getFunction("SERVERLESS_CREATE_TRAFFIC_FILTER"));
        assertNotNull("SERVERLESS_UPDATE_TRAFFIC_FILTER should be registered", context.getFunction("SERVERLESS_UPDATE_TRAFFIC_FILTER"));
        assertNotNull("SERVERLESS_DELETE_TRAFFIC_FILTER should be registered", context.getFunction("SERVERLESS_DELETE_TRAFFIC_FILTER"));
        assertNotNull("SERVERLESS_TRAFFIC_FILTER_METADATA should be registered", context.getFunction("SERVERLESS_TRAFFIC_FILTER_METADATA"));
    }

    public void testFunctionCount() {
        // Total: 9 + 9 + 9 + 4 + 2 + 6 = 39 functions
        int count = 0;
        for (String name : context.getAllFunctions().keySet()) {
            if (name.startsWith("SERVERLESS_")) {
                count++;
            }
        }
        assertEquals("Should have 39 SERVERLESS_ functions", 39, count);
    }
}
