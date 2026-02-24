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
 * Kibana ML API functions for elastic-script.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.RUNBOOK,
    description = "Machine Learning job management functions."
)
public class MlFunctions {

    public static void registerAll(ExecutionContext context) {
        // Anomaly Detection Jobs
        registerAnomalyJobList(context);
        registerAnomalyJobGet(context);
        registerAnomalyJobCreate(context);
        registerAnomalyJobDelete(context);
        registerAnomalyJobOpen(context);
        registerAnomalyJobClose(context);
        registerAnomalyJobStart(context);
        registerAnomalyJobStop(context);
        registerAnomalyJobResults(context);
        
        // Data Frame Analytics
        registerDfaJobList(context);
        registerDfaJobCreate(context);
        registerDfaJobStart(context);
        registerDfaJobStop(context);
        
        // Trained Models
        registerTrainedModelList(context);
        registerTrainedModelGet(context);
        registerTrainedModelDelete(context);
        registerTrainedModelDeploy(context);
        registerTrainedModelUndeploy(context);
    }

    // ========================================================================
    // Anomaly Detection Jobs
    // ========================================================================

    public static void registerAnomalyJobList(ExecutionContext context) {
        context.declareFunction("ML_ANOMALY_JOB_LIST",
            List.of(),
            new BuiltInFunctionDefinition("ML_ANOMALY_JOB_LIST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/ml/anomaly_detectors", null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ML_ANOMALY_JOB_LIST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAnomalyJobGet(ExecutionContext context) {
        context.declareFunction("ML_ANOMALY_JOB_GET",
            List.of(new Parameter("job_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ML_ANOMALY_JOB_GET", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String jobId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/ml/anomaly_detectors/" + jobId, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ML_ANOMALY_JOB_GET failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAnomalyJobCreate(ExecutionContext context) {
        context.declareFunction("ML_ANOMALY_JOB_CREATE",
            List.of(
                new Parameter("job_id", "STRING", ParameterMode.IN),
                new Parameter("config", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ML_ANOMALY_JOB_CREATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String jobId = args.get(0).toString();
                    Map<String, Object> config = toMap(args.get(1));
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "PUT", "/api/ml/anomaly_detectors/" + jobId, config);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ML_ANOMALY_JOB_CREATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAnomalyJobDelete(ExecutionContext context) {
        context.declareFunction("ML_ANOMALY_JOB_DELETE",
            List.of(new Parameter("job_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ML_ANOMALY_JOB_DELETE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String jobId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "DELETE", "/api/ml/anomaly_detectors/" + jobId, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ML_ANOMALY_JOB_DELETE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAnomalyJobOpen(ExecutionContext context) {
        context.declareFunction("ML_ANOMALY_JOB_OPEN",
            List.of(new Parameter("job_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ML_ANOMALY_JOB_OPEN", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String jobId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/ml/anomaly_detectors/" + jobId + "/_open", new HashMap<>());
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ML_ANOMALY_JOB_OPEN failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAnomalyJobClose(ExecutionContext context) {
        context.declareFunction("ML_ANOMALY_JOB_CLOSE",
            List.of(new Parameter("job_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ML_ANOMALY_JOB_CLOSE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String jobId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/ml/anomaly_detectors/" + jobId + "/_close", new HashMap<>());
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ML_ANOMALY_JOB_CLOSE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAnomalyJobStart(ExecutionContext context) {
        context.declareFunction("ML_ANOMALY_JOB_START",
            List.of(
                new Parameter("job_id", "STRING", ParameterMode.IN),
                new Parameter("start", "STRING", ParameterMode.IN),
                new Parameter("end", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ML_ANOMALY_JOB_START", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String jobId = args.get(0).toString();
                    String start = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : "0";
                    String end = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : null;
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("start", start);
                    if (end != null) {
                        body.put("end", end);
                    }
                    
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/ml/anomaly_detectors/" + jobId + "/datafeed/_start", body);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ML_ANOMALY_JOB_START failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAnomalyJobStop(ExecutionContext context) {
        context.declareFunction("ML_ANOMALY_JOB_STOP",
            List.of(new Parameter("job_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ML_ANOMALY_JOB_STOP", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String jobId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/ml/anomaly_detectors/" + jobId + "/datafeed/_stop", new HashMap<>());
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ML_ANOMALY_JOB_STOP failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAnomalyJobResults(ExecutionContext context) {
        context.declareFunction("ML_ANOMALY_JOB_RESULTS",
            List.of(
                new Parameter("job_id", "STRING", ParameterMode.IN),
                new Parameter("result_type", "STRING", ParameterMode.IN),
                new Parameter("size", "INTEGER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ML_ANOMALY_JOB_RESULTS", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String jobId = args.get(0).toString();
                    String resultType = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : "records";
                    int size = args.size() > 2 && args.get(2) != null ? ((Number) args.get(2)).intValue() : 100;
                    
                    String path = "/api/ml/anomaly_detectors/" + jobId + "/results/" + resultType + "?size=" + size;
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", path, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ML_ANOMALY_JOB_RESULTS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    // ========================================================================
    // Data Frame Analytics
    // ========================================================================

    public static void registerDfaJobList(ExecutionContext context) {
        context.declareFunction("ML_DFA_JOB_LIST",
            List.of(),
            new BuiltInFunctionDefinition("ML_DFA_JOB_LIST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/ml/data_frame/analytics", null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ML_DFA_JOB_LIST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerDfaJobCreate(ExecutionContext context) {
        context.declareFunction("ML_DFA_JOB_CREATE",
            List.of(
                new Parameter("job_id", "STRING", ParameterMode.IN),
                new Parameter("config", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ML_DFA_JOB_CREATE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String jobId = args.get(0).toString();
                    Map<String, Object> config = toMap(args.get(1));
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "PUT", "/api/ml/data_frame/analytics/" + jobId, config);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ML_DFA_JOB_CREATE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerDfaJobStart(ExecutionContext context) {
        context.declareFunction("ML_DFA_JOB_START",
            List.of(new Parameter("job_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ML_DFA_JOB_START", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String jobId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/ml/data_frame/analytics/" + jobId + "/_start", new HashMap<>());
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ML_DFA_JOB_START failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerDfaJobStop(ExecutionContext context) {
        context.declareFunction("ML_DFA_JOB_STOP",
            List.of(new Parameter("job_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ML_DFA_JOB_STOP", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String jobId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/ml/data_frame/analytics/" + jobId + "/_stop", new HashMap<>());
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ML_DFA_JOB_STOP failed: " + e.getMessage(), e));
                }
            })
        );
    }

    // ========================================================================
    // Trained Models
    // ========================================================================

    public static void registerTrainedModelList(ExecutionContext context) {
        context.declareFunction("ML_TRAINED_MODEL_LIST",
            List.of(),
            new BuiltInFunctionDefinition("ML_TRAINED_MODEL_LIST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/ml/trained_models", null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ML_TRAINED_MODEL_LIST failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerTrainedModelGet(ExecutionContext context) {
        context.declareFunction("ML_TRAINED_MODEL_GET",
            List.of(new Parameter("model_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ML_TRAINED_MODEL_GET", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String modelId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "GET", "/api/ml/trained_models/" + modelId, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ML_TRAINED_MODEL_GET failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerTrainedModelDelete(ExecutionContext context) {
        context.declareFunction("ML_TRAINED_MODEL_DELETE",
            List.of(new Parameter("model_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ML_TRAINED_MODEL_DELETE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String modelId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "DELETE", "/api/ml/trained_models/" + modelId, null);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ML_TRAINED_MODEL_DELETE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerTrainedModelDeploy(ExecutionContext context) {
        context.declareFunction("ML_TRAINED_MODEL_DEPLOY",
            List.of(new Parameter("model_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ML_TRAINED_MODEL_DEPLOY", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String modelId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/ml/trained_models/" + modelId + "/deployment/_start", new HashMap<>());
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ML_TRAINED_MODEL_DEPLOY failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerTrainedModelUndeploy(ExecutionContext context) {
        context.declareFunction("ML_TRAINED_MODEL_UNDEPLOY",
            List.of(new Parameter("model_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ML_TRAINED_MODEL_UNDEPLOY", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String modelId = args.get(0).toString();
                    Map<String, Object> result = kibanaRequest(getKibanaUrl(), "POST", "/api/ml/trained_models/" + modelId + "/deployment/_stop", new HashMap<>());
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ML_TRAINED_MODEL_UNDEPLOY failed: " + e.getMessage(), e));
                }
            })
        );
    }
}
