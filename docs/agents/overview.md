# Agents Overview

Agents are autonomous executors that use skills to achieve goals.

## What is an Agent?

An **Agent** is an AI-powered entity that:

1. **Has a goal** - A high-level objective to achieve
2. **Uses skills** - Executes skills to accomplish tasks
3. **Makes decisions** - Chooses which skills to run based on context
4. **Is observable** - Every action is logged and traceable
5. **Is controllable** - Human-in-the-loop for critical decisions

```sql
CREATE AGENT incident_responder
GOAL 'Investigate and mitigate production incidents'
SKILLS [
    diagnose_issue,
    check_deployments,
    rollback_service,
    notify_team,
    create_postmortem
]
EXECUTION human_approval  -- Require approval for actions
BEGIN
    -- Agent logic
END AGENT;
```

---

## Why Agents?

### Traditional Automation

```
Alert → Run Script → Hope for the Best
```

Problems:

- Rigid, one-size-fits-all
- No context awareness
- Hard to debug
- Requires constant updates

### Moltler Agents

```
Alert → Agent Observes → Agent Decides → Agent Acts → Agent Learns
```

Benefits:

- Context-aware decisions
- Full observability
- Human oversight
- Continuous improvement

---

## Agent Components

### Goal

The high-level objective:

```sql
GOAL 'Ensure production systems maintain 99.9% uptime'
GOAL 'Respond to security incidents within SLA'
GOAL 'Optimize cloud costs while maintaining performance'
```

### Skills

The capabilities available to the agent:

```sql
SKILLS [
    check_service_health,
    analyze_logs,
    scale_resources,
    notify_team,
    create_incident
]
```

### Execution Mode

How the agent makes decisions:

| Mode | Description |
|------|-------------|
| `autonomous` | Agent acts without approval |
| `human_approval` | Requires approval for actions |
| `supervised` | Human can intervene at any step |
| `dry_run` | Simulates actions without executing |

```sql
EXECUTION human_approval
```

### Triggers

When the agent activates:

```sql
TRIGGERS [
    ON SCHEDULE '*/5 * * * *',           -- Every 5 minutes
    ON ALERT 'high-error-rate',          -- On specific alert
    ON EVENT 'deployment.completed',      -- On event
    ON MANUAL                             -- Manual invocation
]
```

---

## Creating an Agent

### Basic Agent

```sql
CREATE AGENT health_monitor
GOAL 'Monitor system health and alert on issues'
SKILLS [check_health, send_alert]
EXECUTION autonomous
TRIGGERS [ON SCHEDULE '*/5 * * * *']
BEGIN
    DECLARE health = CALL check_health();
    
    IF health.status != 'healthy' THEN
        CALL send_alert(
            title => 'Health Check Failed',
            details => health
        );
    END IF;
END AGENT;
```

### AI-Powered Agent

```sql
CREATE AGENT intelligent_responder
GOAL 'Intelligently respond to production issues'
SKILLS [
    diagnose_issue,
    check_logs,
    check_metrics,
    scale_service,
    restart_service,
    rollback_deployment,
    notify_oncall
]
EXECUTION human_approval
TRIGGERS [ON ALERT 'production-issue']
MODEL 'gpt-4'  -- AI model for decision making
BEGIN
    -- Agent uses AI to decide which skills to call
    -- based on the current context and goal
END AGENT;
```

---

## Agent Lifecycle

### 1. Trigger

Agent activates based on schedule, event, or manual invocation.

### 2. Observe

Agent gathers context:
- Current system state
- Recent events
- Historical patterns

### 3. Decide

Agent determines actions:
- Which skills to run
- In what order
- With what parameters

### 4. Act

Agent executes skills:
- With approval if required
- Logging every step
- Handling errors gracefully

### 5. Learn

Agent improves:
- Records outcomes
- Updates decision weights
- Suggests improvements

---

## Example Agents

### Incident Response Agent

```sql
CREATE AGENT incident_responder
GOAL 'Minimize incident impact and restore service'
SKILLS [
    detect_anomaly,
    correlate_events,
    identify_root_cause,
    execute_runbook,
    notify_stakeholders,
    create_postmortem
]
EXECUTION human_approval
TRIGGERS [ON ALERT 'severity:critical']
MODEL 'gpt-4'
CONFIG {
    "escalation_timeout": "15m",
    "max_auto_actions": 3
}
BEGIN
    -- 1. Gather context
    DECLARE context = {
        "alert": @trigger.alert,
        "time": CURRENT_TIMESTAMP(),
        "recent_deployments": CALL check_recent_deployments(),
        "current_metrics": CALL get_current_metrics()
    };
    
    -- 2. AI decides next steps
    DECLARE plan = AGENT_PLAN(context);
    
    -- 3. Execute plan with approval
    FOR step IN plan.steps LOOP
        AWAIT APPROVAL(step);
        CALL step.skill(step.parameters);
    END LOOP;
END AGENT;
```

### Cost Optimization Agent

```sql
CREATE AGENT cost_optimizer
GOAL 'Reduce cloud costs while maintaining performance'
SKILLS [
    analyze_resource_usage,
    identify_waste,
    recommend_rightsizing,
    schedule_scaling,
    report_savings
]
EXECUTION supervised
TRIGGERS [ON SCHEDULE '0 0 * * 1']  -- Weekly
BEGIN
    DECLARE usage = CALL analyze_resource_usage(days => 30);
    DECLARE waste = CALL identify_waste(usage);
    
    IF waste.potential_savings > 0 THEN
        DECLARE recommendations = CALL recommend_rightsizing(waste);
        
        -- Present recommendations for approval
        AWAIT APPROVAL(recommendations);
        
        -- Apply approved changes
        FOR rec IN recommendations.approved LOOP
            CALL apply_change(rec);
        END LOOP;
        
        CALL report_savings();
    END IF;
END AGENT;
```

### Security Monitor Agent

```sql
CREATE AGENT security_monitor
GOAL 'Detect and respond to security threats'
SKILLS [
    detect_threats,
    analyze_behavior,
    quarantine_host,
    block_ip,
    notify_security_team,
    collect_forensics
]
EXECUTION human_approval
TRIGGERS [ON EVENT 'security.*']
CONFIG {
    "auto_quarantine_threshold": 0.95
}
BEGIN
    DECLARE threat = CALL detect_threats(@trigger.event);
    
    IF threat.confidence > 0.8 THEN
        -- Critical threat - take action
        IF threat.confidence > CONFIG.auto_quarantine_threshold THEN
            CALL quarantine_host(threat.source);
        ELSE
            AWAIT APPROVAL("Quarantine " || threat.source);
            CALL quarantine_host(threat.source);
        END IF;
        
        CALL notify_security_team(threat);
        CALL collect_forensics(threat.source);
    END IF;
END AGENT;
```

---

## Managing Agents

### List Agents

```sql
SHOW AGENTS;
```

### View Agent Details

```sql
SHOW AGENT incident_responder;
```

### Start/Stop Agent

```sql
START AGENT incident_responder;
STOP AGENT incident_responder;
```

### View Agent History

```sql
SHOW AGENT incident_responder HISTORY
WHERE executed_at > NOW() - INTERVAL '7d';
```

### Delete Agent

```sql
DROP AGENT incident_responder;
```

---

## Best Practices

### 1. Start Simple

Begin with basic agents and add complexity gradually:

```sql
-- Start here
CREATE AGENT simple_monitor
SKILLS [check_health, send_alert]
EXECUTION autonomous
BEGIN
    IF NOT CALL check_health() THEN
        CALL send_alert();
    END IF;
END AGENT;

-- Then evolve
CREATE AGENT advanced_monitor
SKILLS [check_health, diagnose_issue, remediate, send_alert]
EXECUTION human_approval
...
```

### 2. Use Human-in-the-Loop

For critical systems, require approval:

```sql
EXECUTION human_approval
```

### 3. Set Guardrails

Limit agent capabilities:

```sql
CONFIG {
    "max_actions_per_run": 5,
    "forbidden_skills": ["delete_data", "terminate_instance"],
    "approval_required_for": ["restart_service", "scale_down"]
}
```

### 4. Monitor Agent Behavior

Review agent decisions regularly:

```sql
-- Check agent effectiveness
SHOW AGENT my_agent METRICS
WHERE period = 'last_30_days';

-- Review decisions
SHOW AGENT my_agent DECISIONS
WHERE outcome = 'suboptimal';
```

---

## What's Next?

<div class="grid cards" markdown>

-   :material-pencil:{ .lg .middle } __Creating Agents__

    Detailed agent creation guide.

    [:octicons-arrow-right-24: Creating Agents](creating-agents.md)

-   :material-play:{ .lg .middle } __Execution Model__

    How agents make decisions.

    [:octicons-arrow-right-24: Execution Model](execution-model.md)

-   :material-human:{ .lg .middle } __Human-in-the-Loop__

    Approval workflows.

    [:octicons-arrow-right-24: Human-in-the-Loop](human-in-the-loop.md)

-   :material-chart-line:{ .lg .middle } __Monitoring__

    Agent observability.

    [:octicons-arrow-right-24: Monitoring](monitoring.md)

</div>
