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
import org.junit.Before;

/**
 * Tests for Moltler connector statement parsing.
 */
public class ConnectorParserTests extends ESTestCase {
    
    private ElasticScriptParser parser;
    
    private ElasticScriptParser createParser(String input) {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new ElasticScriptParser(tokens);
    }
    
    public void testCreateConnectorBasic() {
        String input = "CREATE CONNECTOR my_github TYPE 'github' CONFIG {\"token\": \"xxx\"};";
        parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.connector_statement());
        assertNotNull(ctx.connector_statement().create_connector_statement());
        assertEquals("my_github", ctx.connector_statement().create_connector_statement().ID().getText());
    }
    
    public void testCreateConnectorWithDescription() {
        String input = "CREATE CONNECTOR jira_prod TYPE 'jira' DESCRIPTION 'Production Jira' CONFIG {\"url\": \"https://jira.example.com\"};";
        parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.connector_statement().create_connector_statement());
        assertEquals("jira_prod", ctx.connector_statement().create_connector_statement().ID().getText());
    }
    
    public void testCreateConnectorWithTargetIndex() {
        String input = "CREATE CONNECTOR datadog_metrics TYPE 'datadog' INTO 'datadog-metrics' CONFIG {\"api_key\": \"xxx\"};";
        parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.connector_statement().create_connector_statement());
    }
    
    public void testDropConnector() {
        String input = "DROP CONNECTOR my_github;";
        parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.connector_statement());
        assertNotNull(ctx.connector_statement().drop_connector_statement());
        assertEquals("my_github", ctx.connector_statement().drop_connector_statement().ID().getText());
    }
    
    public void testShowConnectors() {
        String input = "SHOW CONNECTORS;";
        parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.connector_statement());
        assertNotNull(ctx.connector_statement().show_connectors_statement());
    }
    
    public void testShowConnectorDetail() {
        String input = "SHOW CONNECTOR my_github;";
        parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.connector_statement());
        assertNotNull(ctx.connector_statement().show_connectors_statement());
    }
    
    public void testTestConnector() {
        String input = "TEST CONNECTOR my_github;";
        parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.connector_statement());
        assertNotNull(ctx.connector_statement().test_connector_statement());
        assertEquals("my_github", ctx.connector_statement().test_connector_statement().ID().getText());
    }
    
    public void testSyncConnector() {
        String input = "SYNC CONNECTOR my_github TO 'github-data-*';";
        parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.connector_statement());
        assertNotNull(ctx.connector_statement().sync_connector_statement());
    }
    
    public void testSyncConnectorIncremental() {
        String input = "SYNC CONNECTOR my_github TO 'github-data-*' INCREMENTAL ON updated_at;";
        parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.connector_statement().sync_connector_statement());
        assertNotNull(ctx.connector_statement().sync_connector_statement().INCREMENTAL());
    }
    
    public void testSyncConnectorWithSchedule() {
        String input = "SYNC CONNECTOR my_github TO 'github-data-*' SCHEDULE '*/15 * * * *';";
        parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.connector_statement().sync_connector_statement());
        assertNotNull(ctx.connector_statement().sync_connector_statement().SCHEDULE());
    }
    
    public void testSyncConnectorWithEntity() {
        String input = "SYNC CONNECTOR my_github.issues TO 'github-issues-*';";
        parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.connector_statement().sync_connector_statement());
    }
    
    public void testEnableConnector() {
        String input = "ENABLE CONNECTOR my_github;";
        parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.connector_statement());
        assertNotNull(ctx.connector_statement().alter_connector_statement());
    }
    
    public void testDisableConnector() {
        String input = "DISABLE CONNECTOR my_github;";
        parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.connector_statement());
        assertNotNull(ctx.connector_statement().alter_connector_statement());
    }
    
    public void testExecConnectorAction() {
        String input = "EXEC my_github.create_issue(owner = 'org', repo = 'repo', title = 'Bug');";
        parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.connector_statement());
        assertNotNull(ctx.connector_statement().exec_connector_statement());
    }
    
    public void testExecConnectorActionNoArgs() {
        String input = "EXEC my_github.list_repos();";
        parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.connector_statement().exec_connector_statement());
    }
    
    public void testQueryConnector() {
        String input = "QUERY my_jira.issues WHERE status = 'Open' LIMIT 50;";
        parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.connector_statement());
        assertNotNull(ctx.connector_statement().query_connector_statement());
    }
    
    public void testQueryConnectorWithOrder() {
        String input = "QUERY my_github.issues WHERE state = 'open' LIMIT 100 ORDER BY created DESC;";
        parser = createParser(input);
        
        ElasticScriptParser.ProgramContext ctx = parser.program();
        assertNotNull(ctx.connector_statement().query_connector_statement());
        assertNotNull(ctx.connector_statement().query_connector_statement().DESC());
    }
}
