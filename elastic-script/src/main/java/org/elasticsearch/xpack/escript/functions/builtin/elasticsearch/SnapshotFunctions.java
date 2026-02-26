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
import org.elasticsearch.action.admin.cluster.repositories.get.GetRepositoriesRequest;
import org.elasticsearch.action.admin.cluster.repositories.get.GetRepositoriesResponse;
import org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryRequest;
import org.elasticsearch.action.admin.cluster.repositories.delete.DeleteRepositoryRequest;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotRequest;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotResponse;
import org.elasticsearch.action.admin.cluster.snapshots.delete.DeleteSnapshotRequest;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsRequest;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsResponse;
import org.elasticsearch.action.admin.cluster.snapshots.restore.RestoreSnapshotRequest;
import org.elasticsearch.action.admin.cluster.snapshots.restore.RestoreSnapshotResponse;
import org.elasticsearch.action.admin.cluster.snapshots.status.SnapshotsStatusRequest;
import org.elasticsearch.action.admin.cluster.snapshots.status.SnapshotsStatusResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch Snapshot and Restore API functions.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.DATASOURCE,
    description = "Elasticsearch Snapshot and Restore API functions"
)
public class SnapshotFunctions {

    public static void registerAll(ExecutionContext context, Client client) {
        registerGetRepositories(context, client);
        registerCreateRepository(context, client);
        registerDeleteRepository(context, client);
        registerGetSnapshots(context, client);
        registerCreateSnapshot(context, client);
        registerDeleteSnapshot(context, client);
        registerRestoreSnapshot(context, client);
        registerSnapshotStatus(context, client);
    }

    @FunctionSpec(
        name = "ES_GET_REPOSITORIES",
        description = "Get snapshot repositories",
        parameters = {
            @FunctionParam(name = "name", type = "STRING", description = "Repository name or pattern")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Repository configurations"),
        examples = {"ES_GET_REPOSITORIES('*')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetRepositories(ExecutionContext context, Client client) {
        context.declareFunction("ES_GET_REPOSITORIES",
            List.of(new Parameter("name", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_GET_REPOSITORIES", (args, listener) -> {
                String[] names = args.size() > 0 && args.get(0) != null ? 
                    new String[]{args.get(0).toString()} : new String[0];
                
                GetRepositoriesRequest request = new GetRepositoriesRequest(names);
                client.admin().cluster().getRepositories(request, new ActionListener<GetRepositoriesResponse>() {
                    @Override
                    public void onResponse(GetRepositoriesResponse response) {
                        List<Map<String, Object>> repos = new java.util.ArrayList<>();
                        for (var repo : response.repositories()) {
                            Map<String, Object> r = new HashMap<>();
                            r.put("name", repo.name());
                            r.put("type", repo.type());
                            r.put("settings", repo.settings().toString());
                            repos.add(r);
                        }
                        listener.onResponse(repos);
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
        name = "ES_CREATE_REPOSITORY",
        description = "Create a snapshot repository",
        parameters = {
            @FunctionParam(name = "name", type = "STRING", description = "Repository name"),
            @FunctionParam(name = "type", type = "STRING", description = "Repository type (fs, s3, gcs, azure)"),
            @FunctionParam(name = "settings", type = "DOCUMENT", description = "Repository settings")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if acknowledged"),
        examples = {"ES_CREATE_REPOSITORY('my-backup', 'fs', {'location': '/backup'})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerCreateRepository(ExecutionContext context, Client client) {
        context.declareFunction("ES_CREATE_REPOSITORY",
            Arrays.asList(
                new Parameter("name", "STRING", ParameterMode.IN),
                new Parameter("type", "STRING", ParameterMode.IN),
                new Parameter("settings", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_CREATE_REPOSITORY", (args, listener) -> {
                String name = args.get(0).toString();
                String type = args.get(1).toString();
                @SuppressWarnings("unchecked")
                Map<String, Object> settings = (Map<String, Object>) args.get(2);
                
                PutRepositoryRequest request = new PutRepositoryRequest(name);
                request.type(type);
                request.settings(settings);
                
                client.admin().cluster().putRepository(request, new ActionListener<AcknowledgedResponse>() {
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
        name = "ES_DELETE_REPOSITORY",
        description = "Delete a snapshot repository",
        parameters = {
            @FunctionParam(name = "name", type = "STRING", description = "Repository name")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if acknowledged"),
        examples = {"ES_DELETE_REPOSITORY('old-backup')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDeleteRepository(ExecutionContext context, Client client) {
        context.declareFunction("ES_DELETE_REPOSITORY",
            List.of(new Parameter("name", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_DELETE_REPOSITORY", (args, listener) -> {
                String name = args.get(0).toString();
                
                DeleteRepositoryRequest request = new DeleteRepositoryRequest(name);
                client.admin().cluster().deleteRepository(request, new ActionListener<AcknowledgedResponse>() {
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
        name = "ES_GET_SNAPSHOTS",
        description = "Get snapshots from a repository",
        parameters = {
            @FunctionParam(name = "repository", type = "STRING", description = "Repository name"),
            @FunctionParam(name = "snapshot", type = "STRING", description = "Snapshot name or pattern")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Snapshot information"),
        examples = {"ES_GET_SNAPSHOTS('my-backup', '*')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetSnapshots(ExecutionContext context, Client client) {
        context.declareFunction("ES_GET_SNAPSHOTS",
            Arrays.asList(
                new Parameter("repository", "STRING", ParameterMode.IN),
                new Parameter("snapshot", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_GET_SNAPSHOTS", (args, listener) -> {
                String repository = args.get(0).toString();
                String[] snapshots = args.size() > 1 && args.get(1) != null ? 
                    new String[]{args.get(1).toString()} : new String[]{"*"};
                
                GetSnapshotsRequest request = new GetSnapshotsRequest(repository, snapshots);
                client.admin().cluster().getSnapshots(request, new ActionListener<GetSnapshotsResponse>() {
                    @Override
                    public void onResponse(GetSnapshotsResponse response) {
                        List<Map<String, Object>> snapshotList = new java.util.ArrayList<>();
                        for (var snap : response.getSnapshots()) {
                            Map<String, Object> s = new HashMap<>();
                            s.put("snapshot", snap.snapshotId().getName());
                            s.put("uuid", snap.snapshotId().getUUID());
                            s.put("state", snap.state().name());
                            s.put("start_time", snap.startTime());
                            s.put("end_time", snap.endTime());
                            s.put("indices", snap.indices());
                            snapshotList.add(s);
                        }
                        listener.onResponse(snapshotList);
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
        name = "ES_CREATE_SNAPSHOT",
        description = "Create a snapshot",
        parameters = {
            @FunctionParam(name = "repository", type = "STRING", description = "Repository name"),
            @FunctionParam(name = "snapshot", type = "STRING", description = "Snapshot name"),
            @FunctionParam(name = "indices", type = "STRING", description = "Indices to snapshot"),
            @FunctionParam(name = "wait_for_completion", type = "BOOLEAN", description = "Wait for completion")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Snapshot info"),
        examples = {"ES_CREATE_SNAPSHOT('my-backup', 'snap-1', 'logs-*', true)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerCreateSnapshot(ExecutionContext context, Client client) {
        context.declareFunction("ES_CREATE_SNAPSHOT",
            Arrays.asList(
                new Parameter("repository", "STRING", ParameterMode.IN),
                new Parameter("snapshot", "STRING", ParameterMode.IN),
                new Parameter("indices", "STRING", ParameterMode.IN),
                new Parameter("wait_for_completion", "BOOLEAN", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_CREATE_SNAPSHOT", (args, listener) -> {
                String repository = args.get(0).toString();
                String snapshot = args.get(1).toString();
                String indices = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : "*";
                boolean waitForCompletion = args.size() > 3 && Boolean.TRUE.equals(args.get(3));
                
                CreateSnapshotRequest request = new CreateSnapshotRequest(repository, snapshot);
                request.indices(indices);
                request.waitForCompletion(waitForCompletion);
                
                client.admin().cluster().createSnapshot(request, new ActionListener<CreateSnapshotResponse>() {
                    @Override
                    public void onResponse(CreateSnapshotResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        var snap = response.getSnapshotInfo();
                        if (snap != null) {
                            result.put("snapshot", snap.snapshotId().getName());
                            result.put("state", snap.state().name());
                            result.put("indices", snap.indices());
                        } else {
                            result.put("accepted", true);
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
        name = "ES_DELETE_SNAPSHOT",
        description = "Delete a snapshot",
        parameters = {
            @FunctionParam(name = "repository", type = "STRING", description = "Repository name"),
            @FunctionParam(name = "snapshot", type = "STRING", description = "Snapshot name")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if acknowledged"),
        examples = {"ES_DELETE_SNAPSHOT('my-backup', 'old-snap')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDeleteSnapshot(ExecutionContext context, Client client) {
        context.declareFunction("ES_DELETE_SNAPSHOT",
            Arrays.asList(
                new Parameter("repository", "STRING", ParameterMode.IN),
                new Parameter("snapshot", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_DELETE_SNAPSHOT", (args, listener) -> {
                String repository = args.get(0).toString();
                String snapshot = args.get(1).toString();
                
                DeleteSnapshotRequest request = new DeleteSnapshotRequest(repository, snapshot);
                client.admin().cluster().deleteSnapshot(request, new ActionListener<AcknowledgedResponse>() {
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
        name = "ES_RESTORE_SNAPSHOT",
        description = "Restore a snapshot",
        parameters = {
            @FunctionParam(name = "repository", type = "STRING", description = "Repository name"),
            @FunctionParam(name = "snapshot", type = "STRING", description = "Snapshot name"),
            @FunctionParam(name = "indices", type = "STRING", description = "Indices to restore"),
            @FunctionParam(name = "rename_pattern", type = "STRING", description = "Rename pattern"),
            @FunctionParam(name = "rename_replacement", type = "STRING", description = "Rename replacement")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Restore info"),
        examples = {"ES_RESTORE_SNAPSHOT('my-backup', 'snap-1', 'logs-*', null, null)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerRestoreSnapshot(ExecutionContext context, Client client) {
        context.declareFunction("ES_RESTORE_SNAPSHOT",
            Arrays.asList(
                new Parameter("repository", "STRING", ParameterMode.IN),
                new Parameter("snapshot", "STRING", ParameterMode.IN),
                new Parameter("indices", "STRING", ParameterMode.IN),
                new Parameter("rename_pattern", "STRING", ParameterMode.IN),
                new Parameter("rename_replacement", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_RESTORE_SNAPSHOT", (args, listener) -> {
                String repository = args.get(0).toString();
                String snapshot = args.get(1).toString();
                
                RestoreSnapshotRequest request = new RestoreSnapshotRequest(repository, snapshot);
                if (args.size() > 2 && args.get(2) != null) {
                    request.indices(args.get(2).toString());
                }
                if (args.size() > 3 && args.get(3) != null) {
                    request.renamePattern(args.get(3).toString());
                }
                if (args.size() > 4 && args.get(4) != null) {
                    request.renameReplacement(args.get(4).toString());
                }
                
                client.admin().cluster().restoreSnapshot(request, new ActionListener<RestoreSnapshotResponse>() {
                    @Override
                    public void onResponse(RestoreSnapshotResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        var info = response.getRestoreInfo();
                        if (info != null) {
                            result.put("snapshot", info.name());
                            result.put("indices", info.indices());
                            result.put("total_shards", info.totalShards());
                            result.put("successful_shards", info.successfulShards());
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
        name = "ES_SNAPSHOT_STATUS",
        description = "Get snapshot status",
        parameters = {
            @FunctionParam(name = "repository", type = "STRING", description = "Repository name"),
            @FunctionParam(name = "snapshot", type = "STRING", description = "Snapshot name")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Snapshot statuses"),
        examples = {"ES_SNAPSHOT_STATUS('my-backup', 'snap-1')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerSnapshotStatus(ExecutionContext context, Client client) {
        context.declareFunction("ES_SNAPSHOT_STATUS",
            Arrays.asList(
                new Parameter("repository", "STRING", ParameterMode.IN),
                new Parameter("snapshot", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_SNAPSHOT_STATUS", (args, listener) -> {
                String repository = args.get(0).toString();
                String[] snapshots = args.size() > 1 && args.get(1) != null ? 
                    new String[]{args.get(1).toString()} : new String[0];
                
                SnapshotsStatusRequest request = new SnapshotsStatusRequest(repository, snapshots);
                client.admin().cluster().snapshotsStatus(request, new ActionListener<SnapshotsStatusResponse>() {
                    @Override
                    public void onResponse(SnapshotsStatusResponse response) {
                        List<Map<String, Object>> statuses = new java.util.ArrayList<>();
                        for (var status : response.getSnapshots()) {
                            Map<String, Object> s = new HashMap<>();
                            s.put("snapshot", status.getSnapshot().getSnapshotId().getName());
                            s.put("state", status.getState().name());
                            s.put("shards_done", status.getShardsStats().getDone());
                            s.put("shards_total", status.getShardsStats().getTotalShardCount());
                            statuses.add(s);
                        }
                        listener.onResponse(statuses);
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
