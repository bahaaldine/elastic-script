/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.security;

import org.elasticsearch.xcontent.ToXContent;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentParser;

import java.io.IOException;
import java.time.Instant;

/**
 * Represents a role definition in elastic-script.
 *
 * Roles are named groups that permissions can be granted to.
 */
public class RoleDefinition implements ToXContent {

    public static final String INDEX_NAME = ".escript_roles";

    private final String name;
    private final String description;
    private final Instant createdAt;

    private RoleDefinition(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("name", name);
        if (description != null) {
            builder.field("description", description);
        }
        builder.field("created_at", createdAt.toString());
        builder.endObject();
        return builder;
    }

    public static RoleDefinition fromXContent(XContentParser parser) throws IOException {
        Builder builder = builder();

        String currentFieldName = null;
        XContentParser.Token token;

        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (token.isValue()) {
                switch (currentFieldName) {
                    case "name" -> builder.name(parser.text());
                    case "description" -> builder.description(parser.text());
                    case "created_at" -> builder.createdAt(Instant.parse(parser.text()));
                }
            }
        }

        return builder.build();
    }

    @Override
    public String toString() {
        return "RoleDefinition{name='" + name + "', description='" + description + "'}";
    }

    public static class Builder {
        private String name;
        private String description;
        private Instant createdAt;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public RoleDefinition build() {
            return new RoleDefinition(this);
        }
    }
}
