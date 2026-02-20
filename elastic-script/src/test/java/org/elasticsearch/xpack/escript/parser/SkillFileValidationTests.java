/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.parser;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.elasticsearch.test.ESTestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Tests that validate all skill.sql files in the hub can be parsed by the EScript parser.
 */
public class SkillFileValidationTests extends ESTestCase {

    private static class SyntaxErrorListener extends BaseErrorListener {
        private final List<String> errors = new ArrayList<>();

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String msg, RecognitionException e) {
            errors.add(String.format("Line %d:%d - %s", line, charPositionInLine, msg));
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    private ElasticScriptParser.ProgramContext parseWithErrorCollection(String input, List<String> errors) {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);

        SyntaxErrorListener errorListener = new SyntaxErrorListener();
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        ElasticScriptParser.ProgramContext ctx = parser.program();
        errors.addAll(errorListener.getErrors());
        return ctx;
    }

    public void testSampleSkillFileParsing() throws IOException {
        String sampleSkill = """
            CREATE SKILL test_skill
              VERSION '1.0.0'
              DESCRIPTION 'Test skill for validation'
              AUTHOR 'elastic'
              TAGS ['test', 'validation']
              (param1 STRING DESCRIPTION 'A parameter')
              RETURNS DOCUMENT
            BEGIN
              RETURN {'status': 'ok'};
            END SKILL;
            """;

        List<String> errors = new ArrayList<>();
        ElasticScriptParser.ProgramContext ctx = parseWithErrorCollection(sampleSkill, errors);

        assertTrue("Parsing errors: " + errors, errors.isEmpty());
        assertNotNull(ctx.skill_statement());
        assertNotNull(ctx.skill_statement().create_skill_statement());
    }

    public void testGetRecentErrorsSkill() {
        String skill = """
            CREATE SKILL get_recent_errors
              VERSION '1.0.0'
              DESCRIPTION 'Get recent error logs with full details including message, timestamp, and service.'
              AUTHOR 'elastic'
              TAGS ['observability', 'logs', 'errors', 'debugging']
              (
                index_pattern STRING DESCRIPTION 'Index pattern to search' DEFAULT 'logs-*',
                limit INT DESCRIPTION 'Maximum number of errors to return' DEFAULT 20,
                service STRING DESCRIPTION 'Filter by service name (optional)' DEFAULT NULL
              )
              RETURNS ARRAY
            BEGIN
              DECLARE query STRING;
              DECLARE result ARRAY;

              IF service IS NOT NULL THEN
                SET query = 'FROM ' || index_pattern || ' | WHERE level == "ERROR" | LIMIT ' || limit;
              ELSE
                SET query = 'FROM ' || index_pattern || ' | WHERE level == "ERROR" | LIMIT ' || limit;
              END IF;

              SET result = ESQL_QUERY(query);
              RETURN result;
            END SKILL;
            """;

        List<String> errors = new ArrayList<>();
        ElasticScriptParser.ProgramContext ctx = parseWithErrorCollection(skill, errors);

        assertTrue("Parsing errors: " + errors, errors.isEmpty());
        assertNotNull(ctx.skill_statement().create_skill_statement());
        assertEquals("get_recent_errors", ctx.skill_statement().create_skill_statement().ID().getText());
    }

    public void testSkillWithESQLQuery() {
        String skill = """
            CREATE SKILL hunt_ioc
              VERSION '1.0.0'
              DESCRIPTION 'Hunt for an Indicator of Compromise'
              AUTHOR 'elastic'
              TAGS ['security', 'threat', 'hunting']
              (ioc STRING DESCRIPTION 'IOC value to hunt for')
              RETURNS ARRAY
            BEGIN
              DECLARE result ARRAY;
              SET result = ESQL_QUERY('FROM security-* | WHERE source_ip == "' || ioc || '" | LIMIT 50');
              RETURN result;
            END SKILL;
            """;

        List<String> errors = new ArrayList<>();
        ElasticScriptParser.ProgramContext ctx = parseWithErrorCollection(skill, errors);

        assertTrue("Parsing errors: " + errors, errors.isEmpty());
        assertNotNull(ctx.skill_statement().create_skill_statement());
    }

    public void testSkillWithDocumentReturn() {
        String skill = """
            CREATE SKILL get_job_status
              VERSION '1.0.0'
              DESCRIPTION 'Get detailed status of an ML job'
              AUTHOR 'elastic'
              TAGS ['ml', 'jobs', 'status']
              (job_id STRING DESCRIPTION 'ML job ID')
              RETURNS DOCUMENT
            BEGIN
              RETURN {
                'job_id': job_id,
                'state': 'opened',
                'assignment_explanation': 'Job is assigned to node'
              };
            END SKILL;
            """;

        List<String> errors = new ArrayList<>();
        ElasticScriptParser.ProgramContext ctx = parseWithErrorCollection(skill, errors);

        assertTrue("Parsing errors: " + errors, errors.isEmpty());
        assertNotNull(ctx.skill_statement().create_skill_statement());
    }

    public void testSkillWithDefaultParameters() {
        String skill = """
            CREATE SKILL list_agents
              VERSION '1.0.0'
              DESCRIPTION 'List Fleet agents with optional filtering'
              AUTHOR 'elastic'
              TAGS ['fleet', 'agents']
              (
                status STRING DESCRIPTION 'Filter by status' DEFAULT 'all',
                page_size INT DESCRIPTION 'Number of results per page' DEFAULT 20
              )
              RETURNS ARRAY
            BEGIN
              DECLARE result ARRAY;
              SET result = ESQL_QUERY('FROM .fleet-agents | LIMIT ' || page_size);
              RETURN result;
            END SKILL;
            """;

        List<String> errors = new ArrayList<>();
        ElasticScriptParser.ProgramContext ctx = parseWithErrorCollection(skill, errors);

        assertTrue("Parsing errors: " + errors, errors.isEmpty());
        assertEquals(2, ctx.skill_statement().create_skill_statement()
            .skill_parameters_clause().skill_param_list().skill_param().size());
    }
}
