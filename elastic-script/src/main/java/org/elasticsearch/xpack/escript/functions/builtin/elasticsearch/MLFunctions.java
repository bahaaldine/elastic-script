/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.builtin.elasticsearch;

import org.elasticsearch.xpack.escript.functions.api.FunctionCollectionSpec;
import org.elasticsearch.xpack.escript.functions.api.FunctionCategory;
import org.elasticsearch.xpack.escript.functions.api.FunctionParam;
import org.elasticsearch.xpack.escript.functions.api.FunctionReturn;
import org.elasticsearch.xpack.escript.functions.api.FunctionSpec;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;
import org.elasticsearch.xpack.core.ml.action.GetJobsAction;
import org.elasticsearch.xpack.core.ml.action.GetJobsStatsAction;
import org.elasticsearch.xpack.core.ml.action.OpenJobAction;
import org.elasticsearch.xpack.core.ml.action.CloseJobAction;
import org.elasticsearch.xpack.core.ml.action.DeleteJobAction;
import org.elasticsearch.xpack.core.ml.action.GetDatafeedsAction;
import org.elasticsearch.xpack.core.ml.action.GetDatafeedsStatsAction;
import org.elasticsearch.xpack.core.ml.action.StartDatafeedAction;
import org.elasticsearch.xpack.core.ml.action.StopDatafeedAction;
import org.elasticsearch.xpack.core.ml.action.GetRecordsAction;
import org.elasticsearch.xpack.core.ml.action.GetBucketsAction;
import org.elasticsearch.xpack.core.ml.action.GetTrainedModelsAction;
import org.elasticsearch.xpack.core.ml.action.GetTrainedModelsStatsAction;
import org.elasticsearch.xpack.core.ml.action.InferModelAction;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch Machine Learning API functions.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.DATASOURCE,
    description = "Elasticsearch Machine Learning API functions for anomaly detection and trained models"
)
public class MLFunctions {

    public static void registerAll(ExecutionContext context, Client client) {
        registerGetJobs(context, client);
        registerGetJobStats(context, client);
        registerOpenJob(context, client);
        registerCloseJob(context, client);
        registerDeleteJob(context, client);
        registerGetDatafeeds(context, client);
        registerGetDatafeedStats(context, client);
        registerStartDatafeed(context, client);
        registerStopDatafeed(context, client);
        registerGetRecords(context, client);
        registerGetBuckets(context, client);
        registerGetTrainedModels(context, client);
        registerGetTrainedModelStats(context, client);
        registerInferTrainedModel(context, client);
    }

    @FunctionSpec(
        name = "ES_ML_GET_JOBS",
        description = "Get anomaly detection jobs",
        parameters = {
            @FunctionParam(name = "job_id", type = "STRING", description = "Job ID or pattern")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Array of job configurations"),
        examples = {"ES_ML_GET_JOBS('*')", "ES_ML_GET_JOBS('my-job')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetJobs(ExecutionContext context, Client client) {
        context.declareFunction("ES_ML_GET_JOBS",
            List.of(new Parameter("job_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_ML_GET_JOBS", (args, listener) -> {
                String jobId = args.size() > 0 && args.get(0) != null ? args.get(0).toString() : "*";
                
                GetJobsAction.Request request = new GetJobsAction.Request(jobId);
                client.execute(GetJobsAction.INSTANCE, request, new ActionListener<GetJobsAction.Response>() {
                    @Override
                    public void onResponse(GetJobsAction.Response response) {
                        List<Map<String, Object>> jobs = new java.util.ArrayList<>();
                        for (var job : response.getResponse().results()) {
                            Map<String, Object> j = new HashMap<>();
                            j.put("job_id", job.getId());
                            j.put("description", job.getDescription());
                            j.put("create_time", job.getCreateTime() != null ? job.getCreateTime().toEpochMilli() : null);
                            j.put("analysis_config", job.getAnalysisConfig() != null ? 
                                Map.of("bucket_span", job.getAnalysisConfig().getBucketSpan().toString()) : null);
                            jobs.add(j);
                        }
                        listener.onResponse(jobs);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_ML_GET_JOB_STATS",
        description = "Get anomaly detection job statistics",
        parameters = {
            @FunctionParam(name = "job_id", type = "STRING", description = "Job ID or pattern")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Array of job stats"),
        examples = {"ES_ML_GET_JOB_STATS('my-job')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetJobStats(ExecutionContext context, Client client) {
        context.declareFunction("ES_ML_GET_JOB_STATS",
            List.of(new Parameter("job_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_ML_GET_JOB_STATS", (args, listener) -> {
                String jobId = args.size() > 0 && args.get(0) != null ? args.get(0).toString() : "*";
                
                GetJobsStatsAction.Request request = new GetJobsStatsAction.Request(jobId);
                client.execute(GetJobsStatsAction.INSTANCE, request, new ActionListener<GetJobsStatsAction.Response>() {
                    @Override
                    public void onResponse(GetJobsStatsAction.Response response) {
                        List<Map<String, Object>> stats = new java.util.ArrayList<>();
                        for (var jobStats : response.getResponse().results()) {
                            Map<String, Object> s = new HashMap<>();
                            s.put("job_id", jobStats.getJobId());
                            s.put("state", jobStats.getState().name());
                            if (jobStats.getDataCounts() != null) {
                                s.put("processed_record_count", jobStats.getDataCounts().getProcessedRecordCount());
                                s.put("input_record_count", jobStats.getDataCounts().getInputRecordCount());
                            }
                            stats.add(s);
                        }
                        listener.onResponse(stats);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_ML_OPEN_JOB",
        description = "Open an anomaly detection job",
        parameters = {
            @FunctionParam(name = "job_id", type = "STRING", description = "Job ID")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if opened"),
        examples = {"ES_ML_OPEN_JOB('my-job')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerOpenJob(ExecutionContext context, Client client) {
        context.declareFunction("ES_ML_OPEN_JOB",
            List.of(new Parameter("job_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_ML_OPEN_JOB", (args, listener) -> {
                String jobId = args.get(0).toString();
                
                OpenJobAction.Request request = new OpenJobAction.Request(jobId);
                client.execute(OpenJobAction.INSTANCE, request, new ActionListener<OpenJobAction.Response>() {
                    @Override
                    public void onResponse(OpenJobAction.Response response) {
                        listener.onResponse(response.isOpened());
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_ML_CLOSE_JOB",
        description = "Close an anomaly detection job",
        parameters = {
            @FunctionParam(name = "job_id", type = "STRING", description = "Job ID")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if closed"),
        examples = {"ES_ML_CLOSE_JOB('my-job')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerCloseJob(ExecutionContext context, Client client) {
        context.declareFunction("ES_ML_CLOSE_JOB",
            List.of(new Parameter("job_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_ML_CLOSE_JOB", (args, listener) -> {
                String jobId = args.get(0).toString();
                
                CloseJobAction.Request request = new CloseJobAction.Request(jobId);
                client.execute(CloseJobAction.INSTANCE, request, new ActionListener<CloseJobAction.Response>() {
                    @Override
                    public void onResponse(CloseJobAction.Response response) {
                        listener.onResponse(response.isClosed());
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_ML_DELETE_JOB",
        description = "Delete an anomaly detection job",
        parameters = {
            @FunctionParam(name = "job_id", type = "STRING", description = "Job ID"),
            @FunctionParam(name = "force", type = "BOOLEAN", description = "Force delete")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if acknowledged"),
        examples = {"ES_ML_DELETE_JOB('my-job', false)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerDeleteJob(ExecutionContext context, Client client) {
        context.declareFunction("ES_ML_DELETE_JOB",
            Arrays.asList(
                new Parameter("job_id", "STRING", ParameterMode.IN),
                new Parameter("force", "BOOLEAN", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_ML_DELETE_JOB", (args, listener) -> {
                String jobId = args.get(0).toString();
                boolean force = args.size() > 1 && Boolean.TRUE.equals(args.get(1));
                
                DeleteJobAction.Request request = new DeleteJobAction.Request(jobId);
                request.setForce(force);
                
                client.execute(DeleteJobAction.INSTANCE, request, new ActionListener<DeleteJobAction.Response>() {
                    @Override
                    public void onResponse(DeleteJobAction.Response response) {
                        listener.onResponse(response.isAcknowledged());
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_ML_GET_DATAFEEDS",
        description = "Get datafeeds",
        parameters = {
            @FunctionParam(name = "datafeed_id", type = "STRING", description = "Datafeed ID or pattern")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Array of datafeed configurations"),
        examples = {"ES_ML_GET_DATAFEEDS('*')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetDatafeeds(ExecutionContext context, Client client) {
        context.declareFunction("ES_ML_GET_DATAFEEDS",
            List.of(new Parameter("datafeed_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_ML_GET_DATAFEEDS", (args, listener) -> {
                String datafeedId = args.size() > 0 && args.get(0) != null ? args.get(0).toString() : "*";
                
                GetDatafeedsAction.Request request = new GetDatafeedsAction.Request(datafeedId);
                client.execute(GetDatafeedsAction.INSTANCE, request, new ActionListener<GetDatafeedsAction.Response>() {
                    @Override
                    public void onResponse(GetDatafeedsAction.Response response) {
                        List<Map<String, Object>> datafeeds = new java.util.ArrayList<>();
                        for (var df : response.getResponse().results()) {
                            Map<String, Object> d = new HashMap<>();
                            d.put("datafeed_id", df.getId());
                            d.put("job_id", df.getJobId());
                            d.put("indices", df.getIndices());
                            datafeeds.add(d);
                        }
                        listener.onResponse(datafeeds);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_ML_GET_DATAFEED_STATS",
        description = "Get datafeed statistics",
        parameters = {
            @FunctionParam(name = "datafeed_id", type = "STRING", description = "Datafeed ID or pattern")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Array of datafeed stats"),
        examples = {"ES_ML_GET_DATAFEED_STATS('datafeed-my-job')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetDatafeedStats(ExecutionContext context, Client client) {
        context.declareFunction("ES_ML_GET_DATAFEED_STATS",
            List.of(new Parameter("datafeed_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_ML_GET_DATAFEED_STATS", (args, listener) -> {
                String datafeedId = args.size() > 0 && args.get(0) != null ? args.get(0).toString() : "*";
                
                GetDatafeedsStatsAction.Request request = new GetDatafeedsStatsAction.Request(datafeedId);
                client.execute(GetDatafeedsStatsAction.INSTANCE, request, new ActionListener<GetDatafeedsStatsAction.Response>() {
                    @Override
                    public void onResponse(GetDatafeedsStatsAction.Response response) {
                        List<Map<String, Object>> stats = new java.util.ArrayList<>();
                        for (var dfStats : response.getResponse().results()) {
                            Map<String, Object> s = new HashMap<>();
                            s.put("datafeed_id", dfStats.getDatafeedId());
                            s.put("state", dfStats.getDatafeedState().name());
                            stats.add(s);
                        }
                        listener.onResponse(stats);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_ML_START_DATAFEED",
        description = "Start a datafeed",
        parameters = {
            @FunctionParam(name = "datafeed_id", type = "STRING", description = "Datafeed ID"),
            @FunctionParam(name = "start", type = "STRING", description = "Start time"),
            @FunctionParam(name = "end", type = "STRING", description = "End time")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if started"),
        examples = {"ES_ML_START_DATAFEED('datafeed-my-job', '2024-01-01', null)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerStartDatafeed(ExecutionContext context, Client client) {
        context.declareFunction("ES_ML_START_DATAFEED",
            Arrays.asList(
                new Parameter("datafeed_id", "STRING", ParameterMode.IN),
                new Parameter("start", "STRING", ParameterMode.IN),
                new Parameter("end", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_ML_START_DATAFEED", (args, listener) -> {
                String datafeedId = args.get(0).toString();
                
                StartDatafeedAction.Request request = new StartDatafeedAction.Request(datafeedId, 0L);
                client.execute(StartDatafeedAction.INSTANCE, request, new ActionListener<StartDatafeedAction.Response>() {
                    @Override
                    public void onResponse(StartDatafeedAction.Response response) {
                        listener.onResponse(response.isStarted());
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_ML_STOP_DATAFEED",
        description = "Stop a datafeed",
        parameters = {
            @FunctionParam(name = "datafeed_id", type = "STRING", description = "Datafeed ID"),
            @FunctionParam(name = "force", type = "BOOLEAN", description = "Force stop")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if stopped"),
        examples = {"ES_ML_STOP_DATAFEED('datafeed-my-job', false)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerStopDatafeed(ExecutionContext context, Client client) {
        context.declareFunction("ES_ML_STOP_DATAFEED",
            Arrays.asList(
                new Parameter("datafeed_id", "STRING", ParameterMode.IN),
                new Parameter("force", "BOOLEAN", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_ML_STOP_DATAFEED", (args, listener) -> {
                String datafeedId = args.get(0).toString();
                boolean force = args.size() > 1 && Boolean.TRUE.equals(args.get(1));
                
                StopDatafeedAction.Request request = new StopDatafeedAction.Request(datafeedId);
                request.setForce(force);
                
                client.execute(StopDatafeedAction.INSTANCE, request, new ActionListener<StopDatafeedAction.Response>() {
                    @Override
                    public void onResponse(StopDatafeedAction.Response response) {
                        listener.onResponse(response.isStopped());
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_ML_GET_RECORDS",
        description = "Get anomaly records from a job",
        parameters = {
            @FunctionParam(name = "job_id", type = "STRING", description = "Job ID"),
            @FunctionParam(name = "start", type = "STRING", description = "Start time"),
            @FunctionParam(name = "end", type = "STRING", description = "End time"),
            @FunctionParam(name = "record_score", type = "NUMBER", description = "Minimum record score")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Anomaly records"),
        examples = {"ES_ML_GET_RECORDS('my-job', '2024-01-01', '2024-01-02', 75)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetRecords(ExecutionContext context, Client client) {
        context.declareFunction("ES_ML_GET_RECORDS",
            Arrays.asList(
                new Parameter("job_id", "STRING", ParameterMode.IN),
                new Parameter("start", "STRING", ParameterMode.IN),
                new Parameter("end", "STRING", ParameterMode.IN),
                new Parameter("record_score", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_ML_GET_RECORDS", (args, listener) -> {
                String jobId = args.get(0).toString();
                
                GetRecordsAction.Request request = new GetRecordsAction.Request(jobId);
                if (args.size() > 3 && args.get(3) != null) {
                    request.setRecordScore(((Number) args.get(3)).doubleValue());
                }
                
                client.execute(GetRecordsAction.INSTANCE, request, new ActionListener<GetRecordsAction.Response>() {
                    @Override
                    public void onResponse(GetRecordsAction.Response response) {
                        List<Map<String, Object>> records = new java.util.ArrayList<>();
                        for (var record : response.getRecords().results()) {
                            Map<String, Object> r = new HashMap<>();
                            r.put("job_id", record.getJobId());
                            r.put("record_score", record.getRecordScore());
                            r.put("timestamp", record.getTimestamp().toEpochMilli());
                            r.put("is_interim", record.isInterim());
                            records.add(r);
                        }
                        listener.onResponse(records);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_ML_GET_BUCKETS",
        description = "Get buckets from a job",
        parameters = {
            @FunctionParam(name = "job_id", type = "STRING", description = "Job ID"),
            @FunctionParam(name = "anomaly_score", type = "NUMBER", description = "Minimum anomaly score")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Buckets"),
        examples = {"ES_ML_GET_BUCKETS('my-job', 50)"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetBuckets(ExecutionContext context, Client client) {
        context.declareFunction("ES_ML_GET_BUCKETS",
            Arrays.asList(
                new Parameter("job_id", "STRING", ParameterMode.IN),
                new Parameter("anomaly_score", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_ML_GET_BUCKETS", (args, listener) -> {
                String jobId = args.get(0).toString();
                
                GetBucketsAction.Request request = new GetBucketsAction.Request(jobId);
                if (args.size() > 1 && args.get(1) != null) {
                    request.setAnomalyScore(((Number) args.get(1)).doubleValue());
                }
                
                client.execute(GetBucketsAction.INSTANCE, request, new ActionListener<GetBucketsAction.Response>() {
                    @Override
                    public void onResponse(GetBucketsAction.Response response) {
                        List<Map<String, Object>> buckets = new java.util.ArrayList<>();
                        for (var bucket : response.getBuckets().results()) {
                            Map<String, Object> b = new HashMap<>();
                            b.put("job_id", bucket.getJobId());
                            b.put("timestamp", bucket.getTimestamp().toEpochMilli());
                            b.put("anomaly_score", bucket.getAnomalyScore());
                            b.put("event_count", bucket.getEventCount());
                            buckets.add(b);
                        }
                        listener.onResponse(buckets);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_ML_GET_TRAINED_MODELS",
        description = "Get trained models",
        parameters = {
            @FunctionParam(name = "model_id", type = "STRING", description = "Model ID or pattern")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Trained models"),
        examples = {"ES_ML_GET_TRAINED_MODELS('*')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetTrainedModels(ExecutionContext context, Client client) {
        context.declareFunction("ES_ML_GET_TRAINED_MODELS",
            List.of(new Parameter("model_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_ML_GET_TRAINED_MODELS", (args, listener) -> {
                String modelId = args.size() > 0 && args.get(0) != null ? args.get(0).toString() : "*";
                
                GetTrainedModelsAction.Request request = new GetTrainedModelsAction.Request(modelId);
                client.execute(GetTrainedModelsAction.INSTANCE, request, new ActionListener<GetTrainedModelsAction.Response>() {
                    @Override
                    public void onResponse(GetTrainedModelsAction.Response response) {
                        List<Map<String, Object>> models = new java.util.ArrayList<>();
                        for (var model : response.getResources().results()) {
                            Map<String, Object> m = new HashMap<>();
                            m.put("model_id", model.getModelId());
                            m.put("model_type", model.getModelType().name());
                            m.put("create_time", model.getCreateTime().toEpochMilli());
                            models.add(m);
                        }
                        listener.onResponse(models);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_ML_GET_TRAINED_MODEL_STATS",
        description = "Get trained model statistics",
        parameters = {
            @FunctionParam(name = "model_id", type = "STRING", description = "Model ID or pattern")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Model stats"),
        examples = {"ES_ML_GET_TRAINED_MODEL_STATS('my-model')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetTrainedModelStats(ExecutionContext context, Client client) {
        context.declareFunction("ES_ML_GET_TRAINED_MODEL_STATS",
            List.of(new Parameter("model_id", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ES_ML_GET_TRAINED_MODEL_STATS", (args, listener) -> {
                String modelId = args.size() > 0 && args.get(0) != null ? args.get(0).toString() : "*";
                
                GetTrainedModelsStatsAction.Request request = new GetTrainedModelsStatsAction.Request(modelId);
                client.execute(GetTrainedModelsStatsAction.INSTANCE, request, new ActionListener<GetTrainedModelsStatsAction.Response>() {
                    @Override
                    public void onResponse(GetTrainedModelsStatsAction.Response response) {
                        List<Map<String, Object>> stats = new java.util.ArrayList<>();
                        for (var modelStats : response.getResources().results()) {
                            Map<String, Object> s = new HashMap<>();
                            s.put("model_id", modelStats.getModelId());
                            s.put("pipeline_count", modelStats.getPipelineCount());
                            stats.add(s);
                        }
                        listener.onResponse(stats);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }

    @FunctionSpec(
        name = "ES_ML_INFER",
        description = "Run inference on a trained model",
        parameters = {
            @FunctionParam(name = "model_id", type = "STRING", description = "Model ID"),
            @FunctionParam(name = "docs", type = "ARRAY", description = "Documents to infer")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Inference results"),
        examples = {"ES_ML_INFER('my-model', [{'text': 'hello world'}])"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerInferTrainedModel(ExecutionContext context, Client client) {
        context.declareFunction("ES_ML_INFER",
            Arrays.asList(
                new Parameter("model_id", "STRING", ParameterMode.IN),
                new Parameter("docs", "ARRAY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ES_ML_INFER", (args, listener) -> {
                String modelId = args.get(0).toString();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> docs = (List<Map<String, Object>>) args.get(1);
                
                InferModelAction.Request request = InferModelAction.Request.forTextInput(
                    modelId, 
                    org.elasticsearch.xpack.core.ml.inference.trainedmodel.TextClassificationConfigUpdate.EMPTY_UPDATE,
                    docs.stream().map(d -> d.toString()).toList()
                );
                
                client.execute(InferModelAction.INSTANCE, request, new ActionListener<InferModelAction.Response>() {
                    @Override
                    public void onResponse(InferModelAction.Response response) {
                        List<Map<String, Object>> results = new java.util.ArrayList<>();
                        for (var result : response.getInferenceResults()) {
                            Map<String, Object> r = new HashMap<>();
                            r.put("result", result.asMap());
                            results.add(r);
                        }
                        listener.onResponse(results);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            })
        );
    }
}
