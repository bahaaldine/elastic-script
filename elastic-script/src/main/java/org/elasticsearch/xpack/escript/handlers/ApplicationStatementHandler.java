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
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.xpack.escript.applications.ApplicationDefinition;
import org.elasticsearch.xpack.escript.applications.ApplicationRegistry;
import org.elasticsearch.xpack.escript.applications.IntentDefinition;
import org.elasticsearch.xpack.escript.applications.SkillDefinition;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for application statements:
 * CREATE APPLICATION, INSTALL APPLICATION, DROP APPLICATION, ALTER APPLICATION,
 * SHOW APPLICATIONS, EXTEND APPLICATION, APPLICATION | STATUS/PAUSE/RESUME.
 */
public class ApplicationStatementHandler {

    private static final Logger LOGGER = LogManager.getLogger(ApplicationStatementHandler.class);

    private final Client client;
    private final ApplicationRegistry registry;

    public ApplicationStatementHandler(Client client) {
        this.client = client;
        this.registry = new ApplicationRegistry(client);
    }

    /**
     * Handle CREATE APPLICATION statement.
     */
    public void handleCreateApplication(ElasticScriptParser.Create_application_statementContext ctx,
                                        ActionListener<Object> listener) {
        String appName = ctx.ID().getText();
        
        final String description;
        final String version;
        
        // Parse description and version from STRING tokens
        String descTemp = null;
        String versionTemp = "1.0.0";
        
        if (ctx.STRING() != null && !ctx.STRING().isEmpty()) {
            int stringIndex = 0;
            if (ctx.DESCRIPTION() != null) {
                descTemp = stripQuotes(ctx.STRING(stringIndex).getText());
                stringIndex++;
            }
            if (ctx.VERSION() != null && ctx.STRING().size() > stringIndex) {
                versionTemp = stripQuotes(ctx.STRING(stringIndex).getText());
            }
        }
        
        description = descTemp;
        version = versionTemp;

        // Parse sections
        List<ApplicationDefinition.SourceDefinition> sources = new ArrayList<>();
        List<SkillDefinition> skills = new ArrayList<>();
        List<IntentDefinition> intents = new ArrayList<>();
        List<String> jobNames = new ArrayList<>();
        List<String> triggerNames = new ArrayList<>();

        for (ElasticScriptParser.Application_sectionContext sectionCtx : ctx.application_section()) {
            if (sectionCtx.sources_section() != null) {
                sources.addAll(parseSources(sectionCtx.sources_section()));
            } else if (sectionCtx.skills_section() != null) {
                skills.addAll(parseSkills(sectionCtx.skills_section()));
            } else if (sectionCtx.intents_section() != null) {
                intents.addAll(parseIntents(sectionCtx.intents_section()));
            } else if (sectionCtx.jobs_section() != null) {
                jobNames.addAll(parseJobs(sectionCtx.jobs_section()));
            } else if (sectionCtx.triggers_section() != null) {
                triggerNames.addAll(parseTriggers(sectionCtx.triggers_section()));
            }
        }

        ApplicationDefinition app = ApplicationDefinition.builder()
            .name(appName)
            .description(description)
            .version(version)
            .sources(sources)
            .skills(skills)
            .intents(intents)
            .jobNames(jobNames)
            .triggerNames(triggerNames)
            .status(ApplicationDefinition.Status.INSTALLED)
            .createdAt(Instant.now())
            .build();

        LOGGER.debug("Creating application: {} with {} skills, {} intents", 
            appName, skills.size(), intents.size());

        ensureIndexExists(ActionListener.wrap(
            indexExists -> registry.saveApplication(app, ActionListener.wrap(
                success -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("acknowledged", true);
                    result.put("application", appName);
                    result.put("version", version);
                    result.put("skills_count", skills.size());
                    result.put("intents_count", intents.size());
                    result.put("sources_count", sources.size());
                    result.put("status", "INSTALLED");
                    listener.onResponse(result);
                },
                listener::onFailure
            )),
            listener::onFailure
        ));
    }

    /**
     * Handle INSTALL APPLICATION statement.
     */
    public void handleInstallApplication(ElasticScriptParser.Install_application_statementContext ctx,
                                         ActionListener<Object> listener) {
        String appName;
        if (ctx.STRING() != null) {
            appName = stripQuotes(ctx.STRING().getText());
        } else {
            appName = ctx.ID().getText();
        }

        // Parse config items
        Map<String, Object> config = new HashMap<>();
        if (ctx.config_item() != null) {
            for (ElasticScriptParser.Config_itemContext configCtx : ctx.config_item()) {
                String key = configCtx.ID().getText();
                String valueText = configCtx.expression().getText();
                // Simple value extraction - could be enhanced
                Object value = parseConfigValue(valueText);
                config.put(key, value);
            }
        }

        // Get existing application and update with config
        registry.getApplication(appName, ActionListener.wrap(
            optApp -> {
                if (optApp.isEmpty()) {
                    listener.onFailure(new IllegalArgumentException("Application not found: " + appName));
                    return;
                }

                ApplicationDefinition app = optApp.get();
                ApplicationDefinition installed = app.toBuilder()
                    .config(config)
                    .status(ApplicationDefinition.Status.RUNNING)
                    .updatedAt(Instant.now())
                    .build();

                registry.saveApplication(installed, ActionListener.wrap(
                    success -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("acknowledged", true);
                        result.put("application", appName);
                        result.put("status", "RUNNING");
                        result.put("config", config);
                        listener.onResponse(result);
                    },
                    listener::onFailure
                ));
            },
            listener::onFailure
        ));
    }

    /**
     * Handle DROP APPLICATION statement.
     */
    public void handleDropApplication(ElasticScriptParser.Drop_application_statementContext ctx,
                                      ActionListener<Object> listener) {
        String appName = ctx.ID().getText();

        registry.deleteApplication(appName, ActionListener.wrap(
            deleted -> {
                Map<String, Object> result = new HashMap<>();
                result.put("acknowledged", true);
                result.put("application", appName);
                result.put("action", deleted ? "dropped" : "not_found");
                listener.onResponse(result);
            },
            listener::onFailure
        ));
    }

    /**
     * Handle ALTER APPLICATION SET statement.
     */
    public void handleAlterApplicationConfig(ElasticScriptParser.AlterApplicationConfigContext ctx,
                                             ActionListener<Object> listener) {
        String appName = ctx.ID().getText();

        // Parse new config items
        Map<String, Object> newConfig = new HashMap<>();
        for (ElasticScriptParser.Config_itemContext configCtx : ctx.config_item()) {
            String key = configCtx.ID().getText();
            Object value = parseConfigValue(configCtx.expression().getText());
            newConfig.put(key, value);
        }

        registry.getApplication(appName, ActionListener.wrap(
            optApp -> {
                if (optApp.isEmpty()) {
                    listener.onFailure(new IllegalArgumentException("Application not found: " + appName));
                    return;
                }

                ApplicationDefinition app = optApp.get();
                Map<String, Object> mergedConfig = new HashMap<>(app.getConfig());
                mergedConfig.putAll(newConfig);

                ApplicationDefinition updated = app.toBuilder()
                    .config(mergedConfig)
                    .updatedAt(Instant.now())
                    .build();

                registry.saveApplication(updated, ActionListener.wrap(
                    success -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("acknowledged", true);
                        result.put("application", appName);
                        result.put("config", mergedConfig);
                        listener.onResponse(result);
                    },
                    listener::onFailure
                ));
            },
            listener::onFailure
        ));
    }

    /**
     * Handle ALTER APPLICATION ENABLE/DISABLE statement.
     */
    public void handleAlterApplicationEnableDisable(ElasticScriptParser.AlterApplicationEnableDisableContext ctx,
                                                    ActionListener<Object> listener) {
        String appName = ctx.ID().getText();
        boolean enable = ctx.ENABLE() != null;

        ApplicationDefinition.Status newStatus = enable 
            ? ApplicationDefinition.Status.RUNNING 
            : ApplicationDefinition.Status.PAUSED;

        registry.updateStatus(appName, newStatus, ActionListener.wrap(
            success -> {
                Map<String, Object> result = new HashMap<>();
                result.put("acknowledged", true);
                result.put("application", appName);
                result.put("status", newStatus.name());
                listener.onResponse(result);
            },
            listener::onFailure
        ));
    }

    /**
     * Handle SHOW APPLICATIONS statement.
     */
    public void handleShowAllApplications(ActionListener<Object> listener) {
        registry.listApplications(ActionListener.wrap(
            apps -> {
                List<Map<String, Object>> results = new ArrayList<>();
                for (ApplicationDefinition app : apps) {
                    Map<String, Object> appMap = new HashMap<>();
                    appMap.put("name", app.getName());
                    appMap.put("description", app.getDescription());
                    appMap.put("version", app.getVersion());
                    appMap.put("status", app.getStatus().name());
                    appMap.put("skills_count", app.getSkills().size());
                    appMap.put("intents_count", app.getIntents().size());
                    results.add(appMap);
                }
                listener.onResponse(results);
            },
            listener::onFailure
        ));
    }

    /**
     * Handle SHOW APPLICATION name statement.
     */
    public void handleShowApplicationDetail(ElasticScriptParser.ShowApplicationDetailContext ctx,
                                           ActionListener<Object> listener) {
        String appName = ctx.ID().getText();

        registry.getApplication(appName, ActionListener.wrap(
            optApp -> {
                if (optApp.isEmpty()) {
                    listener.onFailure(new IllegalArgumentException("Application not found: " + appName));
                    return;
                }

                ApplicationDefinition app = optApp.get();
                Map<String, Object> result = applicationToMap(app);
                listener.onResponse(result);
            },
            listener::onFailure
        ));
    }

    /**
     * Handle SHOW APPLICATION name SKILLS statement.
     */
    public void handleShowApplicationSkills(ElasticScriptParser.ShowApplicationSkillsContext ctx,
                                           ActionListener<Object> listener) {
        String appName = ctx.ID().getText();

        registry.getApplication(appName, ActionListener.wrap(
            optApp -> {
                if (optApp.isEmpty()) {
                    listener.onFailure(new IllegalArgumentException("Application not found: " + appName));
                    return;
                }

                List<Map<String, Object>> skills = new ArrayList<>();
                for (SkillDefinition skill : optApp.get().getSkills()) {
                    Map<String, Object> skillMap = new HashMap<>();
                    skillMap.put("name", skill.getName());
                    skillMap.put("description", skill.getDescription());
                    skillMap.put("return_type", skill.getReturnType());
                    skillMap.put("procedure", skill.getProcedureName());
                    
                    List<Map<String, Object>> params = new ArrayList<>();
                    for (SkillDefinition.SkillParameter p : skill.getParameters()) {
                        Map<String, Object> paramMap = new HashMap<>();
                        paramMap.put("name", p.getName());
                        paramMap.put("type", p.getType());
                        paramMap.put("required", p.isRequired());
                        params.add(paramMap);
                    }
                    skillMap.put("parameters", params);
                    skills.add(skillMap);
                }
                listener.onResponse(skills);
            },
            listener::onFailure
        ));
    }

    /**
     * Handle SHOW APPLICATION name INTENTS statement.
     */
    public void handleShowApplicationIntents(ElasticScriptParser.ShowApplicationIntentsContext ctx,
                                            ActionListener<Object> listener) {
        String appName = ctx.ID().getText();

        registry.getApplication(appName, ActionListener.wrap(
            optApp -> {
                if (optApp.isEmpty()) {
                    listener.onFailure(new IllegalArgumentException("Application not found: " + appName));
                    return;
                }

                List<Map<String, Object>> intents = new ArrayList<>();
                for (IntentDefinition intent : optApp.get().getIntents()) {
                    Map<String, Object> intentMap = new HashMap<>();
                    intentMap.put("pattern", intent.getPattern());
                    intentMap.put("target_skill", intent.getTargetSkill());
                    intentMap.put("description", intent.getDescription());
                    intents.add(intentMap);
                }
                listener.onResponse(intents);
            },
            listener::onFailure
        ));
    }

    /**
     * Handle APPLICATION 'name' | STATUS statement.
     */
    public void handleAppStatusOperation(ElasticScriptParser.Application_control_statementContext ctx,
                                        ActionListener<Object> listener) {
        String appName = extractAppName(ctx);

        registry.getApplication(appName, ActionListener.wrap(
            optApp -> {
                if (optApp.isEmpty()) {
                    listener.onFailure(new IllegalArgumentException("Application not found: " + appName));
                    return;
                }

                ApplicationDefinition app = optApp.get();
                Map<String, Object> result = new HashMap<>();
                result.put("name", app.getName());
                result.put("status", app.getStatus().name());
                result.put("version", app.getVersion());
                result.put("skills_count", app.getSkills().size());
                result.put("intents_count", app.getIntents().size());
                result.put("jobs_count", app.getJobNames().size());
                result.put("triggers_count", app.getTriggerNames().size());
                result.put("created_at", app.getCreatedAt().toString());
                if (app.getUpdatedAt() != null) {
                    result.put("updated_at", app.getUpdatedAt().toString());
                }
                listener.onResponse(result);
            },
            listener::onFailure
        ));
    }

    /**
     * Handle APPLICATION 'name' | PAUSE statement.
     */
    public void handleAppPauseOperation(ElasticScriptParser.Application_control_statementContext ctx,
                                       ActionListener<Object> listener) {
        String appName = extractAppName(ctx);

        registry.updateStatus(appName, ApplicationDefinition.Status.PAUSED, ActionListener.wrap(
            success -> {
                Map<String, Object> result = new HashMap<>();
                result.put("acknowledged", true);
                result.put("application", appName);
                result.put("status", "PAUSED");
                listener.onResponse(result);
            },
            listener::onFailure
        ));
    }

    /**
     * Handle APPLICATION 'name' | RESUME statement.
     */
    public void handleAppResumeOperation(ElasticScriptParser.Application_control_statementContext ctx,
                                        ActionListener<Object> listener) {
        String appName = extractAppName(ctx);

        registry.updateStatus(appName, ApplicationDefinition.Status.RUNNING, ActionListener.wrap(
            success -> {
                Map<String, Object> result = new HashMap<>();
                result.put("acknowledged", true);
                result.put("application", appName);
                result.put("status", "RUNNING");
                listener.onResponse(result);
            },
            listener::onFailure
        ));
    }

    /**
     * Handle EXTEND APPLICATION name ADD ... statement.
     */
    public void handleExtendApplicationAdd(ElasticScriptParser.ExtendApplicationAddContext ctx,
                                          ActionListener<Object> listener) {
        String appName = ctx.ID().getText();

        registry.getApplication(appName, ActionListener.wrap(
            optApp -> {
                if (optApp.isEmpty()) {
                    listener.onFailure(new IllegalArgumentException("Application not found: " + appName));
                    return;
                }

                ApplicationDefinition app = optApp.get();
                ApplicationDefinition.Builder builder = app.toBuilder();

                ElasticScriptParser.Application_extensionContext extCtx = ctx.application_extension();
                
                if (extCtx instanceof ElasticScriptParser.AddSkillExtensionContext skillCtx) {
                    List<SkillDefinition> skills = new ArrayList<>(app.getSkills());
                    skills.add(parseSkillDefinition(skillCtx.skill_definition()));
                    builder.skills(skills);
                } else if (extCtx instanceof ElasticScriptParser.AddIntentExtensionContext intentCtx) {
                    List<IntentDefinition> intents = new ArrayList<>(app.getIntents());
                    intents.add(parseIntentMapping(intentCtx.intent_mapping()));
                    builder.intents(intents);
                } else if (extCtx instanceof ElasticScriptParser.AddSourceExtensionContext sourceCtx) {
                    List<ApplicationDefinition.SourceDefinition> sources = new ArrayList<>(app.getSources());
                    sources.add(parseSourceDefinition(sourceCtx.source_definition()));
                    builder.sources(sources);
                }

                builder.updatedAt(Instant.now());
                ApplicationDefinition updated = builder.build();

                registry.saveApplication(updated, ActionListener.wrap(
                    success -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("acknowledged", true);
                        result.put("application", appName);
                        result.put("action", "extended");
                        listener.onResponse(result);
                    },
                    listener::onFailure
                ));
            },
            listener::onFailure
        ));
    }

    // ==================== Helper Methods ====================

    private String extractAppName(ElasticScriptParser.Application_control_statementContext ctx) {
        if (ctx.STRING() != null) {
            return stripQuotes(ctx.STRING().getText());
        }
        return ctx.ID().getText();
    }

    private List<ApplicationDefinition.SourceDefinition> parseSources(ElasticScriptParser.Sources_sectionContext ctx) {
        List<ApplicationDefinition.SourceDefinition> sources = new ArrayList<>();
        for (ElasticScriptParser.Source_definitionContext srcCtx : ctx.source_definition()) {
            sources.add(parseSourceDefinition(srcCtx));
        }
        return sources;
    }

    private ApplicationDefinition.SourceDefinition parseSourceDefinition(ElasticScriptParser.Source_definitionContext ctx) {
        String name = ctx.ID().getText();
        String indexPattern = stripQuotes(ctx.STRING().getText());
        return new ApplicationDefinition.SourceDefinition(name, indexPattern);
    }

    private List<SkillDefinition> parseSkills(ElasticScriptParser.Skills_sectionContext ctx) {
        List<SkillDefinition> skills = new ArrayList<>();
        for (ElasticScriptParser.Skill_definitionContext skillCtx : ctx.skill_definition()) {
            skills.add(parseSkillDefinition(skillCtx));
        }
        return skills;
    }

    private SkillDefinition parseSkillDefinition(ElasticScriptParser.Skill_definitionContext ctx) {
        String name = ctx.ID(0).getText();
        String returnType = ctx.datatype().getText();
        String description = null;
        if (ctx.STRING() != null) {
            description = stripQuotes(ctx.STRING().getText());
        }
        String procedureName = ctx.ID(1).getText();

        List<SkillDefinition.SkillParameter> params = new ArrayList<>();
        if (ctx.parameter_list() != null) {
            for (ElasticScriptParser.ParameterContext paramCtx : ctx.parameter_list().parameter()) {
                String paramName = paramCtx.ID().getText();
                String paramType = paramCtx.datatype() != null ? paramCtx.datatype().getText() : "STRING";
                params.add(new SkillDefinition.SkillParameter(paramName, paramType, null, true, null));
            }
        }

        List<String> procArgs = new ArrayList<>();
        if (ctx.argument_list() != null) {
            for (ElasticScriptParser.ExpressionContext exprCtx : ctx.argument_list().expression()) {
                procArgs.add(exprCtx.getText());
            }
        }

        return new SkillDefinition(name, description, params, returnType, procedureName, procArgs);
    }

    private List<IntentDefinition> parseIntents(ElasticScriptParser.Intents_sectionContext ctx) {
        List<IntentDefinition> intents = new ArrayList<>();
        for (ElasticScriptParser.Intent_mappingContext mapCtx : ctx.intent_mapping()) {
            intents.add(parseIntentMapping(mapCtx));
        }
        return intents;
    }

    private IntentDefinition parseIntentMapping(ElasticScriptParser.Intent_mappingContext ctx) {
        String pattern = stripQuotes(ctx.STRING().getText());
        String targetSkill = ctx.ID().getText();
        return new IntentDefinition(pattern, targetSkill, null, null);
    }

    private List<String> parseJobs(ElasticScriptParser.Jobs_sectionContext ctx) {
        List<String> jobNames = new ArrayList<>();
        for (ElasticScriptParser.Job_definitionContext jobCtx : ctx.job_definition()) {
            jobNames.add(jobCtx.ID(0).getText());
        }
        return jobNames;
    }

    private List<String> parseTriggers(ElasticScriptParser.Triggers_sectionContext ctx) {
        List<String> triggerNames = new ArrayList<>();
        for (ElasticScriptParser.Trigger_definitionContext triggerCtx : ctx.trigger_definition()) {
            triggerNames.add(triggerCtx.ID(0).getText());
        }
        return triggerNames;
    }

    private Object parseConfigValue(String valueText) {
        if (valueText.startsWith("'") || valueText.startsWith("\"")) {
            return stripQuotes(valueText);
        }
        try {
            return Integer.parseInt(valueText);
        } catch (NumberFormatException e) {
            try {
                return Double.parseDouble(valueText);
            } catch (NumberFormatException e2) {
                if ("true".equalsIgnoreCase(valueText)) return true;
                if ("false".equalsIgnoreCase(valueText)) return false;
                return valueText;
            }
        }
    }

    private String stripQuotes(String s) {
        if (s == null) return null;
        if ((s.startsWith("'") && s.endsWith("'")) || (s.startsWith("\"") && s.endsWith("\""))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private void ensureIndexExists(ActionListener<Boolean> listener) {
        client.admin().indices().prepareGetIndex(TimeValue.timeValueSeconds(30))
            .setIndices(ApplicationRegistry.INDEX_NAME)
            .execute(ActionListener.wrap(
                response -> listener.onResponse(true),
                e -> {
                    if (e instanceof IndexNotFoundException) {
                        CreateIndexRequest createRequest = new CreateIndexRequest(ApplicationRegistry.INDEX_NAME);
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

    private Map<String, Object> applicationToMap(ApplicationDefinition app) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", app.getName());
        map.put("description", app.getDescription());
        map.put("version", app.getVersion());
        map.put("status", app.getStatus().name());
        map.put("created_at", app.getCreatedAt().toString());
        if (app.getUpdatedAt() != null) {
            map.put("updated_at", app.getUpdatedAt().toString());
        }

        // Sources
        List<Map<String, Object>> sources = new ArrayList<>();
        for (ApplicationDefinition.SourceDefinition src : app.getSources()) {
            Map<String, Object> srcMap = new HashMap<>();
            srcMap.put("name", src.getName());
            srcMap.put("index_pattern", src.getIndexPattern());
            sources.add(srcMap);
        }
        map.put("sources", sources);

        // Skills
        List<Map<String, Object>> skills = new ArrayList<>();
        for (SkillDefinition skill : app.getSkills()) {
            Map<String, Object> skillMap = new HashMap<>();
            skillMap.put("name", skill.getName());
            skillMap.put("description", skill.getDescription());
            skillMap.put("return_type", skill.getReturnType());
            skillMap.put("procedure", skill.getProcedureName());
            skills.add(skillMap);
        }
        map.put("skills", skills);

        // Intents
        List<Map<String, Object>> intents = new ArrayList<>();
        for (IntentDefinition intent : app.getIntents()) {
            Map<String, Object> intentMap = new HashMap<>();
            intentMap.put("pattern", intent.getPattern());
            intentMap.put("target_skill", intent.getTargetSkill());
            intents.add(intentMap);
        }
        map.put("intents", intents);

        map.put("jobs", app.getJobNames());
        map.put("triggers", app.getTriggerNames());
        map.put("config", app.getConfig());

        return map;
    }

    public ApplicationRegistry getRegistry() {
        return registry;
    }
}
