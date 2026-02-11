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
 * Tests for parsing standalone SKILL statements (Moltler grammar).
 */
public class SkillParserTests extends ESTestCase {

    private ElasticScriptParser.ProgramContext parseProgram(String input) {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);
        return parser.program();
    }

    // ========================================================================
    // CREATE SKILL Tests (New Moltler Grammar)
    // ========================================================================

    public void testCreateSkillBasic() {
        String input = """
            CREATE SKILL detect_churn VERSION '1.0.0'
            BEGIN
                PRINT 'Detecting churn';
            END SKILL;
            """;

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);
        assertNotNull(ctx.skill_statement());
        assertNotNull(ctx.skill_statement().create_skill_statement());

        var createCtx = ctx.skill_statement().create_skill_statement();
        assertEquals("detect_churn", createCtx.ID().getText());
    }

    public void testCreateSkillWithDescription() {
        String input = """
            CREATE SKILL analyze_users VERSION '1.0.0'
            DESCRIPTION 'Analyzes user demographics'
            BEGIN
                PRINT 'Analyzing';
            END SKILL;
            """;

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);

        var createCtx = ctx.skill_statement().create_skill_statement();
        assertEquals("analyze_users", createCtx.ID().getText());
        // Description is the second STRING (after VERSION)
        assertEquals(2, createCtx.STRING().size());
    }

    public void testCreateSkillWithAuthor() {
        String input = """
            CREATE SKILL find_customers VERSION '2.0.0'
            DESCRIPTION 'Searches for customers'
            AUTHOR 'Platform Team'
            BEGIN
                PRINT 'Searching';
            END SKILL;
            """;

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);

        var createCtx = ctx.skill_statement().create_skill_statement();
        assertEquals(3, createCtx.STRING().size()); // VERSION, DESCRIPTION, AUTHOR
    }

    public void testCreateSkillWithTags() {
        String input = """
            CREATE SKILL get_system_status VERSION '1.0.0'
            TAGS ['monitoring', 'health']
            BEGIN
                PRINT 'Checking';
            END SKILL;
            """;

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);

        var createCtx = ctx.skill_statement().create_skill_statement();
        assertEquals("get_system_status", createCtx.ID().getText());
        assertNotNull(createCtx.arrayLiteral(0)); // TAGS array
    }

    public void testCreateSkillWithDependencies() {
        String input = """
            CREATE SKILL analyze_logs VERSION '1.0.0'
            REQUIRES ['base_skill', 'helper_skill']
            BEGIN
                PRINT 'Analyzing';
            END SKILL;
            """;

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);

        var createCtx = ctx.skill_statement().create_skill_statement();
        assertNotNull(createCtx.arrayLiteral(0)); // REQUIRES array
    }

    public void testCreateSkillWithParameters() {
        String input = """
            CREATE SKILL parameterized_skill VERSION '1.0.0'
            (severity IN STRING, days IN NUMBER DEFAULT 7)
            BEGIN
                PRINT severity;
            END SKILL;
            """;

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);

        var createCtx = ctx.skill_statement().create_skill_statement();
        assertNotNull(createCtx.skill_parameters_clause());
        assertEquals(2, createCtx.skill_parameters_clause().skill_param_list().skill_param().size());
    }

    public void testCreateSkillWithReturns() {
        String input = """
            CREATE SKILL returning_skill VERSION '1.0.0'
            RETURNS DOCUMENT
            BEGIN
                RETURN {};
            END SKILL;
            """;

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);

        var createCtx = ctx.skill_statement().create_skill_statement();
        assertNotNull(createCtx.RETURNS());
        assertNotNull(createCtx.datatype());
    }

    public void testCreateSkillFull() {
        String input = """
            CREATE SKILL full_skill VERSION '3.0.0'
            DESCRIPTION 'A complete skill'
            AUTHOR 'DevOps'
            TAGS ['ops', 'automation']
            REQUIRES ['base']
            (input_param IN STRING, output_param OUT NUMBER)
            RETURNS DOCUMENT
            BEGIN
                PRINT input_param;
                SET output_param = 42;
                RETURN {"status": "ok"};
            END SKILL;
            """;

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);

        var createCtx = ctx.skill_statement().create_skill_statement();
        assertEquals("full_skill", createCtx.ID().getText());
        assertNotNull(createCtx.skill_parameters_clause());
        assertNotNull(createCtx.RETURNS());
    }

    // ========================================================================
    // DROP SKILL Tests
    // ========================================================================

    public void testDropSkill() {
        String input = "DROP SKILL detect_churn;";

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);
        assertNotNull(ctx.skill_statement().drop_skill_statement());
        assertEquals("detect_churn", ctx.skill_statement().drop_skill_statement().ID().getText());
    }

    // ========================================================================
    // SHOW SKILLS Tests
    // ========================================================================

    public void testShowAllSkills() {
        String input = "SHOW SKILLS;";

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);
        assertNotNull(ctx.skill_statement().show_skills_statement());
        assertTrue(ctx.skill_statement().show_skills_statement() instanceof ElasticScriptParser.ShowAllSkillsContext);
    }

    public void testShowSkillDetail() {
        String input = "SHOW SKILL detect_churn;";

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
        String input = "ALTER SKILL detect_churn SET DESCRIPTION = 'Updated description for churn detection';";

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);
        assertNotNull(ctx.skill_statement().alter_skill_statement());

        var alterCtx = ctx.skill_statement().alter_skill_statement();
        assertEquals("detect_churn", alterCtx.ID().getText());
        assertEquals("DESCRIPTION", alterCtx.skill_property().getText());
    }

    // ========================================================================
    // TEST SKILL Tests
    // ========================================================================

    public void testTestSkillBasic() {
        String input = "TEST SKILL my_skill;";

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);
        assertNotNull(ctx.skill_statement().test_skill_statement());
        assertEquals("my_skill", ctx.skill_statement().test_skill_statement().ID().getText());
    }

    public void testTestSkillWithParams() {
        String input = "TEST SKILL my_skill WITH param1 = 'value1', param2 = 42;";

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);

        var testCtx = ctx.skill_statement().test_skill_statement();
        assertNotNull(testCtx.skill_test_args());
        assertEquals(2, testCtx.skill_test_args().skill_test_arg().size());
    }

    public void testTestSkillWithExpect() {
        String input = "TEST SKILL my_skill WITH x = 1 EXPECT true;";

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);

        var testCtx = ctx.skill_statement().test_skill_statement();
        assertNotNull(testCtx.expression());
    }

    // ========================================================================
    // GENERATE SKILL Tests
    // ========================================================================

    public void testGenerateSkillBasic() {
        String input = "GENERATE SKILL FROM 'Find customers who might leave based on order patterns';";

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);
        assertNotNull(ctx.skill_statement().generate_skill_statement());

        var genCtx = ctx.skill_statement().generate_skill_statement();
        assertEquals(1, genCtx.STRING().size());
    }

    public void testGenerateSkillWithModel() {
        String input = "GENERATE SKILL FROM 'Analyze customer behavior' WITH MODEL 'gpt-4';";

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);

        var genCtx = ctx.skill_statement().generate_skill_statement();
        assertNotNull(genCtx.MODEL());
        assertEquals(2, genCtx.STRING().size());
    }

    public void testGenerateSkillWithSave() {
        String input = "GENERATE SKILL FROM 'Detect anomalies in metrics' SAVE AS anomaly_detector;";

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);

        var genCtx = ctx.skill_statement().generate_skill_statement();
        assertNotNull(genCtx.SAVE_KW());
        assertEquals("anomaly_detector", genCtx.ID().getText());
    }

    public void testGenerateSkillWithModelAndSave() {
        String input = "GENERATE SKILL FROM 'Find trending products' WITH MODEL 'claude-3' SAVE AS trending_products;";

        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertNotNull(ctx);

        var genCtx = ctx.skill_statement().generate_skill_statement();
        assertNotNull(genCtx.MODEL());
        assertNotNull(genCtx.SAVE_KW());
        assertEquals("trending_products", genCtx.ID().getText());
    }
}
