/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.builtin.kibana;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;
import org.elasticsearch.xpack.escript.functions.api.FunctionCategory;
import org.elasticsearch.xpack.escript.functions.api.FunctionCollectionSpec;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.xpack.escript.functions.builtin.kibana.KibanaFunctions.*;

/**
 * Kibana Fleet API functions for elastic-script.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.RUNBOOK,
    description = "Fleet functions for agent and policy management."
)
public class FleetFunctions {

    public static void registerAll(ExecutionContext context) {
        // Agents
        registerAgentList(context);
        registerAgentGet(context);
        registerAgentUnenroll(context);
        registerAgentReassign(context);
        registerAgentUpgrade(context);
        registerAgentBulkUpgrade(context);
        registerAgentAction(context);
        
        // Agent Policies
        registerAgentPolicyList(context);
        registerAgentPolicyGet(context);
        registerAgentPolicyCreate(context);
        registerAgentPolicyUpdate(context);
        registerAgentPolicyDelete(context);
        registerAgentPolicyCopy(context);
        
        // Package Policies
        registerPackagePolicyList(context);
        registerPackagePolicyCreate(context);
        registerPackagePolicyUpdate(context);
        registerPackagePolicyDelete(context);
        
        // Packages (Integrations)
        registerPackageList(context);
        registerPackageInstall(context);
        registerPackageUninstall(context);
        
        // Enrollment Tokens
        registerEnrollmentTokenList(context);
        registerEnrollmentTokenCreate(context);
    }

    // ========================================================================
    // Agents
    // ========================================================================

    public static void registerAgentList(ExecutionContext context) {
        context.declareFunction("AGENT_LIST",
            List.of(
                new Parameter("per_page", "INTEGER", ParameterMode.IN),
                new Parameter("page", "INTEGER", ParameterMode.IN),
                new Parameter("kuery", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("AGENT_LIST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    int perPage = args.size() > 0 && args.get(0) != null ? ((Number) args.get(0)).intValue() : 20;
                    int page = args.size() > 1 && args.get(1) != null ? ((Number) args.get(1)).intValue() : 1;
                    String kuery = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : "";
                    
                    StringBuilder path = new StringBuilder("/api/fleet/agents?perPage=" + perPage + "&page=" + page);
                    if (!kuery.isEmpty()) {
                        path.append("&kuery=").append(java.net.URLEncoder.encode(kuery, "UTF-8"));
                    }
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", path.toString(), null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("AGENT_LIST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAgentGet(ExecutionContext context) {
        context.declareFunction("AGENT_GET",
            List.of(new Parameter("agent_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("AGENT_GET", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String agentId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/fleet/agents/" + agentId, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("AGENT_GET failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAgentUnenroll(ExecutionContext context) {
        context.declareFunction("AGENT_UNENROLL",
            List.of(
                new Parameter("agent_id", "STRING", ParameterMode.IN),
                new Parameter("revoke", "BOOLEAN", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("AGENT_UNENROLL", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String agentId = args.get(0).toString();
                    boolean revoke = args.size() > 1 && args.get(1) != null ? (Boolean) args.get(1) : false;
                    
                    Map<String, Object> body = Map.of("revoke", revoke);
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/fleet/agents/" + agentId + "/unenroll", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("AGENT_UNENROLL failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAgentReassign(ExecutionContext context) {
        context.declareFunction("AGENT_REASSIGN",
            List.of(
                new Parameter("agent_id", "STRING", ParameterMode.IN),
                new Parameter("policy_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("AGENT_REASSIGN", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String agentId = args.get(0).toString();
                    String policyId = args.get(1).toString();
                    
                    Map<String, Object> body = Map.of("policy_id", policyId);
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/fleet/agents/" + agentId + "/reassign", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("AGENT_REASSIGN failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAgentUpgrade(ExecutionContext context) {
        context.declareFunction("AGENT_UPGRADE",
            List.of(
                new Parameter("agent_id", "STRING", ParameterMode.IN),
                new Parameter("version", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("AGENT_UPGRADE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String agentId = args.get(0).toString();
                    String version = args.get(1).toString();
                    
                    Map<String, Object> body = Map.of("version", version);
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/fleet/agents/" + agentId + "/upgrade", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("AGENT_UPGRADE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAgentBulkUpgrade(ExecutionContext context) {
        context.declareFunction("AGENT_BULK_UPGRADE",
            List.of(
                new Parameter("agents", "ARRAY", ParameterMode.IN),
                new Parameter("version", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("AGENT_BULK_UPGRADE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    List<Object> agents = toList(args.get(0));
                    String version = args.get(1).toString();
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("agents", agents);
                    body.put("version", version);
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/fleet/agents/bulk_upgrade", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("AGENT_BULK_UPGRADE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAgentAction(ExecutionContext context) {
        context.declareFunction("AGENT_ACTION",
            List.of(
                new Parameter("agent_id", "STRING", ParameterMode.IN),
                new Parameter("action", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("AGENT_ACTION", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String agentId = args.get(0).toString();
                    Map<String, Object> action = toMap(args.get(1));
                    
                    Map<String, Object> body = Map.of("action", action);
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/fleet/agents/" + agentId + "/actions", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("AGENT_ACTION failed: " + e.getMessage(), e));
                }
            })
        );
    }

    // ========================================================================
    // Agent Policies
    // ========================================================================

    public static void registerAgentPolicyList(ExecutionContext context) {
        context.declareFunction("AGENT_POLICY_LIST",
            List.of(
                new Parameter("per_page", "INTEGER", ParameterMode.IN),
                new Parameter("page", "INTEGER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("AGENT_POLICY_LIST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    int perPage = args.size() > 0 && args.get(0) != null ? ((Number) args.get(0)).intValue() : 20;
                    int page = args.size() > 1 && args.get(1) != null ? ((Number) args.get(1)).intValue() : 1;
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", 
                        "/api/fleet/agent_policies?perPage=" + perPage + "&page=" + page, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("AGENT_POLICY_LIST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAgentPolicyGet(ExecutionContext context) {
        context.declareFunction("AGENT_POLICY_GET",
            List.of(new Parameter("policy_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("AGENT_POLICY_GET", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String policyId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/fleet/agent_policies/" + policyId, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("AGENT_POLICY_GET failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAgentPolicyCreate(ExecutionContext context) {
        context.declareFunction("AGENT_POLICY_CREATE",
            List.of(
                new Parameter("name", "STRING", ParameterMode.IN),
                new Parameter("namespace", "STRING", ParameterMode.IN),
                new Parameter("description", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("AGENT_POLICY_CREATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String name = args.get(0).toString();
                    String namespace = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : "default";
                    String description = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : "";
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("name", name);
                    body.put("namespace", namespace);
                    body.put("description", description);
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/fleet/agent_policies", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("AGENT_POLICY_CREATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAgentPolicyUpdate(ExecutionContext context) {
        context.declareFunction("AGENT_POLICY_UPDATE",
            List.of(
                new Parameter("policy_id", "STRING", ParameterMode.IN),
                new Parameter("updates", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("AGENT_POLICY_UPDATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String policyId = args.get(0).toString();
                    Map<String, Object> updates = toMap(args.get(1));
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "PUT", "/api/fleet/agent_policies/" + policyId, updates);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("AGENT_POLICY_UPDATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAgentPolicyDelete(ExecutionContext context) {
        context.declareFunction("AGENT_POLICY_DELETE",
            List.of(new Parameter("policy_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("AGENT_POLICY_DELETE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String policyId = args.get(0).toString();
                    Map<String, Object> body = Map.of("agentPolicyId", policyId);
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/fleet/agent_policies/delete", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("AGENT_POLICY_DELETE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAgentPolicyCopy(ExecutionContext context) {
        context.declareFunction("AGENT_POLICY_COPY",
            List.of(
                new Parameter("policy_id", "STRING", ParameterMode.IN),
                new Parameter("new_name", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("AGENT_POLICY_COPY", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String policyId = args.get(0).toString();
                    String newName = args.get(1).toString();
                    
                    Map<String, Object> body = Map.of("name", newName);
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/fleet/agent_policies/" + policyId + "/copy", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("AGENT_POLICY_COPY failed: " + e.getMessage(), e));
                }
            })
        );
    }

    // ========================================================================
    // Package Policies
    // ========================================================================

    public static void registerPackagePolicyList(ExecutionContext context) {
        context.declareFunction("PACKAGE_POLICY_LIST",
            List.of(
                new Parameter("per_page", "INTEGER", ParameterMode.IN),
                new Parameter("kuery", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("PACKAGE_POLICY_LIST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    int perPage = args.size() > 0 && args.get(0) != null ? ((Number) args.get(0)).intValue() : 20;
                    String kuery = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : "";
                    
                    StringBuilder path = new StringBuilder("/api/fleet/package_policies?perPage=" + perPage);
                    if (!kuery.isEmpty()) {
                        path.append("&kuery=").append(java.net.URLEncoder.encode(kuery, "UTF-8"));
                    }
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", path.toString(), null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("PACKAGE_POLICY_LIST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerPackagePolicyCreate(ExecutionContext context) {
        context.declareFunction("PACKAGE_POLICY_CREATE",
            List.of(new Parameter("config", "DOCUMENT", ParameterMode.IN)),
            new BuiltInFunctionDefinition("PACKAGE_POLICY_CREATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    Map<String, Object> config = toMap(args.get(0));
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/fleet/package_policies", config);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("PACKAGE_POLICY_CREATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerPackagePolicyUpdate(ExecutionContext context) {
        context.declareFunction("PACKAGE_POLICY_UPDATE",
            List.of(
                new Parameter("policy_id", "STRING", ParameterMode.IN),
                new Parameter("config", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("PACKAGE_POLICY_UPDATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String policyId = args.get(0).toString();
                    Map<String, Object> config = toMap(args.get(1));
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "PUT", "/api/fleet/package_policies/" + policyId, config);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("PACKAGE_POLICY_UPDATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerPackagePolicyDelete(ExecutionContext context) {
        context.declareFunction("PACKAGE_POLICY_DELETE",
            List.of(new Parameter("policy_ids", "ARRAY", ParameterMode.IN)),
            new BuiltInFunctionDefinition("PACKAGE_POLICY_DELETE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    List<Object> policyIds = toList(args.get(0));
                    Map<String, Object> body = Map.of("packagePolicyIds", policyIds);
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/fleet/package_policies/delete", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("PACKAGE_POLICY_DELETE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    // ========================================================================
    // Packages (Integrations)
    // ========================================================================

    public static void registerPackageList(ExecutionContext context) {
        context.declareFunction("PACKAGE_LIST",
            List.of(new Parameter("category", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("PACKAGE_LIST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String category = args.size() > 0 && args.get(0) != null ? args.get(0).toString() : "";
                    
                    StringBuilder path = new StringBuilder("/api/fleet/epm/packages");
                    if (!category.isEmpty()) {
                        path.append("?category=").append(category);
                    }
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", path.toString(), null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("PACKAGE_LIST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerPackageInstall(ExecutionContext context) {
        context.declareFunction("PACKAGE_INSTALL",
            List.of(
                new Parameter("package_name", "STRING", ParameterMode.IN),
                new Parameter("version", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("PACKAGE_INSTALL", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String packageName = args.get(0).toString();
                    String version = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : "";
                    
                    String path = "/api/fleet/epm/packages/" + packageName;
                    if (!version.isEmpty()) {
                        path += "/" + version;
                    }
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", path, new HashMap<>());
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("PACKAGE_INSTALL failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerPackageUninstall(ExecutionContext context) {
        context.declareFunction("PACKAGE_UNINSTALL",
            List.of(
                new Parameter("package_name", "STRING", ParameterMode.IN),
                new Parameter("version", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("PACKAGE_UNINSTALL", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String packageName = args.get(0).toString();
                    String version = args.get(1).toString();
                    
                    String path = "/api/fleet/epm/packages/" + packageName + "/" + version;
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "DELETE", path, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("PACKAGE_UNINSTALL failed: " + e.getMessage(), e));
                }
            })
        );
    }

    // ========================================================================
    // Enrollment Tokens
    // ========================================================================

    public static void registerEnrollmentTokenList(ExecutionContext context) {
        context.declareFunction("ENROLLMENT_TOKEN_LIST",
            List.of(new Parameter("policy_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ENROLLMENT_TOKEN_LIST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String policyId = args.size() > 0 && args.get(0) != null ? args.get(0).toString() : "";
                    
                    StringBuilder path = new StringBuilder("/api/fleet/enrollment_api_keys");
                    if (!policyId.isEmpty()) {
                        path.append("?kuery=").append(java.net.URLEncoder.encode("policy_id:" + policyId, "UTF-8"));
                    }
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", path.toString(), null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ENROLLMENT_TOKEN_LIST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerEnrollmentTokenCreate(ExecutionContext context) {
        context.declareFunction("ENROLLMENT_TOKEN_CREATE",
            List.of(
                new Parameter("policy_id", "STRING", ParameterMode.IN),
                new Parameter("name", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ENROLLMENT_TOKEN_CREATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String policyId = args.get(0).toString();
                    String name = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : "";
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("policy_id", policyId);
                    if (!name.isEmpty()) {
                        body.put("name", name);
                    }
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/fleet/enrollment_api_keys", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ENROLLMENT_TOKEN_CREATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @SuppressWarnings("unchecked")
    private static List<Object> toList(Object obj) {
        if (obj instanceof List) {
            return (List<Object>) obj;
        }
        return new ArrayList<>();
    }
}
