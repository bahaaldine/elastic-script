/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.primitives;

import java.util.List;
import java.util.Map;

/**
 * Represents a cursor definition in elastic-script.
 * 
 * A cursor is a named reference to an ESQL query that can be iterated over.
 * The query is executed lazily when the cursor is first used in a FOR loop.
 * 
 * Example usage in elastic-script:
 * <pre>
 * DECLARE error_logs CURSOR FOR
 *     FROM application-logs 
 *     | WHERE log.level == "ERROR" 
 *     | LIMIT 10;
 * 
 * FOR log_entry IN error_logs LOOP
 *     PRINT log_entry.message;
 * END LOOP;
 * </pre>
 */
public class CursorDefinition {

    private final String name;
    private final String esqlQuery;
    private List<Map<String, Object>> results;
    private boolean executed;

    /**
     * Creates a new cursor definition.
     *
     * @param name The name of the cursor
     * @param esqlQuery The ESQL query associated with this cursor
     */
    public CursorDefinition(String name, String esqlQuery) {
        this.name = name;
        this.esqlQuery = esqlQuery;
        this.results = null;
        this.executed = false;
    }

    /**
     * Gets the cursor name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the ESQL query string.
     */
    public String getEsqlQuery() {
        return esqlQuery;
    }

    /**
     * Checks if the cursor query has been executed.
     */
    public boolean isExecuted() {
        return executed;
    }

    /**
     * Gets the query results. Returns null if not yet executed.
     */
    public List<Map<String, Object>> getResults() {
        return results;
    }

    /**
     * Sets the query results after execution.
     *
     * @param results The list of result rows, each row being a map of column name to value
     */
    public void setResults(List<Map<String, Object>> results) {
        this.results = results;
        this.executed = true;
    }

    /**
     * Resets the cursor so it can be re-executed.
     * Useful if the same cursor needs to be iterated multiple times.
     */
    public void reset() {
        this.results = null;
        this.executed = false;
    }

    @Override
    public String toString() {
        return "CursorDefinition{" +
            "name='" + name + '\'' +
            ", executed=" + executed +
            ", rowCount=" + (results != null ? results.size() : 0) +
            '}';
    }
}



