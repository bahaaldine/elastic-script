# Monitoring Agents

Full observability for agent behavior, decisions, and outcomes.

## Overview

Moltler provides comprehensive monitoring:

- **Execution traces** - Every step recorded
- **Decision logs** - Why the agent chose each action
- **Performance metrics** - Timing, success rates, costs
- **Outcome tracking** - Did the agent achieve its goal?

---

## Execution Dashboard

### View All Executions

```sql
SHOW AGENT my_agent EXECUTIONS
WHERE started_at > NOW() - INTERVAL '24h';
```

Output:

```
┌────────────┬─────────────────────┬──────────┬──────────┬─────────┐
│ ID         │ Started             │ Duration │ Status   │ Actions │
├────────────┼─────────────────────┼──────────┼──────────┼─────────┤
│ exec_abc   │ 2026-01-22 10:30:00 │ 45s      │ success  │ 5       │
│ exec_def   │ 2026-01-22 09:15:00 │ 2m       │ failed   │ 3       │
│ exec_ghi   │ 2026-01-21 23:45:00 │ 30s      │ success  │ 4       │
└────────────┴─────────────────────┴──────────┴──────────┴─────────┘
```

### Execution Details

```sql
SHOW AGENT my_agent EXECUTION 'exec_abc';
```

Output:

```
Execution: exec_abc
Agent: production_guardian
Trigger: alert:high-cpu-usage
Started: 2026-01-22T10:30:00Z
Duration: 45s
Status: ✓ success

Timeline:
──────────────────────────────────────────────────────────
10:30:01 │ OBSERVE
         │ Context: {cpu: 92%, memory: 45%, error_rate: 0.1%}
         │ Recent deployments: v2.3.4 (10 min ago)
         │
10:30:02 │ ORIENT
         │ Analysis: High CPU correlates with recent deployment
         │ Confidence: 0.85
         │
10:30:05 │ DECIDE
         │ Plan: [check_deployment, rollback_if_needed, verify]
         │ Reasoning: "Recent deployment most likely cause"
         │
10:30:10 │ ACT: check_deployment_health
         │ Input: {deployment: 'api-gateway', version: 'v2.3.4'}
         │ Output: {healthy: false, error_rate: 5%}
         │ Duration: 5s
         │
10:30:20 │ ACT: rollback_deployment (APPROVAL)
         │ Requested: "Rollback api-gateway to v2.3.3"
         │ Approved by: jane@company.com (15s)
         │
10:30:35 │ ACT: rollback_deployment
         │ Input: {service: 'api-gateway', version: 'v2.3.3'}
         │ Output: {success: true}
         │ Duration: 10s
         │
10:30:45 │ ACT: verify_health
         │ Input: {service: 'api-gateway'}
         │ Output: {healthy: true, cpu: 45%}
         │ Duration: 5s
         │
10:30:45 │ COMPLETE
         │ Goal achieved: CPU normalized, service healthy
──────────────────────────────────────────────────────────
```

---

## Metrics

### Agent Metrics

```sql
SHOW AGENT my_agent METRICS;
```

Output:

```
┌─────────────────────────────┬──────────────────────────────────┐
│ Metric                      │ Value                            │
├─────────────────────────────┼──────────────────────────────────┤
│ Total executions (24h)      │ 47                               │
│ Success rate                │ 94% (44/47)                      │
│ Avg duration                │ 38s                              │
│ Avg actions per execution   │ 4.2                              │
│ Approvals requested         │ 12                               │
│ Approval rate               │ 92% (11/12)                      │
│ Avg approval time           │ 45s                              │
│ Goals achieved              │ 91% (43/47)                      │
│ Escalations                 │ 3                                │
└─────────────────────────────┴──────────────────────────────────┘
```

### Time-Series Metrics

```sql
-- Executions over time
SHOW AGENT my_agent METRICS
WHERE metric = 'executions'
INTERVAL '1h'
RANGE '7d';

-- Success rate trend
SHOW AGENT my_agent METRICS
WHERE metric = 'success_rate'
INTERVAL '1d'
RANGE '30d';
```

### Skill Metrics

```sql
SHOW AGENT my_agent SKILL_METRICS;
```

Output:

```
┌─────────────────────┬───────┬─────────┬──────────────┬────────────┐
│ Skill               │ Calls │ Success │ Avg Duration │ Errors     │
├─────────────────────┼───────┼─────────┼──────────────┼────────────┤
│ check_health        │ 142   │ 100%    │ 2s           │ 0          │
│ analyze_logs        │ 89    │ 98%     │ 8s           │ 2          │
│ scale_service       │ 23    │ 91%     │ 15s          │ 2          │
│ rollback_deployment │ 5     │ 100%    │ 45s          │ 0          │
│ notify_team         │ 67    │ 100%    │ 1s           │ 0          │
└─────────────────────┴───────┴─────────┴──────────────┴────────────┘
```

---

## Decision Analysis

### View Decisions

```sql
SHOW AGENT my_agent DECISIONS
WHERE execution_id = 'exec_abc';
```

Output:

```
Decision Point 1:
  Context: {cpu: 92%, recent_deployment: true}
  Options considered:
    1. check_deployment [score: 0.85] ← SELECTED
    2. scale_service [score: 0.45]
    3. wait_and_monitor [score: 0.30]
  Reasoning: "Recent deployment correlates with CPU spike"

Decision Point 2:
  Context: {deployment_healthy: false, error_rate: 5%}
  Options considered:
    1. rollback_deployment [score: 0.92] ← SELECTED
    2. restart_pods [score: 0.35]
  Reasoning: "Unhealthy deployment with high error rate warrants rollback"
```

### Decision Patterns

```sql
-- Most common decisions
SHOW AGENT my_agent DECISION_PATTERNS
WHERE period = 'last_30d';
```

Output:

```
┌─────────────────────────────┬───────┬─────────────────────────────┐
│ Pattern                     │ Count │ Typical Trigger             │
├─────────────────────────────┼───────┼─────────────────────────────┤
│ check → scale               │ 45    │ High load alert             │
│ check → notify → wait       │ 32    │ Minor issue, off-hours      │
│ check → rollback → verify   │ 12    │ Post-deployment issues      │
│ check → escalate            │ 8     │ Unknown issue type          │
└─────────────────────────────┴───────┴─────────────────────────────┘
```

---

## Alerting

### Agent Alerts

```sql
CREATE ALERT agent_failure
ON AGENT my_agent
WHEN success_rate < 0.9
OVER INTERVAL '1h'
NOTIFY 'slack:#ops';
```

### Common Alert Conditions

```sql
-- Agent not running
WHEN executions = 0 OVER INTERVAL '30m'

-- High failure rate
WHEN success_rate < 0.9 OVER INTERVAL '1h'

-- Long execution time
WHEN avg_duration > '5m' OVER INTERVAL '1h'

-- Too many escalations
WHEN escalations > 5 OVER INTERVAL '24h'

-- Approval timeout
WHEN approval_timeout_rate > 0.2 OVER INTERVAL '1h'
```

---

## Tracing Integration

### View in Kibana APM

Agents automatically integrate with Kibana APM:

1. Open Kibana → APM
2. Find service: `moltler-agents`
3. View agent transactions

### Trace Context

```sql
-- Get trace ID for execution
SHOW AGENT my_agent EXECUTION 'exec_abc' TRACE;
```

Output:

```
Trace ID: abc123def456
Kibana URL: http://kibana:5601/app/apm/traces/abc123def456
```

### Custom Spans

```sql
BEGIN
    -- Create custom span
    DECLARE span = TRACE_START('custom_operation');
    
    -- Do work
    CALL my_operation();
    
    -- End span
    TRACE_END(span);
END AGENT;
```

---

## Logging

### Structured Logs

All agent activity is logged with structured fields:

```json
{
  "timestamp": "2026-01-22T10:30:00Z",
  "level": "INFO",
  "logger": "moltler.agents",
  "agent": "production_guardian",
  "execution_id": "exec_abc",
  "step": "ACT",
  "skill": "scale_service",
  "message": "Scaling service",
  "context": {
    "service": "api-gateway",
    "from_replicas": 3,
    "to_replicas": 10
  }
}
```

### Log Queries

```sql
-- All agent logs
SHOW AGENT my_agent LOGS
WHERE level IN ('WARN', 'ERROR')
LIMIT 100;

-- Specific execution
SHOW AGENT my_agent LOGS
WHERE execution_id = 'exec_abc';
```

---

## Health Checks

### Agent Health

```sql
SHOW AGENT my_agent HEALTH;
```

Output:

```
Agent: production_guardian
Status: ✓ healthy

Components:
  ✓ Scheduler: running
  ✓ Skill registry: 12 skills loaded
  ✓ Approval channel: Slack connected
  ✓ Model: gpt-4 available

Recent issues: None
```

### System Health

```sql
SHOW AGENTS HEALTH;
```

---

## Cost Tracking

### Execution Costs

```sql
SHOW AGENT my_agent COSTS
WHERE period = 'last_30d';
```

Output:

```
Cost breakdown for production_guardian:
  
  LLM API calls:     $45.23 (2,341 calls)
  Elasticsearch:     $12.50 (queries, storage)
  External APIs:     $8.20 (PagerDuty, Slack)
  ─────────────────────────────
  Total:             $65.93
  
  Cost per execution: $0.28 avg
  Cost per success:   $0.30 avg
```

### Cost Optimization

```sql
SHOW AGENT my_agent COST_OPTIMIZATION;
```

Output:

```
Recommendations:
  1. Cache frequent queries (save ~$15/month)
  2. Use gpt-3.5-turbo for simple decisions (save ~$20/month)
  3. Batch similar notifications (save ~$3/month)
```

---

## Dashboards

### Built-in Dashboard

Access the Moltler dashboard:

```
http://localhost:9200/_plugins/moltler/dashboard
```

### Kibana Dashboard

Import the pre-built Kibana dashboard:

```bash
moltler dashboard install --kibana http://localhost:5601
```

### Custom Dashboard

```sql
-- Export metrics for external dashboards
EXPORT AGENT my_agent METRICS
TO 'prometheus'
ENDPOINT 'http://prometheus:9090/api/v1/write';
```

---

## What's Next?

<div class="grid cards" markdown>

-   :material-creation:{ .lg .middle } __AI Features__

    Generate and improve skills with AI.

    [:octicons-arrow-right-24: Generate Skills](../ai/generate-skills.md)

-   :material-book:{ .lg .middle } __Examples__

    Real-world agent examples.

    [:octicons-arrow-right-24: Examples](../examples/observability-skills.md)

</div>
