/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.connectors;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xcontent.XContentType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sync Engine for Moltler Connectors.
 * 
 * Handles synchronization of data from external connectors to Elasticsearch indices.
 * Supports:
 * - Full sync (fetch all data)
 * - Incremental sync (fetch only new/updated data based on a timestamp field)
 * - Scheduled sync (via external scheduler)
 */
public class SyncEngine {
    
    private static final String SYNC_STATE_INDEX = ".moltler_sync_state";
    private final Client client;
    
    public SyncEngine(Client client) {
        this.client = client;
    }
    
    /**
     * Execute a sync operation for a connector entity.
     * 
     * @param connectorName The name of the connector
     * @param entityName The entity to sync (e.g., "issues", "pull_requests")
     * @param connectorType The type of connector (e.g., "github", "jira")
     * @param config The connector configuration
     * @param targetIndex The Elasticsearch index to sync to
     * @param incrementalField Field to use for incremental sync (null for full sync)
     * @param listener Callback with sync results
     */
    public void sync(String connectorName, String entityName, String connectorType,
                     Map<String, Object> config, String targetIndex, String incrementalField,
                     ActionListener<Map<String, Object>> listener) {
        
        // Get the last sync state for incremental sync
        if (incrementalField != null) {
            getLastSyncState(connectorName, entityName, ActionListener.wrap(
                lastValue -> {
                    executeSyncWithState(connectorName, entityName, connectorType, config, 
                                        targetIndex, incrementalField, lastValue, listener);
                },
                e -> {
                    // No previous sync state - do full sync
                    executeSyncWithState(connectorName, entityName, connectorType, config, 
                                        targetIndex, incrementalField, null, listener);
                }
            ));
        } else {
            // Full sync - no incremental field
            executeSyncWithState(connectorName, entityName, connectorType, config, 
                                targetIndex, null, null, listener);
        }
    }
    
    private void executeSyncWithState(String connectorName, String entityName, String connectorType,
                                      Map<String, Object> config, String targetIndex,
                                      String incrementalField, Object lastValue,
                                      ActionListener<Map<String, Object>> listener) {
        
        // Ensure target index exists
        ensureIndexExists(targetIndex, ActionListener.wrap(
            indexCreated -> {
                // Fetch data from connector
                fetchConnectorData(connectorType, entityName, config, incrementalField, lastValue,
                    ActionListener.wrap(
                        data -> {
                            // Index the fetched data
                            indexData(targetIndex, data, ActionListener.wrap(
                                bulkResult -> {
                                    // Update sync state
                                    updateSyncState(connectorName, entityName, incrementalField, data,
                                        ActionListener.wrap(
                                            stateUpdated -> {
                                                Map<String, Object> result = new HashMap<>();
                                                result.put("status", "completed");
                                                result.put("connector", connectorName);
                                                result.put("entity", entityName);
                                                result.put("target_index", targetIndex);
                                                result.put("documents_synced", data.size());
                                                result.put("incremental", incrementalField != null);
                                                if (lastValue != null) {
                                                    result.put("last_sync_value", lastValue);
                                                }
                                                listener.onResponse(result);
                                            },
                                            listener::onFailure
                                        ));
                                },
                                listener::onFailure
                            ));
                        },
                        listener::onFailure
                    ));
            },
            listener::onFailure
        ));
    }
    
    /**
     * Fetch data from a connector.
     * This is a framework placeholder - actual connector implementations will override this.
     */
    private void fetchConnectorData(String connectorType, String entityName,
                                    Map<String, Object> config,
                                    String incrementalField, Object lastValue,
                                    ActionListener<List<Map<String, Object>>> listener) {
        // Generate sample data for framework demonstration
        List<Map<String, Object>> sampleData = new ArrayList<>();
        
        // Add sample documents based on connector type
        for (int i = 0; i < 5; i++) {
            Map<String, Object> doc = new HashMap<>();
            doc.put("id", connectorType + "_" + entityName + "_" + i);
            doc.put("connector_type", connectorType);
            doc.put("entity_type", entityName);
            doc.put("synced_at", System.currentTimeMillis());
            doc.put("sample_field", "Sample data " + i);
            
            if (incrementalField != null) {
                doc.put(incrementalField, System.currentTimeMillis() - (i * 60000)); // Stagger timestamps
            }
            
            sampleData.add(doc);
        }
        
        // Return sample data - real connectors would fetch from external APIs
        listener.onResponse(sampleData);
    }
    
    /**
     * Index data into Elasticsearch.
     */
    private void indexData(String targetIndex, List<Map<String, Object>> data,
                          ActionListener<BulkResponse> listener) {
        if (data.isEmpty()) {
            listener.onResponse(null);
            return;
        }
        
        BulkRequest bulkRequest = new BulkRequest();
        
        for (Map<String, Object> doc : data) {
            String id = null;
            if (doc.containsKey("id")) {
                id = String.valueOf(doc.get("id"));
            } else if (doc.containsKey("_id")) {
                id = String.valueOf(doc.get("_id"));
            }
            
            IndexRequest indexRequest = new IndexRequest(targetIndex);
            if (id != null) {
                indexRequest.id(id);
            }
            indexRequest.source(doc, XContentType.JSON);
            bulkRequest.add(indexRequest);
        }
        
        client.bulk(bulkRequest, listener);
    }
    
    /**
     * Get the last sync state for a connector entity.
     */
    private void getLastSyncState(String connectorName, String entityName,
                                  ActionListener<Object> listener) {
        String stateId = connectorName + "_" + entityName;
        
        client.get(
            new org.elasticsearch.action.get.GetRequest(SYNC_STATE_INDEX, stateId),
            ActionListener.wrap(
                getResponse -> {
                    if (getResponse.isExists()) {
                        Map<String, Object> state = getResponse.getSourceAsMap();
                        listener.onResponse(state.get("last_value"));
                    } else {
                        listener.onFailure(new RuntimeException("No sync state found"));
                    }
                },
                e -> {
                    if (e instanceof IndexNotFoundException) {
                        listener.onFailure(new RuntimeException("No sync state found"));
                    } else {
                        listener.onFailure(e);
                    }
                }
            )
        );
    }
    
    /**
     * Update the sync state after a successful sync.
     */
    private void updateSyncState(String connectorName, String entityName,
                                 String incrementalField, List<Map<String, Object>> data,
                                 ActionListener<Boolean> listener) {
        if (incrementalField == null || data.isEmpty()) {
            listener.onResponse(true);
            return;
        }
        
        // Find the maximum value of the incremental field
        Object maxValue = null;
        for (Map<String, Object> doc : data) {
            Object value = doc.get(incrementalField);
            if (value != null) {
                if (maxValue == null) {
                    maxValue = value;
                } else if (value instanceof Comparable && maxValue instanceof Comparable) {
                    @SuppressWarnings("unchecked")
                    Comparable<Object> comparableValue = (Comparable<Object>) value;
                    if (comparableValue.compareTo(maxValue) > 0) {
                        maxValue = value;
                    }
                }
            }
        }
        
        if (maxValue == null) {
            listener.onResponse(true);
            return;
        }
        
        final Object finalMaxValue = maxValue;
        
        // Ensure state index exists
        ensureIndexExists(SYNC_STATE_INDEX, ActionListener.wrap(
            indexCreated -> {
                String stateId = connectorName + "_" + entityName;
                
                Map<String, Object> state = new HashMap<>();
                state.put("connector", connectorName);
                state.put("entity", entityName);
                state.put("incremental_field", incrementalField);
                state.put("last_value", finalMaxValue);
                state.put("last_sync_at", System.currentTimeMillis());
                state.put("documents_synced", data.size());
                
                IndexRequest indexRequest = new IndexRequest(SYNC_STATE_INDEX)
                    .id(stateId)
                    .source(state, XContentType.JSON);
                
                client.index(indexRequest, ActionListener.wrap(
                    response -> listener.onResponse(true),
                    listener::onFailure
                ));
            },
            listener::onFailure
        ));
    }
    
    /**
     * Ensure an Elasticsearch index exists.
     */
    private void ensureIndexExists(String indexName, ActionListener<Boolean> listener) {
        client.admin().indices().prepareGetIndex(TimeValue.timeValueSeconds(30)).setIndices(indexName).execute(ActionListener.wrap(
            response -> listener.onResponse(true),
            e -> {
                if (e instanceof IndexNotFoundException) {
                    CreateIndexRequest createRequest = new CreateIndexRequest(indexName);
                    client.admin().indices().create(createRequest, ActionListener.wrap(
                        response -> listener.onResponse(true),
                        listener::onFailure
                    ));
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }
    
    /**
     * Get sync history for a connector.
     */
    public void getSyncHistory(String connectorName, int limit,
                               ActionListener<List<Map<String, Object>>> listener) {
        SearchRequest searchRequest = new SearchRequest(SYNC_STATE_INDEX);
        searchRequest.source(new SearchSourceBuilder()
            .query(QueryBuilders.prefixQuery("connector", connectorName))
            .size(limit)
            .sort("last_sync_at", SortOrder.DESC));
        
        client.search(searchRequest, ActionListener.wrap(
            response -> {
                List<Map<String, Object>> history = new ArrayList<>();
                response.getHits().forEach(hit -> history.add(hit.getSourceAsMap()));
                listener.onResponse(history);
            },
            e -> {
                if (e instanceof IndexNotFoundException) {
                    listener.onResponse(List.of());
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }
}
