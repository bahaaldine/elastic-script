/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.builtin.elasticsearch;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;
import org.elasticsearch.xpack.escript.functions.api.FunctionCategory;
import org.elasticsearch.xpack.escript.functions.api.FunctionCollectionSpec;
import org.elasticsearch.xpack.escript.functions.api.FunctionParam;
import org.elasticsearch.xpack.escript.functions.api.FunctionReturn;
import org.elasticsearch.xpack.escript.functions.api.FunctionSpec;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch Machine Learning API functions using REST HTTP calls.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.DATASOURCE,
    description = "Elasticsearch Machine Learning API functions."
)
public class MLApiFunctions extends ElasticsearchHttpFunctions {

    public static void registerAll(ExecutionContext context) {
        registerGetJobs(context);
        registerGetJobStats(context);
        registerOpenJob(context);
        registerCloseJob(context);
        registerDeleteJob(context);
        registerGetDatafeeds(context);
        registerGetDatafeedStats(context);
        registerStartDatafeed(context);
        registerStopDatafeed(context);
        registerGetRecords(context);
        registerGetBuckets(context);
        registerGetTrainedModels(context);
        registerGetTrainedModelStats(context);
        registerInfer(context);
    }

    @FunctionSpec(
        name = "ES_ML_GET_JOBS",
        description = "Get ML anomaly detection jobs",
        parameters = {
            @FunctionParam(name = "job_id", type = "STRING", description = "Job ID or _all")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Job information"),
        examples = {"ES_ML_GET_JOBS('my-job')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetJobs(ExecutionContext context) {
        context.declareFunction("ES_ML_GET_JOBS",
            List.of(new Parameter("job_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_ML_GET_JOBS", (args, listener) -> {
                try {
                    String jobId = args.size() > 0 && args.get(0) != null ? toString(args.get(0)) : "_all";
                    Map<String, Object> result = esRequest("GET", "/_ml/anomaly_detectors/" + jobId, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_ML_GET_JOBS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_ML_GET_JOB_STATS",
        description = "Get ML job statistics",
        parameters = {
            @FunctionParam(name = "job_id", type = "STRING", description = "Job ID or _all")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Job statistics"),
        examples = {"ES_ML_GET_JOB_STATS('my-job')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetJobStats(ExecutionContext context) {
        context.declareFunction("ES_ML_GET_JOB_STATS",
            List.of(new Parameter("job_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_ML_GET_JOB_STATS", (args, listener) -> {
                try {
                    String jobId = args.size() > 0 && args.get(0) != null ? toString(args.get(0)) : "_all";
                    Map<String, Object> result = esRequest("GET", "/_ml/anomaly_detectors/" + jobId + "/_stats", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_ML_GET_JOB_STATS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_ML_OPEN_JOB",
        description = "Open an ML job",
        parameters = {
            @FunctionParam(name = "job_id", type = "STRING", description = "Job ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Open result"),
        examples = {"ES_ML_OPEN_JOB('my-job')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerOpenJob(ExecutionContext context) {
        context.declareFunction("ES_ML_OPEN_JOB",
            List.of(new Parameter("job_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_ML_OPEN_JOB", (args, listener) -> {
                try {
                    String jobId = toString(args.get(0));
                    Map<String, Object> result = esRequest("POST", "/_ml/anomaly_detectors/" + jobId + "/_open", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_ML_OPEN_JOB failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_ML_CLOSE_JOB",
        description = "Close an ML job",
        parameters = {
            @FunctionParam(name = "job_id", type = "STRING", description = "Job ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Close result"),
        examples = {"ES_ML_CLOSE_JOB('my-job')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerCloseJob(ExecutionContext context) {
        context.declareFunction("ES_ML_CLOSE_JOB",
            List.of(new Parameter("job_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_ML_CLOSE_JOB", (args, listener) -> {
                try {
                    String jobId = toString(args.get(0));
                    Map<String, Object> result = esRequest("POST", "/_ml/anomaly_detectors/" + jobId + "/_close", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_ML_CLOSE_JOB failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_ML_DELETE_JOB",
        description = "Delete an ML job",
        parameters = {
            @FunctionParam(name = "job_id", type = "STRING", description = "Job ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Delete result"),
        examples = {"ES_ML_DELETE_JOB('my-job')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDeleteJob(ExecutionContext context) {
        context.declareFunction("ES_ML_DELETE_JOB",
            List.of(new Parameter("job_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_ML_DELETE_JOB", (args, listener) -> {
                try {
                    String jobId = toString(args.get(0));
                    Map<String, Object> result = esRequest("DELETE", "/_ml/anomaly_detectors/" + jobId, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_ML_DELETE_JOB failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_ML_GET_DATAFEEDS",
        description = "Get ML datafeeds",
        parameters = {
            @FunctionParam(name = "datafeed_id", type = "STRING", description = "Datafeed ID or _all")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Datafeed information"),
        examples = {"ES_ML_GET_DATAFEEDS('my-datafeed')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetDatafeeds(ExecutionContext context) {
        context.declareFunction("ES_ML_GET_DATAFEEDS",
            List.of(new Parameter("datafeed_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_ML_GET_DATAFEEDS", (args, listener) -> {
                try {
                    String datafeedId = args.size() > 0 && args.get(0) != null ? toString(args.get(0)) : "_all";
                    Map<String, Object> result = esRequest("GET", "/_ml/datafeeds/" + datafeedId, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_ML_GET_DATAFEEDS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_ML_GET_DATAFEED_STATS",
        description = "Get ML datafeed statistics",
        parameters = {
            @FunctionParam(name = "datafeed_id", type = "STRING", description = "Datafeed ID or _all")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Datafeed statistics"),
        examples = {"ES_ML_GET_DATAFEED_STATS('my-datafeed')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetDatafeedStats(ExecutionContext context) {
        context.declareFunction("ES_ML_GET_DATAFEED_STATS",
            List.of(new Parameter("datafeed_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_ML_GET_DATAFEED_STATS", (args, listener) -> {
                try {
                    String datafeedId = args.size() > 0 && args.get(0) != null ? toString(args.get(0)) : "_all";
                    Map<String, Object> result = esRequest("GET", "/_ml/datafeeds/" + datafeedId + "/_stats", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_ML_GET_DATAFEED_STATS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_ML_START_DATAFEED",
        description = "Start an ML datafeed",
        parameters = {
            @FunctionParam(name = "datafeed_id", type = "STRING", description = "Datafeed ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Start result"),
        examples = {"ES_ML_START_DATAFEED('my-datafeed')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerStartDatafeed(ExecutionContext context) {
        context.declareFunction("ES_ML_START_DATAFEED",
            List.of(new Parameter("datafeed_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_ML_START_DATAFEED", (args, listener) -> {
                try {
                    String datafeedId = toString(args.get(0));
                    Map<String, Object> result = esRequest("POST", "/_ml/datafeeds/" + datafeedId + "/_start", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_ML_START_DATAFEED failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_ML_STOP_DATAFEED",
        description = "Stop an ML datafeed",
        parameters = {
            @FunctionParam(name = "datafeed_id", type = "STRING", description = "Datafeed ID")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Stop result"),
        examples = {"ES_ML_STOP_DATAFEED('my-datafeed')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerStopDatafeed(ExecutionContext context) {
        context.declareFunction("ES_ML_STOP_DATAFEED",
            List.of(new Parameter("datafeed_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_ML_STOP_DATAFEED", (args, listener) -> {
                try {
                    String datafeedId = toString(args.get(0));
                    Map<String, Object> result = esRequest("POST", "/_ml/datafeeds/" + datafeedId + "/_stop", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_ML_STOP_DATAFEED failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_ML_GET_RECORDS",
        description = "Get ML anomaly records",
        parameters = {
            @FunctionParam(name = "job_id", type = "STRING", description = "Job ID"),
            @FunctionParam(name = "start", type = "STRING", description = "Start time"),
            @FunctionParam(name = "end", type = "STRING", description = "End time")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Anomaly records"),
        examples = {"ES_ML_GET_RECORDS('my-job', 'now-1d', 'now')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetRecords(ExecutionContext context) {
        context.declareFunction("ES_ML_GET_RECORDS",
            List.of(
                new Parameter("job_id", "STRING", ParameterMode.IN),
                new Parameter("start", "STRING", ParameterMode.IN),
                new Parameter("end", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_ML_GET_RECORDS", (args, listener) -> {
                try {
                    String jobId = toString(args.get(0));
                    String start = args.size() > 1 && args.get(1) != null ? toString(args.get(1)) : "";
                    String end = args.size() > 2 && args.get(2) != null ? toString(args.get(2)) : "";
                    
                    Map<String, Object> body = new HashMap<>();
                    if (!start.isEmpty()) body.put("start", start);
                    if (!end.isEmpty()) body.put("end", end);
                    
                    Map<String, Object> result = esRequest("POST", "/_ml/anomaly_detectors/" + jobId + "/results/records", 
                        body.isEmpty() ? null : body);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_ML_GET_RECORDS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_ML_GET_BUCKETS",
        description = "Get ML result buckets",
        parameters = {
            @FunctionParam(name = "job_id", type = "STRING", description = "Job ID"),
            @FunctionParam(name = "start", type = "STRING", description = "Start time"),
            @FunctionParam(name = "end", type = "STRING", description = "End time")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Result buckets"),
        examples = {"ES_ML_GET_BUCKETS('my-job', 'now-1d', 'now')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetBuckets(ExecutionContext context) {
        context.declareFunction("ES_ML_GET_BUCKETS",
            List.of(
                new Parameter("job_id", "STRING", ParameterMode.IN),
                new Parameter("start", "STRING", ParameterMode.IN),
                new Parameter("end", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_ML_GET_BUCKETS", (args, listener) -> {
                try {
                    String jobId = toString(args.get(0));
                    String start = args.size() > 1 && args.get(1) != null ? toString(args.get(1)) : "";
                    String end = args.size() > 2 && args.get(2) != null ? toString(args.get(2)) : "";
                    
                    Map<String, Object> body = new HashMap<>();
                    if (!start.isEmpty()) body.put("start", start);
                    if (!end.isEmpty()) body.put("end", end);
                    
                    Map<String, Object> result = esRequest("POST", "/_ml/anomaly_detectors/" + jobId + "/results/buckets", 
                        body.isEmpty() ? null : body);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_ML_GET_BUCKETS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_ML_GET_TRAINED_MODELS",
        description = "Get trained ML models",
        parameters = {
            @FunctionParam(name = "model_id", type = "STRING", description = "Model ID or _all")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Trained models"),
        examples = {"ES_ML_GET_TRAINED_MODELS('my-model')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetTrainedModels(ExecutionContext context) {
        context.declareFunction("ES_ML_GET_TRAINED_MODELS",
            List.of(new Parameter("model_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_ML_GET_TRAINED_MODELS", (args, listener) -> {
                try {
                    String modelId = args.size() > 0 && args.get(0) != null ? toString(args.get(0)) : "_all";
                    Map<String, Object> result = esRequest("GET", "/_ml/trained_models/" + modelId, null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_ML_GET_TRAINED_MODELS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_ML_GET_TRAINED_MODEL_STATS",
        description = "Get trained model statistics",
        parameters = {
            @FunctionParam(name = "model_id", type = "STRING", description = "Model ID or _all")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Model statistics"),
        examples = {"ES_ML_GET_TRAINED_MODEL_STATS('my-model')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetTrainedModelStats(ExecutionContext context) {
        context.declareFunction("ES_ML_GET_TRAINED_MODEL_STATS",
            List.of(new Parameter("model_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_ML_GET_TRAINED_MODEL_STATS", (args, listener) -> {
                try {
                    String modelId = args.size() > 0 && args.get(0) != null ? toString(args.get(0)) : "_all";
                    Map<String, Object> result = esRequest("GET", "/_ml/trained_models/" + modelId + "/_stats", null);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_ML_GET_TRAINED_MODEL_STATS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ES_ML_INFER",
        description = "Run inference on a trained model",
        parameters = {
            @FunctionParam(name = "model_id", type = "STRING", description = "Model ID"),
            @FunctionParam(name = "docs", type = "ARRAY", description = "Documents to infer on")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Inference results"),
        examples = {"ES_ML_INFER('my-model', [{'text': 'sample text'}])"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerInfer(ExecutionContext context) {
        context.declareFunction("ES_ML_INFER",
            List.of(
                new Parameter("model_id", "STRING", ParameterMode.IN),
                new Parameter("docs", "ARRAY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_ML_INFER", (args, listener) -> {
                try {
                    String modelId = toString(args.get(0));
                    List<?> docs = (List<?>) args.get(1);
                    
                    Map<String, Object> body = new HashMap<>();
                    body.put("docs", docs);
                    
                    Map<String, Object> result = esRequest("POST", "/_ml/trained_models/" + modelId + "/_infer", body);
                    listener.onResponse(result.get("success").equals(true) ? result.get("data") : result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("ES_ML_INFER failed: " + e.getMessage(), e));
                }
            })
        );
    }
}
