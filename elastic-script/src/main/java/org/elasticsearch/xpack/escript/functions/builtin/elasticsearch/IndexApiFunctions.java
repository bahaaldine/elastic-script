/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.builtin.elasticsearch;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;
import org.elasticsearch.xpack.escript.functions.api.FunctionCategory;
import org.elasticsearch.xpack.escript.functions.api.FunctionCollectionSpec;
import org.elasticsearch.xpack.escript.functions.api.FunctionParam;
import org.elasticsearch.xpack.escript.functions.api.FunctionReturn;
import org.elasticsearch.xpack.escript.functions.api.FunctionSpec;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch Index Management API functions using REST HTTP calls.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.DATASOURCE,
    description = "Elasticsearch Index Management API functions."
)
public class IndexApiFunctions extends ElasticsearchHttpFunctions {

    public static void registerAll(ExecutionContext context) {
        registerCreateIndex(context);
        registerDeleteIndex(context);
        registerIndexExists(context);
        registerGetMapping(context);
        registerPutMapping(context);
        registerGetSettings(context);
        registerPutSettings(context);
        registerRefreshIndex(context);
        registerFlushIndex(context);
        registerForceMerge(context);
        registerClearCache(context);
        registerOpenIndex(context);
        registerCloseIndex(context);
        registerGetAlias(context);
        registerPutAlias(context);
        registerDeleteAlias(context);
        registerIndexStats(context);
        registerCloneIndex(context);
        registerShrinkIndex(context);
        registerRolloverIndex(context);
        registerResolveIndex(context);
        registerGetIndexTemplate(context);
        registerPutIndexTemplate(context);
    }

    @FunctionSpec(
        name = "ES_CREATE_INDEX",
        description = "Create a new index",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "settings", type = "DOCUMENT", description = "Index settings"),
            @FunctionParam(name = "mappings", type = "DOCUMENT", description = "Index mappings")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Create index response"),
        examples = {"ES_CREATE_INDEX('my-index', {'number_of_shards': 1}, {'properties': {'field': {'type': 'text'}}})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerCreateIndex(ExecutionContext context) {
        context.declareFunction("ES_CREATE_INDEX",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("settings", "DOCUMENT", ParameterMode.IN),
                new Parameter("mappings", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_CREATE_INDEX", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    Map<String, Object> settings = args.size() > 1 && args.get(1) != null ? toMap(args.get(1)) : null;
                    Map<String, Object> mappings = args.size() > 2 && args.get(2) != null ? toMap(args.get(2)) : null;
                    
                    Map<String, Object> body = new HashMap<>();
                    if (settings != null && !settings.isEmpty()) body.put("settings", settings);
                    if (mappings != null && !mappings.isEmpty()) body.put("mappings", mappings);
                    
                    Map<String, Object> result = esRequest("PUT", "/" + index, body);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_CREATE_INDEX failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_CREATE_INDEX failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_DELETE_INDEX",
        description = "Delete an index",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name or pattern")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Delete response"),
        examples = {"ES_DELETE_INDEX('old-index')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDeleteIndex(ExecutionContext context) {
        context.declareFunction("ES_DELETE_INDEX",
            List.of(new Parameter("index", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_DELETE_INDEX", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    Map<String, Object> result = esRequest("DELETE", "/" + index, null);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_DELETE_INDEX failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_DELETE_INDEX failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_INDEX_EXISTS",
        description = "Check if an index exists",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if index exists"),
        examples = {"ES_INDEX_EXISTS('my-index')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerIndexExists(ExecutionContext context) {
        context.declareFunction("ES_INDEX_EXISTS",
            List.of(new Parameter("index", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_INDEX_EXISTS", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    Map<String, Object> result = esRequest("HEAD", "/" + index, null);
                    listener.onResponse(result.get("success").equals(true));
                } catch (Exception e) {
                    listener.onResponse(false);
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_GET_MAPPING",
        description = "Get index mappings",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name or pattern")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Mappings"),
        examples = {"ES_GET_MAPPING('my-index')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetMapping(ExecutionContext context) {
        context.declareFunction("ES_GET_MAPPING",
            List.of(new Parameter("index", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_GET_MAPPING", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    Map<String, Object> result = esRequest("GET", "/" + index + "/_mapping", null);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_GET_MAPPING failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_GET_MAPPING failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_PUT_MAPPING",
        description = "Update index mappings",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "mappings", type = "DOCUMENT", description = "Mapping properties")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Update response"),
        examples = {"ES_PUT_MAPPING('my-index', {'properties': {'new_field': {'type': 'keyword'}}})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerPutMapping(ExecutionContext context) {
        context.declareFunction("ES_PUT_MAPPING",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("mappings", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_PUT_MAPPING", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    Map<String, Object> mappings = toMap(args.get(1));
                    
                    Map<String, Object> result = esRequest("PUT", "/" + index + "/_mapping", mappings);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_PUT_MAPPING failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_PUT_MAPPING failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_GET_SETTINGS",
        description = "Get index settings",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name or pattern")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Settings"),
        examples = {"ES_GET_SETTINGS('my-index')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetSettings(ExecutionContext context) {
        context.declareFunction("ES_GET_SETTINGS",
            List.of(new Parameter("index", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_GET_SETTINGS", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    Map<String, Object> result = esRequest("GET", "/" + index + "/_settings", null);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_GET_SETTINGS failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_GET_SETTINGS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_PUT_SETTINGS",
        description = "Update index settings",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "settings", type = "DOCUMENT", description = "Settings to update")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Update response"),
        examples = {"ES_PUT_SETTINGS('my-index', {'index': {'refresh_interval': '30s'}})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerPutSettings(ExecutionContext context) {
        context.declareFunction("ES_PUT_SETTINGS",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("settings", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_PUT_SETTINGS", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    Map<String, Object> settings = toMap(args.get(1));
                    
                    Map<String, Object> result = esRequest("PUT", "/" + index + "/_settings", settings);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_PUT_SETTINGS failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_PUT_SETTINGS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_REFRESH_INDEX",
        description = "Refresh an index to make recent changes searchable",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name or pattern")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Refresh response"),
        examples = {"ES_REFRESH_INDEX('my-index')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerRefreshIndex(ExecutionContext context) {
        context.declareFunction("ES_REFRESH_INDEX",
            List.of(new Parameter("index", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_REFRESH_INDEX", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    Map<String, Object> result = esRequest("POST", "/" + index + "/_refresh", null);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_REFRESH_INDEX failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_REFRESH_INDEX failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_FLUSH_INDEX",
        description = "Flush an index",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name or pattern")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Flush response"),
        examples = {"ES_FLUSH_INDEX('my-index')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerFlushIndex(ExecutionContext context) {
        context.declareFunction("ES_FLUSH_INDEX",
            List.of(new Parameter("index", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_FLUSH_INDEX", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    Map<String, Object> result = esRequest("POST", "/" + index + "/_flush", null);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_FLUSH_INDEX failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_FLUSH_INDEX failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_FORCE_MERGE",
        description = "Force merge index segments",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "max_num_segments", type = "NUMBER", description = "Target number of segments")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Force merge response"),
        examples = {"ES_FORCE_MERGE('my-index', 1)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerForceMerge(ExecutionContext context) {
        context.declareFunction("ES_FORCE_MERGE",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("max_num_segments", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_FORCE_MERGE", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    int maxNumSegments = toInt(args.get(1), 1);
                    
                    String path = "/" + index + "/_forcemerge?max_num_segments=" + maxNumSegments;
                    Map<String, Object> result = esRequest("POST", path, null);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_FORCE_MERGE failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_FORCE_MERGE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_CLEAR_CACHE",
        description = "Clear index caches",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name or pattern")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Clear cache response"),
        examples = {"ES_CLEAR_CACHE('my-index')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerClearCache(ExecutionContext context) {
        context.declareFunction("ES_CLEAR_CACHE",
            List.of(new Parameter("index", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_CLEAR_CACHE", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    Map<String, Object> result = esRequest("POST", "/" + index + "/_cache/clear", null);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_CLEAR_CACHE failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_CLEAR_CACHE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_OPEN_INDEX",
        description = "Open a closed index",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Open response"),
        examples = {"ES_OPEN_INDEX('my-index')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerOpenIndex(ExecutionContext context) {
        context.declareFunction("ES_OPEN_INDEX",
            List.of(new Parameter("index", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_OPEN_INDEX", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    Map<String, Object> result = esRequest("POST", "/" + index + "/_open", null);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_OPEN_INDEX failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_OPEN_INDEX failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_CLOSE_INDEX",
        description = "Close an index",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Close response"),
        examples = {"ES_CLOSE_INDEX('my-index')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerCloseIndex(ExecutionContext context) {
        context.declareFunction("ES_CLOSE_INDEX",
            List.of(new Parameter("index", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_CLOSE_INDEX", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    Map<String, Object> result = esRequest("POST", "/" + index + "/_close", null);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_CLOSE_INDEX failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_CLOSE_INDEX failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_GET_ALIAS",
        description = "Get index aliases",
        parameters = {
            @FunctionParam(name = "alias", type = "STRING", description = "Alias name or pattern")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Alias information"),
        examples = {"ES_GET_ALIAS('my-alias')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetAlias(ExecutionContext context) {
        context.declareFunction("ES_GET_ALIAS",
            List.of(new Parameter("alias", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_GET_ALIAS", (args, listener) -> {
                try {
                    String alias = toString(args.get(0));
                    Map<String, Object> result = esRequest("GET", "/_alias/" + alias, null);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_GET_ALIAS failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_GET_ALIAS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_PUT_ALIAS",
        description = "Create or update an alias",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "alias", type = "STRING", description = "Alias name"),
            @FunctionParam(name = "filter", type = "DOCUMENT", description = "Optional filter query")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Alias response"),
        examples = {"ES_PUT_ALIAS('my-index', 'my-alias', null)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerPutAlias(ExecutionContext context) {
        context.declareFunction("ES_PUT_ALIAS",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("alias", "STRING", ParameterMode.IN),
                new Parameter("filter", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_PUT_ALIAS", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    String alias = toString(args.get(1));
                    Map<String, Object> filter = args.size() > 2 && args.get(2) != null ? toMap(args.get(2)) : null;
                    
                    Map<String, Object> body = new HashMap<>();
                    if (filter != null && !filter.isEmpty()) {
                        body.put("filter", filter);
                    }
                    
                    Map<String, Object> result = esRequest("PUT", "/" + index + "/_alias/" + alias, body.isEmpty() ? null : body);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_PUT_ALIAS failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_PUT_ALIAS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_DELETE_ALIAS",
        description = "Delete an alias",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "alias", type = "STRING", description = "Alias name")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Delete response"),
        examples = {"ES_DELETE_ALIAS('my-index', 'my-alias')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDeleteAlias(ExecutionContext context) {
        context.declareFunction("ES_DELETE_ALIAS",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("alias", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_DELETE_ALIAS", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    String alias = toString(args.get(1));
                    
                    Map<String, Object> result = esRequest("DELETE", "/" + index + "/_alias/" + alias, null);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_DELETE_ALIAS failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_DELETE_ALIAS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_INDEX_STATS",
        description = "Get index statistics",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name or pattern")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Index stats"),
        examples = {"ES_INDEX_STATS('my-index')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerIndexStats(ExecutionContext context) {
        context.declareFunction("ES_INDEX_STATS",
            List.of(new Parameter("index", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_INDEX_STATS", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    Map<String, Object> result = esRequest("GET", "/" + index + "/_stats", null);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_INDEX_STATS failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_INDEX_STATS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_CLONE_INDEX",
        description = "Clone an index",
        parameters = {
            @FunctionParam(name = "source", type = "STRING", description = "Source index"),
            @FunctionParam(name = "target", type = "STRING", description = "Target index name")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Clone response"),
        examples = {"ES_CLONE_INDEX('source-index', 'cloned-index')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerCloneIndex(ExecutionContext context) {
        context.declareFunction("ES_CLONE_INDEX",
            List.of(
                new Parameter("source", "STRING", ParameterMode.IN),
                new Parameter("target", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_CLONE_INDEX", (args, listener) -> {
                try {
                    String source = toString(args.get(0));
                    String target = toString(args.get(1));
                    
                    Map<String, Object> result = esRequest("POST", "/" + source + "/_clone/" + target, null);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_CLONE_INDEX failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_CLONE_INDEX failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_SHRINK_INDEX",
        description = "Shrink an index to fewer shards",
        parameters = {
            @FunctionParam(name = "source", type = "STRING", description = "Source index"),
            @FunctionParam(name = "target", type = "STRING", description = "Target index name"),
            @FunctionParam(name = "settings", type = "DOCUMENT", description = "Target settings")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Shrink response"),
        examples = {"ES_SHRINK_INDEX('source-index', 'shrunk-index', {'number_of_shards': 1})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerShrinkIndex(ExecutionContext context) {
        context.declareFunction("ES_SHRINK_INDEX",
            List.of(
                new Parameter("source", "STRING", ParameterMode.IN),
                new Parameter("target", "STRING", ParameterMode.IN),
                new Parameter("settings", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_SHRINK_INDEX", (args, listener) -> {
                try {
                    String source = toString(args.get(0));
                    String target = toString(args.get(1));
                    Map<String, Object> settings = args.size() > 2 && args.get(2) != null ? toMap(args.get(2)) : null;
                    
                    Map<String, Object> body = new HashMap<>();
                    if (settings != null && !settings.isEmpty()) {
                        body.put("settings", settings);
                    }
                    
                    Map<String, Object> result = esRequest("POST", "/" + source + "/_shrink/" + target, body.isEmpty() ? null : body);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_SHRINK_INDEX failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_SHRINK_INDEX failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_ROLLOVER_INDEX",
        description = "Rollover an alias to a new index",
        parameters = {
            @FunctionParam(name = "alias", type = "STRING", description = "Alias to rollover"),
            @FunctionParam(name = "conditions", type = "DOCUMENT", description = "Rollover conditions")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Rollover response"),
        examples = {"ES_ROLLOVER_INDEX('my-alias', {'max_age': '7d', 'max_docs': 1000000})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerRolloverIndex(ExecutionContext context) {
        context.declareFunction("ES_ROLLOVER_INDEX",
            List.of(
                new Parameter("alias", "STRING", ParameterMode.IN),
                new Parameter("conditions", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_ROLLOVER_INDEX", (args, listener) -> {
                try {
                    String alias = toString(args.get(0));
                    Map<String, Object> conditions = args.size() > 1 && args.get(1) != null ? toMap(args.get(1)) : new HashMap<>();
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("conditions", conditions);
                    
                    Map<String, Object> result = esRequest("POST", "/" + alias + "/_rollover", body);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_ROLLOVER_INDEX failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_ROLLOVER_INDEX failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_RESOLVE_INDEX",
        description = "Resolve index patterns to concrete indices",
        parameters = {
            @FunctionParam(name = "pattern", type = "STRING", description = "Index pattern")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Resolved indices"),
        examples = {"ES_RESOLVE_INDEX('logs-*')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerResolveIndex(ExecutionContext context) {
        context.declareFunction("ES_RESOLVE_INDEX",
            List.of(new Parameter("pattern", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_RESOLVE_INDEX", (args, listener) -> {
                try {
                    String pattern = toString(args.get(0));
                    Map<String, Object> result = esRequest("GET", "/_resolve/index/" + pattern, null);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_RESOLVE_INDEX failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_RESOLVE_INDEX failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_GET_INDEX_TEMPLATE",
        description = "Get an index template",
        parameters = {
            @FunctionParam(name = "name", type = "STRING", description = "Template name")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Template definition"),
        examples = {"ES_GET_INDEX_TEMPLATE('my-template')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetIndexTemplate(ExecutionContext context) {
        context.declareFunction("ES_GET_INDEX_TEMPLATE",
            List.of(new Parameter("name", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_GET_INDEX_TEMPLATE", (args, listener) -> {
                try {
                    String name = toString(args.get(0));
                    Map<String, Object> result = esRequest("GET", "/_index_template/" + name, null);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_GET_INDEX_TEMPLATE failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_GET_INDEX_TEMPLATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_PUT_INDEX_TEMPLATE",
        description = "Create or update an index template",
        parameters = {
            @FunctionParam(name = "name", type = "STRING", description = "Template name"),
            @FunctionParam(name = "template", type = "DOCUMENT", description = "Template definition")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Put response"),
        examples = {"ES_PUT_INDEX_TEMPLATE('my-template', {'index_patterns': ['logs-*'], 'template': {'settings': {'number_of_shards': 1}}})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerPutIndexTemplate(ExecutionContext context) {
        context.declareFunction("ES_PUT_INDEX_TEMPLATE",
            List.of(
                new Parameter("name", "STRING", ParameterMode.IN),
                new Parameter("template", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_PUT_INDEX_TEMPLATE", (args, listener) -> {
                try {
                    String name = toString(args.get(0));
                    Map<String, Object> template = toMap(args.get(1));
                    
                    Map<String, Object> result = esRequest("PUT", "/_index_template/" + name, template);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_PUT_INDEX_TEMPLATE failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_PUT_INDEX_TEMPLATE failed: " + e.getMessage(), e));
                }
            })
        );
    }
}
