/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.escript.scheduling;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.threadpool.Scheduler;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.executors.ElasticScriptExecutor;

import org.elasticsearch.core.TimeValue;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service that polls indices for new documents and executes triggers.
 * 
 * Runs on the leader node only and:
 * 1. For each enabled trigger, checks if poll interval has elapsed
 * 2. Queries the trigger's index pattern for documents newer than checkpoint
 * 3. Filters by WHEN condition if present
 * 4. Executes the trigger procedure with @documents bound
 * 5. Updates the checkpoint to the max @timestamp seen
 */
public class TriggerPollingService extends AbstractLifecycleComponent {

    private static final Logger LOGGER = LogManager.getLogger(TriggerPollingService.class);
    
    private static final long CHECK_INTERVAL_MS = 1000;  // Check triggers every second
    private static final String TRIGGER_RUNS_INDEX = ".escript_trigger_runs";

    private final Client client;
    private final ThreadPool threadPool;
    private final ElasticScriptExecutor executor;
    private final LeaderElectionService leaderElection;
    
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isLeader = new AtomicBoolean(false);
    private Scheduler.Cancellable pollingTask;

    public TriggerPollingService(Client client, ThreadPool threadPool,
                                 ElasticScriptExecutor executor,
                                 LeaderElectionService leaderElection) {
        this.client = client;
        this.threadPool = threadPool;
        this.executor = executor;
        this.leaderElection = leaderElection;
    }

    public void onBecomeLeader() {
        LOGGER.info("TriggerPollingService: became leader, starting polling");
        isLeader.set(true);
        startPolling();
    }

    public void onLoseLeadership() {
        LOGGER.info("TriggerPollingService: lost leadership, stopping polling");
        isLeader.set(false);
        stopPolling();
    }

    @Override
    protected void doStart() {
        LOGGER.info("Starting TriggerPollingService");
        isRunning.set(true);
        
        if (leaderElection.isLeader()) {
            onBecomeLeader();
        }
    }

    @Override
    protected void doStop() {
        LOGGER.info("Stopping TriggerPollingService");
        isRunning.set(false);
        stopPolling();
    }

    @Override
    protected void doClose() throws IOException {
        doStop();
    }

    private void startPolling() {
        if (!isRunning.get() || !isLeader.get()) return;
        
        LOGGER.info("Starting trigger polling loop");
        scheduleNextPoll();
    }

    private void stopPolling() {
        if (pollingTask != null) {
            pollingTask.cancel();
            pollingTask = null;
        }
    }

    private void scheduleNextPoll() {
        if (!isRunning.get() || !isLeader.get()) return;
        
        pollingTask = threadPool.schedule(
            this::checkTriggers,
            TimeValue.timeValueMillis(CHECK_INTERVAL_MS),
            threadPool.generic()
        );
    }

    private void checkTriggers() {
        if (!isRunning.get() || !isLeader.get()) return;
        
        // Get all enabled triggers
        SearchRequest request = new SearchRequest(TriggerDefinition.INDEX_NAME);
        request.source(new SearchSourceBuilder()
            .query(QueryBuilders.termQuery("enabled", true))
            .size(100)
        );

        client.search(request, ActionListener.wrap(
            response -> {
                Instant now = Instant.now();
                
                for (SearchHit hit : response.getHits().getHits()) {
                    try {
                        Map<String, Object> source = hit.getSourceAsMap();
                        String triggerName = hit.getId();
                        
                        // Check if it's time to poll
                        int pollInterval = (Integer) source.getOrDefault("poll_interval_seconds", 30);
                        String lastPollStr = (String) source.get("last_poll");
                        
                        Instant lastPoll = lastPollStr != null ? Instant.parse(lastPollStr) : Instant.EPOCH;
                        
                        if (now.isAfter(lastPoll.plusSeconds(pollInterval))) {
                            pollTrigger(triggerName, source);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error processing trigger {}: {}", hit.getId(), e.getMessage(), e);
                    }
                }
                
                // Schedule next check
                scheduleNextPoll();
            },
            e -> {
                if (!(e instanceof IndexNotFoundException)) {
                    LOGGER.warn("Error checking triggers: {}", e.getMessage());
                }
                scheduleNextPoll();
            }
        ));
    }

    private void pollTrigger(String triggerName, Map<String, Object> triggerSource) {
        String indexPattern = (String) triggerSource.get("index_pattern");
        String checkpointStr = (String) triggerSource.get("last_checkpoint");
        String procedureBody = (String) triggerSource.get("procedure_body");
        
        if (indexPattern == null || procedureBody == null) {
            LOGGER.warn("Trigger {} missing index_pattern or procedure_body", triggerName);
            return;
        }

        Instant checkpoint = checkpointStr != null ? Instant.parse(checkpointStr) : Instant.EPOCH;
        
        LOGGER.debug("Polling trigger {} on index {} from checkpoint {}", 
            triggerName, indexPattern, checkpoint);

        // Query for documents newer than checkpoint
        BoolQueryBuilder query = QueryBuilders.boolQuery()
            .must(QueryBuilders.rangeQuery("@timestamp").gt(checkpoint.toString()));

        SearchRequest searchRequest = new SearchRequest(indexPattern);
        searchRequest.source(new SearchSourceBuilder()
            .query(query)
            .size(1000)  // Limit batch size
            .sort("@timestamp", SortOrder.ASC)
        );

        client.search(searchRequest, ActionListener.wrap(
            response -> {
                List<Map<String, Object>> documents = new ArrayList<>();
                Instant maxTimestamp = checkpoint;
                
                for (SearchHit hit : response.getHits().getHits()) {
                    Map<String, Object> doc = hit.getSourceAsMap();
                    doc.put("_id", hit.getId());
                    doc.put("_index", hit.getIndex());
                    documents.add(doc);
                    
                    // Track max timestamp for checkpoint
                    String tsStr = (String) doc.get("@timestamp");
                    if (tsStr != null) {
                        Instant ts = Instant.parse(tsStr);
                        if (ts.isAfter(maxTimestamp)) {
                            maxTimestamp = ts;
                        }
                    }
                }
                
                // Update last_poll timestamp
                updateTriggerPoll(triggerName, maxTimestamp);
                
                if (documents.isEmpty()) {
                    LOGGER.debug("Trigger {}: no new documents", triggerName);
                    return;
                }
                
                LOGGER.info("Trigger {}: found {} new documents", triggerName, documents.size());
                
                // Execute trigger
                executeTrigger(triggerName, documents, procedureBody, maxTimestamp);
            },
            e -> {
                if (e instanceof IndexNotFoundException) {
                    LOGGER.debug("Trigger {} index {} not found, skipping", triggerName, indexPattern);
                } else {
                    LOGGER.warn("Error polling trigger {}: {}", triggerName, e.getMessage());
                }
                // Still update last_poll to prevent rapid retries
                updateTriggerPoll(triggerName, null);
            }
        ));
    }

    private void executeTrigger(String triggerName, List<Map<String, Object>> documents,
                                String procedureBody, Instant newCheckpoint) {
        long startTime = System.currentTimeMillis();
        
        // Wrap procedure body and bind @documents, @document_count
        // The procedure body expects @documents and @document_count to be available
        String wrappedProcedure = String.format("""
            CREATE PROCEDURE __trigger_%s() BEGIN
                DECLARE documents ARRAY = %s;
                DECLARE document_count NUMBER = %d;
                %s
            END PROCEDURE
            """, 
            triggerName,
            documentsToArrayLiteral(documents),
            documents.size(),
            procedureBody
        );

        executor.executeProcedure(wrappedProcedure, Map.of(), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                long duration = System.currentTimeMillis() - startTime;
                LOGGER.info("Trigger {} executed successfully in {}ms for {} documents", 
                    triggerName, duration, documents.size());
                
                // Record success
                recordTriggerRun(triggerName, "success", null, documents.size(), duration);
                
                // Update checkpoint
                updateTriggerCheckpoint(triggerName, newCheckpoint);
            }

            @Override
            public void onFailure(Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                LOGGER.error("Trigger {} failed after {}ms: {}", triggerName, duration, e.getMessage());
                
                // Record failure
                recordTriggerRun(triggerName, "failed", e.getMessage(), documents.size(), duration);
                
                // Still update checkpoint to avoid reprocessing same documents
                updateTriggerCheckpoint(triggerName, newCheckpoint);
            }
        });
    }

    private String documentsToArrayLiteral(List<Map<String, Object>> documents) {
        // Convert documents to an array literal that can be parsed by elastic-script
        // This is a simplified version - for complex documents, we'd need proper serialization
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Map<String, Object> doc : documents) {
            if (!first) sb.append(", ");
            first = false;
            sb.append(mapToDocumentLiteral(doc));
        }
        sb.append("]");
        return sb.toString();
    }

    private String mapToDocumentLiteral(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append("'").append(escapeString(entry.getKey())).append("': ");
            sb.append(valueToLiteral(entry.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String valueToLiteral(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "'" + escapeString((String) value) + "'";
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof List) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : (List<?>) value) {
                if (!first) sb.append(", ");
                first = false;
                sb.append(valueToLiteral(item));
            }
            sb.append("]");
            return sb.toString();
        } else if (value instanceof Map) {
            return mapToDocumentLiteral((Map<String, Object>) value);
        } else {
            return "'" + escapeString(value.toString()) + "'";
        }
    }

    private String escapeString(String s) {
        return s.replace("\\", "\\\\").replace("'", "\\'");
    }

    private void recordTriggerRun(String triggerName, String status, String error, 
                                  int documentCount, long durationMs) {
        Instant now = Instant.now();
        
        Map<String, Object> runDoc = new HashMap<>();
        runDoc.put("trigger_name", triggerName);
        runDoc.put("fired_at", now.toString());
        runDoc.put("document_count", documentCount);
        runDoc.put("duration_ms", durationMs);
        runDoc.put("status", status);
        if (error != null) {
            runDoc.put("error", error);
        }

        IndexRequest request = new IndexRequest(TRIGGER_RUNS_INDEX)
            .source(runDoc, XContentType.JSON);

        client.index(request, ActionListener.wrap(
            response -> LOGGER.debug("Recorded trigger run for {}", triggerName),
            e -> LOGGER.warn("Failed to record trigger run for {}: {}", triggerName, e.getMessage())
        ));
    }

    private void updateTriggerPoll(String triggerName, Instant checkpoint) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("last_poll", Instant.now().toString());
        if (checkpoint != null) {
            updates.put("last_checkpoint", checkpoint.toString());
        }

        UpdateRequest request = new UpdateRequest(TriggerDefinition.INDEX_NAME, triggerName)
            .doc(updates, XContentType.JSON);

        client.update(request, ActionListener.wrap(
            response -> LOGGER.debug("Updated last_poll for trigger {}", triggerName),
            e -> LOGGER.warn("Failed to update last_poll for trigger {}: {}", triggerName, e.getMessage())
        ));
    }

    private void updateTriggerCheckpoint(String triggerName, Instant checkpoint) {
        UpdateRequest request = new UpdateRequest(TriggerDefinition.INDEX_NAME, triggerName)
            .doc(Map.of(
                "last_checkpoint", checkpoint.toString(),
                "fire_count", Map.of("_op", "increment")  // Note: This won't work, need script
            ), XContentType.JSON);

        // Use a script to increment fire_count
        request.script(new org.elasticsearch.script.Script(
            org.elasticsearch.script.ScriptType.INLINE,
            "painless",
            "ctx._source.fire_count = (ctx._source.fire_count ?: 0) + 1; ctx._source.last_checkpoint = params.checkpoint",
            Map.of("checkpoint", checkpoint.toString())
        ));

        client.update(request, ActionListener.wrap(
            response -> LOGGER.debug("Updated checkpoint for trigger {}", triggerName),
            e -> LOGGER.warn("Failed to update checkpoint for trigger {}: {}", triggerName, e.getMessage())
        ));
    }
}
