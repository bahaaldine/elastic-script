/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.builtin.kibana;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;
import org.elasticsearch.xpack.escript.functions.api.FunctionCategory;
import org.elasticsearch.xpack.escript.functions.api.FunctionCollectionSpec;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.xpack.escript.functions.builtin.kibana.KibanaFunctions.*;

/**
 * Kibana Connectors API functions for elastic-script.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.RUNBOOK,
    description = "Kibana Connectors functions for managing action connectors."
)
public class ConnectorFunctions {

    public static void registerAll(ExecutionContext context) {
        registerConnectorList(context);
        registerConnectorGet(context);
        registerConnectorCreate(context);
        registerConnectorUpdate(context);
        registerConnectorDelete(context);
        registerConnectorExecute(context);
        registerConnectorTypes(context);
    }

    public static void registerConnectorList(ExecutionContext context) {
        context.declareFunction("CONNECTOR_LIST",
            List.of(),
            new BuiltInFunctionDefinition("CONNECTOR_LIST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/actions/connectors", null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("CONNECTOR_LIST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerConnectorGet(ExecutionContext context) {
        context.declareFunction("CONNECTOR_GET",
            List.of(new Parameter("connector_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("CONNECTOR_GET", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String connectorId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/actions/connector/" + connectorId, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("CONNECTOR_GET failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerConnectorCreate(ExecutionContext context) {
        context.declareFunction("CONNECTOR_CREATE",
            List.of(
                new Parameter("connector_type", "STRING", ParameterMode.IN),
                new Parameter("name", "STRING", ParameterMode.IN),
                new Parameter("config", "DOCUMENT", ParameterMode.IN),
                new Parameter("secrets", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("CONNECTOR_CREATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String connectorType = args.get(0).toString();
                    String name = args.get(1).toString();
                    Map<String, Object> config = args.size() > 2 && args.get(2) != null ? toMap(args.get(2)) : new HashMap<>();
                    Map<String, Object> secrets = args.size() > 3 && args.get(3) != null ? toMap(args.get(3)) : new HashMap<>();
                    
                    Map<String, Object> connectorData = new HashMap<>();
                    connectorData.put("connector_type_id", connectorType);
                    connectorData.put("name", name);
                    connectorData.put("config", config);
                    connectorData.put("secrets", secrets);
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/actions/connector", connectorData);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("CONNECTOR_CREATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerConnectorUpdate(ExecutionContext context) {
        context.declareFunction("CONNECTOR_UPDATE",
            List.of(
                new Parameter("connector_id", "STRING", ParameterMode.IN),
                new Parameter("name", "STRING", ParameterMode.IN),
                new Parameter("config", "DOCUMENT", ParameterMode.IN),
                new Parameter("secrets", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("CONNECTOR_UPDATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String connectorId = args.get(0).toString();
                    String name = args.get(1).toString();
                    Map<String, Object> config = args.size() > 2 && args.get(2) != null ? toMap(args.get(2)) : new HashMap<>();
                    Map<String, Object> secrets = args.size() > 3 && args.get(3) != null ? toMap(args.get(3)) : new HashMap<>();
                    
                    Map<String, Object> connectorData = new HashMap<>();
                    connectorData.put("name", name);
                    connectorData.put("config", config);
                    connectorData.put("secrets", secrets);
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "PUT", "/api/actions/connector/" + connectorId, connectorData);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("CONNECTOR_UPDATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerConnectorDelete(ExecutionContext context) {
        context.declareFunction("CONNECTOR_DELETE",
            List.of(new Parameter("connector_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("CONNECTOR_DELETE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String connectorId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "DELETE", "/api/actions/connector/" + connectorId, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("CONNECTOR_DELETE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerConnectorExecute(ExecutionContext context) {
        context.declareFunction("CONNECTOR_EXECUTE",
            List.of(
                new Parameter("connector_id", "STRING", ParameterMode.IN),
                new Parameter("params", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("CONNECTOR_EXECUTE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String connectorId = args.get(0).toString();
                    Map<String, Object> params = args.size() > 1 && args.get(1) != null ? toMap(args.get(1)) : new HashMap<>();
                    
                    Map<String, Object> body = Map.of("params", params);
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/actions/connector/" + connectorId + "/_execute", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("CONNECTOR_EXECUTE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerConnectorTypes(ExecutionContext context) {
        context.declareFunction("CONNECTOR_TYPES",
            List.of(),
            new BuiltInFunctionDefinition("CONNECTOR_TYPES", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/actions/connector_types", null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("CONNECTOR_TYPES failed: " + e.getMessage(), e));
                }
            })
        );
    }
}
