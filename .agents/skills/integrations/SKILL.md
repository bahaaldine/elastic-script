---
name: integrations
description: Integrate with external services including AWS, Kubernetes, CI/CD, Terraform, and webhooks. Use when the user needs to automate workflows across multiple systems or trigger external actions.
---

# Integrations

This skill enables you to integrate Elasticsearch with external services for comprehensive automation.

## When to Use

- User needs to **trigger external actions** based on data
- User wants to **automate workflows** across systems
- User needs **AWS**, **Kubernetes**, or **cloud** operations
- User wants to **sync data** with external services
- User asks about **webhooks** or **HTTP integrations**

## HTTP/Webhooks

| Function | Description | Example |
|----------|-------------|---------|
| `HTTP_GET(url, headers?)` | GET request | `HTTP_GET('https://api.example.com/data')` |
| `HTTP_POST(url, body, headers?)` | POST request | `HTTP_POST('https://...', {...})` |
| `WEBHOOK(url, payload)` | Send webhook | `WEBHOOK('https://...', {...})` |

### Generic HTTP Request

```sql
DECLARE response DOCUMENT;
SET response = HTTP_POST(
    'https://api.example.com/v1/data',
    {
        'event': 'alert_triggered',
        'data': {'severity': 'high', 'message': 'Error spike detected'}
    },
    {
        'Authorization': 'Bearer ' || ENV('API_TOKEN'),
        'Content-Type': 'application/json'
    }
);

IF response['status'] = 200 THEN
    PRINT 'Webhook sent successfully';
ELSE
    PRINT 'Webhook failed: ' || response['body'];
END IF;
```

## AWS Integration

| Function | Description | Example |
|----------|-------------|---------|
| `AWS_LAMBDA_INVOKE(function, payload)` | Invoke Lambda | `AWS_LAMBDA_INVOKE('my-func', {...})` |
| `AWS_SSM_RUN(instance, commands)` | Run SSM command | `AWS_SSM_RUN('i-123', ['ls'])` |
| `AWS_SSM_STATUS(command_id)` | Get command status | `AWS_SSM_STATUS('cmd-id')` |
| `AWS_ASG_DESCRIBE(asg_name)` | Describe ASG | `AWS_ASG_DESCRIBE('my-asg')` |
| `AWS_ASG_SET_CAPACITY(asg, min, max, desired)` | Scale ASG | See below |

### Auto-Scale Based on Metrics

```sql
-- Check CPU across fleet
DECLARE avg_cpu NUMBER;
ESQL FROM metrics-*
| WHERE @timestamp > NOW() - 10 MINUTES
| WHERE aws.ec2.instance.id IS NOT NULL
| STATS avg_cpu = AVG(system.cpu.total.pct)
INTO avg_cpu;

DECLARE asg DOCUMENT;
SET asg = AWS_ASG_DESCRIBE('my-web-servers');
DECLARE current NUMBER;
SET current = asg['AutoScalingGroups'][0]['DesiredCapacity'];

IF avg_cpu > 0.80 AND current < 10 THEN
    -- Scale up
    AWS_ASG_SET_CAPACITY('my-web-servers', 2, 10, current + 2);
    SLACK_SEND('#ops', 'Scaling up web servers: ' || current || ' -> ' || (current + 2));
ELSEIF avg_cpu < 0.30 AND current > 2 THEN
    -- Scale down
    AWS_ASG_SET_CAPACITY('my-web-servers', 2, 10, current - 1);
END IF;
```

### Invoke Lambda for Remediation

```sql
DECLARE result DOCUMENT;
SET result = AWS_LAMBDA_INVOKE('restart-service', {
    'service': 'payment-api',
    'reason': 'High error rate detected',
    'triggered_by': 'elasticsearch-runbook'
});

PRINT 'Lambda result: ' || result['Payload'];
```

## Kubernetes Integration

| Function | Description | Example |
|----------|-------------|---------|
| `K8S_GET(resource, name?, namespace?)` | Get resources | `K8S_GET('pods', NULL, 'default')` |
| `K8S_PATCH(resource, name, patch, namespace?)` | Patch resource | See below |
| `K8S_SCALE(resource, name, replicas, namespace?)` | Scale deployment | See below |

### Auto-Scale Kubernetes Deployment

```sql
-- Check app latency
DECLARE p99_latency NUMBER;
ESQL FROM traces-apm*
| WHERE @timestamp > NOW() - 5 MINUTES
| WHERE service.name = 'api-server'
| STATS p99 = PERCENTILE(transaction.duration.us, 99)
INTO p99_latency;

IF p99_latency > 5000000 THEN  -- > 5 seconds
    -- Get current replicas
    DECLARE deploy DOCUMENT;
    SET deploy = K8S_GET('deployments', 'api-server', 'production');
    DECLARE current NUMBER;
    SET current = deploy['spec']['replicas'];
    
    IF current < 10 THEN
        K8S_SCALE('deployments', 'api-server', current + 2, 'production');
        SLACK_SEND('#k8s-ops', 'Scaled api-server: ' || current || ' -> ' || (current + 2) || ' due to high latency');
    END IF;
END IF;
```

### Restart Pods

```sql
-- Rolling restart by patching annotation
DECLARE result DOCUMENT;
SET result = K8S_PATCH('deployments', 'my-app', {
    'spec': {
        'template': {
            'metadata': {
                'annotations': {
                    'kubectl.kubernetes.io/restartedAt': CURRENT_TIMESTAMP()
                }
            }
        }
    }
}, 'production');
```

## CI/CD Integration

### GitHub Actions

| Function | Description | Example |
|----------|-------------|---------|
| `GITHUB_WORKFLOW(owner, repo, workflow, ref, inputs?)` | Trigger workflow | See below |
| `GITHUB_WORKFLOW_STATUS(owner, repo, run_id)` | Get status | See below |

```sql
DECLARE run DOCUMENT;
SET run = GITHUB_WORKFLOW(
    'myorg',
    'my-app',
    'deploy.yml',
    'main',
    {'environment': 'staging', 'version': 'v1.2.3'}
);

PRINT 'Triggered workflow run: ' || run['id'];

-- Wait for completion
DECLARE status STRING;
SET status = 'in_progress';
WHILE status = 'in_progress' OR status = 'queued' LOOP
    SLEEP(30);
    DECLARE check DOCUMENT;
    SET check = GITHUB_WORKFLOW_STATUS('myorg', 'my-app', run['id']);
    SET status = check['status'];
END LOOP;

IF status = 'completed' AND check['conclusion'] = 'success' THEN
    SLACK_SEND('#deployments', 'Deployment successful!');
ELSE
    SLACK_SEND('#deployments', 'Deployment failed: ' || check['conclusion']);
END IF;
```

### Jenkins

| Function | Description | Example |
|----------|-------------|---------|
| `JENKINS_BUILD(job, params?)` | Trigger build | `JENKINS_BUILD('deploy-app', {...})` |
| `JENKINS_STATUS(job, build_number)` | Get build status | `JENKINS_STATUS('deploy-app', 42)` |

## Terraform Cloud

| Function | Description | Example |
|----------|-------------|---------|
| `TF_CLOUD_RUN(org, workspace, message?)` | Start run | See below |
| `TF_CLOUD_STATUS(run_id)` | Get run status | `TF_CLOUD_STATUS('run-123')` |
| `TF_CLOUD_WAIT(run_id, timeout?)` | Wait for completion | `TF_CLOUD_WAIT('run-123', 600)` |
| `TF_CLOUD_OUTPUTS(workspace)` | Get outputs | `TF_CLOUD_OUTPUTS('production')` |

### Infrastructure Provisioning

```sql
DECLARE run DOCUMENT;
SET run = TF_CLOUD_RUN('myorg', 'kubernetes-cluster', 'Auto-scale: adding nodes');

-- Wait for plan and apply
DECLARE final_status DOCUMENT;
SET final_status = TF_CLOUD_WAIT(run['id'], 1800);  -- 30 min timeout

IF final_status['status'] = 'applied' THEN
    SLACK_SEND('#infrastructure', 'Infrastructure update complete');
ELSE
    SLACK_SEND('#infrastructure', 'Infrastructure update failed: ' || final_status['status']);
END IF;
```

## S3 Integration

| Function | Description | Example |
|----------|-------------|---------|
| `S3_GET(bucket, key)` | Get object | `S3_GET('my-bucket', 'config.json')` |
| `S3_PUT(bucket, key, content, content_type?)` | Put object | `S3_PUT('logs', 'report.json', {...})` |
| `S3_LIST(bucket, prefix?)` | List objects | `S3_LIST('my-bucket', 'reports/')` |

### Export Report to S3

```sql
-- Generate report
DECLARE report DOCUMENT;
ESQL FROM logs-*
| WHERE @timestamp > NOW() - 24 HOURS
| STATS 
    total = COUNT(*),
    errors = SUM(CASE WHEN level = 'ERROR' THEN 1 ELSE 0 END)
BY service.name
INTO report;

-- Upload to S3
S3_PUT(
    'reports-bucket',
    'daily-reports/' || CURRENT_DATE() || '.json',
    report,
    'application/json'
);
```

## Pre-built Skills (Moltler)

| Skill | Description |
|-------|-------------|
| `RUN SKILL trigger_github_workflow(repo, workflow)` | Trigger CI/CD |
| `RUN SKILL scale_kubernetes(deployment, replicas)` | Scale K8s |
| `RUN SKILL invoke_lambda(function, payload)` | Run Lambda |
| `RUN SKILL trigger_terraform_run(workspace)` | Apply Terraform |

## Best Practices

1. **Use environment variables** for credentials (never hardcode)
2. **Add timeouts** to external calls
3. **Handle failures gracefully** - External systems can be unavailable
4. **Log all external actions** for audit trails
5. **Test in staging** before production automation
