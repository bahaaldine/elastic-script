/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.builtin.datasources;

import org.elasticsearch.xpack.escript.functions.api.FunctionSpec;
import org.elasticsearch.xpack.escript.functions.api.FunctionParam;
import org.elasticsearch.xpack.escript.functions.api.FunctionReturn;
import org.elasticsearch.xpack.escript.functions.api.FunctionCategory;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionType;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xpack.core.esql.action.ColumnInfo;
import org.elasticsearch.xpack.core.esql.action.EsqlQueryRequest;
import org.elasticsearch.xpack.core.esql.action.EsqlQueryResponse;
import org.elasticsearch.xpack.core.esql.action.EsqlQueryRequestBuilder;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.logging.LogManager;
import org.elasticsearch.logging.Logger;
import org.elasticsearch.xpack.escript.utils.ActionListenerUtils;

/**
 * Provides the ESQL_QUERY built-in function for executing raw ESQL query strings.
 *
 * The {@code ESQL_QUERY} function executes a raw ESQL query string and returns a list of normalized documents.
 * It supports variable substitution using the {@code :var} syntax, where variables defined in the current
 * {@link org.elasticsearch.xpack.escript.context.ExecutionContext} are replaced in the query before execution.
 * The function returns a list of documents, where each document represents a result row and maps column names to values.
 * Nested structures (such as maps or lists) in the result are converted to JSON strings for normalization.
 *
 * Example usages:
 * <pre>
 *   ESQL_QUERY('FROM games | LIMIT 10')
 *   ESQL_QUERY('FROM purchases | WHERE user_id == :userId')
 * </pre>
 *
 * Parameters:
 * <ul>
 *   <li>{@code query} (STRING): The raw ESQL query string to execute. Variables in the form of {@code :varName}
 *   will be replaced using the current execution context.</li>
 * </ul>
 *
 * Returns:
 * <ul>
 *   <li>ARRAY OF DOCUMENT: A list of documents representing the result rows, with each document mapping column names to values.</li>
 * </ul>
 */
@FunctionSpec(
    name = "ESQL_QUERY",
    description = "Executes a raw ESQL query string and returns a list of normalized documents. " +
        "Supports variable substitution using :var syntax.",
    parameters = {
        @FunctionParam(name = "query", type = "STRING", description = "The raw ESQL query string to execute. " +
            "Variables in the form of :varName will be replaced using the current execution context.")
    },
    returnType = @FunctionReturn(
        type = "ARRAY OF DOCUMENT",
        description = "A list of documents representing the result rows, with each document mapping column names to values."
    ),
    examples = {
        "ESQL_QUERY('FROM games | LIMIT 10')",
        "ESQL_QUERY('FROM purchases | WHERE user_id == :userId')"
    },
    category = FunctionCategory.DATASOURCE
)
public class EsqlBuiltInFunctions {

    private static final Logger LOGGER = LogManager.getLogger(EsqlBuiltInFunctions.class);

    /**
     * Registers the ESQL_QUERY built-in function on the provided ExecutionContext.
     * It assumes the ExecutionContext can provide a ProcedureExecutor and Client via helper methods.
     * The function takes one STRING argument (the query), executes it asynchronously,
     * and returns the normalized result (i.e. a List of Maps, in this example).
     */
    public static void registerAll(ExecutionContext context, ProcedureExecutor executor, Client client) {
        LOGGER.info("Registering ESQL built-in functions");

        // Register ESQL_QUERY as a built-in function.
        // It expects a single STRING parameter named "query".
        context.declareFunction("ESQL_QUERY",
            Collections.singletonList(new Parameter("query", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ESQL_QUERY", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 1) {
                    listener.onFailure(new RuntimeException("ESQL_QUERY expects exactly one argument (the query string)"));
                }
                String query = args.get(0).toString();

                // Execute the query asynchronously, using the transferred executeEsqlQuery logic.
                executeEsqlQuery(query, listener, executor, client);
            })
        );
    }

    /**
     * Handles the asynchronous execution of an ESQL query.
     * This method transfers the full async logic from ExecuteStatementHandler.handleAsync.
     *
     * @param query    the raw ESQL query to execute.
     * @param listener the ActionListener that will receive the results or error.
     * @param executor the ProcedureExecutor (providing both raw text and ExecutionContext)
     * @param client   the ES Client used to execute the query.
     */
    @SuppressWarnings("unchecked")
    public static void executeEsqlQuery(String query,
                                         ActionListener<Object> listener,
                                         ProcedureExecutor executor,
                                         Client client) {
        try {
            LOGGER.info("Executing ESQL_QUERY: {}", query);

            // Perform variable substitution on the query (if needed)
            String substitutedQuery = substituteVariables(query, executor);
            LOGGER.info("Query after substitution: {}", substitutedQuery);

            // Build a search request from the substituted query (this example uses a simple query builder).
            // Adjust buildSearchRequest as needed for your use case.
            SearchRequest searchRequest = buildSearchRequest(substitutedQuery);
            LOGGER.info("Search Request: {}", searchRequest);

            // Build the EsqlQueryRequest using the client.
            EsqlQueryRequestBuilder<? extends EsqlQueryRequest, ? extends EsqlQueryResponse> requestBuilder =
                EsqlQueryRequestBuilder.newRequestBuilder(client);
            requestBuilder.query(substitutedQuery);

            // Create the async listener for the ESQL query response.
            ActionListener<EsqlQueryResponse> asyncListener = new ActionListener<EsqlQueryResponse>() {
                @Override
                public void onResponse(EsqlQueryResponse esqlQueryResponse) {
                    try {
                        // Convert columns and rows into a list of maps.
                        List<Map<String, Object>> rowMaps = rowsAsMaps(
                            esqlQueryResponse.response().columns(),
                            esqlQueryResponse.response().rows()
                        );

                        // Normalize rows: convert nested structures to JSON strings if necessary.
                        List<Map<String, Object>> normalizedRows = new ArrayList<>();
                        for (Map<String, Object> row : rowMaps) {
                            Map<String, Object> normalized = new LinkedHashMap<>();
                            for (Map.Entry<String, Object> entry : row.entrySet()) {
                                Object value = entry.getValue();
                                if (value instanceof Map || value instanceof List) {
                                    normalized.put(entry.getKey(), convertToJsonString(value));
                                } else {
                                    normalized.put(entry.getKey(), value);
                                }
                            }
                            normalizedRows.add(normalized);
                        }

                        // Here you could add persist clause logic if needed.
                        // For this example, we simply return the normalized rows.
                        listener.onResponse(normalizedRows);
                    } catch (Exception e) {
                        listener.onFailure(e);
                    }
                }
                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            };

            ActionListener<EsqlQueryResponse> loggedListener = ActionListenerUtils.withLogging(
                asyncListener,
                EsqlBuiltInFunctions.class.getName(),
                "Execute-ESQL-QUERY: " + requestBuilder.request()
            );

            client.<EsqlQueryRequest, EsqlQueryResponse>execute(
                (ActionType<EsqlQueryResponse>) requestBuilder.action(),
                requestBuilder.request(),
                loggedListener
            );
        } catch (Exception ex) {
            listener.onFailure(ex);
        }
    }

    // --- Helper Methods ---

    /**
     * Performs variable substitution on the provided query using values from the executor's ExecutionContext.
     */
    private static String substituteVariables(String original, ProcedureExecutor executor) {
        ExecutionContext context = executor.getContext();
        for (String varName : context.getVariableNames()) {
            Object val = context.getVariable(varName);
            original = original.replace(":" + varName, String.valueOf(val));
        }
        return original;
    }

    /**
     * Builds a SearchRequest from the given query content.
     * Modify this implementation based on your actual search requirements.
     */
    private static SearchRequest buildSearchRequest(String content) {
        String indexName = extractIndexName(content); // Dynamically extract the index from the query
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.simpleQueryStringQuery(content));
        SearchRequest request = new SearchRequest(indexName);
        request.source(sourceBuilder);
        return request;
    }

    private static String extractIndexName(String query) {
        String lower = query.toLowerCase(java.util.Locale.ROOT);
        if (lower.startsWith("from ")) {
            int end = lower.indexOf(' ', 5); // space after index name
            if (end == -1) {
                return query.substring(5).trim(); // only index name present
            }
            return query.substring(5, end).trim(); // index name followed by other clauses
        }
        throw new IllegalArgumentException("Could not extract index name from query: " + query);
    }

    /**
     * Converts the provided rows (obtained from EsqlQueryResponse) into a List of Maps.
     */
    private static List<Map<String, Object>> rowsAsMaps(List<? extends ColumnInfo> columns, Iterable<Iterable<Object>> rowIter)  {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Iterable<Object> rowValues : rowIter) {
            List<Object> rowList = new ArrayList<>();
            rowValues.forEach(rowList::add);
            Map<String, Object> rowMap = new LinkedHashMap<>();
            for (int i = 0; i < columns.size(); i++) {
                String colName = columns.get(i).name();
                Object val = i < rowList.size() ? rowList.get(i) : null;
                rowMap.put(colName, val);
            }
            result.add(rowMap);
        }
        return result;
    }

    /**
     * Converts an object into a JSON string.
     * Replace this with your JSON conversion library if necessary.
     */
    private static String convertToJsonString(Object obj) {
        try {
            return obj.toString();
        } catch (Exception e) {
            return "<error converting to JSON>";
        }
    }
}
