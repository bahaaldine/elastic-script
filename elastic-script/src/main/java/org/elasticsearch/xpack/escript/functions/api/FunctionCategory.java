/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.api;

public enum FunctionCategory {
    STRING,
    NUMBER,
    DATE,
    DOCUMENT,
    DATASOURCE,
    ARRAY,
    GEO,
    NLP,
    SECURITY,
    NETWORK,
    STATS,
    UTILITY
}
