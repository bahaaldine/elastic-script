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
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;
import org.elasticsearch.xpack.core.security.action.user.GetUsersAction;
import org.elasticsearch.xpack.core.security.action.user.GetUsersRequest;
import org.elasticsearch.xpack.core.security.action.user.GetUsersResponse;
import org.elasticsearch.xpack.core.security.action.user.PutUserAction;
import org.elasticsearch.xpack.core.security.action.user.PutUserRequest;
import org.elasticsearch.xpack.core.security.action.user.PutUserResponse;
import org.elasticsearch.xpack.core.security.action.user.DeleteUserAction;
import org.elasticsearch.xpack.core.security.action.user.DeleteUserRequest;
import org.elasticsearch.xpack.core.security.action.user.DeleteUserResponse;
import org.elasticsearch.xpack.core.security.action.role.GetRolesAction;
import org.elasticsearch.xpack.core.security.action.role.GetRolesRequest;
import org.elasticsearch.xpack.core.security.action.role.GetRolesResponse;
import org.elasticsearch.xpack.core.security.action.role.DeleteRoleAction;
import org.elasticsearch.xpack.core.security.action.role.DeleteRoleRequest;
import org.elasticsearch.xpack.core.security.action.role.DeleteRoleResponse;
import org.elasticsearch.xpack.core.security.action.apikey.CreateApiKeyAction;
import org.elasticsearch.xpack.core.security.action.apikey.CreateApiKeyRequest;
import org.elasticsearch.xpack.core.security.action.apikey.CreateApiKeyResponse;
import org.elasticsearch.xpack.core.security.action.apikey.GetApiKeyAction;
import org.elasticsearch.xpack.core.security.action.apikey.GetApiKeyRequest;
import org.elasticsearch.xpack.core.security.action.apikey.GetApiKeyResponse;
import org.elasticsearch.xpack.core.security.action.apikey.InvalidateApiKeyAction;
import org.elasticsearch.xpack.core.security.action.apikey.InvalidateApiKeyRequest;
import org.elasticsearch.xpack.core.security.action.apikey.InvalidateApiKeyResponse;
import org.elasticsearch.xpack.core.security.action.privilege.GetPrivilegesAction;
import org.elasticsearch.xpack.core.security.action.privilege.GetPrivilegesRequest;
import org.elasticsearch.xpack.core.security.action.privilege.GetPrivilegesResponse;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch Security API functions.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.SECURITY,
    description = "Elasticsearch Security API functions for users, roles, and API keys"
)
public class SecurityFunctions {

    public static void registerAll(ExecutionContext context, Client client) {
        registerGetUsers(context, client);
        registerCreateUser(context, client);
        registerDeleteUser(context, client);
        registerGetRoles(context, client);
        registerDeleteRole(context, client);
        registerCreateApiKey(context, client);
        registerGetApiKey(context, client);
        registerInvalidateApiKey(context, client);
        registerGetPrivileges(context, client);
        registerAuthenticate(context, client);
        registerHasPrivileges(context, client);
        registerClearCache(context, client);
    }

    @FunctionSpec(
        name = "ES_GET_USERS",
        description = "Get users",
        parameters = {
            @FunctionParam(name = "username", type = "STRING", description = "Optional username filter")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Array of user objects"),
        examples = {"ES_GET_USERS()", "ES_GET_USERS('admin')"},
        category = FunctionCategory.SECURITY
    )
    public static void registerGetUsers(ExecutionContext context, Client client) {
        context.declareFunction("ES_GET_USERS",
            List.of(new Parameter("username", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_GET_USERS", (args, listener) -> {
                String[] usernames = args.size() > 0 && args.get(0) != null ? 
                    new String[]{args.get(0).toString()} : new String[0];
                
                GetUsersRequest request = new GetUsersRequest(usernames);
                client.execute(GetUsersAction.INSTANCE, request, new ActionListener<GetUsersResponse>() {
                    @Override
                    public void onResponse(GetUsersResponse response) {
                        List<Map<String, Object>> users = new java.util.ArrayList<>();
                        for (var user : response.users()) {
                            Map<String, Object> u = new HashMap<>();
                            u.put("username", user.principal());
                            u.put("roles", Arrays.asList(user.roles()));
                            u.put("full_name", user.fullName());
                            u.put("email", user.email());
                            u.put("enabled", user.enabled());
                            users.add(u);
                        }
                        listener.onResponse(users);
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
        name = "ES_CREATE_USER",
        description = "Create or update a user",
        parameters = {
            @FunctionParam(name = "username", type = "STRING", description = "Username"),
            @FunctionParam(name = "password", type = "STRING", description = "Password"),
            @FunctionParam(name = "roles", type = "ARRAY", description = "Array of role names"),
            @FunctionParam(name = "full_name", type = "STRING", description = "Full name"),
            @FunctionParam(name = "email", type = "STRING", description = "Email")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Creation result"),
        examples = {"ES_CREATE_USER('john', 'secret', ['viewer'], 'John Doe', 'john@example.com')"},
        category = FunctionCategory.SECURITY
    )
    public static void registerCreateUser(ExecutionContext context, Client client) {
        context.declareFunction("ES_CREATE_USER",
            Arrays.asList(
                new Parameter("username", "STRING", ParameterMode.IN),
                new Parameter("password", "STRING", ParameterMode.IN),
                new Parameter("roles", "ARRAY", ParameterMode.IN),
                new Parameter("full_name", "STRING", ParameterMode.IN),
                new Parameter("email", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_CREATE_USER", (args, listener) -> {
                String username = args.get(0).toString();
                String password = args.get(1).toString();
                @SuppressWarnings("unchecked")
                List<String> rolesList = (List<String>) args.get(2);
                String[] roles = rolesList.toArray(new String[0]);
                String fullName = args.size() > 3 && args.get(3) != null ? args.get(3).toString() : null;
                String email = args.size() > 4 && args.get(4) != null ? args.get(4).toString() : null;
                
                PutUserRequest request = new PutUserRequest();
                request.username(username);
                request.passwordHash(password.toCharArray());
                request.roles(roles);
                request.fullName(fullName);
                request.email(email);
                request.enabled(true);
                
                client.execute(PutUserAction.INSTANCE, request, new ActionListener<PutUserResponse>() {
                    @Override
                    public void onResponse(PutUserResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("created", response.created());
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
        name = "ES_DELETE_USER",
        description = "Delete a user",
        parameters = {
            @FunctionParam(name = "username", type = "STRING", description = "Username to delete")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if deleted"),
        examples = {"ES_DELETE_USER('john')"},
        category = FunctionCategory.SECURITY
    )
    public static void registerDeleteUser(ExecutionContext context, Client client) {
        context.declareFunction("ES_DELETE_USER",
            List.of(new Parameter("username", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_DELETE_USER", (args, listener) -> {
                String username = args.get(0).toString();
                
                DeleteUserRequest request = new DeleteUserRequest(username);
                client.execute(DeleteUserAction.INSTANCE, request, new ActionListener<DeleteUserResponse>() {
                    @Override
                    public void onResponse(DeleteUserResponse response) {
                        listener.onResponse(response.found());
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
        name = "ES_GET_ROLES",
        description = "Get roles",
        parameters = {
            @FunctionParam(name = "role_name", type = "STRING", description = "Optional role name filter")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Array of role objects"),
        examples = {"ES_GET_ROLES()", "ES_GET_ROLES('admin')"},
        category = FunctionCategory.SECURITY
    )
    public static void registerGetRoles(ExecutionContext context, Client client) {
        context.declareFunction("ES_GET_ROLES",
            List.of(new Parameter("role_name", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_GET_ROLES", (args, listener) -> {
                String[] roleNames = args.size() > 0 && args.get(0) != null ? 
                    new String[]{args.get(0).toString()} : new String[0];
                
                GetRolesRequest request = new GetRolesRequest(roleNames);
                client.execute(GetRolesAction.INSTANCE, request, new ActionListener<GetRolesResponse>() {
                    @Override
                    public void onResponse(GetRolesResponse response) {
                        List<Map<String, Object>> roles = new java.util.ArrayList<>();
                        for (var role : response.roles()) {
                            Map<String, Object> r = new HashMap<>();
                            r.put("name", role.getName());
                            r.put("cluster", Arrays.asList(role.getClusterPrivileges()));
                            roles.add(r);
                        }
                        listener.onResponse(roles);
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
        name = "ES_DELETE_ROLE",
        description = "Delete a role",
        parameters = {
            @FunctionParam(name = "role_name", type = "STRING", description = "Role name to delete")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if deleted"),
        examples = {"ES_DELETE_ROLE('custom-role')"},
        category = FunctionCategory.SECURITY
    )
    public static void registerDeleteRole(ExecutionContext context, Client client) {
        context.declareFunction("ES_DELETE_ROLE",
            List.of(new Parameter("role_name", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_DELETE_ROLE", (args, listener) -> {
                String roleName = args.get(0).toString();
                
                DeleteRoleRequest request = new DeleteRoleRequest(roleName);
                client.execute(DeleteRoleAction.INSTANCE, request, new ActionListener<DeleteRoleResponse>() {
                    @Override
                    public void onResponse(DeleteRoleResponse response) {
                        listener.onResponse(response.found());
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
        name = "ES_CREATE_API_KEY",
        description = "Create an API key",
        parameters = {
            @FunctionParam(name = "name", type = "STRING", description = "API key name"),
            @FunctionParam(name = "expiration", type = "STRING", description = "Expiration time (e.g., '7d')"),
            @FunctionParam(name = "role_descriptors", type = "DOCUMENT", description = "Optional role descriptors")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "API key with id and encoded key"),
        examples = {"ES_CREATE_API_KEY('my-key', '30d', {})"},
        category = FunctionCategory.SECURITY
    )
    public static void registerCreateApiKey(ExecutionContext context, Client client) {
        context.declareFunction("ES_CREATE_API_KEY",
            Arrays.asList(
                new Parameter("name", "STRING", ParameterMode.IN),
                new Parameter("expiration", "STRING", ParameterMode.IN),
                new Parameter("role_descriptors", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_CREATE_API_KEY", (args, listener) -> {
                String name = args.get(0).toString();
                String expiration = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : null;
                
                CreateApiKeyRequest request = new CreateApiKeyRequest(name, List.of(), 
                    expiration != null ? org.elasticsearch.core.TimeValue.parseTimeValue(expiration, "expiration") : null);
                
                client.execute(CreateApiKeyAction.INSTANCE, request, new ActionListener<CreateApiKeyResponse>() {
                    @Override
                    public void onResponse(CreateApiKeyResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("id", response.getId());
                        result.put("name", response.getName());
                        result.put("api_key", new String(response.getKey().getChars()));
                        result.put("encoded", response.getEncoded());
                        if (response.getExpiration() != null) {
                            result.put("expiration", response.getExpiration().toEpochMilli());
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
        name = "ES_GET_API_KEY",
        description = "Get API key information",
        parameters = {
            @FunctionParam(name = "id", type = "STRING", description = "API key ID"),
            @FunctionParam(name = "name", type = "STRING", description = "API key name"),
            @FunctionParam(name = "username", type = "STRING", description = "Owner username")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Matching API keys"),
        examples = {"ES_GET_API_KEY('key-id', null, null)"},
        category = FunctionCategory.SECURITY
    )
    public static void registerGetApiKey(ExecutionContext context, Client client) {
        context.declareFunction("ES_GET_API_KEY",
            Arrays.asList(
                new Parameter("id", "STRING", ParameterMode.IN),
                new Parameter("name", "STRING", ParameterMode.IN),
                new Parameter("username", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_GET_API_KEY", (args, listener) -> {
                String id = args.size() > 0 && args.get(0) != null ? args.get(0).toString() : null;
                String name = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : null;
                String username = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : null;
                
                GetApiKeyRequest request = GetApiKeyRequest.builder()
                    .apiKeyId(id)
                    .apiKeyName(name)
                    .userName(username)
                    .build();
                
                client.execute(GetApiKeyAction.INSTANCE, request, new ActionListener<GetApiKeyResponse>() {
                    @Override
                    public void onResponse(GetApiKeyResponse response) {
                        List<Map<String, Object>> keys = new java.util.ArrayList<>();
                        for (var apiKey : response.getApiKeyInfoList()) {
                            Map<String, Object> k = new HashMap<>();
                            k.put("id", apiKey.getId());
                            k.put("name", apiKey.getName());
                            k.put("username", apiKey.getUsername());
                            k.put("creation", apiKey.getCreation().toEpochMilli());
                            if (apiKey.getExpiration() != null) {
                                k.put("expiration", apiKey.getExpiration().toEpochMilli());
                            }
                            k.put("invalidated", apiKey.isInvalidated());
                            keys.add(k);
                        }
                        listener.onResponse(keys);
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
        name = "ES_INVALIDATE_API_KEY",
        description = "Invalidate API keys",
        parameters = {
            @FunctionParam(name = "id", type = "STRING", description = "API key ID to invalidate"),
            @FunctionParam(name = "name", type = "STRING", description = "API key name to invalidate")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Invalidation result"),
        examples = {"ES_INVALIDATE_API_KEY('key-id', null)"},
        category = FunctionCategory.SECURITY
    )
    public static void registerInvalidateApiKey(ExecutionContext context, Client client) {
        context.declareFunction("ES_INVALIDATE_API_KEY",
            Arrays.asList(
                new Parameter("id", "STRING", ParameterMode.IN),
                new Parameter("name", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_INVALIDATE_API_KEY", (args, listener) -> {
                String id = args.size() > 0 && args.get(0) != null ? args.get(0).toString() : null;
                String name = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : null;
                
                InvalidateApiKeyRequest request = InvalidateApiKeyRequest.builder()
                    .apiKeyId(id)
                    .apiKeyName(name)
                    .build();
                
                client.execute(InvalidateApiKeyAction.INSTANCE, request, new ActionListener<InvalidateApiKeyResponse>() {
                    @Override
                    public void onResponse(InvalidateApiKeyResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("invalidated_api_keys", response.getInvalidatedApiKeys());
                        result.put("previously_invalidated", response.getPreviouslyInvalidatedApiKeys());
                        result.put("error_count", response.getErrors().size());
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
        name = "ES_GET_PRIVILEGES",
        description = "Get application privileges",
        parameters = {
            @FunctionParam(name = "application", type = "STRING", description = "Application name"),
            @FunctionParam(name = "privilege", type = "STRING", description = "Privilege name")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Privileges"),
        examples = {"ES_GET_PRIVILEGES('myapp', null)"},
        category = FunctionCategory.SECURITY
    )
    public static void registerGetPrivileges(ExecutionContext context, Client client) {
        context.declareFunction("ES_GET_PRIVILEGES",
            Arrays.asList(
                new Parameter("application", "STRING", ParameterMode.IN),
                new Parameter("privilege", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_GET_PRIVILEGES", (args, listener) -> {
                String application = args.size() > 0 && args.get(0) != null ? args.get(0).toString() : null;
                String privilege = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : null;
                
                GetPrivilegesRequest request = new GetPrivilegesRequest(application, 
                    privilege != null ? new String[]{privilege} : null);
                
                client.execute(GetPrivilegesAction.INSTANCE, request, new ActionListener<GetPrivilegesResponse>() {
                    @Override
                    public void onResponse(GetPrivilegesResponse response) {
                        Map<String, Object> result = new HashMap<>();
                        for (var priv : response.privileges()) {
                            Map<String, Object> p = new HashMap<>();
                            p.put("application", priv.getApplication());
                            p.put("name", priv.getName());
                            p.put("actions", priv.getActions());
                            result.put(priv.getApplication() + ":" + priv.getName(), p);
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
        name = "ES_AUTHENTICATE",
        description = "Get current user authentication info",
        parameters = {},
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Authentication details"),
        examples = {"ES_AUTHENTICATE()"},
        category = FunctionCategory.SECURITY
    )
    public static void registerAuthenticate(ExecutionContext context, Client client) {
        context.declareFunction("ES_AUTHENTICATE",
            List.of(),
            new BuiltInFunctionDefinition("ES_AUTHENTICATE", (args, listener) -> {
                org.elasticsearch.xpack.core.security.action.user.AuthenticateRequest request = 
                    new org.elasticsearch.xpack.core.security.action.user.AuthenticateRequest();
                
                client.execute(org.elasticsearch.xpack.core.security.action.user.AuthenticateAction.INSTANCE, request,
                    new ActionListener<org.elasticsearch.xpack.core.security.action.user.AuthenticateResponse>() {
                        @Override
                        public void onResponse(org.elasticsearch.xpack.core.security.action.user.AuthenticateResponse response) {
                            Map<String, Object> result = new HashMap<>();
                            var auth = response.authentication();
                            result.put("username", auth.getEffectiveSubject().getUser().principal());
                            result.put("roles", Arrays.asList(auth.getEffectiveSubject().getUser().roles()));
                            result.put("authentication_type", auth.getAuthenticationType().name());
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
        name = "ES_HAS_PRIVILEGES",
        description = "Check if user has specific privileges",
        parameters = {
            @FunctionParam(name = "cluster", type = "ARRAY", description = "Cluster privileges to check"),
            @FunctionParam(name = "index", type = "ARRAY", description = "Index privileges to check")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Privilege check results"),
        examples = {"ES_HAS_PRIVILEGES(['monitor'], [{'names': ['logs-*'], 'privileges': ['read']}])"},
        category = FunctionCategory.SECURITY
    )
    public static void registerHasPrivileges(ExecutionContext context, Client client) {
        context.declareFunction("ES_HAS_PRIVILEGES",
            Arrays.asList(
                new Parameter("cluster", "ARRAY", ParameterMode.IN),
                new Parameter("index", "ARRAY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_HAS_PRIVILEGES", (args, listener) -> {
                @SuppressWarnings("unchecked")
                List<String> clusterPrivs = args.size() > 0 && args.get(0) != null ? 
                    (List<String>) args.get(0) : List.of();
                
                org.elasticsearch.xpack.core.security.action.user.HasPrivilegesRequest request = 
                    new org.elasticsearch.xpack.core.security.action.user.HasPrivilegesRequest();
                request.clusterPrivileges(clusterPrivs.toArray(new String[0]));
                
                client.execute(org.elasticsearch.xpack.core.security.action.user.HasPrivilegesAction.INSTANCE, request,
                    new ActionListener<org.elasticsearch.xpack.core.security.action.user.HasPrivilegesResponse>() {
                        @Override
                        public void onResponse(org.elasticsearch.xpack.core.security.action.user.HasPrivilegesResponse response) {
                            Map<String, Object> result = new HashMap<>();
                            result.put("has_all_requested", response.isCompleteMatch());
                            result.put("username", response.getUsername());
                            result.put("cluster", response.getClusterPrivileges());
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
        name = "ES_CLEAR_SECURITY_CACHE",
        description = "Clear security caches",
        parameters = {
            @FunctionParam(name = "cache_type", type = "STRING", description = "Cache type: realm, user, roles, privileges")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if cleared"),
        examples = {"ES_CLEAR_SECURITY_CACHE('realm')"},
        category = FunctionCategory.SECURITY
    )
    public static void registerClearCache(ExecutionContext context, Client client) {
        context.declareFunction("ES_CLEAR_SECURITY_CACHE",
            List.of(new Parameter("cache_type", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_CLEAR_SECURITY_CACHE", (args, listener) -> {
                String cacheType = args.get(0).toString();
                
                org.elasticsearch.xpack.core.security.action.realm.ClearRealmCacheRequest request = 
                    new org.elasticsearch.xpack.core.security.action.realm.ClearRealmCacheRequest();
                
                client.execute(org.elasticsearch.xpack.core.security.action.realm.ClearRealmCacheAction.INSTANCE, request,
                    new ActionListener<org.elasticsearch.xpack.core.security.action.realm.ClearRealmCacheResponse>() {
                        @Override
                        public void onResponse(org.elasticsearch.xpack.core.security.action.realm.ClearRealmCacheResponse response) {
                            listener.onResponse(true);
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
