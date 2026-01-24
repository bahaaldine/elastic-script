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
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.threadpool.Scheduler;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xcontent.XContentType;

import org.elasticsearch.core.TimeValue;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Leader election service for elastic-script scheduler.
 * 
 * Uses a simple index-based leader election with TTL:
 * 1. Try to create/update a leader document with our node ID
 * 2. If successful, we are the leader
 * 3. Periodically refresh the leader document (heartbeat)
 * 4. If leader document expires (TTL), another node can take over
 */
public class LeaderElectionService extends AbstractLifecycleComponent {

    private static final Logger LOGGER = LogManager.getLogger(LeaderElectionService.class);
    
    private static final String LEADER_INDEX = ".escript_leader";
    private static final String LEADER_DOC_ID = "scheduler_leader";
    private static final long HEARTBEAT_INTERVAL_MS = 5000;  // 5 seconds
    private static final long LEADER_TTL_MS = 15000;  // 15 seconds (3x heartbeat)

    private final Client client;
    private final ThreadPool threadPool;
    private final ClusterService clusterService;
    private String nodeId;  // Lazily initialized in doStart()
    private final AtomicBoolean isLeader = new AtomicBoolean(false);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    
    private Scheduler.Cancellable heartbeatTask;
    private LeadershipListener listener;

    public interface LeadershipListener {
        void onBecomeLeader();
        void onLoseLeadership();
    }

    public LeaderElectionService(Client client, ThreadPool threadPool, ClusterService clusterService) {
        this.client = client;
        this.threadPool = threadPool;
        this.clusterService = clusterService;
        // Note: Don't call clusterService.localNode() here - cluster state not yet initialized
        // Node ID will be obtained lazily in doStart()
    }

    /**
     * Constructor for testing without ClusterService.
     */
    public LeaderElectionService(Client client, ThreadPool threadPool, String nodeId) {
        this.client = client;
        this.threadPool = threadPool;
        this.clusterService = null;
        this.nodeId = nodeId;
    }

    public void setListener(LeadershipListener listener) {
        this.listener = listener;
    }

    public boolean isLeader() {
        return isLeader.get();
    }

    public String getNodeId() {
        return nodeId;
    }

    @Override
    protected void doStart() {
        // Lazily get the node ID now that cluster state is initialized
        if (nodeId == null && clusterService != null) {
            this.nodeId = clusterService.localNode().getId();
        }
        
        LOGGER.info("Starting leader election service for node {}", nodeId);
        isRunning.set(true);
        
        // Ensure index exists, then start election
        ensureLeaderIndex(ActionListener.wrap(
            success -> startElection(),
            e -> {
                LOGGER.error("Failed to create leader index, retrying in 5s", e);
                scheduleRetry();
            }
        ));
    }

    @Override
    protected void doStop() {
        LOGGER.info("Stopping leader election service");
        isRunning.set(false);
        
        if (heartbeatTask != null) {
            heartbeatTask.cancel();
            heartbeatTask = null;
        }
        
        if (isLeader.getAndSet(false)) {
            if (listener != null) {
                listener.onLoseLeadership();
            }
        }
    }

    @Override
    protected void doClose() throws IOException {
        doStop();
    }

    private void ensureLeaderIndex(ActionListener<Boolean> listener) {
        CreateIndexRequest request = new CreateIndexRequest(LEADER_INDEX);
        request.mapping(Map.of(
            "properties", Map.of(
                "node_id", Map.of("type", "keyword"),
                "last_heartbeat", Map.of("type", "date"),
                "acquired_at", Map.of("type", "date")
            )
        ));
        
        client.admin().indices().create(request, ActionListener.wrap(
            response -> listener.onResponse(true),
            e -> {
                if (e.getMessage() != null && e.getMessage().contains("resource_already_exists")) {
                    listener.onResponse(true);  // Index already exists, that's fine
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }

    private void startElection() {
        if (!isRunning.get()) return;
        
        // First, check if there's an existing leader
        GetRequest getRequest = new GetRequest(LEADER_INDEX, LEADER_DOC_ID);
        client.get(getRequest, ActionListener.wrap(
            response -> {
                if (!response.isExists()) {
                    // No leader, try to become leader
                    tryBecomeLeader();
                } else {
                    // Check if leader is still alive
                    Map<String, Object> source = response.getSourceAsMap();
                    String currentLeader = (String) source.get("node_id");
                    String lastHeartbeat = (String) source.get("last_heartbeat");
                    
                    if (currentLeader != null && currentLeader.equals(nodeId)) {
                        // We are already the leader, just refresh
                        onLeadershipAcquired();
                        sendHeartbeat();
                    } else if (lastHeartbeat != null) {
                        Instant heartbeatTime = Instant.parse(lastHeartbeat);
                        if (Instant.now().toEpochMilli() - heartbeatTime.toEpochMilli() > LEADER_TTL_MS) {
                            // Leader is dead, try to take over
                            LOGGER.info("Leader {} appears dead (last heartbeat: {}), attempting takeover", 
                                currentLeader, lastHeartbeat);
                            tryBecomeLeader();
                        } else {
                            // Leader is alive, we are not the leader
                            onLeadershipLost();
                            scheduleElectionRetry();
                        }
                    } else {
                        tryBecomeLeader();
                    }
                }
            },
            e -> {
                if (e instanceof IndexNotFoundException) {
                    // Index doesn't exist yet, try to create and become leader
                    ensureLeaderIndex(ActionListener.wrap(
                        success -> tryBecomeLeader(),
                        err -> scheduleRetry()
                    ));
                } else {
                    LOGGER.warn("Failed to check leader status, retrying", e);
                    scheduleRetry();
                }
            }
        ));
    }

    private void tryBecomeLeader() {
        if (!isRunning.get()) return;
        
        Instant now = Instant.now();
        IndexRequest request = new IndexRequest(LEADER_INDEX)
            .id(LEADER_DOC_ID)
            .source(Map.of(
                "node_id", nodeId,
                "last_heartbeat", now.toString(),
                "acquired_at", now.toString()
            ), XContentType.JSON)
            .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        
        client.index(request, ActionListener.wrap(
            response -> {
                if (response.getResult() == DocWriteResponse.Result.CREATED || 
                    response.getResult() == DocWriteResponse.Result.UPDATED) {
                    LOGGER.info("Node {} became scheduler leader", nodeId);
                    onLeadershipAcquired();
                    scheduleHeartbeat();
                } else {
                    LOGGER.debug("Failed to become leader, result: {}", response.getResult());
                    scheduleElectionRetry();
                }
            },
            e -> {
                LOGGER.debug("Failed to become leader: {}", e.getMessage());
                scheduleElectionRetry();
            }
        ));
    }

    private void sendHeartbeat() {
        if (!isRunning.get() || !isLeader.get()) return;
        
        IndexRequest request = new IndexRequest(LEADER_INDEX)
            .id(LEADER_DOC_ID)
            .source(Map.of(
                "node_id", nodeId,
                "last_heartbeat", Instant.now().toString()
            ), XContentType.JSON)
            .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        
        client.index(request, ActionListener.wrap(
            response -> {
                LOGGER.trace("Leader heartbeat sent");
                scheduleHeartbeat();
            },
            e -> {
                LOGGER.warn("Failed to send heartbeat, may lose leadership", e);
                // Don't immediately lose leadership, let the next heartbeat try again
                scheduleHeartbeat();
            }
        ));
    }

    private void onLeadershipAcquired() {
        if (!isLeader.getAndSet(true)) {
            LOGGER.info("Node {} acquired scheduler leadership", nodeId);
            if (listener != null) {
                try {
                    listener.onBecomeLeader();
                } catch (Exception e) {
                    LOGGER.error("Error in leadership listener", e);
                }
            }
        }
    }

    private void onLeadershipLost() {
        if (isLeader.getAndSet(false)) {
            LOGGER.info("Node {} lost scheduler leadership", nodeId);
            if (listener != null) {
                try {
                    listener.onLoseLeadership();
                } catch (Exception e) {
                    LOGGER.error("Error in leadership listener", e);
                }
            }
        }
    }

    private void scheduleHeartbeat() {
        if (!isRunning.get()) return;
        
        heartbeatTask = threadPool.schedule(
            this::sendHeartbeat,
            TimeValue.timeValueMillis(HEARTBEAT_INTERVAL_MS),
            threadPool.generic()
        );
    }

    private void scheduleElectionRetry() {
        if (!isRunning.get()) return;
        
        threadPool.schedule(
            this::startElection,
            TimeValue.timeValueMillis(HEARTBEAT_INTERVAL_MS * 2),
            threadPool.generic()
        );
    }

    private void scheduleRetry() {
        if (!isRunning.get()) return;
        
        threadPool.schedule(
            this::startElection,
            TimeValue.timeValueMillis(5000),
            threadPool.generic()
        );
    }
}
