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
    
    // OPEN/CLOSE/FETCH state
    private boolean isOpen;
    private int currentPosition;
    private int rowCount;  // Total rows fetched via FETCH
    private boolean notFound;

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
        this.isOpen = false;
        this.currentPosition = 0;
        this.rowCount = 0;
        this.notFound = false;
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
        this.isOpen = false;
        this.currentPosition = 0;
        this.rowCount = 0;
        this.notFound = false;
    }
    
    // ==================== OPEN/CLOSE/FETCH Support ====================
    
    /**
     * Check if the cursor is open.
     */
    public boolean isOpen() {
        return isOpen;
    }
    
    /**
     * Open the cursor. The query must have been executed first.
     * 
     * @throws IllegalStateException if cursor not executed or already open
     */
    public void open() {
        if (!executed) {
            throw new IllegalStateException("Cursor '" + name + "' has not been executed yet");
        }
        if (isOpen) {
            throw new IllegalStateException("Cursor '" + name + "' is already open");
        }
        this.isOpen = true;
        this.currentPosition = 0;
        this.rowCount = 0;
        this.notFound = results == null || results.isEmpty();
    }
    
    /**
     * Open the cursor with results (executes and opens in one step).
     */
    public void openWithResults(List<Map<String, Object>> results) {
        this.results = results;
        this.executed = true;
        this.isOpen = true;
        this.currentPosition = 0;
        this.rowCount = 0;
        this.notFound = results == null || results.isEmpty();
    }
    
    /**
     * Close the cursor.
     */
    public void close() {
        this.isOpen = false;
        // Keep results for potential re-open
    }
    
    /**
     * Fetch the next row from the cursor.
     * 
     * @return the next row as a Map, or null if no more rows
     * @throws IllegalStateException if cursor not open
     */
    public Map<String, Object> fetchNext() {
        if (!isOpen) {
            throw new IllegalStateException("Cursor '" + name + "' is not open");
        }
        
        if (results == null || currentPosition >= results.size()) {
            notFound = true;
            return null;
        }
        
        Map<String, Object> row = results.get(currentPosition);
        currentPosition++;
        rowCount++;
        notFound = false;
        
        return row;
    }
    
    /**
     * Fetch multiple rows from the cursor.
     * 
     * @param limit maximum number of rows to fetch
     * @return list of rows, may be smaller than limit or empty
     * @throws IllegalStateException if cursor not open
     */
    public List<Map<String, Object>> fetchBatch(int limit) {
        if (!isOpen) {
            throw new IllegalStateException("Cursor '" + name + "' is not open");
        }
        
        if (results == null || currentPosition >= results.size()) {
            notFound = true;
            return new java.util.ArrayList<>();
        }
        
        int endPosition = Math.min(currentPosition + limit, results.size());
        List<Map<String, Object>> batch = new java.util.ArrayList<>(
            results.subList(currentPosition, endPosition)
        );
        
        int fetchedCount = batch.size();
        currentPosition = endPosition;
        rowCount += fetchedCount;
        notFound = fetchedCount == 0;
        
        return batch;
    }
    
    /**
     * Check if the last fetch found no rows (%NOTFOUND).
     */
    public boolean isNotFound() {
        return notFound;
    }
    
    /**
     * Get the total number of rows fetched via FETCH (%ROWCOUNT).
     */
    public int getRowCount() {
        return rowCount;
    }
    
    /**
     * Get the current position in the result set.
     */
    public int getCurrentPosition() {
        return currentPosition;
    }
    
    /**
     * Get the total number of rows in the result set.
     */
    public int getTotalRows() {
        return results != null ? results.size() : 0;
    }
    
    /**
     * Check if there are more rows available.
     */
    public boolean hasMore() {
        return isOpen && results != null && currentPosition < results.size();
    }

    @Override
    public String toString() {
        return "CursorDefinition{" +
            "name='" + name + '\'' +
            ", executed=" + executed +
            ", isOpen=" + isOpen +
            ", position=" + currentPosition +
            ", rowCount=" + rowCount +
            ", totalRows=" + (results != null ? results.size() : 0) +
            '}';
    }
}




