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
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.threadpool.Scheduler;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xpack.escript.executors.ElasticScriptExecutor;

import org.elasticsearch.core.TimeValue;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service that executes scheduled jobs.
 * 
 * <p>Runs on the leader node only and:</p>
 * <ol>
 *   <li>Periodically checks for jobs where next_run is before or equal to now</li>
 *   <li>Executes the job procedure</li>
 *   <li>Records the run result</li>
 *   <li>Calculates and updates next_run</li>
 * </ol>
 */
public class JobSchedulerService extends AbstractLifecycleComponent 
    implements LeaderElectionService.LeadershipListener {

    private static final Logger LOGGER = LogManager.getLogger(JobSchedulerService.class);
    
    private static final long CHECK_INTERVAL_MS = 1000;  // Check every second
    private static final String JOB_RUNS_INDEX = ".escript_job_runs";

    private final Client client;
    private final ThreadPool threadPool;
    private final ElasticScriptExecutor executor;
    private final LeaderElectionService leaderElection;
    
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isLeader = new AtomicBoolean(false);
    private Scheduler.Cancellable schedulerTask;

    public JobSchedulerService(Client client, ThreadPool threadPool, 
                               ElasticScriptExecutor executor,
                               LeaderElectionService leaderElection) {
        this.client = client;
        this.threadPool = threadPool;
        this.executor = executor;
        this.leaderElection = leaderElection;
        
        // Register as leadership listener
        leaderElection.setListener(this);
    }

    @Override
    public void onBecomeLeader() {
        LOGGER.info("JobSchedulerService: became leader, starting scheduler");
        isLeader.set(true);
        startScheduler();
    }

    @Override
    public void onLoseLeadership() {
        LOGGER.info("JobSchedulerService: lost leadership, stopping scheduler");
        isLeader.set(false);
        stopScheduler();
    }

    @Override
    protected void doStart() {
        LOGGER.info("Starting JobSchedulerService");
        isRunning.set(true);
        
        // Scheduler will start when we become leader
        if (leaderElection.isLeader()) {
            onBecomeLeader();
        }
    }

    @Override
    protected void doStop() {
        LOGGER.info("Stopping JobSchedulerService");
        isRunning.set(false);
        stopScheduler();
    }

    @Override
    protected void doClose() throws IOException {
        doStop();
    }

    private void startScheduler() {
        if (!isRunning.get() || !isLeader.get()) return;
        
        LOGGER.info("Starting job scheduler loop");
        scheduleNextCheck();
    }

    private void stopScheduler() {
        if (schedulerTask != null) {
            schedulerTask.cancel();
            schedulerTask = null;
        }
    }

    private void scheduleNextCheck() {
        if (!isRunning.get() || !isLeader.get()) return;
        
        schedulerTask = threadPool.schedule(
            this::checkAndRunJobs,
            TimeValue.timeValueMillis(CHECK_INTERVAL_MS),
            threadPool.generic()
        );
    }

    private void checkAndRunJobs() {
        if (!isRunning.get() || !isLeader.get()) return;
        
        // Search for jobs where next_run <= now and enabled = true
        SearchRequest request = new SearchRequest(JobDefinition.INDEX_NAME);
        request.source(new SearchSourceBuilder()
            .query(QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("enabled", true))
                .must(QueryBuilders.rangeQuery("next_run").lte(Instant.now().toString()))
            )
            .size(100)  // Process up to 100 jobs per cycle
        );

        client.search(request, ActionListener.wrap(
            response -> {
                for (SearchHit hit : response.getHits().getHits()) {
                    try {
                        executeJob(hit.getId(), hit.getSourceAsMap());
                    } catch (Exception e) {
                        LOGGER.error("Error executing job {}: {}", hit.getId(), e.getMessage(), e);
                    }
                }
                // Schedule next check
                scheduleNextCheck();
            },
            e -> {
                if (!(e instanceof IndexNotFoundException)) {
                    LOGGER.warn("Error checking for jobs: {}", e.getMessage());
                }
                // Schedule next check even on error
                scheduleNextCheck();
            }
        ));
    }

    private void executeJob(String jobName, Map<String, Object> jobSource) {
        LOGGER.info("Executing scheduled job: {}", jobName);
        
        String procedureBody = (String) jobSource.get("procedure_body");
        String schedule = (String) jobSource.get("schedule");
        String timezone = (String) jobSource.getOrDefault("timezone", "UTC");
        
        if (procedureBody == null || procedureBody.isEmpty()) {
            LOGGER.error("Job {} has no procedure body", jobName);
            recordJobRun(jobName, "failed", "No procedure body", 0);
            return;
        }

        long startTime = System.currentTimeMillis();
        
        // Wrap the procedure body in a procedure call
        String wrappedProcedure = "CREATE PROCEDURE __job_" + jobName + "() BEGIN " 
            + procedureBody + " END PROCEDURE";

        executor.executeProcedure(wrappedProcedure, Map.of(), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                long duration = System.currentTimeMillis() - startTime;
                LOGGER.info("Job {} completed successfully in {}ms", jobName, duration);
                
                // Record success
                recordJobRun(jobName, "success", null, duration);
                
                // Calculate and update next run time
                updateNextRun(jobName, schedule, timezone);
            }

            @Override
            public void onFailure(Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                LOGGER.error("Job {} failed after {}ms: {}", jobName, duration, e.getMessage());
                
                // Record failure
                recordJobRun(jobName, "failed", e.getMessage(), duration);
                
                // Still update next run time
                updateNextRun(jobName, schedule, timezone);
            }
        });
    }

    private void recordJobRun(String jobName, String status, String error, long durationMs) {
        Instant now = Instant.now();
        
        Map<String, Object> runDoc = new java.util.HashMap<>();
        runDoc.put("job_name", jobName);
        runDoc.put("started_at", now.minusMillis(durationMs).toString());
        runDoc.put("completed_at", now.toString());
        runDoc.put("duration_ms", durationMs);
        runDoc.put("status", status);
        if (error != null) {
            runDoc.put("error", error);
        }

        IndexRequest request = new IndexRequest(JOB_RUNS_INDEX)
            .source(runDoc, XContentType.JSON);

        client.index(request, ActionListener.wrap(
            response -> LOGGER.debug("Recorded job run for {}", jobName),
            e -> LOGGER.warn("Failed to record job run for {}: {}", jobName, e.getMessage())
        ));
    }

    private void updateNextRun(String jobName, String schedule, String timezone) {
        try {
            CronParser cron = new CronParser(schedule);
            Instant nextRun = cron.getNextRunTime(Instant.now(), ZoneId.of(timezone));
            
            UpdateRequest request = new UpdateRequest(JobDefinition.INDEX_NAME, jobName)
                .doc(Map.of(
                    "next_run", nextRun.toString(),
                    "last_run", Instant.now().toString(),
                    "last_status", "success"
                ), XContentType.JSON);

            client.update(request, ActionListener.wrap(
                response -> LOGGER.debug("Updated next_run for job {} to {}", jobName, nextRun),
                e -> LOGGER.warn("Failed to update next_run for job {}: {}", jobName, e.getMessage())
            ));
        } catch (Exception e) {
            LOGGER.error("Failed to calculate next run for job {}: {}", jobName, e.getMessage());
        }
    }
}
