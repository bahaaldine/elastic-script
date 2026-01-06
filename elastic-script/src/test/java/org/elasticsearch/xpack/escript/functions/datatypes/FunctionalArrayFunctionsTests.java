/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.datatypes;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.FunctionDefinition;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.ArrayBuiltInFunctions;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Unit tests for Phase 5 functional array operations.
 */
public class FunctionalArrayFunctionsTests extends ESTestCase {

    private ExecutionContext context;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        context = new ExecutionContext();
        ArrayBuiltInFunctions.registerAll(context);
    }

    private Object executeFunction(String name, List<Object> args) throws Exception {
        FunctionDefinition func = context.getFunction(name);
        assertNotNull("Function " + name + " should be registered", func);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();

        func.execute(args, new ActionListener<Object>() {
            @Override
            public void onResponse(Object response) {
                result.set(response);
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                error.set(e);
                latch.countDown();
            }
        });

        assertTrue("Function should complete within timeout", latch.await(5, TimeUnit.SECONDS));
        if (error.get() != null) {
            throw error.get();
        }
        return result.get();
    }

    // ==================== ARRAY_JOIN Tests ====================

    public void testArrayJoinBasic() throws Exception {
        List<Object> array = Arrays.asList("a", "b", "c");
        Object result = executeFunction("ARRAY_JOIN", Arrays.asList(array, ", "));
        assertEquals("a, b, c", result);
    }

    public void testArrayJoinEmptyDelimiter() throws Exception {
        List<Object> array = Arrays.asList("hello", "world");
        Object result = executeFunction("ARRAY_JOIN", Arrays.asList(array, ""));
        assertEquals("helloworld", result);
    }

    public void testArrayJoinNumbers() throws Exception {
        List<Object> array = Arrays.asList(1, 2, 3);
        Object result = executeFunction("ARRAY_JOIN", Arrays.asList(array, "-"));
        assertEquals("1-2-3", result);
    }

    public void testArrayJoinEmptyArray() throws Exception {
        List<Object> array = new ArrayList<>();
        Object result = executeFunction("ARRAY_JOIN", Arrays.asList(array, ", "));
        assertEquals("", result);
    }

    // ==================== ARRAY_FLATTEN Tests ====================

    public void testArrayFlattenBasic() throws Exception {
        List<Object> nested = Arrays.asList(
            Arrays.asList(1, 2),
            Arrays.asList(3, 4)
        );
        Object result = executeFunction("ARRAY_FLATTEN", List.of(nested));
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    public void testArrayFlattenMixed() throws Exception {
        List<Object> nested = Arrays.asList(
            Arrays.asList(1, 2),
            3,
            Arrays.asList(4, 5)
        );
        Object result = executeFunction("ARRAY_FLATTEN", List.of(nested));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    public void testArrayFlattenNoNesting() throws Exception {
        List<Object> array = Arrays.asList(1, 2, 3);
        Object result = executeFunction("ARRAY_FLATTEN", List.of(array));
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    // ==================== ARRAY_REVERSE Tests ====================

    public void testArrayReverseBasic() throws Exception {
        List<Object> array = Arrays.asList(1, 2, 3);
        Object result = executeFunction("ARRAY_REVERSE", List.of(array));
        assertEquals(Arrays.asList(3, 2, 1), result);
    }

    public void testArrayReverseStrings() throws Exception {
        List<Object> array = Arrays.asList("a", "b", "c");
        Object result = executeFunction("ARRAY_REVERSE", List.of(array));
        assertEquals(Arrays.asList("c", "b", "a"), result);
    }

    // ==================== ARRAY_SLICE Tests ====================

    public void testArraySliceBasic() throws Exception {
        List<Object> array = Arrays.asList(1, 2, 3, 4, 5);
        Object result = executeFunction("ARRAY_SLICE", Arrays.asList(array, 1, 4));
        assertEquals(Arrays.asList(2, 3, 4), result);
    }

    public void testArraySliceFromStart() throws Exception {
        List<Object> array = Arrays.asList(1, 2, 3, 4, 5);
        Object result = executeFunction("ARRAY_SLICE", Arrays.asList(array, 0, 3));
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    public void testArraySliceToEnd() throws Exception {
        List<Object> array = Arrays.asList(1, 2, 3, 4, 5);
        Object result = executeFunction("ARRAY_SLICE", Arrays.asList(array, 3, 10));
        assertEquals(Arrays.asList(4, 5), result);
    }

    public void testArraySliceOutOfBounds() throws Exception {
        List<Object> array = Arrays.asList(1, 2, 3);
        Object result = executeFunction("ARRAY_SLICE", Arrays.asList(array, 5, 10));
        assertEquals(new ArrayList<>(), result);
    }

    // ==================== ARRAY_MAP Tests ====================

    public void testArrayMapExtractProperty() throws Exception {
        List<Object> users = Arrays.asList(
            Map.of("name", "Alice", "age", 30),
            Map.of("name", "Bob", "age", 25)
        );
        Object result = executeFunction("ARRAY_MAP", Arrays.asList(users, "name"));
        assertEquals(Arrays.asList("Alice", "Bob"), result);
    }

    public void testArrayMapMissingProperty() throws Exception {
        List<Object> users = Arrays.asList(
            Map.of("name", "Alice"),
            Map.of("title", "Mr.")
        );
        Object result = executeFunction("ARRAY_MAP", Arrays.asList(users, "name"));
        List<?> list = (List<?>) result;
        assertEquals(2, list.size());
        assertEquals("Alice", list.get(0));
        assertNull(list.get(1));
    }

    // ==================== ARRAY_FILTER_BY Tests ====================
    // Note: ARRAY_FILTER now takes 2 args (array, lambda). For property-based filtering, use ARRAY_FILTER_BY.

    public void testArrayFilterByProperty() throws Exception {
        List<Object> users = Arrays.asList(
            Map.of("name", "Alice", "active", true),
            Map.of("name", "Bob", "active", false),
            Map.of("name", "Carol", "active", true)
        );
        Object result = executeFunction("ARRAY_FILTER_BY", Arrays.asList(users, "active", true));
        List<?> list = (List<?>) result;
        assertEquals(2, list.size());
    }

    public void testArrayFilterByStringProperty() throws Exception {
        List<Object> items = Arrays.asList(
            Map.of("status", "active"),
            Map.of("status", "inactive"),
            Map.of("status", "active")
        );
        Object result = executeFunction("ARRAY_FILTER_BY", Arrays.asList(items, "status", "active"));
        List<?> list = (List<?>) result;
        assertEquals(2, list.size());
    }

    public void testArrayFilterNoMatch() throws Exception {
        List<Object> items = Arrays.asList(
            Map.of("status", "inactive"),
            Map.of("status", "pending")
        );
        Object result = executeFunction("ARRAY_FILTER_BY", Arrays.asList(items, "status", "active"));
        List<?> list = (List<?>) result;
        assertEquals(0, list.size());
    }

    // ==================== ARRAY_REDUCE Tests ====================

    public void testArrayReduceSum() throws Exception {
        List<Object> numbers = Arrays.asList(1, 2, 3, 4, 5);
        Object result = executeFunction("ARRAY_REDUCE", Arrays.asList(numbers, 0));
        assertEquals(15.0, result);
    }

    public void testArrayReduceWithInitial() throws Exception {
        List<Object> numbers = Arrays.asList(1, 2, 3);
        Object result = executeFunction("ARRAY_REDUCE", Arrays.asList(numbers, 10));
        assertEquals(16.0, result);
    }

    public void testArrayReduceConcat() throws Exception {
        List<Object> strings = Arrays.asList("a", "b", "c");
        Object result = executeFunction("ARRAY_REDUCE", Arrays.asList(strings, ""));
        assertEquals("abc", result);
    }

    // ==================== ARRAY_FIND Tests ====================

    public void testArrayFindMatch() throws Exception {
        List<Object> users = Arrays.asList(
            Map.of("id", 1, "name", "Alice"),
            Map.of("id", 2, "name", "Bob")
        );
        Object result = executeFunction("ARRAY_FIND", Arrays.asList(users, "id", 2));
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals("Bob", ((Map<?, ?>) result).get("name"));
    }

    public void testArrayFindNoMatch() throws Exception {
        List<Object> users = Arrays.asList(
            Map.of("id", 1, "name", "Alice"),
            Map.of("id", 2, "name", "Bob")
        );
        Object result = executeFunction("ARRAY_FIND", Arrays.asList(users, "id", 99));
        assertNull(result);
    }

    // ==================== ARRAY_FIND_INDEX Tests ====================

    public void testArrayFindIndexMatch() throws Exception {
        List<Object> users = Arrays.asList(
            Map.of("id", 1, "name", "Alice"),
            Map.of("id", 2, "name", "Bob")
        );
        Object result = executeFunction("ARRAY_FIND_INDEX", Arrays.asList(users, "id", 2));
        assertEquals(1, result);
    }

    public void testArrayFindIndexNoMatch() throws Exception {
        List<Object> users = Arrays.asList(
            Map.of("id", 1, "name", "Alice"),
            Map.of("id", 2, "name", "Bob")
        );
        Object result = executeFunction("ARRAY_FIND_INDEX", Arrays.asList(users, "id", 99));
        assertEquals(-1, result);
    }

    // ==================== ARRAY_EVERY Tests ====================

    public void testArrayEveryAllMatch() throws Exception {
        List<Object> items = Arrays.asList(
            Map.of("active", true),
            Map.of("active", true)
        );
        Object result = executeFunction("ARRAY_EVERY", Arrays.asList(items, "active", true));
        assertEquals(true, result);
    }

    public void testArrayEveryNotAllMatch() throws Exception {
        List<Object> items = Arrays.asList(
            Map.of("active", true),
            Map.of("active", false)
        );
        Object result = executeFunction("ARRAY_EVERY", Arrays.asList(items, "active", true));
        assertEquals(false, result);
    }

    // ==================== ARRAY_SOME Tests ====================

    public void testArraySomeOneMatch() throws Exception {
        List<Object> items = Arrays.asList(
            Map.of("active", false),
            Map.of("active", true)
        );
        Object result = executeFunction("ARRAY_SOME", Arrays.asList(items, "active", true));
        assertEquals(true, result);
    }

    public void testArraySomeNoMatch() throws Exception {
        List<Object> items = Arrays.asList(
            Map.of("active", false),
            Map.of("active", false)
        );
        Object result = executeFunction("ARRAY_SOME", Arrays.asList(items, "active", true));
        assertEquals(false, result);
    }

    // ==================== ARRAY_SORT Tests ====================

    public void testArraySortByProperty() throws Exception {
        List<Object> users = Arrays.asList(
            new HashMap<>(Map.of("name", "Carol", "age", 35)),
            new HashMap<>(Map.of("name", "Alice", "age", 30)),
            new HashMap<>(Map.of("name", "Bob", "age", 25))
        );
        Object result = executeFunction("ARRAY_SORT", Arrays.asList(users, "age"));
        List<?> list = (List<?>) result;
        assertEquals(3, list.size());
        assertEquals(25, ((Map<?, ?>) list.get(0)).get("age"));
        assertEquals(30, ((Map<?, ?>) list.get(1)).get("age"));
        assertEquals(35, ((Map<?, ?>) list.get(2)).get("age"));
    }

    public void testArraySortByName() throws Exception {
        List<Object> users = Arrays.asList(
            new HashMap<>(Map.of("name", "Carol")),
            new HashMap<>(Map.of("name", "Alice")),
            new HashMap<>(Map.of("name", "Bob"))
        );
        Object result = executeFunction("ARRAY_SORT", Arrays.asList(users, "name"));
        List<?> list = (List<?>) result;
        assertEquals(3, list.size());
        assertEquals("Alice", ((Map<?, ?>) list.get(0)).get("name"));
        assertEquals("Bob", ((Map<?, ?>) list.get(1)).get("name"));
        assertEquals("Carol", ((Map<?, ?>) list.get(2)).get("name"));
    }

    public void testArraySortPrimitives() throws Exception {
        List<Object> numbers = new ArrayList<>(Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6));
        Object result = executeFunction("ARRAY_SORT", Arrays.asList(numbers, null));
        List<?> list = (List<?>) result;
        assertEquals(Arrays.asList(1, 1, 2, 3, 4, 5, 6, 9), list);
    }
}

