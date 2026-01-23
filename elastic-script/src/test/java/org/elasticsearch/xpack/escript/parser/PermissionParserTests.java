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
import org.junit.Test;

/**
 * Parser tests for permission/security syntax.
 *
 * Tests:
 * - GRANT statements
 * - REVOKE statements
 * - CREATE ROLE / DROP ROLE
 * - SHOW PERMISSIONS
 */
public class PermissionParserTests extends ESTestCase {

    private ElasticScriptParser createParser(String input) {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new ElasticScriptParser(tokens);
    }

    // ==================== GRANT Tests ====================

    @Test
    public void testParseGrantExecuteOnProcedure() {
        String input = "GRANT EXECUTE ON PROCEDURE my_procedure TO ROLE admin_role";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseGrantExecuteOnFunction() {
        String input = "GRANT EXECUTE ON FUNCTION calculate_total TO ROLE operators";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseGrantExecuteOnPackage() {
        String input = "GRANT EXECUTE ON PACKAGE utils_pkg TO ROLE developers";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseGrantToUser() {
        String input = "GRANT EXECUTE ON PROCEDURE backup_data TO USER 'john.doe@example.com'";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseGrantAllPrivileges() {
        String input = "GRANT ALL PRIVILEGES ON PACKAGE admin_tools TO ROLE superadmin";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseGrantOnJob() {
        String input = "GRANT EXECUTE ON JOB daily_cleanup TO ROLE scheduler";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseGrantOnTrigger() {
        String input = "GRANT EXECUTE ON TRIGGER alert_trigger TO ROLE monitoring";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== REVOKE Tests ====================

    @Test
    public void testParseRevokeExecute() {
        String input = "REVOKE EXECUTE ON PROCEDURE sensitive_proc FROM ROLE guest";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseRevokeFromUser() {
        String input = "REVOKE EXECUTE ON FUNCTION calc FROM USER 'former.employee@example.com'";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseRevokeAllPrivileges() {
        String input = "REVOKE ALL PRIVILEGES ON PACKAGE admin_tools FROM ROLE deprecated_role";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== CREATE ROLE Tests ====================

    @Test
    public void testParseCreateRole() {
        String input = "CREATE ROLE data_analysts";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseCreateRoleWithDescription() {
        String input = "CREATE ROLE operators DESCRIPTION 'Operations team role for runbook execution'";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== DROP ROLE Tests ====================

    @Test
    public void testParseDropRole() {
        String input = "DROP ROLE obsolete_role";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    // ==================== SHOW Tests ====================

    @Test
    public void testParseShowPermissions() {
        String input = "SHOW PERMISSIONS";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseShowPermissionsForRole() {
        String input = "SHOW PERMISSIONS FOR ROLE admin_role";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseShowPermissionsForUser() {
        String input = "SHOW PERMISSIONS FOR USER 'john@example.com'";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }

    @Test
    public void testParseShowRole() {
        String input = "SHOW ROLE admin_role";
        ElasticScriptParser parser = createParser(input);
        ElasticScriptParser.ProgramContext program = parser.program();

        assertNotNull("Program should parse successfully", program);
        assertEquals("Should have no syntax errors", 0, parser.getNumberOfSyntaxErrors());
    }
}
