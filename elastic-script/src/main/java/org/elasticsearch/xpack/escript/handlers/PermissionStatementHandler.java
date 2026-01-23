/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;
import org.elasticsearch.xpack.escript.security.PermissionDefinition;
import org.elasticsearch.xpack.escript.security.RoleDefinition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handler for permission statements: GRANT, REVOKE, CREATE ROLE, DROP ROLE, SHOW PERMISSIONS.
 */
public class PermissionStatementHandler {

    private static final Logger LOGGER = LogManager.getLogger(PermissionStatementHandler.class);

    private final Client client;

    public PermissionStatementHandler(Client client) {
        this.client = client;
    }

    /**
     * Handle GRANT statement.
     */
    public void handleGrant(ElasticScriptParser.Grant_statementContext ctx,
                           ActionListener<Object> listener) {
        // Parse privileges
        Set<String> privileges = parsePrivileges(ctx.privilege_list());

        // Parse object type and name
        String objectType = ctx.object_type().getText().toUpperCase();
        String objectName = ctx.ID().getText();

        // Parse principal
        String[] principal = parsePrincipal(ctx.principal());
        String principalType = principal[0];
        String principalName = principal[1];

        // Build permission
        PermissionDefinition permission = PermissionDefinition.builder()
            .principalType(principalType)
            .principalName(principalName)
            .objectType(objectType)
            .objectName(objectName)
            .privileges(privileges)
            .build();

        LOGGER.debug("Granting {} on {} {} to {} {}", privileges, objectType, objectName,
                    principalType, principalName);

        // Store the permission
        ensureIndexExists(PermissionDefinition.INDEX_NAME, ActionListener.wrap(
            indexExists -> storePermission(permission, listener),
            listener::onFailure
        ));
    }

    /**
     * Handle REVOKE statement.
     */
    public void handleRevoke(ElasticScriptParser.Revoke_statementContext ctx,
                            ActionListener<Object> listener) {
        // Parse object type and name
        String objectType = ctx.object_type().getText().toUpperCase();
        String objectName = ctx.ID().getText();

        // Parse principal
        String[] principal = parsePrincipal(ctx.principal());
        String principalType = principal[0];
        String principalName = principal[1];

        String id = PermissionDefinition.generateId(principalType, principalName, objectType, objectName);

        DeleteRequest request = new DeleteRequest(PermissionDefinition.INDEX_NAME, id);
        client.delete(request, ActionListener.wrap(
            response -> {
                Map<String, Object> result = new HashMap<>();
                result.put("acknowledged", true);
                result.put("action", "revoked");
                result.put("principal_type", principalType);
                result.put("principal_name", principalName);
                result.put("object_type", objectType);
                result.put("object_name", objectName);
                listener.onResponse(result);
            },
            listener::onFailure
        ));
    }

    /**
     * Handle CREATE ROLE statement.
     */
    public void handleCreateRole(ElasticScriptParser.Create_role_statementContext ctx,
                                ActionListener<Object> listener) {
        String roleName = ctx.ID().getText();
        String description = null;
        if (ctx.STRING() != null) {
            description = unquote(ctx.STRING().getText());
        }

        RoleDefinition role = RoleDefinition.builder()
            .name(roleName)
            .description(description)
            .build();

        ensureIndexExists(RoleDefinition.INDEX_NAME, ActionListener.wrap(
            indexExists -> storeRole(role, listener),
            listener::onFailure
        ));
    }

    /**
     * Handle DROP ROLE statement.
     */
    public void handleDropRole(ElasticScriptParser.Drop_role_statementContext ctx,
                              ActionListener<Object> listener) {
        String roleName = ctx.ID().getText();

        DeleteRequest request = new DeleteRequest(RoleDefinition.INDEX_NAME, roleName);
        client.delete(request, ActionListener.wrap(
            response -> {
                Map<String, Object> result = new HashMap<>();
                result.put("acknowledged", true);
                result.put("role", roleName);
                result.put("action", "dropped");
                listener.onResponse(result);
            },
            listener::onFailure
        ));
    }

    /**
     * Handle SHOW PERMISSIONS (all).
     */
    public void handleShowAllPermissions(ActionListener<Object> listener) {
        SearchRequest request = new SearchRequest(PermissionDefinition.INDEX_NAME);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchAllQuery());
        sourceBuilder.size(1000);
        request.source(sourceBuilder);

        client.search(request, ActionListener.wrap(
            response -> {
                List<Map<String, Object>> permissions = new ArrayList<>();
                for (SearchHit hit : response.getHits().getHits()) {
                    permissions.add(hit.getSourceAsMap());
                }
                listener.onResponse(permissions);
            },
            e -> {
                if (e.getCause() instanceof IndexNotFoundException) {
                    listener.onResponse(new ArrayList<>());
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }

    /**
     * Handle SHOW PERMISSIONS FOR principal.
     */
    public void handleShowPrincipalPermissions(ElasticScriptParser.ShowPrincipalPermissionsContext ctx,
                                              ActionListener<Object> listener) {
        String[] principal = parsePrincipal(ctx.principal());
        String principalType = principal[0];
        String principalName = principal[1];

        SearchRequest request = new SearchRequest(PermissionDefinition.INDEX_NAME);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.boolQuery()
            .must(QueryBuilders.termQuery("principal_type", principalType))
            .must(QueryBuilders.termQuery("principal_name", principalName)));
        sourceBuilder.size(1000);
        request.source(sourceBuilder);

        client.search(request, ActionListener.wrap(
            response -> {
                List<Map<String, Object>> permissions = new ArrayList<>();
                for (SearchHit hit : response.getHits().getHits()) {
                    permissions.add(hit.getSourceAsMap());
                }
                listener.onResponse(permissions);
            },
            e -> {
                if (e.getCause() instanceof IndexNotFoundException) {
                    listener.onResponse(new ArrayList<>());
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }

    /**
     * Handle SHOW ROLE name.
     */
    public void handleShowRole(ElasticScriptParser.ShowRoleDetailContext ctx,
                              ActionListener<Object> listener) {
        String roleName = ctx.ID().getText();

        GetRequest request = new GetRequest(RoleDefinition.INDEX_NAME, roleName);
        client.get(request, ActionListener.wrap(
            response -> {
                if (!response.isExists()) {
                    listener.onFailure(new IllegalArgumentException("Role not found: " + roleName));
                    return;
                }
                listener.onResponse(response.getSourceAsMap());
            },
            e -> {
                if (e instanceof IndexNotFoundException) {
                    listener.onFailure(new IllegalArgumentException("Role not found: " + roleName));
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }

    // ==================== Helper Methods ====================

    private Set<String> parsePrivileges(ElasticScriptParser.Privilege_listContext ctx) {
        Set<String> privileges = new HashSet<>();
        if (ctx.ALL_PRIVILEGES() != null) {
            privileges.add("ALL");
        } else {
            for (ElasticScriptParser.PrivilegeContext privCtx : ctx.privilege()) {
                if (privCtx instanceof ElasticScriptParser.ExecutePrivilegeContext) {
                    privileges.add("EXECUTE");
                }
            }
        }
        return privileges;
    }

    private String[] parsePrincipal(ElasticScriptParser.PrincipalContext ctx) {
        if (ctx instanceof ElasticScriptParser.RolePrincipalContext roleCtx) {
            return new String[]{"ROLE", roleCtx.ID().getText()};
        } else if (ctx instanceof ElasticScriptParser.UserPrincipalContext userCtx) {
            return new String[]{"USER", unquote(userCtx.STRING().getText())};
        }
        throw new IllegalArgumentException("Unknown principal type");
    }

    private String unquote(String s) {
        if (s.startsWith("'") && s.endsWith("'")) {
            return s.substring(1, s.length() - 1);
        }
        if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private void ensureIndexExists(String indexName, ActionListener<Boolean> listener) {
        client.admin().indices().prepareGetIndex(TimeValue.timeValueSeconds(30)).setIndices(indexName).execute(ActionListener.wrap(
            response -> listener.onResponse(true),
            e -> {
                if (e instanceof IndexNotFoundException) {
                    CreateIndexRequest createRequest = new CreateIndexRequest(indexName);
                    client.admin().indices().create(createRequest, ActionListener.wrap(
                        response -> listener.onResponse(true),
                        listener::onFailure
                    ));
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }

    private void storePermission(PermissionDefinition permission, ActionListener<Object> listener) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            permission.toXContent(builder, null);

            IndexRequest request = new IndexRequest(PermissionDefinition.INDEX_NAME)
                .id(permission.getId())
                .source(builder);

            client.index(request, ActionListener.wrap(
                response -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("acknowledged", true);
                    result.put("action", "granted");
                    result.put("permission_id", permission.getId());
                    result.put("privileges", permission.getPrivileges());
                    listener.onResponse(result);
                },
                listener::onFailure
            ));
        } catch (IOException e) {
            listener.onFailure(e);
        }
    }

    private void storeRole(RoleDefinition role, ActionListener<Object> listener) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            role.toXContent(builder, null);

            IndexRequest request = new IndexRequest(RoleDefinition.INDEX_NAME)
                .id(role.getName())
                .source(builder);

            client.index(request, ActionListener.wrap(
                response -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("acknowledged", true);
                    result.put("role", role.getName());
                    result.put("action", "created");
                    listener.onResponse(result);
                },
                listener::onFailure
            ));
        } catch (IOException e) {
            listener.onFailure(e);
        }
    }
}
