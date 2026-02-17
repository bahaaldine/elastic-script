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

/**
 * Unit tests for SkillDefinition and related classes.
 */
public class SkillRegistryTests extends ESTestCase {

    public void testSkillDefinitionBasic() {
        SkillDefinition skill = new SkillDefinition(
            "detect_churn",
            "Identifies customers at risk of churning",
            List.of(
                new SkillDefinition.SkillParameter("threshold", "NUMBER", "Days threshold", true, null)
            ),
            "ARRAY",
            "run_churn_analysis",
            List.of("threshold")
        );

        assertEquals("detect_churn", skill.getName());
        assertEquals("Identifies customers at risk of churning", skill.getDescription());
        assertEquals("ARRAY", skill.getReturnType());
        assertEquals("run_churn_analysis", skill.getProcedureName());
        assertEquals(1, skill.getParameters().size());
        assertEquals("threshold", skill.getParameters().get(0).getName());
        assertEquals("NUMBER", skill.getParameters().get(0).getType());
        assertTrue(skill.getParameters().get(0).isRequired());
    }

    public void testSkillDefinitionWithExamples() {
        SkillDefinition skill = new SkillDefinition(
            "find_customers",
            "Searches for customers",
            List.of(
                new SkillDefinition.SkillParameter("query", "STRING", null, true, null)
            ),
            "ARRAY",
            "search_customers",
            List.of("query"),
            List.of("Find customers named John", "Search for users", "Look up customer")
        );

        assertEquals(3, skill.getExamples().size());
        assertEquals("Find customers named John", skill.getExamples().get(0));
    }

    public void testSkillDefinitionWithDefaults() {
        SkillDefinition skill = new SkillDefinition(
            "analyze_logs",
            "Analyzes log entries",
            List.of(
                new SkillDefinition.SkillParameter("severity", "STRING", "Log level", true, null),
                new SkillDefinition.SkillParameter("days", "NUMBER", "Days to analyze", false, 7)
            ),
            "ARRAY",
            "log_analysis",
            List.of("severity", "days")
        );

        assertEquals(2, skill.getParameters().size());
        assertTrue(skill.getParameters().get(0).isRequired());
        assertFalse(skill.getParameters().get(1).isRequired());
        assertEquals(7, skill.getParameters().get(1).getDefaultValue());
    }

    public void testMcpToolSpecGeneration() {
        SkillDefinition skill = new SkillDefinition(
            "detect_churn",
            "Identifies churning customers",
            List.of(
                new SkillDefinition.SkillParameter("threshold", "NUMBER", "Days threshold", true, null),
                new SkillDefinition.SkillParameter("limit", "NUMBER", "Max results", false, 100)
            ),
            "ARRAY",
            "run_churn_analysis",
            List.of("threshold", "limit")
        );

        Map<String, Object> mcpSpec = skill.toMcpToolSpec();

        assertEquals("detect_churn", mcpSpec.get("name"));
        assertEquals("Identifies churning customers", mcpSpec.get("description"));
        assertNotNull(mcpSpec.get("inputSchema"));

        @SuppressWarnings("unchecked")
        Map<String, Object> inputSchema = (Map<String, Object>) mcpSpec.get("inputSchema");
        assertEquals("object", inputSchema.get("type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) inputSchema.get("properties");
        assertTrue(properties.containsKey("threshold"));
        assertTrue(properties.containsKey("limit"));

        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) inputSchema.get("required");
        assertTrue(required.contains("threshold"));
        assertFalse(required.contains("limit")); // Has default, not required
    }

    public void testMcpToolSpecWithExamples() {
        SkillDefinition skill = new SkillDefinition(
            "search",
            "Search for items",
            List.of(),
            "ARRAY",
            "search_impl",
            List.of(),
            List.of("Find items", "Search for products")
        );

        Map<String, Object> mcpSpec = skill.toMcpToolSpec();

        @SuppressWarnings("unchecked")
        List<String> examples = (List<String>) mcpSpec.get("examples");
        assertNotNull(examples);
        assertEquals(2, examples.size());
    }

    public void testSkillParameterEquality() {
        SkillDefinition.SkillParameter param1 = new SkillDefinition.SkillParameter(
            "threshold", "NUMBER", "Days threshold", true, null
        );
        SkillDefinition.SkillParameter param2 = new SkillDefinition.SkillParameter(
            "threshold", "NUMBER", "Different description", true, null
        );
        SkillDefinition.SkillParameter param3 = new SkillDefinition.SkillParameter(
            "other", "STRING", null, false, "default"
        );

        assertEquals(param1, param2); // Same name, type, required
        assertNotEquals(param1, param3);
    }

    public void testSkillDefinitionEquality() {
        SkillDefinition skill1 = new SkillDefinition(
            "detect_churn",
            "Description 1",
            List.of(),
            "ARRAY",
            "proc1",
            List.of()
        );
        SkillDefinition skill2 = new SkillDefinition(
            "detect_churn",
            "Description 1",
            List.of(),
            "ARRAY",
            "proc1",
            List.of()
        );
        SkillDefinition skill3 = new SkillDefinition(
            "other_skill",
            "Description 1",
            List.of(),
            "ARRAY",
            "proc1",
            List.of()
        );

        assertEquals(skill1, skill2);
        assertNotEquals(skill1, skill3);
    }

    public void testSkillWithNoParameters() {
        SkillDefinition skill = new SkillDefinition(
            "get_status",
            "Returns system status",
            List.of(),
            "DOCUMENT",
            "check_status",
            List.of()
        );

        assertEquals(0, skill.getParameters().size());
        assertEquals(0, skill.getProcedureArgs().size());

        Map<String, Object> mcpSpec = skill.toMcpToolSpec();
        @SuppressWarnings("unchecked")
        Map<String, Object> inputSchema = (Map<String, Object>) mcpSpec.get("inputSchema");
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) inputSchema.get("properties");
        assertTrue(properties.isEmpty());
    }

    public void testTypeMapping() {
        // Test that different elastic-script types map to correct JSON Schema types
        SkillDefinition skill = new SkillDefinition(
            "test",
            "Test skill",
            List.of(
                new SkillDefinition.SkillParameter("str", "STRING", null, true, null),
                new SkillDefinition.SkillParameter("num", "NUMBER", null, true, null),
                new SkillDefinition.SkillParameter("bool", "BOOLEAN", null, true, null),
                new SkillDefinition.SkillParameter("arr", "ARRAY", null, true, null),
                new SkillDefinition.SkillParameter("doc", "DOCUMENT", null, true, null)
            ),
            "DOCUMENT",
            "test_proc",
            List.of("str", "num", "bool", "arr", "doc")
        );

        Map<String, Object> mcpSpec = skill.toMcpToolSpec();
        @SuppressWarnings("unchecked")
        Map<String, Object> inputSchema = (Map<String, Object>) mcpSpec.get("inputSchema");
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) inputSchema.get("properties");

        @SuppressWarnings("unchecked")
        Map<String, Object> strProp = (Map<String, Object>) properties.get("str");
        assertEquals("string", strProp.get("type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> numProp = (Map<String, Object>) properties.get("num");
        assertEquals("number", numProp.get("type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> boolProp = (Map<String, Object>) properties.get("bool");
        assertEquals("boolean", boolProp.get("type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> arrProp = (Map<String, Object>) properties.get("arr");
        assertEquals("array", arrProp.get("type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> docProp = (Map<String, Object>) properties.get("doc");
        assertEquals("object", docProp.get("type"));
    }

    // === Documentation Generation Tests ===

    public void testSkillDocumentationGenerationBasic() {
        SkillDefinition skill = new SkillDefinition(
            "analyze_logs",
            "Analyzes application logs for errors and patterns",
            List.of(
                new SkillDefinition.SkillParameter("index_pattern", "STRING", "Index pattern to search", false, "logs-*"),
                new SkillDefinition.SkillParameter("limit", "NUMBER", "Maximum results", false, 10)
            ),
            "ARRAY",
            "run_log_analysis",
            List.of("index_pattern", "limit")
        );
        skill.setVersion("1.0.0");
        skill.setAuthor("DevOps Team");
        skill.setTags(List.of("logs", "monitoring", "observability"));

        String documentation = skill.generateDocumentation();

        // Check header
        assertTrue(documentation.contains("# analyze_logs"));

        // Check version badge
        assertTrue(documentation.contains("version-1.0.0"));

        // Check author badge
        assertTrue(documentation.contains("author-DevOps%20Team"));

        // Check description
        assertTrue(documentation.contains("## Description"));
        assertTrue(documentation.contains("Analyzes application logs for errors and patterns"));

        // Check parameters table
        assertTrue(documentation.contains("## Parameters"));
        assertTrue(documentation.contains("| Name | Type | Required | Default | Description |"));
        assertTrue(documentation.contains("`index_pattern`"));
        assertTrue(documentation.contains("`STRING`"));
        assertTrue(documentation.contains("`logs-*`"));

        // Check returns section
        assertTrue(documentation.contains("## Returns"));
        assertTrue(documentation.contains("`ARRAY`"));

        // Check usage section
        assertTrue(documentation.contains("## Usage"));
        assertTrue(documentation.contains("### Via MCP"));
        assertTrue(documentation.contains("### Via elastic-script"));
        assertTrue(documentation.contains("CALL run_log_analysis"));

        // Check implementation section
        assertTrue(documentation.contains("## Implementation"));
        assertTrue(documentation.contains("wraps the `run_log_analysis` procedure"));

        // Check footer
        assertTrue(documentation.contains("Generated by Moltler"));
    }

    public void testSkillDocumentationWithSourceCode() {
        SkillDefinition skill = new SkillDefinition(
            "greet_user",
            "Greets a user by name",
            List.of(
                new SkillDefinition.SkillParameter("name", "STRING", "User name", false, "World")
            ),
            "DOCUMENT",
            "hello_user",
            List.of("name")
        );
        skill.setVersion("1.0.0");
        skill.setSourceCode("CREATE SKILL greet_user\n  VERSION '1.0.0'\n  DESCRIPTION 'Greets a user by name'\nBEGIN\n  CALL hello_user(name);\nEND SKILL;");

        String documentation = skill.generateDocumentation();

        // Check that source code is included
        assertTrue(documentation.contains("CREATE SKILL greet_user"));
        assertTrue(documentation.contains("CALL hello_user"));
    }

    public void testSkillDocumentationWithExamples() {
        SkillDefinition skill = new SkillDefinition(
            "search_products",
            "Search the product catalog",
            List.of(),
            "ARRAY",
            "product_search",
            List.of(),
            List.of("Find products by name", "Search for items", "Look up products")
        );

        String documentation = skill.generateDocumentation();

        // Check examples section
        assertTrue(documentation.contains("## Examples"));
        assertTrue(documentation.contains("Find products by name"));
        assertTrue(documentation.contains("Search for items"));
        assertTrue(documentation.contains("Look up products"));
    }

    public void testSkillDocumentationWithDependencies() {
        SkillDefinition skill = new SkillDefinition(
            "complex_analysis",
            "Complex analysis skill",
            List.of(),
            "DOCUMENT",
            "run_complex_analysis",
            List.of()
        );
        skill.setDependencies(List.of("base_analysis", "data_processor", "reporter"));

        String documentation = skill.generateDocumentation();

        // Check dependencies section
        assertTrue(documentation.contains("## Dependencies"));
        assertTrue(documentation.contains("`base_analysis`"));
        assertTrue(documentation.contains("`data_processor`"));
        assertTrue(documentation.contains("`reporter`"));
    }

    public void testSkillDocumentationMcpUsageFormat() {
        SkillDefinition skill = new SkillDefinition(
            "test_skill",
            "Test skill",
            List.of(
                new SkillDefinition.SkillParameter("str_param", "STRING", null, false, "default"),
                new SkillDefinition.SkillParameter("num_param", "NUMBER", null, false, 42),
                new SkillDefinition.SkillParameter("bool_param", "BOOLEAN", null, false, true)
            ),
            "DOCUMENT",
            "test_proc",
            List.of("str_param", "num_param", "bool_param")
        );

        String documentation = skill.generateDocumentation();

        // Check MCP usage JSON format
        assertTrue(documentation.contains("\"method\": \"tools/call\""));
        assertTrue(documentation.contains("\"name\": \"test_skill\""));
        assertTrue(documentation.contains("\"str_param\": \"default\""));
        assertTrue(documentation.contains("\"num_param\": 42"));
        assertTrue(documentation.contains("\"bool_param\": true"));
    }

    public void testSkillDocumentationWithNoParameters() {
        SkillDefinition skill = new SkillDefinition(
            "get_status",
            "Returns current system status",
            List.of(),
            "DOCUMENT",
            "check_status",
            List.of()
        );

        String documentation = skill.generateDocumentation();

        // Should not have parameters section
        assertFalse(documentation.contains("## Parameters"));
        
        // But should have MCP usage
        assertTrue(documentation.contains("\"arguments\": {}"));
    }

    public void testSkillDocumentationWithNoDescription() {
        SkillDefinition skill = new SkillDefinition(
            "unnamed_skill",
            null, // No description
            List.of(),
            "DOCUMENT",
            "some_proc",
            List.of()
        );

        String documentation = skill.generateDocumentation();

        // Should have fallback description
        assertTrue(documentation.contains("No description provided."));
    }

    public void testSkillExtendedMetadataGettersSetters() {
        SkillDefinition skill = new SkillDefinition(
            "test",
            "Test",
            List.of(),
            "DOCUMENT",
            "test_proc",
            List.of()
        );

        // Test setters and getters
        skill.setVersion("2.0.0");
        assertEquals("2.0.0", skill.getVersion());

        skill.setAuthor("Test Author");
        assertEquals("Test Author", skill.getAuthor());

        skill.setTags(List.of("tag1", "tag2"));
        assertEquals(2, skill.getTags().size());
        assertTrue(skill.getTags().contains("tag1"));

        skill.setDependencies(List.of("dep1"));
        assertEquals(1, skill.getDependencies().size());

        skill.setDocumentation("# Custom Doc");
        assertEquals("# Custom Doc", skill.getDocumentation());

        skill.setSourceCode("CREATE SKILL ...");
        assertEquals("CREATE SKILL ...", skill.getSourceCode());
    }

    public void testSkillEmptyTagsAndDependenciesReturnEmptyList() {
        SkillDefinition skill = new SkillDefinition(
            "test",
            "Test",
            List.of(),
            "DOCUMENT",
            "test_proc",
            List.of()
        );

        // Should return empty lists, not null
        assertNotNull(skill.getTags());
        assertTrue(skill.getTags().isEmpty());

        assertNotNull(skill.getDependencies());
        assertTrue(skill.getDependencies().isEmpty());
    }
}
