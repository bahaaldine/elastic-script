/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.intent;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;

import java.util.Collections;
import java.util.List;

/**
 * Unit tests for Intent-related classes.
 */
public class IntentTests extends ESTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Clear the registry before each test
        IntentRegistry.getInstance().clear();
    }

    public void testIntentDefinitionBasics() {
        IntentDefinition intent = new IntentDefinition(
            "test_intent",
            "A test intent",
            List.of(new Parameter("service", "STRING", ParameterMode.IN)),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        );

        assertEquals("test_intent", intent.getName());
        assertEquals("A test intent", intent.getDescription());
        assertEquals(1, intent.getParameters().size());
        assertEquals("service", intent.getParameters().get(0).getName());
        assertFalse(intent.hasRequires());
        assertFalse(intent.hasOnFailure());
    }

    public void testIntentDefinitionSignature() {
        IntentDefinition intent = new IntentDefinition(
            "restart_service",
            "Restarts a service",
            List.of(
                new Parameter("service", "STRING", ParameterMode.IN),
                new Parameter("timeout", "NUMBER", ParameterMode.IN)
            ),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        );

        String signature = intent.getSignature();
        assertEquals("restart_service(service STRING, timeout NUMBER)", signature);
    }

    public void testIntentRegistryRegisterAndGet() {
        IntentDefinition intent = new IntentDefinition(
            "scale_pods",
            "Scale Kubernetes pods",
            List.of(new Parameter("replicas", "NUMBER", ParameterMode.IN)),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        );

        IntentRegistry registry = IntentRegistry.getInstance();
        assertFalse(registry.exists("scale_pods"));

        registry.register(intent);

        assertTrue(registry.exists("scale_pods"));
        assertNotNull(registry.get("scale_pods"));
        assertEquals("scale_pods", registry.get("scale_pods").getName());
    }

    public void testIntentRegistryGetAll() {
        IntentRegistry registry = IntentRegistry.getInstance();

        IntentDefinition intent1 = new IntentDefinition(
            "intent_one",
            "First intent",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        );

        IntentDefinition intent2 = new IntentDefinition(
            "intent_two",
            "Second intent",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        );

        registry.register(intent1);
        registry.register(intent2);

        assertEquals(2, registry.getAll().size());
    }

    public void testIntentRegistryClear() {
        IntentRegistry registry = IntentRegistry.getInstance();

        IntentDefinition intent = new IntentDefinition(
            "temp_intent",
            "Temporary",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        );

        registry.register(intent);
        assertTrue(registry.exists("temp_intent"));

        registry.clear();
        assertFalse(registry.exists("temp_intent"));
        assertTrue(registry.getAll().isEmpty());
    }

    public void testIntentRegistryDuplicateThrows() {
        IntentRegistry registry = IntentRegistry.getInstance();

        IntentDefinition intent = new IntentDefinition(
            "duplicate_intent",
            "First version",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        );

        registry.register(intent);

        // Registering again should throw
        IntentDefinition duplicate = new IntentDefinition(
            "duplicate_intent",
            "Second version",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        );

        IllegalArgumentException ex = expectThrows(IllegalArgumentException.class, () -> registry.register(duplicate));
        assertTrue(ex.getMessage().contains("already defined"));
    }
}

