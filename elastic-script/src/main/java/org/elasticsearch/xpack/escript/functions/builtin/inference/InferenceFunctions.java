/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.builtin.inference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.inference.InferenceServiceResults;
import org.elasticsearch.inference.TaskType;
import org.elasticsearch.inference.UnifiedCompletionRequest;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xpack.core.inference.action.DeleteInferenceEndpointAction;
import org.elasticsearch.xpack.core.inference.action.GetInferenceModelAction;
import org.elasticsearch.xpack.core.inference.action.InferenceAction;
import org.elasticsearch.xpack.core.inference.action.PutInferenceModelAction;
import org.elasticsearch.xpack.core.inference.action.UnifiedCompletionAction;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;
import org.elasticsearch.xpack.escript.functions.api.FunctionCategory;
import org.elasticsearch.xpack.escript.functions.api.FunctionCollectionSpec;
import org.elasticsearch.xpack.escript.functions.api.FunctionParam;
import org.elasticsearch.xpack.escript.functions.api.FunctionReturn;
import org.elasticsearch.xpack.escript.functions.api.FunctionSpec;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch Inference API integration functions for elastic-script.
 * Provides access to configured inference endpoints (EIS, OpenAI, Azure, etc.)
 * without requiring credentials in scripts.
 * 
 * These functions use the ES Inference API internally, which means:
 * - All configured inference endpoints are available
 * - Proper ES security and auditing
 * - Rate limiting and monitoring built-in
 */
@FunctionCollectionSpec(
    category = FunctionCategory.NLP,
    description = "Elasticsearch Inference API functions for accessing configured inference endpoints."
)
public class InferenceFunctions {

    private static final Logger LOGGER = LogManager.getLogger(InferenceFunctions.class);
    private static final TimeValue DEFAULT_TIMEOUT = TimeValue.timeValueSeconds(30);

    /**
     * Registers all inference functions with the execution context.
     * Requires an ES client to execute inference actions.
     */
    public static void registerAll(ExecutionContext context, Client client) {
        LOGGER.info("Registering Inference API built-in functions");
        // Endpoint management
        registerInferenceCreateEndpoint(context, client);
        registerInferenceDeleteEndpoint(context, client);
        registerInferenceListEndpoints(context, client);
        registerInferenceGetEndpoint(context, client);
        // Inference operations
        registerInference(context, client);
        registerInferenceChat(context, client);
        registerInferenceEmbed(context, client);
        registerInferenceRerank(context, client);
    }

    // ========================================================================
    // ENDPOINT MANAGEMENT FUNCTIONS
    // ========================================================================

    /**
     * INFERENCE_CREATE_ENDPOINT - Creates a new inference endpoint.
     */
    @FunctionSpec(
        name = "INFERENCE_CREATE_ENDPOINT",
        description = "Creates a new inference endpoint with the specified configuration. " +
            "The endpoint can then be used with INFERENCE, INFERENCE_CHAT, etc.",
        parameters = {
            @FunctionParam(name = "endpoint_id", type = "STRING", description = "Unique identifier for the endpoint"),
            @FunctionParam(name = "task_type", type = "STRING", description = "Task type: completion, chat_completion, text_embedding, rerank, sparse_embedding"),
            @FunctionParam(name = "config_json", type = "STRING", description = "JSON configuration including service and service_settings")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "true if endpoint was created successfully"),
        examples = {
            "INFERENCE_CREATE_ENDPOINT('my-openai', 'completion', '{\"service\":\"openai\",\"service_settings\":{\"api_key\":\"sk-...\",\"model_id\":\"gpt-4o-mini\"}}')",
            "INFERENCE_CREATE_ENDPOINT('my-embeddings', 'text_embedding', '{\"service\":\"openai\",\"service_settings\":{\"api_key\":\"sk-...\",\"model_id\":\"text-embedding-3-small\"}}')"
        },
        category = FunctionCategory.DATASOURCE
    )
    public static void registerInferenceCreateEndpoint(ExecutionContext context, Client client) {
        context.declareFunction("INFERENCE_CREATE_ENDPOINT",
            List.of(
                new Parameter("endpoint_id", "STRING", ParameterMode.IN),
                new Parameter("task_type", "STRING", ParameterMode.IN),
                new Parameter("config_json", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("INFERENCE_CREATE_ENDPOINT", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    if (args.size() < 3) {
                        listener.onFailure(new RuntimeException(
                            "INFERENCE_CREATE_ENDPOINT requires 3 arguments: endpoint_id, task_type, config_json"));
                        return;
                    }

                    String endpointId = args.get(0).toString();
                    String taskTypeStr = args.get(1).toString().toUpperCase();
                    String configJson = args.get(2).toString();
                    
                    TaskType taskType = parseTaskType(taskTypeStr);
                    
                    LOGGER.info("Creating inference endpoint [{}] with task type [{}]", endpointId, taskType);

                    PutInferenceModelAction.Request request = new PutInferenceModelAction.Request(
                        taskType,
                        endpointId,
                        new BytesArray(configJson),
                        XContentType.JSON,
                        DEFAULT_TIMEOUT
                    );

                    client.execute(PutInferenceModelAction.INSTANCE, request, new ActionListener<PutInferenceModelAction.Response>() {
                        @Override
                        public void onResponse(PutInferenceModelAction.Response response) {
                            LOGGER.info("Successfully created inference endpoint [{}]", endpointId);
                            listener.onResponse(true);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            LOGGER.error("Failed to create inference endpoint [{}]: {}", endpointId, e.getMessage());
                            listener.onFailure(new RuntimeException(
                                "Failed to create inference endpoint '" + endpointId + "': " + e.getMessage(), e));
                        }
                    });

                } catch (Exception e) {
                    listener.onFailure(e);
                }
            })
        );
    }

    /**
     * INFERENCE_DELETE_ENDPOINT - Deletes an inference endpoint.
     */
    @FunctionSpec(
        name = "INFERENCE_DELETE_ENDPOINT",
        description = "Deletes an existing inference endpoint.",
        parameters = {
            @FunctionParam(name = "endpoint_id", type = "STRING", description = "The endpoint ID to delete"),
            @FunctionParam(name = "force", type = "BOOLEAN", description = "Force delete even if endpoint is in use. Optional, defaults to false")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "true if endpoint was deleted successfully"),
        examples = {
            "INFERENCE_DELETE_ENDPOINT('my-old-endpoint')",
            "INFERENCE_DELETE_ENDPOINT('unused-endpoint', true)"
        },
        category = FunctionCategory.DATASOURCE
    )
    public static void registerInferenceDeleteEndpoint(ExecutionContext context, Client client) {
        context.declareFunction("INFERENCE_DELETE_ENDPOINT",
            List.of(
                new Parameter("endpoint_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("INFERENCE_DELETE_ENDPOINT", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    if (args.isEmpty()) {
                        listener.onFailure(new RuntimeException(
                            "INFERENCE_DELETE_ENDPOINT requires at least 1 argument: endpoint_id"));
                        return;
                    }

                    String endpointId = args.get(0).toString();
                    boolean force = args.size() > 1 && Boolean.parseBoolean(args.get(1).toString());
                    
                    LOGGER.info("Deleting inference endpoint [{}], force=[{}]", endpointId, force);

                    DeleteInferenceEndpointAction.Request request = new DeleteInferenceEndpointAction.Request(
                        endpointId,
                        TaskType.ANY,
                        force,
                        false  // dryRun
                    );

                    client.execute(DeleteInferenceEndpointAction.INSTANCE, request, new ActionListener<DeleteInferenceEndpointAction.Response>() {
                        @Override
                        public void onResponse(DeleteInferenceEndpointAction.Response response) {
                            LOGGER.info("Successfully deleted inference endpoint [{}]", endpointId);
                            listener.onResponse(true);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            LOGGER.error("Failed to delete inference endpoint [{}]: {}", endpointId, e.getMessage());
                            listener.onFailure(new RuntimeException(
                                "Failed to delete inference endpoint '" + endpointId + "': " + e.getMessage(), e));
                        }
                    });

                } catch (Exception e) {
                    listener.onFailure(e);
                }
            })
        );
    }

    /**
     * INFERENCE_LIST_ENDPOINTS - Lists all inference endpoints.
     */
    @FunctionSpec(
        name = "INFERENCE_LIST_ENDPOINTS",
        description = "Lists all configured inference endpoints. Optionally filter by task type.",
        parameters = {
            @FunctionParam(name = "task_type", type = "STRING", description = "Filter by task type (optional). Use '_all' for all types.")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Array of endpoint configurations"),
        examples = {
            "INFERENCE_LIST_ENDPOINTS()",
            "INFERENCE_LIST_ENDPOINTS('completion')",
            "INFERENCE_LIST_ENDPOINTS('text_embedding')"
        },
        category = FunctionCategory.DATASOURCE
    )
    public static void registerInferenceListEndpoints(ExecutionContext context, Client client) {
        context.declareFunction("INFERENCE_LIST_ENDPOINTS",
            List.of(),  // No required parameters
            new BuiltInFunctionDefinition("INFERENCE_LIST_ENDPOINTS", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    TaskType taskType = TaskType.ANY;
                    if (!args.isEmpty() && args.get(0) != null) {
                        String taskTypeStr = args.get(0).toString().toUpperCase();
                        if (!"_ALL".equals(taskTypeStr)) {
                            taskType = parseTaskType(taskTypeStr);
                        }
                    }
                    
                    LOGGER.debug("Listing inference endpoints for task type [{}]", taskType);

                    GetInferenceModelAction.Request request = new GetInferenceModelAction.Request("_all", taskType);

                    client.execute(GetInferenceModelAction.INSTANCE, request, new ActionListener<GetInferenceModelAction.Response>() {
                        @Override
                        public void onResponse(GetInferenceModelAction.Response response) {
                            try {
                                List<Map<String, Object>> endpoints = new ArrayList<>();
                                for (var model : response.getEndpoints()) {
                                    Map<String, Object> endpoint = new java.util.HashMap<>();
                                    endpoint.put("endpoint_id", model.getInferenceEntityId());
                                    endpoint.put("task_type", model.getTaskType().name());
                                    endpoint.put("service", model.getService());
                                    endpoints.add(endpoint);
                                }
                                LOGGER.debug("Found [{}] inference endpoints", endpoints.size());
                                listener.onResponse(endpoints);
                            } catch (Exception e) {
                                listener.onFailure(new RuntimeException(
                                    "Failed to parse endpoint list: " + e.getMessage(), e));
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            LOGGER.error("Failed to list inference endpoints: {}", e.getMessage());
                            listener.onFailure(new RuntimeException(
                                "Failed to list inference endpoints: " + e.getMessage(), e));
                        }
                    });

                } catch (Exception e) {
                    listener.onFailure(e);
                }
            })
        );
    }

    /**
     * INFERENCE_GET_ENDPOINT - Gets details of a specific inference endpoint.
     */
    @FunctionSpec(
        name = "INFERENCE_GET_ENDPOINT",
        description = "Gets the configuration details of a specific inference endpoint.",
        parameters = {
            @FunctionParam(name = "endpoint_id", type = "STRING", description = "The endpoint ID to retrieve")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "The endpoint configuration as a document"),
        examples = {
            "INFERENCE_GET_ENDPOINT('my-openai-endpoint')"
        },
        category = FunctionCategory.DATASOURCE
    )
    public static void registerInferenceGetEndpoint(ExecutionContext context, Client client) {
        context.declareFunction("INFERENCE_GET_ENDPOINT",
            List.of(
                new Parameter("endpoint_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("INFERENCE_GET_ENDPOINT", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    if (args.isEmpty()) {
                        listener.onFailure(new RuntimeException(
                            "INFERENCE_GET_ENDPOINT requires 1 argument: endpoint_id"));
                        return;
                    }

                    String endpointId = args.get(0).toString();
                    
                    LOGGER.debug("Getting inference endpoint [{}]", endpointId);

                    GetInferenceModelAction.Request request = new GetInferenceModelAction.Request(endpointId, TaskType.ANY);

                    client.execute(GetInferenceModelAction.INSTANCE, request, new ActionListener<GetInferenceModelAction.Response>() {
                        @Override
                        public void onResponse(GetInferenceModelAction.Response response) {
                            try {
                                if (response.getEndpoints().isEmpty()) {
                                    listener.onFailure(new RuntimeException(
                                        "Inference endpoint '" + endpointId + "' not found"));
                                    return;
                                }
                                
                                var model = response.getEndpoints().getFirst();
                                Map<String, Object> endpoint = new java.util.HashMap<>();
                                endpoint.put("endpoint_id", model.getInferenceEntityId());
                                endpoint.put("task_type", model.getTaskType().name());
                                endpoint.put("service", model.getService());
                                
                                LOGGER.debug("Retrieved inference endpoint [{}]", endpointId);
                                listener.onResponse(endpoint);
                            } catch (Exception e) {
                                listener.onFailure(new RuntimeException(
                                    "Failed to parse endpoint: " + e.getMessage(), e));
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            LOGGER.error("Failed to get inference endpoint [{}]: {}", endpointId, e.getMessage());
                            listener.onFailure(new RuntimeException(
                                "Failed to get inference endpoint '" + endpointId + "': " + e.getMessage(), e));
                        }
                    });

                } catch (Exception e) {
                    listener.onFailure(e);
                }
            })
        );
    }

    /**
     * Parses a task type string into a TaskType enum.
     */
    private static TaskType parseTaskType(String taskTypeStr) {
        return switch (taskTypeStr.toUpperCase()) {
            case "COMPLETION" -> TaskType.COMPLETION;
            case "CHAT_COMPLETION" -> TaskType.CHAT_COMPLETION;
            case "TEXT_EMBEDDING", "EMBEDDING" -> TaskType.TEXT_EMBEDDING;
            case "RERANK" -> TaskType.RERANK;
            case "SPARSE_EMBEDDING" -> TaskType.SPARSE_EMBEDDING;
            case "ANY", "_ALL" -> TaskType.ANY;
            default -> throw new IllegalArgumentException(
                "Unknown task type: " + taskTypeStr + 
                ". Valid types: completion, chat_completion, text_embedding, rerank, sparse_embedding");
        };
    }

    // ========================================================================
    // INFERENCE OPERATION FUNCTIONS
    // ========================================================================

    /**
     * INFERENCE - General inference function for text generation/completion.
     * Uses the configured inference endpoint to generate text.
     */
    @FunctionSpec(
        name = "INFERENCE",
        description = "Performs inference using a configured Elasticsearch inference endpoint. " +
            "The endpoint must be pre-configured via the _inference API.",
        parameters = {
            @FunctionParam(name = "endpoint_id", type = "STRING", description = "The inference endpoint ID"),
            @FunctionParam(name = "input", type = "STRING", description = "The input text for inference"),
            @FunctionParam(name = "timeout", type = "STRING", description = "Timeout (e.g., '30s'). Optional, defaults to 30s")
        },
        returnType = @FunctionReturn(type = "STRING", description = "The inference result as text"),
        examples = {
            "INFERENCE('my-openai-endpoint', 'Explain Elasticsearch')",
            "INFERENCE('elser-endpoint', 'search query', '60s')"
        },
        category = FunctionCategory.NLP
    )
    public static void registerInference(ExecutionContext context, Client client) {
        context.declareFunction("INFERENCE",
            List.of(
                new Parameter("endpoint_id", "STRING", ParameterMode.IN),
                new Parameter("input", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("INFERENCE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    if (args.size() < 2) {
                        listener.onFailure(new RuntimeException(
                            "INFERENCE requires at least 2 arguments: endpoint_id, input"));
                        return;
                    }

                    String endpointId = args.get(0).toString();
                    String input = args.get(1).toString();
                    TimeValue timeout = DEFAULT_TIMEOUT;
                    
                    if (args.size() > 2 && args.get(2) != null) {
                        timeout = TimeValue.parseTimeValue(args.get(2).toString(), "timeout");
                    }

                    LOGGER.debug("Calling inference endpoint [{}] with input length [{}]", 
                        endpointId, input.length());

                    InferenceAction.Request request = InferenceAction.Request.builder(endpointId, TaskType.ANY)
                        .setInput(List.of(input))
                        .setInferenceTimeout(timeout)
                        .build();

                    client.execute(InferenceAction.INSTANCE, request, new ActionListener<InferenceAction.Response>() {
                        @Override
                        public void onResponse(InferenceAction.Response response) {
                            try {
                                String result = extractResultAsString(response.getResults());
                                LOGGER.debug("Inference completed successfully for endpoint [{}]", endpointId);
                                listener.onResponse(result);
                            } catch (Exception e) {
                                listener.onFailure(new RuntimeException(
                                    "Failed to parse inference response: " + e.getMessage(), e));
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            LOGGER.error("Inference failed for endpoint [{}]: {}", endpointId, e.getMessage());
                            listener.onFailure(new RuntimeException(
                                "Inference failed for endpoint '" + endpointId + "': " + e.getMessage(), e));
                        }
                    });

                } catch (Exception e) {
                    listener.onFailure(e);
                }
            })
        );
    }

    /**
     * INFERENCE_CHAT - Chat completion using the Unified Completion API.
     * Supports multi-turn conversations with proper message formatting.
     */
    @FunctionSpec(
        name = "INFERENCE_CHAT",
        description = "Performs chat completion using the Unified Completion API (OpenAI-compatible). " +
            "Accepts a JSON array of messages with role and content fields.",
        parameters = {
            @FunctionParam(name = "endpoint_id", type = "STRING", description = "The chat completion inference endpoint ID"),
            @FunctionParam(name = "messages", type = "STRING", description = "JSON array of messages, e.g., [{\"role\":\"user\",\"content\":\"Hello\"}]"),
            @FunctionParam(name = "timeout", type = "STRING", description = "Timeout (e.g., '60s'). Optional, defaults to 30s")
        },
        returnType = @FunctionReturn(type = "STRING", description = "The assistant's response text"),
        examples = {
            "INFERENCE_CHAT('my-openai-endpoint', '[{\"role\":\"user\",\"content\":\"Explain Elasticsearch\"}]')",
            "INFERENCE_CHAT('azure-gpt4', messages_json, '60s')"
        },
        category = FunctionCategory.NLP
    )
    public static void registerInferenceChat(ExecutionContext context, Client client) {
        context.declareFunction("INFERENCE_CHAT",
            List.of(
                new Parameter("endpoint_id", "STRING", ParameterMode.IN),
                new Parameter("messages", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("INFERENCE_CHAT", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    if (args.size() < 2) {
                        listener.onFailure(new RuntimeException(
                            "INFERENCE_CHAT requires at least 2 arguments: endpoint_id, messages"));
                        return;
                    }

                    String endpointId = args.get(0).toString();
                    String messagesJson = args.get(1).toString();
                    TimeValue timeout = DEFAULT_TIMEOUT;
                    
                    if (args.size() > 2 && args.get(2) != null) {
                        timeout = TimeValue.parseTimeValue(args.get(2).toString(), "timeout");
                    }

                    LOGGER.debug("Calling chat completion endpoint [{}]", endpointId);

                    // Parse the messages JSON into UnifiedCompletionRequest.Message objects
                    List<UnifiedCompletionRequest.Message> messages = parseMessages(messagesJson);
                    
                    if (messages.isEmpty()) {
                        listener.onFailure(new RuntimeException("Messages array is empty or invalid"));
                        return;
                    }

                    UnifiedCompletionRequest unifiedRequest = new UnifiedCompletionRequest(
                        messages,
                        null,  // model - use endpoint default
                        null,  // maxCompletionTokens
                        null,  // stop
                        null,  // temperature
                        null,  // toolChoice
                        null,  // tools
                        null   // topP
                    );

                    UnifiedCompletionAction.Request request = new UnifiedCompletionAction.Request(
                        endpointId,
                        TaskType.CHAT_COMPLETION,
                        unifiedRequest,
                        timeout
                    );

                    client.execute(UnifiedCompletionAction.INSTANCE, request, new ActionListener<InferenceAction.Response>() {
                        @Override
                        public void onResponse(InferenceAction.Response response) {
                            try {
                                String result = extractChatResult(response.getResults());
                                LOGGER.debug("Chat completion successful for endpoint [{}]", endpointId);
                                listener.onResponse(result);
                            } catch (Exception e) {
                                listener.onFailure(new RuntimeException(
                                    "Failed to parse chat response: " + e.getMessage(), e));
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            LOGGER.error("Chat completion failed for endpoint [{}]: {}", endpointId, e.getMessage());
                            listener.onFailure(new RuntimeException(
                                "Chat completion failed for endpoint '" + endpointId + "': " + e.getMessage(), e));
                        }
                    });

                } catch (Exception e) {
                    listener.onFailure(e);
                }
            })
        );
    }

    /**
     * INFERENCE_EMBED - Generate embeddings using an inference endpoint.
     */
    @FunctionSpec(
        name = "INFERENCE_EMBED",
        description = "Generates embeddings for the input text using a configured embedding endpoint. " +
            "Returns an array of floating-point numbers representing the embedding vector.",
        parameters = {
            @FunctionParam(name = "endpoint_id", type = "STRING", description = "The embedding inference endpoint ID"),
            @FunctionParam(name = "input", type = "STRING", description = "The text to embed"),
            @FunctionParam(name = "timeout", type = "STRING", description = "Timeout (e.g., '30s'). Optional")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "The embedding vector as an array of numbers"),
        examples = {
            "INFERENCE_EMBED('my-embedding-endpoint', 'text to embed')",
            "INFERENCE_EMBED('elser-endpoint', 'search query')"
        },
        category = FunctionCategory.NLP
    )
    public static void registerInferenceEmbed(ExecutionContext context, Client client) {
        context.declareFunction("INFERENCE_EMBED",
            List.of(
                new Parameter("endpoint_id", "STRING", ParameterMode.IN),
                new Parameter("input", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("INFERENCE_EMBED", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    if (args.size() < 2) {
                        listener.onFailure(new RuntimeException(
                            "INFERENCE_EMBED requires at least 2 arguments: endpoint_id, input"));
                        return;
                    }

                    String endpointId = args.get(0).toString();
                    String input = args.get(1).toString();
                    TimeValue timeout = DEFAULT_TIMEOUT;
                    
                    if (args.size() > 2 && args.get(2) != null) {
                        timeout = TimeValue.parseTimeValue(args.get(2).toString(), "timeout");
                    }

                    LOGGER.debug("Generating embedding via endpoint [{}]", endpointId);

                    InferenceAction.Request request = InferenceAction.Request.builder(endpointId, TaskType.TEXT_EMBEDDING)
                        .setInput(List.of(input))
                        .setInferenceTimeout(timeout)
                        .build();

                    client.execute(InferenceAction.INSTANCE, request, new ActionListener<InferenceAction.Response>() {
                        @Override
                        public void onResponse(InferenceAction.Response response) {
                            try {
                                List<Object> embedding = extractEmbedding(response.getResults());
                                LOGGER.debug("Embedding generated successfully, dimension [{}]", embedding.size());
                                listener.onResponse(embedding);
                            } catch (Exception e) {
                                listener.onFailure(new RuntimeException(
                                    "Failed to parse embedding response: " + e.getMessage(), e));
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            LOGGER.error("Embedding generation failed for endpoint [{}]: {}", endpointId, e.getMessage());
                            listener.onFailure(new RuntimeException(
                                "Embedding generation failed for endpoint '" + endpointId + "': " + e.getMessage(), e));
                        }
                    });

                } catch (Exception e) {
                    listener.onFailure(e);
                }
            })
        );
    }

    /**
     * INFERENCE_RERANK - Rerank documents using an inference endpoint.
     */
    @FunctionSpec(
        name = "INFERENCE_RERANK",
        description = "Reranks a list of documents based on relevance to a query using a reranking endpoint.",
        parameters = {
            @FunctionParam(name = "endpoint_id", type = "STRING", description = "The reranking inference endpoint ID"),
            @FunctionParam(name = "query", type = "STRING", description = "The query to rank against"),
            @FunctionParam(name = "documents", type = "ARRAY", description = "Array of document strings to rerank"),
            @FunctionParam(name = "top_n", type = "NUMBER", description = "Number of top results to return. Optional"),
            @FunctionParam(name = "timeout", type = "STRING", description = "Timeout (e.g., '30s'). Optional")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "Reranked documents with scores"),
        examples = {
            "INFERENCE_RERANK('my-reranker', 'elasticsearch query', doc_array)",
            "INFERENCE_RERANK('cohere-rerank', 'best database', docs, 5)"
        },
        category = FunctionCategory.NLP
    )
    public static void registerInferenceRerank(ExecutionContext context, Client client) {
        context.declareFunction("INFERENCE_RERANK",
            List.of(
                new Parameter("endpoint_id", "STRING", ParameterMode.IN),
                new Parameter("query", "STRING", ParameterMode.IN),
                new Parameter("documents", "ARRAY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("INFERENCE_RERANK", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    if (args.size() < 3) {
                        listener.onFailure(new RuntimeException(
                            "INFERENCE_RERANK requires at least 3 arguments: endpoint_id, query, documents"));
                        return;
                    }

                    String endpointId = args.get(0).toString();
                    String query = args.get(1).toString();
                    
                    @SuppressWarnings("unchecked")
                    List<Object> docsObj = (List<Object>) args.get(2);
                    List<String> documents = new ArrayList<>();
                    for (Object doc : docsObj) {
                        documents.add(doc.toString());
                    }
                    
                    Integer topN = null;
                    TimeValue timeout = DEFAULT_TIMEOUT;
                    
                    if (args.size() > 3 && args.get(3) != null) {
                        topN = ((Number) args.get(3)).intValue();
                    }
                    if (args.size() > 4 && args.get(4) != null) {
                        timeout = TimeValue.parseTimeValue(args.get(4).toString(), "timeout");
                    }

                    LOGGER.debug("Reranking [{}] documents via endpoint [{}]", documents.size(), endpointId);

                    InferenceAction.Request.Builder builder = InferenceAction.Request.builder(endpointId, TaskType.RERANK)
                        .setQuery(query)
                        .setInput(documents)
                        .setInferenceTimeout(timeout)
                        .setReturnDocuments(true);
                    
                    if (topN != null) {
                        builder.setTopN(topN);
                    }

                    client.execute(InferenceAction.INSTANCE, builder.build(), new ActionListener<InferenceAction.Response>() {
                        @Override
                        public void onResponse(InferenceAction.Response response) {
                            try {
                                List<Map<String, Object>> rerankedDocs = extractRerankResults(response.getResults());
                                LOGGER.debug("Reranking completed, returned [{}] documents", rerankedDocs.size());
                                listener.onResponse(rerankedDocs);
                            } catch (Exception e) {
                                listener.onFailure(new RuntimeException(
                                    "Failed to parse rerank response: " + e.getMessage(), e));
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            LOGGER.error("Reranking failed for endpoint [{}]: {}", endpointId, e.getMessage());
                            listener.onFailure(new RuntimeException(
                                "Reranking failed for endpoint '" + endpointId + "': " + e.getMessage(), e));
                        }
                    });

                } catch (Exception e) {
                    listener.onFailure(e);
                }
            })
        );
    }

    /**
     * Extracts the result as a string from inference results.
     * Handles various result types (completion, chat, etc.)
     */
    private static String extractResultAsString(InferenceServiceResults results) {
        if (results == null) {
            return "";
        }
        
        // Try to get the text representation
        // InferenceServiceResults has various implementations, we'll use toString as fallback
        // For completion results, we should extract the actual text
        try {
            var resultsList = results.transformToCoordinationFormat();
            if (resultsList != null && !resultsList.isEmpty()) {
                // Get first result
                var firstResult = resultsList.iterator().next();
                if (firstResult != null) {
                    return firstResult.toString();
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not extract result via coordinationFormat, falling back to toString");
        }
        
        return results.toString();
    }

    /**
     * Extracts embedding vector from inference results.
     */
    private static List<Object> extractEmbedding(InferenceServiceResults results) {
        List<Object> embedding = new ArrayList<>();
        
        if (results == null) {
            LOGGER.debug("extractEmbedding: results is null");
            return embedding;
        }

        LOGGER.debug("extractEmbedding: results class = {}", results.getClass().getName());
        LOGGER.debug("extractEmbedding: results.toString() = {}", results.toString());

        // Try to extract from asMap first - this often has the structured embedding
        try {
            var asMap = results.asMap();
            LOGGER.debug("extractEmbedding: asMap = {}", asMap);
            
            // Look for common embedding keys
            for (String key : new String[]{"embedding", "embeddings", "text_embedding", "values"}) {
                if (asMap.containsKey(key)) {
                    Object embeddingObj = asMap.get(key);
                    LOGGER.debug("extractEmbedding: found key '{}' with type {}", key, 
                        embeddingObj != null ? embeddingObj.getClass().getName() : "null");
                    if (embeddingObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Object> embList = (List<Object>) embeddingObj;
                        LOGGER.debug("extractEmbedding: returning {} dimensions from '{}'", embList.size(), key);
                        return embList;
                    }
                }
            }
            
            // Check if the map itself contains numeric values (the embedding)
            if (!asMap.isEmpty()) {
                // Some implementations put embeddings directly in map with numeric keys or as array elements
                Object firstValue = asMap.values().iterator().next();
                if (firstValue instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> embList = (List<Object>) firstValue;
                    if (!embList.isEmpty() && embList.get(0) instanceof Number) {
                        LOGGER.debug("extractEmbedding: returning {} dimensions from first map value", embList.size());
                        return embList;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not extract embedding via asMap: {}", e.getMessage());
        }

        try {
            // Try transformToCoordinationFormat
            var resultsList = results.transformToCoordinationFormat();
            LOGGER.debug("extractEmbedding: coordinationFormat has results: {}", resultsList != null);
            
            if (resultsList != null) {
                for (var result : resultsList) {
                    LOGGER.debug("extractEmbedding: result class = {}", 
                        result != null ? result.getClass().getName() : "null");
                    
                    // InferenceResults might have asMap method too
                    try {
                        var resultAsMap = result.asMap();
                        LOGGER.debug("extractEmbedding: result.asMap = {}", resultAsMap);
                        for (String key : new String[]{"embedding", "embeddings", "values", "text_embedding"}) {
                            if (resultAsMap.containsKey(key)) {
                                Object embObj = resultAsMap.get(key);
                                if (embObj instanceof List) {
                                    @SuppressWarnings("unchecked")
                                    List<Object> embList = (List<Object>) embObj;
                                    LOGGER.debug("extractEmbedding: returning {} dimensions from result.asMap.{}", embList.size(), key);
                                    return embList;
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.debug("Could not get asMap from result: {}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Could not extract embedding via coordinationFormat: {}", e.getMessage());
        }
        
        LOGGER.warn("extractEmbedding: returning empty list, could not extract from results");
        return embedding;
    }

    /**
     * Extracts reranked documents with scores from inference results.
     */
    private static List<Map<String, Object>> extractRerankResults(InferenceServiceResults results) {
        List<Map<String, Object>> reranked = new ArrayList<>();
        
        if (results == null) {
            LOGGER.debug("extractRerankResults: results is null");
            return reranked;
        }

        LOGGER.debug("extractRerankResults: results class = {}", results.getClass().getName());
        LOGGER.debug("extractRerankResults: results.toString() = {}", results.toString());

        try {
            // First, try asMap which often has the structured result
            var asMap = results.asMap();
            LOGGER.debug("extractRerankResults: asMap = {}", asMap);
            
            if (asMap != null && !asMap.isEmpty()) {
                // Check for common result structures - rerank results
                for (String key : new String[]{"rerank", "results", "documents", "ranked_results"}) {
                    if (asMap.containsKey(key)) {
                        Object rerankObj = asMap.get(key);
                        if (rerankObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<?> rerankList = (List<?>) rerankObj;
                            for (int i = 0; i < rerankList.size(); i++) {
                                Object item = rerankList.get(i);
                                Map<String, Object> doc = new java.util.HashMap<>();
                                doc.put("index", i);
                                if (item instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> itemMap = (Map<String, Object>) item;
                                    doc.putAll(itemMap);
                                } else {
                                    doc.put("value", item != null ? item.toString() : null);
                                }
                                reranked.add(doc);
                            }
                            if (!reranked.isEmpty()) {
                                LOGGER.debug("extractRerankResults: found {} results in '{}' key", reranked.size(), key);
                                return reranked;
                            }
                        }
                    }
                }
                
                // Return the whole map as a single result if it has score/relevance info
                if (asMap.containsKey("score") || asMap.containsKey("relevance_score")) {
                    reranked.add(new java.util.HashMap<>(asMap));
                    LOGGER.debug("extractRerankResults: returning single result with score");
                    return reranked;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not extract rerank results via asMap: {}", e.getMessage());
        }

        try {
            var resultsList = results.transformToCoordinationFormat();
            LOGGER.debug("extractRerankResults: coordinationFormat has results: {}", resultsList != null);
            
            if (resultsList != null) {
                int index = 0;
                for (var result : resultsList) {
                    Map<String, Object> doc = new java.util.HashMap<>();
                    doc.put("index", index);
                    
                    // Try to get structured data from the result
                    try {
                        var resultAsMap = result.asMap();
                        doc.putAll(resultAsMap);
                        LOGGER.debug("extractRerankResults: added result {} from asMap", index);
                    } catch (Exception e) {
                        doc.put("result", result.toString());
                        LOGGER.debug("extractRerankResults: added result {} as string", index);
                    }
                    
                    reranked.add(doc);
                    index++;
                }
                if (!reranked.isEmpty()) {
                    LOGGER.debug("extractRerankResults: returning {} results from coordinationFormat", reranked.size());
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not extract rerank results via coordinationFormat: {}", e.getMessage());
        }
        
        if (reranked.isEmpty()) {
            LOGGER.warn("extractRerankResults: returning empty list, could not extract from results");
        }
        return reranked;
    }

    /**
     * Parses a JSON string of messages into UnifiedCompletionRequest.Message objects.
     * Expected format: [{"role": "user", "content": "Hello"}, {"role": "assistant", "content": "Hi!"}]
     */
    private static List<UnifiedCompletionRequest.Message> parseMessages(String messagesJson) {
        List<UnifiedCompletionRequest.Message> messages = new ArrayList<>();
        
        try {
            // Parse JSON array of messages
            org.elasticsearch.xcontent.XContentParser parser = org.elasticsearch.xcontent.XContentType.JSON
                .xContent()
                .createParser(org.elasticsearch.xcontent.XContentParserConfiguration.EMPTY, messagesJson);
            
            if (parser.nextToken() == org.elasticsearch.xcontent.XContentParser.Token.START_ARRAY) {
                while (parser.nextToken() != org.elasticsearch.xcontent.XContentParser.Token.END_ARRAY) {
                    if (parser.currentToken() == org.elasticsearch.xcontent.XContentParser.Token.START_OBJECT) {
                        String role = null;
                        String content = null;
                        
                        while (parser.nextToken() != org.elasticsearch.xcontent.XContentParser.Token.END_OBJECT) {
                            String fieldName = parser.currentName();
                            parser.nextToken();
                            
                            if ("role".equals(fieldName)) {
                                role = parser.text();
                            } else if ("content".equals(fieldName)) {
                                content = parser.text();
                            }
                        }
                        
                        if (role != null && content != null) {
                            UnifiedCompletionRequest.ContentString contentObj = 
                                new UnifiedCompletionRequest.ContentString(content);
                            UnifiedCompletionRequest.Message message = 
                                new UnifiedCompletionRequest.Message(contentObj, role, null, null);
                            messages.add(message);
                        }
                    }
                }
            }
            parser.close();
        } catch (Exception e) {
            LOGGER.error("Failed to parse messages JSON: {}", e.getMessage());
            throw new RuntimeException("Invalid messages JSON format: " + e.getMessage(), e);
        }
        
        return messages;
    }

    /**
     * Extracts the assistant's response from chat completion results.
     */
    private static String extractChatResult(InferenceServiceResults results) {
        if (results == null) {
            return "";
        }
        
        // Chat completion results should contain the assistant's message
        try {
            var resultsList = results.transformToCoordinationFormat();
            if (resultsList != null && !resultsList.isEmpty()) {
                var firstResult = resultsList.iterator().next();
                if (firstResult != null) {
                    return firstResult.toString();
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not extract chat result via coordinationFormat, falling back to toString");
        }
        
        return results.toString();
    }
}

