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
 * Elasticsearch Snapshot and Restore API functions using REST HTTP calls.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.DATASOURCE,
    description = "Elasticsearch Snapshot and Restore API functions."
)
public class SnapshotApiFunctions extends ElasticsearchHttpFunctions {

    public static void registerAll(ExecutionContext context) {
        registerGetRepositories(context);
        registerCreateRepository(context);
        registerDeleteRepository(context);
        registerGetSnapshots(context);
        registerCreateSnapshot(context);
        registerDeleteSnapshot(context);
        registerRestoreSnapshot(context);
        registerSnapshotStatus(context);
        registerCloneSnapshot(context);
    }

    @FunctionSpec(
        name = "ES_GET_REPOSITORIES",
        description = "Get snapshot repositories",
        parameters = {
            @FunctionParam(name = "repository", type = "STRING", description = "Repository name or _all")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Repository information"),
        examples = {"ES_GET_REPOSITORIES('my-repo')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetRepositories(ExecutionContext context) {
        context.declareFunction("ES_GET_REPOSITORIES",
            List.of(new Parameter("repository", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_GET_REPOSITORIES", (args, listener) -> {
                try {
                    String repo = args.size() > 0 && args.get(0) != null ? toString(args.get(0)) : "_all";
                    Map<String, Object> result = esRequest("GET", "/_snapshot/" + repo, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_GET_REPOSITORIES failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_CREATE_REPOSITORY",
        description = "Create a snapshot repository",
        parameters = {
            @FunctionParam(name = "repository", type = "STRING", description = "Repository name"),
            @FunctionParam(name = "settings", type = "DOCUMENT", description = "Repository settings")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Acknowledgment"),
        examples = {"ES_CREATE_REPOSITORY('my-repo', {'type': 'fs', 'settings': {'location': '/backup'}})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerCreateRepository(ExecutionContext context) {
        context.declareFunction("ES_CREATE_REPOSITORY",
            List.of(
                new Parameter("repository", "STRING", ParameterMode.IN),
                new Parameter("settings", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_CREATE_REPOSITORY", (args, listener) -> {
                try {
                    String repo = toString(args.get(0));
                    Map<String, Object> settings = toMap(args.get(1));
                    Map<String, Object> result = esRequest("PUT", "/_snapshot/" + repo, settings);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_CREATE_REPOSITORY failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_DELETE_REPOSITORY",
        description = "Delete a snapshot repository",
        parameters = {
            @FunctionParam(name = "repository", type = "STRING", description = "Repository name")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Acknowledgment"),
        examples = {"ES_DELETE_REPOSITORY('my-repo')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDeleteRepository(ExecutionContext context) {
        context.declareFunction("ES_DELETE_REPOSITORY",
            List.of(new Parameter("repository", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_DELETE_REPOSITORY", (args, listener) -> {
                try {
                    String repo = toString(args.get(0));
                    Map<String, Object> result = esRequest("DELETE", "/_snapshot/" + repo, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_DELETE_REPOSITORY failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_GET_SNAPSHOTS",
        description = "Get snapshots from a repository",
        parameters = {
            @FunctionParam(name = "repository", type = "STRING", description = "Repository name"),
            @FunctionParam(name = "snapshot", type = "STRING", description = "Snapshot name or _all")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Snapshot information"),
        examples = {"ES_GET_SNAPSHOTS('my-repo', '_all')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetSnapshots(ExecutionContext context) {
        context.declareFunction("ES_GET_SNAPSHOTS",
            List.of(
                new Parameter("repository", "STRING", ParameterMode.IN),
                new Parameter("snapshot", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_GET_SNAPSHOTS", (args, listener) -> {
                try {
                    String repo = toString(args.get(0));
                    String snapshot = args.size() > 1 && args.get(1) != null ? toString(args.get(1)) : "_all";
                    Map<String, Object> result = esRequest("GET", "/_snapshot/" + repo + "/" + snapshot, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_GET_SNAPSHOTS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_CREATE_SNAPSHOT",
        description = "Create a snapshot",
        parameters = {
            @FunctionParam(name = "repository", type = "STRING", description = "Repository name"),
            @FunctionParam(name = "snapshot", type = "STRING", description = "Snapshot name"),
            @FunctionParam(name = "settings", type = "DOCUMENT", description = "Snapshot settings")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Snapshot creation result"),
        examples = {"ES_CREATE_SNAPSHOT('my-repo', 'snapshot-1', {'indices': 'my-index-*'})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerCreateSnapshot(ExecutionContext context) {
        context.declareFunction("ES_CREATE_SNAPSHOT",
            List.of(
                new Parameter("repository", "STRING", ParameterMode.IN),
                new Parameter("snapshot", "STRING", ParameterMode.IN),
                new Parameter("settings", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_CREATE_SNAPSHOT", (args, listener) -> {
                try {
                    String repo = toString(args.get(0));
                    String snapshot = toString(args.get(1));
                    Map<String, Object> settings = args.size() > 2 && args.get(2) != null ? toMap(args.get(2)) : null;
                    Map<String, Object> result = esRequest("PUT", "/_snapshot/" + repo + "/" + snapshot + "?wait_for_completion=true", settings);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_CREATE_SNAPSHOT failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_DELETE_SNAPSHOT",
        description = "Delete a snapshot",
        parameters = {
            @FunctionParam(name = "repository", type = "STRING", description = "Repository name"),
            @FunctionParam(name = "snapshot", type = "STRING", description = "Snapshot name")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Acknowledgment"),
        examples = {"ES_DELETE_SNAPSHOT('my-repo', 'snapshot-1')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDeleteSnapshot(ExecutionContext context) {
        context.declareFunction("ES_DELETE_SNAPSHOT",
            List.of(
                new Parameter("repository", "STRING", ParameterMode.IN),
                new Parameter("snapshot", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_DELETE_SNAPSHOT", (args, listener) -> {
                try {
                    String repo = toString(args.get(0));
                    String snapshot = toString(args.get(1));
                    Map<String, Object> result = esRequest("DELETE", "/_snapshot/" + repo + "/" + snapshot, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_DELETE_SNAPSHOT failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_RESTORE_SNAPSHOT",
        description = "Restore a snapshot",
        parameters = {
            @FunctionParam(name = "repository", type = "STRING", description = "Repository name"),
            @FunctionParam(name = "snapshot", type = "STRING", description = "Snapshot name"),
            @FunctionParam(name = "settings", type = "DOCUMENT", description = "Restore settings")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Restore result"),
        examples = {"ES_RESTORE_SNAPSHOT('my-repo', 'snapshot-1', {'indices': 'my-index-*'})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerRestoreSnapshot(ExecutionContext context) {
        context.declareFunction("ES_RESTORE_SNAPSHOT",
            List.of(
                new Parameter("repository", "STRING", ParameterMode.IN),
                new Parameter("snapshot", "STRING", ParameterMode.IN),
                new Parameter("settings", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_RESTORE_SNAPSHOT", (args, listener) -> {
                try {
                    String repo = toString(args.get(0));
                    String snapshot = toString(args.get(1));
                    Map<String, Object> settings = args.size() > 2 && args.get(2) != null ? toMap(args.get(2)) : null;
                    Map<String, Object> result = esRequest("POST", "/_snapshot/" + repo + "/" + snapshot + "/_restore", settings);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_RESTORE_SNAPSHOT failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_SNAPSHOT_STATUS",
        description = "Get snapshot status",
        parameters = {
            @FunctionParam(name = "repository", type = "STRING", description = "Repository name"),
            @FunctionParam(name = "snapshot", type = "STRING", description = "Snapshot name")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Snapshot status"),
        examples = {"ES_SNAPSHOT_STATUS('my-repo', 'snapshot-1')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerSnapshotStatus(ExecutionContext context) {
        context.declareFunction("ES_SNAPSHOT_STATUS",
            List.of(
                new Parameter("repository", "STRING", ParameterMode.IN),
                new Parameter("snapshot", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_SNAPSHOT_STATUS", (args, listener) -> {
                try {
                    String repo = args.size() > 0 && args.get(0) != null ? toString(args.get(0)) : "";
                    String snapshot = args.size() > 1 && args.get(1) != null ? toString(args.get(1)) : "";
                    String path = "/_snapshot";
                    if (!repo.isEmpty()) {
                        path += "/" + repo;
                        if (!snapshot.isEmpty()) {
                            path += "/" + snapshot;
                        }
                    }
                    path += "/_status";
                    Map<String, Object> result = esRequest("GET", path, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_SNAPSHOT_STATUS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_CLONE_SNAPSHOT",
        description = "Clone a snapshot",
        parameters = {
            @FunctionParam(name = "repository", type = "STRING", description = "Repository name"),
            @FunctionParam(name = "source_snapshot", type = "STRING", description = "Source snapshot name"),
            @FunctionParam(name = "target_snapshot", type = "STRING", description = "Target snapshot name"),
            @FunctionParam(name = "indices", type = "STRING", description = "Indices to clone")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Clone result"),
        examples = {"ES_CLONE_SNAPSHOT('my-repo', 'snapshot-1', 'snapshot-1-clone', 'my-index-*')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerCloneSnapshot(ExecutionContext context) {
        context.declareFunction("ES_CLONE_SNAPSHOT",
            List.of(
                new Parameter("repository", "STRING", ParameterMode.IN),
                new Parameter("source_snapshot", "STRING", ParameterMode.IN),
                new Parameter("target_snapshot", "STRING", ParameterMode.IN),
                new Parameter("indices", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_CLONE_SNAPSHOT", (args, listener) -> {
                try {
                    String repo = toString(args.get(0));
                    String source = toString(args.get(1));
                    String target = toString(args.get(2));
                    String indices = toString(args.get(3));
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("indices", indices);
                    
                    Map<String, Object> result = esRequest("PUT", "/_snapshot/" + repo + "/" + source + "/_clone/" + target, body);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_CLONE_SNAPSHOT failed: " + e.getMessage(), e));
                }
            })
        );
    }
}
