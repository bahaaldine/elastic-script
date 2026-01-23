/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.exceptions;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced exception class for elastic-script that provides structured error information.
 * 
 * This exception can be caught in TRY/CATCH blocks and accessed via the @error binding,
 * which exposes a DOCUMENT with the following fields:
 * <ul>
 *   <li>{@code message} - The error message</li>
 *   <li>{@code code} - An optional error code (e.g., "HTTP_ERROR", "DIVISION_BY_ZERO")</li>
 *   <li>{@code type} - The exception type for named exception matching</li>
 *   <li>{@code stack_trace} - The full stack trace as a string</li>
 *   <li>{@code cause} - The cause message if present</li>
 * </ul>
 * 
 * Example usage:
 * <pre>
 * TRY
 *     THROW 'Something went wrong' WITH CODE 'CUSTOM_ERROR';
 * CATCH
 *     PRINT @error.message;
 *     PRINT @error.code;
 *     PRINT @error.type;
 * END TRY
 * </pre>
 */
public class EScriptException extends RuntimeException {
    
    /** Well-known exception types */
    public static final String TYPE_GENERIC = "error";
    public static final String TYPE_HTTP = "http_error";
    public static final String TYPE_TIMEOUT = "timeout_error";
    public static final String TYPE_DIVISION = "division_error";
    public static final String TYPE_NULL_REFERENCE = "null_reference_error";
    public static final String TYPE_TYPE_MISMATCH = "type_error";
    public static final String TYPE_VALIDATION = "validation_error";
    public static final String TYPE_NOT_FOUND = "not_found_error";
    public static final String TYPE_PERMISSION = "permission_error";
    public static final String TYPE_ESQL = "esql_error";
    public static final String TYPE_FUNCTION = "function_error";
    
    private final String code;
    private final String type;
    
    /**
     * Constructs an EScriptException with just a message.
     * Uses the default type "error" and no code.
     *
     * @param message The error message
     */
    public EScriptException(String message) {
        this(message, null, TYPE_GENERIC, null);
    }
    
    /**
     * Constructs an EScriptException with a message and code.
     *
     * @param message The error message
     * @param code The error code (e.g., "HTTP_404", "VALIDATION_FAILED")
     */
    public EScriptException(String message, String code) {
        this(message, code, TYPE_GENERIC, null);
    }
    
    /**
     * Constructs an EScriptException with message, code, and type.
     *
     * @param message The error message
     * @param code The error code
     * @param type The exception type for named exception matching
     */
    public EScriptException(String message, String code, String type) {
        this(message, code, type, null);
    }
    
    /**
     * Constructs an EScriptException with all fields.
     *
     * @param message The error message
     * @param code The error code
     * @param type The exception type
     * @param cause The underlying cause
     */
    public EScriptException(String message, String code, String type, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.type = type != null ? type : TYPE_GENERIC;
    }
    
    /**
     * Creates an EScriptException from any Throwable.
     * Attempts to infer the type from the exception class.
     *
     * @param throwable The original exception
     * @return An EScriptException wrapping the original
     */
    public static EScriptException from(Throwable throwable) {
        if (throwable instanceof EScriptException) {
            return (EScriptException) throwable;
        }
        
        String message = throwable.getMessage();
        if (message == null || message.isEmpty()) {
            message = throwable.getClass().getSimpleName();
        }
        
        String type = inferType(throwable);
        String code = inferCode(throwable);
        
        return new EScriptException(message, code, type, throwable);
    }
    
    /**
     * Infers the exception type from the throwable class.
     */
    private static String inferType(Throwable throwable) {
        String className = throwable.getClass().getSimpleName().toLowerCase();
        
        if (className.contains("timeout")) {
            return TYPE_TIMEOUT;
        } else if (className.contains("http") || className.contains("connection")) {
            return TYPE_HTTP;
        } else if (className.contains("arithmetic") || className.contains("division")) {
            return TYPE_DIVISION;
        } else if (className.contains("null") || className.contains("npe")) {
            return TYPE_NULL_REFERENCE;
        } else if (className.contains("type") || className.contains("cast")) {
            return TYPE_TYPE_MISMATCH;
        } else if (className.contains("validation") || className.contains("illegal")) {
            return TYPE_VALIDATION;
        } else if (className.contains("notfound") || className.contains("nosuch")) {
            return TYPE_NOT_FOUND;
        } else if (className.contains("permission") || className.contains("security") || className.contains("auth")) {
            return TYPE_PERMISSION;
        }
        
        return TYPE_GENERIC;
    }
    
    /**
     * Infers an error code from the throwable.
     */
    private static String inferCode(Throwable throwable) {
        // Extract code from common patterns
        String message = throwable.getMessage();
        if (message != null) {
            // Look for HTTP status codes
            if (message.contains("404")) return "HTTP_404";
            if (message.contains("500")) return "HTTP_500";
            if (message.contains("401")) return "HTTP_401";
            if (message.contains("403")) return "HTTP_403";
        }
        
        // Use class name as fallback code
        return throwable.getClass().getSimpleName().toUpperCase();
    }
    
    /**
     * Returns the error code.
     *
     * @return The error code, or null if not set
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Returns the exception type for named exception matching.
     *
     * @return The exception type
     */
    public String getType() {
        return type;
    }
    
    /**
     * Returns the full stack trace as a string.
     *
     * @return The stack trace
     */
    public String getStackTraceString() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        this.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * Checks if this exception matches the given named exception type.
     * Used for named CATCH blocks: CATCH http_error
     *
     * @param exceptionName The exception type name to match
     * @return true if this exception matches
     */
    public boolean matchesType(String exceptionName) {
        if (exceptionName == null || exceptionName.isEmpty()) {
            return true; // Catch-all
        }
        return type.equalsIgnoreCase(exceptionName);
    }
    
    /**
     * Converts this exception to a DOCUMENT (Map) for @error binding.
     * This is what gets exposed in the CATCH block as @error.
     *
     * @return A Map containing structured error information
     */
    public Map<String, Object> toDocument() {
        Map<String, Object> doc = new HashMap<>();
        doc.put("message", getMessage() != null ? getMessage() : "Unknown error");
        doc.put("code", code);
        doc.put("type", type);
        doc.put("stack_trace", getStackTraceString());
        
        if (getCause() != null) {
            doc.put("cause", getCause().getMessage());
        }
        
        return doc;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("EScriptException[");
        sb.append("type=").append(type);
        if (code != null) {
            sb.append(", code=").append(code);
        }
        sb.append(", message=").append(getMessage());
        sb.append("]");
        return sb.toString();
    }
}
