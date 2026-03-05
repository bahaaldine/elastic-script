---
name: integrating-services
description: >
  Integrate with external services including AWS, Kubernetes, CI/CD, Terraform,
  and webhooks. Use when triggering external actions, automating cross-system
  workflows, or syncing data with external services.
---

# External Integrations

## HTTP/Webhooks

| Function | Example |
|----------|---------|
| `HTTP_GET(url, headers?)` | `HTTP_GET('https://api.example.com/data')` |
| `HTTP_POST(url, body, headers?)` | POST with JSON |
| `WEBHOOK(url, payload)` | Send webhook |

```sql
SET response = HTTP_POST('https://api.example.com/v1/data',
    {'event': 'alert', 'data': {'severity': 'high'}},
    {'Authorization': 'Bearer ' || ENV('API_TOKEN')}
);
```

## AWS

| Function | Example |
|----------|---------|
| `AWS_LAMBDA_INVOKE(function, payload)` | Invoke Lambda |
| `AWS_SSM_RUN(instance, commands)` | Run SSM command |
| `AWS_SSM_STATUS(command_id)` | Command status |
| `AWS_ASG_DESCRIBE(asg_name)` | Describe ASG |
| `AWS_ASG_SET_CAPACITY(asg, min, max, desired)` | Scale ASG |

```sql
-- Auto-scale based on CPU
ESQL FROM metrics-* | WHERE @timestamp > NOW() - 10 MINUTES | STATS avg_cpu = AVG(system.cpu.total.pct) INTO avg_cpu;

IF avg_cpu > 0.80 THEN
    AWS_ASG_SET_CAPACITY('web-servers', 2, 10, current + 2);
END IF;
```

## Kubernetes

| Function | Example |
|----------|---------|
| `K8S_GET(resource, name?, namespace?)` | `K8S_GET('pods', NULL, 'default')` |
| `K8S_PATCH(resource, name, patch, namespace?)` | Patch resource |
| `K8S_SCALE(resource, name, replicas, namespace?)` | Scale deployment |

```sql
-- Scale based on latency
IF p99_latency > 5000000 THEN
    K8S_SCALE('deployments', 'api-server', current + 2, 'production');
END IF;
```

## CI/CD

### GitHub Actions

| Function | Example |
|----------|---------|
| `GITHUB_WORKFLOW(owner, repo, workflow, ref, inputs?)` | Trigger |
| `GITHUB_WORKFLOW_STATUS(owner, repo, run_id)` | Status |

### Jenkins

| Function | Example |
|----------|---------|
| `JENKINS_BUILD(job, params?)` | Trigger build |
| `JENKINS_STATUS(job, build_number)` | Build status |

## Terraform Cloud

| Function | Example |
|----------|---------|
| `TF_CLOUD_RUN(org, workspace, message?)` | Start run |
| `TF_CLOUD_STATUS(run_id)` | Run status |
| `TF_CLOUD_WAIT(run_id, timeout?)` | Wait for completion |
| `TF_CLOUD_OUTPUTS(workspace)` | Get outputs |

## S3

| Function | Example |
|----------|---------|
| `S3_GET(bucket, key)` | Get object |
| `S3_PUT(bucket, key, content, content_type?)` | Put object |
| `S3_LIST(bucket, prefix?)` | List objects |

## Pre-built Skills

| Skill | Description |
|-------|-------------|
| `RUN SKILL trigger_github_workflow(repo, workflow)` | CI/CD |
| `RUN SKILL scale_kubernetes(deployment, replicas)` | K8s scaling |
| `RUN SKILL invoke_lambda(function, payload)` | Lambda |
| `RUN SKILL trigger_terraform_run(workspace)` | Terraform |

## Best Practices

- Use environment variables for credentials
- Add timeouts to external calls
- Handle failures gracefully
- Log all external actions for audit
