/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.builtin.datatypes;

import org.elasticsearch.xpack.escript.functions.api.FunctionCollectionSpec;
import org.elasticsearch.xpack.escript.functions.api.FunctionCategory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.xpack.escript.functions.api.FunctionParam;
import org.elasticsearch.xpack.escript.functions.api.FunctionReturn;
import org.elasticsearch.xpack.escript.functions.api.FunctionSpec;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;
import org.elasticsearch.action.ActionListener;

import org.elasticsearch.xpack.escript.primitives.LambdaExpression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Collection of built-in array manipulation functions for Elastic Script.
 *
 * This class defines multiple utility functions to operate on arrays,
 * such as computing length, appending/prepending elements, filtering for uniqueness, etc.
 *
 * These functions are registered into the execution context during initialization.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.ARRAY,
    description = "Built-in array manipulation functions like ARRAY_LENGTH, ARRAY_APPEND, etc."
)
public class ArrayBuiltInFunctions {
    private static final Logger LOGGER = LogManager.getLogger(ArrayBuiltInFunctions.class);

    public static void registerAll(ExecutionContext context) {
        LOGGER.info("Registering Array built-in functions");

        registerArrayLength(context);
        registerArrayAppend(context);
        registerArrayPrepend(context);
        registerArrayRemove(context);
        registerArrayContains(context);
        registerArrayDistinct(context);
        
        // Phase 5: Functional array operations
        registerArrayJoin(context);
        registerArrayFlatten(context);
        registerArrayReverse(context);
        registerArraySlice(context);
        registerArrayMap(context);
        registerArrayFilter(context);
        registerArrayReduce(context);
        registerArrayFind(context);
        registerArrayFindIndex(context);
        registerArrayEvery(context);
        registerArraySome(context);
        registerArraySort(context);
    }

    @FunctionSpec(
        name = "ARRAY_LENGTH",
        description = "Returns the number of elements in the given array.",
        parameters = {
            @FunctionParam(name = "array", type = "ARRAY", description = "The array whose length is to be determined")
        },
        returnType = @FunctionReturn(type = "INTEGER", description = "The length of the array"),
        examples = {
            "ARRAY_LENGTH([1, 2, 3]) -> 3"
        },
        category = FunctionCategory.ARRAY
    )
    public static void registerArrayLength(ExecutionContext context) {
        context.declareFunction("ARRAY_LENGTH",
            Collections.singletonList(new Parameter("array", "ARRAY", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ARRAY_LENGTH", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 1) {
                    listener.onFailure(new RuntimeException("ARRAY_LENGTH expects one argument"));
                }
                if ( (args.get(0) instanceof List) == false ) {
                    listener.onFailure(new RuntimeException("ARRAY_LENGTH expects an array argument"));
                }
                List<?> array = (List<?>) args.get(0);
                listener.onResponse(array.size());
            })
        );
    }

    @FunctionSpec(
        name = "ARRAY_APPEND",
        description = "Returns a new array with the specified element appended to the end.",
        parameters = {
            @FunctionParam(name = "array", type = "ARRAY", description = "The array to append to"),
            @FunctionParam(name = "element", type = "ANY", description = "The element to append")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "A new array with the element appended"),
        examples = {
            "ARRAY_APPEND([1, 2], 3) -> [1, 2, 3]"
        },
        category = FunctionCategory.ARRAY
    )
    public static void registerArrayAppend(ExecutionContext context) {
        context.declareFunction("ARRAY_APPEND",
            Arrays.asList(
                new Parameter("array", "ARRAY", ParameterMode.IN),
                new Parameter("element", "ANY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ARRAY_APPEND", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 2) {
                    listener.onFailure(new RuntimeException("ARRAY_APPEND expects two arguments: an array and an element"));
                }
                if ( (args.get(0) instanceof List) == false ) {
                    listener.onFailure(new RuntimeException("ARRAY_APPEND expects the first argument to be an array"));
                }
                List<Object> array = new ArrayList<>((List<?>) args.get(0));
                array.add(args.get(1));
                listener.onResponse(array);
            })
        );
    }

    @FunctionSpec(
        name = "ARRAY_PREPEND",
        description = "Returns a new array with the specified element prepended to the beginning.",
        parameters = {
            @FunctionParam(name = "array", type = "ARRAY", description = "The array to prepend to"),
            @FunctionParam(name = "element", type = "ANY", description = "The element to prepend")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "A new array with the element prepended"),
        examples = {
            "ARRAY_PREPEND([2, 3], 1) -> [1, 2, 3]"
        },
        category = FunctionCategory.ARRAY
    )
    public static void registerArrayPrepend(ExecutionContext context) {
        context.declareFunction("ARRAY_PREPEND",
            Arrays.asList(
                new Parameter("array", "ARRAY", ParameterMode.IN),
                new Parameter("element", "ANY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ARRAY_PREPEND", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 2) {
                    listener.onFailure(new RuntimeException("ARRAY_PREPEND expects two arguments: an array and an element"));
                }
                if ((args.get(0) instanceof List) == false ) {
                    listener.onFailure(new RuntimeException("ARRAY_PREPEND expects the first argument to be an array"));
                }
                List<Object> array = new ArrayList<>();
                array.add(args.get(1));
                array.addAll((List<?>) args.get(0));
                listener.onResponse(array);
            })
        );
    }

    @FunctionSpec(
        name = "ARRAY_REMOVE",
        description = "Returns a new array with all occurrences of the specified element removed.",
        parameters = {
            @FunctionParam(name = "array", type = "ARRAY", description = "The array to remove elements from"),
            @FunctionParam(name = "element", type = "ANY", description = "The element to remove")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "A new array with the element removed"),
        examples = {
            "ARRAY_REMOVE([1, 2, 1, 3], 1) -> [2, 3]"
        },
        category = FunctionCategory.ARRAY
    )
    public static void registerArrayRemove(ExecutionContext context) {
        context.declareFunction("ARRAY_REMOVE",
            Arrays.asList(
                new Parameter("array", "ARRAY", ParameterMode.IN),
                new Parameter("element", "ANY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ARRAY_REMOVE", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 2) {
                    listener.onFailure(new RuntimeException("ARRAY_REMOVE expects two arguments: an array and an element to remove"));
                }
                if ( (args.get(0) instanceof List) == false ) {
                    listener.onFailure(new RuntimeException("ARRAY_REMOVE expects the first argument to be an array"));
                }
                List<Object> array = new ArrayList<>((List<?>) args.get(0));
                Object toRemove = args.get(1);
                array.removeIf(e -> e == null ? toRemove == null : e.equals(toRemove));
                listener.onResponse(array);
            })
        );
    }

    @FunctionSpec(
        name = "ARRAY_CONTAINS",
        description = "Returns true if the array contains the specified element.",
        parameters = {
            @FunctionParam(name = "array", type = "ARRAY", description = "The array to search"),
            @FunctionParam(name = "element", type = "ANY", description = "The element to search for")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if the array contains the element, otherwise false"),
        examples = {
            "ARRAY_CONTAINS([1, 2, 3], 2) -> true"
        },
        category = FunctionCategory.ARRAY
    )
    public static void registerArrayContains(ExecutionContext context) {
        context.declareFunction("ARRAY_CONTAINS",
            Arrays.asList(
                new Parameter("array", "ARRAY", ParameterMode.IN),
                new Parameter("element", "ANY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ARRAY_CONTAINS", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 2) {
                    listener.onFailure(new RuntimeException("ARRAY_CONTAINS expects two arguments: an array and an element"));
                }
                if ( (args.get(0) instanceof List) == false ) {
                    listener.onFailure(new RuntimeException("ARRAY_CONTAINS expects the first argument to be an array"));
                }
                List<?> array = (List<?>) args.get(0);
                Object element = args.get(1);
                listener.onResponse(array.contains(element));
            })
        );
    }

    @FunctionSpec(
        name = "ARRAY_DISTINCT",
        description = "Returns a new array with duplicate elements removed, preserving original order.",
        parameters = {
            @FunctionParam(name = "array", type = "ARRAY", description = "The array to deduplicate")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "A new array with duplicates removed"),
        examples = {
            "ARRAY_DISTINCT([1, 2, 2, 3]) -> [1, 2, 3]"
        },
        category = FunctionCategory.ARRAY
    )
    public static void registerArrayDistinct(ExecutionContext context) {
        context.declareFunction("ARRAY_DISTINCT",
            Collections.singletonList(new Parameter("array", "ARRAY", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ARRAY_DISTINCT", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 1) {
                    listener.onFailure(new RuntimeException("ARRAY_DISTINCT expects one argument: an array"));
                }
                if ( (args.get(0) instanceof List) == false ) {
                    listener.onFailure(new RuntimeException("ARRAY_DISTINCT expects an array argument"));
                }
                List<?> array = (List<?>) args.get(0);
                Set<Object> seen = new HashSet<>();
                List<Object> result = new ArrayList<>();
                for (Object e : array) {
                    if (seen.add(e)) {
                        result.add(e);
                    }
                }
                listener.onResponse(result);
            })
        );
    }

    // ==================== Phase 5: Functional Array Operations ====================

    @FunctionSpec(
        name = "ARRAY_JOIN",
        description = "Joins array elements into a single string with the specified delimiter.",
        parameters = {
            @FunctionParam(name = "array", type = "ARRAY", description = "The array to join"),
            @FunctionParam(name = "delimiter", type = "STRING", description = "The delimiter to use between elements")
        },
        returnType = @FunctionReturn(type = "STRING", description = "The joined string"),
        examples = {
            "ARRAY_JOIN(['a', 'b', 'c'], ', ') -> 'a, b, c'"
        },
        category = FunctionCategory.ARRAY
    )
    public static void registerArrayJoin(ExecutionContext context) {
        context.declareFunction("ARRAY_JOIN",
            Arrays.asList(
                new Parameter("array", "ARRAY", ParameterMode.IN),
                new Parameter("delimiter", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ARRAY_JOIN", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 2) {
                    listener.onFailure(new RuntimeException("ARRAY_JOIN expects two arguments: an array and a delimiter"));
                    return;
                }
                if (!(args.get(0) instanceof List)) {
                    listener.onFailure(new RuntimeException("ARRAY_JOIN expects the first argument to be an array"));
                    return;
                }
                List<?> array = (List<?>) args.get(0);
                String delimiter = args.get(1) != null ? args.get(1).toString() : "";
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < array.size(); i++) {
                    if (i > 0) sb.append(delimiter);
                    sb.append(array.get(i) != null ? array.get(i).toString() : "");
                }
                listener.onResponse(sb.toString());
            })
        );
    }

    @FunctionSpec(
        name = "ARRAY_FLATTEN",
        description = "Flattens a nested array by one level.",
        parameters = {
            @FunctionParam(name = "array", type = "ARRAY", description = "The nested array to flatten")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "The flattened array"),
        examples = {
            "ARRAY_FLATTEN([[1, 2], [3, 4]]) -> [1, 2, 3, 4]"
        },
        category = FunctionCategory.ARRAY
    )
    public static void registerArrayFlatten(ExecutionContext context) {
        context.declareFunction("ARRAY_FLATTEN",
            Collections.singletonList(new Parameter("array", "ARRAY", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ARRAY_FLATTEN", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 1) {
                    listener.onFailure(new RuntimeException("ARRAY_FLATTEN expects one argument"));
                    return;
                }
                if (!(args.get(0) instanceof List)) {
                    listener.onFailure(new RuntimeException("ARRAY_FLATTEN expects an array argument"));
                    return;
                }
                List<?> array = (List<?>) args.get(0);
                List<Object> result = new ArrayList<>();
                for (Object item : array) {
                    if (item instanceof List) {
                        result.addAll((List<?>) item);
                    } else {
                        result.add(item);
                    }
                }
                listener.onResponse(result);
            })
        );
    }

    @FunctionSpec(
        name = "ARRAY_REVERSE",
        description = "Returns a new array with elements in reverse order.",
        parameters = {
            @FunctionParam(name = "array", type = "ARRAY", description = "The array to reverse")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "The reversed array"),
        examples = {
            "ARRAY_REVERSE([1, 2, 3]) -> [3, 2, 1]"
        },
        category = FunctionCategory.ARRAY
    )
    public static void registerArrayReverse(ExecutionContext context) {
        context.declareFunction("ARRAY_REVERSE",
            Collections.singletonList(new Parameter("array", "ARRAY", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ARRAY_REVERSE", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 1) {
                    listener.onFailure(new RuntimeException("ARRAY_REVERSE expects one argument"));
                    return;
                }
                if (!(args.get(0) instanceof List)) {
                    listener.onFailure(new RuntimeException("ARRAY_REVERSE expects an array argument"));
                    return;
                }
                List<Object> array = new ArrayList<>((List<?>) args.get(0));
                Collections.reverse(array);
                listener.onResponse(array);
            })
        );
    }

    @FunctionSpec(
        name = "ARRAY_SLICE",
        description = "Returns a portion of the array from start index to end index (exclusive).",
        parameters = {
            @FunctionParam(name = "array", type = "ARRAY", description = "The array to slice"),
            @FunctionParam(name = "start", type = "NUMBER", description = "The start index (inclusive)"),
            @FunctionParam(name = "end", type = "NUMBER", description = "The end index (exclusive)")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "The sliced portion of the array"),
        examples = {
            "ARRAY_SLICE([1, 2, 3, 4, 5], 1, 4) -> [2, 3, 4]"
        },
        category = FunctionCategory.ARRAY
    )
    public static void registerArraySlice(ExecutionContext context) {
        context.declareFunction("ARRAY_SLICE",
            Arrays.asList(
                new Parameter("array", "ARRAY", ParameterMode.IN),
                new Parameter("start", "NUMBER", ParameterMode.IN),
                new Parameter("end", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ARRAY_SLICE", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 3) {
                    listener.onFailure(new RuntimeException("ARRAY_SLICE expects three arguments: array, start, end"));
                    return;
                }
                if (!(args.get(0) instanceof List)) {
                    listener.onFailure(new RuntimeException("ARRAY_SLICE expects the first argument to be an array"));
                    return;
                }
                List<?> array = (List<?>) args.get(0);
                int start = ((Number) args.get(1)).intValue();
                int end = ((Number) args.get(2)).intValue();
                start = Math.max(0, start);
                end = Math.min(array.size(), end);
                if (start >= end) {
                    listener.onResponse(new ArrayList<>());
                    return;
                }
                listener.onResponse(new ArrayList<>(array.subList(start, end)));
            })
        );
    }

    @FunctionSpec(
        name = "ARRAY_MAP",
        description = "Transforms array elements using a lambda or extracts a property.",
        parameters = {
            @FunctionParam(name = "array", type = "ARRAY", description = "The array to transform"),
            @FunctionParam(name = "mapperOrProperty", type = "ANY", description = "A lambda (x) => expr or property name")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Array of transformed values"),
        examples = {
            "ARRAY_MAP(users, (u) => u['name'])",
            "ARRAY_MAP(users, 'name')"
        },
        category = FunctionCategory.ARRAY
    )
    @SuppressWarnings("unchecked")
    public static void registerArrayMap(ExecutionContext context) {
        context.declareFunction("ARRAY_MAP",
            Arrays.asList(
                new Parameter("array", "ARRAY", ParameterMode.IN),
                new Parameter("mapperOrProperty", "ANY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ARRAY_MAP", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 2) {
                    listener.onFailure(new RuntimeException("ARRAY_MAP expects two arguments: an array and a mapper/property"));
                    return;
                }
                if (!(args.get(0) instanceof List)) {
                    listener.onFailure(new RuntimeException("ARRAY_MAP expects the first argument to be an array"));
                    return;
                }
                List<?> array = (List<?>) args.get(0);
                Object mapper = args.get(1);
                
                if (mapper instanceof LambdaExpression lambda) {
                    // Use lambda for mapping
                    if (lambda.getParameterCount() != 1) {
                        listener.onFailure(new RuntimeException("ARRAY_MAP lambda must accept exactly 1 parameter"));
                        return;
                    }
                    List<Object> result = new ArrayList<>();
                    mapWithLambdaAsync(array, lambda, 0, result, context, listener);
                } else {
                    // Use property name for mapping
                    String property = mapper != null ? mapper.toString() : "";
                    List<Object> result = new ArrayList<>();
                    for (Object item : array) {
                        if (item instanceof java.util.Map) {
                            result.add(((java.util.Map<String, Object>) item).get(property));
                        } else {
                            result.add(null);
                        }
                    }
                    listener.onResponse(result);
                }
            })
        );
    }
    
    private static void mapWithLambdaAsync(List<?> array, LambdaExpression lambda, int index, 
                                           List<Object> result, ExecutionContext context,
                                           ActionListener<Object> listener) {
        if (index >= array.size()) {
            listener.onResponse(result);
            return;
        }
        
        Object item = array.get(index);
        context.invokeLambda(lambda, List.of(item), new ActionListener<Object>() {
            @Override
            public void onResponse(Object mappedValue) {
                result.add(mappedValue);
                mapWithLambdaAsync(array, lambda, index + 1, result, context, listener);
            }
            
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    @FunctionSpec(
        name = "ARRAY_FILTER",
        description = "Filters array elements using a lambda predicate or property matching.",
        parameters = {
            @FunctionParam(name = "array", type = "ARRAY", description = "The array to filter"),
            @FunctionParam(name = "predicateOrProperty", type = "ANY", description = "A lambda (x) => boolean or property name"),
            @FunctionParam(name = "value", type = "ANY", description = "Value to match (only for property mode)")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Filtered array"),
        examples = {
            "ARRAY_FILTER(users, (u) => u['age'] >= 18)",
            "ARRAY_FILTER(users, 'status', 'active')"
        },
        category = FunctionCategory.ARRAY
    )
    @SuppressWarnings("unchecked")
    public static void registerArrayFilter(ExecutionContext context) {
        // Register 2-arg version for lambda filtering
        context.declareFunction("ARRAY_FILTER",
            Arrays.asList(
                new Parameter("array", "ARRAY", ParameterMode.IN),
                new Parameter("predicate", "ANY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ARRAY_FILTER", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 2) {
                    listener.onFailure(new RuntimeException("ARRAY_FILTER expects 2 arguments: array and lambda"));
                    return;
                }
                if (!(args.get(0) instanceof List)) {
                    listener.onFailure(new RuntimeException("ARRAY_FILTER expects the first argument to be an array"));
                    return;
                }
                List<?> array = (List<?>) args.get(0);
                Object predicate = args.get(1);
                
                if (predicate instanceof LambdaExpression lambda) {
                    // Use lambda for filtering
                    if (lambda.getParameterCount() != 1) {
                        listener.onFailure(new RuntimeException("ARRAY_FILTER lambda must accept exactly 1 parameter"));
                        return;
                    }
                    List<Object> result = new ArrayList<>();
                    filterWithLambdaAsync(array, lambda, 0, result, context, listener);
                } else {
                    listener.onFailure(new RuntimeException("ARRAY_FILTER with 2 arguments requires a lambda predicate. " +
                        "For property matching, use ARRAY_FILTER_BY(array, property, value)"));
                }
            })
        );
        
        // Register 3-arg version for property matching
        context.declareFunction("ARRAY_FILTER_BY",
            Arrays.asList(
                new Parameter("array", "ARRAY", ParameterMode.IN),
                new Parameter("property", "STRING", ParameterMode.IN),
                new Parameter("value", "ANY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ARRAY_FILTER_BY", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 3) {
                    listener.onFailure(new RuntimeException("ARRAY_FILTER_BY expects 3 arguments: array, property, value"));
                    return;
                }
                if (!(args.get(0) instanceof List)) {
                    listener.onFailure(new RuntimeException("ARRAY_FILTER_BY expects the first argument to be an array"));
                    return;
                }
                List<?> array = (List<?>) args.get(0);
                String property = args.get(1) != null ? args.get(1).toString() : "";
                Object matchValue = args.get(2);
                List<Object> result = new ArrayList<>();
                for (Object item : array) {
                    if (item instanceof java.util.Map) {
                        Object propValue = ((java.util.Map<String, Object>) item).get(property);
                        if (valuesEqual(propValue, matchValue)) {
                            result.add(item);
                        }
                    }
                }
                listener.onResponse(result);
            })
        );
    }
    
    private static void filterWithLambdaAsync(List<?> array, LambdaExpression lambda, int index, 
                                              List<Object> result, ExecutionContext context,
                                              ActionListener<Object> listener) {
        if (index >= array.size()) {
            listener.onResponse(result);
            return;
        }
        
        Object item = array.get(index);
        context.invokeLambda(lambda, List.of(item), new ActionListener<Object>() {
            @Override
            public void onResponse(Object predicateResult) {
                boolean include = false;
                if (predicateResult instanceof Boolean) {
                    include = (Boolean) predicateResult;
                } else if (predicateResult != null) {
                    include = true; // truthy
                }
                if (include) {
                    result.add(item);
                }
                filterWithLambdaAsync(array, lambda, index + 1, result, context, listener);
            }
            
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    @FunctionSpec(
        name = "ARRAY_REDUCE",
        description = "Reduces array to a single value by summing all numeric elements (or concatenating strings).",
        parameters = {
            @FunctionParam(name = "array", type = "ARRAY", description = "The array to reduce"),
            @FunctionParam(name = "initial", type = "ANY", description = "The initial accumulator value")
        },
        returnType = @FunctionReturn(type = "ANY", description = "The reduced value"),
        examples = {
            "ARRAY_REDUCE([1, 2, 3], 0) -> 6"
        },
        category = FunctionCategory.ARRAY
    )
    public static void registerArrayReduce(ExecutionContext context) {
        context.declareFunction("ARRAY_REDUCE",
            Arrays.asList(
                new Parameter("array", "ARRAY", ParameterMode.IN),
                new Parameter("initial", "ANY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ARRAY_REDUCE", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 2) {
                    listener.onFailure(new RuntimeException("ARRAY_REDUCE expects two arguments: array and initial value"));
                    return;
                }
                if (!(args.get(0) instanceof List)) {
                    listener.onFailure(new RuntimeException("ARRAY_REDUCE expects the first argument to be an array"));
                    return;
                }
                List<?> array = (List<?>) args.get(0);
                Object acc = args.get(1);
                
                // Auto-detect operation based on initial value type
                if (acc instanceof Number) {
                    double result = ((Number) acc).doubleValue();
                    for (Object item : array) {
                        if (item instanceof Number) {
                            result += ((Number) item).doubleValue();
                        }
                    }
                    listener.onResponse(result);
                } else if (acc instanceof String) {
                    StringBuilder sb = new StringBuilder(acc.toString());
                    for (Object item : array) {
                        sb.append(item != null ? item.toString() : "");
                    }
                    listener.onResponse(sb.toString());
                } else {
                    listener.onFailure(new RuntimeException("ARRAY_REDUCE initial value must be NUMBER or STRING"));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ARRAY_FIND",
        description = "Finds the first element where the specified property equals the given value.",
        parameters = {
            @FunctionParam(name = "array", type = "ARRAY", description = "The array to search"),
            @FunctionParam(name = "property", type = "STRING", description = "The property name to check"),
            @FunctionParam(name = "value", type = "ANY", description = "The value to match")
        },
        returnType = @FunctionReturn(type = "ANY", description = "The first matching element, or null"),
        examples = {
            "ARRAY_FIND([{'id': 1}, {'id': 2}], 'id', 2) -> {'id': 2}"
        },
        category = FunctionCategory.ARRAY
    )
    @SuppressWarnings("unchecked")
    public static void registerArrayFind(ExecutionContext context) {
        context.declareFunction("ARRAY_FIND",
            Arrays.asList(
                new Parameter("array", "ARRAY", ParameterMode.IN),
                new Parameter("property", "STRING", ParameterMode.IN),
                new Parameter("value", "ANY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ARRAY_FIND", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 3) {
                    listener.onFailure(new RuntimeException("ARRAY_FIND expects three arguments"));
                    return;
                }
                if (!(args.get(0) instanceof List)) {
                    listener.onFailure(new RuntimeException("ARRAY_FIND expects the first argument to be an array"));
                    return;
                }
                List<?> array = (List<?>) args.get(0);
                String property = args.get(1) != null ? args.get(1).toString() : "";
                Object matchValue = args.get(2);
                for (Object item : array) {
                    if (item instanceof java.util.Map) {
                        Object propValue = ((java.util.Map<String, Object>) item).get(property);
                        if (valuesEqual(propValue, matchValue)) {
                            listener.onResponse(item);
                            return;
                        }
                    }
                }
                listener.onResponse(null);
            })
        );
    }

    @FunctionSpec(
        name = "ARRAY_FIND_INDEX",
        description = "Finds the index of the first element where the specified property equals the given value.",
        parameters = {
            @FunctionParam(name = "array", type = "ARRAY", description = "The array to search"),
            @FunctionParam(name = "property", type = "STRING", description = "The property name to check"),
            @FunctionParam(name = "value", type = "ANY", description = "The value to match")
        },
        returnType = @FunctionReturn(type = "NUMBER", description = "The index of the first match, or -1"),
        examples = {
            "ARRAY_FIND_INDEX([{'id': 1}, {'id': 2}], 'id', 2) -> 1"
        },
        category = FunctionCategory.ARRAY
    )
    @SuppressWarnings("unchecked")
    public static void registerArrayFindIndex(ExecutionContext context) {
        context.declareFunction("ARRAY_FIND_INDEX",
            Arrays.asList(
                new Parameter("array", "ARRAY", ParameterMode.IN),
                new Parameter("property", "STRING", ParameterMode.IN),
                new Parameter("value", "ANY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ARRAY_FIND_INDEX", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 3) {
                    listener.onFailure(new RuntimeException("ARRAY_FIND_INDEX expects three arguments"));
                    return;
                }
                if (!(args.get(0) instanceof List)) {
                    listener.onFailure(new RuntimeException("ARRAY_FIND_INDEX expects the first argument to be an array"));
                    return;
                }
                List<?> array = (List<?>) args.get(0);
                String property = args.get(1) != null ? args.get(1).toString() : "";
                Object matchValue = args.get(2);
                for (int i = 0; i < array.size(); i++) {
                    Object item = array.get(i);
                    if (item instanceof java.util.Map) {
                        Object propValue = ((java.util.Map<String, Object>) item).get(property);
                        if (valuesEqual(propValue, matchValue)) {
                            listener.onResponse(i);
                            return;
                        }
                    }
                }
                listener.onResponse(-1);
            })
        );
    }

    @FunctionSpec(
        name = "ARRAY_EVERY",
        description = "Returns true if all elements have the specified property equal to the given value.",
        parameters = {
            @FunctionParam(name = "array", type = "ARRAY", description = "The array to check"),
            @FunctionParam(name = "property", type = "STRING", description = "The property name to check"),
            @FunctionParam(name = "value", type = "ANY", description = "The value to match")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if all match"),
        examples = {
            "ARRAY_EVERY([{'active': true}, {'active': true}], 'active', true) -> true"
        },
        category = FunctionCategory.ARRAY
    )
    @SuppressWarnings("unchecked")
    public static void registerArrayEvery(ExecutionContext context) {
        context.declareFunction("ARRAY_EVERY",
            Arrays.asList(
                new Parameter("array", "ARRAY", ParameterMode.IN),
                new Parameter("property", "STRING", ParameterMode.IN),
                new Parameter("value", "ANY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ARRAY_EVERY", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 3) {
                    listener.onFailure(new RuntimeException("ARRAY_EVERY expects three arguments"));
                    return;
                }
                if (!(args.get(0) instanceof List)) {
                    listener.onFailure(new RuntimeException("ARRAY_EVERY expects the first argument to be an array"));
                    return;
                }
                List<?> array = (List<?>) args.get(0);
                String property = args.get(1) != null ? args.get(1).toString() : "";
                Object matchValue = args.get(2);
                for (Object item : array) {
                    if (item instanceof java.util.Map) {
                        Object propValue = ((java.util.Map<String, Object>) item).get(property);
                        if (!valuesEqual(propValue, matchValue)) {
                            listener.onResponse(false);
                            return;
                        }
                    } else {
                        listener.onResponse(false);
                        return;
                    }
                }
                listener.onResponse(true);
            })
        );
    }

    @FunctionSpec(
        name = "ARRAY_SOME",
        description = "Returns true if at least one element has the specified property equal to the given value.",
        parameters = {
            @FunctionParam(name = "array", type = "ARRAY", description = "The array to check"),
            @FunctionParam(name = "property", type = "STRING", description = "The property name to check"),
            @FunctionParam(name = "value", type = "ANY", description = "The value to match")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if any match"),
        examples = {
            "ARRAY_SOME([{'active': false}, {'active': true}], 'active', true) -> true"
        },
        category = FunctionCategory.ARRAY
    )
    @SuppressWarnings("unchecked")
    public static void registerArraySome(ExecutionContext context) {
        context.declareFunction("ARRAY_SOME",
            Arrays.asList(
                new Parameter("array", "ARRAY", ParameterMode.IN),
                new Parameter("property", "STRING", ParameterMode.IN),
                new Parameter("value", "ANY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ARRAY_SOME", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 3) {
                    listener.onFailure(new RuntimeException("ARRAY_SOME expects three arguments"));
                    return;
                }
                if (!(args.get(0) instanceof List)) {
                    listener.onFailure(new RuntimeException("ARRAY_SOME expects the first argument to be an array"));
                    return;
                }
                List<?> array = (List<?>) args.get(0);
                String property = args.get(1) != null ? args.get(1).toString() : "";
                Object matchValue = args.get(2);
                for (Object item : array) {
                    if (item instanceof java.util.Map) {
                        Object propValue = ((java.util.Map<String, Object>) item).get(property);
                        if (valuesEqual(propValue, matchValue)) {
                            listener.onResponse(true);
                            return;
                        }
                    }
                }
                listener.onResponse(false);
            })
        );
    }

    @FunctionSpec(
        name = "ARRAY_SORT",
        description = "Returns a new array sorted by the specified property (ascending).",
        parameters = {
            @FunctionParam(name = "array", type = "ARRAY", description = "The array to sort"),
            @FunctionParam(name = "property", type = "STRING", description = "The property name to sort by (optional for primitive arrays)")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "The sorted array"),
        examples = {
            "ARRAY_SORT([{'age': 30}, {'age': 20}], 'age') -> [{'age': 20}, {'age': 30}]"
        },
        category = FunctionCategory.ARRAY
    )
    @SuppressWarnings("unchecked")
    public static void registerArraySort(ExecutionContext context) {
        context.declareFunction("ARRAY_SORT",
            Arrays.asList(
                new Parameter("array", "ARRAY", ParameterMode.IN),
                new Parameter("property", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ARRAY_SORT", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() < 1) {
                    listener.onFailure(new RuntimeException("ARRAY_SORT expects at least one argument"));
                    return;
                }
                if (!(args.get(0) instanceof List)) {
                    listener.onFailure(new RuntimeException("ARRAY_SORT expects the first argument to be an array"));
                    return;
                }
                List<Object> array = new ArrayList<>((List<?>) args.get(0));
                String property = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : null;
                
                if (property != null && !property.isEmpty()) {
                    // Sort by property
                    array.sort((a, b) -> {
                        if (!(a instanceof java.util.Map) || !(b instanceof java.util.Map)) return 0;
                        Object valA = ((java.util.Map<String, Object>) a).get(property);
                        Object valB = ((java.util.Map<String, Object>) b).get(property);
                        return compareValues(valA, valB);
                    });
                } else {
                    // Sort primitive values
                    array.sort((a, b) -> compareValues(a, b));
                }
                listener.onResponse(array);
            })
        );
    }

    // Helper method for value comparison
    private static boolean valuesEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a instanceof Number && b instanceof Number) {
            return ((Number) a).doubleValue() == ((Number) b).doubleValue();
        }
        return a.equals(b);
    }

    // Helper method for sorting comparison
    @SuppressWarnings("unchecked")
    private static int compareValues(Object a, Object b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        if (a instanceof Number && b instanceof Number) {
            return Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
        }
        if (a instanceof String && b instanceof String) {
            return ((String) a).compareTo((String) b);
        }
        if (a instanceof Comparable && b instanceof Comparable) {
            return ((Comparable<Object>) a).compareTo(b);
        }
        return 0;
    }
}
