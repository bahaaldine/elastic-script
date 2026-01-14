/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.xpack.core.esql.action.ColumnInfo;
import org.elasticsearch.xpack.esql.action.EsqlQueryAction;
import org.elasticsearch.xpack.esql.action.EsqlQueryRequest;
import org.elasticsearch.xpack.esql.action.EsqlQueryResponse;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.FunctionDefinition;
import org.elasticsearch.xpack.escript.logging.EScriptLogger;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;
import org.elasticsearch.xpack.escript.utils.ActionListenerUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles ES|QL PROCESS WITH statements that invoke a procedure for each row.
 * 
 * Examples:
 *   FROM logs-* | WHERE level = 'ERROR' | PROCESS WITH handle_error;
 *   FROM logs-* | WHERE level = 'ERROR' | PROCESS WITH handle_error BATCH 50;
 */
public class EsqlProcessStatementHandler {

    private final ProcedureExecutor executor;
    private final Client client;

    public EsqlProcessStatementHandler(ProcedureExecutor executor, Client client) {
        this.executor = executor;
        this.client = client;
    }

    /**
     * Handles the ES|QL PROCESS WITH statement asynchronously.
     */
    @SuppressWarnings("unchecked")
    public void handleAsync(ElasticScriptParser.Esql_process_statementContext ctx, ActionListener<Object> listener) {
        try {
            // Extract the ES|QL query
            String rawQuery = buildEsqlQuery(ctx);
            String executionId = executor.getContext().getExecutionId();

            // Variable substitution
            String substitutedQuery = substituteVariables(rawQuery);
            EScriptLogger.esqlQuery(executionId, substitutedQuery);

            // Get the procedure name
            String procedureName = ctx.ID().getText();

            // Get batch size if specified
            int batchSize = 1; // Default: process one row at a time
            if (ctx.BATCH() != null && ctx.INT() != null) {
                batchSize = Integer.parseInt(ctx.INT().getText());
            }

            final int finalBatchSize = batchSize;

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

                        // Process results with the procedure
                        processRowsWithProcedure(procedureName, results, finalBatchSize, listener);
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
                "ESQL-PROCESS: " + substitutedQuery + " WITH " + procedureName
            );

            client.execute(EsqlQueryAction.INSTANCE, request, loggedListener);

        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    /**
     * Builds the ES|QL query string from the parse tree.
     */
    private String buildEsqlQuery(ElasticScriptParser.Esql_process_statementContext ctx) {
        return executor.getRawText(ctx.esql_text());
    }

    /**
     * Processes rows by invoking the procedure for each row (or batch of rows).
     */
    private void processRowsWithProcedure(
        String procedureName,
        List<Map<String, Object>> rows,
        int batchSize,
        ActionListener<Object> listener
    ) {
        if (rows.isEmpty()) {
            listener.onResponse(0);
            return;
        }

        ExecutionContext ctx = executor.getContext();
        
        // Check if procedure exists
        FunctionDefinition procDef = ctx.getFunction(procedureName);
        if (procDef == null) {
            listener.onFailure(new RuntimeException("Procedure not found: " + procedureName));
            return;
        }

        // Create batches
        List<List<Map<String, Object>>> batches = new ArrayList<>();
        for (int i = 0; i < rows.size(); i += batchSize) {
            int end = Math.min(i + batchSize, rows.size());
            batches.add(rows.subList(i, end));
        }

        // Process batches sequentially
        processBatchesSequentially(procedureName, batches.iterator(), 0, listener);
    }

    /**
     * Processes batches sequentially, one at a time.
     */
    private void processBatchesSequentially(
        String procedureName,
        Iterator<List<Map<String, Object>>> batchIterator,
        int processedCount,
        ActionListener<Object> listener
    ) {
        if (!batchIterator.hasNext()) {
            // All batches processed
            listener.onResponse(processedCount);
            return;
        }

        List<Map<String, Object>> batch = batchIterator.next();
        
        // For batch size 1, pass the single document; otherwise pass the batch
        Object arg = batch.size() == 1 ? batch.get(0) : batch;
        List<Object> args = Collections.singletonList(arg);

        ExecutionContext ctx = executor.getContext();
        FunctionDefinition procDef = ctx.getFunction(procedureName);

        procDef.execute(args, new ActionListener<>() {
            @Override
            public void onResponse(Object result) {
                // Process next batch
                processBatchesSequentially(
                    procedureName,
                    batchIterator,
                    processedCount + batch.size(),
                    listener
                );
            }

            @Override
            public void onFailure(Exception e) {
                // Log error but continue processing (optional: could be configurable)
                EScriptLogger.warn(
                    executor.getContext().getExecutionId(),
                    "Error processing batch: " + e.getMessage()
                );
                // Continue with next batch
                processBatchesSequentially(
                    procedureName,
                    batchIterator,
                    processedCount + batch.size(),
                    listener
                );
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
        ExecutionContext ctx = executor.getContext();

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
