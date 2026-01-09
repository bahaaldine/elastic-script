/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.builtin.runbooks;

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
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * AWS integration functions for elastic-script.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.RUNBOOK,
    description = "AWS integration functions for cloud automation."
)
public class AWSFunctions {

    private static final Logger LOGGER = LogManager.getLogger(AWSFunctions.class);
    private static final String DEFAULT_REGION = "us-east-1";

    public static void registerAll(ExecutionContext context) {
        registerAwsSsmRun(context);
        registerAwsSsmStatus(context);
        registerAwsLambdaInvoke(context);
        registerAwsEc2Reboot(context);
        registerAwsEc2Start(context);
        registerAwsEc2Stop(context);
        registerAwsAsgSetCapacity(context);
        registerAwsAsgDescribe(context);
    }

    private static AwsCredentials getCredentials(List<Object> args, int accessKeyIndex, int secretKeyIndex, int regionIndex) {
        String accessKey = args.size() > accessKeyIndex && args.get(accessKeyIndex) != null 
            ? args.get(accessKeyIndex).toString() : System.getenv("AWS_ACCESS_KEY_ID");
        String secretKey = args.size() > secretKeyIndex && args.get(secretKeyIndex) != null 
            ? args.get(secretKeyIndex).toString() : System.getenv("AWS_SECRET_ACCESS_KEY");
        String region = args.size() > regionIndex && args.get(regionIndex) != null 
            ? args.get(regionIndex).toString() : System.getenv("AWS_REGION");
        if (region == null || region.isEmpty()) region = DEFAULT_REGION;
        
        if (accessKey == null || accessKey.isEmpty()) throw new RuntimeException("AWS_ACCESS_KEY_ID not configured");
        if (secretKey == null || secretKey.isEmpty()) throw new RuntimeException("AWS_SECRET_ACCESS_KEY not configured");
        
        return new AwsCredentials(accessKey, secretKey, region);
    }

    public static void registerAwsSsmRun(ExecutionContext context) {
        context.declareFunction("AWS_SSM_RUN",
            List.of(
                new Parameter("instance_ids", "STRING", ParameterMode.IN),
                new Parameter("command", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("AWS_SSM_RUN", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String instanceIds = args.get(0).toString();
                    String command = args.get(1).toString();
                    AwsCredentials creds = getCredentials(args, 3, 4, 5);
                    
                    XContentBuilder builder = XContentFactory.jsonBuilder();
                    builder.startObject();
                    builder.field("DocumentName", "AWS-RunShellScript");
                    builder.startArray("InstanceIds");
                    for (String id : instanceIds.split(",")) builder.value(id.trim());
                    builder.endArray();
                    builder.startObject("Parameters").startArray("commands").value(command).endArray().endObject();
                    builder.endObject();
                    
                    String response = awsRequest(creds, "ssm", "POST", "/", "AmazonSSM.SendCommand", Strings.toString(builder));
                    Map<String, Object> result = parseJsonToMap(response);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> cmd = (Map<String, Object>) result.get("Command");
                    listener.onResponse(cmd.get("CommandId"));
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("AWS_SSM_RUN failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAwsSsmStatus(ExecutionContext context) {
        context.declareFunction("AWS_SSM_STATUS",
            List.of(
                new Parameter("command_id", "STRING", ParameterMode.IN),
                new Parameter("instance_id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("AWS_SSM_STATUS", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String commandId = args.get(0).toString();
                    String instanceId = args.get(1).toString();
                    AwsCredentials creds = getCredentials(args, 2, 3, 4);
                    
                    XContentBuilder builder = XContentFactory.jsonBuilder();
                    builder.startObject().field("CommandId", commandId).field("InstanceId", instanceId).endObject();
                    
                    String response = awsRequest(creds, "ssm", "POST", "/", "AmazonSSM.GetCommandInvocation", Strings.toString(builder));
                    listener.onResponse(parseJsonToMap(response));
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("AWS_SSM_STATUS failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAwsLambdaInvoke(ExecutionContext context) {
        context.declareFunction("AWS_LAMBDA_INVOKE",
            List.of(new Parameter("function_name", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("AWS_LAMBDA_INVOKE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String functionName = args.get(0).toString();
                    String payload = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : "{}";
                    AwsCredentials creds = getCredentials(args, 3, 4, 5);
                    
                    String encodedFunction = java.net.URLEncoder.encode(functionName, "UTF-8");
                    String path = "/2015-03-31/functions/" + encodedFunction + "/invocations";
                    
                    String response = awsRequest(creds, "lambda", "POST", path, null, payload);
                    
                    if (response != null && !response.isEmpty()) {
                        try {
                            listener.onResponse(parseJsonToMap(response));
                            return;
                        } catch (Exception e) {
                            Map<String, Object> result = new HashMap<>();
                            result.put("response", response);
                            listener.onResponse(result);
                            return;
                        }
                    }
                    Map<String, Object> result = new HashMap<>();
                    result.put("status", "invoked");
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("AWS_LAMBDA_INVOKE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAwsEc2Reboot(ExecutionContext context) {
        context.declareFunction("AWS_EC2_REBOOT",
            List.of(new Parameter("instance_ids", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("AWS_EC2_REBOOT", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String instanceIds = args.get(0).toString();
                    AwsCredentials creds = getCredentials(args, 1, 2, 3);
                    
                    StringBuilder queryParams = new StringBuilder("Action=RebootInstances&Version=2016-11-15");
                    String[] ids = instanceIds.split(",");
                    for (int i = 0; i < ids.length; i++) {
                        queryParams.append("&InstanceId.").append(i + 1).append("=").append(ids[i].trim());
                    }
                    awsRequestEc2(creds, queryParams.toString());
                    listener.onResponse(true);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("AWS_EC2_REBOOT failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAwsEc2Start(ExecutionContext context) {
        context.declareFunction("AWS_EC2_START",
            List.of(new Parameter("instance_ids", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("AWS_EC2_START", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String instanceIds = args.get(0).toString();
                    AwsCredentials creds = getCredentials(args, 1, 2, 3);
                    
                    StringBuilder queryParams = new StringBuilder("Action=StartInstances&Version=2016-11-15");
                    String[] ids = instanceIds.split(",");
                    for (int i = 0; i < ids.length; i++) {
                        queryParams.append("&InstanceId.").append(i + 1).append("=").append(ids[i].trim());
                    }
                    awsRequestEc2(creds, queryParams.toString());
                    listener.onResponse(true);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("AWS_EC2_START failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAwsEc2Stop(ExecutionContext context) {
        context.declareFunction("AWS_EC2_STOP",
            List.of(new Parameter("instance_ids", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("AWS_EC2_STOP", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String instanceIds = args.get(0).toString();
                    boolean force = args.size() > 1 && args.get(1) != null && Boolean.parseBoolean(args.get(1).toString());
                    AwsCredentials creds = getCredentials(args, 2, 3, 4);
                    
                    StringBuilder queryParams = new StringBuilder("Action=StopInstances&Version=2016-11-15");
                    String[] ids = instanceIds.split(",");
                    for (int i = 0; i < ids.length; i++) {
                        queryParams.append("&InstanceId.").append(i + 1).append("=").append(ids[i].trim());
                    }
                    if (force) queryParams.append("&Force=true");
                    awsRequestEc2(creds, queryParams.toString());
                    listener.onResponse(true);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("AWS_EC2_STOP failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAwsAsgSetCapacity(ExecutionContext context) {
        context.declareFunction("AWS_ASG_SET_CAPACITY",
            List.of(
                new Parameter("asg_name", "STRING", ParameterMode.IN),
                new Parameter("desired_capacity", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("AWS_ASG_SET_CAPACITY", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String asgName = args.get(0).toString();
                    int desiredCapacity = ((Number) args.get(1)).intValue();
                    AwsCredentials creds = getCredentials(args, 3, 4, 5);
                    
                    String queryParams = "Action=SetDesiredCapacity&Version=2011-01-01" +
                        "&AutoScalingGroupName=" + java.net.URLEncoder.encode(asgName, "UTF-8") +
                        "&DesiredCapacity=" + desiredCapacity +
                        "&HonorCooldown=true";
                    awsRequestAsg(creds, queryParams);
                    listener.onResponse(true);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("AWS_ASG_SET_CAPACITY failed: " + e.getMessage(), e));
                }
            })
        );
    }

    public static void registerAwsAsgDescribe(ExecutionContext context) {
        context.declareFunction("AWS_ASG_DESCRIBE",
            List.of(new Parameter("asg_name", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("AWS_ASG_DESCRIBE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    String asgName = args.get(0).toString();
                    AwsCredentials creds = getCredentials(args, 1, 2, 3);
                    
                    String queryParams = "Action=DescribeAutoScalingGroups&Version=2011-01-01" +
                        "&AutoScalingGroupNames.member.1=" + java.net.URLEncoder.encode(asgName, "UTF-8");
                    String response = awsRequestAsg(creds, queryParams);
                    Map<String, Object> result = new HashMap<>();
                    result.put("raw_response", response);
                    result.put("asg_name", asgName);
                    listener.onResponse(result);
                } catch (Exception e) {
                    listener.onFailure(new RuntimeException("AWS_ASG_DESCRIBE failed: " + e.getMessage(), e));
                }
            })
        );
    }

    private static String awsRequest(AwsCredentials creds, String service, String method, String path, 
            String amzTarget, String body) throws Exception {
        String host = service + "." + creds.region + ".amazonaws.com";
        String endpoint = "https://" + host + path;
        
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String amzDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
        String dateStamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        String payloadHash = sha256Hex(body != null ? body : "");
        
        TreeMap<String, String> headers = new TreeMap<>();
        headers.put("host", host);
        headers.put("x-amz-date", amzDate);
        headers.put("x-amz-content-sha256", payloadHash);
        if (amzTarget != null) headers.put("x-amz-target", amzTarget);
        
        StringBuilder signedHeadersBuilder = new StringBuilder();
        StringBuilder canonicalHeadersBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (signedHeadersBuilder.length() > 0) signedHeadersBuilder.append(";");
            signedHeadersBuilder.append(entry.getKey());
            canonicalHeadersBuilder.append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
        }
        
        String canonicalRequest = method + "\n" + path + "\n\n" + canonicalHeadersBuilder + "\n" + signedHeadersBuilder + "\n" + payloadHash;
        String credentialScope = dateStamp + "/" + creds.region + "/" + service + "/aws4_request";
        String stringToSign = "AWS4-HMAC-SHA256\n" + amzDate + "\n" + credentialScope + "\n" + sha256Hex(canonicalRequest);
        
        byte[] kDate = hmacSha256(("AWS4" + creds.secretKey).getBytes(StandardCharsets.UTF_8), dateStamp);
        byte[] kRegion = hmacSha256(kDate, creds.region);
        byte[] kService = hmacSha256(kRegion, service);
        byte[] kSigning = hmacSha256(kService, "aws4_request");
        String signature = bytesToHex(hmacSha256(kSigning, stringToSign));
        
        String authorization = "AWS4-HMAC-SHA256 Credential=" + creds.accessKey + "/" + credentialScope + 
            ", SignedHeaders=" + signedHeadersBuilder + ", Signature=" + signature;
        
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Authorization", authorization);
        conn.setRequestProperty("Content-Type", "application/x-amz-json-1.1");
        conn.setRequestProperty("x-amz-date", amzDate);
        conn.setRequestProperty("x-amz-content-sha256", payloadHash);
        if (amzTarget != null) conn.setRequestProperty("x-amz-target", amzTarget);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);
        
        if (body != null && !body.isEmpty()) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }
        
        int responseCode = conn.getResponseCode();
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
        }
        conn.disconnect();
        
        if (responseCode >= 400) throw new RuntimeException("AWS " + service + " error (" + responseCode + "): " + response);
        return response.toString();
    }

    private static String awsRequestEc2(AwsCredentials creds, String queryParams) throws Exception {
        String host = "ec2." + creds.region + ".amazonaws.com";
        String endpoint = "https://" + host + "/";
        
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String amzDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
        String dateStamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        String payloadHash = sha256Hex(queryParams);
        
        TreeMap<String, String> headers = new TreeMap<>();
        headers.put("host", host);
        headers.put("x-amz-date", amzDate);
        headers.put("x-amz-content-sha256", payloadHash);
        
        StringBuilder signedHeadersBuilder = new StringBuilder();
        StringBuilder canonicalHeadersBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (signedHeadersBuilder.length() > 0) signedHeadersBuilder.append(";");
            signedHeadersBuilder.append(entry.getKey());
            canonicalHeadersBuilder.append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
        }
        
        String canonicalRequest = "POST\n/\n\n" + canonicalHeadersBuilder + "\n" + signedHeadersBuilder + "\n" + payloadHash;
        String credentialScope = dateStamp + "/" + creds.region + "/ec2/aws4_request";
        String stringToSign = "AWS4-HMAC-SHA256\n" + amzDate + "\n" + credentialScope + "\n" + sha256Hex(canonicalRequest);
        
        byte[] kDate = hmacSha256(("AWS4" + creds.secretKey).getBytes(StandardCharsets.UTF_8), dateStamp);
        byte[] kRegion = hmacSha256(kDate, creds.region);
        byte[] kService = hmacSha256(kRegion, "ec2");
        byte[] kSigning = hmacSha256(kService, "aws4_request");
        String signature = bytesToHex(hmacSha256(kSigning, stringToSign));
        
        String authorization = "AWS4-HMAC-SHA256 Credential=" + creds.accessKey + "/" + credentialScope + 
            ", SignedHeaders=" + signedHeadersBuilder + ", Signature=" + signature;
        
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", authorization);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("x-amz-date", amzDate);
        conn.setRequestProperty("x-amz-content-sha256", payloadHash);
        conn.setDoOutput(true);
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(queryParams.getBytes(StandardCharsets.UTF_8));
        }
        
        int responseCode = conn.getResponseCode();
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
        }
        conn.disconnect();
        
        if (responseCode >= 400) throw new RuntimeException("AWS EC2 error (" + responseCode + "): " + response);
        return response.toString();
    }

    private static String awsRequestAsg(AwsCredentials creds, String queryParams) throws Exception {
        String host = "autoscaling." + creds.region + ".amazonaws.com";
        String endpoint = "https://" + host + "/";
        
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String amzDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
        String dateStamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        String payloadHash = sha256Hex(queryParams);
        
        TreeMap<String, String> headers = new TreeMap<>();
        headers.put("host", host);
        headers.put("x-amz-date", amzDate);
        headers.put("x-amz-content-sha256", payloadHash);
        
        StringBuilder signedHeadersBuilder = new StringBuilder();
        StringBuilder canonicalHeadersBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (signedHeadersBuilder.length() > 0) signedHeadersBuilder.append(";");
            signedHeadersBuilder.append(entry.getKey());
            canonicalHeadersBuilder.append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
        }
        
        String canonicalRequest = "POST\n/\n\n" + canonicalHeadersBuilder + "\n" + signedHeadersBuilder + "\n" + payloadHash;
        String credentialScope = dateStamp + "/" + creds.region + "/autoscaling/aws4_request";
        String stringToSign = "AWS4-HMAC-SHA256\n" + amzDate + "\n" + credentialScope + "\n" + sha256Hex(canonicalRequest);
        
        byte[] kDate = hmacSha256(("AWS4" + creds.secretKey).getBytes(StandardCharsets.UTF_8), dateStamp);
        byte[] kRegion = hmacSha256(kDate, creds.region);
        byte[] kService = hmacSha256(kRegion, "autoscaling");
        byte[] kSigning = hmacSha256(kService, "aws4_request");
        String signature = bytesToHex(hmacSha256(kSigning, stringToSign));
        
        String authorization = "AWS4-HMAC-SHA256 Credential=" + creds.accessKey + "/" + credentialScope + 
            ", SignedHeaders=" + signedHeadersBuilder + ", Signature=" + signature;
        
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", authorization);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("x-amz-date", amzDate);
        conn.setRequestProperty("x-amz-content-sha256", payloadHash);
        conn.setDoOutput(true);
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(queryParams.getBytes(StandardCharsets.UTF_8));
        }
        
        int responseCode = conn.getResponseCode();
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
        }
        conn.disconnect();
        
        if (responseCode >= 400) throw new RuntimeException("AWS ASG error (" + responseCode + "): " + response);
        return response.toString();
    }

    private static String sha256Hex(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return bytesToHex(digest.digest(data.getBytes(StandardCharsets.UTF_8)));
    }

    private static byte[] hmacSha256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static Map<String, Object> parseJsonToMap(String json) throws Exception {
        try (XContentParser parser = XContentType.JSON.xContent().createParser(
                XContentParserConfiguration.EMPTY, 
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))) {
            return parser.map();
        }
    }

    private static class AwsCredentials {
        final String accessKey;
        final String secretKey;
        final String region;
        AwsCredentials(String accessKey, String secretKey, String region) {
            this.accessKey = accessKey;
            this.secretKey = secretKey;
            this.region = region;
        }
    }
}
