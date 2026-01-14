/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.xpack.esql.action.ColumnInfo;
import org.elasticsearch.xpack.esql.action.EsqlQueryAction;
import org.elasticsearch.xpack.esql.action.EsqlQueryRequest;
import org.elasticsearch.xpack.esql.action.EsqlQueryResponse;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.logging.EScriptLogger;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;
import org.elasticsearch.xpack.escript.utils.ActionListenerUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles ES|QL INTO statements that store query results into a variable or index.
 * 
 * Examples:
 *   FROM logs-* | WHERE level = 'ERROR' | INTO my_results;
 *   FROM logs-* | WHERE level = 'ERROR' | INTO 'destination-index';
 */
public class EsqlIntoStatementHandler {

    private final ProcedureExecutor executor;
    private final Client client;

    public EsqlIntoStatementHandler(ProcedureExecutor executor, Client client) {
        this.executor = executor;
        this.client = client;
    }

    /**
     * Handles the ES|QL INTO statement asynchronously.
     */
    public void handleAsync(ElasticScriptParser.Esql_into_statementContext ctx, ActionListener<Object> listener) {
        try {
            // Extract the ES|QL query (FROM ... without INTO)
            String rawQuery = buildEsqlQuery(ctx.esql_query());
            String executionId = executor.getContext().getExecutionId();

            // Variable substitution
            String substitutedQuery = substituteVariables(rawQuery);
            EScriptLogger.esqlQuery(executionId, substitutedQuery);

            // Determine target (variable or index)
            ElasticScriptParser.Into_targetContext target = ctx.into_target();
            boolean isIndexTarget = target instanceof ElasticScriptParser.IntoIndexContext;
            String targetName;
            
            if (isIndexTarget) {
                // Remove quotes from index name
                String quoted = ((ElasticScriptParser.IntoIndexContext) target).STRING().getText();
                targetName = quoted.substring(1, quoted.length() - 1);
            } else {
                targetName = ((ElasticScriptParser.IntoVariableContext) target).ID().getText();
            }

            // Execute the ES|QL query
            EsqlQueryRequest request = EsqlQueryRequest.syncEsqlQueryRequest(substitutedQuery);

            ActionListener<EsqlQueryResponse> esqlListener = new ActionListener<>() {
                @Override
                public void onResponse(EsqlQueryResponse response) {
                    try {
                        // Convert results to list of maps
                        List<Map<String, Object>> results = rowsAsMaps(
                            (List<ColumnInfo>) response.response().columns(),
                            response.response().rows()
                        );

                        if (isIndexTarget) {
                            // Index results into destination index
                            indexResults(targetName, results, listener);
                        } else {
                            // Store results in variable
                            storeInVariable(targetName, results, listener);
                        }
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
                esqlListener,
                this.getClass().getName(),
                "ESQL-INTO: " + substitutedQuery
            );

            client.execute(EsqlQueryAction.INSTANCE, request, loggedListener);

        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    /**
     * Builds the ES|QL query string from the parse tree.
     */
    private String buildEsqlQuery(ElasticScriptParser.Esql_queryContext ctx) {
        StringBuilder query = new StringBuilder();
        query.append("FROM ");
        query.append(executor.getRawText(ctx.esql_body()));
        return query.toString();
    }

    /**
     * Stores the results in a variable.
     */
    private void storeInVariable(String variableName, List<Map<String, Object>> results, ActionListener<Object> listener) {
        try {
            ExecutionContext exeContext = executor.getContext();
            if (!exeContext.hasVariable(variableName)) {
                exeContext.declareVariable(variableName, "ARRAY");
            }
            exeContext.setVariable(variableName, results);
            listener.onResponse(results);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    /**
     * Indexes the results into the destination index using bulk API.
     */
    private void indexResults(String indexName, List<Map<String, Object>> results, ActionListener<Object> listener) {
        if (results.isEmpty()) {
            listener.onResponse(0);
            return;
        }

        BulkRequest bulkRequest = new BulkRequest();
        for (Map<String, Object> doc : results) {
            IndexRequest indexRequest = new IndexRequest(indexName)
                .source(doc, XContentType.JSON);
            bulkRequest.add(indexRequest);
        }

        client.bulk(bulkRequest, new ActionListener<>() {
            @Override
            public void onResponse(BulkResponse bulkResponse) {
                if (bulkResponse.hasFailures()) {
                    listener.onFailure(new RuntimeException(
                        "Bulk indexing had failures: " + bulkResponse.buildFailureMessage()
                    ));
                } else {
                    listener.onResponse(results.size());
                }
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    /**
     * Converts ES|QL response rows to a list of maps.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> rowsAsMaps(List<ColumnInfo> columns, Iterable<Iterable<Object>> rows) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Iterable<Object> rowValues : rows) {
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
     * Substitutes variables in the query string.
     */
    private String substituteVariables(String original) {
        String result = original;
        ExecutionContext ctx = executor.getContext();

        // Pattern for @variable or :variable
        Pattern pattern = Pattern.compile("[@:]([a-zA-Z_][a-zA-Z0-9_]*)");
        Matcher matcher = pattern.matcher(original);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            if (ctx.hasVariable(varName)) {
                Object value = ctx.getVariable(varName);
                String replacement = valueToString(value);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Converts a value to a string suitable for ES|QL query substitution.
     */
    private String valueToString(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "'" + value.toString().replace("'", "''") + "'";
        } else if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        } else {
            return "'" + value.toString().replace("'", "''") + "'";
        }
    }
}
