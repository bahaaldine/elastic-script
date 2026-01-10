# Integration Functions

Connect elastic-script to external services for automation and orchestration.

## Slack Integration

### SLACK_SEND

Sends a message to a Slack channel.

```sql
SLACK_SEND('#alerts', 'High CPU usage detected on prod-server-01');
```

**Syntax:** `SLACK_SEND(channel, message)`

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| channel | STRING | Channel name (with #) or channel ID |
| message | STRING | Message text |

!!! note "Configuration"
    Set the `SLACK_BOT_TOKEN` environment variable with your Slack bot token.

---

### SLACK_SEND_RICH

Sends a formatted message with blocks.

```sql
DECLARE blocks ARRAY = [
    {"type": "header", "text": {"type": "plain_text", "text": "🚨 Alert"}},
    {"type": "section", "text": {"type": "mrkdwn", "text": "*Service:* api-gateway\n*Status:* DOWN"}}
];

SLACK_SEND_RICH('#incidents', blocks);
```

**Syntax:** `SLACK_SEND_RICH(channel, blocks)`

---

## PagerDuty Integration

### PAGERDUTY_CREATE_INCIDENT

Creates a PagerDuty incident.

```sql
DECLARE incident_key STRING = PAGERDUTY_CREATE_INCIDENT(
    'Database connection pool exhausted',
    'critical',
    'db-service'
);
PRINT 'Created incident: ' || incident_key;
```

**Syntax:** `PAGERDUTY_CREATE_INCIDENT(title, severity, service)`

**Severity Levels:**

| Level | Description |
|-------|-------------|
| `critical` | System down, immediate action required |
| `error` | Major functionality impacted |
| `warning` | Degraded performance |
| `info` | Informational alert |

---

### PAGERDUTY_RESOLVE

Resolves an existing incident.

```sql
PAGERDUTY_RESOLVE(incident_key, 'Issue resolved - connection pool increased');
```

**Syntax:** `PAGERDUTY_RESOLVE(incident_key, resolution_message)`

---

## AWS Integration

### AWS_LAMBDA_INVOKE

Invokes an AWS Lambda function.

```sql
DECLARE payload DOCUMENT = {"action": "scale_up", "count": 2};
DECLARE result DOCUMENT = AWS_LAMBDA_INVOKE('my-lambda-function', payload);
PRINT DOCUMENT_GET(result, 'status');
```

**Syntax:** `AWS_LAMBDA_INVOKE(function_name, payload)`

---

### S3_GET_OBJECT

Retrieves an object from S3.

```sql
DECLARE content STRING = S3_GET_OBJECT('my-bucket', 'config/settings.json');
```

**Syntax:** `S3_GET_OBJECT(bucket, key)`

---

### S3_PUT_OBJECT

Uploads content to S3.

```sql
S3_PUT_OBJECT('my-bucket', 'reports/daily.json', report_content);
```

**Syntax:** `S3_PUT_OBJECT(bucket, key, content)`

---

## Kubernetes Integration

### K8S_GET_PODS

Lists pods in a namespace.

```sql
DECLARE pods ARRAY = K8S_GET_PODS('production');

FOR i IN 0..(ARRAY_LENGTH(pods)-1) LOOP
    DECLARE pod DOCUMENT = pods[i];
    PRINT DOCUMENT_GET(pod, 'name') || ': ' || DOCUMENT_GET(pod, 'status');
END LOOP;
```

**Syntax:** `K8S_GET_PODS(namespace)`

---

### K8S_SCALE_DEPLOYMENT

Scales a Kubernetes deployment.

```sql
K8S_SCALE_DEPLOYMENT('production', 'api-gateway', 5);
PRINT 'Scaled api-gateway to 5 replicas';
```

**Syntax:** `K8S_SCALE_DEPLOYMENT(namespace, deployment, replicas)`

---

### K8S_RESTART_DEPLOYMENT

Triggers a rolling restart.

```sql
K8S_RESTART_DEPLOYMENT('production', 'api-gateway');
```

**Syntax:** `K8S_RESTART_DEPLOYMENT(namespace, deployment)`

---

## HTTP Functions

### HTTP_GET

Makes an HTTP GET request.

```sql
DECLARE response STRING = HTTP_GET('https://api.example.com/status');
```

**Syntax:** `HTTP_GET(url)`

---

### HTTP_POST

Makes an HTTP POST request.

```sql
DECLARE body DOCUMENT = {"key": "value"};
DECLARE response STRING = HTTP_POST('https://api.example.com/data', body);
```

**Syntax:** `HTTP_POST(url, body)`

---

### HTTP_REQUEST

Makes a custom HTTP request.

```sql
DECLARE headers DOCUMENT = {
    "Authorization": "Bearer " || api_token,
    "Content-Type": "application/json"
};

DECLARE response STRING = HTTP_REQUEST(
    'PUT',
    'https://api.example.com/resource/123',
    body,
    headers
);
```

**Syntax:** `HTTP_REQUEST(method, url, body, headers)`

---

## CI/CD Integration

### GITHUB_CREATE_ISSUE

Creates a GitHub issue.

```sql
GITHUB_CREATE_ISSUE(
    'myorg/myrepo',
    'Automated bug report: Memory leak detected',
    'The service is experiencing memory growth over time.\n\nDetails: ...'
);
```

**Syntax:** `GITHUB_CREATE_ISSUE(repo, title, body)`

---

## Example: Complete Incident Response

```sql
CREATE PROCEDURE handle_outage(service STRING, severity STRING)
BEGIN
    PRINT 'Handling outage for ' || service;
    
    -- 1. Create PagerDuty incident
    DECLARE pd_key STRING = PAGERDUTY_CREATE_INCIDENT(
        service || ' is DOWN',
        severity,
        service
    );
    
    -- 2. Notify Slack
    SLACK_SEND('#incidents', '🚨 *' || service || '* is experiencing an outage. PD incident created.');
    
    -- 3. Attempt auto-remediation
    TRY
        K8S_RESTART_DEPLOYMENT('production', service);
        PRINT 'Initiated restart of ' || service;
        
        -- Wait and check status
        -- SLEEP(30);
        
        DECLARE pods ARRAY = K8S_GET_PODS('production');
        DECLARE healthy BOOLEAN = TRUE;
        
        -- Check pod status logic here...
        
        IF healthy THEN
            PAGERDUTY_RESOLVE(pd_key, 'Auto-remediation successful');
            SLACK_SEND('#incidents', '✅ *' || service || '* recovered after restart');
        END IF;
        
    CATCH error
        SLACK_SEND('#incidents', '❌ Auto-remediation failed: ' || error);
    END TRY;
    
    RETURN pd_key;
END PROCEDURE;
```

---

## Environment Variables

| Variable | Service | Description |
|----------|---------|-------------|
| `SLACK_BOT_TOKEN` | Slack | Bot OAuth token |
| `PAGERDUTY_API_KEY` | PagerDuty | API key |
| `AWS_ACCESS_KEY_ID` | AWS | Access key |
| `AWS_SECRET_ACCESS_KEY` | AWS | Secret key |
| `GITHUB_TOKEN` | GitHub | Personal access token |

---

## Best Practices

!!! tip "Idempotency"
    Design procedures to be idempotent - safe to run multiple times without side effects.

!!! warning "Rate Limits"
    External APIs often have rate limits. Add appropriate delays or batching.

!!! tip "Error Handling"
    Always wrap external calls in TRY/CATCH blocks:
    
    ```sql
    TRY
        SLACK_SEND('#alerts', message);
    CATCH error
        PRINT 'Slack notification failed: ' || error;
        -- Fallback to another notification method
    END TRY;
    ```
