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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Slack integration functions for elastic-script.
 * Provides notification and messaging capabilities via Slack API and webhooks.
 * 
 * Two modes of operation:
 * 1. Webhook mode (SLACK_WEBHOOK): Simple notifications via incoming webhooks
 * 2. API mode (SLACK_SEND, etc.): Full API access for advanced features
 */
@FunctionCollectionSpec(
    category = FunctionCategory.NETWORK,
    description = "Slack integration functions for sending messages and notifications."
)
public class SlackFunctions {

    private static final Logger LOGGER = LogManager.getLogger(SlackFunctions.class);
    private static final String SLACK_API_BASE = "https://slack.com/api";

    public static void registerAll(ExecutionContext context) {
        LOGGER.info("Registering Slack built-in functions");
        registerSlackWebhook(context);
        registerSlackSend(context);
        registerSlackSendBlocks(context);
        registerSlackPostReaction(context);
        registerSlackListChannels(context);
    }

    /**
     * Gets the Slack Bot token from environment or parameter.
     */
    private static String getSlackToken(List<Object> args, int tokenArgIndex) {
        // Check if token was passed as argument
        if (args.size() > tokenArgIndex && args.get(tokenArgIndex) != null) {
            String token = args.get(tokenArgIndex).toString();
            if (!token.isEmpty()) {
                return token;
            }
        }
        
        // Fall back to environment variable
        String envToken = System.getenv("SLACK_BOT_TOKEN");
        if (envToken != null && !envToken.isEmpty()) {
            return envToken;
        }
        
        throw new RuntimeException(
            "Slack Bot token not found. Set SLACK_BOT_TOKEN environment variable or pass token parameter."
        );
    }

    private static Map<String, String> createSlackHeaders(String token) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        return headers;
    }

    @FunctionSpec(
        name = "SLACK_WEBHOOK",
        description = "Sends a message to Slack via an incoming webhook URL. " +
                      "This is the simplest way to send notifications without needing a bot token.",
        parameters = {
            @FunctionParam(name = "webhook_url", type = "STRING", description = "The Slack incoming webhook URL"),
            @FunctionParam(name = "message", type = "STRING", description = "The message text to send"),
            @FunctionParam(name = "username", type = "STRING", description = "Optional display name for the message (default: webhook default)"),
            @FunctionParam(name = "icon_emoji", type = "STRING", description = "Optional emoji icon like ':robot_face:' (default: webhook default)")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "true if message was sent successfully"),
        examples = {
            "SLACK_WEBHOOK('https://hooks.slack.com/services/XXX', 'Alert: Server is down!')",
            "SLACK_WEBHOOK(webhook_url, 'Deployment complete', 'DeployBot', ':rocket:')"
        },
        category = FunctionCategory.NETWORK
    )
    public static void registerSlackWebhook(ExecutionContext context) {
        context.declareFunction("SLACK_WEBHOOK",
            List.of(
                new Parameter("webhook_url", "STRING", ParameterMode.IN),
                new Parameter("message", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SLACK_WEBHOOK", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    if (args.size() < 2) {
                        listener.onFailure(new RuntimeException("SLACK_WEBHOOK requires webhook_url and message arguments"));
                        return;
                    }

                    String webhookUrl = args.get(0).toString();
                    String message = args.get(1).toString();
                    String username = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : null;
                    String iconEmoji = args.size() > 3 && args.get(3) != null ? args.get(3).toString() : null;

                    // Validate webhook URL
                    if (!webhookUrl.startsWith("https://hooks.slack.com/")) {
                        listener.onFailure(new RuntimeException("Invalid Slack webhook URL"));
                        return;
                    }

                    // Build request body
                    XContentBuilder builder = XContentFactory.jsonBuilder();
                    builder.startObject();
                    builder.field("text", message);
                    if (username != null && !username.isEmpty()) {
                        builder.field("username", username);
                    }
                    if (iconEmoji != null && !iconEmoji.isEmpty()) {
                        builder.field("icon_emoji", iconEmoji);
                    }
                    builder.endObject();

                    String requestBody = builder.toString();

                    HttpClientUtil.postJson(webhookUrl, null, requestBody, ActionListener.wrap(
                        response -> {
                            // Slack webhooks return "ok" on success
                            listener.onResponse(response.equals("ok") || response.contains("ok"));
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
        name = "SLACK_SEND",
        description = "Sends a message to a Slack channel using the Slack API. " +
                      "Requires a Slack Bot token with chat:write permission.",
        parameters = {
            @FunctionParam(name = "channel", type = "STRING", description = "Channel ID or name (e.g., '#general' or 'C1234567890')"),
            @FunctionParam(name = "message", type = "STRING", description = "The message text to send"),
            @FunctionParam(name = "thread_ts", type = "STRING", description = "Optional thread timestamp to reply to a thread"),
            @FunctionParam(name = "token", type = "STRING", description = "Slack Bot token (optional, uses env var if not provided)")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Response containing ts (timestamp) and channel"),
        examples = {
            "SLACK_SEND('#alerts', 'New error detected in production')",
            "SLACK_SEND('C1234567890', 'Reply to thread', '1234567890.123456')"
        },
        category = FunctionCategory.NETWORK
    )
    public static void registerSlackSend(ExecutionContext context) {
        context.declareFunction("SLACK_SEND",
            List.of(
                new Parameter("channel", "STRING", ParameterMode.IN),
                new Parameter("message", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SLACK_SEND", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    if (args.size() < 2) {
                        listener.onFailure(new RuntimeException("SLACK_SEND requires channel and message arguments"));
                        return;
                    }

                    String channel = args.get(0).toString();
                    String message = args.get(1).toString();
                    String threadTs = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : null;
                    String token = getSlackToken(args, 3);

                    // Build request body
                    XContentBuilder builder = XContentFactory.jsonBuilder();
                    builder.startObject();
                    builder.field("channel", channel);
                    builder.field("text", message);
                    if (threadTs != null && !threadTs.isEmpty()) {
                        builder.field("thread_ts", threadTs);
                    }
                    builder.endObject();

                    String requestBody = builder.toString();
                    String url = SLACK_API_BASE + "/chat.postMessage";

                    HttpClientUtil.postJson(url, createSlackHeaders(token), requestBody, ActionListener.wrap(
                        response -> {
                            try {
                                Map<String, Object> result = parseSlackResponse(response);
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

    @FunctionSpec(
        name = "SLACK_SEND_BLOCKS",
        description = "Sends a rich message with Block Kit blocks to a Slack channel. " +
                      "Allows for complex layouts with sections, buttons, and more.",
        parameters = {
            @FunctionParam(name = "channel", type = "STRING", description = "Channel ID or name"),
            @FunctionParam(name = "blocks", type = "ARRAY OF DOCUMENT", description = "Array of Block Kit block objects"),
            @FunctionParam(name = "fallback_text", type = "STRING", description = "Fallback text for notifications"),
            @FunctionParam(name = "token", type = "STRING", description = "Slack Bot token (optional)")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "Response containing ts and channel"),
        examples = {
            "SLACK_SEND_BLOCKS('#alerts', [{\"type\": \"section\", \"text\": {\"type\": \"mrkdwn\", \"text\": \"*Alert*\"}}], 'Alert notification')"
        },
        category = FunctionCategory.NETWORK
    )
    public static void registerSlackSendBlocks(ExecutionContext context) {
        context.declareFunction("SLACK_SEND_BLOCKS",
            List.of(
                new Parameter("channel", "STRING", ParameterMode.IN),
                new Parameter("blocks", "ARRAY OF DOCUMENT", ParameterMode.IN),
                new Parameter("fallback_text", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SLACK_SEND_BLOCKS", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    if (args.size() < 3) {
                        listener.onFailure(new RuntimeException("SLACK_SEND_BLOCKS requires channel, blocks, and fallback_text arguments"));
                        return;
                    }

                    String channel = args.get(0).toString();
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> blocks = (List<Map<String, Object>>) args.get(1);
                    String fallbackText = args.get(2).toString();
                    String token = getSlackToken(args, 3);

                    // Build request body
                    XContentBuilder builder = XContentFactory.jsonBuilder();
                    builder.startObject();
                    builder.field("channel", channel);
                    builder.field("text", fallbackText);
                    builder.startArray("blocks");
                    for (Map<String, Object> block : blocks) {
                        builder.map(block);
                    }
                    builder.endArray();
                    builder.endObject();

                    String requestBody = builder.toString();
                    String url = SLACK_API_BASE + "/chat.postMessage";

                    HttpClientUtil.postJson(url, createSlackHeaders(token), requestBody, ActionListener.wrap(
                        response -> {
                            try {
                                Map<String, Object> result = parseSlackResponse(response);
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

    @FunctionSpec(
        name = "SLACK_REACT",
        description = "Adds an emoji reaction to a message in Slack.",
        parameters = {
            @FunctionParam(name = "channel", type = "STRING", description = "Channel ID containing the message"),
            @FunctionParam(name = "timestamp", type = "STRING", description = "Message timestamp to react to"),
            @FunctionParam(name = "emoji", type = "STRING", description = "Emoji name without colons (e.g., 'thumbsup')"),
            @FunctionParam(name = "token", type = "STRING", description = "Slack Bot token (optional)")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "true if reaction was added successfully"),
        examples = {
            "SLACK_REACT('C1234567890', '1234567890.123456', 'white_check_mark')"
        },
        category = FunctionCategory.NETWORK
    )
    public static void registerSlackPostReaction(ExecutionContext context) {
        context.declareFunction("SLACK_REACT",
            List.of(
                new Parameter("channel", "STRING", ParameterMode.IN),
                new Parameter("timestamp", "STRING", ParameterMode.IN),
                new Parameter("emoji", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SLACK_REACT", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    if (args.size() < 3) {
                        listener.onFailure(new RuntimeException("SLACK_REACT requires channel, timestamp, and emoji arguments"));
                        return;
                    }

                    String channel = args.get(0).toString();
                    String timestamp = args.get(1).toString();
                    String emoji = args.get(2).toString();
                    String token = getSlackToken(args, 3);

                    // Build request body
                    XContentBuilder builder = XContentFactory.jsonBuilder();
                    builder.startObject();
                    builder.field("channel", channel);
                    builder.field("timestamp", timestamp);
                    builder.field("name", emoji);
                    builder.endObject();

                    String requestBody = builder.toString();
                    String url = SLACK_API_BASE + "/reactions.add";

                    HttpClientUtil.postJson(url, createSlackHeaders(token), requestBody, ActionListener.wrap(
                        response -> {
                            try {
                                Map<String, Object> result = parseSlackResponse(response);
                                listener.onResponse(Boolean.TRUE.equals(result.get("ok")));
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
        name = "SLACK_LIST_CHANNELS",
        description = "Lists public channels in the Slack workspace. Useful for discovering channel IDs.",
        parameters = {
            @FunctionParam(name = "limit", type = "NUMBER", description = "Maximum number of channels to return (default: 100)"),
            @FunctionParam(name = "token", type = "STRING", description = "Slack Bot token (optional)")
        },
        returnType = @FunctionReturn(type = "ARRAY OF DOCUMENT", description = "Array of channel objects with id, name, and other metadata"),
        examples = {
            "SLACK_LIST_CHANNELS()",
            "SLACK_LIST_CHANNELS(50)"
        },
        category = FunctionCategory.NETWORK
    )
    public static void registerSlackListChannels(ExecutionContext context) {
        context.declareFunction("SLACK_LIST_CHANNELS",
            List.of(),
            new BuiltInFunctionDefinition("SLACK_LIST_CHANNELS", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    int limit = args.size() > 0 && args.get(0) != null ? ((Number) args.get(0)).intValue() : 100;
                    String token = getSlackToken(args, 1);

                    String url = SLACK_API_BASE + "/conversations.list?limit=" + limit + "&types=public_channel";

                    HttpClientUtil.get(url, createSlackHeaders(token), ActionListener.wrap(
                        response -> {
                            try {
                                Map<String, Object> result = parseSlackResponse(response);
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> channels = (List<Map<String, Object>>) result.get("channels");
                                listener.onResponse(channels != null ? channels : List.of());
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

    private static Map<String, Object> parseSlackResponse(String jsonResponse) throws Exception {
        try (XContentParser parser = XContentType.JSON.xContent()
                .createParser(XContentParserConfiguration.EMPTY, jsonResponse)) {
            
            Map<String, Object> responseMap = parser.map();
            
            // Check for Slack API errors
            Boolean ok = (Boolean) responseMap.get("ok");
            if (ok == null || !ok) {
                String error = (String) responseMap.get("error");
                throw new RuntimeException("Slack API error: " + (error != null ? error : "unknown"));
            }
            
            return responseMap;
        }
    }
}

