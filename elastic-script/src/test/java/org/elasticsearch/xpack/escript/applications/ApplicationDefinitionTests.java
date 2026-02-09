/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.applications;

import org.elasticsearch.test.ESTestCase;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit tests for ApplicationDefinition, SkillDefinition, and IntentDefinition.
 */
public class ApplicationDefinitionTests extends ESTestCase {

    public void testApplicationDefinitionBuilder() {
        ApplicationDefinition app = ApplicationDefinition.builder()
            .name("customer_analytics")
            .description("Customer Analytics Application")
            .version("1.0.0")
            .sources(List.of(
                new ApplicationDefinition.SourceDefinition("orders", "orders-*"),
                new ApplicationDefinition.SourceDefinition("customers", "customers-*")
            ))
            .build();

        assertThat(app.getName(), equalTo("customer_analytics"));
        assertThat(app.getDescription(), equalTo("Customer Analytics Application"));
        assertThat(app.getVersion(), equalTo("1.0.0"));
        assertThat(app.getSources(), hasSize(2));
        assertThat(app.getStatus(), equalTo(ApplicationDefinition.Status.INSTALLED));
        assertThat(app.getCreatedAt(), notNullValue());
    }

    public void testApplicationDefinitionWithSkills() {
        SkillDefinition detectChurn = new SkillDefinition(
            "detect_churn",
            "Identifies customers likely to churn",
            List.of(
                new SkillDefinition.SkillParameter("threshold", "NUMBER", "Churn probability threshold", true, 0.7),
                new SkillDefinition.SkillParameter("timeframe", "STRING", "Analysis timeframe", false, "30d")
            ),
            "ARRAY",
            "run_churn_analysis",
            List.of("threshold", "timeframe")
        );

        ApplicationDefinition app = ApplicationDefinition.builder()
            .name("customer_analytics")
            .skills(List.of(detectChurn))
            .build();

        assertThat(app.getSkills(), hasSize(1));
        SkillDefinition found = app.findSkill("detect_churn");
        assertThat(found, notNullValue());
        assertThat(found.getDescription(), equalTo("Identifies customers likely to churn"));
        assertThat(found.getParameters(), hasSize(2));
        assertThat(found.getReturnType(), equalTo("ARRAY"));
    }

    public void testApplicationDefinitionWithIntents() {
        IntentDefinition churnIntent = new IntentDefinition(
            "churn|leaving|at risk|might leave",
            "detect_churn",
            "Find customers who might churn",
            null
        );

        ApplicationDefinition app = ApplicationDefinition.builder()
            .name("customer_analytics")
            .intents(List.of(churnIntent))
            .build();

        assertThat(app.getIntents(), hasSize(1));
        
        // Test intent matching
        ApplicationDefinition.IntentMatch match = app.matchIntent("Show me customers who might be leaving");
        assertThat(match, notNullValue());
        assertThat(match.getIntent().getTargetSkill(), equalTo("detect_churn"));
        assertThat(match.getConfidence() > 0, equalTo(true));

        // Test no match
        ApplicationDefinition.IntentMatch noMatch = app.matchIntent("What is the weather today");
        assertThat(noMatch, nullValue());
    }

    public void testIntentMatchingPatterns() {
        IntentDefinition intent = new IntentDefinition(
            "churn|leaving|at risk",
            "detect_churn",
            null,
            null
        );

        // Should match
        assertThat(intent.matches("Find churning customers"), equalTo(true));
        assertThat(intent.matches("Show customers leaving"), equalTo(true));
        assertThat(intent.matches("Customers at risk"), equalTo(true));
        
        // Case insensitive
        assertThat(intent.matches("FIND CHURNING CUSTOMERS"), equalTo(true));
        
        // Should not match
        assertThat(intent.matches("Show sales data"), equalTo(false));
        assertThat(intent.matches(""), equalTo(false));
    }

    public void testSkillMcpToolSpec() {
        SkillDefinition skill = new SkillDefinition(
            "analyze_sales",
            "Analyzes sales patterns and trends",
            List.of(
                new SkillDefinition.SkillParameter("region", "STRING", "Geographic region", true, null),
                new SkillDefinition.SkillParameter("limit", "NUMBER", "Max results", false, 10)
            ),
            "ARRAY",
            "sales_analysis_proc",
            List.of("region", "limit")
        );

        Map<String, Object> mcpSpec = skill.toMcpToolSpec();
        
        assertThat(mcpSpec.get("name"), equalTo("analyze_sales"));
        assertThat(mcpSpec.get("description"), equalTo("Analyzes sales patterns and trends"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> inputSchema = (Map<String, Object>) mcpSpec.get("inputSchema");
        assertThat(inputSchema.get("type"), equalTo("object"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) inputSchema.get("properties");
        assertThat(properties.containsKey("region"), equalTo(true));
        assertThat(properties.containsKey("limit"), equalTo(true));
        
        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) inputSchema.get("required");
        assertThat(required.contains("region"), equalTo(true));
        assertThat(required.contains("limit"), equalTo(false));
    }

    public void testApplicationToBuilder() {
        ApplicationDefinition original = ApplicationDefinition.builder()
            .name("test_app")
            .version("1.0.0")
            .description("Original description")
            .status(ApplicationDefinition.Status.INSTALLED)
            .build();

        ApplicationDefinition modified = original.toBuilder()
            .description("Modified description")
            .status(ApplicationDefinition.Status.RUNNING)
            .build();

        // Original unchanged
        assertThat(original.getDescription(), equalTo("Original description"));
        assertThat(original.getStatus(), equalTo(ApplicationDefinition.Status.INSTALLED));

        // Modified has new values
        assertThat(modified.getName(), equalTo("test_app"));
        assertThat(modified.getDescription(), equalTo("Modified description"));
        assertThat(modified.getStatus(), equalTo(ApplicationDefinition.Status.RUNNING));
    }

    public void testSourceDefinition() {
        ApplicationDefinition.SourceDefinition source = 
            new ApplicationDefinition.SourceDefinition("logs", "logs-*");
        
        assertThat(source.getName(), equalTo("logs"));
        assertThat(source.getIndexPattern(), equalTo("logs-*"));
    }

    public void testIntentMatchScore() {
        IntentDefinition intent = new IntentDefinition(
            "high|priority|urgent|critical",
            "handle_priority",
            null,
            null
        );

        // Multiple keywords match = higher score
        double scoreMultiple = intent.matchScore("high priority urgent issue");
        double scoreSingle = intent.matchScore("this is urgent");
        
        assertThat(scoreMultiple > scoreSingle, equalTo(true));
    }
}
