/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.packages;

import org.elasticsearch.xcontent.ToXContent;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentParser;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a package definition in elastic-script.
 * 
 * A package groups related procedures, functions, and variables together.
 * Packages have:
 * - Specification (CREATE PACKAGE) - declares public interface
 * - Body (CREATE PACKAGE BODY) - implements procedures and functions
 */
public class PackageDefinition implements ToXContent {
    
    public static final String INDEX_NAME = ".escript_packages";
    
    private final String name;
    private final List<ProcedureSpec> procedures;
    private final List<FunctionSpec> functions;
    private final List<VariableSpec> variables;
    private final String specificationText;  // Original CREATE PACKAGE text
    private final String bodyText;           // Original CREATE PACKAGE BODY text
    private final Instant createdAt;
    private final Instant updatedAt;
    
    private PackageDefinition(Builder builder) {
        this.name = builder.name;
        this.procedures = new ArrayList<>(builder.procedures);
        this.functions = new ArrayList<>(builder.functions);
        this.variables = new ArrayList<>(builder.variables);
        this.specificationText = builder.specificationText;
        this.bodyText = builder.bodyText;
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
        this.updatedAt = builder.updatedAt != null ? builder.updatedAt : Instant.now();
    }
    
    public String getName() {
        return name;
    }
    
    public List<ProcedureSpec> getProcedures() {
        return procedures;
    }
    
    public List<FunctionSpec> getFunctions() {
        return functions;
    }
    
    public List<VariableSpec> getVariables() {
        return variables;
    }
    
    public String getSpecificationText() {
        return specificationText;
    }
    
    public String getBodyText() {
        return bodyText;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public boolean hasBody() {
        return bodyText != null && !bodyText.isEmpty();
    }
    
    /**
     * Get a procedure by name.
     */
    public ProcedureSpec getProcedure(String procName) {
        return procedures.stream()
            .filter(p -> p.name.equals(procName))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Get a function by name.
     */
    public FunctionSpec getFunction(String funcName) {
        return functions.stream()
            .filter(f -> f.name.equals(funcName))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Get a variable by name.
     */
    public VariableSpec getVariable(String varName) {
        return variables.stream()
            .filter(v -> v.name.equals(varName))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Check if a member is public.
     */
    public boolean isPublic(String memberName) {
        ProcedureSpec proc = getProcedure(memberName);
        if (proc != null) return proc.isPublic;
        
        FunctionSpec func = getFunction(memberName);
        if (func != null) return func.isPublic;
        
        VariableSpec var = getVariable(memberName);
        if (var != null) return var.isPublic;
        
        return false;
    }
    
    /**
     * Create a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Create a copy with updated body.
     */
    public PackageDefinition withBody(String bodyText) {
        return builder()
            .name(this.name)
            .procedures(this.procedures)
            .functions(this.functions)
            .variables(this.variables)
            .specificationText(this.specificationText)
            .bodyText(bodyText)
            .createdAt(this.createdAt)
            .updatedAt(Instant.now())
            .build();
    }
    
    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("name", name);
        builder.field("specification_text", specificationText);
        builder.field("body_text", bodyText);
        builder.field("created_at", createdAt.toString());
        builder.field("updated_at", updatedAt.toString());
        
        // Procedures
        builder.startArray("procedures");
        for (ProcedureSpec proc : procedures) {
            builder.startObject();
            builder.field("name", proc.name);
            builder.field("is_public", proc.isPublic);
            builder.field("parameters", proc.parameters);
            builder.endObject();
        }
        builder.endArray();
        
        // Functions
        builder.startArray("functions");
        for (FunctionSpec func : functions) {
            builder.startObject();
            builder.field("name", func.name);
            builder.field("is_public", func.isPublic);
            builder.field("parameters", func.parameters);
            builder.field("return_type", func.returnType);
            builder.endObject();
        }
        builder.endArray();
        
        // Variables
        builder.startArray("variables");
        for (VariableSpec var : variables) {
            builder.startObject();
            builder.field("name", var.name);
            builder.field("is_public", var.isPublic);
            builder.field("type", var.type);
            builder.field("default_value", var.defaultValue);
            builder.endObject();
        }
        builder.endArray();
        
        builder.endObject();
        return builder;
    }
    
    /**
     * Parse from XContent.
     */
    public static PackageDefinition fromXContent(XContentParser parser) throws IOException {
        Builder builder = builder();
        
        String currentFieldName = null;
        XContentParser.Token token;
        
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (token.isValue()) {
                switch (currentFieldName) {
                    case "name" -> builder.name(parser.text());
                    case "specification_text" -> builder.specificationText(parser.text());
                    case "body_text" -> builder.bodyText(parser.text());
                    case "created_at" -> builder.createdAt(Instant.parse(parser.text()));
                    case "updated_at" -> builder.updatedAt(Instant.parse(parser.text()));
                }
            } else if (token == XContentParser.Token.START_ARRAY) {
                switch (currentFieldName) {
                    case "procedures" -> builder.procedures(parseProcedures(parser));
                    case "functions" -> builder.functions(parseFunctions(parser));
                    case "variables" -> builder.variables(parseVariables(parser));
                }
            }
        }
        
        return builder.build();
    }
    
    private static List<ProcedureSpec> parseProcedures(XContentParser parser) throws IOException {
        List<ProcedureSpec> procedures = new ArrayList<>();
        while (parser.nextToken() != XContentParser.Token.END_ARRAY) {
            if (parser.currentToken() == XContentParser.Token.START_OBJECT) {
                String name = null;
                boolean isPublic = true;
                List<String> parameters = new ArrayList<>();
                
                String field;
                while (parser.nextToken() != XContentParser.Token.END_OBJECT) {
                    if (parser.currentToken() == XContentParser.Token.FIELD_NAME) {
                        field = parser.currentName();
                        parser.nextToken();
                        switch (field) {
                            case "name" -> name = parser.text();
                            case "is_public" -> isPublic = parser.booleanValue();
                            case "parameters" -> {
                                while (parser.nextToken() != XContentParser.Token.END_ARRAY) {
                                    parameters.add(parser.text());
                                }
                            }
                        }
                    }
                }
                procedures.add(new ProcedureSpec(name, isPublic, parameters));
            }
        }
        return procedures;
    }
    
    private static List<FunctionSpec> parseFunctions(XContentParser parser) throws IOException {
        List<FunctionSpec> functions = new ArrayList<>();
        while (parser.nextToken() != XContentParser.Token.END_ARRAY) {
            if (parser.currentToken() == XContentParser.Token.START_OBJECT) {
                String name = null;
                boolean isPublic = true;
                List<String> parameters = new ArrayList<>();
                String returnType = null;
                
                String field;
                while (parser.nextToken() != XContentParser.Token.END_OBJECT) {
                    if (parser.currentToken() == XContentParser.Token.FIELD_NAME) {
                        field = parser.currentName();
                        parser.nextToken();
                        switch (field) {
                            case "name" -> name = parser.text();
                            case "is_public" -> isPublic = parser.booleanValue();
                            case "return_type" -> returnType = parser.text();
                            case "parameters" -> {
                                while (parser.nextToken() != XContentParser.Token.END_ARRAY) {
                                    parameters.add(parser.text());
                                }
                            }
                        }
                    }
                }
                functions.add(new FunctionSpec(name, isPublic, parameters, returnType));
            }
        }
        return functions;
    }
    
    private static List<VariableSpec> parseVariables(XContentParser parser) throws IOException {
        List<VariableSpec> variables = new ArrayList<>();
        while (parser.nextToken() != XContentParser.Token.END_ARRAY) {
            if (parser.currentToken() == XContentParser.Token.START_OBJECT) {
                String name = null;
                boolean isPublic = true;
                String type = null;
                String defaultValue = null;
                
                String field;
                while (parser.nextToken() != XContentParser.Token.END_OBJECT) {
                    if (parser.currentToken() == XContentParser.Token.FIELD_NAME) {
                        field = parser.currentName();
                        parser.nextToken();
                        switch (field) {
                            case "name" -> name = parser.text();
                            case "is_public" -> isPublic = parser.booleanValue();
                            case "type" -> type = parser.text();
                            case "default_value" -> defaultValue = parser.textOrNull();
                        }
                    }
                }
                variables.add(new VariableSpec(name, isPublic, type, defaultValue));
            }
        }
        return variables;
    }
    
    @Override
    public String toString() {
        return "PackageDefinition{name='" + name + "', procedures=" + procedures.size() + 
               ", functions=" + functions.size() + ", variables=" + variables.size() + 
               ", hasBody=" + hasBody() + "}";
    }
    
    // ==================== Inner Classes ====================
    
    /**
     * Procedure specification.
     */
    public static class ProcedureSpec {
        public final String name;
        public final boolean isPublic;
        public final List<String> parameters;
        
        public ProcedureSpec(String name, boolean isPublic, List<String> parameters) {
            this.name = name;
            this.isPublic = isPublic;
            this.parameters = parameters;
        }
    }
    
    /**
     * Function specification.
     */
    public static class FunctionSpec {
        public final String name;
        public final boolean isPublic;
        public final List<String> parameters;
        public final String returnType;
        
        public FunctionSpec(String name, boolean isPublic, List<String> parameters, String returnType) {
            this.name = name;
            this.isPublic = isPublic;
            this.parameters = parameters;
            this.returnType = returnType;
        }
    }
    
    /**
     * Variable specification.
     */
    public static class VariableSpec {
        public final String name;
        public final boolean isPublic;
        public final String type;
        public final String defaultValue;
        
        public VariableSpec(String name, boolean isPublic, String type, String defaultValue) {
            this.name = name;
            this.isPublic = isPublic;
            this.type = type;
            this.defaultValue = defaultValue;
        }
    }
    
    // ==================== Builder ====================
    
    public static class Builder {
        private String name;
        private List<ProcedureSpec> procedures = new ArrayList<>();
        private List<FunctionSpec> functions = new ArrayList<>();
        private List<VariableSpec> variables = new ArrayList<>();
        private String specificationText;
        private String bodyText;
        private Instant createdAt;
        private Instant updatedAt;
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder procedures(List<ProcedureSpec> procedures) {
            this.procedures = procedures;
            return this;
        }
        
        public Builder addProcedure(ProcedureSpec proc) {
            this.procedures.add(proc);
            return this;
        }
        
        public Builder functions(List<FunctionSpec> functions) {
            this.functions = functions;
            return this;
        }
        
        public Builder addFunction(FunctionSpec func) {
            this.functions.add(func);
            return this;
        }
        
        public Builder variables(List<VariableSpec> variables) {
            this.variables = variables;
            return this;
        }
        
        public Builder addVariable(VariableSpec var) {
            this.variables.add(var);
            return this;
        }
        
        public Builder specificationText(String specificationText) {
            this.specificationText = specificationText;
            return this;
        }
        
        public Builder bodyText(String bodyText) {
            this.bodyText = bodyText;
            return this;
        }
        
        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        
        public PackageDefinition build() {
            return new PackageDefinition(this);
        }
    }
}
