/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.language;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.ArrayBuiltInFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.introspection.IntrospectionFunctions;
import org.elasticsearch.xpack.escript.parser.ElasticScriptLexer;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;
import org.elasticsearch.xpack.escript.primitives.ReturnValue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests for FROM function() feature - using function results as ESQL data sources.
 */
public class FromFunctionTests extends ESTestCase {

    private ThreadPool threadPool;
    private ExecutionContext context;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        threadPool = new TestThreadPool("test");
        context = new ExecutionContext();
        ArrayBuiltInFunctions.registerAll(context);
        IntrospectionFunctions.registerAll(context, null);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        ThreadPool.terminate(threadPool, 10, TimeUnit.SECONDS);
    }

    private Object executeScript(String script) throws Exception {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(script));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);

        ProcedureExecutor executor = new ProcedureExecutor(context, threadPool, null, tokens);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();

        ElasticScriptParser.ProcedureContext procedureCtx = parser.procedure();
        executor.visitProcedureAsync(procedureCtx, new ActionListener<>() {
            @Override
            public void onResponse(Object o) {
                result.set(o);
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                error.set(e);
                latch.countDown();
            }
        });

        assertTrue("Execution timed out", latch.await(10, TimeUnit.SECONDS));
        if (error.get() != null) {
            throw error.get();
        }
        Object rawResult = result.get();
        if (rawResult instanceof ReturnValue returnValue) {
            return returnValue.getValue();
        }
        return rawResult;
    }

    // ==================== Function Source Detection Tests ====================

    public void testIsFunctionSourceDetection() {
        // Test via direct pattern matching (simulating what the executor does)
        assertTrue(isFunctionPattern("FROM ESCRIPT_FUNCTIONS() | LIMIT 10"));
        assertTrue(isFunctionPattern("FROM ESCRIPT_CAPABILITIES()"));
        assertTrue(isFunctionPattern("FROM MY_FUNCTION('arg1', 'arg2')"));
        assertFalse(isFunctionPattern("FROM my_index"));
        assertFalse(isFunctionPattern("FROM logs | WHERE level == 'error'"));
    }

    private boolean isFunctionPattern(String query) {
        String trimmed = query.trim().toUpperCase();
        if (!trimmed.startsWith("FROM ")) {
            return false;
        }
        String afterFrom = query.trim().substring(5).trim();
        return afterFrom.matches("^[A-Za-z_][A-Za-z0-9_]*\\s*\\(.*");
    }

    // ==================== Introspection as Data Source Tests ====================

    public void testFromEscriptFunctions() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE count NUMBER = 0;
                FOR func IN (FROM ESCRIPT_FUNCTIONS()) LOOP
                    SET count = count + 1;
                END LOOP
                RETURN count;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertTrue(((Number) result).intValue() > 0);
    }

    public void testFromEscriptFunctionsWithLimit() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE count NUMBER = 0;
                FOR func IN (FROM ESCRIPT_FUNCTIONS() | LIMIT 5) LOOP
                    SET count = count + 1;
                END LOOP
                RETURN count;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertEquals(5, ((Number) result).intValue());
    }

    public void testFromEscriptVariables() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE x NUMBER = 10;
                DECLARE y STRING = 'hello';
                DECLARE vars ARRAY = [];
                FOR v IN (FROM ESCRIPT_VARIABLES()) LOOP
                    SET vars = ARRAY_APPEND(vars, v['name']);
                END LOOP
                RETURN ARRAY_LENGTH(vars);
            END PROCEDURE
            """;
        Object result = executeScript(script);
        // Should have at least x, y, and vars
        assertTrue(((Number) result).intValue() >= 3);
    }

    // ==================== Filter Operations Tests ====================

    public void testFromFunctionWithWhereFilter() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE count NUMBER = 0;
                FOR func IN (FROM ESCRIPT_FUNCTIONS() | WHERE is_builtin == 'true' | LIMIT 10) LOOP
                    SET count = count + 1;
                END LOOP
                RETURN count;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertTrue(((Number) result).intValue() > 0);
    }

    // ==================== Keep Operations Tests ====================

    public void testFromFunctionWithKeep() throws Exception {
        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE names ARRAY = [];
                FOR func IN (FROM ESCRIPT_FUNCTIONS() | KEEP name | LIMIT 3) LOOP
                    SET names = ARRAY_APPEND(names, func['name']);
                END LOOP
                RETURN ARRAY_LENGTH(names);
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertEquals(3, ((Number) result).intValue());
    }

    // ==================== Custom Function as Source ====================

    public void testCustomFunctionAsSource() throws Exception {
        // Register a custom function that returns an array of documents
        context.declareFunction("GET_USERS",
            List.of(),
            new org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition(
                "GET_USERS",
                (List<Object> args, ActionListener<Object> listener) -> {
                    List<Map<String, Object>> users = List.of(
                        Map.of("name", "Alice", "age", 30, "role", "admin"),
                        Map.of("name", "Bob", "age", 25, "role", "user"),
                        Map.of("name", "Carol", "age", 35, "role", "admin")
                    );
                    listener.onResponse(users);
                }
            )
        );

        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE admin_count NUMBER = 0;
                FOR user IN (FROM GET_USERS() | WHERE role == 'admin') LOOP
                    SET admin_count = admin_count + 1;
                END LOOP
                RETURN admin_count;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertEquals(2, ((Number) result).intValue());
    }

    public void testFunctionWithSortOperation() throws Exception {
        context.declareFunction("GET_PRODUCTS",
            List.of(),
            new org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition(
                "GET_PRODUCTS",
                (List<Object> args, ActionListener<Object> listener) -> {
                    List<Map<String, Object>> products = List.of(
                        Map.of("name", "Apple", "price", 1.5),
                        Map.of("name", "Banana", "price", 0.5),
                        Map.of("name", "Cherry", "price", 3.0)
                    );
                    listener.onResponse(products);
                }
            )
        );

        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE first_name STRING = '';
                FOR prod IN (FROM GET_PRODUCTS() | SORT price | LIMIT 1) LOOP
                    SET first_name = prod['name'];
                END LOOP
                RETURN first_name;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertEquals("Banana", result);
    }

    public void testFunctionWithDescSort() throws Exception {
        context.declareFunction("GET_SCORES",
            List.of(),
            new org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition(
                "GET_SCORES",
                (List<Object> args, ActionListener<Object> listener) -> {
                    List<Map<String, Object>> scores = List.of(
                        Map.of("player", "Alice", "score", 100),
                        Map.of("player", "Bob", "score", 250),
                        Map.of("player", "Carol", "score", 175)
                    );
                    listener.onResponse(scores);
                }
            )
        );

        String script = """
            PROCEDURE test()
            BEGIN
                DECLARE top_player STRING = '';
                FOR s IN (FROM GET_SCORES() | SORT score DESC | LIMIT 1) LOOP
                    SET top_player = s['player'];
                END LOOP
                RETURN top_player;
            END PROCEDURE
            """;
        Object result = executeScript(script);
        assertEquals("Bob", result);
    }
}


