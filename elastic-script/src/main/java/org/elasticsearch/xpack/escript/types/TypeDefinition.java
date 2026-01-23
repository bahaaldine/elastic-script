/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.types;

import org.elasticsearch.xcontent.ToXContent;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentParser;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a user-defined type in elastic-script.
 *
 * Types are record structures with named fields and their data types.
 * Similar to PL/SQL's TYPE ... IS RECORD.
 */
public class TypeDefinition implements ToXContent {

    public static final String INDEX_NAME = ".escript_types";

    private final String name;
    private final List<TypeField> fields;
    private final Instant createdAt;

    private TypeDefinition(Builder builder) {
        this.name = builder.name;
        this.fields = new ArrayList<>(builder.fields);
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
    }

    public String getName() {
        return name;
    }

    public List<TypeField> getFields() {
        return fields;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Get a field by name.
     */
    public TypeField getField(String fieldName) {
        return fields.stream()
            .filter(f -> f.name.equals(fieldName))
            .findFirst()
            .orElse(null);
    }

    /**
     * Check if type has a field.
     */
    public boolean hasField(String fieldName) {
        return getField(fieldName) != null;
    }

    /**
     * Get field names as a list.
     */
    public List<String> getFieldNames() {
        return fields.stream().map(f -> f.name).toList();
    }

    /**
     * Create a new instance of this type with default values.
     */
    public Map<String, Object> createInstance() {
        Map<String, Object> instance = new LinkedHashMap<>();
        for (TypeField field : fields) {
            instance.put(field.name, field.getDefaultValue());
        }
        return instance;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("name", name);
        builder.field("created_at", createdAt.toString());

        builder.startArray("fields");
        for (TypeField field : fields) {
            builder.startObject();
            builder.field("name", field.name);
            builder.field("type", field.type);
            builder.endObject();
        }
        builder.endArray();

        builder.endObject();
        return builder;
    }

    public static TypeDefinition fromXContent(XContentParser parser) throws IOException {
        Builder builder = builder();

        String currentFieldName = null;
        XContentParser.Token token;

        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (token.isValue()) {
                switch (currentFieldName) {
                    case "name" -> builder.name(parser.text());
                    case "created_at" -> builder.createdAt(Instant.parse(parser.text()));
                }
            } else if (token == XContentParser.Token.START_ARRAY && "fields".equals(currentFieldName)) {
                while (parser.nextToken() != XContentParser.Token.END_ARRAY) {
                    if (parser.currentToken() == XContentParser.Token.START_OBJECT) {
                        String fieldName = null;
                        String fieldType = null;

                        while (parser.nextToken() != XContentParser.Token.END_OBJECT) {
                            if (parser.currentToken() == XContentParser.Token.FIELD_NAME) {
                                String field = parser.currentName();
                                parser.nextToken();
                                switch (field) {
                                    case "name" -> fieldName = parser.text();
                                    case "type" -> fieldType = parser.text();
                                }
                            }
                        }

                        if (fieldName != null && fieldType != null) {
                            builder.addField(new TypeField(fieldName, fieldType));
                        }
                    }
                }
            }
        }

        return builder.build();
    }

    @Override
    public String toString() {
        return "TypeDefinition{name='" + name + "', fields=" + fields.size() + "}";
    }

    // ==================== Inner Classes ====================

    /**
     * Represents a field in a type.
     */
    public static class TypeField {
        public final String name;
        public final String type;

        public TypeField(String name, String type) {
            this.name = name;
            this.type = type;
        }

        /**
         * Get default value for this field type.
         */
        public Object getDefaultValue() {
            return switch (type.toUpperCase()) {
                case "NUMBER", "INT", "FLOAT" -> 0;
                case "STRING" -> "";
                case "BOOLEAN" -> false;
                case "DATE" -> null;
                case "ARRAY" -> new ArrayList<>();
                case "DOCUMENT", "MAP" -> new LinkedHashMap<>();
                default -> null;
            };
        }

        @Override
        public String toString() {
            return name + " " + type;
        }
    }

    // ==================== Builder ====================

    public static class Builder {
        private String name;
        private List<TypeField> fields = new ArrayList<>();
        private Instant createdAt;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder addField(TypeField field) {
            this.fields.add(field);
            return this;
        }

        public Builder fields(List<TypeField> fields) {
            this.fields = fields;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public TypeDefinition build() {
            return new TypeDefinition(this);
        }
    }
}
