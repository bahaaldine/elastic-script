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
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a permission grant in elastic-script.
 *
 * Permissions link a principal (user or role) to privileges on specific objects.
 */
public class PermissionDefinition implements ToXContent {

    public static final String INDEX_NAME = ".escript_permissions";

    private final String id;
    private final String principalType;  // "ROLE" or "USER"
    private final String principalName;
    private final String objectType;     // "PROCEDURE", "FUNCTION", "PACKAGE", "JOB", "TRIGGER"
    private final String objectName;
    private final Set<String> privileges;  // "EXECUTE", etc.
    private final Instant grantedAt;
    private final String grantedBy;

    private PermissionDefinition(Builder builder) {
        this.id = builder.id;
        this.principalType = builder.principalType;
        this.principalName = builder.principalName;
        this.objectType = builder.objectType;
        this.objectName = builder.objectName;
        this.privileges = new HashSet<>(builder.privileges);
        this.grantedAt = builder.grantedAt != null ? builder.grantedAt : Instant.now();
        this.grantedBy = builder.grantedBy;
    }

    public String getId() {
        return id;
    }

    public String getPrincipalType() {
        return principalType;
    }

    public String getPrincipalName() {
        return principalName;
    }

    public String getObjectType() {
        return objectType;
    }

    public String getObjectName() {
        return objectName;
    }

    public Set<String> getPrivileges() {
        return privileges;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public String getGrantedBy() {
        return grantedBy;
    }

    public boolean hasPrivilege(String privilege) {
        return privileges.contains(privilege) || privileges.contains("ALL");
    }

    /**
     * Generate a unique ID for this permission.
     */
    public static String generateId(String principalType, String principalName,
                                     String objectType, String objectName) {
        return String.format("%s:%s:%s:%s", principalType, principalName, objectType, objectName);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("id", id);
        builder.field("principal_type", principalType);
        builder.field("principal_name", principalName);
        builder.field("object_type", objectType);
        builder.field("object_name", objectName);
        builder.array("privileges", privileges.toArray(new String[0]));
        builder.field("granted_at", grantedAt.toString());
        if (grantedBy != null) {
            builder.field("granted_by", grantedBy);
        }
        builder.endObject();
        return builder;
    }

    public static PermissionDefinition fromXContent(XContentParser parser) throws IOException {
        Builder builder = builder();

        String currentFieldName = null;
        XContentParser.Token token;

        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (token.isValue()) {
                switch (currentFieldName) {
                    case "id" -> builder.id(parser.text());
                    case "principal_type" -> builder.principalType(parser.text());
                    case "principal_name" -> builder.principalName(parser.text());
                    case "object_type" -> builder.objectType(parser.text());
                    case "object_name" -> builder.objectName(parser.text());
                    case "granted_at" -> builder.grantedAt(Instant.parse(parser.text()));
                    case "granted_by" -> builder.grantedBy(parser.text());
                }
            } else if (token == XContentParser.Token.START_ARRAY && "privileges".equals(currentFieldName)) {
                while (parser.nextToken() != XContentParser.Token.END_ARRAY) {
                    builder.addPrivilege(parser.text());
                }
            }
        }

        return builder.build();
    }

    @Override
    public String toString() {
        return "PermissionDefinition{" +
               "principalType='" + principalType + '\'' +
               ", principalName='" + principalName + '\'' +
               ", objectType='" + objectType + '\'' +
               ", objectName='" + objectName + '\'' +
               ", privileges=" + privileges +
               '}';
    }

    public static class Builder {
        private String id;
        private String principalType;
        private String principalName;
        private String objectType;
        private String objectName;
        private Set<String> privileges = new HashSet<>();
        private Instant grantedAt;
        private String grantedBy;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder principalType(String principalType) {
            this.principalType = principalType;
            return this;
        }

        public Builder principalName(String principalName) {
            this.principalName = principalName;
            return this;
        }

        public Builder objectType(String objectType) {
            this.objectType = objectType;
            return this;
        }

        public Builder objectName(String objectName) {
            this.objectName = objectName;
            return this;
        }

        public Builder privileges(Set<String> privileges) {
            this.privileges = privileges;
            return this;
        }

        public Builder addPrivilege(String privilege) {
            this.privileges.add(privilege);
            return this;
        }

        public Builder grantedAt(Instant grantedAt) {
            this.grantedAt = grantedAt;
            return this;
        }

        public Builder grantedBy(String grantedBy) {
            this.grantedBy = grantedBy;
            return this;
        }

        public PermissionDefinition build() {
            if (id == null) {
                id = generateId(principalType, principalName, objectType, objectName);
            }
            return new PermissionDefinition(this);
        }
    }
}
