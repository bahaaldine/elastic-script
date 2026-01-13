/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.escript.handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;
import org.elasticsearch.xpack.escript.scheduling.CronParser;
import org.elasticsearch.xpack.escript.scheduling.JobDefinition;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for JOB statements: CREATE JOB, ALTER JOB, DROP JOB, SHOW JOBS.
 */
public class JobStatementHandler {

    private static final Logger LOGGER = LogManager.getLogger(JobStatementHandler.class);
    private static final String INDEX_NAME = JobDefinition.INDEX_NAME;

    private final Client client;

    public JobStatementHandler(Client client) {
        this.client = client;
    }

    /**
     * Handle CREATE JOB statement.
     */
    public void handleCreateJob(ElasticScriptParser.Create_job_statementContext ctx, 
                                 String rawProcedureBody,
                                 ActionListener<Object> listener) {
        String jobName = ctx.ID().getText();
        String schedule = unquote(ctx.STRING(0).getText());  // First STRING is schedule
        
        // Parse optional fields
        String timezone = "UTC";
        boolean enabled = true;
        String description = null;
        
        // Check for TIMEZONE (second STRING if present)
        if (ctx.TIMEZONE() != null && ctx.STRING().size() > 1) {
            timezone = unquote(ctx.STRING(1).getText());
        }
        
        // Check for ENABLED
        if (ctx.ENABLED() != null && ctx.BOOLEAN() != null) {
            enabled = Boolean.parseBoolean(ctx.BOOLEAN().getText().toLowerCase());
        }
        
        // Check for DESCRIPTION (last STRING if present after others)
        if (ctx.DESCRIPTION() != null) {
            int descIndex = ctx.STRING().size() - 1;
            if (descIndex > 0) {
                description = unquote(ctx.STRING(descIndex).getText());
            }
        }

        // Validate cron expression
        if (!CronParser.isValid(schedule)) {
            listener.onFailure(new IllegalArgumentException("Invalid cron expression: " + schedule));
            return;
        }

        // Calculate next run time
        CronParser cron = new CronParser(schedule);
        Instant nextRun = cron.getNextRunTime(Instant.now(), ZoneId.of(timezone));

        // Build job definition
        JobDefinition job = JobDefinition.builder()
            .name(jobName)
            .schedule(schedule)
            .timezone(timezone)
            .enabled(enabled)
            .description(description)
            .procedureBody(rawProcedureBody)
            .nextRun(nextRun)
            .build();

        LOGGER.debug("Creating job: {}", job);

        // Ensure index exists, then store job
        ensureIndexExists(ActionListener.wrap(
            indexExists -> storeJob(job, listener),
            listener::onFailure
        ));
    }

    /**
     * Handle ALTER JOB ... ENABLE/DISABLE statement.
     */
    public void handleAlterJobEnableDisable(ElasticScriptParser.AlterJobEnableDisableContext ctx,
                                            ActionListener<Object> listener) {
        String jobName = ctx.ID().getText();
        boolean enable = ctx.ENABLE() != null;

        getJob(jobName, ActionListener.wrap(
            job -> {
                if (job == null) {
                    listener.onFailure(new IllegalArgumentException("Job not found: " + jobName));
                    return;
                }
                JobDefinition updated = job.withEnabled(enable);
                storeJob(updated, listener);
            },
            listener::onFailure
        ));
    }

    /**
     * Handle ALTER JOB ... SCHEDULE statement.
     */
    public void handleAlterJobSchedule(ElasticScriptParser.AlterJobScheduleContext ctx,
                                       ActionListener<Object> listener) {
        String jobName = ctx.ID().getText();
        String newSchedule = unquote(ctx.STRING().getText());

        if (!CronParser.isValid(newSchedule)) {
            listener.onFailure(new IllegalArgumentException("Invalid cron expression: " + newSchedule));
            return;
        }

        getJob(jobName, ActionListener.wrap(
            job -> {
                if (job == null) {
                    listener.onFailure(new IllegalArgumentException("Job not found: " + jobName));
                    return;
                }
                CronParser cron = new CronParser(newSchedule);
                Instant nextRun = cron.getNextRunTime(Instant.now(), job.getZoneId());
                JobDefinition updated = job.withSchedule(newSchedule, nextRun);
                storeJob(updated, listener);
            },
            listener::onFailure
        ));
    }

    /**
     * Handle DROP JOB statement.
     */
    public void handleDropJob(ElasticScriptParser.Drop_job_statementContext ctx,
                              ActionListener<Object> listener) {
        String jobName = ctx.ID().getText();

        DeleteRequest request = new DeleteRequest(INDEX_NAME, jobName);
        client.delete(request, ActionListener.wrap(
            response -> {
                Map<String, Object> result = new HashMap<>();
                result.put("acknowledged", true);
                result.put("job", jobName);
                result.put("action", "dropped");
                listener.onResponse(result);
            },
            listener::onFailure
        ));
    }

    /**
     * Handle SHOW JOBS statement.
     */
    public void handleShowAllJobs(ActionListener<Object> listener) {
        SearchRequest request = new SearchRequest(INDEX_NAME);
        request.source(new SearchSourceBuilder().query(QueryBuilders.matchAllQuery()).size(1000));

        client.search(request, ActionListener.wrap(
            response -> {
                List<Map<String, Object>> jobs = new ArrayList<>();
                for (SearchHit hit : response.getHits().getHits()) {
                    Map<String, Object> job = new HashMap<>(hit.getSourceAsMap());
                    job.put("_id", hit.getId());
                    jobs.add(job);
                }
                listener.onResponse(jobs);
            },
            e -> {
                if (e instanceof IndexNotFoundException) {
                    listener.onResponse(List.of());  // No jobs yet
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }

    /**
     * Handle SHOW JOB name statement.
     */
    public void handleShowJobDetail(ElasticScriptParser.ShowJobDetailContext ctx,
                                    ActionListener<Object> listener) {
        String jobName = ctx.ID().getText();
        getJob(jobName, ActionListener.wrap(
            job -> {
                if (job == null) {
                    listener.onFailure(new IllegalArgumentException("Job not found: " + jobName));
                } else {
                    listener.onResponse(jobToMap(job));
                }
            },
            listener::onFailure
        ));
    }

    /**
     * Handle SHOW JOB RUNS FOR name statement.
     */
    public void handleShowJobRuns(ElasticScriptParser.ShowJobRunsContext ctx,
                                  ActionListener<Object> listener) {
        String jobName = ctx.ID().getText();
        
        SearchRequest request = new SearchRequest(".escript_job_runs");
        request.source(new SearchSourceBuilder()
            .query(QueryBuilders.termQuery("job_name", jobName))
            .size(100)
            .sort("started_at", org.elasticsearch.search.sort.SortOrder.DESC));

        client.search(request, ActionListener.wrap(
            response -> {
                List<Map<String, Object>> runs = new ArrayList<>();
                for (SearchHit hit : response.getHits().getHits()) {
                    runs.add(hit.getSourceAsMap());
                }
                listener.onResponse(runs);
            },
            e -> {
                if (e instanceof IndexNotFoundException) {
                    listener.onResponse(List.of());  // No runs yet
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }

    // ==================== Helper Methods ====================

    private void ensureIndexExists(ActionListener<Boolean> listener) {
        client.admin().indices().prepareGetIndex(TimeValue.timeValueSeconds(30)).setIndices(INDEX_NAME).execute(ActionListener.wrap(
            response -> listener.onResponse(true),
            e -> {
                if (e instanceof IndexNotFoundException) {
                    CreateIndexRequest createRequest = new CreateIndexRequest(INDEX_NAME);
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

    private void storeJob(JobDefinition job, ActionListener<Object> listener) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            job.toXContent(builder, null);
            
            IndexRequest request = new IndexRequest(INDEX_NAME)
                .id(job.getName())
                .source(builder);

            client.index(request, ActionListener.wrap(
                response -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("acknowledged", true);
                    result.put("job", job.getName());
                    result.put("next_run", job.getNextRun() != null ? job.getNextRun().toString() : null);
                    listener.onResponse(result);
                },
                listener::onFailure
            ));
        } catch (IOException e) {
            listener.onFailure(e);
        }
    }

    private void getJob(String jobName, ActionListener<JobDefinition> listener) {
        GetRequest request = new GetRequest(INDEX_NAME, jobName);
        client.get(request, ActionListener.wrap(
            response -> {
                if (!response.isExists()) {
                    listener.onResponse(null);
                    return;
                }
                try {
                    XContentParser parser = XContentHelper.createParser(
                        null, null, response.getSourceAsBytesRef(), XContentType.JSON);
                    JobDefinition job = JobDefinition.fromXContent(parser);
                    listener.onResponse(job);
                } catch (IOException e) {
                    listener.onFailure(e);
                }
            },
            e -> {
                if (e instanceof IndexNotFoundException) {
                    listener.onResponse(null);
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }

    private Map<String, Object> jobToMap(JobDefinition job) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", job.getName());
        map.put("schedule", job.getSchedule());
        map.put("timezone", job.getTimezone());
        map.put("enabled", job.isEnabled());
        map.put("description", job.getDescription());
        map.put("created_at", job.getCreatedAt().toString());
        map.put("updated_at", job.getUpdatedAt().toString());
        map.put("next_run", job.getNextRun() != null ? job.getNextRun().toString() : null);
        map.put("last_run", job.getLastRun() != null ? job.getLastRun().toString() : null);
        map.put("last_status", job.getLastStatus());
        map.put("run_count", job.getRunCount());
        return map;
    }

    private String unquote(String s) {
        if (s == null) return null;
        if ((s.startsWith("'") && s.endsWith("'")) || (s.startsWith("\"") && s.endsWith("\""))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
