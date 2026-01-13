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
import org.elasticsearch.xpack.escript.scheduling.TriggerDefinition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for TRIGGER statements: CREATE TRIGGER, ALTER TRIGGER, DROP TRIGGER, SHOW TRIGGERS.
 */
public class TriggerStatementHandler {

    private static final Logger LOGGER = LogManager.getLogger(TriggerStatementHandler.class);
    private static final String INDEX_NAME = TriggerDefinition.INDEX_NAME;

    private final Client client;

    public TriggerStatementHandler(Client client) {
        this.client = client;
    }

    /**
     * Handle CREATE TRIGGER statement.
     */
    public void handleCreateTrigger(ElasticScriptParser.Create_trigger_statementContext ctx,
                                    String rawCondition,
                                    String rawProcedureBody,
                                    ActionListener<Object> listener) {
        String triggerName = ctx.ID().getText();
        String indexPattern = unquote(ctx.STRING(0).getText());  // First STRING is index pattern

        // Parse poll interval
        int pollIntervalSeconds = TriggerDefinition.DEFAULT_POLL_INTERVAL_SECONDS;
        if (ctx.interval_expression() != null) {
            pollIntervalSeconds = parseInterval(ctx.interval_expression());
        }

        // Parse optional fields
        boolean enabled = true;
        String description = null;

        if (ctx.ENABLED() != null && ctx.BOOLEAN() != null) {
            enabled = Boolean.parseBoolean(ctx.BOOLEAN().getText().toLowerCase());
        }

        if (ctx.DESCRIPTION() != null && ctx.STRING().size() > 1) {
            description = unquote(ctx.STRING(ctx.STRING().size() - 1).getText());
        }

        // Build trigger definition
        TriggerDefinition trigger = TriggerDefinition.builder()
            .name(triggerName)
            .indexPattern(indexPattern)
            .condition(rawCondition)
            .pollIntervalSeconds(pollIntervalSeconds)
            .enabled(enabled)
            .description(description)
            .procedureBody(rawProcedureBody)
            .build();

        LOGGER.debug("Creating trigger: {}", trigger);

        // Ensure index exists, then store trigger
        ensureIndexExists(ActionListener.wrap(
            indexExists -> storeTrigger(trigger, listener),
            listener::onFailure
        ));
    }

    /**
     * Parse interval expression to seconds.
     */
    private int parseInterval(ElasticScriptParser.Interval_expressionContext ctx) {
        int value = Integer.parseInt(ctx.INT().getText());
        String unit = ctx.getChild(1).getText().toUpperCase();

        return switch (unit) {
            case "SECOND", "SECONDS" -> value;
            case "MINUTE", "MINUTES" -> value * 60;
            case "HOUR", "HOURS" -> value * 3600;
            default -> value;  // Default to seconds
        };
    }

    /**
     * Handle ALTER TRIGGER ... ENABLE/DISABLE statement.
     */
    public void handleAlterTriggerEnableDisable(ElasticScriptParser.AlterTriggerEnableDisableContext ctx,
                                                ActionListener<Object> listener) {
        String triggerName = ctx.ID().getText();
        boolean enable = ctx.ENABLE() != null;

        getTrigger(triggerName, ActionListener.wrap(
            trigger -> {
                if (trigger == null) {
                    listener.onFailure(new IllegalArgumentException("Trigger not found: " + triggerName));
                    return;
                }
                TriggerDefinition updated = trigger.withEnabled(enable);
                storeTrigger(updated, listener);
            },
            listener::onFailure
        ));
    }

    /**
     * Handle ALTER TRIGGER ... EVERY interval statement.
     */
    public void handleAlterTriggerInterval(ElasticScriptParser.AlterTriggerIntervalContext ctx,
                                           ActionListener<Object> listener) {
        String triggerName = ctx.ID().getText();
        int newInterval = parseInterval(ctx.interval_expression());

        getTrigger(triggerName, ActionListener.wrap(
            trigger -> {
                if (trigger == null) {
                    listener.onFailure(new IllegalArgumentException("Trigger not found: " + triggerName));
                    return;
                }
                TriggerDefinition updated = trigger.withPollInterval(newInterval);
                storeTrigger(updated, listener);
            },
            listener::onFailure
        ));
    }

    /**
     * Handle DROP TRIGGER statement.
     */
    public void handleDropTrigger(ElasticScriptParser.Drop_trigger_statementContext ctx,
                                  ActionListener<Object> listener) {
        String triggerName = ctx.ID().getText();

        DeleteRequest request = new DeleteRequest(INDEX_NAME, triggerName);
        client.delete(request, ActionListener.wrap(
            response -> {
                Map<String, Object> result = new HashMap<>();
                result.put("acknowledged", true);
                result.put("trigger", triggerName);
                result.put("action", "dropped");
                listener.onResponse(result);
            },
            listener::onFailure
        ));
    }

    /**
     * Handle SHOW TRIGGERS statement.
     */
    public void handleShowAllTriggers(ActionListener<Object> listener) {
        SearchRequest request = new SearchRequest(INDEX_NAME);
        request.source(new SearchSourceBuilder().query(QueryBuilders.matchAllQuery()).size(1000));

        client.search(request, ActionListener.wrap(
            response -> {
                List<Map<String, Object>> triggers = new ArrayList<>();
                for (SearchHit hit : response.getHits().getHits()) {
                    Map<String, Object> trigger = new HashMap<>(hit.getSourceAsMap());
                    trigger.put("_id", hit.getId());
                    triggers.add(trigger);
                }
                listener.onResponse(triggers);
            },
            e -> {
                if (e instanceof IndexNotFoundException) {
                    listener.onResponse(List.of());  // No triggers yet
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }

    /**
     * Handle SHOW TRIGGER name statement.
     */
    public void handleShowTriggerDetail(ElasticScriptParser.ShowTriggerDetailContext ctx,
                                        ActionListener<Object> listener) {
        String triggerName = ctx.ID().getText();
        getTrigger(triggerName, ActionListener.wrap(
            trigger -> {
                if (trigger == null) {
                    listener.onFailure(new IllegalArgumentException("Trigger not found: " + triggerName));
                } else {
                    listener.onResponse(triggerToMap(trigger));
                }
            },
            listener::onFailure
        ));
    }

    /**
     * Handle SHOW TRIGGER RUNS FOR name statement.
     */
    public void handleShowTriggerRuns(ElasticScriptParser.ShowTriggerRunsContext ctx,
                                      ActionListener<Object> listener) {
        String triggerName = ctx.ID().getText();

        SearchRequest request = new SearchRequest(".escript_trigger_runs");
        request.source(new SearchSourceBuilder()
            .query(QueryBuilders.termQuery("trigger_name", triggerName))
            .size(100)
            .sort("fired_at", org.elasticsearch.search.sort.SortOrder.DESC));

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

    private void storeTrigger(TriggerDefinition trigger, ActionListener<Object> listener) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            trigger.toXContent(builder, null);

            IndexRequest request = new IndexRequest(INDEX_NAME)
                .id(trigger.getName())
                .source(builder);

            client.index(request, ActionListener.wrap(
                response -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("acknowledged", true);
                    result.put("trigger", trigger.getName());
                    result.put("index_pattern", trigger.getIndexPattern());
                    result.put("poll_interval_seconds", trigger.getPollIntervalSeconds());
                    listener.onResponse(result);
                },
                listener::onFailure
            ));
        } catch (IOException e) {
            listener.onFailure(e);
        }
    }

    private void getTrigger(String triggerName, ActionListener<TriggerDefinition> listener) {
        GetRequest request = new GetRequest(INDEX_NAME, triggerName);
        client.get(request, ActionListener.wrap(
            response -> {
                if (!response.isExists()) {
                    listener.onResponse(null);
                    return;
                }
                try {
                    XContentParser parser = XContentHelper.createParser(
                        null, null, response.getSourceAsBytesRef(), XContentType.JSON);
                    TriggerDefinition trigger = TriggerDefinition.fromXContent(parser);
                    listener.onResponse(trigger);
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

    private Map<String, Object> triggerToMap(TriggerDefinition trigger) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", trigger.getName());
        map.put("index_pattern", trigger.getIndexPattern());
        map.put("condition", trigger.getCondition());
        map.put("poll_interval_seconds", trigger.getPollIntervalSeconds());
        map.put("enabled", trigger.isEnabled());
        map.put("description", trigger.getDescription());
        map.put("created_at", trigger.getCreatedAt().toString());
        map.put("updated_at", trigger.getUpdatedAt().toString());
        map.put("last_poll", trigger.getLastPoll() != null ? trigger.getLastPoll().toString() : null);
        map.put("last_checkpoint", trigger.getLastCheckpoint() != null ? trigger.getLastCheckpoint().toString() : null);
        map.put("fire_count", trigger.getFireCount());
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
