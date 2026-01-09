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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * AWS S3 integration functions for elastic-script.
 * Provides object storage capabilities for reading and writing files to S3.
 * 
 * These functions require AWS credentials to be set via environment variables:
 * - AWS_ACCESS_KEY_ID
 * - AWS_SECRET_ACCESS_KEY
 * - AWS_REGION (optional, defaults to us-east-1)
 * 
 * Or passed as parameters.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.DATASOURCE,
    description = "AWS S3 integration functions for cloud storage operations."
)
public class S3Functions {

    private static final Logger LOGGER = LogManager.getLogger(S3Functions.class);
    private static final String DEFAULT_REGION = "us-east-1";

    public static void registerAll(ExecutionContext context) {
        registerS3Get(context);
        registerS3Put(context);
        registerS3Delete(context);
        registerS3List(context);
        registerS3Exists(context);
        registerS3CreateBucket(context);
    }

    /**
     * Gets AWS credentials from environment or parameters.
     */
    private static AwsCredentials getCredentials(List<Object> args, int accessKeyIndex, int secretKeyIndex, int regionIndex) {
        String accessKey = null;
        String secretKey = null;
        String region = DEFAULT_REGION;

        // Check if credentials were passed as arguments
        if (args.size() > accessKeyIndex && args.get(accessKeyIndex) != null) {
            accessKey = args.get(accessKeyIndex).toString();
        }
        if (args.size() > secretKeyIndex && args.get(secretKeyIndex) != null) {
            secretKey = args.get(secretKeyIndex).toString();
        }
        if (args.size() > regionIndex && args.get(regionIndex) != null) {
            String r = args.get(regionIndex).toString();
            if (!r.isEmpty()) {
                region = r;
            }
        }

        // Fall back to environment variables
        if (accessKey == null || accessKey.isEmpty()) {
            accessKey = System.getenv("AWS_ACCESS_KEY_ID");
        }
        if (secretKey == null || secretKey.isEmpty()) {
            secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        }
        String envRegion = System.getenv("AWS_REGION");
        if (envRegion != null && !envRegion.isEmpty() && region.equals(DEFAULT_REGION)) {
            region = envRegion;
        }

        if (accessKey == null || accessKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
            throw new RuntimeException(
                "AWS credentials not found. Set AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY environment variables " +
                "or pass access_key and secret_key parameters."
            );
        }

        return new AwsCredentials(accessKey, secretKey, region);
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

    @FunctionSpec(
        name = "S3_GET",
        description = "Reads an object from S3 and returns its contents as a string. " +
                      "For binary files, consider using S3_GET_JSON for structured data.",
        parameters = {
            @FunctionParam(name = "bucket", type = "STRING", description = "The S3 bucket name"),
            @FunctionParam(name = "key", type = "STRING", description = "The object key (path within the bucket)"),
            @FunctionParam(name = "access_key", type = "STRING", description = "AWS access key ID (optional)"),
            @FunctionParam(name = "secret_key", type = "STRING", description = "AWS secret access key (optional)"),
            @FunctionParam(name = "region", type = "STRING", description = "AWS region (optional, default: us-east-1)")
        },
        returnType = @FunctionReturn(type = "STRING", description = "The object contents as a string"),
        examples = {
            "S3_GET('my-bucket', 'data/file.txt')",
            "S3_GET('my-bucket', 'config.json', access_key, secret_key, 'eu-west-1')"
        },
        category = FunctionCategory.DATASOURCE
    )
    public static void registerS3Get(ExecutionContext context) {
        context.declareFunction("S3_GET",
            List.of(
                new Parameter("bucket", "STRING", ParameterMode.IN),
                new Parameter("key", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("S3_GET", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    if (args.size() < 2) {
                        listener.onFailure(new RuntimeException("S3_GET requires bucket and key arguments"));
                        return;
                    }

                    String bucket = args.get(0).toString();
                    String key = args.get(1).toString();
                    AwsCredentials creds = getCredentials(args, 2, 3, 4);

                    String content = s3GetObject(bucket, key, creds);
                    listener.onResponse(content);
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            })
        );
    }

    @FunctionSpec(
        name = "S3_PUT",
        description = "Writes content to an S3 object. Creates the object if it doesn't exist, " +
                      "or overwrites if it does.",
        parameters = {
            @FunctionParam(name = "bucket", type = "STRING", description = "The S3 bucket name"),
            @FunctionParam(name = "key", type = "STRING", description = "The object key (path within the bucket)"),
            @FunctionParam(name = "content", type = "STRING", description = "The content to write"),
            @FunctionParam(name = "content_type", type = "STRING", description = "Content type (optional, default: text/plain)"),
            @FunctionParam(name = "access_key", type = "STRING", description = "AWS access key ID (optional)"),
            @FunctionParam(name = "secret_key", type = "STRING", description = "AWS secret access key (optional)"),
            @FunctionParam(name = "region", type = "STRING", description = "AWS region (optional)")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "true if the object was written successfully"),
        examples = {
            "S3_PUT('my-bucket', 'output/result.json', json_content, 'application/json')",
            "S3_PUT('my-bucket', 'logs/export.txt', log_data)"
        },
        category = FunctionCategory.DATASOURCE
    )
    public static void registerS3Put(ExecutionContext context) {
        context.declareFunction("S3_PUT",
            List.of(
                new Parameter("bucket", "STRING", ParameterMode.IN),
                new Parameter("key", "STRING", ParameterMode.IN),
                new Parameter("content", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("S3_PUT", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    if (args.size() < 3) {
                        listener.onFailure(new RuntimeException("S3_PUT requires bucket, key, and content arguments"));
                        return;
                    }

                    String bucket = args.get(0).toString();
                    String key = args.get(1).toString();
                    String content = args.get(2).toString();
                    String contentType = args.size() > 3 && args.get(3) != null ? args.get(3).toString() : "text/plain";
                    AwsCredentials creds = getCredentials(args, 4, 5, 6);

                    boolean success = s3PutObject(bucket, key, content, contentType, creds);
                    listener.onResponse(success);
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            })
        );
    }

    @FunctionSpec(
        name = "S3_DELETE",
        description = "Deletes an object from S3.",
        parameters = {
            @FunctionParam(name = "bucket", type = "STRING", description = "The S3 bucket name"),
            @FunctionParam(name = "key", type = "STRING", description = "The object key to delete"),
            @FunctionParam(name = "access_key", type = "STRING", description = "AWS access key ID (optional)"),
            @FunctionParam(name = "secret_key", type = "STRING", description = "AWS secret access key (optional)"),
            @FunctionParam(name = "region", type = "STRING", description = "AWS region (optional)")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "true if the object was deleted successfully"),
        examples = {
            "S3_DELETE('my-bucket', 'temp/file.txt')"
        },
        category = FunctionCategory.DATASOURCE
    )
    public static void registerS3Delete(ExecutionContext context) {
        context.declareFunction("S3_DELETE",
            List.of(
                new Parameter("bucket", "STRING", ParameterMode.IN),
                new Parameter("key", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("S3_DELETE", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    if (args.size() < 2) {
                        listener.onFailure(new RuntimeException("S3_DELETE requires bucket and key arguments"));
                        return;
                    }

                    String bucket = args.get(0).toString();
                    String key = args.get(1).toString();
                    AwsCredentials creds = getCredentials(args, 2, 3, 4);

                    boolean success = s3DeleteObject(bucket, key, creds);
                    listener.onResponse(success);
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            })
        );
    }

    @FunctionSpec(
        name = "S3_LIST",
        description = "Lists objects in an S3 bucket with an optional prefix filter.",
        parameters = {
            @FunctionParam(name = "bucket", type = "STRING", description = "The S3 bucket name"),
            @FunctionParam(name = "prefix", type = "STRING", description = "Prefix to filter objects (optional)"),
            @FunctionParam(name = "max_keys", type = "NUMBER", description = "Maximum number of objects to return (default: 1000)"),
            @FunctionParam(name = "access_key", type = "STRING", description = "AWS access key ID (optional)"),
            @FunctionParam(name = "secret_key", type = "STRING", description = "AWS secret access key (optional)"),
            @FunctionParam(name = "region", type = "STRING", description = "AWS region (optional)")
        },
        returnType = @FunctionReturn(type = "ARRAY OF STRING", description = "List of object keys matching the prefix"),
        examples = {
            "S3_LIST('my-bucket')",
            "S3_LIST('my-bucket', 'logs/2024/')",
            "S3_LIST('my-bucket', 'data/', 100)"
        },
        category = FunctionCategory.DATASOURCE
    )
    public static void registerS3List(ExecutionContext context) {
        context.declareFunction("S3_LIST",
            List.of(
                new Parameter("bucket", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("S3_LIST", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    if (args.isEmpty()) {
                        listener.onFailure(new RuntimeException("S3_LIST requires a bucket argument"));
                        return;
                    }

                    String bucket = args.get(0).toString();
                    String prefix = args.size() > 1 && args.get(1) != null ? args.get(1).toString() : "";
                    int maxKeys = args.size() > 2 && args.get(2) != null ? ((Number) args.get(2)).intValue() : 1000;
                    AwsCredentials creds = getCredentials(args, 3, 4, 5);

                    List<String> keys = s3ListObjects(bucket, prefix, maxKeys, creds);
                    listener.onResponse(keys);
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            })
        );
    }

    @FunctionSpec(
        name = "S3_EXISTS",
        description = "Checks if an object exists in S3.",
        parameters = {
            @FunctionParam(name = "bucket", type = "STRING", description = "The S3 bucket name"),
            @FunctionParam(name = "key", type = "STRING", description = "The object key to check"),
            @FunctionParam(name = "access_key", type = "STRING", description = "AWS access key ID (optional)"),
            @FunctionParam(name = "secret_key", type = "STRING", description = "AWS secret access key (optional)"),
            @FunctionParam(name = "region", type = "STRING", description = "AWS region (optional)")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "true if the object exists, false otherwise"),
        examples = {
            "S3_EXISTS('my-bucket', 'data/config.json')"
        },
        category = FunctionCategory.DATASOURCE
    )
    public static void registerS3Exists(ExecutionContext context) {
        context.declareFunction("S3_EXISTS",
            List.of(
                new Parameter("bucket", "STRING", ParameterMode.IN),
                new Parameter("key", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("S3_EXISTS", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    if (args.size() < 2) {
                        listener.onFailure(new RuntimeException("S3_EXISTS requires bucket and key arguments"));
                        return;
                    }

                    String bucket = args.get(0).toString();
                    String key = args.get(1).toString();
                    AwsCredentials creds = getCredentials(args, 2, 3, 4);

                    boolean exists = s3ObjectExists(bucket, key, creds);
                    listener.onResponse(exists);
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            })
        );
    }

    @FunctionSpec(
        name = "S3_CREATE_BUCKET",
        description = "Creates a new S3 bucket. Returns true if created successfully or if bucket already exists.",
        parameters = {
            @FunctionParam(name = "bucket", type = "STRING", description = "The S3 bucket name to create"),
            @FunctionParam(name = "access_key", type = "STRING", description = "AWS access key ID (optional)"),
            @FunctionParam(name = "secret_key", type = "STRING", description = "AWS secret access key (optional)"),
            @FunctionParam(name = "region", type = "STRING", description = "AWS region (optional)")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "true if the bucket was created or already exists"),
        examples = {
            "S3_CREATE_BUCKET('my-new-bucket')",
            "S3_CREATE_BUCKET('my-bucket', 'AKIAIOSFODNN7EXAMPLE', 'wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY', 'us-west-2')"
        },
        category = FunctionCategory.DATASOURCE
    )
    public static void registerS3CreateBucket(ExecutionContext context) {
        context.declareFunction("S3_CREATE_BUCKET",
            List.of(
                new Parameter("bucket", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("S3_CREATE_BUCKET", (List<Object> args, ActionListener<Object> listener) -> {
                try {
                    if (args.isEmpty()) {
                        listener.onFailure(new RuntimeException("S3_CREATE_BUCKET requires a bucket name argument"));
                        return;
                    }

                    String bucket = args.get(0).toString();
                    AwsCredentials creds = getCredentials(args, 1, 2, 3);

                    boolean success = s3CreateBucket(bucket, creds);
                    listener.onResponse(success);
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            })
        );
    }

    // ========================
    // S3 API Implementation
    // ========================

    private static String s3GetObject(String bucket, String key, AwsCredentials creds) throws Exception {
        String host = bucket + ".s3." + creds.region + ".amazonaws.com";
        String url = "https://" + host + "/" + urlEncode(key);
        
        HttpURLConnection conn = createSignedRequest("GET", host, "/" + key, "", creds, null);
        conn.setRequestMethod("GET");
        
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                return content.toString().trim();
            }
        } else if (responseCode == 404) {
            throw new RuntimeException("S3 object not found: s3://" + bucket + "/" + key);
        } else {
            String error = readErrorResponse(conn);
            throw new RuntimeException("S3 GET failed (" + responseCode + "): " + error);
        }
    }

    private static boolean s3PutObject(String bucket, String key, String content, String contentType, AwsCredentials creds) throws Exception {
        String host = bucket + ".s3." + creds.region + ".amazonaws.com";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        
        Map<String, String> additionalHeaders = new HashMap<>();
        additionalHeaders.put("Content-Type", contentType);
        
        HttpURLConnection conn = createSignedRequest("PUT", host, "/" + key, content, creds, additionalHeaders);
        conn.setDoOutput(true);
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(contentBytes);
        }
        
        int responseCode = conn.getResponseCode();
        if (responseCode == 200 || responseCode == 204) {
            return true;
        } else {
            String error = readErrorResponse(conn);
            throw new RuntimeException("S3 PUT failed (" + responseCode + "): " + error);
        }
    }

    private static boolean s3DeleteObject(String bucket, String key, AwsCredentials creds) throws Exception {
        String host = bucket + ".s3." + creds.region + ".amazonaws.com";
        
        HttpURLConnection conn = createSignedRequest("DELETE", host, "/" + key, "", creds, null);
        
        int responseCode = conn.getResponseCode();
        // S3 returns 204 on successful delete, 404 if object doesn't exist (which is also success)
        return responseCode == 204 || responseCode == 404;
    }

    private static List<String> s3ListObjects(String bucket, String prefix, int maxKeys, AwsCredentials creds) throws Exception {
        String host = bucket + ".s3." + creds.region + ".amazonaws.com";
        String queryString = "list-type=2&max-keys=" + maxKeys;
        if (prefix != null && !prefix.isEmpty()) {
            queryString += "&prefix=" + urlEncode(prefix);
        }
        
        HttpURLConnection conn = createSignedRequest("GET", host, "/", queryString, creds, null);
        
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder xml = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    xml.append(line);
                }
                return parseS3ListResponse(xml.toString());
            }
        } else {
            String error = readErrorResponse(conn);
            throw new RuntimeException("S3 LIST failed (" + responseCode + "): " + error);
        }
    }

    private static boolean s3ObjectExists(String bucket, String key, AwsCredentials creds) throws Exception {
        String host = bucket + ".s3." + creds.region + ".amazonaws.com";
        
        HttpURLConnection conn = createSignedRequest("HEAD", host, "/" + key, "", creds, null);
        
        int responseCode = conn.getResponseCode();
        return responseCode == 200;
    }

    private static boolean s3CreateBucket(String bucket, AwsCredentials creds) throws Exception {
        // Use path-style URL for bucket creation
        String host = "s3." + creds.region + ".amazonaws.com";
        
        // For regions other than us-east-1, we need to specify the location constraint
        String payload = "";
        if (!"us-east-1".equals(creds.region)) {
            payload = "<CreateBucketConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">" +
                      "<LocationConstraint>" + creds.region + "</LocationConstraint>" +
                      "</CreateBucketConfiguration>";
        }
        
        Map<String, String> additionalHeaders = new HashMap<>();
        if (!payload.isEmpty()) {
            additionalHeaders.put("content-type", "application/xml");
        }
        
        HttpURLConnection conn = createSignedRequest("PUT", host, "/" + bucket, payload, creds, additionalHeaders);
        
        if (!payload.isEmpty()) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }
        }
        
        int responseCode = conn.getResponseCode();
        
        // 200 = created, 409 = bucket already exists (which is fine)
        if (responseCode == 200 || responseCode == 409) {
            LOGGER.debug("S3 bucket '{}' created or already exists in region '{}'", bucket, creds.region);
            return true;
        } else {
            String error = readErrorResponse(conn);
            throw new RuntimeException("S3 CREATE_BUCKET failed (" + responseCode + "): " + error);
        }
    }

    // ========================
    // AWS Signature V4
    // ========================

    private static HttpURLConnection createSignedRequest(String method, String host, String path, 
            String payload, AwsCredentials creds, Map<String, String> additionalHeaders) throws Exception {
        
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String amzDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
        String dateStamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        String payloadHash = sha256Hex(payload);
        
        // Prepare headers
        TreeMap<String, String> headers = new TreeMap<>();
        headers.put("host", host);
        headers.put("x-amz-date", amzDate);
        headers.put("x-amz-content-sha256", payloadHash);
        if (additionalHeaders != null) {
            for (Map.Entry<String, String> entry : additionalHeaders.entrySet()) {
                headers.put(entry.getKey().toLowerCase(), entry.getValue());
            }
        }
        
        // Create canonical request
        StringBuilder signedHeaderNames = new StringBuilder();
        StringBuilder canonicalHeaders = new StringBuilder();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (signedHeaderNames.length() > 0) signedHeaderNames.append(";");
            signedHeaderNames.append(entry.getKey());
            canonicalHeaders.append(entry.getKey()).append(":").append(entry.getValue().trim()).append("\n");
        }
        
        String canonicalRequest = method + "\n" +
            path + "\n" +
            "" + "\n" +  // query string (empty for simple requests)
            canonicalHeaders + "\n" +
            signedHeaderNames + "\n" +
            payloadHash;
        
        // Create string to sign
        String algorithm = "AWS4-HMAC-SHA256";
        String credentialScope = dateStamp + "/" + creds.region + "/s3/aws4_request";
        String stringToSign = algorithm + "\n" +
            amzDate + "\n" +
            credentialScope + "\n" +
            sha256Hex(canonicalRequest);
        
        // Calculate signature
        byte[] kDate = hmacSha256(("AWS4" + creds.secretKey).getBytes(StandardCharsets.UTF_8), dateStamp);
        byte[] kRegion = hmacSha256(kDate, creds.region);
        byte[] kService = hmacSha256(kRegion, "s3");
        byte[] kSigning = hmacSha256(kService, "aws4_request");
        String signature = bytesToHex(hmacSha256(kSigning, stringToSign));
        
        // Create authorization header
        String authorization = algorithm + " " +
            "Credential=" + creds.accessKey + "/" + credentialScope + ", " +
            "SignedHeaders=" + signedHeaderNames + ", " +
            "Signature=" + signature;
        
        // Create connection
        String urlStr = "https://" + host + path;
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);
        
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            conn.setRequestProperty(entry.getKey(), entry.getValue());
        }
        conn.setRequestProperty("Authorization", authorization);
        
        return conn;
    }

    private static String sha256Hex(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private static byte[] hmacSha256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private static String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8.name())
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
        } catch (Exception e) {
            return value;
        }
    }

    private static String readErrorResponse(HttpURLConnection conn) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            return content.toString();
        } catch (Exception e) {
            return "Unknown error";
        }
    }

    private static List<String> parseS3ListResponse(String xml) {
        List<String> keys = new ArrayList<>();
        // Simple XML parsing for <Key> elements
        int pos = 0;
        while (true) {
            int start = xml.indexOf("<Key>", pos);
            if (start == -1) break;
            int end = xml.indexOf("</Key>", start);
            if (end == -1) break;
            keys.add(xml.substring(start + 5, end));
            pos = end + 6;
        }
        return keys;
    }
}

