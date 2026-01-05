/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.intent;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for storing and retrieving INTENT definitions.
 * 
 * The IntentRegistry provides:
 * - In-memory storage of defined intents
 * - Thread-safe access for concurrent use
 * - Lookup by intent name
 * - Listing all available intents
 * 
 * In the future, this could be extended to persist intents to Elasticsearch.
 */
public class IntentRegistry {
    
    private static final IntentRegistry INSTANCE = new IntentRegistry();
    
    private final Map<String, IntentDefinition> intents;
    
    /**
     * Private constructor for singleton pattern.
     */
    private IntentRegistry() {
        this.intents = new ConcurrentHashMap<>();
    }
    
    /**
     * Gets the singleton instance of the IntentRegistry.
     */
    public static IntentRegistry getInstance() {
        return INSTANCE;
    }
    
    /**
     * Registers a new intent definition.
     *
     * @param intent The intent definition to register
     * @throws IllegalArgumentException if an intent with the same name already exists
     */
    public void register(IntentDefinition intent) {
        if (intents.containsKey(intent.getName())) {
            throw new IllegalArgumentException("Intent '" + intent.getName() + "' is already defined");
        }
        intents.put(intent.getName(), intent);
    }
    
    /**
     * Registers or replaces an intent definition.
     *
     * @param intent The intent definition to register
     */
    public void registerOrReplace(IntentDefinition intent) {
        intents.put(intent.getName(), intent);
    }
    
    /**
     * Gets an intent by name.
     *
     * @param name The intent name
     * @return The intent definition, or null if not found
     */
    public IntentDefinition get(String name) {
        return intents.get(name);
    }
    
    /**
     * Checks if an intent exists.
     *
     * @param name The intent name
     * @return true if the intent exists
     */
    public boolean exists(String name) {
        return intents.containsKey(name);
    }
    
    /**
     * Gets all registered intents.
     *
     * @return Unmodifiable collection of all intent definitions
     */
    public Collection<IntentDefinition> getAll() {
        return Collections.unmodifiableCollection(intents.values());
    }
    
    /**
     * Gets the names of all registered intents.
     *
     * @return Unmodifiable collection of intent names
     */
    public Collection<String> getNames() {
        return Collections.unmodifiableCollection(intents.keySet());
    }
    
    /**
     * Gets the number of registered intents.
     */
    public int size() {
        return intents.size();
    }
    
    /**
     * Removes an intent by name.
     *
     * @param name The intent name
     * @return The removed intent, or null if not found
     */
    public IntentDefinition remove(String name) {
        return intents.remove(name);
    }
    
    /**
     * Clears all registered intents.
     * Primarily for testing purposes.
     */
    public void clear() {
        intents.clear();
    }
}

