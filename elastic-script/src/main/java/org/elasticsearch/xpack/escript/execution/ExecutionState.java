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
import org.elasticsearch.xcontent.ConstructingObjectParser;
import org.elasticsearch.xcontent.ParseField;
import org.elasticsearch.xcontent.ToXContentObject;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentParser;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.elasticsearch.xcontent.ConstructingObjectParser.constructorArg;
import static org.elasticsearch.xcontent.ConstructingObjectParser.optionalConstructorArg;

/**
 * Represents the complete state of an async procedure execution.
 * <p>
 * This is what gets stored in the .escript_executions index.
 * </p>
 * <p>
 * Example document:
 * <pre>
 * {
 *   "execution_id": "exec-abc123",
 *   "name": "daily-analysis",
 *   "procedure": "analyze_logs",
 *   "parameters": { "index": "logs-*" },
 *   "status": "RUNNING",
 *   "progress": { "percent": 45, "message": "Processing..." },
 *   "pipeline": { ... },
 *   "started_at": "2025-01-06T10:00:00Z",
 *   "completed_at": null,
 *   "node": "node-1",
 *   "result": null,
 *   "error": null
 * }
 * </pre>
 */
public class ExecutionState implements Writeable, ToXContentObject {

    public static final String INDEX_NAME = ".escript_executions";

    // Fields
    private final String executionId;
    private final String name; // Optional: from TRACK AS
    private final String procedure;
    private final Map<String, Object> parameters;
    private final ExecutionStatus status;
    private final ExecutionProgress progress;
    private final ExecutionPipeline pipeline;
    private final Instant startedAt;
    private final Instant completedAt;
    private final String node;
    private final Object result;
    private final String error;

    private ExecutionState(Builder builder) {
        this.executionId = Objects.requireNonNull(builder.executionId, "executionId is required");
        this.procedure = Objects.requireNonNull(builder.procedure, "procedure is required");
        this.status = Objects.requireNonNull(builder.status, "status is required");
        this.name = builder.name;
        this.parameters = builder.parameters != null ? Map.copyOf(builder.parameters) : Map.of();
        this.progress = builder.progress;
        this.pipeline = builder.pipeline;
        this.startedAt = builder.startedAt;
        this.completedAt = builder.completedAt;
        this.node = builder.node;
        this.result = builder.result;
        this.error = builder.error;
    }

    /**
     * Read from stream.
     */
    @SuppressWarnings("unchecked")
    public ExecutionState(StreamInput in) throws IOException {
        this.executionId = in.readString();
        this.name = in.readOptionalString();
        this.procedure = in.readString();
        this.parameters = in.readGenericMap();
        this.status = in.readEnum(ExecutionStatus.class);
        this.progress = in.readOptionalWriteable(ExecutionProgress::new);
        this.pipeline = in.readOptionalWriteable(ExecutionPipeline::new);
        this.startedAt = in.readOptionalInstant();
        this.completedAt = in.readOptionalInstant();
        this.node = in.readOptionalString();
        this.result = in.readGenericValue();
        this.error = in.readOptionalString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(executionId);
        out.writeOptionalString(name);
        out.writeString(procedure);
        out.writeGenericMap(parameters);
        out.writeEnum(status);
        out.writeOptionalWriteable(progress);
        out.writeOptionalWriteable(pipeline);
        out.writeOptionalInstant(startedAt);
        out.writeOptionalInstant(completedAt);
        out.writeOptionalString(node);
        out.writeGenericValue(result);
        out.writeOptionalString(error);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("execution_id", executionId);
        if (name != null) {
            builder.field("name", name);
        }
        builder.field("procedure", procedure);
        if (parameters.isEmpty() == false) {
            builder.field("parameters", parameters);
        }
        builder.field("status", status.name());
        if (progress != null) {
            builder.field("progress", progress);
        }
        if (pipeline != null) {
            builder.field("pipeline", pipeline);
        }
        if (startedAt != null) {
            builder.field("started_at", startedAt.toString());
        }
        if (completedAt != null) {
            builder.field("completed_at", completedAt.toString());
        }
        if (node != null) {
            builder.field("node", node);
        }
        if (result != null) {
            builder.field("result", result);
        }
        if (error != null) {
            builder.field("error", error);
        }
        builder.endObject();
        return builder;
    }

    // Getters
    public String getExecutionId() {
        return executionId;
    }

    public String getName() {
        return name;
    }

    public String getProcedure() {
        return procedure;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public ExecutionProgress getProgress() {
        return progress;
    }

    public ExecutionPipeline getPipeline() {
        return pipeline;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getNode() {
        return node;
    }

    public Object getResult() {
        return result;
    }

    public String getError() {
        return error;
    }

    /**
     * Returns the effective name (either explicit name or execution ID).
     */
    public String getEffectiveName() {
        return name != null ? name : executionId;
    }

    /**
     * Returns true if this execution has completed (successfully or not).
     */
    public boolean isTerminal() {
        return status.isTerminal();
    }

    /**
     * Creates a new state with updated status.
     */
    public ExecutionState withStatus(ExecutionStatus newStatus) {
        return toBuilder().status(newStatus).build();
    }

    /**
     * Creates a new state marking completion with result.
     */
    public ExecutionState withCompletion(Object result) {
        return toBuilder()
            .status(ExecutionStatus.COMPLETED)
            .result(result)
            .completedAt(Instant.now())
            .build();
    }

    /**
     * Creates a new state marking failure with error.
     */
    public ExecutionState withFailure(String error) {
        return toBuilder()
            .status(ExecutionStatus.FAILED)
            .error(error)
            .completedAt(Instant.now())
            .build();
    }

    /**
     * Creates a new state with updated progress.
     */
    public ExecutionState withProgress(int percent, String message) {
        return toBuilder()
            .progress(new ExecutionProgress(percent, message))
            .build();
    }

    public Builder toBuilder() {
        return new Builder()
            .executionId(executionId)
            .name(name)
            .procedure(procedure)
            .parameters(parameters)
            .status(status)
            .progress(progress)
            .pipeline(pipeline)
            .startedAt(startedAt)
            .completedAt(completedAt)
            .node(node)
            .result(result)
            .error(error);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutionState that = (ExecutionState) o;
        return Objects.equals(executionId, that.executionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(executionId);
    }

    @Override
    public String toString() {
        return "ExecutionState{" +
            "executionId='" + executionId + '\'' +
            ", name='" + name + '\'' +
            ", procedure='" + procedure + '\'' +
            ", status=" + status +
            '}';
    }

    public static class Builder {
        private String executionId;
        private String name;
        private String procedure;
        private Map<String, Object> parameters;
        private ExecutionStatus status = ExecutionStatus.PENDING;
        private ExecutionProgress progress;
        private ExecutionPipeline pipeline;
        private Instant startedAt;
        private Instant completedAt;
        private String node;
        private Object result;
        private String error;

        public Builder executionId(String executionId) {
            this.executionId = executionId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder procedure(String procedure) {
            this.procedure = procedure;
            return this;
        }

        public Builder parameters(Map<String, Object> parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder status(ExecutionStatus status) {
            this.status = status;
            return this;
        }

        public Builder progress(ExecutionProgress progress) {
            this.progress = progress;
            return this;
        }

        public Builder pipeline(ExecutionPipeline pipeline) {
            this.pipeline = pipeline;
            return this;
        }

        public Builder startedAt(Instant startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public Builder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public Builder node(String node) {
            this.node = node;
            return this;
        }

        public Builder result(Object result) {
            this.result = result;
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public ExecutionState build() {
            return new ExecutionState(this);
        }
    }
}


