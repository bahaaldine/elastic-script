/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;

/**
 * Handles ES|QL PROCESS WITH statements that process query results with a procedure.
 * 
 * Note: This feature is planned for a future release.
 * Currently, use CURSOR for ES|QL row-by-row processing:
 *   DECLARE logs CURSOR FOR FROM logs-* | WHERE level = 'ERROR';
 *   FOR log IN logs LOOP
 *       CALL my_procedure(log);
 *   END LOOP
 * 
 * Planned syntax:
 *   FROM logs-* | WHERE level = 'ERROR' PROCESS WITH my_procedure;
 *   FROM logs-* | WHERE level = 'ERROR' PROCESS WITH my_procedure BATCH 50;
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
    public void handleAsync(ElasticScriptParser.Esql_process_statementContext ctx, ActionListener<Object> listener) {
        // Feature not yet implemented
        listener.onFailure(new UnsupportedOperationException(
            "ES|QL PROCESS WITH statement is not yet implemented. " +
            "Use CURSOR instead: DECLARE logs CURSOR FOR FROM logs-* | WHERE x = y; FOR log IN logs LOOP ... END LOOP"
        ));
    }
}
