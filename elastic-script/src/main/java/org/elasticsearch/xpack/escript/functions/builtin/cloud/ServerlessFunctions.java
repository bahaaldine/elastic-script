/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.builtin.cloud;

import org.elasticsearch.xpack.escript.functions.api.FunctionCollectionSpec;
import org.elasticsearch.xpack.escript.functions.api.FunctionCategory;
import org.elasticsearch.xpack.escript.functions.api.FunctionParam;
import org.elasticsearch.xpack.escript.functions.api.FunctionReturn;
import org.elasticsearch.xpack.escript.functions.api.FunctionSpec;

import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.common.Strings;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.XContentParserConfiguration;
import org.elasticsearch.xcontent.XContentType;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Built-in functions for Elastic Cloud Serverless API.
 * Base URL: https://api.elastic-cloud.com
 * 
 * Covers:
 * - Elasticsearch Projects (9 functions)
 * - Observability Projects (9 functions)
 * - Security Projects (9 functions)
 * - Linked Candidate Projects (4 functions)
 * - Regions (2 functions)
 * - Traffic Filters (6 functions)
 * 
 * Total: 39 functions
 */
@FunctionCollectionSpec(
    category = FunctionCategory.CLOUD,
    description = "Elastic Cloud Serverless API functions for managing projects, regions, and traffic filters"
)
public class ServerlessFunctions {

    private static final String BASE_URL = "https://api.elastic-cloud.com";

    public static void registerAll(ExecutionContext context) {
        // Elasticsearch Projects
        registerListElasticsearchProjects(context);
        registerGetElasticsearchProject(context);
        registerCreateElasticsearchProject(context);
        registerUpdateElasticsearchProject(context);
        registerDeleteElasticsearchProject(context);
        registerGetElasticsearchProjectStatus(context);
        registerGetElasticsearchProjectRoles(context);
        registerResetElasticsearchProjectCredentials(context);
        registerResumeElasticsearchProject(context);

        // Observability Projects
        registerListObservabilityProjects(context);
        registerGetObservabilityProject(context);
        registerCreateObservabilityProject(context);
        registerUpdateObservabilityProject(context);
        registerDeleteObservabilityProject(context);
        registerGetObservabilityProjectStatus(context);
        registerGetObservabilityProjectRoles(context);
        registerResetObservabilityProjectCredentials(context);
        registerResumeObservabilityProject(context);

        // Security Projects
        registerListSecurityProjects(context);
        registerGetSecurityProject(context);
        registerCreateSecurityProject(context);
        registerUpdateSecurityProject(context);
        registerDeleteSecurityProject(context);
        registerGetSecurityProjectStatus(context);
        registerGetSecurityProjectRoles(context);
        registerResetSecurityProjectCredentials(context);
        registerResumeSecurityProject(context);

        // Linked Candidate Projects
        registerGetElasticsearchLinkCandidates(context);
        registerGetObservabilityLinkCandidates(context);
        registerGetSecurityLinkCandidates(context);
        registerGetWorkplaceAILinkCandidates(context);

        // Regions
        registerListRegions(context);
        registerGetRegion(context);

        // Traffic Filters
        registerListTrafficFilters(context);
        registerGetTrafficFilter(context);
        registerCreateTrafficFilter(context);
        registerUpdateTrafficFilter(context);
        registerDeleteTrafficFilter(context);
        registerGetTrafficFilterMetadata(context);
    }

    // ============================================
    // Elasticsearch Projects
    // ============================================

    @FunctionSpec(
        name = "SERVERLESS_LIST_ES_PROJECTS",
        description = "List all Elasticsearch serverless projects",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "List of Elasticsearch projects"),
        examples = {"SERVERLESS_LIST_ES_PROJECTS('my-api-key')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerListElasticsearchProjects(ExecutionContext context) {
        context.declareFunction("SERVERLESS_LIST_ES_PROJECTS",
            List.of(new Parameter("api_key", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("SERVERLESS_LIST_ES_PROJECTS", (args, listener) -> {
                String apiKey = args.get(0).toString();
                executeAsync(() -> apiRequest("GET", "/api/v1/serverless/projects/elasticsearch", null, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_GET_ES_PROJECT",
        description = "Get an Elasticsearch serverless project by ID",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "project_id", type = "STRING", description = "Project ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Elasticsearch project details"),
        examples = {"SERVERLESS_GET_ES_PROJECT('api-key', 'project-123')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerGetElasticsearchProject(ExecutionContext context) {
        context.declareFunction("SERVERLESS_GET_ES_PROJECT",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("project_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_GET_ES_PROJECT", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String projectId = args.get(1).toString();
                executeAsync(() -> apiRequest("GET", "/api/v1/serverless/projects/elasticsearch/" + projectId, null, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_CREATE_ES_PROJECT",
        description = "Create a new Elasticsearch serverless project",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "name", type = "STRING", description = "Project name"),
            @FunctionParam(name = "region_id", type = "STRING", description = "Region ID (e.g., 'aws-us-east-1')"),
            @FunctionParam(name = "config", type = "DOCUMENT", description = "Optional additional configuration")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Created project with credentials"),
        examples = {"SERVERLESS_CREATE_ES_PROJECT('api-key', 'my-project', 'aws-us-east-1', {})"},
        category = FunctionCategory.CLOUD
    )
    public static void registerCreateElasticsearchProject(ExecutionContext context) {
        context.declareFunction("SERVERLESS_CREATE_ES_PROJECT",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("name", "STRING", ParameterMode.IN),
                new Parameter("region_id", "STRING", ParameterMode.IN),
                new Parameter("config", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_CREATE_ES_PROJECT", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String name = args.get(1).toString();
                String regionId = args.get(2).toString();
                @SuppressWarnings("unchecked")
                Map<String, Object> config = args.get(3) instanceof Map ? (Map<String, Object>) args.get(3) : new HashMap<>();
                
                Map<String, Object> body = new HashMap<>(config);
                body.put("name", name);
                body.put("region_id", regionId);
                
                executeAsync(() -> apiRequest("POST", "/api/v1/serverless/projects/elasticsearch", body, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_UPDATE_ES_PROJECT",
        description = "Update an Elasticsearch serverless project",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "project_id", type = "STRING", description = "Project ID"),
            @FunctionParam(name = "updates", type = "DOCUMENT", description = "Fields to update (name, alias, etc.)")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Updated project"),
        examples = {"SERVERLESS_UPDATE_ES_PROJECT('api-key', 'proj-123', {'name': 'new-name'})"},
        category = FunctionCategory.CLOUD
    )
    public static void registerUpdateElasticsearchProject(ExecutionContext context) {
        context.declareFunction("SERVERLESS_UPDATE_ES_PROJECT",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("project_id", "STRING", ParameterMode.IN),
                new Parameter("updates", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_UPDATE_ES_PROJECT", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String projectId = args.get(1).toString();
                @SuppressWarnings("unchecked")
                Map<String, Object> updates = (Map<String, Object>) args.get(2);
                executeAsync(() -> apiRequest("PATCH", "/api/v1/serverless/projects/elasticsearch/" + projectId, updates, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_DELETE_ES_PROJECT",
        description = "Delete an Elasticsearch serverless project",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "project_id", type = "STRING", description = "Project ID to delete")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Deletion confirmation"),
        examples = {"SERVERLESS_DELETE_ES_PROJECT('api-key', 'proj-123')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerDeleteElasticsearchProject(ExecutionContext context) {
        context.declareFunction("SERVERLESS_DELETE_ES_PROJECT",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("project_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_DELETE_ES_PROJECT", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String projectId = args.get(1).toString();
                executeAsync(() -> apiRequest("DELETE", "/api/v1/serverless/projects/elasticsearch/" + projectId, null, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_ES_PROJECT_STATUS",
        description = "Get the status of an Elasticsearch serverless project",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "project_id", type = "STRING", description = "Project ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Project status"),
        examples = {"SERVERLESS_ES_PROJECT_STATUS('api-key', 'proj-123')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerGetElasticsearchProjectStatus(ExecutionContext context) {
        context.declareFunction("SERVERLESS_ES_PROJECT_STATUS",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("project_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_ES_PROJECT_STATUS", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String projectId = args.get(1).toString();
                executeAsync(() -> apiRequest("GET", "/api/v1/serverless/projects/elasticsearch/" + projectId + "/status", null, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_ES_PROJECT_ROLES",
        description = "Get roles for an Elasticsearch serverless project",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "project_id", type = "STRING", description = "Project ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Project roles"),
        examples = {"SERVERLESS_ES_PROJECT_ROLES('api-key', 'proj-123')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerGetElasticsearchProjectRoles(ExecutionContext context) {
        context.declareFunction("SERVERLESS_ES_PROJECT_ROLES",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("project_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_ES_PROJECT_ROLES", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String projectId = args.get(1).toString();
                executeAsync(() -> apiRequest("GET", "/api/v1/serverless/projects/elasticsearch/" + projectId + "/roles", null, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_RESET_ES_CREDENTIALS",
        description = "Reset credentials for an Elasticsearch serverless project",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "project_id", type = "STRING", description = "Project ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "New credentials"),
        examples = {"SERVERLESS_RESET_ES_CREDENTIALS('api-key', 'proj-123')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerResetElasticsearchProjectCredentials(ExecutionContext context) {
        context.declareFunction("SERVERLESS_RESET_ES_CREDENTIALS",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("project_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_RESET_ES_CREDENTIALS", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String projectId = args.get(1).toString();
                executeAsync(() -> apiRequest("POST", "/api/v1/serverless/projects/elasticsearch/" + projectId + "/_reset-credentials", null, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_RESUME_ES_PROJECT",
        description = "Resume a paused Elasticsearch serverless project",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "project_id", type = "STRING", description = "Project ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Resume confirmation"),
        examples = {"SERVERLESS_RESUME_ES_PROJECT('api-key', 'proj-123')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerResumeElasticsearchProject(ExecutionContext context) {
        context.declareFunction("SERVERLESS_RESUME_ES_PROJECT",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("project_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_RESUME_ES_PROJECT", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String projectId = args.get(1).toString();
                executeAsync(() -> apiRequest("POST", "/api/v1/serverless/projects/elasticsearch/" + projectId + "/_resume", null, apiKey), listener);
            })
        );
    }

    // ============================================
    // Observability Projects
    // ============================================

    @FunctionSpec(
        name = "SERVERLESS_LIST_OBSERVABILITY_PROJECTS",
        description = "List all Observability serverless projects",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "List of Observability projects"),
        examples = {"SERVERLESS_LIST_OBSERVABILITY_PROJECTS('my-api-key')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerListObservabilityProjects(ExecutionContext context) {
        context.declareFunction("SERVERLESS_LIST_OBSERVABILITY_PROJECTS",
            List.of(new Parameter("api_key", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("SERVERLESS_LIST_OBSERVABILITY_PROJECTS", (args, listener) -> {
                String apiKey = args.get(0).toString();
                executeAsync(() -> apiRequest("GET", "/api/v1/serverless/projects/observability", null, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_GET_OBSERVABILITY_PROJECT",
        description = "Get an Observability serverless project by ID",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "project_id", type = "STRING", description = "Project ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Observability project details"),
        examples = {"SERVERLESS_GET_OBSERVABILITY_PROJECT('api-key', 'project-123')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerGetObservabilityProject(ExecutionContext context) {
        context.declareFunction("SERVERLESS_GET_OBSERVABILITY_PROJECT",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("project_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_GET_OBSERVABILITY_PROJECT", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String projectId = args.get(1).toString();
                executeAsync(() -> apiRequest("GET", "/api/v1/serverless/projects/observability/" + projectId, null, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_CREATE_OBSERVABILITY_PROJECT",
        description = "Create a new Observability serverless project",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "name", type = "STRING", description = "Project name"),
            @FunctionParam(name = "region_id", type = "STRING", description = "Region ID"),
            @FunctionParam(name = "config", type = "DOCUMENT", description = "Optional additional configuration")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Created project with credentials"),
        examples = {"SERVERLESS_CREATE_OBSERVABILITY_PROJECT('api-key', 'my-obs-project', 'aws-us-east-1', {})"},
        category = FunctionCategory.CLOUD
    )
    public static void registerCreateObservabilityProject(ExecutionContext context) {
        context.declareFunction("SERVERLESS_CREATE_OBSERVABILITY_PROJECT",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("name", "STRING", ParameterMode.IN),
                new Parameter("region_id", "STRING", ParameterMode.IN),
                new Parameter("config", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_CREATE_OBSERVABILITY_PROJECT", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String name = args.get(1).toString();
                String regionId = args.get(2).toString();
                @SuppressWarnings("unchecked")
                Map<String, Object> config = args.get(3) instanceof Map ? (Map<String, Object>) args.get(3) : new HashMap<>();
                
                Map<String, Object> body = new HashMap<>(config);
                body.put("name", name);
                body.put("region_id", regionId);
                
                executeAsync(() -> apiRequest("POST", "/api/v1/serverless/projects/observability", body, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_UPDATE_OBSERVABILITY_PROJECT",
        description = "Update an Observability serverless project",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "project_id", type = "STRING", description = "Project ID"),
            @FunctionParam(name = "updates", type = "DOCUMENT", description = "Fields to update")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Updated project"),
        examples = {"SERVERLESS_UPDATE_OBSERVABILITY_PROJECT('api-key', 'proj-123', {'name': 'new-name'})"},
        category = FunctionCategory.CLOUD
    )
    public static void registerUpdateObservabilityProject(ExecutionContext context) {
        context.declareFunction("SERVERLESS_UPDATE_OBSERVABILITY_PROJECT",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("project_id", "STRING", ParameterMode.IN),
                new Parameter("updates", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_UPDATE_OBSERVABILITY_PROJECT", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String projectId = args.get(1).toString();
                @SuppressWarnings("unchecked")
                Map<String, Object> updates = (Map<String, Object>) args.get(2);
                executeAsync(() -> apiRequest("PATCH", "/api/v1/serverless/projects/observability/" + projectId, updates, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_DELETE_OBSERVABILITY_PROJECT",
        description = "Delete an Observability serverless project",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "project_id", type = "STRING", description = "Project ID to delete")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Deletion confirmation"),
        examples = {"SERVERLESS_DELETE_OBSERVABILITY_PROJECT('api-key', 'proj-123')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerDeleteObservabilityProject(ExecutionContext context) {
        context.declareFunction("SERVERLESS_DELETE_OBSERVABILITY_PROJECT",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("project_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_DELETE_OBSERVABILITY_PROJECT", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String projectId = args.get(1).toString();
                executeAsync(() -> apiRequest("DELETE", "/api/v1/serverless/projects/observability/" + projectId, null, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_OBSERVABILITY_PROJECT_STATUS",
        description = "Get the status of an Observability serverless project",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "project_id", type = "STRING", description = "Project ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Project status"),
        examples = {"SERVERLESS_OBSERVABILITY_PROJECT_STATUS('api-key', 'proj-123')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerGetObservabilityProjectStatus(ExecutionContext context) {
        context.declareFunction("SERVERLESS_OBSERVABILITY_PROJECT_STATUS",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("project_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_OBSERVABILITY_PROJECT_STATUS", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String projectId = args.get(1).toString();
                executeAsync(() -> apiRequest("GET", "/api/v1/serverless/projects/observability/" + projectId + "/status", null, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_OBSERVABILITY_PROJECT_ROLES",
        description = "Get roles for an Observability serverless project",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "project_id", type = "STRING", description = "Project ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Project roles"),
        examples = {"SERVERLESS_OBSERVABILITY_PROJECT_ROLES('api-key', 'proj-123')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerGetObservabilityProjectRoles(ExecutionContext context) {
        context.declareFunction("SERVERLESS_OBSERVABILITY_PROJECT_ROLES",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("project_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_OBSERVABILITY_PROJECT_ROLES", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String projectId = args.get(1).toString();
                executeAsync(() -> apiRequest("GET", "/api/v1/serverless/projects/observability/" + projectId + "/roles", null, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_RESET_OBSERVABILITY_CREDENTIALS",
        description = "Reset credentials for an Observability serverless project",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "project_id", type = "STRING", description = "Project ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "New credentials"),
        examples = {"SERVERLESS_RESET_OBSERVABILITY_CREDENTIALS('api-key', 'proj-123')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerResetObservabilityProjectCredentials(ExecutionContext context) {
        context.declareFunction("SERVERLESS_RESET_OBSERVABILITY_CREDENTIALS",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("project_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_RESET_OBSERVABILITY_CREDENTIALS", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String projectId = args.get(1).toString();
                executeAsync(() -> apiRequest("POST", "/api/v1/serverless/projects/observability/" + projectId + "/_reset-credentials", null, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_RESUME_OBSERVABILITY_PROJECT",
        description = "Resume a paused Observability serverless project",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "project_id", type = "STRING", description = "Project ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Resume confirmation"),
        examples = {"SERVERLESS_RESUME_OBSERVABILITY_PROJECT('api-key', 'proj-123')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerResumeObservabilityProject(ExecutionContext context) {
        context.declareFunction("SERVERLESS_RESUME_OBSERVABILITY_PROJECT",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("project_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_RESUME_OBSERVABILITY_PROJECT", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String projectId = args.get(1).toString();
                executeAsync(() -> apiRequest("POST", "/api/v1/serverless/projects/observability/" + projectId + "/_resume", null, apiKey), listener);
            })
        );
    }

    // ============================================
    // Security Projects
    // ============================================

    @FunctionSpec(
        name = "SERVERLESS_LIST_SECURITY_PROJECTS",
        description = "List all Security serverless projects",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "List of Security projects"),
        examples = {"SERVERLESS_LIST_SECURITY_PROJECTS('my-api-key')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerListSecurityProjects(ExecutionContext context) {
        context.declareFunction("SERVERLESS_LIST_SECURITY_PROJECTS",
            List.of(new Parameter("api_key", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("SERVERLESS_LIST_SECURITY_PROJECTS", (args, listener) -> {
                String apiKey = args.get(0).toString();
                executeAsync(() -> apiRequest("GET", "/api/v1/serverless/projects/security", null, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_GET_SECURITY_PROJECT",
        description = "Get a Security serverless project by ID",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "project_id", type = "STRING", description = "Project ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Security project details"),
        examples = {"SERVERLESS_GET_SECURITY_PROJECT('api-key', 'project-123')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerGetSecurityProject(ExecutionContext context) {
        context.declareFunction("SERVERLESS_GET_SECURITY_PROJECT",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("project_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_GET_SECURITY_PROJECT", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String projectId = args.get(1).toString();
                executeAsync(() -> apiRequest("GET", "/api/v1/serverless/projects/security/" + projectId, null, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_CREATE_SECURITY_PROJECT",
        description = "Create a new Security serverless project",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "name", type = "STRING", description = "Project name"),
            @FunctionParam(name = "region_id", type = "STRING", description = "Region ID"),
            @FunctionParam(name = "config", type = "DOCUMENT", description = "Optional additional configuration")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Created project with credentials"),
        examples = {"SERVERLESS_CREATE_SECURITY_PROJECT('api-key', 'my-sec-project', 'aws-us-east-1', {})"},
        category = FunctionCategory.CLOUD
    )
    public static void registerCreateSecurityProject(ExecutionContext context) {
        context.declareFunction("SERVERLESS_CREATE_SECURITY_PROJECT",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("name", "STRING", ParameterMode.IN),
                new Parameter("region_id", "STRING", ParameterMode.IN),
                new Parameter("config", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_CREATE_SECURITY_PROJECT", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String name = args.get(1).toString();
                String regionId = args.get(2).toString();
                @SuppressWarnings("unchecked")
                Map<String, Object> config = args.get(3) instanceof Map ? (Map<String, Object>) args.get(3) : new HashMap<>();
                
                Map<String, Object> body = new HashMap<>(config);
                body.put("name", name);
                body.put("region_id", regionId);
                
                executeAsync(() -> apiRequest("POST", "/api/v1/serverless/projects/security", body, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_UPDATE_SECURITY_PROJECT",
        description = "Update a Security serverless project",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "project_id", type = "STRING", description = "Project ID"),
            @FunctionParam(name = "updates", type = "DOCUMENT", description = "Fields to update")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Updated project"),
        examples = {"SERVERLESS_UPDATE_SECURITY_PROJECT('api-key', 'proj-123', {'name': 'new-name'})"},
        category = FunctionCategory.CLOUD
    )
    public static void registerUpdateSecurityProject(ExecutionContext context) {
        context.declareFunction("SERVERLESS_UPDATE_SECURITY_PROJECT",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("project_id", "STRING", ParameterMode.IN),
                new Parameter("updates", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_UPDATE_SECURITY_PROJECT", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String projectId = args.get(1).toString();
                @SuppressWarnings("unchecked")
                Map<String, Object> updates = (Map<String, Object>) args.get(2);
                executeAsync(() -> apiRequest("PATCH", "/api/v1/serverless/projects/security/" + projectId, updates, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_DELETE_SECURITY_PROJECT",
        description = "Delete a Security serverless project",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "project_id", type = "STRING", description = "Project ID to delete")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Deletion confirmation"),
        examples = {"SERVERLESS_DELETE_SECURITY_PROJECT('api-key', 'proj-123')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerDeleteSecurityProject(ExecutionContext context) {
        context.declareFunction("SERVERLESS_DELETE_SECURITY_PROJECT",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("project_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_DELETE_SECURITY_PROJECT", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String projectId = args.get(1).toString();
                executeAsync(() -> apiRequest("DELETE", "/api/v1/serverless/projects/security/" + projectId, null, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_SECURITY_PROJECT_STATUS",
        description = "Get the status of a Security serverless project",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "project_id", type = "STRING", description = "Project ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Project status"),
        examples = {"SERVERLESS_SECURITY_PROJECT_STATUS('api-key', 'proj-123')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerGetSecurityProjectStatus(ExecutionContext context) {
        context.declareFunction("SERVERLESS_SECURITY_PROJECT_STATUS",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("project_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_SECURITY_PROJECT_STATUS", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String projectId = args.get(1).toString();
                executeAsync(() -> apiRequest("GET", "/api/v1/serverless/projects/security/" + projectId + "/status", null, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_SECURITY_PROJECT_ROLES",
        description = "Get roles for a Security serverless project",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "project_id", type = "STRING", description = "Project ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Project roles"),
        examples = {"SERVERLESS_SECURITY_PROJECT_ROLES('api-key', 'proj-123')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerGetSecurityProjectRoles(ExecutionContext context) {
        context.declareFunction("SERVERLESS_SECURITY_PROJECT_ROLES",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("project_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_SECURITY_PROJECT_ROLES", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String projectId = args.get(1).toString();
                executeAsync(() -> apiRequest("GET", "/api/v1/serverless/projects/security/" + projectId + "/roles", null, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_RESET_SECURITY_CREDENTIALS",
        description = "Reset credentials for a Security serverless project",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "project_id", type = "STRING", description = "Project ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "New credentials"),
        examples = {"SERVERLESS_RESET_SECURITY_CREDENTIALS('api-key', 'proj-123')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerResetSecurityProjectCredentials(ExecutionContext context) {
        context.declareFunction("SERVERLESS_RESET_SECURITY_CREDENTIALS",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("project_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_RESET_SECURITY_CREDENTIALS", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String projectId = args.get(1).toString();
                executeAsync(() -> apiRequest("POST", "/api/v1/serverless/projects/security/" + projectId + "/_reset-credentials", null, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_RESUME_SECURITY_PROJECT",
        description = "Resume a paused Security serverless project",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "project_id", type = "STRING", description = "Project ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Resume confirmation"),
        examples = {"SERVERLESS_RESUME_SECURITY_PROJECT('api-key', 'proj-123')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerResumeSecurityProject(ExecutionContext context) {
        context.declareFunction("SERVERLESS_RESUME_SECURITY_PROJECT",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("project_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_RESUME_SECURITY_PROJECT", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String projectId = args.get(1).toString();
                executeAsync(() -> apiRequest("POST", "/api/v1/serverless/projects/security/" + projectId + "/_resume", null, apiKey), listener);
            })
        );
    }

    // ============================================
    // Linked Candidate Projects
    // ============================================

    @FunctionSpec(
        name = "SERVERLESS_ES_LINK_CANDIDATES",
        description = "Get Elasticsearch project link candidates",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "project_id", type = "STRING", description = "Source project ID")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "List of linkable projects"),
        examples = {"SERVERLESS_ES_LINK_CANDIDATES('api-key', 'proj-123')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerGetElasticsearchLinkCandidates(ExecutionContext context) {
        context.declareFunction("SERVERLESS_ES_LINK_CANDIDATES",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("project_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_ES_LINK_CANDIDATES", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String projectId = args.get(1).toString();
                executeAsync(() -> apiRequest("GET", "/api/v1/serverless/projects/elasticsearch/" + projectId + "/link-candidates", null, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_OBSERVABILITY_LINK_CANDIDATES",
        description = "Get Observability project link candidates",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "project_id", type = "STRING", description = "Source project ID")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "List of linkable projects"),
        examples = {"SERVERLESS_OBSERVABILITY_LINK_CANDIDATES('api-key', 'proj-123')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerGetObservabilityLinkCandidates(ExecutionContext context) {
        context.declareFunction("SERVERLESS_OBSERVABILITY_LINK_CANDIDATES",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("project_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_OBSERVABILITY_LINK_CANDIDATES", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String projectId = args.get(1).toString();
                executeAsync(() -> apiRequest("GET", "/api/v1/serverless/projects/observability/" + projectId + "/link-candidates", null, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_SECURITY_LINK_CANDIDATES",
        description = "Get Security project link candidates",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "project_id", type = "STRING", description = "Source project ID")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "List of linkable projects"),
        examples = {"SERVERLESS_SECURITY_LINK_CANDIDATES('api-key', 'proj-123')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerGetSecurityLinkCandidates(ExecutionContext context) {
        context.declareFunction("SERVERLESS_SECURITY_LINK_CANDIDATES",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("project_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_SECURITY_LINK_CANDIDATES", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String projectId = args.get(1).toString();
                executeAsync(() -> apiRequest("GET", "/api/v1/serverless/projects/security/" + projectId + "/link-candidates", null, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_WORKPLACE_AI_LINK_CANDIDATES",
        description = "Get Workplace AI project link candidates",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "project_id", type = "STRING", description = "Source project ID")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "List of linkable projects"),
        examples = {"SERVERLESS_WORKPLACE_AI_LINK_CANDIDATES('api-key', 'proj-123')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerGetWorkplaceAILinkCandidates(ExecutionContext context) {
        context.declareFunction("SERVERLESS_WORKPLACE_AI_LINK_CANDIDATES",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("project_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_WORKPLACE_AI_LINK_CANDIDATES", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String projectId = args.get(1).toString();
                executeAsync(() -> apiRequest("GET", "/api/v1/serverless/projects/workplace-ai/" + projectId + "/link-candidates", null, apiKey), listener);
            })
        );
    }

    // ============================================
    // Regions
    // ============================================

    @FunctionSpec(
        name = "SERVERLESS_LIST_REGIONS",
        description = "List all available regions for serverless projects",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "List of available regions"),
        examples = {"SERVERLESS_LIST_REGIONS('my-api-key')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerListRegions(ExecutionContext context) {
        context.declareFunction("SERVERLESS_LIST_REGIONS",
            List.of(new Parameter("api_key", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("SERVERLESS_LIST_REGIONS", (args, listener) -> {
                String apiKey = args.get(0).toString();
                executeAsync(() -> apiRequest("GET", "/api/v1/serverless/regions", null, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_GET_REGION",
        description = "Get details of a specific region",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "region_id", type = "STRING", description = "Region ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Region details"),
        examples = {"SERVERLESS_GET_REGION('api-key', 'aws-us-east-1')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerGetRegion(ExecutionContext context) {
        context.declareFunction("SERVERLESS_GET_REGION",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("region_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_GET_REGION", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String regionId = args.get(1).toString();
                executeAsync(() -> apiRequest("GET", "/api/v1/serverless/regions/" + regionId, null, apiKey), listener);
            })
        );
    }

    // ============================================
    // Traffic Filters
    // ============================================

    @FunctionSpec(
        name = "SERVERLESS_LIST_TRAFFIC_FILTERS",
        description = "List all traffic filters",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "List of traffic filters"),
        examples = {"SERVERLESS_LIST_TRAFFIC_FILTERS('my-api-key')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerListTrafficFilters(ExecutionContext context) {
        context.declareFunction("SERVERLESS_LIST_TRAFFIC_FILTERS",
            List.of(new Parameter("api_key", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("SERVERLESS_LIST_TRAFFIC_FILTERS", (args, listener) -> {
                String apiKey = args.get(0).toString();
                executeAsync(() -> apiRequest("GET", "/api/v1/serverless/traffic-filters", null, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_GET_TRAFFIC_FILTER",
        description = "Get a traffic filter by ID",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "filter_id", type = "STRING", description = "Traffic filter ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Traffic filter details"),
        examples = {"SERVERLESS_GET_TRAFFIC_FILTER('api-key', 'filter-123')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerGetTrafficFilter(ExecutionContext context) {
        context.declareFunction("SERVERLESS_GET_TRAFFIC_FILTER",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("filter_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_GET_TRAFFIC_FILTER", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String filterId = args.get(1).toString();
                executeAsync(() -> apiRequest("GET", "/api/v1/serverless/traffic-filters/" + filterId, null, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_CREATE_TRAFFIC_FILTER",
        description = "Create a new traffic filter",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "name", type = "STRING", description = "Filter name"),
            @FunctionParam(name = "type", type = "STRING", description = "Filter type (ip, vpce, azure_private_endpoint, gcp_private_service_connect)"),
            @FunctionParam(name = "rules", type = "ARRAY", description = "Array of filter rules"),
            @FunctionParam(name = "region", type = "STRING", description = "Region for the filter")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Created traffic filter"),
        examples = {"SERVERLESS_CREATE_TRAFFIC_FILTER('api-key', 'my-filter', 'ip', [{'source': '192.168.1.0/24'}], 'aws-us-east-1')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerCreateTrafficFilter(ExecutionContext context) {
        context.declareFunction("SERVERLESS_CREATE_TRAFFIC_FILTER",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("name", "STRING", ParameterMode.IN),
                new Parameter("type", "STRING", ParameterMode.IN),
                new Parameter("rules", "ARRAY", ParameterMode.IN),
                new Parameter("region", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_CREATE_TRAFFIC_FILTER", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String name = args.get(1).toString();
                String type = args.get(2).toString();
                @SuppressWarnings("unchecked")
                List<Object> rules = (List<Object>) args.get(3);
                String region = args.get(4).toString();
                
                Map<String, Object> body = new HashMap<>();
                body.put("name", name);
                body.put("type", type);
                body.put("rules", rules);
                body.put("region", region);
                
                executeAsync(() -> apiRequest("POST", "/api/v1/serverless/traffic-filters", body, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_UPDATE_TRAFFIC_FILTER",
        description = "Update a traffic filter",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "filter_id", type = "STRING", description = "Traffic filter ID"),
            @FunctionParam(name = "updates", type = "DOCUMENT", description = "Fields to update")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Updated traffic filter"),
        examples = {"SERVERLESS_UPDATE_TRAFFIC_FILTER('api-key', 'filter-123', {'name': 'updated-name'})"},
        category = FunctionCategory.CLOUD
    )
    public static void registerUpdateTrafficFilter(ExecutionContext context) {
        context.declareFunction("SERVERLESS_UPDATE_TRAFFIC_FILTER",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("filter_id", "STRING", ParameterMode.IN),
                new Parameter("updates", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_UPDATE_TRAFFIC_FILTER", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String filterId = args.get(1).toString();
                @SuppressWarnings("unchecked")
                Map<String, Object> updates = (Map<String, Object>) args.get(2);
                executeAsync(() -> apiRequest("PATCH", "/api/v1/serverless/traffic-filters/" + filterId, updates, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_DELETE_TRAFFIC_FILTER",
        description = "Delete a traffic filter",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key"),
            @FunctionParam(name = "filter_id", type = "STRING", description = "Traffic filter ID to delete")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Deletion confirmation"),
        examples = {"SERVERLESS_DELETE_TRAFFIC_FILTER('api-key', 'filter-123')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerDeleteTrafficFilter(ExecutionContext context) {
        context.declareFunction("SERVERLESS_DELETE_TRAFFIC_FILTER",
            Arrays.asList(
                new Parameter("api_key", "STRING", ParameterMode.IN),
                new Parameter("filter_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SERVERLESS_DELETE_TRAFFIC_FILTER", (args, listener) -> {
                String apiKey = args.get(0).toString();
                String filterId = args.get(1).toString();
                executeAsync(() -> apiRequest("DELETE", "/api/v1/serverless/traffic-filters/" + filterId, null, apiKey), listener);
            })
        );
    }

    @FunctionSpec(
        name = "SERVERLESS_TRAFFIC_FILTER_METADATA",
        description = "Get PrivateLink region metadata for traffic filters",
        parameters = {
            @FunctionParam(name = "api_key", type = "STRING", description = "Elastic Cloud API key")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "PrivateLink metadata"),
        examples = {"SERVERLESS_TRAFFIC_FILTER_METADATA('my-api-key')"},
        category = FunctionCategory.CLOUD
    )
    public static void registerGetTrafficFilterMetadata(ExecutionContext context) {
        context.declareFunction("SERVERLESS_TRAFFIC_FILTER_METADATA",
            List.of(new Parameter("api_key", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("SERVERLESS_TRAFFIC_FILTER_METADATA", (args, listener) -> {
                String apiKey = args.get(0).toString();
                executeAsync(() -> apiRequest("GET", "/api/v1/serverless/traffic-filters/metadata", null, apiKey), listener);
            })
        );
    }

    // ============================================
    // Helper Methods
    // ============================================

    private static void executeAsync(java.util.concurrent.Callable<Object> task, ActionListener<Object> listener) {
        try {
            Object result = task.call();
            listener.onResponse(result);
        } catch (Exception e) {
            listener.onFailure(new RuntimeException("Serverless API error: " + e.getMessage(), e));
        }
    }

    @SuppressWarnings("unchecked")
    private static Object apiRequest(String method, String path, Map<String, Object> body, String apiKey) throws Exception {
        URL url = new URL(BASE_URL + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method.equals("PATCH") ? "POST" : method);
        conn.setRequestProperty("Authorization", "ApiKey " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        
        if (method.equals("PATCH")) {
            conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
        }
        
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);
        
        if (body != null && (method.equals("POST") || method.equals("PUT") || method.equals("PATCH"))) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = mapToJson(body).getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }
        
        int responseCode = conn.getResponseCode();
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                    responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                    StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }
        
        String responseBody = response.toString();
        
        if (responseCode >= 400) {
            throw new RuntimeException("API error " + responseCode + ": " + responseBody);
        }
        
        if (responseBody.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("code", responseCode);
            return result;
        }
        
        return parseJson(responseBody);
    }

    private static String mapToJson(Map<String, Object> map) throws Exception {
        try (XContentBuilder builder = XContentFactory.jsonBuilder()) {
            builder.map(map);
            return Strings.toString(builder);
        }
    }

    private static Object parseJson(String json) throws Exception {
        if (json == null || json.isEmpty()) {
            return new HashMap<>();
        }
        try (XContentParser parser = XContentType.JSON.xContent().createParser(
                XContentParserConfiguration.EMPTY, 
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))) {
            XContentParser.Token token = parser.nextToken();
            if (token == XContentParser.Token.START_ARRAY) {
                return parser.list();
            } else {
                return parser.map();
            }
        }
    }
}
