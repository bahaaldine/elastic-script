/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.parser;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.test.ESTestCase;

/**
 * Tests for parsing standalone SKILL statements.
 */
public class SkillParserTests extends ESTestCase {

    private ElasticScriptParser.ProgramContext parseProgram(String input) {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);
        return parser.program();
    }

    // ========================================================================
    // CREATE SKILL Tests
    // ========================================================================

    public void testCreateSkillBasic() {
        String input = "CREATE SKILL detect_churn(threshold NUMBER) "
            + "RETURNS ARRAY "
            + "DESCRIPTION 'Identifies customers likely to churn' "
            + "PROCEDURE run_churn_analysis(threshold) "
            + "END SKILL";

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);
        assertNotNull(ctx.skill_statement());
        assertNotNull(ctx.skill_statement().create_skill_statement());

        var createCtx = ctx.skill_statement().create_skill_statement();
        assertEquals("detect_churn", createCtx.ID(0).getText());
        assertEquals("run_churn_analysis", createCtx.ID(1).getText());
        assertNotNull(createCtx.skill_param_list());
        assertEquals(1, createCtx.skill_param_list().skill_param().size());
    }

    public void testCreateSkillMultipleParams() {
        String input = "CREATE SKILL analyze_users(min_age NUMBER, max_age NUMBER, status STRING) "
            + "RETURNS DOCUMENT "
            + "DESCRIPTION 'Analyzes user demographics' "
            + "PROCEDURE user_analysis(min_age, max_age, status) "
            + "END SKILL";

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);

        var createCtx = ctx.skill_statement().create_skill_statement();
        assertEquals("analyze_users", createCtx.ID(0).getText());
        assertEquals(3, createCtx.skill_param_list().skill_param().size());
    }

    public void testCreateSkillWithExamples() {
        String input = "CREATE SKILL find_customers(query STRING) "
            + "RETURNS ARRAY "
            + "DESCRIPTION 'Searches for customers matching criteria' "
            + "EXAMPLES 'Find customers in New York', 'Show me premium users', 'List inactive accounts' "
            + "PROCEDURE search_customers(query) "
            + "END SKILL";

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);

        var createCtx = ctx.skill_statement().create_skill_statement();
        assertNotNull(createCtx.EXAMPLES());
        // 4 strings total: 1 for description, 3 for examples
        assertEquals(4, createCtx.STRING().size());
    }

    public void testCreateSkillNoParams() {
        String input = "CREATE SKILL get_system_status() "
            + "RETURNS DOCUMENT "
            + "DESCRIPTION 'Returns current system status' "
            + "PROCEDURE check_system() "
            + "END SKILL";

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);

        var createCtx = ctx.skill_statement().create_skill_statement();
        assertEquals("get_system_status", createCtx.ID(0).getText());
        assertNull(createCtx.skill_param_list());
    }

    public void testCreateSkillWithParamDescriptions() {
        String input = "CREATE SKILL analyze_logs(severity STRING DESCRIPTION 'Log severity level', "
            + "days NUMBER DESCRIPTION 'Number of days to analyze' DEFAULT 7) "
            + "RETURNS ARRAY "
            + "DESCRIPTION 'Analyzes log entries' "
            + "PROCEDURE log_analysis(severity, days) "
            + "END SKILL";

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);

        var createCtx = ctx.skill_statement().create_skill_statement();
        assertEquals(2, createCtx.skill_param_list().skill_param().size());

        var param2 = createCtx.skill_param_list().skill_param(1);
        assertNotNull(param2.DEFAULT());
    }

    // ========================================================================
    // DROP SKILL Tests
    // ========================================================================

    public void testDropSkill() {
        String input = "DROP SKILL detect_churn";

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);
        assertNotNull(ctx.skill_statement().drop_skill_statement());
        assertEquals("detect_churn", ctx.skill_statement().drop_skill_statement().ID().getText());
    }

    // ========================================================================
    // SHOW SKILLS Tests
    // ========================================================================

    public void testShowAllSkills() {
        String input = "SHOW SKILLS";

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);
        assertNotNull(ctx.skill_statement().show_skills_statement());
        assertTrue(ctx.skill_statement().show_skills_statement() instanceof ElasticScriptParser.ShowAllSkillsContext);
    }

    public void testShowSkillDetail() {
        String input = "SHOW SKILL detect_churn";

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);
        assertTrue(ctx.skill_statement().show_skills_statement() instanceof ElasticScriptParser.ShowSkillDetailContext);

        var showCtx = (ElasticScriptParser.ShowSkillDetailContext) ctx.skill_statement().show_skills_statement();
        assertEquals("detect_churn", showCtx.ID().getText());
    }

    // ========================================================================
    // ALTER SKILL Tests
    // ========================================================================

    public void testAlterSkillDescription() {
        String input = "ALTER SKILL detect_churn SET DESCRIPTION = 'Updated description for churn detection'";

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);
        assertNotNull(ctx.skill_statement().alter_skill_statement());

        var alterCtx = ctx.skill_statement().alter_skill_statement();
        assertEquals("detect_churn", alterCtx.ID().getText());
        assertEquals("DESCRIPTION", alterCtx.skill_property().getText());
    }

    // ========================================================================
    // GENERATE SKILL Tests
    // ========================================================================

    public void testGenerateSkillBasic() {
        String input = "GENERATE SKILL FROM 'Find customers who might leave based on order patterns'";

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);
        assertNotNull(ctx.skill_statement().generate_skill_statement());

        var genCtx = ctx.skill_statement().generate_skill_statement();
        assertEquals(1, genCtx.STRING().size());
    }

    public void testGenerateSkillWithModel() {
        String input = "GENERATE SKILL FROM 'Analyze customer behavior' WITH MODEL 'gpt-4'";

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);

        var genCtx = ctx.skill_statement().generate_skill_statement();
        assertNotNull(genCtx.MODEL());
        assertEquals(2, genCtx.STRING().size());
    }

    public void testGenerateSkillWithSave() {
        String input = "GENERATE SKILL FROM 'Detect anomalies in metrics' SAVE AS anomaly_detector";

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);

        var genCtx = ctx.skill_statement().generate_skill_statement();
        assertNotNull(genCtx.SAVE_KW());
        assertEquals("anomaly_detector", genCtx.ID().getText());
    }

    public void testGenerateSkillWithModelAndSave() {
        String input = "GENERATE SKILL FROM 'Find trending products' WITH MODEL 'claude-3' SAVE AS trending_products";

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);

        var genCtx = ctx.skill_statement().generate_skill_statement();
        assertNotNull(genCtx.MODEL());
        assertNotNull(genCtx.SAVE_KW());
        assertEquals("trending_products", genCtx.ID().getText());
    }
}
