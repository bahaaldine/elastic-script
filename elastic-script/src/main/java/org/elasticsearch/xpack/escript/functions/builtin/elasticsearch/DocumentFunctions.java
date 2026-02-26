/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.builtin.elasticsearch;

import org.elasticsearch.xpack.escript.functions.api.FunctionCollectionSpec;
import org.elasticsearch.xpack.escript.functions.api.FunctionCategory;
import org.elasticsearch.xpack.escript.functions.api.FunctionParam;
import org.elasticsearch.xpack.escript.functions.api.FunctionReturn;
import org.elasticsearch.xpack.escript.functions.api.FunctionSpec;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.index.reindex.ReindexRequest;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch Document API functions.
 * Covers: index, get, delete, update, bulk, mget, reindex, update_by_query, delete_by_query
 */
@FunctionCollectionSpec(
    category = FunctionCategory.DATASOURCE,
    description = "Elasticsearch Document API functions for CRUD operations"
)
public class DocumentFunctions {

    public static void registerAll(ExecutionContext context, Client client) {
        registerIndex(context, client);
        registerGet(context, client);
        registerExists(context, client);
        registerDelete(context, client);
        registerUpdate(context, client);
        registerBulk(context, client);
        registerMget(context, client);
        registerReindex(context, client);
        registerUpdateByQuery(context, client);
        registerDeleteByQuery(context, client);
        registerGetSource(context, client);
        registerCreate(context, client);
    }

    @FunctionSpec(
        name = "ES_INDEX",
        description = "Index a document into Elasticsearch",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "document", type = "DOCUMENT", description = "Document to index"),
            @FunctionParam(name = "id", type = "STRING", description = "Optional document ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Index response with _id, _version, result"),
        examples = {"ES_INDEX('my-index', {'field': 'value'}, 'doc-1')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerIndex(ExecutionContext context, Client client) {
        context.declareFunction("ES_INDEX",
            Arrays.asList(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("document", "DOCUMENT", ParameterMode.IN),
                new Parameter("id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_INDEX", (args, listener) -> {
                String index = args.get(0).toString();
                @SuppressWarnings("unchecked")
                Map<String, Object> doc = (Map<String, Object>) args.get(1);
                String id = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : null;
                
                IndexRequest request = new IndexRequest(index).source(doc, XContentType.JSON);
                if (id != null && !id.isEmpty()) {
                    request.id(id);
                }
                
                client.index(request, new ActionListener<IndexResponse>() {
                    @Override
                    public void onResponse(IndexResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("_index", response.getIndex());
                        result.put("_id", response.getId());
                        result.put("_version", response.getVersion());
                        result.put("result", response.getResult().getLowercase());
                        listener.onResponse(result);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
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
        returnType = @FunctionReturn(type = "DOCUMENT", description = "The document source"),
        examples = {"ES_GET('my-index', 'doc-1')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGet(ExecutionContext context, Client client) {
        context.declareFunction("ES_GET",
            Arrays.asList(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_GET", (args, listener) -> {
                String index = args.get(0).toString();
                String id = args.get(1).toString();
                
                GetRequest request = new GetRequest(index, id);
                client.get(request, new ActionListener<GetResponse>() {
                    @Override
                    public void onResponse(GetResponse response) {
                        if (response.isExists()) {
                            Map<String, Object> result = new HashMap<>(response.getSourceAsMap());
                            result.put("_id", response.getId());
                            result.put("_index", response.getIndex());
                            result.put("_version", response.getVersion());
                            listener.onResponse(result);
                        } else {
                            listener.onResponse(null);
                        }
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_EXISTS",
        description = "Check if a document exists",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "id", type = "STRING", description = "Document ID")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if document exists"),
        examples = {"ES_EXISTS('my-index', 'doc-1')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerExists(ExecutionContext context, Client client) {
        context.declareFunction("ES_EXISTS",
            Arrays.asList(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_EXISTS", (args, listener) -> {
                String index = args.get(0).toString();
                String id = args.get(1).toString();
                
                GetRequest request = new GetRequest(index, id).fetchSourceContext(org.elasticsearch.search.fetch.subphase.FetchSourceContext.DO_NOT_FETCH_SOURCE);
                client.get(request, new ActionListener<GetResponse>() {
                    @Override
                    public void onResponse(GetResponse response) {
                        listener.onResponse(response.isExists());
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_DELETE",
        description = "Delete a document by ID",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "id", type = "STRING", description = "Document ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Delete response"),
        examples = {"ES_DELETE('my-index', 'doc-1')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDelete(ExecutionContext context, Client client) {
        context.declareFunction("ES_DELETE",
            Arrays.asList(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_DELETE", (args, listener) -> {
                String index = args.get(0).toString();
                String id = args.get(1).toString();
                
                DeleteRequest request = new DeleteRequest(index, id);
                client.delete(request, new ActionListener<DeleteResponse>() {
                    @Override
                    public void onResponse(DeleteResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("_index", response.getIndex());
                        result.put("_id", response.getId());
                        result.put("_version", response.getVersion());
                        result.put("result", response.getResult().getLowercase());
                        listener.onResponse(result);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_UPDATE",
        description = "Update a document by ID",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "id", type = "STRING", description = "Document ID"),
            @FunctionParam(name = "doc", type = "DOCUMENT", description = "Partial document to merge")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Update response"),
        examples = {"ES_UPDATE('my-index', 'doc-1', {'field': 'new-value'})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerUpdate(ExecutionContext context, Client client) {
        context.declareFunction("ES_UPDATE",
            Arrays.asList(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("id", "STRING", ParameterMode.IN),
                new Parameter("doc", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_UPDATE", (args, listener) -> {
                String index = args.get(0).toString();
                String id = args.get(1).toString();
                @SuppressWarnings("unchecked")
                Map<String, Object> doc = (Map<String, Object>) args.get(2);
                
                UpdateRequest request = new UpdateRequest(index, id).doc(doc, XContentType.JSON);
                client.update(request, new ActionListener<UpdateResponse>() {
                    @Override
                    public void onResponse(UpdateResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("_index", response.getIndex());
                        result.put("_id", response.getId());
                        result.put("_version", response.getVersion());
                        result.put("result", response.getResult().getLowercase());
                        listener.onResponse(result);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_BULK",
        description = "Perform bulk operations (index, update, delete)",
        parameters = {
            @FunctionParam(name = "operations", type = "ARRAY", description = "Array of {action, index, id, doc}")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Bulk response with stats"),
        examples = {"ES_BULK([{'action': 'index', 'index': 'test', 'doc': {'f': 1}}])"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerBulk(ExecutionContext context, Client client) {
        context.declareFunction("ES_BULK",
            List.of(new Parameter("operations", "ARRAY", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_BULK", (args, listener) -> {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> operations = (List<Map<String, Object>>) args.get(0);
                
                BulkRequest bulkRequest = new BulkRequest();
                for (Map<String, Object> op : operations) {
                    String action = op.get("action").toString();
                    String index = op.get("index").toString();
                    String id = op.containsKey("id") ? op.get("id").toString() : null;
                    
                    switch (action.toLowerCase()) {
                        case "index":
                            @SuppressWarnings("unchecked")
                            Map<String, Object> indexDoc = (Map<String, Object>) op.get("doc");
                            IndexRequest indexReq = new IndexRequest(index).source(indexDoc, XContentType.JSON);
                            if (id != null) indexReq.id(id);
                            bulkRequest.add(indexReq);
                            break;
                        case "delete":
                            bulkRequest.add(new DeleteRequest(index, id));
                            break;
                        case "update":
                            @SuppressWarnings("unchecked")
                            Map<String, Object> updateDoc = (Map<String, Object>) op.get("doc");
                            bulkRequest.add(new UpdateRequest(index, id).doc(updateDoc, XContentType.JSON));
                            break;
                    }
                }
                
                client.bulk(bulkRequest, new ActionListener<BulkResponse>() {
                    @Override
                    public void onResponse(BulkResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("took", response.getTook().millis());
                        result.put("errors", response.hasFailures());
                        result.put("items_count", response.getItems().length);
                        listener.onResponse(result);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
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
        returnType = @FunctionReturn(type = "ARRAY", description = "Array of documents"),
        examples = {"ES_MGET('my-index', ['id1', 'id2', 'id3'])"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerMget(ExecutionContext context, Client client) {
        context.declareFunction("ES_MGET",
            Arrays.asList(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("ids", "ARRAY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_MGET", (args, listener) -> {
                String index = args.get(0).toString();
                @SuppressWarnings("unchecked")
                List<Object> ids = (List<Object>) args.get(1);
                
                MultiGetRequest request = new MultiGetRequest();
                for (Object id : ids) {
                    request.add(new MultiGetRequest.Item(index, id.toString()));
                }
                
                client.multiGet(request, new ActionListener<MultiGetResponse>() {
                    @Override
                    public void onResponse(MultiGetResponse response) {
                        List<Map<String, Object>> results = new java.util.ArrayList<>();
                        for (var item : response.getResponses()) {
                            if (!item.isFailed() && item.getResponse().isExists()) {
                                Map<String, Object> doc = new HashMap<>(item.getResponse().getSourceAsMap());
                                doc.put("_id", item.getResponse().getId());
                                results.add(doc);
                            }
                        }
                        listener.onResponse(results);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_REINDEX",
        description = "Reindex documents from source to destination index",
        parameters = {
            @FunctionParam(name = "source_index", type = "STRING", description = "Source index"),
            @FunctionParam(name = "dest_index", type = "STRING", description = "Destination index"),
            @FunctionParam(name = "query", type = "DOCUMENT", description = "Optional query to filter documents")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Reindex stats"),
        examples = {"ES_REINDEX('old-index', 'new-index', {'match_all': {}})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerReindex(ExecutionContext context, Client client) {
        context.declareFunction("ES_REINDEX",
            Arrays.asList(
                new Parameter("source_index", "STRING", ParameterMode.IN),
                new Parameter("dest_index", "STRING", ParameterMode.IN),
                new Parameter("query", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_REINDEX", (args, listener) -> {
                String sourceIndex = args.get(0).toString();
                String destIndex = args.get(1).toString();
                
                ReindexRequest request = new ReindexRequest();
                request.setSourceIndices(sourceIndex);
                request.setDestIndex(destIndex);
                
                client.execute(org.elasticsearch.index.reindex.ReindexAction.INSTANCE, request, 
                    new ActionListener<BulkByScrollResponse>() {
                        @Override
                        public void onResponse(BulkByScrollResponse response) {
                            Map<String, Object> result = new HashMap<>();
                            result.put("took", response.getTook().millis());
                            result.put("total", response.getTotal());
                            result.put("created", response.getCreated());
                            result.put("updated", response.getUpdated());
                            result.put("deleted", response.getDeleted());
                            result.put("failures", response.getBulkFailures().size());
                            listener.onResponse(result);
                        }
                        @Override
                        public void onFailure(Exception e) {
                            listener.onFailure(e);
                        }
                    });
            })
        );
    }

    @FunctionSpec(
        name = "ES_UPDATE_BY_QUERY",
        description = "Update documents matching a query",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "query", type = "STRING", description = "Query string"),
            @FunctionParam(name = "script", type = "STRING", description = "Painless script to execute")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Update stats"),
        examples = {"ES_UPDATE_BY_QUERY('my-index', 'status:pending', 'ctx._source.status = \"processed\"')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerUpdateByQuery(ExecutionContext context, Client client) {
        context.declareFunction("ES_UPDATE_BY_QUERY",
            Arrays.asList(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("query", "STRING", ParameterMode.IN),
                new Parameter("script", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_UPDATE_BY_QUERY", (args, listener) -> {
                String index = args.get(0).toString();
                String queryString = args.get(1).toString();
                String scriptSource = args.get(2).toString();
                
                UpdateByQueryRequest request = new UpdateByQueryRequest(index);
                request.setQuery(QueryBuilders.queryStringQuery(queryString));
                request.setScript(new Script(ScriptType.INLINE, "painless", scriptSource, Map.of()));
                
                client.execute(org.elasticsearch.index.reindex.UpdateByQueryAction.INSTANCE, request,
                    new ActionListener<BulkByScrollResponse>() {
                        @Override
                        public void onResponse(BulkByScrollResponse response) {
                            Map<String, Object> result = new HashMap<>();
                            result.put("took", response.getTook().millis());
                            result.put("total", response.getTotal());
                            result.put("updated", response.getUpdated());
                            result.put("failures", response.getBulkFailures().size());
                            listener.onResponse(result);
                        }
                        @Override
                        public void onFailure(Exception e) {
                            listener.onFailure(e);
                        }
                    });
            })
        );
    }

    @FunctionSpec(
        name = "ES_DELETE_BY_QUERY",
        description = "Delete documents matching a query",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "query", type = "STRING", description = "Query string")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Delete stats"),
        examples = {"ES_DELETE_BY_QUERY('my-index', 'status:obsolete')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDeleteByQuery(ExecutionContext context, Client client) {
        context.declareFunction("ES_DELETE_BY_QUERY",
            Arrays.asList(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("query", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_DELETE_BY_QUERY", (args, listener) -> {
                String index = args.get(0).toString();
                String queryString = args.get(1).toString();
                
                DeleteByQueryRequest request = new DeleteByQueryRequest(index);
                request.setQuery(QueryBuilders.queryStringQuery(queryString));
                
                client.execute(org.elasticsearch.index.reindex.DeleteByQueryAction.INSTANCE, request,
                    new ActionListener<BulkByScrollResponse>() {
                        @Override
                        public void onResponse(BulkByScrollResponse response) {
                            Map<String, Object> result = new HashMap<>();
                            result.put("took", response.getTook().millis());
                            result.put("total", response.getTotal());
                            result.put("deleted", response.getDeleted());
                            result.put("failures", response.getBulkFailures().size());
                            listener.onResponse(result);
                        }
                        @Override
                        public void onFailure(Exception e) {
                            listener.onFailure(e);
                        }
                    });
            })
        );
    }

    @FunctionSpec(
        name = "ES_GET_SOURCE",
        description = "Get only the source of a document",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "id", type = "STRING", description = "Document ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Document source only"),
        examples = {"ES_GET_SOURCE('my-index', 'doc-1')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetSource(ExecutionContext context, Client client) {
        context.declareFunction("ES_GET_SOURCE",
            Arrays.asList(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_GET_SOURCE", (args, listener) -> {
                String index = args.get(0).toString();
                String id = args.get(1).toString();
                
                GetRequest request = new GetRequest(index, id);
                client.get(request, new ActionListener<GetResponse>() {
                    @Override
                    public void onResponse(GetResponse response) {
                        if (response.isExists()) {
                            listener.onResponse(response.getSourceAsMap());
                        } else {
                            listener.onResponse(null);
                        }
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_CREATE",
        description = "Create a new document (fails if ID exists)",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "id", type = "STRING", description = "Document ID"),
            @FunctionParam(name = "document", type = "DOCUMENT", description = "Document to create")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Create response"),
        examples = {"ES_CREATE('my-index', 'doc-1', {'field': 'value'})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerCreate(ExecutionContext context, Client client) {
        context.declareFunction("ES_CREATE",
            Arrays.asList(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("id", "STRING", ParameterMode.IN),
                new Parameter("document", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_CREATE", (args, listener) -> {
                String index = args.get(0).toString();
                String id = args.get(1).toString();
                @SuppressWarnings("unchecked")
                Map<String, Object> doc = (Map<String, Object>) args.get(2);
                
                IndexRequest request = new IndexRequest(index)
                    .id(id)
                    .source(doc, XContentType.JSON)
                    .create(true);
                
                client.index(request, new ActionListener<IndexResponse>() {
                    @Override
                    public void onResponse(IndexResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("_index", response.getIndex());
                        result.put("_id", response.getId());
                        result.put("_version", response.getVersion());
                        result.put("result", response.getResult().getLowercase());
                        listener.onResponse(result);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }
}
