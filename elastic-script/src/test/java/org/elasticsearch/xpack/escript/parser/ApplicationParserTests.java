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
import org.elasticsearch.xpack.escript.handlers.ElasticScriptErrorListener;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for APPLICATION grammar parsing.
 */
public class ApplicationParserTests extends ESTestCase {

    private ElasticScriptParser.ProgramContext parseProgram(String input) {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);
        
        // Add error listener that throws on syntax errors
        parser.removeErrorListeners();
        parser.addErrorListener(new ElasticScriptErrorListener());
        
        return parser.program();
    }

    public void testCreateApplicationBasic() {
        String input = """
            CREATE APPLICATION customer_analytics
            DESCRIPTION 'Customer Analytics Application'
            VERSION '1.0.0'
            END APPLICATION
            """;
        
        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertThat(ctx.application_statement(), notNullValue());
        assertThat(ctx.application_statement().create_application_statement(), notNullValue());
        
        ElasticScriptParser.Create_application_statementContext createCtx = 
            ctx.application_statement().create_application_statement();
        assertThat(createCtx.ID().getText(), equalTo("customer_analytics"));
    }

    public void testCreateApplicationWithSources() {
        String input = """
            CREATE APPLICATION log_monitor
            SOURCES (
                logs FROM 'logs-*',
                metrics FROM 'metrics-*'
            )
            END APPLICATION
            """;
        
        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertThat(ctx.application_statement(), notNullValue());
        
        ElasticScriptParser.Create_application_statementContext createCtx = 
            ctx.application_statement().create_application_statement();
        assertThat(createCtx.application_section(), hasSize(1));
        assertThat(createCtx.application_section(0).sources_section(), notNullValue());
        assertThat(createCtx.application_section(0).sources_section().source_definition(), hasSize(2));
    }

    public void testCreateApplicationWithSkills() {
        String input = """
            CREATE APPLICATION customer_analytics
            SKILLS (
                detect_churn(threshold NUMBER) RETURNS ARRAY DESCRIPTION 'Find churning customers' AS run_churn(threshold)
            )
            END APPLICATION
            """;
        
        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertThat(ctx.application_statement(), notNullValue());
        
        ElasticScriptParser.Create_application_statementContext createCtx = 
            ctx.application_statement().create_application_statement();
        assertThat(createCtx.application_section(0).skills_section(), notNullValue());
        assertThat(createCtx.application_section(0).skills_section().skill_definition(), hasSize(1));
    }

    public void testCreateApplicationWithIntents() {
        String input = """
            CREATE APPLICATION customer_analytics
            INTENTS (
                'churn|leaving|at risk' => detect_churn,
                'sales|revenue|orders' => analyze_sales
            )
            END APPLICATION
            """;
        
        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertThat(ctx.application_statement(), notNullValue());
        
        ElasticScriptParser.Create_application_statementContext createCtx = 
            ctx.application_statement().create_application_statement();
        assertThat(createCtx.application_section(0).intents_section(), notNullValue());
        assertThat(createCtx.application_section(0).intents_section().intent_mapping(), hasSize(2));
    }

    public void testInstallApplication() {
        String input = "INSTALL APPLICATION customer_analytics";
        
        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertThat(ctx.application_statement(), notNullValue());
        assertThat(ctx.application_statement().install_application_statement(), notNullValue());
    }

    public void testInstallApplicationWithConfig() {
        String input = """
            INSTALL APPLICATION customer_analytics
            CONFIG (
                orders_index => 'my-orders-*',
                threshold => 0.8
            )
            """;
        
        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertThat(ctx.application_statement(), notNullValue());
        
        ElasticScriptParser.Install_application_statementContext installCtx = 
            ctx.application_statement().install_application_statement();
        assertThat(installCtx.config_item(), hasSize(2));
    }

    public void testDropApplication() {
        String input = "DROP APPLICATION customer_analytics";
        
        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertThat(ctx.application_statement(), notNullValue());
        assertThat(ctx.application_statement().drop_application_statement(), notNullValue());
        assertThat(ctx.application_statement().drop_application_statement().ID().getText(), 
            equalTo("customer_analytics"));
    }

    public void testAlterApplicationEnable() {
        String input = "ALTER APPLICATION customer_analytics ENABLE";
        
        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertThat(ctx.application_statement(), notNullValue());
        assertThat(ctx.application_statement().alter_application_statement(), notNullValue());
    }

    public void testAlterApplicationDisable() {
        String input = "ALTER APPLICATION customer_analytics DISABLE";
        
        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertThat(ctx.application_statement(), notNullValue());
        assertThat(ctx.application_statement().alter_application_statement(), notNullValue());
    }

    public void testShowApplications() {
        String input = "SHOW APPLICATIONS";
        
        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertThat(ctx.application_statement(), notNullValue());
        assertThat(ctx.application_statement().show_applications_statement(), notNullValue());
    }

    public void testShowApplicationDetail() {
        String input = "SHOW APPLICATION customer_analytics";
        
        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertThat(ctx.application_statement(), notNullValue());
        assertThat(ctx.application_statement().show_applications_statement(), notNullValue());
    }

    public void testShowApplicationSkills() {
        String input = "SHOW APPLICATION customer_analytics SKILLS";
        
        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertThat(ctx.application_statement(), notNullValue());
        assertThat(ctx.application_statement().show_applications_statement(), notNullValue());
    }

    public void testShowApplicationIntents() {
        String input = "SHOW APPLICATION customer_analytics INTENTS";
        
        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertThat(ctx.application_statement(), notNullValue());
        assertThat(ctx.application_statement().show_applications_statement(), notNullValue());
    }

    public void testApplicationPipeStatus() {
        String input = "APPLICATION customer_analytics | STATUS";
        
        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertThat(ctx.application_statement(), notNullValue());
        assertThat(ctx.application_statement().application_control_statement(), notNullValue());
    }

    public void testApplicationPipePause() {
        String input = "APPLICATION customer_analytics | PAUSE";
        
        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertThat(ctx.application_statement(), notNullValue());
        assertThat(ctx.application_statement().application_control_statement(), notNullValue());
    }

    public void testApplicationPipeResume() {
        String input = "APPLICATION customer_analytics | RESUME";
        
        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertThat(ctx.application_statement(), notNullValue());
        assertThat(ctx.application_statement().application_control_statement(), notNullValue());
    }

    public void testExtendApplicationAddSkill() {
        String input = """
            EXTEND APPLICATION customer_analytics
            ADD SKILL analyze_region(region STRING) RETURNS ARRAY DESCRIPTION 'Analyze by region' AS region_analysis(region)
            """;
        
        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertThat(ctx.application_statement(), notNullValue());
        assertThat(ctx.application_statement().extend_application_statement(), notNullValue());
    }

    public void testExtendApplicationAddIntent() {
        String input = """
            EXTEND APPLICATION customer_analytics
            ADD INTENT 'region|area|territory' => analyze_region
            """;
        
        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertThat(ctx.application_statement(), notNullValue());
        assertThat(ctx.application_statement().extend_application_statement(), notNullValue());
    }

    public void testCompleteApplication() {
        String input = """
            CREATE APPLICATION customer_analytics
            DESCRIPTION 'Complete customer analytics platform'
            VERSION '2.0.0'
            SOURCES (
                orders FROM 'orders-*',
                customers FROM 'customers-*'
            )
            SKILLS (
                detect_churn(threshold NUMBER) RETURNS ARRAY DESCRIPTION 'Find churning customers' AS run_churn_analysis(threshold),
                analyze_sales(region STRING) RETURNS DOCUMENT DESCRIPTION 'Sales analysis' AS sales_report(region)
            )
            INTENTS (
                'churn|leaving|at risk' => detect_churn,
                'sales|revenue|orders' => analyze_sales
            )
            END APPLICATION
            """;
        
        ElasticScriptParser.ProgramContext ctx = parseProgram(input);
        assertThat(ctx.application_statement(), notNullValue());
        
        ElasticScriptParser.Create_application_statementContext createCtx = 
            ctx.application_statement().create_application_statement();
        assertThat(createCtx.ID().getText(), equalTo("customer_analytics"));
        assertThat(createCtx.application_section(), hasSize(3));
    }
}
