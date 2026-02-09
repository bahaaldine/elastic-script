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
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Defines an intent mapping from natural language to a skill or procedure.
 * 
 * Intents allow users to interact with applications using natural language.
 * The system matches user input against patterns and routes to the appropriate skill.
 * 
 * Example:
 * <pre>
 * INTENT "churn|leaving|at risk" => detect_churn
 * </pre>
 * 
 * User says: "Show me customers who might be leaving"
 * System matches pattern "leaving" and invokes detect_churn skill
 */
public class IntentDefinition implements Writeable, ToXContentObject {

    private final String pattern;
    private final String targetSkill;
    private final String description;
    private final List<ParameterMapping> parameterMappings;
    private transient Pattern compiledPattern;

    public IntentDefinition(
        String pattern,
        String targetSkill,
        String description,
        List<ParameterMapping> parameterMappings
    ) {
        this.pattern = Objects.requireNonNull(pattern, "pattern is required");
        this.targetSkill = Objects.requireNonNull(targetSkill, "targetSkill is required");
        this.description = description;
        this.parameterMappings = parameterMappings != null ? List.copyOf(parameterMappings) : List.of();
    }

    public IntentDefinition(StreamInput in) throws IOException {
        this.pattern = in.readString();
        this.targetSkill = in.readString();
        this.description = in.readOptionalString();
        this.parameterMappings = in.readCollectionAsList(ParameterMapping::new);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(pattern);
        out.writeString(targetSkill);
        out.writeOptionalString(description);
        out.writeCollection(parameterMappings);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("pattern", pattern);
        builder.field("target_skill", targetSkill);
        if (description != null) {
            builder.field("description", description);
        }
        if (!parameterMappings.isEmpty()) {
            builder.startArray("parameter_mappings");
            for (ParameterMapping mapping : parameterMappings) {
                mapping.toXContent(builder, params);
            }
            builder.endArray();
        }
        builder.endObject();
        return builder;
    }

    public String getPattern() {
        return pattern;
    }

    public String getTargetSkill() {
        return targetSkill;
    }

    public String getDescription() {
        return description;
    }

    public List<ParameterMapping> getParameterMappings() {
        return parameterMappings;
    }

    /**
     * Gets the compiled regex pattern, creating it lazily.
     * The pattern string uses | as OR separator: "churn|leaving|at risk"
     */
    public Pattern getCompiledPattern() {
        if (compiledPattern == null) {
            // Convert simple pattern to regex
            // "churn|leaving|at risk" -> "(?i).*(?:churn|leaving|at risk).*"
            String regex = "(?i).*(?:" + pattern + ").*";
            compiledPattern = Pattern.compile(regex);
        }
        return compiledPattern;
    }

    /**
     * Tests if the given user input matches this intent.
     */
    public boolean matches(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return false;
        }
        return getCompiledPattern().matcher(userInput).matches();
    }

    /**
     * Calculates a match score for ranking purposes.
     * Higher score = more specific match.
     */
    public double matchScore(String userInput) {
        if (!matches(userInput)) {
            return 0.0;
        }
        // Simple scoring: more pattern parts that match = higher score
        String[] patternParts = pattern.split("\\|");
        int matches = 0;
        String lowerInput = userInput.toLowerCase();
        for (String part : patternParts) {
            if (lowerInput.contains(part.toLowerCase().trim())) {
                matches++;
            }
        }
        return (double) matches / patternParts.length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntentDefinition that = (IntentDefinition) o;
        return Objects.equals(pattern, that.pattern) &&
               Objects.equals(targetSkill, that.targetSkill);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pattern, targetSkill);
    }

    /**
     * Maps a named entity or slot in user input to a skill parameter.
     * 
     * Example: "Show me customers from {region}" 
     *   -> ParameterMapping("region", "region_filter")
     */
    public static class ParameterMapping implements Writeable, ToXContentObject {
        private final String slotName;
        private final String parameterName;
        private final String extractionPattern;

        public ParameterMapping(String slotName, String parameterName, String extractionPattern) {
            this.slotName = slotName;
            this.parameterName = parameterName;
            this.extractionPattern = extractionPattern;
        }

        public ParameterMapping(StreamInput in) throws IOException {
            this.slotName = in.readString();
            this.parameterName = in.readString();
            this.extractionPattern = in.readOptionalString();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeString(slotName);
            out.writeString(parameterName);
            out.writeOptionalString(extractionPattern);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.field("slot", slotName);
            builder.field("parameter", parameterName);
            if (extractionPattern != null) {
                builder.field("extraction_pattern", extractionPattern);
            }
            builder.endObject();
            return builder;
        }

        public String getSlotName() {
            return slotName;
        }

        public String getParameterName() {
            return parameterName;
        }

        public String getExtractionPattern() {
            return extractionPattern;
        }
    }
}
