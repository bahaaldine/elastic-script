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
 * Handles ES|QL INTO statements that store query results into a variable or index.
 * 
 * Note: This feature is planned for a future release.
 * Currently, use ESQL_QUERY() function for ES|QL integration:
 *   DECLARE result ARRAY = ESQL_QUERY('FROM logs-* | WHERE level = "ERROR"');
 * 
 * Planned syntax:
 *   FROM logs-* | WHERE level = 'ERROR' INTO my_results;
 *   FROM logs-* | WHERE level = 'ERROR' INTO 'destination-index';
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
        // Feature not yet implemented
        listener.onFailure(new UnsupportedOperationException(
            "ES|QL INTO statement is not yet implemented. " +
            "Use ESQL_QUERY() function instead: DECLARE result = ESQL_QUERY('FROM logs-* | WHERE x = y');"
        ));
    }
}
