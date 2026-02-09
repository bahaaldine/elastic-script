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
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.xpack.escript.applications.SkillDefinition;
import org.elasticsearch.xpack.escript.applications.SkillRegistry;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handler for standalone skill statements:
 * CREATE SKILL, DROP SKILL, SHOW SKILLS, ALTER SKILL, GENERATE SKILL.
 */
public class SkillStatementHandler {

    private static final Logger LOGGER = LogManager.getLogger(SkillStatementHandler.class);

    private final Client client;
    private final SkillRegistry registry;

    public SkillStatementHandler(Client client) {
        this.client = client;
        this.registry = new SkillRegistry(client);
    }

    /**
     * Handle CREATE SKILL statement.
     * 
     * CREATE SKILL name(params) RETURNS type DESCRIPTION 'desc'
     *   EXAMPLES 'example1', 'example2'
     *   PROCEDURE proc_name(args)
     * END SKILL
     */
    public void handleCreateSkill(ElasticScriptParser.Create_skill_statementContext ctx,
                                  ActionListener<Object> listener) {
        // Parse skill name (first ID)
        String skillName = ctx.ID(0).getText();
        
        // Parse procedure name (second ID)
        String procedureName = ctx.ID(1).getText();
        
        // Parse parameters
        List<SkillDefinition.SkillParameter> parameters = new ArrayList<>();
        if (ctx.skill_param_list() != null) {
            for (ElasticScriptParser.Skill_paramContext paramCtx : ctx.skill_param_list().skill_param()) {
                String paramName = paramCtx.ID().getText();
                String paramType = paramCtx.datatype().getText().toUpperCase();
                String paramDesc = null;
                Object defaultValue = null;
                
                // Check for parameter description
                if (paramCtx.DESCRIPTION() != null && paramCtx.STRING() != null) {
                    paramDesc = stripQuotes(paramCtx.STRING().getText());
                }
                
                // Check for default value
                if (paramCtx.DEFAULT() != null && paramCtx.expression() != null) {
                    defaultValue = paramCtx.expression().getText();
                }
                
                // If has default value, not required
                boolean required = (defaultValue == null);
                
                parameters.add(new SkillDefinition.SkillParameter(
                    paramName, paramType, paramDesc, required, defaultValue
                ));
            }
        }
        
        // Parse return type
        String returnType = null;
        if (ctx.RETURNS() != null && ctx.datatype() != null) {
            returnType = ctx.datatype().getText().toUpperCase();
        }
        
        // Parse description
        String description = null;
        List<String> allStrings = ctx.STRING().stream()
            .map(s -> stripQuotes(s.getText()))
            .collect(Collectors.toList());
        
        int stringIndex = 0;
        if (ctx.DESCRIPTION() != null && !allStrings.isEmpty()) {
            description = allStrings.get(stringIndex++);
        }
        
        // Parse examples (remaining strings after description)
        List<String> examples = new ArrayList<>();
        if (ctx.EXAMPLES() != null) {
            for (int i = stringIndex; i < allStrings.size(); i++) {
                examples.add(allStrings.get(i));
            }
        }
        
        // Parse procedure arguments
        List<String> procedureArgs = new ArrayList<>();
        if (ctx.argument_list() != null) {
            for (ElasticScriptParser.ExpressionContext exprCtx : ctx.argument_list().expression()) {
                procedureArgs.add(exprCtx.getText());
            }
        }
        
        // Build skill definition
        SkillDefinition skill = new SkillDefinition(
            skillName,
            description,
            parameters,
            returnType,
            procedureName,
            procedureArgs,
            examples
        );
        
        LOGGER.debug("Creating skill: {} -> procedure {}", skillName, procedureName);
        
        // Save to registry
        registry.saveSkill(skill, ActionListener.wrap(
            success -> {
                Map<String, Object> result = new HashMap<>();
                result.put("action", "CREATE SKILL");
                result.put("skill", skillName);
                result.put("procedure", procedureName);
                result.put("parameters", parameters.size());
                result.put("examples", examples.size());
                result.put("success", success);
                listener.onResponse(result);
            },
            listener::onFailure
        ));
    }

    /**
     * Handle DROP SKILL statement.
     */
    public void handleDropSkill(ElasticScriptParser.Drop_skill_statementContext ctx,
                                ActionListener<Object> listener) {
        String skillName = ctx.ID().getText();
        
        LOGGER.debug("Dropping skill: {}", skillName);
        
        registry.deleteSkill(skillName, ActionListener.wrap(
            deleted -> {
                Map<String, Object> result = new HashMap<>();
                result.put("action", "DROP SKILL");
                result.put("skill", skillName);
                result.put("deleted", deleted);
                listener.onResponse(result);
            },
            listener::onFailure
        ));
    }

    /**
     * Handle SHOW SKILLS statement.
     */
    public void handleShowAllSkills(ActionListener<Object> listener) {
        LOGGER.debug("Listing all standalone skills");
        
        registry.listSkills(ActionListener.wrap(
            skills -> {
                List<Map<String, Object>> skillList = new ArrayList<>();
                for (SkillDefinition skill : skills) {
                    Map<String, Object> skillInfo = new HashMap<>();
                    skillInfo.put("name", skill.getName());
                    skillInfo.put("description", skill.getDescription());
                    skillInfo.put("procedure", skill.getProcedureName());
                    skillInfo.put("parameters", skill.getParameters().size());
                    skillInfo.put("return_type", skill.getReturnType());
                    skillList.add(skillInfo);
                }
                
                Map<String, Object> result = new HashMap<>();
                result.put("action", "SHOW SKILLS");
                result.put("count", skills.size());
                result.put("skills", skillList);
                listener.onResponse(result);
            },
            listener::onFailure
        ));
    }

    /**
     * Handle SHOW SKILL name statement.
     */
    public void handleShowSkillDetail(ElasticScriptParser.ShowSkillDetailContext ctx,
                                      ActionListener<Object> listener) {
        String skillName = ctx.ID().getText();
        
        LOGGER.debug("Showing skill detail: {}", skillName);
        
        registry.getSkill(skillName, ActionListener.wrap(
            optSkill -> {
                if (optSkill.isEmpty()) {
                    listener.onFailure(new IllegalArgumentException("Skill not found: " + skillName));
                    return;
                }
                
                SkillDefinition skill = optSkill.get();
                Map<String, Object> result = new HashMap<>();
                result.put("action", "SHOW SKILL");
                result.put("name", skill.getName());
                result.put("description", skill.getDescription());
                result.put("procedure", skill.getProcedureName());
                result.put("procedure_args", skill.getProcedureArgs());
                result.put("return_type", skill.getReturnType());
                result.put("examples", skill.getExamples());
                
                List<Map<String, Object>> paramList = new ArrayList<>();
                for (SkillDefinition.SkillParameter param : skill.getParameters()) {
                    Map<String, Object> paramInfo = new HashMap<>();
                    paramInfo.put("name", param.getName());
                    paramInfo.put("type", param.getType());
                    paramInfo.put("description", param.getDescription());
                    paramInfo.put("required", param.isRequired());
                    paramInfo.put("default", param.getDefaultValue());
                    paramList.add(paramInfo);
                }
                result.put("parameters", paramList);
                
                // Also include MCP tool spec
                result.put("mcp_tool_spec", skill.toMcpToolSpec());
                
                listener.onResponse(result);
            },
            listener::onFailure
        ));
    }

    /**
     * Handle ALTER SKILL statement.
     * ALTER SKILL name SET DESCRIPTION = 'new desc'
     */
    public void handleAlterSkill(ElasticScriptParser.Alter_skill_statementContext ctx,
                                 ActionListener<Object> listener) {
        String skillName = ctx.ID().getText();
        String property = ctx.skill_property().getText().toUpperCase();
        String newValue = ctx.expression().getText();
        
        // Strip quotes if it's a string value
        if (newValue.startsWith("'") || newValue.startsWith("\"")) {
            newValue = stripQuotes(newValue);
        }
        
        LOGGER.debug("Altering skill {}: {} = {}", skillName, property, newValue);
        
        final String finalNewValue = newValue;
        
        if ("DESCRIPTION".equals(property)) {
            registry.updateDescription(skillName, finalNewValue, ActionListener.wrap(
                success -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("action", "ALTER SKILL");
                    result.put("skill", skillName);
                    result.put("property", property);
                    result.put("new_value", finalNewValue);
                    result.put("success", success);
                    listener.onResponse(result);
                },
                listener::onFailure
            ));
        } else {
            listener.onFailure(new UnsupportedOperationException(
                "ALTER SKILL SET " + property + " is not yet supported"));
        }
    }

    /**
     * Handle GENERATE SKILL FROM 'description' statement.
     * Uses LLM to generate a skill definition from natural language.
     */
    public void handleGenerateSkill(ElasticScriptParser.Generate_skill_statementContext ctx,
                                    ActionListener<Object> listener) {
        String description = stripQuotes(ctx.STRING(0).getText());
        
        // Optional: get model name
        String model = "gpt-4";
        if (ctx.MODEL() != null && ctx.STRING().size() > 1) {
            model = stripQuotes(ctx.STRING(1).getText());
        }
        
        // Optional: get save name
        String saveName = null;
        if (ctx.SAVE_KW() != null && ctx.ID() != null) {
            saveName = ctx.ID().getText();
        }
        
        LOGGER.debug("Generating skill from description: '{}' using model: {}", description, model);
        
        // Build prompt for LLM
        String prompt = buildSkillGenerationPrompt(description);
        
        // For now, we'll create a placeholder response
        // In a full implementation, this would call the LLM
        final String finalModel = model;
        final String finalSaveName = saveName;
        
        // Call LLM to generate skill definition
        generateSkillWithLLM(prompt, finalModel, description, finalSaveName, listener);
    }

    private String buildSkillGenerationPrompt(String userDescription) {
        return "You are an elastic-script skill designer. Based on the user's description, "
            + "generate a skill definition.\n\n"
            + "User's description: \"" + userDescription + "\"\n\n"
            + "Generate a JSON response with:\n"
            + "{\n"
            + "  \"name\": \"suggested_skill_name\",\n"
            + "  \"description\": \"Clear description of what the skill does\",\n"
            + "  \"parameters\": [\n"
            + "    {\"name\": \"param_name\", \"type\": \"STRING|NUMBER|BOOLEAN|ARRAY|DOCUMENT\", "
            + "\"description\": \"param description\", \"required\": true}\n"
            + "  ],\n"
            + "  \"return_type\": \"ARRAY|DOCUMENT|STRING|NUMBER|BOOLEAN\",\n"
            + "  \"suggested_procedure_name\": \"name_of_procedure_to_implement\",\n"
            + "  \"procedure_implementation_hint\": \"Brief description of what the procedure should do\"\n"
            + "}\n\n"
            + "Focus on:\n"
            + "1. Elasticsearch/data-centric operations\n"
            + "2. Clear, descriptive naming\n"
            + "3. Appropriate parameter types\n"
            + "4. Useful examples for AI discovery";
    }

    private void generateSkillWithLLM(String prompt, String model, String originalDescription,
                                       String saveName, ActionListener<Object> listener) {
        // For now, generate a smart template
        // TODO: Integrate actual LLM call when OPENAI_API_KEY is configured
        
        Map<String, Object> result = new HashMap<>();
        result.put("action", "GENERATE SKILL");
        result.put("status", "template");
        result.put("model", model);
        result.put("message", "Generated skill template based on your description. "
            + "To create this skill, execute the CREATE SKILL statement below.");

        String suggestedName = generateSkillNameFromDescription(originalDescription);
        Map<String, Object> template = new HashMap<>();
        template.put("name", suggestedName);
        template.put("description", originalDescription);
        template.put("parameters", List.of(
            Map.of("name", "input", "type", "STRING", "description", "Input parameter", "required", true)
        ));
        template.put("return_type", "DOCUMENT");
        template.put("suggested_procedure_name", "impl_" + suggestedName);
        template.put("create_statement", buildCreateSkillStatement(suggestedName, originalDescription));

        result.put("suggested_skill", template);

        if (saveName != null) {
            result.put("save_as", saveName);
        }

        listener.onResponse(result);
    }

    private String generateSkillNameFromDescription(String description) {
        // Simple name generation from description
        String[] words = description.toLowerCase()
            .replaceAll("[^a-z0-9\\s]", "")
            .split("\\s+");
        
        StringBuilder name = new StringBuilder();
        int count = 0;
        for (String word : words) {
            if (word.length() > 2 && !isStopWord(word) && count < 3) {
                if (name.length() > 0) name.append("_");
                name.append(word);
                count++;
            }
        }
        
        return name.length() > 0 ? name.toString() : "generated_skill";
    }

    private boolean isStopWord(String word) {
        return List.of("the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "with", "by", "from", "as", "is", "was", "are", "were", "been", "be",
            "have", "has", "had", "do", "does", "did", "will", "would", "could", "should",
            "may", "might", "must", "that", "which", "who", "whom", "this", "these", "those",
            "find", "get", "show", "display", "list", "search").contains(word);
    }

    private String buildCreateSkillStatement(String name, String description) {
        String escapedDesc = description.replace("'", "''");
        return "CREATE SKILL " + name + "(input STRING)\n"
            + "  RETURNS DOCUMENT\n"
            + "  DESCRIPTION '" + escapedDesc + "'\n"
            + "  EXAMPLES 'Example query for " + name + "'\n"
            + "  PROCEDURE impl_" + name + "(input)\n"
            + "END SKILL";
    }

    private String stripQuotes(String s) {
        if (s == null) return null;
        if ((s.startsWith("'") && s.endsWith("'")) || (s.startsWith("\"") && s.endsWith("\""))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
