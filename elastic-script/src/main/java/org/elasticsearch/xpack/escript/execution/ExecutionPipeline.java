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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the complete pipeline of continuations for an async execution.
 *
 * A pipeline contains ON_DONE handlers (called on success), ON_FAIL handlers (called on failure),
 * and FINALLY handlers (always called at the end).
 *
 * Example: {@code analyze_logs() | ON_DONE process(@result) | ON_FAIL alert(@error) | FINALLY cleanup();}
 */
public class ExecutionPipeline implements Writeable, ToXContentObject {

    private final List<Continuation> onDoneHandlers;
    private final List<Continuation> onFailHandlers;
    private final List<Continuation> finallyHandlers;
    private final String trackName;
    private final Integer timeoutSeconds;

    private ExecutionPipeline(Builder builder) {
        this.onDoneHandlers = Collections.unmodifiableList(builder.onDoneHandlers);
        this.onFailHandlers = Collections.unmodifiableList(builder.onFailHandlers);
        this.finallyHandlers = Collections.unmodifiableList(builder.finallyHandlers);
        this.trackName = builder.trackName;
        this.timeoutSeconds = builder.timeoutSeconds;
    }

    /**
     * Read from stream.
     */
    public ExecutionPipeline(StreamInput in) throws IOException {
        this.onDoneHandlers = in.readCollectionAsList(Continuation::new);
        this.onFailHandlers = in.readCollectionAsList(Continuation::new);
        this.finallyHandlers = in.readCollectionAsList(Continuation::new);
        this.trackName = in.readOptionalString();
        this.timeoutSeconds = in.readOptionalVInt();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeCollection(onDoneHandlers);
        out.writeCollection(onFailHandlers);
        out.writeCollection(finallyHandlers);
        out.writeOptionalString(trackName);
        out.writeOptionalVInt(timeoutSeconds);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        if (onDoneHandlers.isEmpty() == false) {
            builder.startArray("on_done");
            for (Continuation c : onDoneHandlers) {
                c.toXContent(builder, params);
            }
            builder.endArray();
        }
        if (onFailHandlers.isEmpty() == false) {
            builder.startArray("on_fail");
            for (Continuation c : onFailHandlers) {
                c.toXContent(builder, params);
            }
            builder.endArray();
        }
        if (finallyHandlers.isEmpty() == false) {
            builder.startArray("finally");
            for (Continuation c : finallyHandlers) {
                c.toXContent(builder, params);
            }
            builder.endArray();
        }
        if (trackName != null) {
            builder.field("track_name", trackName);
        }
        if (timeoutSeconds != null) {
            builder.field("timeout_seconds", timeoutSeconds);
        }
        builder.endObject();
        return builder;
    }

    public List<Continuation> getOnDoneHandlers() {
        return onDoneHandlers;
    }

    public List<Continuation> getOnFailHandlers() {
        return onFailHandlers;
    }

    public List<Continuation> getFinallyHandlers() {
        return finallyHandlers;
    }

    public Optional<String> getTrackName() {
        return Optional.ofNullable(trackName);
    }

    public Optional<Integer> getTimeoutSeconds() {
        return Optional.ofNullable(timeoutSeconds);
    }

    public boolean hasOnDone() {
        return onDoneHandlers.isEmpty() == false;
    }

    public boolean hasOnFail() {
        return onFailHandlers.isEmpty() == false;
    }

    public boolean hasFinally() {
        return finallyHandlers.isEmpty() == false;
    }

    public boolean isTracked() {
        return trackName != null;
    }

    public boolean hasTimeout() {
        return timeoutSeconds != null;
    }

    /**
     * Returns true if this is a "fire and forget" execution with no continuations.
     */
    public boolean isFireAndForget() {
        return onDoneHandlers.isEmpty() && onFailHandlers.isEmpty() && finallyHandlers.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutionPipeline that = (ExecutionPipeline) o;
        return Objects.equals(onDoneHandlers, that.onDoneHandlers)
            && Objects.equals(onFailHandlers, that.onFailHandlers)
            && Objects.equals(finallyHandlers, that.finallyHandlers)
            && Objects.equals(trackName, that.trackName)
            && Objects.equals(timeoutSeconds, that.timeoutSeconds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(onDoneHandlers, onFailHandlers, finallyHandlers, trackName, timeoutSeconds);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<Continuation> onDoneHandlers = new ArrayList<>();
        private final List<Continuation> onFailHandlers = new ArrayList<>();
        private final List<Continuation> finallyHandlers = new ArrayList<>();
        private String trackName;
        private Integer timeoutSeconds;

        public Builder addOnDone(Continuation continuation) {
            this.onDoneHandlers.add(continuation);
            return this;
        }

        public Builder addOnFail(Continuation continuation) {
            this.onFailHandlers.add(continuation);
            return this;
        }

        public Builder addFinally(Continuation continuation) {
            this.finallyHandlers.add(continuation);
            return this;
        }

        public Builder trackAs(String name) {
            this.trackName = name;
            return this;
        }

        public Builder timeout(int seconds) {
            this.timeoutSeconds = seconds;
            return this;
        }

        public ExecutionPipeline build() {
            return new ExecutionPipeline(this);
        }
    }
}

