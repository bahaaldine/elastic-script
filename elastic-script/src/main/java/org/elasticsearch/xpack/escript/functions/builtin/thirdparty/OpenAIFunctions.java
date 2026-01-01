/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.builtin.thirdparty;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.common.Strings;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.XContentParserConfiguration;
import org.elasticsearch.xcontent.XContentType;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI integration functions for elastic-script.
 * Provides LLM capabilities including text completion, chat, and embeddings.
 * 
 * These functions require an OpenAI API key to be set via the OPENAI_API_KEY
 * environment variable or passed as a parameter.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.NLP,
    description = "OpenAI integration functions for LLM completions, chat, and embeddings."
)
public class OpenAIFunctions {

    private static final Logger LOGGER = LogManager.getLogger(OpenAIFunctions.class);
    private static final String OPENAI_API_BASE = "https://api.openai.com/v1";
    private static final String DEFAULT_COMPLETION_MODEL = "gpt-4o-mini";
    private static final String DEFAULT_EMBEDDING_MODEL = "text-embedding-3-small";

    public static void registerAll(ExecutionContext context) {
        LOGGER.info("Registering OpenAI built-in functions");
        registerLlmComplete(context);
        registerLlmChat(context);
        registerLlmEmbed(context);
        registerLlmSummarize(context);
        registerLlmClassify(context);
        registerLlmExtract(context);
    }

    /**
     * Gets the OpenAI API key from environment or throws an error.
     */
    private static String getApiKey(List<Object> args, int apiKeyArgIndex) {
        // Check if API key was passed as argument
        if (args.size() > apiKeyArgIndex && args.get(apiKeyArgIndex) != null) {
            String key = args.get(apiKeyArgIndex).toString();
            if (!key.isEmpty()) {
                return key;
            }
        }
        
        // Fall back to environment variable
        String envKey = System.getenv("OPENAI_API_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            return envKey;
        }
        
        throw new RuntimeException(
            "OpenAI API key not found. Set OPENAI_API_KEY environment variable or pass api_key parameter."
        );
    }

    private static Map<String, String> createHeaders(String apiKey) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + apiKey);
        return headers;
    }

    @FunctionSpec(
        name = "LLM_COMPLETE",
        description = "Generates a text completion using OpenAI's API. Returns the generated text.",
        parameters = {
            @FunctionParam(name = "prompt", type = "STRING", description = "The prompt to complete"),
            @FunctionParam(name = "model", type = "STRING", description = "The model to use (default: gpt-4o-mini)"),
            @FunctionParam(name = "api_key", type = "STRING", description = "OpenAI API key (optional, uses env var if not provided)")
        },
        returnType = @FunctionReturn(type = "STRING", description = "The generated completion text"),
        examples = {
            "LLM_COMPLETE('Explain Elasticsearch in one sentence')",
            "LLM_COMPLETE('Translate to French: Hello', 'gpt-4o')"
        },
        category = FunctionCategory.NLP
    )
    public static void registerLlmComplete(ExecutionContext context) {
        context.declareFunction("LLM_COMPLETE",
            List.of(
                new Parameter("prompt", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("LLM_COMPLETE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    if (args.isEmpty()) {
                        listener.onFailure(new RuntimeException("LLM_COMPLETE requires at least a prompt argument"));
                        return;
                    }

                    String prompt = args.get(0).toString();
                    String model = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : DEFAULT_COMPLETION_MODEL;
                    String apiKey = getApiKey(args, 2);

                    // Build request body
                    XContentBuilder builder = XContentFactory.jsonBuilder();
                    builder.startObject();
                    builder.field("model", model);
                    builder.startArray("messages");
                    builder.startObject();
                    builder.field("role", "user");
                    builder.field("content", prompt);
                    builder.endObject();
                    builder.endArray();
                    builder.endObject();

                    String requestBody = Strings.toString(builder);
                    String url = OPENAI_API_BASE + "/chat/completions";

                    HttpClientUtil.postJson(url, createHeaders(apiKey), requestBody, ActionListener.wrap(
                        response -> {
                            try {
                                String content = extractChatContent(response);
                                listener.onResponse(content);
                            } catch (Exception e) {
                                listener.onFailure(e);
                            }
                        },
                        listener::onFailure
                    ));
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            })
        );
    }

    @FunctionSpec(
        name = "LLM_CHAT",
        description = "Sends a chat conversation to OpenAI and returns the assistant's response. " +
                      "Messages should be an array of documents with 'role' and 'content' fields.",
        parameters = {
            @FunctionParam(name = "messages", type = "ARRAY OF DOCUMENT", description = "Array of message objects with 'role' and 'content'"),
            @FunctionParam(name = "model", type = "STRING", description = "The model to use (default: gpt-4o-mini)"),
            @FunctionParam(name = "api_key", type = "STRING", description = "OpenAI API key (optional)")
        },
        returnType = @FunctionReturn(type = "STRING", description = "The assistant's response"),
        examples = {
            "LLM_CHAT([{'role': 'system', 'content': 'You are helpful.'}, {'role': 'user', 'content': 'Hi!'}])"
        },
        category = FunctionCategory.NLP
    )
    public static void registerLlmChat(ExecutionContext context) {
        context.declareFunction("LLM_CHAT",
            List.of(
                new Parameter("messages", "ARRAY OF DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("LLM_CHAT", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    if (args.isEmpty()) {
                        listener.onFailure(new RuntimeException("LLM_CHAT requires a messages argument"));
                        return;
                    }

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> messages = (List<Map<String, Object>>) args.get(0);
                    String model = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : DEFAULT_COMPLETION_MODEL;
                    String apiKey = getApiKey(args, 2);

                    // Build request body
                    XContentBuilder builder = XContentFactory.jsonBuilder();
                    builder.startObject();
                    builder.field("model", model);
                    builder.startArray("messages");
                    for (Map<String, Object> message : messages) {
                        builder.startObject();
                        builder.field("role", message.get("role").toString());
                        builder.field("content", message.get("content").toString());
                        builder.endObject();
                    }
                    builder.endArray();
                    builder.endObject();

                    String requestBody = Strings.toString(builder);
                    String url = OPENAI_API_BASE + "/chat/completions";

                    HttpClientUtil.postJson(url, createHeaders(apiKey), requestBody, ActionListener.wrap(
                        response -> {
                            try {
                                String content = extractChatContent(response);
                                listener.onResponse(content);
                            } catch (Exception e) {
                                listener.onFailure(e);
                            }
                        },
                        listener::onFailure
                    ));
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            })
        );
    }

    @FunctionSpec(
        name = "LLM_EMBED",
        description = "Generates an embedding vector for the given text using OpenAI's embedding API. " +
                      "Returns an array of floats representing the text embedding.",
        parameters = {
            @FunctionParam(name = "text", type = "STRING", description = "The text to embed"),
            @FunctionParam(name = "model", type = "STRING", description = "The embedding model (default: text-embedding-3-small)"),
            @FunctionParam(name = "api_key", type = "STRING", description = "OpenAI API key (optional)")
        },
        returnType = @FunctionReturn(type = "ARRAY OF FLOAT", description = "The embedding vector"),
        examples = {
            "LLM_EMBED('Elasticsearch is a search engine')",
            "LLM_EMBED('Hello world', 'text-embedding-3-large')"
        },
        category = FunctionCategory.NLP
    )
    public static void registerLlmEmbed(ExecutionContext context) {
        context.declareFunction("LLM_EMBED",
            List.of(
                new Parameter("text", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("LLM_EMBED", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    if (args.isEmpty()) {
                        listener.onFailure(new RuntimeException("LLM_EMBED requires a text argument"));
                        return;
                    }

                    String text = args.get(0).toString();
                    String model = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : DEFAULT_EMBEDDING_MODEL;
                    String apiKey = getApiKey(args, 2);

                    // Build request body
                    XContentBuilder builder = XContentFactory.jsonBuilder();
                    builder.startObject();
                    builder.field("model", model);
                    builder.field("input", text);
                    builder.endObject();

                    String requestBody = Strings.toString(builder);
                    String url = OPENAI_API_BASE + "/embeddings";

                    HttpClientUtil.postJson(url, createHeaders(apiKey), requestBody, ActionListener.wrap(
                        response -> {
                            try {
                                List<Double> embedding = extractEmbedding(response);
                                listener.onResponse(embedding);
                            } catch (Exception e) {
                                listener.onFailure(e);
                            }
                        },
                        listener::onFailure
                    ));
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            })
        );
    }

    @FunctionSpec(
        name = "LLM_SUMMARIZE",
        description = "Summarizes the given text using an LLM. Returns a concise summary.",
        parameters = {
            @FunctionParam(name = "text", type = "STRING", description = "The text to summarize"),
            @FunctionParam(name = "max_words", type = "NUMBER", description = "Maximum words in summary (default: 100)"),
            @FunctionParam(name = "api_key", type = "STRING", description = "OpenAI API key (optional)")
        },
        returnType = @FunctionReturn(type = "STRING", description = "The summarized text"),
        examples = {
            "LLM_SUMMARIZE(long_document)",
            "LLM_SUMMARIZE(log_output, 50)"
        },
        category = FunctionCategory.NLP
    )
    public static void registerLlmSummarize(ExecutionContext context) {
        context.declareFunction("LLM_SUMMARIZE",
            List.of(
                new Parameter("text", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("LLM_SUMMARIZE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    if (args.isEmpty()) {
                        listener.onFailure(new RuntimeException("LLM_SUMMARIZE requires a text argument"));
                        return;
                    }

                    String text = args.get(0).toString();
                    int maxWords = args.size() > 1 && args.get(1) != null ? ((Number) args.get(1)).intValue() : 100;
                    String apiKey = getApiKey(args, 2);

                    String prompt = String.format(
                        "Summarize the following text in no more than %d words. Be concise and capture the key points:\n\n%s",
                        maxWords, text
                    );

                    // Build request body
                    XContentBuilder builder = XContentFactory.jsonBuilder();
                    builder.startObject();
                    builder.field("model", DEFAULT_COMPLETION_MODEL);
                    builder.startArray("messages");
                    builder.startObject();
                    builder.field("role", "user");
                    builder.field("content", prompt);
                    builder.endObject();
                    builder.endArray();
                    builder.endObject();

                    String requestBody = Strings.toString(builder);
                    String url = OPENAI_API_BASE + "/chat/completions";

                    HttpClientUtil.postJson(url, createHeaders(apiKey), requestBody, ActionListener.wrap(
                        response -> {
                            try {
                                String content = extractChatContent(response);
                                listener.onResponse(content);
                            } catch (Exception e) {
                                listener.onFailure(e);
                            }
                        },
                        listener::onFailure
                    ));
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            })
        );
    }

    @FunctionSpec(
        name = "LLM_CLASSIFY",
        description = "Classifies the given text into one of the provided categories using an LLM.",
        parameters = {
            @FunctionParam(name = "text", type = "STRING", description = "The text to classify"),
            @FunctionParam(name = "categories", type = "ARRAY OF STRING", description = "List of possible categories"),
            @FunctionParam(name = "api_key", type = "STRING", description = "OpenAI API key (optional)")
        },
        returnType = @FunctionReturn(type = "STRING", description = "The selected category"),
        examples = {
            "LLM_CLASSIFY('Server is down!', ['critical', 'warning', 'info'])",
            "LLM_CLASSIFY(email_body, ['spam', 'not_spam'])"
        },
        category = FunctionCategory.NLP
    )
    public static void registerLlmClassify(ExecutionContext context) {
        context.declareFunction("LLM_CLASSIFY",
            List.of(
                new Parameter("text", "STRING", ParameterMode.IN),
                new Parameter("categories", "ARRAY OF STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("LLM_CLASSIFY", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    if (args.size() < 2) {
                        listener.onFailure(new RuntimeException("LLM_CLASSIFY requires text and categories arguments"));
                        return;
                    }

                    String text = args.get(0).toString();
                    @SuppressWarnings("unchecked")
                    List<String> categories = (List<String>) args.get(1);
                    String apiKey = getApiKey(args, 2);

                    String categoriesStr = String.join(", ", categories);
                    String prompt = String.format(
                        "Classify the following text into exactly one of these categories: [%s]\n\n" +
                        "Text: %s\n\n" +
                        "Respond with only the category name, nothing else.",
                        categoriesStr, text
                    );

                    // Build request body
                    XContentBuilder builder = XContentFactory.jsonBuilder();
                    builder.startObject();
                    builder.field("model", DEFAULT_COMPLETION_MODEL);
                    builder.startArray("messages");
                    builder.startObject();
                    builder.field("role", "user");
                    builder.field("content", prompt);
                    builder.endObject();
                    builder.endArray();
                    builder.endObject();

                    String requestBody = Strings.toString(builder);
                    String url = OPENAI_API_BASE + "/chat/completions";

                    HttpClientUtil.postJson(url, createHeaders(apiKey), requestBody, ActionListener.wrap(
                        response -> {
                            try {
                                String content = extractChatContent(response).trim();
                                listener.onResponse(content);
                            } catch (Exception e) {
                                listener.onFailure(e);
                            }
                        },
                        listener::onFailure
                    ));
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            })
        );
    }

    @FunctionSpec(
        name = "LLM_EXTRACT",
        description = "Extracts structured information from text based on a schema. " +
                      "Returns a document with the extracted fields.",
        parameters = {
            @FunctionParam(name = "text", type = "STRING", description = "The text to extract from"),
            @FunctionParam(name = "fields", type = "ARRAY OF STRING", description = "List of field names to extract"),
            @FunctionParam(name = "api_key", type = "STRING", description = "OpenAI API key (optional)")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Document with extracted fields"),
        examples = {
            "LLM_EXTRACT('John Doe, age 30, from NYC', ['name', 'age', 'city'])",
            "LLM_EXTRACT(error_log, ['error_type', 'timestamp', 'severity'])"
        },
        category = FunctionCategory.NLP
    )
    public static void registerLlmExtract(ExecutionContext context) {
        context.declareFunction("LLM_EXTRACT",
            List.of(
                new Parameter("text", "STRING", ParameterMode.IN),
                new Parameter("fields", "ARRAY OF STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("LLM_EXTRACT", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    if (args.size() < 2) {
                        listener.onFailure(new RuntimeException("LLM_EXTRACT requires text and fields arguments"));
                        return;
                    }

                    String text = args.get(0).toString();
                    @SuppressWarnings("unchecked")
                    List<String> fields = (List<String>) args.get(1);
                    String apiKey = getApiKey(args, 2);

                    String fieldsStr = String.join(", ", fields);
                    String prompt = String.format(
                        "Extract the following fields from the text: [%s]\n\n" +
                        "Text: %s\n\n" +
                        "Respond with valid JSON only, using the field names as keys. " +
                        "Use null for fields that cannot be found.",
                        fieldsStr, text
                    );

                    // Build request body
                    XContentBuilder builder = XContentFactory.jsonBuilder();
                    builder.startObject();
                    builder.field("model", DEFAULT_COMPLETION_MODEL);
                    builder.startArray("messages");
                    builder.startObject();
                    builder.field("role", "user");
                    builder.field("content", prompt);
                    builder.endObject();
                    builder.endArray();
                    builder.field("response_format", Map.of("type", "json_object"));
                    builder.endObject();

                    String requestBody = Strings.toString(builder);
                    String url = OPENAI_API_BASE + "/chat/completions";

                    HttpClientUtil.postJson(url, createHeaders(apiKey), requestBody, ActionListener.wrap(
                        response -> {
                            try {
                                String content = extractChatContent(response);
                                Map<String, Object> result = parseJsonToMap(content);
                                listener.onResponse(result);
                            } catch (Exception e) {
                                listener.onFailure(e);
                            }
                        },
                        listener::onFailure
                    ));
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            })
        );
    }

    // -------------------------
    // Response Parsing Helpers
    // -------------------------

    private static String extractChatContent(String jsonResponse) throws Exception {
        try (XContentParser parser = XContentType.JSON.xContent()
                .createParser(XContentParserConfiguration.EMPTY, jsonResponse)) {
            
            Map<String, Object> responseMap = parser.map();
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("No choices in OpenAI response");
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            
            if (message == null) {
                throw new RuntimeException("No message in OpenAI response");
            }
            
            return message.get("content").toString();
        }
    }

    private static List<Double> extractEmbedding(String jsonResponse) throws Exception {
        try (XContentParser parser = XContentType.JSON.xContent()
                .createParser(XContentParserConfiguration.EMPTY, jsonResponse)) {
            
            Map<String, Object> responseMap = parser.map();
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) responseMap.get("data");
            
            if (data == null || data.isEmpty()) {
                throw new RuntimeException("No embedding data in OpenAI response");
            }
            
            @SuppressWarnings("unchecked")
            List<Double> embedding = (List<Double>) data.get(0).get("embedding");
            
            if (embedding == null) {
                throw new RuntimeException("No embedding vector in OpenAI response");
            }
            
            return embedding;
        }
    }

    private static Map<String, Object> parseJsonToMap(String json) throws Exception {
        try (XContentParser parser = XContentType.JSON.xContent()
                .createParser(XContentParserConfiguration.EMPTY, json)) {
            return parser.map();
        }
    }
}

