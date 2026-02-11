# Creating Agents

This guide covers everything you need to know about creating agents in Moltler.

## Agent Syntax

```sql
CREATE AGENT agent_name
GOAL 'description'
SKILLS [skill1, skill2, ...]
[EXECUTION mode]
[TRIGGERS [...]]
[MODEL 'model_name']
[CONFIG {...}]
BEGIN
    -- Agent logic
END AGENT;
```

---

## Goals

### Defining Goals

A goal should be:

- **Specific** - Clear about what to achieve
- **Measurable** - Possible to determine success
- **Actionable** - Can be accomplished with available skills

```sql
-- Good goals
GOAL 'Maintain 99.9% uptime for production services'
GOAL 'Respond to critical incidents within 5 minutes'
GOAL 'Reduce mean time to recovery below 30 minutes'

-- Bad goals (too vague)
GOAL 'Make things better'
GOAL 'Help with operations'
```

### Goal Metrics

Define success metrics:

```sql
CREATE AGENT uptime_guardian
GOAL 'Maintain 99.9% uptime for production services'
GOAL_METRICS {
    "uptime_target": 0.999,
    "mttr_target_minutes": 30,
    "incident_response_time_minutes": 5
}
...
```

---

## Skills

### Skill Selection

Choose skills that:

1. Work toward the goal
2. Complement each other
3. Cover the required actions

```sql
SKILLS [
    -- Detection
    detect_anomaly,
    analyze_metrics,
    
    -- Diagnosis
    correlate_events,
    identify_root_cause,
    
    -- Remediation
    scale_service,
    restart_pods,
    rollback_deployment,
    
    -- Communication
    notify_team,
    update_status_page
]
```

### Skill Versions

Pin specific versions for stability:

```sql
SKILLS [
    detect_anomaly@1.0.0,
    analyze_metrics@^2.0.0,  -- Any 2.x
    notify_team@latest
]
```

### Skill Permissions

Restrict which skills can be auto-executed:

```sql
SKILLS [
    check_health,           -- Can auto-execute
    restart_service[manual],  -- Requires manual approval
    delete_data[forbidden]    -- Cannot execute
]
```

---

## Execution Modes

### Autonomous

Agent acts without human intervention:

```sql
CREATE AGENT auto_scaler
EXECUTION autonomous
BEGIN
    -- Agent can execute any skill automatically
END AGENT;
```

Use for:

- Low-risk operations
- Time-critical responses
- Well-tested scenarios

### Human Approval

Requires approval before each action:

```sql
CREATE AGENT careful_responder
EXECUTION human_approval
BEGIN
    DECLARE plan = AGENT_PLAN(context);
    
    FOR action IN plan LOOP
        -- Waits for human approval
        AWAIT APPROVAL(action);
        CALL action.skill(action.params);
    END LOOP;
END AGENT;
```

Use for:

- High-risk operations
- Production changes
- New or untested scenarios

### Supervised

Human can observe and intervene at any point:

```sql
CREATE AGENT supervised_worker
EXECUTION supervised
BEGIN
    -- Human receives notifications but doesn't need to approve
    -- Can intervene at any time
END AGENT;
```

Use for:

- Training new agents
- Gradual trust building
- Audit requirements

### Dry Run

Simulates actions without executing:

```sql
CREATE AGENT test_agent
EXECUTION dry_run
BEGIN
    -- All actions are simulated and logged
    -- Nothing actually executes
END AGENT;
```

Use for:

- Testing agent logic
- Validating behavior
- Demonstrating capabilities

---

## Triggers

### Scheduled Triggers

Run on a schedule:

```sql
TRIGGERS [
    ON SCHEDULE '*/5 * * * *',     -- Every 5 minutes
    ON SCHEDULE '0 * * * *',       -- Every hour
    ON SCHEDULE '0 9 * * MON-FRI'  -- 9 AM weekdays
]
```

### Event Triggers

Respond to events:

```sql
TRIGGERS [
    ON EVENT 'alert.fired',
    ON EVENT 'deployment.completed',
    ON EVENT 'ticket.created'
]
```

### Alert Triggers

Respond to specific alerts:

```sql
TRIGGERS [
    ON ALERT 'high-cpu-usage',
    ON ALERT WHERE severity = 'critical',
    ON ALERT WHERE tags CONTAINS 'production'
]
```

### Manual Triggers

Allow manual invocation:

```sql
TRIGGERS [
    ON MANUAL,  -- Can be invoked manually
    ON SCHEDULE '0 * * * *'  -- Also runs hourly
]
```

### Combined Triggers

```sql
TRIGGERS [
    ON SCHEDULE '*/5 * * * *',
    ON ALERT WHERE severity IN ('high', 'critical'),
    ON EVENT 'deployment.*',
    ON MANUAL
]
```

---

## AI Models

### Model Selection

Choose an AI model for decision-making:

```sql
CREATE AGENT smart_responder
MODEL 'gpt-4'  -- OpenAI GPT-4
BEGIN
    -- Agent uses GPT-4 for complex decisions
END AGENT;
```

Available models:

| Model | Best For |
|-------|----------|
| `gpt-4` | Complex reasoning, nuanced decisions |
| `gpt-3.5-turbo` | Faster, simpler decisions |
| `claude-3` | Long context, detailed analysis |
| `local` | Privacy-sensitive, offline operation |

### Model Configuration

```sql
CREATE AGENT my_agent
MODEL 'gpt-4'
MODEL_CONFIG {
    "temperature": 0.3,      -- More deterministic
    "max_tokens": 1000,
    "system_prompt": "You are an SRE assistant focused on reliability."
}
BEGIN
    ...
END AGENT;
```

---

## Configuration

### Agent Configuration

```sql
CREATE AGENT my_agent
CONFIG {
    -- Timeouts
    "action_timeout": "5m",
    "total_timeout": "30m",
    
    -- Limits
    "max_actions": 10,
    "max_retries": 3,
    
    -- Behavior
    "continue_on_error": false,
    "log_level": "debug",
    
    -- Escalation
    "escalate_after": "15m",
    "escalation_channel": "#oncall"
}
BEGIN
    ...
END AGENT;
```

### Environment-Specific Config

```sql
CREATE AGENT my_agent
CONFIG {
    "production": {
        "execution": "human_approval",
        "max_actions": 5
    },
    "staging": {
        "execution": "autonomous",
        "max_actions": 20
    }
}
BEGIN
    ...
END AGENT;
```

---

## Agent Logic

### Basic Logic

```sql
BEGIN
    -- Simple if/then
    IF condition THEN
        CALL skill();
    END IF;
END AGENT;
```

### Context-Aware Logic

```sql
BEGIN
    -- Gather context
    DECLARE context = {
        "trigger": @trigger,
        "time": CURRENT_TIMESTAMP(),
        "day_of_week": EXTRACT_DAY_OF_WEEK(CURRENT_TIMESTAMP())
    };
    
    -- Different behavior on weekends
    IF context.day_of_week IN (0, 6) THEN
        SET context.execution = 'conservative';
    END IF;
    
    -- Make decision
    DECLARE action = AGENT_DECIDE(context);
    CALL action.skill(action.params);
END AGENT;
```

### AI-Powered Logic

```sql
BEGIN
    -- Let AI decide
    DECLARE plan = AGENT_PLAN(
        goal => @goal,
        context => @context,
        available_skills => @skills
    );
    
    -- Execute plan
    FOR step IN plan.steps LOOP
        DECLARE result = CALL step.skill(step.params);
        
        -- Check if goal is met
        IF AGENT_GOAL_MET(result) THEN
            EXIT;
        END IF;
    END LOOP;
END AGENT;
```

### Error Handling

```sql
BEGIN
    TRY
        CALL risky_operation();
    CATCH timeout_error THEN
        CALL notify_team('Operation timed out');
        CALL fallback_operation();
    CATCH OTHERS THEN
        CALL notify_team('Unexpected error: ' || ERROR_MESSAGE());
        RAISE;  -- Re-raise to trigger escalation
    END TRY;
END AGENT;
```

---

## Complete Example

```sql
CREATE AGENT production_guardian
VERSION '1.0.0'
DESCRIPTION 'Monitors and protects production systems'
GOAL 'Maintain 99.9% availability and respond to incidents within SLA'

SKILLS [
    -- Monitoring
    check_service_health,
    analyze_metrics,
    detect_anomalies,
    
    -- Diagnosis
    correlate_events,
    identify_root_cause,
    check_recent_deployments,
    
    -- Remediation
    scale_service,
    restart_unhealthy_pods,
    rollback_deployment[approval_required],
    
    -- Communication
    send_alert,
    update_status_page,
    create_incident_ticket
]

EXECUTION supervised
MODEL 'gpt-4'

TRIGGERS [
    ON SCHEDULE '* * * * *',           -- Every minute
    ON ALERT WHERE severity = 'critical',
    ON EVENT 'deployment.completed',
    ON MANUAL
]

CONFIG {
    "services": ["api", "web", "worker"],
    "health_check_interval": "1m",
    "incident_threshold": 3,
    "auto_remediation_enabled": true,
    "escalation_timeout": "10m"
}

BEGIN
    -- 1. Check health of all services
    FOR service IN CONFIG.services LOOP
        DECLARE health = CALL check_service_health(service);
        
        IF health.status = 'degraded' THEN
            -- Auto-remediate common issues
            IF health.issue = 'high_memory' THEN
                CALL restart_unhealthy_pods(service);
            ELSIF health.issue = 'high_load' THEN
                CALL scale_service(service, replicas => health.current_replicas * 2);
            END IF;
            
            CALL send_alert(
                channel => '#ops',
                message => service || ' is ' || health.status
            );
        
        ELSIF health.status = 'down' THEN
            -- Critical issue - gather context and escalate
            DECLARE context = {
                "service": service,
                "health": health,
                "recent_deployments": CALL check_recent_deployments(service),
                "correlated_events": CALL correlate_events(service)
            };
            
            -- AI determines root cause
            DECLARE root_cause = CALL identify_root_cause(context);
            
            -- Suggest remediation
            DECLARE remediation = AGENT_SUGGEST(
                issue => root_cause,
                available_actions => ['restart', 'scale', 'rollback']
            );
            
            -- Execute with approval
            AWAIT APPROVAL(remediation);
            
            IF remediation.action = 'rollback' THEN
                CALL rollback_deployment(service);
            ELSIF remediation.action = 'scale' THEN
                CALL scale_service(service, replicas => remediation.replicas);
            ELSE
                CALL restart_unhealthy_pods(service);
            END IF;
            
            -- Update status page
            CALL update_status_page(
                component => service,
                status => 'investigating'
            );
            
            -- Create incident ticket
            CALL create_incident_ticket(
                title => 'Service degradation: ' || service,
                context => context,
                root_cause => root_cause
            );
        END IF;
    END LOOP;
END AGENT;
```

---

## What's Next?

<div class="grid cards" markdown>

-   :material-play:{ .lg .middle } __Execution Model__

    How agents make decisions.

    [:octicons-arrow-right-24: Execution Model](execution-model.md)

-   :material-human:{ .lg .middle } __Human-in-the-Loop__

    Approval workflows.

    [:octicons-arrow-right-24: Human-in-the-Loop](human-in-the-loop.md)

</div>
