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
import org.elasticsearch.xpack.escript.parser.ElasticScriptLexer;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;

import org.elasticsearch.xpack.escript.primitives.ReturnValue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests for new language primitives: NOT, !, IS NULL, IS NOT NULL, CONTINUE
 */
public class LanguagePrimitivesTests extends ESTestCase {

    private ThreadPool threadPool;
    private ExecutionContext context;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        threadPool = new TestThreadPool("test");
        context = new ExecutionContext();
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
        // Unwrap ReturnValue if present
        if (rawResult instanceof ReturnValue returnValue) {
            return returnValue.getValue();
        }
        return rawResult;
    }

    public void testNotOperatorWithBoolean() throws Exception {
        String script = """
            PROCEDURE test_not()
            BEGIN
                DECLARE active BOOLEAN = true;
                IF NOT active THEN
                    RETURN 'inactive';
                ELSE
                    RETURN 'active';
                END IF
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("active", result);
    }

    public void testNotOperatorNegation() throws Exception {
        String script = """
            PROCEDURE test_not_negation()
            BEGIN
                DECLARE active BOOLEAN = false;
                IF NOT active THEN
                    RETURN 'was false';
                ELSE
                    RETURN 'was true';
                END IF
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("was false", result);
    }

    public void testBangOperator() throws Exception {
        String script = """
            PROCEDURE test_bang()
            BEGIN
                DECLARE active BOOLEAN = true;
                IF !active THEN
                    RETURN 'inactive';
                ELSE
                    RETURN 'active';
                END IF
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("active", result);
    }

    public void testNotWithTruthyValue() throws Exception {
        String script = """
            PROCEDURE test_truthy()
            BEGIN
                DECLARE count NUMBER = 5;
                IF NOT count THEN
                    RETURN 'falsy';
                ELSE
                    RETURN 'truthy';
                END IF
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("truthy", result);
    }

    public void testNotWithZero() throws Exception {
        String script = """
            PROCEDURE test_zero()
            BEGIN
                DECLARE count NUMBER = 0;
                IF NOT count THEN
                    RETURN 'zero is falsy';
                ELSE
                    RETURN 'zero is truthy';
                END IF
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("zero is falsy", result);
    }

    public void testIsNull() throws Exception {
        // Test IS NULL with null literal
        String script = """
            PROCEDURE test_is_null()
            BEGIN
                IF null IS NULL THEN
                    RETURN 'is null';
                ELSE
                    RETURN 'not null';
                END IF
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("is null", result);
    }

    public void testIsNotNull() throws Exception {
        String script = """
            PROCEDURE test_is_not_null()
            BEGIN
                DECLARE value STRING = 'hello';
                IF value IS NOT NULL THEN
                    RETURN 'has value';
                ELSE
                    RETURN 'null';
                END IF
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("has value", result);
    }

    public void testContinueStatement() throws Exception {
        String script = """
            PROCEDURE test_continue()
            BEGIN
                DECLARE sum NUMBER = 0;
                FOR i IN 1..10 LOOP
                    IF i == 5 THEN
                        CONTINUE;
                    END IF
                    SET sum = sum + i;
                END LOOP
                RETURN sum;
            END PROCEDURE
            """;
        
        // Sum of 1..10 = 55, minus 5 = 50
        Object result = executeScript(script);
        assertEquals(50.0, result);
    }

    public void testContinueInWhileLoop() throws Exception {
        String script = """
            PROCEDURE test_while_continue()
            BEGIN
                DECLARE i NUMBER = 0;
                DECLARE sum NUMBER = 0;
                WHILE i < 10 LOOP
                    SET i = i + 1;
                    IF i == 3 OR i == 7 THEN
                        CONTINUE;
                    END IF
                    SET sum = sum + i;
                END LOOP
                RETURN sum;
            END PROCEDURE
            """;
        
        // Sum of 1..10 = 55, minus 3 and 7 = 45
        Object result = executeScript(script);
        assertEquals(45.0, result);
    }

    public void testContinueInArrayLoop() throws Exception {
        String script = """
            PROCEDURE test_array_continue()
            BEGIN
                DECLARE items ARRAY = [1, 2, 3, 4, 5];
                DECLARE sum NUMBER = 0;
                FOR item IN items LOOP
                    IF item == 3 THEN
                        CONTINUE;
                    END IF
                    SET sum = sum + item;
                END LOOP
                RETURN sum;
            END PROCEDURE
            """;
        
        // Sum of 1,2,4,5 = 12 (skipping 3)
        Object result = executeScript(script);
        assertEquals(12.0, result);
    }

    // ========== Phase 2: Modulo, IN, SWITCH/CASE ==========

    public void testModuloOperator() throws Exception {
        String script = """
            PROCEDURE test_modulo()
            BEGIN
                DECLARE a NUMBER = 17;
                DECLARE b NUMBER = 5;
                RETURN a % b;
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals(2.0, result);
    }

    public void testModuloForEvenOdd() throws Exception {
        String script = """
            PROCEDURE test_even_odd()
            BEGIN
                DECLARE num NUMBER = 10;
                IF num % 2 == 0 THEN
                    RETURN 'even';
                ELSE
                    RETURN 'odd';
                END IF
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("even", result);
    }

    public void testInListWithNumbers() throws Exception {
        String script = """
            PROCEDURE test_in_list()
            BEGIN
                DECLARE x NUMBER = 5;
                IF x IN (1, 3, 5, 7, 9) THEN
                    RETURN 'odd digit';
                ELSE
                    RETURN 'not odd digit';
                END IF
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("odd digit", result);
    }

    public void testInListNotFound() throws Exception {
        String script = """
            PROCEDURE test_in_list_not_found()
            BEGIN
                DECLARE x NUMBER = 6;
                IF x IN (1, 3, 5, 7, 9) THEN
                    RETURN 'found';
                ELSE
                    RETURN 'not found';
                END IF
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("not found", result);
    }

    public void testNotInList() throws Exception {
        String script = """
            PROCEDURE test_not_in_list()
            BEGIN
                DECLARE status STRING = 'active';
                IF status NOT IN ('deleted', 'archived', 'suspended') THEN
                    RETURN 'valid';
                ELSE
                    RETURN 'invalid';
                END IF
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("valid", result);
    }

    public void testInArray() throws Exception {
        String script = """
            PROCEDURE test_in_array()
            BEGIN
                DECLARE allowed ARRAY = ["admin", "user", "guest"];
                DECLARE role STRING = "user";
                IF role IN allowed THEN
                    RETURN 'allowed';
                ELSE
                    RETURN 'denied';
                END IF
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("allowed", result);
    }

    public void testNotInArray() throws Exception {
        String script = """
            PROCEDURE test_not_in_array()
            BEGIN
                DECLARE blocked ARRAY = ["banned", "spam"];
                DECLARE user STRING = "normal";
                IF user NOT IN blocked THEN
                    RETURN 'ok';
                ELSE
                    RETURN 'blocked';
                END IF
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("ok", result);
    }

    public void testSwitchCaseNumber() throws Exception {
        String script = """
            PROCEDURE test_switch_number()
            BEGIN
                DECLARE day NUMBER = 3;
                SWITCH day
                    CASE 1:
                        RETURN 'Monday';
                    CASE 2:
                        RETURN 'Tuesday';
                    CASE 3:
                        RETURN 'Wednesday';
                    CASE 4:
                        RETURN 'Thursday';
                    CASE 5:
                        RETURN 'Friday';
                    DEFAULT:
                        RETURN 'Weekend';
                END SWITCH
                RETURN 'Unknown';
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("Wednesday", result);
    }

    public void testSwitchCaseDefault() throws Exception {
        String script = """
            PROCEDURE test_switch_default()
            BEGIN
                DECLARE day NUMBER = 7;
                SWITCH day
                    CASE 1:
                        RETURN 'Monday';
                    CASE 2:
                        RETURN 'Tuesday';
                    DEFAULT:
                        RETURN 'Other day';
                END SWITCH
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("Other day", result);
    }

    public void testSwitchCaseString() throws Exception {
        String script = """
            PROCEDURE test_switch_string()
            BEGIN
                DECLARE status STRING = 'warning';
                SWITCH status
                    CASE 'info':
                        RETURN 0;
                    CASE 'warning':
                        RETURN 1;
                    CASE 'error':
                        RETURN 2;
                    DEFAULT:
                        RETURN -1;
                END SWITCH
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals(1.0, result);
    }

    public void testSwitchWithMultipleStatements() throws Exception {
        String script = """
            PROCEDURE test_switch_multi()
            BEGIN
                DECLARE level NUMBER = 2;
                DECLARE msg STRING = '';
                DECLARE code NUMBER = 0;
                SWITCH level
                    CASE 1:
                        SET msg = 'Low';
                        SET code = 100;
                    CASE 2:
                        SET msg = 'Medium';
                        SET code = 200;
                    CASE 3:
                        SET msg = 'High';
                        SET code = 300;
                END SWITCH
                RETURN msg || ':' || code;
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("Medium:200.0", result);
    }

    // ========== Phase 3: Null Coalescing, Ternary, Safe Navigation ==========

    public void testNullCoalesceWithNull() throws Exception {
        String script = """
            PROCEDURE test_null_coalesce()
            BEGIN
                DECLARE doc DOCUMENT = {"name": "test"};
                RETURN doc['missing'] ?? 'Unknown';
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("Unknown", result);
    }

    public void testNullCoalesceWithValue() throws Exception {
        String script = """
            PROCEDURE test_null_coalesce_value()
            BEGIN
                DECLARE name STRING = 'John';
                RETURN name ?? 'Unknown';
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("John", result);
    }

    public void testNullCoalesceChain() throws Exception {
        String script = """
            PROCEDURE test_null_coalesce_chain()
            BEGIN
                DECLARE doc DOCUMENT = {"a": "value"};
                RETURN doc['missing1'] ?? doc['missing2'] ?? 'Default';
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("Default", result);
    }

    public void testTernaryTrue() throws Exception {
        String script = """
            PROCEDURE test_ternary_true()
            BEGIN
                DECLARE active BOOLEAN = true;
                RETURN active ? 'Active' : 'Inactive';
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("Active", result);
    }

    public void testTernaryFalse() throws Exception {
        String script = """
            PROCEDURE test_ternary_false()
            BEGIN
                DECLARE active BOOLEAN = false;
                RETURN active ? 'Active' : 'Inactive';
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("Inactive", result);
    }

    public void testTernaryWithExpression() throws Exception {
        String script = """
            PROCEDURE test_ternary_expr()
            BEGIN
                DECLARE count NUMBER = 5;
                RETURN count > 0 ? 'Has items' : 'Empty';
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("Has items", result);
    }

    public void testTernaryWithNumbers() throws Exception {
        String script = """
            PROCEDURE test_ternary_numbers()
            BEGIN
                DECLARE error_count NUMBER = 3;
                RETURN error_count > 0 ? 1 : 0;
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals(1.0, result);
    }

    public void testSafeNavigationWithValue() throws Exception {
        String script = """
            PROCEDURE test_safe_nav()
            BEGIN
                DECLARE user DOCUMENT = {"name": "John", "address": {"city": "NYC"}};
                RETURN user?.name;
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("John", result);
    }

    public void testSafeNavigationWithMissingField() throws Exception {
        String script = """
            PROCEDURE test_safe_nav_missing()
            BEGIN
                DECLARE user DOCUMENT = {"name": "John"};
                RETURN user['profile']?.settings ?? 'No settings';
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("No settings", result);
    }

    public void testSafeNavigationChain() throws Exception {
        String script = """
            PROCEDURE test_safe_nav_chain()
            BEGIN
                DECLARE user DOCUMENT = {"profile": {"settings": {"theme": "dark"}}};
                RETURN user?.profile?.settings?.theme;
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("dark", result);
    }

    public void testCombinedTernaryAndCoalesce() throws Exception {
        String script = """
            PROCEDURE test_combined()
            BEGIN
                DECLARE doc DOCUMENT = {"data": "test"};
                DECLARE has_value BOOLEAN = false;
                RETURN has_value ? 'Has value' : (doc['missing'] ?? 'Fallback');
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("Fallback", result);
    }

    // ========== Phase 4: VAR and CONST ==========

    public void testVarWithString() throws Exception {
        String script = """
            PROCEDURE test_var_string()
            BEGIN
                VAR name = 'John';
                RETURN name;
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("John", result);
    }

    public void testVarWithNumber() throws Exception {
        String script = """
            PROCEDURE test_var_number()
            BEGIN
                VAR count = 42;
                SET count = count + 8;
                RETURN count;
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals(50.0, result);
    }

    public void testVarWithArray() throws Exception {
        String script = """
            PROCEDURE test_var_array()
            BEGIN
                VAR items = [1, 2, 3];
                DECLARE sum NUMBER = 0;
                FOR item IN items LOOP
                    SET sum = sum + item;
                END LOOP
                RETURN sum;
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals(6.0, result);
    }

    public void testVarWithBoolean() throws Exception {
        String script = """
            PROCEDURE test_var_boolean()
            BEGIN
                VAR active = true;
                RETURN active ? 'yes' : 'no';
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("yes", result);
    }

    public void testVarWithDocument() throws Exception {
        String script = """
            PROCEDURE test_var_document()
            BEGIN
                VAR user = {"name": "Alice", "age": 30};
                RETURN user['name'];
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("Alice", result);
    }

    public void testMultipleVarDeclarations() throws Exception {
        String script = """
            PROCEDURE test_multi_var()
            BEGIN
                VAR a = 10, b = 20, c = 30;
                RETURN a + b + c;
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals(60.0, result);
    }

    public void testConstBasic() throws Exception {
        String script = """
            PROCEDURE test_const()
            BEGIN
                CONST MAX_RETRIES = 3;
                RETURN MAX_RETRIES;
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals(3.0, result);
    }

    public void testConstWithExplicitType() throws Exception {
        String script = """
            PROCEDURE test_const_typed()
            BEGIN
                CONST API_URL STRING = 'https://api.example.com';
                RETURN API_URL;
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("https://api.example.com", result);
    }

    public void testConstInExpression() throws Exception {
        String script = """
            PROCEDURE test_const_expr()
            BEGIN
                CONST BASE NUMBER = 100;
                CONST MULTIPLIER NUMBER = 2;
                RETURN BASE * MULTIPLIER;
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals(200.0, result);
    }

    public void testConstImmutability() throws Exception {
        String script = """
            PROCEDURE test_const_immutable()
            BEGIN
                CONST VALUE = 10;
                SET VALUE = 20;
                RETURN VALUE;
            END PROCEDURE
            """;
        
        try {
            executeScript(script);
            fail("Expected exception when modifying a constant");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Cannot modify constant") ||
                       e.getMessage().contains("immutable"));
        }
    }

    public void testVarAndConstTogether() throws Exception {
        String script = """
            PROCEDURE test_var_const_together()
            BEGIN
                CONST FACTOR = 10;
                VAR value = 5;
                SET value = value * FACTOR;
                RETURN value;
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals(50.0, result);
    }

    // ========== Phase 5: Case-Insensitive Booleans ==========

    public void testBooleanUppercaseTrue() throws Exception {
        String script = """
            PROCEDURE test_bool_upper_true()
            BEGIN
                DECLARE active BOOLEAN = TRUE;
                RETURN active ? 'yes' : 'no';
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("yes", result);
    }

    public void testBooleanUppercaseFalse() throws Exception {
        String script = """
            PROCEDURE test_bool_upper_false()
            BEGIN
                DECLARE active BOOLEAN = FALSE;
                RETURN active ? 'yes' : 'no';
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("no", result);
    }

    public void testBooleanMixedCaseTrue() throws Exception {
        String script = """
            PROCEDURE test_bool_mixed_true()
            BEGIN
                DECLARE active BOOLEAN = True;
                RETURN active ? 'yes' : 'no';
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("yes", result);
    }

    public void testBooleanMixedCaseFalse() throws Exception {
        String script = """
            PROCEDURE test_bool_mixed_false()
            BEGIN
                DECLARE active BOOLEAN = False;
                RETURN active ? 'yes' : 'no';
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("no", result);
    }

    public void testBooleanLowercaseStillWorks() throws Exception {
        String script = """
            PROCEDURE test_bool_lower()
            BEGIN
                DECLARE a BOOLEAN = true;
                DECLARE b BOOLEAN = false;
                RETURN a ? (b ? 'both' : 'only a') : 'neither';
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("only a", result);
    }

    public void testVarWithUppercaseBoolean() throws Exception {
        String script = """
            PROCEDURE test_var_bool_upper()
            BEGIN
                VAR active = TRUE;
                VAR inactive = FALSE;
                RETURN active ? (inactive ? 'both' : 'only active') : 'neither';
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("only active", result);
    }

    // ========== Phase 6: Array Literals with Single-Quoted Strings ==========

    public void testArrayWithSingleQuotedStrings() throws Exception {
        String script = """
            PROCEDURE test_array_single_quotes()
            BEGIN
                DECLARE items ARRAY = ['apple', 'banana', 'cherry'];
                RETURN items[0];
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("apple", result);
    }

    public void testArrayWithMixedQuotes() throws Exception {
        String script = """
            PROCEDURE test_array_mixed_quotes()
            BEGIN
                DECLARE items ARRAY = ['single', "double"];
                RETURN items[0] || ' and ' || items[1];
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("single and double", result);
    }

    public void testArrayIterationWithSingleQuotes() throws Exception {
        String script = """
            PROCEDURE test_array_iter_single_quotes()
            BEGIN
                DECLARE fruits ARRAY = ['a', 'b', 'c'];
                DECLARE result STRING = '';
                FOR fruit IN fruits LOOP
                    SET result = result || fruit;
                END LOOP
                RETURN result;
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("abc", result);
    }

    public void testArrayAccessWithSingleQuotedStrings() throws Exception {
        // Test accessing multiple elements from array with single-quoted strings
        String script = """
            PROCEDURE test_array_access_single_quotes()
            BEGIN
                DECLARE items ARRAY = ['one', 'two', 'three', 'four', 'five'];
                RETURN items[0] || '-' || items[2] || '-' || items[4];
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("one-three-five", result);
    }

    public void testVarArrayWithSingleQuotes() throws Exception {
        String script = """
            PROCEDURE test_var_array_single_quotes()
            BEGIN
                VAR colors = ['red', 'green', 'blue'];
                RETURN colors[1];
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("green", result);
    }

    public void testComplexTypesDemo() throws Exception {
        // This is the full demo_types procedure from the notebook
        String script = """
            PROCEDURE demo_types()
            BEGIN
                DECLARE message STRING = 'Hello';
                DECLARE count NUMBER = 42;
                DECLARE is_active BOOLEAN = TRUE;
                DECLARE user DOCUMENT = { "name": "Alice", "role": "admin" };
                DECLARE items ARRAY = ['apple', 'banana', 'cherry'];
                VAR inferred_type = 'This is a string';
                CONST PI = 3.14159;
                RETURN message || ':' || count || ':' || is_active || ':' || user['name'] || ':' || items[0];
            END PROCEDURE
            """;
        
        Object result = executeScript(script);
        assertEquals("Hello:42.0:true:Alice:apple", result);
    }
}

