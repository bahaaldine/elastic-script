/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.builtin.datatypes;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Built-in functions for MAP (associative array) operations.
 * 
 * MAP is a key-value data structure where keys can be strings or numbers,
 * and values can be any type.
 * 
 * <h2>Available Functions</h2>
 * <ul>
 *   <li>{@code MAP_GET(map, key)} - Get value by key</li>
 *   <li>{@code MAP_PUT(map, key, value)} - Return new map with key-value added</li>
 *   <li>{@code MAP_REMOVE(map, key)} - Return new map with key removed</li>
 *   <li>{@code MAP_KEYS(map)} - Return array of all keys</li>
 *   <li>{@code MAP_VALUES(map)} - Return array of all values</li>
 *   <li>{@code MAP_SIZE(map)} - Return number of entries</li>
 *   <li>{@code MAP_CONTAINS_KEY(map, key)} - Check if key exists</li>
 *   <li>{@code MAP_CONTAINS_VALUE(map, value)} - Check if value exists</li>
 *   <li>{@code MAP_MERGE(map1, map2)} - Merge two maps (map2 overwrites)</li>
 *   <li>{@code MAP_FROM_ARRAYS(keys, values)} - Create map from two arrays</li>
 *   <li>{@code MAP_ENTRIES(map)} - Return array of {key, value} documents</li>
 * </ul>
 */
public class MapBuiltInFunctions {

    /**
     * Registers all MAP functions in the execution context.
     */
    public static void registerAll(ExecutionContext context) {
        registerMapGet(context);
        registerMapPut(context);
        registerMapRemove(context);
        registerMapKeys(context);
        registerMapValues(context);
        registerMapSize(context);
        registerMapContainsKey(context);
        registerMapContainsValue(context);
        registerMapMerge(context);
        registerMapFromArrays(context);
        registerMapEntries(context);
        registerMapGetOrDefault(context);
    }

    private static void registerMapGet(ExecutionContext context) {
        BuiltInFunctionDefinition func = new BuiltInFunctionDefinition("MAP_GET", (args, listener) -> {
            Map<?, ?> map = args.get(0) instanceof Map ? (Map<?, ?>) args.get(0) : null;
            Object key = args.get(1);
            if (map == null) {
                listener.onResponse(null);
                return;
            }
            Object value = map.get(key);
            if (value == null && key != null) {
                value = map.get(String.valueOf(key));
            }
            listener.onResponse(value);
        });
        func.setParameters(Arrays.asList(
            new Parameter("map", "MAP", ParameterMode.IN),
            new Parameter("key", "ANY", ParameterMode.IN)
        ));
        context.declareFunction("MAP_GET", func);
    }

    @SuppressWarnings("unchecked")
    private static void registerMapPut(ExecutionContext context) {
        BuiltInFunctionDefinition func = new BuiltInFunctionDefinition("MAP_PUT", (args, listener) -> {
            Map<?, ?> map = args.get(0) instanceof Map ? (Map<?, ?>) args.get(0) : null;
            Object key = args.get(1);
            Object value = args.get(2);
            Map<Object, Object> result = new LinkedHashMap<>();
            if (map != null) {
                result.putAll((Map<Object, Object>) map);
            }
            result.put(key, value);
            listener.onResponse(result);
        });
        func.setParameters(Arrays.asList(
            new Parameter("map", "MAP", ParameterMode.IN),
            new Parameter("key", "ANY", ParameterMode.IN),
            new Parameter("value", "ANY", ParameterMode.IN)
        ));
        context.declareFunction("MAP_PUT", func);
    }

    @SuppressWarnings("unchecked")
    private static void registerMapRemove(ExecutionContext context) {
        BuiltInFunctionDefinition func = new BuiltInFunctionDefinition("MAP_REMOVE", (args, listener) -> {
            Map<?, ?> map = args.get(0) instanceof Map ? (Map<?, ?>) args.get(0) : null;
            Object key = args.get(1);
            Map<Object, Object> result = new LinkedHashMap<>();
            if (map != null) {
                result.putAll((Map<Object, Object>) map);
            }
            result.remove(key);
            if (key != null) {
                result.remove(String.valueOf(key));
            }
            listener.onResponse(result);
        });
        func.setParameters(Arrays.asList(
            new Parameter("map", "MAP", ParameterMode.IN),
            new Parameter("key", "ANY", ParameterMode.IN)
        ));
        context.declareFunction("MAP_REMOVE", func);
    }

    private static void registerMapKeys(ExecutionContext context) {
        BuiltInFunctionDefinition func = new BuiltInFunctionDefinition("MAP_KEYS", (args, listener) -> {
            Map<?, ?> map = args.get(0) instanceof Map ? (Map<?, ?>) args.get(0) : null;
            listener.onResponse(map == null ? new ArrayList<>() : new ArrayList<>(map.keySet()));
        });
        func.setParameters(Collections.singletonList(new Parameter("map", "MAP", ParameterMode.IN)));
        context.declareFunction("MAP_KEYS", func);
    }

    private static void registerMapValues(ExecutionContext context) {
        BuiltInFunctionDefinition func = new BuiltInFunctionDefinition("MAP_VALUES", (args, listener) -> {
            Map<?, ?> map = args.get(0) instanceof Map ? (Map<?, ?>) args.get(0) : null;
            listener.onResponse(map == null ? new ArrayList<>() : new ArrayList<>(map.values()));
        });
        func.setParameters(Collections.singletonList(new Parameter("map", "MAP", ParameterMode.IN)));
        context.declareFunction("MAP_VALUES", func);
    }

    private static void registerMapSize(ExecutionContext context) {
        BuiltInFunctionDefinition func = new BuiltInFunctionDefinition("MAP_SIZE", (args, listener) -> {
            Map<?, ?> map = args.get(0) instanceof Map ? (Map<?, ?>) args.get(0) : null;
            listener.onResponse(map == null ? 0 : map.size());
        });
        func.setParameters(Collections.singletonList(new Parameter("map", "MAP", ParameterMode.IN)));
        context.declareFunction("MAP_SIZE", func);
    }

    private static void registerMapContainsKey(ExecutionContext context) {
        BuiltInFunctionDefinition func = new BuiltInFunctionDefinition("MAP_CONTAINS_KEY", (args, listener) -> {
            Map<?, ?> map = args.get(0) instanceof Map ? (Map<?, ?>) args.get(0) : null;
            Object key = args.get(1);
            if (map == null) {
                listener.onResponse(false);
                return;
            }
            boolean contains = map.containsKey(key);
            if (!contains && key != null) {
                contains = map.containsKey(String.valueOf(key));
            }
            listener.onResponse(contains);
        });
        func.setParameters(Arrays.asList(
            new Parameter("map", "MAP", ParameterMode.IN),
            new Parameter("key", "ANY", ParameterMode.IN)
        ));
        context.declareFunction("MAP_CONTAINS_KEY", func);
    }

    private static void registerMapContainsValue(ExecutionContext context) {
        BuiltInFunctionDefinition func = new BuiltInFunctionDefinition("MAP_CONTAINS_VALUE", (args, listener) -> {
            Map<?, ?> map = args.get(0) instanceof Map ? (Map<?, ?>) args.get(0) : null;
            Object value = args.get(1);
            listener.onResponse(map != null && map.containsValue(value));
        });
        func.setParameters(Arrays.asList(
            new Parameter("map", "MAP", ParameterMode.IN),
            new Parameter("value", "ANY", ParameterMode.IN)
        ));
        context.declareFunction("MAP_CONTAINS_VALUE", func);
    }

    @SuppressWarnings("unchecked")
    private static void registerMapMerge(ExecutionContext context) {
        BuiltInFunctionDefinition func = new BuiltInFunctionDefinition("MAP_MERGE", (args, listener) -> {
            Map<?, ?> map1 = args.get(0) instanceof Map ? (Map<?, ?>) args.get(0) : null;
            Map<?, ?> map2 = args.get(1) instanceof Map ? (Map<?, ?>) args.get(1) : null;
            Map<Object, Object> result = new LinkedHashMap<>();
            if (map1 != null) {
                result.putAll((Map<Object, Object>) map1);
            }
            if (map2 != null) {
                result.putAll((Map<Object, Object>) map2);
            }
            listener.onResponse(result);
        });
        func.setParameters(Arrays.asList(
            new Parameter("map1", "MAP", ParameterMode.IN),
            new Parameter("map2", "MAP", ParameterMode.IN)
        ));
        context.declareFunction("MAP_MERGE", func);
    }

    private static void registerMapFromArrays(ExecutionContext context) {
        BuiltInFunctionDefinition func = new BuiltInFunctionDefinition("MAP_FROM_ARRAYS", (args, listener) -> {
            List<?> keys = args.get(0) instanceof List ? (List<?>) args.get(0) : null;
            List<?> values = args.get(1) instanceof List ? (List<?>) args.get(1) : null;
            if (keys == null || values == null) {
                listener.onResponse(new LinkedHashMap<>());
                return;
            }
            if (keys.size() != values.size()) {
                listener.onFailure(new RuntimeException(
                    "Keys and values arrays must have the same length"));
                return;
            }
            Map<Object, Object> result = new LinkedHashMap<>();
            for (int i = 0; i < keys.size(); i++) {
                result.put(keys.get(i), values.get(i));
            }
            listener.onResponse(result);
        });
        func.setParameters(Arrays.asList(
            new Parameter("keys", "ARRAY", ParameterMode.IN),
            new Parameter("values", "ARRAY", ParameterMode.IN)
        ));
        context.declareFunction("MAP_FROM_ARRAYS", func);
    }

    private static void registerMapEntries(ExecutionContext context) {
        BuiltInFunctionDefinition func = new BuiltInFunctionDefinition("MAP_ENTRIES", (args, listener) -> {
            Map<?, ?> map = args.get(0) instanceof Map ? (Map<?, ?>) args.get(0) : null;
            if (map == null) {
                listener.onResponse(new ArrayList<>());
                return;
            }
            List<Map<String, Object>> entries = new ArrayList<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Map<String, Object> entryDoc = new LinkedHashMap<>();
                entryDoc.put("key", entry.getKey());
                entryDoc.put("value", entry.getValue());
                entries.add(entryDoc);
            }
            listener.onResponse(entries);
        });
        func.setParameters(Collections.singletonList(new Parameter("map", "MAP", ParameterMode.IN)));
        context.declareFunction("MAP_ENTRIES", func);
    }

    private static void registerMapGetOrDefault(ExecutionContext context) {
        BuiltInFunctionDefinition func = new BuiltInFunctionDefinition("MAP_GET_OR_DEFAULT", (args, listener) -> {
            Map<?, ?> map = args.get(0) instanceof Map ? (Map<?, ?>) args.get(0) : null;
            Object key = args.get(1);
            Object defaultValue = args.get(2);
            if (map == null) {
                listener.onResponse(defaultValue);
                return;
            }
            Object value = map.get(key);
            if (value == null && key != null) {
                value = map.get(String.valueOf(key));
            }
            listener.onResponse(value != null ? value : defaultValue);
        });
        func.setParameters(Arrays.asList(
            new Parameter("map", "MAP", ParameterMode.IN),
            new Parameter("key", "ANY", ParameterMode.IN),
            new Parameter("defaultValue", "ANY", ParameterMode.IN)
        ));
        context.declareFunction("MAP_GET_OR_DEFAULT", func);
    }
}
