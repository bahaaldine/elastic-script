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
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;
import org.elasticsearch.xpack.escript.types.TypeDefinition;
import org.elasticsearch.xpack.escript.types.TypeDefinition.TypeField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for type statements: CREATE TYPE, DROP TYPE, SHOW TYPES.
 */
public class TypeStatementHandler {

    private static final Logger LOGGER = LogManager.getLogger(TypeStatementHandler.class);
    private static final String INDEX_NAME = TypeDefinition.INDEX_NAME;

    private final Client client;

    public TypeStatementHandler(Client client) {
        this.client = client;
    }

    /**
     * Handle CREATE TYPE statement.
     */
    public void handleCreateType(ElasticScriptParser.Create_type_statementContext ctx,
                                ActionListener<Object> listener) {
        String typeName = ctx.ID().getText();

        // Parse fields
        List<TypeField> fields = new ArrayList<>();
        for (ElasticScriptParser.Type_fieldContext fieldCtx : ctx.type_field_list().type_field()) {
            String fieldName = fieldCtx.ID().getText();
            String fieldType = fieldCtx.datatype().getText();
            fields.add(new TypeField(fieldName, fieldType));
        }

        // Build type definition
        TypeDefinition typeDef = TypeDefinition.builder()
            .name(typeName)
            .fields(fields)
            .build();

        LOGGER.debug("Creating type: {} with {} fields", typeName, fields.size());

        // Store the type
        ensureIndexExists(ActionListener.wrap(
            indexExists -> storeType(typeDef, listener),
            listener::onFailure
        ));
    }

    /**
     * Handle DROP TYPE statement.
     */
    public void handleDropType(ElasticScriptParser.Drop_type_statementContext ctx,
                              ActionListener<Object> listener) {
        String typeName = ctx.ID().getText();

        DeleteRequest request = new DeleteRequest(INDEX_NAME, typeName);
        client.delete(request, ActionListener.wrap(
            response -> {
                Map<String, Object> result = new HashMap<>();
                result.put("acknowledged", true);
                result.put("type", typeName);
                result.put("action", "dropped");
                listener.onResponse(result);
            },
            listener::onFailure
        ));
    }

    /**
     * Handle SHOW TYPES statement.
     */
    public void handleShowAllTypes(ActionListener<Object> listener) {
        SearchRequest request = new SearchRequest(INDEX_NAME);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchAllQuery());
        sourceBuilder.size(1000);
        request.source(sourceBuilder);

        client.search(request, ActionListener.wrap(
            response -> {
                List<Map<String, Object>> types = new ArrayList<>();
                for (SearchHit hit : response.getHits().getHits()) {
                    types.add(hit.getSourceAsMap());
                }
                listener.onResponse(types);
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
     * Handle SHOW TYPE name statement.
     */
    public void handleShowTypeDetail(ElasticScriptParser.ShowTypeDetailContext ctx,
                                    ActionListener<Object> listener) {
        String typeName = ctx.ID().getText();

        GetRequest request = new GetRequest(INDEX_NAME, typeName);
        client.get(request, ActionListener.wrap(
            response -> {
                if (!response.isExists()) {
                    listener.onFailure(new IllegalArgumentException("Type not found: " + typeName));
                    return;
                }
                listener.onResponse(response.getSourceAsMap());
            },
            e -> {
                if (e instanceof IndexNotFoundException) {
                    listener.onFailure(new IllegalArgumentException("Type not found: " + typeName));
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }

    // ==================== Helper Methods ====================

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

    private void storeType(TypeDefinition typeDef, ActionListener<Object> listener) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            typeDef.toXContent(builder, null);

            IndexRequest request = new IndexRequest(INDEX_NAME)
                .id(typeDef.getName())
                .source(builder);

            client.index(request, ActionListener.wrap(
                response -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("acknowledged", true);
                    result.put("type", typeDef.getName());
                    result.put("fields", typeDef.getFieldNames());
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
