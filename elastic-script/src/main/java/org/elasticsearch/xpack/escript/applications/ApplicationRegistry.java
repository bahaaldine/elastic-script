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
import org.elasticsearch.xcontent.XContentType;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing application definitions.
 * 
 * Provides storage in Elasticsearch with local caching for fast lookups.
 */
public class ApplicationRegistry {

    private static final Logger LOGGER = LogManager.getLogger(ApplicationRegistry.class);
    public static final String INDEX_NAME = ".escript_applications";

    private final Client client;
    private final ConcurrentHashMap<String, ApplicationDefinition> cache;

    public ApplicationRegistry(Client client) {
        this.client = client;
        this.cache = new ConcurrentHashMap<>();
    }

    /**
     * Stores an application definition.
     */
    public void saveApplication(ApplicationDefinition app, ActionListener<Boolean> listener) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            app.toXContent(builder, ToXContent.EMPTY_PARAMS);

            IndexRequest request = new IndexRequest(INDEX_NAME)
                .id(app.getName())
                .source(builder)
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

            client.index(request, ActionListener.wrap(
                response -> {
                    boolean success = response.getResult() == DocWriteResponse.Result.CREATED
                        || response.getResult() == DocWriteResponse.Result.UPDATED;
                    if (success) {
                        cache.put(app.getName(), app);
                        LOGGER.debug("Saved application: {}", app.getName());
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
     * Gets an application by name.
     */
    public void getApplication(String name, ActionListener<Optional<ApplicationDefinition>> listener) {
        // Check cache first
        ApplicationDefinition cached = cache.get(name);
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
                        ApplicationDefinition app = parseFromSource(response.getSourceAsMap());
                        cache.put(name, app);
                        listener.onResponse(Optional.of(app));
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
     * Lists all applications.
     */
    public void listApplications(ActionListener<List<ApplicationDefinition>> listener) {
        SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchAllQuery());
        sourceBuilder.size(1000);
        searchRequest.source(sourceBuilder);

        client.search(searchRequest, ActionListener.wrap(
            searchResponse -> {
                List<ApplicationDefinition> results = new ArrayList<>();
                for (SearchHit hit : searchResponse.getHits().getHits()) {
                    try {
                        ApplicationDefinition app = parseFromSource(hit.getSourceAsMap());
                        results.add(app);
                        cache.put(app.getName(), app);
                    } catch (Exception e) {
                        LOGGER.warn("Failed to parse application from hit: {}", hit.getId(), e);
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
     * Deletes an application.
     */
    public void deleteApplication(String name, ActionListener<Boolean> listener) {
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
     * Updates application status.
     */
    public void updateStatus(String name, ApplicationDefinition.Status newStatus, ActionListener<Boolean> listener) {
        getApplication(name, ActionListener.wrap(
            optApp -> {
                if (optApp.isEmpty()) {
                    listener.onFailure(new IllegalArgumentException("Application not found: " + name));
                    return;
                }
                ApplicationDefinition updated = optApp.get().toBuilder()
                    .status(newStatus)
                    .updatedAt(Instant.now())
                    .build();
                saveApplication(updated, listener);
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
     * Gets all skills from all installed applications.
     * Useful for MCP server registration.
     */
    public void getAllSkills(ActionListener<List<SkillDefinition>> listener) {
        listApplications(ActionListener.wrap(
            apps -> {
                List<SkillDefinition> allSkills = new ArrayList<>();
                for (ApplicationDefinition app : apps) {
                    if (app.getStatus() == ApplicationDefinition.Status.RUNNING) {
                        allSkills.addAll(app.getSkills());
                    }
                }
                listener.onResponse(allSkills);
            },
            listener::onFailure
        ));
    }

    /**
     * Matches user input against all intents in running applications.
     */
    public void matchIntent(String userInput, ActionListener<ApplicationDefinition.IntentMatch> listener) {
        listApplications(ActionListener.wrap(
            apps -> {
                ApplicationDefinition.IntentMatch bestMatch = null;
                for (ApplicationDefinition app : apps) {
                    if (app.getStatus() == ApplicationDefinition.Status.RUNNING) {
                        ApplicationDefinition.IntentMatch match = app.matchIntent(userInput);
                        if (match != null && (bestMatch == null || match.getConfidence() > bestMatch.getConfidence())) {
                            bestMatch = match;
                        }
                    }
                }
                listener.onResponse(bestMatch);
            },
            listener::onFailure
        ));
    }

    @SuppressWarnings("unchecked")
    private ApplicationDefinition parseFromSource(Map<String, Object> source) {
        ApplicationDefinition.Builder builder = ApplicationDefinition.builder();
        
        builder.name((String) source.get("name"));
        builder.description((String) source.get("description"));
        builder.version((String) source.get("version"));
        
        String statusStr = (String) source.get("status");
        if (statusStr != null) {
            builder.status(ApplicationDefinition.Status.valueOf(statusStr));
        }
        
        // Parse sources
        List<Map<String, Object>> sourcesData = (List<Map<String, Object>>) source.get("sources");
        if (sourcesData != null) {
            List<ApplicationDefinition.SourceDefinition> sources = new ArrayList<>();
            for (Map<String, Object> s : sourcesData) {
                sources.add(new ApplicationDefinition.SourceDefinition(
                    (String) s.get("name"),
                    (String) s.get("index_pattern")
                ));
            }
            builder.sources(sources);
        }
        
        // Parse skills
        List<Map<String, Object>> skillsData = (List<Map<String, Object>>) source.get("skills");
        if (skillsData != null) {
            List<SkillDefinition> skills = new ArrayList<>();
            for (Map<String, Object> s : skillsData) {
                List<SkillDefinition.SkillParameter> params = new ArrayList<>();
                List<Map<String, Object>> paramsData = (List<Map<String, Object>>) s.get("parameters");
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
                skills.add(new SkillDefinition(
                    (String) s.get("name"),
                    (String) s.get("description"),
                    params,
                    (String) s.get("return_type"),
                    (String) s.get("procedure"),
                    (List<String>) s.get("procedure_args")
                ));
            }
            builder.skills(skills);
        }
        
        // Parse intents
        List<Map<String, Object>> intentsData = (List<Map<String, Object>>) source.get("intents");
        if (intentsData != null) {
            List<IntentDefinition> intents = new ArrayList<>();
            for (Map<String, Object> i : intentsData) {
                intents.add(new IntentDefinition(
                    (String) i.get("pattern"),
                    (String) i.get("target_skill"),
                    (String) i.get("description"),
                    null  // Parameter mappings parsed separately if needed
                ));
            }
            builder.intents(intents);
        }
        
        // Parse job and trigger names
        builder.jobNames((List<String>) source.get("jobs"));
        builder.triggerNames((List<String>) source.get("triggers"));
        builder.config((Map<String, Object>) source.get("config"));
        
        String createdAt = (String) source.get("created_at");
        if (createdAt != null) {
            builder.createdAt(Instant.parse(createdAt));
        }
        
        String updatedAt = (String) source.get("updated_at");
        if (updatedAt != null) {
            builder.updatedAt(Instant.parse(updatedAt));
        }
        
        builder.createdBy((String) source.get("created_by"));
        
        return builder.build();
    }
}
