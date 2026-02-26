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
 * Elasticsearch Search API functions using REST HTTP calls.
 * Implements: search, count, scroll, aggregations, msearch, field_caps, etc.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.DATASOURCE,
    description = "Elasticsearch Search API functions for querying and aggregating data."
)
public class SearchApiFunctions extends ElasticsearchHttpFunctions {

    public static void registerAll(ExecutionContext context) {
        registerSearch(context);
        registerSearchQuery(context);
        registerCount(context);
        registerScroll(context);
        registerClearScroll(context);
        registerMsearch(context);
        registerFieldCaps(context);
        registerTermsAgg(context);
        registerStatsAgg(context);
        registerDateHistogram(context);
        registerSearchTemplate(context);
        registerKnnSearch(context);
        registerExplain(context);
        registerValidate(context);
        registerTermVectors(context);
    }

    @FunctionSpec(
        name = "ES_SEARCH",
        description = "Execute a search query",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index pattern"),
            @FunctionParam(name = "body", type = "DOCUMENT", description = "Search request body")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Search response with hits"),
        examples = {"ES_SEARCH('logs-*', {'query': {'match_all': {}}, 'size': 10})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerSearch(ExecutionContext context) {
        context.declareFunction("ES_SEARCH",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("body", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_SEARCH", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    Map<String, Object> body = args.size() > 1 && args.get(1) != null ? toMap(args.get(1)) : new HashMap<>();
                    
                    Map<String, Object> result = esRequest("POST", "/" + index + "/_search", body);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_SEARCH failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_SEARCH failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_SEARCH_QUERY",
        description = "Execute a simple query string search",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index pattern"),
            @FunctionParam(name = "query", type = "STRING", description = "Query string"),
            @FunctionParam(name = "size", type = "NUMBER", description = "Maximum results")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Search response"),
        examples = {"ES_SEARCH_QUERY('logs-*', 'level:ERROR', 100)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerSearchQuery(ExecutionContext context) {
        context.declareFunction("ES_SEARCH_QUERY",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("query", "STRING", ParameterMode.IN),
                new Parameter("size", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_SEARCH_QUERY", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    String queryString = toString(args.get(1));
                    int size = toInt(args.get(2), 10);
                    
                    Map<String, Object> body = new HashMap<>();
                    Map<String, Object> queryStringQuery = new HashMap<>();
                    queryStringQuery.put("query", queryString);
                    Map<String, Object> query = new HashMap<>();
                    query.put("query_string", queryStringQuery);
                    body.put("query", query);
                    body.put("size", size);
                    
                    Map<String, Object> result = esRequest("POST", "/" + index + "/_search", body);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_SEARCH_QUERY failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_SEARCH_QUERY failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_COUNT",
        description = "Count documents matching a query",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index pattern"),
            @FunctionParam(name = "query", type = "DOCUMENT", description = "Query to count")
        },
        returnType = @FunctionReturn(type = "NUMBER", description = "Document count"),
        examples = {"ES_COUNT('logs-*', {'match': {'level': 'ERROR'}})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerCount(ExecutionContext context) {
        context.declareFunction("ES_COUNT",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("query", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_COUNT", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    Map<String, Object> query = args.size() > 1 && args.get(1) != null ? toMap(args.get(1)) : null;
                    
                    Map<String, Object> body = new HashMap<>();
                    if (query != null && !query.isEmpty()) {
                        body.put("query", query);
                    }
                    
                    Map<String, Object> result = esRequest("POST", "/" + index + "/_count", body);
                    if (result.get("success").equals(true)) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = (Map<String, Object>) result.get("data");
                        listener.onResponse(data.get("count"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_COUNT failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_COUNT failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_SCROLL",
        description = "Create a scroll context for paginating large result sets",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index pattern"),
            @FunctionParam(name = "body", type = "DOCUMENT", description = "Search body"),
            @FunctionParam(name = "scroll_time", type = "STRING", description = "Scroll keepalive time (e.g., '5m')")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Scroll response with scroll_id"),
        examples = {"ES_SCROLL('logs-*', {'query': {'match_all': {}}, 'size': 1000}, '5m')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerScroll(ExecutionContext context) {
        context.declareFunction("ES_SCROLL",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("body", "DOCUMENT", ParameterMode.IN),
                new Parameter("scroll_time", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_SCROLL", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    Map<String, Object> body = toMap(args.get(1));
                    String scrollTime = args.size() > 2 && args.get(2) != null ? toString(args.get(2)) : "1m";
                    
                    String path = "/" + index + "/_search?scroll=" + scrollTime;
                    Map<String, Object> result = esRequest("POST", path, body);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_SCROLL failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_SCROLL failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_SCROLL_NEXT",
        description = "Get next page of scroll results",
        parameters = {
            @FunctionParam(name = "scroll_id", type = "STRING", description = "Scroll ID from previous response"),
            @FunctionParam(name = "scroll_time", type = "STRING", description = "Scroll keepalive time")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Next page of results"),
        examples = {"ES_SCROLL_NEXT('DXF1...', '5m')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerScrollNext(ExecutionContext context) {
        context.declareFunction("ES_SCROLL_NEXT",
            List.of(
                new Parameter("scroll_id", "STRING", ParameterMode.IN),
                new Parameter("scroll_time", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_SCROLL_NEXT", (args, listener) -> {
                try {
                    String scrollId = toString(args.get(0));
                    String scrollTime = args.size() > 1 && args.get(1) != null ? toString(args.get(1)) : "1m";
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("scroll_id", scrollId);
                    body.put("scroll", scrollTime);
                    
                    Map<String, Object> result = esRequest("POST", "/_search/scroll", body);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_SCROLL_NEXT failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_SCROLL_NEXT failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_CLEAR_SCROLL",
        description = "Clear a scroll context",
        parameters = {
            @FunctionParam(name = "scroll_id", type = "STRING", description = "Scroll ID to clear")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if cleared"),
        examples = {"ES_CLEAR_SCROLL('DXF1...')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerClearScroll(ExecutionContext context) {
        context.declareFunction("ES_CLEAR_SCROLL",
            List.of(new Parameter("scroll_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_CLEAR_SCROLL", (args, listener) -> {
                try {
                    String scrollId = toString(args.get(0));
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("scroll_id", scrollId);
                    
                    Map<String, Object> result = esRequest("DELETE", "/_search/scroll", body);
                    listener.onResponse(result.get("success").equals(true));
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_CLEAR_SCROLL failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_MSEARCH",
        description = "Execute multiple searches in one request",
        parameters = {
            @FunctionParam(name = "searches", type = "STRING", description = "NDJSON multi-search body")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Multi-search response"),
        examples = {"ES_MSEARCH('{\"index\":\"logs-*\"}\\n{\"query\":{\"match_all\":{}}}\\n')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerMsearch(ExecutionContext context) {
        context.declareFunction("ES_MSEARCH",
            List.of(new Parameter("searches", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_MSEARCH", (args, listener) -> {
                try {
                    String searches = toString(args.get(0));
                    
                    Map<String, Object> result = esRequestRaw("POST", "/_msearch", searches);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_MSEARCH failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_MSEARCH failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_FIELD_CAPS",
        description = "Get field capabilities across indices",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index pattern"),
            @FunctionParam(name = "fields", type = "STRING", description = "Comma-separated field names")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Field capabilities"),
        examples = {"ES_FIELD_CAPS('logs-*', 'level,message,timestamp')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerFieldCaps(ExecutionContext context) {
        context.declareFunction("ES_FIELD_CAPS",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("fields", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_FIELD_CAPS", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    String fields = toString(args.get(1));
                    
                    String path = "/" + index + "/_field_caps?fields=" + fields;
                    Map<String, Object> result = esRequest("GET", path, null);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_FIELD_CAPS failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_FIELD_CAPS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_TERMS_AGG",
        description = "Execute a terms aggregation",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index pattern"),
            @FunctionParam(name = "field", type = "STRING", description = "Field to aggregate"),
            @FunctionParam(name = "size", type = "NUMBER", description = "Number of buckets"),
            @FunctionParam(name = "query", type = "DOCUMENT", description = "Optional filter query")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Array of {key, doc_count}"),
        examples = {"ES_TERMS_AGG('logs-*', 'level.keyword', 10, null)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerTermsAgg(ExecutionContext context) {
        context.declareFunction("ES_TERMS_AGG",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("field", "STRING", ParameterMode.IN),
                new Parameter("size", "NUMBER", ParameterMode.IN),
                new Parameter("query", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_TERMS_AGG", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    String field = toString(args.get(1));
                    int size = toInt(args.get(2), 10);
                    Map<String, Object> query = args.size() > 3 && args.get(3) != null ? toMap(args.get(3)) : null;
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("size", 0);
                    
                    if (query != null && !query.isEmpty()) {
                        body.put("query", query);
                    }
                    
                    Map<String, Object> termsAgg = new HashMap<>();
                    termsAgg.put("field", field);
                    termsAgg.put("size", size);
                    
                    Map<String, Object> aggs = new HashMap<>();
                    Map<String, Object> termsWrapper = new HashMap<>();
                    termsWrapper.put("terms", termsAgg);
                    aggs.put("result", termsWrapper);
                    body.put("aggs", aggs);
                    
                    Map<String, Object> result = esRequest("POST", "/" + index + "/_search", body);
                    if (result.get("success").equals(true)) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = (Map<String, Object>) result.get("data");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> aggregations = (Map<String, Object>) data.get("aggregations");
                        if (aggregations != null) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> resultAgg = (Map<String, Object>) aggregations.get("result");
                            listener.onResponse(resultAgg.get("buckets"));
                        } else {
                            listener.onResponse(List.of());
                        }
                    } else {
                        listener.onFailure(new RuntimeException("ES_TERMS_AGG failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_TERMS_AGG failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_STATS_AGG",
        description = "Execute a stats aggregation on a numeric field",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index pattern"),
            @FunctionParam(name = "field", type = "STRING", description = "Numeric field"),
            @FunctionParam(name = "query", type = "DOCUMENT", description = "Optional filter query")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Stats: count, min, max, avg, sum"),
        examples = {"ES_STATS_AGG('metrics-*', 'cpu_usage', null)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerStatsAgg(ExecutionContext context) {
        context.declareFunction("ES_STATS_AGG",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("field", "STRING", ParameterMode.IN),
                new Parameter("query", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_STATS_AGG", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    String field = toString(args.get(1));
                    Map<String, Object> query = args.size() > 2 && args.get(2) != null ? toMap(args.get(2)) : null;
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("size", 0);
                    
                    if (query != null && !query.isEmpty()) {
                        body.put("query", query);
                    }
                    
                    Map<String, Object> statsAgg = new HashMap<>();
                    statsAgg.put("field", field);
                    
                    Map<String, Object> aggs = new HashMap<>();
                    Map<String, Object> statsWrapper = new HashMap<>();
                    statsWrapper.put("stats", statsAgg);
                    aggs.put("result", statsWrapper);
                    body.put("aggs", aggs);
                    
                    Map<String, Object> result = esRequest("POST", "/" + index + "/_search", body);
                    if (result.get("success").equals(true)) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = (Map<String, Object>) result.get("data");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> aggregations = (Map<String, Object>) data.get("aggregations");
                        if (aggregations != null) {
                            listener.onResponse(aggregations.get("result"));
                        } else {
                            listener.onResponse(new HashMap<>());
                        }
                    } else {
                        listener.onFailure(new RuntimeException("ES_STATS_AGG failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_STATS_AGG failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_DATE_HISTOGRAM",
        description = "Execute a date histogram aggregation",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index pattern"),
            @FunctionParam(name = "field", type = "STRING", description = "Date field"),
            @FunctionParam(name = "interval", type = "STRING", description = "Calendar interval (minute, hour, day, week, month)"),
            @FunctionParam(name = "query", type = "DOCUMENT", description = "Optional filter query")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Array of {key_as_string, doc_count}"),
        examples = {"ES_DATE_HISTOGRAM('logs-*', '@timestamp', 'hour', null)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDateHistogram(ExecutionContext context) {
        context.declareFunction("ES_DATE_HISTOGRAM",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("field", "STRING", ParameterMode.IN),
                new Parameter("interval", "STRING", ParameterMode.IN),
                new Parameter("query", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_DATE_HISTOGRAM", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    String field = toString(args.get(1));
                    String interval = toString(args.get(2));
                    Map<String, Object> query = args.size() > 3 && args.get(3) != null ? toMap(args.get(3)) : null;
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("size", 0);
                    
                    if (query != null && !query.isEmpty()) {
                        body.put("query", query);
                    }
                    
                    Map<String, Object> dateHistAgg = new HashMap<>();
                    dateHistAgg.put("field", field);
                    dateHistAgg.put("calendar_interval", interval);
                    
                    Map<String, Object> aggs = new HashMap<>();
                    Map<String, Object> dateHistWrapper = new HashMap<>();
                    dateHistWrapper.put("date_histogram", dateHistAgg);
                    aggs.put("result", dateHistWrapper);
                    body.put("aggs", aggs);
                    
                    Map<String, Object> result = esRequest("POST", "/" + index + "/_search", body);
                    if (result.get("success").equals(true)) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = (Map<String, Object>) result.get("data");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> aggregations = (Map<String, Object>) data.get("aggregations");
                        if (aggregations != null) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> resultAgg = (Map<String, Object>) aggregations.get("result");
                            listener.onResponse(resultAgg.get("buckets"));
                        } else {
                            listener.onResponse(List.of());
                        }
                    } else {
                        listener.onFailure(new RuntimeException("ES_DATE_HISTOGRAM failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_DATE_HISTOGRAM failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_SEARCH_TEMPLATE",
        description = "Execute a search template",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index pattern"),
            @FunctionParam(name = "template_id", type = "STRING", description = "Stored template ID"),
            @FunctionParam(name = "params", type = "DOCUMENT", description = "Template parameters")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Search response"),
        examples = {"ES_SEARCH_TEMPLATE('logs-*', 'my_template', {'query_string': 'ERROR'})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerSearchTemplate(ExecutionContext context) {
        context.declareFunction("ES_SEARCH_TEMPLATE",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("template_id", "STRING", ParameterMode.IN),
                new Parameter("params", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_SEARCH_TEMPLATE", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    String templateId = toString(args.get(1));
                    Map<String, Object> params = args.size() > 2 && args.get(2) != null ? toMap(args.get(2)) : new HashMap<>();
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("id", templateId);
                    body.put("params", params);
                    
                    Map<String, Object> result = esRequest("POST", "/" + index + "/_search/template", body);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_SEARCH_TEMPLATE failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_SEARCH_TEMPLATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_KNN_SEARCH",
        description = "Execute a k-nearest neighbor search",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "field", type = "STRING", description = "Vector field"),
            @FunctionParam(name = "query_vector", type = "ARRAY", description = "Query vector"),
            @FunctionParam(name = "k", type = "NUMBER", description = "Number of nearest neighbors")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "KNN search response"),
        examples = {"ES_KNN_SEARCH('embeddings', 'vector', [0.1, 0.2, 0.3], 10)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerKnnSearch(ExecutionContext context) {
        context.declareFunction("ES_KNN_SEARCH",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("field", "STRING", ParameterMode.IN),
                new Parameter("query_vector", "ARRAY", ParameterMode.IN),
                new Parameter("k", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_KNN_SEARCH", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    String field = toString(args.get(1));
                    List<?> queryVector = (List<?>) args.get(2);
                    int k = toInt(args.get(3), 10);
                    
                    Map<String, Object> knn = new HashMap<>();
                    knn.put("field", field);
                    knn.put("query_vector", queryVector);
                    knn.put("k", k);
                    knn.put("num_candidates", k * 10);
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("knn", knn);
                    
                    Map<String, Object> result = esRequest("POST", "/" + index + "/_search", body);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_KNN_SEARCH failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_KNN_SEARCH failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_EXPLAIN",
        description = "Explain why a document matches or doesn't match a query",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "id", type = "STRING", description = "Document ID"),
            @FunctionParam(name = "query", type = "DOCUMENT", description = "Query to explain")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Explain response"),
        examples = {"ES_EXPLAIN('my-index', 'doc-1', {'match': {'title': 'test'}})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerExplain(ExecutionContext context) {
        context.declareFunction("ES_EXPLAIN",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("id", "STRING", ParameterMode.IN),
                new Parameter("query", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_EXPLAIN", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    String id = toString(args.get(1));
                    Map<String, Object> query = toMap(args.get(2));
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("query", query);
                    
                    Map<String, Object> result = esRequest("POST", "/" + index + "/_explain/" + id, body);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_EXPLAIN failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_EXPLAIN failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_VALIDATE",
        description = "Validate a query without executing it",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index pattern"),
            @FunctionParam(name = "query", type = "DOCUMENT", description = "Query to validate")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Validation result"),
        examples = {"ES_VALIDATE('logs-*', {'match': {'message': 'test'}})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerValidate(ExecutionContext context) {
        context.declareFunction("ES_VALIDATE",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("query", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_VALIDATE", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    Map<String, Object> query = toMap(args.get(1));
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("query", query);
                    
                    Map<String, Object> result = esRequest("POST", "/" + index + "/_validate/query?explain=true", body);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_VALIDATE failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_VALIDATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_TERM_VECTORS",
        description = "Get term vectors for a document",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "id", type = "STRING", description = "Document ID"),
            @FunctionParam(name = "fields", type = "STRING", description = "Comma-separated field names")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Term vectors"),
        examples = {"ES_TERM_VECTORS('my-index', 'doc-1', 'title,content')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerTermVectors(ExecutionContext context) {
        context.declareFunction("ES_TERM_VECTORS",
            List.of(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("id", "STRING", ParameterMode.IN),
                new Parameter("fields", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_TERM_VECTORS", (args, listener) -> {
                try {
                    String index = toString(args.get(0));
                    String id = toString(args.get(1));
                    String fields = toString(args.get(2));
                    
                    String path = "/" + index + "/_termvectors/" + id + "?fields=" + fields;
                    Map<String, Object> result = esRequest("GET", path, null);
                    if (result.get("success").equals(true)) {
                        listener.onResponse(result.get("data"));
                    } else {
                        listener.onFailure(new RuntimeException("ES_TERM_VECTORS failed: " + result.get("error")));
                    }
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_TERM_VECTORS failed: " + e.getMessage(), e));
                }
            })
        );
    }
}
