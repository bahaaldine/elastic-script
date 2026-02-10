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
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for SHOW PROCEDURES and SHOW FUNCTIONS statements.
 * 
 * Syntax:
 * - SHOW PROCEDURES - lists all stored procedures
 * - SHOW PROCEDURE name - shows details about a specific procedure
 * - SHOW FUNCTIONS - lists all stored functions
 * - SHOW FUNCTION name - shows details about a specific function
 */
public class ShowStatementHandler {

    private static final Logger LOGGER = LogManager.getLogger(ShowStatementHandler.class);

    private static final String PROCEDURES_INDEX = ".elastic_script_procedures";
    private static final String FUNCTIONS_INDEX = ".elastic_script_functions";

    private final Client client;

    public ShowStatementHandler(Client client) {
        this.client = client;
    }

    /**
     * Handle SHOW PROCEDURES - lists all stored procedures.
     */
    public void handleShowAllProcedures(ActionListener<Object> listener) {
        LOGGER.debug("Listing all stored procedures");

        SearchRequest searchRequest = new SearchRequest(PROCEDURES_INDEX);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchAllQuery());
        sourceBuilder.size(1000); // Reasonable limit
        sourceBuilder.fetchSource(new String[]{"parameters"}, null);
        searchRequest.source(sourceBuilder);

        client.search(searchRequest, new ActionListener<>() {
            @Override
            public void onResponse(org.elasticsearch.action.search.SearchResponse response) {
                try {
                    List<Map<String, Object>> procedureList = new ArrayList<>();

                    for (SearchHit hit : response.getHits().getHits()) {
                        Map<String, Object> procInfo = new HashMap<>();
                        procInfo.put("name", hit.getId());

                        Map<String, Object> source = hit.getSourceAsMap();
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> params = (List<Map<String, Object>>) source.get("parameters");

                        procInfo.put("parameter_count", params != null ? params.size() : 0);

                        // Build signature
                        StringBuilder sig = new StringBuilder();
                        sig.append(hit.getId()).append("(");
                        if (params != null && !params.isEmpty()) {
                            for (int i = 0; i < params.size(); i++) {
                                if (i > 0) sig.append(", ");
                                Map<String, Object> p = params.get(i);
                                sig.append(p.get("name")).append(" ").append(p.get("type"));
                            }
                        }
                        sig.append(")");
                        procInfo.put("signature", sig.toString());

                        procedureList.add(procInfo);
                    }

                    // Sort by name
                    procedureList.sort((a, b) -> ((String) a.get("name")).compareTo((String) b.get("name")));

                    Map<String, Object> result = new HashMap<>();
                    result.put("action", "SHOW PROCEDURES");
                    result.put("count", procedureList.size());
                    result.put("procedures", procedureList);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            }

            @Override
            public void onFailure(Exception e) {
                // If index doesn't exist, return empty list
                String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                if (msg.contains("index_not_found") || msg.contains("no such index") || 
                    e instanceof org.elasticsearch.index.IndexNotFoundException) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("action", "SHOW PROCEDURES");
                    result.put("count", 0);
                    result.put("procedures", List.of());
                    listener.onResponse(result);
                } else {
                    listener.onFailure(e);
                }
            }
        });
    }

    /**
     * Handle SHOW PROCEDURE name - shows details about a specific procedure.
     */
    public void handleShowProcedureDetail(ElasticScriptParser.ShowProcedureDetailContext ctx,
                                          ActionListener<Object> listener) {
        String procedureName = ctx.ID().getText();
        LOGGER.debug("Showing details for procedure: {}", procedureName);

        GetRequest getRequest = new GetRequest(PROCEDURES_INDEX, procedureName);

        client.get(getRequest, new ActionListener<>() {
            @Override
            public void onResponse(org.elasticsearch.action.get.GetResponse response) {
                Map<String, Object> result = new HashMap<>();
                result.put("action", "SHOW PROCEDURE");
                result.put("name", procedureName);

                if (!response.isExists()) {
                    result.put("exists", false);
                    listener.onResponse(result);
                    return;
                }

                result.put("exists", true);

                Map<String, Object> source = response.getSourceAsMap();

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> params = (List<Map<String, Object>>) source.get("parameters");
                result.put("parameter_count", params != null ? params.size() : 0);
                result.put("parameters", params != null ? params : List.of());

                // Include the procedure source code
                result.put("source", source.get("procedure"));

                // Build signature
                StringBuilder sig = new StringBuilder();
                sig.append(procedureName).append("(");
                if (params != null && !params.isEmpty()) {
                    for (int i = 0; i < params.size(); i++) {
                        if (i > 0) sig.append(", ");
                        Map<String, Object> p = params.get(i);
                        sig.append(p.get("name")).append(" ").append(p.get("type"));
                    }
                }
                sig.append(")");
                result.put("signature", sig.toString());

                listener.onResponse(result);
            }

            @Override
            public void onFailure(Exception e) {
                // If index doesn't exist, procedure doesn't exist
                String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                if (msg.contains("index_not_found") || msg.contains("no such index") || 
                    e instanceof org.elasticsearch.index.IndexNotFoundException) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("action", "SHOW PROCEDURE");
                    result.put("name", procedureName);
                    result.put("exists", false);
                    listener.onResponse(result);
                } else {
                    listener.onFailure(e);
                }
            }
        });
    }

    /**
     * Handle SHOW FUNCTIONS - lists all stored functions.
     */
    public void handleShowAllFunctions(ActionListener<Object> listener) {
        LOGGER.debug("Listing all stored functions");

        SearchRequest searchRequest = new SearchRequest(FUNCTIONS_INDEX);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchAllQuery());
        sourceBuilder.size(1000); // Reasonable limit
        sourceBuilder.fetchSource(new String[]{"parameters", "return_type"}, null);
        searchRequest.source(sourceBuilder);

        client.search(searchRequest, new ActionListener<>() {
            @Override
            public void onResponse(org.elasticsearch.action.search.SearchResponse response) {
                try {
                    List<Map<String, Object>> functionList = new ArrayList<>();

                    for (SearchHit hit : response.getHits().getHits()) {
                        Map<String, Object> funcInfo = new HashMap<>();
                        funcInfo.put("name", hit.getId());

                        Map<String, Object> source = hit.getSourceAsMap();
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> params = (List<Map<String, Object>>) source.get("parameters");
                        String returnType = (String) source.get("return_type");

                        funcInfo.put("parameter_count", params != null ? params.size() : 0);
                        funcInfo.put("return_type", returnType != null ? returnType : "UNKNOWN");

                        // Build signature
                        StringBuilder sig = new StringBuilder();
                        sig.append(hit.getId()).append("(");
                        if (params != null && !params.isEmpty()) {
                            for (int i = 0; i < params.size(); i++) {
                                if (i > 0) sig.append(", ");
                                Map<String, Object> p = params.get(i);
                                sig.append(p.get("name")).append(" ").append(p.get("type"));
                            }
                        }
                        sig.append(") RETURNS ");
                        sig.append(returnType != null ? returnType : "UNKNOWN");
                        funcInfo.put("signature", sig.toString());

                        functionList.add(funcInfo);
                    }

                    // Sort by name
                    functionList.sort((a, b) -> ((String) a.get("name")).compareTo((String) b.get("name")));

                    Map<String, Object> result = new HashMap<>();
                    result.put("action", "SHOW FUNCTIONS");
                    result.put("count", functionList.size());
                    result.put("functions", functionList);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            }

            @Override
            public void onFailure(Exception e) {
                // If index doesn't exist, return empty list
                String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                if (msg.contains("index_not_found") || msg.contains("no such index") || 
                    e instanceof org.elasticsearch.index.IndexNotFoundException) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("action", "SHOW FUNCTIONS");
                    result.put("count", 0);
                    result.put("functions", List.of());
                    listener.onResponse(result);
                } else {
                    listener.onFailure(e);
                }
            }
        });
    }

    /**
     * Handle SHOW FUNCTION name - shows details about a specific function.
     */
    public void handleShowFunctionDetail(ElasticScriptParser.ShowFunctionDetailContext ctx,
                                         ActionListener<Object> listener) {
        String functionName = ctx.ID().getText();
        LOGGER.debug("Showing details for function: {}", functionName);

        GetRequest getRequest = new GetRequest(FUNCTIONS_INDEX, functionName);

        client.get(getRequest, new ActionListener<>() {
            @Override
            public void onResponse(org.elasticsearch.action.get.GetResponse response) {
                Map<String, Object> result = new HashMap<>();
                result.put("action", "SHOW FUNCTION");
                result.put("name", functionName);

                if (!response.isExists()) {
                    result.put("exists", false);
                    listener.onResponse(result);
                    return;
                }

                result.put("exists", true);

                Map<String, Object> source = response.getSourceAsMap();

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> params = (List<Map<String, Object>>) source.get("parameters");
                String returnType = (String) source.get("return_type");
                
                result.put("parameter_count", params != null ? params.size() : 0);
                result.put("parameters", params != null ? params : List.of());
                result.put("return_type", returnType != null ? returnType : "UNKNOWN");

                // Include the function source code
                result.put("source", source.get("function"));

                // Build signature
                StringBuilder sig = new StringBuilder();
                sig.append(functionName).append("(");
                if (params != null && !params.isEmpty()) {
                    for (int i = 0; i < params.size(); i++) {
                        if (i > 0) sig.append(", ");
                        Map<String, Object> p = params.get(i);
                        sig.append(p.get("name")).append(" ").append(p.get("type"));
                    }
                }
                sig.append(") RETURNS ");
                sig.append(returnType != null ? returnType : "UNKNOWN");
                result.put("signature", sig.toString());

                listener.onResponse(result);
            }

            @Override
            public void onFailure(Exception e) {
                // If index doesn't exist, function doesn't exist
                String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                if (msg.contains("index_not_found") || msg.contains("no such index") || 
                    e instanceof org.elasticsearch.index.IndexNotFoundException) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("action", "SHOW FUNCTION");
                    result.put("name", functionName);
                    result.put("exists", false);
                    listener.onResponse(result);
                } else {
                    listener.onFailure(e);
                }
            }
        });
    }
}
