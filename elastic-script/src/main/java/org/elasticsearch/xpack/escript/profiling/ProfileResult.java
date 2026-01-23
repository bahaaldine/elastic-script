/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.profiling;

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
 * Represents profiling results for a procedure/function execution.
 *
 * Captures timing information for:
 * - Total execution time
 * - Individual statement execution times
 * - Function call times
 * - ESQL query times
 */
public class ProfileResult implements ToXContent {

    public static final String INDEX_NAME = ".escript_profiles";

    private final String id;
    private final String procedureName;
    private final Instant startTime;
    private final Instant endTime;
    private final long totalDurationMs;
    private final List<StatementProfile> statements;
    private final List<FunctionCallProfile> functionCalls;
    private final List<EsqlQueryProfile> esqlQueries;
    private final Map<String, Object> metadata;

    private ProfileResult(Builder builder) {
        this.id = builder.id;
        this.procedureName = builder.procedureName;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.totalDurationMs = builder.totalDurationMs;
        this.statements = new ArrayList<>(builder.statements);
        this.functionCalls = new ArrayList<>(builder.functionCalls);
        this.esqlQueries = new ArrayList<>(builder.esqlQueries);
        this.metadata = new HashMap<>(builder.metadata);
    }

    public String getId() {
        return id;
    }

    public String getProcedureName() {
        return procedureName;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public long getTotalDurationMs() {
        return totalDurationMs;
    }

    public List<StatementProfile> getStatements() {
        return statements;
    }

    public List<FunctionCallProfile> getFunctionCalls() {
        return functionCalls;
    }

    public List<EsqlQueryProfile> getEsqlQueries() {
        return esqlQueries;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Get the slowest statements.
     */
    public List<StatementProfile> getSlowestStatements(int limit) {
        return statements.stream()
            .sorted((a, b) -> Long.compare(b.durationMs, a.durationMs))
            .limit(limit)
            .toList();
    }

    /**
     * Get the slowest function calls.
     */
    public List<FunctionCallProfile> getSlowestFunctions(int limit) {
        return functionCalls.stream()
            .sorted((a, b) -> Long.compare(b.durationMs, a.durationMs))
            .limit(limit)
            .toList();
    }

    /**
     * Get the slowest ESQL queries.
     */
    public List<EsqlQueryProfile> getSlowestQueries(int limit) {
        return esqlQueries.stream()
            .sorted((a, b) -> Long.compare(b.durationMs, a.durationMs))
            .limit(limit)
            .toList();
    }

    /**
     * Get time spent in ESQL queries.
     */
    public long getTotalEsqlTimeMs() {
        return esqlQueries.stream().mapToLong(q -> q.durationMs).sum();
    }

    /**
     * Get time spent in function calls.
     */
    public long getTotalFunctionTimeMs() {
        return functionCalls.stream().mapToLong(f -> f.durationMs).sum();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("id", id);
        builder.field("procedure_name", procedureName);
        builder.field("start_time", startTime.toString());
        builder.field("end_time", endTime.toString());
        builder.field("total_duration_ms", totalDurationMs);
        builder.field("total_esql_time_ms", getTotalEsqlTimeMs());
        builder.field("total_function_time_ms", getTotalFunctionTimeMs());
        builder.field("statement_count", statements.size());
        builder.field("function_call_count", functionCalls.size());
        builder.field("esql_query_count", esqlQueries.size());

        // Statements
        builder.startArray("statements");
        for (StatementProfile stmt : statements) {
            builder.startObject();
            builder.field("type", stmt.type);
            builder.field("line", stmt.lineNumber);
            builder.field("duration_ms", stmt.durationMs);
            builder.field("text", stmt.text);
            builder.endObject();
        }
        builder.endArray();

        // Function calls
        builder.startArray("function_calls");
        for (FunctionCallProfile func : functionCalls) {
            builder.startObject();
            builder.field("name", func.name);
            builder.field("duration_ms", func.durationMs);
            builder.field("call_count", func.callCount);
            builder.endObject();
        }
        builder.endArray();

        // ESQL queries
        builder.startArray("esql_queries");
        for (EsqlQueryProfile query : esqlQueries) {
            builder.startObject();
            builder.field("query", query.query);
            builder.field("duration_ms", query.durationMs);
            builder.field("row_count", query.rowCount);
            builder.endObject();
        }
        builder.endArray();

        // Metadata
        if (!metadata.isEmpty()) {
            builder.field("metadata", metadata);
        }

        builder.endObject();
        return builder;
    }

    @Override
    public String toString() {
        return "ProfileResult{procedure='" + procedureName + "', totalMs=" + totalDurationMs +
               ", statements=" + statements.size() + ", functions=" + functionCalls.size() +
               ", esqlQueries=" + esqlQueries.size() + "}";
    }

    // ==================== Inner Classes ====================

    /**
     * Profile for a single statement.
     */
    public static class StatementProfile {
        public final String type;
        public final int lineNumber;
        public final long durationMs;
        public final String text;

        public StatementProfile(String type, int lineNumber, long durationMs, String text) {
            this.type = type;
            this.lineNumber = lineNumber;
            this.durationMs = durationMs;
            this.text = text;
        }
    }

    /**
     * Profile for a function call.
     */
    public static class FunctionCallProfile {
        public final String name;
        public final long durationMs;
        public final int callCount;

        public FunctionCallProfile(String name, long durationMs, int callCount) {
            this.name = name;
            this.durationMs = durationMs;
            this.callCount = callCount;
        }
    }

    /**
     * Profile for an ESQL query.
     */
    public static class EsqlQueryProfile {
        public final String query;
        public final long durationMs;
        public final int rowCount;

        public EsqlQueryProfile(String query, long durationMs, int rowCount) {
            this.query = query;
            this.durationMs = durationMs;
            this.rowCount = rowCount;
        }
    }

    // ==================== Builder ====================

    public static class Builder {
        private String id;
        private String procedureName;
        private Instant startTime;
        private Instant endTime;
        private long totalDurationMs;
        private List<StatementProfile> statements = new ArrayList<>();
        private List<FunctionCallProfile> functionCalls = new ArrayList<>();
        private List<EsqlQueryProfile> esqlQueries = new ArrayList<>();
        private Map<String, Object> metadata = new HashMap<>();

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder procedureName(String procedureName) {
            this.procedureName = procedureName;
            return this;
        }

        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(Instant endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder totalDurationMs(long totalDurationMs) {
            this.totalDurationMs = totalDurationMs;
            return this;
        }

        public Builder addStatement(StatementProfile statement) {
            this.statements.add(statement);
            return this;
        }

        public Builder addFunctionCall(FunctionCallProfile functionCall) {
            this.functionCalls.add(functionCall);
            return this;
        }

        public Builder addEsqlQuery(EsqlQueryProfile query) {
            this.esqlQueries.add(query);
            return this;
        }

        public Builder metadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public ProfileResult build() {
            if (id == null) {
                id = procedureName + "_" + Instant.now().toEpochMilli();
            }
            return new ProfileResult(this);
        }
    }
}
