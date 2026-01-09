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
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.ToXContent;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xcontent.XContentType;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing async procedure executions.
 *
 * Provides both in-memory caching (for fast local access) and Elasticsearch persistence
 * (for durability, queryability, and cross-node visibility).
 */
public class ExecutionRegistry {

    private static final Logger LOGGER = LogManager.getLogger(ExecutionRegistry.class);
    public static final String INDEX_NAME = ".escript_executions";

    private final Client client;
    private final ConcurrentHashMap<String, ExecutionState> localCache;
    private final String localNodeName;

    public ExecutionRegistry(Client client, String localNodeName) {
        this.client = client;
        this.localCache = new ConcurrentHashMap<>();
        this.localNodeName = localNodeName;
    }

    /**
     * Generates a unique execution ID.
     */
    public static String generateExecutionId() {
        return "exec-" + UUID.randomUUID().toString().substring(0, 12);
    }

    /**
     * Creates a new execution and persists it.
     */
    public void createExecution(
        String procedure,
        Map<String, Object> parameters,
        ExecutionPipeline pipeline,
        ActionListener<ExecutionState> listener
    ) {
        String executionId = generateExecutionId();
        String trackName = pipeline.getTrackName().orElse(null);

        ExecutionState state = ExecutionState.builder()
            .executionId(executionId)
            .name(trackName)
            .procedure(procedure)
            .parameters(parameters)
            .status(ExecutionStatus.PENDING)
            .pipeline(pipeline)
            .startedAt(Instant.now())
            .node(localNodeName)
            .build();

        persistState(state, ActionListener.wrap(
            success -> {
                localCache.put(executionId, state);
                if (trackName != null) {
                    localCache.put(trackName, state);
                }
                LOGGER.info("Created execution: {} (track: {})", executionId, trackName);
                listener.onResponse(state);
            },
            listener::onFailure
        ));
    }

    /**
     * Updates an execution state.
     */
    public void updateExecution(ExecutionState state, ActionListener<ExecutionState> listener) {
        persistState(state, ActionListener.wrap(
            success -> {
                localCache.put(state.getExecutionId(), state);
                if (state.getName() != null) {
                    localCache.put(state.getName(), state);
                }
                LOGGER.debug("Updated execution: {} -> {}", state.getExecutionId(), state.getStatus());
                listener.onResponse(state);
            },
            listener::onFailure
        ));
    }

    /**
     * Gets an execution by ID or track name.
     */
    public void getExecution(String idOrName, ActionListener<Optional<ExecutionState>> listener) {
        // Try local cache first
        ExecutionState cached = localCache.get(idOrName);
        if (cached != null) {
            listener.onResponse(Optional.of(cached));
            return;
        }

        // Fall back to Elasticsearch
        fetchFromElasticsearch(idOrName, ActionListener.wrap(
            state -> {
                if (state.isPresent()) {
                    ExecutionState s = state.get();
                    localCache.put(s.getExecutionId(), s);
                    if (s.getName() != null) {
                        localCache.put(s.getName(), s);
                    }
                }
                listener.onResponse(state);
            },
            listener::onFailure
        ));
    }

    /**
     * Lists all executions matching the given status.
     */
    public void listExecutions(ExecutionStatus status, int limit, ActionListener<List<ExecutionState>> listener) {
        SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        
        if (status != null) {
            sourceBuilder.query(QueryBuilders.termQuery("status", status.name()));
        } else {
            sourceBuilder.query(QueryBuilders.matchAllQuery());
        }
        sourceBuilder.size(limit);
        sourceBuilder.sort("started_at", org.elasticsearch.search.sort.SortOrder.DESC);
        
        searchRequest.source(sourceBuilder);

        client.search(searchRequest, ActionListener.wrap(
            searchResponse -> {
                List<ExecutionState> results = new ArrayList<>();
                for (SearchHit hit : searchResponse.getHits().getHits()) {
                    try {
                        ExecutionState state = parseFromSource(hit.getSourceAsMap());
                        results.add(state);
                    } catch (Exception e) {
                        LOGGER.warn("Failed to parse execution state from hit: {}", hit.getId(), e);
                    }
                }
                listener.onResponse(results);
            },
            e -> {
                // Index might not exist yet
                if (e.getMessage() != null && e.getMessage().contains("index_not_found")) {
                    listener.onResponse(List.of());
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }

    /**
     * Marks an execution as running.
     */
    public void markRunning(String executionId, ActionListener<ExecutionState> listener) {
        getExecution(executionId, ActionListener.wrap(
            optState -> {
                if (optState.isEmpty()) {
                    listener.onFailure(new IllegalArgumentException("Execution not found: " + executionId));
                    return;
                }
                ExecutionState updated = optState.get().withStatus(ExecutionStatus.RUNNING);
                updateExecution(updated, listener);
            },
            listener::onFailure
        ));
    }

    /**
     * Marks an execution as completed with result.
     */
    public void markCompleted(String executionId, Object result, ActionListener<ExecutionState> listener) {
        getExecution(executionId, ActionListener.wrap(
            optState -> {
                if (optState.isEmpty()) {
                    listener.onFailure(new IllegalArgumentException("Execution not found: " + executionId));
                    return;
                }
                ExecutionState updated = optState.get().withCompletion(result);
                updateExecution(updated, listener);
            },
            listener::onFailure
        ));
    }

    /**
     * Marks an execution as failed with error.
     */
    public void markFailed(String executionId, String error, ActionListener<ExecutionState> listener) {
        getExecution(executionId, ActionListener.wrap(
            optState -> {
                if (optState.isEmpty()) {
                    listener.onFailure(new IllegalArgumentException("Execution not found: " + executionId));
                    return;
                }
                ExecutionState updated = optState.get().withFailure(error);
                updateExecution(updated, listener);
            },
            listener::onFailure
        ));
    }

    /**
     * Marks an execution as cancelled.
     */
    public void markCancelled(String executionId, ActionListener<ExecutionState> listener) {
        getExecution(executionId, ActionListener.wrap(
            optState -> {
                if (optState.isEmpty()) {
                    listener.onFailure(new IllegalArgumentException("Execution not found: " + executionId));
                    return;
                }
                ExecutionState current = optState.get();
                if (current.isTerminal()) {
                    listener.onFailure(new IllegalStateException("Cannot cancel terminal execution: " + current.getStatus()));
                    return;
                }
                ExecutionState updated = current.withStatus(ExecutionStatus.CANCELLED)
                    .toBuilder()
                    .completedAt(Instant.now())
                    .build();
                updateExecution(updated, listener);
            },
            listener::onFailure
        ));
    }

    /**
     * Updates progress for an execution.
     */
    public void updateProgress(String executionId, int percent, String message, ActionListener<ExecutionState> listener) {
        getExecution(executionId, ActionListener.wrap(
            optState -> {
                if (optState.isEmpty()) {
                    listener.onFailure(new IllegalArgumentException("Execution not found: " + executionId));
                    return;
                }
                ExecutionState updated = optState.get().withProgress(percent, message);
                updateExecution(updated, listener);
            },
            listener::onFailure
        ));
    }

    /**
     * Clears local cache (useful for testing).
     */
    public void clearLocalCache() {
        localCache.clear();
    }

    // --- Private helpers ---

    private void persistState(ExecutionState state, ActionListener<Boolean> listener) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            state.toXContent(builder, ToXContent.EMPTY_PARAMS);
            
            IndexRequest request = new IndexRequest(INDEX_NAME)
                .id(state.getExecutionId())
                .source(builder)
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

            client.index(request, ActionListener.wrap(
                response -> listener.onResponse(response.getResult() == DocWriteResponse.Result.CREATED 
                    || response.getResult() == DocWriteResponse.Result.UPDATED),
                listener::onFailure
            ));
        } catch (IOException e) {
            listener.onFailure(e);
        }
    }

    private void fetchFromElasticsearch(String idOrName, ActionListener<Optional<ExecutionState>> listener) {
        // First try by ID
        GetRequest getRequest = new GetRequest(INDEX_NAME, idOrName);
        
        client.get(getRequest, ActionListener.wrap(
            getResponse -> {
                if (getResponse.isExists()) {
                    try {
                        ExecutionState state = parseFromSource(getResponse.getSourceAsMap());
                        listener.onResponse(Optional.of(state));
                    } catch (Exception e) {
                        listener.onFailure(e);
                    }
                } else {
                    // Try by name
                    searchByName(idOrName, listener);
                }
            },
            e -> {
                // Index might not exist
                if (e.getMessage() != null && e.getMessage().contains("index_not_found")) {
                    listener.onResponse(Optional.empty());
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }

    private void searchByName(String name, ActionListener<Optional<ExecutionState>> listener) {
        SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.termQuery("name", name));
        sourceBuilder.size(1);
        sourceBuilder.sort("started_at", org.elasticsearch.search.sort.SortOrder.DESC);
        searchRequest.source(sourceBuilder);

        client.search(searchRequest, ActionListener.wrap(
            searchResponse -> {
                if (searchResponse.getHits().getTotalHits().value() > 0) {
                    SearchHit hit = searchResponse.getHits().getHits()[0];
                    try {
                        ExecutionState state = parseFromSource(hit.getSourceAsMap());
                        listener.onResponse(Optional.of(state));
                    } catch (Exception e) {
                        listener.onFailure(e);
                    }
                } else {
                    listener.onResponse(Optional.empty());
                }
            },
            e -> {
                if (e.getMessage() != null && e.getMessage().contains("index_not_found")) {
                    listener.onResponse(Optional.empty());
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }

    @SuppressWarnings("unchecked")
    private ExecutionState parseFromSource(Map<String, Object> source) {
        ExecutionState.Builder builder = ExecutionState.builder();
        
        builder.executionId((String) source.get("execution_id"));
        builder.name((String) source.get("name"));
        builder.procedure((String) source.get("procedure"));
        builder.parameters((Map<String, Object>) source.get("parameters"));
        builder.status(ExecutionStatus.valueOf((String) source.get("status")));
        
        String startedAt = (String) source.get("started_at");
        if (startedAt != null) {
            builder.startedAt(Instant.parse(startedAt));
        }
        
        String completedAt = (String) source.get("completed_at");
        if (completedAt != null) {
            builder.completedAt(Instant.parse(completedAt));
        }
        
        builder.node((String) source.get("node"));
        builder.result(source.get("result"));
        builder.error((String) source.get("error"));
        
        Map<String, Object> progressMap = (Map<String, Object>) source.get("progress");
        if (progressMap != null) {
            int percent = ((Number) progressMap.get("percent")).intValue();
            String message = (String) progressMap.get("message");
            builder.progress(new ExecutionProgress(percent, message));
        }
        
        // Note: Pipeline is not reconstructed from source for now
        // as it's primarily used for execution, not querying
        
        return builder.build();
    }
}



