# Agent Execution Model

This guide explains how Moltler agents observe, decide, and act.

## The OODA Loop

Agents follow the OODA loop:

```
┌─────────────┐
│   OBSERVE   │ ← Gather context
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   ORIENT    │ ← Analyze situation
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   DECIDE    │ ← Choose action
└──────┬──────┘
       │
       ▼
┌─────────────┐
│     ACT     │ ← Execute skill
└──────┬──────┘
       │
       └──────────► (repeat)
```

---

## Observe Phase

The agent gathers context:

```sql
-- Built-in context variables
@trigger       -- What triggered the agent
@context       -- Current state
@history       -- Previous executions
@goal          -- Agent's goal
@skills        -- Available skills
```

### Automatic Context

Moltler automatically provides:

```json
{
  "trigger": {
    "type": "alert",
    "name": "high-cpu-usage",
    "severity": "critical",
    "time": "2026-01-22T10:30:00Z"
  },
  "environment": {
    "cluster": "production",
    "services": ["api", "web", "worker"]
  },
  "recent_events": [...],
  "metrics": {...}
}
```

### Custom Context

Add custom context:

```sql
BEGIN
    -- Enrich context
    DECLARE context = @context;
    SET context.deployments = CALL check_recent_deployments();
    SET context.oncall = CALL get_oncall_engineer();
    SET context.slo_status = CALL check_slo_budget();
END AGENT;
```

---

## Orient Phase

The agent analyzes the situation:

### Rule-Based Analysis

```sql
BEGIN
    -- Categorize the issue
    DECLARE category = CASE
        WHEN @trigger.alert LIKE '%cpu%' THEN 'performance'
        WHEN @trigger.alert LIKE '%memory%' THEN 'resource'
        WHEN @trigger.alert LIKE '%error%' THEN 'reliability'
        ELSE 'unknown'
    END;
    
    -- Assess severity
    DECLARE severity = CALL assess_severity(category, @context);
END AGENT;
```

### AI-Powered Analysis

```sql
BEGIN
    -- Use AI to understand the situation
    DECLARE analysis = AGENT_ANALYZE(
        context => @context,
        question => 'What is happening and what are the potential causes?'
    );
    
    -- Analysis includes:
    -- - summary: Brief description
    -- - potential_causes: Ranked list
    -- - confidence: 0-1
    -- - additional_context_needed: What else to gather
END AGENT;
```

---

## Decide Phase

The agent chooses what to do:

### Rule-Based Decisions

```sql
BEGIN
    -- Decision tree
    IF @context.severity = 'critical' AND @context.is_business_hours THEN
        SET action = 'escalate_immediately';
    ELSIF @context.recent_deployment THEN
        SET action = 'investigate_deployment';
    ELSE
        SET action = 'gather_more_info';
    END IF;
END AGENT;
```

### AI-Powered Decisions

```sql
BEGIN
    -- AI generates a plan
    DECLARE plan = AGENT_PLAN(
        goal => @goal,
        context => @context,
        constraints => {
            "max_steps": 5,
            "time_limit": "10m",
            "require_approval": ["rollback", "scale_down"]
        }
    );
    
    -- Plan structure:
    -- {
    --   "steps": [
    --     {"skill": "diagnose_issue", "params": {...}, "reason": "..."},
    --     {"skill": "scale_service", "params": {...}, "reason": "..."}
    --   ],
    --   "confidence": 0.85,
    --   "alternatives": [...]
    -- }
END AGENT;
```

### Weighted Decisions

```sql
BEGIN
    -- Score each option
    DECLARE options = [
        {"action": "restart", "score": calculate_restart_score(@context)},
        {"action": "scale", "score": calculate_scale_score(@context)},
        {"action": "rollback", "score": calculate_rollback_score(@context)}
    ];
    
    -- Choose highest scored action
    DECLARE best = ARRAY_REDUCE(options, 
        (best, current) => CASE WHEN current.score > best.score THEN current ELSE best END
    );
    
    CALL execute_action(best.action);
END AGENT;
```

---

## Act Phase

The agent executes skills:

### Sequential Execution

```sql
BEGIN
    -- Execute steps in order
    CALL step_1();
    CALL step_2();
    CALL step_3();
END AGENT;
```

### Conditional Execution

```sql
BEGIN
    DECLARE result = CALL diagnose();
    
    IF result.needs_restart THEN
        CALL restart_service();
    ELSIF result.needs_scaling THEN
        CALL scale_service();
    END IF;
END AGENT;
```

### Parallel Execution

```sql
BEGIN
    -- Execute multiple skills in parallel
    PARALLEL [
        CALL check_logs(),
        CALL check_metrics(),
        CALL check_traces()
    ] | ON_ALL_DONE CALL analyze_results(@results);
END AGENT;
```

### With Approval

```sql
BEGIN
    FOR action IN plan.steps LOOP
        IF action.requires_approval THEN
            AWAIT APPROVAL(action);
        END IF;
        
        CALL action.skill(action.params);
    END LOOP;
END AGENT;
```

---

## State Management

### Execution State

Each execution has state:

```sql
-- Access execution state
@execution.id           -- Unique execution ID
@execution.started_at   -- Start time
@execution.status       -- 'running', 'waiting', 'completed'
@execution.step         -- Current step number
```

### Persistent State

Store state across executions:

```sql
BEGIN
    -- Load previous state
    DECLARE state = AGENT_STATE('incident_count') ?? 0;
    
    -- Update state
    SET state = state + 1;
    
    -- Save state
    AGENT_STATE_SET('incident_count', state);
END AGENT;
```

### Shared State

Share state between agents:

```sql
BEGIN
    -- Read shared state
    DECLARE lock = AGENT_LOCK('service_restart');
    
    IF lock.acquired THEN
        CALL restart_service();
        AGENT_UNLOCK('service_restart');
    ELSE
        PRINT 'Another agent is already restarting';
    END IF;
END AGENT;
```

---

## Error Handling

### Retry Logic

```sql
BEGIN
    DECLARE retries = 0;
    DECLARE max_retries = 3;
    
    WHILE retries < max_retries LOOP
        TRY
            CALL risky_operation();
            EXIT;  -- Success
        CATCH
            SET retries = retries + 1;
            IF retries < max_retries THEN
                WAIT INTERVAL '5s';
            ELSE
                RAISE;
            END IF;
        END TRY;
    END LOOP;
END AGENT;
```

### Fallback Actions

```sql
BEGIN
    TRY
        CALL primary_action();
    CATCH
        CALL fallback_action();
    END TRY;
END AGENT;
```

### Escalation

```sql
BEGIN
    TRY
        CALL automated_response();
    CATCH
        -- Escalate to human
        CALL notify_oncall(
            message => 'Automated response failed',
            error => ERROR_MESSAGE()
        );
        
        -- Wait for human
        AWAIT HUMAN_INTERVENTION();
    END TRY;
END AGENT;
```

---

## Execution Tracing

### Automatic Tracing

Every execution is traced:

```sql
-- View execution trace
SHOW AGENT my_agent EXECUTION '12345';
```

Output:

```
Execution: 12345
Started: 2026-01-22T10:30:00Z
Duration: 45s
Status: completed

Steps:
  1. [10:30:01] check_health() → {status: 'degraded'}
  2. [10:30:05] analyze_cause() → {cause: 'memory_pressure'}
  3. [10:30:10] APPROVAL REQUESTED: scale_service(replicas: 5)
  4. [10:30:25] APPROVED by jane@company.com
  5. [10:30:26] scale_service(replicas: 5) → {success: true}
  6. [10:30:35] verify_health() → {status: 'healthy'}

Result: Goal achieved - service restored
```

### Custom Logging

```sql
BEGIN
    AGENT_LOG('Starting health check', level => 'info');
    
    DECLARE result = CALL check_health();
    
    AGENT_LOG('Health check complete', 
        level => 'debug',
        data => result
    );
END AGENT;
```

---

## Performance Considerations

### Timeouts

```sql
CREATE AGENT my_agent
CONFIG {
    "step_timeout": "30s",
    "total_timeout": "5m"
}
BEGIN
    -- Steps will timeout after 30s
    -- Entire execution will timeout after 5m
END AGENT;
```

### Concurrency

```sql
CREATE AGENT my_agent
CONFIG {
    "max_concurrent_executions": 1,  -- Prevent overlapping
    "queue_overflow": "drop"         -- or "wait"
}
...
```

### Resource Limits

```sql
CREATE AGENT my_agent
CONFIG {
    "max_memory": "256MB",
    "max_api_calls": 100
}
...
```

---

## What's Next?

<div class="grid cards" markdown>

-   :material-human:{ .lg .middle } __Human-in-the-Loop__

    Approval workflows.

    [:octicons-arrow-right-24: Human-in-the-Loop](human-in-the-loop.md)

-   :material-chart-line:{ .lg .middle } __Monitoring__

    Agent observability.

    [:octicons-arrow-right-24: Monitoring](monitoring.md)

</div>
