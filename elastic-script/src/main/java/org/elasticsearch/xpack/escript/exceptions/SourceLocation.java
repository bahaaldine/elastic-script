/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a source location in an elastic-script file.
 * Used for error reporting with line and column numbers.
 */
public class SourceLocation {
    
    private final int line;
    private final int column;
    private final int endLine;
    private final int endColumn;
    private final String procedureName;
    private final String statementType;
    
    /**
     * Creates a source location from ANTLR context.
     */
    public SourceLocation(ParserRuleContext ctx) {
        this(ctx, null, null);
    }
    
    /**
     * Creates a source location with procedure context.
     */
    public SourceLocation(ParserRuleContext ctx, String procedureName, String statementType) {
        if (ctx != null && ctx.getStart() != null) {
            this.line = ctx.getStart().getLine();
            this.column = ctx.getStart().getCharPositionInLine() + 1; // 1-based
            Token stop = ctx.getStop();
            if (stop != null) {
                this.endLine = stop.getLine();
                this.endColumn = stop.getCharPositionInLine() + 1;
            } else {
                this.endLine = this.line;
                this.endColumn = this.column;
            }
        } else {
            this.line = 0;
            this.column = 0;
            this.endLine = 0;
            this.endColumn = 0;
        }
        this.procedureName = procedureName;
        this.statementType = statementType;
    }
    
    /**
     * Creates a source location with explicit values.
     */
    public SourceLocation(int line, int column, String procedureName, String statementType) {
        this.line = line;
        this.column = column;
        this.endLine = line;
        this.endColumn = column;
        this.procedureName = procedureName;
        this.statementType = statementType;
    }
    
    public int getLine() {
        return line;
    }
    
    public int getColumn() {
        return column;
    }
    
    public int getEndLine() {
        return endLine;
    }
    
    public int getEndColumn() {
        return endColumn;
    }
    
    public String getProcedureName() {
        return procedureName;
    }
    
    public String getStatementType() {
        return statementType;
    }
    
    /**
     * Returns true if this location has valid line/column info.
     */
    public boolean hasPosition() {
        return line > 0;
    }
    
    /**
     * Returns a human-readable location string.
     * Examples: "line 42", "line 42, column 5", "in procedure 'foo' at line 42"
     */
    public String toLocationString() {
        StringBuilder sb = new StringBuilder();
        
        if (procedureName != null && !procedureName.isEmpty()) {
            sb.append("in procedure '").append(procedureName).append("' ");
        }
        
        if (line > 0) {
            sb.append("at line ").append(line);
            if (column > 0) {
                sb.append(", column ").append(column);
            }
        }
        
        return sb.toString().trim();
    }
    
    /**
     * Converts to a document for @error binding.
     */
    public Map<String, Object> toDocument() {
        Map<String, Object> doc = new HashMap<>();
        doc.put("line", line);
        doc.put("column", column);
        doc.put("end_line", endLine);
        doc.put("end_column", endColumn);
        if (procedureName != null) {
            doc.put("procedure", procedureName);
        }
        if (statementType != null) {
            doc.put("statement", statementType);
        }
        return doc;
    }
    
    @Override
    public String toString() {
        if (!hasPosition()) {
            return "<unknown location>";
        }
        return line + ":" + column;
    }
}
