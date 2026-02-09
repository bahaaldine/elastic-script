/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.applications;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.xcontent.ToXContentObject;
import org.elasticsearch.xcontent.XContentBuilder;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Defines an Intelligent Data Application.
 * 
 * An application bundles:
 * - Sources: Data indices the application works with
 * - Skills: AI-callable procedures with metadata
 * - Intents: Natural language patterns mapped to skills
 * - Jobs: Scheduled background tasks
 * - Triggers: Event-driven reactions
 * 
 * Applications are the top-level deployment unit for elastic-script.
 */
public class ApplicationDefinition implements Writeable, ToXContentObject {

    public enum Status {
        INSTALLED,
        RUNNING,
        PAUSED,
        ERROR
    }

    private final String name;
    private final String description;
    private final String version;
    private final List<SourceDefinition> sources;
    private final List<SkillDefinition> skills;
    private final List<IntentDefinition> intents;
    private final List<String> jobNames;        // References to jobs
    private final List<String> triggerNames;    // References to triggers
    private final Map<String, Object> config;   // Runtime configuration
    private final Status status;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final String createdBy;

    private ApplicationDefinition(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "name is required");
        this.description = builder.description;
        this.version = builder.version != null ? builder.version : "1.0.0";
        this.sources = builder.sources != null ? List.copyOf(builder.sources) : List.of();
        this.skills = builder.skills != null ? List.copyOf(builder.skills) : List.of();
        this.intents = builder.intents != null ? List.copyOf(builder.intents) : List.of();
        this.jobNames = builder.jobNames != null ? List.copyOf(builder.jobNames) : List.of();
        this.triggerNames = builder.triggerNames != null ? List.copyOf(builder.triggerNames) : List.of();
        this.config = builder.config != null ? Map.copyOf(builder.config) : Map.of();
        this.status = builder.status != null ? builder.status : Status.INSTALLED;
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
        this.updatedAt = builder.updatedAt;
        this.createdBy = builder.createdBy;
    }

    public ApplicationDefinition(StreamInput in) throws IOException {
        this.name = in.readString();
        this.description = in.readOptionalString();
        this.version = in.readString();
        this.sources = in.readCollectionAsList(SourceDefinition::new);
        this.skills = in.readCollectionAsList(SkillDefinition::new);
        this.intents = in.readCollectionAsList(IntentDefinition::new);
        this.jobNames = in.readStringCollectionAsList();
        this.triggerNames = in.readStringCollectionAsList();
        this.config = in.readGenericMap();
        this.status = in.readEnum(Status.class);
        this.createdAt = in.readInstant();
        this.updatedAt = in.readOptionalInstant();
        this.createdBy = in.readOptionalString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(name);
        out.writeOptionalString(description);
        out.writeString(version);
        out.writeCollection(sources);
        out.writeCollection(skills);
        out.writeCollection(intents);
        out.writeStringCollection(jobNames);
        out.writeStringCollection(triggerNames);
        out.writeGenericMap(config);
        out.writeEnum(status);
        out.writeInstant(createdAt);
        out.writeOptionalInstant(updatedAt);
        out.writeOptionalString(createdBy);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("name", name);
        if (description != null) {
            builder.field("description", description);
        }
        builder.field("version", version);
        builder.field("status", status.name());
        
        if (!sources.isEmpty()) {
            builder.startArray("sources");
            for (SourceDefinition source : sources) {
                source.toXContent(builder, params);
            }
            builder.endArray();
        }
        
        if (!skills.isEmpty()) {
            builder.startArray("skills");
            for (SkillDefinition skill : skills) {
                skill.toXContent(builder, params);
            }
            builder.endArray();
        }
        
        if (!intents.isEmpty()) {
            builder.startArray("intents");
            for (IntentDefinition intent : intents) {
                intent.toXContent(builder, params);
            }
            builder.endArray();
        }
        
        if (!jobNames.isEmpty()) {
            builder.field("jobs", jobNames);
        }
        
        if (!triggerNames.isEmpty()) {
            builder.field("triggers", triggerNames);
        }
        
        if (!config.isEmpty()) {
            builder.field("config", config);
        }
        
        builder.field("created_at", createdAt.toString());
        if (updatedAt != null) {
            builder.field("updated_at", updatedAt.toString());
        }
        if (createdBy != null) {
            builder.field("created_by", createdBy);
        }
        
        builder.endObject();
        return builder;
    }

    // Getters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getVersion() { return version; }
    public List<SourceDefinition> getSources() { return sources; }
    public List<SkillDefinition> getSkills() { return skills; }
    public List<IntentDefinition> getIntents() { return intents; }
    public List<String> getJobNames() { return jobNames; }
    public List<String> getTriggerNames() { return triggerNames; }
    public Map<String, Object> getConfig() { return config; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getCreatedBy() { return createdBy; }

    /**
     * Finds a skill by name.
     */
    public SkillDefinition findSkill(String skillName) {
        return skills.stream()
            .filter(s -> s.getName().equalsIgnoreCase(skillName))
            .findFirst()
            .orElse(null);
    }

    /**
     * Matches user input against intents and returns the best match.
     */
    public IntentMatch matchIntent(String userInput) {
        IntentDefinition bestMatch = null;
        double bestScore = 0;
        
        for (IntentDefinition intent : intents) {
            double score = intent.matchScore(userInput);
            if (score > bestScore) {
                bestScore = score;
                bestMatch = intent;
            }
        }
        
        if (bestMatch != null && bestScore > 0) {
            return new IntentMatch(bestMatch, bestScore);
        }
        return null;
    }

    /**
     * Creates a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder from this definition (for modifications).
     */
    public Builder toBuilder() {
        return new Builder()
            .name(name)
            .description(description)
            .version(version)
            .sources(sources)
            .skills(skills)
            .intents(intents)
            .jobNames(jobNames)
            .triggerNames(triggerNames)
            .config(config)
            .status(status)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .createdBy(createdBy);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApplicationDefinition that = (ApplicationDefinition) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version);
    }

    /**
     * Result of intent matching.
     */
    public static class IntentMatch {
        private final IntentDefinition intent;
        private final double confidence;

        public IntentMatch(IntentDefinition intent, double confidence) {
            this.intent = intent;
            this.confidence = confidence;
        }

        public IntentDefinition getIntent() { return intent; }
        public double getConfidence() { return confidence; }
    }

    /**
     * Builder for ApplicationDefinition.
     */
    public static class Builder {
        private String name;
        private String description;
        private String version;
        private List<SourceDefinition> sources;
        private List<SkillDefinition> skills;
        private List<IntentDefinition> intents;
        private List<String> jobNames;
        private List<String> triggerNames;
        private Map<String, Object> config;
        private Status status;
        private Instant createdAt;
        private Instant updatedAt;
        private String createdBy;

        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder version(String version) { this.version = version; return this; }
        public Builder sources(List<SourceDefinition> sources) { this.sources = sources; return this; }
        public Builder skills(List<SkillDefinition> skills) { this.skills = skills; return this; }
        public Builder intents(List<IntentDefinition> intents) { this.intents = intents; return this; }
        public Builder jobNames(List<String> jobNames) { this.jobNames = jobNames; return this; }
        public Builder triggerNames(List<String> triggerNames) { this.triggerNames = triggerNames; return this; }
        public Builder config(Map<String, Object> config) { this.config = config; return this; }
        public Builder status(Status status) { this.status = status; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder createdBy(String createdBy) { this.createdBy = createdBy; return this; }

        public ApplicationDefinition build() {
            return new ApplicationDefinition(this);
        }
    }

    /**
     * Defines a data source for an application.
     */
    public static class SourceDefinition implements Writeable, ToXContentObject {
        private final String name;
        private final String indexPattern;

        public SourceDefinition(String name, String indexPattern) {
            this.name = Objects.requireNonNull(name);
            this.indexPattern = Objects.requireNonNull(indexPattern);
        }

        public SourceDefinition(StreamInput in) throws IOException {
            this.name = in.readString();
            this.indexPattern = in.readString();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeString(name);
            out.writeString(indexPattern);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.field("name", name);
            builder.field("index_pattern", indexPattern);
            builder.endObject();
            return builder;
        }

        public String getName() { return name; }
        public String getIndexPattern() { return indexPattern; }
    }
}
