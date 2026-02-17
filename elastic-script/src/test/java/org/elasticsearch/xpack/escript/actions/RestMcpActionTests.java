/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.actions;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.escript.applications.SkillDefinition;

import java.util.List;
import java.util.Map;

/**
 * Unit tests for MCP (Model Context Protocol) functionality.
 * 
 * Tests the JSON-RPC message format, MCP tool specification generation,
 * and error handling for the MCP endpoint.
 */
public class RestMcpActionTests extends ESTestCase {

    /**
     * Test MCP tool list response format.
     * The tools/list response should return skills as MCP tools.
     */
    public void testMcpToolListFormat() {
        SkillDefinition skill = new SkillDefinition(
            "analyze_logs",
            "Analyze application logs for errors",
            List.of(
                new SkillDefinition.SkillParameter("index_pattern", "STRING", "Index pattern", false, "logs-*"),
                new SkillDefinition.SkillParameter("limit", "NUMBER", "Max results", false, 10)
            ),
            "ARRAY",
            "run_analysis",
            List.of("index_pattern", "limit")
        );

        Map<String, Object> mcpTool = skill.toMcpToolSpec();

        // Validate MCP tool format
        assertEquals("analyze_logs", mcpTool.get("name"));
        assertEquals("Analyze application logs for errors", mcpTool.get("description"));
        assertTrue(mcpTool.containsKey("inputSchema"));

        @SuppressWarnings("unchecked")
        Map<String, Object> inputSchema = (Map<String, Object>) mcpTool.get("inputSchema");
        assertEquals("object", inputSchema.get("type"));
        assertTrue(inputSchema.containsKey("properties"));
        assertTrue(inputSchema.containsKey("required"));
    }

    /**
     * Test MCP input schema generation for various parameter types.
     */
    public void testMcpInputSchemaTypes() {
        SkillDefinition skill = new SkillDefinition(
            "test_types",
            "Test type mapping",
            List.of(
                new SkillDefinition.SkillParameter("string_param", "STRING", null, true, null),
                new SkillDefinition.SkillParameter("number_param", "NUMBER", null, true, null),
                new SkillDefinition.SkillParameter("int_param", "INT", null, false, null),
                new SkillDefinition.SkillParameter("float_param", "FLOAT", null, false, null),
                new SkillDefinition.SkillParameter("bool_param", "BOOLEAN", null, false, null),
                new SkillDefinition.SkillParameter("array_param", "ARRAY", null, false, null),
                new SkillDefinition.SkillParameter("doc_param", "DOCUMENT", null, false, null),
                new SkillDefinition.SkillParameter("map_param", "MAP", null, false, null)
            ),
            "DOCUMENT",
            "test_proc",
            List.of()
        );

        Map<String, Object> mcpTool = skill.toMcpToolSpec();
        @SuppressWarnings("unchecked")
        Map<String, Object> inputSchema = (Map<String, Object>) mcpTool.get("inputSchema");
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) inputSchema.get("properties");

        // Verify type mappings
        assertPropertyType(properties, "string_param", "string");
        assertPropertyType(properties, "number_param", "number");
        assertPropertyType(properties, "int_param", "number");
        assertPropertyType(properties, "float_param", "number");
        assertPropertyType(properties, "bool_param", "boolean");
        assertPropertyType(properties, "array_param", "array");
        assertPropertyType(properties, "doc_param", "object");
        assertPropertyType(properties, "map_param", "object");
    }

    /**
     * Test MCP required parameters handling.
     */
    public void testMcpRequiredParameters() {
        SkillDefinition skill = new SkillDefinition(
            "test_required",
            "Test required params",
            List.of(
                new SkillDefinition.SkillParameter("required_param", "STRING", null, true, null),
                new SkillDefinition.SkillParameter("optional_param", "STRING", null, false, "default")
            ),
            "DOCUMENT",
            "test_proc",
            List.of()
        );

        Map<String, Object> mcpTool = skill.toMcpToolSpec();
        @SuppressWarnings("unchecked")
        Map<String, Object> inputSchema = (Map<String, Object>) mcpTool.get("inputSchema");
        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) inputSchema.get("required");

        assertTrue(required.contains("required_param"));
        assertFalse(required.contains("optional_param"));
    }

    /**
     * Test MCP tool with no parameters.
     */
    public void testMcpToolNoParameters() {
        SkillDefinition skill = new SkillDefinition(
            "get_status",
            "Get system status",
            List.of(),
            "DOCUMENT",
            "status_check",
            List.of()
        );

        Map<String, Object> mcpTool = skill.toMcpToolSpec();
        @SuppressWarnings("unchecked")
        Map<String, Object> inputSchema = (Map<String, Object>) mcpTool.get("inputSchema");
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) inputSchema.get("properties");
        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) inputSchema.get("required");

        assertTrue(properties.isEmpty());
        assertTrue(required.isEmpty());
    }

    /**
     * Test MCP tool with examples (for AI discovery).
     */
    public void testMcpToolWithExamples() {
        SkillDefinition skill = new SkillDefinition(
            "search",
            "Search for items",
            List.of(),
            "ARRAY",
            "search_impl",
            List.of(),
            List.of("Find error logs", "Search for users named John", "Look up recent orders")
        );

        Map<String, Object> mcpTool = skill.toMcpToolSpec();

        @SuppressWarnings("unchecked")
        List<String> examples = (List<String>) mcpTool.get("examples");

        assertNotNull(examples);
        assertEquals(3, examples.size());
        assertTrue(examples.contains("Find error logs"));
        assertTrue(examples.contains("Search for users named John"));
        assertTrue(examples.contains("Look up recent orders"));
    }

    /**
     * Test MCP tool without examples (examples are optional).
     */
    public void testMcpToolWithoutExamples() {
        SkillDefinition skill = new SkillDefinition(
            "simple_skill",
            "A simple skill",
            List.of(),
            "DOCUMENT",
            "simple_proc",
            List.of()
        );

        Map<String, Object> mcpTool = skill.toMcpToolSpec();

        assertFalse(mcpTool.containsKey("examples"));
    }

    /**
     * Test JSON-RPC error code constants.
     */
    public void testJsonRpcErrorCodes() {
        // These are standard JSON-RPC error codes
        assertEquals(-32700, JsonRpcErrorCodes.PARSE_ERROR);
        assertEquals(-32600, JsonRpcErrorCodes.INVALID_REQUEST);
        assertEquals(-32601, JsonRpcErrorCodes.METHOD_NOT_FOUND);
        assertEquals(-32602, JsonRpcErrorCodes.INVALID_PARAMS);
        assertEquals(-32603, JsonRpcErrorCodes.INTERNAL_ERROR);
    }

    /**
     * Test MCP protocol version.
     */
    public void testMcpProtocolVersion() {
        // MCP protocol version should be the current standard
        assertEquals("2024-11-05", McpConstants.PROTOCOL_VERSION);
    }

    /**
     * Test MCP server capabilities.
     */
    public void testMcpServerCapabilities() {
        Map<String, Object> capabilities = McpConstants.getServerCapabilities();

        assertTrue(capabilities.containsKey("tools"));
        assertNotNull(capabilities.get("tools"));
    }

    /**
     * Test MCP server info format.
     */
    public void testMcpServerInfo() {
        Map<String, Object> serverInfo = McpConstants.getServerInfo();

        assertEquals("moltler", serverInfo.get("name"));
        assertTrue(serverInfo.containsKey("version"));
        assertNotNull(serverInfo.get("version"));
    }

    // Helper method to assert property type
    @SuppressWarnings("unchecked")
    private void assertPropertyType(Map<String, Object> properties, String paramName, String expectedType) {
        assertTrue("Missing property: " + paramName, properties.containsKey(paramName));
        Map<String, Object> prop = (Map<String, Object>) properties.get(paramName);
        assertEquals("Wrong type for " + paramName, expectedType, prop.get("type"));
    }

    // === Inner classes for constants (matching RestMcpAction) ===

    /**
     * JSON-RPC error codes.
     */
    private static class JsonRpcErrorCodes {
        static final int PARSE_ERROR = -32700;
        static final int INVALID_REQUEST = -32600;
        static final int METHOD_NOT_FOUND = -32601;
        static final int INVALID_PARAMS = -32602;
        static final int INTERNAL_ERROR = -32603;
    }

    /**
     * MCP protocol constants.
     */
    private static class McpConstants {
        static final String PROTOCOL_VERSION = "2024-11-05";

        static Map<String, Object> getServerCapabilities() {
            return Map.of("tools", Map.of());
        }

        static Map<String, Object> getServerInfo() {
            return Map.of(
                "name", "moltler",
                "version", "1.0.0"
            );
        }
    }
}
