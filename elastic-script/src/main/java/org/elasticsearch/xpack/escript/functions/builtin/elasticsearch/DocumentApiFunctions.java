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
 * Elasticsearch Document API functions using REST HTTP calls.
 * Implements: index, get, delete, update, bulk, mget, reindex, update_by_query, delete_by_query
 */
@FunctionCollectionSpec(
    category = FunctionCategory.DATASOURCE,
    description = "Elasticsearch Document API functions for CRUD operations."
)
public class DocumentApiFunctions extends ElasticsearchHttpFunctions {

    public static void registerAll(ExecutionContext context) {
        registerIndex(context);
        registerGet(context);
        registerExists(context);
        registerDelete(context);
        registerUpdate(context);
        registerBulk(context);
        registerMget(context);
        registerReindex(context);
        registerUpdateByQuery(context);
        registerDeleteByQuery(context);
        registerGetSource(context);
        registerCreate(context);
    }

    @FunctionSpec(
        name = "ES_INDEX",
        description = "Index a document into Elasticsearch",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Target index name"),
            @FunctionParam(name = "document", type = "DOCUMENT", description = "Document to index"),
            @FunctionParam(name = "id", type = "STRING", description = "Optional document ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Index result with _id, _version, result"),
        examples = {"ES_INDEX('my-index', {'field': 'value'}, 'doc-1')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerIndex(ExecutionContext context) {
        context.declareFunction("ES_INDEX",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("document", "DOCUMENT", ParameterMode.IN),
                new Parameter("id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_INDEX", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    Map<String, Object> doc = toMap(args.get(1));
                    String id = args.size() > 2 ? toString(args.get(2)) : null;
                    
                    String path = id != null && !id.isEmpty() 
                        ? "/" + index + "/_doc/" + id 
                        : "/" + index + "/_doc";
                    String method = id != null && !id.isEmpty() ? "PUT" : "POST";
                    
                    Map<String, Object> result = esRequest(method, path, doc);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_INDEX failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_INDEX failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_GET",
        description = "Get a document by ID from Elasticsearch",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "id", type = "STRING", description = "Document ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Document with _source"),
        examples = {"ES_GET('my-index', 'doc-1')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGet(ExecutionContext context) {
        context.declareFunction("ES_GET",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_GET", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    String id = toString(args.get(1));
                    
                    Map<String, Object> result = esRequest("GET", "/" + index + "/_doc/" + id, null);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_GET failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_GET failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_EXISTS",
        description = "Check if a document exists in Elasticsearch",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "id", type = "STRING", description = "Document ID")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if document exists"),
        examples = {"ES_EXISTS('my-index', 'doc-1')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerExists(ExecutionContext context) {
        context.declareFunction("ES_EXISTS",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_EXISTS", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    String id = toString(args.get(1));
                    
                    Map<String, Object> result = esRequest("HEAD", "/" + index + "/_doc/" + id, null);
                    listener.onResponse(result.get("success").equals(true));
                } catch (Exception e) {
                    listener.onResponse(false);
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_DELETE",
        description = "Delete a document from Elasticsearch",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "id", type = "STRING", description = "Document ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Delete result"),
        examples = {"ES_DELETE('my-index', 'doc-1')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDelete(ExecutionContext context) {
        context.declareFunction("ES_DELETE",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_DELETE", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    String id = toString(args.get(1));
                    
                    Map<String, Object> result = esRequest("DELETE", "/" + index + "/_doc/" + id, null);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_DELETE failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_DELETE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_UPDATE",
        description = "Update a document in Elasticsearch",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "id", type = "STRING", description = "Document ID"),
            @FunctionParam(name = "doc", type = "DOCUMENT", description = "Partial document or script")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Update result"),
        examples = {"ES_UPDATE('my-index', 'doc-1', {'doc': {'field': 'new_value'}})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerUpdate(ExecutionContext context) {
        context.declareFunction("ES_UPDATE",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("id", "STRING", ParameterMode.IN),
                new Parameter("doc", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_UPDATE", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    String id = toString(args.get(1));
                    Map<String, Object> updateDoc = toMap(args.get(2));
                    
                    Map<String, Object> body = new HashMap<>();
                    if (updateDoc.containsKey("doc") || updateDoc.containsKey("script")) {
                        body = updateDoc;
                    } else {
                        body.put("doc", updateDoc);
                    }
                    
                    Map<String, Object> result = esRequest("POST", "/" + index + "/_update/" + id, body);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_UPDATE failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_UPDATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_BULK",
        description = "Execute bulk operations in Elasticsearch",
        parameters = {
            @FunctionParam(name = "operations", type = "STRING", description = "NDJSON bulk operations")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Bulk response"),
        examples = {"ES_BULK('{\"index\":{\"_index\":\"test\"}}\\n{\"field\":\"value\"}\\n')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerBulk(ExecutionContext context) {
        context.declareFunction("ES_BULK",
            List.of(new Parameter("operations", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_BULK", (args, listener) -> {
                try {
                    String operations = toString(args.get(0));
                    
                    Map<String, Object> result = esRequestRaw("POST", "/_bulk", operations);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_BULK failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_BULK failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_MGET",
        description = "Get multiple documents by IDs",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "ids", type = "ARRAY", description = "Array of document IDs")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Multi-get response with docs array"),
        examples = {"ES_MGET('my-index', ['id1', 'id2', 'id3'])"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerMget(ExecutionContext context) {
        context.declareFunction("ES_MGET",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("ids", "ARRAY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_MGET", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    List<?> ids = (List<?>) args.get(1);
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("ids", ids);
                    
                    Map<String, Object> result = esRequest("POST", "/" + index + "/_mget", body);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_MGET failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_MGET failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_REINDEX",
        description = "Reindex documents from one index to another",
        parameters = {
            @FunctionParam(name = "source_index", type = "STRING", description = "Source index"),
            @FunctionParam(name = "dest_index", type = "STRING", description = "Destination index"),
            @FunctionParam(name = "query", type = "DOCUMENT", description = "Optional query to filter source documents")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Reindex response"),
        examples = {"ES_REINDEX('old-index', 'new-index', null)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerReindex(ExecutionContext context) {
        context.declareFunction("ES_REINDEX",
            List.of(
                new Parameter("source_index", "STRING", ParameterMode.IN),
                new Parameter("dest_index", "STRING", ParameterMode.IN),
                new Parameter("query", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_REINDEX", (args, listener) -> {
                try {
                    String sourceIndex = toString(args.get(0));
                    String destIndex = toString(args.get(1));
                    Map<String, Object> query = args.size() > 2 && args.get(2) != null ? toMap(args.get(2)) : null;
                    
                    Map<String, Object> source = new HashMap<>();
                    source.put("index", sourceIndex);
                    if (query != null && !query.isEmpty()) {
                        source.put("query", query);
                    }
                    
                    Map<String, Object> dest = new HashMap<>();
                    dest.put("index", destIndex);
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("source", source);
                    body.put("dest", dest);
                    
                    Map<String, Object> result = esRequest("POST", "/_reindex", body);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_REINDEX failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_REINDEX failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_UPDATE_BY_QUERY",
        description = "Update documents matching a query",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "query", type = "DOCUMENT", description = "Query to match documents"),
            @FunctionParam(name = "script", type = "STRING", description = "Script to execute on each document")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Update by query response"),
        examples = {"ES_UPDATE_BY_QUERY('my-index', {'match_all': {}}, 'ctx._source.count++')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerUpdateByQuery(ExecutionContext context) {
        context.declareFunction("ES_UPDATE_BY_QUERY",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("query", "DOCUMENT", ParameterMode.IN),
                new Parameter("script", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_UPDATE_BY_QUERY", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    Map<String, Object> query = toMap(args.get(1));
                    String script = toString(args.get(2));
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("query", query);
                    
                    Map<String, Object> scriptObj = new HashMap<>();
                    scriptObj.put("source", script);
                    scriptObj.put("lang", "painless");
                    body.put("script", scriptObj);
                    
                    Map<String, Object> result = esRequest("POST", "/" + index + "/_update_by_query", body);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_UPDATE_BY_QUERY failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_UPDATE_BY_QUERY failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_DELETE_BY_QUERY",
        description = "Delete documents matching a query",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "query", type = "DOCUMENT", description = "Query to match documents to delete")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Delete by query response"),
        examples = {"ES_DELETE_BY_QUERY('my-index', {'term': {'status': 'deleted'}})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDeleteByQuery(ExecutionContext context) {
        context.declareFunction("ES_DELETE_BY_QUERY",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("query", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_DELETE_BY_QUERY", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    Map<String, Object> query = toMap(args.get(1));
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("query", query);
                    
                    Map<String, Object> result = esRequest("POST", "/" + index + "/_delete_by_query", body);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_DELETE_BY_QUERY failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_DELETE_BY_QUERY failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_GET_SOURCE",
        description = "Get only the _source of a document",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "id", type = "STRING", description = "Document ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Document source"),
        examples = {"ES_GET_SOURCE('my-index', 'doc-1')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetSource(ExecutionContext context) {
        context.declareFunction("ES_GET_SOURCE",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_GET_SOURCE", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    String id = toString(args.get(1));
                    
                    Map<String, Object> result = esRequest("GET", "/" + index + "/_source/" + id, null);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_GET_SOURCE failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_GET_SOURCE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_CREATE",
        description = "Create a document with explicit ID (fails if exists)",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "id", type = "STRING", description = "Document ID"),
            @FunctionParam(name = "document", type = "DOCUMENT", description = "Document to create")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Create result"),
        examples = {"ES_CREATE('my-index', 'doc-1', {'field': 'value'})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerCreate(ExecutionContext context) {
        context.declareFunction("ES_CREATE",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("id", "STRING", ParameterMode.IN),
                new Parameter("document", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_CREATE", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    String id = toString(args.get(1));
                    Map<String, Object> doc = toMap(args.get(2));
                    
                    Map<String, Object> result = esRequest("PUT", "/" + index + "/_create/" + id, doc);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_CREATE failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_CREATE failed: " + e.getMessage(), e));
                }
            })
        );
    }
}
