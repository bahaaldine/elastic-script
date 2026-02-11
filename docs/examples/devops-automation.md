# DevOps Automation Skills

Skills for automating DevOps workflows, CI/CD, and infrastructure management.

## Deployment Skills

### Safe Deployment

```sql
CREATE SKILL safe_deploy
VERSION '1.0.0'
DESCRIPTION 'Performs a safe deployment with validation and rollback capability'
PARAMETERS (
    service STRING,
    version STRING,
    environment STRING DEFAULT 'staging',
    canary_percent NUMBER DEFAULT 10,
    validation_time STRING DEFAULT '5m'
)
RETURNS DOCUMENT
BEGIN
    DECLARE deployment_id = 'deploy-' || service || '-' || 
                            REPLACE(CURRENT_TIMESTAMP(), ':', '-');
    
    -- Pre-deployment checks
    DECLARE health = CALL check_service_health(service);
    IF health.status != 'healthy' THEN
        RETURN {
            "status": "aborted",
            "reason": "Service unhealthy before deployment",
            "health": health
        };
    END IF;
    
    -- Record baseline metrics
    DECLARE baseline = CALL get_service_metrics(service, duration => '10m');
    
    -- Start canary deployment
    CALL log_deployment_start(deployment_id, service, version, environment);
    
    TRY
        -- Deploy canary
        CALL k8s_set_image(
            deployment => service,
            container => service,
            image => 'registry/' || service || ':' || version,
            replicas_percent => canary_percent
        );
        
        -- Wait for pods to be ready
        CALL wait_for_pods_ready(service, timeout => '2m');
        
        -- Validate canary
        WAIT INTERVAL validation_time;
        
        DECLARE canary_metrics = CALL get_service_metrics(service, duration => validation_time);
        DECLARE validation = CALL compare_metrics(baseline, canary_metrics);
        
        IF validation.status != 'pass' THEN
            -- Rollback canary
            CALL k8s_rollback(deployment => service);
            
            RETURN {
                "status": "rolled_back",
                "deployment_id": deployment_id,
                "reason": "Canary validation failed",
                "validation": validation
            };
        END IF;
        
        -- Full rollout
        CALL k8s_set_image(
            deployment => service,
            container => service,
            image => 'registry/' || service || ':' || version,
            replicas_percent => 100
        );
        
        CALL wait_for_pods_ready(service, timeout => '5m');
        
        -- Final validation
        WAIT INTERVAL '1m';
        DECLARE final_health = CALL check_service_health(service);
        
        IF final_health.status != 'healthy' THEN
            CALL k8s_rollback(deployment => service);
            RETURN {
                "status": "rolled_back",
                "deployment_id": deployment_id,
                "reason": "Post-deployment health check failed"
            };
        END IF;
        
        CALL log_deployment_complete(deployment_id, 'success');
        
        RETURN {
            "status": "success",
            "deployment_id": deployment_id,
            "service": service,
            "version": version,
            "environment": environment
        };
        
    CATCH
        CALL k8s_rollback(deployment => service);
        CALL log_deployment_complete(deployment_id, 'failed', ERROR_MESSAGE());
        
        RETURN {
            "status": "failed",
            "deployment_id": deployment_id,
            "error": ERROR_MESSAGE()
        };
    END TRY;
END SKILL;
```

### Rollback Deployment

```sql
CREATE SKILL rollback_deployment
VERSION '1.0.0'
DESCRIPTION 'Rolls back a deployment to the previous version'
PARAMETERS (
    service STRING,
    environment STRING DEFAULT 'production',
    to_version STRING DEFAULT NULL  -- NULL means previous version
)
RETURNS DOCUMENT
BEGIN
    -- Get current and previous versions
    DECLARE history CURSOR FOR
        FROM deployments-*
        | WHERE service.name == service
        | WHERE environment == environment
        | WHERE status == 'success'
        | SORT @timestamp DESC
        | LIMIT 2;
    
    OPEN history;
    DECLARE current_deploy, previous_deploy;
    FETCH history INTO current_deploy;
    FETCH history INTO previous_deploy;
    CLOSE history;
    
    DECLARE target_version = to_version ?? previous_deploy.version;
    
    IF target_version IS NULL THEN
        RETURN {
            "status": "error",
            "reason": "No previous version found"
        };
    END IF;
    
    -- Perform rollback
    CALL k8s_set_image(
        deployment => service,
        container => service,
        image => 'registry/' || service || ':' || target_version
    );
    
    CALL wait_for_pods_ready(service, timeout => '3m');
    
    -- Verify health
    DECLARE health = CALL check_service_health(service);
    
    -- Notify team
    CALL slack_send(
        channel => '#deployments',
        message => ':rewind: Rolled back ' || service || 
                   ' from ' || current_deploy.version || 
                   ' to ' || target_version
    );
    
    RETURN {
        "status": "success",
        "service": service,
        "from_version": current_deploy.version,
        "to_version": target_version,
        "health": health
    };
END SKILL;
```

---

## CI/CD Integration

### Trigger Pipeline

```sql
CREATE SKILL trigger_pipeline
VERSION '1.0.0'
DESCRIPTION 'Triggers a CI/CD pipeline'
PARAMETERS (
    provider STRING CHECK IN ('github', 'gitlab', 'jenkins'),
    repo STRING,
    workflow STRING,
    branch STRING DEFAULT 'main',
    inputs DOCUMENT DEFAULT {}
)
RETURNS DOCUMENT
BEGIN
    DECLARE result;
    
    IF provider = 'github' THEN
        SET result = GITHUB_WORKFLOW(
            repo => repo,
            workflow => workflow,
            ref => branch,
            inputs => inputs
        );
    ELSIF provider = 'gitlab' THEN
        SET result = GITLAB_PIPELINE(
            project => repo,
            ref => branch,
            variables => inputs
        );
    ELSIF provider = 'jenkins' THEN
        SET result = JENKINS_BUILD(
            job => workflow,
            parameters => inputs
        );
    END IF;
    
    RETURN {
        "provider": provider,
        "triggered_at": CURRENT_TIMESTAMP(),
        "run_id": result.id,
        "url": result.url
    };
END SKILL;
```

### Wait for Pipeline

```sql
CREATE SKILL wait_for_pipeline
VERSION '1.0.0'
DESCRIPTION 'Waits for a CI/CD pipeline to complete'
PARAMETERS (
    provider STRING,
    run_id STRING,
    timeout STRING DEFAULT '30m',
    poll_interval STRING DEFAULT '30s'
)
RETURNS DOCUMENT
BEGIN
    DECLARE deadline = CURRENT_TIMESTAMP() + INTERVAL timeout;
    DECLARE status = 'pending';
    DECLARE result;
    
    WHILE status IN ('pending', 'in_progress', 'queued') 
          AND CURRENT_TIMESTAMP() < deadline LOOP
        
        IF provider = 'github' THEN
            SET result = GITHUB_WORKFLOW_STATUS(run_id => run_id);
        ELSIF provider = 'gitlab' THEN
            SET result = GITLAB_PIPELINE_STATUS(pipeline_id => run_id);
        ELSIF provider = 'jenkins' THEN
            SET result = JENKINS_STATUS(build_id => run_id);
        END IF;
        
        SET status = result.status;
        
        IF status NOT IN ('pending', 'in_progress', 'queued') THEN
            EXIT;
        END IF;
        
        WAIT INTERVAL poll_interval;
    END LOOP;
    
    RETURN {
        "provider": provider,
        "run_id": run_id,
        "status": status,
        "conclusion": result.conclusion,
        "duration": result.duration,
        "url": result.url
    };
END SKILL;
```

---

## Infrastructure Management

### Scale Service

```sql
CREATE SKILL scale_service
VERSION '1.0.0'
DESCRIPTION 'Scales a Kubernetes service'
PARAMETERS (
    service STRING,
    replicas NUMBER DEFAULT NULL,
    scale_factor NUMBER DEFAULT NULL,
    min_replicas NUMBER DEFAULT 1,
    max_replicas NUMBER DEFAULT 50
)
RETURNS DOCUMENT
BEGIN
    -- Get current replicas
    DECLARE current = K8S_GET(
        kind => 'deployment',
        name => service
    );
    
    DECLARE current_replicas = current.spec.replicas;
    DECLARE target_replicas;
    
    IF replicas IS NOT NULL THEN
        SET target_replicas = replicas;
    ELSIF scale_factor IS NOT NULL THEN
        SET target_replicas = ROUND(current_replicas * scale_factor);
    ELSE
        RETURN {"error": "Must specify replicas or scale_factor"};
    END IF;
    
    -- Apply limits
    SET target_replicas = GREATEST(min_replicas, 
                          LEAST(max_replicas, target_replicas));
    
    -- Scale
    K8S_SCALE(
        kind => 'deployment',
        name => service,
        replicas => target_replicas
    );
    
    -- Wait for rollout
    CALL wait_for_pods_ready(service, timeout => '3m');
    
    RETURN {
        "service": service,
        "previous_replicas": current_replicas,
        "current_replicas": target_replicas,
        "scaled_at": CURRENT_TIMESTAMP()
    };
END SKILL;
```

### Drain Node

```sql
CREATE SKILL drain_node
VERSION '1.0.0'
DESCRIPTION 'Safely drains a Kubernetes node for maintenance'
PARAMETERS (
    node_name STRING,
    timeout STRING DEFAULT '10m',
    force BOOLEAN DEFAULT FALSE
)
RETURNS DOCUMENT
BEGIN
    -- Cordon the node
    K8S_PATCH(
        kind => 'node',
        name => node_name,
        patch => {"spec": {"unschedulable": true}}
    );
    
    -- Get pods on node
    DECLARE pods = K8S_GET(
        kind => 'pod',
        field_selector => 'spec.nodeName=' || node_name
    );
    
    DECLARE evicted = 0;
    DECLARE failed = 0;
    
    -- Evict pods
    FOR pod IN pods.items LOOP
        TRY
            K8S_EVICT(
                name => pod.metadata.name,
                namespace => pod.metadata.namespace
            );
            SET evicted = evicted + 1;
        CATCH
            IF force THEN
                K8S_DELETE(
                    kind => 'pod',
                    name => pod.metadata.name,
                    namespace => pod.metadata.namespace,
                    grace_period => 0
                );
                SET evicted = evicted + 1;
            ELSE
                SET failed = failed + 1;
            END IF;
        END TRY;
    END LOOP;
    
    RETURN {
        "node": node_name,
        "status": CASE WHEN failed = 0 THEN 'drained' ELSE 'partial' END,
        "pods_evicted": evicted,
        "pods_failed": failed
    };
END SKILL;
```

---

## Certificate Management

### Check Certificate Expiry

```sql
CREATE SKILL check_certificate_expiry
VERSION '1.0.0'
DESCRIPTION 'Checks SSL certificate expiration dates'
PARAMETERS (
    domains ARRAY,
    warning_days NUMBER DEFAULT 30,
    critical_days NUMBER DEFAULT 7
)
RETURNS DOCUMENT
BEGIN
    DECLARE results ARRAY = [];
    DECLARE expiring ARRAY = [];
    
    FOR domain IN domains LOOP
        TRY
            DECLARE cert = HTTP_GET_CERTIFICATE(domain);
            DECLARE days_until_expiry = DATE_DIFF('day', 
                CURRENT_TIMESTAMP(), cert.not_after);
            
            DECLARE status = CASE
                WHEN days_until_expiry <= critical_days THEN 'critical'
                WHEN days_until_expiry <= warning_days THEN 'warning'
                ELSE 'ok'
            END;
            
            DECLARE result = {
                "domain": domain,
                "issuer": cert.issuer,
                "expires": cert.not_after,
                "days_until_expiry": days_until_expiry,
                "status": status
            };
            
            SET results = ARRAY_APPEND(results, result);
            
            IF status IN ('warning', 'critical') THEN
                SET expiring = ARRAY_APPEND(expiring, result);
            END IF;
        CATCH
            SET results = ARRAY_APPEND(results, {
                "domain": domain,
                "status": "error",
                "error": ERROR_MESSAGE()
            });
        END TRY;
    END LOOP;
    
    RETURN {
        "checked_at": CURRENT_TIMESTAMP(),
        "total_domains": ARRAY_LENGTH(domains),
        "expiring_count": ARRAY_LENGTH(expiring),
        "results": results,
        "expiring": expiring
    };
END SKILL;
```

### Rotate Secret

```sql
CREATE SKILL rotate_secret
VERSION '1.0.0'
DESCRIPTION 'Rotates a Kubernetes secret'
PARAMETERS (
    secret_name STRING,
    namespace STRING DEFAULT 'default',
    generator STRING CHECK IN ('random', 'aws_iam', 'vault')
)
RETURNS DOCUMENT
BEGIN
    -- Generate new secret value
    DECLARE new_value;
    
    IF generator = 'random' THEN
        SET new_value = GENERATE_RANDOM_STRING(32);
    ELSIF generator = 'aws_iam' THEN
        DECLARE keys = AWS_IAM_ROTATE_KEY();
        SET new_value = keys.secret_access_key;
    ELSIF generator = 'vault' THEN
        DECLARE token = VAULT_GENERATE_TOKEN();
        SET new_value = token.auth.client_token;
    END IF;
    
    -- Backup old secret
    DECLARE old_secret = K8S_GET(
        kind => 'secret',
        name => secret_name,
        namespace => namespace
    );
    
    -- Update secret
    K8S_PATCH(
        kind => 'secret',
        name => secret_name,
        namespace => namespace,
        patch => {
            "data": {
                "value": BASE64_ENCODE(new_value)
            }
        }
    );
    
    -- Restart deployments using this secret
    DECLARE deployments = K8S_GET(
        kind => 'deployment',
        namespace => namespace,
        label_selector => 'uses-secret=' || secret_name
    );
    
    FOR deploy IN deployments.items LOOP
        K8S_ROLLOUT_RESTART(
            kind => 'deployment',
            name => deploy.metadata.name,
            namespace => namespace
        );
    END LOOP;
    
    RETURN {
        "secret": secret_name,
        "rotated_at": CURRENT_TIMESTAMP(),
        "deployments_restarted": ARRAY_LENGTH(deployments.items)
    };
END SKILL;
```

---

## Cost Management

### Find Unused Resources

```sql
CREATE SKILL find_unused_resources
VERSION '1.0.0'
DESCRIPTION 'Identifies unused cloud resources'
PARAMETERS (
    provider STRING CHECK IN ('aws', 'gcp', 'azure'),
    idle_days NUMBER DEFAULT 7
)
RETURNS DOCUMENT
BEGIN
    DECLARE unused ARRAY = [];
    
    IF provider = 'aws' THEN
        -- Check EC2 instances
        DECLARE instances = AWS_EC2_DESCRIBE(
            filters => [{"Name": "instance-state-name", "Values": ["running"]}]
        );
        
        FOR instance IN instances LOOP
            DECLARE metrics = AWS_CLOUDWATCH_GET(
                metric_name => 'CPUUtilization',
                dimensions => [{"Name": "InstanceId", "Value": instance.InstanceId}],
                start_time => NOW() - INTERVAL idle_days || 'd',
                end_time => NOW(),
                statistics => ['Average']
            );
            
            IF metrics.average < 5 THEN  -- Less than 5% CPU
                SET unused = ARRAY_APPEND(unused, {
                    "type": "ec2_instance",
                    "id": instance.InstanceId,
                    "name": instance.Tags.Name,
                    "avg_cpu": metrics.average,
                    "monthly_cost": instance.estimated_monthly_cost
                });
            END IF;
        END LOOP;
        
        -- Check EBS volumes
        DECLARE volumes = AWS_EBS_DESCRIBE(
            filters => [{"Name": "status", "Values": ["available"]}]
        );
        
        FOR volume IN volumes LOOP
            SET unused = ARRAY_APPEND(unused, {
                "type": "ebs_volume",
                "id": volume.VolumeId,
                "size_gb": volume.Size,
                "monthly_cost": volume.Size * 0.10  -- $0.10/GB estimate
            });
        END LOOP;
    END IF;
    
    DECLARE total_savings = ARRAY_REDUCE(unused, 
        (sum, item) => sum + (item.monthly_cost ?? 0), 0);
    
    RETURN {
        "provider": provider,
        "analyzed_at": CURRENT_TIMESTAMP(),
        "unused_resources": ARRAY_LENGTH(unused),
        "potential_monthly_savings": ROUND(total_savings, 2),
        "resources": unused
    };
END SKILL;
```

---

## What's Next?

<div class="grid cards" markdown>

-   :material-tools:{ .lg .middle } __CLI Reference__

    Use the Moltler CLI.

    [:octicons-arrow-right-24: CLI](../tools/cli.md)

-   :material-language-python:{ .lg .middle } __Python SDK__

    Integrate with Python.

    [:octicons-arrow-right-24: Python SDK](../tools/python-sdk.md)

</div>
