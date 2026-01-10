# Runbook Integrations

!!! tip "Interactive Notebook"
    This page is generated from a Jupyter notebook. For the best experience, 
    run it interactively in Jupyter:
    
    ```bash
    ./scripts/quick-start.sh
    # Then open http://localhost:8888/notebooks/05-runbook-integrations.ipynb
    ```
    
    [:fontawesome-brands-github: View on GitHub](https://github.com/bahaaldine/elastic-script/blob/main/notebooks/05-runbook-integrations.ipynb){ .md-button }

---


This notebook demonstrates elastic-script's built-in integrations with external services for SRE automation.

## Available Integrations

| Service | Functions | Use Cases |
|---------|-----------|-----------|
| **Kubernetes** | `K8S_GET_PODS()`, `K8S_SCALE_DEPLOYMENT()`, `K8S_GET_LOGS()` | Pod management, scaling |
| **PagerDuty** | `PAGERDUTY_TRIGGER()`, `PAGERDUTY_RESOLVE()` | Incident management |
| **AWS** | `AWS_EC2_DESCRIBE()`, `AWS_LAMBDA_INVOKE()` | Cloud operations |
| **CI/CD** | `GITHUB_TRIGGER_WORKFLOW()`, `JENKINS_TRIGGER_BUILD()` | Deployments |


## Kubernetes Integration


```sql
-- List pods in a namespace
CREATE PROCEDURE list_pods(namespace STRING)
BEGIN
    DECLARE pods ARRAY = K8S_GET_PODS(namespace);
    PRINT 'Pods in ' || namespace || ':';

    FOR pod IN pods LOOP
        PRINT pod['name'] || ' - ' || pod['status'];
    END LOOP

    RETURN pods;
END PROCEDURE

```

```sql
-- Auto-scale based on error rate
CREATE PROCEDURE auto_scale_on_errors(namespace STRING, deployment STRING, threshold NUMBER)
BEGIN
    DECLARE error_count NUMBER = 0;
    DECLARE logs CURSOR FOR
        FROM logs-sample
        | WHERE level == "ERROR"
        | LIMIT 100;

    FOR log IN logs LOOP
        SET error_count = error_count + 1;
    END LOOP

    IF error_count > threshold THEN
        PRINT '⚠️ High error rate detected: ' || error_count;
        -- Scale up the deployment
        DECLARE result DOCUMENT = K8S_SCALE_DEPLOYMENT(namespace, deployment, 3);
        PRINT 'Scaled deployment to 3 replicas';
        RETURN result;
    ELSE
        PRINT '✓ Error rate normal: ' || error_count;
        RETURN { "status": "ok", "errors": error_count };
    END IF
END PROCEDURE

```

## PagerDuty Integration


```sql
-- Create an incident when errors exceed threshold
CREATE PROCEDURE alert_on_critical_errors()
BEGIN
    DECLARE error_count NUMBER = 0;
    DECLARE logs CURSOR FOR
        FROM logs-sample
        | WHERE level == "ERROR"
        | LIMIT 100;

    FOR log IN logs LOOP
        SET error_count = error_count + 1;
    END LOOP

    IF error_count > 50 THEN
        DECLARE incident DOCUMENT = PAGERDUTY_TRIGGER(
            'Critical error rate detected',
            'Error count: ' || error_count || ' in the last hour',
            'critical'
        );
        PRINT '🚨 PagerDuty incident created: ' || incident['id'];
        RETURN incident;
    ELSE
        RETURN { "status": "ok", "message": "No alert needed" };
    END IF
END PROCEDURE

```

## GitHub Actions Integration


```sql
-- Trigger deployment workflow
CREATE PROCEDURE deploy_hotfix(repo STRING, branch STRING)
BEGIN
    PRINT 'Triggering deployment for ' || repo || ' branch: ' || branch;

    DECLARE result DOCUMENT = GITHUB_TRIGGER_WORKFLOW(
        repo,
        'deploy.yml',
        branch,
        { "environment": "production", "hotfix": true }
    );

    PRINT '✓ Workflow triggered: ' || result['run_id'];
    RETURN result;
END PROCEDURE

```

## Complete Incident Response Runbook


```sql
-- Full automated incident response
CREATE PROCEDURE incident_response(service STRING)
BEGIN
    PRINT '🔍 Starting incident response for: ' || service;

    -- 1. Analyze logs
    DECLARE errors CURSOR FOR
        FROM logs-sample
        | WHERE level == "ERROR"
        | LIMIT 20;

    DECLARE error_count NUMBER = 0;
    FOR err IN errors LOOP
        SET error_count = error_count + 1;
    END LOOP

    -- 2. Check if critical
    IF error_count > 10 THEN
        PRINT '⚠️ Critical error rate: ' || error_count;

        -- 3. Scale up pods
        K8S_SCALE_DEPLOYMENT('production', service, 5);
        PRINT '📈 Scaled ' || service || ' to 5 replicas';

        -- 4. Create PagerDuty incident
        DECLARE incident DOCUMENT = PAGERDUTY_TRIGGER(
            service || ' critical errors',
            'Auto-scaled to 5 replicas. Error count: ' || error_count,
            'high'
        );
        PRINT '📟 PagerDuty incident: ' || incident['id'];

        -- 5. Generate AI analysis
        DECLARE prompt STRING = 'Analyze ' || error_count || ' errors for ' || service;
        DECLARE analysis STRING = LLM_COMPLETE(prompt);

        RETURN {
            "service": service,
            "errors": error_count,
            "actions_taken": ["scaled_up", "pagerduty_created"],
            "analysis": analysis
        };
    ELSE
        PRINT '✓ ' || service || ' is healthy. Errors: ' || error_count;
        RETURN { "service": service, "status": "healthy", "errors": error_count };
    END IF
END PROCEDURE

```

