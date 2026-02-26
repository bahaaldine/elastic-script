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
 * Elasticsearch Security API functions using REST HTTP calls.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.DATASOURCE,
    description = "Elasticsearch Security API functions."
)
public class SecurityApiFunctions extends ElasticsearchHttpFunctions {

    public static void registerAll(ExecutionContext context) {
        registerGetUsers(context);
        registerCreateUser(context);
        registerDeleteUser(context);
        registerGetRoles(context);
        registerCreateRole(context);
        registerDeleteRole(context);
        registerCreateApiKey(context);
        registerGetApiKey(context);
        registerInvalidateApiKey(context);
        registerAuthenticate(context);
        registerHasPrivileges(context);
        registerGetPrivileges(context);
    }

    @FunctionSpec(
        name = "ES_GET_USERS",
        description = "Get security users",
        parameters = {
            @FunctionParam(name = "username", type = "STRING", description = "Username or empty for all")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "User information"),
        examples = {"ES_GET_USERS('elastic')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetUsers(ExecutionContext context) {
        context.declareFunction("ES_GET_USERS",
            List.of(new Parameter("username", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_GET_USERS", (args, listener) -> {
                try {
                    String username = args.size() > 0 && args.get(0) != null ? toString(args.get(0)) : "";
                    String path = username.isEmpty() ? "/_security/user" : "/_security/user/" + username;
                    Map<String, Object> result = esRequest("GET", path, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_GET_USERS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_CREATE_USER",
        description = "Create or update a user",
        parameters = {
            @FunctionParam(name = "username", type = "STRING", description = "Username"),
            @FunctionParam(name = "user", type = "DOCUMENT", description = "User definition")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Create result"),
        examples = {"ES_CREATE_USER('newuser', {'password': 'secret', 'roles': ['admin']})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerCreateUser(ExecutionContext context) {
        context.declareFunction("ES_CREATE_USER",
            List.of(
                new Parameter("username", "STRING", ParameterMode.IN),
                new Parameter("user", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_CREATE_USER", (args, listener) -> {
                try {
                    String username = toString(args.get(0));
                    Map<String, Object> user = toMap(args.get(1));
                    Map<String, Object> result = esRequest("PUT", "/_security/user/" + username, user);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_CREATE_USER failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_DELETE_USER",
        description = "Delete a user",
        parameters = {
            @FunctionParam(name = "username", type = "STRING", description = "Username")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Delete result"),
        examples = {"ES_DELETE_USER('olduser')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDeleteUser(ExecutionContext context) {
        context.declareFunction("ES_DELETE_USER",
            List.of(new Parameter("username", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_DELETE_USER", (args, listener) -> {
                try {
                    String username = toString(args.get(0));
                    Map<String, Object> result = esRequest("DELETE", "/_security/user/" + username, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_DELETE_USER failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_GET_ROLES",
        description = "Get security roles",
        parameters = {
            @FunctionParam(name = "role_name", type = "STRING", description = "Role name or empty for all")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Role information"),
        examples = {"ES_GET_ROLES('admin')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetRoles(ExecutionContext context) {
        context.declareFunction("ES_GET_ROLES",
            List.of(new Parameter("role_name", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_GET_ROLES", (args, listener) -> {
                try {
                    String roleName = args.size() > 0 && args.get(0) != null ? toString(args.get(0)) : "";
                    String path = roleName.isEmpty() ? "/_security/role" : "/_security/role/" + roleName;
                    Map<String, Object> result = esRequest("GET", path, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_GET_ROLES failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_CREATE_ROLE",
        description = "Create or update a role",
        parameters = {
            @FunctionParam(name = "role_name", type = "STRING", description = "Role name"),
            @FunctionParam(name = "role", type = "DOCUMENT", description = "Role definition")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Create result"),
        examples = {"ES_CREATE_ROLE('my-role', {'cluster': ['monitor'], 'indices': [...]})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerCreateRole(ExecutionContext context) {
        context.declareFunction("ES_CREATE_ROLE",
            List.of(
                new Parameter("role_name", "STRING", ParameterMode.IN),
                new Parameter("role", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_CREATE_ROLE", (args, listener) -> {
                try {
                    String roleName = toString(args.get(0));
                    Map<String, Object> role = toMap(args.get(1));
                    Map<String, Object> result = esRequest("PUT", "/_security/role/" + roleName, role);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_CREATE_ROLE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_DELETE_ROLE",
        description = "Delete a role",
        parameters = {
            @FunctionParam(name = "role_name", type = "STRING", description = "Role name")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Delete result"),
        examples = {"ES_DELETE_ROLE('my-role')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDeleteRole(ExecutionContext context) {
        context.declareFunction("ES_DELETE_ROLE",
            List.of(new Parameter("role_name", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_DELETE_ROLE", (args, listener) -> {
                try {
                    String roleName = toString(args.get(0));
                    Map<String, Object> result = esRequest("DELETE", "/_security/role/" + roleName, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_DELETE_ROLE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_CREATE_API_KEY",
        description = "Create an API key",
        parameters = {
            @FunctionParam(name = "name", type = "STRING", description = "API key name"),
            @FunctionParam(name = "role_descriptors", type = "DOCUMENT", description = "Role descriptors"),
            @FunctionParam(name = "expiration", type = "STRING", description = "Expiration time (e.g., '1d')")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "API key with id and encoded key"),
        examples = {"ES_CREATE_API_KEY('my-key', {}, '30d')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerCreateApiKey(ExecutionContext context) {
        context.declareFunction("ES_CREATE_API_KEY",
            List.of(
                new Parameter("name", "STRING", ParameterMode.IN),
                new Parameter("role_descriptors", "DOCUMENT", ParameterMode.IN),
                new Parameter("expiration", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_CREATE_API_KEY", (args, listener) -> {
                try {
                    String name = toString(args.get(0));
                    Map<String, Object> roleDescriptors = args.size() > 1 && args.get(1) != null ? toMap(args.get(1)) : new HashMap<>();
                    String expiration = args.size() > 2 && args.get(2) != null ? toString(args.get(2)) : null;
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("name", name);
                    if (!roleDescriptors.isEmpty()) {
                        body.put("role_descriptors", roleDescriptors);
                    }
                    if (expiration != null && !expiration.isEmpty()) {
                        body.put("expiration", expiration);
                    }
                    
                    Map<String, Object> result = esRequest("POST", "/_security/api_key", body);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_CREATE_API_KEY failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_GET_API_KEY",
        description = "Get API key information",
        parameters = {
            @FunctionParam(name = "id", type = "STRING", description = "API key ID"),
            @FunctionParam(name = "name", type = "STRING", description = "API key name")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "API key information"),
        examples = {"ES_GET_API_KEY('key-id', null)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetApiKey(ExecutionContext context) {
        context.declareFunction("ES_GET_API_KEY",
            List.of(
                new Parameter("id", "STRING", ParameterMode.IN),
                new Parameter("name", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_GET_API_KEY", (args, listener) -> {
                try {
                    String id = args.get(0) != null ? toString(args.get(0)) : null;
                    String name = args.size() > 1 && args.get(1) != null ? toString(args.get(1)) : null;
                    
                    StringBuilder path = new StringBuilder("/_security/api_key?");
                    if (id != null && !id.isEmpty()) {
                        path.append("id=").append(id);
                    } else if (name != null && !name.isEmpty()) {
                        path.append("name=").append(name);
                    }
                    
                    Map<String, Object> result = esRequest("GET", path.toString(), null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_GET_API_KEY failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_INVALIDATE_API_KEY",
        description = "Invalidate an API key",
        parameters = {
            @FunctionParam(name = "id", type = "STRING", description = "API key ID to invalidate")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Invalidation result"),
        examples = {"ES_INVALIDATE_API_KEY('key-id')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerInvalidateApiKey(ExecutionContext context) {
        context.declareFunction("ES_INVALIDATE_API_KEY",
            List.of(new Parameter("id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_INVALIDATE_API_KEY", (args, listener) -> {
                try {
                    String id = toString(args.get(0));
                    Map<String, Object> body = new HashMap<>();
                    body.put("ids", List.of(id));
                    Map<String, Object> result = esRequest("DELETE", "/_security/api_key", body);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_INVALIDATE_API_KEY failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_AUTHENTICATE",
        description = "Get current authenticated user",
        parameters = {},
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Authenticated user info"),
        examples = {"ES_AUTHENTICATE()"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerAuthenticate(ExecutionContext context) {
        context.declareFunction("ES_AUTHENTICATE",
            List.of(),
            new BuiltInFunctionDefinition("ES_AUTHENTICATE", (args, listener) -> {
                try {
                    Map<String, Object> result = esRequest("GET", "/_security/_authenticate", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_AUTHENTICATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_HAS_PRIVILEGES",
        description = "Check if user has specified privileges",
        parameters = {
            @FunctionParam(name = "privileges", type = "DOCUMENT", description = "Privileges to check")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Privilege check result"),
        examples = {"ES_HAS_PRIVILEGES({'cluster': ['monitor'], 'index': [...]})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerHasPrivileges(ExecutionContext context) {
        context.declareFunction("ES_HAS_PRIVILEGES",
            List.of(new Parameter("privileges", "DOCUMENT", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_HAS_PRIVILEGES", (args, listener) -> {
                try {
                    Map<String, Object> privileges = toMap(args.get(0));
                    Map<String, Object> result = esRequest("POST", "/_security/user/_has_privileges", privileges);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_HAS_PRIVILEGES failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_GET_PRIVILEGES",
        description = "Get user privileges",
        parameters = {},
        returnType = @FunctionReturn(type = "DOCUMENT", description = "User privileges"),
        examples = {"ES_GET_PRIVILEGES()"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetPrivileges(ExecutionContext context) {
        context.declareFunction("ES_GET_PRIVILEGES",
            List.of(),
            new BuiltInFunctionDefinition("ES_GET_PRIVILEGES", (args, listener) -> {
                try {
                    Map<String, Object> result = esRequest("GET", "/_security/privilege", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_GET_PRIVILEGES failed: " + e.getMessage(), e));
                }
            })
        );
    }
}
