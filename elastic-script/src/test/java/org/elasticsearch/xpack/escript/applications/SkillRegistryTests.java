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
}
