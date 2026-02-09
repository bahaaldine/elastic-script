/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.applications;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.ToXContent;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing standalone skill definitions.
 * 
 * Standalone skills are first-class objects that can be:
 * - Created independently of applications
 * - Referenced by multiple applications
 * - Discovered and used by AI agents directly
 * 
 * Skills are stored in the .escript_skills index.
 */
public class SkillRegistry {

    private static final Logger LOGGER = LogManager.getLogger(SkillRegistry.class);
    public static final String INDEX_NAME = ".escript_skills";

    private final Client client;
    private final ConcurrentHashMap<String, SkillDefinition> cache;

    public SkillRegistry(Client client) {
        this.client = client;
        this.cache = new ConcurrentHashMap<>();
    }

    /**
     * Stores a skill definition.
     */
    public void saveSkill(SkillDefinition skill, ActionListener<Boolean> listener) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            skill.toXContent(builder, ToXContent.EMPTY_PARAMS);

            IndexRequest request = new IndexRequest(INDEX_NAME)
                .id(skill.getName())
                .source(builder)
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

            client.index(request, ActionListener.wrap(
                response -> {
                    boolean success = response.getResult() == DocWriteResponse.Result.CREATED
                        || response.getResult() == DocWriteResponse.Result.UPDATED;
                    if (success) {
                        cache.put(skill.getName(), skill);
                        LOGGER.debug("Saved skill: {}", skill.getName());
                    }
                    listener.onResponse(success);
                },
                listener::onFailure
            ));
        } catch (IOException e) {
            listener.onFailure(e);
        }
    }

    /**
     * Gets a skill by name.
     */
    public void getSkill(String name, ActionListener<Optional<SkillDefinition>> listener) {
        // Check cache first
        SkillDefinition cached = cache.get(name);
        if (cached != null) {
            listener.onResponse(Optional.of(cached));
            return;
        }

        // Fetch from Elasticsearch
        GetRequest request = new GetRequest(INDEX_NAME, name);
        client.get(request, ActionListener.wrap(
            response -> {
                if (response.isExists()) {
                    try {
                        SkillDefinition skill = parseFromSource(response.getSourceAsMap());
                        cache.put(name, skill);
                        listener.onResponse(Optional.of(skill));
                    } catch (Exception e) {
                        listener.onFailure(e);
                    }
                } else {
                    listener.onResponse(Optional.empty());
                }
            },
            e -> {
                if (e.getMessage() != null && e.getMessage().contains("index_not_found")) {
                    listener.onResponse(Optional.empty());
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }

    /**
     * Lists all standalone skills.
     */
    public void listSkills(ActionListener<List<SkillDefinition>> listener) {
        SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchAllQuery());
        sourceBuilder.size(1000);
        searchRequest.source(sourceBuilder);

        client.search(searchRequest, ActionListener.wrap(
            searchResponse -> {
                List<SkillDefinition> results = new ArrayList<>();
                for (SearchHit hit : searchResponse.getHits().getHits()) {
                    try {
                        SkillDefinition skill = parseFromSource(hit.getSourceAsMap());
                        results.add(skill);
                        cache.put(skill.getName(), skill);
                    } catch (Exception e) {
                        LOGGER.warn("Failed to parse skill from hit: {}", hit.getId(), e);
                    }
                }
                listener.onResponse(results);
            },
            e -> {
                if (e.getMessage() != null && e.getMessage().contains("index_not_found")) {
                    listener.onResponse(List.of());
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }

    /**
     * Deletes a skill.
     */
    public void deleteSkill(String name, ActionListener<Boolean> listener) {
        DeleteRequest request = new DeleteRequest(INDEX_NAME, name)
            .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

        client.delete(request, ActionListener.wrap(
            response -> {
                cache.remove(name);
                listener.onResponse(response.getResult() == DocWriteResponse.Result.DELETED);
            },
            listener::onFailure
        ));
    }

    /**
     * Updates a skill's description.
     */
    public void updateDescription(String name, String newDescription, ActionListener<Boolean> listener) {
        getSkill(name, ActionListener.wrap(
            optSkill -> {
                if (optSkill.isEmpty()) {
                    listener.onFailure(new IllegalArgumentException("Skill not found: " + name));
                    return;
                }
                SkillDefinition existing = optSkill.get();
                SkillDefinition updated = new SkillDefinition(
                    existing.getName(),
                    newDescription,
                    existing.getParameters(),
                    existing.getReturnType(),
                    existing.getProcedureName(),
                    existing.getProcedureArgs(),
                    existing.getExamples()
                );
                saveSkill(updated, listener);
            },
            listener::onFailure
        ));
    }

    /**
     * Clears the local cache.
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * Searches skills by keyword in name, description, or examples.
     */
    public void searchSkills(String keyword, ActionListener<List<SkillDefinition>> listener) {
        SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.multiMatchQuery(keyword, "name", "description", "examples"));
        sourceBuilder.size(100);
        searchRequest.source(sourceBuilder);

        client.search(searchRequest, ActionListener.wrap(
            searchResponse -> {
                List<SkillDefinition> results = new ArrayList<>();
                for (SearchHit hit : searchResponse.getHits().getHits()) {
                    try {
                        SkillDefinition skill = parseFromSource(hit.getSourceAsMap());
                        results.add(skill);
                    } catch (Exception e) {
                        LOGGER.warn("Failed to parse skill from hit: {}", hit.getId(), e);
                    }
                }
                listener.onResponse(results);
            },
            e -> {
                if (e.getMessage() != null && e.getMessage().contains("index_not_found")) {
                    listener.onResponse(List.of());
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }

    @SuppressWarnings("unchecked")
    private SkillDefinition parseFromSource(Map<String, Object> source) {
        String name = (String) source.get("name");
        String description = (String) source.get("description");
        String returnType = (String) source.get("return_type");
        String procedureName = (String) source.get("procedure");
        List<String> procedureArgs = (List<String>) source.get("procedure_args");
        List<String> examples = (List<String>) source.get("examples");

        List<SkillDefinition.SkillParameter> params = new ArrayList<>();
        List<Map<String, Object>> paramsData = (List<Map<String, Object>>) source.get("parameters");
        if (paramsData != null) {
            for (Map<String, Object> p : paramsData) {
                params.add(new SkillDefinition.SkillParameter(
                    (String) p.get("name"),
                    (String) p.get("type"),
                    (String) p.get("description"),
                    p.get("required") != null ? (Boolean) p.get("required") : true,
                    p.get("default")
                ));
            }
        }

        return new SkillDefinition(
            name,
            description,
            params,
            returnType,
            procedureName,
            procedureArgs != null ? procedureArgs : List.of(),
            examples != null ? examples : List.of()
        );
    }
}
