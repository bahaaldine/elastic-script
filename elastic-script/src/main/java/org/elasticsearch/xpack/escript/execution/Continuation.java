/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.execution;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.xcontent.ToXContentObject;
import org.elasticsearch.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Represents a continuation handler in a pipe chain.
 * A continuation defines what happens after an execution completes, fails, or at the end.
 *
 * Example syntax:
 * {@code analyze_logs() | ON_DONE process(@result) | ON_FAIL handle(@error) | FINALLY cleanup();}
 */
public class Continuation implements Writeable, ToXContentObject {

    public enum Type {
        ON_DONE,
        ON_FAIL,
        FINALLY
    }

    private final Type type;
    private final String handlerName;
    private final List<String> argumentBindings; // @result, @error, etc.
    private final String inlineLambda; // Optional: for inline lambda continuations

    /**
     * Creates a named handler continuation.
     */
    public Continuation(Type type, String handlerName, List<String> argumentBindings) {
        this.type = Objects.requireNonNull(type, "type is required");
        this.handlerName = Objects.requireNonNull(handlerName, "handlerName is required");
        this.argumentBindings = argumentBindings != null ? List.copyOf(argumentBindings) : List.of();
        this.inlineLambda = null;
    }

    /**
     * Creates an inline lambda continuation.
     */
    public Continuation(Type type, String inlineLambda) {
        this.type = Objects.requireNonNull(type, "type is required");
        this.handlerName = null;
        this.argumentBindings = List.of();
        this.inlineLambda = Objects.requireNonNull(inlineLambda, "inlineLambda is required");
    }

    /**
     * Read from stream.
     */
    public Continuation(StreamInput in) throws IOException {
        this.type = in.readEnum(Type.class);
        this.handlerName = in.readOptionalString();
        this.argumentBindings = in.readStringCollectionAsList();
        this.inlineLambda = in.readOptionalString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeEnum(type);
        out.writeOptionalString(handlerName);
        out.writeStringCollection(argumentBindings);
        out.writeOptionalString(inlineLambda);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("type", type.name());
        if (handlerName != null) {
            builder.field("handler", handlerName);
        }
        if (argumentBindings.isEmpty() == false) {
            builder.field("bindings", argumentBindings);
        }
        if (inlineLambda != null) {
            builder.field("lambda", inlineLambda);
        }
        builder.endObject();
        return builder;
    }

    public Type getType() {
        return type;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public List<String> getArgumentBindings() {
        return argumentBindings;
    }

    public String getInlineLambda() {
        return inlineLambda;
    }

    public boolean isInlineLambda() {
        return inlineLambda != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Continuation that = (Continuation) o;
        return type == that.type
            && Objects.equals(handlerName, that.handlerName)
            && Objects.equals(argumentBindings, that.argumentBindings)
            && Objects.equals(inlineLambda, that.inlineLambda);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, handlerName, argumentBindings, inlineLambda);
    }

    @Override
    public String toString() {
        if (isInlineLambda()) {
            return type.name() + " => { ... }";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(type.name()).append(" ").append(handlerName).append("(");
        sb.append(String.join(", ", argumentBindings));
        sb.append(")");
        return sb.toString();
    }
}

