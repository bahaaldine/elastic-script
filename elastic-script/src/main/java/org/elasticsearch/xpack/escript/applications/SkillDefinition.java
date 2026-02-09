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
import java.util.Map;
import java.util.Objects;

/**
 * Defines a skill that can be invoked by AI agents or natural language.
 * 
 * Skills are the external interface to application functionality.
 * They wrap procedures with metadata suitable for AI agent consumption.
 * 
 * Example:
 * <pre>
 * SKILL detect_churn(threshold NUMBER, timeframe STRING) 
 *   RETURNS ARRAY 
 *   DESCRIPTION 'Identifies customers likely to churn'
 *   AS run_churn_analysis(threshold, timeframe)
 * </pre>
 */
public class SkillDefinition implements Writeable, ToXContentObject {

    private final String name;
    private final String description;
    private final List<SkillParameter> parameters;
    private final String returnType;
    private final String procedureName;
    private final List<String> procedureArgs;
    private final List<String> examples;  // Natural language examples for AI discovery

    public SkillDefinition(
        String name,
        String description,
        List<SkillParameter> parameters,
        String returnType,
        String procedureName,
        List<String> procedureArgs
    ) {
        this(name, description, parameters, returnType, procedureName, procedureArgs, null);
    }

    public SkillDefinition(
        String name,
        String description,
        List<SkillParameter> parameters,
        String returnType,
        String procedureName,
        List<String> procedureArgs,
        List<String> examples
    ) {
        this.name = Objects.requireNonNull(name, "name is required");
        this.description = description;
        this.parameters = parameters != null ? List.copyOf(parameters) : List.of();
        this.returnType = returnType;
        this.procedureName = Objects.requireNonNull(procedureName, "procedureName is required");
        this.procedureArgs = procedureArgs != null ? List.copyOf(procedureArgs) : List.of();
        this.examples = examples != null ? List.copyOf(examples) : List.of();
    }

    public SkillDefinition(StreamInput in) throws IOException {
        this.name = in.readString();
        this.description = in.readOptionalString();
        this.parameters = in.readCollectionAsList(SkillParameter::new);
        this.returnType = in.readOptionalString();
        this.procedureName = in.readString();
        this.procedureArgs = in.readStringCollectionAsList();
        this.examples = in.readStringCollectionAsList();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(name);
        out.writeOptionalString(description);
        out.writeCollection(parameters);
        out.writeOptionalString(returnType);
        out.writeString(procedureName);
        out.writeStringCollection(procedureArgs);
        out.writeStringCollection(examples);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("name", name);
        if (description != null) {
            builder.field("description", description);
        }
        if (!parameters.isEmpty()) {
            builder.startArray("parameters");
            for (SkillParameter param : parameters) {
                param.toXContent(builder, params);
            }
            builder.endArray();
        }
        if (returnType != null) {
            builder.field("return_type", returnType);
        }
        builder.field("procedure", procedureName);
        if (!procedureArgs.isEmpty()) {
            builder.field("procedure_args", procedureArgs);
        }
        if (!examples.isEmpty()) {
            builder.field("examples", examples);
        }
        builder.endObject();
        return builder;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<SkillParameter> getParameters() {
        return parameters;
    }

    public String getReturnType() {
        return returnType;
    }

    public String getProcedureName() {
        return procedureName;
    }

    public List<String> getProcedureArgs() {
        return procedureArgs;
    }

    public List<String> getExamples() {
        return examples;
    }

    /**
     * Generates MCP tool specification for this skill.
     */
    public Map<String, Object> toMcpToolSpec() {
        var spec = new java.util.HashMap<String, Object>();
        spec.put("name", name);
        spec.put("description", description != null ? description : "");
        spec.put("inputSchema", buildInputSchema());
        if (!examples.isEmpty()) {
            spec.put("examples", examples);
        }
        return spec;
    }

    private Map<String, Object> buildInputSchema() {
        var properties = new java.util.HashMap<String, Object>();
        var required = new java.util.ArrayList<String>();

        for (SkillParameter param : parameters) {
            properties.put(param.getName(), Map.of(
                "type", mapTypeToJsonSchema(param.getType()),
                "description", param.getDescription() != null ? param.getDescription() : ""
            ));
            if (param.isRequired()) {
                required.add(param.getName());
            }
        }

        return Map.of(
            "type", "object",
            "properties", properties,
            "required", required
        );
    }

    private String mapTypeToJsonSchema(String escriptType) {
        if (escriptType == null) return "string";
        return switch (escriptType.toUpperCase()) {
            case "NUMBER", "INT", "FLOAT" -> "number";
            case "BOOLEAN" -> "boolean";
            case "ARRAY" -> "array";
            case "DOCUMENT", "MAP" -> "object";
            default -> "string";
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SkillDefinition that = (SkillDefinition) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(description, that.description) &&
               Objects.equals(parameters, that.parameters) &&
               Objects.equals(returnType, that.returnType) &&
               Objects.equals(procedureName, that.procedureName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, parameters, returnType, procedureName);
    }

    /**
     * Represents a parameter for a skill.
     */
    public static class SkillParameter implements Writeable, ToXContentObject {
        private final String name;
        private final String type;
        private final String description;
        private final boolean required;
        private final Object defaultValue;

        public SkillParameter(String name, String type, String description, boolean required, Object defaultValue) {
            this.name = name;
            this.type = type;
            this.description = description;
            this.required = required;
            this.defaultValue = defaultValue;
        }

        public SkillParameter(StreamInput in) throws IOException {
            this.name = in.readString();
            this.type = in.readOptionalString();
            this.description = in.readOptionalString();
            this.required = in.readBoolean();
            this.defaultValue = in.readGenericValue();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeString(name);
            out.writeOptionalString(type);
            out.writeOptionalString(description);
            out.writeBoolean(required);
            out.writeGenericValue(defaultValue);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.field("name", name);
            if (type != null) {
                builder.field("type", type);
            }
            if (description != null) {
                builder.field("description", description);
            }
            builder.field("required", required);
            if (defaultValue != null) {
                builder.field("default", defaultValue);
            }
            builder.endObject();
            return builder;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public String getDescription() {
            return description;
        }

        public boolean isRequired() {
            return required;
        }

        public Object getDefaultValue() {
            return defaultValue;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SkillParameter that = (SkillParameter) o;
            return required == that.required &&
                   Objects.equals(name, that.name) &&
                   Objects.equals(type, that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type, required);
        }
    }
}
