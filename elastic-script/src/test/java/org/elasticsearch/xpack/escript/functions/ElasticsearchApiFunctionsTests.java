/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.builtin.elasticsearch.DocumentApiFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.elasticsearch.SearchApiFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.elasticsearch.IndexApiFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.elasticsearch.ClusterApiFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.elasticsearch.ILMApiFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.elasticsearch.DataStreamApiFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.elasticsearch.SnapshotApiFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.elasticsearch.IngestApiFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.elasticsearch.MLApiFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.elasticsearch.TransformApiFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.elasticsearch.WatcherApiFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.elasticsearch.SecurityApiFunctions;

import java.util.Set;

/**
 * Tests that Elasticsearch REST API functions are properly registered.
 * These functions use HTTP REST calls (not Java Client) for stability.
 */
public class ElasticsearchApiFunctionsTests extends ESTestCase {

    private ExecutionContext context;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new ExecutionContext();
    }

    public void testDocumentApiFunctionsRegistered() {
        DocumentApiFunctions.registerAll(context);
        
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

    public void testSearchApiFunctionsRegistered() {
        SearchApiFunctions.registerAll(context);
        
        Set<String> expectedFunctions = Set.of(
            "ES_SEARCH", "ES_COUNT", "ES_SCROLL", "ES_CLEAR_SCROLL", "ES_MSEARCH",
            "ES_FIELD_CAPS", "ES_EXPLAIN", "ES_SEARCH_TEMPLATE", "ES_KNN_SEARCH",
            "ES_VALIDATE", "ES_TERM_VECTORS"
        );
        
        for (String funcName : expectedFunctions) {
            assertNotNull("Function " + funcName + " should be registered", 
                context.getFunction(funcName));
        }
    }

    public void testIndexApiFunctionsRegistered() {
        IndexApiFunctions.registerAll(context);
        
        Set<String> expectedFunctions = Set.of(
            "ES_CREATE_INDEX", "ES_DELETE_INDEX", "ES_INDEX_EXISTS",
            "ES_CLOSE_INDEX", "ES_OPEN_INDEX", "ES_GET_MAPPING", "ES_PUT_MAPPING",
            "ES_GET_SETTINGS", "ES_PUT_SETTINGS", "ES_INDEX_STATS", "ES_REFRESH_INDEX",
            "ES_FLUSH_INDEX", "ES_FORCE_MERGE", "ES_SHRINK_INDEX",
            "ES_ROLLOVER_INDEX", "ES_CLONE_INDEX", "ES_GET_ALIAS", "ES_PUT_ALIAS",
            "ES_DELETE_ALIAS"
        );
        
        for (String funcName : expectedFunctions) {
            assertNotNull("Function " + funcName + " should be registered", 
                context.getFunction(funcName));
        }
    }

    public void testClusterApiFunctionsRegistered() {
        ClusterApiFunctions.registerAll(context);
        
        Set<String> expectedFunctions = Set.of(
            "ES_CLUSTER_HEALTH", "ES_CLUSTER_STATE", "ES_CLUSTER_STATS", "ES_CLUSTER_SETTINGS",
            "ES_UPDATE_CLUSTER_SETTINGS", "ES_NODES_INFO", "ES_NODES_STATS", "ES_NODE_HOT_THREADS",
            "ES_PENDING_TASKS", "ES_LIST_TASKS", "ES_CANCEL_TASK", "ES_ALLOCATION_EXPLAIN",
            "ES_REMOTE_INFO", "ES_CLUSTER_INFO"
        );
        
        for (String funcName : expectedFunctions) {
            assertNotNull("Function " + funcName + " should be registered", 
                context.getFunction(funcName));
        }
    }

    public void testILMApiFunctionsRegistered() {
        ILMApiFunctions.registerAll(context);
        
        Set<String> expectedFunctions = Set.of(
            "ES_ILM_GET_POLICY", "ES_ILM_PUT_POLICY", "ES_ILM_DELETE_POLICY", "ES_ILM_EXPLAIN",
            "ES_ILM_MOVE_TO_STEP", "ES_ILM_RETRY", "ES_ILM_REMOVE_POLICY", "ES_ILM_STATUS",
            "ES_ILM_START", "ES_ILM_STOP"
        );
        
        for (String funcName : expectedFunctions) {
            assertNotNull("Function " + funcName + " should be registered", 
                context.getFunction(funcName));
        }
    }

    public void testDataStreamApiFunctionsRegistered() {
        DataStreamApiFunctions.registerAll(context);
        
        Set<String> expectedFunctions = Set.of(
            "ES_CREATE_DATA_STREAM", "ES_DELETE_DATA_STREAM", "ES_GET_DATA_STREAM",
            "ES_DATA_STREAM_STATS", "ES_MIGRATE_TO_DATA_STREAM", "ES_MODIFY_DATA_STREAM"
        );
        
        for (String funcName : expectedFunctions) {
            assertNotNull("Function " + funcName + " should be registered", 
                context.getFunction(funcName));
        }
    }

    public void testSnapshotApiFunctionsRegistered() {
        SnapshotApiFunctions.registerAll(context);
        
        Set<String> expectedFunctions = Set.of(
            "ES_GET_REPOSITORIES", "ES_CREATE_REPOSITORY", "ES_DELETE_REPOSITORY",
            "ES_GET_SNAPSHOTS", "ES_CREATE_SNAPSHOT", "ES_DELETE_SNAPSHOT",
            "ES_RESTORE_SNAPSHOT", "ES_SNAPSHOT_STATUS", "ES_CLONE_SNAPSHOT"
        );
        
        for (String funcName : expectedFunctions) {
            assertNotNull("Function " + funcName + " should be registered", 
                context.getFunction(funcName));
        }
    }

    public void testIngestApiFunctionsRegistered() {
        IngestApiFunctions.registerAll(context);
        
        Set<String> expectedFunctions = Set.of(
            "ES_GET_PIPELINE", "ES_PUT_PIPELINE", "ES_DELETE_PIPELINE",
            "ES_SIMULATE_PIPELINE", "ES_GROK_PATTERNS"
        );
        
        for (String funcName : expectedFunctions) {
            assertNotNull("Function " + funcName + " should be registered", 
                context.getFunction(funcName));
        }
    }

    public void testMLApiFunctionsRegistered() {
        MLApiFunctions.registerAll(context);
        
        Set<String> expectedFunctions = Set.of(
            "ES_ML_GET_JOBS", "ES_ML_GET_JOB_STATS", "ES_ML_OPEN_JOB", "ES_ML_CLOSE_JOB",
            "ES_ML_DELETE_JOB", "ES_ML_GET_DATAFEEDS", "ES_ML_GET_DATAFEED_STATS",
            "ES_ML_START_DATAFEED", "ES_ML_STOP_DATAFEED", "ES_ML_GET_RECORDS",
            "ES_ML_GET_BUCKETS", "ES_ML_GET_TRAINED_MODELS", "ES_ML_GET_TRAINED_MODEL_STATS",
            "ES_ML_INFER"
        );
        
        for (String funcName : expectedFunctions) {
            assertNotNull("Function " + funcName + " should be registered", 
                context.getFunction(funcName));
        }
    }

    public void testTransformApiFunctionsRegistered() {
        TransformApiFunctions.registerAll(context);
        
        Set<String> expectedFunctions = Set.of(
            "ES_GET_TRANSFORM", "ES_GET_TRANSFORM_STATS", "ES_START_TRANSFORM",
            "ES_STOP_TRANSFORM", "ES_DELETE_TRANSFORM", "ES_PREVIEW_TRANSFORM",
            "ES_PUT_TRANSFORM", "ES_RESET_TRANSFORM"
        );
        
        for (String funcName : expectedFunctions) {
            assertNotNull("Function " + funcName + " should be registered", 
                context.getFunction(funcName));
        }
    }

    public void testWatcherApiFunctionsRegistered() {
        WatcherApiFunctions.registerAll(context);
        
        Set<String> expectedFunctions = Set.of(
            "ES_GET_WATCH", "ES_PUT_WATCH", "ES_DELETE_WATCH", "ES_EXECUTE_WATCH",
            "ES_ACTIVATE_WATCH", "ES_DEACTIVATE_WATCH", "ES_ACK_WATCH",
            "ES_WATCHER_STATS", "ES_START_WATCHER", "ES_STOP_WATCHER"
        );
        
        for (String funcName : expectedFunctions) {
            assertNotNull("Function " + funcName + " should be registered", 
                context.getFunction(funcName));
        }
    }

    public void testSecurityApiFunctionsRegistered() {
        SecurityApiFunctions.registerAll(context);
        
        Set<String> expectedFunctions = Set.of(
            "ES_GET_USERS", "ES_CREATE_USER", "ES_DELETE_USER", "ES_GET_ROLES",
            "ES_CREATE_ROLE", "ES_DELETE_ROLE", "ES_CREATE_API_KEY", "ES_GET_API_KEY",
            "ES_INVALIDATE_API_KEY", "ES_AUTHENTICATE", "ES_HAS_PRIVILEGES", "ES_GET_PRIVILEGES"
        );
        
        for (String funcName : expectedFunctions) {
            assertNotNull("Function " + funcName + " should be registered", 
                context.getFunction(funcName));
        }
    }

    public void testAllApiFunctionsTotalCount() {
        DocumentApiFunctions.registerAll(context);
        SearchApiFunctions.registerAll(context);
        IndexApiFunctions.registerAll(context);
        ClusterApiFunctions.registerAll(context);
        ILMApiFunctions.registerAll(context);
        DataStreamApiFunctions.registerAll(context);
        SnapshotApiFunctions.registerAll(context);
        IngestApiFunctions.registerAll(context);
        MLApiFunctions.registerAll(context);
        TransformApiFunctions.registerAll(context);
        WatcherApiFunctions.registerAll(context);
        SecurityApiFunctions.registerAll(context);
        
        int totalFunctions = context.getAllFunctions().size();
        assertTrue("Should have at least 100 ES API functions registered, got " + totalFunctions, 
            totalFunctions >= 100);
    }
}
