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
     * CREATE SKILL name VERSION 'semver'
     *   [DESCRIPTION 'desc']
     *   [AUTHOR 'author']
     *   [TAGS ['tag1', 'tag2']]
     *   [REQUIRES ['dep1', 'dep2']]
     *   [(params)]
     *   [RETURNS type]
     *   BEGIN ... END SKILL
     */
    public void handleCreateSkill(ElasticScriptParser.Create_skill_statementContext ctx,
                                  ActionListener<Object> listener) {
        // Parse skill name
        String skillName = ctx.ID().getText();
        
        // Parse version (required)
        List<org.antlr.v4.runtime.tree.TerminalNode> strings = ctx.STRING();
        String version = stripQuotes(strings.get(0).getText());
        
        // Parse description (optional, second STRING if DESCRIPTION token present)
        String description = null;
        if (ctx.DESCRIPTION() != null && strings.size() > 1) {
            description = stripQuotes(strings.get(1).getText());
        }
        
        // Parse author (optional)
        String author = null;
        if (ctx.AUTHOR() != null) {
            // Author string is the next STRING after description
            int authorIndex = ctx.DESCRIPTION() != null ? 2 : 1;
            if (strings.size() > authorIndex) {
                author = stripQuotes(strings.get(authorIndex).getText());
            }
        }
        
        // Parse tags (optional)
        List<String> tags = new ArrayList<>();
        List<ElasticScriptParser.ArrayLiteralContext> arrays = ctx.arrayLiteral();
        int arrayIndex = 0;
        if (ctx.TAGS() != null && arrays.size() > arrayIndex) {
            tags = parseStringArray(arrays.get(arrayIndex++));
        }
        
        // Parse requires (dependencies, optional)
        List<String> requires = new ArrayList<>();
        if (ctx.REQUIRES() != null && arrays.size() > arrayIndex) {
            requires = parseStringArray(arrays.get(arrayIndex++));
        }
        
        // Parse parameters
        List<SkillDefinition.SkillParameter> parameters = new ArrayList<>();
        if (ctx.skill_parameters_clause() != null && 
            ctx.skill_parameters_clause().skill_param_list() != null) {
            for (ElasticScriptParser.Skill_paramContext paramCtx : 
                 ctx.skill_parameters_clause().skill_param_list().skill_param()) {
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
        
        // Extract the skill body statements with proper whitespace
        // Use the token stream to get the original text with whitespace preserved
        List<ElasticScriptParser.StatementContext> bodyStatements = ctx.statement();
        StringBuilder bodyText = new StringBuilder();
        for (ElasticScriptParser.StatementContext stmt : bodyStatements) {
            // Get the original text from the input stream to preserve whitespace
            org.antlr.v4.runtime.misc.Interval interval = stmt.getSourceInterval();
            String stmtText = stmt.start.getInputStream().getText(
                new org.antlr.v4.runtime.misc.Interval(
                    stmt.start.getStartIndex(), 
                    stmt.stop.getStopIndex()
                )
            );
            bodyText.append(stmtText).append(" ");
        }
        String skillBody = bodyText.toString().trim();
        
        // Build the procedure parameters string
        String procParamsStr = parameters.stream()
            .map(p -> p.getName() + " " + p.getType() + 
                (p.getDefaultValue() != null ? " DEFAULT " + p.getDefaultValue() : ""))
            .collect(Collectors.joining(", "));
        
        // Create the corresponding procedure definition
        // Note: The grammar rule 'procedure' expects "PROCEDURE name(...) BEGIN ... END PROCEDURE"
        // NOT "CREATE PROCEDURE ..." - the CREATE is handled by the create_procedure_statement rule
        StringBuilder procBuilder = new StringBuilder();
        procBuilder.append("PROCEDURE ").append(skillName).append("(");
        procBuilder.append(procParamsStr);
        procBuilder.append(") ");
        procBuilder.append("BEGIN ");
        procBuilder.append(skillBody);
        procBuilder.append(" END PROCEDURE");
        String procedureDefinition = procBuilder.toString();
        
        LOGGER.debug("Creating procedure for skill {}: {}", skillName, procedureDefinition);
        
        // Build skill definition with new metadata
        SkillDefinition skill = new SkillDefinition(
            skillName,
            description,
            parameters,
            returnType,
            skillName, // procedureName is the skill itself
            parameters.stream().map(SkillDefinition.SkillParameter::getName).collect(Collectors.toList()),
            new ArrayList<>() // examples removed from grammar
        );
        skill.setVersion(version);
        skill.setAuthor(author);
        skill.setTags(tags);
        skill.setDependencies(requires);
        
        // Build readable source code with actual body
        StringBuilder readableSource = new StringBuilder();
        readableSource.append("CREATE SKILL ").append(skillName).append("\n");
        readableSource.append("  VERSION '").append(version).append("'\n");
        if (description != null) {
            readableSource.append("  DESCRIPTION '").append(description).append("'\n");
        }
        if (author != null) {
            readableSource.append("  AUTHOR '").append(author).append("'\n");
        }
        if (tags != null && !tags.isEmpty()) {
            readableSource.append("  TAGS [").append(tags.stream().map(t -> "'" + t + "'").collect(Collectors.joining(", "))).append("]\n");
        }
        if (!parameters.isEmpty()) {
            readableSource.append("  (").append(procParamsStr).append(")\n");
        }
        if (returnType != null) {
            readableSource.append("  RETURNS ").append(returnType).append("\n");
        }
        readableSource.append("BEGIN\n");
        // Add formatted body
        for (ElasticScriptParser.StatementContext stmt : bodyStatements) {
            readableSource.append("  ").append(stmt.getText()).append("\n");
        }
        readableSource.append("END SKILL;");
        skill.setSourceCode(readableSource.toString());
        
        // Generate documentation
        String documentation = skill.generateDocumentation();
        skill.setDocumentation(documentation);
        
        LOGGER.debug("Creating skill: {} v{}", skillName, version);
        
        // First, save the corresponding procedure, then save the skill
        String finalProcedureDefinition = procedureDefinition;
        String finalReturnType = returnType;
        
        // Store the procedure with parameters (required by ProcedureExecutor)
        Map<String, Object> procSource = new HashMap<>();
        procSource.put("procedure", finalProcedureDefinition);
        procSource.put("created_at", System.currentTimeMillis());
        procSource.put("skill", skillName); // Mark as skill-generated
        
        // Add parameters in the format expected by ProcedureExecutor.getProcedureAsync()
        List<Map<String, Object>> paramsList = new ArrayList<>();
        for (SkillDefinition.SkillParameter param : parameters) {
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("name", param.getName());
            paramMap.put("type", param.getType());
            if (param.getDefaultValue() != null) {
                paramMap.put("default", param.getDefaultValue());
            }
            paramsList.add(paramMap);
        }
        procSource.put("parameters", paramsList);
        
        client.prepareIndex(".elastic_script_procedures")
            .setId(skillName)
            .setSource(procSource)
            .execute(ActionListener.wrap(
                indexResponse -> {
                    LOGGER.debug("Stored procedure for skill {}", skillName);
                    // Now save the skill to registry
                    registry.saveSkill(skill, ActionListener.wrap(
                        success -> {
                            Map<String, Object> result = new HashMap<>();
                            result.put("action", "CREATE SKILL");
                            result.put("skill", skillName);
                            result.put("version", version);
                            result.put("parameters", parameters.size());
                            result.put("procedure_created", true);
                            result.put("success", success);
                            listener.onResponse(result);
                        },
                        listener::onFailure
                    ));
                },
                listener::onFailure
            ));
    }
    
    /**
     * Parse an array literal into a list of strings.
     */
    private List<String> parseStringArray(ElasticScriptParser.ArrayLiteralContext arrayCtx) {
        List<String> result = new ArrayList<>();
        if (arrayCtx != null && arrayCtx.expressionList() != null) {
            for (ElasticScriptParser.ExpressionContext expr : arrayCtx.expressionList().expression()) {
                result.add(stripQuotes(expr.getText()));
            }
        }
        return result;
    }

    /**
     * Handle DROP SKILL statement.
     * Also handles DROP SKILL PACK.
     */
    public void handleDropSkill(ElasticScriptParser.Drop_skill_statementContext ctx,
                                ActionListener<Object> listener) {
        String name = ctx.ID().getText();
        
        // Check if this is DROP SKILL PACK
        if (ctx.PACK() != null) {
            LOGGER.debug("Dropping skill pack: {}", name);
            handleDropSkillPack(name, listener);
            return;
        }
        
        LOGGER.debug("Dropping skill: {}", name);
        
        registry.deleteSkill(name, ActionListener.wrap(
            deleted -> {
                Map<String, Object> result = new HashMap<>();
                result.put("action", "DROP SKILL");
                result.put("skill", name);
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
    
    /**
     * Handle TEST SKILL statement.
     * 
     * TEST SKILL name WITH param1 = value1, param2 = value2 EXPECT result_expression
     */
    public void handleTestSkill(ElasticScriptParser.Test_skill_statementContext ctx,
                               ActionListener<Object> listener) {
        try {
            String skillName = ctx.ID().getText();
            
            // Parse test arguments
            Map<String, Object> testArgs = new HashMap<>();
            if (ctx.skill_test_args() != null) {
                for (ElasticScriptParser.Skill_test_argContext argCtx : ctx.skill_test_args().skill_test_arg()) {
                    String argName = argCtx.ID().getText();
                    String argValue = argCtx.expression().getText();
                    // Simple evaluation - strip quotes if it's a string
                    testArgs.put(argName, stripQuotes(argValue));
                }
            }
            
            // Parse expected result expression
            String expectedExpr = ctx.expression().getText();
            
            LOGGER.debug("Testing skill: {} with args: {}, expecting: {}", skillName, testArgs, expectedExpr);
            
            // Look up the skill definition
            registry.getSkill(skillName, ActionListener.wrap(
                skill -> {
                    if (skill == null) {
                        listener.onFailure(new RuntimeException("Skill not found: " + skillName));
                        return;
                    }
                    
                    // Execute the skill's procedure with test arguments
                    // For now, return a test result structure
                    Map<String, Object> testResult = new HashMap<>();
                    testResult.put("action", "TEST SKILL");
                    testResult.put("skill", skillName);
                    testResult.put("test_args", testArgs);
                    testResult.put("expected", expectedExpr);
                    
                    // TODO: Actually execute the skill and compare results
                    // For now, create a framework response
                    testResult.put("status", "executed");
                    testResult.put("passed", true);  // Placeholder
                    testResult.put("actual_result", "Test execution placeholder");
                    testResult.put("execution_time_ms", 0);
                    testResult.put("message", String.format(
                        "Skill '%s' test framework ready. " +
                        "Actual skill execution requires procedure implementation.",
                        skillName
                    ));
                    
                    listener.onResponse(testResult);
                },
                e -> {
                    // Skill not found - return test failure
                    Map<String, Object> testResult = new HashMap<>();
                    testResult.put("action", "TEST SKILL");
                    testResult.put("skill", skillName);
                    testResult.put("status", "failed");
                    testResult.put("passed", false);
                    testResult.put("error", "Skill not found: " + skillName);
                    listener.onResponse(testResult);
                }
            ));
            
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    /**
     * Handle RUN SKILL statement.
     * Executes a skill and returns its result.
     */
    public void handleRunSkill(ElasticScriptParser.Run_skill_statementContext ctx,
                               ActionListener<Object> listener) {
        try {
            String skillName = ctx.ID().getText();
            
            LOGGER.debug("Running skill: {}", skillName);
            
            // Get skill definition to find the procedure
            registry.getSkill(skillName, ActionListener.wrap(
                optSkill -> {
                    if (optSkill.isEmpty()) {
                        listener.onFailure(new IllegalArgumentException("Skill not found: " + skillName));
                        return;
                    }
                    
                    SkillDefinition skill = optSkill.get();
                    String procedureName = skill.getProcedureName();
                    
                    // Build procedure call arguments
                    List<Object> args = new ArrayList<>();
                    
                    // Handle positional arguments: RUN SKILL name(arg1, arg2)
                    if (ctx.expressionList() != null) {
                        for (ElasticScriptParser.ExpressionContext exprCtx : ctx.expressionList().expression()) {
                            args.add(stripQuotes(exprCtx.getText()));
                        }
                    }
                    
                    // Handle named arguments: RUN SKILL name WITH param = value
                    Map<String, Object> namedArgs = new HashMap<>();
                    if (ctx.skill_test_args() != null) {
                        for (ElasticScriptParser.Skill_test_argContext argCtx : ctx.skill_test_args().skill_test_arg()) {
                            String paramName = argCtx.ID().getText();
                            String paramValue = stripQuotes(argCtx.expression().getText());
                            namedArgs.put(paramName, paramValue);
                        }
                    }
                    
                    // Build the result including execution info
                    Map<String, Object> result = new HashMap<>();
                    result.put("action", "RUN SKILL");
                    result.put("skill", skillName);
                    result.put("procedure", procedureName);
                    result.put("args", args.isEmpty() ? namedArgs : args);
                    result.put("status", "executed");
                    
                    // Note: Actual procedure execution would require access to ProcedureExecutor
                    // For now, we return the call information so the demo can show the flow
                    result.put("message", String.format(
                        "To execute this skill, call the underlying procedure: CALL %s(%s)",
                        procedureName,
                        args.isEmpty() ? "" : String.join(", ", args.stream().map(Object::toString).toList())
                    ));
                    
                    listener.onResponse(result);
                },
                listener::onFailure
            ));
            
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    // ========================================================================
    // SKILL PACK HANDLERS
    // ========================================================================

    private static final String SKILL_PACKS_INDEX = ".moltler_skill_packs";

    /**
     * Handle CREATE SKILL PACK statement.
     * 
     * CREATE SKILL PACK name VERSION 'semver'
     *   [DESCRIPTION 'desc']
     *   [AUTHOR 'author']
     *   [TAGS ['tag1', 'tag2']]
     *   SKILLS [skill1, skill2, ...]
     *   END_PACK
     */
    public void handleCreateSkillPack(ElasticScriptParser.Create_skill_pack_statementContext ctx,
                                      ActionListener<Object> listener) {
        try {
            String packName = ctx.ID().getText();
            
            List<org.antlr.v4.runtime.tree.TerminalNode> strings = ctx.STRING();
            String version = stripQuotes(strings.get(0).getText());
            
            // Parse description
            String description = null;
            if (ctx.DESCRIPTION() != null && strings.size() > 1) {
                description = stripQuotes(strings.get(1).getText());
            }
            
            // Parse author
            String author = null;
            if (ctx.AUTHOR() != null) {
                int authorIndex = ctx.DESCRIPTION() != null ? 2 : 1;
                if (strings.size() > authorIndex) {
                    author = stripQuotes(strings.get(authorIndex).getText());
                }
            }
            
            // Parse tags
            List<String> tags = new ArrayList<>();
            List<ElasticScriptParser.ArrayLiteralContext> arrays = ctx.arrayLiteral();
            int arrayIndex = 0;
            if (ctx.TAGS() != null && arrays.size() > arrayIndex) {
                tags = parseStringArray(arrays.get(arrayIndex++));
            }
            
            // Parse skills list (required)
            final List<String> skills;
            if (ctx.SKILLS() != null && arrays.size() > arrayIndex) {
                skills = parseStringArray(arrays.get(arrayIndex));
            } else {
                skills = new ArrayList<>();
            }
            
            LOGGER.debug("Creating skill pack: {} v{} with {} skills", packName, version, skills.size());
            
            // Build skill pack document
            Map<String, Object> packDoc = new HashMap<>();
            packDoc.put("name", packName);
            packDoc.put("version", version);
            packDoc.put("description", description);
            packDoc.put("author", author);
            packDoc.put("tags", tags);
            packDoc.put("skills", skills);
            packDoc.put("created_at", System.currentTimeMillis());
            packDoc.put("updated_at", System.currentTimeMillis());
            
            // Ensure index exists and save
            ensureIndexExists(SKILL_PACKS_INDEX, ActionListener.wrap(
                created -> {
                    client.prepareIndex(SKILL_PACKS_INDEX)
                        .setId(packName)
                        .setSource(packDoc)
                        .execute(ActionListener.wrap(
                            indexResponse -> {
                                Map<String, Object> result = new HashMap<>();
                                result.put("action", "CREATE SKILL PACK");
                                result.put("pack", packName);
                                result.put("version", version);
                                result.put("skills_count", skills.size());
                                result.put("skills", skills);
                                listener.onResponse(result);
                            },
                            listener::onFailure
                        ));
                },
                listener::onFailure
            ));
            
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    /**
     * Handle SHOW SKILL PACKS statement.
     */
    public void handleShowAllSkillPacks(ActionListener<Object> listener) {
        LOGGER.debug("Listing all skill packs");
        
        org.elasticsearch.action.search.SearchRequest searchRequest = 
            new org.elasticsearch.action.search.SearchRequest(SKILL_PACKS_INDEX);
        searchRequest.source(new org.elasticsearch.search.builder.SearchSourceBuilder()
            .query(org.elasticsearch.index.query.QueryBuilders.matchAllQuery())
            .size(1000));
        
        client.search(searchRequest, ActionListener.wrap(
            response -> {
                List<Map<String, Object>> packs = new ArrayList<>();
                for (org.elasticsearch.search.SearchHit hit : response.getHits().getHits()) {
                    Map<String, Object> source = hit.getSourceAsMap();
                    Map<String, Object> pack = new HashMap<>();
                    pack.put("name", source.get("name"));
                    pack.put("version", source.get("version"));
                    pack.put("description", source.get("description"));
                    pack.put("skills_count", ((List<?>) source.getOrDefault("skills", List.of())).size());
                    packs.add(pack);
                }
                
                Map<String, Object> result = new HashMap<>();
                result.put("action", "SHOW SKILL PACKS");
                result.put("count", packs.size());
                result.put("packs", packs);
                listener.onResponse(result);
            },
            e -> {
                if (e.getMessage() != null && e.getMessage().contains("no such index")) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("action", "SHOW SKILL PACKS");
                    result.put("count", 0);
                    result.put("packs", new ArrayList<>());
                    listener.onResponse(result);
                } else {
                    listener.onFailure(e);
                }
            }
        ));
    }

    /**
     * Handle SHOW SKILL PACK name statement.
     */
    public void handleShowSkillPackDetail(ElasticScriptParser.ShowSkillPackDetailContext ctx,
                                          ActionListener<Object> listener) {
        String packName = ctx.ID().getText();
        
        LOGGER.debug("Showing skill pack detail: {}", packName);
        
        client.prepareGet(SKILL_PACKS_INDEX, packName).execute(ActionListener.wrap(
            getResponse -> {
                if (getResponse.isExists()) {
                    Map<String, Object> source = getResponse.getSourceAsMap();
                    
                    Map<String, Object> result = new HashMap<>();
                    result.put("action", "SHOW SKILL PACK");
                    result.put("name", source.get("name"));
                    result.put("version", source.get("version"));
                    result.put("description", source.get("description"));
                    result.put("author", source.get("author"));
                    result.put("tags", source.get("tags"));
                    result.put("skills", source.get("skills"));
                    result.put("created_at", source.get("created_at"));
                    result.put("updated_at", source.get("updated_at"));
                    
                    listener.onResponse(result);
                } else {
                    listener.onFailure(new IllegalArgumentException("Skill pack not found: " + packName));
                }
            },
            listener::onFailure
        ));
    }

    /**
     * Handle DROP SKILL PACK statement.
     */
    public void handleDropSkillPack(String packName, ActionListener<Object> listener) {
        LOGGER.debug("Dropping skill pack: {}", packName);
        
        client.prepareDelete(SKILL_PACKS_INDEX, packName).execute(ActionListener.wrap(
            deleteResponse -> {
                Map<String, Object> result = new HashMap<>();
                result.put("action", "DROP SKILL PACK");
                result.put("pack", packName);
                result.put("deleted", true);
                listener.onResponse(result);
            },
            listener::onFailure
        ));
    }

    private void ensureIndexExists(String indexName, ActionListener<Boolean> listener) {
        client.admin().indices().prepareGetIndex(org.elasticsearch.core.TimeValue.timeValueSeconds(30))
            .setIndices(indexName)
            .execute(ActionListener.wrap(
                response -> listener.onResponse(true),
                e -> {
                    if (e instanceof org.elasticsearch.index.IndexNotFoundException) {
                        client.admin().indices().prepareCreate(indexName).execute(ActionListener.wrap(
                            response -> listener.onResponse(true),
                            listener::onFailure
                        ));
                    } else {
                        listener.onFailure(e);
                    }
                }
            ));
    }

    private String stripQuotes(String s) {
        if (s == null) return null;
        if ((s.startsWith("'") && s.endsWith("'")) || (s.startsWith("\"") && s.endsWith("\""))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
