# Human-in-the-Loop

Control agent behavior with approval workflows and human oversight.

## Overview

Human-in-the-loop (HITL) ensures humans remain in control of critical decisions:

```
Agent Proposes → Human Reviews → Human Approves/Rejects → Agent Executes
```

---

## Approval Modes

### Per-Action Approval

Every action requires approval:

```sql
CREATE AGENT careful_agent
EXECUTION human_approval
BEGIN
    -- Each skill call waits for approval
    AWAIT APPROVAL("Check service health");
    CALL check_health();
    
    AWAIT APPROVAL("Scale service to 5 replicas");
    CALL scale_service(replicas => 5);
END AGENT;
```

### Conditional Approval

Only certain actions need approval:

```sql
CREATE AGENT smart_agent
EXECUTION autonomous
BEGIN
    -- Safe actions - no approval needed
    CALL check_health();
    CALL gather_metrics();
    
    -- Risky actions - require approval
    IF needs_scaling THEN
        AWAIT APPROVAL("Scale service");
        CALL scale_service();
    END IF;
    
    -- Critical actions - always require approval
    AWAIT APPROVAL("Rollback deployment");
    CALL rollback_deployment();
END AGENT;
```

### Skill-Level Approval

Define approval requirements per skill:

```sql
CREATE AGENT my_agent
SKILLS [
    check_health,                      -- Auto
    analyze_logs,                      -- Auto
    restart_service[approval],         -- Needs approval
    rollback_deployment[approval],     -- Needs approval
    delete_data[forbidden]             -- Cannot execute
]
...
```

---

## Approval Requests

### Basic Request

```sql
AWAIT APPROVAL("Brief description of action");
```

### Detailed Request

```sql
AWAIT APPROVAL({
    "action": "Scale service",
    "details": {
        "service": "api-gateway",
        "current_replicas": 3,
        "target_replicas": 10,
        "reason": "High load detected"
    },
    "risk_level": "medium",
    "timeout": "10m",
    "alternatives": [
        "Wait and monitor",
        "Scale to 5 instead"
    ]
});
```

### With Timeout

```sql
DECLARE approved = AWAIT APPROVAL(
    action => "Restart service",
    timeout => INTERVAL '5m',
    default => 'reject'  -- Auto-reject if no response
);

IF approved THEN
    CALL restart_service();
ELSE
    CALL notify_team("Action timed out or rejected");
END IF;
```

---

## Approval Channels

### Slack

```sql
CREATE AGENT my_agent
APPROVAL_CHANNEL 'slack'
APPROVAL_CONFIG {
    "channel": "#approvals",
    "mention": "@oncall"
}
...
```

Approval request in Slack:

```
🤖 Agent: production_guardian
📋 Action: Scale api-gateway from 3 to 10 replicas
📝 Reason: CPU usage at 85%
⚠️ Risk: Medium

[✅ Approve] [❌ Reject] [ℹ️ Details]
```

### PagerDuty

```sql
CREATE AGENT my_agent
APPROVAL_CHANNEL 'pagerduty'
APPROVAL_CONFIG {
    "service_id": "P123ABC",
    "urgency": "high"
}
...
```

### Email

```sql
CREATE AGENT my_agent
APPROVAL_CHANNEL 'email'
APPROVAL_CONFIG {
    "recipients": ["oncall@company.com"],
    "reply_to_approve": true
}
...
```

### Web UI

```sql
CREATE AGENT my_agent
APPROVAL_CHANNEL 'web'
APPROVAL_CONFIG {
    "dashboard_url": "https://moltler.company.com/approvals"
}
...
```

---

## Approval Policies

### Role-Based

```sql
CREATE AGENT my_agent
APPROVAL_POLICY {
    "default": ["sre", "oncall"],
    "production_changes": ["sre-lead", "manager"],
    "data_deletion": ["data-owner", "legal"]
}
...
```

### Time-Based

```sql
CREATE AGENT my_agent
APPROVAL_POLICY {
    "business_hours": {
        "approvers": ["team-member"],
        "auto_approve_after": "10m"
    },
    "off_hours": {
        "approvers": ["oncall"],
        "auto_approve_after": "5m",
        "escalate_after": "2m"
    }
}
...
```

### Risk-Based

```sql
CREATE AGENT my_agent
APPROVAL_POLICY {
    "low_risk": {
        "auto_approve": true
    },
    "medium_risk": {
        "approvers": ["team-member"],
        "count_required": 1
    },
    "high_risk": {
        "approvers": ["team-lead", "manager"],
        "count_required": 2
    }
}
...
```

---

## Escalation

### Automatic Escalation

```sql
CREATE AGENT my_agent
APPROVAL_CONFIG {
    "escalation": [
        {"after": "5m", "to": "team-lead"},
        {"after": "10m", "to": "manager"},
        {"after": "15m", "to": "vp-engineering"}
    ]
}
...
```

### Manual Escalation

```sql
BEGIN
    DECLARE approval = AWAIT APPROVAL(
        action => "Critical action",
        timeout => INTERVAL '5m'
    );
    
    IF approval.status = 'timeout' THEN
        ESCALATE TO 'manager';
        AWAIT APPROVAL(action => "Critical action (escalated)");
    END IF;
END AGENT;
```

---

## Feedback Loop

### Approval Feedback

```sql
BEGIN
    DECLARE result = AWAIT APPROVAL({
        "action": "Scale service",
        "allow_feedback": true
    });
    
    IF result.status = 'rejected' THEN
        -- Learn from rejection
        AGENT_LEARN({
            "action": "scale_service",
            "context": @context,
            "outcome": "rejected",
            "reason": result.rejection_reason
        });
    END IF;
END AGENT;
```

### Post-Action Review

```sql
BEGIN
    CALL action();
    
    -- Request feedback after action
    DECLARE feedback = AWAIT FEEDBACK({
        "action": "scale_service",
        "result": @last_result,
        "question": "Was this the right action?"
    });
    
    AGENT_LEARN(feedback);
END AGENT;
```

---

## Audit Trail

All approvals are logged:

```sql
-- View approval history
SHOW AGENT my_agent APPROVALS
WHERE created_at > NOW() - INTERVAL '7d';
```

Output:

```
┌──────────────────────┬─────────────────────┬──────────┬─────────────┬─────────────────┐
│ Timestamp            │ Action              │ Status   │ Approver    │ Response Time   │
├──────────────────────┼─────────────────────┼──────────┼─────────────┼─────────────────┤
│ 2026-01-22 10:30:00  │ Scale api-gateway   │ approved │ jane@co.com │ 45s             │
│ 2026-01-22 09:15:00  │ Rollback deployment │ rejected │ john@co.com │ 2m              │
│ 2026-01-21 23:45:00  │ Restart pods        │ timeout  │ -           │ 10m (auto-rej)  │
└──────────────────────┴─────────────────────┴──────────┴─────────────┴─────────────────┘
```

---

## Best Practices

### 1. Clear Action Descriptions

```sql
-- Good
AWAIT APPROVAL("Scale api-gateway from 3 to 10 replicas due to 85% CPU usage");

-- Bad
AWAIT APPROVAL("Scale");
```

### 2. Include Context

```sql
AWAIT APPROVAL({
    "action": "Rollback deployment",
    "service": "api-gateway",
    "current_version": "v2.3.4",
    "target_version": "v2.3.3",
    "reason": "Error rate increased 500% after deploy",
    "evidence": {
        "error_rate": "15%",
        "normal_rate": "0.3%",
        "affected_endpoints": ["/api/users", "/api/orders"]
    }
});
```

### 3. Set Reasonable Timeouts

```sql
-- Critical: short timeout, auto-escalate
AWAIT APPROVAL(action => "Critical", timeout => '2m', escalate => true);

-- Non-urgent: longer timeout
AWAIT APPROVAL(action => "Cleanup", timeout => '1h');
```

### 4. Provide Alternatives

```sql
AWAIT APPROVAL({
    "action": "Scale to 10 replicas",
    "alternatives": [
        {"label": "Scale to 5 instead", "action_id": "scale_5"},
        {"label": "Wait 10 minutes", "action_id": "wait"},
        {"label": "Reject and investigate", "action_id": "investigate"}
    ]
});
```

---

## What's Next?

<div class="grid cards" markdown>

-   :material-chart-line:{ .lg .middle } __Monitoring Agents__

    Observe agent behavior.

    [:octicons-arrow-right-24: Monitoring](monitoring.md)

</div>
