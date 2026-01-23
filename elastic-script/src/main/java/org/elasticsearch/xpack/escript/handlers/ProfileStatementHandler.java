/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;
import org.elasticsearch.xpack.escript.profiling.ProfileResult;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for profiler statements: PROFILE, SHOW PROFILE, CLEAR PROFILE, ANALYZE PROFILE.
 */
public class ProfileStatementHandler {

    private static final Logger LOGGER = LogManager.getLogger(ProfileStatementHandler.class);
    private static final String INDEX_NAME = ProfileResult.INDEX_NAME;

    private final Client client;

    // In-memory storage for the last profile result
    private static volatile ProfileResult lastProfile;

    public ProfileStatementHandler(Client client) {
        this.client = client;
    }

    /**
     * Store a profile result.
     */
    public void storeProfile(ProfileResult profile, ActionListener<Object> listener) {
        lastProfile = profile;

        ensureIndexExists(ActionListener.wrap(
            indexExists -> {
                try {
                    XContentBuilder builder = XContentFactory.jsonBuilder();
                    profile.toXContent(builder, null);

                    IndexRequest request = new IndexRequest(INDEX_NAME)
                        .id(profile.getId())
                        .source(builder);

                    client.index(request, ActionListener.wrap(
                        response -> {
                            Map<String, Object> result = new HashMap<>();
                            result.put("acknowledged", true);
                            result.put("profile_id", profile.getId());
                            result.put("procedure", profile.getProcedureName());
                            result.put("total_duration_ms", profile.getTotalDurationMs());
                            result.put("statement_count", profile.getStatements().size());
                            result.put("function_call_count", profile.getFunctionCalls().size());
                            result.put("esql_query_count", profile.getEsqlQueries().size());
                            listener.onResponse(result);
                        },
                        listener::onFailure
                    ));
                } catch (IOException e) {
                    listener.onFailure(e);
                }
            },
            listener::onFailure
        ));
    }

    /**
     * Handle SHOW PROFILES - show all stored profiles.
     */
    public void handleShowAllProfiles(ActionListener<Object> listener) {
        SearchRequest request = new SearchRequest(INDEX_NAME);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchAllQuery());
        sourceBuilder.size(100);
        sourceBuilder.sort("start_time", SortOrder.DESC);
        request.source(sourceBuilder);

        client.search(request, ActionListener.wrap(
            response -> {
                List<Map<String, Object>> profiles = new ArrayList<>();
                for (SearchHit hit : response.getHits().getHits()) {
                    Map<String, Object> profile = new HashMap<>(hit.getSourceAsMap());
                    // Add summary info
                    profile.put("_id", hit.getId());
                    profiles.add(profile);
                }
                listener.onResponse(profiles);
            },
            e -> {
                if (e.getCause() instanceof IndexNotFoundException) {
                    listener.onResponse(new ArrayList<>());
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }

    /**
     * Handle SHOW PROFILE - show the last profile.
     */
    public void handleShowLastProfile(ActionListener<Object> listener) {
        if (lastProfile != null) {
            listener.onResponse(profileToMap(lastProfile));
            return;
        }

        // If no in-memory profile, get the most recent from the index
        SearchRequest request = new SearchRequest(INDEX_NAME);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchAllQuery());
        sourceBuilder.size(1);
        sourceBuilder.sort("start_time", SortOrder.DESC);
        request.source(sourceBuilder);

        client.search(request, ActionListener.wrap(
            response -> {
                if (response.getHits().getHits().length > 0) {
                    listener.onResponse(response.getHits().getHits()[0].getSourceAsMap());
                } else {
                    listener.onFailure(new IllegalStateException("No profile data available. Run PROFILE CALL to collect profiling data."));
                }
            },
            e -> {
                if (e.getCause() instanceof IndexNotFoundException) {
                    listener.onFailure(new IllegalStateException("No profile data available. Run PROFILE CALL to collect profiling data."));
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }

    /**
     * Handle SHOW PROFILE FOR procedure_name.
     */
    public void handleShowProfileFor(ElasticScriptParser.ShowProfileForContext ctx,
                                     ActionListener<Object> listener) {
        String procedureName = ctx.ID().getText();

        SearchRequest request = new SearchRequest(INDEX_NAME);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.termQuery("procedure_name", procedureName));
        sourceBuilder.size(1);
        sourceBuilder.sort("start_time", SortOrder.DESC);
        request.source(sourceBuilder);

        client.search(request, ActionListener.wrap(
            response -> {
                if (response.getHits().getHits().length > 0) {
                    listener.onResponse(response.getHits().getHits()[0].getSourceAsMap());
                } else {
                    listener.onFailure(new IllegalArgumentException("No profile found for procedure: " + procedureName));
                }
            },
            e -> {
                if (e.getCause() instanceof IndexNotFoundException) {
                    listener.onFailure(new IllegalArgumentException("No profile found for procedure: " + procedureName));
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }

    /**
     * Handle CLEAR PROFILES - clear all profiles by deleting and recreating the index.
     */
    public void handleClearAllProfiles(ActionListener<Object> listener) {
        lastProfile = null;

        // Delete the index to clear all profiles
        DeleteIndexRequest request = new DeleteIndexRequest(INDEX_NAME);
        client.admin().indices().delete(request, ActionListener.wrap(
            response -> {
                Map<String, Object> result = new HashMap<>();
                result.put("acknowledged", true);
                result.put("action", "all profiles cleared");
                listener.onResponse(result);
            },
            e -> {
                if (e instanceof IndexNotFoundException) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("acknowledged", true);
                    result.put("action", "no profiles to clear");
                    listener.onResponse(result);
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }

    /**
     * Handle CLEAR PROFILE FOR procedure_name.
     * Note: This is a simplified implementation that just acknowledges the request.
     * Full implementation would use delete-by-query.
     */
    public void handleClearProfileFor(ElasticScriptParser.ClearProfileForContext ctx,
                                      ActionListener<Object> listener) {
        String procedureName = ctx.ID().getText();

        // For now, just acknowledge - full implementation would use delete-by-query
        Map<String, Object> result = new HashMap<>();
        result.put("acknowledged", true);
        result.put("procedure", procedureName);
        result.put("message", "Profiles for " + procedureName + " will be cleared on next CLEAR PROFILES");
        listener.onResponse(result);
    }

    /**
     * Handle ANALYZE PROFILE - provide recommendations.
     */
    public void handleAnalyzeLastProfile(ActionListener<Object> listener) {
        if (lastProfile == null) {
            listener.onFailure(new IllegalStateException("No profile data available. Run PROFILE CALL first."));
            return;
        }

        listener.onResponse(analyzeProfile(lastProfile));
    }

    /**
     * Handle ANALYZE PROFILE FOR procedure_name.
     */
    public void handleAnalyzeProfileFor(ElasticScriptParser.AnalyzeProfileForContext ctx,
                                        ActionListener<Object> listener) {
        String procedureName = ctx.ID().getText();

        SearchRequest request = new SearchRequest(INDEX_NAME);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.termQuery("procedure_name", procedureName));
        sourceBuilder.size(1);
        sourceBuilder.sort("start_time", SortOrder.DESC);
        request.source(sourceBuilder);

        client.search(request, ActionListener.wrap(
            response -> {
                if (response.getHits().getHits().length > 0) {
                    Map<String, Object> profileData = response.getHits().getHits()[0].getSourceAsMap();
                    listener.onResponse(analyzeProfileData(profileData));
                } else {
                    listener.onFailure(new IllegalArgumentException("No profile found for procedure: " + procedureName));
                }
            },
            e -> listener.onFailure(e)
        ));
    }

    // ==================== Helper Methods ====================

    private void ensureIndexExists(ActionListener<Boolean> listener) {
        client.admin().indices().prepareGetIndex(TimeValue.timeValueSeconds(30)).setIndices(INDEX_NAME).execute(ActionListener.wrap(
            response -> listener.onResponse(true),
            e -> {
                if (e instanceof IndexNotFoundException) {
                    CreateIndexRequest createRequest = new CreateIndexRequest(INDEX_NAME);
                    client.admin().indices().create(createRequest, ActionListener.wrap(
                        response -> listener.onResponse(true),
                        listener::onFailure
                    ));
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }

    private Map<String, Object> profileToMap(ProfileResult profile) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", profile.getId());
        map.put("procedure_name", profile.getProcedureName());
        map.put("start_time", profile.getStartTime().toString());
        map.put("end_time", profile.getEndTime().toString());
        map.put("total_duration_ms", profile.getTotalDurationMs());
        map.put("total_esql_time_ms", profile.getTotalEsqlTimeMs());
        map.put("total_function_time_ms", profile.getTotalFunctionTimeMs());
        map.put("statement_count", profile.getStatements().size());
        map.put("function_call_count", profile.getFunctionCalls().size());
        map.put("esql_query_count", profile.getEsqlQueries().size());

        // Top 5 slowest statements
        List<Map<String, Object>> slowestStmts = new ArrayList<>();
        for (ProfileResult.StatementProfile stmt : profile.getSlowestStatements(5)) {
            Map<String, Object> s = new HashMap<>();
            s.put("type", stmt.type);
            s.put("line", stmt.lineNumber);
            s.put("duration_ms", stmt.durationMs);
            s.put("text", stmt.text);
            slowestStmts.add(s);
        }
        map.put("slowest_statements", slowestStmts);

        // Top 5 slowest queries
        List<Map<String, Object>> slowestQueries = new ArrayList<>();
        for (ProfileResult.EsqlQueryProfile query : profile.getSlowestQueries(5)) {
            Map<String, Object> q = new HashMap<>();
            q.put("query", query.query);
            q.put("duration_ms", query.durationMs);
            q.put("row_count", query.rowCount);
            slowestQueries.add(q);
        }
        map.put("slowest_esql_queries", slowestQueries);

        return map;
    }

    private Map<String, Object> analyzeProfile(ProfileResult profile) {
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("procedure_name", profile.getProcedureName());
        analysis.put("total_duration_ms", profile.getTotalDurationMs());

        // Time breakdown
        long esqlTime = profile.getTotalEsqlTimeMs();
        long funcTime = profile.getTotalFunctionTimeMs();
        long otherTime = profile.getTotalDurationMs() - esqlTime - funcTime;

        Map<String, Object> breakdown = new HashMap<>();
        breakdown.put("esql_time_ms", esqlTime);
        breakdown.put("esql_time_percent", profile.getTotalDurationMs() > 0 ? (esqlTime * 100.0 / profile.getTotalDurationMs()) : 0);
        breakdown.put("function_time_ms", funcTime);
        breakdown.put("function_time_percent", profile.getTotalDurationMs() > 0 ? (funcTime * 100.0 / profile.getTotalDurationMs()) : 0);
        breakdown.put("other_time_ms", otherTime);
        breakdown.put("other_time_percent", profile.getTotalDurationMs() > 0 ? (otherTime * 100.0 / profile.getTotalDurationMs()) : 0);
        analysis.put("time_breakdown", breakdown);

        // Recommendations
        List<String> recommendations = new ArrayList<>();

        if (esqlTime > profile.getTotalDurationMs() * 0.7) {
            recommendations.add("ESQL queries account for >70% of execution time. Consider optimizing queries or adding indices.");
        }

        if (profile.getEsqlQueries().size() > 10) {
            recommendations.add("High number of ESQL queries (" + profile.getEsqlQueries().size() + "). Consider batching or using BULK COLLECT.");
        }

        for (ProfileResult.EsqlQueryProfile query : profile.getSlowestQueries(3)) {
            if (query.durationMs > 1000) {
                recommendations.add("Slow query (>" + query.durationMs + "ms): " + truncate(query.query, 100));
            }
        }

        for (ProfileResult.StatementProfile stmt : profile.getSlowestStatements(3)) {
            if (stmt.durationMs > 500) {
                recommendations.add("Slow statement at line " + stmt.lineNumber + " (" + stmt.durationMs + "ms): " + stmt.type);
            }
        }

        if (recommendations.isEmpty()) {
            recommendations.add("No significant performance issues detected.");
        }

        analysis.put("recommendations", recommendations);

        return analysis;
    }

    private Map<String, Object> analyzeProfileData(Map<String, Object> profileData) {
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("procedure_name", profileData.get("procedure_name"));
        analysis.put("total_duration_ms", profileData.get("total_duration_ms"));

        long totalMs = ((Number) profileData.get("total_duration_ms")).longValue();
        long esqlMs = profileData.containsKey("total_esql_time_ms") ?
            ((Number) profileData.get("total_esql_time_ms")).longValue() : 0;
        long funcMs = profileData.containsKey("total_function_time_ms") ?
            ((Number) profileData.get("total_function_time_ms")).longValue() : 0;

        Map<String, Object> breakdown = new HashMap<>();
        breakdown.put("esql_time_ms", esqlMs);
        breakdown.put("esql_time_percent", totalMs > 0 ? (esqlMs * 100.0 / totalMs) : 0);
        breakdown.put("function_time_ms", funcMs);
        breakdown.put("function_time_percent", totalMs > 0 ? (funcMs * 100.0 / totalMs) : 0);
        analysis.put("time_breakdown", breakdown);

        List<String> recommendations = new ArrayList<>();
        if (esqlMs > totalMs * 0.7) {
            recommendations.add("ESQL queries account for >70% of execution time. Consider optimizing queries.");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("No significant performance issues detected.");
        }
        analysis.put("recommendations", recommendations);

        return analysis;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }

    /**
     * Get the last profile result (for use by other components).
     */
    public static ProfileResult getLastProfile() {
        return lastProfile;
    }

    /**
     * Set the last profile result.
     */
    public static void setLastProfile(ProfileResult profile) {
        lastProfile = profile;
    }
}
