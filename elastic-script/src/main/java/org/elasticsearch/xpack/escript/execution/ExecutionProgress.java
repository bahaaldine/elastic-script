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
import java.util.Objects;

/**
 * Progress information for an ongoing execution.
 */
public class ExecutionProgress implements Writeable, ToXContentObject {

    private final int percent;
    private final String message;

    public ExecutionProgress(int percent, String message) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("percent must be between 0 and 100");
        }
        this.percent = percent;
        this.message = message;
    }

    public ExecutionProgress(StreamInput in) throws IOException {
        this.percent = in.readVInt();
        this.message = in.readOptionalString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeVInt(percent);
        out.writeOptionalString(message);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("percent", percent);
        if (message != null) {
            builder.field("message", message);
        }
        builder.endObject();
        return builder;
    }

    public int getPercent() {
        return percent;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutionProgress that = (ExecutionProgress) o;
        return percent == that.percent && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(percent, message);
    }

    @Override
    public String toString() {
        return percent + "% - " + (message != null ? message : "");
    }

    /**
     * Creates a progress at 0%.
     */
    public static ExecutionProgress starting() {
        return new ExecutionProgress(0, "Starting...");
    }

    /**
     * Creates a progress at 100%.
     */
    public static ExecutionProgress complete() {
        return new ExecutionProgress(100, "Complete");
    }
}


