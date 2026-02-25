/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.builtin.datatypes;

import org.elasticsearch.xpack.escript.functions.api.FunctionCollectionSpec;
import org.elasticsearch.xpack.escript.functions.api.FunctionCategory;
import org.elasticsearch.xpack.escript.functions.api.FunctionParam;
import org.elasticsearch.xpack.escript.functions.api.FunctionReturn;
import org.elasticsearch.xpack.escript.functions.api.FunctionSpec;

import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.action.ActionListener;

import java.util.Arrays;
import java.util.List;

/**
 * Type conversion functions: CAST, TO_STRING, TO_NUMBER, TO_INT, TO_FLOAT, TO_BOOLEAN
 */
@FunctionCollectionSpec(
    category = FunctionCategory.CONVERSION,
    description = "Type conversion functions for casting between data types"
)
public class ConversionFunctions {

    public static void registerAll(ExecutionContext context) {
        registerCast(context);
        registerToString(context);
        registerToNumber(context);
        registerToInt(context);
        registerToFloat(context);
        registerToBoolean(context);
    }

    @FunctionSpec(
        name = "CAST",
        description = "Converts a value to a specified type. Syntax: CAST(value AS TYPE)",
        parameters = {
            @FunctionParam(name = "value", type = "ANY", description = "The value to convert"),
            @FunctionParam(name = "type", type = "STRING", description = "Target type: STRING, NUMBER, INT, FLOAT, BOOLEAN")
        },
        returnType = @FunctionReturn(type = "ANY", description = "The converted value"),
        examples = {
            "CAST(123 AS STRING) -> '123'",
            "CAST('42' AS NUMBER) -> 42",
            "CAST(1 AS BOOLEAN) -> true"
        },
        category = FunctionCategory.CONVERSION
    )
    public static void registerCast(ExecutionContext context) {
        context.declareFunction("CAST",
            Arrays.asList(
                new Parameter("value", "ANY", ParameterMode.IN),
                new Parameter("type", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("CAST", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 2) {
                    listener.onFailure(new RuntimeException("CAST expects two arguments: value and type"));
                    return;
                }
                Object value = args.get(0);
                String targetType = args.get(1) != null ? args.get(1).toString().toUpperCase() : "STRING";
                
                try {
                    Object result = convertValue(value, targetType);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("CAST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "TO_STRING",
        description = "Converts any value to its string representation",
        parameters = {
            @FunctionParam(name = "value", type = "ANY", description = "The value to convert")
        },
        returnType = @FunctionReturn(type = "STRING", description = "String representation of the value"),
        examples = {
            "TO_STRING(123) -> '123'",
            "TO_STRING(true) -> 'true'",
            "TO_STRING(null) -> 'null'"
        },
        category = FunctionCategory.CONVERSION
    )
    public static void registerToString(ExecutionContext context) {
        context.declareFunction("TO_STRING",
            List.of(new Parameter("value", "ANY", ParameterMode.IN)),
            new BuiltInFunctionDefinition("TO_STRING", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.isEmpty()) {
                    listener.onFailure(new RuntimeException("TO_STRING expects one argument"));
                    return;
                }
                Object value = args.get(0);
                listener.onResponse(value != null ? value.toString() : "null");
            })
        );
    }

    @FunctionSpec(
        name = "TO_NUMBER",
        description = "Converts a value to a number (double)",
        parameters = {
            @FunctionParam(name = "value", type = "ANY", description = "The value to convert")
        },
        returnType = @FunctionReturn(type = "NUMBER", description = "Numeric value"),
        examples = {
            "TO_NUMBER('42.5') -> 42.5",
            "TO_NUMBER(true) -> 1.0",
            "TO_NUMBER('100') -> 100.0"
        },
        category = FunctionCategory.CONVERSION
    )
    public static void registerToNumber(ExecutionContext context) {
        context.declareFunction("TO_NUMBER",
            List.of(new Parameter("value", "ANY", ParameterMode.IN)),
            new BuiltInFunctionDefinition("TO_NUMBER", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.isEmpty()) {
                    listener.onFailure(new RuntimeException("TO_NUMBER expects one argument"));
                    return;
                }
                try {
                    Object result = convertValue(args.get(0), "NUMBER");
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("TO_NUMBER failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "TO_INT",
        description = "Converts a value to an integer",
        parameters = {
            @FunctionParam(name = "value", type = "ANY", description = "The value to convert")
        },
        returnType = @FunctionReturn(type = "INTEGER", description = "Integer value"),
        examples = {
            "TO_INT('42') -> 42",
            "TO_INT(42.9) -> 42",
            "TO_INT(true) -> 1"
        },
        category = FunctionCategory.CONVERSION
    )
    public static void registerToInt(ExecutionContext context) {
        context.declareFunction("TO_INT",
            List.of(new Parameter("value", "ANY", ParameterMode.IN)),
            new BuiltInFunctionDefinition("TO_INT", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.isEmpty()) {
                    listener.onFailure(new RuntimeException("TO_INT expects one argument"));
                    return;
                }
                try {
                    Object result = convertValue(args.get(0), "INT");
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("TO_INT failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "TO_FLOAT",
        description = "Converts a value to a floating-point number",
        parameters = {
            @FunctionParam(name = "value", type = "ANY", description = "The value to convert")
        },
        returnType = @FunctionReturn(type = "FLOAT", description = "Floating-point value"),
        examples = {
            "TO_FLOAT('3.14') -> 3.14",
            "TO_FLOAT(42) -> 42.0"
        },
        category = FunctionCategory.CONVERSION
    )
    public static void registerToFloat(ExecutionContext context) {
        context.declareFunction("TO_FLOAT",
            List.of(new Parameter("value", "ANY", ParameterMode.IN)),
            new BuiltInFunctionDefinition("TO_FLOAT", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.isEmpty()) {
                    listener.onFailure(new RuntimeException("TO_FLOAT expects one argument"));
                    return;
                }
                try {
                    Object result = convertValue(args.get(0), "FLOAT");
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("TO_FLOAT failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "TO_BOOLEAN",
        description = "Converts a value to a boolean",
        parameters = {
            @FunctionParam(name = "value", type = "ANY", description = "The value to convert")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "Boolean value"),
        examples = {
            "TO_BOOLEAN('true') -> true",
            "TO_BOOLEAN(1) -> true",
            "TO_BOOLEAN(0) -> false",
            "TO_BOOLEAN('yes') -> true"
        },
        category = FunctionCategory.CONVERSION
    )
    public static void registerToBoolean(ExecutionContext context) {
        context.declareFunction("TO_BOOLEAN",
            List.of(new Parameter("value", "ANY", ParameterMode.IN)),
            new BuiltInFunctionDefinition("TO_BOOLEAN", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.isEmpty()) {
                    listener.onFailure(new RuntimeException("TO_BOOLEAN expects one argument"));
                    return;
                }
                try {
                    Object result = convertValue(args.get(0), "BOOLEAN");
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("TO_BOOLEAN failed: " + e.getMessage(), e));
                }
            })
        );
    }

    /**
     * Core conversion logic
     */
    private static Object convertValue(Object value, String targetType) {
        if (value == null) {
            return null;
        }
        
        switch (targetType) {
            case "STRING":
                return value.toString();
                
            case "NUMBER":
            case "FLOAT":
            case "DOUBLE":
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                } else if (value instanceof Boolean) {
                    return ((Boolean) value) ? 1.0 : 0.0;
                } else {
                    return Double.parseDouble(value.toString().trim());
                }
                
            case "INT":
            case "INTEGER":
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                } else if (value instanceof Boolean) {
                    return ((Boolean) value) ? 1 : 0;
                } else {
                    String str = value.toString().trim();
                    if (str.contains(".")) {
                        return (int) Double.parseDouble(str);
                    }
                    return Integer.parseInt(str);
                }
                
            case "LONG":
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                } else if (value instanceof Boolean) {
                    return ((Boolean) value) ? 1L : 0L;
                } else {
                    String str = value.toString().trim();
                    if (str.contains(".")) {
                        return (long) Double.parseDouble(str);
                    }
                    return Long.parseLong(str);
                }
                
            case "BOOLEAN":
            case "BOOL":
                if (value instanceof Boolean) {
                    return value;
                } else if (value instanceof Number) {
                    return ((Number) value).doubleValue() != 0;
                } else {
                    String str = value.toString().trim().toLowerCase();
                    return str.equals("true") || str.equals("yes") || str.equals("1") || str.equals("on");
                }
                
            default:
                throw new RuntimeException("Unknown target type: " + targetType + 
                    ". Supported types: STRING, NUMBER, INT, FLOAT, BOOLEAN");
        }
    }
}
