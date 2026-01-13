/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.escript.scheduling;

import org.elasticsearch.xcontent.ToXContentObject;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentParser;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;

/**
 * Represents an event trigger definition.
 * Triggers poll indices for new documents and execute procedures when conditions are met.
 */
public class TriggerDefinition implements ToXContentObject {

    public static final String INDEX_NAME = ".escript_triggers";
    public static final int DEFAULT_POLL_INTERVAL_SECONDS = 30;

    private final String name;
    private final String indexPattern;        // Index pattern to watch
    private final String condition;           // WHEN condition (stored as string for parsing)
    private final int pollIntervalSeconds;    // How often to poll
    private final boolean enabled;
    private final String description;
    private final String procedureBody;       // The procedure code to execute
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant lastPoll;           // When we last polled
    private final Instant lastCheckpoint;     // @timestamp of last processed document
    private final long fireCount;             // Number of times trigger has fired

    public TriggerDefinition(
        String name,
        String indexPattern,
        String condition,
        int pollIntervalSeconds,
        boolean enabled,
        String description,
        String procedureBody,
        Instant createdAt,
        Instant updatedAt,
        Instant lastPoll,
        Instant lastCheckpoint,
        long fireCount
    ) {
        this.name = Objects.requireNonNull(name, "Trigger name is required");
        this.indexPattern = Objects.requireNonNull(indexPattern, "Index pattern is required");
        this.condition = condition;  // Can be null (no WHEN clause)
        this.pollIntervalSeconds = pollIntervalSeconds > 0 ? pollIntervalSeconds : DEFAULT_POLL_INTERVAL_SECONDS;
        this.enabled = enabled;
        this.description = description;
        this.procedureBody = Objects.requireNonNull(procedureBody, "Procedure body is required");
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
        this.lastPoll = lastPoll;
        this.lastCheckpoint = lastCheckpoint;
        this.fireCount = fireCount;
    }

    // Getters
    public String getName() { return name; }
    public String getIndexPattern() { return indexPattern; }
    public String getCondition() { return condition; }
    public int getPollIntervalSeconds() { return pollIntervalSeconds; }
    public boolean isEnabled() { return enabled; }
    public String getDescription() { return description; }
    public String getProcedureBody() { return procedureBody; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getLastPoll() { return lastPoll; }
    public Instant getLastCheckpoint() { return lastCheckpoint; }
    public long getFireCount() { return fireCount; }

    /**
     * Check if it's time to poll based on interval.
     */
    public boolean shouldPoll(Instant now) {
        if (!enabled) {
            return false;
        }
        if (lastPoll == null) {
            return true;
        }
        return now.isAfter(lastPoll.plusSeconds(pollIntervalSeconds));
    }

    /**
     * Create a new TriggerDefinition with updated enabled status.
     */
    public TriggerDefinition withEnabled(boolean enabled) {
        return new TriggerDefinition(
            name, indexPattern, condition, pollIntervalSeconds, enabled, description, procedureBody,
            createdAt, Instant.now(), lastPoll, lastCheckpoint, fireCount
        );
    }

    /**
     * Create a new TriggerDefinition with updated poll interval.
     */
    public TriggerDefinition withPollInterval(int seconds) {
        return new TriggerDefinition(
            name, indexPattern, condition, seconds, enabled, description, procedureBody,
            createdAt, Instant.now(), lastPoll, lastCheckpoint, fireCount
        );
    }

    /**
     * Create a new TriggerDefinition after a poll.
     */
    public TriggerDefinition afterPoll(Instant newCheckpoint, boolean fired) {
        return new TriggerDefinition(
            name, indexPattern, condition, pollIntervalSeconds, enabled, description, procedureBody,
            createdAt, Instant.now(), Instant.now(), newCheckpoint != null ? newCheckpoint : lastCheckpoint,
            fired ? fireCount + 1 : fireCount
        );
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("name", name);
        builder.field("index_pattern", indexPattern);
        if (condition != null) {
            builder.field("condition", condition);
        }
        builder.field("poll_interval_seconds", pollIntervalSeconds);
        builder.field("enabled", enabled);
        if (description != null) {
            builder.field("description", description);
        }
        builder.field("procedure_body", procedureBody);
        builder.field("created_at", createdAt.toString());
        builder.field("updated_at", updatedAt.toString());
        if (lastPoll != null) {
            builder.field("last_poll", lastPoll.toString());
        }
        if (lastCheckpoint != null) {
            builder.field("last_checkpoint", lastCheckpoint.toString());
        }
        builder.field("fire_count", fireCount);
        builder.endObject();
        return builder;
    }

    /**
     * Parse a TriggerDefinition from XContent.
     */
    public static TriggerDefinition fromXContent(XContentParser parser) throws IOException {
        String name = null;
        String indexPattern = null;
        String condition = null;
        int pollIntervalSeconds = DEFAULT_POLL_INTERVAL_SECONDS;
        boolean enabled = true;
        String description = null;
        String procedureBody = null;
        Instant createdAt = null;
        Instant updatedAt = null;
        Instant lastPoll = null;
        Instant lastCheckpoint = null;
        long fireCount = 0;

        XContentParser.Token token = parser.currentToken();
        if (token == null) {
            token = parser.nextToken();
        }
        if (token == XContentParser.Token.START_OBJECT) {
            while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                if (token == XContentParser.Token.FIELD_NAME) {
                    String fieldName = parser.currentName();
                    parser.nextToken();
                    switch (fieldName) {
                        case "name" -> name = parser.text();
                        case "index_pattern" -> indexPattern = parser.text();
                        case "condition" -> condition = parser.text();
                        case "poll_interval_seconds" -> pollIntervalSeconds = parser.intValue();
                        case "enabled" -> enabled = parser.booleanValue();
                        case "description" -> description = parser.text();
                        case "procedure_body" -> procedureBody = parser.text();
                        case "created_at" -> createdAt = Instant.parse(parser.text());
                        case "updated_at" -> updatedAt = Instant.parse(parser.text());
                        case "last_poll" -> lastPoll = Instant.parse(parser.text());
                        case "last_checkpoint" -> lastCheckpoint = Instant.parse(parser.text());
                        case "fire_count" -> fireCount = parser.longValue();
                    }
                }
            }
        }

        return new TriggerDefinition(
            name, indexPattern, condition, pollIntervalSeconds, enabled, description, procedureBody,
            createdAt, updatedAt, lastPoll, lastCheckpoint, fireCount
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TriggerDefinition that = (TriggerDefinition) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "TriggerDefinition{name='" + name + "', indexPattern='" + indexPattern + 
               "', interval=" + pollIntervalSeconds + "s, enabled=" + enabled + "}";
    }

    /**
     * Builder for creating TriggerDefinition instances.
     */
    public static class Builder {
        private String name;
        private String indexPattern;
        private String condition;
        private int pollIntervalSeconds = DEFAULT_POLL_INTERVAL_SECONDS;
        private boolean enabled = true;
        private String description;
        private String procedureBody;

        public Builder name(String name) { this.name = name; return this; }
        public Builder indexPattern(String indexPattern) { this.indexPattern = indexPattern; return this; }
        public Builder condition(String condition) { this.condition = condition; return this; }
        public Builder pollIntervalSeconds(int seconds) { this.pollIntervalSeconds = seconds; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder procedureBody(String procedureBody) { this.procedureBody = procedureBody; return this; }

        public TriggerDefinition build() {
            return new TriggerDefinition(
                name, indexPattern, condition, pollIntervalSeconds, enabled, description, procedureBody,
                Instant.now(), Instant.now(), null, null, 0
            );
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
