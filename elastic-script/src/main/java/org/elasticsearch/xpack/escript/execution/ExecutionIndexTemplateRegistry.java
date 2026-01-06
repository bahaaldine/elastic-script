/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.execution;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;

import java.io.IOException;

/**
 * Manages the creation of the .escript_executions index with proper mappings.
 */
public class ExecutionIndexTemplateRegistry {

    private static final Logger LOGGER = LogManager.getLogger(ExecutionIndexTemplateRegistry.class);
    public static final String INDEX_NAME = ".escript_executions";

    private final Client client;
    private final ClusterService clusterService;
    private volatile boolean indexCreated = false;

    public ExecutionIndexTemplateRegistry(Client client, ClusterService clusterService) {
        this.client = client;
        this.clusterService = clusterService;
    }

    /**
     * Ensures the execution index exists. Safe to call multiple times.
     */
    public void ensureIndexExists(ActionListener<Boolean> listener) {
        if (indexCreated) {
            listener.onResponse(true);
            return;
        }

        // Try to create index - will fail gracefully if it already exists
        createIndex(listener);
    }

    private void createIndex(ActionListener<Boolean> listener) {
        try {
            CreateIndexRequest request = new CreateIndexRequest(INDEX_NAME);
            request.settings(getIndexSettings());
            request.mapping(getIndexMapping());

            client.admin().indices().create(request, ActionListener.wrap(
                response -> {
                    if (response.isAcknowledged()) {
                        indexCreated = true;
                        LOGGER.info("Created {} index", INDEX_NAME);
                        listener.onResponse(true);
                    } else {
                        listener.onResponse(false);
                    }
                },
                e -> {
                    // Index might already exist due to race condition
                    if (e.getMessage() != null && e.getMessage().contains("resource_already_exists")) {
                        indexCreated = true;
                        listener.onResponse(true);
                    } else {
                        LOGGER.warn("Failed to create {} index", INDEX_NAME, e);
                        listener.onFailure(e);
                    }
                }
            ));
        } catch (IOException e) {
            listener.onFailure(e);
        }
    }

    private Settings getIndexSettings() {
        return Settings.builder()
            .put("index.number_of_shards", 1)
            .put("index.number_of_replicas", 1)
            .put("index.auto_expand_replicas", "0-1")
            .put("index.hidden", true)  // System index
            .build();
    }

    private XContentBuilder getIndexMapping() throws IOException {
        return XContentFactory.jsonBuilder()
            .startObject()
                .startObject("properties")
                    // Core fields
                    .startObject("execution_id")
                        .field("type", "keyword")
                    .endObject()
                    .startObject("name")
                        .field("type", "keyword")
                    .endObject()
                    .startObject("procedure")
                        .field("type", "keyword")
                    .endObject()
                    .startObject("parameters")
                        .field("type", "object")
                        .field("enabled", false)  // Don't index parameters
                    .endObject()
                    
                    // Status
                    .startObject("status")
                        .field("type", "keyword")
                    .endObject()
                    
                    // Progress
                    .startObject("progress")
                        .field("type", "object")
                        .startObject("properties")
                            .startObject("percent")
                                .field("type", "integer")
                            .endObject()
                            .startObject("message")
                                .field("type", "text")
                            .endObject()
                        .endObject()
                    .endObject()
                    
                    // Pipeline (stored but not indexed)
                    .startObject("pipeline")
                        .field("type", "object")
                        .field("enabled", false)
                    .endObject()
                    
                    // Timestamps
                    .startObject("started_at")
                        .field("type", "date")
                    .endObject()
                    .startObject("completed_at")
                        .field("type", "date")
                    .endObject()
                    
                    // Execution context
                    .startObject("node")
                        .field("type", "keyword")
                    .endObject()
                    
                    // Results
                    .startObject("result")
                        .field("type", "object")
                        .field("enabled", false)  // Don't index result
                    .endObject()
                    .startObject("error")
                        .field("type", "text")
                        .startObject("fields")
                            .startObject("keyword")
                                .field("type", "keyword")
                                .field("ignore_above", 256)
                            .endObject()
                        .endObject()
                    .endObject()
                    
                .endObject()
            .endObject();
    }

    /**
     * Gets the index name.
     */
    public static String getIndexName() {
        return INDEX_NAME;
    }
}

