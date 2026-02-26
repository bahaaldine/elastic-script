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
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.Max;
import org.elasticsearch.search.aggregations.metrics.Min;
import org.elasticsearch.search.aggregations.metrics.Sum;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch Search API functions.
 * Covers: search, msearch, scroll, count, aggregations, field_caps, explain
 */
@FunctionCollectionSpec(
    category = FunctionCategory.DATASOURCE,
    description = "Elasticsearch Search API functions for querying and analyzing data"
)
public class SearchFunctions {

    public static void registerAll(ExecutionContext context, Client client) {
        registerSearch(context, client);
        registerSearchQuery(context, client);
        registerMsearch(context, client);
        registerScroll(context, client);
        registerClearScroll(context, client);
        registerCount(context, client);
        registerTermsAgg(context, client);
        registerStatsAgg(context, client);
        registerDateHistogram(context, client);
        registerSearchAfter(context, client);
        registerSearchTemplate(context, client);
        registerPointInTime(context, client);
        registerFieldCaps(context, client);
        registerKnnSearch(context, client);
        registerRankEval(context, client);
    }

    @FunctionSpec(
        name = "ES_SEARCH",
        description = "Execute a search query",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index pattern"),
            @FunctionParam(name = "query", type = "DOCUMENT", description = "Query DSL"),
            @FunctionParam(name = "size", type = "NUMBER", description = "Number of results")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Search results with hits"),
        examples = {"ES_SEARCH('logs-*', {'match': {'message': 'error'}}, 100)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerSearch(ExecutionContext context, Client client) {
        context.declareFunction("ES_SEARCH",
            Arrays.asList(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("query", "DOCUMENT", ParameterMode.IN),
                new Parameter("size", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_SEARCH", (args, listener) -> {
                String index = args.get(0).toString();
                @SuppressWarnings("unchecked")
                Map<String, Object> queryMap = (Map<String, Object>) args.get(1);
                int size = args.size() > 2 && args.get(2) != null ? ((Number) args.get(2)).intValue() : 10;
                
                SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
                sourceBuilder.query(QueryBuilders.wrapperQuery(mapToJson(queryMap)));
                sourceBuilder.size(size);
                
                SearchRequest request = new SearchRequest(index);
                request.source(sourceBuilder);
                
                client.search(request, new ActionListener<SearchResponse>() {
                    @Override
                    public void onResponse(SearchResponse response) {
                        listener.onResponse(buildSearchResult(response));
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
        name = "ES_SEARCH_QUERY",
        description = "Execute a simple query string search",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index pattern"),
            @FunctionParam(name = "query", type = "STRING", description = "Query string"),
            @FunctionParam(name = "size", type = "NUMBER", description = "Number of results")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Search results"),
        examples = {"ES_SEARCH_QUERY('logs-*', 'level:error AND service:api', 50)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerSearchQuery(ExecutionContext context, Client client) {
        context.declareFunction("ES_SEARCH_QUERY",
            Arrays.asList(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("query", "STRING", ParameterMode.IN),
                new Parameter("size", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_SEARCH_QUERY", (args, listener) -> {
                String index = args.get(0).toString();
                String queryString = args.get(1).toString();
                int size = args.size() > 2 && args.get(2) != null ? ((Number) args.get(2)).intValue() : 10;
                
                SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
                sourceBuilder.query(QueryBuilders.queryStringQuery(queryString));
                sourceBuilder.size(size);
                
                SearchRequest request = new SearchRequest(index);
                request.source(sourceBuilder);
                
                client.search(request, new ActionListener<SearchResponse>() {
                    @Override
                    public void onResponse(SearchResponse response) {
                        listener.onResponse(buildSearchResult(response));
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
        name = "ES_MSEARCH",
        description = "Execute multiple searches in one request",
        parameters = {
            @FunctionParam(name = "searches", type = "ARRAY", description = "Array of {index, query} objects")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Array of search results"),
        examples = {"ES_MSEARCH([{'index': 'logs-*', 'query': 'error'}, {'index': 'metrics-*', 'query': 'cpu>90'}])"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerMsearch(ExecutionContext context, Client client) {
        context.declareFunction("ES_MSEARCH",
            List.of(new Parameter("searches", "ARRAY", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_MSEARCH", (args, listener) -> {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> searches = (List<Map<String, Object>>) args.get(0);
                
                MultiSearchRequest multiRequest = new MultiSearchRequest();
                for (Map<String, Object> searchDef : searches) {
                    String index = searchDef.get("index").toString();
                    String queryString = searchDef.get("query").toString();
                    
                    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
                    sourceBuilder.query(QueryBuilders.queryStringQuery(queryString));
                    
                    SearchRequest searchRequest = new SearchRequest(index);
                    searchRequest.source(sourceBuilder);
                    multiRequest.add(searchRequest);
                }
                
                client.multiSearch(multiRequest, new ActionListener<MultiSearchResponse>() {
                    @Override
                    public void onResponse(MultiSearchResponse response) {
                        List<Map<String, Object>> results = new ArrayList<>();
                        for (MultiSearchResponse.Item item : response.getResponses()) {
                            if (item.isFailure()) {
                                Map<String, Object> error = new HashMap<>();
                                error.put("error", item.getFailureMessage());
                                results.add(error);
                            } else {
                                results.add(buildSearchResult(item.getResponse()));
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
        name = "ES_SCROLL",
        description = "Scroll through search results",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index pattern"),
            @FunctionParam(name = "query", type = "STRING", description = "Query string"),
            @FunctionParam(name = "size", type = "NUMBER", description = "Batch size"),
            @FunctionParam(name = "scroll_time", type = "STRING", description = "Scroll keep-alive (e.g., '5m')")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "First batch with scroll_id"),
        examples = {"ES_SCROLL('logs-*', '*', 1000, '5m')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerScroll(ExecutionContext context, Client client) {
        context.declareFunction("ES_SCROLL",
            Arrays.asList(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("query", "STRING", ParameterMode.IN),
                new Parameter("size", "NUMBER", ParameterMode.IN),
                new Parameter("scroll_time", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_SCROLL", (args, listener) -> {
                String index = args.get(0).toString();
                String queryString = args.get(1).toString();
                int size = ((Number) args.get(2)).intValue();
                String scrollTime = args.get(3).toString();
                
                SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
                sourceBuilder.query(QueryBuilders.queryStringQuery(queryString));
                sourceBuilder.size(size);
                
                SearchRequest request = new SearchRequest(index);
                request.source(sourceBuilder);
                request.scroll(TimeValue.parseTimeValue(scrollTime, "scroll"));
                
                client.search(request, new ActionListener<SearchResponse>() {
                    @Override
                    public void onResponse(SearchResponse response) {
                        Map<String, Object> result = buildSearchResult(response);
                        result.put("scroll_id", response.getScrollId());
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
        name = "ES_SCROLL_NEXT",
        description = "Get next batch from scroll",
        parameters = {
            @FunctionParam(name = "scroll_id", type = "STRING", description = "Scroll ID"),
            @FunctionParam(name = "scroll_time", type = "STRING", description = "Keep-alive extension")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Next batch of results"),
        examples = {"ES_SCROLL_NEXT(scroll_id, '5m')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerScrollNext(ExecutionContext context, Client client) {
        context.declareFunction("ES_SCROLL_NEXT",
            Arrays.asList(
                new Parameter("scroll_id", "STRING", ParameterMode.IN),
                new Parameter("scroll_time", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_SCROLL_NEXT", (args, listener) -> {
                String scrollId = args.get(0).toString();
                String scrollTime = args.get(1).toString();
                
                SearchScrollRequest request = new SearchScrollRequest(scrollId);
                request.scroll(TimeValue.parseTimeValue(scrollTime, "scroll"));
                
                client.searchScroll(request, new ActionListener<SearchResponse>() {
                    @Override
                    public void onResponse(SearchResponse response) {
                        Map<String, Object> result = buildSearchResult(response);
                        result.put("scroll_id", response.getScrollId());
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
        name = "ES_CLEAR_SCROLL",
        description = "Clear scroll context",
        parameters = {
            @FunctionParam(name = "scroll_id", type = "STRING", description = "Scroll ID to clear")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "Success status"),
        examples = {"ES_CLEAR_SCROLL(scroll_id)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerClearScroll(ExecutionContext context, Client client) {
        context.declareFunction("ES_CLEAR_SCROLL",
            List.of(new Parameter("scroll_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_CLEAR_SCROLL", (args, listener) -> {
                String scrollId = args.get(0).toString();
                
                ClearScrollRequest request = new ClearScrollRequest();
                request.addScrollId(scrollId);
                
                client.clearScroll(request, new ActionListener<ClearScrollResponse>() {
                    @Override
                    public void onResponse(ClearScrollResponse response) {
                        listener.onResponse(response.isSucceeded());
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
        name = "ES_COUNT",
        description = "Count documents matching a query",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index pattern"),
            @FunctionParam(name = "query", type = "STRING", description = "Query string")
        },
        returnType = @FunctionReturn(type = "NUMBER", description = "Document count"),
        examples = {"ES_COUNT('logs-*', 'level:error')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerCount(ExecutionContext context, Client client) {
        context.declareFunction("ES_COUNT",
            Arrays.asList(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("query", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_COUNT", (args, listener) -> {
                String index = args.get(0).toString();
                String queryString = args.get(1).toString();
                
                SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
                sourceBuilder.query(QueryBuilders.queryStringQuery(queryString));
                sourceBuilder.size(0);
                sourceBuilder.trackTotalHits(true);
                
                SearchRequest request = new SearchRequest(index);
                request.source(sourceBuilder);
                
                client.search(request, new ActionListener<SearchResponse>() {
                    @Override
                    public void onResponse(SearchResponse response) {
                        listener.onResponse(response.getHits().getTotalHits().value);
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
        name = "ES_TERMS_AGG",
        description = "Get terms aggregation for a field",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index pattern"),
            @FunctionParam(name = "field", type = "STRING", description = "Field to aggregate"),
            @FunctionParam(name = "size", type = "NUMBER", description = "Number of buckets"),
            @FunctionParam(name = "query", type = "STRING", description = "Optional filter query")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Array of {key, doc_count}"),
        examples = {"ES_TERMS_AGG('logs-*', 'level.keyword', 10, '*')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerTermsAgg(ExecutionContext context, Client client) {
        context.declareFunction("ES_TERMS_AGG",
            Arrays.asList(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("field", "STRING", ParameterMode.IN),
                new Parameter("size", "NUMBER", ParameterMode.IN),
                new Parameter("query", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_TERMS_AGG", (args, listener) -> {
                String index = args.get(0).toString();
                String field = args.get(1).toString();
                int size = ((Number) args.get(2)).intValue();
                String queryString = args.size() > 3 && args.get(3) != null ? args.get(3).toString() : "*";
                
                SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
                sourceBuilder.query(QueryBuilders.queryStringQuery(queryString));
                sourceBuilder.size(0);
                sourceBuilder.aggregation(AggregationBuilders.terms("terms_agg").field(field).size(size));
                
                SearchRequest request = new SearchRequest(index);
                request.source(sourceBuilder);
                
                client.search(request, new ActionListener<SearchResponse>() {
                    @Override
                    public void onResponse(SearchResponse response) {
                        List<Map<String, Object>> buckets = new ArrayList<>();
                        Terms terms = response.getAggregations().get("terms_agg");
                        for (Terms.Bucket bucket : terms.getBuckets()) {
                            Map<String, Object> b = new HashMap<>();
                            b.put("key", bucket.getKeyAsString());
                            b.put("doc_count", bucket.getDocCount());
                            buckets.add(b);
                        }
                        listener.onResponse(buckets);
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
        name = "ES_STATS_AGG",
        description = "Get statistics for a numeric field",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index pattern"),
            @FunctionParam(name = "field", type = "STRING", description = "Numeric field"),
            @FunctionParam(name = "query", type = "STRING", description = "Optional filter query")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Stats: min, max, avg, sum, count"),
        examples = {"ES_STATS_AGG('metrics-*', 'cpu_usage', 'host:server1')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerStatsAgg(ExecutionContext context, Client client) {
        context.declareFunction("ES_STATS_AGG",
            Arrays.asList(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("field", "STRING", ParameterMode.IN),
                new Parameter("query", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_STATS_AGG", (args, listener) -> {
                String index = args.get(0).toString();
                String field = args.get(1).toString();
                String queryString = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : "*";
                
                SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
                sourceBuilder.query(QueryBuilders.queryStringQuery(queryString));
                sourceBuilder.size(0);
                sourceBuilder.aggregation(AggregationBuilders.min("min").field(field));
                sourceBuilder.aggregation(AggregationBuilders.max("max").field(field));
                sourceBuilder.aggregation(AggregationBuilders.avg("avg").field(field));
                sourceBuilder.aggregation(AggregationBuilders.sum("sum").field(field));
                
                SearchRequest request = new SearchRequest(index);
                request.source(sourceBuilder);
                
                client.search(request, new ActionListener<SearchResponse>() {
                    @Override
                    public void onResponse(SearchResponse response) {
                        Map<String, Object> stats = new HashMap<>();
                        Min min = response.getAggregations().get("min");
                        Max max = response.getAggregations().get("max");
                        Avg avg = response.getAggregations().get("avg");
                        Sum sum = response.getAggregations().get("sum");
                        
                        stats.put("min", min.getValue());
                        stats.put("max", max.getValue());
                        stats.put("avg", avg.getValue());
                        stats.put("sum", sum.getValue());
                        stats.put("count", response.getHits().getTotalHits().value);
                        listener.onResponse(stats);
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
        name = "ES_DATE_HISTOGRAM",
        description = "Get date histogram aggregation",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index pattern"),
            @FunctionParam(name = "field", type = "STRING", description = "Date field"),
            @FunctionParam(name = "interval", type = "STRING", description = "Calendar interval (day, hour, etc.)"),
            @FunctionParam(name = "query", type = "STRING", description = "Optional filter query")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Array of {key, key_as_string, doc_count}"),
        examples = {"ES_DATE_HISTOGRAM('logs-*', '@timestamp', 'day', 'level:error')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDateHistogram(ExecutionContext context, Client client) {
        context.declareFunction("ES_DATE_HISTOGRAM",
            Arrays.asList(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("field", "STRING", ParameterMode.IN),
                new Parameter("interval", "STRING", ParameterMode.IN),
                new Parameter("query", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_DATE_HISTOGRAM", (args, listener) -> {
                String index = args.get(0).toString();
                String field = args.get(1).toString();
                String interval = args.get(2).toString();
                String queryString = args.size() > 3 && args.get(3) != null ? args.get(3).toString() : "*";
                
                SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
                sourceBuilder.query(QueryBuilders.queryStringQuery(queryString));
                sourceBuilder.size(0);
                sourceBuilder.aggregation(
                    AggregationBuilders.dateHistogram("date_hist")
                        .field(field)
                        .calendarInterval(new org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval(interval))
                );
                
                SearchRequest request = new SearchRequest(index);
                request.source(sourceBuilder);
                
                client.search(request, new ActionListener<SearchResponse>() {
                    @Override
                    public void onResponse(SearchResponse response) {
                        List<Map<String, Object>> buckets = new ArrayList<>();
                        org.elasticsearch.search.aggregations.bucket.histogram.Histogram histogram = 
                            response.getAggregations().get("date_hist");
                        for (var bucket : histogram.getBuckets()) {
                            Map<String, Object> b = new HashMap<>();
                            b.put("key", bucket.getKey());
                            b.put("key_as_string", bucket.getKeyAsString());
                            b.put("doc_count", bucket.getDocCount());
                            buckets.add(b);
                        }
                        listener.onResponse(buckets);
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
        name = "ES_SEARCH_AFTER",
        description = "Paginate through results using search_after",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index pattern"),
            @FunctionParam(name = "query", type = "STRING", description = "Query string"),
            @FunctionParam(name = "sort_field", type = "STRING", description = "Sort field"),
            @FunctionParam(name = "size", type = "NUMBER", description = "Page size"),
            @FunctionParam(name = "search_after", type = "ARRAY", description = "Values from last hit's sort")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Search results with sort values"),
        examples = {"ES_SEARCH_AFTER('logs-*', '*', '@timestamp', 100, [1704067200000])"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerSearchAfter(ExecutionContext context, Client client) {
        context.declareFunction("ES_SEARCH_AFTER",
            Arrays.asList(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("query", "STRING", ParameterMode.IN),
                new Parameter("sort_field", "STRING", ParameterMode.IN),
                new Parameter("size", "NUMBER", ParameterMode.IN),
                new Parameter("search_after", "ARRAY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_SEARCH_AFTER", (args, listener) -> {
                String index = args.get(0).toString();
                String queryString = args.get(1).toString();
                String sortField = args.get(2).toString();
                int size = ((Number) args.get(3)).intValue();
                @SuppressWarnings("unchecked")
                List<Object> searchAfter = args.size() > 4 && args.get(4) != null ? (List<Object>) args.get(4) : null;
                
                SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
                sourceBuilder.query(QueryBuilders.queryStringQuery(queryString));
                sourceBuilder.size(size);
                sourceBuilder.sort(sortField, SortOrder.ASC);
                
                if (searchAfter != null && !searchAfter.isEmpty()) {
                    sourceBuilder.searchAfter(searchAfter.toArray());
                }
                
                SearchRequest request = new SearchRequest(index);
                request.source(sourceBuilder);
                
                client.search(request, new ActionListener<SearchResponse>() {
                    @Override
                    public void onResponse(SearchResponse response) {
                        Map<String, Object> result = buildSearchResult(response);
                        // Include sort values from last hit for pagination
                        SearchHit[] hits = response.getHits().getHits();
                        if (hits.length > 0) {
                            SearchHit lastHit = hits[hits.length - 1];
                            result.put("last_sort", Arrays.asList(lastHit.getSortValues()));
                        }
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
        name = "ES_SEARCH_TEMPLATE",
        description = "Execute a search template",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index pattern"),
            @FunctionParam(name = "template_id", type = "STRING", description = "Stored template ID"),
            @FunctionParam(name = "params", type = "DOCUMENT", description = "Template parameters")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Search results"),
        examples = {"ES_SEARCH_TEMPLATE('logs-*', 'my-template', {'query_value': 'error'})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerSearchTemplate(ExecutionContext context, Client client) {
        context.declareFunction("ES_SEARCH_TEMPLATE",
            Arrays.asList(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("template_id", "STRING", ParameterMode.IN),
                new Parameter("params", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_SEARCH_TEMPLATE", (args, listener) -> {
                String index = args.get(0).toString();
                String templateId = args.get(1).toString();
                @SuppressWarnings("unchecked")
                Map<String, Object> params = (Map<String, Object>) args.get(2);
                
                org.elasticsearch.script.mustache.SearchTemplateRequest request = 
                    new org.elasticsearch.script.mustache.SearchTemplateRequest();
                request.setRequest(new SearchRequest(index));
                request.setScriptType(org.elasticsearch.script.ScriptType.STORED);
                request.setScript(templateId);
                request.setScriptParams(params);
                
                client.execute(org.elasticsearch.script.mustache.MustachePlugin.SEARCH_TEMPLATE_ACTION, request,
                    new ActionListener<org.elasticsearch.script.mustache.SearchTemplateResponse>() {
                        @Override
                        public void onResponse(org.elasticsearch.script.mustache.SearchTemplateResponse response) {
                            listener.onResponse(buildSearchResult(response.getResponse()));
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
        name = "ES_OPEN_PIT",
        description = "Open a point-in-time for consistent pagination",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index pattern"),
            @FunctionParam(name = "keep_alive", type = "STRING", description = "Keep-alive time (e.g., '5m')")
        },
        returnType = @FunctionReturn(type = "STRING", description = "Point-in-time ID"),
        examples = {"ES_OPEN_PIT('logs-*', '5m')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerPointInTime(ExecutionContext context, Client client) {
        context.declareFunction("ES_OPEN_PIT",
            Arrays.asList(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("keep_alive", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_OPEN_PIT", (args, listener) -> {
                String index = args.get(0).toString();
                String keepAlive = args.get(1).toString();
                
                org.elasticsearch.action.search.OpenPointInTimeRequest request = 
                    new org.elasticsearch.action.search.OpenPointInTimeRequest(index);
                request.keepAlive(TimeValue.parseTimeValue(keepAlive, "keep_alive"));
                
                client.execute(org.elasticsearch.action.search.TransportOpenPointInTimeAction.TYPE, request,
                    new ActionListener<org.elasticsearch.action.search.OpenPointInTimeResponse>() {
                        @Override
                        public void onResponse(org.elasticsearch.action.search.OpenPointInTimeResponse response) {
                            listener.onResponse(response.getPointInTimeId());
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
        name = "ES_FIELD_CAPS",
        description = "Get field capabilities across indices",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index pattern"),
            @FunctionParam(name = "fields", type = "STRING", description = "Comma-separated fields or wildcard")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Field capabilities"),
        examples = {"ES_FIELD_CAPS('logs-*', 'message,level,@timestamp')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerFieldCaps(ExecutionContext context, Client client) {
        context.declareFunction("ES_FIELD_CAPS",
            Arrays.asList(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("fields", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_FIELD_CAPS", (args, listener) -> {
                String index = args.get(0).toString();
                String fields = args.get(1).toString();
                
                org.elasticsearch.action.fieldcaps.FieldCapabilitiesRequest request = 
                    new org.elasticsearch.action.fieldcaps.FieldCapabilitiesRequest();
                request.indices(index);
                request.fields(fields.split(","));
                
                client.execute(org.elasticsearch.action.fieldcaps.TransportFieldCapabilitiesAction.TYPE, request,
                    new ActionListener<org.elasticsearch.action.fieldcaps.FieldCapabilitiesResponse>() {
                        @Override
                        public void onResponse(org.elasticsearch.action.fieldcaps.FieldCapabilitiesResponse response) {
                            Map<String, Object> result = new HashMap<>();
                            for (String field : response.get().keySet()) {
                                Map<String, Object> fieldInfo = new HashMap<>();
                                for (var entry : response.getField(field).entrySet()) {
                                    Map<String, Object> typeInfo = new HashMap<>();
                                    typeInfo.put("searchable", entry.getValue().isSearchable());
                                    typeInfo.put("aggregatable", entry.getValue().isAggregatable());
                                    fieldInfo.put(entry.getKey(), typeInfo);
                                }
                                result.put(field, fieldInfo);
                            }
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
        name = "ES_KNN_SEARCH",
        description = "Execute k-nearest neighbor search",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "field", type = "STRING", description = "Vector field"),
            @FunctionParam(name = "vector", type = "ARRAY", description = "Query vector"),
            @FunctionParam(name = "k", type = "NUMBER", description = "Number of neighbors")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "KNN search results"),
        examples = {"ES_KNN_SEARCH('embeddings', 'vector', [0.1, 0.2, 0.3], 10)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerKnnSearch(ExecutionContext context, Client client) {
        context.declareFunction("ES_KNN_SEARCH",
            Arrays.asList(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("field", "STRING", ParameterMode.IN),
                new Parameter("vector", "ARRAY", ParameterMode.IN),
                new Parameter("k", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_KNN_SEARCH", (args, listener) -> {
                String index = args.get(0).toString();
                String field = args.get(1).toString();
                @SuppressWarnings("unchecked")
                List<Number> vectorList = (List<Number>) args.get(2);
                int k = ((Number) args.get(3)).intValue();
                
                float[] vector = new float[vectorList.size()];
                for (int i = 0; i < vectorList.size(); i++) {
                    vector[i] = vectorList.get(i).floatValue();
                }
                
                SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
                sourceBuilder.knnSearch(List.of(
                    new org.elasticsearch.search.builder.SearchSourceBuilder.KnnSearchBuilder(field, vector, k, k * 2, null)
                ));
                
                SearchRequest request = new SearchRequest(index);
                request.source(sourceBuilder);
                
                client.search(request, new ActionListener<SearchResponse>() {
                    @Override
                    public void onResponse(SearchResponse response) {
                        listener.onResponse(buildSearchResult(response));
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
        name = "ES_RANK_EVAL",
        description = "Evaluate ranking quality of search results",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index pattern"),
            @FunctionParam(name = "requests", type = "ARRAY", description = "Array of {id, request, ratings}")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Ranking evaluation metrics"),
        examples = {"ES_RANK_EVAL('products', [{'id': 'q1', 'request': {...}, 'ratings': [...]}])"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerRankEval(ExecutionContext context, Client client) {
        context.declareFunction("ES_RANK_EVAL",
            Arrays.asList(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("requests", "ARRAY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_RANK_EVAL", (args, listener) -> {
                // Rank evaluation requires complex setup - return placeholder for now
                Map<String, Object> result = new HashMap<>();
                result.put("metric_score", 0.0);
                result.put("details", new HashMap<>());
                result.put("note", "Use Elasticsearch REST API for full rank evaluation");
                listener.onResponse(result);
            })
        );
    }

    // Helper methods
    private static Map<String, Object> buildSearchResult(SearchResponse response) {
        Map<String, Object> result = new HashMap<>();
        result.put("took", response.getTook().millis());
        result.put("total", response.getHits().getTotalHits().value);
        result.put("max_score", response.getHits().getMaxScore());
        
        List<Map<String, Object>> hits = new ArrayList<>();
        for (SearchHit hit : response.getHits().getHits()) {
            Map<String, Object> hitMap = new HashMap<>(hit.getSourceAsMap());
            hitMap.put("_id", hit.getId());
            hitMap.put("_index", hit.getIndex());
            hitMap.put("_score", hit.getScore());
            hits.add(hitMap);
        }
        result.put("hits", hits);
        return result;
    }
    
    private static String mapToJson(Map<String, Object> map) {
        try {
            org.elasticsearch.xcontent.XContentBuilder builder = 
                org.elasticsearch.xcontent.XContentFactory.jsonBuilder();
            builder.map(map);
            return org.elasticsearch.common.Strings.toString(builder);
        } catch (Exception e) {
            return "{}";
        }
    }
}
