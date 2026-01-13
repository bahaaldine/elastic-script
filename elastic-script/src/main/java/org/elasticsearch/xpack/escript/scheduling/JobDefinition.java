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
import java.time.ZoneId;
import java.util.Objects;

/**
 * Represents a scheduled job definition.
 * Jobs are stored in the .escript_jobs index and executed based on cron schedule.
 */
public class JobDefinition implements ToXContentObject {

    public static final String INDEX_NAME = ".escript_jobs";

    private final String name;
    private final String schedule;           // Cron expression
    private final String timezone;           // Timezone for schedule
    private final boolean enabled;
    private final String description;
    private final String procedureBody;      // The procedure code to execute
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant nextRun;
    private final Instant lastRun;
    private final String lastStatus;         // success, failed, running
    private final long runCount;

    public JobDefinition(
        String name,
        String schedule,
        String timezone,
        boolean enabled,
        String description,
        String procedureBody,
        Instant createdAt,
        Instant updatedAt,
        Instant nextRun,
        Instant lastRun,
        String lastStatus,
        long runCount
    ) {
        this.name = Objects.requireNonNull(name, "Job name is required");
        this.schedule = Objects.requireNonNull(schedule, "Schedule is required");
        this.timezone = timezone != null ? timezone : "UTC";
        this.enabled = enabled;
        this.description = description;
        this.procedureBody = Objects.requireNonNull(procedureBody, "Procedure body is required");
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
        this.nextRun = nextRun;
        this.lastRun = lastRun;
        this.lastStatus = lastStatus;
        this.runCount = runCount;
    }

    // Getters
    public String getName() { return name; }
    public String getSchedule() { return schedule; }
    public String getTimezone() { return timezone; }
    public ZoneId getZoneId() { return ZoneId.of(timezone); }
    public boolean isEnabled() { return enabled; }
    public String getDescription() { return description; }
    public String getProcedureBody() { return procedureBody; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getNextRun() { return nextRun; }
    public Instant getLastRun() { return lastRun; }
    public String getLastStatus() { return lastStatus; }
    public long getRunCount() { return runCount; }

    /**
     * Create a new JobDefinition with updated fields for enabling/disabling.
     */
    public JobDefinition withEnabled(boolean enabled) {
        return new JobDefinition(
            name, schedule, timezone, enabled, description, procedureBody,
            createdAt, Instant.now(), nextRun, lastRun, lastStatus, runCount
        );
    }

    /**
     * Create a new JobDefinition with updated schedule.
     */
    public JobDefinition withSchedule(String newSchedule, Instant newNextRun) {
        return new JobDefinition(
            name, newSchedule, timezone, enabled, description, procedureBody,
            createdAt, Instant.now(), newNextRun, lastRun, lastStatus, runCount
        );
    }

    /**
     * Create a new JobDefinition after a run.
     */
    public JobDefinition afterRun(String status, Instant nextRun) {
        return new JobDefinition(
            name, schedule, timezone, enabled, description, procedureBody,
            createdAt, Instant.now(), nextRun, Instant.now(), status, runCount + 1
        );
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("name", name);
        builder.field("schedule", schedule);
        builder.field("timezone", timezone);
        builder.field("enabled", enabled);
        if (description != null) {
            builder.field("description", description);
        }
        builder.field("procedure_body", procedureBody);
        builder.field("created_at", createdAt.toString());
        builder.field("updated_at", updatedAt.toString());
        if (nextRun != null) {
            builder.field("next_run", nextRun.toString());
        }
        if (lastRun != null) {
            builder.field("last_run", lastRun.toString());
        }
        if (lastStatus != null) {
            builder.field("last_status", lastStatus);
        }
        builder.field("run_count", runCount);
        builder.endObject();
        return builder;
    }

    /**
     * Parse a JobDefinition from XContent.
     */
    public static JobDefinition fromXContent(XContentParser parser) throws IOException {
        String name = null;
        String schedule = null;
        String timezone = "UTC";
        boolean enabled = true;
        String description = null;
        String procedureBody = null;
        Instant createdAt = null;
        Instant updatedAt = null;
        Instant nextRun = null;
        Instant lastRun = null;
        String lastStatus = null;
        long runCount = 0;

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
                        case "schedule" -> schedule = parser.text();
                        case "timezone" -> timezone = parser.text();
                        case "enabled" -> enabled = parser.booleanValue();
                        case "description" -> description = parser.text();
                        case "procedure_body" -> procedureBody = parser.text();
                        case "created_at" -> createdAt = Instant.parse(parser.text());
                        case "updated_at" -> updatedAt = Instant.parse(parser.text());
                        case "next_run" -> nextRun = Instant.parse(parser.text());
                        case "last_run" -> lastRun = Instant.parse(parser.text());
                        case "last_status" -> lastStatus = parser.text();
                        case "run_count" -> runCount = parser.longValue();
                    }
                }
            }
        }

        return new JobDefinition(
            name, schedule, timezone, enabled, description, procedureBody,
            createdAt, updatedAt, nextRun, lastRun, lastStatus, runCount
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobDefinition that = (JobDefinition) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "JobDefinition{name='" + name + "', schedule='" + schedule + "', enabled=" + enabled + "}";
    }

    /**
     * Builder for creating JobDefinition instances.
     */
    public static class Builder {
        private String name;
        private String schedule;
        private String timezone = "UTC";
        private boolean enabled = true;
        private String description;
        private String procedureBody;
        private Instant nextRun;

        public Builder name(String name) { this.name = name; return this; }
        public Builder schedule(String schedule) { this.schedule = schedule; return this; }
        public Builder timezone(String timezone) { this.timezone = timezone; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder procedureBody(String procedureBody) { this.procedureBody = procedureBody; return this; }
        public Builder nextRun(Instant nextRun) { this.nextRun = nextRun; return this; }

        public JobDefinition build() {
            return new JobDefinition(
                name, schedule, timezone, enabled, description, procedureBody,
                Instant.now(), Instant.now(), nextRun, null, null, 0
            );
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
