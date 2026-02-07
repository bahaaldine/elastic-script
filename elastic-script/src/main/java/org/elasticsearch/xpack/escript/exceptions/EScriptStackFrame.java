/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.exceptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single frame in an elastic-script call stack.
 * Similar to Java's StackTraceElement but for elastic-script.
 */
public class EScriptStackFrame {
    
    private final String procedureName;
    private final String statementType;
    private final int line;
    private final int column;
    private final String snippet;
    
    /**
     * Creates a stack frame.
     *
     * @param procedureName The procedure/function name, or null for anonymous block
     * @param statementType The type of statement (e.g., "CALL", "SET", "IF")
     * @param line The line number (1-based)
     * @param column The column number (1-based)
     * @param snippet A short code snippet for context
     */
    public EScriptStackFrame(String procedureName, String statementType, int line, int column, String snippet) {
        this.procedureName = procedureName != null ? procedureName : "<anonymous>";
        this.statementType = statementType != null ? statementType : "UNKNOWN";
        this.line = line;
        this.column = column;
        this.snippet = snippet;
    }
    
    /**
     * Creates a stack frame from a SourceLocation.
     */
    public EScriptStackFrame(SourceLocation location, String snippet) {
        this(
            location.getProcedureName(),
            location.getStatementType(),
            location.getLine(),
            location.getColumn(),
            snippet
        );
    }
    
    public String getProcedureName() {
        return procedureName;
    }
    
    public String getStatementType() {
        return statementType;
    }
    
    public int getLine() {
        return line;
    }
    
    public int getColumn() {
        return column;
    }
    
    public String getSnippet() {
        return snippet;
    }
    
    /**
     * Formats this frame as a string, similar to Java stack trace format.
     * Example: "at my_procedure (line 42) [CALL]"
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("    at ").append(procedureName);
        if (line > 0) {
            sb.append(" (line ").append(line);
            if (column > 0) {
                sb.append(":").append(column);
            }
            sb.append(")");
        }
        sb.append(" [").append(statementType).append("]");
        if (snippet != null && !snippet.isEmpty()) {
            sb.append(": ").append(truncateSnippet(snippet, 60));
        }
        return sb.toString();
    }
    
    /**
     * Truncates a code snippet to the specified length.
     */
    private String truncateSnippet(String text, int maxLength) {
        if (text == null) return "";
        // Remove newlines and excess whitespace
        text = text.replaceAll("\\s+", " ").trim();
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Converts to a document for @error.stack binding.
     */
    public Map<String, Object> toDocument() {
        Map<String, Object> doc = new HashMap<>();
        doc.put("procedure", procedureName);
        doc.put("statement", statementType);
        doc.put("line", line);
        doc.put("column", column);
        if (snippet != null) {
            doc.put("snippet", snippet);
        }
        return doc;
    }
}
