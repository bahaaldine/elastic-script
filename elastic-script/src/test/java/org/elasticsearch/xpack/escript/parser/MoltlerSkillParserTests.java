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
 * Tests for Moltler skill statement parsing with versioning and metadata.
 */
public class MoltlerSkillParserTests extends ESTestCase {
    
    private ElasticScriptParser createParser(String input) {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new ElasticScriptParser(tokens);
    }
    
    public void testCreateSkillWithVersion() {
        String input = """
            CREATE SKILL analyze_logs VERSION '1.0.0'
            BEGIN
                PRINT 'Analyzing logs';
            END SKILL;
            """;
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.skill_statement());
        assertNotNull(ctx.skill_statement().create_skill_statement());
        assertEquals("analyze_logs", ctx.skill_statement().create_skill_statement().ID().getText());
    }
    
    public void testCreateSkillWithDescription() {
        String input = """
            CREATE SKILL check_health VERSION '1.0.0' DESCRIPTION 'Check system health'
            BEGIN
                PRINT 'Checking';
            END SKILL;
            """;
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.skill_statement().create_skill_statement());
    }
    
    public void testCreateSkillWithAuthor() {
        String input = """
            CREATE SKILL my_skill VERSION '2.0.0' DESCRIPTION 'My skill' AUTHOR 'DevOps Team'
            BEGIN
                PRINT 'Running';
            END SKILL;
            """;
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.skill_statement().create_skill_statement());
    }
    
    public void testCreateSkillWithTags() {
        String input = """
            CREATE SKILL tagged_skill VERSION '1.0.0' TAGS ['observability', 'logs', 'monitoring']
            BEGIN
                PRINT 'Tagged';
            END SKILL;
            """;
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.skill_statement().create_skill_statement());
    }
    
    public void testCreateSkillWithDependencies() {
        String input = """
            CREATE SKILL dependent_skill VERSION '1.0.0' REQUIRES ['base_skill', 'helper_skill']
            BEGIN
                PRINT 'Has dependencies';
            END SKILL;
            """;
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.skill_statement().create_skill_statement());
    }
    
    public void testCreateSkillWithParameters() {
        String input = """
            CREATE SKILL param_skill VERSION '1.0.0' (index IN STRING, threshold IN NUMBER DEFAULT 0.9)
            BEGIN
                PRINT index;
            END SKILL;
            """;
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.skill_statement().create_skill_statement());
        assertNotNull(ctx.skill_statement().create_skill_statement().skill_parameters_clause());
    }
    
    public void testCreateSkillWithReturns() {
        String input = """
            CREATE SKILL returning_skill VERSION '1.0.0' RETURNS DOCUMENT
            BEGIN
                RETURN {};
            END SKILL;
            """;
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.skill_statement().create_skill_statement());
    }
    
    public void testCreateSkillFull() {
        String input = """
            CREATE SKILL full_skill
                VERSION '3.0.0'
                DESCRIPTION 'A complete skill example'
                AUTHOR 'Platform Team'
                TAGS ['operations', 'automation']
                REQUIRES ['base_skill']
                (input_param IN STRING, output_param OUT NUMBER)
                RETURNS DOCUMENT
            BEGIN
                PRINT input_param;
                SET output_param = 42;
                RETURN {"status": "ok"};
            END SKILL;
            """;
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.skill_statement().create_skill_statement());
    }
    
    public void testDropSkill() {
        String input = "DROP SKILL my_skill;";
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.skill_statement());
        assertNotNull(ctx.skill_statement().drop_skill_statement());
    }
    
    public void testShowSkills() {
        String input = "SHOW SKILLS;";
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.skill_statement());
        assertNotNull(ctx.skill_statement().show_skills_statement());
    }
    
    public void testShowSkillDetail() {
        String input = "SHOW SKILL my_skill;";
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.skill_statement());
        assertNotNull(ctx.skill_statement().show_skills_statement());
    }
    
    public void testTestSkill() {
        String input = "TEST SKILL my_skill WITH param1 = 'value1' EXPECT true;";
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.skill_statement());
        assertNotNull(ctx.skill_statement().test_skill_statement());
    }
    
    public void testTestSkillWithMultipleParams() {
        String input = "TEST SKILL complex_skill WITH input = 'test', threshold = 0.8, enabled = true EXPECT {\"status\": \"ok\"};";
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.skill_statement().test_skill_statement());
    }
    
    public void testGenerateSkill() {
        String input = "GENERATE SKILL FROM 'Monitor disk usage and alert when over 80%' AS disk_monitor;";
        ElasticScriptParser parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.skill_statement());
        assertNotNull(ctx.skill_statement().generate_skill_statement());
    }
}
