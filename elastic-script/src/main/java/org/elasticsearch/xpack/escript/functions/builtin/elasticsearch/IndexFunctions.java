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
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesResponse;
import org.elasticsearch.action.admin.indices.cache.clear.ClearIndicesCacheRequest;
import org.elasticsearch.action.admin.indices.cache.clear.ClearIndicesCacheResponse;
import org.elasticsearch.action.admin.indices.close.CloseIndexRequest;
import org.elasticsearch.action.admin.indices.close.CloseIndexResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.admin.indices.flush.FlushResponse;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeRequest;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest;
import org.elasticsearch.action.admin.indices.open.OpenIndexResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsRequest;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.common.settings.Settings;
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
 * Elasticsearch Index Management API functions.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.DATASOURCE,
    description = "Elasticsearch Index Management API functions"
)
public class IndexFunctions {

    public static void registerAll(ExecutionContext context, Client client) {
        registerCreateIndex(context, client);
        registerDeleteIndex(context, client);
        registerIndexExists(context, client);
        registerGetMapping(context, client);
        registerPutMapping(context, client);
        registerGetSettings(context, client);
        registerPutSettings(context, client);
        registerRefreshIndex(context, client);
        registerFlushIndex(context, client);
        registerForceMerge(context, client);
        registerClearCache(context, client);
        registerOpenIndex(context, client);
        registerCloseIndex(context, client);
        registerGetAlias(context, client);
        registerPutAlias(context, client);
        registerDeleteAlias(context, client);
        registerIndexStats(context, client);
        registerCloneIndex(context, client);
        registerShrinkIndex(context, client);
        registerSplitIndex(context, client);
        registerRolloverIndex(context, client);
        registerResolveIndex(context, client);
    }

    @FunctionSpec(
        name = "ES_CREATE_INDEX",
        description = "Create a new index with optional settings and mappings",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "settings", type = "DOCUMENT", description = "Index settings"),
            @FunctionParam(name = "mappings", type = "DOCUMENT", description = "Index mappings")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if created"),
        examples = {"ES_CREATE_INDEX('my-index', {'number_of_shards': 1}, {'properties': {'field': {'type': 'text'}}})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerCreateIndex(ExecutionContext context, Client client) {
        context.declareFunction("ES_CREATE_INDEX",
            Arrays.asList(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("settings", "DOCUMENT", ParameterMode.IN),
                new Parameter("mappings", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_CREATE_INDEX", (args, listener) -> {
                String index = args.get(0).toString();
                
                CreateIndexRequest request = new CreateIndexRequest(index);
                
                if (args.size() > 1 && args.get(1) != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> settings = (Map<String, Object>) args.get(1);
                    Settings.Builder settingsBuilder = Settings.builder();
                    for (Map.Entry<String, Object> entry : settings.entrySet()) {
                        settingsBuilder.put(entry.getKey(), entry.getValue().toString());
                    }
                    request.settings(settingsBuilder);
                }
                
                if (args.size() > 2 && args.get(2) != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> mappings = (Map<String, Object>) args.get(2);
                    request.mapping(mappings);
                }
                
                client.admin().indices().create(request, new ActionListener<CreateIndexResponse>() {
                    @Override
                    public void onResponse(CreateIndexResponse response) {
                        listener.onResponse(response.isAcknowledged());
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
        name = "ES_DELETE_INDEX",
        description = "Delete an index",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name or pattern")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if deleted"),
        examples = {"ES_DELETE_INDEX('old-index')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDeleteIndex(ExecutionContext context, Client client) {
        context.declareFunction("ES_DELETE_INDEX",
            List.of(new Parameter("index", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_DELETE_INDEX", (args, listener) -> {
                String index = args.get(0).toString();
                
                DeleteIndexRequest request = new DeleteIndexRequest(index);
                client.admin().indices().delete(request, new ActionListener<AcknowledgedResponse>() {
                    @Override
                    public void onResponse(AcknowledgedResponse response) {
                        listener.onResponse(response.isAcknowledged());
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
        name = "ES_INDEX_EXISTS",
        description = "Check if an index exists",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if exists"),
        examples = {"ES_INDEX_EXISTS('my-index')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerIndexExists(ExecutionContext context, Client client) {
        context.declareFunction("ES_INDEX_EXISTS",
            List.of(new Parameter("index", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_INDEX_EXISTS", (args, listener) -> {
                String index = args.get(0).toString();
                
                client.admin().indices().prepareExists(index).execute(new ActionListener<org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse>() {
                    @Override
                    public void onResponse(org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse response) {
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
        name = "ES_GET_MAPPING",
        description = "Get index mappings",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Index mappings"),
        examples = {"ES_GET_MAPPING('my-index')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetMapping(ExecutionContext context, Client client) {
        context.declareFunction("ES_GET_MAPPING",
            List.of(new Parameter("index", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_GET_MAPPING", (args, listener) -> {
                String index = args.get(0).toString();
                
                GetMappingsRequest request = new GetMappingsRequest().indices(index);
                client.admin().indices().getMappings(request, new ActionListener<GetMappingsResponse>() {
                    @Override
                    public void onResponse(GetMappingsResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        for (var entry : response.mappings().entrySet()) {
                            MappingMetadata mapping = entry.getValue();
                            result.put(entry.getKey(), mapping.sourceAsMap());
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
        name = "ES_PUT_MAPPING",
        description = "Update index mappings",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "mappings", type = "DOCUMENT", description = "Mapping definition")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if acknowledged"),
        examples = {"ES_PUT_MAPPING('my-index', {'properties': {'new_field': {'type': 'keyword'}}})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerPutMapping(ExecutionContext context, Client client) {
        context.declareFunction("ES_PUT_MAPPING",
            Arrays.asList(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("mappings", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_PUT_MAPPING", (args, listener) -> {
                String index = args.get(0).toString();
                @SuppressWarnings("unchecked")
                Map<String, Object> mappings = (Map<String, Object>) args.get(1);
                
                PutMappingRequest request = new PutMappingRequest(index);
                request.source(mappings);
                
                client.admin().indices().putMapping(request, new ActionListener<AcknowledgedResponse>() {
                    @Override
                    public void onResponse(AcknowledgedResponse response) {
                        listener.onResponse(response.isAcknowledged());
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
        name = "ES_GET_SETTINGS",
        description = "Get index settings",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Index settings"),
        examples = {"ES_GET_SETTINGS('my-index')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetSettings(ExecutionContext context, Client client) {
        context.declareFunction("ES_GET_SETTINGS",
            List.of(new Parameter("index", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_GET_SETTINGS", (args, listener) -> {
                String index = args.get(0).toString();
                
                GetSettingsRequest request = new GetSettingsRequest().indices(index);
                client.admin().indices().getSettings(request, new ActionListener<GetSettingsResponse>() {
                    @Override
                    public void onResponse(GetSettingsResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        for (var cursor : response.getIndexToSettings().entrySet()) {
                            Map<String, Object> settings = new HashMap<>();
                            Settings indexSettings = cursor.getValue();
                            for (String key : indexSettings.keySet()) {
                                settings.put(key, indexSettings.get(key));
                            }
                            result.put(cursor.getKey(), settings);
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
        name = "ES_PUT_SETTINGS",
        description = "Update index settings",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "settings", type = "DOCUMENT", description = "Settings to update")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if acknowledged"),
        examples = {"ES_PUT_SETTINGS('my-index', {'index.number_of_replicas': 2})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerPutSettings(ExecutionContext context, Client client) {
        context.declareFunction("ES_PUT_SETTINGS",
            Arrays.asList(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("settings", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_PUT_SETTINGS", (args, listener) -> {
                String index = args.get(0).toString();
                @SuppressWarnings("unchecked")
                Map<String, Object> settingsMap = (Map<String, Object>) args.get(1);
                
                Settings.Builder settingsBuilder = Settings.builder();
                for (Map.Entry<String, Object> entry : settingsMap.entrySet()) {
                    settingsBuilder.put(entry.getKey(), entry.getValue().toString());
                }
                
                UpdateSettingsRequest request = new UpdateSettingsRequest(index);
                request.settings(settingsBuilder);
                
                client.admin().indices().updateSettings(request, new ActionListener<AcknowledgedResponse>() {
                    @Override
                    public void onResponse(AcknowledgedResponse response) {
                        listener.onResponse(response.isAcknowledged());
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
        name = "ES_REFRESH_INDEX",
        description = "Refresh an index to make changes visible",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name or pattern")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Refresh stats"),
        examples = {"ES_REFRESH_INDEX('my-index')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerRefreshIndex(ExecutionContext context, Client client) {
        context.declareFunction("ES_REFRESH_INDEX",
            List.of(new Parameter("index", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_REFRESH_INDEX", (args, listener) -> {
                String index = args.get(0).toString();
                
                RefreshRequest request = new RefreshRequest(index);
                client.admin().indices().refresh(request, new ActionListener<RefreshResponse>() {
                    @Override
                    public void onResponse(RefreshResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("total_shards", response.getTotalShards());
                        result.put("successful_shards", response.getSuccessfulShards());
                        result.put("failed_shards", response.getFailedShards());
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
        name = "ES_FLUSH_INDEX",
        description = "Flush an index to persist changes to disk",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name or pattern")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Flush stats"),
        examples = {"ES_FLUSH_INDEX('my-index')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerFlushIndex(ExecutionContext context, Client client) {
        context.declareFunction("ES_FLUSH_INDEX",
            List.of(new Parameter("index", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_FLUSH_INDEX", (args, listener) -> {
                String index = args.get(0).toString();
                
                FlushRequest request = new FlushRequest(index);
                client.admin().indices().flush(request, new ActionListener<FlushResponse>() {
                    @Override
                    public void onResponse(FlushResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("total_shards", response.getTotalShards());
                        result.put("successful_shards", response.getSuccessfulShards());
                        result.put("failed_shards", response.getFailedShards());
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
        name = "ES_FORCE_MERGE",
        description = "Force merge index segments",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "max_segments", type = "NUMBER", description = "Target number of segments")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Merge stats"),
        examples = {"ES_FORCE_MERGE('my-index', 1)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerForceMerge(ExecutionContext context, Client client) {
        context.declareFunction("ES_FORCE_MERGE",
            Arrays.asList(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("max_segments", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_FORCE_MERGE", (args, listener) -> {
                String index = args.get(0).toString();
                int maxSegments = args.size() > 1 && args.get(1) != null ? ((Number) args.get(1)).intValue() : 1;
                
                ForceMergeRequest request = new ForceMergeRequest(index);
                request.maxNumSegments(maxSegments);
                
                client.admin().indices().forceMerge(request, new ActionListener<ForceMergeResponse>() {
                    @Override
                    public void onResponse(ForceMergeResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("total_shards", response.getTotalShards());
                        result.put("successful_shards", response.getSuccessfulShards());
                        result.put("failed_shards", response.getFailedShards());
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
        name = "ES_CLEAR_CACHE",
        description = "Clear index caches",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name or pattern")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Cache clear stats"),
        examples = {"ES_CLEAR_CACHE('my-index')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerClearCache(ExecutionContext context, Client client) {
        context.declareFunction("ES_CLEAR_CACHE",
            List.of(new Parameter("index", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_CLEAR_CACHE", (args, listener) -> {
                String index = args.get(0).toString();
                
                ClearIndicesCacheRequest request = new ClearIndicesCacheRequest(index);
                client.admin().indices().clearCache(request, new ActionListener<ClearIndicesCacheResponse>() {
                    @Override
                    public void onResponse(ClearIndicesCacheResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("total_shards", response.getTotalShards());
                        result.put("successful_shards", response.getSuccessfulShards());
                        result.put("failed_shards", response.getFailedShards());
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
        name = "ES_OPEN_INDEX",
        description = "Open a closed index",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if acknowledged"),
        examples = {"ES_OPEN_INDEX('my-index')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerOpenIndex(ExecutionContext context, Client client) {
        context.declareFunction("ES_OPEN_INDEX",
            List.of(new Parameter("index", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_OPEN_INDEX", (args, listener) -> {
                String index = args.get(0).toString();
                
                OpenIndexRequest request = new OpenIndexRequest(index);
                client.admin().indices().open(request, new ActionListener<OpenIndexResponse>() {
                    @Override
                    public void onResponse(OpenIndexResponse response) {
                        listener.onResponse(response.isAcknowledged());
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
        name = "ES_CLOSE_INDEX",
        description = "Close an index",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if acknowledged"),
        examples = {"ES_CLOSE_INDEX('my-index')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerCloseIndex(ExecutionContext context, Client client) {
        context.declareFunction("ES_CLOSE_INDEX",
            List.of(new Parameter("index", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_CLOSE_INDEX", (args, listener) -> {
                String index = args.get(0).toString();
                
                CloseIndexRequest request = new CloseIndexRequest(index);
                client.admin().indices().close(request, new ActionListener<CloseIndexResponse>() {
                    @Override
                    public void onResponse(CloseIndexResponse response) {
                        listener.onResponse(response.isAcknowledged());
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
        name = "ES_GET_ALIAS",
        description = "Get aliases for an index",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name or pattern")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Alias information"),
        examples = {"ES_GET_ALIAS('my-index')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetAlias(ExecutionContext context, Client client) {
        context.declareFunction("ES_GET_ALIAS",
            List.of(new Parameter("index", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_GET_ALIAS", (args, listener) -> {
                String index = args.get(0).toString();
                
                GetAliasesRequest request = new GetAliasesRequest().indices(index);
                client.admin().indices().getAliases(request, new ActionListener<GetAliasesResponse>() {
                    @Override
                    public void onResponse(GetAliasesResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        for (var entry : response.getAliases().entrySet()) {
                            List<String> aliases = new java.util.ArrayList<>();
                            for (var alias : entry.getValue()) {
                                aliases.add(alias.alias());
                            }
                            result.put(entry.getKey(), aliases);
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
        name = "ES_PUT_ALIAS",
        description = "Add an alias to an index",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "alias", type = "STRING", description = "Alias name")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if acknowledged"),
        examples = {"ES_PUT_ALIAS('my-index-v2', 'my-index')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerPutAlias(ExecutionContext context, Client client) {
        context.declareFunction("ES_PUT_ALIAS",
            Arrays.asList(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("alias", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_PUT_ALIAS", (args, listener) -> {
                String index = args.get(0).toString();
                String alias = args.get(1).toString();
                
                IndicesAliasesRequest request = new IndicesAliasesRequest();
                request.addAliasAction(IndicesAliasesRequest.AliasActions.add().index(index).alias(alias));
                
                client.admin().indices().aliases(request, new ActionListener<AcknowledgedResponse>() {
                    @Override
                    public void onResponse(AcknowledgedResponse response) {
                        listener.onResponse(response.isAcknowledged());
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
        name = "ES_DELETE_ALIAS",
        description = "Remove an alias from an index",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name"),
            @FunctionParam(name = "alias", type = "STRING", description = "Alias name")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if acknowledged"),
        examples = {"ES_DELETE_ALIAS('my-index', 'old-alias')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDeleteAlias(ExecutionContext context, Client client) {
        context.declareFunction("ES_DELETE_ALIAS",
            Arrays.asList(
                new Parameter("index", "STRING", ParameterMode.IN),
                new Parameter("alias", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_DELETE_ALIAS", (args, listener) -> {
                String index = args.get(0).toString();
                String alias = args.get(1).toString();
                
                IndicesAliasesRequest request = new IndicesAliasesRequest();
                request.addAliasAction(IndicesAliasesRequest.AliasActions.remove().index(index).alias(alias));
                
                client.admin().indices().aliases(request, new ActionListener<AcknowledgedResponse>() {
                    @Override
                    public void onResponse(AcknowledgedResponse response) {
                        listener.onResponse(response.isAcknowledged());
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
        name = "ES_INDEX_STATS",
        description = "Get index statistics",
        parameters = {
            @FunctionParam(name = "index", type = "STRING", description = "Index name or pattern")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Index statistics"),
        examples = {"ES_INDEX_STATS('my-index')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerIndexStats(ExecutionContext context, Client client) {
        context.declareFunction("ES_INDEX_STATS",
            List.of(new Parameter("index", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_INDEX_STATS", (args, listener) -> {
                String index = args.get(0).toString();
                
                IndicesStatsRequest request = new IndicesStatsRequest().indices(index);
                client.admin().indices().stats(request, new ActionListener<IndicesStatsResponse>() {
                    @Override
                    public void onResponse(IndicesStatsResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        var total = response.getTotal();
                        
                        Map<String, Object> docs = new HashMap<>();
                        docs.put("count", total.getDocs().getCount());
                        docs.put("deleted", total.getDocs().getDeleted());
                        result.put("docs", docs);
                        
                        Map<String, Object> store = new HashMap<>();
                        store.put("size_in_bytes", total.getStore().sizeInBytes());
                        result.put("store", store);
                        
                        Map<String, Object> indexing = new HashMap<>();
                        indexing.put("index_total", total.getIndexing().getTotal().getIndexCount());
                        indexing.put("index_time_ms", total.getIndexing().getTotal().getIndexTime().millis());
                        result.put("indexing", indexing);
                        
                        Map<String, Object> search = new HashMap<>();
                        search.put("query_total", total.getSearch().getTotal().getQueryCount());
                        search.put("query_time_ms", total.getSearch().getTotal().getQueryTime().millis());
                        result.put("search", search);
                        
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
        name = "ES_CLONE_INDEX",
        description = "Clone an index",
        parameters = {
            @FunctionParam(name = "source_index", type = "STRING", description = "Source index"),
            @FunctionParam(name = "target_index", type = "STRING", description = "Target index")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if acknowledged"),
        examples = {"ES_CLONE_INDEX('my-index', 'my-index-clone')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerCloneIndex(ExecutionContext context, Client client) {
        context.declareFunction("ES_CLONE_INDEX",
            Arrays.asList(
                new Parameter("source_index", "STRING", ParameterMode.IN),
                new Parameter("target_index", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_CLONE_INDEX", (args, listener) -> {
                String sourceIndex = args.get(0).toString();
                String targetIndex = args.get(1).toString();
                
                org.elasticsearch.action.admin.indices.shrink.ResizeRequest request = 
                    new org.elasticsearch.action.admin.indices.shrink.ResizeRequest(targetIndex, sourceIndex);
                request.setResizeType(org.elasticsearch.action.admin.indices.shrink.ResizeType.CLONE);
                
                client.admin().indices().resizeIndex(request, new ActionListener<org.elasticsearch.action.admin.indices.shrink.ResizeResponse>() {
                    @Override
                    public void onResponse(org.elasticsearch.action.admin.indices.shrink.ResizeResponse response) {
                        listener.onResponse(response.isAcknowledged());
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
        name = "ES_SHRINK_INDEX",
        description = "Shrink an index to fewer shards",
        parameters = {
            @FunctionParam(name = "source_index", type = "STRING", description = "Source index"),
            @FunctionParam(name = "target_index", type = "STRING", description = "Target index"),
            @FunctionParam(name = "num_shards", type = "NUMBER", description = "Target number of shards")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if acknowledged"),
        examples = {"ES_SHRINK_INDEX('my-index', 'my-index-shrunk', 1)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerShrinkIndex(ExecutionContext context, Client client) {
        context.declareFunction("ES_SHRINK_INDEX",
            Arrays.asList(
                new Parameter("source_index", "STRING", ParameterMode.IN),
                new Parameter("target_index", "STRING", ParameterMode.IN),
                new Parameter("num_shards", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_SHRINK_INDEX", (args, listener) -> {
                String sourceIndex = args.get(0).toString();
                String targetIndex = args.get(1).toString();
                int numShards = ((Number) args.get(2)).intValue();
                
                org.elasticsearch.action.admin.indices.shrink.ResizeRequest request = 
                    new org.elasticsearch.action.admin.indices.shrink.ResizeRequest(targetIndex, sourceIndex);
                request.setResizeType(org.elasticsearch.action.admin.indices.shrink.ResizeType.SHRINK);
                request.getTargetIndexRequest().settings(Settings.builder()
                    .put("index.number_of_shards", numShards)
                    .build());
                
                client.admin().indices().resizeIndex(request, new ActionListener<org.elasticsearch.action.admin.indices.shrink.ResizeResponse>() {
                    @Override
                    public void onResponse(org.elasticsearch.action.admin.indices.shrink.ResizeResponse response) {
                        listener.onResponse(response.isAcknowledged());
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
        name = "ES_SPLIT_INDEX",
        description = "Split an index into more shards",
        parameters = {
            @FunctionParam(name = "source_index", type = "STRING", description = "Source index"),
            @FunctionParam(name = "target_index", type = "STRING", description = "Target index"),
            @FunctionParam(name = "num_shards", type = "NUMBER", description = "Target number of shards")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if acknowledged"),
        examples = {"ES_SPLIT_INDEX('my-index', 'my-index-split', 4)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerSplitIndex(ExecutionContext context, Client client) {
        context.declareFunction("ES_SPLIT_INDEX",
            Arrays.asList(
                new Parameter("source_index", "STRING", ParameterMode.IN),
                new Parameter("target_index", "STRING", ParameterMode.IN),
                new Parameter("num_shards", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_SPLIT_INDEX", (args, listener) -> {
                String sourceIndex = args.get(0).toString();
                String targetIndex = args.get(1).toString();
                int numShards = ((Number) args.get(2)).intValue();
                
                org.elasticsearch.action.admin.indices.shrink.ResizeRequest request = 
                    new org.elasticsearch.action.admin.indices.shrink.ResizeRequest(targetIndex, sourceIndex);
                request.setResizeType(org.elasticsearch.action.admin.indices.shrink.ResizeType.SPLIT);
                request.getTargetIndexRequest().settings(Settings.builder()
                    .put("index.number_of_shards", numShards)
                    .build());
                
                client.admin().indices().resizeIndex(request, new ActionListener<org.elasticsearch.action.admin.indices.shrink.ResizeResponse>() {
                    @Override
                    public void onResponse(org.elasticsearch.action.admin.indices.shrink.ResizeResponse response) {
                        listener.onResponse(response.isAcknowledged());
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
        name = "ES_ROLLOVER_INDEX",
        description = "Rollover an index alias to a new index",
        parameters = {
            @FunctionParam(name = "alias", type = "STRING", description = "Alias to rollover"),
            @FunctionParam(name = "conditions", type = "DOCUMENT", description = "Rollover conditions")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Rollover result"),
        examples = {"ES_ROLLOVER_INDEX('logs', {'max_docs': 10000000})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerRolloverIndex(ExecutionContext context, Client client) {
        context.declareFunction("ES_ROLLOVER_INDEX",
            Arrays.asList(
                new Parameter("alias", "STRING", ParameterMode.IN),
                new Parameter("conditions", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_ROLLOVER_INDEX", (args, listener) -> {
                String alias = args.get(0).toString();
                @SuppressWarnings("unchecked")
                Map<String, Object> conditions = (Map<String, Object>) args.get(1);
                
                org.elasticsearch.action.admin.indices.rollover.RolloverRequest request = 
                    new org.elasticsearch.action.admin.indices.rollover.RolloverRequest(alias, null);
                
                if (conditions.containsKey("max_docs")) {
                    request.addMaxIndexDocsCondition(((Number) conditions.get("max_docs")).longValue());
                }
                if (conditions.containsKey("max_age")) {
                    request.addMaxIndexAgeCondition(org.elasticsearch.core.TimeValue.parseTimeValue(
                        conditions.get("max_age").toString(), "max_age"));
                }
                if (conditions.containsKey("max_size")) {
                    request.addMaxIndexSizeCondition(org.elasticsearch.common.unit.ByteSizeValue.parseBytesSizeValue(
                        conditions.get("max_size").toString(), "max_size"));
                }
                
                client.admin().indices().rolloverIndex(request, new ActionListener<org.elasticsearch.action.admin.indices.rollover.RolloverResponse>() {
                    @Override
                    public void onResponse(org.elasticsearch.action.admin.indices.rollover.RolloverResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("acknowledged", response.isAcknowledged());
                        result.put("rolled_over", response.isRolledOver());
                        result.put("old_index", response.getOldIndex());
                        result.put("new_index", response.getNewIndex());
                        result.put("conditions_met", response.getConditionStatus());
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
        name = "ES_RESOLVE_INDEX",
        description = "Resolve index names and aliases",
        parameters = {
            @FunctionParam(name = "name", type = "STRING", description = "Index name or pattern")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Resolved indices and aliases"),
        examples = {"ES_RESOLVE_INDEX('logs-*')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerResolveIndex(ExecutionContext context, Client client) {
        context.declareFunction("ES_RESOLVE_INDEX",
            List.of(new Parameter("name", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_RESOLVE_INDEX", (args, listener) -> {
                String name = args.get(0).toString();
                
                org.elasticsearch.action.admin.indices.resolve.ResolveIndexAction.Request request = 
                    new org.elasticsearch.action.admin.indices.resolve.ResolveIndexAction.Request(new String[]{name});
                
                client.execute(org.elasticsearch.action.admin.indices.resolve.ResolveIndexAction.INSTANCE, request,
                    new ActionListener<org.elasticsearch.action.admin.indices.resolve.ResolveIndexAction.Response>() {
                        @Override
                        public void onResponse(org.elasticsearch.action.admin.indices.resolve.ResolveIndexAction.Response response) {
                            Map<String, Object> result = new HashMap<>();
                            List<String> indices = new java.util.ArrayList<>();
                            for (var idx : response.getIndices()) {
                                indices.add(idx.getName());
                            }
                            result.put("indices", indices);
                            
                            List<String> aliases = new java.util.ArrayList<>();
                            for (var alias : response.getAliases()) {
                                aliases.add(alias.getName());
                            }
                            result.put("aliases", aliases);
                            
                            List<String> dataStreams = new java.util.ArrayList<>();
                            for (var ds : response.getDataStreams()) {
                                dataStreams.add(ds.getName());
                            }
                            result.put("data_streams", dataStreams);
                            
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
