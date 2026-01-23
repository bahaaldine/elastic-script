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
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xpack.escript.packages.PackageDefinition;
import org.elasticsearch.xpack.escript.packages.PackageDefinition.ProcedureSpec;
import org.elasticsearch.xpack.escript.packages.PackageDefinition.FunctionSpec;
import org.elasticsearch.xpack.escript.packages.PackageDefinition.VariableSpec;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for package statements: CREATE PACKAGE, CREATE PACKAGE BODY, DROP PACKAGE, SHOW PACKAGE.
 */
public class PackageStatementHandler {

    private static final Logger LOGGER = LogManager.getLogger(PackageStatementHandler.class);
    private static final String INDEX_NAME = PackageDefinition.INDEX_NAME;

    private final Client client;

    public PackageStatementHandler(Client client) {
        this.client = client;
    }

    /**
     * Handle CREATE PACKAGE statement.
     */
    public void handleCreatePackage(ElasticScriptParser.Create_package_statementContext ctx,
                                    String rawSpecificationText,
                                    ActionListener<Object> listener) {
        String packageName = ctx.ID().getText();

        // Parse package spec items
        List<ProcedureSpec> procedures = new ArrayList<>();
        List<FunctionSpec> functions = new ArrayList<>();
        List<VariableSpec> variables = new ArrayList<>();

        for (ElasticScriptParser.Package_spec_itemContext itemCtx : ctx.package_spec_item()) {
            if (itemCtx.package_procedure_spec() != null) {
                procedures.add(parseProcedureSpec(itemCtx.package_procedure_spec()));
            } else if (itemCtx.package_function_spec() != null) {
                functions.add(parseFunctionSpec(itemCtx.package_function_spec()));
            } else if (itemCtx.package_variable_spec() != null) {
                variables.add(parseVariableSpec(itemCtx.package_variable_spec()));
            }
        }

        // Build package definition
        PackageDefinition pkg = PackageDefinition.builder()
            .name(packageName)
            .procedures(procedures)
            .functions(functions)
            .variables(variables)
            .specificationText(rawSpecificationText)
            .build();

        LOGGER.debug("Creating package: {}", pkg);

        // Store the package
        ensureIndexExists(ActionListener.wrap(
            indexExists -> storePackage(pkg, listener),
            listener::onFailure
        ));
    }

    /**
     * Handle CREATE PACKAGE BODY statement.
     */
    public void handleCreatePackageBody(ElasticScriptParser.Create_package_body_statementContext ctx,
                                        String rawBodyText,
                                        ActionListener<Object> listener) {
        String packageName = ctx.ID().getText();

        // Get the existing package
        getPackage(packageName, ActionListener.wrap(
            existingPkg -> {
                if (existingPkg == null) {
                    listener.onFailure(new IllegalArgumentException(
                        "Package '" + packageName + "' does not exist. Create the package specification first."));
                    return;
                }

                // Update with body
                PackageDefinition updatedPkg = existingPkg.withBody(rawBodyText);
                storePackage(updatedPkg, listener);
            },
            listener::onFailure
        ));
    }

    /**
     * Handle DROP PACKAGE statement.
     */
    public void handleDropPackage(ElasticScriptParser.Drop_package_statementContext ctx,
                                  ActionListener<Object> listener) {
        String packageName = ctx.ID().getText();

        DeleteRequest request = new DeleteRequest(INDEX_NAME, packageName);
        client.delete(request, ActionListener.wrap(
            response -> {
                Map<String, Object> result = new HashMap<>();
                result.put("acknowledged", true);
                result.put("package", packageName);
                result.put("action", "dropped");
                listener.onResponse(result);
            },
            listener::onFailure
        ));
    }

    /**
     * Handle SHOW PACKAGE name statement.
     */
    public void handleShowPackage(ElasticScriptParser.ShowPackageDetailContext ctx,
                                  ActionListener<Object> listener) {
        String packageName = ctx.ID().getText();

        getPackage(packageName, ActionListener.wrap(
            pkg -> {
                if (pkg == null) {
                    listener.onFailure(new IllegalArgumentException("Package not found: " + packageName));
                } else {
                    listener.onResponse(packageToMap(pkg));
                }
            },
            listener::onFailure
        ));
    }

    // ==================== Helper Methods ====================

    private ProcedureSpec parseProcedureSpec(ElasticScriptParser.Package_procedure_specContext ctx) {
        String name = ctx.ID().getText();
        boolean isPublic = ctx.PRIVATE() == null;  // Default is public
        List<String> parameters = new ArrayList<>();

        if (ctx.parameter_list() != null) {
            for (ElasticScriptParser.ParameterContext paramCtx : ctx.parameter_list().parameter()) {
                parameters.add(paramCtx.getText());
            }
        }

        return new ProcedureSpec(name, isPublic, parameters);
    }

    private FunctionSpec parseFunctionSpec(ElasticScriptParser.Package_function_specContext ctx) {
        String name = ctx.ID().getText();
        boolean isPublic = ctx.PRIVATE() == null;  // Default is public
        List<String> parameters = new ArrayList<>();
        String returnType = ctx.datatype().getText();

        if (ctx.parameter_list() != null) {
            for (ElasticScriptParser.ParameterContext paramCtx : ctx.parameter_list().parameter()) {
                parameters.add(paramCtx.getText());
            }
        }

        return new FunctionSpec(name, isPublic, parameters, returnType);
    }

    private VariableSpec parseVariableSpec(ElasticScriptParser.Package_variable_specContext ctx) {
        String name = ctx.ID().getText();
        boolean isPublic = ctx.PRIVATE() == null;  // Default is public
        String type = ctx.datatype().getText();
        String defaultValue = null;

        if (ctx.expression() != null) {
            defaultValue = ctx.expression().getText();
        }

        return new VariableSpec(name, isPublic, type, defaultValue);
    }

    private void ensureIndexExists(ActionListener<Boolean> listener) {
        client.admin().indices().prepareGetIndex(TimeValue.timeValueSeconds(30)).setIndices(INDEX_NAME).execute(ActionListener.wrap(
            response -> listener.onResponse(true),
            e -> {
                if (e instanceof IndexNotFoundException) {
                    CreateIndexRequest createRequest = new CreateIndexRequest(INDEX_NAME);
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

    private void storePackage(PackageDefinition pkg, ActionListener<Object> listener) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            pkg.toXContent(builder, null);

            IndexRequest request = new IndexRequest(INDEX_NAME)
                .id(pkg.getName())
                .source(builder);

            client.index(request, ActionListener.wrap(
                response -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("acknowledged", true);
                    result.put("package", pkg.getName());
                    result.put("procedures", pkg.getProcedures().size());
                    result.put("functions", pkg.getFunctions().size());
                    result.put("variables", pkg.getVariables().size());
                    result.put("has_body", pkg.hasBody());
                    listener.onResponse(result);
                },
                listener::onFailure
            ));
        } catch (IOException e) {
            listener.onFailure(e);
        }
    }

    private void getPackage(String packageName, ActionListener<PackageDefinition> listener) {
        GetRequest request = new GetRequest(INDEX_NAME, packageName);
        client.get(request, ActionListener.wrap(
            response -> {
                if (!response.isExists()) {
                    listener.onResponse(null);
                    return;
                }
                try {
                    XContentParser parser = XContentHelper.createParser(
                        null, null, response.getSourceAsBytesRef(), XContentType.JSON);
                    PackageDefinition pkg = PackageDefinition.fromXContent(parser);
                    listener.onResponse(pkg);
                } catch (IOException e) {
                    listener.onFailure(e);
                }
            },
            e -> {
                if (e instanceof IndexNotFoundException) {
                    listener.onResponse(null);
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }

    private Map<String, Object> packageToMap(PackageDefinition pkg) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", pkg.getName());
        map.put("has_body", pkg.hasBody());
        map.put("created_at", pkg.getCreatedAt().toString());
        map.put("updated_at", pkg.getUpdatedAt().toString());

        // Procedures
        List<Map<String, Object>> procs = new ArrayList<>();
        for (ProcedureSpec proc : pkg.getProcedures()) {
            Map<String, Object> p = new HashMap<>();
            p.put("name", proc.name);
            p.put("is_public", proc.isPublic);
            p.put("parameters", proc.parameters);
            procs.add(p);
        }
        map.put("procedures", procs);

        // Functions
        List<Map<String, Object>> funcs = new ArrayList<>();
        for (FunctionSpec func : pkg.getFunctions()) {
            Map<String, Object> f = new HashMap<>();
            f.put("name", func.name);
            f.put("is_public", func.isPublic);
            f.put("parameters", func.parameters);
            f.put("return_type", func.returnType);
            funcs.add(f);
        }
        map.put("functions", funcs);

        // Variables
        List<Map<String, Object>> vars = new ArrayList<>();
        for (VariableSpec var : pkg.getVariables()) {
            Map<String, Object> v = new HashMap<>();
            v.put("name", var.name);
            v.put("is_public", var.isPublic);
            v.put("type", var.type);
            v.put("default_value", var.defaultValue);
            vars.add(v);
        }
        map.put("variables", vars);

        return map;
    }
}
